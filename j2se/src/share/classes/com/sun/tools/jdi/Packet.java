/*
 * @(#)Packet.java	1.34 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.tools.jdi;

import com.sun.jdi.*;
import java.io.IOException;

public class Packet extends Object {
    public final static short NoFlags = 0x0;
    public final static short Reply = 0x80;
    public final static short ReplyNoError = 0x0;

    static int uID = 1;
    final static byte[] nullData = new byte[0];

    // Note! flags, cmdSet, and cmd are all byte values.
    // We represent them as shorts to make them easier
    // to work with. 
    int id;
    short flags;
    short cmdSet;
    short cmd;
    short errorCode;
    byte[] data;
    volatile boolean replied = false;

    /**
     * Return byte representation of the packet
     */
    public byte[] toByteArray() {
	int len = data.length + 11;
	byte b[] = new byte[len];
	b[0] = (byte)((len >>> 24) & 0xff);
	b[1] = (byte)((len >>> 16) & 0xff);
 	b[2] = (byte)((len >>>  8) & 0xff);
	b[3] = (byte)((len >>>  0) & 0xff);
	b[4] = (byte)((id >>> 24) & 0xff);
	b[5] = (byte)((id >>> 16) & 0xff);
	b[6] = (byte)((id >>>  8) & 0xff);
	b[7] = (byte)((id >>>  0) & 0xff);
	b[8] = (byte)flags;
	if ((flags & Packet.Reply) == 0) {
	    b[9] = (byte)cmdSet;
	    b[10] = (byte)cmd;
	} else {
	    b[9] = (byte)((errorCode >>>  8) & 0xff);
	    b[10] = (byte)((errorCode >>>  0) & 0xff);
	}
	if (data.length > 0) {
	    System.arraycopy(data, 0, b, 11, data.length);
	}
	return b;
    }

    /**
     * Create a packet from its byte array representation
     */
    public static Packet fromByteArray(byte b[]) throws IOException { 
	if (b.length < 11) {
	    throw new IOException("packet is insufficient size");
	}

	int b0 = b[0] & 0xff;
	int b1 = b[1] & 0xff;
	int b2 = b[2] & 0xff;
	int b3 = b[3] & 0xff;
	int len = ((b0 << 24) | (b1 << 16) | (b2 << 8) | (b3 << 0));
	if (len != b.length) {
	    throw new IOException("length size mis-match");
	}

	int b4 = b[4] & 0xff;
	int b5 = b[5] & 0xff;
	int b6 = b[6] & 0xff;
	int b7 = b[7] & 0xff;

	Packet p = new Packet();
	p.id = ((b4 << 24) | (b5 << 16) | (b6 << 8) | (b7 << 0));

	p.flags = (short)(b[8] & 0xff);

	if ((p.flags & Packet.Reply) == 0) {
	    p.cmdSet = (short)(b[9] & 0xff);
	    p.cmd = (short)(b[10] & 0xff);
	} else {
	    short b9 = (short)(b[9] & 0xff);
	    short b10 = (short)(b[10] & 0xff); 
	    p.errorCode = (short)((b9 << 8) + (b10 << 0));
	}

	p.data = new byte[b.length - 11];
	System.arraycopy(b, 11, p.data, 0, p.data.length);
	return p;
    }

    Packet()
    {
        id = uniqID();
        flags = NoFlags;
        data = nullData;
    }

    static synchronized private int uniqID()
    {
        /*
         * JDWP spec does not require this id to be sequential and 
         * increasing, but our implementation does. See 
         * VirtualMachine.notifySuspend, for example.
         */
        return uID++;
    }
}
