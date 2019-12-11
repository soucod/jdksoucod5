#ifdef USE_PRAGMA_IDENT_SRC
#pragma ident "@(#)dump_md.cpp	1.3 04/07/29 16:36:00 JVM"
#endif
/*
 * Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

# include "incls/_precompiled.incl"
# include "incls/_dump_md.cpp.incl"



// Generate the self-patching vtable method:
//
// This method will be called (as any other Klass virtual method) with
// the Klass itself as the first argument.  Example:
//
// 	oop obj;
// 	int size = obj->klass()->klass_part()->oop_size(this);
//
// for which the virtual method call is Klass::oop_size();
//
// The dummy method is called with the Klass object as the first
// operand, and an object as the second argument.
//

//=====================================================================

// All of the dummy methods in the vtable are essentially identical,
// differing only by an ordinal constant, and they bear no releationship
// to the original method which the caller intended. Also, there needs
// to be 'vtbl_list_size' instances of the vtable in order to
// differentiate between the 'vtable_list_size' original Klass objects.

#define __ masm->

void CompactingPermGenGen::generate_vtable_methods(void** vtbl_list,
                                                   void** vtable,
                                                   char** md_top,
                                                   char* md_end,
                                                   char** mc_top,
                                                   char* mc_end) {

  intptr_t vtable_bytes = (num_virtuals * vtbl_list_size) * sizeof(void*);
  *(intptr_t *)(*md_top) = vtable_bytes;
  *md_top += sizeof(intptr_t);
  void** dummy_vtable = (void**)*md_top;
  *vtable = dummy_vtable;
  *md_top += vtable_bytes;

  // Get ready to generate dummy methods.

  CodeBuffer* cb = new CodeBuffer((unsigned char*)*mc_top, mc_end - *mc_top);
  MacroAssembler* masm = new MacroAssembler(cb);

  Label common_code;
  for (int i = 0; i < vtbl_list_size; ++i) {
    for (int j = 0; j < num_virtuals; ++j) {
      dummy_vtable[num_virtuals * i + j] = (void*)masm->pc();

      // Load eax with a value indicating vtable/offset pair.
      // -- bits[ 7..0]  (8 bits) which virtual method in table?
      // -- bits[12..8]  (5 bits) which virtual method table?
      // -- must fit in 13-bit instruction immediate field.
      __ movl(eax, (i << 8) + j);
      __ jmp(common_code);
    }
  }

  __ bind(common_code);

#ifdef WIN32
  // Expecting to be called with "thiscall" conventions -- the arguments
  // are on the stack, except that the "this" pointer is in ecx.
#else
  // Expecting to be called with Unix conventions -- the arguments
  // are on the stack, including the "this" pointer. 
#endif

  // In addition, eax was set (above) to the offset of the method in the
  // table.

#ifdef WIN32
  __ pushl(ecx);			// save "this"
#endif
  __ movl(ecx, eax);
  __ shrl(ecx, 8);			// isolate vtable identifier.
  __ shll(ecx, LogBytesPerWord);
  __ movl(edx, Address(ecx, (int)vtbl_list)); // get correct vtable address.
#ifdef WIN32
  __ popl(ecx);				// restore "this"
#else
  __ movl(ecx, Address(esp, 4));	// fetch "this"
#endif
  __ movl(Address(ecx, 0), edx);	// update vtable pointer.

  __ andl(eax, 0x00ff);			// isolate vtable method index
  __ shll(eax, LogBytesPerWord);
  __ addl(eax, edx);			// address of real method pointer.
  __ jmp(Address(eax, 0));		// get real method pointer.

  __ flush();

  *mc_top = (char*)__ pc();
}

