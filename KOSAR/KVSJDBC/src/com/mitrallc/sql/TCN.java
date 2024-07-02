package com.mitrallc.sql;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.kosar.kosar;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.sqltrig.OracleDBAdapter;

public class TCN {
	public static String select = "SELECT";
	public static String from = "FROM";
	public static String where = "WHERE";
	public static String orderby = "ORDER BY";
	public static String groupby = "GROUP BY";
	public static String and = "AND";
	public static AtomicInteger [] numOfDBMSHits= new AtomicInteger [kosar.NumFragments];
	public static AtomicInteger [] numOfTriggers = new AtomicInteger [kosar.NumFragments];
	Connection conn = null;
	Statement st = null;
	public static int getNumOfDBMSHits(){
		//System.out.println("Num of Threads "+TL.length);
		int res=0;
		for (int i=0;i<numOfDBMSHits.length;i++){
			
			res+=numOfDBMSHits[i].get();
		}
		return res;
	}
	public static int getNumOfTriggers(){
		//System.out.println("Num of Threads "+TL.length);
		int res=0;
		for (int i=0;i<numOfTriggers.length;i++){
			
			res+=numOfTriggers[i].get();
		}
		
		return res;
		
	}
	public void execute(String qry, Connection conn) throws KosarSQLException {
		this.conn = conn;
		
		ArrayList<String> tableNames = new ArrayList<String>();
		tableNames = parseQuery(qry);
		if (tableNames != null) {
			try {
				this.st = this.conn.createStatement();
				for (int i = 0; i < tableNames.size(); i++) {
					if (!OracleDBAdapter.DoesTableExist(tableNames.get(i)
							+ "keys", conn)) {
						try {
							//System.out.println("do you build tables?!");
							this.st.executeUpdate("CREATE TABLE "
									+ tableNames.get(i)
									+ "keys (qry CHAR(1000))");
							st.executeUpdate("create or replace TRIGGER "
									+ tableNames.get(i)
									+ "TrigI BEFORE INSERT ON "
									+ tableNames.get(i)
									+ " FOR EACH ROW  DECLARE KEYTODELETE CLOB;  COSAR_RET_VAL BINARY_INTEGER; "
									+ " k1 CLOB := TO_CLOB(''); seperator CLOB := TO_CLOB(' ');"
									+ " BEGIN FOR somekey IN (SELECT * FROM "
									+ tableNames.get(i)
									+ "Keys)"
									+ " LOOP "
									+ " k1 := CONCAT(k1, somekey.qry);"
									+ " k1 := CONCAT(k1, seperator);"
									+ " END LOOP;"
									+ " IF (k1 != ' ') THEN "
									+ " COSAR_Ret_Val := COSARTriggerDeleteMultiCall('10.0.0.195:4000,10.0.0.195:4001,10.0.0.195:4002,10.0.0.195:4003,10.0.0.195:4004', 'RAYS', k1, 0);"
									+ " END IF;" + " DELETE FROM "
									+ tableNames.get(i) + "keys; " + " END;");
							
							numOfTriggers[kosar.getFragment(qry)].incrementAndGet();
							//System.out.println(numOfTriggers[42]);
							st.executeUpdate("create or replace TRIGGER "
									+ tableNames.get(i)
									+ "TrigD BEFORE DELETE ON "
									+ tableNames.get(i)
									+ " FOR EACH ROW  DECLARE KEYTODELETE CLOB;  COSAR_RET_VAL BINARY_INTEGER; "
									+ " k1 CLOB := TO_CLOB(''); seperator CLOB := TO_CLOB(' ');"
									+ " BEGIN FOR somekey IN (SELECT * FROM "
									+ tableNames.get(i)
									+ "Keys) LOOP "
									+ " k1 := CONCAT(k1, somekey.qry);"
									+ " k1 := CONCAT(k1, seperator);"
									+ " END LOOP;"
									+ " IF (k1 != ' ') THEN "
									+ " COSAR_Ret_Val := COSARTriggerDeleteMultiCall('10.0.0.195:4000,10.0.0.195:4001,10.0.0.195:4002,10.0.0.195:4003,10.0.0.195:4004', 'RAYS', k1, 0);"
									+ " END IF;" + " DELETE FROM "
									+ tableNames.get(i) + "keys; " + " END;");
							numOfTriggers[kosar.getFragment(qry)].incrementAndGet();
							st.executeUpdate("create or replace TRIGGER "
									+ tableNames.get(i)
									+ "TrigU BEFORE UPDATE ON "
									+ tableNames.get(i)
									+ " FOR EACH ROW  DECLARE KEYTODELETE CLOB;  COSAR_RET_VAL BINARY_INTEGER; "
									+ " k1 CLOB := TO_CLOB(''); seperator CLOB := TO_CLOB(' ');"
									+ " BEGIN FOR somekey IN (SELECT * FROM "
									+ tableNames.get(i)
									+ "Keys) LOOP "
									+ " k1 := CONCAT(k1, somekey.qry);"
									+ " k1 := CONCAT(k1, seperator);"
									+ " END LOOP;"
									+ " IF (k1 != ' ') THEN "
									+ " COSAR_Ret_Val := COSARTriggerDeleteMultiCall('10.0.0.195:4000,10.0.0.195:4001,10.0.0.195:4002,10.0.0.195:4003,10.0.0.195:4004', 'RAYS', k1, 0);"
									+ " END IF;" + " DELETE FROM "
									+ tableNames.get(i) + "keys; " + " END;");
							numOfTriggers[kosar.getFragment(qry)].incrementAndGet();
						}

						catch (Exception e) {
							System.out.println("table exist");
						}

						
					}
					
					String revsql = qry.replace(" ", "_");
					//System.out.println("before" + numOfDBMSHits[kosar.getFragment(revsql)]);
					
					//System.out.println("after" + numOfDBMSHits[kosar.getFragment(revsql)]);
					revsql = revsql.replaceAll("'", "\"");
					//System.out.println("revsql : "+revsql);
					this.st.executeUpdate("INSERT INTO " + tableNames.get(i)
							+ "keys (qry) VALUES('" + revsql + "')");
				}

			} catch (SQLException e) {
				throw new KosarSQLException(e.getMessage());
			} finally {
				try {
					this.st.close();
				} catch (SQLException e) {
					throw new KosarSQLException(e.getMessage());
				}
			}

		}
	}

