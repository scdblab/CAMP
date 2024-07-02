package com.camp.simulator;

import com.camp.algorithm.CAMP;
import com.camp.algorithm.Request;

public class Experiment1 {
/*
	public static int NumThreads = 4;
	public static int UniqueObjects = 10000;
	public static double CacheSizeToDB = 1.0;
	public static int NumIterations = 100;
	public static int Precision = 4;
	
	public static int ObjectSize = 100;


	public static void main(String[] args) {

		long DBSize = ObjectSize * UniqueObjects;
		long CacheSize = (long)(DBSize * CacheSizeToDB);

		int values [] = {1, 10, 100, 1000};

		String templateKey = "profileImage%d";
		Request [] requests = new Request [UniqueObjects];

		for(int i = 0; i < UniqueObjects; i++){
			Request request = new Request(i, String.format(templateKey, i), ObjectSize, values[i % 4], true );
			requests[i] = request;
		}

		//for(NumThreads = 1; NumThreads <= 16; NumThreads = NumThreads *2){
		
		if(NumThreads == 4){
		
		CAMP camp = new CAMP(CacheSize, Precision, 1, null);

		long startTime = System.nanoTime();
		
		for(int i = 0; i < NumIterations; i++){
			Simulator simulators[] = new Simulator [ NumThreads ];

			for(int j = 0 ; j < NumThreads; j ++){
				Simulator simulator = new Simulator( camp, null, requests, j, NumThreads );
				simulators[j] = simulator;
			}
			for(Thread s : simulators){
				s.start();
			}
			for(Thread s: simulators){
				try {
					s.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}	
			}
		}
		
		long endTime = System.nanoTime();
		
		camp.PrintCampStats();
		
		System.out.println("Total execution time in seconds: " + ((endTime - startTime) / 1000000000.0));
		}
	} */
}
