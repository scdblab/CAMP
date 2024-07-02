package com.mitrallc.sqltrig;

import java.sql.Connection;
import java.util.Vector;

public class OracleTrigGenerator {
	public static final String StartProc = "CREATE OR REPLACE PROCEDURE ";
	public static final String StartTrig = "CREATE OR REPLACE TRIGGER ";
	public static final String insert = "BEFORE INSERT ON ";
	public static final String delete = "BEFORE DELETE ON ";
	public static final String update = "BEFORE UPDATE ON ";
	public static final String BeginVariableDec = "DECLARE";
	public static final String EndVariableDec = "BEGIN";
	public static final String EndTrigger ="END;";
	boolean verbose = false;
	public static boolean DEBUG = false;  //Be very very careful before turning this flag on; generates unique triggers; one per query
	private Connection db_conn;
	
	public static boolean AreTriggersEqual(String ExistTrigger,String cmd){
		String trig1 = ExistTrigger.substring( ExistTrigger.indexOf("BEFORE") );
		String trig2 = cmd.substring( cmd.indexOf("BEFORE") );
		boolean result = trig1.equalsIgnoreCase(trig2);
		return result;
	}

	public static String AssembleFromDBMS(String trigname, String tablename, String triggertype, String triggertiming, String triggerstmt){
		String result = StartTrig+" "+trigname+" ";

		switch(triggertiming.trim()){
		case "INSERT":
			result += insert;
			break;
		case "UPDATE":
			result += update;
			break;
		case "DELETE":
			result += delete;
			break;
		default:
			break;
		}
		result += tablename+" FOR EACH ROW ";
		result += triggerstmt;
		//result += ";";

		return result;
	}

	public boolean IsItTrigger(String cmd){
		if (cmd.startsWith(StartTrig))
			return true;
		return false;
	}
	
	public static QueryToTrigger.TriggerType WhatIsTriggerType(String cmd){
		if (cmd.contains(insert)) return QueryToTrigger.TriggerType.insert;
		if (cmd.contains(delete)) return QueryToTrigger.TriggerType.delete;
		if (cmd.contains(update)) return QueryToTrigger.TriggerType.update;
		return null;
	}
	
	public static String TableName(String cmd, QueryToTrigger.TriggerType ttype){
		int stidx;
		String namestarts=null;
		if (cmd.contains(insert)) namestarts = cmd.substring( cmd.indexOf(insert) + insert.length() ).trim();
		if (cmd.contains(delete)) namestarts = cmd.substring( cmd.indexOf(delete) + delete.length() ).trim();
		if (cmd.contains(update)) namestarts = cmd.substring( cmd.indexOf(update) + update.length() ).trim();
		if (namestarts != null){
			int endidx = namestarts.indexOf(" ");
			return namestarts.substring(0, endidx);
		}
		return namestarts;
	}
	
	public OracleTrigGenerator(Connection db_connection) 
	{
		db_conn = db_connection;
	}
	
	public String TrigProcName(String ProcName, String Operation){
		return " "+ProcName+Operation+" ";
	}
	
	public boolean SelectDeleteTriggerBody(String qry, String TblName, String ProcName, Vector<String> proj, Vector<String> attrs, Vector<String> vals, StringBuffer triggerbody)
	{
		boolean found = false;
		triggerbody.append(""+StartTrig+" "+ProcName+" ");
//		for (int i = 0; i < attrs.size(); i++){
//			triggerbody.append(""+attrs.elementAt(i).toString().length());
//		}
//		for (int i = 0; i < proj.size(); i++){
//            triggerbody.append(""+proj.elementAt(i).toString().length());
//		}
		if (DEBUG) triggerbody.append("/* "+qry+" */");
		//Done creating the trigger name
		triggerbody.append(delete+TblName+" FOR EACH ROW DECLARE DeleteIT_DLL_Val BINARY_INTEGER; KEYTODELETE  CLOB; /* DELETEKEY CLOB; */ BEGIN KEYTODELETE := ' '; ");
		//triggerbody.append(delete+TblName+" FOR EACH ROW DECLARE DeleteIT_DLL_Val BINARY_INTEGER; KEYTODELETE  CLOB; /* DELETEKEY CLOB; */ BEGIN KEYTODELETE := ' '; /*DELETEKEY := ' ';*/");
		
		triggerbody.append(" KEYTODELETE := CONCAT('"+QueryToTrigger.InternalToken);
		//triggerbody.append(" DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', CONCAT('"+InternalToken);
		//for (int i = 0; i < proj.size(); i++)
			triggerbody.append(OracleDBAdapter.TokenizeAttr(proj,attrs)); //proj.elementAt(i).toString().length());
		triggerbody.append("',");
		switch (attrs.size()) {
		case 1:
			triggerbody.append(":OLD."+attrs.elementAt(0));
			break;
		default:
			for (int i = 0; i < attrs.size()-1; i++){
				triggerbody.append("CONCAT(:OLD."+attrs.elementAt(i).toString()+", ");
			}
			triggerbody.append(":OLD."+attrs.elementAt(attrs.size()-1).toString());
		}
		for (int i = 0; i < attrs.size(); i++) triggerbody.append(")");
		triggerbody.append(";");
			

		triggerbody.append("DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', KEYTODELETE, 0);");
		
		triggerbody.append(" END;");
		
		//System.out.print("Trigger:  "+triggerbody.toString());
		return true;
	}
	
