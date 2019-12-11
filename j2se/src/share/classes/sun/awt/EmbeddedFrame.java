/*
 * @(#)EmbeddedFrame.java	1.34 04/06/08
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.peer.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.Set;

/**
 * A generic container used for embedding Java components, usually applets.
 * An EmbeddedFrame has two related uses:
 *
 * . Within a Java-based application, an EmbeddedFrame serves as a sort of
 *   firewall, preventing the contained components or applets from using
 *   getParent() to find parent components, such as menubars.
 *
 * . Within a C-based application, an EmbeddedFrame contains a window handle
 *   which was created by the application, which serves as the top-level
 *   Java window.  EmbeddedFrames created for this purpose are passed-in a
 *   handle of an existing window created by the application.  The window
 *   handle should be of the appropriate native type for a specific
 *   platform, as stored in the pData field of the ComponentPeer.
 *
 * @version 	1.15, 04/07/00
 * @author 	Thomas Ball
 */
public abstract class EmbeddedFrame extends Frame 
                          implements KeyEventDispatcher, PropertyChangeListener {

    private boolean isCursorAllowed = true;
    private static Field fieldPeer;
    private static Field currentCycleRoot;
    private boolean supportsXEmbed = false;
    private KeyboardFocusManager appletKFM;
    // JDK 1.1 compatibility
    private static final long serialVersionUID = 2967042741780317130L;

    // Use these in traverseOut method to determine directions
    protected static final boolean FORWARD = true;
    protected static final boolean BACKWARD = false;

    public boolean supportsXEmbed() {
        return supportsXEmbed && SunToolkit.needsXEmbed();
    }
    
    protected EmbeddedFrame(boolean supportsXEmbed) {
        this((long)0, supportsXEmbed);
    }
    
    
    protected EmbeddedFrame() {
        this((long)0);
    }

    /**
     * @deprecated This constructor will be removed in 1.5
     */
    @Deprecated
    protected EmbeddedFrame(int handle) {
        this((long)handle);
    }

    protected EmbeddedFrame(long handle) {
        this(handle, false);
    }

    protected EmbeddedFrame(long handle, boolean supportsXEmbed) {
        this.supportsXEmbed = supportsXEmbed;
    }

    /**
     * Block introspection of a parent window by this child.
     */
    public Container getParent() {
        return null;
    }

    /**
     * Needed to track which KeyboardFocusManager is current. We want to avoid memory
     * leaks, so when KFM stops being current, we remove ourselves as listeners.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // We don't handle any other properties. Skip it.
        if (!evt.getPropertyName().equals("managingFocus")) {
            return;
        }

        // We only do it if it stops being current. Technically, we should
        // never get an event about KFM starting being current.
        if (evt.getNewValue() == Boolean.TRUE) {
            return;
        }

	// should be the same as appletKFM
        removeTraversingOutListeners((KeyboardFocusManager)evt.getSource()); 

	appletKFM = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (isVisible()) {
            addTraversingOutListeners(appletKFM);
        }
    }

    /**
     * Register us as KeyEventDispatcher and property "managingFocus" listeners.
     */
    private void addTraversingOutListeners(KeyboardFocusManager kfm) {
        kfm.addKeyEventDispatcher(this);
        kfm.addPropertyChangeListener("managingFocus", this);
    }

    /**
     * Deregister us as KeyEventDispatcher and property "managingFocus" listeners.
     */
    private void removeTraversingOutListeners(KeyboardFocusManager kfm) {
        kfm.removeKeyEventDispatcher(this);
        kfm.removePropertyChangeListener("managingFocus", this);
    }

    /**
     * Because there may be many AppContexts, and we can't be sure where this
     * EmbeddedFrame is first created or shown, we can't automatically determine
     * the correct KeyboardFocusManager to attach to as KeyEventDispatcher.
     * Those who want to use the functionality of traversing out of the EmbeddedFrame
     * must call this method on the Applet's AppContext. After that, all the changes
     * can be handled automatically, including possible replacement of 
     * KeyboardFocusManager.
     */
    public void registerListeners() {
        if (appletKFM != null) {
            removeTraversingOutListeners(appletKFM);
        }
        appletKFM = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (isVisible()) {
            addTraversingOutListeners(appletKFM);
        }
    }

    /**
     * Needed to avoid memory leak: we register this EmbeddedFrame as a listener with
     * KeyboardFocusManager of applet's AppContext. We don't want the KFM to keep
     * reference to our EmbeddedFrame forever if the Frame is no longer in use, so we
     * add listeners in show() and remove them in hide().
     */
    public void show() {
        if (appletKFM != null) {
            addTraversingOutListeners(appletKFM);
        }
        super.show();
    }

    /**
     * Needed to avoid memory leak: we register this EmbeddedFrame as a listener with
     * KeyboardFocusManager of applet's AppContext. We don't want the KFM to keep
     * reference to our EmbeddedFrame forever if the Frame is no longer in use, so we
     * add listeners in show() and remove them in hide().
     */
    public void hide() {
        if (appletKFM != null) {
            removeTraversingOutListeners(appletKFM);
        }
        super.hide();
    }

    /**
     * Need this method to detect when the focus may have chance to leave the
     * focus cycle root which is EmbeddedFrame. Mostly, the code here is copied
     * from DefaultKeyboardFocusManager.processKeyEvent with some minor
     * modifications.
     */
    public boolean dispatchKeyEvent(KeyEvent e) {

        // We can't guarantee that this is called on the same AppContext as EmbeddedFrame
        // belongs to. That's why we can't use public methods to find current focus cycle
        // root. Instead, we access KFM's private field directly.
        if (currentCycleRoot == null) {
            currentCycleRoot = (Field)AccessController.doPrivileged(new PrivilegedAction() {
	        public Object run() {
                    try {
                        Field unaccessibleRoot = KeyboardFocusManager.class.
                                                     getDeclaredField("currentFocusCycleRoot");
                        if (unaccessibleRoot != null) {
                            unaccessibleRoot.setAccessible(true);
                        }
                        return unaccessibleRoot;
                    } catch (NoSuchFieldException e1) {
                        assert false;
                    } catch (SecurityException e2) {
                        assert false;
                    }
                    return null;
                }
            });
        }

        Container currentRoot = null;
        if (currentCycleRoot != null) {
            try {
                // The field is static, so we can pass null to Field.get() as the argument.
                currentRoot = (Container)currentCycleRoot.get(null);
            } catch (IllegalAccessException e3) {
                // This is impossible: currentCycleRoot would be null if setAccessible failed.
                assert false;
            }
        }

        // if we are not in EmbeddedFrame's cycle, we should not try to leave.
        if (this != currentRoot) {
            return false;
        }

        // KEY_TYPED events cannot be focus traversal keys
        if (e.getID() == KeyEvent.KEY_TYPED) {
            return false;
        }

        if (!getFocusTraversalKeysEnabled() || e.isConsumed()) {
            return false;
        }

        AWTKeyStroke stroke = AWTKeyStroke.getAWTKeyStrokeForEvent(e);
        Set toTest;
        Component currentFocused = e.getComponent();

        Component last = getFocusTraversalPolicy().getLastComponent(this);
        toTest = getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        if (toTest.contains(stroke) && (currentFocused == last || last == null)) { 
            if (traverseOut(FORWARD)) {
                e.consume();
                return true;
            } 
        }

        Component first = getFocusTraversalPolicy().getFirstComponent(this);
        toTest = getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
        if (toTest.contains(stroke) && (currentFocused == first || first == null)) {
            if (traverseOut(BACKWARD)) {
                e.consume();
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called from dispatchKeyEvent in the following two cases:
     * 1. The focus is on the first Component of this EmbeddedFrame and we are
     *    about to transfer the focus backward.
     * 2. The focus in on the last Component of this EmbeddedFrame and we are
     *    about to transfer the focus forward.
     * This is needed to give the opportuity for keyboard focus to leave the 
     * EmbeddedFrame. Override this method, initiate focus transfer in it and
     * return true if you want the focus to leave EmbeddedFrame's cycle.
     * The direction parameter specifies which of the two mentioned cases is 
     * happening. Use FORWARD and BACKWARD constants defined in EmbeddedFrame
     * to avoid confusing boolean values.
     *
     * @param direction FORWARD or BACKWARD
     * @return true, if EmbeddedFrame wants the focus to leave it, 
     *         false otherwise.
     */
    protected boolean traverseOut(boolean direction) {
        return false;
    }

    /**
     * Block modifying any frame attributes, since they aren't applicable
     * for EmbeddedFrames.
     */
    public void setTitle(String title) {}
    public void setIconImage(Image image) {}
    public void setMenuBar(MenuBar mb) {}
    public void setResizable(boolean resizable) {}
    public void remove(MenuComponent m) {}

    public boolean isResizable() {
	return true;
    }

    public void addNotify() {
        synchronized (getTreeLock()) {
	    if (getPeer() == null) {
	        setPeer(new NullEmbeddedFramePeer());
	    }
	    super.addNotify();
	}
    }

    // These three functions consitute RFE 4100710. Do not remove.
    public void setCursorAllowed(boolean isCursorAllowed) {
        this.isCursorAllowed = isCursorAllowed;
	getPeer().updateCursorImmediately();
    }
    public boolean isCursorAllowed() {
        return isCursorAllowed;
    }
    public Cursor getCursor() {
        return (isCursorAllowed)
	    ? super.getCursor()
	    : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    protected  void setPeer(final ComponentPeer p){
        if (fieldPeer == null) {
            fieldPeer = (Field)AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        try {
                            Field lnkPeer = Component.class.getDeclaredField("peer");
                            if (lnkPeer != null) {                 
                                lnkPeer.setAccessible(true);
                            }
                            return lnkPeer;
                        } catch (NoSuchFieldException e) {
                            assert false;
                        } catch (SecurityException e) {
                            assert false;
                        }
                        return null;
                    }//run
                });
        }
        try{
            if (fieldPeer !=null){
                fieldPeer.set(EmbeddedFrame.this, p);
            }
        } catch (IllegalAccessException e) {
            assert false;
        }
    };  //setPeer method ends

    /**
     * Synthesize native message to activate or deactivate EmbeddedFrame window
     * depending on the value of parameter <code>b</code>.
     * Peers should override this method if they are to implement
     * this functionality.
     * @param b  if <code>true</code>, activates the window;
     * otherwise, deactivates the window
     */
    public void synthesizeWindowActivation(boolean b) {}

    protected void setLocationPrivate(int x, int y) {
        Dimension size = getSize();
        setBoundsPrivate(x, y, size.width, size.height);
    }

    protected void setBoundsPrivate(int x, int y, int width, int height) {
        final FramePeer peer = (FramePeer)getPeer();
        if (peer != null) {
            peer.setBoundsPrivate(x, y, width, height);
        }
    }

    private static class NullEmbeddedFramePeer
        extends NullComponentPeer implements FramePeer {
        public void setTitle(String title) {}
        public void setIconImage(Image im) {}
        public void setMenuBar(MenuBar mb) {}
        public void setResizable(boolean resizeable) {}
        public void setState(int state) {}
        public int getState() { return Frame.NORMAL; }
	public void setMaximizedBounds(Rectangle b) {}
        public void toFront() {}
        public void toBack() {}
        public void updateAlwaysOnTop() {}
        public Component getGlobalHeavyweightFocusOwner() { return null; }
        public void setBoundsPrivate(int x, int y, int width, int height) {            
            setBounds(x, y, width, height, SET_BOUNDS);
        }

        /**
         * @see java.awt.peer.ContainerPeer#restack
         */
        public void restack() {
            throw new UnsupportedOperationException();
        }
        
        /**
         * @see java.awt.peer.ContainerPeer#isRestackSupported
         */
        public boolean isRestackSupported() {
            return false;
        }
        public boolean requestWindowFocus() {            
            return false;
        }
    }
} // class EmbeddedFrame
