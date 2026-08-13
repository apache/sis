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
package org.apache.sis.gml;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.referencing.CRS;

// Test dependencies
import org.junit.jupiter.api.Test;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Tests the {@link GMLWriter} version-selecting facade.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GMLWriterTest extends TestCase {
    /**
     * The CRS written as {@code srsName="EPSG:4326"} in all fixture files.
     */
    private final CoordinateReferenceSystem wgs84;

    /**
     * Creates a new test case.
     */
    public GMLWriterTest() throws Exception {
        wgs84 = CRS.forCode("EPSG:4326");
    }

    /**
     * Writes a single point using the facade targeting the given version, and returns
     * the resulting XML document as a string.
     */
    private String write(final GMLVersion version) throws Exception {
        final Point g = GeometryFactory.createPoint(
                GeometryFactory.createSequence(NDArrays.of(SampleSystem.of(wgs84), 10.0, 20.0)));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GMLWriter writer = new GMLWriter(out, version)) {
            writer.writeGeometry(g);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Tests that {@link GMLVersion#V2} delegates to {@link GML2Writer}.
     */
    @Test
    public void testWriteGML2Version() throws Exception {
        assertXmlEquals(TestData.V2.openStream(TestData.POINT), write(GMLVersion.V2), "xmlns:*");
    }

    /**
     * Tests that {@link GMLVersion#V3} delegates to {@link GML3Writer}.
     */
    @Test
    public void testWriteGML3Version() throws Exception {
        assertXmlEquals(TestData.V3.openStream(TestData.POINT), write(GMLVersion.V3), "xmlns:*");
    }
}
