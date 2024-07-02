package com.camp.performance;

import java.io.*;
import java.util.ArrayList;

public class PerformanceSplitter {

	public final String PerformanceLog = "C:\\PerfLogs\\Admin\\CAMP\\sys_log.csv";
	public final String ExperimentLog = "C:\\KOSAR\\TimerLog\\ExperimentTimes.csv";
	public final String SplitLog = "C:\\KOSAR\\TimerLog\\SplitLog.csv";
	
	public final String DateFormat = "MM/dd/yyyy HH:mm:ss.SSS";
	
	ArrayList<PerformanceLogData> performanceLogData = new ArrayList<PerformanceLogData>();
	ArrayList<ExperimentLogData> experimentLogData = new ArrayList<ExperimentLogData>();
	
	public void LoadPerformanceData(){
		try{
			BufferedReader br = new BufferedReader(new FileReader(ExperimentLog));
			String line;
			while ((line = br.readLine()) != null) {	
					experimentLogData.add(new ExperimentLogData(line));
			}

			br.close();
		}
		catch(Exception e){

		}
	}

	public void LoadExpermentLog(){
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(PerformanceLog));
			String line;
			int counter = 0;
			while ((line = br.readLine()) != null) {	
				if(counter > 1)
					performanceLogData.add(new PerformanceLogData(line));
				counter++;
			}

			br.close();
		}
		catch(Exception e){

		}
		
		
	}
	
	public void PrintCPULog(){
		PrintWriter writer = null;
		try {
			 writer = new PrintWriter(SplitLog);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		writer.println("Filename,Cache Percent, Precision, Num threads, Probability, AVG CPU");
		String template = "%s,%f,%d,%d,%f,%f";
		
		int previousPrecision = 0;
		int previousNumThread = 0;
		for(ExperimentLogData eld: experimentLogData){
			if(eld.precision < previousPrecision || eld.numThreads < previousNumThread)
				writer.println();
			
			String line = String.format(template, eld.filename, eld.cachePercentage,
					eld.precision, eld.numThreads, eld.probability, eld.averageCPU);
			
			previousPrecision = eld.precision;
			previousNumThread = eld.numThreads;
			
			writer.println(line);	
		}
		
		writer.flush();
		writer.close();
	}
	
	public void SplitLog(){

		this.LoadPerformanceData();
		this.LoadExpermentLog();
		
		for(int j = 0; j < experimentLogData.size(); j++){
			ExperimentLogData eld = experimentLogData.get(j);
		
			int counter = 0;
			double CPU = 0;
			
			for(int i = 0; i < performanceLogData.size(); i++){
				PerformanceLogData pld = performanceLogData.get(i);
			
				if(pld.date.before(eld.startTime)){
						performanceLogData.remove(i);
						i--;
				}
				else if(pld.date.after(eld.endTime)){
					break;
				}
				else{
					eld.aggregateCPU += pld.CPU;
					eld.numDataPts++;
				}
			}		
			eld.averageCPU = eld.aggregateCPU / eld.numDataPts;		
		}
		
		PrintCPULog();
		
		System.out.println("Hey");
	}

	public static void main(String[] args) {

		PerformanceSplitter splitter = new PerformanceSplitter();

		splitter.SplitLog();


	}


}