	public static int CorrectSyntax(String query, StringBuffer SelectClause,
			StringBuffer FromClause, StringBuffer WhereClause,
			StringBuffer OrderByClause) {
		int NumANDS = 0;
		if (SelectClause == null || FromClause == null || WhereClause == null
				|| OrderByClause == null) {
			System.out
					.println("Error (CorrectSyntax method of QueryToTrigger class):  Input parameters cannot be null.  They must be instantiated by the caller");
			return -1;
		}

		StringBuffer revQ = new StringBuffer(query.toUpperCase());

		int idxS, idxF, idxW, idxq, idxO;
		idxS = revQ.indexOf(select);
		idxF = revQ.indexOf(from);
		idxW = revQ.indexOf(where);
		idxq = revQ.indexOf(groupby);
		idxO = revQ.indexOf(orderby);

		if (idxO < 0)
			idxO = revQ.length(); // needed to compute the where clause
		else {
			OrderByClause.append(query.substring(idxO + orderby.length(),
					revQ.length()).trim());
		}

		if (idxW > 0) {
			if (idxS < 0 || idxF < 0 || idxW < 0 || idxF < idxS || idxW < idxS
					|| idxW < idxF)
				return -1;
			FromClause.append(query.substring(idxF + from.length(), idxW)
					.trim());
		} else {
			if (idxS < 0 || idxF < 0 || idxF < idxS)
				return -1;
			if (idxq >= 0)
				FromClause.append(query.substring(idxF + from.length(), idxq)
						.trim());
			else if (idxO >= 0)
				FromClause.append(query.substring(idxF + from.length(), idxO)
						.trim());
			else
				FromClause.append(query.substring(idxF + from.length()).trim());
		}

		SelectClause.append(query.substring(idxS + select.length(), idxF)
				.trim());

		if (idxW > 0) {
			String wp = query.substring(idxW + where.length(), idxO).trim()
					.replace("(", " ").replace(")", " ");
			String[] wp2 = wp.toUpperCase().split(" AND ");
			String[] preds = new String[wp2.length];
			int ioffset = 0;
			for (int i = 0; i < wp2.length; i++) {
				int idx = wp.toUpperCase().indexOf(" AND ");
				if (idx > 0) {
					preds[i] = wp.substring(0, idx);
					wp = wp.substring(idx + " AND ".length());
				} else {
					preds[i] = wp; // only one token exists
				}
			}
			// String[] preds = query.substring(idxW+where.length(),
			// idxO).trim().replace("(",
			// "").replace(")","").toUpperCase().split(" AND ");

			NumANDS = preds.length - 1;

			Vector<String> sp = new Vector<String>();
			for (int i = 0; i < preds.length; i++) {
				if (sp.size() == 0)
					sp.add(preds[i]);
				else {
					int j;
					for (j = 0; j < sp.size(); j++)
						if (sp.elementAt(j).compareTo(preds[i]) >= 0)
							break;
					sp.add(j, preds[i]);
				}
			}

			for (int i = 0; i < sp.size(); i++) {
				if (i > 0)
					WhereClause.append(" AND ");
				WhereClause.append(sp.elementAt(i));
			}
			// WhereClause.append( query.substring(idxW+where.length(),
			// revQ.length()).trim().replace("(", "").replace(")","") );
			// WhereClause.append( query.substring(idxW+where.length(),
			// revQ.length()).trim() );

			// String wherecl = revQ.substring(idxW+where.length(),
			// revQ.length()).trim();
			//
			// if (wherecl.indexOf(" OR ") >= 0 || wherecl.indexOf(" NOT ") >= 0
			// || wherecl.indexOf(" LIKE ") >= 0) return -1;
			//
			//
			// boolean done = false;
			// do{
			// if (wherecl.indexOf(" AND ") >= 0){
			// NumANDS++;
			// wherecl =
			// wherecl.substring(wherecl.indexOf(" AND ")+and.length());
			// } else done = true;
			// } while (!done);
		}

		return NumANDS;
	}

