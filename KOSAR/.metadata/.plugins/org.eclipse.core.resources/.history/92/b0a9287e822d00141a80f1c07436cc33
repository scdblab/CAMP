/**
 * 
 */
package com.mitrallc.sqltrig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import com.mitrallc.config.DBConnector;


/**
 * @author Shahram Ghandeharizadeh, Refactor of QueryToTrigger on July 21, 2014
 * Methods of this class transform a query string to a trigger.
 * As of May 17, 2011, the query must reference a single table and use conjunctive predicate.
 * Methods of this class are thread safe because no global variables are maintained.
 */
public class OracleQueryToTrigger extends QueryToTrigger {

	public static boolean IsItTrigger(String cmd, Connection db_conn){
		OracleTrigGenerator og = new OracleTrigGenerator(db_conn);
		return og.IsItTrigger(cmd);
	}

	public static OracleQueryToTrigger.TriggerType WhatIsTriggerType(String cmd, Connection db_conn){
		OracleTrigGenerator og = new OracleTrigGenerator(db_conn);
		return og.WhatIsTriggerType(cmd);
	}

	public static String TableName(String cmd, OracleQueryToTrigger.TriggerType ttype, Connection db_conn){
		OracleTrigGenerator og = new OracleTrigGenerator(db_conn);
		return og.TableName(cmd, ttype);
	}

