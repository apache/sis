/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.metadata.sql;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.DelayedExecutor;
import org.apache.sis.internal.system.DelayedRunnable;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.metadata.sql.SQLBuilder;


/**
 * A pool of prepared statements with a maximal capacity. Oldest statements are automatically
 * closed and removed from the map when the number of statements exceed the maximal capacity.
 * Inactive statements are also closed after some timeout.
 *
 * <div class="note"><b>Note:</b>
 * this class duplicates the work done by statement pools in modern JDBC drivers. Nevertheless
 * it still useful in our case since we retain some additional JDBC resources together with the
 * {@link PreparedStatement}, for example the {@link ResultSet} created from that statement.</div>
 *
 * Every access to this pool <strong>must</strong> be synchronized on {@code this}.
 * Synchronization is caller's responsibility; this class is not thread safe alone.
 * Synchronization will be verified if assertions are enabled.
 *
 * <div class="note"><b>Rational:</b>
 * synchronization must be performed by the caller because we typically need synchronized block
 * wider than the {@code get} and {@code put} scope. Execution of a prepared statement may also
 * need to be done inside the synchronized block, because a single JDBC connection can not be
 * assumed thread-safe.</div>
 *
 * <p>Usage example:</p>
 * {@preformat java
 *     ResultPool pool = …;
 *     Class<?>   type = …;
 *     synchronized (pool) {
 *         // Get an entry, or create a new one if no entry is available.
 *         MetadataResult statement = pool.take(type, preferredIndex);
 *         if (statement == null) {
 *             statement = new MetadataResult(someStatement);
 *         }
 *         // Use the statement and give it back to the pool once we are done.
 *         // We do not put it back in case of SQLException.
 *         Object value = statement.getValue(…);
 *         preferredIndex = pool.recycle(statement, preferredIndex);
 *     }
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class ResultPool {
    /**
     * The timeout before to close a prepared statement, in nanoseconds. This is set to 2 seconds,
     * which is a bit short but should be okay if the {@link DataSource} creates pooled connections.
     * In case there is no connection pool, then the mechanism defined in this package will hopefully
     * keeps the performance at a reasonable level.
     */
    private static final long TIMEOUT = 2000_000000;

    /**
     * An extra delay to add to the {@link #TIMEOUT} in order to increase the chances to
     * close many statements at once.
     */
    private static final int EXTRA_DELAY = 500_000000;

    /**
     * The data source object for fetching the connection to the database.
     */
    private final DataSource dataSource;

    /**
     * The connection to the database. This is automatically closed and set to {@code null}
     * after all statements have been closed.
     *
     * @see #connection()
     */
    private Connection connection;

    /**
     * JDBC statements available for reuse. The array length is the maximal number of statements that can
     * be cached; it should be a reasonably small length. This array may contain null element anywhere.
     */
    private final MetadataResult[] cache;

    /**
     * Whether at least one {@link CloseTask} is scheduled for execution.
     */
    private boolean isCloseScheduled;

    /**
     * Where to report the warnings. This is not necessarily a logger, since users can register listeners.
     */
    final WarningListeners<MetadataSource> listeners;

    /**
     * A helper class used for constructing SQL statements.
     * This helper is created when first needed, then kept until the connection is closed.
     */
    private SQLBuilder helper;

    /**
     * Creates a new pool for the given data source.
     *
     * @param dataSource  the source of connections to the database.
     * @param owner       the object that will contain this {@code ResultPool}.
     */
    ResultPool(final DataSource dataSource, final MetadataSource owner) {
        this.dataSource = dataSource;
        this.listeners  = new WarningListeners<>(owner);
        this.cache      = new MetadataResult[10];
    }

    /**
     * Creates a new pool with the same configuration than the given pool.
     * The new pool will use its own connection - it will not be shared.
     *
     * @param source  the pool from which to copy the configuration.
     */
    ResultPool(final ResultPool source) {
        dataSource = source.dataSource;
        listeners  = source.listeners;
        cache      = new MetadataResult[source.cache.length];
    }

    /**
     * Returns the connection to the database, creating a new one if needed. This method shall
     * be invoked inside a synchronized block wider than just the scope of this method in order
     * to ensure that the connection is used by only one thread at time. This is also necessary
     * for preventing the background thread to close the connection too early.
     *
     * @return the connection to the database.
     * @throws SQLException if an error occurred while fetching the connection.
     */
    final Connection connection() throws SQLException {
        assert Thread.holdsLock(this);
        Connection c = connection;
        if (c == null) {
            connection = c = dataSource.getConnection();
            Logging.log(MetadataSource.class, "lookup", Initializer.connected(connection.getMetaData()));
        }
        return c;
    }

    /**
     * Returns a helper class for building SQL statements.
     */
    final SQLBuilder helper() throws SQLException {
        assert Thread.holdsLock(this);
        if (helper == null) {
            helper = new SQLBuilder(connection().getMetaData());
        }
        return helper;
    }

    /**
     * Returns a statement that can be reused for the given interface, or {@code null} if none.
     *
     * @param  type            the interface for which to reuse a prepared statement.
     * @param  preferredIndex  index in the cache array where to search first. This is only a hint for increasing
     *         the chances to find quickly a {@code MetadataResult} instance for the right type and identifier.
     */
    final MetadataResult take(final Class<?> type, final int preferredIndex) {
        assert Thread.holdsLock(this);
        if (preferredIndex >= 0 && preferredIndex < cache.length) {
            final MetadataResult statement = cache[preferredIndex];
            if (statement != null && statement.type == type) {
                cache[preferredIndex] = null;
                return statement;
            }
        }
        for (int i=0; i < cache.length; i++) {
            final MetadataResult statement = cache[i];
            if (statement != null && statement.type == type) {
                cache[i] = null;
                return statement;
            }
        }
        return null;
    }

    /**
     * Flags the given {@code MetadataResult} as available for reuse.
     *
     * @param  statement       the prepared statement to cache.
     * @param  preferredIndex  index in the cache array to use if the corresponding slot is available.
     * @return index in the cache array where the result has been actually stored, or -1 if none.
     */
    final int recycle(final MetadataResult statement, int preferredIndex) {
        assert Thread.holdsLock(this);
        if (preferredIndex < 0 || preferredIndex >= cache.length || cache[preferredIndex] != null) {
            preferredIndex = 0;
            while (cache[preferredIndex] != null) {
                if (++preferredIndex >= cache.length) {
                    return -1;
                }
            }
        }
        cache[preferredIndex] = statement;
        statement.expireTime = System.nanoTime() + TIMEOUT;
        if (!isCloseScheduled) {
            DelayedExecutor.schedule(new CloseTask(System.nanoTime() + (TIMEOUT + EXTRA_DELAY)));
            isCloseScheduled = true;
        }
        return preferredIndex;
    }

    /**
     * A task to be executed later for closing all expired {@link MetadataResult}.
     * A result is expired if {@link MetadataResult#expireTime} is later than {@link System#nanoTime()}.
     */
    private final class CloseTask extends DelayedRunnable {
        /**
         * Creates a new task to be executed later.
         *
         * @param timestamp  time of execution of this task, in nanoseconds relative to {@link System#nanoTime()}.
         */
        CloseTask(final long timestamp) {
            super(timestamp);
        }

        /**
         * Invoked in a background thread for closing all expired {@link MetadataResult} instances.
         */
        @Override public void run() {
            closeExpired();
        }
    }

    /**
     * Executed in a background thread for closing statements after their expiration time.
     * This task will be given to the executor every time the first statement is recycled.
     */
    final synchronized void closeExpired() {
        isCloseScheduled = false;
        long delay = 0;
        final long currentTime = System.nanoTime();
        for (int i=0; i < cache.length; i++) {
            final MetadataResult statement = cache[i];
            if (statement != null) {
                /*
                 * Note: we really need to compute t1 - t0 and compare the delays.
                 * Do not simplify the equations in a way that result in comparisons
                 * like t1 > t0. See System.nanoTime() javadoc for more information.
                 */
                final long wait = statement.expireTime - currentTime;
                if (wait > delay) {
                    delay = wait;
                } else {
                    cache[i] = null;
                    closeQuietly(statement);
                }
            }
        }
        if (delay > 0) {
            // Some statements can not be disposed yet.
            DelayedExecutor.schedule(new CloseTask(currentTime + delay + EXTRA_DELAY));
            isCloseScheduled = true;
        } else {
            // No more prepared statements.
            final Connection c = this.connection;
            connection = null;
            helper = null;
            closeQuietly(c);
        }
    }

    /**
     * Closes the given resource without throwing exception. In case of failure while closing the resource,
     * the message is logged but the process continue since we are not supposed to use the resource anymore.
     * This method is invoked from methods that can not throw a SQL exception.
     */
    private void closeQuietly(final AutoCloseable resource) {
        if (resource != null) try {
            resource.close();
        } catch (Exception e) {
            /*
             * Catch Exception rather than SQLException because this method is invoked from semi-critical code
             * which need to never fail, otherwise some memory leak could occur. Pretend that the message come
             * from ResultPool.closeExpired(), which is the closest we can get to a public API.
             */
            final LogRecord record = new LogRecord(Level.WARNING, e.toString());
            record.setSourceClassName(ResultPool.class.getCanonicalName());
            record.setSourceMethodName("closeExpired");
            record.setLoggerName(Loggers.SQL);
            record.setThrown(e);
            listeners.warning(record);
        }
    }

    /**
     * Closes all statements and removes them from the pool.
     *
     * @throws SQLException if an error occurred while closing the statements.
     */
    final synchronized void close() throws SQLException {
        for (int i=0; i < cache.length; i++) {
            final MetadataResult statement = cache[i];
            if (statement != null) {
                statement.close();
                cache[i] = null;
            }
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
        helper = null;
    }
}
