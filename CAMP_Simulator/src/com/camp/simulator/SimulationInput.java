package com.camp.simulator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.camp.algorithm.Request;

public class SimulationInput {

	String outputFile;
	long cacheSize;
	double cachePercentage;
	int numThreads;
	int precision;
	double insertionProbability;
	Map<Integer, Request[]> requests;
	int numBuckets;
	
	ConcurrentHashMap<String, Request> map;
	String traceFile;
	
}
