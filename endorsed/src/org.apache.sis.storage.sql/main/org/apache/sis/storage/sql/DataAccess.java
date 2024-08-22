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
package org.apache.sis.storage.sql;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.sql.feature.Database;
import org.apache.sis.storage.sql.feature.InfoStatements;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Errors;


/**
 * Provides a <abbr>SQL</abbr> connection to the database, together with helper methods.
 * Each {@code DataAccess} object can hold a {@link Connection} (created when first needed),
 * and sometime a read or write lock. The locks are used only for some databases such as SQLite,
 * when concurrency issues are observed during database transactions.
 *
 * <p>This object shall be used in a {@code try ... finally} block for ensuring that the connection
 * is closed and the lock (if any) released. Note that the <abbr>SQL</abbr> connection does not need
 * to be closed by users, because it will be closed by the {@link #close()} method of this data access
 * object. Example:</p>
 *
 * {@snippet lang="java" :
 *   SQLStore store = ...;
 *   try (DataAccess dao = store.newDataAccess(false)) {
 *       Connection cnx = dao.getConnection();
 *       try (Statement stmt = cnx.createStatement()) {
 *           // Perform some SQL queries here.
 *       }
 *   }
 *   }
 *
 * This class is not thread safe. Each instance should be used by a single thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class DataAccess implements AutoCloseable {
    /**
     * The data store for which this object is providing data access.
     * The value of this field is specified at construction time.
     */
    protected final SQLStore store;

    /**
     * The SQL connection, created when first needed.
     */
    private Connection connection;

    /**
     * Helper methods for fetching information such as coordinate reference systems.
     * Created when first needed.
     */
    private InfoStatements statements;

    /**
     * A read or write lock to unlock when the data access will be closed, or {@code null} if none.
     */
    private Lock lock;

    /**
     * Whether this data access object will allow write operations.
     * This flag also determines whether the lock to acquire (if any) will be a read lock or a write lock.
     * The value of this field is specified at construction time.
     */
    protected final boolean write;

    /**
     * Whether this data access object has been closed.
     *
     * @see #ensureOpen()
     * @see #close()
     */
    private boolean isClosed;

    /**
     * Creates a new data access object for the given SQL store.
     *
     * @param  store  the data store for which this object is providing data access.
     * @param  write  whether write operations may be requested.
     *
     * @see SQLStore#newDataAccess(boolean)
     */
    protected DataAccess(final SQLStore store, final boolean write) {
        this.store = Objects.requireNonNull(store);
        this.write = write;
    }

    /**
     * Returns the error message for the exception to throw when the connection is closed.
     */
    private String closed() {
        return Errors.forLocale(store.getLocale()).getString(Errors.Keys.ConnectionClosed);
    }

    /**
     * Returns the connection to the database.
     * The connection is established on the first time that this method is invoked,
     * then the same connection is returned until this data access object is {@linkplain #close() closed}.
     * If a read or write lock is needed, it will be acquired before the connection is established.
     *
     * @return connection to the database.
     * @throws SQLException if the connection cannot be established.
     */
    public final Connection getConnection() throws SQLException {
        if (isClosed) {
            throw new SQLNonTransientConnectionException(closed());
        }
        if (connection == null) {
            final ReadWriteLock transactionLocks = store.transactionLocks;
            if (transactionLocks != null) {
                final Lock c = write ? transactionLocks.writeLock() : transactionLocks.readLock();
                c.lock();
                lock = c;       // Store only if the lock succeed.
            }
            connection = store.getDataSource().getConnection();
        }
        return connection;
    }

    /**
     * Returns the helper object for fetching information from {@code SPATIAL_REF_SYS} table.
     * The helper object is created the first time that this method is invoked.
     */
    private InfoStatements statements() throws Exception {
        if (statements == null) {
            final Connection c = getConnection();
            synchronized (store) {
                final Database<?> model = store.model(c);
                if (model.dialect.supportsReadOnlyUpdate()) {
                    // TODO: should be in `getConnection() if we could remove the need for `supportsReadOnlyUpdate()`.
                    c.setReadOnly(!write);
                }
                statements = model.createInfoStatements(c);
            }
        }
        return statements;
    }

    /**
     * Returns the coordinate reference system associated to the given identifier. The spatial reference
     * system identifiers (<abbr>SRID</abbr>) are the primary keys of the {@code "SPATIAL_REF_SYS"} table
     * (the name of that table may vary depending on which spatial schema standard is used).
     * Those identifiers are specific to each database and are not necessarily related to EPSG codes.
     * They should be considered as opaque identifiers.
     *
     * <h4>Undefined <abbr>CRS</abbr></h4>
     * Some standards such as Geopackage define 0 as "undefined geographic <abbr>CRS</abbr>" and -1 as
     * "undefined Cartesian <abbr>CRS</abbr>". This method returns {@code null} for all undefined <abbr>CRS</abbr>,
     * regardless their type. No default value is returned because this class cannot guess the datum and units of
     * measurement of an undefined <abbr>CRS</abbr>. All <abbr>SRID</abbr> equal or less than zero are considered
     * undefined.
     *
     * <h4>Axis order</h4>
     * Some standards such as Geopackage mandate (east, north) axis order. {@code SQLStore} uses the axis order
     * as defined in the <abbr>WKT</abbr> descriptions of the {@code "SPATIAL_REF_SYS"} table. No reordering is
     * applied. It is data producer's responsibility to provide definitions with the expected axis order.
     *
     * @param  srid  a primary key value of the {@code "SPATIAL_REF_SYS"} table.
     * @return the <abbr>CRS</abbr> associated to the given <abbr>SRID</abbr>, or {@code null} if the given
     *         <abbr>SRID</abbr> is a code explicitly associated to an undefined <abbr>CRS</abbr>.
     * @throws NoSuchDataException if no <abbr>CRS</abbr> is associated to the given <abbr>SRID</abbr>.
     * @throws DataStoreException if the query failed for another reason.
     */
    public CoordinateReferenceSystem findCRS(final int srid) throws DataStoreException {
        if (isClosed) {
            throw new DataStoreClosedException(closed());
        }
        if (srid <= 0) {
            return null;
        }
        Database<?> database = store.modelOrNull();
        CoordinateReferenceSystem crs;
        if (database == null || (crs = database.getCachedCRS(srid)) == null) try {
            crs = statements().fetchCRS(srid);
        } catch (DataStoreContentException e) {
            throw new NoSuchDataException(e.getMessage(), e.getCause());
        } catch (DataStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new DataStoreException(Exceptions.unwrap(e));
        }
        return crs;
    }

    /**
     * Returns the <abbr>SRID</abbr> associated to the given spatial reference system.
     * This method is the converse of {@link #findCRS(int)}.
     *
     * <p>If the {@code write} argument given at construction time was {@code true}, then this method is allowed
     * to add a new row in the {@code "SPATIAL_REF_SYS"} table if the given <abbr>CRS</abbr> is not found.</p>
     *
     * @param  crs  the CRS for which to find a SRID, or {@code null}.
     * @return SRID for the given <abbr>CRS</abbr>, or 0 if the given <abbr>CRS</abbr> was null.
     * @throws DataStoreException if an <abbr>SQL</abbr> error, parsing error or other error occurred.
     */
    public int findSRID(final CoordinateReferenceSystem crs) throws DataStoreException {
        if (isClosed) {
            throw new DataStoreClosedException(closed());
        }
        if (crs == null) {
            return 0;
        }
        Database<?> database = store.modelOrNull();
        if (database != null) {
            Integer srid = database.getCachedSRID(crs);
            if (srid != null) return srid;
        }
        final int srid;
        try {
            srid = statements().findSRID(crs);
        } catch (DataStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new DataStoreException(Exceptions.unwrap(e));
        }
        return srid;
    }

    /**
     * Closes the connection and release the read or write lock (if any).
     *
     * @throws SQLException if an error occurred while closing the connection.
     */
    @Override
    public void close() throws SQLException {
        isClosed = true;                // Set first in case an exception is thrown.
        try {
            try {
                final InfoStatements c = statements;
                if (c != null) {
                    statements = null;
                    c.close();
                }
            } finally {
                final Connection c = connection;
                if (c != null) {
                    connection = null;
                    c.close();          // Does nothing if already closed by the user.
                }
            }
        } finally {
            final Lock c = lock;
            if (c != null) {
                lock = null;
                c.unlock();
            }
        }
    }
}
