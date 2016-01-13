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
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.ReferenceQueueConsumer;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.logging.Logging;


/**
 * Closes JDBC resources when {@link AuthorityCodes} is garbage collected.
 * Those weak references are stored in the {@link EPSGDataAccess#authorityCodes} map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class CloseableReference<T> extends WeakReference<T> implements Disposable {
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
     * Creates a new phantom reference which will close the given statements
     * when the given referenced object will be garbage collected.
     */
    CloseableReference(final T ref, final EPSGDataAccess factory, final Statement[] statements) {
        super(ref, ReferenceQueueConsumer.QUEUE);
        this.statements = statements;
        this.factory = factory;
    }

    /**
     * Closes the statements. If an exception occurred, it will be thrown only after all statements have been closed.
     *
     * @throws SQLException if an error occurred while closing the statements.
     */
    final void close() throws SQLException {
        SQLException exception = null;
        synchronized (factory) {
            for (int i=statements.length; --i >= 0;) {
                final Statement s = statements[i];
                statements[i] = null;
                if (s != null) try {
                    s.close();
                } catch (SQLException e) {
                    if (exception == null) {
                        exception = e;
                    } else {
                        // exception.addSuppressed(e) on the JDK7 branch.
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
            Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY), AuthorityCodes.class, "close", exception);
        }
    }
}
