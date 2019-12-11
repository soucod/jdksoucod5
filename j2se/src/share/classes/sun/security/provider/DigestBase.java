/*
 * @(#)DigestBase.java	1.3 04/03/08
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.provider;

import java.security.MessageDigestSpi;
import java.security.DigestException;
import java.security.ProviderException;

/**
 * Common base message digest implementation for the Sun provider.
 * It implements all the JCA methods as suitable for a Java message digest
 * implementation of an algorithm based on a compression function (as all
 * commonly used algorithms are). The individual digest subclasses only need to
 * implement the following methods:
 *
 *  . abstract void implCompress(byte[] b, int ofs);
 *  . abstract void implDigest(byte[] out, int ofs);
 *  . abstract void implReset();
 *  . public abstract Object clone();
 *
 * See the inline documentation for details.
 *
 * @since   1.5
 * @version 1.3, 03/08/04
 * @author  Andreas Sterbenz
 */
abstract class DigestBase extends MessageDigestSpi implements Cloneable {
    
    // one element byte array, temporary storage for update(byte)
    private final byte[] oneByte = new byte[1];
    
    // algorithm name to use in the exception message
    private final String algorithm;
    // length of the message digest in bytes
    private final int digestLength;
    
    // size of the input to the compression function in bytes
    private final int blockSize;
    // buffer to store partial blocks, blockSize bytes large
    private final byte[] buffer;
    // offset into buffer
    private int bufOfs;

    // number of bytes processed so far. subclasses should not modify
    // this value.
    // also used as a flag to indicate reset status
    // -1: need to call engineReset() before next call to update()
    //  0: is already reset
    long bytesProcessed;
    
    /**
     * Main constructor.
     */
    DigestBase(String algorithm, int digestLength, int blockSize) {
	super();
	this.algorithm = algorithm;
	this.digestLength = digestLength;
	this.blockSize = blockSize;
	buffer = new byte[blockSize];
    }
    
    /**
     * Constructor for cloning. Replicates common data.
     */
    DigestBase(DigestBase base) {
	this.algorithm = base.algorithm;
	this.digestLength = base.digestLength;
	this.blockSize = base.blockSize;
	this.buffer = (byte[])base.buffer.clone();
	this.bufOfs = base.bufOfs;
	this.bytesProcessed = base.bytesProcessed;
    }
    
    // return digest length. See JCA doc.
    protected final int engineGetDigestLength() {
	return digestLength;
    }
    
    // single byte update. See JCA doc.
    protected final void engineUpdate(byte b) {
	oneByte[0] = b;
	engineUpdate(oneByte, 0, 1);
    }
    
    // array update. See JCA doc.
    protected final void engineUpdate(byte[] b, int ofs, int len) {
	if (len == 0) {
	    return;
	}
	if (bytesProcessed < 0) {
	    engineReset();
	}
	bytesProcessed += len;
	// if buffer is not empty, we need to fill it before proceeding
	if (bufOfs != 0) {
	    int n = Math.min(len, blockSize - bufOfs);
	    System.arraycopy(b, ofs, buffer, bufOfs, n);
	    bufOfs += n;
	    ofs += n;
	    len -= n;
	    if (bufOfs >= blockSize) {
		// compress completed block now
		implCompress(buffer, 0);
		bufOfs = 0;
	    }
	}
	// compress complete blocks
	while (len >= blockSize) {
	    implCompress(b, ofs);
	    len -= blockSize;
	    ofs += blockSize;
	}
	// copy remainder to buffer
	if (len > 0) {
	    System.arraycopy(b, ofs, buffer, 0, len);
	    bufOfs = len;
	}
    }
    
    // reset this object. See JCA doc.
    protected final void engineReset() {
	if (bytesProcessed == 0) {
	    // already reset, ignore
	    return;
	}
	implReset();
	bufOfs = 0;
	bytesProcessed = 0;
    }
    
    // return the digest. See JCA doc.
    protected final byte[] engineDigest() {
	byte[] b = new byte[digestLength];
	try {
	    engineDigest(b, 0, b.length);
	} catch (DigestException e) {
	    throw (ProviderException)
	    	new ProviderException("Internal error").initCause(e);
	}
	return b;
    }
    
    // return the digest in the specified array. See JCA doc.
    protected final int engineDigest(byte[] out, int ofs, int len) 
	    throws DigestException {
	if (len < digestLength) {
	    throw new DigestException("Length must be at least "
	    	+ digestLength + " for " + algorithm + "digests");
	}
	if (ofs + len > out.length) {
	    throw new DigestException("Buffer too short to store digest");
	}
	if (bytesProcessed < 0) {
	    engineReset();
	}
	implDigest(out, ofs);
	bytesProcessed = -1;
	return digestLength;
    }
    