	public boolean SelectInsertTriggerBody(String qry, String TblName, String ProcName, Vector<String> proj, Vector<String> attrs, Vector<String> vals, StringBuffer triggerbody)
	{
		boolean found = false;
		triggerbody.append(""+StartTrig+" "+ProcName+" ");
//		for (int i = 0; i < attrs.size(); i++){
//			triggerbody.append(""+attrs.elementAt(i).toString().length());
//		}
//		for (int i = 0; i < proj.size(); i++){
//            triggerbody.append(""+proj.elementAt(i).toString().length());
//		}
		if (DEBUG) triggerbody.append("/* "+qry+" */");
		//Done creating the trigger name
		triggerbody.append(insert+TblName+" FOR EACH ROW DECLARE DeleteIT_DLL_Val BINARY_INTEGER; KEYTODELETE  CLOB; /* DELETEKEY CLOB; */ BEGIN KEYTODELETE := ' '; ");
		//triggerbody.append(insert+TblName+" FOR EACH ROW DECLARE DeleteIT_DLL_Val BINARY_INTEGER; KEYTODELETE  CLOB; /* DELETEKEY CLOB; */ BEGIN KEYTODELETE := ' '; /*DELETEKEY := ' ';*/");
			
		triggerbody.append(" KEYTODELETE := CONCAT('"+QueryToTrigger.InternalToken);
		//triggerbody.append(" DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', CONCAT('"+InternalToken);
		//for (int i = 0; i < proj.size(); i++)
			triggerbody.append(OracleDBAdapter.TokenizeAttr(proj, attrs));//proj.elementAt(i).toString().length());
		triggerbody.append("',");
		switch (attrs.size()) {
		case 1:
			triggerbody.append(":NEW."+attrs.elementAt(0));
			break;
		default:
			for (int i = 0; i < attrs.size()-1; i++){
				triggerbody.append("CONCAT(:NEW."+attrs.elementAt(i).toString()+", ");
			}
			triggerbody.append(":NEW."+attrs.elementAt(attrs.size()-1).toString());
		}
		for (int i = 0; i < attrs.size(); i++) triggerbody.append(")");
		triggerbody.append(";");
			

		triggerbody.append(" DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', KEYTODELETE, 0);");
		
		triggerbody.append(" END;");
		
		//System.out.print("Trigger:  "+triggerbody.toString());
		return true;
	}
	
	public boolean IsTypeAcceptable(int tp){
		
		if (tp==java.sql.Types.BIGINT 
				|| tp==java.sql.Types.CHAR 
				|| tp==java.sql.Types.DECIMAL
				|| tp==java.sql.Types.DOUBLE
				|| tp==java.sql.Types.FLOAT
				|| tp==java.sql.Types.INTEGER
				|| tp==java.sql.Types.LONGVARCHAR
				|| tp==java.sql.Types.NUMERIC
				|| tp==java.sql.Types.REAL
				|| tp==java.sql.Types.SMALLINT
				|| tp==java.sql.Types.TINYINT
				|| tp==java.sql.Types.VARCHAR
				)
			return true;
		else return false;
		
	}
	
