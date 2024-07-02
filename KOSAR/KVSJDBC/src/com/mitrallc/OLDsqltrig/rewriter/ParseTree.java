package com.mitrallc.sqltrig.rewriter;

import java.util.Vector;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;

public class ParseTree {
	private ParseNode root;
	private Vector<ParseNode> root_forest;
	
	public ParseTree() {
		root = null;
		root_forest = new Vector<ParseNode>();
	}
	
	private boolean EqualsIgnoreParentheses(Expression exp1, Expression exp2) {
		if (exp1 == null || exp2 == null) {
			return false;
		}
		
		Expression exp1nop = exp1;
		Expression exp2nop = exp2;
		
		if (exp1 instanceof Parenthesis) {
			exp1nop = ((Parenthesis)exp1).getExpression();
		}
		
		if (exp2 instanceof Parenthesis) {
			exp2nop = ((Parenthesis)exp2).getExpression();
		}
		
		return exp1nop.equals(exp2nop);
		
		
		
//		
//		String str1 = exp1.toString().trim();
//		String str2 = exp2.toString().trim();
//		
//		// Strip parentheses
//		if (str1.indexOf("(") == 0 && str1.lastIndexOf(")") == (str1.length() - 1)) {
//			str1 = str1.substring(1, str1.length() - 1);
//		}
//		
//		// Strip parentheses
//		if (str2.indexOf("(") == 0 && str2.lastIndexOf(")") == (str2.length() - 1)) {
//			str2 = str2.substring(1, str2.length() - 1);
//		}
//		
//		return str1.equals(str2);
	}
	
	private ParseNode findRoot(Expression exp) {
		for (ParseNode node : root_forest) {
			if (node.getNodevalue().equals(exp) || EqualsIgnoreParentheses(node.getNodevalue(), exp)) {
				return node;
			}
		}
		
		return null;
	}
	
	private void addRootToForest(ParseNode root) {
		root_forest.add(root);
	}
	
	private void removeRootFromForest(ParseNode root) {
		root_forest.remove(root);
	}
	
	private int max(int val1, int val2) {
		if(val1 > val2) {
			return val1;
		}
		
		return val2;
	}
	
	private int maxDepth(ParseNode root) {
		return max(maxDepth(root.getLeftchild()) + 1, maxDepth(root.getRightchild()) + 1);
	}
	
	private String printTree(String prefix, ParseNode root) {
		String output = prefix + "\\-- ";
		if (root.isLeafNode()) {
			output += root;
		} else {
			output += root.getNodeType();
		}
		output += "\r\n";
		
		if (root.getLeftchild() != null) {
			output += printTree(prefix + "|   ", root.getLeftchild());
//			output += "\r\n";
		}
		
		if (root.getRightchild() != null) {
			output += printTree(prefix + "|   ", root.getRightchild());
//			output += "\r\n";
		}
		
		return output;
	}
	
	public String toString() {
		String output = "";
		for (ParseNode root : root_forest) {
			output += "\r\n\r\n" + root + "\r\n";
			output += printTree("", root);
		}
		return output;
	}
	
//	private void print(String prefix, boolean isTail) {
//        System.out.println(prefix + (isTail ? "\\--- " : "|--- ") + name);
//        for (int i = 0; i < children.size() - 1; i++) {
//            children.get(i).print(prefix + (isTail ? "    " : "|    "), false);
//        }
//        if (children.size() >= 1) {
//            children.get(children.size() - 1).print(prefix + (isTail ?"    " : "|    "), true);
//        }
//    }
	
	/***
	 * Add a single expression as a root and leaf node.
	 * @param exp
	 */
	public void addNode(Expression exp) {
		ParseNode newnode = findRoot(exp);
		
		if (newnode == null) {
			newnode = new ParseNode(exp);
		}
		
		addRootToForest(newnode);
	}
	
	
	public void addNode(Expression exp, BinaryExpression parent, String node_type) {
		ParseNode newnode = null; 
		
		// Check if any of the existing roots match this new node.
		newnode = findRoot(exp);
		if (newnode != null) {
			// This means that the tree already exists for this node. Remove it from the forest.
			removeRootFromForest(newnode);			
		} else {
			newnode = new ParseNode(exp);
		}
		
		ParseNode root = findRoot(parent);
		
		// Check if the parent does not yet exist as a root
		if (root == null) {
			root = new ParseNode(parent);
			root.setNodeType(node_type);
			
			// Add the new node as the left child in this new tree.
			root.setLeftchild(newnode);
			newnode.setParent(root);			
			
			// Add the new root to the forest.
			addRootToForest(root);
			return;
		}
		
		// Parent root found. Store node as right sub-tree.
		root.setRightchild(newnode);
		newnode.setParent(root);
		return;
		
//		if (root == null) {
//			root = new ParseNode(parent);
//			root.setLeftchild(newnode);
//			newnode.setParent(root);
//			return;
//		}
//		
//		ParseNode current_node = root;
//		while(true) {
//			if (parent.equals(current_node.getNodevalue())) {
//				current_node.setRightchild(newnode);
//				newnode.setParent(current_node);
//				return;
//			}
//			
//			if (current_node.getRightchild() != null) {
//				current_node = current_node.getRightchild();
//			} else {
//				break;
//			}
//		}
//		
//		if (current_node.getLeftchild() == null) {
//			// Create a new tree
//			
//		} else {
//			// At this point, current_node should be some node where the right child is null.
//			ParseNode new_parent = new ParseNode(parent);
//			current_node.setRightchild(new_parent);
//			new_parent.setLeftchild(newnode);
//			newnode.setParent(new_parent);
//		}
//		
////		if (root != null && parent.equals(root.getNodevalue())) {			
////			root.setRightchild(newnode);
////			newnode.setParent(root);
////		} else {
////			ParseNode oldroot = root;
////			root = new ParseNode(parent);
////			if (oldroot != null) {
////				root.setLeftchild(oldroot);
////				root.setRightchild(newnode);
////			} else {
////				root.setLeftchild(newnode);
////			}
////			
////			newnode.setParent(root);
////		}
	}
	
	public ParseNode getRootNode() {
		if (root != null) {
			return root;
		}
		
		if (root_forest.size() > 0) {
			root = root_forest.firstElement();
		}
		
		return root;
	}
}
