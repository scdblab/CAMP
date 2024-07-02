package com.mitrallc.sqltrig.rewriter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Select;

public class QueryRewrite {
	private static final boolean DEBUG_MODE = false;
	
	public static String printSubtree(ParseNode root) {
		if (root.isLeafNode()) {
			return root.toString();
		} else if (root.getNodeType().equals("AND")) {
			return printSubtree(root.getLeftchild()) + " AND " + 
					printSubtree(root.getRightchild());
		} else if (root.getNodeType().equals("OR")) {
			if (root.isLeftVisited()) {
				return printSubtree(root.getRightchild());
			} 
			
			root.setLeftVisited(true);
			return printSubtree(root.getLeftchild());
		} else {
			System.out.println("ERROR printing subtree. Invalid node type: " + root.getNodeType());
		}
		
		return null;
	}
	
	public static void findOrNodes(ParseNode root, Vector<ParseNode> or_nodes) {
		if(root.getLeftchild() != null) {
			findOrNodes(root.getLeftchild(), or_nodes);
		}
		
		if("OR".equals(root.getNodeType())) {
			or_nodes.add(root);
		}
		
		if(root.getRightchild() != null) {
			findOrNodes(root.getRightchild(), or_nodes);
		}
	}
	
	public static void setLeftVisitedFalse(Vector<ParseNode> or_nodes, int starting_count) {
		for (int i = starting_count; i < or_nodes.size(); i++) {
			or_nodes.get(i).setLeftVisited(false);
		}
	}
	
	public static boolean checkAllVisited(Vector<ParseNode> or_nodes) {
		for(ParseNode node : or_nodes) {
			if (!node.isLeftVisited()) {
				return false;
			}
		}
		return true;
	}
	
	public static Set<String> enumerateQueries(ParseNode root) {
		Vector<ParseNode> or_nodes = new Vector<ParseNode>();
		findOrNodes(root, or_nodes);
		// Reset left_visited state to false for all
		setLeftVisitedFalse(or_nodes, 0);	
		
		String query;
		HashMap<String, Integer> query_map = new HashMap<String, Integer>();
		
		for (int current = 0; current < or_nodes.size(); current++) {
			while(!checkAllVisited(or_nodes)) {
				or_nodes.get(current).setLeftVisited(false);
				query = printSubtree(root);
				if (query_map.containsKey(query)) {					
					// DEBUG: keep track of how many duplicate calculations there were.
					query_map.put(query, query_map.get(query) + 1);
					break;
				}				
				query_map.put(query, 1);
			}
			
			// Repeat this step to capture the query when the last OR node has just been set to visited
			// TODO: clean up this logic, there must be a better way of doing this.
			or_nodes.get(current).setLeftVisited(false);
			query = printSubtree(root);
			if (query_map.containsKey(query)) {					
				// DEBUG: keep track of how many duplicate calculations there were.
				query_map.put(query, query_map.get(query) + 1);
			} else {
				query_map.put(query, 1);
			}
			
			// Reset left_visited state of remaining OR nodes
			setLeftVisitedFalse(or_nodes, current + 1);			
		}
		
		// Repeat this step to capture the query when all OR nodes have left_visited = true
		// TODO: clean up this logic, there must be a better way of doing this.
		query = printSubtree(root);
		if (query_map.containsKey(query)) {					
			// DEBUG: keep track of how many duplicate calculations there were.
			query_map.put(query, query_map.get(query) + 1);
		} else {
			query_map.put(query, 1);
		}

		return query_map.keySet();
	}
	
	public static Set<String> expandQueries(ParseNode node) {
		Set<String> string_list = new TreeSet<String>();
		if (node == null) {
			return string_list;
		}
		
		if (node.isLeafNode()) {
			string_list.add(node.toString());
		} else if ("AND".equals(node.getNodeType())) {
			Set<String> left_sublist = expandQueries(node.getLeftchild());
			Set<String> right_sublist = expandQueries(node.getRightchild());
			
			for (String left : left_sublist) {
				for (String right : right_sublist) {
					string_list.add(left + " AND " + right);
				}
			}
		} else if ("OR".equals(node.getNodeType())) {
			Set<String> left_sublist = expandQueries(node.getLeftchild());
			Set<String> right_sublist = expandQueries(node.getRightchild());
			
			for (String left : left_sublist) {
				string_list.add(left);
			}
			
			for (String right : right_sublist) {
				string_list.add(right);
			}
		} else {
			System.out.println("ERROR. Invalid node type: " + node.getNodeType());
			string_list = null;
		}
		
		return string_list;
	}
	
