/**
 * Copyright (c) 2008 Greg Whalin
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the BSD license
 *
 * This library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.
 *
 * You should have received a copy of the BSD License along with this
 * library.
 *
 * @author Kevin Burton
 * @author greg whalin <greg@meetup.com> 
 */
package com.meetup.memcached.test;

import com.meetup.memcached.*;

import java.util.*;
import java.io.IOException;
import java.io.Serializable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

public class UnitTests {

	// logger
	private static Logger log =
			Logger.getLogger( UnitTests.class.getName() );

	public static MemcachedClient mc  = null;
	
	public static final int EXP_LEASE_TIME = 1000;


	public static void test1() {
		try {
			mc.set( "foo", Boolean.TRUE );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Boolean b = (Boolean)mc.get( "foo" );
		assert b.booleanValue();
		log.error( "+ store/retrieve Boolean type test passed" );
	}

	public static void test2() {
		try {
			mc.set( "foo", new Integer( Integer.MAX_VALUE ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Integer i = (Integer)mc.get( "foo" );
		assert i.intValue() == Integer.MAX_VALUE;
		log.error( "+ store/retrieve Integer type test passed" );
	}

	public static void test3() {
		String input = "test of string encoding";
		try {
			mc.set( "foo", input );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		String s = (String)mc.get( "foo" );
		assert s.equals( input );
		log.error( "+ store/retrieve String type test passed" );
	}

	public static void test4() {
		try {
			mc.set( "foo", new Character( 'z' ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Character c = (Character)mc.get( "foo" );
		assert c.charValue() == 'z';
		log.error( "+ store/retrieve Character type test passed" );
	}

	public static void test5() {
		try {
			mc.set( "foo", new Byte( (byte)127 ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Byte b = (Byte)mc.get( "foo" );
		assert b.byteValue() == 127;
		log.error( "+ store/retrieve Byte type test passed" );
	}

	public static void test6() {
		try {
			mc.set( "foo", new StringBuffer( "hello" ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		StringBuffer o = (StringBuffer)mc.get( "foo" );
		assert o.toString().equals( "hello" );
		log.error( "+ store/retrieve StringBuffer type test passed" );
	}

	public static void test7() {
		try {
			mc.set( "foo", new Short( (short)100 ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Short o = (Short)mc.get( "foo" );
		assert o.shortValue() == 100;
		log.error( "+ store/retrieve Short type test passed" );
	}

	public static void test8() {
		try {
			mc.set( "foo", new Long( Long.MAX_VALUE ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Long o = (Long)mc.get( "foo" );
		assert o.longValue() == Long.MAX_VALUE;
		log.error( "+ store/retrieve Long type test passed" );
	}

	public static void test9() {
		try {
			mc.set( "foo", new Double( 1.1 ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Double o = (Double)mc.get( "foo" );
		assert o.doubleValue() == 1.1;
		log.error( "+ store/retrieve Double type test passed" );
	}

	public static void test10() {
		try {
			mc.set( "foo", new Float( 1.1f ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Float o = (Float)mc.get( "foo" );
		assert o.floatValue() == 1.1f;
		log.error( "+ store/retrieve Float type test passed" );
	}

	public static void test11() {
		try {
			mc.set( "foo", new Integer( 100 ), new Date( System.currentTimeMillis() ));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		try { Thread.sleep( 1000 ); } catch ( Exception ex ) { }
		assert mc.get( "foo" ) == null;
		log.error( "+ store/retrieve w/ expiration test passed" );
	}

	public static void test12() {
		long i = 0;
		try {
			mc.storeCounter("foo", i);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		mc.incr("foo"); // foo now == 1
		mc.incr("foo", (long)5); // foo now == 6
		long j = mc.decr("foo", (long)2); // foo now == 4
		assert j == 4;
		assert j == mc.getCounter( "foo" );
		log.error( "+ incr/decr test passed" );
	}

	public static void test13() {
		Date d1 = new Date();
		try {
			mc.set("foo", d1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Date d2 = (Date) mc.get("foo");
		assert d1.equals( d2 );
		log.error( "+ store/retrieve Date type test passed" );
	}

	public static void test14() {
		assert !mc.keyExists( "foobar123" );
		try {
			mc.set( "foobar123", new Integer( 100000) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		assert mc.keyExists( "foobar123" );
		log.error( "+ store/retrieve test passed" );

		assert !mc.keyExists( "counterTest123" );
		try {
			mc.storeCounter( "counterTest123", 0 );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		assert mc.keyExists( "counterTest123" );
		log.error( "+ counter store test passed" );
	}

	@SuppressWarnings("rawtypes")
	public static void test15() {

		Map stats = mc.statsItems();
		assert stats != null;

		stats = mc.statsSlabs();
		assert stats != null;

		log.error( "+ stats test passed" );
	}

	public static void test16() {
		try {
			assert !mc.set( "foo", null );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		log.error( "+ invalid data store [null] test passed" );
	}

	public static void test17() {
		try {
			mc.set( "foo bar", Boolean.TRUE );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		Boolean b = (Boolean)mc.get( "foo bar" );
		assert b.booleanValue();
		log.error( "+ store/retrieve Boolean type test passed" );
	}

	public static void test18() {
		try {
			mc.addOrIncr( "foo" );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		} // foo now == 0
		mc.incr( "foo" ); // foo now == 1
		mc.incr( "foo", (long)5 ); // foo now == 6

		try {
			mc.addOrIncr( "foo" );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		} // foo now 7

		long j = mc.decr( "foo", (long)3 ); // foo now == 4
		assert j == 4;
		assert j == mc.getCounter( "foo" );

		log.error( "+ incr/decr test passed" );
	}

	public static void test19() {
		int max = 100;
		String[] keys = new String[ max ];
		for ( int i=0; i<max; i++ ) {
			keys[i] = Integer.toString(i);
			try {
				mc.set( keys[i], "value"+i );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert(false);
			}
		}

		Map<String,Object> results = mc.getMulti( keys );
		for ( int i=0; i<max; i++ ) {
			assert results.get( keys[i]).equals( "value"+i );
		}
		log.error( "+ getMulti test passed" );
	}

	public static void test20( int max, int skip, int start ) {
		log.warn( String.format( "test 20 starting with start=%5d skip=%5d max=%7d", start, skip, max ) );
		int numEntries = max/skip+1;
		String[] keys = new String[ numEntries ];
		byte[][] vals = new byte[ numEntries ][];

		int size = start;
		for ( int i=0; i<numEntries; i++ ) {
			keys[i] = Integer.toString( size );
			vals[i] = new byte[size + 1];
			for ( int j=0; j<size + 1; j++ )
				vals[i][j] = (byte)j;

			try {
				mc.set( keys[i], vals[i] );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert(false);
			}
			size += skip;
		}

		Map<String,Object> results = mc.getMulti( keys );
		for ( int i=0; i<numEntries; i++ )
			assert Arrays.equals( (byte[])results.get( keys[i]), vals[i] );

		log.warn( String.format( "test 20 finished with start=%5d skip=%5d max=%7d", start, skip, max ) );
	}

	public static void test21() {
		try {
			mc.set( "foo", new StringBuilder( "hello" ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		StringBuilder o = (StringBuilder)mc.get( "foo" );
		assert o.toString().equals( "hello" );
		log.error( "+ store/retrieve StringBuilder type test passed" );
	}

	public static void test22() {
		byte[] b = new byte[10];
		for ( int i = 0; i < 10; i++ )
			b[i] = (byte)i;

		try {
			mc.set( "foo", b );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		assert Arrays.equals( (byte[])mc.get( "foo" ), b );
		log.error( "+ store/retrieve byte[] type test passed" );
	}

	public static void test23() {
		TestClass tc = new TestClass( "foo", "bar", new Integer( 32 ) );
		try {
			mc.set( "foo", tc );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		assert tc.equals( (TestClass)mc.get( "foo" ) );
		log.error( "+ store/retrieve serialized object test passed" );
	}

	public static void test24() {

		String[] allKeys = { "key1", "key2", "key3", "key4", "key5", "key6", "key7" };
		String[] setKeys = { "key1", "key3", "key5", "key7" };

		for ( String key : setKeys ) {
			try {
				mc.set( key, key );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert(false);
			}
		}

		Map<String,Object> results = mc.getMulti( allKeys );

		assert allKeys.length == results.size();
		for ( String key : setKeys ) {
			String val = (String)results.get( key );
			assert key.equals( val );
		}

		log.error( "+ getMulti w/ keys that don't exist test passed" );
	}

	/***
	 * Normal delete, get, set and get again.
	 * @param mc
	 * @throws Exception 
	 */
	public static void test99( MemcachedClient mc) throws Exception
	{
		String key = "key99";
		String value = "value99";
		Object result = null;

		mc.delete(key);
		result = mc.get(key);

		mc.set(key, value);

		result = mc.get(key);
		System.out.println("Result: " + result);
		assert value.equals(result);
	}

	/***
	 * Test get and overwrite transient value
	 * @param mc
	 * @throws Exception 
	 */
	public static void test100( MemcachedClient mc ) throws Exception
	{
		String key = "key100";
		String value = "value100";		
		Object result = null;


		result = mc.get(key);		
		mc.set(key, value);

		mc.delete(key);

		result = mc.get(key);
		//assert value.equals(result);
		value += "new";
		mc.set(key, value);

		result = mc.get(key);
		assert value.equals(result);
	}

	/* disbaled for now
	public static void testSQLTrig( MemcachedClient mc ) throws Exception 
	{
		String key = "keySQLTrig";
		String key2 = "keySQLTrigSecond";
		String value = "valueSQLTrig";	

		String it1 = "test_internal_token";
		String it2 = "test_internal_token_second";
		String it3 = "third_test_internal_token_third";
		Vector<QueryRegistration> registrationList = new Vector<QueryRegistration>();
		QueryRegistration registration = new QueryRegistration("select userid from users where userid=10", false);
		registration.addTrigger("test trigger");
		registration.addInternalToken(it1);
		registration.addInternalToken(it2);	
		registrationList.add(registration);

		mc.delete(key);
		mc.delete(key2);

		Object result = mc.get(key);
		assert result == null;
		mc.setSQLTrig("settrig", key, value, null, null, false, registrationList);

		registration.addInternalToken(it3);	
		mc.setSQLTrig("settrig", key, value + "change", null, null, false, registrationList);

		result = mc.get(key);
		assert result != null;
		assert result.equals(value + "change");

		result = mc.get(key2);
		assert result == null;
		mc.setSQLTrig("settrig", key2, value + "newkey", null, null, false, registrationList);

		result = mc.get(key2);
		assert result != null;
		assert result.equals(value + "newkey");

		// Test delete
		//mc.deleteTemp(it1, null, null);


//		mc.delete(key);
//		mc.delete(key2);
//		
//		Object result = mc.get(key);
//		assert result == null;
//		mc.setSQLTrig("settrig", key, value, null, null, false, registrationList);
//		
//		
//		result = mc.get(key);
//		assert value.equals(result);
//		registration.addInternalToken("test internal token third");	
//		mc.setSQLTrig("settrig", key, value + "change", null, null, false, registrationList);
//		
//		result = mc.get(key);
//		assert result != null;
//		assert result.equals(value + "change");
//		
//		result = mc.get(key2);
//		assert result == null;
//		mc.setSQLTrig("settrig", key2, value + "newkey", null, null, false, registrationList);
//		
//		result = mc.get(key2);
//		assert result != null;
//		assert result.equals(value + "newkey");
	}
	//*/

	public static void testGetMulti( MemcachedClient mc ) 
	{
		String keys[] = {"keySQLTrig", "keySQLTrigSecond"};
		Map<String, Object> results = mc.getMulti(keys);

		for(String ret_key : results.keySet()) {
			System.out.println(ret_key + " : " + results.get(ret_key).toString());
		}
	}

//	public static void testKeyValueOOM( MemcachedClient mc ) throws Exception 
//	{
//		String key_prefix = "testKeyValueOOM";
//		int data_size = 1024 * 10;
//		byte byte_buff[] = new byte[data_size];
//		ByteBuffer data = ByteBuffer.wrap(byte_buff, 0, data_size);
//		String value = "value_" + key_prefix + "_value";
//
//
//		Object result = null;
//
//		for(int i = 0; i < 10000; i++) {
//			//mc.delete(key_prefix + i);
//			result = mc.iqget(key_prefix + i);
//			assert result == null;
//
//			data.rewind();
//			value = "value_" + key_prefix + i + "_value";
//			data.put(value.getBytes());
//			data.rewind();
//			mc.iqset(key_prefix + i, data.array());
//		}
//	}

//	/***
//	 * Test MVCC gumball race condition
//	 * @param mc
//	 * @param mc2
//	 * @throws Exception 
//	 */
//	public static void testGumballMVCCrace( MemcachedClient mc, MemcachedClient mc2 ) throws Exception {
//
//		String key = "key1";
//		String value_v1 = "value1_v1";
//		String value_v2 = "value1_v2";
//		Object result = null;
//
//		mc.delete(key);
//
//		result = mc2.iqget(key);
//		assert result == null;
//
//		result = mc.iqget(key);
//		assert result == null;
//
//		mc.iqset(key, value_v1);
//		result = mc.iqget(key);
//		assert result.equals(value_v1);
//		assert mc.delete(key);
//
//		mc2.iqset(key, value_v2);
//
//		// Check that gumball denied the set
//		result = mc2.iqget(key);
//		assert result == null;
//
//		mc2.iqset(key, value_v2);
//		result = mc2.iqget(key);
//		assert result.equals(value_v2);		
//	}
//
//
//	/***
//	 * Test gumball race condition
//	 * @param mc
//	 * @param mc2
//	 * @throws Exception 
//	 */
//	public static void testGumballRace( MemcachedClient mc ) throws Exception {
//
//		String key = "key1";
//		String value_v1 = "value1_v1";
//		String value_v2 = "value1_v2";
//		Object result = null;
//
//		mc.delete(key);
//		result = mc.iqget(key);
//		assert result == null;
//
//		mc.delete(key);
//
//		mc.iqset(key, value_v1);
//		result = mc.iqget(key);
//
//		// Check that gumball denied the set
//		assert result == null;
//
//		// Test a proper set now
//		result = mc.iqget(key);
//		mc.iqset(key, value_v2);
//		result = mc.iqget(key);
//		assert value_v2.equals(result);		
//	}

	public static void testDeleteRace( MemcachedClient mc, MemcachedClient mc2 ) throws Exception {
		String key = "key1";
		String value_v1 = "value1_v1";
		String value_v2 = "value1_v2";
		Object result = null;

		mc.delete(key);
		result = mc.iqget(key);
		assert result == null;

		mc2.delete(key);

		mc.iqset(key, value_v1);
		result = mc.iqget(key);		
		assert result == null;		

		mc.iqset(key, value_v2);
		result = mc.iqget(key);
		assert value_v2.equals(result);
	}

	public static void testExponentialBackoff( MemcachedClient mc, MemcachedClient mc2 ) throws Exception {

	}


//	public static void testGumballOrder( MemcachedClient mc, MemcachedClient mc2 ) throws Exception {
//
//		String key = "key1";
//		String value_v1 = "value1_v1";
//		String value_v2 = "value1_v2";
//		Object result = null;
//
//		mc.delete(key);
//		result = mc.iqget(key);
//		assert result == null;
//
//		result = mc2.iqget(key);
//		assert result == null;
//
//		mc2.iqset(key, value_v2);
//
//		mc.iqset(key, value_v1);
//		result = mc.iqget(key);
//		assert value_v2.equals(result);		
//	}

	public static void testLease( MemcachedClient mc ) throws Exception {
		String key = "testleasekey1";
		String value = "tempvalue1";
		Object ret_value = null;

		// Disable backoff. So that get after lease doesn't wait.
		mc.disableBackoff();

		mc.delete(key);

		assert mc.iqget(key) == null;			
		assert mc.iqset(key, value);		
		ret_value = mc.iqget(key);
		assert value.equals(ret_value);

		// Re-enable backoff for other tests.
		mc.enableBackoff();

		System.out.println("OK");
	}

	public static void testLeaseConflict( MemcachedClient mc ) throws Exception {
		String key = "testleasekey2";
		String value = "tempvalue2";
		Object ret_value = null;

		// Disable backoff. So that get after lease doesn't wait.
		mc.disableBackoff();

		mc.delete(key);

		assert mc.iqget(key) == null;	
		mc.delete(key);
		assert mc.iqset(key, value) == false;

		assert mc.iqget(key) == null;
		assert mc.iqset(key, value);
		ret_value = mc.iqget(key);
		assert value.equals(ret_value);

		// Re-enable backoff for other tests.
		mc.enableBackoff();

		System.out.println("OK");
	}

	public static void testLeaseMultiple( MemcachedClient mc1, MemcachedClient mc2 ) throws Exception {
		String key = "testleasekey3";
		String value = "tempvalue3";
		Object ret_value = null;

		// Disable backoff. So that get after hold doesn't wait.
		mc1.disableBackoff();
		mc2.disableBackoff();

		mc1.delete(key);
		assert mc1.iqget(key) == null;
		assert mc2.iqget(key) == null;	// Should also fail to acquire lease because mc1 has it

		assert mc1.iqset(key, value);
		ret_value = mc1.iqget(key);
		assert value.equals(ret_value);	
		ret_value = mc2.iqget(key);
		assert value.equals(ret_value);

		// Re-enable backoff for other tests.
		mc1.enableBackoff();
		mc2.disableBackoff();
		System.out.println("OK");
	}

//	public static void testHoldRelease( MemcachedClient mc ) throws Exception {
//		String key = "testholdkey1";
//		mc.writeHold(key);
//		mc.releaseHold(key);
//		System.out.println("OK");
//	}
//
//	public static void testHoldAfterGetMiss( MemcachedClient mc ) throws Exception {
//		String key = "testholdkey2";
//		String value = "tempvalue2";
//		Object ret_value = null;
//
//		// Disable backoff. So that get after hold doesn't wait.
//		mc.disableBackoff();
//
//		mc.delete(key);
//
//		assert mc.iqget(key) == null;
//
//		assert mc.writeHold(key);
//
//		assert mc.iqset(key, value) == false;	// This set should fail because the lease token won't match
//
//		assert mc.iqget(key) == null;	// Someone else is holding the lease
//
//		assert mc.releaseHold(key);
//
//		assert mc.iqget(key) == null;	// Should successfully obtain lease
//
//		assert mc.iqset(key, value);
//
//		ret_value = mc.iqget(key);
//		assert value.equals(ret_value);
//
//		// Re-enable backoff for other tests.
//		mc.enableBackoff();
//
//		System.out.println("OK");
//	}
//
//	public static void testHoldAfterSet(MemcachedClient mc) throws Exception {
//		String key = "testholdkey3";
//		String value = "tempvalue3";
//		Object ret_value = null;
//
//		// Disable backoff. So that get after hold doesn't wait.
//		mc.disableBackoff();
//
//		mc.delete(key);
//		assert mc.iqget(key) == null;
//		assert mc.iqset(key, value);
//
//		ret_value = mc.iqget(key);
//		assert value.equals(ret_value);
//
//		assert mc.writeHold(key);
//		ret_value = mc.iqget(key);
//		assert value.equals(ret_value);
//		assert mc.releaseHold(key);
//
//		assert mc.iqget(key) == null;
//
//		// Re-enable backoff for other tests.
//		mc.enableBackoff();
//		System.out.println("OK");
//	}
//
//	public static void testMultipleHold(MemcachedClient mc, MemcachedClient mc2) throws Exception {
//		String key = "testholdkey4";
//		String value = "tempvalue4";
//		Object ret_value = null;
//
//		// Disable backoff. So that get after hold doesn't wait.
//		mc.disableBackoff();
//		mc2.disableBackoff();
//
//		mc.delete(key);
//		assert mc.writeHold(key);
//		assert mc.iqget(key) == null;
//		assert mc2.writeHold(key);
//		assert mc.releaseHold(key);
//
//		// Hold by mc2 should not be release yet, so this sequence will fail.
//		assert mc.iqget(key) == null;
//		assert mc.iqset(key, value) == false;
//		assert mc.iqget(key) == null;
//
//		assert mc2.releaseHold(key);
//
//		// Hold by mc2 now released, so this sequence will succeed.
//		assert mc.iqget(key) == null;
//		assert mc.iqset(key, value);
//		ret_value = mc.iqget(key);
//		assert value.equals(ret_value);
//
//		// Re-enable backoff for other tests.
//		mc.enableBackoff();
//		mc2.enableBackoff();
//		System.out.println("OK");
//	}

	public static void runAlTests( MemcachedClient mc, MemcachedClient mc2 ) throws Exception {
		//test99(mc);
		//testGumballMVCCrace(mc, mc2);
		test14();
		for ( int t = 0; t < 2; t++ ) {
			mc.setCompressEnable( ( t&1 ) == 1 );

			test1();
			test2();
			test3();
			test4();
			test5();
			test6();
			test7();
			test8();
			test9();
			test10();
//			test11();
			test12();
			test13();
			test15();
			test16();
			test17();
			test21();
			test22();
			test23();
			test24();

			for ( int i = 0; i < 3; i++ )
				test19();

			test20( 8191, 1, 0 );
			test20( 8192, 1, 0 );
			test20( 8193, 1, 0 );

			test20( 16384, 100, 0 );
			test20( 17000, 128, 0 );

			test20( 128*1024, 1023, 0 );
			test20( 128*1024, 1023, 1 );
			test20( 128*1024, 1024, 0 );
			test20( 128*1024, 1024, 1 );

			test20( 128*1024, 1023, 0 );
			test20( 128*1024, 1023, 1 );
			test20( 128*1024, 1024, 0 );
			test20( 128*1024, 1024, 1 );

			test20( 900*1024, 32*1024, 0 );
			test20( 900*1024, 32*1024, 1 );
		}

	}

	public static void testReadLease(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		testLease(mc);
		testLeaseConflict(mc);
		testLeaseMultiple(mc, mc2);
	}

//	public static void testWriteLease(MemcachedClient mc, MemcachedClient mc2) throws Exception {
//		testHoldRelease(mc);
//		testHoldAfterSet(mc);
//		testHoldAfterGetMiss(mc);
//		testMultipleHold(mc, mc2);
//	}

	public static void testRead(MemcachedClient mc) throws Exception {
		String key = "key1", value = "value1";

		// try to empty first
		mc.delete(key);		
		assert mc.iqget(key) == null;

		// put new value into the cache
		mc.iqset(key, value);		
		assert mc.iqget(key).equals(value);

		System.out.println("OK READ");
	}
	
	public static void testQARead_RMW(MemcachedClient mc) throws Exception {
		String key = "qareadkey1";
		String value = "value1";
		String value2 = "value2";
		
		// Clean the cache.
		mc.delete(key);
		assert mc.iqget(key) == null;
		
		// put new value into the cache
		mc.iqset(key, value);		
		assert mc.iqget(key).equals(value);
		
		assert value.equals(mc.quarantineAndRead(key));		
		mc.swapAndRelease(key, value2);
		
		assert value2.equals(mc.iqget(key));
		
		System.out.println("testQARead_RMW OK");
	}
	
	public static void testQARead_RMWemptyCache(MemcachedClient mc) throws Exception {
		String key = "qareadkey1";
		String value = "value1";
		
		// Clean the cache.
		mc.delete(key);
		
		// SaR with null value
		assert mc.quarantineAndRead(key) == null;		
		mc.swapAndRelease(key, null);
		// There should be nothing.
		assert mc.iqget(key) == null;
		assert mc.iqset(key, value) == true;
		assert value.equals(mc.iqget(key));
		
		System.out.println("testQARead_RMWemptyCache OK");
	}
	
	public static void testQARead_RMWemptyCacheIlease(MemcachedClient mc) throws Exception {
		String key = "qareadkey1";
		String value = "value1";
		
		// Clean the cache.
		mc.delete(key);
		assert mc.iqget(key) == null;
		
		// Case 1: SaR with null value
		assert mc.quarantineAndRead(key) == null;		
		mc.swapAndRelease(key, null);
		
		// There should be nothing.
		assert mc.iqget(key) == null;
		
		// Case 2: SaR with a value
		assert mc.quarantineAndRead(key) == null;		
		mc.swapAndRelease(key, value);
		
		assert value.equals(mc.iqget(key));		
		
		System.out.println("testQARead_RMWemptyCacheIlease OK");
	}
	
	public static void testQARead_RMWfailedQuarantine(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key = "qareadkey1";
		String value = "value1";
		String value2 = "value2";
		
		// Clean the cache.
		mc.delete(key);
		assert mc.quarantineAndRead(key) == null;
		
		try {
			mc2.quarantineAndRead(key);
			// Exception should be thrown before this.
			assert false;
		} catch (IQException e) {
			
		}
		
		// This swap should fail.
		mc2.swapAndRelease(key, value);
		
		mc.swapAndRelease(key, value2);
		assert value2.equals(mc.iqget(key));
		
		System.out.println("testQARead_RMWfailedQuarantine OK");
	}

	public static void testRMW(MemcachedClient mc) throws Exception {
		String key = "key1", value = "value1", newVal = "newVal", retVal = null;

		// try to empty first
		mc.delete(key);
		assert mc.iqget(key) == null;

		// put new value into the cache
		mc.iqset(key, value);
		assert mc.iqget(key).equals(value);

		// try to write hold, should return the token successfully
		assert mc.quarantineAndRead(key).equals(value);

		// release token and set new value
		mc.swapAndRelease(key, newVal);

		// client now should get the new value
		retVal = (String)mc.iqget(key);
		assert newVal.equals(retVal);

		System.out.println("OK RMW");
	}

	public static void testReadOnWriteLease(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key = "key1", value = "value1";

		mc2.disableBackoff();

		// try to empty first
		mc1.delete(key);
		assert mc1.iqget(key) == null;

		// put new value into the cache
		mc1.iqset(key, value);
		assert mc1.iqget(key).equals(value);

		// try to write hold, should return the token successfully
		assert mc1.quarantineAndRead(key).equals(value);

		// try to read the value, should read the value
		assert mc2.iqget(key).equals(value);

		System.out.println("OK READ_ON_WRITE_LEASE");
	}

	public static void testWriteOnWriteLease(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key = "key1", value = "value1";

		mc2.disableBackoff();

		// try to empty first
		mc1.delete(key);
		assert mc1.iqget(key) == null;

		// put new value into the cache
		mc1.iqset(key, value);
		assert mc1.iqget(key).equals(value);

		// try to write hold, should return the token successfully
		assert mc1.quarantineAndRead(key).equals(value);	

		// mc2 try to request for token
		try {
			mc2.quarantineAndRead(key);
			assert false;
		} catch (IQException e) { }

		System.out.println("OK_WRITE_ON_WRITE_LEASE");
	}

	public static void testWriteAfterWriteLease(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key = "key1", value = "value1", newVal = "newVal";

		mc2.disableBackoff();

		// try to empty first
		mc1.delete(key);
		assert mc1.iqget(key) == null;

		// put new value into the cache
		mc1.iqset(key, value);
		assert mc1.iqget(key).equals(value);

		// try to write hold, should return the token successfully
		assert mc1.quarantineAndRead(key).equals(value);

		// release token and set new value
		mc1.swapAndRelease(key, newVal);

		// mc2 try to request for token
		assert mc2.quarantineAndRead(key).equals(newVal);

		// release token and set new value
		mc2.swapAndRelease(key, "something_new");

		System.out.println("OK_WRITE_AFTER_WRITE_LEASE");
	}	

	public static void testReleaseTokenWithoutXLease(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key = "key1", value = "value1", newVal = "newVal";

		mc.disableBackoff();
		mc2.disableBackoff();

		// try to empty first
		mc.delete(key);
		assert mc.iqget(key) == null;

		// put new value into the cache
		mc.iqset(key, value);
		assert mc.iqget(key).equals(value);

		// try to release token 
		// release token and set new value
		mc.swapAndRelease(key, newVal);

		// read the value again, it should be the old value
		assert mc.iqget(key).equals(value);

		// try to get a token
		assert mc.quarantineAndRead(key).equals(value);

		// try to release token, should fail
		mc2.swapAndRelease(key, newVal);
		assert mc2.iqget(key).equals(value);

		System.out.println("OK_RELEASE_TOKEN_WITHOUT_XLEASE");
	}

	public static void testRMWLeaseTimedOut(MemcachedClient mc) throws Exception {
		String key = "key1", value = "value1", newVal = "newVal";

		mc.disableBackoff();

		// try to empty first
		mc.delete(key);
		assert mc.iqget(key) == null;

		// put new value into the cache
		mc.iqset(key, value);
		assert mc.iqget(key).equals(value);

		// try to get token
		assert mc.quarantineAndRead(key).equals(value);

		Thread.sleep(EXP_LEASE_TIME + 1000);

		mc.swapAndRelease(key, newVal);

		assert mc.iqget(key) == null;

		System.out.println("OK_TEST_RMW_TIMED_OUT");
	}


	// Test that when multiple keys are xLeased, an xLease failure of a later key will
	// cause all earlier keys to be released.
	// mc1 wants to xLease key1 and key2. 
	// 1. mc1 successfully xLeases key1. 
	// 2. mc2 sneaks in and xLeases key2.
	// 3. when mc1 tries to xLease key2, it should fail. 
	// 4. after this, mc2 should be able to xLease key1.
	public static void testFailedXLease(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = "testxleasekey6a";
		String key2 = "testxleasekey6b";
		String value = "tempvalue6";
		String new_value1 = "tempvalue6_new1";
		String new_value2 = "tempvalue6_new2";

		// Disable backoff. So that get after hold doesn't wait in this test.
		mc1.disableBackoff();
		mc2.disableBackoff();

		// Empty the cache
		mc1.delete(key1);
		mc1.delete(key2);

		// Assign a value for key1 in the cache
		assert mc1.iqget(key1) == null;
		mc1.iqset(key1, value);
		assert mc1.iqget(key1).equals(value);

		// Assign a value for key2 in the cache
		assert mc1.iqget(key2) == null;
		mc1.iqset(key2, value);
		assert mc1.iqget(key2).equals(value);

		assert mc1.quarantineAndRead(key1).equals(value);
		assert mc2.quarantineAndRead(key2).equals(value);

		try {
			mc1.quarantineAndRead(key2);
			assert false;
		} catch (IQException e) { }
		
		assert mc2.quarantineAndRead(key1).equals(value);

		// Attempt to swap with mc1
		mc1.swapAndRelease(key1, new_value1);		// This should fail because lease is lost.
		mc1.swapAndRelease(key2, new_value1);		// This should fail because lease is lost.

		// Check that the value is still the old one
		assert mc1.iqget(key1).equals(value);
		assert mc1.iqget(key2).equals(value);

		// Do the actual swaps with mc2
		mc2.swapAndRelease(key1, new_value2);
		mc2.swapAndRelease(key2, new_value2);

		// Check that the value is now the new one
		assert mc1.iqget(key1).equals(new_value2);
		assert mc2.iqget(key2).equals(new_value2);


		// Re-enable backoff for other tests.
		mc1.enableBackoff();
		mc2.enableBackoff();
		System.out.println("OK FAILED_XLEASE_CLEANUP");
	}

	public static void testGetNoLease(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key = "testGetNoLeasekey1", value = "testvalue1";

		// Try getting when the cache is empty (no lease token or value).
		// This call assumes the cache is empty to begin with, which is not
		// necessarily true, so no assert checks here.
		mc.get(key);

		mc.delete(key);
		assert mc.get(key) == null;
		assert mc2.iqget(key) == null;
		assert mc.get(key) == null;
		assert mc2.iqset(key, value);
		assert value.equals(mc2.iqget(key));

		assert value.equals(mc.get(key));
		assert value.equals(mc2.get(key));
		System.out.println("OK GET_NO_LEASE");
	}

	public static void testReleaseX(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key = "testReleaseXkey1", value = "testvalue1";

		// Prevent backoff for this test.
		mc2.disableBackoff();

		// Clear key from the cache.
		mc1.delete(key);

		// mc1 acquires the lease
		assert mc1.iqget(key) == null;

		// mc2 should fail to acquire a lease and not be able to set
		assert mc2.iqget(key) == null;
		try { 
			mc2.iqset(key, value);
			assert false;
		} catch (IQException e) {
			
		}
		
		assert mc2.iqget(key) == null;

		// mc1 releases the lease. mc2 should now be able to get and set
		assert mc1.releaseX(key);

		assert mc2.iqget(key) == null;
		assert mc2.iqset(key, value);
		assert mc2.quarantineAndRead(key).equals(value);
		mc2.swapAndRelease(key, "newVal");

		//		assert mc2.get(key) == null;
		//		assert mc2.set(key, value);
		//		assert value.equals(mc2.get(key));

		// Reset backoff setting.
		mc2.enableBackoff();

		System.out.println("OK RELEASE_X");
	}

	/** Testing cases for SXQ cache **/
	/** @author hieun 
	 * @throws Exception **/

	/*
	 * Testing one client QaReg and DaR
	 */
	public static void sxqQaReg(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = "key1", value1 = "value1";

		// Prevent backoff for this test.
		mc1.disableBackoff();

		// Clear key from the cache.
		mc1.delete(key1);

		// mc1 acquires the lease
		assert mc1.iqget(key1) == null;		

		// mc1 should store value successfully
		assert mc1.iqset(key1, value1);

		// notice KVS for invalidated key
		String tid = UUID.randomUUID().toString();

		assert mc1.quarantineAndRegister(tid, key1);

		assert mc2.iqget(key1).equals(value1);

		assert mc1.deleteAndRelease(tid);

		assert mc1.iqget(key1) == null;

		System.out.println("SXQ_QAREG_OK");
	}

	/*
	 * Test one client try to QaReg multiple-keys
	 */
	public static void sxqQaReg2(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = "key1", value1 = "value1";
		String key2 = "key2", value2 = "value2";
		String key3 = "key3", value3 = "value3";

		// Prevent backoff for this test.
		mc1.disableBackoff();

		// Clear key from the cache.
		mc1.delete(key1);
		mc1.delete(key2);
		mc1.delete(key3);

		// mc1 acquires the lease
		assert mc1.iqget(key1) == null;		
		assert mc1.iqget(key2) == null;
		assert mc1.iqget(key3) == null;

		// mc1 should store value successfully
		assert mc1.iqset(key1, value1);
		assert mc1.iqset(key2, value2);
		assert mc1.iqset(key3, value3);

		// notice KVS for invalidated key
		String tid = UUID.randomUUID().toString();

		assert mc1.quarantineAndRegister(tid, key1);
		assert mc1.quarantineAndRegister(tid, key2);
		assert mc1.quarantineAndRegister(tid, key3);

		assert mc1.deleteAndRelease(tid);

		assert mc1.iqget(key1) == null;
		assert mc2.iqget(key2) == null;
		assert mc2.iqget(key3) == null;

		System.out.println("SXQ_QAREG_2_OK");
	}	

	/**
	 * Test multiple QaReg
	 */
	public static void sxqQaReg3(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1="key1", key2="key2", key3="key3", key4="key4", key5="key5";
		String val1="val1", val2="val2", val3="val3", val4="val4", val5="val5";

		// Prevent backoff for this test.
		mc1.disableBackoff();
		mc2.disableBackoff();

		// Clear key from the cache.
		mc1.delete(key1);
		mc1.delete(key2);
		mc1.delete(key3);
		mc1.delete(key4);
		mc1.delete(key5);

		// mc1 acquires the lease
		assert mc1.iqget(key1) == null;		
		assert mc1.iqget(key2) == null;
		assert mc1.iqget(key3) == null;
		assert mc1.iqget(key4) == null;
		assert mc1.iqget(key5) == null;

		// mc1 should store value successfully
		assert mc1.iqset(key1, val1);
		assert mc1.iqset(key2, val2);
		assert mc1.iqset(key3, val3);
		assert mc1.iqset(key4, val4);
		assert mc1.iqset(key5, val5);

		String tid = UUID.randomUUID().toString();
		String tid2 = UUID.randomUUID().toString();

		assert mc1.quarantineAndRegister(tid, key1);
		assert mc1.quarantineAndRegister(tid, key2);
		assert mc1.quarantineAndRegister(tid, key3);

		assert mc2.quarantineAndRegister(tid2, key3);
		assert mc2.quarantineAndRegister(tid2, key4);
		assert mc2.quarantineAndRegister(tid2, key5);

		assert mc1.deleteAndRelease(tid);		
		assert mc1.iqget(key1) == null;
		assert mc1.iqget(key2) == null;
		assert mc1.iqget(key3) == null;

		try {
			mc1.iqset(key3, "testVal");
			assert false;
		} catch (IQException e) {}
		assert mc1.iqget(key3) == null;

		assert mc2.deleteAndRelease(tid2);
		assert mc2.iqget(key3) == null;
		assert mc2.iqget(key4) == null;
		assert mc2.iqget(key5) == null;	

		System.out.println("SXQ_QAREG_3_OK");
	}

	/**
	 * Test QaReg with no key exists
	 */
	public static void sxq4(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = UUID.randomUUID().toString();
		String value1="value1";

		// test QaReg with no key exists
		String tid = UUID.randomUUID().toString();
		assert mc1.quarantineAndRegister(tid, key1) == true;

		mc2.disableBackoff();

		assert mc2.iqget(key1) == null;

		// this should fail because the previous get should not be granted a shared lease
		try {
			mc2.iqset(key1, value1);
			assert false;
		} catch (Exception e) {
			
		}

		assert mc1.deleteAndRelease(tid) == true;

		System.out.println("SXQ_4_OK");
	}

	/**
	 * Try Set and SaR during Q leases
	 */
	public static void sxq5(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = "key1", val1="val1", val2="val2";

		mc1.disableBackoff();

		mc1.delete(key1);

		// a lease should be returned here
		assert mc1.iqget(key1) == null;

		// wait 2 sec for the lease to expire
		Thread.sleep(2000);

		// now QaReg should be granted a lease
		String tid = UUID.randomUUID().toString();
		assert mc2.quarantineAndRegister(tid, key1) == true;

		// set should fail because token does not match
		assert mc1.iqset(key1, val1) == false;

		assert mc2.deleteAndRelease(tid);

		// a shared lease now can be granted here
		mc1.iqget(key1);
		assert mc1.iqset(key1, val1);
		assert mc1.iqget(key1).equals(val1);

		// a x lease can be grated here
		assert mc1.quarantineAndRead(key1).equals(val1);

		// wait an amount of time for x lease to expire
		Thread.sleep(EXP_LEASE_TIME + 1000);

		// now QaReg should be granted a lease
		assert mc2.quarantineAndRegister(tid, key1) == true;

		// should fail
		mc1.swapAndRelease(key1, "new_val");		
		assert mc1.iqget(key1) == null;

		// release Q lease, so the following get and set can success
		assert mc2.deleteAndRelease(tid);		
		assert mc1.iqget(key1) == null;		
		assert mc1.iqset(key1, val2);

		assert mc1.iqget(key1).equals(val2);

		System.out.println("SXQ_5_OK");
	}

	/**
	 * Test QaReg in items that have S lease or X lease
	 */
	public static void sxq6(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1="key1", val1="val1";

		mc1.disableBackoff();

		mc1.delete(key1);

		assert mc1.iqget(key1) == null;

		// QaReg here should be ok
		String tid = UUID.randomUUID().toString();
		assert mc2.quarantineAndRegister(tid, key1);

		// set then cannot successcdes
		assert mc1.iqset(key1, val1) == false;

		// delete and release Q lease
		assert mc2.deleteAndRelease(tid);

		assert mc1.iqget(key1) == null;
		assert mc1.iqset(key1, val1);

		assert mc1.iqget(key1).equals(val1);

		// grant an x lease for mc1
		assert mc1.quarantineAndRead(key1).equals(val1);

		// should fail to grant a Q lease here
		assert mc2.quarantineAndRegister(tid, key1) == false;

		mc1.swapAndRelease(key1, "new_val");

		assert mc1.iqget(key1).equals("new_val");

		System.out.println("SXQ_6_OK");
	}

	/**
	 * Test DaR
	 */
	public static void sxq7(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = UUID.randomUUID().toString();

		mc1.disableBackoff();

		String tid = UUID.randomUUID().toString();
		assert mc1.deleteAndRelease(tid) == false;

		mc1.delete(key1);
		assert mc1.deleteAndRelease(tid) == false;

		mc1.iqget(key1);
		assert mc1.deleteAndRelease(tid) == false;

		mc1.iqset(key1, "val1");
		assert mc1.iqget(key1).equals("val1");

		assert mc1.quarantineAndRead(key1).equals("val1");
		assert mc1.deleteAndRelease(tid) == false;
		mc1.swapAndRelease(key1, "newVal");

		assert mc1.iqget(key1).equals("newVal");

		mc1.quarantineAndRegister(tid, key1);
		assert mc1.deleteAndRelease(UUID.randomUUID().toString()) == false;
		assert mc1.deleteAndRelease(tid) == true;

		assert mc1.iqget(key1) == null;
		assert mc1.iqset(key1, "val2");

		assert mc1.iqget(key1).equals("val2");

		System.out.println("SXQ_7_OK");
	}

	/**
	 * Test QaC when item has no value, shared leases or Qinv
	 * @param mc1
	 * @param mc2
	 * @throws Exception
	 */
	public static void sxq8(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1=UUID.randomUUID().toString(), val1="val1";

		assert mc1.quarantineAndRead(key1) == null;

		mc1.delete(key1);
		assert mc1.quarantineAndRead(key1) == null;

		// item should not be granted a shared lease here
		assert mc1.iqget(key1) == null;

		// QaRead should not backoff
		assert mc1.quarantineAndRead(key1) == null;

		try {
			mc1.iqset(key1, "temp");
			assert false;
		} catch (IQException e) {}
		
		mc1.swapAndRelease(key1, val1);
		mc1.swapAndRelease(key1, "temp2");
		mc1.swapAndRelease(key1, "temp3");
		mc1.swapAndRelease(key1, "temp4");

		assert mc1.iqget(key1).equals(val1);

		// now item has value, so QaReg should be success
		assert mc1.quarantineAndRead(key1).equals(val1);
		mc1.swapAndRelease(key1, "newVal");

		assert mc1.iqget(key1).equals("newVal");

		//mc2.disableBackoff();		
		String tid2 = UUID.randomUUID().toString();
		assert mc2.quarantineAndRegister(tid2, key1) == true;

		assert mc1.quarantineAndRead(key1).equals("newVal");
		mc1.swapAndRelease(key1, "temp");		// this should fail		

		assert mc1.deleteAndRelease(tid2) == true;		
		assert mc1.iqget(key1) == null;

		System.out.println("SXQ_8_OK");
	}

	/**
	 * Use the same tid for 2 QaReg session
	 */
	public static void sxq9(MemcachedClient mc1) throws Exception {
		String key1="key1", val1="val1";
		String key2="key2";
		String key3="key3", val3="val3";
		String key4="key4";
		String key5="key5";

		mc1.delete(key1);
		mc1.delete(key2);
		mc1.delete(key3);
		mc1.delete(key4);
		mc1.delete(key5);

		mc1.iqget(key1); mc1.iqset(key1, val1);
		//mc1.get(key2); mc1.set(key2, val2);
		mc1.iqget(key3); mc1.iqset(key3, val3);

		String tid = UUID.randomUUID().toString();
		mc1.quarantineAndRegister(tid, key1);
		mc1.quarantineAndRegister(tid, key2);
		mc1.quarantineAndRegister(tid, key3);

		assert mc1.deleteAndRelease(tid);

		assert mc1.deleteAndRelease(tid) == false;

		mc1.quarantineAndRegister(tid, key3);
		mc1.quarantineAndRegister(tid, key4);
		mc1.quarantineAndRegister(tid, key5);

		assert mc1.deleteAndRelease(tid) == true;

		assert mc1.deleteAndRelease(tid) == false;

		System.out.println("SXQ_9_OK");
	}

	public static void test_incr(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = UUID.randomUUID().toString();
		long val1 = 100;

		mc2.enableBackoff();

		// no lease, no value
		assert mc1.incr(key1, 5) == -1;
		assert mc1.incr(key1) == -1;

		// i-lease, no value
		assert mc2.iqget(key1) == null;
		assert mc1.incr(key1) == -1;
		assert mc1.incr(key1, 5) == -1;
		mc2.iqset(key1, val1);
		assert mc1.iqget(key1).equals(val1);

		// test set negative and zero value
		mc2.delete(key1);
		mc2.iqget(key1);
		mc2.iqset(key1, "7");
		mc1.iqget(key1).equals("7");
		assert mc1.incr(key1) == 8;
		assert mc1.incr(key1, 5) == 13;
		mc2.delete(key1);
		mc2.iqget(key1);
		mc2.iqset(key1, "-7");
		mc1.iqget(key1).equals("-7");
		assert mc1.incr(key1) == -1;
		assert mc1.incr(key1, 5) == -1;    

		// Qinv lease (has or has not value)
		mc2.delete(key1);
		String tid = UUID.randomUUID().toString();
		mc2.quarantineAndRegister(tid, key1);
		assert mc1.incr(key1) == -1;
		assert mc1.incr(key1, 5) == -1;
		mc2.deleteAndRelease(tid);     
		mc2.iqget(key1);
		mc2.iqset(key1, "2");
		assert mc1.iqget(key1).equals("2");
		mc2.quarantineAndRegister(tid, key1);
		assert mc1.incr(key1) == 3;
		assert mc1.incr(key1, 5) == 8;
		mc2.deleteAndRelease(tid);
		assert mc1.get(key1) == null;

		// no lease, has value
		mc2.iqget(key1);
		mc2.iqset(key1, "3");
		assert mc1.iqget(key1).equals("3");
		assert mc1.incr(key1) == 4;
		assert mc1.incr(key1, 2) == 6;

		// Qref lease
//		assert mc2.quarantineAndCompare(key1, "6") == false;
//		assert mc1.incr(key1) == 7;
//		assert mc1.incr(key1, 3) == 10;
//		mc2.swapAndRelease(key1, "some_new_val");
//		assert mc1.iqget(key1).equals("10");

		System.out.println("TEST_INCR_OK");
	}

	public static void test_decr(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1 = UUID.randomUUID().toString();
		long val1 = 100;

		mc2.enableBackoff();

		// no lease, no value
		assert mc1.decr(key1, 5) == -1;
		assert mc1.decr(key1) == -1;

		// i-lease, no value
		assert mc2.iqget(key1) == null;
		assert mc1.decr(key1) == -1;
		assert mc1.decr(key1, 5) == -1;
		mc2.iqset(key1, val1);
		assert mc1.iqget(key1).equals(val1);

		// test set negative and zero value
		mc2.delete(key1);
		mc2.iqget(key1);
		mc2.iqset(key1, "7");
		mc1.iqget(key1).equals("7");
		assert mc1.decr(key1) == 6;
		assert mc1.decr(key1, 5) == 1;
		mc2.delete(key1);
		mc2.iqget(key1);
		mc2.iqset(key1, "-7");
		mc1.iqget(key1).equals("-7");
		assert mc1.decr(key1) == -1;
		assert mc1.decr(key1, 5) == -1;    

		// Qinv lease (has or has not value)
		mc2.delete(key1);
		String tid = UUID.randomUUID().toString();
		mc2.quarantineAndRegister(tid, key1);
		assert mc1.decr(key1) == -1;
		assert mc1.decr(key1, 5) == -1;
		mc2.deleteAndRelease(tid);     
		mc2.iqget(key1);
		mc2.iqset(key1, "12");
		assert mc1.iqget(key1).equals("12");
		mc2.quarantineAndRegister(tid, key1);
		assert mc1.decr(key1) == 11;
		assert mc1.decr(key1, 5) == 6;
		mc2.deleteAndRelease(tid);
		assert mc1.get(key1) == null;

		// no lease, has value
		mc2.iqget(key1);
		mc2.iqset(key1, "13");
		assert mc1.iqget(key1).equals("13");
		assert mc1.decr(key1) == 12;
		assert mc1.decr(key1, 2) == 10;

		// Qref lease
//		assert mc2.quarantineAndCompare(key1, "10") == false;
//		assert mc1.decr(key1) == 9;
//		assert mc1.decr(key1, 3) == 6;
//		mc2.swapAndRelease(key1, "some_new_val");
//		assert mc1.iqget(key1).equals("6");

		System.out.println("TEST_DECR_OK");
	}  

	public static void test_delete(MemcachedClient mc1, MemcachedClient mc2) throws Exception {
		String key1= UUID.randomUUID().toString();
		String val1="val1", val2="val2";

		mc2.enableBackoff();

		// no lease, no value
		assert mc1.delete(key1) == false;

		// i-lease, no value
		assert mc2.iqget(key1) == null;
		assert mc1.delete(key1) == true;
		assert mc2.iqget(key1) == null;             // should not back-off here
		assert mc1.delete(key1) == true;

		// Qinv lease, no value
		String tid = UUID.randomUUID().toString();
		assert mc2.quarantineAndRegister(tid, key1) == true;
		assert mc1.delete(key1) == true;
		assert mc2.deleteAndRelease(tid) == true;

		// no lease, with value
		assert mc2.iqget(key1) == null;
		mc2.iqset(key1, val1);
		assert mc1.iqget(key1).equals(val1);
		assert mc1.delete(key1) == true;

		// Qinv lease, with value
		assert mc2.iqget(key1) == null;
		mc2.iqset(key1, val2);
		assert mc1.iqget(key1).equals(val2);
		assert mc2.quarantineAndRegister(key1, val2);
		assert mc2.delete(key1);

		// Qref lease, with value
//		assert mc2.iqget(key1) == null;
//		mc2.iqset(key1, val1);
//		assert mc1.iqget(key1).equals(val1);
//		assert mc2.quarantineAndCompare(key1, val1);
//		assert mc2.delete(key1);
//
//		assert mc2.iqget(key1) == null;
//		assert mc1.delete(key1) == true;

		System.out.println("TEST_DELETE_OK");
	}

	public static void test_append(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1", val1="val1";
		String randomKey = UUID.randomUUID().toString();
		assert mc.append(randomKey, "some_rand_val") == false;

		// no lease, no val
		mc.delete(key1);       
		assert mc.append(key1, "new_val") == false;

		// i-lease (no val)
		assert mc.iqget(key1) == null;
		assert mc.append(key1, "new_val") == false;

		// no lease, has value
		assert mc.iqset(key1, val1);
		assert mc.iqget(key1).equals(val1);
		assert mc.append(key1, "new_val") == true;
		assert mc.iqget(key1).equals(val1 + "new_val");

		// Qinv-lease, no value
		mc.delete(key1);
		String tid = UUID.randomUUID().toString();
		mc2.quarantineAndRegister(tid, key1);
		assert mc2.append(key1, "new_val") == false;
		mc2.deleteAndRelease(tid);
		assert mc2.iqget(key1) == null;

		// Qinv-lease, has value
		mc2.iqset(key1, val1);
		assert mc.iqget(key1).equals(val1);
		mc2.quarantineAndRegister(tid, key1);
		assert mc.append(key1, "new_val") == true;
		assert mc.iqget(key1).equals(val1 + "new_val");
		mc2.deleteAndRelease(tid);
		assert mc.iqget(key1) == null;

		// Qref-lease (has value)
//		mc.iqset(key1, val1);
//		assert mc.iqget(key1).equals(val1);
//		assert mc2.quarantineAndCompare(key1, val1) == true;
//		assert mc.append(key1, "new_val") == true;
//		assert mc.iqget(key1).equals(val1 + "new_val");
//		mc2.swapAndRelease(key1, "new");      
//		assert mc.iqget(key1).equals("new") == false;

		System.out.println("TEST_APPEND_OK");
	}

	public static void test_prepend(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1", val1="val1";
		String randomKey = UUID.randomUUID().toString();
		assert mc.prepend(randomKey, "some_rand_val") == false;

		// no lease, no val
		mc.delete(key1);       
		assert mc.prepend(key1, "new_val") == false;

		// i-lease (no val)
		assert mc.iqget(key1) == null;
		assert mc.prepend(key1, "new_val") == false;

		// no lease, has value
		mc.iqset(key1, val1);
		assert mc.iqget(key1).equals(val1);
		assert mc.prepend(key1, "new_val") == true;
		assert mc.iqget(key1).equals("new_val" + val1);

		// Qinv-lease, no value
		mc.delete(key1);
		String tid = UUID.randomUUID().toString();
		mc2.quarantineAndRegister(tid, key1);
		assert mc2.prepend(key1, "new_val") == false;
		mc2.deleteAndRelease(tid);
		assert mc2.iqget(key1) == null;

		// Qinv-lease, has value
		mc2.iqset(key1, val1);
		assert mc.iqget(key1).equals(val1);
		mc2.quarantineAndRegister(tid, key1);
		assert mc.prepend(key1, "new_val") == true;
		assert mc.iqget(key1).equals("new_val" + val1);
		mc2.deleteAndRelease(tid);
		assert mc.iqget(key1) == null;

		// Qref-lease (has value)
//		mc.iqset(key1, val1);
//		assert mc.iqget(key1).equals(val1);
//		assert mc2.quarantineAndCompare(key1, val1) == true;
//		assert mc.prepend(key1, "new_val") == true;
//		assert mc.iqget(key1).equals("new_val" + val1);
//		mc2.swapAndRelease(key1, "new");      
//		assert mc.iqget(key1).equals("new") == false;

		System.out.println("TEST_PREPEND_OK");
	}

	public static void test_add(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1", val1="val1", randKey=UUID.randomUUID().toString();

		mc.disableBackoff();

		// no lease, no val
		mc.add(randKey, "randVal");
		assert mc.iqget(randKey).equals("randVal");
		mc.delete(key1);
		mc.add(key1, val1);
		assert mc.iqget(key1).equals(val1);

		// i-lease (no val)
		mc.delete(key1);
		assert mc.iqget(key1) == null;
		mc.add(key1, val1);
		assert mc2.iqget(key1).equals(val1);
		assert mc.iqset(key1, "set_val");
		assert mc2.iqget(key1).equals("set_val");

		// no lease, has val
		assert mc.add(key1, "new_val") == false;

		// Qinv, no val
		mc.delete(key1);
		String tid = UUID.randomUUID().toString();
		mc.quarantineAndRegister(tid, key1);
		mc2.add(key1, val1);
		assert mc2.iqget(key1).equals(val1);
		mc.deleteAndRelease(tid);
		assert mc2.iqget(key1) == null;

		// Qinv, has val
		mc2.iqset(key1, val1);
		assert mc2.iqget(key1).equals(val1);
		mc.quarantineAndRegister(tid, key1);
		assert mc2.add(key1, "new_val") == false;
		assert mc2.iqget(key1).equals(val1);
		mc.deleteAndRelease(tid);
		assert mc2.iqget(key1) == null;

		// Qref (has val)
//		assert mc2.iqset(key1, val1) == true;
//		assert mc2.iqget(key1).equals(val1);
//		assert mc.quarantineAndCompare(key1, val1);
//		assert mc2.add(key1, "new_val") == false;
//		assert mc2.iqget(key1).equals(val1);
//		mc.swapAndRelease(key1, "new");
//		assert mc2.iqget(key1).equals("new");      

		System.out.println("TEST_ADD_OK");
	}

	public static void test_replace(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1", val1="val1", randKey=UUID.randomUUID().toString();

		mc2.disableBackoff();

		// no val, no lease
		assert !mc.replace(randKey, "randVal");
		assert mc.iqget(randKey) == null;

		assert mc.delete(key1);
		assert !mc.replace(key1, val1);
		assert mc.iqget(key1) == null;
		assert mc.iqset(key1, val1);
		assert mc2.iqget(key1).equals(val1);

		// i-lease (no val)
		mc.delete(key1);
		assert mc.iqget(key1) == null;
		mc.replace(key1, val1);
		assert mc2.iqget(key1) == null;
		assert mc.iqset(key1, "set_val");
		assert mc2.iqget(key1).equals("set_val");

		// no lease, has val
		assert mc.replace(key1, "new_val");
		assert mc.iqget(key1).equals("new_val");

		// Qinv, no val
		mc.delete(key1);
		String tid = UUID.randomUUID().toString();
		mc.quarantineAndRegister(tid, key1);
		mc2.replace(key1, val1);
		assert mc2.iqget(key1) == null;
		mc.deleteAndRelease(tid);
		assert mc2.iqget(key1) == null;

		// Qinv, has val
		mc2.iqset(key1, val1);
		assert mc2.iqget(key1).equals(val1);
		mc.quarantineAndRegister(tid, key1);
		assert mc2.replace(key1, "new_val");
		assert mc2.iqget(key1).equals("new_val");
		mc.deleteAndRelease(tid);
		assert mc2.iqget(key1) == null;

		// Qref (has val)
//		assert mc2.iqset(key1, val1) == true;
//		assert mc2.iqget(key1).equals(val1);
//		assert mc.quarantineAndCompare(key1, val1);
//		assert mc2.replace(key1, "new_val");
//		assert mc2.iqget(key1).equals("new_val");
//		mc.swapAndRelease(key1, "new");
//		assert mc2.iqget(key1).equals("new_val");      

		System.out.println("TEST_REPLACE_OK");
	}

	public static void test_get(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String randKey=UUID.randomUUID().toString(), key1="key1", val1="val1";

		mc2.disableBackoff();

		// no lease, no value
		assert mc.get(randKey) == null;
		mc.delete(key1);
		assert mc.get(key1) == null;

		// i-lease (no val)
		assert mc.iqget(key1) == null;      // get an i-lease
		assert mc2.get(key1) == null;       // should return null
		assert mc.iqset(key1, val1) == true;    // set should be success
		assert mc.iqget(key1).equals(val1);

		// no lease, has val
		assert mc.get(key1).equals(val1);

		// Qinv, no val
		assert mc.delete(key1);
		String tid = UUID.randomUUID().toString();
		assert mc.quarantineAndRegister(tid, key1);
		assert mc2.get(key1) == null;
		assert mc.deleteAndRelease(tid);
		assert mc2.get(key1) == null;

		// Qinv, has val
		assert mc.iqget(key1) == null;
		assert mc.iqset(key1, val1);
		assert mc.quarantineAndRegister(tid, key1);
		assert mc2.get(key1).equals(val1);
		assert mc.deleteAndRelease(tid);        // this function should works
		assert mc2.get(key1) == null;

		// Qref (has val)
//		assert mc.iqget(key1) == null;
//		assert mc.iqset(key1, val1);
//		assert mc.iqget(key1).equals(val1);
//		assert mc.quarantineAndCompare(key1, val1) == true;
//		assert mc2.get(key1).equals(val1);
//		mc.swapAndRelease(key1, "new_val");
//		assert mc2.get(key1).equals("new_val");
		
		mc.delete(key1);
		mc.iqget(key1);
		assert mc.get(key1) == null;
		mc.iqset(key1, "val1");
		assert mc.iqget(key1).equals("val1");

		System.out.println("TEST_GET_OK");
	}

	public static void test_set(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String randKey = UUID.randomUUID().toString();
		String key1="key1", val1="val1";

		mc2.disableBackoff();

		// no lease, no val
		assert mc.set(randKey, "randVal");
		assert mc2.get(randKey).equals("randVal");
		mc.delete(key1);
		assert mc.set(key1, val1);
		assert mc2.get(key1).equals(val1);

		// i-lease (no val)
		mc.delete(key1);
		assert mc.iqget(key1) == null;
		mc2.set(key1, val1);
		assert mc2.get(key1).equals(val1);
		assert mc.iqset(key1, "new_val");
		assert mc2.get(key1).equals("new_val");

		// no lease, has val
		mc.set(key1, "new_val_1");
		assert mc2.get(key1).equals("new_val_1");

		// Qinv lease, no val
		mc.delete(key1);
		assert mc2.get(key1) == null;
		String tid = UUID.randomUUID().toString();
		assert mc.quarantineAndRegister(tid, key1);
		assert mc2.set(key1, val1);
		assert mc2.get(key1).equals(val1);
		assert mc.deleteAndRelease(tid);
		assert mc2.get(key1) == null;

		// Qinv lease, has val
		assert mc.set(key1, val1);
		assert mc.get(key1).equals(val1);
		assert mc.quarantineAndRegister(tid, key1);
		assert mc2.set(key1, "new_val");
		assert mc2.get(key1).equals("new_val");
		assert mc.deleteAndRelease(tid);
		assert mc.get(key1) == null;

		// Qref (has val)
//		assert mc.iqget(key1) == null;
//		assert mc.iqset(key1, val1);
//		assert mc.quarantineAndCompare(key1, val1);
//		assert mc2.set(key1, "new_val");
//		assert mc2.get(key1).equals("new_val");
//		mc.swapAndRelease(key1, "new_val_1");
//		assert mc2.get(key1).equals("new_val_1");

		System.out.println("TEST_SET_OK");
	}	
	
	public static void test_unlease(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1", key2="key2";
		
		mc.delete(key1);
		mc.delete(key2);
		
		mc.iqget(key1);
		mc.iqget(key2);
		mc.iqset(key2, "val2");
		
		mc2.quarantineAndRead(key2).equals("val2");
		
		// this should fail and release all the lease
		try {
			mc.quarantineAndRead(key2);
			assert false;
		} catch (IQException e) {
			
		}
		
		try {
			mc.iqset(key1, "val1");		// this should fail because the lease was released
			assert false;
		} catch (IQException e) {}
		
		assert mc.get(key1) == null;
		
		mc2.iqget(key1);
		mc2.iqset(key1, "val1");
		assert mc2.iqget(key1).equals("val1");
		
		mc2.swapAndRelease(key2, "new_val");
		mc2.iqget(key2).equals("new_val");
		
		System.out.println("TEST_UNLEASE_OK");
	}
	
	public static void test_QaRead(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1", key2="key2";
		
		// clean cache server
		mc.delete(key1);
		mc.delete(key2);
		
		// no lease, no value
		assert mc.quarantineAndRead(key1) == null;
		mc.swapAndRelease(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		
		// no lease, has value
		assert mc.quarantineAndRead(key1).equals("val1");
		mc.swapAndRelease(key1, "new_val");
		assert mc.iqget(key1).equals("new_val");
		
		// has i-lease
		mc.iqget(key2);
		assert mc.quarantineAndRead(key2) == null;
		assert mc.iqset(key2, "temp_val") == false;
		mc.swapAndRelease(key2, "val2");
		mc.iqget(key2).equals("val2");
		
		// has q-lease, no val
		mc.delete(key1);
		assert mc.quarantineAndRead(key1) == null;
		try {
			mc2.quarantineAndRead(key1);
			assert false;
		} catch (IQException e) {
			
		}
		mc2.swapAndRelease(key1, "temp");
		assert mc2.get(key1) == null;
		mc.swapAndRelease(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		
		// has q-lease, has val
		assert mc.quarantineAndRead(key1).equals("val1");
		try {
			mc2.quarantineAndRead(key1);
			assert false;
		} catch (IQException e) {
			
		}
		mc2.swapAndRelease(key1, "temp");
		assert mc2.get(key1).equals("val1");
		mc.swapAndRelease(key1, "newval1");
		assert mc.iqget(key1).equals("newval1");	
		
		// clean cache server at the end
		mc.delete(key1);
		mc.delete(key2);
		
		System.out.println("test_QaRead OK");
	}
	
	public static void test_SaR_HasVal(MemcachedClient mc, MemcachedClient mc2) throws Exception {		
		String key1="key1", key2="key2";
		
		mc.delete(key1);
		mc.delete(key2);
		
		// no value, no lease
		mc.quarantineAndRead(key1);
		Thread.sleep(EXP_LEASE_TIME + 1000);					// wait 3 seconds for the q lease to expire		
		mc.swapAndRelease(key1, "val1");
		assert mc.get(key1) == null;
		System.out.println("passed");
		
		// no value, has i lease
		mc2.quarantineAndRead(key1);
		Thread.sleep(EXP_LEASE_TIME + 1000);					// wait 3 seconds for the q lease to expire	
		assert mc.iqget(key1) == null;		
		mc2.swapAndRelease(key1, "val1");
		assert mc2.get(key1) == null;
		assert mc.iqset(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		System.out.println("passed");
		
		// has value, no lease
		mc.quarantineAndRead(key1);
		Thread.sleep(EXP_LEASE_TIME + 1000);
		mc.swapAndRelease(key1, "new_val");
		assert mc.get(key1) == null;
		System.out.println("passed");
		
		// q lease, no val
		mc.quarantineAndRead(key2);
		Thread.sleep(EXP_LEASE_TIME + 1000);
		mc2.quarantineAndRead(key2);
		mc.swapAndRelease(key2, "val2");
		assert mc.get(key2) == null;
		mc2.swapAndRelease(key2, "val2");
		assert mc.get(key2).equals("val2");
		System.out.println("passed");
		
		// q lease, has val
		mc.quarantineAndRead(key2);
		Thread.sleep(EXP_LEASE_TIME + 1000);
		mc2.iqget(key2);
		assert mc2.iqset(key2, "val2");
		mc2.quarantineAndRead(key2);
		mc.swapAndRelease(key2, "newval2");
		assert mc.get(key2).equals("val2");
		mc2.swapAndRelease(key2, "newval2");
		assert mc.get(key2).equals("newval2");
		System.out.println("passed");	
		
		System.out.println("test_SaR_HasVal OK");
	}
	
	public static void test_SaR_NoVal(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1", key2="key2";
		
		// clean the cache
		mc.delete(key1);
		mc.delete(key2);
		
		// no lease, no value
		mc.quarantineAndRead(key1);
		Thread.sleep(3000);
		mc.swapAndRelease(key1, null);
		assert mc.get(key1) == null;
		System.out.println("passed");
		
		// i lease
		mc.quarantineAndRead(key1);
		Thread.sleep(3000);
		mc.iqget(key1);
		mc.swapAndRelease(key1, null);
		assert mc.get(key1) == null;
		mc.iqset(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		System.out.println("passed");
		
		// no lease, has value
		mc.quarantineAndRead(key1);
		Thread.sleep(3000);
		mc.swapAndRelease(key1, null);
		assert mc.get(key1) == null;
		System.out.println("passed");
		
		// q lease, no val
		mc.quarantineAndRead(key1);
		Thread.sleep(3000);
		mc2.quarantineAndRead(key1);
		mc.swapAndRelease(key1, null);
		assert mc.get(key1) == null;
		mc2.swapAndRelease(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		System.out.println("passed");
		
		// q lease, has val
		mc.quarantineAndRead(key1);
		Thread.sleep(3000);
		mc2.iqget(key1);
		assert mc2.iqset(key1, "val1");
		assert mc2.iqget(key1).equals("val1");
		mc2.quarantineAndRead(key1);
		mc.swapAndRelease(key1, "temp");
		assert mc.iqget(key1).equals("val1");
		mc2.swapAndRelease(key1, null);
		assert mc2.get(key1) == null;
		System.out.println("passed");
		
		System.out.println("test_SaR_NoVal OK");
	}
	
	/**
	 * Normally, operation should behave like what describes in the state table.
	 * However, when operations come from the same thread (same whalin client instance),
	 * some operations should behave more flexible.
	 * 1. If a QaRead(k) comes and observes that there already has a q lease on
	 * k granted to the same client, instead trying backing-off, it will allow
	 * the program to proceed (return null but not throw IQ exception)
	 * 
	 * 2. If an IQGet(k) comes and observes that there already has a i lease or
	 * q lease on key k granted to the same client, instead trying to back-off,
	 * it will allow the program to proceed (return value if there is value on that
	 * item from the cache)
	 * 
	 */
	public static void test_getlease_same_thread(MemcachedClient mc) throws Exception {
		String key1="key1";
		
		// enable back off on whalin client
		mc.enableBackoff();
		
		// iqget on i-lease item
		mc.delete(key1);
		mc.iqget(key1);
		mc.iqget(key1);	// should not try back-off here
		mc.iqget(key1);
		mc.iqset(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		
		// qaread on q-lease item
		assert mc.quarantineAndRead(key1).equals("val1");
		assert mc.quarantineAndRead(key1).equals("val1");		// should return null but not throw exception\
		mc.swapAndRelease(key1, "newval1");
		assert mc.iqget(key1).equals("newval1");
		
		// iqget on q-lease item
		mc.quarantineAndRead(key1);
		assert mc.iqget(key1).equals("newval1");
		try {
			mc.iqset(key1, "temp");
		} catch (IQException e) {}
		assert mc.iqget(key1).equals("newval1");
		mc.swapAndRelease(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		
		// qaread on i-lease item (handle normally)
		mc.delete(key1);
		mc.iqget(key1);
		assert mc.quarantineAndRead(key1) == null;		// q-lease overrides i-lease
		assert mc.iqset(key1, "temp") == false;			// MUST fail!
		assert mc.iqget(key1) == null;					// no back-off, no value return
		mc.swapAndRelease(key1, "val1");				// successfully swap new item
		assert mc.iqget(key1).equals("val1");
		
		System.out.println("test_getlease_same_thread OK");
	}
	
	public static void test_getlease_same_thread_expired(MemcachedClient mc) throws Exception {
		String key1="key1";
		
		mc.enableBackoff();
		
		mc.delete(key1);
		
		// iqget follows iqget expired
		assert mc.iqget(key1) == null;
		Thread.sleep(EXP_LEASE_TIME + 1000);
		assert mc.iqget(key1) == null;
		assert mc.iqset(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		
		// qaread follows iqget expired
		assert mc.delete(key1);
		assert mc.iqget(key1) == null;
		Thread.sleep(EXP_LEASE_TIME + 1000);
		assert mc.quarantineAndRead(key1) == null;
		mc.swapAndRelease(key1, "newval1");
		assert mc.iqget(key1).equals("newval1");
		
		// iqget follows qaread expired
		assert mc.delete(key1);
		assert mc.quarantineAndRead(key1) == null;
		Thread.sleep(EXP_LEASE_TIME + 1000);
		assert mc.iqget(key1) == null;			// should not back-off here
		assert mc.iqset(key1, "val1");
		assert mc.iqget(key1).equals("val1");
		
		// qaread follows qaread expired
		assert mc.quarantineAndRead(key1).equals("val1");
		Thread.sleep(EXP_LEASE_TIME + 1000); 
		assert mc.quarantineAndRead(key1) == null;
		mc.swapAndRelease(key1, "newval1");
		mc.iqget(key1).equals("newval1");
		mc.swapAndRelease(key1, "newval2");
		mc.iqget(key1).equals("newval1");
		
		System.out.println("test_getlease_same_thread_expired OK");
	}
	
	public static void test_getlease_different_thread(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key="key1";
		
		mc.enableBackoff();
		mc2.enableBackoff();
		
		// client2 tries qaread on client1's q-item
		mc.delete(key);		
		mc.quarantineAndRead(key);
		try {
			mc2.quarantineAndRead(key);
			assert false;
		} catch (IQException e) {
			
		}
		mc.swapAndRelease(key, "val1");
		mc.iqget(key).equals("val1");
		mc.quarantineAndRead(key);
		try {
			mc2.quarantineAndRead(key);
			assert false;
		} catch (IQException e) {
			
		}
		mc.swapAndRelease(key, "val2");
		mc.iqget(key).equals("val2");	
		System.out.println("passed");
		
		// client2 tries qaread on client1's i-item
		mc.delete(key);
		mc.iqget(key);
		mc2.quarantineAndRead(key);
		assert mc.iqset(key, "val") == false;
		mc2.swapAndRelease(key, "val1");
		mc.iqget(key).equals("val1");
		System.out.println("passed");
		
		// client2 tries iqget on client1's q-item
		// should see long waiting time here because iqget back-off
		System.out.println("should observe long waiting time here because iqget back-off");
		mc.delete(key);
		mc.quarantineAndRead(key);
		mc2.iqget(key);
		mc2.iqset(key, "newval1");
		mc.get(key).equals("newval1");
		System.out.println("passed");
		
		// client2 tries iqget on client1's i-item
		// should observe long waiting time here because iqget back-off
		System.out.println("should observe long waiting time here because iqget back-off");
		mc.delete(key);		
		mc.iqget(key);
		mc2.iqget(key);
		mc2.iqset(key, "val1");
		mc.iqset(key, "temp");
		assert mc.get(key).equals("val1");
		mc2.iqget(key).equals("val1");
		System.out.println("passed");
		
		System.out.println("test_getlease_different_thread OK");
	}
	

	/** =============================== **/

	public static void test_delete_leases(MemcachedClient mc, MemcachedClient mc2) throws Exception {
		String key1="key1";
		
		mc.iqget(key1);
		
		mc2.iqget("key2");
		
		mc.iqget("key3");
		mc.iqget("key4");
		
		
	}
	
	/**
	 * This runs through some simple tests of the MemcacheClient.
	 *
	 * Command line args:
	 * args[0] = number of threads to spawn
	 * args[1] = number of runs per thread
	 * args[2] = size of object to store
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel( Level.WARN );

		if ( !UnitTests.class.desiredAssertionStatus() ) {
			System.err.println( "WARNING: assertions are disabled!" );
			try { Thread.sleep( 3000 ); } catch ( InterruptedException e ) {}
		}

		String[] serverlist = {
				//				"10.0.0.90:10001"
				"10.0.0.220:11211"
				//			"192.168.1.50:1620",
				//			"192.168.1.50:1621",
				//			"192.168.1.50:1622",
				//			"192.168.1.50:1623",
				//			"192.168.1.50:1624",
				//			"192.168.1.50:1625",
				//			"192.168.1.50:1626",
				//			"192.168.1.50:1627",
				//			"192.168.1.50:1628",
				//			"192.168.1.50:1629"
		};

		//Integer[] weights = { 1, 1, 1, 1, 10, 5, 1, 1, 1, 3 };
		Integer[] weights = {1}; 

		if ( args.length > 0 )
			serverlist = args;

		// initialize the pool for memcache servers
		SockIOPool pool = SockIOPool.getInstance( "test" );
		pool.setServers( serverlist );
		pool.setWeights( weights );
		pool.setMaxConn( 250 );
		pool.setNagle( false );
		pool.setHashingAlg( SockIOPool.CONSISTENT_HASH );
		pool.initialize();

		mc = new MemcachedClient( "test" );
		MemcachedClient mc2 = new MemcachedClient("test");

		try {
			test11();
			runAlTests( mc, mc2 );
			test99(mc);
			test100(mc);
			
			testDeleteRace(mc, mc2);
			testReadLease(mc, mc2);

			testRead(mc);
			testRMW(mc);
			testReadOnWriteLease(mc, mc2);
			testWriteOnWriteLease(mc, mc2);
			testWriteAfterWriteLease(mc, mc2);
			testReleaseTokenWithoutXLease(mc, mc2);
			testFailedXLease(mc, mc2);
			testGetNoLease(mc, mc2);
			testReleaseX(mc, mc2);
			sxqQaReg(mc, mc2);
			sxqQaReg2(mc, mc2);
			sxqQaReg3(mc, mc2);
			sxq4(mc, mc2);
			sxq6(mc, mc2);
			sxq7(mc, mc2);
			sxq8(mc, mc2);
			sxq9(mc);

			test_incr(mc, mc2);
			test_decr(mc, mc2);
			test_delete(mc, mc2);
			test_append(mc, mc2);
			test_prepend(mc, mc2);
			test_add(mc, mc2);
			test_replace(mc, mc2);
			test_get(mc, mc2);
			test_set(mc, mc2);		
			test_unlease(mc, mc2);
			
			testQARead_RMW(mc);
			testQARead_RMWemptyCacheIlease(mc);
			testQARead_RMWemptyCache(mc);
			testQARead_RMWfailedQuarantine(mc, mc2);
			
			test_QaRead(mc, mc2);
			test_getlease_same_thread(mc);
			
			test_getlease_different_thread(mc, mc2);			
			
			System.out.println("Following tests requires expired_lease_time=" + EXP_LEASE_TIME + " ms to execute");
			test_SaR_HasVal(mc, mc2);
			test_SaR_NoVal(mc, mc2);
			test_getlease_same_thread_expired(mc);
			sxq5(mc, mc2);
			testRMWLeaseTimedOut(mc);
			
			test_delete_leases(mc,  mc2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** 
	 * Class for testing serializing of objects. 
	 * 
	 * @author $Author: $
	 * @version $Revision: $ $Date: $
	 */
	public static final class TestClass implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String field1;
		private String field2;
		private Integer field3;

		public TestClass( String field1, String field2, Integer field3 ) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
		}

		public String getField1() { return this.field1; }
		public String getField2() { return this.field2; }
		public Integer getField3() { return this.field3; }

		public boolean equals( Object o ) {
			if ( this == o ) return true;
			if ( !( o instanceof TestClass ) ) return false;

			TestClass obj = (TestClass)o;

			return ( ( this.field1 == obj.getField1() || ( this.field1 != null && this.field1.equals( obj.getField1() ) ) )
					&& ( this.field2 == obj.getField2() || ( this.field2 != null && this.field2.equals( obj.getField2() ) ) )
					&& ( this.field3 == obj.getField3() || ( this.field3 != null && this.field3.equals( obj.getField3() ) ) ) );
		}
	}
}
