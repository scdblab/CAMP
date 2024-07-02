package com.meetup.memcached.test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;

import com.meetup.memcached.MemcachedClient;
import com.meetup.memcached.SockIOPool;

public class BGSimTestNoLock {
	public static class Timestamp {
		private long seconds;
		private long nanoseconds;
		
		private long token;
		
		private static int LARGER = 1;
		private static int SMALLER = -1;
		private static int SAME = 0;
		
		public Timestamp (long token) {
			this.token = token;
		}
		
		public Timestamp (long seconds, long nanoseconds) {
			this.seconds = seconds;
			this.nanoseconds = nanoseconds;
		}
		
		public Timestamp (Timestamp other)
		{
			this.seconds = other.seconds;
			this.nanoseconds = other.nanoseconds;
			this.token = other.token;
		}
		
		public int compare(Timestamp other)
		{			
//			if(this.seconds > other.seconds) {
//				return LARGER;
//			} else if(this.seconds < other.seconds) {
//				return SMALLER;
//			} 
//			
//			if(this.nanoseconds > other.nanoseconds) {
//				return LARGER;
//			} else if(this.nanoseconds < other.nanoseconds) {
//				return SMALLER;
//			}
//			
//			return SAME;
			
			if (this.token > other.token) {
				return LARGER;
			} else if (this.token < other.token){
				return SMALLER;
			}
			return SAME;
		}
		
		public void setToken(long token) {
			this.token = token;
		}
		
		public long getToken() {
			return this.token;
		}
		
		public String toString()
		{
			return this.seconds + "." + this.nanoseconds + " s";
		}
	}
	
	public static class TestItem {
		private AtomicLong stats_num_get;
		private AtomicLong stats_num_get_success;
		private AtomicLong stats_num_set;
		private AtomicLong stats_num_get_stale;
		private AtomicLong stats_num_delete;
		private AtomicLong value_counter;
		private ByteBuffer item_value;
		private Timestamp item_timestamp;
		private long size = 0;
		
		public TestItem()
		{
			stats_num_get = new AtomicLong(0);
			stats_num_get_success = new AtomicLong(0);
			stats_num_set = new AtomicLong(0);
			stats_num_get_stale = new AtomicLong(0);
			stats_num_delete = new AtomicLong(0);
			value_counter = new AtomicLong(0);
			item_value = null;
			setTimestamp(null);
		}
		
		public byte[] getValue() 
		{
			return item_value.array();
		}
		public void setValue(byte[] item_value, int size) 
		{
			this.item_value = ByteBuffer.wrap(item_value, 0, size);
			this.item_value.rewind();
		}
		public long getValueLong() 
		{
			//return item_value.getLong(0);
			return value_counter.getAndIncrement();
		}
		public void setValueLong(long value) 
		{
//			//this.item_value.putLong(0, value);
//			this.value_counter.
			this.value_counter.set(value);
		}
		
		public long getNumGet() 
		{
			return stats_num_get.get();
		}
		public void incrNumGet() 
		{			
			this.stats_num_get.incrementAndGet();
		}
		public long getNumStale() 
		{
			return stats_num_get_stale.get();
		}
		public void incrNumStale() 
		{
			this.stats_num_get_stale.incrementAndGet();
		}
		public long getNumSet() 
		{
			return stats_num_set.get();
		}
		public void incrNumSet() 
		{
			this.stats_num_set.incrementAndGet();
		}
		public long getNumDelete() 
		{
			return stats_num_delete.get();
		}
		public void incrNumDelete() 
		{
			this.stats_num_delete.incrementAndGet();
		}

		public int compareTimestamp(Timestamp miss_timestamp) 
		{
			return item_timestamp.compare(miss_timestamp);
		}

		public void setTimestamp(Timestamp item_timestamp) 
		{
			if(item_timestamp == null) {
				this.item_timestamp = null;
			} else {
				this.item_timestamp = new Timestamp(item_timestamp);
			}
		}
		
		public long getTimestampSeconds()
		{
			return this.item_timestamp.seconds;
		}
		
		public long getTimestampNanoseconds()
		{
			return this.item_timestamp.nanoseconds;
		}
		
		public String toString()
		{
			return "Value=" + this.getValueLong() + ", Timestamp=" + this.item_timestamp.toString();
		}

		public long getNumGetSuccess() {
			return stats_num_get_success.get();
		}

		public void incrNumGetSuccess() {
			this.stats_num_get_success.incrementAndGet();
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}
		
	}
	
	class RunThread extends Thread
	{
		MemcachedClient mc;
		long rand_seed;
		int thread_id;

		public RunThread(int threadID)
		{
			this.thread_id = threadID;
			this.rand_seed = 1;		
			this.mc = new MemcachedClient(POOL_INSTANCE_NAME);
			this.mc.setCompressEnable(false);
		}
		
