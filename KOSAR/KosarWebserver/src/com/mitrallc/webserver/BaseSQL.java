package com.mitrallc.webserver;

public class BaseSQL extends Webpage {

	String sqlStats = "";
	public void getSQLStats(){
		
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
		"<ul>"+ getMenuString()+
		"</ul>"+
		"</div><!--menu-->"+
		"<div id=\"main\">"+
		"<div id=\"maincontents\">"+
		"<h2>Query Template Statistics</h2>"+

			sqlStats +
			
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

