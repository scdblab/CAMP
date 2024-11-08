package com.mitrallc.kosar;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import com.mitrallc.mysqltrig.MySQLQueryToTrigger;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.ResultSet;
import com.mitrallc.sqltrig.OracleQueryToTrigger;
import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;

public class kosar {
	private static boolean IssuingTriggerCMDS=false;

	private static final int NumTriggerListenerThreads = 5;
	public static int NumFragments = 101;
	private static long Delta = 31;
	private static long inflation = 2; // factor of 2
	private static long Tadjust = 0;
	//private static ConcurrentHashMap<String, dust>[] RS = new ConcurrentHashMap[NumFragments];
	private static Object[] RS = new Object[NumFragments]; // Its type is really
	// private static
	// ConcurrentHashMap<String,
	// dust>[] RS = new
	// ConcurrentHashMap<String,
	// dust>[NumFragments]();
	public static Object[] ITs = new Object[NumFragments]; // Its type is really
	// public static
	// ConcurrentHashMap<String,
	// ConcurrentHashMap<String,String>
	// >[] ITs = null;
	private static java.sql.Connection db_conn = null;
	private com.mitrallc.sql.ResultSet a = null;

	public static QueryToTrigger qt = null;

	private static boolean verbose = false;

	private static int MonitorFreq = 10 * 1000;

	public static Semaphore StartTrigListeners = new Semaphore(0, true);
	public static Stats KosarMonitor = new Stats(MonitorFreq);
	public static ReplacementTechnique RP = new ReplacementTechnique(); //This is the replacement technique module


	public static boolean isIssuingTrigCMDS() {
		return IssuingTriggerCMDS;
	}

	public static void setIssuingTrigCMDS(boolean issuingCMDS) {
		IssuingTriggerCMDS = issuingCMDS;
	}

	public static int getFragment(String key) {
		if (NumFragments <= 0) {
			return -1;
		}

		/*
		 * int retHash = 5381; int len = 0;
		 * 
		 * while (len < key.length()) { retHash = ((retHash << 5) + retHash) +
		 * key.charAt(len); len++; }
		 * 
		 * if (retHash < 0) { retHash = -retHash; }
		 */

		int hash = (key.hashCode() < 0 ? ((~key.hashCode()) + 1) : key
				.hashCode());
		hash = hash % NumFragments;
		return hash;
	}

	public void DeleteIT(String internalToken) {
		InternalTokenElement ite = ((ConcurrentHashMap<String, InternalTokenElement>) ITs[this
		                                                                                  .getFragment(internalToken)]).get(internalToken);
		if (ite == null)
			ite = new InternalTokenElement();

		ite.setGumball();

		for(String qry : ite.getQueryStringKeySet()) {
			if (KosarSoloDriver.webServer != null)
				KosarSoloDriver.KosarInvalKeysAttemptedEventMonitor.newEvent(1);
			
			DeleteDust(qry);
		}
	}

	/***
	 * This method removes a dust by eliminating all the pointers to it:
	 * 1.  Removes it from the RS
	 * 2.  Removes it from the replacement policy
	 * 3.  Removes it from the QTmeta data structure that tracks instances of a query template 
	 */
	public static void DeleteDust(dust elt){
		DeleteCachedQry(elt.getKey());
		RP.DeleteKV(elt, getFragment(elt.getKey()));
		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(elt.getQueryTemplate());
		qtelt.deleteQInstance(elt.getKey());
	}

	public static void DeleteDust(String qry){
		int fragid = getFragment(qry);
		dust elt = ((ConcurrentHashMap<String, dust>) RS[fragid]).get(qry);

		//This delete may reference a dust that does not exist
		if (elt == null) return;

		DeleteCachedQry(qry);

		//Remove from the replacement policy data structure
		RP.DeleteKV(elt, fragid);

		//Remove from a list of the query instances for the template of this query
		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(elt.getQueryTemplate());
		if (qtelt != null) qtelt.deleteQInstance(elt.getKey());
	}

