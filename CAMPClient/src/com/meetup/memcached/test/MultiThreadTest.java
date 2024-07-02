package com.meetup.memcached.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.meetup.memcached.MemcachedClient;
import com.meetup.memcached.SockIOPool;

public class MultiThreadTest {
	public static class Timestamp {
		public long seconds;
		public long nanoseconds;
		
		private static int LARGER = 1;
		private static int SMALLER = -1;
		private static int SAME = 0;
		
		public Timestamp (long seconds, long nanoseconds) {
			this.seconds = seconds;
			this.nanoseconds = nanoseconds;
		}
		
		public Timestamp (Timestamp other)
		{
			this.seconds = other.seconds;
			this.nanoseconds = other.nanoseconds;
		}
		
		public int compare(Timestamp other)
		{			
			if(this.seconds > other.seconds) {
				return LARGER;
			} else if(this.seconds < other.seconds) {
				return SMALLER;
			} 
			
			if(this.nanoseconds > other.nanoseconds) {
				return LARGER;
			} else if(this.nanoseconds < other.nanoseconds) {
				return SMALLER;
			}
			
			return SAME;
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
	
	class WriterThread extends Thread
	{
		MemcachedClient mc;
		long rand_seed;
		int thread_id;

		public WriterThread(int threadID)
		{
			this.thread_id = threadID;
			this.rand_seed = 1;		
			this.mc = new MemcachedClient(POOL_INSTANCE_NAME);
		}
		
		public void run()
		{
			Random rand_key = new Random(rand_seed);
			Random rand_op = new Random(rand_seed);
			Random rand_time = new Random(rand_seed);
			Random rand_size = new Random(rand_seed);
			
			int key;
			int size;
			int op = 1;
			TestItem mapValue = null;
			byte[] store_value_buf = new byte[max_value_size];
			TestItem storeValue = new TestItem();
			Timestamp miss_timestamp = null;
			boolean executed_op = true;
			boolean mc_result = false;
			long time_offset = max_timestamp_offset * this.thread_id;
			
			if(time_offset == 0) {
				time_offset = 1;
			}
			
			for(int i = 0; i < num_iterations; ) {
				if(executed_op) {
					op = rand_op.nextInt(100);
				}
				
				executed_op = false;		
				
				if(key_generation_method == GenerationMethod.RANDOM) {
					key = rand_key.nextInt(num_keys);
				} else if(key_generation_method == GenerationMethod.SEQUENTIAL) {
					key = i % num_keys;
				} else {
					System.out.println("No or Invalid Key generation specified.");
					key = 1;
				}
				
				if(op < percent_set_write) {
					// Do Set operation
					mapValue = expectedValue.remove(key);
					if(mapValue == null) {					
						continue;
					}

					
										
					// Generate a miss_timestamp
					if(timestamp_generation_method == GenerationMethod.CONSTANT) {
						miss_timestamp = getOffsetTime(getCurrentTime(), time_offset);
					} else if (timestamp_generation_method == GenerationMethod.RANDOM) {
						time_offset = rand_time.nextInt(max_timestamp_offset);
						miss_timestamp = getOffsetTime(getCurrentTime(), time_offset);
					}
					
					size = rand_size.nextInt(max_value_size - min_value_size) + min_value_size;					
					storeValue.setValue(store_value_buf, size);
					storeValue.setValueLong(mapValue.getValueLong() + 1);
					storeValue.setTimestamp(miss_timestamp);
					
					//mc.setGumball(key_prefix + key, miss_timestamp.seconds, miss_timestamp.nanoseconds);
					try {
						mc_result = mc.set(key_prefix + key, storeValue.getValue());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mc_result = false;
					}
					// TODO: check result?
					
					// Only update if timestamp is greater than currently stored
					if(mc_result == true && mapValue.compareTimestamp(miss_timestamp) <= 0) {
						mapValue.setValueLong(storeValue.getValueLong());						
						mapValue.setTimestamp(miss_timestamp);
					}
					mapValue.incrNumSet();
					
					expectedValue.put(key, mapValue);
					
				} else {
					// Do Delete operation
					try {
						mc_result = mc.delete(key_prefix + key);
						// TODO: check result?
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				executed_op = true;
				i++;
			}
		}
	}
	
	class ReaderThread extends Thread
	{
		MemcachedClient mc;
		long rand_seed;
		int thread_id;
		
		public ReaderThread(int threadID)
		{
			this.thread_id = threadID;
			this.rand_seed = 1;		
			this.mc = new MemcachedClient(POOL_INSTANCE_NAME);
		}
		
		public void run()
		{
			int key;
			Random rand = new Random(rand_seed);
			TestItem mapValue = null;
			byte[] result = null;
			TestItem resultValue = new TestItem();
			
			for(int i = 0; i < num_iterations; ) {
				key = rand.nextInt(num_keys);
				mapValue = expectedValue.remove(key);
				
				if(mapValue == null) {					
					continue;
				}
				
				mapValue.incrNumGet();
				result = (byte[])mc.get(key_prefix + key);
				if(result != null) {
					resultValue.setValue(result, result.length);
					mapValue.incrNumGetSuccess();
					if(resultValue.getValueLong() != mapValue.getValueLong()) {
						mapValue.incrNumStale();
						if(DEBUGMODE) {
							System.out.println("Stale: Key " + key + ", Expected " + mapValue.getValueLong() + ", Read " + resultValue.getValueLong());
						}
					}
				}
				
				expectedValue.put(key, mapValue);
				
				i++;
			}
			
		}
	}
	
	
	
	
	public static final boolean DEBUGMODE = false;
	public static final String POOL_INSTANCE_NAME = "multi_thread_test";
	
	private String key_prefix = "profile";
	private int num_iterations = 1000000;
	private int num_write_threads = 10;
	private int	num_read_threads = 5;
	private double	percent_set_write = 100;
	private int	num_keys = 20000;
	private int min_value_size = 200;
	private int max_value_size = 1024 * 7;
	private int max_timestamp_offset = 1000;
	private Timestamp base_time;
	private long base_time_millis;
	private GenerationMethod timestamp_generation_method = GenerationMethod.RANDOM;
	private GenerationMethod key_generation_method = GenerationMethod.RANDOM; //GenerationMethod.SEQUENTIAL;
	
	private enum GenerationMethod {
		RANDOM, CONSTANT, SEQUENTIAL
	}
	
	private ConcurrentHashMap<Integer, TestItem> expectedValue;
	
	public Timestamp getCurrentTime()
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
	
	public void initialize()
	{
		
		
		// initialize the pool for memcache servers
		String serverlist[] = {"10.0.0.200:11211"};
		Integer[] weights = {1}; 
		
		SockIOPool pool = SockIOPool.getInstance( POOL_INSTANCE_NAME );
		pool.setServers( serverlist );
		pool.setWeights( weights );
		pool.setMaxConn( 250 );
		pool.setNagle( false );
		pool.setHashingAlg( SockIOPool.CONSISTENT_HASH );
		pool.initialize();
		
		// Perform a CacheGet to obtain a miss_timestamp. 
		//  Set that as the TS_base_server along with the current time.
		String miss_key = POOL_INSTANCE_NAME + "_reseved_key";
		MemcachedClient mc = new MemcachedClient(POOL_INSTANCE_NAME);
		Object result;
		
		for (int i = 0; i < num_keys; i++) {
			try {
				mc.delete(key_prefix + i);
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
		
		result = mc.get(miss_key);
		assert result == null;
		if(result == null) {
			this.base_time_millis = System.currentTimeMillis();
			this.base_time = new Timestamp(base_time_millis / 1000, (base_time_millis % 1000) * 1000000);
			//this.base_time = new Timestamp(mc.getGumballSeconds(miss_key), mc.getGumballNanoseconds(miss_key));
		} 
			
		
		// Initialize all values and timestamps to 0.
		
		TestItem value = null;
		this.expectedValue = new ConcurrentHashMap<Integer, TestItem>();
		for(int i = 0; i < num_keys; i++) {
			value = new TestItem();
			value.setValue(new byte[max_value_size], max_value_size);
			value.setValueLong(0);
			//value.setTimestamp(new Timestamp(-1, -1));
			value.setTimestamp(getOffsetTime(getCurrentTime(), max_timestamp_offset));
			expectedValue.put(i, value);
						
			//mc.setGumball(key_prefix + i, value.getTimestampSeconds(), value.getTimestampNanoseconds());
			try {
				mc.set(key_prefix + i, value.getValue());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void run()
	{
		System.out.println("Running with: ");
		System.out.println(
				"num_iterations="+num_iterations+", "+
				"num_write_threads="+num_write_threads+", "+
				"num_read_threads="+num_read_threads+", "+
				"percent_set_write="+percent_set_write+"%, "+
				"num_keys="+num_keys+", "+
				"\n" +
				"min_value_size="+min_value_size+", "+
				"max_value_size="+max_value_size+", "+
				"max_timestamp_offset="+max_timestamp_offset+", "+
				"timestamp_generation_method="+timestamp_generation_method.toString()+", "+
				"key_generation_method="+key_generation_method.toString()+", "+
				"\n"
		);
		// Create n_s and n_g threads.
		Thread writer_threads[] = new Thread[num_write_threads];
		Thread reader_threads[] = new Thread[num_read_threads];
		for(int i = 0; i < num_write_threads; i++) {
			writer_threads[i] = new WriterThread(i);
		}

		for(int i = 0; i < num_read_threads; i++) {
			reader_threads[i] = new ReaderThread(i);
		}

		for(int i = 0; i < num_write_threads; i++) {
			writer_threads[i].start();
		}

		for(int i = 0; i < num_read_threads; i++) {
			reader_threads[i].start();
		}
		
		for(int i = 0; i < num_write_threads; i++) {
			try {
				writer_threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for(int i = 0; i < num_read_threads; i++) {
			try {
				reader_threads[i].join();
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
			stats_num_get_success += expectedValue.get(key).getNumGetSuccess();
		}
		
		System.out.println("Gets = " + stats_num_get);
		System.out.println("Gets Success = " + stats_num_get_success);
		System.out.println("Stale Gets = " + stats_num_get_stale);
		System.out.println("Sets = " + stats_num_set);
		//System.out.println("Deletes = " + stats_num_delete); // TODO: deletes can't be counted the same way, doesn't acquire TestItem from HashMap
		
	}
	
	public void shutdown()
	{
		SockIOPool pool = SockIOPool.getInstance( POOL_INSTANCE_NAME );
		pool.shutDown();
	}
	
	public static void main(String [] args)
	{
		MultiThreadTest test = new MultiThreadTest();
		test.initialize();
		test.run();
		test.shutdown();
	}
}