	public boolean SelectUpdateTriggerBody(String qry, String TblName, String ProcName, Vector<String> proj, Vector<String> attrs, Vector<String> vals, StringBuffer triggerbody)
	{
		boolean found = false;
		triggerbody.append(""+StartTrig+" "+ProcName+" ");
//		int cntr = attrs.size(); if (cntr > 4) cntr = 4;
//		for (int i = 0; i < cntr; i++){
//			triggerbody.append(""+attrs.elementAt(i).toString().length());
//		}
//		cntr = proj.size(); if (cntr>4) cntr = 4;
//		for (int i = 0; i < cntr; i++){
//            triggerbody.append(""+proj.elementAt(i).toString().length());
//		}
		
		//Done creating the trigger name
		if (DEBUG) triggerbody.append("/* "+qry+" */");
		triggerbody.append(update+TblName+" FOR EACH ROW DECLARE DeleteIT_DLL_Val BINARY_INTEGER; KEYTODELETE  CLOB; /* DELETEKEY CLOB; */ BEGIN KEYTODELETE := ' '; ");
		//triggerbody.append(update+TblName+" FOR EACH ROW DECLARE DeleteIT_DLL_Val BINARY_INTEGER; KEYTODELETE  CLOB; /* DELETEKEY CLOB; */ BEGIN KEYTODELETE := ' '; /*DELETEKEY := ' ';*/");
		
		
		/* BLOB
		 * If a field is a BLOB then a comparison cannot be made.
		 * Proceed to invalidate the cached entry
		 */
		boolean DoLimit = true;
		int tp;
		for (int i = 0; i < proj.size(); i++)
		{
			tp = QueryToTrigger.dba.GetColumnType(db_conn, TblName, (String)proj.elementAt(i));
			if (!IsTypeAcceptable(tp)) DoLimit = false;
		}
		for (int i = 0; i < attrs.size(); i++)
		{
			tp = QueryToTrigger.dba.GetColumnType(db_conn, TblName, (String)attrs.elementAt(i));
			if (!IsTypeAcceptable(tp)) DoLimit = false;
		}
		if (DoLimit && proj.size()> 0) {
			triggerbody.append(" IF (");
			for (int i = 0; i < proj.size(); i++)
			{
				triggerbody.append(":OLD."+proj.elementAt(i).toString().trim()+" <> :NEW."+proj.elementAt(i).toString().trim());
				if (i < proj.size()-1) triggerbody.append(" OR ");
			}
			for (int i = 0; i <attrs.size(); i++){
				found = false;
				for (int k = 0; k < proj.size(); k++)
					if (attrs.elementAt(i).toUpperCase().equals( proj.elementAt(k).toUpperCase() ) )
					{
						found = true;
						k = proj.size();
					}
				if (!found)
					triggerbody.append(" OR :OLD."+attrs.elementAt(i).toString().trim()+" <> :NEW."+attrs.elementAt(i).toString().trim());
			}
			triggerbody.append(") THEN  ");
		}
		/*
		 * Code reduction to isolate performance problem
		 */
		triggerbody.append(" KEYTODELETE := CONCAT('"+QueryToTrigger.InternalToken);
		//triggerbody.append(" DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', CONCAT('"+InternalToken);
		//for (int i = 0; i < proj.size(); i++)
			triggerbody.append(OracleDBAdapter.TokenizeAttr(proj, attrs)); //proj.elementAt(i).toString().length());
		triggerbody.append("',");
		switch (attrs.size()) {
		case 1:
			triggerbody.append(":OLD."+attrs.elementAt(0));
			break;
		default:
			for (int i = 0; i < attrs.size()-1; i++){
				triggerbody.append("CONCAT(:OLD."+attrs.elementAt(i).toString()+", ");
			}
			triggerbody.append(":OLD."+attrs.elementAt(attrs.size()-1).toString());
		}
		for (int i = 0; i < attrs.size(); i++) triggerbody.append(")");
		triggerbody.append(";");
		
		triggerbody.append(" DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', KEYTODELETE, 0);");

		
		////////////////////////////////////////////////////////////////////////////
		//And now, generate it for the :NEW
		triggerbody.append(" KEYTODELETE := CONCAT('"+QueryToTrigger.InternalToken);
		//triggerbody.append(" DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', CONCAT('"+InternalToken);
		//for (int i = 0; i < proj.size(); i++)
		triggerbody.append(OracleDBAdapter.TokenizeAttr(proj, attrs));//proj.elementAt(i).toString().length());
		triggerbody.append("',");
		switch (attrs.size()) {
		case 1:
			triggerbody.append(":NEW."+attrs.elementAt(0));
			break;
		default:
			for (int i = 0; i < attrs.size()-1; i++){
				triggerbody.append("CONCAT(:NEW."+attrs.elementAt(i).toString()+", ");
			}
			triggerbody.append(":NEW."+attrs.elementAt(attrs.size()-1).toString());
		}
		for (int i = 0; i < attrs.size(); i++) triggerbody.append(")");
		triggerbody.append(";");
			

		triggerbody.append(" DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', KEYTODELETE, 0);");
		
		
		
		//BLOB kludge 
		if (DoLimit && proj.size() > 0) triggerbody.append(" END IF;");
		
		triggerbody.append(" END;");
		
		//System.out.print("Trigger:  "+triggerbody.toString());
		return true;
		
//		CREATE TRIGGER StreamStatusChg3
//		BEFORE UPDATE ON user_cameras
//		FOR EACH ROW
//		DECLARE
//		  uid number := 5;
//		  k1  CLOB := TO_CLOB('cameraTab');
//		  k2  CLOB := TO_CLOB('profile-userinformation');
//		  k3  CLOB := TO_CLOB('LoadLivestreamsJSP');
//		  myfid friends.userid%TYPE;
//		  CURSOR FriendCursor IS 
//		      select userid from friends 
//		      where status='2' AND ( userid=uid OR friendid=uid );
//		BEGIN
//		  uid := :NEW.user_id;
//		  /* dbms_output.put_line('Trigger is before; uid = ' || uid); */
//		  k1 := CONCAT(k1,uid);
//		  k2 := CONCAT(k2,uid);
//		  dbms_output.put_line('k1 = ' || k1 || ', k2 = ' || k2);
//		  OPEN FriendCursor;
//		  LOOP
//		     FETCH FriendCursor INTO myfid;
//		     IF myfid != uid THEN dbms_output.put_line('k3 = ' || CONCAT(k3,myfid) );
//		     END IF;
//		     EXIT WHEN FriendCursor%NOTFOUND;
//		  END LOOP;
//		  CLOSE FriendCursor;
//		END;
//		/
	}
	
	
	public boolean ProcBodyOneTable(QueryToTrigger trt, StringBuffer proc, String ProcName, QualificationList ql, String TblName, int ClausesInWhere)
	{
		boolean results = true;
		StringBuffer InputArgs = new StringBuffer();

		if (proc == null){
			System.out.println("Error, input proc cannot be null.");
			return false;
		}
		int NumArgs = trt.GenInputArgs(ql, InputArgs);
		String joinpreds = trt.GenJoinPredStrings(ql);
		
		proc.append(StartProc+" "+ ProcName +" ("+ InputArgs.toString() +"/*, DELETEKEY IN OUT CLOB*/) ");
		proc.append("IS TOKEN CLOB := TO_CLOB('"+ joinpreds +""+ClausesInWhere+"');");
		proc.append("DeleteIT_DLL_Val BINARY_INTEGER;");
		proc.append("KEYTODELETE CLOB; ");
		proc.append("BEGIN ");
		
		proc.append("KEYTODELETE := CONCAT(");
		for (int i = 1; i < NumArgs+1; i++){
			proc.append("arg"+i);
			if (i < NumArgs) proc.append(", CONCAT(");
		}
		proc.append(", TOKEN");
		for (int i = 0; i < NumArgs; i++) proc.append(")");
		proc.append("; ");
		
		if (DEBUG) proc.append(" DBMS_OUTPUT.PUT_LINE('Proc "+ProcName+" delete '||KEYTODELETE); ");
		
		proc.append("");
		
		proc.append("DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', KEYTODELETE, 0);");
		proc.append("END;");
		return results;
	}
	
