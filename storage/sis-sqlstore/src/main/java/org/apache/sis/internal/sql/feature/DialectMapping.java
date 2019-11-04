package org.apache.sis.internal.sql.feature;

import java.util.Optional;

import org.apache.sis.internal.metadata.sql.Dialect;

public interface DialectMapping {

    Dialect getDialect();

    Optional<ColumnAdapter<?>> getMapping(final int sqlType, final String sqlTypeName);
}
