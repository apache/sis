package org.apache.sis.internal.sql.feature;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import org.apache.sis.internal.sql.feature.SpatialFunctions.ColumnAdapter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

class FeatureAdapter {

    final FeatureType type;

    private final List<PropertyMapper> attributeMappers;

    FeatureAdapter(FeatureType type, List<PropertyMapper> attributeMappers) {
        ensureNonNull("Target feature type", type);
        ensureNonNull("Attribute mappers", attributeMappers);
        this.type = type;
        this.attributeMappers = Collections.unmodifiableList(new ArrayList<>(attributeMappers));
    }

    Feature read(final ResultSet cursor) throws SQLException {
        final Feature result = readAttributes(cursor);
        addImports(result, cursor);
        addExports(result);
        return result;
    }

    private void addImports(final Feature target, final ResultSet cursor) {
        // TODO: see Features class
    }

    private void addExports(final Feature target) {
        // TODO: see Features class
    }

    private Feature readAttributes(final ResultSet cursor) throws SQLException {
        final Feature result = type.newInstance();
        for (PropertyMapper mapper : attributeMappers) mapper.read(cursor, result);
        return result;
    }

    List<Feature> prefetch(final int size, final ResultSet cursor) throws SQLException {
        // TODO: optimize by resolving import associations by  batch import fetching.
        final ArrayList<Feature> features = new ArrayList<>(size);
        for (int i = 0 ; i < size && cursor.next() ; i++) {
            features.add(read(cursor));
        }

        return features;
    }

    static class PropertyMapper {
        // TODO: by using a indexed implementation of Feature, we could avoid the name mapping. However, a JMH benchmark
        // would be required in order to be sure it's impacting performance positively.
        final String propertyName;
        final int columnIndex;
        final ColumnAdapter fetchValue;

        PropertyMapper(String propertyName, int columnIndex, ColumnAdapter fetchValue) {
            this.propertyName = propertyName;
            this.columnIndex = columnIndex;
            this.fetchValue = fetchValue;
        }

        private void read(ResultSet cursor, Feature target) throws SQLException {
            final Object value = fetchValue.apply(cursor, columnIndex);
            if (value != null) target.setPropertyValue(propertyName, value);
        }
    }
}
