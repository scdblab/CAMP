package com.mitrallc.sqltrig.rewriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.*;
import net.sf.jsqlparser.statement.select.*;

public class ParseTreeGenerator implements 
		SelectVisitor, FromItemVisitor, ExpressionVisitor, ItemsListVisitor {
	private List<String> tables;
	private List<SelectItem> select_items;
	private List<Join> join_items;
//	private Vector<String> query_list;
//	private LinkedList<String> predicate_stack;
//	private LinkedList<BinaryExpression> parent_stack;
	private ParseTree parse_tree;
	private boolean selectVisited;
	private boolean unsupportedQuery;
	private String originalQuery;

	public ParseTree getParseTree(Select select, String original_sql) {
//		query_list = new Vector<String>();
//		predicate_stack = new LinkedList<String>();
//		parent_stack = new LinkedList<BinaryExpression>();
		tables = new Vector<String>();
		parse_tree = new ParseTree();
		join_items = new Vector<Join>();
		select_items = null;	// Gets initialized by call to plainSelect.getSelectItems()
		
		selectVisited = false;
		unsupportedQuery = false;
		originalQuery = original_sql;
		
		select.getSelectBody().accept(this);
		
//		for (String predicate: predicate_stack) {
//			System.out.println(predicate);
//		}
		return parse_tree;
	}
	
	/***
	 * Get rid of aggregate functions like COUNT and AVG
	 * @param item
	 * @return
	 */
	private Expression removeFunction(SelectExpressionItem item) {		
		if (item.getExpression() instanceof Function) {
			Function f = (Function)item.getExpression();
			return f.getParameters().getExpressions().get(0);
		}
		return item.getExpression();
	}
	
	private boolean isConstant(Expression exp) {
		if (exp instanceof LongValue) {
			return true;
		} else if (exp instanceof DoubleValue) {
			return true;
		} else if (exp instanceof StringValue) {
			return true;
		} else if (exp instanceof NullValue) {
			return true;
		}
		
		return false;
	}
	
	private String removeConstants(BinaryExpression item) {		
		if (isConstant(item.getRightExpression()) && isConstant(item.getLeftExpression())) {
			// Ignore both.
			return "";
		}
		
		String leftoutput = "";
		String rightoutput = "";
		
		if (!isConstant(item.getLeftExpression())) {
			// Ignore the right expression constant.
			leftoutput += extractString(item.getLeftExpression());
		} 		
		
		if (!isConstant(item.getRightExpression())) {
			rightoutput += extractString(item.getRightExpression());
		} 
		
		// Ignore the left expression constant.
		if (!leftoutput.isEmpty() && !rightoutput.isEmpty()) {
			return leftoutput + " " + item.getStringExpression() + " " + rightoutput;
		} else if (!leftoutput.isEmpty()) {
			return leftoutput;
		} else if (!rightoutput.isEmpty()) {
			return rightoutput;
		}
		
		return "";
	}
	
	private String extractString(Expression exp) {
		if (exp instanceof Function) {
			// TODO: currently disabled. Handle this in SQLTrig? Or in a separate stage?
			//       want to rewrite aggregates to something SQLTrig supports. Examples:
			//			count(*) -> primary key
			//		 	count(col1) -> col1
//			return removeFunction(expitem).toString();
			
			return exp.toString();
		} else if (exp instanceof Subtraction) {
			return removeConstants((Subtraction)exp);
		} else if (exp instanceof Addition) {
			return removeConstants((Addition)exp);
		} else if (exp instanceof Parenthesis) {
			return extractString(((Parenthesis) exp).getExpression());
		}
		
		return exp.toString();
	}
	
	private String extractString(SelectItem item) {
		if (item instanceof SelectExpressionItem) {
			SelectExpressionItem expitem = (SelectExpressionItem)item;
			return extractString(expitem.getExpression());
		}
		return item.toString();
	}
	
	public String generateQuery(String where_clause) {
		if (unsupportedQuery) {
			System.out.println("Unsupported query type. Returning original query" );
			return originalQuery;
		}
		
		String query = "SELECT ";
		Iterator<SelectItem> select_it = select_items.iterator();
		while(select_it.hasNext()) {
			query += extractString(select_it.next());
			if (select_it.hasNext()) {
				query += ", ";
			}
		}
		
		query += " FROM ";
		Iterator<String> table_it = tables.iterator();
		while (table_it.hasNext()) {
			query += table_it.next();
			if (table_it.hasNext()) {
				query += ", ";
			}
		}
		
		if (!where_clause.isEmpty() || !join_items.isEmpty()) {
			query += " WHERE " + where_clause;
			
			if (!where_clause.isEmpty() && !join_items.isEmpty()) {
				query += " AND ";
			}
			
			Iterator<Join> join_it = join_items.iterator();
			while (join_it.hasNext()) {
				// Assumes that the join has an OnExpression
				query += join_it.next().getOnExpression().toString();
				if (join_it.hasNext()) {
					query += " AND ";
				}
			}
		}		
		
		return query;
	}

	public void visit(PlainSelect plainSelect) {
		if (selectVisited) {
			unsupportedQuery = true;
		}
		selectVisited = true;		
		
		plainSelect.getFromItem().accept(this);
		select_items = plainSelect.getSelectItems();
		
		if (plainSelect.getJoins() != null) {
			for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
				Join join = (Join) joinsIt.next();
				join.getRightItem().accept(this);
				
				if (join.getOnExpression() != null) {
//					System.out.println("Join: " + join.toString());
//					System.out.println("Join right: " + join.getRightItem().toString());
//					System.out.println("JoinOnExp: " + join.getOnExpression().toString());
					
					if (join.isOuter() || join.isLeft() || join.isRight()) {
						unsupportedQuery = true;
					}
					join_items.add(join);
				}
			}
		}
		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(this);
			
			// If the root of the parse tree is null, that means there
			// were no binary expressions. Add the whole where clause
			// as the root of the parse tree.
			if (parse_tree.getRootNode() == null) {
				parse_tree.addNode(plainSelect.getWhere());
			}
		}

	}