		private void gotoSleep(long sleep_time)
		{
			try {
				Thread.sleep(sleep_time);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public boolean continueRunning(int count, Date start_time) {
			if(runtime_seconds > 0) {
				return new Date().getTime() - start_time.getTime() < runtime_seconds * 1000;
			} else if(num_iterations > 0) {
				return count < num_iterations;
			} else {
				return true;
			}
		}
		
		public void run()
		{
			Random rand_key = new Random(rand_seed);
			Random rand_delete = new Random(rand_seed);
			Random rand_time = new Random(rand_seed);
			Random rand_size = new Random(rand_seed);
			
			int total_execution_time = 0;
			int key;
			int size;
			int delete_chance = 1;
			TestItem mapValue = null;
			byte[] result = null;
			TestItem resultValue = new TestItem();
			byte[] store_value_buf = new byte[max_value_size];
			TestItem storeValue = new TestItem();
			boolean mc_result = false;
			long sleep_time = max_sleep_time * this.thread_id;
			DistOfAccess distrib = null;
			
			if(key_generation_method == GenerationMethod.ZIPFIAN) {
				distrib = new DistOfAccess(num_keys,"Zipfian",true, 0.27, this.thread_id);	// default mean of 0.27
			}
			
			if(sleep_time == 0) {
				sleep_time = 1;
			}
			
			Date start_time = new Date(); 
			Date lastUpdateTime = new Date();
			
			for(int i = 0; continueRunning(i, start_time); ) {
				if(key_generation_method == GenerationMethod.RANDOM) {
					key = rand_key.nextInt(num_keys);
				} else if(key_generation_method == GenerationMethod.SEQUENTIAL) {
					key = i % num_keys;
				} else if(key_generation_method == GenerationMethod.ZIPFIAN) {
					key = distrib.GenerateOneItem() - 1;
				} else {
					System.out.println("No or Invalid Key generation specified.");
					key = 1;
				}
				
				mapValue = expectedValue.get(key);
				
				result = (byte[])mc.get(getCacheKey(key));
				
				if(result == null) {
					// Key-Value pair not found, attempt to set it.
					
					// Create a new value
					Timestamp ts = new Timestamp(0);
										
					ts.setToken(mc.getLeaseToken(getCacheKey(key)));
					size = rand_size.nextInt(max_value_size - min_value_size) + min_value_size;		
					storeValue.setValue(store_value_buf, size);
					storeValue.setValueLong(mapValue.getValueLong());
					storeValue.setTimestamp(ts);
					
										
					if(sleeptime_generation_method == GenerationMethod.CONSTANT) {
						// No need to do anything, already set
					} else if (sleeptime_generation_method == GenerationMethod.RANDOM) {
						sleep_time = rand_time.nextInt(max_sleep_time);
						if(sleep_time == 0)
						{
							//sleep_time = 1;
						}
					}
					
					// Sleep for some time
					gotoSleep(sleep_time);			
					
					// Try to store it
					try {
						mc_result = mc.set(getCacheKey(key), storeValue.getValue());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mc_result = false;
					}
					
					
					if (mc_result) {
						mapValue.incrNumSet();
					}
					
					mapValue.incrNumGet();
					
				} else {
					resultValue.setValue(result, result.length);
					mapValue.incrNumGetSuccess();
					
					delete_chance = rand_delete.nextInt(1000);
					if (delete_chance < percent_delete_chance * 10) {
						try {
							mc.delete(getCacheKey(key));
							mapValue.incrNumDelete();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				
					mapValue.incrNumGet();
				}
		
				i++;
				
				if(this.thread_id == 0 && 
						new Date().getTime() - lastUpdateTime.getTime() > UPDATE_INTERVAL) {
					total_execution_time += UPDATE_INTERVAL / 1000;
					System.out.println(new Date() + ", " + total_execution_time + "s : Processed " + i + " requests, Random backoff: " + MemcachedClient.getNumBackoff() );
					lastUpdateTime = new Date();
				}
			}
			
		}
	}
	
	public static String CACHE_HOSTNAME = "10.0.0.75";
	public static String CACHE_PORT = "11211";
	public static int LISTENER_PORT = 11111;
	public static String cache_type = "twemcache_leases";
	public static String cache_params = "-m 10000 -t 8 -c 2048 -g 40 -G 10000 -v 4";
	public static String start_command = "/home/dblab/workspace/" + cache_type + "/src/twemcache " + cache_params;
	public static String output_filename = "stats.csv";
	public static final boolean DEBUGMODE = true;
	public static boolean START_CACHE = false;
	public static boolean	RESET_CACHE_DATA = false;
	public static final String 	POOL_INSTANCE_NAME = "multi_thread_test";
	public static final int 	UPDATE_INTERVAL = 10000;
	public static int lease_duration = 10000;
	
	public static String experiment_id = "-1";
	
	private String key_prefix = "profile";
	private static int num_iterations = -1; //10000;
	private static int runtime_seconds = 120;
	private int num_threads = 400;
	private double	percent_delete_chance = 50;
	private int	num_keys = 20000;
	private int min_value_size = 50;
	private int max_value_size = 200;
	private int max_sleep_time = 10;
	
	
	
//	private int num_iterations = 1000000;
//	private int runtime_seconds = -1;
//	private int num_write_threads = 20;
//	private int	num_read_threads = 10;
//	private double	percent_set_write = 50;
//	private int	num_keys = 20000;
//	private int min_value_size = 200;
//	private int max_value_size = 1024 * 7;
//	private int max_timestamp_offset = 1000;
	private Timestamp base_time;
	private long base_time_millis;
	private GenerationMethod sleeptime_generation_method = GenerationMethod.RANDOM;
	private GenerationMethod key_generation_method = GenerationMethod.ZIPFIAN; //GenerationMethod.SEQUENTIAL;
	
	private enum GenerationMethod {
		RANDOM, CONSTANT, SEQUENTIAL, ZIPFIAN
	}
	
	private ConcurrentHashMap<Integer, TestItem> expectedValue;
	
	public Timestamp getCurrentTime2()
	{
		long current_time = System.currentTimeMillis() - base_time_millis;
		assert current_time > 0;
		
		Timestamp time = new Timestamp(getBaseTime());
		time.seconds += (current_time / 1000);
		time.nanoseconds = (current_time % 1000) * 1000000; 	// Convert milliseconds to nanoseconds  
		return time;
	}
	
	public Timestamp getOffsetTime(Timestamp time, long offset)
	{
		Timestamp result = new Timestamp(time);
		result.seconds -= offset / 1000;
		result.nanoseconds -= (offset % 1000) * 1000000;
		
		if(result.nanoseconds < 0) {
			result.seconds -= 1;
			result.nanoseconds += 1000000000;
		}
		return result;
	}
	
	public Timestamp getBaseTime()
	{
		return this.base_time;
	}
	
	private String getCacheKey(int id) 
	{
		return this.key_prefix + id;
	}
	
	public void initialize() throws Exception
	{
		if (START_CACHE) {
			CacheUtilities.stopMemcached(CACHE_HOSTNAME, LISTENER_PORT);
			CacheUtilities.startMemcached(CACHE_HOSTNAME, LISTENER_PORT, start_command);
		}
		
		// initialize the pool for memcache servers
		String serverlist[] = {CACHE_HOSTNAME + ":" + CACHE_PORT};
		Integer[] weights = {1}; 
		
		SockIOPool pool = SockIOPool.getInstance( POOL_INSTANCE_NAME );
		pool.setServers( serverlist );
		pool.setWeights( weights );
		pool.setMaxConn( 250 );
		pool.setNagle( false );
		pool.setHashingAlg( SockIOPool.CONSISTENT_HASH );
		pool.initialize();
		
		MemcachedClient mc = new MemcachedClient(POOL_INSTANCE_NAME);
		Object result;
		
		if(RESET_CACHE_DATA) {
			System.out.println("Resetting cache data");
			for (int i = 0; i < num_keys; i++) {
				try {
					mc.delete(getCacheKey(i));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		
		// Initialize all values and timestamps to 0.
		TestItem value = null;
		this.expectedValue = new ConcurrentHashMap<Integer, TestItem>();
		for(int i = 0; i < num_keys; i++) {
			value = new TestItem();
			value.setValue(new byte[max_value_size], max_value_size);
			value.setValueLong(0);
			value.setTimestamp(new Timestamp(-1, -1));
			//value.setTimestamp(getOffsetTime(getCurrentTime(), max_timestamp_offset));			
						
			//mc.setGumball(getCacheKey(i), value.getTimestampSeconds(), value.getTimestampNanoseconds());
			if(RESET_CACHE_DATA) {
				result = mc.get(getCacheKey(i));
				assert result == null;
				mc.set(getCacheKey(i), value.getValue());
			}
			expectedValue.put(i, value);
//			if( 
//					mc.set(getCacheKey(i), value.getValue()); 
//					) {
//				expectedValue.put(i, value);
//			} else {
//				throw new Exception ("ERROR: could not set initial value for key " + getCacheKey(i));
//			}
		}
	}
	
	public void run()
	{
		System.out.println("Running with: ");
		System.out.println(
				"experiment_id="+experiment_id+", "+
				"num_iterations="+num_iterations+", "+
				"num_threads="+num_threads+", "+
				"percent_delete_chance="+percent_delete_chance+"%, "+
				"num_keys="+num_keys+", "+
				"\n" +
				"min_value_size="+min_value_size+", "+
				"max_value_size="+max_value_size+", "+
				"max_timestamp_offset="+max_sleep_time+", "+
				"sleeptime_generation_method="+sleeptime_generation_method.toString()+", "+
				"key_generation_method="+key_generation_method.toString()+", "+
				"\n"
		);
		// Create n_s and n_g threads.
		Thread run_threads[] = new Thread[num_threads];
		for(int i = 0; i < num_threads; i++) {
			run_threads[i] = new RunThread(i);
		}

		for(int i = 0; i < num_threads; i++) {
			run_threads[i].start();
		}
		
		for(int i = 0; i < num_threads; i++) {
			try {
				run_threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		long stats_num_get = 0;
		long stats_num_get_success = 0;
		long stats_num_set = 0;
		long stats_num_get_stale = 0;
		long stats_num_delete = 0;
		for(Integer key : expectedValue.keySet()) {
			stats_num_get += expectedValue.get(key).getNumGet();
			stats_num_get_stale += expectedValue.get(key).getNumStale();
			stats_num_set += expectedValue.get(key).getNumSet();
			stats_num_delete += expectedValue.get(key).getNumDelete();
			stats_num_get_success += expectedValue.get(key).getNumGetSuccess();
		}
		
		System.out.println("Gets = " + stats_num_get);
		System.out.println("Gets Success = " + stats_num_get_success);
		System.out.println("Stale Gets = " + stats_num_get_stale);
		System.out.println("Sets = " + stats_num_set);
		System.out.println("Deletes = " + stats_num_delete); // TODO: deletes can't be counted the same way, doesn't acquire TestItem from HashMap
		
		System.out.println("Throughput = " + (stats_num_get/runtime_seconds) + " gets/sec");		

		try {
			File outfile = new File(output_filename);
			boolean print_header = false;
			if(!outfile.exists()) {
				print_header = true;
			}
			DataOutputStream fout = new DataOutputStream(new FileOutputStream(output_filename, true));
			if(print_header) {
				fout.writeBytes(
						"Date, Runtime(s), ExperimentID, " +
						"PercentDelete, LeaseDuration, NumThreads, NumKeys, ServiceTime, " +
						"NumGets, NumGetsSuccess, NumSets, NumDeletes, Throughput, " +
						"NumBackoff, " +
						"\r\n");
			}
						
			fout.writeBytes(
					new Date().toString() + ", " + 
					runtime_seconds + ", " + 
					experiment_id + ", " +
					percent_delete_chance + ", " +
					lease_duration + ", " +
					num_threads + ", " +
					num_keys + ", " +
					max_sleep_time + ", " +
					stats_num_get + ", " +
					stats_num_get_success + ", " +
					stats_num_set + ", " +
					stats_num_delete + ", " +
					(stats_num_get / runtime_seconds) + ", " +
					MemcachedClient.getNumBackoff() + ", " +
					"\r\n"
					);
			fout.flush();
			fout.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void shutdown()
	{
		SockIOPool pool = SockIOPool.getInstance( POOL_INSTANCE_NAME );
		pool.shutDown();
	}
	
	public static void main(String [] args) throws Exception
	{
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel( Level.ERROR );
		
		BGSimTestNoLock test = new BGSimTestNoLock();
		int expected_args = 10;
		
		if(args.length > 0 && args.length != expected_args) {
			System.out.println("Invalid number of arguments. Expected " + expected_args);
			return;			
		} else if(args.length == expected_args) {
			int cnt = 0;
			
			BGSimTestNoLock.START_CACHE = "1".equals(args[cnt++]);
			BGSimTestNoLock.RESET_CACHE_DATA = "1".equals(args[cnt++]);
			BGSimTestNoLock.runtime_seconds = Integer.parseInt(args[cnt++]);			
			test.percent_delete_chance = Double.parseDouble(args[cnt++]);
			BGSimTestNoLock.lease_duration = Integer.parseInt(args[cnt++]);
			test.num_threads = Integer.parseInt(args[cnt++]);
			test.num_keys = Integer.parseInt(args[cnt++]);
			test.max_sleep_time = Integer.parseInt(args[cnt++]);
			BGSimTestNoLock.output_filename = args[cnt++];
			BGSimTestNoLock.experiment_id = args[cnt++];
			
			for(int i = 0; i < args.length; i++) {
				BGSimTestNoLock.experiment_id += "+" + args[i];
			}
		}
		
		test.initialize();
		test.run();
		test.shutdown();
	}
}
