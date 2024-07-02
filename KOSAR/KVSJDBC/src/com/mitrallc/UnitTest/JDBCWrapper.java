package com.mitrallc.UnitTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.mitrallc.config.DBConnector;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.sql.KosarSoloDriver;

public class JDBCWrapper {
	public static boolean UseKosarSolo = true;
	public static int NUMDEPTS = 200;
	public static int NUMEMPS = 2000;
	public static int EMPDEPT = NUMEMPS / NUMDEPTS;
	public static int NumIterations = 10000;

	private static String oracleDriver = "oracle.jdbc.driver.OracleDriver";
	private static String kosarDriver = "com.mitrallc.sql.KosarSoloDriver";

	public static void dropTable(Statement st, String table_name) {
		try {
			st.executeUpdate(" DROP TABLE " + table_name
					+ " cascade constraints");
		} catch (Exception e) {

		}
	}

	public static void createSchema(Connection con) {
		try {
			Statement st = con.createStatement();
			dropTable(st, "Emp");
			dropTable(st, "Dept");

			st.executeUpdate("Create Table Emp (eid int, ename varchar(30), age int, salary int, dno int)");
			/*st.executeUpdate("create or replace TRIGGER  EMPTrig BEFORE INSERT ON EMP" +
					" FOR EACH ROW  DECLARE KEYTODELETE CLOB;  COSAR_RET_VAL BINARY_INTEGER; " +
					" k1 CLOB := TO_CLOB(''); seperator CLOB := TO_CLOB(' ');" +
					" BEGIN FOR somekey IN (SELECT * FROM EMPKeys)" +
					" LOOP " +
					" k1 := CONCAT(k1, somekey.COLUMN1);" +
					" k1 := CONCAT(k1, seperator);" +
					" END LOOP;" +
					" IF (k1 != ' ') THEN " +
					" COSAR_Ret_Val := COSARTriggerDeleteMultiCall('10.0.0.195:4000,10.0.0.195:4001,10.0.0.195:4002,10.0.0.195:4003,10.0.0.195:4004', 'RAYS', k1, 0);" +
					" END IF;" +
					" DELETE FROM EMPkeys; " +
					" END;");*/
			st.executeUpdate("Create Table Dept (dno int, dname varchar(30), floor int, mgrid int)");
		/*	st.executeUpdate(
					" create or replace TRIGGER DEPTTrig  BEFORE INSERT ON Dept" +
					" FOR EACH ROW  DECLARE KEYTODELETE CLOB;  COSAR_RET_VAL BINARY_INTEGER; " +
					" k1 CLOB := TO_CLOB(''); seperator CLOB := TO_CLOB(' ');" +
					" BEGIN FOR somekey IN (SELECT * FROM deptKeys)" +
					" LOOP " +
					" k1 := CONCAT(k1, somekey.COL1);" +
					" k1 := CONCAT(k1, seperator);" +
					" END LOOP;" +
					" IF (k1 != ' ') THEN " +
					" COSAR_Ret_Val := COSARTriggerDeleteMultiCall('10.0.0.195:4000,10.0.0.195:4001,10.0.0.195:4002,10.0.0.195:4003,10.0.0.195:4004', 'RAYS', k1, 0);" +
					" END IF;" +
					" DELETE FROM deptkeys; " +
					" END;");*/
			st.close();
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace();
			}
		}

	}

	public static void PhysicalOptimization(Connection con) {

		try {
			Statement st = con.createStatement();
			st.executeUpdate("Create index deptdname on dept (dname)");
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void InsertOneDeptRow(Connection con, int dno, String dname,
			int floor, int mgrid) {
		try {
			Statement st = con.createStatement();
			String upd = "Insert into Dept (dno, dname, floor, mgrid) values ("
					+ dno + ", '" + dname + "', " + floor + ", " + mgrid + ")";
			st.executeUpdate(upd);
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void InsertOneEmpRow(Connection con, int eid, String ename,
			int age, int salary, int dno) {
		try {
			Statement st = con.createStatement();
			String upd = "Insert into Emp (eid, ename, age, salary, dno) values ("
					+ eid
					+ ", '"
					+ ename
					+ "', "
					+ age
					+ ", "
					+ salary
					+ ", "
					+ dno + ")";
			st.executeUpdate(upd);
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void InsertEmps(Connection con, int NumEmps, int NumDepts) {
		String ename;
		int eage, esalary;
		for (int i = 1; i < NumEmps + 1; i++) {
			if (i % 2 == 0) {
				ename = "John" + i;
				eage = 37;
				esalary = 35000;
			} else {
				ename = "Mary" + i;
				eage = 35;
				esalary = 39000;
			}
			InsertOneEmpRow(con, i, ename, eage, esalary, (i % NumDepts) + 1);
		}
	}

	public static String GetDeptName(int deptid) {
		return "Dept" + (deptid % NUMDEPTS);
	}

	public static void InsertDepts(Connection con, int NumDepts) {
		for (int i = 1; i < NUMDEPTS + 1; i++) {
			InsertOneDeptRow(con, i, GetDeptName(i), i, i);
		}
	}

	public static void VerifyData(Connection con) {
		/*
		 * Ensure the number of employees is NUMEMPS and number of departments
		 * is NUMDEPTS
		 */
		try {
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery("select count(*) from Dept");
			if (rs.next()) {
				if (NUMDEPTS != rs.getInt(1))
					System.out.println("Error, the number of deparments "
							+ rs.getInt(1)
							+ " does not match the anticipated number "
							+ NUMDEPTS + ".");
				else
					System.out.println("Successfully created Dept table with "
							+ NUMDEPTS + " rows.");
			}
			rs = st.executeQuery("select count(*) from Emp");
			if (rs.next()) {
				if (NUMEMPS != rs.getInt(1))
					System.out.println("Error, the number of employees "
							+ rs.getInt(1)
							+ " does not match the anticipated number "
							+ NUMEMPS + ".");
				else
					System.out.println("Successfully created Emp table with "
							+ NUMEMPS + " rows.");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void BuildDB() {
		/*
		 * This method creates two tables Emp and Dept Emp(ss#, name, age,
		 * salary, dno) Dept(dno, dname, floor, mgrSS#)
		 */
		Connection conn = null;

		try {
			if (UseKosarSolo) {
				// Use the COSAR wrapper version of the JDBC driver
				Class.forName(kosarDriver);
				// DriverManager.getConnection(paramString, paramProperties)

				// Initialize the cache connection pool
				// int num_connections_per_server = 10;
				// CacheConnectionPool.addServer(RaysConfig.getCacheServerHostname(),
				// RaysConfig.getCacheServerPort());
				// CacheConnectionPool.init(num_connections_per_server, true);
			} else
				Class.forName(oracleDriver);

			String url = DBConnector.getConnectionString();
			conn = DriverManager.getConnection(url, DBConnector.getUsername(),
					DBConnector.getPassword());

			System.out.println("connected");// createSchema();
			createSchema(conn);
			InsertDepts(conn, NUMDEPTS);
			InsertEmps(conn, NUMEMPS, NUMDEPTS);
			PhysicalOptimization(conn);
			VerifyData(conn);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void ComputeDeptEmployees(Connection con, String dname) {
		/*
		 * This method computes employees working for a specified department
		 */
		try {
			Statement st = con.createStatement();
			String qry = "select e.eid, d.dno from Dept d, Emp e where d.dno=e.dno and d.dname='"
					+ dname + "'";
			ResultSet rs = st.executeQuery(qry);
			int cntr = 0;
			while (rs.next()) {
				cntr++;
			}
			if (cntr != EMPDEPT) {
				System.out.println("\nError, there should be " + EMPDEPT
						+ " employees for department " + dname);
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void ComputeCountDeptEmployees(Connection con, String dname) {
		/*
		 * This method computes employees working for a specified department
		 */
		try {
			Statement st = con.createStatement();
			String qry = "select count(e.eid) from Dept d, Emp e where d.dno=e.dno and d.dname='"
					+ dname + "'";
			ResultSet rs = st.executeQuery(qry);

			if (rs.next()) {
				if (rs.getInt(1) != EMPDEPT) {
					System.out.println("\nError, there should be " + EMPDEPT
							+ " employees for department " + dname);
				}
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void ComputeTimeToPerformJoins(int NumIters) {
		long start_time, end_time;
		Connection conn = null;
		try {
			if (UseKosarSolo)
				Class.forName(kosarDriver);
			else
				Class.forName(oracleDriver);

			Properties p = new Properties();
			p.setProperty("user", DBConnector.getUsername());
			p.setProperty("password", DBConnector.getPassword());

			String url = DBConnector.getConnectionString();
			// conn =
			// DriverManager.getConnection(url,DBConnector.getUsername(),DBConnector.getPassword());
			conn = DriverManager.getConnection(url, p);

			start_time = System.currentTimeMillis();
			for (int i = 1; i < NumIters + 1; i++) {
				ComputeDeptEmployees(conn, GetDeptName(i));
			}
			end_time = System.currentTimeMillis();
			System.out.println("Total time to perform 1st " + NumIters
					+ " joins of Emp and Dept table: "
					+ (end_time - start_time) + " ms");
			System.out.println("\t Time per query = " + (end_time - start_time)
					* 1.0 / NumIters + " ms");

			start_time = System.currentTimeMillis();
			for (int i = 1; i < NumIters + 1; i++) {
				ComputeDeptEmployees(conn, GetDeptName(i));
			}
			end_time = System.currentTimeMillis();
			System.out.println("Total time to perform 2nd " + NumIters
					+ " joins of Emp and Dept table: "
					+ (end_time - start_time) + " ms");
			System.out.println("\t Time per query = " + (end_time - start_time)
					* 1.0 / NumIters + " ms");

			start_time = System.currentTimeMillis();
			for (int i = 1; i < NumIters + 1; i++) {
				ComputeCountDeptEmployees(conn, GetDeptName(i));
			}
			end_time = System.currentTimeMillis();
			System.out.println("Total time to perform 1st " + NumIters
					+ " count joins of Emp and Dept table: "
					+ (end_time - start_time) + " ms");
			System.out.println("\t Time per query = " + (end_time - start_time)
					* 1.0 / NumIters + " ms");

			start_time = System.currentTimeMillis();
			for (int i = 1; i < NumIters + 1; i++) {
				ComputeCountDeptEmployees(conn, GetDeptName(i));
			}
			end_time = System.currentTimeMillis();
			System.out.println("Total time to perform 2nd " + NumIters
					+ " count joins of Emp and Dept table: "
					+ (end_time - start_time) + " ms");
			System.out.println("\t Time per query = " + (end_time - start_time)
					* 1.0 / NumIters + " ms");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int GetDeptNo(String dname) {
		int result = -1;
		return result;
	}

	public void UpdateEmployee(int ss, int dname) {

	}

	public void JDBCwrapperTest() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Sanity checks
		System.out.println("Start simple test:");
		System.out.println("\t Create " + NUMDEPTS + " departments with "
				+ NUMEMPS + " employees.");
		if (NUMEMPS < NUMDEPTS) {
			System.out.println("\nError, NUMEMPS (" + NUMEMPS
					+ ") must be equal to or a multiple of NUMDEPTS ("
					+ NUMDEPTS + ").");
			System.out
					.println("\t Adjust these constants at the beginning of JDBCwrapperTest and try again.");
			System.out.println("\t Exiting!");
			return;
		} else if (NUMEMPS % NUMDEPTS != 0) {
			System.out.println("\nError, NUMEMPS (" + NUMEMPS
					+ ") must be a multiple of NUMDEPTS (" + NUMDEPTS + ").");
			System.out
					.println("\t Consider setting NUMEMPS to "
							+ NUMDEPTS
							* (NUMEMPS / NUMDEPTS)
							+ " and try again.  This constant is defined at the beginning of JDBCwrapperTest.");
			System.out.println("\t Resetting NUMEMPS to " + NUMDEPTS
					* (NUMEMPS / NUMDEPTS) + " and continuing.");
			NUMEMPS = NUMDEPTS * (NUMEMPS / NUMDEPTS);
			EMPDEPT = NUMEMPS / NUMDEPTS;
		}
		BuildDB();
		System.out.println("Built the database successfully.");

		System.out
				.println("\nIssuing "
						+ NumIterations
						+ " joins between Emp and Dept tables for different departments.");
		ComputeTimeToPerformJoins(NumIterations);

		System.out.println("End successfully.");
	}

}