	public static void DeleteCachedQry(String qry) {
		dust ds = ((ConcurrentHashMap<String, dust>) RS[getFragment(qry)]).get(qry);

		if(ds == null) {
			//ERROR, the following line should be removed
			//FIX in response to the statement above:  ds = new dust();
			//REMOVE ABOVE TWO LINES AFTER EXTENSIVE TESTING

		}
		else {
			//ERROR, the ds element must be removed from RS to free memory
			//FIX in response to the statement above: ds.setRS(null);
			//REMOVE ABOVE TWO LINES AFTER EXTENSIVE TESTING
			if (verbose)
				System.out.println("Delete cached qry="+qry);
			((ConcurrentHashMap<String, dust>) RS[getFragment(qry)]).remove(qry);
			
			if (KosarSoloDriver.webServer != null) 
				KosarSoloDriver.KosarInvalidatedKeysEventMonitor.newEvent(1);
		}
	}

	public static void clearCache() {
		if (verbose) System.out.println("Flushing the KVS.");
		KosarSoloDriver.kosarEnabled = false;
		for (int i = 0; i < NumFragments; i++) {
			if ((ConcurrentHashMap<String, dust>) RS[i] != null)
				((ConcurrentHashMap<String, dust>) RS[i]).clear();
			if ((ConcurrentHashMap<String, ConcurrentHashMap<String, String>>) ITs[i] != null)
				((ConcurrentHashMap<String, ConcurrentHashMap<String, String>>) ITs[i])
				.clear();
		}
		RP = new ReplacementTechnique();
		QueryToTrigger.FlushQTQI();
		
		if (KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarKeysCachedEventMonitor.reset();
	
		KosarSoloDriver.kosarEnabled = true;
	}

	public com.mitrallc.sql.ResultSet GetQueryResult(String qry) {
		com.mitrallc.sql.ResultSet myres = null;
		int fragid = this.getFragment(qry);
		KosarMonitor.IncrementNumReqs();
		// If qry exists then return resultset
		// If either ki-gi exists or no entry exists then return null with Tmiss
		// timestamp.		
		dust ds = ((ConcurrentHashMap<String, dust>) RS[fragid]).get(qry);

		if( ds != null) {
			//If the query template of this query instance has been disabled then do not serve using the KVS
			QTmeta qtelt = QueryToTrigger.TriggerCache.get(ds.getQueryTemplate());
			if (!qtelt.isSwitchButtonOn()) return null;

			com.mitrallc.sql.ResultSet localDS = ds.getRS();
			if(localDS != null){
				KosarMonitor.IncrementNumHit();
				myres = new com.mitrallc.sql.ResultSet(localDS);
				RP.RegisterHit(ds, fragid);

				//Register stats:  Increment the number of query instances for this template
				if (KosarSoloDriver.webServer != null){
					QTmeta qtm = QueryToTrigger.TriggerCache.get(ds.getQueryTemplate());
					qtm.addKVSHits();
				}
			}
		}
		return myres;
	}

	public void attemptToCache(String sql, com.mitrallc.sql.ResultSet rs,
			long Tmiss) {

		if (verbose)
			System.out.println("Qry: " + sql);

		int fragid = this.getFragment(sql);

		dust ds = ((ConcurrentHashMap<String, dust>) RS[fragid])
				.get(sql);

		/******  Get Internal Keylist ******/
		String ParamQry = qt.TokenizeWhereClause(sql);
		if (ParamQry == null)
			ParamQry = sql; // qry has no where clause

		QTmeta qtm = QueryToTrigger.TriggerCache.get(ParamQry);
		if (qtm == null){
			//First time this query template has been encountered
			qtm = new QTmeta();
			qtm.setQueryTemplate(ParamQry);
			QueryToTrigger.TriggerCache.put(ParamQry,qtm);
		}
		if (!qtm.isSwitchButtonOn())
			return; //Query template is disabled; return without trying to cache

		if (qtm.isTriggersRegistered()){
			// The triggers associated with the query have already been
			// registered.
			// Insert ki-vi with its time stamp set to Tmiss.
			putInCache(sql, ds, fragid, Tmiss, rs, ParamQry);

			//Register stats:  Increment the number of query instances for this template
			if (KosarSoloDriver.webServer != null){
				qtm = QueryToTrigger.TriggerCache.get(ParamQry);
				qtm.addNumQueryInstances();
			}

		} else {
			if (!qtm.isTrigsInProgress()){
				regthread.AddQry(sql); //Add the query to be registered
				qtm.setTrigsInProgress(true);  //Mark the query template so that it is not inserted over and over again
			}
		}

		return;
	}

	public kosar(String url, Properties arg1, java.sql.Connection conn) {
		// Initialize kosar's connection to the RDBMS
		db_conn = conn;
		// start the thread to register triggers
		for (int i = 0; i < NumFragments; i++) {
			RS[i] = new ConcurrentHashMap<String, dust>();
			if (RS[i] == null) {
				System.out
				.println("KosarSolo:  Failed to initialize a hashmap for (IT, key).");
				return;
			}
		}

		for (int i = 0; i < NumFragments; i++) {
			ITs[i] = new ConcurrentHashMap<String, InternalTokenElement>();
			if (ITs[i] == null) {
				System.out
				.println("KosarSolo:  Failed to initialize a hashmap for ITs.");
				return;
			}
		}

		switch (QueryToTrigger.getTargetSystem()) {
		case MySQL:
			qt = new MySQLQueryToTrigger();
			break;
		case Oracle:
			qt = new OracleQueryToTrigger();
			break;
		default: 
			System.out.println("Error in kosar constructor:  RDBMS type is unknown");
			System.out.println("Check KosarSoloDriver to verify it reads the RDBMS type from the configuration file and sets the type of the RDBMS");
			System.out.println("Aboring, cannot proceed forward");
			System.exit(-1);
			break;
		}

		if (qt == null) {
			System.out
			.println("KosarSolo:  Failed to obtain a QueryToTrigger() instance:  Expect stale data.");
			return;
		}

		/*if (KosarSoloDriver.getFlags().coordinatorExists()) {
			synchronized (QueryToTrigger.class) {
				if (QueryToTrigger.getIPport() == null
						|| QueryToTrigger.getIPport().equals("")) {
					new TriggerListener().setIpPortAddress();
				}
			}

		} else {*/
		for (int j = 0; j < NumTriggerListenerThreads; j++) {
			TriggerListener TL = new TriggerListener(this); // Start the
			// registration
			// background
			// thread
			new Thread(TL, "KosarTrigListener" + j).start();
			try {
				StartTrigListeners.acquire(); // Wait for the
				// TriggerListeners to start
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//}

		synchronized (KosarMonitor) {
			if (!KosarMonitor.isAlive())
				KosarMonitor.start();
		}

		return;
	}

	public void putInCache(String sql, dust ds, int fragid, long Tmiss, ResultSet rs, String QueryTemplate) {

		long TC = System.currentTimeMillis();
		// If (TC-Tmiss > Delta) then ignore the put operation
		if ((TC - Tmiss) >= Delta) {
			Tadjust = System.currentTimeMillis();
			Delta = inflation * (TC - Tmiss);
			return;
		}

		// If (Tmiss < Tadjust) then ignore the put operation.
		if (Tmiss <= Tadjust) {
			return;
		}

		//Get Internal keys
		StringBuffer COSARKey = new StringBuffer();
		qt.GetKey(sql, COSARKey, db_conn);

		/*
		 * Iterates over internal_key_list for the tokens.
		 * Goes into ITs, finds the matching tokens, and checks if they have
		 * gumball.  
		 */
		String tokenList[] = COSARKey.toString().trim().split(" ");
		
		//System.out.println("Number of elts in tokenList =" + tokenList.length);
		
		if (verbose){
			System.out.println("qry="+sql+", "+tokenList.length+" tokens");
			for (int i=0; i < tokenList.length; i++){
				System.out.println("\t Token="+tokenList[i]);
			}
		}
		
		for (int i=0; i < tokenList.length; i++) {
			String token = tokenList[i];
			//for(String token : internal_key_list) {
			InternalTokenElement ite = ((ConcurrentHashMap<String, InternalTokenElement>) ITs[this.getFragment(token)])
					.get(token);
			// If (gi exists and Tmiss is before Tgi) then ignore the put operation
			if (ite != null && ite.isGumball() && Tmiss <= ite.getGumballTS()) {
				return;
			}
			// If (vi exists and its time stamp is after Tmiss) then ignore the put
			// operation.
			if (ite != null && !ite.isGumball() && ds != null && Tmiss <= ds.getLastWrite()) {
				return;
			}
		}

		if (ds == null)
			ds = new dust();
		ds.setKey(sql);
		ds.setRS(rs);
		ds.setLastWrite(Tmiss);
		ds.setQueryTemplate(QueryTemplate);
		// Associate internal keys with the query string

		//Try to insert in the cache.  If there is insufficient memory then do not cache.
		if (!RP.InsertKV(ds, fragid)) return;

		InternalTokenElement placeHolderITE;
		ConcurrentHashMap<String, String> uniqueKeyCheck = new ConcurrentHashMap<String, String>();
		/*
		 * Iterate through internal key list
		 */
		for (int i=0; i < tokenList.length; i++) {

			String token = tokenList[i];

			placeHolderITE = ((ConcurrentHashMap<String, InternalTokenElement>) ITs[this
			                                                                        .getFragment(token)])
			                                                                        .get(token);

			if (placeHolderITE == null)
				placeHolderITE = new InternalTokenElement(); //create new ITE and new map

			placeHolderITE.getQueryStringMap().put(sql, "1");
			uniqueKeyCheck.put(token, "1");  //if next key (sql query string) is same, skip it.

			((ConcurrentHashMap<String, InternalTokenElement>) ITs[this
			                                                       .getFragment(token)])
			                                                       .put(token, placeHolderITE);
			//}
			if (verbose)
				System.out.println("Caching IT="+token+", qry="+sql);
		}

		KosarMonitor.IncrementNumKeyValues();
		// Place the query and its result in the hash table.
		((ConcurrentHashMap<String, dust>) RS[fragid]).put(sql,
				ds);
		//Maintain this query instance as a cached entry for its template
		if (ds == null) System.out.println("Error:  ds is NULL");
		if (ds.getQueryTemplate() == null) System.out.println("Error:  ds method QueryTemplate returned NULL!");
		else {
			QTmeta qtelt = QueryToTrigger.TriggerCache.get( ds.getQueryTemplate() );
			if (qtelt != null) qtelt.setQInstances(sql, ds);
			else System.out.println("Error, qtelt is null");
		}

	}
	
	/**
	 * This getInternalTokensFromQry must be replaced with the GetKey method of QueryToTrigger for consistency across all SQL RDBMSs.
	 * @param qry
	 * @return
	 */

	public Vector<String> getInternalTokensFromQry(String qry) {
		/******  Get Internal Keylist ******/
		String ParamQry = qt.TokenizeWhereClause(qry);
		if (ParamQry == null)
			ParamQry = qry; // qry has no where clause

		Vector<String> internal_key_list = new Vector<String>();
		Vector<String> trgr = new Vector<String>();

		qt.TransformQuery(qry, trgr, internal_key_list, regthread.db_conn);
		/***********************************/


		return internal_key_list;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
