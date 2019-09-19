package org.apache.sis.internal.sql.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.sis.util.ArgumentChecks;

/**
 * Represents SQL primary key constraint. Main information is columns composing the key.
 *
 * @author "Alexis Manin (Geomatys)"
 */
interface PrimaryKey {

    static Optional<PrimaryKey> create(List<String> cols) {
        if (cols == null || cols.isEmpty()) return Optional.empty();
        if (cols.size() == 1) return Optional.of(new Simple(cols.get(0)));
        return Optional.of(new Composite(cols));
    }

    //Class<T> getViewType();
    List<String> getColumns();

    class Simple implements PrimaryKey {
        final String column;

        public Simple(String column) {
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

        public Composite(List<String> columns) {
            ArgumentChecks.ensureNonEmpty("Primary key column names", columns);
            this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        }

        @Override
        public List<String> getColumns() {
            return columns;
        }
    }
}
