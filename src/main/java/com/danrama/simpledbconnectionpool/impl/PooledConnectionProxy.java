package com.danrama.simpledbconnectionpool.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import com.danrama.simpledbconnectionpool.exception.ConnectionPoolAlreadyReleasedConnectionException;

/**
 * The invocation handler for the dynamic proxy created via the <code>PooledConnectionFactory</code>.  This class essentially
 * wraps a raw database connection and manages its relationship with a connection pool, tracking whether it is released
 * back into the pool.
 * <p>
 * Once a pooled connection is released it can no longer be used by the client for any database operations.  Any attempt to
 * use it throws a <code>ConnectionPoolAlreadyReleasedConnectionException</code>
 * exception.
 *  
 * @author Daniel Bloomfield Ramagem
 * @see PooledConnection
 * @see PooledConnectionProxyFactory
 * @see ConnectionPoolAlreadyReleasedConnectionException
 */
@ThreadSafe
public class PooledConnectionProxy implements InvocationHandler {
	// the associated originating pool
	private AbstractConnectionPool connectionPool;
	
	// the underlying raw database connection
	private Connection wrappedConnection;
	
	// flags that the connection has already been released back to pool and 
	// cannot be reused by the client
	@GuardedBy("this") private boolean hasBeenReleasedBackToPool;

	/**
	 * Construct a new instance.
	 * 
	 * @param pool the originating connection pool
	 * @param connection the raw database connection to be wrapped
	 */
	public PooledConnectionProxy(AbstractConnectionPool pool, Connection connection) {
		this.connectionPool = pool;
		wrappedConnection = connection;
	}
	
	/*
	 * The main Java dynamic proxy method that handles all invocations for the proxy object.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// handle PooledConnection.getPool()
		if (method.getName().equals("getPool")) {
			return connectionPool;			
		}
		
		// synchronize access to the hasBeenReleasedBackToPool flag in order to make it thread safe
		synchronized (this) {
			// handle PooledConnection.setReleasedBackToPool()
			if (method.getName().equals("setReleasedBackToPool")) {
				// flag that this connection is now released
				hasBeenReleasedBackToPool = true;

				// free the reference to wrapped raw database connection so that it may get garbage-collected
				wrappedConnection = null;

				// nothing to return
				return null;
			}

			// handle PooledConnection.isReleasedBackToPool()
			if (method.getName().equals("isReleasedBackToPool")) {
				return hasBeenReleasedBackToPool;
			}

			// if this connection has already been released back to the pool then the client can no longer use it
			if (hasBeenReleasedBackToPool) {
				throw new ConnectionPoolAlreadyReleasedConnectionException();
			}
		}
		
		// connection is not released, so it is still usable by client
		try {
			// delegate the client call to the underlying wrapped connection
			return method.invoke(wrappedConnection, args);
		} catch (InvocationTargetException e) {
			// if an error occurs, re-throw the actual cause of the exception
			throw e.getCause();
		}
	}
}
