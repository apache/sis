package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.apache.sis.internal.metadata.sql.Dialect;

public interface DialectMapping extends SQLCloseable {

    Spi getSpi();

    Optional<ColumnAdapter<?>> getMapping(final SQLColumn columnDefinition);

    interface Spi {
        /**
         *
         * @param c The connection to use to connect to the database. It will be read-only.
         * @return A component compatible with database of given connection, or nothing if the database is not supported
         * by this component.
         * @throws SQLException If an error occurs while fetching information from database.
         */
        Optional<DialectMapping> create(final Connection c) throws SQLException;

        Dialect getDialect();
    }
}
