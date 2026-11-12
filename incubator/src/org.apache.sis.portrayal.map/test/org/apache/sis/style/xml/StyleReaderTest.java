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

import java.awt.Color;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.sis.measure.Units;
import org.apache.sis.style.se1.ChannelSelection;
import org.apache.sis.style.se1.ContrastEnhancement;
import org.apache.sis.style.se1.ContrastMethod;
import org.apache.sis.style.se1.CoverageStyle;
import org.apache.sis.style.se1.ExternalGraphic;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.Font;
import org.apache.sis.style.se1.Graphic;
import org.apache.sis.style.se1.GraphicStroke;
import org.apache.sis.style.se1.Halo;
import org.apache.sis.style.se1.LinePlacement;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.Mark;
import org.apache.sis.style.se1.OverlapBehavior;
import org.apache.sis.style.se1.PointPlacement;
import org.apache.sis.style.se1.PointSymbolizer;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.RasterSymbolizer;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.SelectedChannel;
import org.apache.sis.style.se1.SemanticType;
import org.apache.sis.style.se1.ShadedRelief;
import org.apache.sis.style.se1.Stroke;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.style.se1.TextSymbolizer;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.se1.function.Recode;
import org.apache.sis.style.sld1.CoverageExtent;
import org.apache.sis.style.sld1.FeatureTypeConstraint;
import org.apache.sis.style.sld1.InlineFeature;
import org.apache.sis.style.sld1.LayerCoverageConstraints;
import org.apache.sis.style.sld1.NamedLayer;
import org.apache.sis.style.sld1.NamedStyle;
import org.apache.sis.style.sld1.RemoteOWS;
import org.apache.sis.style.sld1.StyledLayerDescriptor;
import org.apache.sis.style.sld1.UserLayer;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Filter;


