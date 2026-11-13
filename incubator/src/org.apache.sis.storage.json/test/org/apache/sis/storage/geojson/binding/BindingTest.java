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
package org.apache.sis.storage.geojson.binding;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.json.AbstractBindingTest;


/**
 * Test geojson bindings.
 *
 * @author Johann Sorel (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BindingTest extends AbstractBindingTest {

    /**
     * A mapper which rejects JSON objects having the same member twice.
     */
    private final ObjectMapper strict = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    /**
     * Writes the given object and verifies that the result does not contain duplicated members.
     */
    private void assertNoDuplicatedMember(final Object object) throws IOException {
        final String json = mapper.writeValueAsString(object);
        assertDoesNotThrow(() -> strict.readTree(json), json);
    }

    /**
     * Tests reading and writing a feature.
     * The {@code type} member shall not be collected as an unknown field, and shall be written only once.
     *
     * @throws IOException if an error occurred while reading or writing the JSON.
     */
    @Test
    public void testFeature() throws IOException {
        final GeoJSONPoint point = new GeoJSONPoint();
        point.setCoordinates(List.of(102.0, 0.5));
        final GeoJSONFeature expected = new GeoJSONFeature();
        expected.setGeometry(point);
        expected.setProperties(Map.of("prop0", "value0"));
        compare("geojson_feature.json", expected);

        final GeoJSONFeature feature = mapper.readValue(readResource("geojson_feature.json"), GeoJSONFeature.class);
        assertTrue(feature.otherFields().isEmpty(), "Unexpected unknown fields: " + feature.otherFields());
        assertNoDuplicatedMember(feature);
    }

    /**
     * Tests reading and writing a feature collection.
     * The {@code type} member shall be written only once, both on the collection and on its features.
     *
     * @throws IOException if an error occurred while reading or writing the JSON.
     */
    @Test
    public void testFeatureCollection() throws IOException {
        final GeoJSONFeatureCollection collection = mapper.readValue(
                readResource("geojson_featurecollection.json"), GeoJSONFeatureCollection.class);
        assertTrue(collection.otherFields().isEmpty(), "Unexpected unknown fields: " + collection.otherFields());
        assertEquals(3, collection.getFeatures().size());
        for (final GeoJSONFeature feature : collection.getFeatures()) {
            assertTrue(feature.otherFields().isEmpty(), "Unexpected unknown fields: " + feature.otherFields());
        }
        assertNoDuplicatedMember(collection);
    }

    /**
     * Tests that an object with a {@code type} member different than the expected type is rejected.
     */
    @Test
    public void testWrongType() {
        JsonMappingException e;
        e = assertThrows(JsonMappingException.class, () -> mapper.readValue(
                "{\"type\":\"FeatureCollection\",\"features\":[]}", GeoJSONFeature.class));
        assertTrue(e.getMessage().contains("FeatureCollection"), e.getMessage());

        e = assertThrows(JsonMappingException.class, () -> mapper.readValue(
                "{\"type\":\"Feature\",\"geometry\":null,\"properties\":null}", GeoJSONFeatureCollection.class));
        assertTrue(e.getMessage().contains("Feature"), e.getMessage());

        // A geometry where a feature is expected in the "features" array.
        assertThrows(JsonMappingException.class, () -> mapper.readValue(
                "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Point\",\"coordinates\":[1,2]}]}",
                GeoJSONFeatureCollection.class));
    }
}
