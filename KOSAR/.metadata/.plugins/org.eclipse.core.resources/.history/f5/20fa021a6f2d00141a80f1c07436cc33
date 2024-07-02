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
 * @author shahram
 * Methods of this class transform a query string to a trigger.
 * As of May 17, 2011, the query must reference a single table and use conjunctive predicate.
 * Methods of this class are thread safe because no global variables are maintained.
 */
public class QueryToTrigger {
	public enum RDBMS { 
		Oracle, MySQL, PostgreSQL 
	}

	public static RDBMS TargetSystem = RDBMS.MySQL;

	public static RDBMS getTargetSystem() {
		return TargetSystem;
	}


	public static void setTargetSystem(RDBMS targetSystem) {
		TargetSystem = targetSystem;
	}


	public enum OpType {
		GETKEY, GENTRIGGER
	}
	public enum TriggerType {
		insert, delete, update
	}
	public static String select = "SELECT";
	public static String from = "FROM ";
	public static String where = "WHERE";
	public static String orderby = "ORDER BY";

	public static String groupby = "GROUP BY";
	public static String and = "AND";
	public static String InternalToken = "_";
	protected static boolean verbose = false;
	public static OracleDBAdapter dba = new OracleDBAdapter();

	private static String IPport = "";
	public static Semaphore RegIPport = new Semaphore(1, true);


	public static COSARsqlRewrite rewrite = new COSARsqlRewrite();
	private static ConcurrentHashMap<String,String> ProcName = new ConcurrentHashMap();
	public static ConcurrentHashMap<String,QTmeta> TriggerCache = new ConcurrentHashMap();
	protected static Logger logger = Logger.getLogger(QueryToTrigger.class.getName());


	protected static String[] sampleqrys = { //ToggleQrys = {
		"SELECT count(*) FROM  friendship WHERE inviteeID = 5 AND status = 1 "
		//"SELECT count(*) FROM  friendship WHERE (inviterID = 5 OR inviteeID = 5) AND status = 2"
		//"SELECT count(*) FROM  friendship WHERE (inviterID = 230 OR inviteeID = 230) AND status = 2"
		//"SELECT userid, inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address, email, tel FROM users, friendship WHERE ((inviterid = 0 AND userid = inviteeid) OR (inviteeid = 0 AND userid = inviterid)) AND status='2'"
		//"SELECT count(*) FROM  friendship WHERE (inviterID = 5 OR inviteeID = 5) AND status = 2"
		//"SELECT userid, inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address, email, tel FROM users, friendship WHERE inviteeid = 50 AND userid = inviterid AND status = 2"
		//"select INVITEEID from friendship where inviteeid='0'"
		/*"select userid from friends where userid='0' AND status='2'",
		"select userid from friends where status='2' AND friendid='0' ",
		"select receiver from messageinbox where receiver='0' AND read_flag='0' and receiverdeleted=0",
		"select userid from friends where friendid='0' AND status='1'",
		"select userid from friends where userid='0' AND status='1'",
		"select user_id from user_updates where friend_id='0' AND accepted = 3 AND expDeleteR=0",
		"select user_id from user_updates where user_id='0' AND accepted=1 AND deleteS=0",
		"select user_id from user_updates where user_id='0' AND accepted=3 AND deleteS=0 AND expDeleteS=0",
		"select friend_id from livestream where friend_id='0'",
		"select user_id from user_cameras where user_id='0' AND is_streaming='1'",
		"SELECT CHANGED FROM users WHERE user_id='0'",
		"SELECT u.user_id, u.uname FROM users u, user_cameras uc, friends f WHERE (f.userid =0 AND f.friendid = u.user_id) AND uc.is_streaming = '1' AND u.user_id = uc.user_id AND f.status = '2'",
		"SELECT u.user_id, u.uname FROM users u, user_cameras uc, friends f WHERE (f.userid = u.user_id AND f.friendid =0 ) AND uc.is_streaming = '1' AND u.user_id = uc.user_id AND f.status = '2'",
		"select e.eid, d.dno from Dept d, Emp e where d.dno=e.dno and d.dname='Dept5'", 
		"select e.eid, d.dno from Dept d, Emp e where d.dno=e.dno and d.dname='Dept1'", 
		"select count(*) from dept where dname='Dept1'",
		"select count(*) as rowcount from dept order by dno", */
		//"select * from camera_ports where camera_id=52"
		//"select COUNT(*) AS rowcount from friends where friendid='97' AND status='1'"
		//"select COUNT(*) AS rowcountcam from user_cameras where user_id='5' AND is_streaming='1'"
		//"SELECT is_streaming FROM user_cameras WHERE user_id = '52' AND camera_id = '52'"
		//"select uc.virtual_clip_id, uc.clip_id, uc.duration, uc.title, uc.thumbnail, uc.filename, uc.description, uc.block_id_start, uc.block_id_end, u.uname, uc.cdate, uc.ctime, uc.privacy_level,rv.recommended_to,rv.recommended_by from user_clips uc, users u, recommended_videos rv where rv.recommended_to=5 and u.user_id=rv.recommended_to and uc.virtual_clip_id=rv.virtual_clip_id order by rv.recommend_id DESC"
		//"select COUNT(*) AS rowcountcam from user_cameras where user_id='3' AND is_streaming='1'"
		//"SELECT * FROM resources WHERE walluserid = 5 AND rownum =5 ORDER BY rid desc"
		//"SELECT u.user_id, u.uname, status FROM users u, friends f WHERE u.user_id=f.friendid and f.userid=0 AND status=2"
		//"select * from Emp where name='Joe' and salary=30000" 
		//"select picture_content from Emp e, Dept d where e.dno = d.dno and d.dname='Toy' and e.name = 'Joe'"
		//"select count(e.eid) AS saaf from Dept d, Emp e where (d.dno=e.dno and d.dname='Dept14')"
		//"select COUNT(*) AS rowcount from user_updates where (friend_id='6' AND accepted = 3 AND expDeleteR=0) OR (user_id='6' AND accepted=1 AND deleteS=0) OR (user_id='6' AND accepted=3 AND deleteS=0 AND expDeleteS=0)"
		//"select COUNT(*) AS rowcount from friends where (userid='273' OR friendid='273') AND status='2'  "
		//"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='27' OR friendid='27' ) "
		//"select count(*) from emp"
		//"select COUNT(*) AS rowcount from user_cameras where user_id='0'"
		//"SELECT * FROM user_cameras WHERE user_id ='0'"
		//"SELECT num_streaming, URL FROM camera WHERE camera_id = 390",
		//"SELECT num_streaming FROM camera WHERE camera_id = 3"
		//" SELECT name, id FROM root.categories "
		//"SELECT count FROM root.regions WHERE name='OR--Portland'"
		//"SELECT count(*) FROM  friendship WHERE (inviterID = 55 OR inviteeID = 55) AND status = 2"
		//"select * from users u, friends f, user_cameras uc where f.status=2 and (f.userid='"+1+"' or f.friendid='"+1+"') and "+
		//               " (uc.user_id=f.friendid or uc.user_id = f.userid) and uc.is_streaming='1' and u.user_id=uc.user_id "
		//"SELECT * FROM resources WHERE walluserid = 123 AND rownum <5 ORDER BY rid desc"
		//"SELECT userid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel,pic,tpic FROM users, friendship WHERE ((inviterid=500 and userid=inviteeid) or (inviteeid=500 and userid=inviterid)) and status = 2"
		//"SELECT * FROM users, friendship WHERE ((inviterid=55 and userid=inviteeid) or (inviteeid=55 and userid=inviterid)) and status = 1"
		//"select opType,rid,min(starttime) as minTime from tupdate group by rid order by rid"
		//"SELECT root.users.id FROM root.users WHERE nickname='user687' AND password='password687'"
		//"select u.user_id, u.uname from users u, user_cameras uc, friends f where (f.userid =0 and f.friendid = u.user_id) and uc.is_streaming = '1' and u.user_id = uc.user_id and f.status = '2'"
		//"SELECT * from user_cameras u, camera c, user_updates uu WHERE u.is_streaming='1' AND u.user_id='11' AND u.camera_id=c.camera_id AND uu.user_id=u.user_id AND uu.camera_id=c.camera_id AND uu.friend_id=u.user_id"
		//"SELECT userid, inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address, email, tel FROM users, friendship WHERE inviteeid = 6 AND userid = inviterid AND status = 2",
		//"SELECT count(*) FROM  friendship WHERE (inviterID = 6 OR inviteeID = 6) AND status = 2",
		//"SELECT count(*) FROM  friendship WHERE inviteeID = 6 AND status = 1",
		//"SELECT count(*) FROM  resources WHERE wallUserID = 6",
		//"SELECT userid, inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address, email, tel FROM users, friendship WHERE ((inviterid = 0 AND userid = inviteeid) OR (inviteeid = 0 AND userid = inviterid)) AND status = 2"

	};

