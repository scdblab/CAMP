package com.camp.performance;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExperimentLogData {

	public final String DateFormat = "MM/dd/yyyy HH:mm:ss.SSS";
	
	String filename;
	double cachePercentage;
	int numThreads;
	int precision;
	double probability;
	Date startTime;
	Date endTime;
	
	int numDataPts = 0;
	double aggregateCPU = 0;
	double averageCPU = 0;
	
	public ExperimentLogData(String line){
		
		String [] splitStr = line.split(",");
		
		filename = splitStr[0];
		cachePercentage = Double.parseDouble(splitStr[1]);
		numThreads = Integer.parseInt(splitStr[2]);
		precision = Integer.parseInt(splitStr[3]);
		probability = Double.parseDouble(splitStr[4]);

		
		DateFormat df = new SimpleDateFormat(DateFormat);
		try {
			startTime =  df.parse(splitStr[5]);
		} catch (ParseException e) {
			e.printStackTrace();
		}  
		
		try {
			endTime =  df.parse(splitStr[6]);
		} catch (ParseException e) {
			e.printStackTrace();
		} 
		
	}

}