	public boolean ProcBodyJoinTable(String Inputqry, QueryToTrigger trt, StringBuffer proc, String ProcName, QualificationList ql, String TblName, Vector<StringBuffer> InArgs, boolean AllSelectClausesReferenceOneTable, int NumClausesInWhere)
	{
		boolean results = true;
		
		StringBuffer qry = new StringBuffer();
		StringBuffer InputArgs = new StringBuffer();
		Vector VarArgs = new Vector();
		String joinpreds = trt.GenJoinPredStrings(ql);
		Vector Vargs = new Vector();

		//int NumArgs = trt.JoinGenInputArgs(ql, TblName, InputArgs);
		trt.JoinGenInputArgs(ql, TblName, Vargs);

		String CursorQry = trt.GenCurosorQry(ql, TblName, qry, InputArgs, VarArgs, InArgs, AllSelectClausesReferenceOneTable);
		
		proc.append(StartProc+" "+ ProcName +" ("+InputArgs.toString()+" /*, DELETEKEY IN OUT CLOB*/) ");
		proc.append(" IS ");
		if (DEBUG) proc.append("/* "+Inputqry+" */");
		//proc.append(" "+VarArgs.toString()+" ");
		for (int i = 0; i < VarArgs.size(); i++){
			proc.append("var"+(i+1)+" "+VarArgs.elementAt(i).toString());
			proc.append("; ");
		}
		proc.append(" TOKEN CLOB := TO_CLOB('"+ joinpreds +""+NumClausesInWhere+"');");
		proc.append(" DeleteIT_DLL_Val BINARY_INTEGER;");
		proc.append(" KEYTODELETE CLOB; ");
		proc.append(" CURSOR CRS1 IS "+qry+"; ");
		proc.append(" BEGIN ");
		proc.append(" OPEN CRS1; ");
		proc.append(" LOOP ");
		proc.append(" FETCH CRS1 into ");
		for (int i = 0; i < VarArgs.size(); i++){
			proc.append("var"+(i+1));
			if (i < VarArgs.size()-1) proc.append(", ");
		}
		proc.append("; ");
		proc.append(" EXIT WHEN CRS1%NOTFOUND; ");
		proc.append("KEYTODELETE := CONCAT(");
		for (int i = 0; i < VarArgs.size(); i++){
			proc.append("var"+(i+1));
			if (i < VarArgs.size()-1){
				proc.append(", CONCAT(");
			}
		}
		proc.append(", TOKEN");
		for (int i = 0; i < VarArgs.size(); i++) proc.append(")");
		proc.append("; ");
		
		if (DEBUG) proc.append(" DBMS_OUTPUT.PUT_LINE('Proc "+ProcName+" delete '||KEYTODELETE); ");
		
		proc.append("DeleteIT_DLL_Val := KOSARTriggerDeleteCall('RAYS', KEYTODELETE, 0);");
		proc.append(" END LOOP; ");
		proc.append(" CLOSE CRS1; ");
		proc.append("END;");
		if (verbose) System.out.println(""+proc.toString());
		return results;
	}
	
