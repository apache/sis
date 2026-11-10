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
package org.apache.sis.style.xml;

import java.util.List;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.DistanceOperator;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.NullOperator;
import org.opengis.filter.ResourceId;
import org.opengis.filter.SpatialOperatorName;


/**
 * Tests the reading and writing of filters and expressions.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class FilterXmlTest extends StyleXmlTestCase {
    /**
     * Creates a new test case for filters.
     */
    public FilterXmlTest() {
    }

    /**
     * Reads a filter from the given test file.
     */
    private static Filter<?> readFilter(final String file) throws Exception {
        return read("filter/" + file, Filter.class);
    }

    /**
     * Writes the given filter in all versions, reads it back and verifies that the result is equal to the original.
     */
    private static void assertRoundTrip(final Filter<?> filter) throws Exception {
        for (final StyleVersion version : StyleVersion.values()) {
            final String xml = write(filter, version);
            assertEquals(filter, parse(xml), xml);
        }
    }

    /**
     * Tests reading and writing comparison operators.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testComparison() throws Exception {
        for (final String file : new String[] {
            "Filter_Comparison_PropertyIsEqualTo.xml",
            "Filter_Comparison_PropertyIsNotEqualTo.xml",
            "Filter_Comparison_PropertyIsLessThan.xml",
            "Filter_Comparison_PropertyIsGreaterThan.xml",
            "Filter_Comparison_PropertyIsLessThanOrEqualTo.xml",
            "Filter_Comparison_PropertyIsGreaterThanOrEqualTo.xml"})
        {
            final Filter<?> filter = readFilter(file);
            assertInstanceOf(BinaryComparisonOperator.class, filter, file);
            assertRoundTrip(filter);
        }
    }

    /**
     * Tests reading and writing the {@code PropertyIsLike} operator in both versions of Filter Encoding.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testLike() throws Exception {
        for (final String file : new String[] {"Filter_Comparison_PropertyIsLike_v100.xml", "Filter_Comparison_PropertyIsLike_v110.xml"}) {
            final LikeOperator<?> filter = assertInstanceOf(LikeOperator.class, readFilter(file), file);
            assertEquals('*', filter.getWildCard(),   file);
            assertEquals('#', filter.getSingleChar(), file);
            assertEquals('!', filter.getEscapeChar(), file);
            assertEquals("LAST_NAME", xpath(filter.getExpressions().get(0)), file);
            assertEquals("JOHN*", value(filter.getExpressions().get(1)), file);
            assertRoundTrip(filter);
        }
        String xml = write(readFilter("Filter_Comparison_PropertyIsLike_v110.xml"), StyleVersion.V1_0_0);
        assertTrue(xml.contains("escape=\"!\""), xml);
        xml = write(readFilter("Filter_Comparison_PropertyIsLike_v100.xml"), StyleVersion.V1_1_0);
        assertTrue(xml.contains("escapeChar=\"!\""), xml);
    }

    /**
     * Tests reading the {@code matchCase} attribute of Filter Encoding 1.1.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testMatchCase() throws Exception {
        final LogicalOperator<?> filter = assertInstanceOf(LogicalOperator.class, readFilter("Filter_Comparison_matchCase.xml"));
        final List<? extends Filter<?>> operands = filter.getOperands();
        assertEquals(3, operands.size());
        assertFalse(assertInstanceOf(BinaryComparisonOperator.class, operands.get(0)).isMatchingCase());
        assertTrue (assertInstanceOf(BinaryComparisonOperator.class, operands.get(1)).isMatchingCase());
        assertFalse(assertInstanceOf(LikeOperator.class, operands.get(2)).isMatchingCase());
        final String xml = write(filter, StyleVersion.V1_1_0);
        assertTrue(xml.contains("matchCase=\"false\""), xml);
        assertEquals(filter, parse(xml));
    }

    /**
     * Tests reading and writing the {@code PropertyIsBetween} and {@code PropertyIsNull} operators.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testBetweenAndNull() throws Exception {
        final var between = assertInstanceOf(BetweenComparisonOperator.class, readFilter("Filter_Comparison_PropertyIsBetween.xml"));
        assertEquals("feature_property_name", xpath(between.getExpression()));
        assertEquals("455", value(between.getLowerBoundary()));
        assertEquals("457", value(between.getUpperBoundary()));
        assertRoundTrip(between);

        final var isNull = assertInstanceOf(NullOperator.class, readFilter("Filter_Comparison_PropertyIsNull.xml"));
        assertEquals("feature_property_name", xpath((Expression<?,?>) isNull.getExpressions().get(0)));
        assertRoundTrip(isNull);
    }

    /**
     * Tests reading and writing logical operators.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testLogical() throws Exception {
        for (final String file : new String[] {"Filter_Logical_And.xml", "Filter_Logical_Or.xml", "Filter_Logical_Not.xml"}) {
            final Filter<?> filter = readFilter(file);
            assertInstanceOf(LogicalOperator.class, filter, file);
            assertRoundTrip(filter);
        }
        final var filter = assertInstanceOf(LogicalOperator.class, readFilter("Filter_Logical_Nested.xml"));
        assertEquals(LogicalOperatorName.OR, filter.getOperatorType());
        final var not = assertInstanceOf(LogicalOperator.class, filter.getOperands().get(1));
        assertEquals(LogicalOperatorName.NOT, not.getOperatorType());
        assertRoundTrip(filter);
    }

    /**
     * Tests reading and writing a list of feature identifiers.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testIdentifiers() throws Exception {
        final var filter = assertInstanceOf(LogicalOperator.class, readFilter("Filter_Id_feature.xml"));
        assertEquals(3, filter.getOperands().size());
        final var id = assertInstanceOf(ResourceId.class, filter.getOperands().get(0));
        assertEquals("TREESA_1M.1234", id.getIdentifier());
        assertRoundTrip(filter);

        final String xml = write(filter, StyleVersion.V1_1_0);
        assertFalse(xml.contains("<ogc:Or>"), xml);         // Identifiers shall be directly in the filter.
    }

    /**
     * Tests reading and writing an arithmetic expression as the root element.
     *
     * @throws Exception if an error occurred while reading or writing an expression.
     */
    @Test
    public void testArithmetic() throws Exception {
        final var expression = read("filter/Expression_Add.xml", Expression.class);
        assertEquals("Add", FilterWriter.functionName(expression));
        final String xml = write(expression, StyleVersion.V1_1_0);
        assertTrue(xml.contains("<ogc:Add "), xml);          // Root element, followed by namespace declarations.
        assertEquals(expression, parse(xml));
    }

    /**
     * Tests reading and writing a spatial operator with a GML polygon.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testIntersects() throws Exception {
        final var filter = assertInstanceOf(BinarySpatialOperator.class, parse(
                "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">\n" +
                "  <ogc:Intersects>\n" +
                "    <ogc:PropertyName>geom</ogc:PropertyName>\n" +
                "    <gml:Polygon srsName=\"EPSG:4326\">\n" +
                "      <gml:exterior><gml:LinearRing>\n" +
                "        <gml:posList>46 11 49 11 49 14 46 14 46 11</gml:posList>\n" +
                "      </gml:LinearRing></gml:exterior>\n" +
                "    </gml:Polygon>\n" +
                "  </ogc:Intersects>\n" +
                "</ogc:Filter>"));
        assertEquals(SpatialOperatorName.INTERSECTS, filter.getOperatorType());
        assertEquals("geom", xpath((Expression<?,?>) filter.getExpressions().get(0)));
        assertInstanceOf(Geometry.class, value((Expression<?,?>) filter.getExpressions().get(1)));

        String xml = write(filter, StyleVersion.V1_1_0);
        assertTrue(xml.contains("xmlns=\"http://www.opengis.net/gml\""), xml);
        assertTrue(xml.contains("<posList"), xml);          // May have a `srsDimension` attribute.
        assertInstanceOf(BinarySpatialOperator.class, parse(xml));

        xml = write(filter, StyleVersion.V1_0_0);
        assertTrue(xml.contains("<coordinates"), xml);
        assertInstanceOf(BinarySpatialOperator.class, parse(xml));
    }

    /**
     * Tests reading and writing a {@code BBOX} operator in both versions.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testBBOX() throws Exception {
        final var filter = assertInstanceOf(BinarySpatialOperator.class, parse(
                "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">\n" +
                "  <ogc:BBOX>\n" +
                "    <ogc:PropertyName>geom</ogc:PropertyName>\n" +
                "    <gml:Envelope srsName=\"EPSG:4326\">\n" +
                "      <gml:lowerCorner>13 31</gml:lowerCorner>\n" +
                "      <gml:upperCorner>35 42</gml:upperCorner>\n" +
                "    </gml:Envelope>\n" +
                "  </ogc:BBOX>\n" +
                "</ogc:Filter>"));
        assertEquals(SpatialOperatorName.BBOX, filter.getOperatorType());
        final var bounds = assertInstanceOf(Envelope.class, value((Expression<?,?>) filter.getExpressions().get(1)));
        assertEquals(13, bounds.getMinimum(0));
        assertEquals(42, bounds.getMaximum(1));

        String xml = write(filter, StyleVersion.V1_1_0);
        assertTrue(xml.contains("<Envelope"), xml);
        assertInstanceOf(BinarySpatialOperator.class, parse(xml));

        xml = write(filter, StyleVersion.V1_0_0);
        assertTrue(xml.contains("<Box"), xml);
        assertInstanceOf(BinarySpatialOperator.class, parse(xml));
    }

    /**
     * Tests reading and writing a distance operator.
     * The geometry uses a projected CRS in metres for avoiding the need to transform it.
     *
     * @throws Exception if an error occurred while reading or writing a filter.
     */
    @Test
    public void testDWithin() throws Exception {
        final var filter = assertInstanceOf(DistanceOperator.class, parse(
                "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">\n" +
                "  <ogc:DWithin>\n" +
                "    <ogc:PropertyName>geom</ogc:PropertyName>\n" +
                "    <gml:Point srsName=\"EPSG:32631\"><gml:pos>500000 4649776</gml:pos></gml:Point>\n" +
                "    <ogc:Distance units=\"km\">2</ogc:Distance>\n" +
                "  </ogc:DWithin>\n" +
                "</ogc:Filter>"));
        assertEquals(DistanceOperatorName.WITHIN, filter.getOperatorType());
        assertEquals(2000, filter.getDistance().to(Units.METRE).getValue().doubleValue(), 1E-9);

        final String xml = write(filter, StyleVersion.V1_1_0);
        assertTrue(xml.contains("<ogc:DWithin>"), xml);
        assertTrue(xml.contains("<ogc:Distance units=\"km\">2</ogc:Distance>"), xml);
        assertInstanceOf(DistanceOperator.class, parse(xml));
    }
}
