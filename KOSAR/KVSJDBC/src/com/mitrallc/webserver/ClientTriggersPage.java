package com.mitrallc.webserver;

import java.util.Enumeration;
import java.util.Vector;

import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;

public class ClientTriggersPage extends BaseTriggers{

	public void getRegisteredTriggers() {
		title = "Queries and Triggers";
		int counter = 0;
		System.out.println("Triggers.java:  Inside KosarClientWebPage");
		if (QueryToTrigger.TriggerCache == null) 
			System.out.println("Triggers.java:  TriggerCache is NULL");
		Enumeration<QTmeta> eqt = QueryToTrigger.TriggerCache.elements();
		
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();counter++){
			QTmeta qtelt = e.nextElement();
			Vector<String> Triggers = qtelt.getTg();
			if (Triggers == null) continue;
			registeredTriggers += "<br><br><p class=\"align\">"+
					"<b>"+qtelt.getQueryTemplate()+"</b>"+
					"</p>";
			for (int i=0; i < Triggers.size(); i++){
				if (Triggers.elementAt(i).indexOf("BEFORE INSERT")> 0){
					registeredTriggers += "<b>Insert trigger</b>";
					registeredTriggers += "<p class=\"align\">";
					registeredTriggers += Triggers.elementAt(i);
					registeredTriggers += "</p>";
				} else if (Triggers.elementAt(i).indexOf("BEFORE DELETE")> 0){
					registeredTriggers += "<b>Delete trigger</b>";
					registeredTriggers += "<p class=\"align\">";
					registeredTriggers += Triggers.elementAt(i);
					registeredTriggers += "</p>";
				} else if (Triggers.elementAt(i).indexOf("BEFORE UPDATE")> 0){
					registeredTriggers += "<b>Update trigger</b>";
					registeredTriggers += "<p class=\"align\">";
					registeredTriggers += Triggers.elementAt(i);
					registeredTriggers += "</p>";
				} else if (Triggers.elementAt(i).indexOf("PROCEDURE") > 0) {
					registeredTriggers += "<b>Procedure</b>";
					registeredTriggers += "<p class=\"align\">";
					registeredTriggers += Triggers.elementAt(i);
					registeredTriggers += "</p>";
				} else {
					registeredTriggers += "<b>Unknown trigger</b>";
					registeredTriggers += "<p class=\"align\">";
					registeredTriggers += Triggers.elementAt(i);
					registeredTriggers += "</p>";
				}
			}
		}
		System.out.println("Triggers.java:  Done with getQueryTempStats");
	}
}
