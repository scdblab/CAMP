package com.mitrallc.webserver;

import java.util.Enumeration;

import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;

public class ClientCachedPage extends Webpage {
	private boolean verbose = false;
	public String GenScriptForButtonOff(){
		int counter = 0;
		String body = "<script>"+
				"function uncheckRadio(obj){"+
				"switch(obj.id){";

		Enumeration<QTmeta> eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();counter++){
			QTmeta qtelt = e.nextElement();

			body += "case \""+counter+"\":"+
					"if (obj.checked=true)"+
					"obj.checked=false;"+
					"break;";
		}
		body += "}"+
				"}"+
				"</script>";
		return body;
	}

	public String GenRadioButtons(){
		String mybutton="";
		String result="";
		String tplt=null;
		int counter = 0;
		Enumeration<QTmeta> eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();counter++){
			QTmeta qtelt = e.nextElement();
			if (qtelt == null){
				mybutton += "<td><input type=\"radio\" id=\""+counter+"\" name=\"Error, qtelet is null!\" ondblclick=\"uncheckRadio(this)\"></td>";
			} else if (qtelt.isSwitchButtonOn())
				mybutton = "<td><input type=\"radio\" id=\""+counter+"\" name=\""+qtelt.getQueryTemplate()+"\" ondblclick=\"uncheckRadio(this)\"></td>";
			else 
				mybutton = "<td><input type=\"radio\" id=\""+counter+"\" name=\""+qtelt.getQueryTemplate()+"\" checked=\"checked\" ondblclick=\"uncheckRadio(this)\"></td>";

			if (qtelt != null) tplt = qtelt.getQueryTemplate();
			result += "<tr>"+ 
					mybutton +
					"<td><p class=\"queryalign\">"+tplt+"</p></td>"+
					"</tr>";
		}
		if (counter == 0)
			return "<tr>"+
			"<td><input type=\"radio\" id=\""+counter+"\" name=\"No query templates\" ondblclick=\"uncheckRadio(this)\"></td>"+
			"<td><p class=\"queryalign\">No query templates</p></td>"+
			"</tr>";

		return result;
	}

	public String content(){
		Stylesheet s = new Stylesheet();
		if (verbose) System.out.println("Inside content() method of cachedStatic");
		return "<!doctype html>" +
		"<html>"+
		"<head>"+
		s.content()+
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
		"<fieldset>"+
		"<legend><h2>Cached Query Templates</h2></legend>"+
		"<p>These are the cached query templates.  Select those that should not be cached and click Submit.</p>"+
		"<div id=\"queryform\">"+
		"<form name=\"cachedForm\" action=\"\" method=\"POST\" onsubmit=\"selectedQueries()\">"+
		"<table>"+
		GenRadioButtons()+ 
		"<td colspan=\"2\" class=\"submit\"><input id=\"button\" type=\"submit\" value=\"Submit\"></td>"+
		"</tr>"+
		"</table>"+
		"</form>"+
		"</div><!--queryform-->"+
		"</fieldset>"+
		   "</div><!--maincontents-->"+
		    "</div><!--main-->"+
		"<br class=\"space\" />"+
		"</div><!--content-->"+
		"<div class=\"footer\"><p>&copy 2014 by Mitra LLC</p></div>"+
	"</div><!--container-->"+
		
		GenScriptForButtonOff()+

		"</body>"+
		"</html>";
	}
}