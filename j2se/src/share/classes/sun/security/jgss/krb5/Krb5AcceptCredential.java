/*
 * @(#)Krb5AcceptCredential.java	1.17 04/04/01
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
  
package sun.security.jgss.krb5;

import org.ietf.jgss.*;
import sun.security.jgss.spi.*;
import sun.security.krb5.*;
import javax.security.auth.kerberos.*;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.AccessControlContext;
import sun.security.jgss.LoginUtility;
import javax.security.auth.DestroyFailedException;

/**
 * Implements the krb5 acceptor credential element.
 *
 * @author Mayank Upadhyay
 * @version 1.17, 04/01/04
 * @since 1.4
 */
public class Krb5AcceptCredential
    extends KerberosKey
    implements Krb5CredElement {

    private static final long serialVersionUID = 7714332137352567952L;
    
    private Krb5NameElement name;

    /**
     * We cache an EncryptionKey representation of this key because many
     * Krb5 operation require a key in that form. At some point we might do 
     * away with EncryptionKey altogether and use the base class
     * KerberosKey everywhere.
     */
    private EncryptionKey[] krb5EncryptionKeys;
    
    private Krb5AcceptCredential(Krb5NameElement name, KerberosKey[] keys) {
	/*
	 * Initialize this instance with the data from the acquired
	 * KerberosKey. This class needs to be a KerberosKey too
	 * hence we can't just store a reference.
	 */
	super(keys[0].getPrincipal(),
	      keys[0].getEncoded(),
	      keys[0].getKeyType(),
	      keys[0].getVersionNumber());

	this.name = name;
	// Cache this for later use by the sun.security.krb5 package.
	krb5EncryptionKeys = new EncryptionKey[keys.length];
	for (int i = 0; i < keys.length; i++) {
	    krb5EncryptionKeys[i] = new EncryptionKey(keys[i].getEncoded(),
				    keys[i].getKeyType(),
				    new Integer(keys[i].getVersionNumber()));
	}
    }
    
    static Krb5AcceptCredential getInstance(Krb5NameElement name)
	throws GSSException {
	
	KerberosKey[] keys = getKeysFromSubject(name);
	if (keys == null || keys.length == 0)
	    throw new GSSException(GSSException.NO_CRED, -1, 
				   "Failed to find any Kerberos Key");
	
	if (name == null) {
	    String fullName = keys[0].getPrincipal().getName();
	    name = Krb5NameElement.getInstance(fullName, 
				       Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL);
	}
	
	return new Krb5AcceptCredential(name, keys);
    }
    
    /**
     * Returns the principal name for this credential. The name
     * is in mechanism specific format.
     *
     * @return GSSNameSpi representing principal name of this credential
     * @exception GSSException may be thrown
     */
    public final GSSNameSpi getName() throws GSSException {
	return name;
    }

    /**
     * Returns the init lifetime remaining.
     *
     * @return the init lifetime remaining in seconds
     * @exception GSSException may be thrown
     */
    public int getInitLifetime() throws GSSException {
	return 0;
    }

    /**
     * Returns the accept lifetime remaining.
     *
     * @return the accept lifetime remaining in seconds
     * @exception GSSException may be thrown
     */
    public int getAcceptLifetime() throws GSSException {
	return GSSCredential.INDEFINITE_LIFETIME;
    }
    
    public boolean isInitiatorCredential() throws GSSException {
	return false;
    }
    
    public boolean isAcceptorCredential() throws GSSException {
        return true;
    }
    
    /**
     * Returns the oid representing the underlying credential
     * mechanism oid.
     *
     * @return the Oid for this credential mechanism
     * @exception GSSException may be thrown
     */
    public final Oid getMechanism() {
	return Krb5MechFactory.GSS_KRB5_MECH_OID;
    }

    public final java.security.Provider getProvider() {
	return Krb5MechFactory.PROVIDER;
    }

    EncryptionKey[] getKrb5EncryptionKeys() {
	return krb5EncryptionKeys;
    }

    /** 
     * Called to invalidate this credential element.
     */ 
    public void dispose() throws GSSException {
	try {
	    destroy();
	} catch (DestroyFailedException e) {
	    GSSException gssException =
		new GSSException(GSSException.FAILURE, -1,
		 "Could not destroy credentials - " + e.getMessage());
	    gssException.initCause(e);
	}
    }

    /**
     * Destroys the locally cached EncryptionKey value and then calls
     * destroy in the base class.
     */
    public void destroy() throws DestroyFailedException {
	if (krb5EncryptionKeys != null) {
	    for (int i = 0; i < krb5EncryptionKeys.length; i++) {
		krb5EncryptionKeys[i].destroy();
	    }
	    krb5EncryptionKeys = null;
	}

	super.destroy();
    }

    private static KerberosKey[] getKeysFromSubject(Krb5NameElement name)
	throws GSSException {

	final String serverPrinc = (name == null? null: 
	    name.getKrb5PrincipalName().getName());
	final AccessControlContext acc = AccessController.getContext();
	try {
	    return (KerberosKey[]) AccessController.doPrivileged(
		new PrivilegedExceptionAction() {
		public Object run() throws Exception {
		    return Krb5Util.getKeysFromSubject(
			LoginUtility.GSS_ACCEPT_ENTRY, serverPrinc, acc);
		}});
	} catch (PrivilegedActionException e) {
	    GSSException ge = 
		new GSSException(GSSException.NO_CRED, -1,
		    "Attempt to obtain new ACCEPT credentials failed!");
	    ge.initCause(e.getException());
	    throw ge;
	}
    }
}
