package com.mitrallc.mysqltrig;

import java.sql.Connection;
import java.util.Vector;

public class TableInfo {
	String TableName="";
	Vector<String> VRegTrig=new Vector<String>();
	
	public String getTableName() {
		return TableName;
	}
	public void setTableName(String tableName) {
		TableName = tableName;
	}
	public Vector<String> getVRegTrig() {
		return VRegTrig;
	}
	
	public void addVRegTrig(String cmd) {
		//If trigger has already been registered then there is nothing to do
		if (IndexOfTrigger(cmd) >=0 ) return ;
		
		VRegTrig.add(cmd);
	}
	
	public int IndexOfTrigger(String cmd){
		//Traverse the list of triggers in the VRegTrig vector to 
		//see if any is of type of the trigger that must be registered:  
		//Obtain the type of each trigger using mysqlTrigGenerator.WhatIsTriggerType 
		//and see if it equals the type of trigger that is being registered. 
		int returnval=-1;
		MySQLQueryToTrigger.TriggerType newtrigtype;
		MySQLQueryToTrigger.TriggerType VRegelementtpye;
		//Get the type of the incoming trigger
		newtrigtype=mysqlTrigGenerator.WhatIsTriggerType(cmd);
		for(int i=0;i<VRegTrig.size();i++){
			VRegelementtpye=mysqlTrigGenerator.WhatIsTriggerType(VRegTrig.elementAt(i));
			if (VRegelementtpye.equals(newtrigtype)){
				//Get the type of this trigger element of VRegTrig
				//Compare it with the type of the incoming trigger cmd
				//If they are equal return true
				return i;
			}
		}
		return returnval;
	}
	public String triggerTypeExists(String cmd){
		//Traverse the list of triggers in the VRegTrig vector to 
		//see if any is of type of the trigger that must be registered:  
		//Obtain the type of each trigger using mysqlTrigGenerator.WhatIsTriggerType 
		//and see if it equals the type of trigger that is being registered. 
		int idx = IndexOfTrigger(cmd);
		if (idx >= 0) return VRegTrig.elementAt(idx).toString();
		return null;
	}
	public void trigremove(String cmd){
		int idx = IndexOfTrigger(cmd);
		VRegTrig.remove(idx);

	}
}