package com.mitrallc.kosar;

import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.sql.KosarSoloDriver;

public class Stats  extends Thread {
	
	private static int FreqInMillisec = 10*1000;
	
	private static AtomicInteger NumReqs = new AtomicInteger(0);
	private static AtomicInteger NumHits = new AtomicInteger(0);
	private static AtomicInteger NumKeyValues = new AtomicInteger(0);
	private static AtomicInteger NumTriggers = new AtomicInteger(0);
	private static AtomicInteger NumProcs = new AtomicInteger(0);
	
	private static int numreqs = 0;
	public static int numhits = 0;
	private static int numkeyvalues = 0;
	private static int numtriggers = 0;
	private static int numprocs = 0;
	private static int numBackOffs = 0;
	
	public void IncrementNumReqs(){
		numreqs = NumReqs.incrementAndGet();
	}
	
	public void IncrementNumBackOffs(){
		numreqs = NumReqs.incrementAndGet();
	}
	public void IncrementNumHit() {
		numhits = NumHits.incrementAndGet();
	}
	
	public void IncrementNumKeyValues() {
		numkeyvalues = NumKeyValues.incrementAndGet();
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarKeysCachedEventMonitor.newEvent(1);
	}
	
	public void IncrementNumTriggers() {
		numtriggers = NumTriggers.incrementAndGet();
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarTriggerRegEventMonitor.newEvent(1);
	}
	
	public void IncrementNumProcs() {
		numprocs = NumProcs.incrementAndGet();
	}
	
	public Stats(int howfreq){
	}
	
	public void run() {
		for ( ; ; ){
			try {
				Thread.sleep(FreqInMillisec);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("Num of elements in KVs = "+kosar.GetNumKeys() + " NumOfDeletedKeys= " + kosar.getNumDeletedKeys()+" NumOfDeletedCalls = " + kosar.getNumDeletedCalls());
			System.out.println("Num requests = "+numreqs+", NumHits = "+numhits+", # Key Values = "+numkeyvalues+", # Reg Triggers = "+numtriggers+", # Reg Procs = "+numprocs +", # Number of Backoffs = "+numBackOffs);
		}
	}

}
