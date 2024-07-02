package com.camp.simulator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PerformanceTimer {

	public final String output = "C:\\KOSAR\\TimerLog\\ExperimentTimes.csv";
//	public final String output = "/home/shahram/KOSAR/TimerLog/ExperimentTimes.csv";
	
	public final String LineTemplate = "%s,%f,%d,%d,%f,%s";
	
	PrintWriter writer =null;
	
	int NumThreads;
	double PercentCache;
	String TraceFile;
	int Precision;
	double Probability;
	
	public PerformanceTimer(){
		try {
			writer = new PrintWriter(output, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void StartTrackTime(SimulationInput si){
		
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
		String dateString = formatter.format(new java.util.Date());
		
		String line = String.format(this.LineTemplate, si.traceFile, si.cachePercentage, 
				si.numThreads, si.precision, si.insertionProbability, dateString);
		
		writer.print(line);	
	}
	
	public void EndTrackTime(){
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
		String dateString = formatter.format(new java.util.Date());
		
		writer.println(","+ dateString);
		
	}
	public void CloseWriter(){
		writer.flush();
		writer.close();
	}

}
