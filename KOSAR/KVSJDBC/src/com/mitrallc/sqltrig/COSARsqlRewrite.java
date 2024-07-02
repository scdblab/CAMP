package com.mitrallc.sqltrig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

//import oracle.jdbc.OracleDatabaseMetaData;
import com.mitrallc.config.DBConnector;

public class COSARsqlRewrite {
	private static QueryToTrigger qtt = null;
	private static String as = " AS ";

	public static OracleDBAdapter dba = new OracleDBAdapter();
	private static int MAX_PROJECTION_LIST = 10;
	
	private static boolean verbose = false;

//	static String[] sampleqrys = {
//		"select * from users u, friends f where u.user_id=f.friendid",
//		"select * from users u, friends f where u.user_id=f.friendid and status='2'",
//		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='5' OR friendid='5' )",
//		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='5' OR friendid='5' )",
//
//		"select user_id from users where name='sally' order by salary",
//		"select max(user_id) from users",
//		"select COUNT(*) AS rowcount from messageinbox where receiver='5' AND read_flag='0' and receiverdeleted=0",
//		"select COUNT(*) AS rowcount from friends where (friendid='5' OR userid='7') AND status='1'",
//		"select COUNT(*) AS rowcount from user_updates where (friend_id='6' AND accepted = 3 AND expDeleteR=0) OR (user_id='6' AND accepted=1 AND deleteS=0) OR (user_id='6' AND accepted=3 AND deleteS=0 AND expDeleteS=0)",
//		"select COUNT(*) AS rowcountli from livestream where friend_id='5'",
//		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='5' OR friendid='5' )",
//		"select max(friendid) from friends where user_id > 10",
//		"select COUNT(*) AS rowcountcam from user_cameras where user_id='5' AND is_streaming='1'"
//	};
	
