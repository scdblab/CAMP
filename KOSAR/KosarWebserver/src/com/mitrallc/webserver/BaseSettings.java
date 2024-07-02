package com.mitrallc.webserver;

public class BaseSettings extends Webpage{
	static int RefreshVal = 30; //Granularity is always in seconds
	static int SlidingWindow=60;       //Granularity is always in seconds
	public static int Granularity=60;         //Graunularity is always in seconds
	static String BoxVal="Bytes";
	static String optionVal="nothing";
	String KosarOnOff="ON";
	String onOfftext="off";
	String Color="green";
	String replicationString = "";
	public void setReplicationString() {

	}
	public static int getRefreshVal(){
		return RefreshVal;
	}
	public static int getGranularity(){
		return Granularity;
	}
	public static String getBoxVal(){
		return BoxVal;
	}
	public static String getoptionVal(){
		return optionVal;
	}
	public void setSlidingWindowVal(int NewVal){
		if (NewVal <= 0) NewVal=5;
		SlidingWindow= NewVal;
		return;
	} 
	public void setGranularity(int NewVal){
		if (NewVal <= 0) NewVal=5;
		Granularity=NewVal;
		EventMonitor.setGranularityInSeconds(NewVal);
		return;
	}
	public void setRefreshVal(int NewVal){
		if (NewVal <= 0) NewVal=5;
		RefreshVal = NewVal;
		return;
	}
	public void setBox(String NewVal){
		BoxVal = NewVal;
		return;
	}
	public void setoptionVal(String NewVal){
		optionVal=NewVal;
		return;
	}
	public void setNumReplicas(int value) {
		
	}
	
	public String getOnOff(){
		return "<div id=\"actiondiv\">"+	
				"<fieldset>"+
	"<legend><h2>Actions</h2></legend>"+
	"<table class=\"actionform\">"+
	"<form  name=\"actionform1\" action=\"\" method=\"post\" onSubmit=\"return warning1()\">"+
	"<tr>"+
	"<td>KOSAR</td>"+
	"<td><input type=\"submit\" style=\"background-color:"+Color+"; padding-left:19px; padding-right:19px;\" id=\"kosarOnOff\"  name=\"kosarOnOff\" value=\""+KosarOnOff+"\" >(Click to turn "+onOfftext+")</td>"+
	"</tr>"+
	"</form>"+
	"<form name=\"actionform2\" action=\"\" method=\"post\" onSubmit=\"return warning2()\">"+
                            "<tr>"+
                            "<td>KVS</td>"+
                            "<td><input id=\"kvsFlush\" type=\"submit\" name=\"kvsFlush\" value=\"FLUSH\" >(Click to flush KVS)</td>"+   
                            "</tr>"+
                            "</form>"+
                            "</table>"+
                            "</fieldset>"+
			"</div><!--actiondiv-->";
	}
	public boolean setKosarOnOff(String NewVal){
		KosarOnOff=NewVal;
		boolean kosarOn = true;
		if (KosarOnOff.equalsIgnoreCase("ON")){
			onOfftext="on";
			KosarOnOff="OFF";
			Color="red";
			kosarOn = false;
		}
		else {
			onOfftext="off";
			KosarOnOff="ON";
			Color="green";
			kosarOn = true;
		}
		return kosarOn;
	}

	void doResetKosar() {
		//Overridden in ClientSettingsPage
	}
	
