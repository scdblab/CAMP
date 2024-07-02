package com.mitrallc.common;

import com.mitrallc.util.SocketIO;

public class TokenObject {
	byte[] token;
	SocketIO socket;
	boolean cache;
	
	public TokenObject(SocketIO socket, byte[] token, boolean cache) {
		this.token = token;
		this.socket = socket;
		this.cache = cache;
	}

	public byte[] getRequestArray() {
		return token;
	}

	public void setToken(byte[] token) {
		this.token = token;
	}

	public SocketIO getSocket() {
		return socket;
	}

	public void setSocket(SocketIO socket) {
		this.socket = socket;
	}
	public boolean isCache() {
		return cache;
	}
	
}
