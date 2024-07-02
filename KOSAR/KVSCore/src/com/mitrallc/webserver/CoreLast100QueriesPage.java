package com.mitrallc.webserver;

import com.mitrallc.core.KosarCore;

public class CoreLast100QueriesPage extends BaseLast100Queries {
	public void getLast100QueryList() {
		queryList = KosarCore.last100readQueries.getQueryList();
	}
}
