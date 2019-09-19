package org.apache.sis.internal.sql.feature;

import java.sql.ResultSetMetaData;
import java.util.Optional;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

class SQLColumn {
    final int type;
    final String typeName;
    private final boolean isNullable;
    private final ColumnRef naming;
    private final int precision;

    SQLColumn(int type, String typeName, boolean isNullable, ColumnRef naming, int precision) {
        this.type = type;
        this.typeName = typeName;
        this.isNullable = isNullable;
        this.naming = naming;
        this.precision = precision;
    }

    public ColumnRef getName() {
        return naming;
    }

    public int getType() {
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isNullable() {
        return isNullable;
    }

    /**
     * Same as {@link ResultSetMetaData#getPrecision(int)}.
     * @return 0 if unknown. For texts, maximum number of characters allowed. For numerics, max precision. For blobs,
     * number of bytes allowed.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * TODO: implement.
     * Note : This method could be used not only for geometric fields, but also on numeric ones representing 1D
     * systems.
     *
     * @return null for now, implementation needed.
     */
    public Optional<CoordinateReferenceSystem> getCrs() {
        return Optional.empty();
    }
}