    /**
     * Core compression function. Processes blockSize bytes at a time
     * and updates the state of this object.
     */
    abstract void implCompress(byte[] b, int ofs);
    
    /**
     * Return the digest. Subclasses do not need to reset() themselves,
     * DigestBase calls implReset() when necessary.
     */
    abstract void implDigest(byte[] out, int ofs);
    
    /**
     * Reset subclass specific state to their initial values. DigestBase
     * calls this method when necessary.
     */
    abstract void implReset();
    
    /**
     * Clone this digest. Should be implemented as "return new MyDigest(this)".
     * That constructor should first call "super(baseDigest)" and then copy
     * subclass specific data.
     */
    public abstract Object clone();
    
    // padding used for the MD5, and SHA-* message digests
    static final byte[] padding;
    
    static {
	// we need 128 byte padding for SHA-384/512
	// and an additional 8 bytes for the high 8 bytes of the 16
	// byte bit counter in SHA-384/512
	padding = new byte[136];
	padding[0] = (byte)0x80;
    }

    /**
     * byte[] to int[] conversion, little endian byte order.
     */
    static void b2iLittle(byte[] in, int inOfs, int[] out, int outOfs, int len) {
	while (len > 0) {
	    int i = ((in[inOfs    ] & 0xff)      )
	    	  | ((in[inOfs + 1] & 0xff) <<  8)
		  | ((in[inOfs + 2] & 0xff) << 16)
		  | ((in[inOfs + 3]       ) << 24);
	    out[outOfs++] = i;
	    inOfs += 4;
	    len -= 4;
	}
    }
    
    /**
     * int[] to byte[] conversion, little endian byte order.
     */
    static void i2bLittle(int[] in, int inOfs, byte[] out, int outOfs, int len) {
	while (len > 0) {
	    int i = in[inOfs++];
	    out[outOfs++] = (byte)(i      );
	    out[outOfs++] = (byte)(i >>  8);
	    out[outOfs++] = (byte)(i >> 16);
	    out[outOfs++] = (byte)(i >> 24);
	    len -= 4;
	}
    }

    /**
     * byte[] to int[] conversion, big endian byte order.
     */
    static void b2iBig(byte[] in, int inOfs, int[] out, int outOfs, int len) {
	while (len > 0) {
	    int i = ((in[inOfs + 3] & 0xff)      )
	    	  | ((in[inOfs + 2] & 0xff) <<  8)
		  | ((in[inOfs + 1] & 0xff) << 16)
		  | ((in[inOfs    ]       ) << 24);
	    out[outOfs++] = i;
	    inOfs += 4;
	    len -= 4;
	}
    }
    
    /**
     * int[] to byte[] conversion, big endian byte order.
     */
    static void i2bBig(int[] in, int inOfs, byte[] out, int outOfs, int len) {
	while (len > 0) {
	    int i = in[inOfs++];
	    out[outOfs++] = (byte)(i >> 24);
	    out[outOfs++] = (byte)(i >> 16);
	    out[outOfs++] = (byte)(i >>  8);
	    out[outOfs++] = (byte)(i      );
	    len -= 4;
	}
    }

    /**
     * byte[] to long[] conversion, big endian byte order.
     */
    static void b2lBig(byte[] in, int inOfs, long[] out, int outOfs, int len) {
	while (len > 0) {
	    int i1 = ((in[inOfs + 3] & 0xff)      )
	    	   | ((in[inOfs + 2] & 0xff) <<  8)
		   | ((in[inOfs + 1] & 0xff) << 16)
		   | ((in[inOfs    ]       ) << 24);
	    inOfs += 4;
	    int i2 = ((in[inOfs + 3] & 0xff)      )
	    	   | ((in[inOfs + 2] & 0xff) <<  8)
		   | ((in[inOfs + 1] & 0xff) << 16)
		   | ((in[inOfs    ]       ) << 24);
	    out[outOfs++] = ((long)i1 << 32) | (i2 & 0xffffffffL);
	    inOfs += 4;
	    len -= 8;
	}
    }
    
    /**
     * long[] to byte[] conversion, big endian byte order.
     */
    static void l2bBig(long[] in, int inOfs, byte[] out, int outOfs, int len) {
	while (len > 0) {
	    long i = in[inOfs++];
	    out[outOfs++] = (byte)(i >> 56);
	    out[outOfs++] = (byte)(i >> 48);
	    out[outOfs++] = (byte)(i >> 40);
	    out[outOfs++] = (byte)(i >> 32);
	    out[outOfs++] = (byte)(i >> 24);
	    out[outOfs++] = (byte)(i >> 16);
	    out[outOfs++] = (byte)(i >>  8);
	    out[outOfs++] = (byte)(i      );
	    len -= 8;
	}
    }
    
}

