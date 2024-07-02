package com.mitrallc.webserver;

import java.util.Enumeration;

import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;

public class ClientSQLPage extends BaseSQL {
	public void getSQLStats() {
		int counter = 0;
		
		int TotalReqPerGranularity = 0;
		int TotalReq = 0;

		Enumeration<QTmeta> eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();){
			QTmeta qtelt = e.nextElement();
			TotalReqPerGranularity += qtelt.getKVSHitsMoving();
			TotalReq += qtelt.getKVSHits();

		}
		
		eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();counter++){
			QTmeta qtelt = e.nextElement();
			sqlStats += "<dl>"+
					qtelt.getQueryTemplate() +
					"</dt>"+
					"<dd>- Number of instances in the KVS: "+qtelt.getNumQueryInstances()+"</dd>"+
					//"<dd>- Executed by the RDBMS 4</dd>"+
					"<dd>- "+ClientMainpage.FormatInts(qtelt.getKVSHitsMoving())+" processed by the KVS in "+ClientSettingsPage.getGranularity()+" seconds (Total="+ClientMainpage.FormatInts(qtelt.getKVSHits())+")</dd>"+
					"<dd>- "+ClientMainpage.FormatDouble(ClientMainpage.ComputeRatioMax1((double) qtelt.getKVSHitsMoving() , (double) TotalReqPerGranularity))+"% of queries processed by the KVS in "+ClientSettingsPage.getGranularity()+" seconds (Total="+ClientMainpage.FormatDouble(ClientMainpage.ComputeRatioMax1((double) qtelt.getKVSHits() , (double) TotalReq))+"%)</dd>"+
					"</dl>";
		}
	}
}
