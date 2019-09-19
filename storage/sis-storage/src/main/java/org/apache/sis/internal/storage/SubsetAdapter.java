package org.apache.sis.internal.storage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.storage.FeatureSet;

import static org.apache.sis.internal.storage.query.SimpleQuery.UNLIMITED;

public final class SubsetAdapter {

    final Function<FeatureSet, AdapterBuilder> driverSupplier;

    public SubsetAdapter(Function<FeatureSet, AdapterBuilder> driverSupplier) {
        this.driverSupplier = driverSupplier;
    }

    public final FeatureSet subset(final FeatureSet source, SimpleQuery query) {
        final AdapterBuilder driver = driverSupplier.apply(source);

        final SimpleQuery remaining = new SimpleQuery();

        final long offset = query.getOffset();
        if (offset > 0) remaining.setOffset(driver.offset(offset));

        final long limit = query.getLimit();
        if (limit != UNLIMITED) remaining.setLimit(driver.limit(limit));

        if (filteringRequired(query)) remaining.setFilter(driver.filter(query.getFilter()));

        if (sortRequired(query) && !driver.sort(query.getSortBy())) remaining.setSortBy(query.getSortBy());

        if (!allColumnsIncluded(query)) {
            final SimpleQuery.Column[] remainingCols = driver.select(query.getColumns());
            if (remainingCols != null && remainingCols.length > 0)
                remaining.setColumns(remainingCols);
        }

        final FeatureSet driverSubset = driver.build().orElse(source);

        return isNoOp(remaining)? driverSubset : remaining.execute(driverSubset);
    }

    protected final static boolean isNoOp(final SimpleQuery in) {
        return in.getOffset() <= 0
                && in.getLimit() == UNLIMITED
                && allColumnsIncluded(in)
                && !filteringRequired(in)
                && !sortRequired(in);
    }

    protected final static boolean sortRequired(final SimpleQuery in) {
        final SortBy[] sortBy = in.getSortBy();
        return sortBy != null && sortBy.length > 0 && Arrays.stream(sortBy).anyMatch(Objects::nonNull);
    }

    protected final static boolean allColumnsIncluded(final SimpleQuery in) {
        final List<SimpleQuery.Column> cols = in.getColumns();
        return cols == null || cols.isEmpty();
    }

    protected final static boolean filteringRequired(SimpleQuery in) {
        final Filter filter = in.getFilter();
        return filter != Filter.INCLUDE;
    }

    public interface AdapterBuilder {

        /**
         * Specify an offset to use in custom query.
         *
         * @param offset The offset to handle.
         * @return 0 if this builder can handle completely given offset. The input value if underlying driver cannot
         * manage the offset itself. Note that you can return another value in case the driver and default query system
         * must be stacked. Imagine the case of a partitioned storage, Maybe the driver can handle fixed offsets, and
         * let default implementation managed remaining part of the offset downstream. For example, in a storage where
         * features are chunked 10 by 10, when querying an offset of 12, the inner driver can configure the second
         * partition to be loaded (element 10 to 20), and let default query skip 2 elements after that.
         *
         * @throws IllegalArgumentException If given value is illegal for the driver.
         */
        long offset(long offset);

        /**
         * Set a maximum number of elements to retrieve from custom query.
         *
         * @param limit The count of features to handle.
         * @return {@link SimpleQuery#UNLIMITED} if this builder can handle completely given limit. The input value if
         * underlying driver cannot do it itself, or must be stacked with default query system. Imagine the case of a
         * partitioned storage, Maybe the driver can load entire chunks of data, and let default implementation cut last
         * returned chunk. For example, in a storage where features are chunked 10 by 10, when querying a limit of 12,
         * the inner driver can return two complete partitions (20 elements), and let default query stop processing
         * after 12 elements have gone through.
         *
         * @throws IllegalArgumentException If given value is illegal for the driver.
         */
        long limit(long limit);

        Filter filter(final Filter filter);

        /**
         * Submit a sort subquery to the driver.
         *
         * @param comparison The columns to sort, as specified in {{@link SimpleQuery#getSortBy()}}.
         * @return True if driver handles the comparison. If false, it means that driver won't perform any sort, and the
         * default implementation (i.e {@link SimpleQuery} must handle it.
         */
        boolean sort(final SortBy[] comparison);

        /**
         * Specify a subset of columns to return to the driver.
         * @param columns The columns
         * @return
         */
        SimpleQuery.Column[] select(List<SimpleQuery.Column> columns);

        /**
         * Take a snapshot of all parameters given to query adaptation.
         *
         * @return A custom driver query. If custom query is a no-op, returns an empty shell.
         */
        Optional<FeatureSet> build();
    }
}
