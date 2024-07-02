package com.mitrallc.communication;

/**
 * This class contains the booleans that specify the modes Kosar can work in.
 * The modes are: Single Node Mode/MultiNode mode and failure handling modes
 * where cache can be read from but not written and cache can neither be read
 * from nor be written to.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
public class CacheModeController {
	private static volatile boolean cache_read_allowed = false;
	private static volatile boolean cache_update_allowed = false;
	private static volatile boolean multi_node_config = false;

	public static void setMultiNodeConfig() {
		multi_node_config = true;
	}

	public static void setSingleNodeConfig() {
		multi_node_config = false;
	}

	public static boolean isMultiNodeConfigOn() {
		return multi_node_config;
	}

	public static boolean isSingleNodeConfigOn() {
		return !multi_node_config;
	}

	public static void disableQueryCaching() {
		cache_read_allowed = false;
		cache_update_allowed = false;
	}

	public static void disableCacheRead() {
		cache_read_allowed = false;
	}

	public static void disableCacheUpdate() {
		cache_update_allowed = false;
	}

	public static void enableQueryCaching() {
		cache_read_allowed = true;
		cache_update_allowed = true;
	}

	public static void enableCacheRead() {
		cache_read_allowed = true;
	}

	public static void enableCacheUpdate() {
		cache_update_allowed = true;
	}

	public static boolean isCacheUpdateAllowed() {
		return cache_update_allowed;
	}

	public static boolean isCacheReadAllowed() {
		return cache_read_allowed;
	}
}
