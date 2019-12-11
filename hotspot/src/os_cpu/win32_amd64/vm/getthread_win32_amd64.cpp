#ifdef USE_PRAGMA_IDENT_SRC
#pragma ident "@(#)getthread_win32_amd64.cpp	1.2 03/12/23 16:38:26 JVM"
#endif
/*
 * Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

// Provides an entry point we can link against and
// a buffer we can emit code into. The buffer is
// filled by ThreadLocalStorage::generate_code_for_get_thread
// and called from ThreadLocalStorage::thread()

// do not include precompiled header file
#include "incls/_getThread_win32_amd64.cpp.incl"