//	public void visit(UnionType union) {
//		for (Iterator iter = union.getPlainSelects().iterator(); iter.hasNext();) {
//			PlainSelect plainSelect = (PlainSelect) iter.next();
//			visit(plainSelect);
//		}
//	}

	public void visit(Table tableName) {
		String tableWholeName = tableName.getWholeTableName();
//		System.out.println("Orig: " + tableName);
//		System.out.println("Alias: " + tableName.getAlias());
//		System.out.println("Whole: " + tableWholeName);

		if (tableName.getAlias() != null) {
			tableWholeName += " " + tableName.getAlias();
		}
		tables.add(tableWholeName);
	}

	public void visit(SubSelect subSelect) {
		subSelect.getSelectBody().accept(this);
	}

	public void visit(Addition addition) {
		visitBinaryExpression(addition);
	}

	public void visit(AndExpression andExpression) {
//		parent_stack.push(andExpression);
		visitBinaryExpression(andExpression);
//		parent_stack.pop();
	}

	public void visit(Between between) {
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionEnd().accept(this);
	}

	public void visit(Column tableColumn) {
	}

	public void visit(Division division) {
		visitBinaryExpression(division);
	}

	public void visit(DoubleValue doubleValue) {
	}

	public void visit(EqualsTo equalsTo) {
		visitBinaryExpression(equalsTo);
	}

	public void visit(Function function) {
	}

	public void visit(GreaterThan greaterThan) {
		visitBinaryExpression(greaterThan);
	}

	public void visit(GreaterThanEquals greaterThanEquals) {
		visitBinaryExpression(greaterThanEquals);
	}

	public void visit(InExpression inExpression) {
		inExpression.getLeftExpression().accept(this);
		inExpression.getRightItemsList().accept(this);
	}

	public void visit(InverseExpression inverseExpression) {
		inverseExpression.getExpression().accept(this);
	}

	public void visit(IsNullExpression isNullExpression) {
	}

	public void visit(JdbcParameter jdbcParameter) {
	}

	public void visit(LikeExpression likeExpression) {
		visitBinaryExpression(likeExpression);
	}

	public void visit(ExistsExpression existsExpression) {
		existsExpression.getRightExpression().accept(this);
	}

	public void visit(LongValue longValue) {
	}

	public void visit(MinorThan minorThan) {
		visitBinaryExpression(minorThan);
	}

	public void visit(MinorThanEquals minorThanEquals) {
		visitBinaryExpression(minorThanEquals);
	}

	public void visit(Multiplication multiplication) {
		visitBinaryExpression(multiplication);
	}

	public void visit(NotEqualsTo notEqualsTo) {
		visitBinaryExpression(notEqualsTo);
	}

	public void visit(NullValue nullValue) {
	}

	public void visit(OrExpression orExpression) {
//		parent_stack.push(orExpression);
		visitBinaryExpression(orExpression);
//		parent_stack.pop();
	}

	public void visit(Parenthesis parenthesis) {
		parenthesis.getExpression().accept(this);
	}

	public void visit(StringValue stringValue) {
	}

	public void visit(Subtraction subtraction) {
		visitBinaryExpression(subtraction);
	}

	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		binaryExpression.getLeftExpression().accept(this);		
		binaryExpression.getRightExpression().accept(this);
		
		Expression left = binaryExpression.getLeftExpression();
		
		if (binaryExpression instanceof OrExpression) {
			parse_tree.addNode(binaryExpression.getLeftExpression(), binaryExpression, "OR");
			parse_tree.addNode(binaryExpression.getRightExpression(), binaryExpression, "OR");
		} else if (binaryExpression instanceof AndExpression) {
			parse_tree.addNode(binaryExpression.getLeftExpression(), binaryExpression, "AND");
			parse_tree.addNode(binaryExpression.getRightExpression(), binaryExpression, "AND");
		}
		
		if (binaryExpression instanceof OrExpression) {
//			predicate_stack.push(binaryExpression.getLeftExpression().toString());
//			predicate_stack.push(binaryExpression.getRightExpression().toString());
						
//			int num_items = predicate_stack.size();
//			System.out.print(predicate_stack.pop());
//			System.out.print(" OR ");
//			System.out.println(predicate_stack.pop());
		} else if (binaryExpression instanceof AndExpression) {
//			parent_stack.push(binaryExpression);
			
//			int num_items = predicate_stack.size();
//			System.out.print(predicate_stack.pop());
//			System.out.print(" AND ");
//			System.out.println(predicate_stack.pop());
		}
