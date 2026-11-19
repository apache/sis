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
import org.apache.sis.style.se1.GraphicStroke;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.Mark;
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
 * Tests {@link LineToScene2D}, with one test for each {@link LineSymbolizer} parameter.
 *
 * <p>Unless otherwise specified, the rendering grid is 100×100 pixels over [-50 … 50]° in CRS:84,
 * so one pixel is one degree. A coordinate (λ,φ) is located at pixel coordinate (λ+50, 50-φ),
 * the pixel (i,j) covering [i … i+1] × [j … j+1].
 * Assertions are done on pixels with a margin of at least one pixel from the expected edges.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LineToScene2DTest {

    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    private final GridGeometry grid;

    private final GeometryFactory GF = new GeometryFactory();

    /**
     * Creates a new test case.
     */
    public LineToScene2DTest() {
        final GeneralEnvelope env = new GeneralEnvelope(CRS84);
        env.setRange(0, -50, 50);
        env.setRange(1, -50, 50);
        grid = new GridGeometry(new GridExtent(100, 100), env, GridOrientation.REFLECTION_Y);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Utilities //////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Creates a JTS line string in the given CRS.
     */
    private Geometry line(CoordinateReferenceSystem crs, double... xy) {
        final Coordinate[] coords = new Coordinate[xy.length / 2];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = new Coordinate(xy[i*2], xy[i*2+1]);
        }
        final Geometry geom = GF.createLineString(coords);
        geom.setUserData(crs);
        return geom;
    }

    /**
     * Creates a JTS line string in CRS:84.
     */
    private Geometry line(double... xy) {
        return line(CRS84, xy);
    }

    /**
     * The horizontal line used by most tests, from pixel (20,50) to pixel (80,50).
     */
    private Geometry horizontalLine() {
        return line(-30, 0, 30, 0);
    }

    /**
     * Creates a JTS linear ring in CRS:84.
     */
    private LinearRing ring(double... xy) {
        final Coordinate[] coords = new Coordinate[xy.length / 2];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = new Coordinate(xy[i*2], xy[i*2+1]);
        }
        return GF.createLinearRing(coords);
    }

    /**
     * Creates a new line symbolizer with a stroke of given width.
     */
    private static LineSymbolizer<Feature> symbolizer(double width) {
        final LineSymbolizer<Feature> symbolizer = new LineSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.getStroke().setWidth(FF.literal(width));
        return symbolizer;
    }

    /**
     * Creates a red square mark of given size, without outline.
     */
    private static Mark<Feature> redSquare() {
        final Mark<Feature> mark = new Mark<>(FeatureTypeStyle.FACTORY);
        mark.setWellKnownName(FF.literal("square"));
        final Fill<Feature> fill = new Fill<>(FeatureTypeStyle.FACTORY);
        fill.setColor(FF.literal(Color.RED));
        mark.setFill(fill);
        mark.setStroke(null);
        return mark;
    }

    /**
     * Creates a graphic stroke made of red squares of given size.
     */
    private static GraphicStroke<Feature> redSquares(double size) {
        final GraphicStroke<Feature> gs = new GraphicStroke<>(FeatureTypeStyle.FACTORY);
        gs.getGraphic().graphicalSymbols().add(redSquare());
        gs.getGraphic().setSize(FF.literal(size));
        return gs;
    }

    /**
     * Creates a map layer with one feature per geometry, rendered with the given symbolizer.
     * Geometries are stored in the "geom" attribute, which is the default geometry.
     */
    private static MapLayer layer(LineSymbolizer<Feature> symbolizer, Geometry... geometries) {
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

    private static MapLayer layer(LineSymbolizer<Feature> symbolizer, FeatureType type, Feature... features) {
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

    /**
     * Renders the given geometries with the given symbolizer on the default grid.
     */
    private BufferedImage render(LineSymbolizer<Feature> symbolizer, Geometry... geometries) throws RenderingException {
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
    // LineSymbolizer parameters //////////////////////////////////////////////
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
        feature.setPropertyValue("geom",  line(-30, 0,  30, 0));     // Pixel row 50.
        feature.setPropertyValue("geom2", line(-30, 20, 30, 20));    // Pixel row 30.

        // Default geometry.
        final LineSymbolizer<Feature> symbolizer = symbolizer(4);
        BufferedImage image = render(grid, layer(symbolizer, type, feature));
        assertPainted(image, 50, 50);
        assertEmpty  (image, 50, 30);

        // Explicit geometry.
        symbolizer.setGeometry(FF.property("geom2"));
        image = render(grid, layer(symbolizer, type, feature));
        assertEmpty  (image, 50, 50);
        assertPainted(image, 50, 30);
    }

    /**
     * Tests the unit of measure, on a grid in metres where one pixel is 10 metres.
     */
    @Test
    public void testUnitOfMeasure() throws RenderingException {
        final CoordinateReferenceSystem utm = CommonCRS.WGS84.universal(0, 3);
        final GeneralEnvelope env = new GeneralEnvelope(utm);
        env.setRange(0, 500000, 501000);
        env.setRange(1, 0, 1000);
        final GridGeometry metric = new GridGeometry(new GridExtent(100, 100), env, GridOrientation.REFLECTION_Y);
        final Geometry geom = line(utm, 500200, 500, 500800, 500);   // Pixel row 50.

        // 50 metres = 5 pixels.
        final LineSymbolizer<Feature> symbolizer = symbolizer(50);
        symbolizer.setUnitOfMeasure(Units.METRE);
        BufferedImage image = render(metric, layer(symbolizer, geom));
        assertPainted(image, 50, 49);
        assertPainted(image, 50, 51);
        assertEmpty  (image, 50, 45);
        assertEmpty  (image, 50, 55);

        // 50 pixels.
        symbolizer.setUnitOfMeasure(Units.PIXEL);
        image = render(metric, layer(symbolizer, geom));
        assertPainted(image, 50, 30);
        assertPainted(image, 50, 70);
        assertEmpty  (image, 50, 20);
        assertEmpty  (image, 50, 80);
    }

    /**
     * Tests the perpendicular offset: positive values to the left of the line direction.
     */
    @Test
    public void testPerpendicularOffset() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(4);
        symbolizer.setPerpendicularOffset(FF.literal(10.0));
        BufferedImage image = render(symbolizer, horizontalLine());      // West to east.
        assertEmpty  (image, 50, 50);
        assertPainted(image, 50, 40);
        assertEmpty  (image, 50, 60);

        symbolizer.setPerpendicularOffset(FF.literal(-10.0));
        image = render(symbolizer, horizontalLine());
        assertEmpty  (image, 50, 50);
        assertEmpty  (image, 50, 40);
        assertPainted(image, 50, 60);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Stroke parameters //////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the stroke color.
     */
    @Test
    public void testStrokeColor() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(5);
        BufferedImage image = render(symbolizer, horizontalLine());
        assertColor(Color.BLACK, image, 50, 50);        // Default color.

        symbolizer.getStroke().setColor(FF.literal(Color.RED));
        image = render(symbolizer, horizontalLine());
        assertColor(Color.RED, image, 50, 50);
        assertEmpty(image, 50, 55);
    }

    /**
     * Tests the stroke opacity.
     */
    @Test
    public void testStrokeOpacity() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(5);
        symbolizer.getStroke().setOpacity(FF.literal(0.5));
        BufferedImage image = render(symbolizer, horizontalLine());
        final int alpha = image.getRGB(50, 50) >>> 24;
        assertTrue(alpha > 120 && alpha < 136, () -> "Unexpected alpha " + alpha);

        symbolizer.getStroke().setOpacity(FF.literal(0.0));
        image = render(symbolizer, horizontalLine());
        assertEmpty(image, 50, 50);
    }

    /**
     * Tests the stroke width.
     */
    @Test
    public void testStrokeWidth() throws RenderingException {
        BufferedImage image = render(symbolizer(9), horizontalLine());
        assertPainted(image, 50, 47);
        assertPainted(image, 50, 53);
        assertEmpty  (image, 50, 44);
        assertEmpty  (image, 50, 56);

        image = render(symbolizer(21), horizontalLine());
        assertPainted(image, 50, 41);
        assertPainted(image, 50, 59);
        assertEmpty  (image, 50, 38);
        assertEmpty  (image, 50, 62);
    }

    /**
     * Tests the stroke line join, on a line turning by 90°.
     * The corner is at pixel (50,50) and the outer side of the turn is toward the lower-right.
     */
    @Test
    public void testStrokeLineJoin() throws RenderingException {
        final Geometry geom = line(-30, 0,  0, 0,  0, 30);
        final LineSymbolizer<Feature> symbolizer = symbolizer(20);
        symbolizer.getStroke().setLineCap(FF.literal("butt"));

        symbolizer.getStroke().setLineJoin(FF.literal("mitre"));
        BufferedImage image = render(symbolizer, geom);
        assertPainted(image, 55, 45);       // Inside the second segment.
        assertPainted(image, 58, 58);       // Outer corner.

        symbolizer.getStroke().setLineJoin(FF.literal("bevel"));
        image = render(symbolizer, geom);
        assertPainted(image, 55, 45);
        assertPainted(image, 53, 53);       // Inside the bevel triangle.
        assertEmpty  (image, 58, 58);

        symbolizer.getStroke().setLineJoin(FF.literal("round"));
        image = render(symbolizer, geom);
        assertPainted(image, 55, 45);
        assertPainted(image, 56, 56);       // Inside the arc of radius 10, outside the bevel triangle.
        assertEmpty  (image, 58, 58);
        BufferedImage bevel = render(symbolizer(20), geom);       // Default join is bevel.
        assertEmpty(bevel, 58, 58);
    }

    /**
     * Tests the stroke line cap, at the east end of the line (pixel 80) with a half width of 5 pixels.
     */
    @Test
    public void testStrokeLineCap() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(10);

        symbolizer.getStroke().setLineCap(FF.literal("butt"));
        BufferedImage image = render(symbolizer, horizontalLine());
        assertPainted(image, 78, 50);
        assertEmpty  (image, 83, 50);
        assertEmpty  (image, 83, 54);

        symbolizer.getStroke().setLineCap(FF.literal("square"));
        image = render(symbolizer, horizontalLine());
        assertPainted(image, 78, 50);
        assertPainted(image, 83, 50);
        assertPainted(image, 83, 53);
        assertEmpty  (image, 87, 50);

        symbolizer.getStroke().setLineCap(FF.literal("round"));
        image = render(symbolizer, horizontalLine());
        assertPainted(image, 78, 50);
        assertPainted(image, 83, 50);
        assertEmpty  (image, 84, 54);       // Outside the half circle, inside the square.
        assertEmpty  (image, 87, 50);
    }

    /**
     * Tests the stroke dash array.
     * The line starts at pixel 20 and the dashes are 10 pixels long separated by 10 pixels.
     */
    @Test
    public void testStrokeDashArray() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(6);
        symbolizer.getStroke().setLineCap(FF.literal("butt"));
        symbolizer.getStroke().setDashArray(FF.literal(new float[] {10, 10}));
        final BufferedImage image = render(symbolizer, horizontalLine());
        assertPainted(image, 25, 50);
        assertEmpty  (image, 35, 50);
        assertPainted(image, 45, 50);
        assertEmpty  (image, 55, 50);
        assertPainted(image, 65, 50);
        assertEmpty  (image, 75, 50);
    }

    /**
     * Tests the stroke dash offset: the dash pattern starts 10 pixels later, so with an empty dash.
     */
    @Test
    public void testStrokeDashOffset() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(6);
        symbolizer.getStroke().setLineCap(FF.literal("butt"));
        symbolizer.getStroke().setDashArray(FF.literal(new float[] {10, 10}));
        symbolizer.getStroke().setDashOffset(FF.literal(10));
        final BufferedImage image = render(symbolizer, horizontalLine());
        assertEmpty  (image, 25, 50);
        assertPainted(image, 35, 50);
        assertEmpty  (image, 45, 50);
        assertPainted(image, 55, 50);
        assertEmpty  (image, 65, 50);
        assertPainted(image, 75, 50);
    }

    /**
     * Tests the stroke graphic fill: the stroke is filled with a pattern of red squares.
     */
    @Test
    public void testStrokeGraphicFill() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(10);
        final GraphicFill<Feature> gf = new GraphicFill<>(FeatureTypeStyle.FACTORY);
        gf.getGraphic().graphicalSymbols().add(redSquare());
        gf.getGraphic().setSize(FF.literal(4.0));
        symbolizer.getStroke().setGraphicFill(gf);
        final BufferedImage image = render(symbolizer, horizontalLine());
        assertColor(Color.RED, image, 30, 50);
        assertColor(Color.RED, image, 50, 48);
        assertColor(Color.RED, image, 70, 52);
        assertEmpty(image, 50, 40);
        assertEmpty(image, 50, 60);
    }

    /**
     * Tests the stroke graphic stroke: squares of 6 pixels repeated along the line without gap.
     * The squares cover rows 47 to 52 inclusive.
     */
    @Test
    public void testStrokeGraphicStroke() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(1);
        symbolizer.getStroke().setGraphicStroke(redSquares(6));
        final BufferedImage image = render(symbolizer, horizontalLine());
        for (int x = 21; x < 80; x++) {
            assertColor(Color.RED, image, x, 50);
        }
        assertColor(Color.RED, image, 50, 48);
        assertColor(Color.RED, image, 50, 52);
        assertEmpty(image, 50, 45);
        assertEmpty(image, 50, 55);
        assertEmpty(image, 15, 50);
        assertEmpty(image, 85, 50);
    }

    /**
     * Tests the graphic stroke initial gap: the first square is centered 15 pixels after the line start,
     * then squares of 6 pixels are repeated every 10 pixels (gap of 4 pixels).
     */
    @Test
    public void testStrokeGraphicStrokeInitialGap() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(1);
        final GraphicStroke<Feature> gs = redSquares(6);
        gs.setGap(FF.literal(4.0));
        gs.setInitialGap(FF.literal(15.0));
        symbolizer.getStroke().setGraphicStroke(gs);
        final BufferedImage image = render(symbolizer, horizontalLine());
        assertEmpty  (image, 20, 50);           // Line start.
        assertEmpty  (image, 25, 50);
        assertColor  (Color.RED, image, 35, 50);
        assertEmpty  (image, 40, 50);
        assertColor  (Color.RED, image, 45, 50);
    }

    /**
     * Tests the graphic stroke gap: squares of 6 pixels repeated every 10 pixels, centered on 20, 30, 40, …
     */
    @Test
    public void testStrokeGraphicStrokeGap() throws RenderingException {
        final LineSymbolizer<Feature> symbolizer = symbolizer(1);
        final GraphicStroke<Feature> gs = redSquares(6);
        gs.setGap(FF.literal(4.0));
        symbolizer.getStroke().setGraphicStroke(gs);
        final BufferedImage image = render(symbolizer, horizontalLine());
        for (int x = 20; x <= 80; x += 10) {
            assertColor(Color.RED, image, x, 50);
            if (x < 80) {
                assertEmpty(image, x + 5, 50);      // Between two squares.
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Other behaviors ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests that the intersection takes the stroke width in account.
     */
    @Test
    public void testIntersects() throws RenderingException {
        final Rectangle mask = new Rectangle(48, 56, 4, 2);
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid)
                .intersects(layer(symbolizer(2), horizontalLine()), mask)) {
            assertEquals(0, result.count());
        }
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid)
                .intersects(layer(symbolizer(20), horizontalLine()), mask)) {
            assertEquals(1, result.count());
        }
        // Graphic stroke.
        final LineSymbolizer<Feature> symbolizer = symbolizer(1);
        symbolizer.getStroke().setGraphicStroke(redSquares(20));
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid)
                .intersects(layer(symbolizer, horizontalLine()), mask)) {
            assertEquals(1, result.count());
        }
    }

    /**
     * Tests the rendering of polygons: both exterior and interior rings are stroked.
     */
    @Test
    public void testPolygonRings() throws RenderingException {
        final Geometry polygon = GF.createPolygon(
                ring(-30, -30,  30, -30,  30, 30,  -30, 30,  -30, -30),
                new LinearRing[] {ring(-10, -10,  10, -10,  10, 10,  -10, 10,  -10, -10)});
        polygon.setUserData(CRS84);
        final BufferedImage image = render(symbolizer(4), polygon);
        assertPainted(image, 20, 50);       // Exterior ring.
        assertPainted(image, 40, 50);       // Interior ring.
        assertEmpty  (image, 30, 50);       // Between rings.
        assertEmpty  (image, 50, 50);       // Inside the hole.
    }

    /**
     * Tests the rendering of a feature holding a SIS geometry instead of a JTS geometry.
     * The presentation is created directly because the SIS geometry library is not yet
     * registered for the spatial filters used by the portrayer.
     */
    @Test
    public void testSISGeometry() throws RenderingException {
        final var factory = org.apache.sis.geometries.GeometryFactory.DEFAULT;
        final var geom = factory.createLineString(factory.createDataPoints(CRS84, -30.0, 0.0, 30.0, 0.0));

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Object.class).setName("geom");
        final Feature feature = ftb.build().newInstance();
        feature.setPropertyValue("geom", geom);

        final LineSymbolizer<Feature> symbolizer = symbolizer(5);
        symbolizer.setGeometry(FF.property("geom"));

        final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final Scene2D scene = new Scene2D(grid, image.createGraphics());
        SymbolizerToScene2D.create(scene, symbolizer).paint(new SEPresentation(null, null, feature, symbolizer), (s) -> {});
        assertPainted(image, 50, 50);
        assertPainted(image, 25, 50);
        assertEmpty  (image, 50, 45);
        assertEmpty  (image, 15, 50);
    }
}
