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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.style.se1.CoverageStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.PointSymbolizer;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.RasterSymbolizer;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.sld1.CoverageConstraint;
import org.apache.sis.style.sld1.CoverageExtent;
import org.apache.sis.style.sld1.LayerCoverageConstraints;
import org.apache.sis.style.sld1.RangeAxis;
import org.apache.sis.style.sld1.RemoteOWS;
import org.apache.sis.style.sld1.StyledLayerDescriptor;
import org.apache.sis.style.sld1.UserLayer;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link StyleWriter}. Most tests read a test file, write it, read the result and write it again.
 * The two written documents shall be identical.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class StyleWriterTest extends StyleXmlTestCase {
    /**
     * Test files in Symbology Encoding 1.1 or Styled Layer Descriptor 1.1.
     */
    private static final String[] FILES_V110 = {
        "se1/SE_ChannelSelection_ContrastEnhancement.xml",
        "se1/SE_ColorMap_Ramp.xml",
        "se1/SE_coveragestyle_v110.xml",
        "se1/SE_ExternalGraphic_InlineContent.xml",
        "se1/SE_ExternalGraphic_OnlineResource.xml",
        "se1/SE_Fill_GraphicFill.xml",
        "se1/SE_fts_v110.xml",
        "se1/SE_Function_Categorize.xml",
        "se1/SE_lineplacement_v110.xml",
        "se1/SE_Mark_TrueTypeFont.xml",
        "se1/SE_rule_elsefilter_v110.xml",
        "se1/SE_rule_v110.xml",
        "se1/SE_Stroke_GraphicStroke.xml",
        "se1/SE_SvgParameter_Interpolate.xml",
        "se1/SE_symbol_line_v110.xml",
        "se1/SE_symbol_point_v110.xml",
        "se1/SE_symbol_polygon_v110.xml",
        "se1/SE_symbol_raster_v110.xml",
        "se1/SE_symbol_text_v110.xml",
        "sld1/SLD_FeatureTypeConstraint_v110.xml",
        "sld1/SLD_LayerCoverageConstraints_RangeAxis.xml",
        "sld1/SLD_LayerCoverageConstraints_TimePeriod.xml",
        "sld1/SLD_NamedLayer_NamedStyle_v110.xml",
        "sld1/SLD_UserLayer_RemoteOWS_v110.xml",
        "sld1/SLD_UserStyle_v110.xml",
        "sld1/SLD_UseSLDLibrary.xml"
    };

    /**
     * Test files in Styled Layer Descriptor 1.0.
     */
    private static final String[] FILES_V100 = {
        "se1/SE_fts_v100.xml",
        "se1/SE_rule_v100.xml",
        "se1/SE_symbol_line_v100.xml",
        "se1/SE_symbol_point_v100.xml",
        "se1/SE_symbol_polygon_v100.xml",
        "se1/SE_symbol_raster_v100.xml",
        "se1/SE_symbol_text_v100.xml",
        "sld1/SLD_FeatureTypeConstraint_v100.xml",
        "sld1/SLD_NamedLayer_NamedStyle_v100.xml",
        "sld1/SLD_UserLayer_RemoteOWS_v100.xml",
        "sld1/SLD_UserStyle_v100.xml"
    };

    /**
     * Creates a new test case for the writers.
     */
    public StyleWriterTest() {
    }

    /**
     * Reads the given file, writes it in the given version, then reads and writes again.
     * Verifies that the two written documents are identical.
     *
     * @return the first written document.
     */
    private static String assertIdempotent(final String file, final StyleVersion version) throws Exception {
        final String first  = write(read(file, Object.class), version);
        final String second = write(parse(first), version);
        assertEquals(first, second, file);
        return first;
    }

    /**
     * Tests writing all test files of version 1.1 in the same version.
     *
     * @throws Exception if an error occurred while reading or writing a style.
     */
    @Test
    public void testRoundTripVersion11() throws Exception {
        for (final String file : FILES_V110) {
            final String xml = assertIdempotent(file, StyleVersion.V1_1_0);
            assertTrue(xml.contains("xmlns:se=\"http://www.opengis.net/se\""), file);
        }
    }

    /**
     * Tests writing all test files of version 1.0 in the same version.
     *
     * @throws Exception if an error occurred while reading or writing a style.
     */
    @Test
    public void testRoundTripVersion10() throws Exception {
        for (final String file : FILES_V100) {
            final String xml = assertIdempotent(file, StyleVersion.V1_0_0);
            assertFalse(xml.contains("<se:"), file);
        }
    }

    /**
     * Tests the conversion of a point symbolizer from version 1.1 to version 1.0.
     * The anchor point and displacement do not exist in version 1.0.
     *
     * @throws Exception if an error occurred while reading or writing a style.
     */
    @Test
    public void testConversionToVersion10() throws Exception {
        final PointSymbolizer<?> symbolizer = read("se1/SE_symbol_point_v110.xml", PointSymbolizer.class);
        final var out = new ByteArrayOutputStream();
        try (StyleWriter writer = new StyleWriter(out, StyleVersion.V1_0_0)) {
            writer.write(symbolizer);
            assertFalse(writer.getWarnings().isEmpty(), "Expected a warning about the anchor point.");
        }
        final String xml = out.toString("UTF-8");
        assertTrue (xml.contains("<sld:CssParameter name=\"stroke\">#404040</sld:CssParameter>"), xml);
        assertFalse(xml.contains("AnchorPoint"), xml);
        final PointSymbolizer<?> converted = assertInstanceOf(PointSymbolizer.class, parse(xml));
        assertEquals(32.0, value(converted.getGraphic().getSize()));
    }

    /**
     * Tests the conversion of a point symbolizer from version 1.0 to version 1.1.
     *
     * @throws Exception if an error occurred while reading or writing a style.
     */
    @Test
    public void testConversionToVersion11() throws Exception {
        final PointSymbolizer<?> symbolizer = read("se1/SE_symbol_point_v100.xml", PointSymbolizer.class);
        final String xml = write(symbolizer, StyleVersion.V1_1_0);
        assertTrue(xml.contains("<se:SvgParameter name=\"stroke\">#404040</se:SvgParameter>"), xml);
        final PointSymbolizer<?> converted = assertInstanceOf(PointSymbolizer.class, parse(xml));
        assertEquals(110.0, value(converted.getGraphic().getRotation()));
    }

    /**
     * Tests writing the functions of Symbology Encoding in both versions.
     * In version 1.0, the functions are written as {@code <ogc:Function>} elements.
     *
     * @throws Exception if an error occurred while reading or writing a style.
     */
    @Test
    public void testFunctions() throws Exception {
        final PolygonSymbolizer<?> symbolizer = read("se1/SE_Function_Categorize.xml", PolygonSymbolizer.class);
        String xml = write(symbolizer, StyleVersion.V1_1_0);
        assertTrue(xml.contains("<se:Categorize fallbackValue=\"#000000\" threshholdsBelongTo=\"preceding\">"), xml);
        assertTrue(xml.contains("<se:Threshold>1000</se:Threshold>"), xml);

        xml = write(symbolizer, StyleVersion.V1_0_0);
        assertTrue(xml.contains("<ogc:Function name=\"Categorize\">"), xml);
        final Fill<?> fill = (Fill<?>) assertInstanceOf(PolygonSymbolizer.class, parse(xml)).getFill().get();
        final Categorize<?,?> function = assertInstanceOf(Categorize.class, fill.getColor());
        assertEquals(Categorize.ThresholdsBelongTo.PRECEDING, function.getThresholdsBelongTo());
        assertEquals(4, function.getValues().size());
    }

    /**
     * Tests writing a color map in both versions.
     *
     * @throws Exception if an error occurred while reading or writing a style.
     */
    @Test
    public void testColorMap() throws Exception {
        final RasterSymbolizer<?> symbolizer = read("se1/SE_symbol_raster_v100.xml", RasterSymbolizer.class);
        String xml = write(symbolizer, StyleVersion.V1_0_0);
        assertTrue(xml.contains("<sld:ColorMapEntry color=\"#454545\" quantity=\"100\"/>"), xml);
        assertTrue(xml.contains("<sld:OverlapBehavior><sld:EARLIEST_ON_TOP/></sld:OverlapBehavior>"), xml);

        xml = write(symbolizer, StyleVersion.V1_1_0);
        assertTrue(xml.contains("<se:Interpolate mode=\"linear\" method=\"color\">"), xml);
        assertTrue(xml.contains("<se:OverlapBehavior>EARLIEST_ON_TOP</se:OverlapBehavior>"), xml);
        final RasterSymbolizer<?> converted = assertInstanceOf(RasterSymbolizer.class, parse(xml));
        final Interpolate<?> function = assertInstanceOf(Interpolate.class,
                converted.getColorMap().orElseThrow().getFunction().orElseThrow());
        /*
         * In version 1.1, the values of functions are read as literal strings (see StyleReaderTest.testInterpolate).
         * The functions decode hexadecimal strings as colors, so this is equivalent to the `Color` read in 1.0.
         */
        assertEquals("#555555", value(function.getInterpolationPoints().get(1).getValue()));
    }

    /**
     * Tests writing a styled layer descriptor built programmatically.
     *
     * @throws Exception if an error occurred while reading or writing a style.
     */
    @Test
    public void testStyledLayerDescriptor() throws Exception {
        final var remote = new RemoteOWS();
        remote.setService("WCS");
        remote.setOnlineResource(new DefaultOnlineResource(URI.create("http://example.org/wcs")));
        final var axis = new RangeAxis();
        axis.setName("Band");
        axis.setValue("band_1");
        final var extent = new CoverageExtent();
        extent.rangeAxes().add(axis);
        final var constraint = new CoverageConstraint();
        constraint.setCoverageName("the_coverage");
        constraint.setCoverageExtent(extent);
        final var constraints = new LayerCoverageConstraints();
        constraints.constraints().add(constraint);
        final var style = new Symbology();
        style.setName("the_style");
        style.setDefault(true);
        final var layer = new UserLayer();
        layer.setName("the_layer");
        layer.setSource(remote);
        layer.setConstraints(constraints);
        layer.styles().add(style);
        final var sld = new StyledLayerDescriptor();
        sld.setName("the_sld");
        sld.layers().add(layer);

        final String xml = write(sld, StyleVersion.V1_1_0);
        assertTrue(xml.contains("<sld:StyledLayerDescriptor xmlns:sld=\"http://www.opengis.net/sld\""), xml);
        assertTrue(xml.contains("version=\"1.1.0\""), xml);
        assertTrue(xml.contains("xlink:href=\"http://example.org/wcs\""), xml);
        assertEquals(sld, parse(xml));

        final var out = new ByteArrayOutputStream();
        try (StyleWriter writer = new StyleWriter(out, StyleVersion.V1_0_0)) {
            writer.write(sld);
            assertFalse(writer.getWarnings().isEmpty(), "Expected a warning about the coverage constraints.");
        }
        assertFalse(out.toString("UTF-8").contains("LayerCoverageConstraints"));
    }

    /**
     * Verifies that coverage styles cannot be written in version 1.0.
     *
     * @throws Exception if an error occurred while reading a style.
     */
    @Test
    public void testCoverageStyleInVersion10() throws Exception {
        final CoverageStyle style = read("se1/SE_coveragestyle_v110.xml", CoverageStyle.class);
        assertThrows(DataStoreContentException.class, () -> write(style, StyleVersion.V1_0_0));
    }
}
