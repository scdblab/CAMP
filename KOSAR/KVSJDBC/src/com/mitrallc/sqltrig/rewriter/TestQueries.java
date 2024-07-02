package com.mitrallc.sqltrig.rewriter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;
import java.util.Set;
import java.util.Vector;

import com.rays.config.DBConnector;
import com.rays.sqltrig.QueryToTrigger;

public class TestQueries {
	public static final String INPUT_SEPARATOR = "\\$\\$\\$";
	public static final String OUTPUT_SEPARATOR = "\t";
	public static void loadQueries(String filename, Vector<String> query_list, Vector<Integer> expected_queries) {
		BufferedReader fin = null;
		String line = "";
		String tokens[] = null;
		
		try {
			fin = new BufferedReader(new FileReader(filename));
			
			line = fin.readLine();
			while(line != null) {
				tokens = line.split(INPUT_SEPARATOR);
				expected_queries.add(Integer.parseInt(tokens[0].trim()));
				query_list.add(tokens[1]);
				line = fin.readLine();
			}
			
			fin.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public static void rewriteQuery(String query, Vector<Vector<String>> rewrite_list, Connection db_conn) {
		Vector<String> rewrite = new Vector<String>();
		Set<String> rewrite_set = QueryRewrite.rewriteQuery(query);
//		if (COSARsqlRewrite.RewriteQuery(query, rewrite, db_conn)) {
//			rewrite_list.add(rewrite);
//		}
		
//		if (rewrite_set != null && rewrite_set.size() > 0) {
		Vector<String> rewrite_vector = new Vector<String>();
		for (String sql : rewrite_set) {
			rewrite_vector.add(sql);
		}
		
		rewrite_list.add(rewrite_vector);
//		} else {
//			rewrite_list.add(new Vector<String>());
//		}
	}
	
	public static void generateTriggers(String query, Vector<Vector<String>> trigger_list, 
			Vector<Vector<String>> token_list, Connection db_conn) {
		QueryToTrigger QTT = new QueryToTrigger();
		Vector<String> trig_vector = new Vector<String>();
		Vector<String> internal_key_vector = new Vector<String>();
		QTT.TransformQuery(query, trig_vector, internal_key_vector, db_conn);
		
		trigger_list.add(trig_vector);
		token_list.add(internal_key_vector);
	}
	
	public static void outputTriggers(Vector<Vector<String>> trigger_list) throws IOException {
		DataOutputStream fout = null;
		fout = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream("triggers.sql")));
		
		for (Vector<String> trig_set : trigger_list) {
			for (String trig : trig_set) {
				fout.writeBytes(trig);
				fout.writeBytes("\r\n / \r\n");
			}
		}
		
		fout.flush();
		fout.close();
	}
	
	public static void outputQueries(
			Vector<String> query_list, 
			Vector<Integer> expected_rewrites, 
			Vector<Vector<String>> rewrite_list,
			String input_filename,
			String output_directory,
			String output_filename) throws IOException {
		int matching_count = 0;
		int nonmatching_count = 0;
		DataOutputStream fout = null;
		
		if (output_filename != null && !output_filename.isEmpty()) {
			fout = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(output_directory + output_filename, false)));
			
