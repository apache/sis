package org.apache.sis.internal.sql.feature;

import java.sql.SQLException;
import java.util.function.Function;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * Useful to customize value retrieval on result sets. Example:
 * {@code
 * SQLBiFunction<ResultSet, Integer, Integer> get = ResultSet::getInt;
 * }
 * @param <T> Type of the first arguement of the function.
 * @param <U> Type of the second argument of the function.
 * @param <R> Type of the function result.
 */
@FunctionalInterface
interface SQLBiFunction<T, U, R> {
    R apply(T t, U u) throws SQLException;

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> SQLBiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
        ensureNonNull("After function", after);
        return (T t, U u) -> after.apply(apply(t, u));
    }
}