	/*
	 * 1. Take everything between FROM and WHERE clause.  If it has a comma then the query is referencing more than one table.  Return "False".
	 * 2. If the WHERE clause contains "OR"/"NOT"/"IS LIKE" return FALSE.
	 * 3. Trim the substring obtained from Step 1.  Identify tuple variable T (if any).
	 * 4. Trim the substring from Step 2 and remove all references to variable T; make sure they are formatted as "attr comp constant".
	 * 5. Extract the list of attributes and constants from Step 4 into list L.
	 * 6. Sort the list of Step 5.
	 * 7. Use the constants in L to construct the internal key.
	 * 8. Use the attributes in L to construct the body of the trigger.
	 */
	public boolean TQ(String query, Vector<String> trgrVector, StringBuffer COSARKey, Connection db_conn, OpType task) {
		String tokenizedQuery;
		if (verbose) 
			System.out.println("\nQry input to Trigger: "+query);
		int ANDs = 0;
		int TargetListLength=0;
		int TupVarListLength=0;
		OracleTrigGenerator og = new OracleTrigGenerator(db_conn);
		StringBuffer trgr = new StringBuffer();
		QualificationList ql = new QualificationList();
		Vector QTPList = new Vector();
		boolean AllSelectClausesReferenceOneTable = false;  //applies to processing of qualification lists with join predicates

		if (trgrVector == null || COSARKey == null || query == null) {
			System.out.println("Error (TransformQuery):  None of the input parameters can be null.");
			return false;
		}

		StringBuffer SelectClause = new StringBuffer();
		StringBuffer FromClause = new StringBuffer();
		StringBuffer WhereClause = new StringBuffer();
		StringBuffer OrderByClause = new StringBuffer();

		if ((ANDs=CorrectSyntax (query, SelectClause, FromClause, WhereClause, OrderByClause)) < 0) return false;

		TargetListLength = SelectClause.toString().length();
		TupVarListLength = FromClause.toString().length();

		if (SelectClause == null || FromClause == null || WhereClause == null){
			System.out.println("Error, one of the clauses is null.");
			System.exit(-1);
		}

		StringBuffer Tbl = new StringBuffer();
		if (RefOneTable(FromClause.toString(), Tbl)) {
			//System.out.println("Table = "+Tbl.toString()+", Number of ANDs = "+ ANDs );

			Vector<String> attrs = new Vector<String>();
			Vector<String> vals = new Vector<String>();
			Vector<String> proj = new Vector<String>();

			if (!ProjAttrs (SelectClause.toString(), proj)) return false;
			if (!AttrsVals (WhereClause.toString(), attrs, vals, ANDs, FromClause.toString())){ 
				//Generate table level notification:  IT is the tablename
				String tbl = Tbl.toString(); //FromClause.toString().trim();
				COSARKey.append(tbl);  //IT is the tablename

				og.CreateDeleteTrig(query, tbl, tbl, trgr);
				trgrVector.addElement(trgr.toString());

				trgr = new StringBuffer();
				og.CreateUpdateTrig(query, tbl, tbl, trgr);
				trgrVector.addElement(trgr.toString());

				trgr = new StringBuffer();
				og.CreateInsertTrig(query, tbl, tbl, trgr);
				trgrVector.addElement(trgr.toString());
				return true;
			}

			if (InternalKey(proj, vals, COSARKey)){
				if (task == OpType.GETKEY) return true;
			} else { 
				System.out.println("Error (TransformQuery):  InternalKey failed!");
				return false;
			}

			tokenizedQuery = TokenizeWhereClause( query );
			//String ProcName = ""+Tbl.toString()+""+query.length()+""+ANDs+"0"+proj.size(); //Value 0 because number of join predicates, NumJoinPreds(ql), is zero
			String ProcName = ProcTrigName(Tbl.toString(), ANDs, 0, TargetListLength, TupVarListLength, tokenizedQuery);

			int cntr = attrs.size(); if (cntr > 4) cntr = 4;
			for (int i = 0; i < cntr; i++){
				ProcName=ProcName+attrs.elementAt(i).toString().length();
			}
			cntr = proj.size(); if (cntr>4) cntr = 4;
			for (int i = 0; i < cntr; i++){
				ProcName=ProcName+proj.elementAt(i).toString().length();
			}

			if (verbose)
				System.out.println("Procedure name is: "+ProcName);

			if (ProcName.length() > 30) {
				System.out.println("\nERROR:  ProcName is too long.\n");
				System.out.println("\t Input query:  "+query);
				System.out.println("\t Select clause:  "+SelectClause.toString()+", length="+TargetListLength);
				System.out.println("\t From clause:  "+FromClause.toString()+", length="+TupVarListLength);
			}

			if (! og.SelectUpdateTriggerBody(query, Tbl.toString(), ProcName+"U", proj, attrs, vals, trgr)){ 
				System.out.println("Error (TransformQuery):  Failed to create triggerbody"); 
				return false;
			} else trgrVector.addElement(trgr.toString());

			trgr=new StringBuffer();

			if (! og.SelectDeleteTriggerBody(query, Tbl.toString(), ProcName+"D", proj, attrs, vals, trgr)){ 
				System.out.println("Error (TransformQuery):  Failed to create triggerbody"); 
				return false;
			} else trgrVector.addElement(trgr.toString());

			trgr=new StringBuffer();

			if (! og.SelectInsertTriggerBody(query, Tbl.toString(), ProcName+"I", proj, attrs, vals, trgr)){ 
				System.out.println("Error (TransformQuery):  Failed to create triggerbody"); 
				return false;
			} else trgrVector.addElement(trgr.toString());

			trgr=new StringBuffer();


		} else {
			int ClausesInWhere = LogicalWhereClause(ql, QTPList, FromClause, WhereClause, db_conn);
			if ( ClausesInWhere < 0  ) return false;

			COSARKey.append(GenerateInternalKey(ql,ClausesInWhere));
			if (task == OpType.GETKEY) return true;

			String SingleSelectTable = OneTablesReferencedByAllSelectOps(ql);
			StringBuffer proc = new StringBuffer();

			tokenizedQuery = TokenizeWhereClause( query );

			if (SingleSelectTable != null){
				AllSelectClausesReferenceOneTable = true;
				//Generate a special body of code for the table referenced by the select operator.
				//String ProcName = ""+SingleSelectTable+""+query.length()+""+NumSelPreds(ql)+""+NumJoinPreds(ql);
				String ProcName = ProcTrigName(SingleSelectTable, NumSelPreds(ql), NumJoinPreds(ql), TargetListLength, TupVarListLength, tokenizedQuery);

				if (verbose)
					System.out.println("Procedure name is: "+ProcName);
				if (og.ProcBodyOneTable(this, proc,ProcName,ql,SingleSelectTable, ClausesInWhere)) trgrVector.addElement(proc.toString());
				if (verbose) System.out.println(proc.toString());
				proc = new StringBuffer();
				if (og.CreateInsertTrig(query, this, proc,ProcName,ql,SingleSelectTable, null, true)) trgrVector.addElement(proc.toString());
				if (verbose) System.out.println(proc.toString());
				proc = new StringBuffer();
				if (og.CreateDeleteTrig(query, this, proc,ProcName,ql,SingleSelectTable, null, true)) trgrVector.addElement(proc.toString());
				if (verbose) System.out.println(proc.toString());
				proc = new StringBuffer();
				if (og.CreateUpdateTrig(query, this, proc,ProcName,ql,SingleSelectTable, null, true)) trgrVector.addElement(proc.toString());
				if (verbose) System.out.println(proc.toString());
			}

			for (int i = 0; i < ql.GetTblList().length; i++){
				//String ProcName = ""+ql.GetTblList()[i]+""+query.length()+""+NumSelPreds(ql)+""+NumJoinPreds(ql);
				String ProcName = ProcTrigName(ql.GetTblList()[i], NumSelPreds(ql), NumJoinPreds(ql), TargetListLength, TupVarListLength, tokenizedQuery);

				if (verbose)
					System.out.println("Procedure name is: "+ProcName);
				Vector<StringBuffer> InArgs = new Vector<StringBuffer>();
				if (SingleSelectTable != null  && SingleSelectTable.equals(ql.GetTblList()[i])) continue;
				proc = new StringBuffer();
				if ( og.ProcBodyJoinTable(query, this, proc, ProcName, ql, ql.GetTblList()[i], InArgs, AllSelectClausesReferenceOneTable, ClausesInWhere) ) trgrVector.addElement(proc.toString());
				proc = new StringBuffer();
				if (og.CreateInsertTrig(query, this, proc,ProcName,ql,ql.GetTblList()[i], InArgs, false)) trgrVector.addElement(proc.toString());
				if (verbose) System.out.println(proc.toString());
				proc = new StringBuffer();
				if (og.CreateDeleteTrig(query, this, proc,ProcName,ql,ql.GetTblList()[i], InArgs, false)) trgrVector.addElement(proc.toString());
				if (verbose) System.out.println(proc.toString());
				proc = new StringBuffer();
				if (og.CreateUpdateTrig(query, this, proc,ProcName,ql,ql.GetTblList()[i], InArgs, false)) trgrVector.addElement(proc.toString());
				if (verbose) System.out.println(proc.toString());
			}
		}
		return true;
	}


