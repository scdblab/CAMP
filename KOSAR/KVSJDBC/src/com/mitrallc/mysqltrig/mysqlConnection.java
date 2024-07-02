package com.mitrallc.mysqltrig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;


public class mysqlConnection {
	private static String connectionString = "jdbc:mysql://localhost:3306/kosardb";
	private static String username = "root";		
	private static String password = "root"; 


	public static Connection db_conn = null;
	public static boolean verbose=false; 

	protected static String[] sampleqrys = { 
		"SELECT inviterid, inviteeid FROM friendship WHERE inviterid = 0 AND status = 2"

		//"SELECT userid, inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address, email, tel FROM users, friendship WHERE ((inviterid = 0 AND userid = inviteeid) OR (inviteeid = 0 AND userid = inviterid)) AND status = 2"
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

	public static void puttingQueue(String qry){
		if (!regthread.AddQry(qry))
			System.out.println("mysqlConnection failed to add qry to the queue for trigger registeration.");
			System.out.println("\t Qry: "+qry);
	}
	public static void main(String[] args) {

		Vector<String> V = new Vector<String>();
		String QueryTemplate;
		String cmd1 = "DROP PROCEDURE IF EXISTS FRIENDSHIP2110017;";
		String cmd4="CREATE TABLE FRIENDSHIP"
				+ "(INVITERID INTEGER NOT NULL, INVITEEID INTEGER NOT NULL,"
				+ "STATUS INTEGER DEFAULT 1)";
		String cmd5="DROP TRIGGER IF EXISTS FRIENDSHIP10191019699I";
		String cmd6="CREATE TRIGGER  FRIENDSHIP10191019699I BEFORE INSERT ON FRIENDSHIP FOR EACH ROW BEGIN DECLARE DELETEKEY varchar(2048) DEFAULT ' '; DECLARE DeleteIT_DLL_Val int; DECLARE KEYTODELETE varchar(2048) DEFAULT ' ';  SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE)); SET DELETEKEY = CONCAT(DELETEKEY, ' '); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT('_IIB2',CONCAT(NEW.inviterID, NEW.status))); SET DELETEKEY = CONCAT(DELETEKEY, ' '); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT('_IIB2',CONCAT(NEW.inviteeID, NEW.status)));IF (DELETEKEY != ' ') THEN  SET DeleteIT_DLL_Val = KOSARTriggerDeleteMultiCall('', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN UPDATE `Failed to connect to KOSAR KVS CORE.` SET x=1; END IF; END IF; END;";
		try {
			// Create a connection object to the dbms
			Class.forName ("com.mysql.jdbc.Driver");
			db_conn = DriverManager.getConnection(connectionString,username,password);

		} catch(Exception e){
			e.printStackTrace();
			return ;
		}
		MySQLQueryToTrigger qt = new MySQLQueryToTrigger();
		regthread rt=new regthread(db_conn);
		rt.start();
		for(int j=0;j<100;j++){
			Vector<String> key = new Vector<String>();
			QueryTemplate = qt.TransformQuery(sampleqrys[0], V, key, db_conn);
			//System.out.println("This is query templae:");
			for (int i=0;i<V.size();i++){
				puttingQueue(V.elementAt(i));
				System.out.println("mysqlConnection "+V.elementAt(i));
			}}
		try {
			rt.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(verbose)
			System.out.println("mysqlConnection:regthread died,ending.");

	}
}
