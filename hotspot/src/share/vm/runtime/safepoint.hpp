#ifdef USE_PRAGMA_IDENT_HDR
#pragma ident "@(#)safepoint.hpp	1.88 04/06/09 09:33:03 JVM"
#endif
/*
 * Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

//
// Safepoint synchronization
////
// The VMThread or CMS_thread uses the SafepointSynchronize::begin/end
// methods to enter/exit a safepoint region. The begin method will roll
// all JavaThreads forward to a safepoint.
//
// JavaThreads must use the ThreadSafepointState abstraction (defined in
// thread.hpp) to indicate that that they are at a safepoint.
//
// The Mutex/Condition variable and ObjectLocker classes calls the enter/
// exit safepoint methods, when a thread is blocked/restarted. Hence, all mutex exter/
// exit points *must* be at a safepoint.


class ThreadSafepointState;
class SnippetCache;
class nmethod;
class SafepointHandler;

//
// Implements roll-forward to safepoint (safepoint synchronization)
// 
class SafepointSynchronize : AllStatic {
 public:
  enum SynchronizeState {
      _not_synchronized = 0,                   // Threads not synchronized at a safepoint
                                               // Keep this value 0. See the coment in do_call_back()
      _synchronizing    = 1,                   // Synchronizing in progress  
      _synchronized     = 2                    // All Java threads are stopped at a safepoint. Only VM thread is running
  };

  enum SafepointingThread {
      _null_thread  = 0,
      _vm_thread    = 1,
      _other_thread = 2
  };

 private:
  static volatile SynchronizeState _state;     // Threads might read this flag directly, without acquireing the Threads_lock
  static volatile int _waiting_to_block;       // No. of threads we are waiting for to block.
  
  // This counter is used for fast versions of jni_Get<Primitive>Field.
  // An even value means there is no ongoing safepoint operations.
  // The counter is incremented ONLY at the beginning and end of each
  // safepoint. The fact that Threads_lock is held throughout each pair of
  // increments (at the beginning and end of each safepoint) guarantees
  // race freedom.
  static volatile int _safepoint_counter;

  static long     _last_safepoint;             // Time of last safepoint

  // Used in mutex acquisition special case code (look for
  // _suppress_signal) by VM thread only.
  // The variable is written by any safepointing thread (VMThread or
  // CMSThread) under protection of the Threads_lock, but may be read
  // by the VM thread (only) without any locking. Race freedom
  // follows via interlocking wrt setting and clearing of the
  // _state variable, which is volatile, and whose reading protects
  // the reading of this field by the VM thread in the mutex
  // acquisition code.
  static SafepointingThread _safepointing_thread;

  // statistics
#ifndef PRODUCT
  static long       _total_safepoints;
  static long       _total_threads;
  static long       _max_threads;
  static long       _num_of_suspends;
  static long       _cumm_num_of_suspends;
  static double     _total_secs;
  static double     _max_secs;
  static double     _min_secs;
  static long       _suspendstates[_thread_max_state];
#endif

  static void begin_statistics(int nof_threads) PRODUCT_RETURN;
  static void end_statistics(double seconds)    PRODUCT_RETURN;
  static void print_statistics(double seconds)  PRODUCT_RETURN;

public:

  // Main entry points

  // Roll all threads forward to safepoint. Must be called by the
  // VMThread or CMS_thread.
  static void begin();
  static void end();                    // Start all suspended threads again...

  // Machine-depended methods        
  static bool safepoint_safe                      (JavaThread *thread, JavaThreadState state);
  static void patch_return_instruction_md         (address cb_pc);

  // Query
  inline static bool is_at_safepoint()                    { return _state == _synchronized;  }
  inline static bool is_synchronizing()                   { return _state == _synchronizing;  }

  // In support of sneaky locking by vm thread
#ifdef ASSERT
  static bool at_vmthread_safepoint();
#else
  inline static bool at_vmthread_safepoint()  {
    return (_state == _synchronized) && (_safepointing_thread == _vm_thread);
  }
#endif // ASSERT

  inline static bool do_call_back() {
    // workaround the redundant code generated by CC 5.2
    // for _state != _not_synchronized. Should change back when
    // RFE 4865691 is done.
    return _state;
  }
  
  // Called when a thread volantary blocks
  static void   block(JavaThread *thread);
  static void   signal_thread_at_safepoint()              { _waiting_to_block--; }  

  // Exception handling for illegal bytecode patching
  static address  handle_illegal_instruction_exception(JavaThread *thread);

  // Exception handling for page polling
  static address  handle_polling_page_exception(JavaThread *thread);

  // VM Thread interface for determining safepoint rate
  static long last_non_safepoint_interval()               { return os::javaTimeMillis() - _last_safepoint; }
  static bool is_cleanup_needed();
  static void do_cleanup_tasks();

  // debugging
  static void print_state()                                PRODUCT_RETURN;
  static void safepoint_msg(const char* format, ...)       PRODUCT_RETURN;
#ifndef PRODUCT
  static void log_suspension(JavaThreadState state)        { _num_of_suspends += 1; _suspendstates[state]+=1;}   
#endif
  static void set_is_at_safepoint()                        { _state = _synchronized; }
  static void set_is_not_at_safepoint()                    { _state = _not_synchronized; }

  // assembly support
  static address address_of_state()                        { return (address)&_state; }  

  static address safepoint_counter_addr()                  { return (address)&_safepoint_counter; }
};


// State class for a thread suspended at a safepoint
class ThreadSafepointState: public CHeapObj {
 public:  
  enum suspend_type {    
    _running                =  0, // Thread state not yet determined (i.e., not at a safepoint yet)
    _at_safepoint           =  1, // Thread at a safepoint (f.ex., when blocked on a lock)
    _call_back              =  2, // Keep executing and wait for callback (if thread is in interpreter or vm)
    _compiled_safepoint     =  3  // Compiled safepoint
  };
 private:
  volatile bool _at_poll_safepoint;  // At polling page safepoint (NOT a poll return safepoint)
  volatile suspend_type          _type;  
  JavaThread *          _thread;    
  // use an ExtendedPC to be sure we have real containment -- on SPARC the pc
  // might be at the delay slot of a branch so we don't contain the PC, but we do
  // contain the nPC
  ExtendedPC            _addr;
  SafepointHandler*     _handle;  // Used when _type == _safepoint_handler

  // Thead local code buffer
  NOT_CORE(ThreadCodeBuffer* _code_buffer;)
  NOT_CORE(bool _code_buffer_is_enabled;)

 public:       
  ThreadSafepointState(JavaThread *thread);
  ~ThreadSafepointState();

  // examine/roll-forward/restart   
  void examine_state_of_thread(int iterations);               
  void roll_forward(suspend_type type, nmethod* nm = NULL, bool disable_resume_for_running_thread = false);
  void restart();  
  
  // Query  
  JavaThread*         thread() const	      { return _thread; }
  suspend_type        type() const	      { return _type; }    
  bool                is_running() const      { return (_type==_running); } 
  ExtendedPC          current_address() const { return _addr; } 
  SafepointHandler*   handle() const          { return _handle; }
      
  bool caller_must_gc_arguments() const;  
#ifndef CORE
  // Local code-cache management
  ThreadCodeBuffer* allocate_code_buffer(int size_in_bytes, nmethod *code, address real_pc);
  void              enable_code_buffer() { _code_buffer_is_enabled = true; }
  void              disable_code_buffer(){ _code_buffer_is_enabled = false; }
  void              destroy_code_buffer();
  ThreadCodeBuffer* code_buffer()        { return _code_buffer_is_enabled ? _code_buffer : NULL; }
  void              notify_set_thread_pc_result(bool success);


  // adjustment of pc if it points into a thread local codebuffer (might block)
  inline address compute_adjusted_pc(address pc);

  // the inverse adjustment of pc, if it can be redirected into the codebuffer:
  inline address maybe_capture_pc(address pc);
#endif
  bool              is_at_poll_safepoint() { return _at_poll_safepoint; }
  void              set_at_poll_safepoint(bool val) { _at_poll_safepoint = val; }

  // debugging  
  void print(); 
  JavaThreadState _stop_state; // Usefull for debugging a deadlock (to be taken out)
  address         _stop_pc;    // Usefull for debugging a deadlock (to be taken out)

  // Initialize
  static void create(JavaThread *thread);
  static void destroy(JavaThread *thread);
};

#ifndef CORE
// Inlined code

inline address ThreadSafepointState::compute_adjusted_pc(address pc) {
  if( !SafepointPolling ) {
    ThreadCodeBuffer *cb = code_buffer();
    if (cb != NULL && cb->contains(pc)) pc = cb->compute_adjusted_pc(pc);    
  }
  return pc;
}

inline address ThreadSafepointState::maybe_capture_pc(address pc) {
  ThreadCodeBuffer *cb = code_buffer();
  if (cb != NULL && cb->captures(pc)) pc = cb->capture_pc(pc);
  return pc;
}

#endif
// ---------------------------------------------------------------------------------------------------------------
// Specialized safepoint handlers for different parts of the code

class SafepointHandler : public CHeapObj {
  JavaThread *            _thread;  
 public:
  SafepointHandler(JavaThread *thread)   { _thread = thread; }  

  // Main virtuals. Initiates safepoint operation for thread, and release thread after safepoint operation is
  // completed.
  virtual void setup    (ThreadSafepointState *state, nmethod* nm) = 0;
  virtual void release  (ThreadSafepointState *state) = 0;
  virtual const char *name()   = 0;  
  virtual bool is_compiled_code_safepoint_handler()     { return false; }
  

  // Call-backs from safepoint code
  virtual address  handle_illegal_instruction_exception()   { ShouldNotReachHere(); return NULL;}

  virtual address  handle_polling_page_exception()          { ShouldNotReachHere(); return NULL;}

  virtual bool caller_must_gc_arguments() const         { ShouldNotReachHere(); return false; };  

  // query
  JavaThread* thread()  { return _thread; }

  // general helper methods
  void set_state_to_running();

  // debugging
  virtual void print()            { tty->print_cr("SafepointHandler: %s", name()); }
  void safepoint_msg(const char* format, ...) {
    if (ShowSafepointMsgs) {
      va_list ap;
      va_start(ap, format);
      tty->vprint_cr(format, ap);
      va_end(ap);
    }
  }
};

// -------------------------------------------------------------------------------------------------------------
// Specialized Safepoint handler for compiled code.
#ifndef CORE

class CompiledCodeSafepointHandler: public SafepointHandler {
  nmethod *_nm;
  bool    _caller_must_gc_args;    
private:
  // Platform-dependent computation of how big the ThreadCodeBuffer
  // needs to be to handle the given nmethod. Returns
  // instructions_size() for all 32-bit platforms; only returns a
  // different result on 64-bit platforms where trampolining of
  // runtime calls is necessary.
  static int  pd_thread_code_buffer_size(nmethod* nm);
  // Patch a ThreadCodeBuffer's runtime calls with trampolines if
  // necessary. Currently this is only done (and is only necessary) on
  // Solaris/SPARC 64-bit.
  static void pd_patch_runtime_calls_with_trampolines(ThreadCodeBuffer* cb, int offset_of_first_trampoline);
  
public:
  CompiledCodeSafepointHandler(JavaThread *thread) : SafepointHandler(thread) { _nm = NULL; };  

  void setup  (ThreadSafepointState *state, nmethod* nm);
  void release(ThreadSafepointState *state);  
  void check_has_escaped(CodeBlob *stub_cb);
  address  handle_illegal_instruction_exception();
  address  handle_polling_page_exception();
  
  virtual void print();

  bool is_compiled_code_safepoint_handler() { return true; }
  
  const char *name()          { return "CompiledCodeSafepointHandler"; }
  nmethod *get_nmethod()      { return _nm; }  

  // Called by SafepointBlob
  bool caller_must_gc_arguments() const { return _caller_must_gc_args; };
};

#endif