	public static ArrayList<String> parseQuery(String qry) {
		// TODO Auto-generated method stub
		StringBuffer SelectClause = new StringBuffer();
		StringBuffer FromClause = new StringBuffer();
		StringBuffer WhereClause = new StringBuffer();
		StringBuffer OrderByClause = new StringBuffer();
		int ANDs = 0;
		if ((ANDs = CorrectSyntax(qry, SelectClause, FromClause, WhereClause,
				OrderByClause)) < 0) {
			System.out.println("Could not parse query: " + qry);
			return null;
		}

		String[] TargetTbls = FromClause.toString().toUpperCase().split(",");
		ArrayList<String> Tbls = new ArrayList<String>();
		String[] TupVars = new String[TargetTbls.length];

		for (int i = 0; i < TargetTbls.length; i++) {
			TargetTbls[i] = TargetTbls[i].trim();
			if (TargetTbls[i].indexOf(" ") > 0) {
				Tbls.add(TargetTbls[i].substring(0, TargetTbls[i].indexOf(" ")));
				TupVars[i] = TargetTbls[i].substring(
						TargetTbls[i].indexOf(" ") + 1, TargetTbls[i].length());
			} else {
				Tbls.add(TargetTbls[i].trim());
				TupVars[i] = null;
			}
		}
		return Tbls;
	}

}
