package com.mitrallc.kosar;

import java.util.Vector;

public class query {
	String Qry = null;
	String ParamQry = null;
	Vector<String> trgr = null;
	Vector<String> ITs = null;

	public void setQuery(String k){
		Qry = k;
	}
	
	public String getQuery(){
		return Qry;
	}
	
	public void setParamQuery(String k){
		ParamQry = k;
	}
	
	public String getParamQuery(){
		return ParamQry;
	}
	
	public void setTriggers(Vector<String> k){
		trgr = k;
	}
	
	public Vector<String> getTriggers(){
		return trgr;
	}
	
	public void setInternalKeys(Vector<String> k){
		ITs = k;
	}
	
	public Vector<String> getInternalKeys(){
		return ITs;
	}
}