			fout.writeBytes("\r\n" + input_filename + "\r\n");
			fout.writeBytes("date" + OUTPUT_SEPARATOR + 
					"original query" + OUTPUT_SEPARATOR + 
					"num_expected_rewrites" + OUTPUT_SEPARATOR + 
					"num_actual_rewrites" + OUTPUT_SEPARATOR + 
					"match_expected" + OUTPUT_SEPARATOR + 
					"rewrites\r\n");
		}
		
		for (int i = 0; i < rewrite_list.size(); i++) {
			System.out.println((i + 1) + ". Original: " + query_list.get(i));			
			System.out.println("\tRewrite: ");
			
			if (fout != null) {
				fout.writeBytes(
						new Date().toString() + OUTPUT_SEPARATOR + 
						query_list.get(i) + OUTPUT_SEPARATOR + 
						expected_rewrites.get(i) + OUTPUT_SEPARATOR);
			}
			
			int rewrite_count = 0;
			for (String rewrite : rewrite_list.get(i)) {				
				if (!rewrite.isEmpty()) {
					rewrite_count++;
				}
				System.out.println("\t\t" + (rewrite_count) + ". " + rewrite);
			}
			
			// Go through list of rewrites again after count has been checked to output to the file
			if (fout != null) {
				fout.writeBytes(rewrite_count + OUTPUT_SEPARATOR);
				if (expected_rewrites.get(i) != rewrite_count) {
					fout.writeBytes("0");
				}
				fout.writeBytes(OUTPUT_SEPARATOR);
				
				for (String rewrite : rewrite_list.get(i)) {
					fout.writeBytes(rewrite.trim() + OUTPUT_SEPARATOR);
				}
				
				fout.writeBytes("\r\n");
			}
			
			if (expected_rewrites.get(i) != rewrite_count) {
				System.out.println("MISMATCH!! # queries expected: " + expected_rewrites.get(i) +
						", observed: " + rewrite_count);
				nonmatching_count++;
			} else {
				matching_count++;
			}
		}
		
		if (fout != null) {
			fout.writeBytes("\r\nMatching" + OUTPUT_SEPARATOR + matching_count + OUTPUT_SEPARATOR + 
					"Not Matching" + OUTPUT_SEPARATOR + nonmatching_count);
			fout.writeBytes("\r\n");
			fout.flush();
			fout.close();
		}
		
		
		System.out.println("Matching: " + matching_count + ", Not matching: " + nonmatching_count);
	}
	
	public static void main1(String[] args) {
		String query = "SELECT * FROM comments WHERE to_user_id = ?";
		
		Vector<String> query_list = null;
		Vector<Integer> expected_rewrites = null;
		Vector<Vector<String>> rewrite_list = null;
		Vector<Vector<String>> token_list = null;
		
		Connection db_conn = null;
		try {
			// Create a connection object to the dbms
				Class.forName ("oracle.jdbc.driver.OracleDriver");
				db_conn = DriverManager.getConnection(
						DBConnector.getConnectionString(),
						DBConnector.getUsername(),
						DBConnector.getPassword());

		} catch(Exception e){
			e.printStackTrace();
			return ;
		}
		

		query_list = new Vector<String>();
		expected_rewrites = new Vector<Integer>();
		rewrite_list = new Vector<Vector<String>>();
		token_list = new Vector<Vector<String>>();
		generateTriggers(query, rewrite_list, token_list, db_conn);
		
		for (Vector<String> str_list : rewrite_list) {
			System.out.println(query);
			for (String trig : str_list) {
				System.out.println("\t" + trig);
			}
		}
	}
	
	public static void main(String[] args) {
		if ( !TestQueries.class.desiredAssertionStatus() ) {
			System.err.println( "WARNING: assertions are disabled!" );
			try { Thread.sleep( 3000 ); } catch ( InterruptedException e ) {}
		}
		
		
		
		Vector<String> query_list = null;
		Vector<Integer> expected_rewrites = null;
		Vector<Vector<String>> rewrite_list = null;
		Vector<Vector<String>> token_list = null;
		
		Connection db_conn = null;
		try {
			// Create a connection object to the dbms
				Class.forName ("oracle.jdbc.driver.OracleDriver");
				db_conn = DriverManager.getConnection(
						DBConnector.getConnectionString(),
						DBConnector.getUsername(),
						DBConnector.getPassword());

		} catch(Exception e){
			e.printStackTrace();
			return ;
		}
		
		String directory = "queries_rewrite_hint/";
		String output_directory = "";
		String input_filenames[] = {
				"rubis_servlet_java.txt",
//				"bg_jdbc_basic2R1T.txt",
//				"bg_jdbc_boosted2R1T.txt",
//				"tpcw_pruivo_mysql.txt",
//				"rubbos_servlet_java.txt",
		};
		
		for (String input_filename : input_filenames) {
			query_list = new Vector<String>();
			expected_rewrites = new Vector<Integer>();
			rewrite_list = new Vector<Vector<String>>();
			token_list = new Vector<Vector<String>>();
			
			String output_filename = input_filename + "processed_trig.tsv";
			loadQueries(directory + input_filename, query_list, expected_rewrites);
			for(String query : query_list) {
				//rewriteQuery(query, rewrite_list, db_conn);
				generateTriggers(query, rewrite_list, token_list, db_conn);
			}
			
			assert(query_list.size() == rewrite_list.size());
			
			try {
//				outputQueries(query_list, expected_rewrites, rewrite_list, 
//						input_filename, output_directory, output_filename);
				outputTriggers(rewrite_list);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