	public String content(){
	Stylesheet s = new Stylesheet();
	
	String checkbox = null;
	switch(BoxVal){
	case "Bytes": checkbox = "<td><p>Granularity of memory:<input id=\"box_1\"type=\"checkbox\" name=\"boxBytes\" value=\"Bytes\" onclick=\"boxcheck(this)\" checked> Bytes<input id=\"box_2\"type=\"checkbox\" name=\"boxKBytes\" value=\"KBytes\"onclick=\"boxcheck(this)\"> KBytes <input id=\"box_3\"type=\"checkbox\" name=\"boxMBytes\" value=\"MBytes\" onclick=\"boxcheck(this)\"> MBytes <input id=\"box_4\"type=\"checkbox\" name=\"boxGBytes\" value=\"GBytes\" onclick=\"boxcheck(this)\"> GBytes</p>";
	break;
	case "KBytes": checkbox = "<td><p>Granularity of memory:<input id=\"box_1\"type=\"checkbox\" name=\"boxBytes\" value=\"Bytes\" onclick=\"boxcheck(this)\" > Bytes<input id=\"box_2\"type=\"checkbox\" name=\"boxKBytes\" value=\"KBytes\"onclick=\"boxcheck(this)\" checked> KBytes <input id=\"box_3\"type=\"checkbox\" name=\"boxMBytes\" value=\"MBytes\" onclick=\"boxcheck(this)\"> MBytes <input id=\"box_4\"type=\"checkbox\" name=\"boxGBytes\" value=\"GBytes\" onclick=\"boxcheck(this)\"> GBytes</p>";
	break;
	case "MBytes": checkbox = "<td><p>Granularity of memory:<input id=\"box_1\"type=\"checkbox\" name=\"boxBytes\" value=\"Bytes\" onclick=\"boxcheck(this)\" > Bytes<input id=\"box_2\"type=\"checkbox\" name=\"boxKBytes\" value=\"KBytes\"onclick=\"boxcheck(this)\"> KBytes <input id=\"box_3\"type=\"checkbox\" name=\"boxMBytes\" value=\"MBytes\" onclick=\"boxcheck(this)\" checked> MBytes <input id=\"box_4\"type=\"checkbox\" name=\"boxGBytes\" value=\"GBytes\" onclick=\"boxcheck(this)\"> GBytes</p>";
	break;
	case "GBytes": checkbox = "<td><p>Granularity of memory:<input id=\"box_1\"type=\"checkbox\" name=\"boxBytes\" value=\"Bytes\" onclick=\"boxcheck(this)\" > Bytes<input id=\"box_2\"type=\"checkbox\" name=\"boxKBytes\" value=\"KBytes\"onclick=\"boxcheck(this)\"> KBytes <input id=\"box_3\"type=\"checkbox\" name=\"boxMBytes\" value=\"MBytes\" onclick=\"boxcheck(this)\"> MBytes <input id=\"box_4\"type=\"checkbox\" name=\"boxGBytes\" value=\"GBytes\" onclick=\"boxcheck(this)\" checked> GBytes</p>";
	break;
	default: checkbox = "<td><p>Granularity of memory:<input id=\"box_1\"type=\"checkbox\" name=\"boxBytes\" value=\"Bytes\" onclick=\"boxcheck(this)\" > Bytes<input id=\"box_2\"type=\"checkbox\" name=\"boxKBytes\" value=\"KBytes\"onclick=\"boxcheck(this)\"> KBytes <input id=\"box_3\"type=\"checkbox\" name=\"boxMBytes\" value=\"MBytes\" onclick=\"boxcheck(this)\"> MBytes <input id=\"box_4\"type=\"checkbox\" name=\"boxGBytes\" value=\"GBytes\" onclick=\"boxcheck(this)\"> GBytes</p>";
	break;
	}
	
	
	return "<!doctype html>" +
			"<html>"+
			"<head>"+
			s.content()+"<!doctype html>"+
		"<meta charset=\"UTF-8\">"+
		"<title>KOSAR</title>"+
	"</head>"+
	"<body>"+
	"<div id=\"container\">"+
			"<div id=\"banner\">"+
				"<h1>kosar " + name + "</h1>"+
			"</div>"+
			"<div id=\"content\">"+
			"<div id=\"menu\">"+
				"<ul>"+getMenuString()+
				"</ul>"+
			"</div>"+
			"<div id=\"main\">"+
				"<div id=\"maincontents\">"+
					"<fieldset>"+
					"<legend><h2>Display Settings</h2></legend>"+
					"<form name=\"memoryform\" action=\"\" method=\"post\">"+
					"<table>"+
		"<tr>"+
		
			"<td> <p>Refresh every <input type=\"text\" size=\"1\"name=\"refresh\" value=\""+this.getRefreshVal()+"\"> seconds</p>"+
			"</td>"+
		"</tr>"+
		"<tr>"+ checkbox +
			"</td>"+
		"</tr>"+
		"<tr><td><p>Interval of time for the reported hits/percentage of queries:<input type=\"text\" size=\"1\" name=\"rp-hitrate\" value=\""+this.getGranularity()+"\"> seconds</p></td></tr>" +
		replicationString
		+"<tr><td class=\"submit\"><input type=\"submit\" value=\"Submit\"></td></tr>"+
						"</table>"+
				"</form>"+
				"</fieldset>"+
	    	getOnOff()+
		"</div><!--maincontents-->"+
                                        "</div><!--main-->"+    
                                        "<br class=\"space\"/>"+
                                        "</div><!--content-->"+
                                    	"<div class=\"footer\"><p>&copy 2014 by Mitra LLC</p></div>"+
                                        "<script>"+
                                        "function warning1(){"+
                                        "kosarVal= document.forms[\"actionform1\"][\"kosarOnOff\"].value;"+
                                        "if (kosarVal==\"ON\"){"+
                                        "if(confirm (\"Are you sure? (Performance will degrade severely.)\") ==true)"+
                                            "return true;"+
    										
    										" else return false;}"+                             
                                        "if (kosarVal==\"OFF\")"+
                                        "return true;"+	
										"}"+
			"</script>"+
	"<script>"+
                                        "function warning2(){"+
										"kvsval=document.forms[\"actionform2\"][\"kvsFlush\"].value;"+
										"if (kvsval==\"FLUSH\"){"+
										"if(confirm (\"Are you sure? (Performance degrades severely.)\") ==true)"+
                                        "return true;"+
										"else return false;}"+
										"}"+
			"</script>"+

	"<script>"+
		"function boxcheck(obj){"+
		"switch(obj.value){"+
		"case \"Bytes\":"+
		"document.getElementById(\"box_1\").checked=true;"+
		 "document.getElementById(\"box_2\").checked=false;"+
            "document.getElementById(\"box_3\").checked=false;"+
            "document.getElementById(\"box_4\").checked=false;"+
			
		"break;"+
		"case \"KBytes\":"+
		"document.getElementById(\"box_2\").checked=true;"+
		"document.getElementById(\"box_1\").checked=false;"+
            "document.getElementById(\"box_3\").checked=false;"+
            "document.getElementById(\"box_4\").checked=false;"+
			
			
		"break;"+
		"case \"MBytes\":"+
		"document.getElementById(\"box_3\").checked=true;"+
		"document.getElementById(\"box_1\").checked=false;"+
            "document.getElementById(\"box_2\").checked=false;"+
            "document.getElementById(\"box_4\").checked=false;"+
		
		"break;"+
		"case \"GBytes\":"+
		"document.getElementById(\"box_4\").checked=true;"+
		 "document.getElementById(\"box_1\").checked=false;"+
            "document.getElementById(\"box_2\").checked=false;"+
            "document.getElementById(\"box_3\").checked=false;"+
			
		"break;"+
		"}"+
		"}"+ 
	"</script>"+
	"</body>"+
	"</html>"
			;
}
}
