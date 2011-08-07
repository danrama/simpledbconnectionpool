package com.danrama.simpledbconnectionpool.exception;

/**
 * An exception that indicates that an error occurred trying to obtain a new connection from the database to put in the pool.
 * 
 * @author Daniel Bloomfield Ramagem
 */
public class ConnectionPoolNewConnectionException extends ConnectionPoolException {
	private static final long serialVersionUID = 1L;

	public ConnectionPoolNewConnectionException() {
		super();
	}
		
	public ConnectionPoolNewConnectionException(String message) {		
		super(message);
	}

	public ConnectionPoolNewConnectionException(Exception e) {
		super(e);
	}
	
	public ConnectionPoolNewConnectionException(String message, Exception e) {
		super(message, e);
	}
}
