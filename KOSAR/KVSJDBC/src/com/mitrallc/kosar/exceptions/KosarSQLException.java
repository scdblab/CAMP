package com.mitrallc.kosar.exceptions;

import java.sql.SQLException;

public class KosarSQLException extends SQLException {
	private static final long serialVersionUID = 4971130881229096534L;

	public KosarSQLException(String message) {
		super(message);
	}

}