//		if (!(binaryExpression instanceof OrExpression) && 
//				!(binaryExpression instanceof AndExpression)) {
		else {
//			System.out.println(binaryExpression);
//			predicate_stack.push(binaryExpression.toString());
			
//			parse_tree.addNode(binaryExpression, parent_stack.peekFirst());
		}
	}

	public void visit(ExpressionList expressionList) {
		for (Iterator iter = expressionList.getExpressions().iterator(); iter.hasNext();) {
			Expression expression = (Expression) iter.next();
			expression.accept(this);
		}

	}

	public void visit(DateValue dateValue) {
	}
	
	public void visit(TimestampValue timestampValue) {
	}
	
	public void visit(TimeValue timeValue) {
	}

	public void visit(CaseExpression caseExpression) {
	}

	public void visit(WhenClause whenClause) {
	}

	public void visit(AllComparisonExpression allComparisonExpression) {
		allComparisonExpression.getSubSelect().getSelectBody().accept(this);
	}

	public void visit(AnyComparisonExpression anyComparisonExpression) {
		anyComparisonExpression.getSubSelect().getSelectBody().accept(this);
	}

	public void visit(SubJoin subjoin) {
		subjoin.getLeft().accept(this);
		subjoin.getJoin().getRightItem().accept(this);
	}

	@Override
	public void visit(Concat arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Matches arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseAnd arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseOr arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseXor arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MultiExpressionList arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(JdbcNamedParameter arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(CastExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Modulo arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AnalyticExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ExtractExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IntervalExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OracleHierarchicalExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(LateralSubSelect arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ValuesList arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SetOperationList arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(WithItem arg0) {
		// TODO Auto-generated method stub
		
	}

}
