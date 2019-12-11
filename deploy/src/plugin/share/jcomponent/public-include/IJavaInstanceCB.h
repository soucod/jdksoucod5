/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#if !defined(__IJavaInstanceCB__)
#define __IJavaInstanceCB__

#include "IEgo.h"

interface IJavaInstanceCB : public IEgo {
public:
    virtual void showStatus(char *)=0;
    virtual void showDocument(char *, char *)=0;
    virtual void findProxy(char * , char **)=0;
    virtual void findCookie(char *, char **)=0;
    virtual void javascriptRequest(char *)=0;
    virtual void setCookie(char *, char *)=0;
};

#endif
