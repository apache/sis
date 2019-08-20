package org.apache.sis.internal.sql.feature;

import java.util.Objects;

import org.apache.sis.internal.metadata.sql.SQLBuilder;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * A column reference. Specify name of the column, and optionally an alias to use for public visibility.
 * By default, column has no alias. To create a column with an alias, use {@code ColumnRef myCol = new ColumnRef("colName).as("myAlias");}
 */
public final class ColumnRef {
    final String name;
    final String alias;
    final String attrName;

    public ColumnRef(String name) {
        ensureNonNull("Column name", name);
        this.name = this.attrName = name;
        alias = null;
    }

    private ColumnRef(final String name, final String alias) {
        ensureNonNull("Column alias", alias);
        this.name = name;
        this.alias = this.attrName = alias;
    }

    ColumnRef as(final String alias) {
        return new ColumnRef(name, alias);
    }

    SQLBuilder append(final SQLBuilder target) {
        target.appendIdentifier(name);
        if (alias != null) {
            target.append(" AS ").appendIdentifier(alias);
        }

        return target;
    }

    public String getAttributeName() {
        return attrName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnRef)) return false;
        ColumnRef columnRef = (ColumnRef) o;
        return name.equals(columnRef.name) &&
                Objects.equals(alias, columnRef.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias);
    }
}
