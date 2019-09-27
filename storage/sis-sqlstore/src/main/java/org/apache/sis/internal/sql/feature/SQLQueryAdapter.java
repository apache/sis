package org.apache.sis.internal.sql.feature;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import org.apache.sis.internal.storage.SubsetAdapter;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.storage.FeatureSet;

public class SQLQueryAdapter implements SubsetAdapter.AdapterBuilder {

    final Table parent;

    private ColumnRef[] columns;
    private SortBy[] sorting;

    private CharSequence where;

    public SQLQueryAdapter(Table parent) {
        this.parent = parent;
    }

    /**
     * No-op implementation. SQL optimisation is dynamically applied through {@link StreamSQL}.
     * @param offset The offset to handle.
     * @return Input offset.
     */
    @Override
    public long offset(long offset) {
        return offset;
    }

    /**
     * No-op implementation. SQL optimisation is dynamically applied through {@link StreamSQL}.
     * @param limit The limit to handle.
     * @return Input limit.
     */
    @Override
    public long limit(long limit) {
        return limit;
    }

    @Override
    public Filter filter(Filter filter) {
        try {
            final Object result = filter.accept(new ANSIInterpreter(), null);
            if (ANSIInterpreter.isNonEmptyText(result)) {
                where = (CharSequence) result;
                return Filter.INCLUDE;
            }
        } catch (UnsupportedOperationException e) {
            // TODO: log
            where = null;
        }

        return filter;
    }

    @Override
    public boolean sort(SortBy[] comparison) {
        sorting = Arrays.copyOf(comparison, comparison.length);
        return false;
    }

    @Override
    public boolean select(List<SimpleQuery.Column> columns) {
        /* We've got a lot of trouble with current column API. It defines an expression and an alias, which allow to
         * infer output property type. However, it's very difficult with current methods to infer source columns used
         * for building output. Note that we could check if column expression is a property name or a literal, but if
         * any column is not one of those two, it means it uses unknown (for us) SQL columns, so we cannot filter
         * selected columns safely.
         */
        return false;
    }

    @Override
    public Optional<FeatureSet> build() {
        if (isNoOp()) return Optional.empty();
        return Optional.of(new TableSubset(parent, sorting, where));
    }

    private boolean isNoOp() {
        return (sorting == null || sorting.length < 1)
                && (columns == null || columns.length < 1)
                && (where == null || where.length() < 1);
    }
}
