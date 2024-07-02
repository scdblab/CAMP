package com.camp.simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.camp.algorithm.CAMP;
import com.camp.algorithm.CampRounding;

public class StatsHelper {

	public  int numThreads = 0;
	
	public  int totalHeapAccesses = 0;
	public   int numReqs = 0;
	public   int numHits = 0;
	public   long costNotInCache = 0;
	public   long totalCost = 0;
	public   int numMisses = 0;
	public   int totalNumMisses = 0;
	
	public  int numKeyValues = 0;
	public  int numLRUQueues = 0;
	public  int numHeapElts = 0;
	
	public  long cacheSize = 0;
	public  long cacheThreshold = 0;
	public  long currentCacheUsage = 0;
	
	public  long lookupTime = 0;
	public  long insertTime = 0;
	public  long deleteTime = 0;
	
	public  long totalTime = 0;
	
	public long evictions = 0;
	
	
	public  double GetHitToRequestRatio(){
		
		return ((double)numHits) / numReqs;
		
	}
	
	public  double GetMissToRequestRatio(){
		
		return ((double)numMisses) / numReqs;
		
	}
	
	public  double GetCostToMissRatio(){
		
		return ((float)costNotInCache) / totalCost;
		
	}
	
	 void CompileStats(Simulator simulators [], CAMP camp){	
		
		for(Simulator s: simulators){
			totalHeapAccesses += s.stats.heapAccesses;
			numReqs += s.stats.numReqs;
			numHits += s.stats.numHits;
			costNotInCache += s.stats.costNotInCache;
			totalCost += s.stats.totalCost;
			numMisses += s.stats.numMisses;
			totalNumMisses += s.stats.totalNumMisses;
			totalTime += s.totalRunTime;
			evictions += s.stats.evictions;
		}
		
		numKeyValues = camp.GetNumberOfKeyItems();
		numLRUQueues = camp.LRUHT.size();
		numHeapElts = camp.PQ.size();
		
		cacheSize = camp.cacheSize;
		cacheThreshold = camp.cacheThreshold.get();
		currentCacheUsage = camp.currentCacheUsage.get();
	}
	
	 void PrintStats(String OutputFilename){
		
		String template = "%d,%d,%f,%f,%d,%d,%d,%f,%f,%d";

		double hitToRequestRatio = ((double)numHits) / numReqs;
		double missToRequestRatio = ((double)numMisses) / numReqs;
		double costToMissRatio = ((float)costNotInCache) / totalCost;
		
		double avgTime = (totalTime /  1000000000.0) / numThreads;
		double requestsPerSecond = numReqs / (avgTime);
		
		
		String line = String.format(template, CampRounding.Precision, numThreads,
				GetMissToRequestRatio(), GetCostToMissRatio(), 
				numKeyValues, numLRUQueues, numHeapElts, avgTime, requestsPerSecond, evictions);

		System.out.println(line);

		File file = new File(OutputFilename);

		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
			}
		}

		try{
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(OutputFilename, true)));
			out.println(line);
			out.flush();
			out.close();
		}catch (IOException e) {
			//exception handling left as an exercise for the reader
		}

		System.out.println("Precision, " + CampRounding.Precision);	
		System.out.println("Cache Size, " + cacheSize);
		System.out.println("Threshold Size, " + cacheThreshold);
		System.out.println("Cache Usage, " + currentCacheUsage);
		System.out.println("Number of Key Items, " + numKeyValues);

		System.out.println("Number of heap elements, " + numHeapElts);
		System.out.println("Number of LRU Queues, " + numLRUQueues);

		System.out.println("Num Requests, " + numReqs);
		System.out.println("Num Hits, " + numHits);
		System.out.println("Cost In Cache, " + costNotInCache);
		System.out.println("Total Cost, " + totalCost);
		
		
		
		System.out.println("Hit to Request Ratio, " + hitToRequestRatio);
		System.out.println("Miss To Request Ratio, " + missToRequestRatio);
		System.out.println("Cost To Miss Ratio, " + costToMissRatio);
		System.out.println("Heap Accesses, " + totalHeapAccesses);
		
		
		System.out.println("Average Thread Run Time in seconds: " + avgTime);
		System.out.println();
		
	
	}
}
