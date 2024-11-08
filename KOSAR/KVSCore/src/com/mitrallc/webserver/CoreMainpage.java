package com.mitrallc.webserver;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mitrallc.core.KosarCore;
import com.mitrallc.core.RequestHandler;

public class CoreMainpage extends BaseMainpage{
	public  String content(){
	Stylesheet s = new Stylesheet();
	return "<!doctype html>" +
"<html>"+
"<head>"+
s.content()+
	"<meta charset=\"UTF-8\">"+
	"<meta http-equiv=\"refresh\" content=\""+BaseSettings.getRefreshVal()+"\">"+
	"<title>KOSAR</title>"+
"</head>"+
"<body>"+
	"<div id=\"container\">"+
		"<div id=\"banner\">"+
			"<h1>kosar " + name + "</h1>"+
			"</div><!--banner-->"+
		"<div id=\"content\">"+
		"<div id=\"menu\">"+
			"<ul>"+
				getMenuString() +
				"</ul>"+
				"</div><!--menu-->"+
				"<div id=\"main\">"+
				"<div id=\"maincontents\">"+
			"<h2>Performance Metrics</h2>"+
			"<p>Refresh every "+BaseSettings.getRefreshVal()+" seconds</p>"+
			"<p>Degree of replication: " + getNumReplicas() + "</p>"+
	"<p>Number of clients: " + (KosarCore.clientToIPMap==null ? 0 : KosarCore.clientToIPMap.size()) + "</p>"+
	getJVMStatString()+
	getClientContent()+
    "</div><!--maincontents-->"+
	"</div><!--main-->"+
	"<br class=\"space\"/>"+
		"</div><!--content-->"+
		"<div class=\"footer\"><p>&copy 2014 by Mitra LLC</p></div>"+
"</div><!--container-->"+
"</body>"+
"</html>";
}
	
	 public static int unsignedByteToInt(byte b) {
		    return (int) b & 0xFF;
	 }
	 public String PrintIPAddr(byte[] ipelts){
		 String res = "";
		 for (int i=0; i < ipelts.length; i++){
			 if (res.length() > 0) res += ":";
			 res += unsignedByteToInt(ipelts[i]);
		 }
		 return res;
	 }
	public String getClientContent() {
		String body = "";
		int index = 1;
		for(Object client : KosarCore.clientToIPMap.keySet()) {
			body += "<dl>"+
		    "<dt>Client " + index + 
		    "<dt>"+
		    	"<dd>- ClientID : " + Arrays.toString(((ByteBuffer)client).array()) + "</dd>" +
			    "<dd>- IP address: " + PrintIPAddr (((ByteBuffer)KosarCore.clientToIPMap.get((ByteBuffer)client)).array()) + "</dd>"+
			    "<dd>- Ports: " + getPortsForIP(((ByteBuffer)KosarCore.clientToIPMap.get((ByteBuffer)client)).array()) + "</dd>"+
			    "<dd>- Number of registered triggers: " + KosarCore.triggersRegPerClient.get((ByteBuffer)client) + "</dd>"+
			    "<dd>- Rate of requests/"+ BaseSettings.getGranularity()+ " seconds: " + KosarCore.requestRateEventMonitor.get((ByteBuffer)client).numberOfEventsPerGranularity()+"</dd>"+
			    "<dd>- Number of query instances: "+ KosarCore.numQueryInstances.get((ByteBuffer)client).get()+ "</dd>"+
			"</dl>";
			index++;
		}
		return body;
	}
	public String getNumReplicas() {
		switch(KosarCore.getNumReplicas()) {
		case 0:
			return "No cooperation";
		case -1: 
			if(KosarCore.clientToIPMap != null && KosarCore.clientToIPMap.size()==1)
				return "Cooperation with 1 replica";
			else 
				return "Cooperation with " + KosarCore.clientToIPMap.size() + " replicas";
		case 1:
			return "Cooperation with " + KosarCore.getNumReplicas() + " replica";
		default:
			return "Cooperation with " + KosarCore.getNumReplicas() + " replicas";
		}
	}
	public String getJVMStatString() {
		return "<p>Maximum JVM memory: "+FormatMemory(Runtime.getRuntime().maxMemory())+"</p>"+
				"<p>Used JVM memory: "+FormatMemory( Runtime.getRuntime().totalMemory())+"</p>";
	}
	public String getPortsForIP(byte[] ip) {
		String ports = "";
		for(RequestHandler rh : KosarCore.handlers) {
			if(ByteBuffer.wrap(rh.getSocket().getSocket().getInetAddress().getAddress())
					.equals(ByteBuffer.wrap(ip)))
				if(ports.length() > 0)
					 ports += " , ";
				ports += rh.getSocket().getSocket().getPort();
					
		}
		return ports;
	}
}
