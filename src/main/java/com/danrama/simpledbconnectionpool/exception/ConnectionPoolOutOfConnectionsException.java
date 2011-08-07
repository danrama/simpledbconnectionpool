package com.danrama.simpledbconnectionpool.exception;

/**
 * An exception that signals that the pool has no available connections at the moment.
 * 
 * @author Daniel Bloomfield Ramagem
 */
public class ConnectionPoolOutOfConnectionsException extends ConnectionPoolException {
	private static final long serialVersionUID = 1L;

	public ConnectionPoolOutOfConnectionsException() {
		super();
	}
		
	public ConnectionPoolOutOfConnectionsException(String message) {		
		super(message);
	}

	public ConnectionPoolOutOfConnectionsException(Exception e) {
		super(e);
	}
	
	public ConnectionPoolOutOfConnectionsException(String message, Exception e) {
		super(message, e);
	}
}
