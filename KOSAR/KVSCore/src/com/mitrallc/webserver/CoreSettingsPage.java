package com.mitrallc.webserver;

import com.mitrallc.core.KosarCore;



public class CoreSettingsPage extends BaseSettings{
	public String GetOptions(int numclients, int InputSelectedOption){
		String res="";
		int SelectedOption=InputSelectedOption;
		if (InputSelectedOption > numclients) SelectedOption = numclients;
		if (SelectedOption == 0) res += "<option value=\"0\" selected=\"selected\">No cooperation</option>";
		else res += "<option value=\"0\">No cooperation</option>";
		
		for (int i=1; i < numclients+1; i++){
			if (SelectedOption == i) res += "<option value=\""+i+"\" selected=\"selected\">Cooperate with "+i+" replica</option>";
			else res += "<option value=\""+i+"\">Cooperate with "+i+" replica</option>";
		}
		if (SelectedOption == -1) res += "<option value=\"-1\" selected=\"selected\">Full cooperation</option>";
		else res += "<option value=\"-1\">Full cooperation</option>";
		return res;
	}
	
	public void setReplicationString() {
		replicationString = "<tr><td><p>Degree of replication: <select name=\"cooperation\">"+
				GetOptions(KosarCore.clientToIPMap.size(), KosarCore.getNumReplicas())+
			"</select></p></td></tr>";

	}
	public void setNumReplicas(int numReplicas) {
		KosarCore.setNumReplicas(numReplicas);
	}
	
}
