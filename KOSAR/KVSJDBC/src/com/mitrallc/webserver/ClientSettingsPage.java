package com.mitrallc.webserver;

import com.mitrallc.kosar.kosar;
import com.mitrallc.sql.KosarSoloDriver;

public class ClientSettingsPage extends BaseSettings{
	
	public boolean setKosarOnOff(String NewVal){
		boolean kosarOnOff = super.setKosarOnOff(NewVal);
		doSetKosar(kosarOnOff);
		return kosarOnOff;
	}

	private void doSetKosar(boolean kosarOnOrOff) {
		//True is on, false is off
		KosarSoloDriver.kosarEnabled = kosarOnOrOff;
	}

	public void doResetKosar() {
		KosarSoloDriver.kosarEnabled = false;
		kosar.clearCache();
		KosarSoloDriver.kosarEnabled = true;
	}
	
	public String getOnOff(){
		if(KosarSoloDriver.kosarEnabled==true)
			setKosarOnOff("OFF");
		else
			setKosarOnOff("ON");
		return super.getOnOff();
	}
}