	public boolean CreateInsertTrig(String qry, QueryToTrigger trt, StringBuffer proc, String ProcName, QualificationList ql, String TblName, Vector<StringBuffer> InArgs, boolean SELECT)
	{
		boolean results = true;
		if (proc == null || ql == null){
			System.out.println("Error in CreateInsertTrig:  Input proc and ql cannot be null");
			return false;
		}
		StringBuffer res = new StringBuffer();
		if (InArgs == null){
		int Args;
		if (SELECT)
			Args = trt.CallInputArgs(":NEW", ql, res);
		else
			Args = trt.CallInputArgsJoin(":NEW", ql, res, TblName);
		} else {
			for (int i=0; i < InArgs.size(); i++){
				res.append(":NEW."+InArgs.elementAt(i).toString());
				if (i < InArgs.size()-1) res.append(", ");
			}
		}
		
		proc.append(StartTrig+ TrigProcName(ProcName, "I"));
		//proc.append(StartTrig+" I"+ProcName+" ");
		if (DEBUG) proc.append("/* "+qry+" */");
		proc.append(insert+TblName);
		proc.append(" FOR EACH ROW DECLARE DELETEKEY CLOB; KEYTODELETE CLOB; DeleteIT_DLL_Val BINARY_INTEGER; ");
		proc.append(" BEGIN ");
		if (DEBUG) proc.append (" DBMS_OUTPUT.PUT_LINE('Trigger "+ProcName+"'); ");
		proc.append(" "+ProcName+"("+ res.toString() +" /*, DELETEKEY*/); ");
		proc.append(" END; ");
		return results;
	}
	
