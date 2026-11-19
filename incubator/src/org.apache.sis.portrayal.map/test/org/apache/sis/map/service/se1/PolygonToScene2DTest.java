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
package org.apache.sis.map.service.se1;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.GraphicsPortrayer;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.MemoryFeatureSet;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.GraphicFill;
import org.apache.sis.style.se1.Mark;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.Stroke;
import org.apache.sis.style.se1.Symbology;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * Tests {@link PolygonToScene2D}, with one test for each {@link PolygonSymbolizer} parameter.
 *
 * <p>Unless otherwise specified, the rendering grid is 100×100 pixels over [-50 … 50]° in CRS:84,
 * so one pixel is one degree. A coordinate (λ,φ) is located at pixel coordinate (λ+50, 50-φ).
 * The default square polygon covers [-20 … 20]°, which is pixels 30 to 69 inclusive in both directions.
 * Assertions are done on pixels with a margin of at least one pixel from the expected edges.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PolygonToScene2DTest {

    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    private final GridGeometry grid;

    private final GeometryFactory GF = new GeometryFactory();

    /**
     * Creates a new test case.
     */
    public PolygonToScene2DTest() {
        final GeneralEnvelope env = new GeneralEnvelope(CRS84);
        env.setRange(0, -50, 50);
        env.setRange(1, -50, 50);
        grid = new GridGeometry(new GridExtent(100, 100), env, GridOrientation.REFLECTION_Y);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Utilities //////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    private Coordinate[] coordinates(double... xy) {
        final Coordinate[] coords = new Coordinate[xy.length / 2];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = new Coordinate(xy[i*2], xy[i*2+1]);
        }
        return coords;
    }

    /**
     * Creates a JTS square ring centered on (cx,cy) with the given half size.
     */
    private LinearRing square(double cx, double cy, double half) {
        return GF.createLinearRing(coordinates(
                cx - half, cy - half,  cx + half, cy - half,  cx + half, cy + half,  cx - half, cy + half,  cx - half, cy - half));
    }

    /**
     * Creates a JTS polygon in the given CRS with the given exterior ring and holes.
     */
    private Geometry polygon(CoordinateReferenceSystem crs, LinearRing exterior, LinearRing... holes) {
        final Geometry geom = GF.createPolygon(exterior, holes);
        geom.setUserData(crs);
        return geom;
    }

    /**
     * The square polygon [-20 … 20]° used by most tests, covering pixels 30 to 69.
     */
    private Geometry square() {
        return polygon(CRS84, square(0, 0, 20));
    }

    /**
     * The square polygon [-20 … 20]° with a hole [-10 … 10]°, which is pixels 40 to 59.
     */
    private Geometry squareWithHole() {
        return polygon(CRS84, square(0, 0, 20), square(0, 0, 10));
    }

    /**
     * Creates a polygon symbolizer with a fill of the given color and no stroke.
     */
    private static PolygonSymbolizer<Feature> fillOnly(Color color) {
        final PolygonSymbolizer<Feature> symbolizer = new PolygonSymbolizer<>(FeatureTypeStyle.FACTORY);
        final Fill<Feature> fill = new Fill<>(FeatureTypeStyle.FACTORY);
        fill.setColor(FF.literal(color));
        symbolizer.setFill(fill);
        symbolizer.setStroke(null);
        return symbolizer;
    }

    /**
     * Creates a polygon symbolizer with a stroke of the given color and width, and no fill.
     */
    private static PolygonSymbolizer<Feature> strokeOnly(Color color, double width) {
        final PolygonSymbolizer<Feature> symbolizer = new PolygonSymbolizer<>(FeatureTypeStyle.FACTORY);
        final Stroke<Feature> stroke = new Stroke<>(FeatureTypeStyle.FACTORY);
        stroke.setColor(FF.literal(color));
        stroke.setWidth(FF.literal(width));
        symbolizer.setStroke(stroke);
        symbolizer.setFill(null);
        return symbolizer;
    }

    /**
     * Creates a map layer with one feature per geometry, rendered with the given symbolizer.
     */
    private static MapLayer layer(PolygonSymbolizer<Feature> symbolizer, Geometry... geometries) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType type = ftb.build();
        final Feature[] features = new Feature[geometries.length];
        for (int i = 0; i < geometries.length; i++) {
            features[i] = type.newInstance();
            features[i].setPropertyValue("geom", geometries[i]);
        }
        return layer(symbolizer, type, features);
    }

    private static MapLayer layer(PolygonSymbolizer<Feature> symbolizer, FeatureType type, Feature... features) {
        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        final Rule<Feature> rule = new Rule<>(FeatureTypeStyle.FACTORY);
        rule.symbolizers().add(symbolizer);
        fts.rules().add(rule);

        final MapLayer layer = new MapLayer();
        layer.setData(new MemoryFeatureSet(null, type, List.of(features)));
        layer.setStyle(style);
        return layer;
    }

    private BufferedImage render(PolygonSymbolizer<Feature> symbolizer, Geometry... geometries) throws RenderingException {
        return render(grid, layer(symbolizer, geometries));
    }

    private static BufferedImage render(GridGeometry grid, MapLayer layer) throws RenderingException {
        return new GraphicsPortrayer().setDomain(grid).portray(layer).getImage();
    }

    private static boolean isPainted(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) != 0;
    }

    private static void assertPainted(BufferedImage image, int x, int y) {
        assertTrue(isPainted(image, x, y), () -> "Pixel (" + x + "," + y + ") should be painted.");
    }

    private static void assertEmpty(BufferedImage image, int x, int y) {
        assertFalse(isPainted(image, x, y), () -> "Pixel (" + x + "," + y + ") should be empty, found "
                + Integer.toHexString(image.getRGB(x, y)));
    }

    private static void assertColor(Color expected, BufferedImage image, int x, int y) {
        assertEquals(Integer.toHexString(expected.getRGB()), Integer.toHexString(image.getRGB(x, y)),
                () -> "Pixel (" + x + "," + y + ")");
    }

    // ////////////////////////////////////////////////////////////////////////
    // PolygonSymbolizer parameters ///////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the symbolizer geometry expression: only the referenced geometry is rendered.
     */
    @Test
    public void testGeometry() throws RenderingException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        ftb.addAttribute(Geometry.class).setName("geom2");
        final FeatureType type = ftb.build();
        final Feature feature = type.newInstance();
        feature.setPropertyValue("geom",  polygon(CRS84, square(-30, 0, 10)));     // Pixels 10 to 29.
        feature.setPropertyValue("geom2", polygon(CRS84, square( 30, 0, 10)));     // Pixels 70 to 89.

        final PolygonSymbolizer<Feature> symbolizer = fillOnly(Color.RED);
        BufferedImage image = render(grid, layer(symbolizer, type, feature));
        assertPainted(image, 20, 50);
        assertEmpty  (image, 80, 50);

        symbolizer.setGeometry(FF.property("geom2"));
        image = render(grid, layer(symbolizer, type, feature));
        assertEmpty  (image, 20, 50);
        assertPainted(image, 80, 50);
    }

    /**
     * Tests the unit of measure, on a grid in metres where one pixel is 10 metres.
     * The displacement of 200 metres moves the polygon by 20 pixels.
     */
    @Test
    public void testUnitOfMeasure() throws RenderingException {
        final CoordinateReferenceSystem utm = CommonCRS.WGS84.universal(0, 3);
        final GeneralEnvelope env = new GeneralEnvelope(utm);
        env.setRange(0, 500000, 501000);
        env.setRange(1, 0, 1000);
        final GridGeometry metric = new GridGeometry(new GridExtent(100, 100), env, GridOrientation.REFLECTION_Y);
        final Geometry geom = polygon(utm, square(500300, 500, 100));      // Pixels 20 to 39 horizontally.

        final PolygonSymbolizer<Feature> symbolizer = fillOnly(Color.RED);
        symbolizer.getDisplacement().setDisplacementX(FF.literal(200.0));
        symbolizer.setUnitOfMeasure(Units.METRE);
        BufferedImage image = render(metric, layer(symbolizer, geom));
        assertEmpty  (image, 30, 50);
        assertPainted(image, 50, 50);       // Pixels 40 to 59.
        assertEmpty  (image, 70, 50);

        symbolizer.setUnitOfMeasure(Units.PIXEL);       // 200 pixels, outside the image.
        image = render(metric, layer(symbolizer, geom));
        assertEmpty(image, 30, 50);
        assertEmpty(image, 50, 50);
    }

    /**
     * Tests the fill color, including the default gray color.
     */
    @Test
    public void testFillColor() throws RenderingException {
        final PolygonSymbolizer<Feature> symbolizer = new PolygonSymbolizer<>(FeatureTypeStyle.FACTORY);
        BufferedImage image = render(symbolizer, square());
        assertColor(Color.GRAY, image, 50, 50);         // Default fill.
        // Default stroke of 1 pixel on the edge at x=30, which may fall on pixel 29 or 30.
        assertTrue(image.getRGB(29, 50) == Color.BLACK.getRGB() || image.getRGB(30, 50) == Color.BLACK.getRGB(),
                "Default black stroke expected on the polygon edge.");
        assertEmpty(image, 25, 50);

        image = render(fillOnly(Color.RED), square());
        assertColor(Color.RED, image, 50, 50);
        assertColor(Color.RED, image, 32, 32);
        assertEmpty(image, 27, 50);
        assertEmpty(image, 50, 72);
    }

    /**
     * Tests the fill opacity.
     */
    @Test
    public void testFillOpacity() throws RenderingException {
        final PolygonSymbolizer<Feature> symbolizer = fillOnly(Color.RED);
        symbolizer.getFill().get().setOpacity(FF.literal(0.5));
        BufferedImage image = render(symbolizer, square());
        final int alpha = image.getRGB(50, 50) >>> 24;
        assertTrue(alpha > 120 && alpha < 136, () -> "Unexpected alpha " + alpha);

        symbolizer.getFill().get().setOpacity(FF.literal(0.0));
        image = render(symbolizer, square());
        assertEmpty(image, 50, 50);
    }

    /**
     * Tests the fill graphic fill: the polygon is filled with a pattern of red squares on transparent background.
     */
    @Test
    public void testFillGraphicFill() throws RenderingException {
        final Mark<Feature> mark = new Mark<>(FeatureTypeStyle.FACTORY);
        mark.setWellKnownName(FF.literal("square"));
        final Fill<Feature> markFill = new Fill<>(FeatureTypeStyle.FACTORY);
        markFill.setColor(FF.literal(Color.RED));
        mark.setFill(markFill);
        mark.setStroke(null);
        final GraphicFill<Feature> gf = new GraphicFill<>(FeatureTypeStyle.FACTORY);
        gf.getGraphic().graphicalSymbols().add(mark);
        gf.getGraphic().setSize(FF.literal(4.0));

        final PolygonSymbolizer<Feature> symbolizer = fillOnly(Color.BLUE);
        symbolizer.getFill().get().setGraphicFill(gf);
        final BufferedImage image = render(symbolizer, square());
        assertColor(Color.RED, image, 35, 35);
        assertColor(Color.RED, image, 50, 50);
        assertColor(Color.RED, image, 65, 65);
        assertEmpty(image, 27, 50);
    }

    /**
     * Tests the fill when it is disabled: only the outline is painted.
     */
    @Test
    public void testNoFill() throws RenderingException {
        final BufferedImage image = render(strokeOnly(Color.BLUE, 4), square());
        assertColor(Color.BLUE, image, 30, 50);
        assertColor(Color.BLUE, image, 50, 70);
        assertEmpty(image, 50, 50);
        assertEmpty(image, 35, 50);
    }

    /**
     * Tests the stroke on all rings, and the absence of stroke.
     */
    @Test
    public void testStroke() throws RenderingException {
        BufferedImage image = render(strokeOnly(Color.BLUE, 4), squareWithHole());
        assertColor(Color.BLUE, image, 30, 50);         // Exterior ring.
        assertColor(Color.BLUE, image, 40, 50);         // Interior ring.
        assertEmpty(image, 35, 50);
        assertEmpty(image, 50, 50);

        // Dashes along the ring, to verify that stroke parameters are honored.
        final PolygonSymbolizer<Feature> dashed = strokeOnly(Color.BLUE, 4);
        dashed.getStroke().get().setLineCap(FF.literal("butt"));
        dashed.getStroke().get().setDashArray(FF.literal(new float[] {10, 10}));
        image = render(dashed, square());
        // First ring segment goes from (30,70) to (70,70) because the JTS ring starts at the lower-left corner.
        assertColor(Color.BLUE, image, 35, 70);
        assertEmpty(image, 45, 70);
        assertColor(Color.BLUE, image, 55, 70);

        // Fill only: no outline.
        image = render(fillOnly(Color.RED), square());
        assertColor(Color.RED, image, 31, 50);
    }

    /**
     * Tests the displacement: positive X to the right, positive Y upward.
     */
    @Test
    public void testDisplacement() throws RenderingException {
        final PolygonSymbolizer<Feature> symbolizer = fillOnly(Color.RED);
        symbolizer.getDisplacement().setDisplacementX(FF.literal(10.0));
        symbolizer.getDisplacement().setDisplacementY(FF.literal(10.0));
        final BufferedImage image = render(symbolizer, square());      // Now pixels 40 to 79 by 20 to 59.
        assertEmpty  (image, 35, 50);
        assertPainted(image, 75, 50);
        assertPainted(image, 50, 25);
        assertEmpty  (image, 50, 65);
    }

    /**
     * Tests the perpendicular offset: positive values make the polygon larger, including smaller holes.
     */
    @Test
    public void testPerpendicularOffset() throws RenderingException {
        final PolygonSymbolizer<Feature> symbolizer = fillOnly(Color.RED);
        symbolizer.setPerpendicularOffset(FF.literal(5.0));
        BufferedImage image = render(symbolizer, squareWithHole());    // Exterior 25 to 74, hole 45 to 54.
        assertPainted(image, 27, 50);
        assertEmpty  (image, 22, 50);
        assertPainted(image, 42, 50);       // Was in the hole before offset.
        assertEmpty  (image, 50, 50);

        symbolizer.setPerpendicularOffset(FF.literal(-5.0));
        image = render(symbolizer, square());                          // 35 to 64.
        assertEmpty  (image, 32, 50);
        assertPainted(image, 37, 50);
        assertPainted(image, 62, 50);
        assertEmpty  (image, 67, 50);

        // The polygon collapses.
        symbolizer.setPerpendicularOffset(FF.literal(-25.0));
        image = render(symbolizer, square());
        assertEmpty(image, 50, 50);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Other behaviors ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests that a linear geometry is closed implicitly and filled.
     */
    @Test
    public void testLineGeometry() throws RenderingException {
        final Geometry line = GF.createLineString(coordinates(-20, -20,  20, -20,  0, 20));
        line.setUserData(CRS84);
        final BufferedImage image = render(fillOnly(Color.RED), line);
        assertColor(Color.RED, image, 50, 60);
        assertEmpty(image, 33, 35);
    }

    /**
     * Tests a point geometry: SE specifies that a small square polygon is rendered.
     * The point (0,0) is at pixel corner (50,50) and the square has the default mark size of 6 pixels.
     */
    @Test
    public void testPointGeometry() throws RenderingException {
        final Geometry point = GF.createPoint(new Coordinate(0, 0));
        point.setUserData(CRS84);
        final BufferedImage image = render(fillOnly(Color.RED), point);
        assertColor(Color.RED, image, 50, 50);
        assertColor(Color.RED, image, 47, 52);
        assertEmpty(image, 54, 50);
        assertEmpty(image, 50, 45);
    }

    /**
     * Tests that rings are stroked as closed paths: the first vertex of a ring gets the line join
     * (bevel by default) like other vertices, instead of two line caps (square by default).
     */
    @Test
    public void testStrokeClosedRing() throws RenderingException {
        final BufferedImage image = render(strokeOnly(Color.BLUE, 10), square());
        // The JTS ring starts at the lower-left corner (30,70). The bevel cuts the outer corner.
        assertEmpty(image, 26, 74);
        assertColor(Color.BLUE, image, 29, 71);
        // Same join at the upper-right corner, which is not the first vertex.
        assertEmpty(image, 74, 26);
        assertColor(Color.BLUE, image, 71, 29);
    }

    /**
     * Tests that intersections take the fill and holes in account.
     */
    @Test
    public void testIntersects() throws RenderingException {
        final MapLayer layer = layer(fillOnly(Color.RED), squareWithHole());
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(33, 48, 4, 4))) {
            assertEquals(1, result.count());        // Inside the fill.
        }
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(48, 48, 4, 4))) {
            assertEquals(0, result.count());        // Inside the hole.
        }
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(5, 5, 4, 4))) {
            assertEquals(0, result.count());        // Outside.
        }
    }

    /**
     * Tests the rendering of a feature holding a SIS curve polygon.
     * The presentation is created directly because the SIS geometry library is not yet
     * registered for the spatial filters used by the portrayer.
     */
    @Test
    public void testSISGeometry() throws RenderingException {
        final var factory = org.apache.sis.geometries.GeometryFactory.DEFAULT;
        // Circle of radius 20 centered on (0,0), which is pixels 30 to 69.
        final var circle = factory.createCircularString(factory.createDataPoints(CRS84, -20.0, 0.0,  20.0, 0.0,  -20.0, 0.0));
        final var geom = factory.createCurvePolygon(circle, List.of());

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Object.class).setName("geom");
        final Feature feature = ftb.build().newInstance();
        feature.setPropertyValue("geom", geom);

        final PolygonSymbolizer<Feature> symbolizer = fillOnly(Color.RED);
        symbolizer.setGeometry(FF.property("geom"));

        final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final Scene2D scene = new Scene2D(grid, image.createGraphics());
        SymbolizerToScene2D.create(scene, symbolizer).paint(new SEPresentation(null, null, feature, symbolizer), (s) -> {});
        assertColor(Color.RED, image, 50, 50);
        assertColor(Color.RED, image, 33, 50);
        assertEmpty(image, 33, 33);         // Outside the circle, inside its bounding box.
        assertEmpty(image, 25, 50);
    }
}
