package com.mitrallc.webserver;

//import com.mitrallc.sql.KosarSoloDriver;

public class BaseLast100Queries extends Webpage{
	
	String queryList = "";
	public void getLast100QueryList() {
		
	}
	public String content(){
		Stylesheet s = new Stylesheet();
		return "<!doctype html>" +
		"<html>"+
		"<head>"+
		s.content()+
		"<meta charset=\"UTF-8\">"+
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
				    getMenuString()+
				        "</ul>"+
			    "</div><!--menu-->"+
			    "<div id=\"main\">"+
				    "<div id=\"maincontents\">"+
					    "<h2>Queries</h2>"+
					    queryList +
					    "</div><!--maincontents-->"+
					    "</div><!--main-->"+
						"<br class=\"space\"/>"+
			        "</div><!--content-->"+
					"<div class=\"footer\"><p>&copy 2014 by Mitra LLC</p></div>"+
				"</div><!--container-->"+
				"</body>"+
				"</html>";
				}
			}