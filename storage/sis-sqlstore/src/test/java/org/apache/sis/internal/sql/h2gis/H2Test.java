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
package org.apache.sis.internal.sql.h2gis;

import java.util.stream.Stream;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Optional dependencies
import org.locationtech.jts.geom.Geometry;

// Branch-dependent imports
import org.opengis.feature.Feature;


/**
 * Tests using H2 GIS.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class H2Test extends TestCase {
    /**
     * Number of feature instances found.
     * This is used for making sure that the feature stream was not empty.
     *
     * @see #validate(Feature)
     */
    private int featureCount;

    /**
     * Tests reading features.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testSpatialFeatures() throws Exception {
        try (TestDatabase database = TestDatabase.createOnH2("SpatialFeatures")) {
            database.executeSQL(H2Test.class, "file:SpatialFeatures.sql");
            final StorageConnector connector = new StorageConnector(database.source);
            connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.JTS);
            final ResourceDefinition table = ResourceDefinition.table(null, null, "SpatialData");
            try (SQLStore store = new SQLStore(new SQLStoreProvider(), connector, table)) {
                final FeatureSet resource = store.findResource("SpatialData");
                try (Stream<Feature> features = resource.features(false)) {
                    features.forEach(this::validate);
                    assertEquals("featureCount", 3, featureCount);
                }
            }
        }
    }

    /**
     * Invoked for each feature instances for performing some checks on the feature.
     * This method performs only a superficial verification of geometries.
     */
    private void validate(final Feature feature) {
        featureCount++;
        final Geometry geometry = (Geometry) feature.getPropertyValue("geometry");
        // TODO: verify geometries.
    }
}
