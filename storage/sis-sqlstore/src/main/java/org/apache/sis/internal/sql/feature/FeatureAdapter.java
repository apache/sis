package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

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

    ResultSetAdapter prepare(final Connection target) {
        final List<ReadyMapper> rtu = attributeMappers.stream()
                .map(mapper -> mapper.prepare(target))
                .collect(Collectors.toList());
        return new ResultSetAdapter(rtu);
    }

    final class ResultSetAdapter {
        final List<ReadyMapper> mappers;

        ResultSetAdapter(List<ReadyMapper> mappers) {
            this.mappers = mappers;
        }

        Feature read(final ResultSet cursor) throws SQLException {
            final Feature result = readAttributes(cursor);
            addImports(result, cursor);
            addExports(result);
            return result;
        }

        private Feature readAttributes(final ResultSet cursor) throws SQLException {
            final Feature result = type.newInstance();
            for (ReadyMapper mapper : mappers) mapper.read(cursor, result);
            return result;
        }

        //final SQLBiFunction<ResultSet, Integer, ?>[] adapters;
        List<Feature> prefetch(final int size, final ResultSet cursor) throws SQLException {
            // TODO: optimize by resolving import associations by  batch import fetching.
            final ArrayList<Feature> features = new ArrayList<>(size);
            for (int i = 0 ; i < size && cursor.next() ; i++) {
                features.add(read(cursor));
            }

            return features;
        }

        private void addImports(final Feature target, final ResultSet cursor) {
            // TODO: see Features class
        }

        private void addExports(final Feature target) {
            // TODO: see Features class
        }
    }

    static final class PropertyMapper {
        // TODO: by using a indexed implementation of Feature, we could avoid the name mapping. However, a JMH benchmark
        // would be required in order to be sure it's impacting performance positively. also, features are sparse by
        // nature, and an indexed implementation could (to verify, still) be bad on memory footprint.
        final String propertyName;
        final int columnIndex;
        final ColumnAdapter fetchValue;

        PropertyMapper(String propertyName, int columnIndex, ColumnAdapter fetchValue) {
            this.propertyName = propertyName;
            this.columnIndex = columnIndex;
            this.fetchValue = fetchValue;
        }

        ReadyMapper prepare(final Connection target) {
            return new ReadyMapper(this, fetchValue.prepare(target));
        }
    }

    private static class ReadyMapper {
        final SQLBiFunction<ResultSet, Integer, ?> reader;
        final PropertyMapper parent;

        public ReadyMapper(PropertyMapper parent, SQLBiFunction<ResultSet, Integer, ?> reader) {
            this.reader = reader;
            this.parent = parent;
        }

        private void read(ResultSet cursor, Feature target) throws SQLException {
            final Object value = reader.apply(cursor, parent.columnIndex);
            if (value != null) target.setPropertyValue(parent.propertyName, value);
        }
    }
}
