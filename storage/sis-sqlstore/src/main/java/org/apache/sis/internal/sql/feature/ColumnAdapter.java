package org.apache.sis.internal.sql.feature;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * Utility to handle conversion of a result set cell value. This object is a bi-function whose input is a result set
 * placed on row of interest, and an index specifying which column defines the cell to read on this line.
 *
 * @param <T> Type of object decoded from cell.
 */
class ColumnAdapter<T> implements SQLBiFunction<ResultSet, Integer, T> {
    final Class<T> javaType;
    private final SQLBiFunction<ResultSet, Integer, T> fetchValue;

    protected ColumnAdapter(Class<T> javaType, SQLBiFunction<ResultSet, Integer, T> fetchValue) {
        ensureNonNull("Result java type", javaType);
        ensureNonNull("Function for value retrieval", fetchValue);
        this.javaType = javaType;
        this.fetchValue = fetchValue;
    }

    @Override
    public T apply(ResultSet resultSet, Integer integer) throws SQLException {
        return fetchValue.apply(resultSet, integer);
    }

    @Override
    public <V> SQLBiFunction<ResultSet, Integer, V> andThen(Function<? super T, ? extends V> after) {
        return fetchValue.andThen(after);
    }
}
