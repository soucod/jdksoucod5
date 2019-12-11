/*
 * @(#)ORBConfigurator.java	1.10 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.corba.se.spi.orb ;

/** Interface used to configure an ORB instance.  The DataCollector dc has all
 * available config info available.  The configure method may constructor a 
 * parser, call dc.setParser( parser ), get the consolidated properties from dc,
 * and parse this information.  The configure method may also register ORB
 * components with the ORB and perform other parts of ORB initialization.
 * Implementations of this interface must have a public no-args constructor.
 */
public interface ORBConfigurator {
    void configure( DataCollector dc, ORB orb ) ;
}

