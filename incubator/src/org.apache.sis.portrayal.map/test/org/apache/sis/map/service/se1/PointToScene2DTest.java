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
import org.apache.sis.style.se1.ExternalGraphic;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.Graphic;
import org.apache.sis.style.se1.Mark;
import org.apache.sis.style.se1.PointSymbolizer;
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
 * Tests {@link PointToScene2D}, with one test for each {@link PointSymbolizer} and {@link Graphic} parameter.
 *
 * <p>Unless otherwise specified, the rendering grid is 100×100 pixels over [-50 … 50]° in CRS:84,
 * so one pixel is one degree. A coordinate (λ,φ) is located at pixel coordinate (λ+50, 50-φ).
 * The default point is (0,0), which is the corner between pixels 49 and 50 in both directions.
 * Assertions are done on pixels with a margin of at least one pixel from the expected edges.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PointToScene2DTest {

    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    private final GridGeometry grid;

    private final GeometryFactory GF = new GeometryFactory();

    /**
     * Creates a new test case.
     */
    public PointToScene2DTest() {
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

    private <G extends Geometry> G withCRS(G geom, CoordinateReferenceSystem crs) {
        geom.setUserData(crs);
        return geom;
    }

    private Geometry point(double x, double y) {
        return withCRS(GF.createPoint(new Coordinate(x, y)), CRS84);
    }

    private Geometry origin() {
        return point(0, 0);
    }

    /**
     * Creates a mark with the given well-known name, fill color and outline. Null colors disable fill or outline.
     */
    private static Mark<Feature> mark(String wkn, Color fill, Color outline, double outlineWidth) {
        final Mark<Feature> mark = new Mark<>(FeatureTypeStyle.FACTORY);
        mark.setWellKnownName(FF.literal(wkn));
        if (fill != null) {
            final Fill<Feature> f = new Fill<>(FeatureTypeStyle.FACTORY);
            f.setColor(FF.literal(fill));
            mark.setFill(f);
        } else {
            mark.setFill(null);
        }
        if (outline != null) {
            final Stroke<Feature> s = new Stroke<>(FeatureTypeStyle.FACTORY);
            s.setColor(FF.literal(outline));
            s.setWidth(FF.literal(outlineWidth));
            mark.setStroke(s);
        } else {
            mark.setStroke(null);
        }
        return mark;
    }

    /**
     * Creates a point symbolizer drawing the given mark with the given size.
     */
    private static PointSymbolizer<Feature> symbolizer(Mark<Feature> mark, double size) {
        final PointSymbolizer<Feature> symbolizer = new PointSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.getGraphic().graphicalSymbols().add(mark);
        symbolizer.getGraphic().setSize(FF.literal(size));
        return symbolizer;
    }

    /**
     * Creates a point symbolizer drawing a red square without outline.
     */
    private static PointSymbolizer<Feature> redSquare(double size) {
        return symbolizer(mark("square", Color.RED, null, 0), size);
    }

    private static MapLayer layer(PointSymbolizer<Feature> symbolizer, Geometry... geometries) {
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

    private static MapLayer layer(PointSymbolizer<Feature> symbolizer, FeatureType type, Feature... features) {
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

    private BufferedImage render(PointSymbolizer<Feature> symbolizer, Geometry... geometries) throws RenderingException {
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

    private static void assertAlpha(int expected, BufferedImage image, int x, int y) {
        final int alpha = image.getRGB(x, y) >>> 24;
        assertTrue(Math.abs(alpha - expected) <= 8, () -> "Pixel (" + x + "," + y + "): unexpected alpha " + alpha);
    }

    /**
     * Counts the painted pixels in the given rectangle.
     */
    private static int countPainted(BufferedImage image, Rectangle area) {
        int count = 0;
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                if (isPainted(image, x, y)) count++;
            }
        }
        return count;
    }

    // ////////////////////////////////////////////////////////////////////////
    // PointSymbolizer parameters /////////////////////////////////////////////
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
        feature.setPropertyValue("geom",  point(-20, 0));      // Pixel (30,50).
        feature.setPropertyValue("geom2", point( 20, 0));      // Pixel (70,50).

        final PointSymbolizer<Feature> symbolizer = redSquare(10);
        BufferedImage image = render(grid, layer(symbolizer, type, feature));
        assertPainted(image, 30, 50);
        assertEmpty  (image, 70, 50);

        symbolizer.setGeometry(FF.property("geom2"));
        image = render(grid, layer(symbolizer, type, feature));
        assertEmpty  (image, 30, 50);
        assertPainted(image, 70, 50);
    }

    /**
     * Tests the unit of measure, on a grid in metres where one pixel is 10 metres.
     * The size of 100 metres is 10 pixels.
     */
    @Test
    public void testUnitOfMeasure() throws RenderingException {
        final CoordinateReferenceSystem utm = CommonCRS.WGS84.universal(0, 3);
        final GeneralEnvelope env = new GeneralEnvelope(utm);
        env.setRange(0, 500000, 501000);
        env.setRange(1, 0, 1000);
        final GridGeometry metric = new GridGeometry(new GridExtent(100, 100), env, GridOrientation.REFLECTION_Y);
        final Geometry geom = withCRS(GF.createPoint(new Coordinate(500500, 500)), utm);     // Pixel (50,50).

        final PointSymbolizer<Feature> symbolizer = redSquare(100);
        symbolizer.setUnitOfMeasure(Units.METRE);
        BufferedImage image = render(metric, layer(symbolizer, geom));
        assertPainted(image, 53, 50);
        assertEmpty  (image, 57, 50);

        symbolizer.setUnitOfMeasure(Units.PIXEL);
        image = render(metric, layer(symbolizer, geom));
        assertPainted(image, 57, 50);
        assertPainted(image, 90, 50);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Graphic parameters /////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the default graphic: gray square of 6 pixels with black outline.
     */
    @Test
    public void testDefaultGraphic() throws RenderingException {
        final BufferedImage image = render(new PointSymbolizer<>(FeatureTypeStyle.FACTORY), origin());
        assertColor(Color.GRAY, image, 50, 50);
        assertEmpty(image, 55, 50);
        assertEmpty(image, 44, 50);
    }

    /**
     * Tests all well-known mark names, with a size of 20 pixels centered on (50,50).
     */
    @Test
    public void testMarkWellKnownName() throws RenderingException {
        BufferedImage image = render(symbolizer(mark("square", Color.RED, null, 0), 20), origin());
        assertPainted(image, 50, 50);
        assertPainted(image, 58, 58);           // Square corner.
        assertEmpty  (image, 61, 50);

        image = render(symbolizer(mark("circle", Color.RED, null, 0), 20), origin());
        assertPainted(image, 50, 50);
        assertPainted(image, 58, 50);
        assertEmpty  (image, 58, 58);           // Outside the circle, inside its bounding square.

        image = render(symbolizer(mark("triangle", Color.RED, null, 0), 20), origin());
        assertPainted(image, 50, 45);           // Below the apex at y=42.5.
        assertEmpty  (image, 43, 44);           // Beside the apex.
        assertPainted(image, 43, 56);           // Near the base at y=57.5.

        image = render(symbolizer(mark("star", Color.RED, null, 0), 20), origin());
        assertPainted(image, 50, 50);
        assertPainted(image, 50, 45);           // Upper branch.
        assertEmpty  (image, 57, 42);           // Between upper and right branches.

        image = render(symbolizer(mark("cross", Color.RED, null, 0), 20), origin());
        assertPainted(image, 50, 57);           // Vertical arm.
        assertPainted(image, 57, 50);           // Horizontal arm.
        assertEmpty  (image, 56, 56);

        image = render(symbolizer(mark("x", Color.RED, null, 0), 20), origin());
        assertPainted(image, 56, 56);           // Diagonal arm.
        assertEmpty  (image, 50, 57);
        assertEmpty  (image, 57, 50);
    }

    /**
     * Tests the mark fill color and opacity.
     */
    @Test
    public void testMarkFill() throws RenderingException {
        final Mark<Feature> mark = mark("square", Color.BLUE, null, 0);
        BufferedImage image = render(symbolizer(mark, 20), origin());
        assertColor(Color.BLUE, image, 50, 50);

        mark.getFill().get().setOpacity(FF.literal(0.5));
        image = render(symbolizer(mark, 20), origin());
        assertAlpha(128, image, 50, 50);
    }

    /**
     * Tests the mark stroke: only the outline is painted when there is no fill.
     */
    @Test
    public void testMarkStroke() throws RenderingException {
        final BufferedImage image = render(symbolizer(mark("square", null, Color.BLUE, 2), 20), origin());
        assertColor(Color.BLUE, image, 50, 40);         // Upper edge at y=40.
        assertColor(Color.BLUE, image, 59, 50);         // Right edge at x=60.
        assertEmpty(image, 50, 50);
        assertEmpty(image, 50, 37);
    }

    /**
     * Tests the graphic opacity.
     */
    @Test
    public void testGraphicOpacity() throws RenderingException {
        final PointSymbolizer<Feature> symbolizer = redSquare(20);
        symbolizer.getGraphic().setOpacity(FF.literal(0.5));
        BufferedImage image = render(symbolizer, origin());
        assertAlpha(128, image, 50, 50);

        symbolizer.getGraphic().setOpacity(FF.literal(0.0));
        image = render(symbolizer, origin());
        assertEmpty(image, 50, 50);
    }

    /**
     * Tests the graphic size.
     */
    @Test
    public void testGraphicSize() throws RenderingException {
        BufferedImage image = render(redSquare(10), origin());          // Pixels 45 to 54.
        assertPainted(image, 53, 50);
        assertEmpty  (image, 57, 50);

        image = render(redSquare(30), origin());                        // Pixels 35 to 64.
        assertPainted(image, 62, 50);
        assertEmpty  (image, 67, 50);
    }

    /**
     * Tests the graphic rotation, clockwise in degrees around the center.
     */
    @Test
    public void testGraphicRotation() throws RenderingException {
        PointSymbolizer<Feature> symbolizer = redSquare(20);
        symbolizer.getGraphic().setRotation(FF.literal(45.0));
        BufferedImage image = render(symbolizer, origin());             // Diamond with half diagonal 14.1.
        assertEmpty  (image, 58, 58);       // Was the square corner.
        assertPainted(image, 50, 62);       // Lower diamond corner, outside the unrotated square.

        // Triangle with apex up, rotated clockwise by 90°: apex toward the right.
        symbolizer = symbolizer(mark("triangle", Color.RED, null, 0), 20);
        symbolizer.getGraphic().setRotation(FF.literal(90.0));
        image = render(symbolizer, origin());
        assertPainted(image, 54, 50);       // Toward the apex.
        assertPainted(image, 44, 43);       // Near the base, which is now on the left.
        assertEmpty  (image, 56, 43);       // Would be painted for a counter-clockwise rotation.
    }

    /**
     * Tests the graphic anchor point: (0,0) is the lower-left corner of the graphic, (1,1) the upper-right one.
     */
    @Test
    public void testGraphicAnchorPoint() throws RenderingException {
        final PointSymbolizer<Feature> symbolizer = redSquare(10);
        symbolizer.getGraphic().getAnchorPoint().setAnchorPointX(FF.literal(0.0));
        symbolizer.getGraphic().getAnchorPoint().setAnchorPointY(FF.literal(0.0));
        BufferedImage image = render(symbolizer, origin());             // Pixels 50 to 59 by 40 to 49.
        assertPainted(image, 55, 45);
        assertEmpty  (image, 45, 55);

        symbolizer.getGraphic().getAnchorPoint().setAnchorPointX(FF.literal(1.0));
        symbolizer.getGraphic().getAnchorPoint().setAnchorPointY(FF.literal(1.0));
        image = render(symbolizer, origin());                           // Pixels 40 to 49 by 50 to 59.
        assertEmpty  (image, 55, 45);
        assertPainted(image, 45, 55);
    }

    /**
     * Tests the graphic displacement: positive X to the right, positive Y upward, not rotated with the graphic.
     */
    @Test
    public void testGraphicDisplacement() throws RenderingException {
        final PointSymbolizer<Feature> symbolizer = redSquare(10);
        symbolizer.getGraphic().getDisplacement().setDisplacementX(FF.literal(20.0));
        symbolizer.getGraphic().getDisplacement().setDisplacementY(FF.literal(20.0));
        symbolizer.getGraphic().setRotation(FF.literal(90.0));
        final BufferedImage image = render(symbolizer, origin());       // Centered on (70,30).
        assertEmpty  (image, 50, 50);
        assertPainted(image, 70, 30);
        assertEmpty  (image, 70, 70);       // Where a displacement rotated with the graphic would put it.
    }

    // ////////////////////////////////////////////////////////////////////////
    // Other behaviors ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests that each point of a multi-point gets a graphic.
     */
    @Test
    public void testMultiPoint() throws RenderingException {
        final Geometry geom = withCRS(GF.createMultiPointFromCoords(coordinates(-30, 0,  0, 0,  30, 0)), CRS84);
        final BufferedImage image = render(redSquare(6), geom);
        assertPainted(image, 20, 50);
        assertPainted(image, 50, 50);
        assertPainted(image, 80, 50);
        assertEmpty  (image, 35, 50);
        assertEmpty  (image, 65, 50);
    }

    /**
     * Tests that a polygon gets a single graphic on a point inside it.
     */
    @Test
    public void testPolygon() throws RenderingException {
        // L-shaped polygon whose centroid is outside the polygon.
        final Geometry polygon = withCRS(GF.createPolygon(coordinates(
                -40, -40,  40, -40,  40, -30,  -30, -30,  -30, 40,  -40, 40,  -40, -40)), CRS84);
        final BufferedImage image = render(redSquare(4), polygon);
        final int inside = countPainted(image, new Rectangle(10, 80, 80, 10))
                         + countPainted(image, new Rectangle(10, 10, 10, 70));
        assertTrue(inside > 0, "Graphic expected inside the polygon.");
        assertEquals(inside, countPainted(image, new Rectangle(0, 0, 100, 100)), "Graphic expected only inside the polygon.");
        assertTrue(inside <= 16, "Expected a single graphic.");
    }

    /**
     * Tests that a line gets a single graphic in its middle.
     */
    @Test
    public void testLine() throws RenderingException {
        final Geometry line = withCRS(GF.createLineString(coordinates(-30, 0,  30, 0)), CRS84);
        final BufferedImage image = render(redSquare(6), line);
        assertPainted(image, 50, 50);
        assertEmpty  (image, 20, 50);
        assertEmpty  (image, 80, 50);
    }

    /**
     * Tests that external graphics, which are not supported yet, are ignored without error.
     */
    @Test
    public void testExternalGraphicIgnored() throws RenderingException {
        final PointSymbolizer<Feature> symbolizer = new PointSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.getGraphic().graphicalSymbols().add(new ExternalGraphic<>(FeatureTypeStyle.FACTORY));
        final BufferedImage image = render(symbolizer, origin());
        assertEquals(0, countPainted(image, new Rectangle(0, 0, 100, 100)));
    }

    /**
     * Tests the intersection with the graphic bounds.
     */
    @Test
    public void testIntersects() throws RenderingException {
        final MapLayer layer = layer(redSquare(20), origin());     // Pixels 40 to 59.
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(56, 56, 2, 2))) {
            assertEquals(1, result.count());
        }
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(62, 62, 2, 2))) {
            assertEquals(0, result.count());
        }
    }

    /**
     * Tests the rendering of a feature holding a SIS point.
     * The presentation is created directly because the SIS geometry library is not yet
     * registered for the spatial filters used by the portrayer.
     */
    @Test
    public void testSISGeometry() throws RenderingException {
        final var geom = org.apache.sis.geometries.GeometryFactory.DEFAULT.createPoint(CRS84, 20.0, 20.0);   // Pixel (70,30).

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Object.class).setName("geom");
        final Feature feature = ftb.build().newInstance();
        feature.setPropertyValue("geom", geom);

        final PointSymbolizer<Feature> symbolizer = redSquare(10);
        symbolizer.setGeometry(FF.property("geom"));

        final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final Scene2D scene = new Scene2D(grid, image.createGraphics());
        SymbolizerToScene2D.create(scene, symbolizer).paint(new SEPresentation(null, null, feature, symbolizer), (s) -> {});
        assertColor(Color.RED, image, 70, 30);
        assertEmpty(image, 50, 50);
    }
}
