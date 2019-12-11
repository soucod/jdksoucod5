/*
 * @(#)JNLPSigningException.java	1.10 03/12/19
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.javaws.exceptions;
import com.sun.javaws.jnl.LaunchDesc;
import com.sun.deploy.resources.ResourceManager;

/**
 * Exception thrown if the LaunchDescriptor does not match the
 * signed launch descriptor included in the JAR file designated
 * as main.
 */

public class JNLPSigningException extends LaunchDescException {
    String _signedSource  = null;
    
    /** The message should be the field that did not match */
    public JNLPSigningException(LaunchDesc ld, String signedSource) {
	super(ld, null);
	_signedSource = signedSource;
    }
    
    /** FIXIT: Needs to be localized */
    public String getRealMessage() { return ResourceManager.getString("launch.error.badsignedjnlp"); }
    
    public String getSignedSource() { return _signedSource; }
    
    public String toString() {
	return "JNLPSigningException["+ getMessage() + "]"; };
}