	public String TransformQuery(String query, Vector<String> trgrVector, Vector<String> CK, Connection db_conn) {
		boolean res = true;
		Set<String> Q = null;
		boolean GotHit = false;

		if (TargetSystem == RDBMS.Oracle){
			query = OracleQueryRewrite.ReWriteQry(query);
			if (verbose){
				System.out.println("QueryToTrigger.TransformQuery "+query);
			}
		}

		logger.debug("TransformQuery with query="+query);
		//if (query.contains("!=")) return null;
		//Range queries are not supported
		if (query.contains(">") || query.contains("<") || query.contains("!=")) return null;

		//Return false if the query string is null.
		if (query.trim().length() == 0) return null;

		StringBuffer COSARKey = new StringBuffer();
		if (trgrVector == null || CK == null){
			System.out.println("Error in TransformQuery:  Input vectors are null.");
			return null;
		}

		Vector<String> triggers = new Vector<String>();
		Vector<String> ITs = new Vector<String>();

		String celt = TokenizeWhereClause(query);
		QTmeta tg = null;
		if (celt != null){
			tg = TriggerCache.get(celt);

			
			//Check to see if the query template has been disabled
			if (tg!= null && !tg.isSwitchButtonOn()) return null;
		}
		
		//System.out.println("Tokenized query: "+celt);
		if (tg != null && tg.getTg() != null){
			//System.out.println("Found triggers in the hash table.\n");
			GotHit = true;
		}
		//		String CapQuery = query.toUpperCase();
		//		if (CapQuery.contains(" OR ") || CapQuery.contains(")OR ") || CapQuery.contains(")OR(") || CapQuery.contains(" OR(")) 
		//		{
		//			Q = rewrite.TransformDisjuncts(query);
		//
		//			if (verbose){
		//				System.out.println("Rewrote query as:");
		//				for (int i=0; i < Q.size(); i++)
		//					System.out.println("\t"+Q.elementAt(i));
		//			}
		//		}
		Q = com.mitrallc.sqltrig.rewriter.QueryRewrite.rewriteQuery(query);		

		if (Q.size() == 0) Q.add(query);

		//if (Q.size() == 0) Q.addElement(query);

		//Q = new Vector();
		//Q.addElement(query);
		//		for (int k=0; k < Q.size(); k++) {
		//			Vector<String> qrys = new Vector<String>();
		//			//check if the query is supported. It doesn't support disjunct, negation and nested queries
		//			if (! rewrite.IsQuerySupported(Q.elementAt(k).toString()) ) return false;
		//			
		//			res = rewrite.RewriteQuery(Q.elementAt(k).toString(), qrys, db_conn);
		for (String newqry : Q) {
			Vector<String> qrys = new Vector<String>();
			if (! rewrite.IsQuerySupported(newqry) ) return null;
			res = rewrite.RewriteQuery(newqry, qrys, db_conn);
			if (res){
				for (int i = 0; i < qrys.size(); i++){
					COSARKey = new StringBuffer();
					Vector<String> qry = new Vector<String>();
					//					if (TQ(qrys.elementAt(i).toString(), qry, COSARKey, db_conn, OpType.TOKENIZE)){
					//						celt = qry.elementAt(0).toString();
					//						System.out.println("Tokenize query "+ celt);
					//					} else return false;
					//					Vector<String> tg = TriggerCache.get(celt);
					if (GotHit) {
						if (!TQ(qrys.elementAt(i).toString(), triggers, COSARKey, db_conn, OpType.GETKEY)) return null;
					} else {
						if (!TQ(qrys.elementAt(i).toString(), triggers, COSARKey, db_conn, OpType.GENTRIGGER)) return null;
					}
					ITs.addElement(COSARKey.toString());
				}
			}
		}
		
		//Shahram ERROR:  The following code copies content.  How about just setting pointers as follows:
		//trgrVector = tg.getTg();
		if (GotHit){
			for (int i = 0; i < tg.getTg().size(); i++)
				trgrVector.addElement(tg.getTg().elementAt(i));
			for (int i=0; i < ITs.size(); i++)
				CK.addElement(ITs.elementAt(i));
		}
		else{ 
			if (ITs.size() < triggers.size()){
				int delta = triggers.size()-ITs.size();
				String elt = ITs.elementAt(0);
				for (int i=0; i < delta; i++)
					ITs.addElement(elt);
			}
			OracleOptimizeTriggers opt = new OracleOptimizeTriggers();
			opt.Optimize(triggers, trgrVector, 2);
			
			QTmeta qm = new QTmeta();
			qm.setQueryTemplate(celt);
			qm.setTg((Vector) trgrVector.clone());

			if (celt != null){
				TriggerCache.put(celt, qm);
			}
			
			for (int i=0; i < ITs.size(); i++)
				CK.addElement(ITs.elementAt(i));
		}

//		if (CK.size() < trgrVector.size()){
//			int delta = trgrVector.size()-CK.size();
//			String elt = CK.elementAt(0);
//			for (int i=0; i < delta; i++)
//				CK.addElement(elt);
//		}

		logger.debug("Qry: "+query);
		for (int i=0; i < trgrVector.size(); i++)
			logger.debug("\t"+trgrVector.elementAt(i).toString());
		for (int i=0; i < CK.size(); i++)
			logger.debug("\t"+CK.elementAt(i));
		logger.debug("\n");

		return celt;
	}