	public static Set<String> rewriteQuery(String sql) {
		Set<String> query_rewrites = null;
		CCJSqlParserManager pm = new CCJSqlParserManager();
		net.sf.jsqlparser.statement.Statement statement = null;
		try {
			statement = pm.parse(new StringReader(sql));
			if (statement instanceof Select) {
				Select selectStatement = (Select) statement;
				if (DEBUG_MODE) System.out.println(selectStatement.getSelectBody());
				
				ParseTreeGenerator tree_gen = new ParseTreeGenerator();
				ParseTree tree = tree_gen.getParseTree(selectStatement, sql);				
				if (DEBUG_MODE) System.out.println(tree);
				
				query_rewrites = new TreeSet<String>();
									
				Set<String> where_clauses = QueryRewrite.expandQueries(tree.getRootNode());
				if (where_clauses.isEmpty()) {
					String rewrite = tree_gen.generateQuery("");
					query_rewrites.add(rewrite);

					if (DEBUG_MODE) System.out.println(rewrite);
				} else {
					for(String where_clause : where_clauses) {
						String rewrite = tree_gen.generateQuery(where_clause);
						query_rewrites.add(rewrite);
						
						if (DEBUG_MODE) System.out.println(rewrite);
					}
				}
			}
		} catch (JSQLParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return query_rewrites;
	}
	
	public static void main(String[] args) {
//		CCJSqlParserManager pm = new CCJSqlParserManager();
		String sql =
//				"SELECT * " +
//				"FROM MY_TABLE1 " +
//				"WHERE " +					
//					"(MyCol1 = MyCol2 OR MyCol3 < MyCol4)" +
//					" AND MyCol1 = MyCol2 " +
//					" OR " +
//					"(MyCol1 = MyCol2 AND MyCol5 > MyCol6)";
		
//				"SELECT * " +
//				"FROM MY_TABLE1 " +
//				"WHERE " +
////					"MyCol11 < MyCol12 AND " +
//					"(MyCol1 = MyCol2 OR MyCol3 <> MyCol4) AND " + 
//					"(MyCol5 = MyCol6 OR MyCol7 > MyCol8 OR MyCol9 = MyCol10)";  
				
//				"SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel " +
//				"FROM users, friendship " +
//				"WHERE ((inviterid=? and userid=inviteeid) or (inviteeid=? and userid=inviterid)) and status = 2";

//				"SELECT J.i_id,J.i_thumbnail " +
//				"from item I, item J " +
//				"where " +
//				"(I.i_related1 = J.i_id or " +
//				"I.i_related2 = J.i_id or " +
//				"I.i_related3 = J.i_id or " +
//				"I.i_related4 = J.i_id or " +
//				"I.i_related5 = J.i_id) and I.i_id = ?";
				
//				"select COUNT(*) AS rowcount " +
//				"from friends " +
//				"where status='2' AND ( userid=  '27' OR friendid='27' )";
				
//				"select * " +
//				"from users u, user_cameras uc, friends f " +
//				"where " +
//					"u.user_id = uc.user_id and is_streaming='1' and " +
//					"u.user_id=f.userid and f.status='2' and  " +
//					"(f.friendid='273' OR f.userid='273') ";
				
//				"SELECT * FROM MyTable WHERE " + 
//				"(MyCol1 = MyCol2 OR MyCol3 <> MyCol4)  AND " + 
//				"(MyCol5 = MyCol6 OR MyCol7 > MyCol8 OR MyCol9 = MyCol10)";  
				
				"SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "+
						" WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)" ;
						

		
		
//		net.sf.jsqlparser.statement.Statement statement = null;
//		try {
//			statement = pm.parse(new StringReader(sql));
//			if (statement instanceof Select) {
//				Select selectStatement = (Select) statement;
//				System.out.println(selectStatement.getSelectBody());
//				
//				ParseTreeGenerator cf = new ParseTreeGenerator();
//				ParseTree tree = cf.getParseTree(selectStatement);
//				System.out.println(tree);
//				
//				//Set<String> query_rewrites = QueryRewrite.enumerateQueries(tree.getRootNode());
//				Set<String> query_rewrites = QueryRewrite.rewriteQueries(tree.getRootNode(), cf);
//				for(String query : query_rewrites) {
//					System.out.println(query);
//				}
//			}
//		} catch (JSQLParserException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		for(String query : query_rewrites) {
			System.out.println(query);
		}
	}
}
