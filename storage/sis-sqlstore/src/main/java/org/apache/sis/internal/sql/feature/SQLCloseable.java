package org.apache.sis.internal.sql.feature;

import java.sql.SQLException;

public interface SQLCloseable extends AutoCloseable {
    @Override
    void close() throws SQLException;
}