	public boolean CreateDeleteTrig(String qry, QueryToTrigger trt, StringBuffer proc, String ProcName, QualificationList ql, String TblName, Vector<StringBuffer> InArgs, boolean SELECT)
	{
		boolean results = true;
		if (proc == null || ql == null){
			System.out.println("Error in CreateInsertTrig:  Input proc and ql cannot be null");
			return false;
		}
		StringBuffer res = new StringBuffer();
		if (InArgs == null) {
			int Args;
			if (SELECT) Args = trt.CallInputArgs(":OLD", ql, res);
			else Args = trt.CallInputArgsJoin(":OLD", ql, res, TblName);
		} else {
			for (int i=0; i < InArgs.size(); i++){
				res.append(":OLD."+InArgs.elementAt(i).toString());
				if (i < InArgs.size()-1) res.append(", ");
			}
		}
		proc.append(StartTrig + TrigProcName(ProcName,"D"));
		//proc.append(StartTrig+" D"+ProcName+" ");
		if (DEBUG) proc.append("/* "+qry+" */");
		proc.append(delete+TblName);
		proc.append(" FOR EACH ROW DECLARE DELETEKEY CLOB; KEYTODELETE CLOB; DeleteIT_DLL_Val BINARY_INTEGER; ");
		proc.append(" BEGIN ");
		if (DEBUG) proc.append (" DBMS_OUTPUT.PUT_LINE('Trigger "+ProcName+"'); ");
		proc.append(" "+ProcName+"("+ res.toString() +" /*, DELETEKEY*/); ");
		proc.append(" END; ");
		return results;
	}
	
	public boolean CreateUpdateTrig(String qry, QueryToTrigger trt, StringBuffer proc, String ProcName, QualificationList ql, String TblName, Vector<StringBuffer> InArgs, boolean SELECT)
	{
		boolean results = true;
		if (proc == null || ql == null){
			System.out.println("Error in CreateInsertTrig:  Input proc and ql cannot be null");
			return false;
		}
		StringBuffer res = new StringBuffer();
		proc.append(StartTrig + TrigProcName(ProcName,"U") );
		//proc.append(StartTrig+" U"+ProcName+" ");
		if (DEBUG) proc.append("/* "+qry+" */");
		proc.append(update+TblName);
		proc.append(" FOR EACH ROW DECLARE DELETEKEY CLOB; KEYTODELETE CLOB; DeleteIT_DLL_Val BINARY_INTEGER; ");
		proc.append(" BEGIN ");
		if (DEBUG) proc.append (" DBMS_OUTPUT.PUT_LINE('Trigger "+ProcName+"'); ");
		if (InArgs == null){
			int Args=0;
			if (SELECT) Args = trt.CallInputArgs(":OLD", ql, res);
			else Args = trt.CallInputArgsJoin(":OLD", ql, res, TblName);
		} else {
			for (int i=0; i < InArgs.size(); i++) {
				res.append(":OLD."+InArgs.elementAt(i).toString());
				if (i < InArgs.size()-1) res.append(", ");
			}
		}

		proc.append(" "+ProcName+"("+ res.toString() +" /*, DELETEKEY*/); ");

		res = new StringBuffer();

		if(InArgs == null){
			int Args;
			//if (SELECT) Args = trt.CallInputArgs(":NEW", ql, res);
			//else Args = trt.CallInputArgsJoin(":NEW", ql, res, TblName);
			if (SELECT) Args = trt.CallInputArgs("", ql, res);
			else Args = trt.CallInputArgsJoin("", ql, res, TblName);
			String[] attrs = res.toString().split(",");
			res = new StringBuffer();

			/* BLOB kludge */
			boolean DoLimit = true;
			int tp;
			for (int i = 0; i < attrs.length; i++)
			{
				tp = QueryToTrigger.dba.GetColumnType(db_conn, TblName, (String)attrs[i].trim());
				if (!IsTypeAcceptable(tp)) DoLimit = false;
			}
			if (DoLimit){
				res.append("IF (");
				for (int i=0; i < attrs.length; i++){
					res.append(":NEW"+attrs[i].trim()+" <> :OLD"+attrs[i].trim());
					if (i < attrs.length-1) res.append(" OR ");
				}
				res.append(") THEN  "); 
			}
			res.append(" "+ProcName+"(");
			for (int i = 0; i < attrs.length; i++) {
				res.append(":New"+attrs[i].trim());
				if (i < attrs.length-1) res.append(", ");
			}
			res.append(" /*, DELETEKEY*/); ");
			
			if (DoLimit) res.append(" END IF;");
			
			proc.append(""+res);
		} else {
			//Delete new only if the referenced attribute values for the new are different than the old
			/* BLOB kludge */
			boolean DoLimit = true;
			int tp;
			for (int i = 0; i < InArgs.size(); i++)
			{
				tp = QueryToTrigger.dba.GetColumnType(db_conn, TblName, InArgs.elementAt(i).toString());
				if (!IsTypeAcceptable(tp)) DoLimit = false;
			}
			if (DoLimit){
				res.append("IF (");
				for (int i=0; i < InArgs.size(); i++) {
					res.append(":NEW."+InArgs.elementAt(i).toString()+" <> :OLD."+InArgs.elementAt(i).toString());
					if (i < InArgs.size()-1) res.append(" OR ");
				}
				res.append(") THEN ");
			}
			res.append(" "+ProcName+"(");
			for (int i=0; i < InArgs.size(); i++) {
				res.append(":NEW."+InArgs.elementAt(i).toString());
				if (i < InArgs.size()-1) res.append(", ");
			}
			res.append(" /*, DELETEKEY*/); ");
			if (DoLimit) res.append(" END IF;");
			proc.append(""+res);
		}
		
		proc.append(" END; ");
		return results;
	}
	
