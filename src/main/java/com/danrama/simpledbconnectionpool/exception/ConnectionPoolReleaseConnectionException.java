package com.danrama.simpledbconnectionpool.exception;

/**
 * An exception indicating an error during the release of a connection back into the pool.
 * 
 * @author Daniel Bloomfield Ramagem
 */
public class ConnectionPoolReleaseConnectionException extends ConnectionPoolException {
	private static final long serialVersionUID = 1L;

	public ConnectionPoolReleaseConnectionException() {
		super();
	}
		
	public ConnectionPoolReleaseConnectionException(String message) {		
		super(message);
	}

	public ConnectionPoolReleaseConnectionException(Exception e) {
		super(e);
	}
	
	public ConnectionPoolReleaseConnectionException(String message, Exception e) {
		super(message, e);
	}
}
