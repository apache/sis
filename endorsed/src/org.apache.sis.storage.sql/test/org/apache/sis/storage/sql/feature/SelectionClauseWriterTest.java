/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.sql.feature;

import java.util.List;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.Optimization;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.metadata.sql.TestDatabase;
import org.apache.sis.referencing.crs.HardCodedCRS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.SpatialOperator;


/**
 * Tests the formatting of {@link Filter} as a SQL {@code WHERE} statement body.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SelectionClauseWriterTest extends TestCase implements SchemaModifier {
    /**
     * The factory to use for creating the filter objects.
     */
    private final FilterFactory<Feature, Object, ?> FF;

    /**
     * A dummy table for testing purpose.
     */
    private Table table;

    /**
     * Creates a new test.
     */
    public SelectionClauseWriterTest() {
        FF = DefaultFilterFactory.forFeatures();
    }

    /**
     * Tests on Derby.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnDerby() throws Exception {
        try (TestDatabase db = TestDatabase.create("SelectionClause")) {
            db.executeSQL(List.of("CREATE TABLE TEST (ALPHA INTEGER, BETA INTEGER, GAMMA INTEGER, PI FLOAT);"));
            final var connector = new StorageConnector(db.source);
            connector.setOption(SchemaModifier.OPTION_KEY, this);
            try (DataStore store = new SQLStoreProvider().open(connector)) {
                table = (Table) store.findResource("TEST");
                testSimpleFilter();
                testGeometricFilter();
                testGeometricFilterWithTransform();
            }
        }
    }

    /**
     * Creates a filter without geometry and verifies that it is translated to the expected SQL fragment.
     */
    private void testSimpleFilter() {
        final Filter<Feature> filter = FF.and(
                FF.greater(FF.property("ALPHA"), FF.property("BETA")),
                FF.or(FF.isNull(FF.property("GAMMA")),
                      FF.equal(FF.literal(3.14), FF.property("PI"))));

        verifySQL(filter, "((\"ALPHA\" > \"BETA\") AND (\"GAMMA\" IS NULL OR (3.14 = \"PI\")))");
    }

    /**
     * Creates a filter with a geometric objects and verifies that it is translated to the expected SQL fragment.
     */
    private void testGeometricFilter() {
        final SpatialOperator<Feature> filter = FF.intersects(
                FF.property("ALPHA"),
                FF.literal(new GeneralEnvelope(new double[] {-12.3, 43.3}, new double[] {2.1, 51.7})));

        verifySQL(filter, "ST_Intersects(\"ALPHA\", " +
                "ST_GeomFromText('POLYGON ((-12.3 43.3, -12.3 51.7, 2.1 51.7, 2.1 43.3, -12.3 43.3))'))");
    }

    /**
     * Invoked when the feature type interred from the test database is created.
     * This method add a CRS on a property for testing purpose.
     */
    @Override
    public FeatureType editFeatureType(final TableReference table, final FeatureTypeBuilder feature) {
        assertEquals("",     table.catalog);
        assertEquals("APP",  table.schema);
        assertEquals("TEST", table.table);
        ((AttributeTypeBuilder<?>) feature.getProperty("BETA")).setCRS(HardCodedCRS.WGS84);
        return feature.build();
    }

    /**
     * Verifies that a spatial operator transforms literal value before-hand if possible.
     */
    private void testGeometricFilterWithTransform() {
        final var bbox = new GeneralEnvelope(HardCodedCRS.WGS84_LATITUDE_FIRST);
        bbox.setEnvelope(-10, 20, -5, 25);

        Filter<Feature> filter = FF.intersects(FF.property("BETA"), FF.literal(bbox));
        final var optimization = new Optimization();
        optimization.setFinalFeatureType(table.featureType);
        verifySQL(optimization.apply(filter), "ST_Intersects(\"BETA\", " +
                "ST_GeomFromText('POLYGON ((20 -10, 25 -10, 25 -5, 20 -5, 20 -10))'))");
    }

    /**
     * Formats the given filter as a SQL {@code WHERE} statement body
     * and verifies that the result is equal to the expected string.
     */
    private void verifySQL(final Filter<Feature> filter, final String expected) {
        final var sql = new SelectionClause(table);
        assertTrue(sql.tryAppend(SelectionClauseWriter.DEFAULT, filter));
        assertEquals(expected, sql.toString());
    }
}
