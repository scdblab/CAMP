package com.mitrallc.webserver;

import java.text.NumberFormat;
import java.util.Locale;

public class BaseMainpage extends Webpage{
	
	public static String FormatInts(int input){
		if (input < 0) input = 1000;
		return NumberFormat.getNumberInstance(Locale.US).format(input);
	}
	
	public long ConvertToKB(long memsizebytes){
		return (memsizebytes/1024);
	}
	public long ConvertToMB(long memsizebytes){
		return (memsizebytes/(1024*1024));
	}
	public long ConvertToGB(long memsizebytes){
		return (memsizebytes/(1024*1024*1024));
	}
	public String FormatMemory(long memsizebytes){
		String results = "";
		switch(BaseSettings.BoxVal){
		case "Bytes": results=""+memsizebytes; break;
		case "KBytes": results=""+ConvertToKB(memsizebytes); break;
		case "MBytes": results=""+ConvertToMB(memsizebytes); break;
		case "GBytes": results=""+ConvertToGB(memsizebytes); break;
		default: break;
		} 
		return results+" "+BaseSettings.getBoxVal();
	}
	public String content(){
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
			"<dl id=\"DBMS\">"+
				"<dt><strong>KOSAR</strong><dt>"+
					getJVMStatString()+
					getJDBCStatString()+
			"</dl>"+
			"<dl id=\"DBMS\">"+
				getQueryResponseStatString()+
			"</dl>"+
			"</div><!--maincontents-->"+
			"</div><!--main-->"+
			"<br class=\"space\"/>"+
				"</div><!--content-->"+
				"<div class=\"footer\"><p>&copy 2014 by Mitra LLC</p></div>"+
		"</div><!--container-->"+
"</body>"+
"</html>";	
	}
	
	public String getJVMStatString() {
		return "<dd>- Maximum JVM memory: "+FormatMemory(Runtime.getRuntime().maxMemory())+"</dd>"+
				"<dd>- Used JVM memory: "+FormatMemory( Runtime.getRuntime().totalMemory())+"</dd>";
	}
	public String getJDBCStatString() {
		return "";
	}
	public String getQueryResponseStatString() {
		return "";
	}
}
