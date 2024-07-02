package com.mitrallc.sqltrig.rewriter;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Select;

public class ParseQueryTest {
//    public static void main(String args[]) throws Exception {
//        PLSQL3Lexer lex = new PLSQL3Lexer(new ANTLRStringStream("select a from b where c = d and e = f;"));
//        CommonTokenStream tokens = new CommonTokenStream(lex);
// 
//        PLSQL3Parser parser = new PLSQL3Parser(tokens); // created from T.g
//        PLSQL3Parser.select_statement_return r = parser.select_statement(); // launch parsing
//        
//        if ( r!=null ) System.out.println(((CommonTree)r.tree).toStringTree());
//        
//        System.out.println(parser.toString());
// 
//        CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
//        nodes.setTokenStream(tokens);
//        System.out.println(nodes.toString());
////        TP walker = new TP(nodes); // created from TP.g
////        TP.startRule_return r2 = walker.startRule();
////        CommonTree rt = ((CommonTree)r2.tree);
////        // if tree parser constructs trees
////        if ( rt!=null ) System.out.println(((CommonTree)r2.tree).toStringTree());               
//    }
	
	public static void main(String[] args) {
		CCJSqlParserManager pm = new CCJSqlParserManager();
//		String sql = "SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "+
//		" WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)" ;
		
		String sql =
				"SELECT * " +
				"FROM MY_TABLE1 " +
				"WHERE " +					
					"(MyCol1 = MyCol2 OR MyCol3 < MyCol4)" +
					" AND MyCol1 = MyCol2 " +
					" OR " +
					"(MyCol1 = MyCol2 AND MyCol5 > MyCol6)";



//				"SELECT * " +
//				"FROM MY_TABLE1 " +
//				"WHERE " +
//					"MY_COL1 = MY_COL2" +
//					" OR " +
//					"(MY_COL3 < MY_COL4" +
//					" OR " +
//					"MY_COL5 <> MY_COL6)" +
//					" OR " +
//					"MY_COL7 > MY_COL8" +
//					" AND " +
//					"(MY_COL9 = MY_COL10 AND MY_COL11 = MY_COL12)";
		
		
//		"SELECT * " +
//		"FROM MY_TABLE1 " +
//		"WHERE " +
//			"MY_COL1 = MY_COL2" +
//			" AND " +
//			"MY_COL3 < MY_COL4" +
//			
//			
//		"SELECT * " +
//		"FROM MY_TABLE1 " +
//		"WHERE " +
//			"MY_COL1 = MY_COL2" +
//			" AND " +
//			"MY_COL5 <> MY_COL6";
		net.sf.jsqlparser.statement.Statement statement = null;
		try {
			statement = pm.parse(new StringReader(sql));
			
			/* 
			now you should use a class that implements StatementVisitor to decide what to do
			based on the kind of the statement, that is SELECT or INSERT etc. but here we are only
			interested in SELECTS
			*/
			if (statement instanceof Select) {
				Select selectStatement = (Select) statement;
//				TableNamesFinder tablesNamesFinder = new TableNamesFinder();
//				List tableList = tablesNamesFinder.getTableList(selectStatement);
//				for (Iterator iter = tableList.iterator(); iter.hasNext();) {
//					String tableName = (String)iter.next();
//					System.out.println(tableName);
//				}
				
				ParseTreeGenerator cf = new ParseTreeGenerator();
				ParseTree tree = cf.getParseTree(selectStatement, sql);
				System.out.println(tree);
			}
		} catch (JSQLParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}