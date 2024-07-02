package com.mitrallc.webserver;

import java.util.Enumeration;
import java.util.Vector;

/*import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;*/

public class BaseTriggers extends Webpage{

	String registeredTriggers = "";
	String title = "";
	public void getRegisteredTriggers() {
		
	}
	public void setRegisteredTriggers(String registeredTriggers) {
		this.registeredTriggers = registeredTriggers;
	}
	
	public String content(){
		Stylesheet s = new Stylesheet();
		return
				"<!doctype html>" +
				"<html>"+
					"<head>"+
						s.content()+
						"<meta charset=\"UTF-8\">"+
						"<title>KOSAR</title>"+
					"</head>"+
		"<body>"+
		"<div id=\"container\">"+
		"<div id=\"banner\">"+
		"<h1>kosar " + name+"</h1>"+
		"</div><!--banner-->"+
		"<div id=\"content\">"+    
		"<div id=\"menu\">"+
		"<ul>"+
		getMenuString()+
		"</ul>"+
		"</div><!--menu-->"+
		"<div id=\"main\">"+
		"<div id=\"maincontents\">"+
		"<h2>"+title+"</h2>"+
		"<p class=\"align\">Each SQL query template is followed by its insert, delete, and update trigger.</p>"+		
		registeredTriggers +
		"</div><!--maincontents-->"+
		"</div><!--main-->"+
		"<br class=\"space\"/>"+    
		"</div><!--content-->"+
		"<div class=\"footer\"><p>&copy 2014 by Mitra LLC</p></div>"+
		"</div><!--container-->"+      
		"</body>"+
		"</html>"    
		;
	}
}