/*
 * @(#)ClassPrepareRequest.java	1.20 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.jdi.request;

import com.sun.jdi.*;

/**
 * Request for notification when a class is prepared in the target VM.
 * When an enabled ClassPrepareRequest is satisfied, an
 * {@link com.sun.jdi.event.EventSet event set} containing a
 * {@link com.sun.jdi.event.ClassPrepareEvent ClassPrepareEvent}
 * will be placed on the 
 * {@link com.sun.jdi.event.EventQueue EventQueue}.
 * The collection of existing ClassPrepareRequests is 
 * managed by the {@link EventRequestManager}
 * <p>
 * Class preparation is defined in the Java Virtual Machine
 * Specification.
 *
 * @see com.sun.jdi.event.ClassPrepareEvent
 * @see com.sun.jdi.event.EventQueue
 * @see EventRequestManager
 *
 * @author Robert Field
 * @since  1.3
 */
public interface ClassPrepareRequest extends EventRequest {

    /**
     * Restricts the events generated by this request to be the
     * preparation of the given reference type and any subtypes. 
     * An event will be generated for any prepared reference type that can
     * be safely cast to the given reference type. 
     *
     * @param refType the reference type to filter on.
     * @throws InvalidRequestStateException if this request is currently
     * enabled or has been deleted. 
     * Filters may be added only to disabled requests.
     */
    void addClassFilter(ReferenceType refType);

    /**
     * Restricts the events generated by this request to the 
     * preparation of reference types whose name matches this restricted
     * regular expression. Regular expressions are limited 
     * to exact matches and patterns that begin with '*' or end with '*'; 
     * for example, "*.Foo" or "java.*".
     *
     * @param classPattern the pattern String to filter for.
     * @throws InvalidRequestStateException if this request is currently
     * enabled or has been deleted. 
     * Filters may be added only to disabled requests.
     */
    void addClassFilter(String classPattern);

    /**
     * Restricts the events generated by this request to the 
     * preparation of reference types whose name does <b>not</b> match
     * this restricted regular expression. Regular expressions are limited 
     * to exact matches and patterns that begin with '*' or end with '*'; 
     * for example, "*.Foo" or "java.*".
     *
     * @param classPattern the pattern String to filter against.
     * @throws InvalidRequestStateException if this request is currently
     * enabled or has been deleted. 
     * Filters may be added only to disabled requests.
     */
    void addClassExclusionFilter(String classPattern);
}
