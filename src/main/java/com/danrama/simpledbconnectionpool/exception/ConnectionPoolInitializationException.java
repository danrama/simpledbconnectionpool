package com.danrama.simpledbconnectionpool.exception;

/**
 * An exception signaling an error during the creation of the connection pool. 
 * 
 * @author Daniel Bloomfield Ramagem
 */
public class ConnectionPoolInitializationException extends ConnectionPoolException {
	private static final long serialVersionUID = 1L;

	public ConnectionPoolInitializationException() {
		super();
	}
		
	public ConnectionPoolInitializationException(String message) {		
		super(message);
	}

	public ConnectionPoolInitializationException(Exception e) {
		super(e);
	}
	
	public ConnectionPoolInitializationException(String message, Exception e) {
		super(message, e);
	}
}
