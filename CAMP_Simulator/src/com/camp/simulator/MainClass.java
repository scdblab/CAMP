package com.camp.simulator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.camp.algorithm.CAMP;
import com.camp.algorithm.Request;

public class MainClass {

	// static String OutputPath = "/home/shahram/KOSAR/Simulator/Simulations";
	static String OutputPath = "C:\\KOSAR\\Simulator\\Simulations";
	static PerformanceTimer performanceTimer;
	
	static int NumThreads = -1;
	static int Precision = -1;
	static int NumQueries = 10;

	static int NumIterations = 1;
	//static int NumIterations = 10;

	static Integer [] ThreadsToRun = { 1 };
	//static Integer [] ThreadsToRun = { 1,2,3,4,5,6,7,8 };
	
	static Integer [] PrecisionsToRun = { 1,2,4,8 }; // Fix so 32 will run correctly

	//static Integer [] PrecisionsToRun = { 1,2,4,8,16,24 }; // Fix so 32 will run correctly

	static Double [] InsertionProbabilitiesToRun = { 1.0 };

	static double[] cachePercentages = { .2 };

	//static double[] cachePercentages = { 1.0 };
	//static Integer [] PrecisionsToRun = { 4 };

	static ArrayList<Integer> PrecisionList = new ArrayList<Integer>();
	static ArrayList<Integer> ThreadList = new ArrayList<Integer>();

	public static final String RootDirectory = "C:\\KOSAR\\";
//	public static final String RootDirectory = "/home/shahram/KOSAR/";
	
//	public static final String InputDirectory = "InputFiles\\";
//	public static final String OutputDirectory = "OutputFiles_JAVA\\";
	
	public static final String InputDirectory = "InputFiles/";
	public static final String OutputDirectory = "OutputFiles_JAVA/";

	public static String [] FilesToRun = 
		{
		//	"traceZipf0.27-short.dat"
		"traceZipf0.27-hint.dat",
		//	"traceZipf0.27-hint-expcost.dat",
		"traceZipf0.99-hint.dat"
		//	"traceZipf0.99-hint-expcost.dat",
		};

	public static boolean PrintOutput = false;

