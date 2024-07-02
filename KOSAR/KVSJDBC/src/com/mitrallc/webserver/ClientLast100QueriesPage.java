package com.mitrallc.webserver;

import com.mitrallc.sql.KosarSoloDriver;

public class ClientLast100QueriesPage extends BaseLast100Queries {
	
	public void getLast100QueryList() {
		queryList = KosarSoloDriver.last100readQueries.getQueryList();
	}
}
