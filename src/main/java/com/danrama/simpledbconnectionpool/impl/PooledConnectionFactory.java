package com.danrama.simpledbconnectionpool.impl;

import java.lang.reflect.Proxy;
import java.sql.Connection;

import net.jcip.annotations.ThreadSafe;

import com.danrama.simpledbconnectionpool.exception.ConnectionPoolAlreadyReleasedConnectionException;

/**
 * A factory for creating dynamic proxies of type <code>PooledConnection</code> that wrap a raw instance of a database
 * connection.
 * 
 * @author Daniel Bloomfield Ramagem
 * @see PooledConnection
 * @see PooledConnectionProxy
 * @see ConnectionPoolAlreadyReleasedConnectionException
 */
@ThreadSafe
public class PooledConnectionFactory {
	/**
	 * Factory method for creating new instances of pooled connections.
	 * 
	 * @param pool the pool the connection belongs to
	 * @param connection the raw database connection being wrapped
	 * @return the pooled connection dynamic proxy wrapper
	 */
	public static PooledConnection createPooledConnection(AbstractConnectionPool pool, Connection connection) {
		return (PooledConnection) Proxy.newProxyInstance(PooledConnection.class.getClassLoader(), 
				new Class[] { PooledConnection.class }, 
				new PooledConnectionProxy(pool, connection));
	}
}
