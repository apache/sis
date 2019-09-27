package org.apache.sis.internal.sql.feature;

import java.util.Optional;
import java.util.stream.Stream;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.sort.SortBy;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;

public class TableSubset implements FeatureSet {

    final Table parent;
    final SortBy[] sorting;
    final CharSequence where;

    public TableSubset(Table parent, SortBy[] sorting, CharSequence where) {
        this.parent = parent;
        this.sorting = sorting;
        this.where = where;
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return parent.getType();
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        final Features.Builder builder = new Features.Builder(parent)
                .where(where)
                .sortBy(sorting);
        return new StreamSQL(builder, parent.source, parallel);
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return parent.getEnvelope();
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.empty();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return parent.getMetadata();
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
        parent.addListener(listener, eventType);
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
        parent.removeListener(listener, eventType);
    }
}
