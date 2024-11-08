package com.mitrallc.webserver;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventMonitor {
	class MyEvent {
		long timeStamp;
		int numEvents;
		int duration;
		
		public int getDuration() {
			return duration;
		}

		public void setDuration(int duration) {
			this.duration = duration;
		}

		public long getTimeStamp() {
			return timeStamp;
		}

		public void setTimeStamp(long timeStamp) {
			this.timeStamp = timeStamp;
		}

		public int getNumEvents() {
			return numEvents;
		}

		public void setNumEvents(int numEvents) {
			this.numEvents = numEvents;
		}

		MyEvent(long timeStamp, int numEvents, int duration) {
			this.timeStamp = timeStamp;
			this.numEvents = numEvents;
			this.duration = duration;
		}
	}
	
	Queue<MyEvent> events = new ConcurrentLinkedQueue<MyEvent>();
	Queue<MyEvent> freeEvents = new ConcurrentLinkedQueue<MyEvent>();
	//Queue<MyEvent> events = new LinkedList<MyEvent>();
	
	static int GranularityInMillis;
	int numEventsSince100ms;
	int totalEventDurationSince100ms;
	private int numTotalEventsSinceGenesis;
	long last100msTimeStamp;
	long genesisTime;
	
	public EventMonitor(int GranularityInSeconds) {
		genesisTime = System.currentTimeMillis();
		numTotalEventsSinceGenesis = 0;
		numEventsSince100ms = 0;
		totalEventDurationSince100ms = 0;
		last100msTimeStamp = genesisTime;
		GranularityInMillis = GranularityInSeconds *1000;
	}
	public void newEvent(int duration) {
		long currentTime = System.currentTimeMillis();
		numEventsSince100ms++;
		totalEventDurationSince100ms += duration;
		numTotalEventsSinceGenesis++;
		
		if(currentTime - last100msTimeStamp >= 100) {
			MyEvent tempEvent = null;
			if((tempEvent = freeEvents.poll()) != null) {
				tempEvent.setTimeStamp(currentTime);
				tempEvent.setNumEvents(numEventsSince100ms);
				tempEvent.setDuration(totalEventDurationSince100ms);
			}
			else
				tempEvent = new MyEvent(currentTime, numEventsSince100ms, totalEventDurationSince100ms);
			
			events.add(tempEvent);
			numEventsSince100ms = 0;
			totalEventDurationSince100ms = 0;
			last100msTimeStamp = currentTime;
		}
		removeOldEvents();
	}
	public double averageEventDurationPerGranularity() {
		removeOldEvents();
		int numEvents = 0;
		int totalDuration = 0;
		for( MyEvent event : events ){
			numEvents += event.getNumEvents();
			totalDuration += event.getDuration();
		}

		return (double)totalDuration/(double)numEvents;
	}
	public int numberOfEventsPerGranularity() {
		removeOldEvents();
		int numEvents = 0;
		for( MyEvent event : events )
			numEvents += event.numEvents;
		return numEvents;
	}
	public int numberOfTotalEvents() {
		removeOldEvents();
		
		//The counter may wrap around
		if (numTotalEventsSinceGenesis < 0)
			numTotalEventsSinceGenesis = 1;
		
		return numTotalEventsSinceGenesis;
	}
	public double totalAvgNumberOfEventsPerSecond() {
		//Not implemented; method does not support numTotalEventsSinceGenesis when
		//it attempts to exceed Integer.MAX.
		removeOldEvents();
		double timeElapsed = System.currentTimeMillis() - genesisTime;
		double avgEventsPerSecond = (double)numTotalEventsSinceGenesis*1000/timeElapsed;
		return avgEventsPerSecond;
	}
	private void removeOldEvents() {
		MyEvent tempEvent = null;
		long currentTime = System.currentTimeMillis();
		
		while(!events.isEmpty()) {
			synchronized(events) {
				if(currentTime-events.peek().timeStamp >= GranularityInMillis)
					freeEvents.add(events.poll());
				else break;
			}
		}
		/*while((tempEvent = events.poll()) != null) {
			if(currentTime - tempEvent.timeStamp >= GranularityInMillis) {
				freeEvents.add(tempEvent);
			}
			else break;
		}*/
	}
	private void checkHeadAndTail() {
		long head = events.peek().timeStamp;
		long tail = -1;
		for(MyEvent e : events){
			tail = e.timeStamp;
		}
		System.out.println("DIFF " + (tail-head));
	}
	private void calculateExpectedEventsPerGranularity(int Granularity, int sleepTimeInMillis) {
		double value = GranularityInMillis/sleepTimeInMillis;
		int tempInt = (int)value;
	}
	public static void setGranularityInSeconds(int granularity) {
		GranularityInMillis = granularity*1000;
	}
	public void reset() {
		numTotalEventsSinceGenesis = 0;
	}
	
	/*******Unit Tests*********/
	private void UnitTest1(int numIterations, int sleepTimeInMillis, int eventDuration) {
		//Avg number of events since the start of this test.
		//If we want average events per second since the start of this test,
		//we would need to either reset genesis time to 0, or create a method specifying a time range.
		double time = (System.currentTimeMillis() -genesisTime)/1000;
		double expectedAvgNumberOfEventsPerSecond = (numTotalEventsSinceGenesis + numIterations)/(time+(numIterations*sleepTimeInMillis/1000));
		double upperBoundAvg = expectedAvgNumberOfEventsPerSecond * 1.1;
		double lowerBoundAvg = expectedAvgNumberOfEventsPerSecond * 0.9;
		double oldNumTotalEvents = numTotalEventsSinceGenesis;
		double calculatedAvgNumberOfEventsPerSecond = -1;
		double percentOffset = -1;
		double expectedAverageEventDuration = (double)eventDuration;
		double upperBoundEventDuration = expectedAverageEventDuration * 1.1;
		double lowerBoundEventDuration = expectedAverageEventDuration * 0.9;
		
		//Perform Test
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < numIterations; i++) {
			try {
				Thread.sleep(sleepTimeInMillis);
			} catch (InterruptedException e) {
				System.out.println("Error sleeping for " + sleepTimeInMillis + " seconds.");
				e.printStackTrace();
			}
			newEvent(eventDuration);
		}
		long endTime = System.currentTimeMillis();
		double timeElapsed = endTime-startTime;
		
		//Calculate Results
		double expectedElapsedTime = sleepTimeInMillis*numIterations;
		double elapsedTimeError = (double)Math.abs(expectedElapsedTime-timeElapsed)/expectedElapsedTime;
		calculatedAvgNumberOfEventsPerSecond = totalAvgNumberOfEventsPerSecond();
		percentOffset = Math.abs(expectedAvgNumberOfEventsPerSecond-calculatedAvgNumberOfEventsPerSecond)
				/expectedAvgNumberOfEventsPerSecond;
		double calculatedAverageEventDuration = averageEventDurationPerGranularity();
		
		//Print Results
		System.out.println("Unit Test 1: " + numIterations + " iterations, " + sleepTimeInMillis + " sleeptime");
		System.out.println("Calculated Avg Number of Events Per Second: " + calculatedAvgNumberOfEventsPerSecond);
		System.out.println("Expected Avg Number of Events Per Second: " + expectedAvgNumberOfEventsPerSecond);
		System.out.println("\t" + percentOffset + "% error");
		System.out.println("\t" + elapsedTimeError + "% sleeptime error.  Expected " 
				+ (int)expectedElapsedTime + "ms, Slept " + (int)timeElapsed + "ms");
		
		percentOffset = Math.abs(expectedAverageEventDuration-calculatedAverageEventDuration)
				/expectedAverageEventDuration;
		System.out.println("Calculated Average Event Duration: " + calculatedAverageEventDuration);
		System.out.println("Expected Average Event Duration: " + expectedAverageEventDuration);
		System.out.println("\t" + percentOffset + "% error");
		
		//Error check
		if(calculatedAvgNumberOfEventsPerSecond < lowerBoundAvg ||
				calculatedAvgNumberOfEventsPerSecond > upperBoundAvg)
			System.out.println("Error: Total average number of events per second is " 
					+ totalAvgNumberOfEventsPerSecond());
		
		if(numberOfTotalEvents() != numIterations+oldNumTotalEvents)
			System.out.println("Error: Total number of events is " + numberOfTotalEvents() + ", not the expected value " 
					+ numIterations);
		
		if(calculatedAverageEventDuration > upperBoundEventDuration ||
				calculatedAverageEventDuration < lowerBoundEventDuration)
			System.out.println("Error: Average Event Duration is " + calculatedAverageEventDuration
					+ ", not the expected value " + expectedAverageEventDuration);
		
		System.out.println();
	}
	private void UnitTest2(int numIterations, int sleepTimeInMillis, int Granularity) {
		setGranularityInSeconds(Granularity);
		double expectedEventsPerGranularity = Math.ceil((double)GranularityInMillis/(double)sleepTimeInMillis);
		int calculatedEventsPerGranularity = -1;
		double upperBoundEventsPerGranularity = (double)expectedEventsPerGranularity * 1.1;
		double lowerBoundEventsPerGranularity = (double)expectedEventsPerGranularity * 0.9;
		double percentOffset = -1;
		
		//Perform Tests
		UnitTest1(numIterations, sleepTimeInMillis, 1);
		calculatedEventsPerGranularity = numberOfEventsPerGranularity();
		percentOffset = (double)Math.abs(expectedEventsPerGranularity-calculatedEventsPerGranularity)
				/expectedEventsPerGranularity;
		
		//Print Results
		System.out.println("Unit Test 2: Iterations: " + numIterations + ", SleepTime: " + sleepTimeInMillis + ", Granularity: " + Granularity +"s");
		System.out.println("Calculated Number of Events Per Granularity: " + calculatedEventsPerGranularity);
		System.out.println("Expected Number of Events Per Granularity: " + expectedEventsPerGranularity);
		System.out.println("\t" + percentOffset + "% error");
		
		//Error check
		if(calculatedEventsPerGranularity < lowerBoundEventsPerGranularity ||
				calculatedEventsPerGranularity > upperBoundEventsPerGranularity)
			System.out.println("Error: Total number of events per granularity " 
					+ Granularity + " second(s) is " + calculatedEventsPerGranularity
					+ " not the expected value " 
					+ expectedEventsPerGranularity);
		
	}
	private void UnitTest3() {
		//UnitTest2(100,100,1);
		//UnitTest2(100,200,1);
		/*UnitTest2(100,300,1);
		UnitTest2(100,100,3);
		UnitTest2(100,200,3);
		UnitTest2(100,300,3);
		UnitTest2(100,100,5);
		UnitTest2(100,200,5);
		UnitTest2(100,300,5);
		UnitTest2(100,100,5);
		UnitTest2(100,200,5);
		UnitTest2(100,300,5);*/
		UnitTest2(100,100,7);
		UnitTest2(100,200,7);
		UnitTest2(100,300,7);
		
	}
	private void UnitTest4() {
		Thread threads[] = new Thread[10000];
		for(int i = 0; i < threads.length; i++) {
			final int j = i;
			threads[i] = new Thread() {
				public void run() {
					while(true) {
						/*newEvent(1);
						numberOfEventsPerGranularity();
						totalAvgNumberOfEventsPerSecond();
						System.out.println(numberOfTotalEvents());*/
						UnitTest2(10,100,3);
					}
				}
			};
			threads[i].start();
		}
	}
	public static void main(String args[]) {
		int numIterations = 100;
		int sleepTimeInMillis = 100;
		int Granularity = 1;
		EventMonitor testMonitor = new EventMonitor(3);
		//testMonitor.UnitTest1(numIterations, sleepTimeInMillis,50);
		//testMonitor.UnitTest2(numIterations, sleepTimeInMillis, Granularity);
		//testMonitor.UnitTest3();
		//testMonitor.UnitTest4();
	}
}