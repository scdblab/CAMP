package com.camp.simulator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.camp.algorithm.Request;
import com.camp.algorithm.dust;

public class RequestManager {

	public long NumRequests = 0;
	public long TotalQuerySize = 0;
	public long UniqueQuerySize = 0;
	public int NumUniqueRequests = 0;
	
	static final String templateKey = "profileImage%d";
	
	//public Request ScrambledRequests [];
	
	public int NumBuckets = 1000;
	
	public ConcurrentHashMap <String, Request> map = new ConcurrentHashMap<String, Request>();
	
	@SuppressWarnings("unchecked")
	// public Map<String, Request> [] RequestHashMaps = new HashMap[NumBuckets]; 
	
	
	
	public RequestManager(){
		
		/*(for(int i = 0; i < RequestHashMaps.length; i++){
			RequestHashMaps[i] = new HashMap<String,Request>();
		} */
	}
	
	public Map<Integer,Request[]> GetRequests(String filename){
	
		ArrayList<Request> requests = new ArrayList<Request>();
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			
			while ((line = br.readLine()) != null) {
			  			
				String [] splitStr = line.split(",");
				int index = Integer.parseInt(splitStr[0]);
				String key = splitStr[1].replace(":", "");
				int size = Integer.parseInt(splitStr[2]);
				int cost = Integer.parseInt(splitStr[3]);
				boolean repeat = true;
				if(splitStr.length > 4){
					int rep = Integer.parseInt(splitStr[4]);
					if(rep == 1){
						repeat = true;
					}
					else
						repeat = false;
				//	repeat = Boolean.parseBoolean(splitStr[4]);
				}
				
				Request r = new Request(index, key, size, cost, repeat);
				
				requests.add(r);
				
				//RequestHashMaps[index % NumBuckets].put(key, r);					
				
				Object i = map.get(r.key);
				if(i == null){
					map.put(r.key, r);
					UniqueQuerySize += size;
					NumUniqueRequests++;
				}
			
				NumRequests++;
				TotalQuerySize += size;
			}
			br.close();
			}
			catch(FileNotFoundException ex){
				System.out.println(ex.getMessage());
				return null;
			}
			catch(IOException ex){
				System.out.println(ex.getMessage());
				return null;
			}
			catch(Exception ex){
				System.out.println(ex.getMessage());
				return null;
			}
		    
			HashMap <Integer, Request[]> retMap = new HashMap<Integer, Request[]>();
		
			int elementsPerArray = requests.size() / NumBuckets;
		
			for(int i = 0; i < NumBuckets; i++){
				ArrayList<Request> elements = new ArrayList<Request>();
				for(int j = 0; j < elementsPerArray; j++){
					elements.add( requests.get( i * elementsPerArray + j ) );
				}
				Request [] reqs = elements.toArray(new Request[elements.size()]);
				retMap.put(i, reqs);
			}
		
		return retMap;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer,Request[]> GenerateExperiment(int numTotalRequests, int numUniqueRequests, int[] requestSizes, int[] requestCosts){
		
		//Request requests[] = new Request[numTotalRequests];
		this.NumRequests = numTotalRequests;
		
		this.NumBuckets = requestCosts.length;
		
		Map<Integer,Request[]> reqs = new HashMap<Integer, Request[]>();
				
		ArrayList<Request> requests = new ArrayList<Request>();
		
		int interval = numTotalRequests / requestCosts.length;
		int currentInterval = 0;
		int numUniquePerCost = numUniqueRequests / requestCosts.length;
		
		int idx = 0;
		for(int i = 0; i < requestCosts.length; i++){
			
			Request [] tempReqs = new Request[numUniquePerCost];
			for(int j = 0; j < numUniquePerCost; j++)
			{
				Request request = new Request(idx, String.format(templateKey, idx % numUniqueRequests), 
						requestSizes[idx % requestSizes.length ],
						requestCosts[i],
						true);
				
				tempReqs[j] = request;
				
				if(i < numUniqueRequests){
					UniqueQuerySize += requestSizes[i % requestSizes.length];
					map.put(request.key, request);			
				}
				idx++;
			}
			reqs.put(i, tempReqs);			
		}
		
		/*
		for(int i = 0; i < numTotalRequests; i++){
			
			Request request = new Request(i, String.format(templateKey, i % numUniqueRequests), 
								requestSizes[i % requestSizes.length],
								requestCosts[i % requestCosts.length],
								true);

			
			
			
			//requests.add(arg0)
			
			//RequestHashMaps[i % numUniqueRequests].put(request.key, request);
			
			//requests[i] = request;
			
			TotalQuerySize += requestSizes[i % requestSizes.length];
		} */
		
	/*	for(int i = 0; i < numUniqueRequests; i++){
			
			for(Request req : RequestHashMaps[i].values())
				System.out.println(req.key);		
		} */
		
		return reqs;
	}
	
	public Request[] GenerateAllIncreasingRequests(int numOfRequests){
		
		Request requests [] = new Request[numOfRequests];
		
		this.NumRequests = numOfRequests;
		
		int values [] = {1, 10, 100, 1000};
		
		for(int i = 0; i < requests.length; i ++){
			Request request = new Request(i, String.format(templateKey, i % 100), 1000, values[i % 4], true );
			
			TotalQuerySize += 1000;
			UniqueQuerySize += 1000;
			
		/*	requests[i].key = String.format(templateKey, i);
			requests[i].cost = 1000;
			requests[i].size = 1000;
			requests[i].repeat = true; */
			requests[i] = request;
		}
		
		return requests;
		
	}
	
	public Request[] GenerateAllUniqueRequests(int numOfRequests){
		
		Request requests [] = new Request[numOfRequests];
		
		this.NumRequests = numOfRequests;
		
		String templateKey = "profileImage%d";
		
		for(int i = 0; i < requests.length; i ++){
			Request request = new Request(i, String.format(templateKey, i), 1000, 1000, true );
			
			TotalQuerySize += 1000;
			UniqueQuerySize += 1000;
			
		/*	requests[i].key = String.format(templateKey, i);
			requests[i].cost = 1000;
			requests[i].size = 1000;
			requests[i].repeat = true; */
			requests[i] = request;
		}
		
		return requests;
		
	}
	
	public long CacheSize(String filename){
		
	    long fileSize = 0;
	    
		return fileSize;
	}
	
	
	
	

	
	
	
	public static void main(String[] args) {
		
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
	    System.out.print("Enter a trace file:");
	    String traceFile = "";
		try {
			traceFile = input.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	       
	    
	    long fileSize = 0;
	    
		try{
			BufferedReader br = new BufferedReader(new FileReader(traceFile));
			String line;
			
			while ((line = br.readLine()) != null) {
			  
				String [] splitStr = line.split(",");
				int index = Integer.parseInt(splitStr[0]);
				String key = splitStr[1].replace(":", "");
				int size = Integer.parseInt(splitStr[2]);
				int cost = Integer.parseInt(splitStr[3]);
				boolean repeat = true;
			
				fileSize += size;
				
			}
			br.close();
			}
			catch(FileNotFoundException ex){
				System.out.println(ex.getMessage());
				return;
			}
			catch(IOException ex){
				System.out.println(ex.getMessage());
				return;
			}
			catch(Exception ex){
				System.out.println(ex.getMessage());
				return;
			}
		
		
		System.out.println("Total size of Queries: " + fileSize);

	}
	
}
