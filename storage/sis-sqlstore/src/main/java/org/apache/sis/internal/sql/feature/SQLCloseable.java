package org.apache.sis.internal.sql.feature;

import java.sql.SQLException;

/**
 * Specialisation of {@link AutoCloseable standard closeable objects} for SQL related resources.
 */
interface SQLCloseable extends AutoCloseable {
    @Override
    void close() throws SQLException;
}