	/*
	 * The following generate triggers at the granularity of a table
	 * for insert/delete/update
	 */
	public boolean CreateInsertTrig(String qry, String ProcName, String TblName, StringBuffer proc)
	{
		boolean res = true;
		if (proc == null){
			System.out.println("Error in CreateInsertTrig:  Input proc and ql cannot be null");
			return false;
		}
		proc.append(StartTrig+TrigProcName(ProcName,"I"));
		//proc.append(StartTrig+" I"+ProcName+" ");
		proc.append(insert+TblName);
		proc.append(" FOR EACH ROW DECLARE KEYTODELETE CLOB; DeleteIT_DLL_Val BINARY_INTEGER; /* DELETEKEY CLOB; */");
		proc.append(" BEGIN ");
		proc.append(" KEYTODELETE := CONCAT('"+TblName+"', KEYTODELETE); ");
		proc.append("IF (DELETEKEY != ' ') THEN  DeleteIT_DLL_Val := KOSARTriggerDeleteMultiCall('"+QueryToTrigger.getIPport()+"', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN RAISE_APPLICATION_ERROR( -20001, 'Failed to connect to KOSAR KVS CORE.'); END IF; END IF; END;");
		return res;
	}
	
	public boolean CreateDeleteTrig(String qry, String ProcName, String TblName, StringBuffer proc)
	{
		boolean res = true;
		if (proc == null){
			System.out.println("Error in CreateInsertTrig:  Input proc and ql cannot be null");
			return false;
		}
		proc.append(StartTrig+TrigProcName(ProcName,"D"));
		//proc.append(StartTrig+" D"+ProcName+" ");
		proc.append(delete+TblName);
		proc.append(" FOR EACH ROW DECLARE KEYTODELETE CLOB; DeleteIT_DLL_Val BINARY_INTEGER; /* DELETEKEY CLOB; */");
		proc.append(" BEGIN ");
		proc.append(" KEYTODELETE := CONCAT('"+TblName+"', KEYTODELETE); ");
		proc.append("IF (DELETEKEY != ' ') THEN  DeleteIT_DLL_Val := KOSARTriggerDeleteMultiCall('"+QueryToTrigger.getIPport()+"', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN RAISE_APPLICATION_ERROR( -20001, 'Failed to connect to KOSAR KVS CORE.'); END IF; END IF; END;");
		return res;
	}
	
	public boolean CreateUpdateTrig(String qry, String ProcName, String TblName, StringBuffer proc)
	{
		boolean res = true;
		if (proc == null){
			System.out.println("Error in CreateInsertTrig:  Input proc and ql cannot be null");
			return false;
		}
		proc.append(StartTrig+TrigProcName(ProcName,"U"));
		//proc.append(StartTrig+" U"+ProcName+" ");
		proc.append(update+TblName);
		proc.append(" FOR EACH ROW DECLARE KEYTODELETE CLOB; DeleteIT_DLL_Val BINARY_INTEGER; /* DELETEKEY CLOB; */");
		proc.append(" BEGIN ");
		proc.append(" KEYTODELETE := CONCAT('"+TblName+"', KEYTODELETE); ");
		proc.append("IF (DELETEKEY != ' ') THEN  DeleteIT_DLL_Val := KOSARTriggerDeleteMultiCall('"+QueryToTrigger.getIPport()+"', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN RAISE_APPLICATION_ERROR( -20001, 'Failed to connect to KOSAR KVS CORE.'); END IF; END IF; END;");
		return res;
	}
	
}
