package com.meetup.memcached.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;

import com.meetup.memcached.MemcachedClient;
import com.meetup.memcached.SockIOPool;

public class BGSimTest {
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
		private long stats_num_get = 0;
		private long stats_num_get_success = 0;
		private long stats_num_set = 0;
		private long stats_num_get_stale = 0;
		private long stats_num_delete = 0;
		private ByteBuffer item_value;
		private Timestamp item_timestamp;
		private long size = 0;
		
		public TestItem()
		{
			stats_num_get = 0;
			stats_num_set = 0;
			stats_num_get_stale = 0;
			stats_num_delete = 0;
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
			return item_value.getLong(0);
		}
		public void setValueLong(long value) 
		{
			this.item_value.putLong(0, value);
		}
		
		public long getNumGet() 
		{
			return stats_num_get;
		}
		public void incrNumGet() 
		{			
			this.stats_num_get++;
		}
		public long getNumStale() 
		{
			return stats_num_get_stale;
		}
		public void incrNumStale() 
		{
			this.stats_num_get_stale++;
		}
		public long getNumSet() 
		{
			return stats_num_set;
		}
		public void incrNumSet() 
		{
			this.stats_num_set++;
		}
		public long getNumDelete() 
		{
			return stats_num_delete;
		}
		public void incrNumDelete() 
		{
			this.stats_num_delete++;
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
			return stats_num_get_success;
		}

		public void incrNumGetSuccess() {
			this.stats_num_get_success++;
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
		
		public void run()
		{
			Random rand_key = new Random(rand_seed);
			Random rand_delete = new Random(rand_seed);
			Random rand_time = new Random(rand_seed);
			Random rand_size = new Random(rand_seed);
			
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
			
			if(sleep_time == 0) {
				sleep_time = 1;
			}
			
			for(int i = 0; i < num_iterations; ) {
				if(key_generation_method == GenerationMethod.RANDOM) {
					key = rand_key.nextInt(num_keys);
				} else if(key_generation_method == GenerationMethod.SEQUENTIAL) {
					key = i % num_keys;
				} else {
					System.out.println("No or Invalid Key generation specified.");
					key = 1;
				}
				
				mapValue = expectedValue.remove(key);
				if(mapValue == null) {					
					continue;
				}
				
				result = (byte[])mc.get(getCacheKey(key));
				
				if(result == null) {
					// Key-Value pair not found, attempt to set it.
					
					// Create a new value
					Timestamp ts = new Timestamp(0);
//					ts.seconds = mc.getGumballSeconds(getCacheKey(key));
//					ts.nanoseconds = mc.getGumballNanoseconds(getCacheKey(key));
					
					ts.setToken(mc.getLeaseToken(getCacheKey(key)));
					size = rand_size.nextInt(max_value_size - min_value_size) + min_value_size;		
					storeValue.setValue(store_value_buf, size);
					storeValue.setValueLong(mapValue.getValueLong() + 1);
					storeValue.setTimestamp(ts);
					
					expectedValue.put(key, mapValue);
					
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

					mapValue = expectedValue.remove(key);
					while(mapValue == null) {
						mapValue = expectedValue.remove(key);
					}
					
					
					// Try to store it
					try {
						mc_result = mc.set(getCacheKey(key), storeValue.getValue());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mc_result = false;
					}
					
					
					if (mc_result && mapValue.compareTimestamp(ts) <= 0) {
						mapValue.setValueLong(storeValue.getValueLong());
						mapValue.incrNumSet();
						mapValue.setTimestamp(ts);
					}
					
					mapValue.incrNumGet();
					
				} else {
					resultValue.setValue(result, result.length);
					mapValue.incrNumGetSuccess();
										
					if(resultValue.getValueLong() != mapValue.getValueLong()) {
						mapValue.incrNumStale();
						if(DEBUGMODE) {
							System.out.println("Stale: Key " + key + ", Expected " + mapValue.getValueLong() + ", Read " + resultValue.getValueLong());
						}
					}
					
					delete_chance = rand_delete.nextInt(100);
					if (delete_chance < percent_delete_chance) {
						try {
							mc.delete(getCacheKey(key));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					mapValue.incrNumGet();
				}
				
				expectedValue.put(key, mapValue);				
				i++;
				
				if((i % 1000) == 0 && this.thread_id == 0) {
					System.out.println("Processed " + i + " requests");
				}
			}
			
		}
	}
	
	
	
	public static final boolean DEBUGMODE = true;
	public static final String POOL_INSTANCE_NAME = "multi_thread_test";
	
	private String key_prefix = "profile";
	private int num_iterations = 1000;
	private int num_threads = 2;
	private double	percent_delete_chance = 50;
	private int	num_keys = 1;
	private int min_value_size = 200;
	private int max_value_size = 1024 * 3;
	private int max_sleep_time = 10;

	private Timestamp base_time;
	private long base_time_millis;
	private GenerationMethod sleeptime_generation_method = GenerationMethod.RANDOM;
	private GenerationMethod key_generation_method = GenerationMethod.RANDOM; //GenerationMethod.SEQUENTIAL;
	
	private enum GenerationMethod {
		RANDOM, CONSTANT, SEQUENTIAL
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
		// initialize the pool for memcache servers
		String serverlist[] = {"10.0.0.75:11211"};
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
		
		for (int i = 0; i < num_keys; i++) {
			try {
				mc.delete(getCacheKey(i));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			result = mc.get(getCacheKey(i));
			assert result == null;
			if( mc.set(getCacheKey(i), value.getValue()) ) {
				expectedValue.put(i, value);
			} else {
				throw new Exception ("ERROR: could not set initial value for key " + getCacheKey(i));
			}
		}
	}
	
	public void run()
	{
		System.out.println("Running with: ");
		System.out.println(
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
		for(Integer key : expectedValue.keySet()) {
			stats_num_get += expectedValue.get(key).getNumGet();
			stats_num_get_stale += expectedValue.get(key).getNumStale();
			stats_num_set += expectedValue.get(key).getNumSet();
			expectedValue.get(key).getNumDelete();
			stats_num_get_success += expectedValue.get(key).getNumGetSuccess();
		}
		
		System.out.println("Gets = " + stats_num_get);
		System.out.println("Gets Success = " + stats_num_get_success);
		System.out.println("Stale Gets = " + stats_num_get_stale);
		System.out.println("Sets = " + stats_num_set);
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
		
		BGSimTest test = new BGSimTest();
		test.initialize();
		test.run();
		test.shutdown();
	}
}
