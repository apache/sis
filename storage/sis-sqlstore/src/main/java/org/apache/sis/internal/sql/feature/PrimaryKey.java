package org.apache.sis.internal.sql.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.sis.util.ArgumentChecks;

/**
 * Represents SQL primary key constraint. Main information is columns composing the key.
 *
 * @implNote For now, only list of columns composing the key are returned. However, in the future it would be possible
 * to add other information, as a value type to describe how to expose primary key value.
 *
 * @author "Alexis Manin (Geomatys)"
 */
interface PrimaryKey {

    static Optional<PrimaryKey> create(List<String> cols) {
        if (cols == null || cols.isEmpty()) return Optional.empty();
        if (cols.size() == 1) return Optional.of(new Simple(cols.get(0)));
        return Optional.of(new Composite(cols));
    }

    /**
     *
     * @return List of column names composing the key. Should neither be null nor empty.
     */
    List<String> getColumns();

    class Simple implements PrimaryKey {
        final String column;

        Simple(String column) {
            this.column = column;
        }

        @Override
        public List<String> getColumns() { return Collections.singletonList(column); }
    }

    class Composite implements PrimaryKey {
        /**
         * Name of columns composing primary keys.
         */
        private final List<String> columns;

        Composite(List<String> columns) {
            ArgumentChecks.ensureNonEmpty("Primary key column names", columns);
            this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        }

        @Override
        public List<String> getColumns() {
            return columns;
        }
    }
}