	public static boolean FlushQTQI(){
		Enumeration<QTmeta> eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();){
			QTmeta qtelt = e.nextElement();
			if (qtelt != null) qtelt.FlushQT();
		}
		return true;
	}


	public static void UpdateTriggerCache(HashMap<String,String> disabledqts){		
		Enumeration<QTmeta> eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();){
			QTmeta qtelt = e.nextElement();
			if (qtelt == null) System.out.println("Error:  qtelt is NULL");
			else if ( disabledqts.get( qtelt.getQueryTemplate() ) != null ){
				if (verbose) System.out.println("\n\n\tProcessing "+qtelt.getQueryTemplate());
				qtelt.setSwitchButton(false);
			} else qtelt.setSwitchButton(true);
		}
	}

	public static void SetIPport(String address){
		try {
			RegIPport.acquire();
		} catch (InterruptedException e) {
			System.out.println("Failed to obtain the semaphore in SetIPport");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (IPport.length()==0)
			IPport += address;
		else IPport += ","+address;
		RegIPport.release();
	}

	public static String getIPport(){
		String ports = "";
		try {
			RegIPport.acquire();
		} catch (InterruptedException e) {
			System.out.println("Failed to obtain the semaphore in SetIPport");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ports = IPport;
		RegIPport.release();
		return ports;
	}

	public static void delIPport(String addr){

	}

	public static boolean IsItTrigger(String cmd, Connection db_conn){
		OracleTrigGenerator og = new OracleTrigGenerator(db_conn);
		return og.IsItTrigger(cmd);
	}

	public static QueryToTrigger.TriggerType WhatIsTriggerType(String cmd, Connection db_conn){
		OracleTrigGenerator og = new OracleTrigGenerator(db_conn);
		return og.WhatIsTriggerType(cmd);
	}

	public static String TableName(String cmd, QueryToTrigger.TriggerType ttype, Connection db_conn){
		OracleTrigGenerator og = new OracleTrigGenerator(db_conn);
		return og.TableName(cmd, ttype);
	}

	/*
	 * CorrectSyntax does two things: 
	 * First, it extracts the select, from and where clause of the SQL command.
	 * Second, it checks to make sure the where clause does not contains OR, NOT, LIKE.
	 */
	public int OLDCorrectSyntax (String query, StringBuffer SelectClause, StringBuffer FromClause, StringBuffer WhereClause)
	{
		int NumANDS = 0;
		if (SelectClause == null || FromClause == null || WhereClause == null){
			System.out.println("Error (CorrectSyntax method of QueryToTrigger class):  Input parameters cannot be null.  They must be instantiated by the caller");
			return -1;
		}

		StringBuffer revQ = new StringBuffer( query.toUpperCase() );

		int idxS, idxF, idxW;
		idxS = revQ.indexOf(select);
		idxF = revQ.indexOf(from);
		idxW = revQ.indexOf(where);
		if (idxW > 0){
			if ( idxS < 0 || idxF < 0 || idxW < 0 || idxF < idxS || idxW < idxS || idxW < idxF) return -1;
			FromClause.append( query.substring(idxF+from.length(), idxW).trim() );
		} else { 
			if ( idxS < 0 || idxF < 0 || idxF < idxS) return -1;
			FromClause.append( query.substring(idxF+from.length(), revQ.length()).trim() );
		}


		SelectClause.append( query.substring(idxS+select.length(), idxF).trim() );

		if (idxW > 0){
			WhereClause.append( query.substring(idxW+where.length(), revQ.length()).trim().replace("(", "").replace(")","") );
			//WhereClause.append( query.substring(idxW+where.length(), revQ.length()).trim() );


			String wherecl = revQ.substring(idxW+where.length(), revQ.length()).trim();

			if (wherecl.indexOf(" OR ") >= 0 || wherecl.indexOf(" NOT ") >= 0 || wherecl.indexOf(" LIKE ") >= 0) return -1;


			boolean done = false;
			do{
				if (wherecl.indexOf(" AND ") >= 0){
					NumANDS++;
					wherecl = wherecl.substring(wherecl.indexOf(" AND ")+and.length());
				} else done = true;	
			} while (!done);
		}

		return NumANDS;
	}

	/*
	 * CorrectSyntax does two things: 
	 * First, it extracts the select, from and where clause of the SQL command.
	 * Second, it checks to make sure the where clause does not contains OR, NOT, LIKE.
	 */
	public int CorrectSyntax (String query, StringBuffer SelectClause, StringBuffer FromClause, StringBuffer WhereClause, StringBuffer OrderByClause)
	{
		int NumANDS = 0;
		if (SelectClause == null || FromClause == null || WhereClause == null || OrderByClause == null){
			System.out.println("Error (CorrectSyntax method of QueryToTrigger class):  Input parameters cannot be null.  They must be instantiated by the caller");
			return -1;
		}

		StringBuffer revQ = new StringBuffer( query.toUpperCase() );

		int idxS, idxF, idxW, idxq, idxO;
		idxS = revQ.indexOf(select);
		idxF = revQ.indexOf(from);
		idxW = revQ.indexOf(where);
		idxq = revQ.indexOf(groupby);
		idxO = revQ.indexOf(orderby);

		if (idxO < 0) idxO = revQ.length(); //needed to compute the where clause
		else {
			OrderByClause.append( query.substring(idxO+orderby.length(), revQ.length()).trim() );
		}

		if (idxW > 0){
			if ( idxS < 0 || idxF < 0 || idxW < 0 || idxF < idxS || idxW < idxS || idxW < idxF) return -1;
			FromClause.append( query.substring(idxF+from.length(), idxW).trim() );
		} else { 
			if ( idxS < 0 || idxF < 0 || idxF < idxS) return -1;
			if (idxq >=0) FromClause.append( query.substring(idxF+from.length(), idxq).trim() );
			else if (idxO >= 0) FromClause.append( query.substring(idxF+from.length(), idxO).trim() );
			else FromClause.append( query.substring(idxF+from.length()).trim() );
		}


		SelectClause.append( query.substring(idxS+select.length(), idxF).trim() );

		if (idxW > 0){
			String wp = query.substring(idxW+where.length(), idxO).trim().replace("(", " ").replace(")"," ");
			String[] wp2 = wp.toUpperCase().split(" AND ");
			String[] preds = new String[wp2.length];
			int ioffset = 0;
			for (int i = 0; i < wp2.length; i++){
				int idx = wp.toUpperCase().indexOf(" AND ");
				if (idx > 0) {
					preds[i] = wp.substring(0, idx);
					wp = wp.substring(idx+" AND ".length());
				} else {
					preds[i] = wp;  //only one token exists
				}
			}
			//String[] preds = query.substring(idxW+where.length(), idxO).trim().replace("(", "").replace(")","").toUpperCase().split(" AND ");

			NumANDS = preds.length - 1;

			Vector<String> sp = new Vector<String>();
			for (int i = 0; i < preds.length; i++)
			{
				if (sp.size() == 0) sp.add(preds[i]);
				else {
					int j;
					for (j = 0; j < sp.size(); j++)
						if (sp.elementAt(j).compareTo(preds[i]) >= 0) break;
					sp.add(j, preds[i]);
				}
			}

			for (int i = 0; i < sp.size(); i++){
				if (i > 0) WhereClause.append(" AND ");
				WhereClause.append(sp.elementAt(i));
			}
			//WhereClause.append( query.substring(idxW+where.length(), revQ.length()).trim().replace("(", "").replace(")","") );
			//WhereClause.append( query.substring(idxW+where.length(), revQ.length()).trim() );


			//			String wherecl = revQ.substring(idxW+where.length(), revQ.length()).trim();
			//		
			//			if (wherecl.indexOf(" OR ") >= 0 || wherecl.indexOf(" NOT ") >= 0 || wherecl.indexOf(" LIKE ") >= 0) return -1;
			//
			//
			//			boolean done = false;
			//			do{
			//				if (wherecl.indexOf(" AND ") >= 0){
			//					NumANDS++;
			//					wherecl = wherecl.substring(wherecl.indexOf(" AND ")+and.length());
			//				} else done = true;	
			//			} while (!done);
		}

		return NumANDS;
	}

	public boolean RefOneTable(String FromClause, StringBuffer TblName)
	{
		if (TblName == null){
			System.out.println("Error (RefOneTable method of QueryToTrigger class):  Input parameters cannot be null.  They must be instantiated by the caller");
			return false;
		}

		String frm = FromClause.trim();

		if (frm.indexOf(",") == frm.length()-1)
			frm = frm.substring(0, frm.length()-1);

		if (frm.indexOf(",") > 0) return false;
		if (frm.indexOf(" ") > 0)
			frm = frm.substring(0,frm.indexOf(" "));

		TblName.append( frm.trim().toUpperCase() );
		return true;
	}

	public boolean ProjAttrs (String wcl, Vector<String> proj) {
		int pidx = 0;
		String tkn;
		int endidx = wcl.indexOf(",");
		if (endidx < 0) {
			pidx = wcl.lastIndexOf(".");
			if (pidx < 0 && wcl.trim().length() > 0) proj.add(wcl.trim());
			else if (pidx >= 0) proj.add(wcl.substring(pidx+1,wcl.length()));
		}
		else {
			String tokens[] = wcl.split(",");
			for (int i=0; i < tokens.length; i++){
				//do {
				tkn = tokens[i]; //wcl.substring(0,endidx).trim();
				pidx = tkn.lastIndexOf(".");
				if (pidx < 0) proj.add(tkn);
				else proj.add(tkn.substring(pidx+1,tkn.length()));

				//wcl = wcl.substring(endidx+1).trim();
				//endidx = wcl.indexOf(",");
				//} while (endidx >= 0);
			}
		}

		return true;
	}

	//If this method returns false then it means the query is not supported because it has more than one tuple variable
	public boolean GetTupleVars (String FromClause, Vector<String> trg){
		boolean result = true;
		//Comma seperate the From Clause
		String[] felt = FromClause.split(",");

		for (int i=0; i < felt.length; i++){
			int idx = felt[i].trim().indexOf(" ");
			if (idx > 0){
				trg.addElement ( felt[i].trim().substring(idx).trim() );
			}
		}

		//Currently, more than one tuple variables are not supported
		if (trg.size() > 1) return false;

		return result;
	}

	public boolean AttrsVals (String wcl, Vector<String> attrs, Vector<String> vals, int ANDs, String FromClause) {
		/*
		 * Iterate the list, consuming attr, comparison operator, value
		 * To do: 
		 * 1.  Extend to include other comparison operators.
		 * 2.  Sort the attribute list based on elements.
		 * 3.  Remove the assumption that the from caluse must refernece a single tuple variable
		 */
		int sidx = 0;
		int endidx = wcl.indexOf("AND");
		if (endidx == -1 && wcl.length()==0) return false; //No select clause

		//Obtain the tuple variables
		Vector<String> trg = new Vector<String>();
		boolean supported = GetTupleVars(FromClause, trg);

		//Remove the tuple variable from the where clause
		for (int i=0; i < trg.size(); i++){
			wcl=wcl.replace(trg.elementAt(i)+".", "");
		}

		do {
			endidx = wcl.indexOf("=");
			attrs.add(wcl.substring(0,endidx).trim());
			sidx = endidx + 1;
			wcl = wcl.substring(sidx).trim();
			endidx = wcl.indexOf(" ");
			if (endidx < 0)
				endidx = wcl.length();  //This is end of the token
			vals.add(wcl.substring(0,endidx).replaceAll("'","").trim());
			ANDs--;
			if (ANDs >= 0)
			{
				sidx = endidx + 1;
				wcl = wcl.substring(sidx).trim();
				wcl = wcl.substring(and.length()).trim();
			}
		} while (ANDs >= 0);

		return true;
	}



	public boolean InternalKey(Vector<String> proj, Vector<String> vals, StringBuffer key) {
		if (proj == null || vals==null || key == null) {
			System.out.println("Error (InternalKey):  Input key is null; this is not possible.");
			return false;
			//System.exit(-1);
		}
		if (verbose) System.out.println("size of proj is "+proj.size());
		if (proj.size() == 0) return false;

		key.append(""+InternalToken);
		//for (int i = 0; i < proj.size(); i++) 
		key.append(OracleDBAdapter.TokenizeAttr(proj, vals)); //key.append(proj.elementAt(i).toString().length());
		for (int i = 0; i < vals.size(); i++) key.append(vals.elementAt(i).toString());
		return true;
	}



	public String GetOperator(String cls){
		String results = null;
		if (cls.contains("<=")) return ("<=");
		else if (cls.contains(">=")) return (">=");
		else if (cls.contains("!=")) return ("!=");
		else if (cls.contains("^=")) return ("^=");
		else if (cls.contains("<>")) return ("<>");
		else if (cls.contains("=")) return ("=");
		else if (cls.contains("<")) return ("<");
		else if (cls.contains(">")) return (">");
		return results;
	}

	public String ExtractLiteral(String cls){
		String results = null;
		int start, end;
		if ( ( start=cls.indexOf("'") ) > 0) {
			end = cls.substring(start+1).indexOf("'");
			results = cls.substring(start+1, start+1+end);
		}
		return results;
	}

	public QNode ProcessSubClause(String sc, String[] tbls, String[] tups, Connection DBcon)
	{
		int start, end;
		QNode results = null;
		String targetAttr = null;
		String targetTVar = null;
		Vector clmn;

		results = new QNode();

		if ((start = sc.indexOf(".")) > 0) {
			//this is an attribute reference using either a tuple variable or a relation variable
			targetTVar = sc.substring(0, start);
			targetAttr = sc.substring(start+1, sc.length());

			results.SetParamAttr(targetAttr);
			for (int i = 0 ; i < tups.length; i++) {
				if (tups[i].equals(targetTVar) || tups[i].equals(targetTVar.toUpperCase() )) results.SetTBLname(tbls[i]);
			}
			if (results.GetTBLname() == null) results.SetTBLname(targetTVar);
		} else {
			//This might be an attribute or a literal.  If an attribute then the table property will be set; otherwise, it will be a null.
			targetAttr = sc.trim().toUpperCase();
			results.SetParamAttr(targetAttr);
			for (int i = 0; i < tbls.length; i++){
				clmn = dba.GetColumnNames(DBcon, tbls[i]);
				for (int j = 0; j < clmn.size(); j++){
					if ( clmn.elementAt(j).toString().equals(targetAttr) ) {
						results.SetTBLname(tbls[i]);
						i = tbls.length+1;
						j = clmn.size()+1;
					}
				}
			}
		}

		return results;
	}

	public int LogicalWhereClause(QualificationList ql, Vector qtpl, StringBuffer FromClause, StringBuffer WhereClause, Connection db_conn)
	{
		boolean results = true;
		int start, end;

		/*
		 * Use the FROM clause to obtain referenced table names and tuple variables
		 */
		String[] TargetTbls = FromClause.toString().toUpperCase().split(",");
		String[] Tbls = new String[TargetTbls.length];
		String[] TupVars = new String[TargetTbls.length];

		for (int i = 0; i < TargetTbls.length; i++) {
			TargetTbls[i]=TargetTbls[i].trim();
			if (TargetTbls[i].indexOf(" ") > 0)
			{
				Tbls[i] = TargetTbls[i].substring(0,TargetTbls[i].indexOf(" "));
				TupVars[i] = TargetTbls[i].substring(TargetTbls[i].indexOf(" ")+1, TargetTbls[i].length());
			} else {
				Tbls[i] = TargetTbls[i].trim();
				TupVars[i]=null;
			}
			QTablePredList qtp = new QTablePredList();
			qtp.SetTableName(Tbls[i]);
			qtpl.addElement(qtp);
		}

		//Maintain the list of tables referenced by this query in the qualification list
		ql.SetTblList(Tbls);


		/*
		 * Process the WHERE clause to construct the selection and join predicates
		 * ASSUMPTION:  Support AND clauses only
		 */
		String WC1 = WhereClause.toString().replace("(", " ");  //By now, the and has been capitalized.
		String WC = WC1.replace(")", " ");
		String Clauses[] = WC.split(" AND ");
		String literal;
		String Clausei, Opi;
		QNode qn1=null;
		QNode qn2=null;
		QCmpOp qc=null;

		for (int i = 0; i < Clauses.length; i++) {
			Clausei = Clauses[i].trim();
			if ( (literal = ExtractLiteral(Clauses[i])) != null){
				qn1 = new QNode();
				qn1.SetParamAttr(literal);
				Clausei = Clausei.replaceFirst("'"+literal+"'","");
			}
			qc = new QCmpOp();
			qc.SetPredicate(Clausei);
			qc.SetCmpOpType( Opi = GetOperator(Clausei) );

			if (Opi == null){
				System.out.println("Error in LogicalWhereClause method of QueryToTrigger:  Failed to parse query.");
				System.out.println("\t From clause: "+FromClause.toString());
				System.out.println("\t Where clause: "+WhereClause.toString());
				return -1; //false;
			}

			if (qn1 != null) {
				//This is a select clause:  comparison of an indexed tuple variable with a constant
				Clausei = Clausei.replaceFirst(Opi,"").trim();
				qn2 = ProcessSubClause(Clausei, Tbls, TupVars, db_conn);
				qc.SetLeftNode(qn1);
				qc.SetRightNode(qn2);
				qc.SetPred(QCmpOp.PredType.SELECT);
			} else {
				//This is a join clause:  comparison of two indexed tuple variables.
				start = Clausei.indexOf(Opi);
				end = start+Opi.length();

				String left, right;
				left = Clausei.substring(0,start).trim();
				right = Clausei.substring(end, Clausei.length()).trim();

				qn1 = ProcessSubClause(left, Tbls, TupVars, db_conn);
				qn2 = ProcessSubClause(right, Tbls, TupVars, db_conn);
				if (qn1.GetTBLname() == null || qn2.GetTBLname() == null) qc.SetPred(QCmpOp.PredType.SELECT);
				else qc.SetPred(QCmpOp.PredType.JOIN);

				qc.SetLeftNode(qn1);
				qc.SetRightNode(qn2);
			}

			//Set the table referenced by this select operator
			if (qc.GetPredType() == QCmpOp.PredType.SELECT) {
				if (qc.GetLeftNode().GetTBLname() != null) qc.SetTblName(qc.GetLeftNode().GetTBLname());
				if (qc.GetRightNode().GetTBLname() != null) qc.SetTblName(qc.GetRightNode().GetTBLname());
			}


			QBooleanOp tq = null;
			if ( (tq=ql.GetBoolOp()) == null) {
				//Very first iteration of this loop
				QBooleanOp qb = new QBooleanOp();
				qb.SetBoolOp(QBooleanOp.BoolOpType.AND);
				qb.SetRightCmpOp(qc);
				ql.SetBoolOp(qb);
			} else {
				if (tq.GetRightCmpOp() == null) tq.SetRightCmpOp(qc);
				else if (tq.GetLeftCmpOp() == null) tq.SetLeftCmpOp(qc);
				else {
					//Brand new node
					QBooleanOp qb = new QBooleanOp();
					qb.SetBoolOp(QBooleanOp.BoolOpType.AND);
					qb.SetRightCmpOp(qc);
					qb.SetChild( ql.GetBoolOp() );
					ql.GetBoolOp().SetParent(qb);
					ql.SetBoolOp(qb);
				}
			}

			qn1=qn2=null;
			qc=null;
		}

		return Clauses.length;
	}

	public String OneTablesReferencedByAllSelectOps(QualificationList ql)
	{
		String results = null;
		if (ql == null) return null;
		QBooleanOp qb = ql.GetBoolOp();
		String Tablename = null;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().GetPredType() == QCmpOp.PredType.SELECT) {
				results = qb.GetLeftCmpOp().GetTblName();
				if (Tablename == null) Tablename = results;
				else if (! Tablename.equals(results) ) return null;
			}
			if (qb.GetRightCmpOp() !=null && qb.GetRightCmpOp().GetPredType() == QCmpOp.PredType.SELECT) {
				results = qb.GetRightCmpOp().GetTblName();
				if (Tablename == null) Tablename = results;
				else if (! Tablename.equals(results) ) return null;
			}
			qb = qb.GetChild();
		}
		return results;
	}

	public StringBuffer TablesReferenced(QCmpOp cmp)
	{
		StringBuffer results = new StringBuffer();
		if (cmp == null) return null;
		switch (cmp.GetPredType()) {
		case SELECT: 
			results.append(""+cmp.GetTblName());
			break;
		case JOIN: 
			results.append(""+cmp.GetLeftNode().GetTBLname()+",");
			results.append(""+cmp.GetRightNode().GetTBLname());
			break;
		default: break;
		}
		return results;
	}

	public boolean IsTableReferenced(QualificationList ql, String TblName)
	{
		boolean results = false;
		StringBuffer TblsRef = new StringBuffer();
		if (ql == null) return false;
		QBooleanOp qb = ql.GetBoolOp();
		while (qb != null){

			qb = qb.GetChild();
		}
		return results;
	}

	public String GenCurosorQry(QualificationList ql, String TBLname, StringBuffer qry, StringBuffer InputArgs, Vector VarArgs, Vector<StringBuffer> InArgs, boolean AllSelectClausesReferenceOneTable){
		String results = null;
		if (ql == null) return null;
		if (qry == null || InputArgs == null) return null;
		StringBuffer targetlist = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		Vector T = new Vector();
		Vector A = new Vector();

		//Compute the target list of the cursor
		QNode qn = null;
		int i = 1;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.SELECT){
				T.addElement(qb.GetLeftCmpOp().GetTblName());
				A.addElement(qb.GetLeftCmpOp().GetSelectAttrName());
			}
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.SELECT){
				T.addElement(qb.GetRightCmpOp().GetTblName());
				A.addElement(qb.GetRightCmpOp().GetSelectAttrName());
			}
			qb=qb.GetChild();
		}

		for (int j = 0; j < T.size(); j++)
		{
			if (T.elementAt(j).toString().equals(TBLname)){
				StringBuffer token = new StringBuffer();
				token.append(""+A.elementAt(j).toString());
				int argid = DuplicateFreeInsertArgs(token, InArgs);
				targetlist.append(" InArg"+argid);
			} else {
				targetlist.append(T.elementAt(j).toString()+"."+A.elementAt(j));
			}
			VarArgs.addElement(" "+T.elementAt(j).toString()+"."+A.elementAt(j)+"%TYPE");
			//VarArgs.append("var"+(j+1)+" "+T.elementAt(j).toString()+"."+A.elementAt(j)+"%TYPE");
			if (j < T.size()-1)targetlist.append(", ");
			//if (j < T.size()-1)VarArgs.append(", ");
		}

		//Compute the tuple variable list of the cursor
		StringBuffer tupVar = new StringBuffer();
		int lgt = ql.GetTblList().length;
		for (int j=0; j < lgt; j++)
		{
			if (! TBLname.equals(ql.GetTblList()[j]) ){
				if (tupVar.length()>0) tupVar.append(", ");
				tupVar.append(ql.GetTblList()[j]);
			}
		}

		//Compute the qualification list of the cursor

		StringBuffer qualList = new StringBuffer();
		qb = ql.GetBoolOp();
		qn = null;
		i = 1;
		//		if (AllSelectClausesReferenceOneTable) {
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.JOIN){
				qn = qb.GetLeftCmpOp().WhichNodeRefsTable(TBLname);
				if (qn != null){	
					if (qualList.length() > 0) qualList.append(" AND ");
					if (!GenAutoPred(qb.GetLeftCmpOp(),qn,qualList,InArgs))
						System.out.println("Error in GenCurosorQry, failed to create qualification list.");
				} else {
					if (qualList.length() > 0) qualList.append(" AND ");
					qb.GetLeftCmpOp().GiveJoinPredicate(qualList);
				}
			}

			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.JOIN){
				qn = qb.GetRightCmpOp().WhichNodeRefsTable(TBLname);
				if (qn != null){	
					if (qualList.length() > 0) qualList.append(" AND ");
					if (!GenAutoPred(qb.GetRightCmpOp(),qn,qualList,InArgs)) 
						System.out.println("Error in GenCurosorQry, failed to create qualification list.");
				} else {
					if (qualList.length() > 0) qualList.append(" AND ");
					qb.GetRightCmpOp().GiveJoinPredicate(qualList);
				}
			}
			qb=qb.GetChild();
		}
		//		} else {
		//			while (qb != null){
		//				if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.JOIN){
		//					qn = qb.GetLeftCmpOp().WhichNodeRefsTable(TBLname);
		//					if (qn != null){	
		//						if (qualList.length() > 0) qualList.append(" AND ");
		//						if (!GenAutoPred(qb.GetLeftCmpOp(),qn,qualList,InArgs))
		//							System.out.println("Error in GenCurosorQry, failed to create qualification list.");
		//					} 
		//					if (qualList.length() > 0) qualList.append(" AND ");
		//					qb.GetLeftCmpOp().GiveJoinPredicate(qualList);
		//					
		//				}
		//
		//				if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.JOIN){
		//					qn = qb.GetRightCmpOp().WhichNodeRefsTable(TBLname);
		//					if (qn != null){	
		//						if (qualList.length() > 0) qualList.append(" AND ");
		//						if (!GenAutoPred(qb.GetRightCmpOp(),qn,qualList,InArgs)) 
		//							System.out.println("Error in GenCurosorQry, failed to create qualification list.");
		//					}
		//					if (qualList.length() > 0) qualList.append(" AND ");
		//					qb.GetRightCmpOp().GiveJoinPredicate(qualList);	
		//				}
		//				qb = qb.GetChild();
		//			}
		//		}

		//Generate query for the cursor
		qry.append("SELECT "+targetlist+" ");
		qry.append("FROM "+tupVar.toString()+" ");
		qry.append("WHERE "+qualList.toString() );
		if (verbose) System.out.println("Qry: "+qry);

		//Generate the input arguments
		for (int k=0; k< InArgs.size(); k++){
			InputArgs.append("InArg"+(k+1)+" IN "+TBLname+"."+InArgs.elementAt(k).toString()+"%TYPE" );
			if (k < InArgs.size()-1) InputArgs.append(", ");
		}
		if (verbose) System.out.println("Input Args: "+InputArgs.toString());
		return results;
	}

	public int DuplicateFreeInsertArgs(StringBuffer token, Vector<StringBuffer> Args){		
		int argid = -1;
		for (int i=0; i < Args.size(); i++){
			if ( Args.elementAt(i).toString().equals(token.toString()) ) argid = i;
		}

		if (argid == -1){
			Args.add(token);
			argid = Args.size();
		} else argid+=1;

		return argid;
	}

	public boolean GenAutoPred(QCmpOp qb, QNode qn, StringBuffer qualList, Vector<StringBuffer> Args){
		boolean results = true;
		QNode tnode = null;
		if (qualList == null || qn == null) return false;

		if (qb.GetRightNode() == qn) tnode = qb.GetLeftNode();
		else tnode = qb.GetRightNode();

		StringBuffer token = new StringBuffer();
		token.append(""+qn.GetParamAttr());

		int argid = DuplicateFreeInsertArgs(token, Args);

		qualList.append(""+tnode.GetTBLname()+"."+tnode.GetParamAttr()+""+qb.GetCmpOpType()+"InArg"+argid);
		return results;
	}



	public void JoinGenInputArgs(QualificationList ql, String TBLname, Vector Arg){
		String results = null;
		if (ql == null) return ;
		//if (res == null) return -1;
		//StringBuffer res = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		QNode qn = null;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.JOIN){
				qn = qb.GetLeftCmpOp().WhichNodeRefsTable(TBLname);
				if (qn != null) {
					Arg.add( (""+qn.GetTBLname()+"."+qn.GetParamAttr()) );
					//					if (res.length() != 0) res.append(", ");
					//					res.append("arg"+i+" IN "+qn.GetTBLname()+"."+qn.GetParamAttr()+"%TYPE");
					//					i++;
				}
			}
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.JOIN){
				qn = qb.GetRightCmpOp().WhichNodeRefsTable(TBLname);
				if (qn != null){
					Arg.add( (""+qn.GetTBLname()+"."+qn.GetParamAttr()) );
					//					if (res.length() != 0) res.append(", ");
					//					res.append("arg"+i+" IN "+qn.GetTBLname()+"."+qn.GetParamAttr()+"%TYPE");
					//					i++;
				}
			}
			qb=qb.GetChild();
		}
		return ;
	}

	public int GenInputArgs(QualificationList ql, StringBuffer res){
		String results = null;
		if (ql == null) return -1;
		if (res == null) return -1;
		//StringBuffer res = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		int i = 1;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.SELECT){
				if (res.length() != 0) res.append(", ");
				res.append("arg"+i+" IN "+qb.GetLeftCmpOp().GetSelectTblName()+"."+qb.GetLeftCmpOp().GetSelectAttrName()+"%TYPE");
				i++;
			}
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.SELECT){
				if (res.length() != 0) res.append(", ");
				res.append("arg"+i+" IN "+qb.GetRightCmpOp().GetSelectTblName()+"."+qb.GetRightCmpOp().GetSelectAttrName()+"%TYPE");
				i++;
			}
			qb=qb.GetChild();
		}
		return i-1;
	}

	public int CallInputArgs(String token, QualificationList ql, StringBuffer res){
		String results = null;
		if (ql == null) return -1;
		if (res == null) return -1;
		//StringBuffer res = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		int i = 1;
		String currentElt;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.SELECT){
				currentElt = ""+token+"."+qb.GetLeftCmpOp().GetSelectAttrName();
				//Do not produce duplicates
				if (res.toString().indexOf(currentElt) < 0){
					if (res.length() != 0) res.append(", ");
					res.append(""+token+"."+qb.GetLeftCmpOp().GetSelectAttrName());
					i++;
				}
			}
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.SELECT){
				currentElt = ""+token+"."+qb.GetRightCmpOp().GetSelectAttrName();
				if (res.toString().indexOf(currentElt)<0){
					if (res.length() != 0) res.append(", ");
					res.append(""+token+"."+qb.GetRightCmpOp().GetSelectAttrName());
					i++;
				}
			}
			qb=qb.GetChild();
		}
		return i-1;
	}

	public int CallInputArgsJoin(String token, QualificationList ql, StringBuffer res, String TblName){
		StringBuffer qualList = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		QNode qn = null;
		String Elt;
		int cntr=1;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.JOIN){
				qn = qb.GetLeftCmpOp().WhichNodeRefsTable(TblName);
				if (qn != null){
					Elt = ""+token+"."+qn.GetParamAttr();
					//Duplicate elimination
					if (res.toString().indexOf(Elt)<0){
						if (res.length() != 0) res.append(", ");
						res.append(Elt);
					}
				}
				cntr++;
			}

			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.JOIN){
				qn = qb.GetRightCmpOp().WhichNodeRefsTable(TblName);
				if (qn != null){	
					Elt = ""+token+"."+qn.GetParamAttr();
					//Duplicate elimination
					if (res.toString().indexOf(Elt)<0){
						if (res.length() != 0) res.append(", ");
						res.append(""+token+"."+qn.GetParamAttr());
					}
				}
				cntr++;
			}
			qb=qb.GetChild();
		}
		return cntr-1;
	}

	public String GenJoinPredStrings(QualificationList ql){
		String results = null;
		if (ql == null) return null;
		StringBuffer res = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.JOIN)
				res.append(qb.GetLeftCmpOp().GetPredicate().replace(" ", ""));
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.JOIN)
				res.append(qb.GetRightCmpOp().GetPredicate().replace(" ", ""));
			qb=qb.GetChild();
		}
		return res.toString();
	}

	public String GenSelPredStrings(QualificationList ql){
		String results = null;
		if (ql == null) return null;
		StringBuffer res = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.SELECT)
				res.append(qb.GetLeftCmpOp().GetSelectLiteral().trim());
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.SELECT)
				res.append(qb.GetRightCmpOp().GetSelectLiteral().trim());
			qb=qb.GetChild();
		}
		return res.toString();
	}

	public int NumJoinPreds(QualificationList ql){
		String results = null;
		if (ql == null) return 0;
		StringBuffer res = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		int i=0;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.JOIN)
				i++;
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.JOIN)
				i++;
			qb=qb.GetChild();
		}
		return i;
	}

	public int NumSelPreds(QualificationList ql){
		String results = null;
		if (ql == null) return 0;
		StringBuffer res = new StringBuffer();
		QBooleanOp qb = ql.GetBoolOp();
		int i=0;
		while (qb != null){
			if (qb.GetLeftCmpOp() != null && qb.GetLeftCmpOp().PredKind == QCmpOp.PredType.SELECT)
				i++;
			if (qb.GetRightCmpOp() != null && qb.GetRightCmpOp().PredKind == QCmpOp.PredType.SELECT)
				i++;
			qb=qb.GetChild();
		}
		return i;
	}

	public String GenerateInternalKey(QualificationList ql, int NumClausesInWhere){
		String results = "";
		String jr = GenJoinPredStrings(ql);
		String sr = GenSelPredStrings(ql);
		if (sr == null && jr == null) return null;
		if (sr != null) results += sr;
		if (jr != null){
			results += jr;
			results += ""+NumClausesInWhere;
		}
		return results;
	}

	public String ProcTrigName(String TargetTable, int NumProj, int NumJoin, int TargetListLength, int QualListLength, String qry){
		String name = ""+TargetTable+""+NumProj+""+NumJoin+""+TargetListLength+""+QualListLength;
		String newname = name;
		int counter=0;
		boolean done = false;
		while (! done) {
			if (ProcName.get(newname) == null) done = true;
			else if (ProcName.get(newname) != qry){
				counter++;
				newname = name + counter;
			} else done = true;
		}
		ProcName.put(newname, qry);
		return newname;
	}


	public String TokenizeWhereClause(String query){
		StringBuffer revQ = new StringBuffer( query.toUpperCase() );
		boolean done = false;
		int idxW;

		idxW = revQ.indexOf(where);
		if (idxW < 0) return null;

		String hdr = query.substring(0, idxW);
		String w = query.substring(idxW + where.length());

		//Obtain tuple variables

		int pred = w.indexOf("'");
		String output = "";
		if (pred >= 0){
			done = false;
			String tmp = w;
			while (!done) {
				int sidx = tmp.indexOf("'");
				if (sidx > 0) output += tmp.substring(0, sidx)+" ? ";
				int eidx = tmp.substring(sidx+1).indexOf("'");
				if (sidx < 0 && eidx < 0) {
					output += tmp.substring(sidx+eidx+2).trim();
					done = true;
				} else tmp = tmp.substring(sidx+eidx+2).trim();
				if (tmp.length() == 0 ) done = true;
			}
		}
		if (output.length() == 0 && w.length() >= 0) output = w.trim();

		//Identify the index of tuple variables and remove them from the equation
		//Generate the question mark
		//Replace each unique string back to a tuple variable

		for (int i=0; i < 10; i++)
			output = output.replaceAll(""+i,"?");

		while (output.indexOf("??") >= 0)
			output = output.replace("??","?");

		return hdr+" where "+output;
	}

	public boolean GetKey(String qry, StringBuffer COSARKey, Connection db_conn) {
		int ANDs = 0;
		int TargetListLength=0;
		int TupVarListLength=0;


		if ( IsQuerySupported(qry) == false ) return false;
		Set<String> Q = null;
		Q = com.mitrallc.sqltrig.rewriter.QueryRewrite.rewriteQuery(qry);		

		if (Q.size() == 0) Q.add(qry);

		for (String eachqry : Q) {
			Vector<String> qrys = new Vector<String>();
			if (!rewrite.RewriteQuery(eachqry, qrys, db_conn)) return false;

			for (int i=0; i < qrys.size(); i++){
				String newqry = qrys.elementAt(i);

				QualificationList ql = new QualificationList();
				StringBuffer SelectClause = new StringBuffer();
				StringBuffer FromClause = new StringBuffer();
				StringBuffer WhereClause = new StringBuffer();
				StringBuffer OrderByClause = new StringBuffer();

				if ((ANDs=CorrectSyntax (newqry, SelectClause, FromClause, WhereClause, OrderByClause)) < 0) return false;

				TargetListLength = SelectClause.toString().length();
				TupVarListLength = FromClause.toString().length();

				if (SelectClause == null || FromClause == null){
					System.out.println("Error, query is not supported "+newqry);
					return false;
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
					}
					else InternalKey(proj, vals, COSARKey);
				} else {
					Vector QTPList = new Vector();
					int ClausesInWhere = LogicalWhereClause(ql, QTPList, FromClause, WhereClause, db_conn);
					if ( ClausesInWhere < 0  ) return false;

					COSARKey.append(GenerateInternalKey(ql,ClausesInWhere));
				}
				COSARKey.append(" ");
			}
		}
		return true;
	}


	public static boolean IsQuerySupported(String query){
		if (! query.toUpperCase().contains("SELECT")) return false;  //not supported
		if (! query.toUpperCase().contains("FROM")) return false;    //not supported
		if (query.contains(">") || query.contains("<") || query.contains("!=")) return false;
		return true;
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
		if (tg != null){
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
			//			if (ITs.size() < triggers.size()){
			//				int delta = triggers.size()-ITs.size();
			//				String elt = ITs.elementAt(0);
			//				for (int i=0; i < delta; i++)
			//					ITs.addElement(elt);
			//			}
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

		//		logger.debug("Qry: "+query);
		//		for (int i=0; i < trgrVector.size(); i++)
		//			logger.debug("\t"+trgrVector.elementAt(i).toString());
		//		for (int i=0; i < CK.size(); i++)
		//			logger.debug("\t"+CK.elementAt(i));
		//		logger.debug("\n");

		return celt;
	}
}