/**
 * Tests {@link StyleReader} on symbology and styled layer descriptor documents.
 * The filters are tested by {@link FilterXmlTest}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class StyleReaderTest extends StyleXmlTestCase {
    /**
     * Creates a new test case.
     */
    public StyleReaderTest() {
    }

    /**
     * Verifies the properties of the mark used in the point symbolizer test files.
     */
    private static void verifyMark(final Graphic<?> graphic) {
        final Mark<?> mark = assertInstanceOf(Mark.class, graphic.graphicalSymbols().get(0));
        assertEquals("square", value(mark.getWellKnownName()));
        assertEquals(new Color(0x808080), value(mark.getFill().orElseThrow().getColor()));
        assertEquals(1.0, value(mark.getFill().orElseThrow().getOpacity()));
        final Stroke<?> stroke = mark.getStroke().orElseThrow();
        assertEquals(new Color(0x404040), value(stroke.getColor()));
        assertEquals(13.0,    value(stroke.getWidth()));
        assertEquals(0.4,     value(stroke.getOpacity()));
        assertEquals("bevel", value(stroke.getLineJoin()));
        assertEquals("butt",  value(stroke.getLineCap()));
        assertEquals(2,       value(stroke.getDashOffset()));     // Rounded from 2.3.
        assertEquals(0.7,     value(graphic.getOpacity()));
        assertEquals(32.0,    value(graphic.getSize()));
        assertEquals(110.0,   value(graphic.getRotation()));
    }

    /**
     * Tests reading a point symbolizer in Symbology Encoding 1.1.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testPointSymbolizer110() throws Exception {
        try (InputStream in = open("se1/SE_symbol_point_v110.xml"); StyleReader reader = new StyleReader(in)) {
            final PointSymbolizer<?> symbolizer = reader.read(PointSymbolizer.class);
            assertEquals(Optional.of(StyleVersion.V1_1_0), reader.getVersion());
            assertEquals(Units.PIXEL, symbolizer.getUnitOfMeasure());
            assertEquals("geom", xpath(symbolizer.getGeometry()));
            final Graphic<?> graphic = symbolizer.getGraphic();
            verifyMark(graphic);
            assertEquals(23.0, value(graphic.getAnchorPoint().getAnchorPointX()));
            assertEquals(12.0, value(graphic.getAnchorPoint().getAnchorPointY()));
            assertEquals(21.0, value(graphic.getDisplacement().getDisplacementX()));
            assertEquals(15.0, value(graphic.getDisplacement().getDisplacementY()));
            assertFalse(reader.getWarnings().isEmpty(), "Expected a warning about the rounded dash offset.");
        }
    }

    /**
     * Tests reading a point symbolizer in Styled Layer Descriptor 1.0.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testPointSymbolizer100() throws Exception {
        try (InputStream in = open("se1/SE_symbol_point_v100.xml"); StyleReader reader = new StyleReader(in)) {
            final PointSymbolizer<?> symbolizer = reader.read(PointSymbolizer.class);
            assertEquals(Optional.of(StyleVersion.V1_0_0), reader.getVersion());
            assertEquals("geom", xpath(symbolizer.getGeometry()));
            verifyMark(symbolizer.getGraphic());
        }
    }

    /**
     * Tests reading a raster symbolizer in Styled Layer Descriptor 1.0,
     * including a color map made of {@code <ColorMapEntry>} elements.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testRasterSymbolizer100() throws Exception {
        final RasterSymbolizer<?> symbolizer = read("se1/SE_symbol_raster_v100.xml", RasterSymbolizer.class);
        assertEquals(0.32, value(symbolizer.getOpacity()));
        final SelectedChannel<?>[] channels = symbolizer.getChannelSelection().orElseThrow().getChannels();
        assertEquals(3, channels.length);
        assertEquals("band_1", value(channels[0].getSourceChannelName()));
        assertEquals("band_2", value(channels[1].getSourceChannelName()));
        assertEquals("band_3", value(channels[2].getSourceChannelName()));
        assertEquals(OverlapBehavior.EARLIEST_ON_TOP, symbolizer.getOverlapBehavior());

        final Interpolate<?> function = assertInstanceOf(Interpolate.class,
                symbolizer.getColorMap().orElseThrow().getFunction().orElseThrow());
        assertEquals(Optional.of(Interpolate.Method.COLOR), function.getMethod());
        final List<? extends Interpolate.InterpolationPoint<?>> points = function.getInterpolationPoints();
        assertEquals(2, points.size());
        assertEquals(100.0, value(points.get(0).getData()));
        assertEquals(200.0, value(points.get(1).getData()));
        assertEquals(new Color(0x454545), value(points.get(0).getValue()));
        assertEquals(new Color(0x555555), value(points.get(1).getValue()));

        final ContrastEnhancement<?> contrast = symbolizer.getContrastEnhancement().orElseThrow();
        assertEquals(ContrastMethod.NORMALIZE, contrast.getMethod());
        assertEquals(3.0, value(contrast.getGammaValue()));
        final ShadedRelief<?> relief = symbolizer.getShadedRelief().orElseThrow();
        assertEquals(Boolean.TRUE, value(relief.isBrightnessOnly()));
        assertEquals(5.0, value(relief.getReliefFactor()));
        assertInstanceOf(LineSymbolizer.class, symbolizer.getImageOutline().orElseThrow());
    }

    /**
     * Tests reading a raster symbolizer in Symbology Encoding 1.1.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testRasterSymbolizer110() throws Exception {
        final RasterSymbolizer<?> symbolizer = read("se1/SE_symbol_raster_v110.xml", RasterSymbolizer.class);
        assertEquals(Units.METRE, symbolizer.getUnitOfMeasure());
        assertEquals(OverlapBehavior.EARLIEST_ON_TOP, symbolizer.getOverlapBehavior());
        final LineSymbolizer<?> outline = assertInstanceOf(LineSymbolizer.class, symbolizer.getImageOutline().orElseThrow());
        assertEquals("the line symbolizer name", outline.getName().orElseThrow());
        assertEquals("Line symbolizer title", outline.getDescription().orElseThrow().getTitle().orElseThrow().toString());
        final Stroke<?> stroke = outline.getStroke();
        assertEquals(new Color(0xFF0000), value(stroke.getColor()));
        assertArrayEquals(new float[] {3, 6}, (float[]) value(stroke.getDashArray().orElseThrow()));
        assertEquals(3.0, value(stroke.getWidth()));
        assertEquals(5.0, value(outline.getPerpendicularOffset()));
    }

    /**
     * Tests reading a text symbolizer in Symbology Encoding 1.1.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testTextSymbolizer110() throws Exception {
        final TextSymbolizer<?> symbolizer = read("se1/SE_symbol_text_v110.xml", TextSymbolizer.class);
        assertEquals(Units.FOOT, symbolizer.getUnitOfMeasure());
        assertEquals("aField", value(symbolizer.getLabel()));
        final Font<?> font = symbolizer.getFont();
        assertEquals(2, font.family().size());
        assertEquals("arial",  value(font.family().get(0)));
        assertEquals("serif",  value(font.family().get(1)));
        assertEquals(17.0,     value(font.getSize()));
        assertEquals("italic", value(font.getStyle()));
        assertEquals("bold",   value(font.getWeight()));
        final PointPlacement<?> placement = assertInstanceOf(PointPlacement.class, symbolizer.getLabelPlacement());
        assertEquals(0.5, value(placement.getAnchorPoint().getAnchorPointX()));
        final Halo<?> halo = symbolizer.getHalo().orElseThrow();
        assertEquals(5.0, value(halo.getRadius()));
        assertEquals(new Color(0xFFAFAF), value(halo.getFill().getColor()));
        assertEquals(0.52, value(halo.getFill().getOpacity()));
        assertEquals(new Color(0xFFC800), value(symbolizer.getFill().getColor()));
    }

    /**
     * Tests reading a {@code <LinePlacement>} with all its properties.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testLinePlacement() throws Exception {
        final TextSymbolizer<?> symbolizer = read("se1/SE_lineplacement_v110.xml", TextSymbolizer.class);
        assertEquals("road_name", xpath(symbolizer.getLabel()));
        final LinePlacement<?> placement = assertInstanceOf(LinePlacement.class, symbolizer.getLabelPlacement());
        assertEquals(4.0,           value(placement.getPerpendicularOffset()));
        assertEquals(Boolean.TRUE,  value(placement.isRepeated()));
        assertEquals(15.0,          value(placement.getInitialGap()));
        assertEquals(50.0,          value(placement.getGap()));
        assertEquals(Boolean.FALSE, value(placement.isAligned()));
        assertEquals(Boolean.TRUE,  value(placement.getGeneralizeLine()));
    }

    /**
     * Tests reading a {@code <Categorize>} function in a fill color.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testCategorize() throws Exception {
        final PolygonSymbolizer<?> symbolizer = read("se1/SE_Function_Categorize.xml", PolygonSymbolizer.class);
        final Fill<?> fill = symbolizer.getFill().orElseThrow();
        assertEquals(0.6, value(fill.getOpacity()));
        final Categorize<?,?> function = assertInstanceOf(Categorize.class, fill.getColor());
        assertEquals("population", xpath(function.getLookupValue()));
        assertEquals(Categorize.ThresholdsBelongTo.PRECEDING, function.getThresholdsBelongTo());
        assertEquals("#000000", value(function.getFallbackValue().orElseThrow()));
        assertEquals(4, function.getValues().size());
        assertEquals(3, function.getThresholds().size());
        assertEquals("#00FF00", value(function.getValues().get(0)));
        assertEquals(1000.0,    value(function.getThresholds().get(0)));
        assertEquals("#FF0000", value(function.getValues().get(3)));
    }

    /**
     * Tests reading an {@code <Interpolate>} function in a fill color.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testInterpolate() throws Exception {
        final PolygonSymbolizer<?> symbolizer = read("se1/SE_SvgParameter_Interpolate.xml", PolygonSymbolizer.class);
        final Interpolate<?> function = assertInstanceOf(Interpolate.class, symbolizer.getFill().orElseThrow().getColor());
        assertEquals("band_1_mean", xpath(function.getLookupValue()));
        assertEquals(Interpolate.Mode.LINEAR, function.getMode());
        assertEquals(Optional.of(Interpolate.Method.COLOR), function.getMethod());
        assertEquals("#FF0000", value(function.getFallbackValue().orElseThrow()));
        final List<? extends Interpolate.InterpolationPoint<?>> points = function.getInterpolationPoints();
        assertEquals(3, points.size());
        assertEquals(128.0, value(points.get(1).getData()));
        assertEquals("#00FF00", value(points.get(1).getValue()));
    }

    /**
     * Tests reading a color map made of {@code <ColorMapEntry>} elements with the {@code type} attribute.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testColorMapRamp() throws Exception {
        final RasterSymbolizer<?> symbolizer = read("se1/SE_ColorMap_Ramp.xml", RasterSymbolizer.class);
        final Interpolate<?> function = assertInstanceOf(Interpolate.class,
                symbolizer.getColorMap().orElseThrow().getFunction().orElseThrow());
        final List<? extends Interpolate.InterpolationPoint<?>> points = function.getInterpolationPoints();
        assertEquals(3, points.size());
        assertEquals(0.017, value(points.get(0).getData()));
        assertEquals(0, ((Color) value(points.get(0).getValue())).getAlpha());
        assertEquals(new Color(0xFDE725), value(points.get(2).getValue()));
    }

    /**
     * Tests reading a channel selection with contrast enhancements.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testChannelSelection() throws Exception {
        final RasterSymbolizer<?> symbolizer = read("se1/SE_ChannelSelection_ContrastEnhancement.xml", RasterSymbolizer.class);
        final ChannelSelection<?> selection = symbolizer.getChannelSelection().orElseThrow();
        final SelectedChannel<?>[] channels = selection.getChannels();
        assertEquals(3, channels.length);
        assertNull(channels[1], "Green channel");
        assertEquals(ContrastMethod.HISTOGRAM, channels[0].getContrastEnhancement().orElseThrow().getMethod());
        final ContrastEnhancement<?> blue = channels[2].getContrastEnhancement().orElseThrow();
        assertEquals(ContrastMethod.NORMALIZE, blue.getMethod());
        assertEquals(2.5, value(blue.getGammaValue()));
    }

    /**
     * Tests reading external graphics, from an online resource and inline.
     *
     * @throws Exception if an error occurred while reading the test files.
     */
    @Test
    public void testExternalGraphic() throws Exception {
        PointSymbolizer<?> symbolizer = read("se1/SE_ExternalGraphic_OnlineResource.xml", PointSymbolizer.class);
        ExternalGraphic<?> graphic = assertInstanceOf(ExternalGraphic.class, symbolizer.getGraphic().graphicalSymbols().get(0));
        assertEquals(URI.create("http://www.opengis.net/images/symbol.png"), graphic.getOnlineResource().orElseThrow().getLinkage());
        assertEquals("image/png", graphic.getFormat().orElseThrow());
        assertEquals(1, graphic.colorReplacements().size());
        final Recode<?> recode = assertInstanceOf(Recode.class, graphic.colorReplacements().get(0).getFunction().orElseThrow());
        assertEquals("#000000", value(recode.getFallbackValue().orElseThrow()));
        assertEquals(1, recode.getMapItems().size());
        assertEquals(1.0, value(recode.getMapItems().get(0).getData()));
        assertEquals("#FF0000", value(recode.getMapItems().get(0).getValue()));

        symbolizer = read("se1/SE_ExternalGraphic_InlineContent.xml", PointSymbolizer.class);
        graphic = assertInstanceOf(ExternalGraphic.class, symbolizer.getGraphic().graphicalSymbols().get(0));
        final InlineImage image = assertInstanceOf(InlineImage.class, graphic.getInlineContent().orElseThrow());
        assertArrayEquals(Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="), image.data);
        assertEquals(1, image.getIconWidth(),  "The inline PNG should be decodable.");
        assertEquals(1, image.getIconHeight(), "The inline PNG should be decodable.");
        assertEquals("image/png", graphic.getFormat().orElseThrow());
    }

    /**
     * Tests reading a stroke drawn with a graphic.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testGraphicStroke() throws Exception {
        final LineSymbolizer<?> symbolizer = read("se1/SE_Stroke_GraphicStroke.xml", LineSymbolizer.class);
        final GraphicStroke<?> stroke = symbolizer.getStroke().getGraphicStroke().orElseThrow();
        assertEquals(2.5,  value(stroke.getInitialGap()));
        assertEquals(10.0, value(stroke.getGap()));
        assertEquals(6.0,  value(stroke.getGraphic().getSize()));
        final Mark<?> mark = assertInstanceOf(Mark.class, stroke.getGraphic().graphicalSymbols().get(0));
        assertEquals("circle", value(mark.getWellKnownName()));
    }

    /**
     * Tests reading a feature type style with a filtered rule and an "else" rule.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testFeatureTypeStyle() throws Exception {
        FeatureTypeStyle style = read("se1/SE_rule_elsefilter_v110.xml", FeatureTypeStyle.class);
        assertEquals("style_with_else", style.getName().orElseThrow());
        assertEquals("the_feature_type_name", style.getFeatureTypeName().orElseThrow().toString());
        assertEquals(Set.of(SemanticType.POLYGON), style.semanticTypeIdentifiers());
        assertEquals(2, style.rules().size());
        final Rule<?> matched = style.rules().get(0);
        assertEquals("matched_rule", matched.getName().orElseThrow());
        final var filter = assertInstanceOf(BinaryComparisonOperator.class, matched.getFilter());
        assertEquals(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN, filter.getOperatorType());
        assertFalse(matched.isElseFilter());
        assertTrue(style.rules().get(1).isElseFilter());

        style = read("se1/SE_fts_v110.xml", FeatureTypeStyle.class);
        assertEquals("name", style.getName().orElseThrow());
        assertEquals("abstract", style.getDescription().orElseThrow().getAbstract().orElseThrow().toString());
        assertEquals(6, style.semanticTypeIdentifiers().size());
        assertEquals(3, style.rules().size());
    }

    /**
     * Tests reading a rule with all its properties.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testRule() throws Exception {
        final Rule<?> rule = read("se1/SE_rule_v110.xml", Rule.class);
        assertEquals("name", rule.getName().orElseThrow());
        assertEquals("title", rule.getDescription().orElseThrow().getTitle().orElseThrow().toString());
        assertTrue(rule.getLegend().isPresent());
        assertEquals(Filter.include(), rule.getFilter());
        assertEquals(500,  rule.getMinScaleDenominator());
        assertEquals(1000, rule.getMaxScaleDenominator());
        assertEquals(5, rule.symbolizers().size());
    }

    /**
     * Tests reading a coverage style.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testCoverageStyle() throws Exception {
        final CoverageStyle style = read("se1/SE_coveragestyle_v110.xml", CoverageStyle.class);
        assertEquals("coverage_style_name", style.getName().orElseThrow());
        assertEquals("the_coverage_name", style.getFeatureTypeName().orElseThrow().toString());
        assertEquals(Set.of(SemanticType.RASTER), style.semanticTypeIdentifiers());
        final Rule<?> rule = style.rules().get(0);
        assertEquals(1000,   rule.getMinScaleDenominator());
        assertEquals(100000, rule.getMaxScaleDenominator());
        final RasterSymbolizer<?> symbolizer = assertInstanceOf(RasterSymbolizer.class, rule.symbolizers().get(0));
        assertEquals(0.8, value(symbolizer.getOpacity()));
        final SelectedChannel<?>[] channels = symbolizer.getChannelSelection().orElseThrow().getChannels();
        assertEquals(1, channels.length);
        assertEquals("band_1", value(channels[0].getSourceChannelName()));
    }

    /**
     * Tests reading user styles in both versions.
     *
     * @throws Exception if an error occurred while reading the test files.
     */
    @Test
    public void testUserStyle() throws Exception {
        for (final String file : new String[] {"sld1/SLD_UserStyle_v100.xml", "sld1/SLD_UserStyle_v110.xml"}) {
            final Symbology style = read(file, Symbology.class);
            assertEquals("name", style.getName().orElseThrow(), file);
            assertEquals("title", style.getDescription().orElseThrow().getTitle().orElseThrow().toString(), file);
            assertTrue(style.isDefault(), file);
            assertEquals(3, style.featureTypeStyles().size(), file);
        }
    }

    /**
     * Tests reading a user style containing a coverage style, which is not supported.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testUserStyleWithCoverageStyle() throws Exception {
        try (InputStream in = open("sld1/SLD_UserStyle_CoverageStyle.xml"); StyleReader reader = new StyleReader(in)) {
            final Symbology style = reader.read(Symbology.class);
            assertEquals("coverage_user_style", style.getName().orElseThrow());
            assertTrue(style.isDefault());
            assertTrue(style.featureTypeStyles().isEmpty());
            assertEquals(1, reader.getWarnings().size());
        }
    }

    /**
     * Tests reading a named layer referencing a named style.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testNamedLayer() throws Exception {
        final NamedLayer layer = read("sld1/SLD_NamedLayer_NamedStyle_v110.xml", NamedLayer.class);
        assertEquals("named_layer", layer.getName().orElseThrow());
        final NamedStyle style = assertInstanceOf(NamedStyle.class, layer.styles().get(0));
        assertEquals("named_style", style.getName().orElseThrow());
        assertEquals("Named style title", style.getDescription().orElseThrow().getTitle().orElseThrow().toString());
    }

    /**
     * Tests reading feature type constraints in both versions.
     *
     * @throws Exception if an error occurred while reading the test files.
     */
    @Test
    public void testFeatureTypeConstraint() throws Exception {
        for (final String file : new String[] {"sld1/SLD_FeatureTypeConstraint_v100.xml", "sld1/SLD_FeatureTypeConstraint_v110.xml"}) {
            final NamedLayer layer = read(file, NamedLayer.class);
            final FeatureTypeConstraint constraint = layer.getConstraints().orElseThrow().constraints().get(0);
            assertEquals("the_feature_type_name", constraint.getFeatureTypeName().orElseThrow().toString(), file);
            final var filter = assertInstanceOf(BinaryComparisonOperator.class, constraint.getFilter().orElseThrow());
            assertEquals(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, filter.getOperatorType(), file);
            assertEquals("extent_name",  constraint.extents().get(0).getName().orElseThrow(), file);
            assertEquals("extent_value", constraint.extents().get(0).getValue().orElseThrow(), file);
        }
    }

    /**
     * Tests reading coverage constraints.
     *
     * @throws Exception if an error occurred while reading the test files.
     */
    @Test
    public void testCoverageConstraints() throws Exception {
        UserLayer layer = read("sld1/SLD_LayerCoverageConstraints_RangeAxis.xml", UserLayer.class);
        LayerCoverageConstraints constraints = assertInstanceOf(LayerCoverageConstraints.class, layer.getConstraints().orElseThrow());
        assertEquals("the_coverage_name", constraints.constraints().get(0).getCoverageName().orElseThrow());
        CoverageExtent extent = constraints.constraints().get(0).getCoverageExtent().orElseThrow();
        assertEquals(2, extent.rangeAxes().size());
        assertEquals("Band",   extent.rangeAxes().get(0).getName().orElseThrow());
        assertEquals("band_1", extent.rangeAxes().get(0).getValue().orElseThrow());

        layer = read("sld1/SLD_LayerCoverageConstraints_TimePeriod.xml", UserLayer.class);
        constraints = assertInstanceOf(LayerCoverageConstraints.class, layer.getConstraints().orElseThrow());
        extent = constraints.constraints().get(0).getCoverageExtent().orElseThrow();
        assertEquals("2005-05-17T00:00:00Z/2005-05-23T00:00:00Z", extent.getTimePeriod().orElseThrow());
    }

    /**
     * Tests reading user layers with a remote service or inline features.
     *
     * @throws Exception if an error occurred while reading the test files.
     */
    @Test
    public void testUserLayer() throws Exception {
        UserLayer layer = read("sld1/SLD_UserLayer_RemoteOWS_v110.xml", UserLayer.class);
        final RemoteOWS remote = assertInstanceOf(RemoteOWS.class, layer.getSource().orElseThrow());
        assertEquals("WCS", remote.getService().orElseThrow());
        assertEquals(URI.create("http://example.org/coverages"), remote.getOnlineResource().orElseThrow().getLinkage());

        try (InputStream in = open("sld1/SLD_UserLayer_InlineFeature.xml"); StyleReader reader = new StyleReader(in)) {
            layer = reader.read(UserLayer.class);
            assertInstanceOf(InlineFeature.class, layer.getSource().orElseThrow());
            assertEquals(1, reader.getWarnings().size());
        }
    }

    /**
     * Tests reading a styled layer descriptor using an external library.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testStyledLayerDescriptor() throws Exception {
        final StyledLayerDescriptor sld = read("sld1/SLD_UseSLDLibrary.xml", StyledLayerDescriptor.class);
        assertEquals("sld_with_library", sld.getName().orElseThrow());
        assertEquals(1, sld.libraries().size());
        assertEquals(URI.create("http://example.org/aSharedSLDFile.xml"),
                     sld.libraries().get(0).getOnlineResource().orElseThrow().getLinkage());
    }
}
