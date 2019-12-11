/*
 * @(#)XStateProtocol.java	1.3 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */


package sun.awt.X11;

public interface XStateProtocol {

    /**
     * Returns whether or not the protocol supports the transition to the state
     * represented by <code>state</code>. <code>State</code> contains encoded state
     * as a bit mask of state defined in java.awt.Frame
     */
    boolean supportsState(int state);
    
    /**
     * Moves window into the state.
     */
    void setState(XWindowPeer window, int state);

    /**
     * Returns current state of the window
     */
    int getState(XWindowPeer window);

    /**
     * Detects whether or not this event is indicates state change
     */
    boolean isStateChange(XPropertyEvent e);

    /**
     * Work around for 4775545.
     */
    void unshadeKludge(XWindowPeer window);
}