	public static void main(String[] args) {
		Connection db_conn = null;
		Vector<String> V = new Vector<String>();
		String QueryTemplate;
		
		try {
			// Create a connection object to the dbms
			Class.forName ("oracle.jdbc.driver.OracleDriver");
			db_conn = DriverManager.getConnection(DBConnector.getConnectionString(),DBConnector.getUsername(),DBConnector.getPassword());

		} catch(Exception e){
			e.printStackTrace();
			return ;
		}

		OracleQueryToTrigger qt = new OracleQueryToTrigger();

		for (int f=0; f < 4; f++){
			for (int i = 0; i < sampleqrys.length; i++){
				System.out.println("\n--------------------------------------------");
				System.out.println("Qry: "+sampleqrys[i]);
				System.out.println("--------------------------------------------");
				V = new Vector<String>();
				
				StringBuffer sp = new StringBuffer();
				boolean keyres = qt.GetKey(sampleqrys[i], sp, db_conn);
				if (keyres)
					System.out.println("GetKey computes key to be "+sp.toString());

				Vector<String> key = new Vector<String>();
				QueryTemplate = qt.TransformQuery(sampleqrys[i], V, key, db_conn);
				if (QueryTemplate != null){
					System.out.println("Query: "+sampleqrys[i]);
					
					System.out.println("Template: "+QueryTemplate);
					System.out.println("\t "+V.size()+" Trigger:  ");
					for (int j = 0; j < V.size(); j++)
						System.out.println("\t\t "+V.elementAt(j).toString());
					System.out.println("\t "+key.size()+" Keys:  ");
					for (int j=0; j < key.size(); j++)
						System.out.println("\t\t "+key.elementAt(j).toString());
					//System.out.println("\t Internal key: "+ key +"\n");
				} else System.out.println("Query is not supported!");
			}
		}
	}

}
