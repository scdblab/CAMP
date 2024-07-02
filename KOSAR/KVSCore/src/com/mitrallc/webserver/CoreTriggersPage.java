package com.mitrallc.webserver;

import java.util.Enumeration;
import java.util.Vector;

import com.mitrallc.core.KosarCore;
import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;

public class CoreTriggersPage extends BaseTriggers{
	public void getRegisteredTriggers() {
		title = "Triggers";
		registeredTriggers = "";
		for(String trigger : KosarCore.triggerMap.keySet()) {
			String type = "";
			if(trigger.contains("INSERT"))
				type = "Insert";
			else if(trigger.contains("UPDATE"))
				type = "Update";
			else if(trigger.contains("DELETE"))
				type = "Delete";
			registeredTriggers += "<b>"+type+" trigger</b>" +
					"<p class=\"align\">" +
					trigger + "</p>";
		}
	}
}
