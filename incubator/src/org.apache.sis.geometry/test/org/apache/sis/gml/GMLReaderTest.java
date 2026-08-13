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

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.MultiLineString;

import org.apache.sis.storage.DataStoreContentException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.gml.GeometryAssert.assertGeometryEquals;


/**
 * Tests the {@link GMLReader} version-detecting facade.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GMLReaderTest {
    /**
     * Reads the geometry contained in the given test file, using the {@link GMLReader} facade.
     */
    private static Geometry read(final TestData data, final String filename) throws Exception {
        try (InputStream in = data.openStream(filename);
             GMLReader reader = new GMLReader(in))
        {
            return reader.readGeometry();
        }
    }

    /**
     * Reads the geometry contained in the given XML text, using the {@link GMLReader} facade.
     */
    private static Geometry readInline(final String xml) throws Exception {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
             GMLReader reader = new GMLReader(in))
        {
            return reader.readGeometry();
        }
    }

    /**
     * Tests that a GML 2.0-style document (common type, unversioned namespace, legacy
     * {@code gml:coordinates} encoding) is dispatched to a reader that produces the exact
     * same result as {@link GML2Reader} directly.
     */
    @Test
    public void testDispatchToGML2StyleCommonType() throws Exception {
        final Geometry expected;
        try (InputStream in = TestData.V2.openStream(TestData.POINT); GML2Reader reader = new GML2Reader(in)) {
            expected = reader.readGeometry();
        }
        final Geometry actual = read(TestData.V2, TestData.POINT);
        assertGeometryEquals(expected, actual);
        assertEquals(expected.getCoordinateReferenceSystem(), actual.getCoordinateReferenceSystem());
    }

    /**
     * Tests that a GML 3.2 document (versioned namespace) is dispatched to a reader that
     * produces the exact same result as {@link GML3Reader} directly.
     */
    @Test
    public void testDispatchForVersionedNamespace() throws Exception {
        final Geometry expected;
        try (InputStream in = TestData.V3.openStream(TestData.POINT); GML3Reader reader = new GML3Reader(in)) {
            expected = reader.readGeometry();
        }
        final Geometry actual = read(TestData.V3, TestData.POINT);
        assertGeometryEquals(expected, actual);
        assertEquals(expected.getCoordinateReferenceSystem(), actual.getCoordinateReferenceSystem());
    }

    /**
     * Tests that a GML3-only element name ({@code MultiCurve}) under the unversioned namespace
     * is unambiguously dispatched to {@link GML3Reader} (the only reader that understands it).
     */
    @Test
    public void testDispatchForGML3OnlyLocalNameUnderUnversionedNamespace() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:MultiCurve xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:4326\">"
                + "<gml:curveMember><gml:LineString><gml:posList>0.0 0.0 10.0 10.0</gml:posList></gml:LineString></gml:curveMember>"
                + "</gml:MultiCurve>";
        final Geometry g = readInline(xml);
        assertInstanceOf(MultiLineString.class, g);
    }

    /**
     * Tests that the facade preserves the specific rejection message produced by the concrete
     * reader, rather than masking it with a generic error.
     *
     * <p>The construct used here is a {@code gml:Clothoid} curve segment, whose Apache SIS
     * interface exists but has no implementation. {@code gml:Curve} used to serve this purpose,
     * back when no general curve could be read at all.</p>
     */
    @Test
    public void testDispatchPreservesSpecificRejectionMessage() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:Clothoid><gml:refLocation/></gml:Clothoid>"
                + "</gml:segments></gml:Curve>";
        final DataStoreContentException e = assertThrows(DataStoreContentException.class,
                () -> readInline(xml));
        assertTrue(e.getMessage().contains("Clothoid"),
                () -> "Expected message to name the element but got: " + e.getMessage());
    }
}