	public static void RunSimulation(SimulationInput si){

		PrintWriter writer;
		if(PrintOutput == false)
			writer = null;
		else{
			try{
				writer = new PrintWriter(si.outputFile);
			}
			catch(Exception e){
				e.printStackTrace();
				return;
			}
		}
		//cacheSize = 167000;
		CAMP camp = new CAMP(si.cacheSize, si.precision, si.insertionProbability);

		long timeNanoSeconds = 0;

		Simulator simulators[] = new Simulator [ si.numThreads ];

		for(int i = 0 ; i < si.numThreads; i ++){
						
			Simulator simulator = new Simulator( camp, writer, si.requests, i, si.numThreads, si.map, si.numBuckets );
			simulator.NumIterations = NumIterations;
			simulators[i] = simulator;
		}
		
		long startTime = System.nanoTime();
		
		performanceTimer.StartTrackTime(si);
		for(Simulator s : simulators){
			s.start();
		}

		for(Simulator s: simulators){
			try {
				s.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
		performanceTimer.EndTrackTime();
		long endTime = System.nanoTime();
		timeNanoSeconds += (endTime - startTime);

		for(Simulator s: simulators){
			System.out.println("Run Time for " + s.getId() + " : " + s.totalRunTime);
		}

		if(writer != null)
			writer.flush();

		StatsHelper helper = new StatsHelper();
		helper.CompileStats(simulators, camp);
		helper.numThreads = si.numThreads;
		helper.PrintStats(si.outputFile);	

		for(int i = 0; i < si.numBuckets; i++){
			Request [] reqs = si.requests.get(i);
			for(Request req : reqs){
				req.repeat = req.orig_repeat;
			}	
		}
	}

	public static void SetupAndRunSimulation(String traceFile, String exp, long cacheSize, boolean verbose){

		String outputFile;

		Map<Integer, Request[]> hashmapRequests = null;
		RequestManager requestManager = new RequestManager();

		if(traceFile != ""){
			 //requests = requestManager.GetRequests(traceFile);
			outputFile = traceFile.substring(0, traceFile.lastIndexOf('.'));
			outputFile = traceFile.substring(traceFile.lastIndexOf("//"));
			
			System.out.println("Output file line (line 132) Main Class " + outputFile);
			  
		}
		else if (exp != ""){

			if(exp.compareTo("exp1") == 0){

				int numTotalRequests =  10000;
				int numUniqueRequests = 10000;
				int [] requestSizes = {100};
				int [] requestCosts = {1, 10, 100, 1000};

			//	requests = requestManager.GenerateExperiment(numTotalRequests, numUniqueRequests, requestSizes, requestCosts);
				System.out.println("Requests Generated for experiment: " + exp);
				outputFile = "/" + exp + NumQueries;
				System.out.println("Output file line (line 147) Main Class " + outputFile);

			}
			else if(exp.compareTo("exp2") == 0){

				int numTotalRequests =  80000;
				int numUniqueRequests = 80000;
				int [] requestSizes = {100};
				int [] requestCosts = {1, 8, 97, 1003};

			// requests = requestManager.GenerateExperiment(numTotalRequests, numUniqueRequests, requestSizes, requestCosts);
				System.out.println("Requests Generated for experiment: " + exp);
				outputFile = "/" + exp + NumQueries;
				
				System.out.println("Output file line (line 158) Main Class " + outputFile);

				
			} 
		   if(exp.compareTo("100U") == 0){

				int numTotalRequests =  160000;
				int numUniqueRequests = 1600;
				int [] requestSizes = {100};
				//int [] requestCosts = {1,  13, 57, 123, 641, 1013, 1987, 3021};
				int [] requestCosts = {1,  13, 57, 123};


				hashmapRequests = requestManager.GenerateExperiment(numTotalRequests, numUniqueRequests, requestSizes, requestCosts);
				System.out.println("Requests Generated for experiment: " + exp);
				outputFile = "/" + exp + NumQueries;

				System.out.println("Output file line (line 177) Main Class " + outputFile);


			}
			else{
				System.out.println("Unknown experiment");
				return;
			}
		}
		else{
			//requests = requestManager.GenerateAllUniqueRequests(NumQueries);
			// requests = requestManager.GenerateAllIncreasingRequests(NumQueries);		
			System.out.println("Unique Requests Generated");
			outputFile = "/UniqueQueries_" + NumQueries;
			
			System.out.println("Output file line (line 192) Main Class " + outputFile);
			 
			
		}

		long uniqueQuerySize = requestManager.UniqueQuerySize;

		if(cacheSize == -1){ // means run percentages
			for(double cachePercent : cachePercentages){

				long simulationCacheSize = (long) (uniqueQuerySize * cachePercent);

				System.out.println("**** Cache Size : " + cachePercent + " - " + simulationCacheSize);


				for(Integer precision : PrecisionList){

					System.out.println("Precision: " + precision);

					for(Integer numThreadsToRun: ThreadList){

						System.out.println("Threads: " + numThreadsToRun);

						for(Double insertionProbability: InsertionProbabilitiesToRun){
							System.out.println("Insertion Probability: " + insertionProbability);

							System.out.println("***** Cache Percent = " + cachePercent + " : " + simulationCacheSize + " *****");
							String tempOutputFile = OutputPath + outputFile + "_" + cachePercent +"_output.txt";

							System.out.println("Output line (line 223) Main Class " + tempOutputFile);

							
							SimulationInput si = new SimulationInput();
							si.outputFile = tempOutputFile;
							si.cacheSize = simulationCacheSize;
							si.numThreads = numThreadsToRun;
							si.precision = precision;
							si.insertionProbability = insertionProbability;
							
							if(hashmapRequests == null)
								si.requests = new HashMap<Integer, Request[]>();
							
							si.requests = hashmapRequests;
							si.map = requestManager.map;
							si.traceFile = "Unique requests";
							si.cachePercentage= cachePercent;
							si.numBuckets = requestManager.NumBuckets;
							RunSimulation(si);

						}
					}
				}
			}
		}

		else{ //use inputted value
			String tempOutputFile = OutputPath + outputFile + "_" + cacheSize +"_output.txt";

			System.out.println("Output file line (line 247) Main Class " + tempOutputFile);

			
			for(Integer precision : PrecisionList){
				System.out.println("Precision: " + precision);
				for(Integer numThreadsToRun: ThreadList){				
					System.out.println("Threads: " + numThreadsToRun);
					for(Double insertionProbability: InsertionProbabilitiesToRun){
						System.out.println("Insertion Probability: " + insertionProbability);

						SimulationInput si = new SimulationInput();
						si.outputFile = tempOutputFile;
						si.cacheSize = cacheSize;
						si.numThreads = numThreadsToRun;
						si.precision = precision;
						si.insertionProbability = insertionProbability;
						si.requests = hashmapRequests;
						si.map = requestManager.map;
						si.traceFile = "Unique requests";
						si.cachePercentage= 0;
						RunSimulation(si);	

					}
				}
			}
		}
	}

	public static void SimulateFiles(long cacheSize, boolean verbose){

		String outputFile = RootDirectory + OutputDirectory + "AllFilesOutput.csv";

		File file = new File(outputFile);

		//if file doesnt exists, then create it
		try{
			if(file.exists()){
				file.delete();
			}
			file.createNewFile();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		//stats.GetMissToRequestRatio(), stats.GetCostToMissRatio(), LRUHT.size(), PQ.size());

		// reset file

		for(String filename : FilesToRun){

			String FullPath = RootDirectory + InputDirectory + filename;
			
			System.out.println("input file line (line 303) Main Class " + FullPath);

			
			RequestManager requestManager = new RequestManager();
			Map<Integer, Request[]> hashmapRequests = requestManager.GetRequests(FullPath);
			
			int NumBuckets = requestManager.NumBuckets;
			long uniqueQuerySize = requestManager.UniqueQuerySize;

			for(double cachePercent : cachePercentages){

				long simulationCacheSize = (long) (uniqueQuerySize * cachePercent);

				WriteToFile("File: " + filename + ",Cache Percent: " + cachePercent + ",Cache Size: " + simulationCacheSize, outputFile);

				WriteToFile("Precision, Num Threads, Miss To Request, Cost To Miss, Num Key Values, # LRU Queues, Heap Elements, time (s), requests per sec, evictions", outputFile);

				System.out.println("**** Cache Size : " + cachePercent + " - " + simulationCacheSize);

				for(Integer precision : PrecisionList){

					System.out.println("Precision: " + precision);

					for(Integer numThreadsToRun: ThreadList){
						System.out.println("Threads: " + numThreadsToRun);
						for(Double insertionProbability: InsertionProbabilitiesToRun){
							System.out.println("Insertion Probability: " + insertionProbability);

							SimulationInput si = new SimulationInput();
							si.outputFile = outputFile;
							si.cacheSize = simulationCacheSize;
							si.numThreads = numThreadsToRun;
							si.precision = precision;
							si.insertionProbability = insertionProbability;
							si.requests = hashmapRequests;
							si.map = requestManager.map;
							si.traceFile = filename;
							si.cachePercentage= cachePercent;
							si.numBuckets = NumBuckets;
							RunSimulation(si);

						}
					}
				}

				WriteToFile("", outputFile);

			}
		}

		//CreateScript(cacheSize);
		// Create Script

	}

	public static void WriteToFile(String data, String outputFile){

		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)))) {
			out.println(data);
			out.close();
		}catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/*
	public static void CreateScript(int cacheSize){

		String C_Template = 

		String Line_Template = "./simulate %s %d %d";


		for(String filename : FilesToRun){

			String FullPath = RootDirectory + InputDirectory + filename;
			RequestManager requestManager = new RequestManager();
			Request [] requests = requestManager.GetRequests(FullPath);

			long uniqueQuerySize = requestManager.UniqueQuerySize;

			for(double cachePercent : cachePercentages){

				long simulationCacheSize = (long) (uniqueQuerySize * cachePercent);

				WriteToFile("File: " + filename + ",Cache Percent: " + cachePercent + ",Cache Size: " + simulationCacheSize, outputFile);

				WriteToFile("Precision,Miss To Request, Cost To Miss, Num Key Values, # LRU Queues, Heap Elements", outputFile);

				System.out.println("**** Cache Size : " + cachePercent + " - " + simulationCacheSize);

				for(Integer precision : PrecisionList){

					System.out.println("Precision: " + precision);

					for(Integer numThreadsToRun: ThreadList){

						System.out.println("Threads: " + numThreadsToRun);

						RunSimulation(outputFile, simulationCacheSize, numThreadsToRun, precision, requests );
						//	public static void RunSimulation(String outputFile, long cacheSize, int numThreads, int precision, Request requests []){			
					}
				}

				WriteToFile("", outputFile);

			}
		}

	}

	 */

