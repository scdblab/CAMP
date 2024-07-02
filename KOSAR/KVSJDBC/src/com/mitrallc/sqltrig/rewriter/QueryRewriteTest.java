package com.mitrallc.sqltrig.rewriter;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Set;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Select;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryRewriteTest {	
	
	@Before
	public void SetUp() {
	}
	
	private void AssertThatSetContains(Set<String> str_set, String str) 
			throws AssertionError {
		try {
			assertTrue(str_set.contains(str));
		} catch (AssertionError ae) {
			System.out.println("Set contents: ");
			for (String elem : str_set) {
				System.out.println("\t" + elem);
			}
			
			System.out.println("\r\nString that wasn't found: " + str);
			
			throw ae;
		}
	}

	
	@Test
	public void andQuery() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE d = e AND f = g";
			
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// In this case, no rewrite necessary. Output should match input
		assertEquals(1, query_rewrites.size());		
		assertTrue(query_rewrites.contains(sql));		
	}
	
	@Test
	public void orQuery() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE d = e OR f = g";
		String expected_rewrites[] = {
				"SELECT a FROM b, c WHERE d = e",
				"SELECT a FROM b, c WHERE f = g",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);		
		}
	}
	
	@Test
	public void orAndQuery() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE d = e OR f = g AND h = i";
		// AND has a higher operator precedence than OR.
		String expected_rewrites[] = {
				"SELECT a FROM b, c WHERE d = e",
				"SELECT a FROM b, c WHERE f = g AND h = i",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);		
		}
	}
	
	@Test
	public void orAndParenQuery() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE (d = e OR f = g) AND h = i";
		String expected_rewrites[] = {
				"SELECT a FROM b, c WHERE d = e AND h = i",
				"SELECT a FROM b, c WHERE f = g AND h = i",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);		
		}
	}
	
	@Test
	public void nestedOrParenQuery() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE ((d = e OR f = g) OR h = i) OR (j = k OR l = m)";
		String expected_rewrites[] = {
				"SELECT a FROM b, c WHERE d = e",
				"SELECT a FROM b, c WHERE f = g",
				"SELECT a FROM b, c WHERE h = i",
				"SELECT a FROM b, c WHERE j = k",
				"SELECT a FROM b, c WHERE l = m",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);		
		}
	}
	
	@Test
	public void nestedAndParenQuery() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE ((d = e AND f = g) AND h = i) AND (j = k AND l = m)";
		String expected_rewrites[] = {
				"SELECT a FROM b, c WHERE d = e AND f = g AND h = i AND j = k AND l = m"
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void nestedOrAndParenQuery() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE ((d = e OR f = g) AND h = i) OR (j = k AND l = m)";
		String expected_rewrites[] = {
				"SELECT a FROM b, c WHERE d = e AND h = i",
				"SELECT a FROM b, c WHERE f = g AND h = i",
				"SELECT a FROM b, c WHERE j = k AND l = m",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void variousOperators() throws JSQLParserException {
		String sql = "SELECT a FROM b, c WHERE ((d > e OR f <> g) AND h < i) OR (j >= k AND l <= m)";
		String expected_rewrites[] = {
				"SELECT a FROM b, c WHERE d > e AND h < i",
				"SELECT a FROM b, c WHERE f <> g AND h < i",
				"SELECT a FROM b, c WHERE j >= k AND l <= m",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void multiSelect() throws JSQLParserException {
		String sql = "SELECT a1, a2, avg(a3), count(a4) FROM b, c " +
				"WHERE ((d > e OR f <> g) AND h < i) OR (j >= k AND l <= m) " +
				"GROUP BY a3 ORDER BY a2 ";
		String expected_rewrites[] = {
				"SELECT a1, a2, avg(a3), count(a4) FROM b, c WHERE d > e AND h < i",
				"SELECT a1, a2, avg(a3), count(a4) FROM b, c WHERE f <> g AND h < i",
				"SELECT a1, a2, avg(a3), count(a4) FROM b, c WHERE j >= k AND l <= m",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void singleClauseGroupByOrderBy() throws JSQLParserException {
		String sql = "SELECT a1, a2, avg(a3), count(a4) FROM b, c " +
				"WHERE d = e " +
				"GROUP BY a3 ORDER BY a2 ";
		String expected_rewrites[] = {
				"SELECT a1, a2, avg(a3), count(a4) FROM b, c WHERE d = e",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void aliases() throws JSQLParserException {
		String sql = "SELECT b.a1, c.a2, avg(a3) as avga3, count(a4) as counta4 " +
				"FROM table1 b, table2 c " +
				"WHERE b.d = c.e " +
				"GROUP BY a3 ORDER BY a2 ";
		String expected_rewrites[] = {
				"SELECT b.a1, c.a2, avg(a3), count(a4) FROM table1 b, table2 c WHERE b.d = c.e",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void innerJoin() throws JSQLParserException {
		String sql = "SELECT b.a1, c.a2 " +
				"FROM table1 b " +
				"INNER JOIN table2 c " +
				"ON b.f = c.g " +
				"WHERE b.d = c.e " +
				"GROUP BY a3 ORDER BY a2 ";
		String expected_rewrites[] = {
				"SELECT b.a1, c.a2 FROM table1 b, table2 c WHERE b.d = c.e AND b.f = c.g",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void innerJoinNoWhere() throws JSQLParserException {
		String sql = "SELECT b.a1, c.a2 " +
				"FROM table1 b " +
				"INNER JOIN table2 c " +
				"ON b.f = c.g " +
				"GROUP BY a3 ORDER BY a2 ";
		String expected_rewrites[] = {
				"SELECT b.a1, c.a2 FROM table1 b, table2 c WHERE b.f = c.g",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void otherJoins() throws JSQLParserException {
		String sql = "SELECT b.a1, c.a2 " +
				"FROM table1 b " +
					"LEFT JOIN table2 c ON b.f = c.g, " +
					"table3 b2 RIGHT JOIN table4 c2 ON b2.h = c2.i AND b2.j = c2.k " +
				"WHERE b.d = c.e " +
				"GROUP BY b.a1 ORDER BY c.a2";
		
		// This is an unsupported query type for now. It should return the original query.
		String expected_rewrites[] = {
				sql	
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
	
	@Test
	public void constants() throws JSQLParserException {
		String sql = "SELECT 4321 - MAX(b.a1) - 1234 + (91 - 2) " +
				"FROM table1 b, table2 c " +
				"WHERE b.d = c.e ";
		String expected_rewrites[] = {
				"SELECT MAX(b.a1) FROM table1 b, table2 c WHERE b.d = c.e",
		};
	
		// Rewrite query.
		Set<String> query_rewrites = QueryRewrite.rewriteQuery(sql);
		
		// Output should match expected rewrites.
		assertEquals(expected_rewrites.length, query_rewrites.size());
		
		for(String expected : expected_rewrites) {
			AssertThatSetContains(query_rewrites, expected);
		}
	}
}
