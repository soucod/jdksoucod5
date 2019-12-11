/*
 * @(#)XOperations.java	1.11 04/06/28
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sun.tools.jconsole.inspector;

// java import
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.*;
import java.awt.Insets;
import java.awt.Dimension;
import java.util.*;
import java.io.*;

// jaw import
import javax.management.*;

import sun.tools.jconsole.Resources;
import sun.tools.jconsole.MBeansTab;

public abstract class XOperations extends JPanel implements ActionListener {

    public final static String OPERATION_INVOCATION_EVENT = 
	"jam.xoperations.invoke.result"; 
    private java.util.List<NotificationListener> notificationListenersList;
    
    private Hashtable<JButton, OperationEntry> operationEntryTable;
    
    private XMBean mbean;
    private MBeanInfo mbeanInfo;
    private MBeansTab mbeansTab;
    public XOperations(MBeansTab mbeansTab) {
	super(new GridLayout(1,1));
	this.mbeansTab = mbeansTab;
	operationEntryTable = new Hashtable<JButton, OperationEntry>();
	ArrayList<NotificationListener> l = 
	    new ArrayList<NotificationListener>(1);
	notificationListenersList = 
	    Collections.synchronizedList(l);
    }

    public void removeOperations() {
	removeAll();
    }
    
    public void loadOperations(XMBean mbean,MBeanInfo mbeanInfo) {
	this.mbean = mbean;
	this.mbeanInfo = mbeanInfo;
	// add operations information
	MBeanOperationInfo operations[] = mbeanInfo.getOperations();
	invalidate();
	
	// remove listeners, if any
	Component listeners[] = getComponents();
	for (int i = 0; i < listeners.length; i++)
	    if (listeners[i] instanceof JButton)
		((JButton)listeners[i]).removeActionListener(this);
	
	removeAll();
	setLayout(new BorderLayout());
	
	JButton methodButton;
	JLabel methodLabel;
	JPanel innerPanelLeft,innerPanelRight;
	JPanel outerPanelLeft,outerPanelRight;
	outerPanelLeft  = new JPanel(new GridLayout(operations.length,1));
	outerPanelRight = new JPanel(new GridLayout(operations.length,1));
	
	for (int i=0;i<operations.length;i++) {
	    innerPanelLeft  = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    innerPanelRight = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    innerPanelLeft.add(methodLabel = 
			       new JLabel(Utils.
					  getReadableClassName(operations[i].
							     getReturnType()),
					  JLabel.RIGHT));
	    if (methodLabel.getText().length()>20) {
		methodLabel.setText(methodLabel.getText().
				    substring(methodLabel.getText().
					      lastIndexOf(".")+1,
					     methodLabel.getText().length()));
	    }
	    
	    methodButton = new JButton(operations[i].getName());
	    methodButton.setToolTipText(operations[i].getDescription());
	    boolean callable = isCallable(operations[i].getSignature());
	    if(callable)
		methodButton.addActionListener(this);
	    else
		methodButton.setEnabled(false);
	    
	    MBeanParameterInfo[] signature = operations[i].getSignature();
	    OperationEntry paramEntry = new OperationEntry(operations[i],
							   callable,
							   methodButton,
							   this);
	    operationEntryTable.put(methodButton, paramEntry);
	    innerPanelRight.add(methodButton);
		if(signature.length==0)
		    innerPanelRight.add(new JLabel("( )",JLabel.CENTER));
		else
		    innerPanelRight.add(paramEntry);

	    outerPanelLeft.add(innerPanelLeft,BorderLayout.WEST);
	    outerPanelRight.add(innerPanelRight,BorderLayout.CENTER);
	}
	add(outerPanelLeft,BorderLayout.WEST);
	add(outerPanelRight,BorderLayout.CENTER);
	validate();		
    }
    
    private boolean isCallable(MBeanParameterInfo[] signature) {
	for(int i = 0; i < signature.length; i++) {
	    if(!Utils.isEditableType(signature[i].getType()))
		return false;
	}
	return true;
    }

    public void actionPerformed(final ActionEvent e) {
	performInvokeRequest((JButton)e.getSource());
    }
    
    void performInvokeRequest(final JButton button) {
	mbeansTab.workerAdd(new Runnable() {
		public void run() {
		    try {
			OperationEntry entryIf = 
			    operationEntryTable.get(button);
			Object result = null;
			result = mbean.invoke(button.getText(),
					      entryIf.getParameters(),
					      entryIf.getSignature());
			
			//sends result notification to upper level if 
			// there is a return value
			if(entryIf.getReturnType() != null && 
			   !entryIf.getReturnType().equals("void")) 
			   fireChangedNotification(OPERATION_INVOCATION_EVENT,
						   button,
						   result);
			else
			    EventQueue.invokeLater(new 
				ThreadDialog(XOperations.this, 
					     Resources.
					     getText("Method successfully" +
						     " invoked"),
					     Resources.getText("Info"), 
					    JOptionPane.INFORMATION_MESSAGE));
			
		    }
		    catch (Throwable ex) {
			ex = Utils.getActualException(ex);
			String message = ex.toString();
			EventQueue.invokeLater(new 
			    ThreadDialog(XOperations.this, 
					 Resources.
					 getText("Problem invoking") + 
					 " " + button.getText() +
					 " : "+ message,
					 Resources.getText("Error"), 
					 JOptionPane.ERROR_MESSAGE));
		    }
		}
	    });
    }


    public void addOperationsListener(NotificationListener nl) {
	    notificationListenersList.add(nl);
	}
	
    public void removeOperationsListener(NotificationListener nl) {
	notificationListenersList.remove(nl);
    }
	
    private void fireChangedNotification(String type,
					 Object source,
					 Object handback) {
	Notification e = new Notification(type,source,0);
	for(NotificationListener nl : notificationListenersList)
	    nl.handleNotification(e,handback);
    }

    protected abstract MBeanOperationInfo[] 
	updateOperations(MBeanOperationInfo[] operations);
}

    
    
