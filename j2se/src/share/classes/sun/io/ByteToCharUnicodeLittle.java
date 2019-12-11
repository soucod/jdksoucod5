/*
 * @(#)ByteToCharUnicodeLittle.java	1.11 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.io;
import java.io.*;


/**
 * Convert byte arrays containing Unicode characters into arrays of actual
 * Unicode characters, assuming a little-endian byte order.
 *
 * @version 	1.11, 03/12/19
 * @author	Mark Reinhold
 */

public class ByteToCharUnicodeLittle extends ByteToCharUnicode {

    public ByteToCharUnicodeLittle() {
	super(LITTLE, true);
    }

}