	static String[] sampleqrys = {
		//"select COUNT(*) AS rowcount from friends where (userid='273' OR friendid='273') AND status='2'  "
		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='27' OR friendid='27' ) "
		/*"select count(*) from Emp",
		"select count(*) from Emp where salary > 10",
		"select e.eid, d.dno from Dept d, Emp e where d.dno=e.dno and d.dname='Dept1'"*/
		/* "select uc.virtual_clip_id, uc.clip_id, uc.duration, uc.title, uc.thumbnail,"
		+ " uc.filename, uc.description, uc.block_id_start, uc.block_id_end, u.uname,"
		+ " uc.cdate, uc.ctime, uc.privacy_level,rv.recommended_to,rv.recommended_by "
		+ "from user_clips uc, users u, recommended_videos rv "
		+ "where rv.recommended_to=699"
		+ " and u.user_id=rv.recommended_to and uc.virtual_clip_id=rv.virtual_clip_id "
		+ "order by rv.recommend_id DESC"*/
		//		"SELECT * FROM user_updates WHERE friend_id ='272' AND accepted=3 AND expDeleteR=0 order by seqnum desc ",
//		"SELECT * FROM user_cameras WHERE user_id ='45'",
//		"select COUNT(*) AS rowcount from user_cameras where user_id='45'"
		};
	
	static String[] Bsampleqrys = { //ToggleQrys = {
		"select count(*) AS rowcount from livestream li, user_updates uu, actualliveviewer ali where uu.camera_id='27'  AND li.update_id=uu.update_id AND ali.lid=li.lid and uu.user_id='27' and (ali.watching=1 or ali.is_recording=1)",
		"select * from camera_ports where camera_id=27'",
		"select uname, username, city,state, country, joindate, lastdate,dob from users where user_id='27'",
		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='27' OR friendid='27' ) ",
		"select COUNT(*) AS rowcount from messageinbox where receiver='27' AND read_flag='0' and receiverdeleted=0",
		"select COUNT(*) AS rowcount from friends where (friendid='27' OR userid='27') AND status='1'",
		"select COUNT(*) AS rowcount from user_updates where (friend_id='27' AND accepted = 3 AND expDeleteR=0) OR (user_id='27' AND accepted=1 AND deleteS=0) OR (user_id='27' AND accepted=3 AND deleteS=0 AND expDeleteS=0)",
		"select COUNT(*) AS rowcountli from livestream where friend_id='27'",
		"select COUNT(*) AS rowcountcam from user_cameras where user_id='27' AND is_streaming='1'",
		"select picture_content FROM users WHERE user_id='27'",
		"select * FROM users u WHERE u.user_id IN (select user_id from user_cameras WHERE is_streaming='1') AND u.user_id IN (select userid from friends where friendid='27', AND status='2' UNION select friendid from friends where userid='27', AND status='2')",
		"select * from user_cameras u, camera c, user_updates uu WHERE u.is_streaming='1' AND u.user_id='27' AND u.camera_id=c.camera_id AND uu.user_id=u.user_id AND uu.camera_id=c.camera_id AND uu.friend_id=u.user_id",
		"select * from user_updates uu, livestream li, camera c WHERE uu.update_id=li.update_id  and uu.friend_id='27' and li.edited=0 and uu.camera_id=c.camera_id",
		"select uc.virtual_clip_id, uc.clip_id, uc.duration, uc.title, uc.thumbnail, uc.filename, uc.description, uc.block_id_start, uc.block_id_end, u.uname, uc.cdate, uc.ctime, uc.privacy_level,rv.recommended_to,rv.recommended_by from user_clips uc, users u, recommended_videos rv where rv.recommended_to=27 and u.user_id=rv.recommended_to and uc.virtual_clip_id=rv.virtual_clip_id order by rv.recommend_id DESC ",
		"select COUNT(*) AS rowcountcam from user_cameras where user_id='27' AND is_streaming='1'",
		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='27' OR friendid='27' )",
		"select Cam.URL, Cam.camera_id, Cam.num_recording, UC.cname, UC.is_streaming, UC.is_recording, UC.privacy FROM camera Cam, user_cameras UC WHERE UC.user_id=27 AND UC.camera_id=Cam.camera_id AND UC.catID=0",
		"select update_id from user_updates where user_id='27' and friend_id='27' and camera_id='27'",
		"select COUNT(*) AS rowcount from indirectviewers where camera_id='27' and watching=1 and user_id='27'",
		"select category_id from category where user_id='27'",
		"select COUNT(*) AS rowcount from user_cameras where user_id='27'",
		"select * FROM user_cameras WHERE user_id = '27' AND camera_id = '27'",
		"select num_streaming, URL FROM camera WHERE camera_id = 27'",
		"select * from camera_ports where camera_id=27'",
		"INSERT INTO camera_ports(camera_id, host_addr, port_num) VALUES ('27', '192.168.3.76','14341')",
		"select is_streaming FROM user_cameras WHERE user_id = '27' AND camera_id = '27'",
		"UPDATE user_cameras SET is_streaming=1, startTime='16:11:37 30-May-2011' WHERE user_id='27' AND camera_id='27'",
		"UPDATE camera SET num_streaming=num_streaming+1 WHERE camera_id=27"
	};
	
	static String[] Tsampleqrys = { //BrowsingQrys = {
		"select uname, username, city,state, country, joindate, lastdate,dob from users where user_id='272' ",
		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='272' OR friendid='272' ) ",
		"select COUNT(*) AS rowcount from messageinbox where receiver='272' AND read_flag='0' and receiverdeleted=0 ",
		"select COUNT(*) AS rowcount from friends where (friendid='272' OR userid='272') AND status='1' ",
		"select COUNT(*) AS rowcount from user_updates where (friend_id='272' AND accepted = 3 AND expDeleteR=0) OR (user_id='272' AND accepted=1 AND deleteS=0) OR (user_id='272' AND accepted=3 AND deleteS=0 AND expDeleteS=0) ",
		"select COUNT(*) AS rowcountli from livestream where friend_id='272' ",
		"select COUNT(*) AS rowcountcam from user_cameras where user_id='272' AND is_streaming='1' ",
		"SELECT picture_content FROM users WHERE user_id='272' ",
		"select * FROM users u WHERE u.user_id IN (SELECT user_id from user_cameras WHERE is_streaming='1') AND u.user_id IN (select userid from friends where friendid='272' AND status='2' UNION select friendid from friends where userid='272' AND status='2') ",
		"SELECT * from user_cameras u, camera c, user_updates uu WHERE u.is_streaming='1' AND u.user_id='272' AND u.camera_id=c.camera_id AND uu.user_id=u.user_id AND uu.camera_id=c.camera_id AND uu.friend_id=u.user_id ",
		"SELECT * from user_updates uu, livestream li, camera c WHERE uu.update_id=li.update_id  and uu.friend_id='272' and li.edited=0 and uu.camera_id=c.camera_id ",
		"select uc.virtual_clip_id, uc.clip_id, uc.duration, uc.title, uc.thumbnail, uc.filename, uc.description, uc.block_id_start, uc.block_id_end, u.uname, uc.cdate, uc.ctime, uc.privacy_level,rv.recommended_to,rv.recommended_by from user_clips uc, users u, recommended_videos rv where rv.recommended_to=272 and u.user_id=rv.recommended_to and uc.virtual_clip_id=rv.virtual_clip_id order by rv.recommend_id DESC ",
		"select COUNT(*) AS rowcountcam from user_cameras where user_id='272' AND is_streaming='1' ",
		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='272' OR friendid='272' ) ",
		"select COUNT(*) AS rowcount from user_updates where (friend_id='272' AND accepted = 3 AND expDeleteR=0) OR (user_id='272' AND accepted=1 AND deleteS=0) OR (user_id='272' AND accepted=3 AND deleteS=0 AND expDeleteS=0) ",
		"SELECT * FROM user_updates WHERE friend_id ='272' AND accepted=3 AND expDeleteR=0 order by seqnum desc ",
		"select COUNT(*) AS rowcount from friends where status='2' AND ( userid='272' OR friendid='272' ) ",
		"SELECT * FROM users u , friends f WHERE u.user_id=f.userid and friendid=272 AND status='2' ",
		"SELECT * FROM users u , friends f WHERE u.user_id=f.userid and friendid=272 AND status='2' ",
		"SELECT * FROM users u, friends f WHERE u.user_id=f.friendid and f.userid=272 AND status='2' ",
		"SELECT picture_content FROM users WHERE user_id='271' ",
		"SELECT * FROM frndgroup f,grpmembership g WHERE f.group_id=g.group_id and g.friend_id='271' and f.user_id='272' ",
		"SELECT picture_content FROM users WHERE user_id='273' ",
		"SELECT * FROM frndgroup f,grpmembership g WHERE f.group_id=g.group_id and g.friend_id='273' and f.user_id='272' ",
		"select * from users where user_id='273' ",
		"select COUNT(*) AS rowcount from friends where (userid='273' OR friendid='273') AND status='2'  ",
		"select COUNT(*) AS rowcount from friends where friendid='272' AND status='1' ",
		"select COUNT(*) AS rowcount from messageinbox where receiver='272' AND read_flag='0' and receiverdeleted=0 ",
		"select COUNT(*) AS rowcount from user_updates where (friend_id='272' AND accepted = 3 AND deleteS=0 AND expDeleteR=0) OR (user_id='272' AND accepted=1 AND deleteS=0) OR (user_id='272' AND accepted=3 AND deleteS=0 AND expDeleteS=0) ",
		"SELECT picture_content FROM users WHERE user_id='273' ",
		"select * from users u, user_cameras uc, friends f where u.user_id = uc.user_id and is_streaming='1' and u.user_id=f.userid and f.status='2' and  (f.friendid='273' OR f.userid='273') ",
		"select * from users u, user_cameras uc, friends f where u.user_id = uc.user_id and is_streaming='1' and u.user_id=f.userid and f.status='2' and  (f.friendid='273' OR f.userid='273') ",
		"SELECT * from user_cameras u, camera c, user_updates uu WHERE u.is_streaming='1' AND u.user_id='273' AND u.camera_id=c.camera_id AND uu.user_id=u.user_id AND uu.camera_id=c.camera_id AND uu.friend_id=u.user_id AND u.privacy='1' ",
		"SELECT * from user_cameras u, camera c, user_updates uu WHERE u.is_streaming='1' AND u.user_id='273' AND u.camera_id=c.camera_id AND uu.user_id=u.user_id AND uu.camera_id=c.camera_id AND uu.friend_id=u.user_id AND u.privacy='1' ",
		"select COUNT(*) AS rowcount from friends where userid='272' AND friendid='273' AND status='2' ",
		"select COUNT(*) AS rowcount from friends where userid='273' AND friendid='272' AND status='2' ",
		"select COUNT(*) AS rowcount from user_cameras where user_id='272' ",
		"SELECT * FROM user_cameras WHERE user_id ='272'"
	};
	
	private static String GenerateSelectionClause(Vector clmns, String TupleVar)
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < clmns.size(); i++){
			if (sb.length() > 0) sb.append(",");
			if (TupleVar != null) sb.append(""+TupleVar+".");
			sb.append(""+clmns.elementAt(i).toString());
		}
		return sb.toString();
	}
	
	
	public static String RewriteSelectStar(StringBuffer SelectClause, StringBuffer FromClause, Connection DBcon)
	{
		String tos = FromClause.toString().trim();
		String tablename = null;
		String tuplevar = null;
		StringBuffer selcl = new StringBuffer();
		int start, end;
		if (tos.indexOf(" ") < 0) {
			//One table is referenced with no tuple variables
			Vector clmn = dba.GetColumnNames(DBcon, tos);
			return GenerateSelectionClause(clmn, null);
		} else {
			//Extract list of tuple variables
			if (tos.indexOf(",") < 0)
			{
				//Only one tuple variable
				start = tos.indexOf(" ");
				tablename = tos.substring(0, start);
				tuplevar = tos.substring(start, tos.length());
				Vector clmn = dba.GetColumnNames(DBcon, tablename);
				return GenerateSelectionClause(clmn,tuplevar);
			} else {
				//Multiple tuple variables
				String[] terms = tos.split(",");
				for (int i = 0; i < terms.length; i++){
					terms[i] = terms[i].trim();
					start = terms[i].indexOf(" ");
					end = terms[i].length();
					if (start > 0){
						tablename = terms[i].substring(0, start);
						tuplevar = terms[i].substring(start, end);
					} else {
						tablename = terms[i];
						tuplevar = null;
					}
					Vector clmn = dba.GetColumnNames(DBcon, tablename);
					if (clmn.size() > 0 && selcl.length() > 0) selcl.append(",");
					selcl.append( GenerateSelectionClause(clmn,tuplevar) );
				}
			}
		}
		return selcl.toString();
	}
	
	
	public static int NumberOfTupleVariables(StringBuffer FromClause)
	{
		String tos = FromClause.toString();
		if (tos.trim().indexOf(" ") > 0) {
			String[] terms = FromClause.toString().trim().split(",");
			return terms.length;
		} 
		return 1;
	}
	
	
	public static int GetOnePrimKeyFromClause(StringBuffer FromClause, Connection DBcon, StringBuffer results)
	{
		{
			int start, end;
			int elts = 0;
			String tablename, tuplevar;
			StringBuffer pk=new StringBuffer();
			
			if (results == null) System.out.println("Error (PrimaryKeysFromClause in COSARsqlRewrite.java):  input results cannot be null.");
			
			String tos = FromClause.toString();
			if (tos.trim().indexOf(" ") > 0) {
				//Multiple tuple variables; obtain the primary keys of the referenced tables and incorporate
				String[] terms = FromClause.toString().trim().split(",");
			
				for (int i = 0; i < terms.length; i++){
					terms[i] = terms[i].trim();
					start = terms[i].indexOf(" ");
					end = terms[i].length();
					if (start > 0){
						tablename = terms[i].substring(0, start);
						tuplevar = terms[i].substring(start, end);
					} else {
						tablename = terms[i];
						tuplevar = null;
					}
					pk = new StringBuffer();
					dba.GetPrimaryKey(DBcon, tablename, pk);
					//Vector clmn = dba.GetColumnNames(db_conn, tablename);
					if (pk.length() > 0 && results.length() > 0) results.append(",");
					String[] attrs = pk.toString().split(",");
					if (attrs.length > 0){
						elts ++;
						results.append(""+tuplevar+"."+attrs[0]);
					}
//					elts += attrs.length;
//					for (int k = 0; k < attrs.length; k++){
//						results.append( ""+tuplevar+"."+attrs[k] );
//						if (k < attrs.length-1) results.append(",");
//					}
				}
			} else {
				//One tuple variable
				//Obtain the primary keys of the table referenced in the from clause
				if ( ( elts = dba.GetPrimaryKey(DBcon, FromClause.toString().trim(), pk) ) > 0)
					if (verbose) System.out.println(""+pk.toString());
				String[] terms = pk.toString().trim().split(",");
				if (terms.length > 0) {
					results.append(terms[0]);
					elts++;
				}
			}
			return elts;
		}
	}
	
	public static boolean GetAnyAttr(StringBuffer FromClause, Connection DBcon, StringBuffer results){
		boolean res = true;
		Vector V = dba.GetColumnNames(DBcon, FromClause.toString().trim());
		if (V.size() > 0)
			results.append(V.elementAt(0).toString());
		else res = false;
		return res;
	}
	
	public static int PrimaryKeysFromClause(StringBuffer FromClause, Connection DBcon, StringBuffer results)
	{
		int start, end;
		int elts = 0;
		String tablename, tuplevar;
		StringBuffer pk=new StringBuffer();
		
		if (results == null) System.out.println("Error (PrimaryKeysFromClause in COSARsqlRewrite.java):  input results cannot be null.");
		
		String tos = FromClause.toString();
		if (tos.trim().indexOf(" ") > 0) {
			//Multiple tuple variables; obtain the primary keys of the referenced tables and incorporate
			String[] terms = FromClause.toString().trim().split(",");
		
			for (int i = 0; i < terms.length; i++){
				terms[i] = terms[i].trim();
				start = terms[i].indexOf(" ");
				end = terms[i].length();
				if (start > 0){
					tablename = terms[i].substring(0, start);
					tuplevar = terms[i].substring(start, end);
				} else {
					tablename = terms[i];
					tuplevar = null;
				}
				pk = new StringBuffer();
				dba.GetPrimaryKey(DBcon, tablename, pk);
				//Vector clmn = dba.GetColumnNames(db_conn, tablename);
				if (pk.length() > 0 && results.length() > 0) results.append(",");
				String[] attrs = pk.toString().split(",");
				elts += attrs.length;
				for (int k = 0; k < attrs.length; k++){
					results.append( ""+tuplevar+"."+attrs[k] );
					if (k < attrs.length-1) results.append(",");
				}
			}
		} else {
			//One tuple variable
			//Obtain the primary keys of the table referenced in the from clause
			if ( ( elts = dba.GetPrimaryKey(DBcon, FromClause.toString().trim(), pk) ) > 0)
				if (verbose) System.out.println(""+pk.toString());
			results.append(pk.toString());
		}
		return elts;
	}
	
	public static boolean RewriteAgg(StringBuffer SelectClause, StringBuffer FromClause, Connection DBcon)
	{
		int start, end;
		String tos;
		if ( ( SelectClause.toString().toUpperCase().contains(as) ) ) {
			tos = SelectClause.toString();
			start = tos.toUpperCase().indexOf(as);
			end = start + as.length();
			tos = SelectClause.toString().substring(end).trim();
			if (tos.indexOf(" ") > 0) end += tos.indexOf(" ");
			else end += tos.length();
			SelectClause.replace(start, end, "");
		}
		if (SelectClause.toString().contains("*")) {
			System.out.println("Error in rewriting aggregate queries:  No * allowed for min/max/average");
			System.out.println("Input select clause = "+SelectClause.toString());
			System.out.println("Input from clause = "+FromClause.toString());
			return false;
		} else {
			tos = SelectClause.toString();
			start = tos.indexOf(")");
			if (start < tos.length()) SelectClause.replace(start, tos.length(), "");
			end = tos.indexOf("(");
			SelectClause.replace(0,end+1,"");
		}
		return true;
	}
	
		
		
	public static boolean RewriteCount(StringBuffer SelectClause, StringBuffer FromClause, Connection DBcon)
	{
		int start, end;
		String tablename, tuplevar;
		StringBuffer pk = new StringBuffer();
		String tos=SelectClause.toString().toUpperCase();
		if ( ( tos.contains(as) ) ) {
			start = tos.indexOf(as);
			end = start + as.length();
			tos = SelectClause.toString().substring(end).trim();
			if (tos.indexOf(" ") > 0) end += tos.indexOf(" ");
			else end += tos.length();
			SelectClause.replace(start, end, "");
		}
		
		//If the select clause is "count(*)" then obtain meta-data and replace with the primary key of the referenced table
		//Otherwise, select clause is "count(attr)" and replace with attr
		if (SelectClause.toString().contains("*")) {
			int elts = 0;
			/*SelectClause.replace(0, SelectClause.length(), "");
			elts = PrimaryKeysFromClause(FromClause, DBcon, SelectClause);
			if (elts == 0) {
				if (!GetAnyAttr(FromClause, DBcon, SelectClause)) return false;
			}*/
			//Make sure there is only one table in the from clause
			tos = FromClause.toString();
			if (tos.trim().indexOf(" ") > 0) {
				//Multiple tuple variables; obtain the primary keys of the referenced tables and incorporate
				String[] terms = FromClause.toString().trim().split(",");
				SelectClause.replace(0, SelectClause.length(), "");
				for (int i = 0; i < terms.length; i++){
					terms[i] = terms[i].trim();
					start = terms[i].indexOf(" ");
					end = terms[i].length();
					if (start > 0){
						tablename = terms[i].substring(0, start);
						tuplevar = terms[i].substring(start, end);
					} else {
						tablename = terms[i];
						tuplevar = null;
					}
					elts = dba.GetPrimaryKey(DBcon, tablename, pk);
					if (elts == 0) 
						if (!GetAnyAttr(new StringBuffer(tablename), DBcon, pk)) return false;
					
					//Vector clmn = dba.GetColumnNames(db_conn, tablename);
					if (pk.length() > 0 && SelectClause.length() > 0) SelectClause.append(",");
					String[] attrs = pk.toString().split(",");
					for (int k = 0; k < attrs.length; k++){
						SelectClause.append( ""+tuplevar+"."+attrs[k] );
						if (k < attrs.length-1) SelectClause.append(",");
					}
				}
			} else {
				//One tuple variable
				//Obtain the primary keys of the table referenced in the from clause
				if (dba.GetPrimaryKey(DBcon, FromClause.toString().trim(), pk) == 0)
					if (!GetAnyAttr(FromClause, DBcon, pk)) return false;
				SelectClause.replace(0, SelectClause.length(), "");
				SelectClause.append(pk.toString());
			}
		} else {
			tos = SelectClause.toString();
			start = tos.indexOf(")");
			SelectClause.replace(start, tos.length(), "");
			end = tos.indexOf("(");
			SelectClause.replace(0,end+1,"");
		}
		
		return true;
	}
	
	public static boolean IsQuerySupported(String query)
	{
		boolean result = true;
		String qry = query.toUpperCase();
		if (qry.contains(" OR ") || qry.contains(")OR ") || qry.contains(")OR(") || qry.contains(" OR(")) return false; //Disjuncts are not supported
		if (qry.contains(" NOT ")|| qry.contains(")NOT ") || qry.contains(")NOT(") || qry.contains(" NOT(")) return false; //Negation is not supported
		if (qry.split("SELECT").length > 2) return false;  //nested queries are not supported
		if (qry.contains(">") || qry.contains("<") || qry.contains("<=") || qry.contains(">=") || qry.contains("!=")) return false; //only '=' is supported.
		if (qry.contains(" HAVING ")) return false;
		return result;
	}

	int GetToken(String QualList, StringBuffer Token, Vector<String> andPreds, Vector<String> orPreds, String LastOp){
		int Sidx, Eidx, Sidx2, Eidx2;
		boolean nestedparen = false;
		if (QualList.trim().indexOf("(")==0){
			//find the close pren
			//Obtain everything in between and make a recursive call
			Sidx = QualList.indexOf("(");
			Eidx = QualList.indexOf(")");

			Sidx2 = QualList.substring(Sidx+1).indexOf("(");
			while (Sidx2 >= 0 && Sidx2 < Eidx){
				int offset = QualList.substring(Eidx+1).indexOf(")");
				Sidx2 = QualList.substring(Eidx+1).indexOf("(");
				Eidx += offset+1;
				nestedparen = true;
				//Token.append( GetToken(QualList.substring(Sidx+1+Sidx2, Eidx+1), Token) );
			}
			if (nestedparen){
				GetToken( QualList.substring(Sidx+1, Eidx).trim(), Token, andPreds, orPreds, LastOp);
				//Token.append( QualList.substring(Sidx+1, Eidx).trim() );
				return Eidx+1;
			} else {
				String left = QualList.substring(Sidx+1, Eidx).trim();
				String tmp = QualList.substring(Eidx+1).trim();
				int ANDidx = tmp.trim().toUpperCase().indexOf("AND");
				int ORidx = tmp.trim().toUpperCase().indexOf("OR");
				
				int idx=-1;

				if (ANDidx >= 0 && ORidx >=0){
					if (ANDidx > ORidx) idx = ORidx;
					else idx = ANDidx;
				} else if (ANDidx >= 0) idx = ANDidx;
				else if (ORidx >= 0) idx = ORidx;
				
				if (idx < 0 && (LastOp.equals("") || LastOp.equals("AND"))) andPreds.add(left);
				else if (idx < 0 && LastOp.equals("OR")) orPreds.add(left);
				else if (idx == ANDidx)
				{
					andPreds.add(left);
					tmp = tmp.substring("AND".length() ).trim();
					if (tmp != null && tmp.length() > 0) andPreds.add(tmp);
				} else if (idx == ORidx)
				{
					orPreds.add(left);
					tmp = tmp.substring("OR".length()).trim();
					if (tmp != null && tmp.length() > 0) orPreds.add(tmp);
				} else System.out.println("May Day!  Wrong state in COSARSqlRewrite:GetToken with QualList="+QualList+", Token="+Token.toString());
			}
			return QualList.length();
		} 
		else {
			int ANDidx = QualList.trim().toUpperCase().indexOf("AND");
			int ORidx = QualList.trim().toUpperCase().indexOf("OR");
			int idx=-1;

			if (ANDidx >= 0 && ORidx >=0){
				if (ANDidx > ORidx) idx = ORidx;
				else idx = ANDidx;
			} else if (ANDidx >= 0) idx = ANDidx;
			else if (ORidx >= 0) idx = ORidx;

			if (idx < 0 && QualList.length() > 0){ 
				Token.append( QualList );
				return QualList.length();
			}
			else Token.append( QualList.trim().substring(0, idx) );
			return idx;
		}
		//return QualList.length();
	}
	
	 boolean ProcessPreds(String QualList, Vector<String> Qrys){
		String LastOp=""; 
		Vector<String> andPreds = new Vector<String>();
		Vector<String> orPreds = new Vector<String>();

		boolean done = false;
		int Sidx=0, Eidx=0;
		StringBuffer FirstOp;
		String tmp = QualList.trim();
		
		while (!done){
			FirstOp = new StringBuffer();
			Eidx = GetToken(tmp, FirstOp, andPreds, orPreds, LastOp);
			if (Eidx < tmp.length())tmp = tmp.substring(Eidx).trim();
			else tmp = "";
			
			int ANDidx = tmp.trim().toUpperCase().indexOf("AND");
			int ORidx = tmp.trim().toUpperCase().indexOf("OR");
			int idx=0;
			
			if (ANDidx >= 0 && ORidx >=0){
				if (ANDidx > ORidx) idx = ORidx;
				else idx = ANDidx;
			} else if (ANDidx >= 0) idx = ANDidx;
			else idx = ORidx;
			
			if (idx >= 0){

				if (ANDidx == idx){ 
					LastOp = "AND";
					Sidx = "AND".length();
				}
				else {
					LastOp="OR";
					Sidx = "OR".length();
				}

				tmp = tmp.substring(Sidx).trim();
			}

			if (LastOp == "AND") {
				if (FirstOp.length()>0) andPreds.addElement(FirstOp.toString());
			} else {
				if (FirstOp.length()>0) orPreds.addElement(FirstOp.toString());
			}
			
			if (tmp=="" || tmp==null || tmp.length()==0) 
				done = true;
		}
		
		if (andPreds.size() == 0 && orPreds.size() > 0){
			for (int i=0; i< orPreds.size(); i++) 
				Qrys.add(orPreds.elementAt(i));
		} else 
			if (andPreds.size() > 0 && orPreds.size() == 0) {
				boolean dn = false;
				boolean WithOR = false;
				while (!dn){
					dn = true;
					Vector<String> q = new Vector<String>();
					int j;
					for (j=0; j < andPreds.size(); j++){
						String elt = andPreds.elementAt(j).toUpperCase();
						if (elt.contains(" OR ") || elt.contains(")OR ") || elt.contains(")OR(") || elt.contains(" OR(")){
							dn = false;
							boolean res = ProcessPreds(andPreds.elementAt(j).trim(),q);
							WithOR = true;
							break;
						}
					}
					if (dn == false) {
						andPreds.removeElementAt(j);
						Vector<String> newAnd = new Vector<String>();
						for (j=0; j < q.size(); j++){
							String elt = q.elementAt(j);
							for (int k=0; k < andPreds.size(); k++)
								newAnd.addElement( andPreds.elementAt(k)+" AND "+ elt);
						}
						andPreds = newAnd;
					}
				}
				if (WithOR){
					for (int i=0; i< andPreds.size(); i++) 
						Qrys.add( andPreds.elementAt(i) );
				} else {
					String qry = andPreds.elementAt(0);
					for (int i=1; i< andPreds.size(); i++) 
						qry = qry + " AND " + andPreds.elementAt(i);
					Qrys.add(qry);
				}
		} else 
			if (andPreds.size() > 0 && orPreds.size() > 0){
				for (int k = 0; k < orPreds.size(); k++) {
					String qry = orPreds.elementAt(k);
					for (int l = 0; l < andPreds.size(); l++)
						qry = qry+" AND "+andPreds.elementAt(l);
					Qrys.add(qry);
				}
			}
		return true;
	}
	
	
	
	/*
	 * TransformDisjuncts
	 * Author:  Shahram Ghandeharizadeh, April 22, 2012
	 * Functionality:  Consumes a disjunctive query and breaks it into multiple queries.
	 * Assumption:  We assume there are no nested parantheses such as "OR (... OR ( ... ) OR ...)"
	 */
	public Vector<String> TransformDisjuncts(String query){
		Vector<String> V = new Vector<String>();
		StringBuffer SelectClause = new StringBuffer();
		StringBuffer FromClause = new StringBuffer();
		StringBuffer WhereClause = new StringBuffer();
		StringBuffer OrderByClause = new StringBuffer();
		String wp="";
		String where="";
		
		int NumANDS = 0;
		if (SelectClause == null || FromClause == null || WhereClause == null || OrderByClause == null){
			System.out.println("Error (CorrectSyntax method of QueryToTrigger class):  Input parameters cannot be null.  They must be instantiated by the caller");
			return null;
		}
		
		StringBuffer revQ = new StringBuffer( query.toUpperCase() );
		
		int idxS, idxF, idxW, idxO;
		idxS = revQ.indexOf(QueryToTrigger.select);
		idxF = revQ.indexOf(QueryToTrigger.from);
		idxW = revQ.indexOf(QueryToTrigger.where);
		idxO = revQ.indexOf(QueryToTrigger.orderby);
		
		String hdr = query.substring(0, idxW)+ " WHERE ";
		
		if (idxO < 0) idxO = revQ.length(); //needed to compute the where clause
		else {
			OrderByClause.append( query.substring(idxO+QueryToTrigger.orderby.length(), revQ.length()).trim() );
		}
		
		if (idxW > 0){
			if ( idxS < 0 || idxF < 0 || idxW < 0 || idxF < idxS || idxW < idxS || idxW < idxF) return null;
			FromClause.append( query.substring(idxF+QueryToTrigger.from.length(), idxW).trim() );
		} else { 
			if ( idxS < 0 || idxF < 0 || idxF < idxS) return null;
			FromClause.append( query.substring(idxF+QueryToTrigger.from.length(), idxO).trim() );
		}
			
		if (idxW > 0){
			wp = query.substring(idxW+ QueryToTrigger.where.length(), idxO).trim();
		}
		//Remove order by clause
		where = wp.toUpperCase();
		if (where.contains("ORDER BY"))
		{
			int start = where.indexOf("ORDER BY");
			wp = wp.substring(0,start);
			//wp.replace(start, wp.length(), "");
		}
		
		Vector<String> Qrys = new Vector<String>();
		boolean res = ProcessPreds(wp, Qrys);
		boolean done = false;
		while (!done){
			done = true;
			int i;
			for (i = 0; i < Qrys.size(); i++)
				if (Qrys.elementAt(i).indexOf("(") >= 0)
				{
					done = false;
					break;
				}
			if (!done) {
				String qry = Qrys.elementAt(i);

				Qrys.removeElementAt(i);
				res = ProcessPreds(qry, Qrys);
			}
		}
		for (int i = 0; i < Qrys.size(); i++){
			V.addElement(""+hdr+""+Qrys.elementAt(i));
			//System.out.println("Qry: "+Qrys.elementAt(i));
		}
		return V;
	}
	
	/*
	 * RewriteQuery returns true only if the original query is re-written.
	 * If the original query requires no re-write then it is placed in the input query and returns true.
	 * Otherwise, the original query has somehow caused RewriteQuery to fail and return false;
	 */
	public static boolean RewriteQuery(String query, Vector rewrite, Connection DBcon)
	{
		boolean success = true;
		boolean Modified = false;
		int ANDs = 0;
		
		if( DBcon == null )
		{
			System.out.println("Error (RewriteQuery), DBcon is null.  Allocate DBMS connection prior to calling this method.");
			return false;
		}
		
		if (rewrite == null){
			System.out.println("Error (RewriteQuery), rewrite is null.  Allocate memory space prior to calling this method.");
			return false;
		}
		
		if (verbose)
			System.out.println("RewriteQuery("+query+")");
		
		StringBuffer SelectClause = new StringBuffer();
		StringBuffer FromClause = new StringBuffer();
		StringBuffer WhereClause = new StringBuffer();
		StringBuffer OrderByClause = new StringBuffer();
		
		if (qtt == null) qtt = new QueryToTrigger();
		ANDs=qtt.CorrectSyntax (query, SelectClause, FromClause, WhereClause, OrderByClause);
		
		//Remove order by clause
		String where = WhereClause.toString().toUpperCase();
		if (where.contains("ORDER BY"))
		{
			int start = where.indexOf("ORDER BY");
			WhereClause.replace(start, WhereClause.length(), "");
			Modified = true;
		}
		
		String sel = SelectClause.toString().trim().toUpperCase();
		sel = sel.replace(" ", "");
		if (sel.startsWith("*"))
		{
			String sb;
			StringBuffer res = new StringBuffer();
			String selcl = RewriteSelectStar(SelectClause, FromClause, DBcon);
			SelectClause.replace(0, SelectClause.length(), "");
			SelectClause.append(selcl);
			Modified = true;
		} else {
			if (sel.contains("COUNT("))
			{
				//If it contains a "having" clause then there is no re-write to be done; only table level is supported.
				if (WhereClause.toString().toUpperCase().contains("HAVING")) {
					rewrite.addElement(query);
					return false;
				}
				//Fix the select clause
				if (RewriteCount(SelectClause, FromClause, DBcon)) Modified = true;
			}
			if (sel.contains("SUM(") || sel.contains("MAX(") || sel.contains("MIN(") || sel.contains("AVG(") || sel.contains("MEDIAN(")) {
				if (RewriteAgg(SelectClause, FromClause, DBcon)) Modified = true;
			}
		}
		
		/* Not required for SQLTrig
		//Ensure the SELECT clause has no more than the maximum specified attributes (MAX_PROJECTION_LIST)
		if (Modified && SelectClause.toString().split(",").length > MAX_PROJECTION_LIST) {
			StringBuffer res = new StringBuffer();
			StringBuffer pkeySTR = new StringBuffer();
			int elts = 0;
			
			if ( NumberOfTupleVariables(FromClause) > 1) {
				elts = GetOnePrimKeyFromClause(FromClause, DBcon, pkeySTR); 
				res.append("SELECT "+pkeySTR.toString() + "");
			} else res.append("SELECT ");
			
			String[] terms = SelectClause.toString().split(",");
			boolean firstTIME= true;
			int i;
			int cnt = 0;
			int numattrs = elts;
			for (i = 1; i < terms.length+1; i++) {
				if (pkeySTR.toString().contains(terms[i-1])) cnt++;
				else {
					if (firstTIME && elts > 0){
						res.append(", ");
						firstTIME = false;
					}
					res.append(""+terms[i-1]);
					cnt++;
					numattrs ++;
					if ( numattrs % MAX_PROJECTION_LIST == 0){
						res.append(" FROM "+FromClause.toString());
						if (WhereClause.toString().trim().length() > 0)
							res.append(" WHERE "+ WhereClause.toString());

						rewrite.addElement(res.toString());
						res = new StringBuffer();
						if (elts > 0){
							res.append("SELECT "+pkeySTR.toString()+"");
							//if (i < terms.length) res.append(", ");
						}
						else res.append("SELECT ");
						firstTIME = true;
						cnt = 0;
						numattrs = elts;
					} else if (i < terms.length) res.append(",");
				}
			} 
			//Collect residue attributes and generate a final select statement.
			if (cnt != 0){
				res.append(" FROM "+FromClause.toString());
				if (WhereClause.toString().trim().length() > 0)
					res.append(" WHERE "+ WhereClause.toString());

				rewrite.addElement(res.toString());
			}
		} else */
		if (Modified)
			if (WhereClause.length() > 0)
				rewrite.addElement("SELECT "+SelectClause.toString()+" FROM "+FromClause.toString()+ " WHERE " + WhereClause.toString() );
			else rewrite.addElement("SELECT "+SelectClause.toString()+" FROM "+FromClause.toString() );
		else rewrite.addElement(query);
		
		if (verbose)
			if (!Modified)  
				System.out.println("No rewrite!  Return original qry "+query);
			else {
				for (int k=0; k < rewrite.size(); k++)
					System.out.println("\t"+rewrite.elementAt(k).toString() );
			}
		
		return success;
	}
	
	public static void COSARsqlRewrite(int MaxSelectProjAttrs)
	{
		MAX_PROJECTION_LIST = MaxSelectProjAttrs;
		qtt = new QueryToTrigger();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connection db_conn = null;
		try {
			// Create a connection object to the dbms
				Class.forName ("oracle.jdbc.driver.OracleDriver");
				db_conn = DriverManager.getConnection(DBConnector.getConnectionString(),DBConnector.getUsername(),DBConnector.getPassword());

		} catch(Exception e){
			e.printStackTrace();
			return ;
		}
		

		
		//OracleDatabaseMetaData odm = new OracleDatabaseMetaData(db_conn); 
		// TODO Auto-generated method stub
		qtt = new QueryToTrigger();
		
		for (int i = 0; i < sampleqrys.length; i++){
			System.out.print("Input sql: "+sampleqrys[i]+"; ");
			Vector rewrite = new Vector();
			if (RewriteQuery(sampleqrys[i], rewrite, db_conn)) {
				System.out.println("Rewritten as: "+rewrite.size()+" queries.");
				for (int k=0; k < rewrite.size(); k++)
					System.out.println("\t"+rewrite.elementAt(k).toString() );
			} else System.out.println("No rewrite!");
		}
	}

}
