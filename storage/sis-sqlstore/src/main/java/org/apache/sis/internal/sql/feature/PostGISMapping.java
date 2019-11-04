package org.apache.sis.internal.sql.feature;

import java.sql.Types;
import java.util.Optional;

import org.apache.sis.internal.metadata.sql.Dialect;

public class PostGISMapping implements DialectMapping {
    @Override
    public Dialect getDialect() {
        return Dialect.POSTGRESQL;
    }

    @Override
    public Optional<ColumnAdapter<?>> getMapping(int sqlType, String sqlTypeName) {
        switch (sqlType) {
            case (Types.OTHER):
        }
        return Optional.empty();
    }

    private ColumnAdapter<?> forOther(String sqlTypeName) {
        switch (sqlTypeName.toLowerCase()) {
            case "geometry":
            case "geography":
            default: return null;
        }
    }
}
