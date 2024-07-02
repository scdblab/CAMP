package com.camp.simulator;


import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.camp.algorithm.*;

public class Simulator extends Thread {

	boolean verbose = true;

	PrintWriter writer;
	CAMP camp;
	int threadNum;
	int totalThreads;

	public long totalRunTime = 0;

	public static int NumIterations;

	public static final boolean UNIQUE = false;

	//Request [] requests;

	Map<Integer, Request[]> mapOfRequests;

	int NumBuckets;

	ConcurrentHashMap<String, Request> itemMap;
	CAMPStats stats = new CAMPStats();

	public Simulator(CAMP camp, PrintWriter writer, Map<Integer, Request[]> requests, int threadNum, int totalThreads, ConcurrentHashMap<String,Request>map, int NumBuckets){
		this.camp = camp;
		this.writer = writer;
		this.mapOfRequests = requests;
		this.threadNum = threadNum;
		this.totalThreads = totalThreads;
		this.itemMap = map;
		this.NumBuckets = NumBuckets;
	}

	public Simulator(String traceFile, long cacheSize, int precision, boolean verbose, 
			String outputFilename){
		this.verbose = verbose;	
	}

	@Override
	public void run() {

		long startTime = System.nanoTime(); 
		for(int j = 0; j < NumIterations; j++){
			for(int k = threadNum; k < NumBuckets; k += this.totalThreads){
				Request [] requests = mapOfRequests.get(k);
				for(int i = 0; i < requests.length; i ++ ){
					if(UNIQUE){
						Object o = itemMap.remove(requests[i].key);
						if(o == null)
							continue;
					}

					camp.CAMP_Handle_Request(requests[i], stats);

					if(UNIQUE){
						itemMap.put(requests[i].key, requests[i]);
					}

					if(requests[i].repeat == false)
						requests[i].repeat = true;
				}
			}
		}
		long endTime = System.nanoTime();
		totalRunTime = (endTime - startTime);

		if(writer != null)
			writer.flush();	
	}

}
