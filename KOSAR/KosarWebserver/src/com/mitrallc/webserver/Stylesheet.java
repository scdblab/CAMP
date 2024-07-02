package com.mitrallc.webserver;
public class Stylesheet {
    public String content(){
        return "<style>"+
"html,body {"+
"margin:0;"+
"padding:0;"+
"min-height:600px;"+
"height:auto;"+
"position:relative;"+
"bottom:0;"+
"right:0;"+
"overflow:auto;"+
"}"+

"div#container{"+
    "margin:0;"+
    "padding:0;"+ 
    "min-width:900px;"+
    "overflow:auto;"+
	"min-height:100%;"+
	"height:auto;"+
"}"+
"div #banner h1{"+
"text-transform:uppercase;"+
"color:#FFFFFF;"+
"font-size:40px;"+
"text-align:center;"+
"margin:0;"+
"}"+

"div #banner{"+
"line-height:100px;"+
"height: 100px;"+
"background-color:#060628;"+
"}"+

"div #menu{"+
"font-family:\"Times New Roman\", Times, serif;"+
"background-color:#060628;"+
"float:left;"+
"width:200px;"+
"}"+
"div #menu ul {"+
    "list-style-type: none;"+
    "margin: 0;"+
    "padding: 0;"+
    "display:inline-block;"+
"}"+

    "div #menu ul li {"+
        "display:inline-block;"+
        "width:100%;"+
    "}"+
    "div #menu a:link, a:visited {"+
    "text-align: center;"+
    "color: #FFFFFF;"+
    "text-decoration: none;"+
    "display: block;"+
    "padding: 10px 10px 10px 10px;"+
    "font-family: 'Open Sans', sans-serif;"+
    "margin: 0;"+
    "font-weight: bold;"+
    "font-size: 14px;"+
    "position: relative;"+
"}"+
"div #menu a:hover, a:active {"+
"background-color: #81BEF7"+
"}"+

"div #main{"+
"height:100%;"+
"margin-left:45px;"+
"float:left;"+
"clear:right;"+
"margin-bottom:100px;"+
"width:60%;"+
"margin-top:20px;"+
"}"+

"div#maincontents{"+
"margin-bottom:20px;"+
"}"+
"div.footer{"+
"font-size:11px;"+
"height:55px;"+
"position:absolute;"+
"bottom:0;"+
"left:0;"+
"width:100%;"+
"padding:0;"+
"clear:both;"+
"text-align:center;"+
"margin-top:50px;}"+

".submit{"+
"text-align:center;"+
"}"+
"div#content{"+
"overflow:auto;"+
"height:auto;"+
"min-height:100%;"+
"}"+

"h1 {"+
"font-size:40px;"+
"text-align:center;"+
"margin:0;"+
"}"+

"td.actionform{"+
"text-align:center;"+
"}"+

"#queryform {"+
"font-size:14px;"+
"}"+
".align{"+
"font-size:13px;"+
"text-align:justify;"+
"}"+
"p.queryalign  {"+
"text-align:justify;"+
"display:inline;"+
"}"+
"div.queryalign{"+
"margin-left:5px;"+
"}"+
"table{"+
"width:100%;"+
"}"+
".space{"+
"clear:both;"+

"}</style>";
        
    }

}