	public static void main(String[] args) {

		String format = "-threads numthreads -precision precision -file <tracefile> -cache <cachesize> -verbose <verbose>[0 or 1] ";

		performanceTimer = new PerformanceTimer();
		
		//String format = "filename cache-size precision [verbose (0 or 1)]";

		if(args.length < 2){
			System.out.println("Usage: " + format);
			return;
		}

		String traceFile = "";
		long cacheSize = -1;
		boolean verbose = false;
		String experiment = "";

		try{

			for(int i = 0; i < args.length; i++){

				if(args[i].compareTo("-threads") == 0 && i+1 < args.length){
					NumThreads = Integer.parseInt(args[i+1]);
					i++;
				}
				else if(args[i].compareTo("-precision") == 0 && i+1 < args.length){
					Precision = Integer.parseInt(args[i+1]);
					i++;
				}
				else if(args[i].compareTo("-exp") == 0 && i+1 < args.length){
					experiment = args[i+1];
					i++;
				}
				else if(args[i].compareTo("-file") == 0 && i+1 < args.length){
					traceFile = args[i+1];
					i++;
				}
				else if(args[i].compareTo("-cache") == 0 && i+1 < args.length){
					cacheSize = Long.parseLong(args[i+1]);
					i++;
				}
				else if(args[i].compareTo("-verbose") == 0 && i+1 < args.length){
					verbose = Integer.parseInt(args[3]) == 1 ? true : false;
					i++;
				}
				else{
					System.out.println("Unknown command line parameter: " + args[i]+ ". Usage: " + format);
					return;
				}
			}
		}
		catch(Exception e){	
			System.out.println("Commandline argument is not of correct type. " + format);
			return;
		}

		if(NumThreads == -1)
			ThreadList = new ArrayList<Integer>(Arrays.asList(ThreadsToRun));
		else{
			ThreadList.add(NumThreads);
		}

		if(Precision == -1)
			PrecisionList = new ArrayList<Integer>(Arrays.asList(PrecisionsToRun));
		else{
			PrecisionList.add(Precision);
		}			

		if(experiment.compareTo("files") == 0){
			SimulateFiles(cacheSize, verbose);
		}
		else{
			SetupAndRunSimulation(traceFile, experiment, cacheSize, verbose);
		}

		performanceTimer.CloseWriter();

	}
}
