package com.camp.performance;

import java.util.Date;
import java.text.*;

public class PerformanceLogData {

	public final String DateFormat = "MM/dd/yyyy HH:mm:ss.SSS";
	
	Date date;
	Double CPU;
	
	public PerformanceLogData(String line){
		
		String [] splitLine = line.split(",");
		String dateStr = splitLine[0];
		String doubleStr = splitLine[1];
		
		dateStr = dateStr.replace("\"", "");
		doubleStr = doubleStr.replace("\"", "");
		
		DateFormat df = new SimpleDateFormat(DateFormat);
		try {
			date =  df.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}  
		
		CPU = Double.parseDouble(doubleStr);
		
		
	}
}
