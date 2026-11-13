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
package org.apache.sis.storage.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Point;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.storage.geojson.binding.GeoJSONFeature;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Tests {@link GeoJSON}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeoJSONTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parses the given JSON text as a GeoJSON feature.
     */
    private GeoJSONFeature parse(final String json) throws Exception {
        return mapper.readValue(json, GeoJSONFeature.class);
    }

    /**
     * Returns the number of properties which are not SIS conventions.
     */
    private static long countUserProperties(final FeatureType type) {
        return type.getProperties(true).stream().filter((p) -> !AttributeConvention.contains(p.getName())).count();
    }

    /**
     * Tests a feature with a null {@code properties} member, as allowed by RFC 7946 section 3.2.
     *
     * @throws Exception if an error occurred while reading the feature.
     */
    @Test
    public void testNullProperties() throws Exception {
        final GeoJSONFeature json = parse(
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[1,2]},\"properties\":null}");
        assertNull(json.getProperties());

        final Feature feature = GeoJSON.fromGeoJSON(json, null);
        assertInstanceOf(Point.class, feature.getPropertyValue(AttributeConvention.GEOMETRY));
        assertEquals(0, countUserProperties(feature.getType()));

        // Same test with a feature type given by the caller.
        final Feature other = GeoJSON.fromGeoJSON(json, feature.getType());
        assertInstanceOf(Point.class, other.getPropertyValue(AttributeConvention.GEOMETRY));
    }

    /**
     * Tests a feature with a property having a null value.
     *
     * @throws Exception if an error occurred while reading the feature.
     */
    @Test
    public void testNullPropertyValue() throws Exception {
        final GeoJSONFeature json = parse(
                "{\"type\":\"Feature\",\"geometry\":null,\"properties\":{\"a\":null,\"b\":\"text\"}}");

        final Feature feature = GeoJSON.fromGeoJSON(json, null);
        assertNull(feature.getPropertyValue("a"));
        assertEquals("text", feature.getPropertyValue("b"));
        assertEquals(Object.class, ((AttributeType<?>) feature.getType().getProperty("a")).getValueClass());
        assertEquals(String.class, ((AttributeType<?>) feature.getType().getProperty("b")).getValueClass());
        assertEquals(2, countUserProperties(feature.getType()));

        // Same test with a feature type given by the caller.
        final Feature other = GeoJSON.fromGeoJSON(json, feature.getType());
        assertNull(other.getPropertyValue("a"));
        assertEquals("text", other.getPropertyValue("b"));
    }
}
