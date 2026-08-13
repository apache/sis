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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.referencing.CRS;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.gml.GeometryAssert.assertCRS;
import static org.apache.sis.gml.GeometryAssert.assertGeometryEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Tests the {@link JAXBGeometryAdapter} class using a minimal JAXB-annotated test class.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class JAXBGeometryAdapterTest {
    /**
     * The CRS attached to the geometries used in this test.
     */
    private final CoordinateReferenceSystem wgs84;

    /**
     * Creates a new test case.
     */
    public JAXBGeometryAdapterTest() throws Exception {
        wgs84 = CRS.forCode("EPSG:4326");
    }

    /**
     * A minimal JAXB-annotated class with an {@link org.apache.sis.geometries.Geometry}
     * property bound through {@link JAXBGeometryAdapter}, used only for testing purpose.
     */
    @XmlRootElement(name = "feature")
    public static final class TestFeature {
        /**
         * A simple scalar attribute, unrelated to the adapter, kept alongside
         * the geometry to verify that the rest of the object marshals normally.
         */
        @XmlAttribute
        public String name;

        /**
         * The geometry property. {@code @XmlAnyElement} (not {@code @XmlElement}) is used
         * because, once {@code @XmlJavaTypeAdapter} is applied, JAXB evaluates it against
         * the adapter value type ({@code org.w3c.dom.Element}) — a GML element is inserted
         * or read as-is, without a synthetic wrapper element.
         */
        @XmlAnyElement
        @XmlJavaTypeAdapter(JAXBGeometryAdapter.class)
        public Geometry geometry;

        /**
         * Creates a feature with no properties set. Required by JAXB.
         */
        public TestFeature() {
        }

        /**
         * Creates a feature with the given properties.
         */
        TestFeature(final String name, final Geometry geometry) {
            this.name = name;
            this.geometry = geometry;
        }
    }

    /**
     * Marshals the given feature to a XML string.
     */
    private static String marshal(final TestFeature feature) throws Exception {
        final JAXBContext context = JAXBContext.newInstance(TestFeature.class);
        final Marshaller marshaller = context.createMarshaller();
        final StringWriter out = new StringWriter();
        marshaller.marshal(feature, out);
        return out.toString();
    }

    /**
     * Unmarshals the given XML string to a feature.
     */
    private static TestFeature unmarshal(final String xml) throws Exception {
        final JAXBContext context = JAXBContext.newInstance(TestFeature.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return (TestFeature) unmarshaller.unmarshal(new StringReader(xml));
    }

    /**
     * Creates a point sequence in the {@link #wgs84} CRS from a flat list of ordinates.
     */
    private PointSequence sequence(final double... ordinates) {
        return GeometryFactory.createSequence(NDArrays.of(SampleSystem.of(wgs84), ordinates));
    }

    /**
     * Creates a closed rectangular ring in the {@link #wgs84} CRS.
     */
    private LinearRing ring(final double minX, final double minY, final double maxX, final double maxY) {
        return GeometryFactory.createLinearRing(sequence(
                minX, minY,
                maxX, minY,
                maxX, maxY,
                minX, maxY,
                minX, minY));
    }

    /**
     * Tests marshalling then unmarshalling a feature with a {@link Point} geometry.
     */
    @Test
    @Disabled("TODO missing jaxb implementation")
    public void testPoint() throws Exception {
        final Point expected = GeometryFactory.createPoint(sequence(10.0, 20.0));

        final String xml = marshal(new TestFeature("A", expected));
        assertTrue(xml.toLowerCase().contains("point"), () -> "Expected a GML Point element in: " + xml);

        final TestFeature result = unmarshal(xml);
        assertEquals("A", result.name);
        assertInstanceOf(Point.class, result.geometry);
        assertGeometryEquals(expected, result.geometry);
        assertCRS(wgs84, result.geometry);
    }

    /**
     * Tests marshalling then unmarshalling a feature with a {@link Polygon} geometry (with a hole).
     */
    @Test
    @Disabled("TODO missing jaxb implementation")
    public void testPolygon() throws Exception {
        final Polygon expected = GeometryFactory.createPolygon(ring(0, 0, 10, 10), List.of(ring(2, 2, 4, 4)));

        final String xml = marshal(new TestFeature("B", expected));
        assertTrue(xml.toLowerCase().contains("polygon"), () -> "Expected a GML Polygon element in: " + xml);

        final TestFeature result = unmarshal(xml);
        assertEquals("B", result.name);
        assertInstanceOf(Polygon.class, result.geometry);
        assertGeometryEquals(expected, result.geometry);
        assertCRS(wgs84, result.geometry);
    }

    /**
     * Tests that a feature with no geometry marshals and unmarshals without error,
     * with the geometry property remaining {@code null}.
     */
    @Test
    @Disabled("TODO missing jaxb implementation")
    public void testNullGeometry() throws Exception {
        final String xml = marshal(new TestFeature("C", null));
        final TestFeature result = unmarshal(xml);
        assertEquals("C", result.name);
        assertNull(result.geometry);
    }
}
