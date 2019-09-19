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

    private SimpleQuery.Column[] columns;
    private SortBy[] sorting;

    public SQLQueryAdapter(Table parent) {
        this.parent = parent;
    }

    @Override
    public long offset(long offset) {
        return offset; // Done by stream overload
    }

    @Override
    public long limit(long limit) {
        return limit; // Done by stream overload
    }

    @Override
    public Filter filter(Filter filter) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 18/09/2019
    }

    @Override
    public boolean sort(SortBy[] comparison) {
        sorting = Arrays.copyOf(comparison, comparison.length);
        return true;
    }

    @Override
    public SimpleQuery.Column[] select(List<SimpleQuery.Column> columns) {
        this.columns = columns.toArray(new SimpleQuery.Column[columns.size()]);
        return null;
    }

    @Override
    public Optional<FeatureSet> build() {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 18/09/2019
    }
}
