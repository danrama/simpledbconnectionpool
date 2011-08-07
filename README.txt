Simple Database Connection Pool (SDCP)
======================================

Contents
--------
 * Quick Start
 * Introduction
 * Usage
 * Overview of Solution
 * Implementation Details
 * Project Organization
 * Logging
 * Tests
 * Requirements & Dependencies

Quick Start
-----------
After unzipping the project bundle, open a command line and change into the root directory of the project.  Issue the
following Maven commands (make sure Maven is already installed--see http://maven.apache.org):

  mvn compile      # compiles main source code
  
  mvn test-compile # compile test source code
  
  mvn test         # run tests

Introduction
------------
The Simple Database Connection Pool (SDBCP) project provides basic infrastructure for creating a database connection pool
library.  The project is intended to be very open-ended but the requirements/goals below were arbitrarily established (in no
particular order) and served as guidance as the implementation proceeded.

 * Provide an implementation of a connection pool that would work between two thresholds: a minimum amount of connections to
   cache at any time and a maximum amount of outstanding connections to allow.
 * Client is responsible for providing all database connection resources: the driver class as well as appropriate database URL
   strings and properties.
 * Establish a light framework for potentially building additional types of connection pools.
 * Prevent/warn a client from using a pooled connection reference once it had been returned to the pool.
 * Handle concurrent client access to the connection pool.
 * Use custom exceptions for pool-related errors that should be visible to the client.
 * Verify connections that are returned to the pool, discarding ones that are closed/invalid and replacing them with new
 connections.
 * Create a test suite to validate and verify basic operation.  Tests should not need a real database.
   
The next sections in this doc provide high-level and low-level details of the framework established, as well as information
about usage, dependencies, and more.

Usage
-----
A ConnectionPoolFactory class is provided as a convenience for clients to create new connection pools.  The factory currently
only offers a single type of connection pool (the "expanding pool").

The client needs to first configure the factory with an appropriate database driver classname (this is optional,
and the client may use other ways of loading JDBC drivers into the system prior to using the factory class), URL,
and properties.  It can then create and use connection pools.

Example usage:

  // setup the database connection parameters
  String driverClassname = "org.postgresql.Driver"; 
  String url = "jdbc:postgresql://localhost/test";
  Properties props = new Properties();
  props.setProperty("user", "fred");
  props.setProperty("password", "secret");
  
  // configure the ConnectionPoolFactory
  ConnectionPoolFactory poolFactory = new ConnectionPoolFactory(driverClassname, url, props);
  
  // create a new expanding pool with minimum size 5, maximum 10
  ConnectionPool myConnectionPool = poolFactory.createConnectionPool(5, 10);
  
  // use the pool
  try { 
    // get connection from pool
    Connection conn = myConnectionPool.getConnection();
  } catch (ConnectionPoolException e) {
    ...
  }
  ...
  try {
    // release connection back into the pool
    myConnectionPool.releaseConnection(conn);
  } catch (ConnectionPoolException e) {
    ...
  }

Overview of Framework
---------------------
This project implements a simple database connection pool that expands and contracts between a maximum and minimum number of
database connections, as configured by the client.

On creation the pool will fill itself with a minimum set of open connections.  As the client retrieves connections from the
pool, the latter preemptively creates additional connections to maintain its available cache size.  As the established upper
limit of open connections is reached, the pool issues its remaining cached connections until it becomes empty.  At this point
the client cannot obtain more connections from the pool.

As previously obtained connections are released back into the pool they are recycled and made available again for retrieval.
The returned connections are recaptured until the pool is filled up again to its minimum level. ubsequent releases are then
closed and discarded.

Any released connections still being referenced by clients become unusable.  Any attempt to perform SQL operations will throw
an exception indicating that the connection is no longer active.

Implementation Details
----------------------

SIMPLIFIED CLASS DIAGRAM (needs 100 char columns to format properly)

                             /----------------------------------------------------------------+
  ConnectionPool<Interface> +                                                                 |
           ^                 \-------------------------------> java.sql.Connection <-------+  |
           |                                                           ^                   |  |
           |              /---> java.sql.Driver                        |                   |  |
  AbstractConnectionPool +                              /---> PooledConnection<Interface>  |  |
           ^              \---> PooledConnectionFactory +                                  |  |
           |                                            \---> PooledConnectionProxy +------+  |
           |                                                           +             \        |
  ExpandingSizeConnectionPool <----------------------------------------|             |        |
           |                                                                         |        |
           |     java.sql.SQLException <---------------------------------------------C--------+
           |               ^                                                         |
           |               |                                                         |
           +---> ConnectionPoolException                                             |
                           ^                                                         |
                           |                                                         |
                           +--- ConnectionPoolInitializationException                |
                           |                                                         |
                           +--- ConnectionPoolNewConnectionException                 |
                           |                                                         |
                           +--- ConnectionPoolOutOfConnectionsException              |
                           |                                                         |
                           +--- ConnectionPoolReleaseConnectionException             |
                           |                                                         |
                           +--- ConnectionPoolAlreadyReleasedConnectionException <---+

  LEGEND:
  ^ = inheritance/implementation
  > = dependency/reference


The ConnectionPool interface is implemented by the abstract class AbstractConnectionPool which serves as a starting point for
creating connection pools by holding on to the database driver and being responsible for creating raw database connections as
well as PooledConnection instances.

The PooledConnectionFactory wraps raw database connections inside of dynamic proxies implementing the PooledConnection
interface (which in turn extends Connection).  PooledConnections monitor whether they have been released back
into the pool and prevent further operation by the client (in this case a ConnectionPoolAlreadyReleasedConnectionException is
thrown).

The ExpandingSizeConnectionPool extends the AbstractConnectionPool and manages an internal cache of PooledConnections.  It
retrieves new PooledConnections as necessary to reach a minimum cache size and recycles or releases the returned connections
as necessary to maintain the connections between a minimum cache size and maximum connections allocated.

As the pool performs its operations it catches any underlying database errors from trying to establish or close the raw
connections, and wraps these conditions with a custom exception hierarchy that provides some detail to the client of when/why
the error occurred.

Finally, thread safety is maintained across all classes by, when appropriate, properly enforcing synchronized access across
critical sections.  Symbolic annotation "markers" (e.g., @ThreadSafe, @GuardedBy) from the library provided by the "Java
Concurrency in Practice" book (Goetz, B. et al. Addison Wesley,  2006) are used in the source code to indicate care with
issues of concurrent access.  They are purely for decorative purposes and do not affect compilation or the runtime behavior
of the code.

Project Organization
--------------------
The project uses Maven and the pom.xml file is found at the root folder.

The source code is organized around three Java packages:

 * com.danrama.simpledbconnectionpool -> the root package, contains the connection pool
                                interface and a convenience factory class for
                                creating new pools
 
 * com.danrama.simpledbconnectionpool.impl -> concrete implementations of connection
                                     pools live here, along with any support
                                     classes
 
 * com.danrama.simpledbconnectionpool.exception -> holds the connection pool exception
                                          class hierarchy

Logging
-------
The Log4J library is used for logging messages in the project.  It is configured via the "log4j.properties" file found in
src/main/resources.  The default implementation only prints to standard output and displays messages of ALL levels for the
"com.danrama.connectionpool.*" packages.

This project makes use of the following log levels:

 * DEBUG -> print out messages about the internal pool state: pool is being refilled; detail whether a released connection is
            recycled or not
             
 * WARN -> print out messages to alert the user of failures that should be recoverable: the pool is out of connections; a
           connection being released is already closed
           
 * INFO -> print out messages about high-level events: successful pool creation; successful connection retrieval; successful
           connection release

Tests
-----
JUnit and EasyMock are used to create basic unit tests that verify and validate the correct operation of the
ConnectionPoolFactory and the ExpandingSizeConnectionPool classes:

 * ConnectionPoolFactoryTest (1 test) -> uses a real database driver (HSQLDB)
     to verify that the pool works:
   - testCreateConnectionPoolWithRealDatabaseDriver

 * ExpandingSizeConnectionPoolTest (12 tests) -> performs a variety of tests
     against the pool and uses a mock database driver and connection: 
   - testPoolCreationWithBadParams
   - testPoolCreationWithGoodParams
   - testPoolOfFixedSizeBasicRetrievalAndRelease
   - testPoolWithZeroMinimumSizeBasicRetrievalAndRelease
   - testPoolExpandingSizeBasicRetrievalAndRelease
   - testPoolUsageGetMaximumConnections
   - testPoolUsageExceedMaxConnections
   - testPoolUsageReleaseTheSameConnectionMoreThanOnce
   - testPoolUsageTryToReuseReleasedConnection
   - testPoolUsageReturnAClosedConnection
   - testPoolUsageSimulateGettingAConnectionWithSQLException
   - testPoolUsageSimulateReturningAConnectionWithSQLException

A separate source directory, mimicking the main package structure, is used for hosting the test source code:
src/test/java/com/danrama/connectionpool/*.

To execute the tests run the Maven test call: "mvn test".

Requirements & Dependencies
---------------------------
* JDK 5 and above 
* Log4J 1.2.14 (compile, runtime, test)
* JUnit 4.8.1 (test)
* EasyMock 2.5.2 (test)
* HSQLDB 1.8.0.10 (test)
* jcip-annotations 1.0 (compile)
