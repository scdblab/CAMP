package com.mitrallc.common;


public class Flags {
	private volatile boolean coordinatorExists=false;
	private volatile boolean coordinatorConnected=false;

	public boolean coordinatorExists() {
		return coordinatorExists;
	}

	public void setCoordinatorExists(boolean coordinatorExists) {
		this.coordinatorExists = coordinatorExists;
	}

	public boolean isCoordinatorConnected() {
		return coordinatorConnected;
	}

	public void setCoordinatorConnected(boolean coordinatorConnected) {
		this.coordinatorConnected = coordinatorConnected;
	}

}
