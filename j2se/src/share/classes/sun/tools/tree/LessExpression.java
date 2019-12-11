/*
 * @(#)LessExpression.java	1.24 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.tools.tree;

import sun.tools.java.*;
import sun.tools.asm.Assembler;
import sun.tools.asm.Label;

/**
 * WARNING: The contents of this source file are not part of any
 * supported API.  Code that depends on them does so at its own risk:
 * they are subject to change or removal without notice.
 */
public
class LessExpression extends BinaryCompareExpression {
    /**
     * constructor
     */
    public LessExpression(long where, Expression left, Expression right) {
	super(LT, where, left, right);
    }

    /**
     * Evaluate
     */
    Expression eval(int a, int b) {
	return new BooleanExpression(where, a < b);
    }
    Expression eval(long a, long b) {
	return new BooleanExpression(where, a < b);
    }
    Expression eval(float a, float b) {
	return new BooleanExpression(where, a < b);
    }
    Expression eval(double a, double b) {
	return new BooleanExpression(where, a < b);
    }

    /**
     * Simplify
     */
    Expression simplify() {
	if (left.isConstant() && !right.isConstant()) {
	    return new GreaterExpression(where, right, left);
	}
	return this;
    }

    /**
     * Code
     */
    void codeBranch(Environment env, Context ctx, Assembler asm, Label lbl, boolean whenTrue) {
	left.codeValue(env, ctx, asm);
	switch (left.type.getTypeCode()) {
	  case TC_INT:
	    if (!right.equals(0)) {
		right.codeValue(env, ctx, asm);
		asm.add(where, whenTrue ? opc_if_icmplt : opc_if_icmpge, lbl, whenTrue);
		return;
	    }
	    break;
	  case TC_LONG:
	    right.codeValue(env, ctx, asm);
	    asm.add(where, opc_lcmp);
	    break;
	  case TC_FLOAT:
	    right.codeValue(env, ctx, asm);
	    asm.add(where, opc_fcmpg);
	    break;
	  case TC_DOUBLE:
	    right.codeValue(env, ctx, asm);
	    asm.add(where, opc_dcmpg);
	    break;
	  default:
	    throw new CompilerError("Unexpected Type");
	}
	asm.add(where, whenTrue ? opc_iflt : opc_ifge, lbl, whenTrue);
    }
}
