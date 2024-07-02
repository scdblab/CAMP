package com.mitrallc.sqltrig.rewriter;

import net.sf.jsqlparser.expression.Expression;

public class ParseNode {
	private ParseNode parent;
	private Expression nodevalue;
	private ParseNode leftchild;
	private ParseNode rightchild;
	private String nodeType; 
	private boolean leftVisited;
	
	public ParseNode() {
		nodevalue = null;
		leftchild = null;
		rightchild = null;
		parent = null;
		nodeType = null;
		setLeftVisited(false);
	}
	
	public ParseNode(Expression exp) {
		nodevalue = exp;
		leftchild = null;
		rightchild = null;
		parent = null;
		nodeType = null;
		setLeftVisited(false);
	}
	
	public String toString() {
		return nodevalue.toString();
	}

	public ParseNode getParent() {
		return parent;
	}
	public void setParent(ParseNode parent) {
		this.parent = parent;
	}
	public ParseNode getLeftchild() {
		return leftchild;
	}
	public void setLeftchild(ParseNode leftchild) {
		this.leftchild = leftchild;
	}
	public ParseNode getRightchild() {
		return rightchild;
	}
	public void setRightchild(ParseNode rightchild) {
		this.rightchild = rightchild;
	}
	
	public Expression getNodevalue() {
		return nodevalue;
	}
	public void setNodevalue(Expression nodevalue) {
		this.nodevalue = nodevalue;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	
	public boolean isLeafNode() {
		return this.leftchild == null && this.rightchild == null;
	}

	public boolean isLeftVisited() {
		return leftVisited;
	}

	public void setLeftVisited(boolean leftVisited) {
		this.leftVisited = leftVisited;
	}
	
	
}
