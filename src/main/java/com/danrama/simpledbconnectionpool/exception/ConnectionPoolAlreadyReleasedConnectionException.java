package com.danrama.simpledbconnectionpool.exception;

/**
 * An exception that signals that the connection has already been released back into the pool and cannot be further used.
 * 
 * @author Daniel Bloomfield Ramagem
 */
public class ConnectionPoolAlreadyReleasedConnectionException extends ConnectionPoolException {
	private static final long serialVersionUID = 1L;

	public ConnectionPoolAlreadyReleasedConnectionException() {
		super();
	}

	public ConnectionPoolAlreadyReleasedConnectionException(String message) {
		super(message);
	}
}
