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
package org.apache.sis.referencing.factory.sql;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.sis.system.ReferenceQueueConsumer;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.logging.Logging;


/**
 * Closes JDBC statements when {@link AuthorityCodes} is garbage collected.
 * Those weak references are stored in the {@link EPSGDataAccess#authorityCodes} map as cached values.
 * Connection is not closed by this class because they will be closed when {@link EPSGDataAccess} will
 * be closed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see EPSGFactory#canClose(EPSGDataAccess)
 */
final class CloseableReference extends WeakReference<AuthorityCodes> implements Disposable {
    /**
     * The EPSG factory, used for synchronization lock.
     */
    private final EPSGDataAccess factory;

    /**
     * The statements to close. Statements will be closed in reverse order, with null elements ignored.
     * A synchronization lock will be hold on the array.
     */
    private final Statement[] statements;

    /**
     * Whether the referenced {@link AuthorityCodes} has been given to the user.
     * If {@code false}, we can invoke {@link #close()} without waiting for the garbage collection.
     */
    boolean published;

    /**
     * Creates a new phantom reference which will close the given statements
     * when the given referenced object will be garbage collected.
     */
    CloseableReference(final AuthorityCodes ref, final EPSGDataAccess factory, final Statement[] statements) {
        super(ref, ReferenceQueueConsumer.QUEUE);
        this.statements = statements;
        this.factory = factory;
    }

    /**
     * Closes the statements. If an exception occurred, it will be thrown only after all statements have been closed.
     * The connection is not closed in this method because it will be closed later by (indirectly)
     * {@link org.apache.sis.referencing.factory.ConcurrentAuthorityFactory#close(List)}.
     *
     * @throws SQLException if an error occurred while closing the statements.
     */
    final void close() throws SQLException {
        SQLException exception = null;
        synchronized (factory) {
            for (int i = statements.length; --i >= 0;) {
                final Statement s = statements[i];
                statements[i] = null;
                if (s != null) try {
                    s.close();
                } catch (SQLException e) {
                    if (exception == null) {
                        exception = e;
                    } else {
                        exception.addSuppressed(e);
                    }
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Invoked indirectly by the garbage collector.
     */
    @Override
    public void dispose() {
        try {
            close();
        } catch (SQLException exception) {
            /*
             * There is nothing we can do here. It is not even worth to throw an unchecked exception because
             * this method is invoked from a background thread, so the exception would not reach user's code.
             * Pretend that the logging come from AuthorityCodes because it is closer to a public API (or at
             * least, easier to guess that it is related to the EPSGDataAccess.getAuthorityCodes() method).
             */
            Logging.unexpectedException(EPSGDataAccess.LOGGER, AuthorityCodes.class, "close", exception);
        }
    }
}
