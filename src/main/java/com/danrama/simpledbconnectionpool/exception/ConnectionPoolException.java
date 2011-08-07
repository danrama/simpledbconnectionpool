package com.danrama.simpledbconnectionpool.exception;

import java.sql.SQLException;

/**
 * The root exception of all connection pool related errors.
 * 
 * @author Daniel Bloomfield Ramagem
 */
public class ConnectionPoolException extends SQLException {
	private static final long serialVersionUID = 1L;

	public ConnectionPoolException() {
		super();
	}
		
	public ConnectionPoolException(String message) {		
		super(message);
	}

	public ConnectionPoolException(Exception e) {
		super(e);
	}
	
	public ConnectionPoolException(String message, Exception e) {
		super(message, e);
	}
}
