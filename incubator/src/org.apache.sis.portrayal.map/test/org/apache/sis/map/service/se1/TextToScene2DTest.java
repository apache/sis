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
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.Halo;
import org.apache.sis.style.se1.LinePlacement;
import org.apache.sis.style.se1.PointPlacement;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.style.se1.TextSymbolizer;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * Tests {@link TextToScene2D}, with one test for each {@link TextSymbolizer} parameter.
 *
 * <p>The rendering grid is 200×100 pixels over [-100 … 100]° × [-50 … 50]° in CRS:84,
 * so one pixel is one degree. A coordinate (λ,φ) is located at pixel coordinate (λ+100, 50-φ).
 * Since glyph shapes depend on the installed fonts, assertions are done on the bounds of painted
 * pixels, on counts of painted pixels or on comparisons between images, with tolerances.
 * Labels use the letter "H", which has no descender.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TextToScene2DTest {

    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    private final GridGeometry grid;

    private final GeometryFactory GF = new GeometryFactory();

    /**
     * Creates a new test case.
     */
    public TextToScene2DTest() {
        final GeneralEnvelope env = new GeneralEnvelope(CRS84);
        env.setRange(0, -100, 100);
        env.setRange(1, -50, 50);
        grid = new GridGeometry(new GridExtent(200, 100), env, GridOrientation.REFLECTION_Y);
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

    private Geometry origin() {
        return withCRS(GF.createPoint(new Coordinate(0, 0)), CRS84);       // Pixel corner (100,50).
    }

    private Geometry line(double... xy) {
        return withCRS(GF.createLineString(coordinates(xy)), CRS84);
    }

    /**
     * Creates a text symbolizer with the given label, font size in pixels and a sans-serif font.
     */
    private static TextSymbolizer<Feature> text(String label, double size) {
        final TextSymbolizer<Feature> symbolizer = new TextSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.setLabel(FF.literal(label));
        symbolizer.getFont().family().add(FF.literal("sans-serif"));
        symbolizer.getFont().setSize(FF.literal(size));
        return symbolizer;
    }

    private static PointPlacement<Feature> pointPlacement(TextSymbolizer<Feature> symbolizer) {
        final PointPlacement<Feature> placement = new PointPlacement<>(FeatureTypeStyle.FACTORY);
        symbolizer.setLabelPlacement(placement);
        return placement;
    }

    private static LinePlacement<Feature> linePlacement(TextSymbolizer<Feature> symbolizer) {
        final LinePlacement<Feature> placement = new LinePlacement<>(FeatureTypeStyle.FACTORY);
        symbolizer.setLabelPlacement(placement);
        return placement;
    }

    private static MapLayer layer(TextSymbolizer<Feature> symbolizer, Geometry... geometries) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        ftb.addAttribute(String.class).setName("name");
        final FeatureType type = ftb.build();
        final Feature[] features = new Feature[geometries.length];
        for (int i = 0; i < geometries.length; i++) {
            features[i] = type.newInstance();
            features[i].setPropertyValue("geom", geometries[i]);
            features[i].setPropertyValue("name", "HHH");
        }
        return layer(symbolizer, type, features);
    }

    private static MapLayer layer(TextSymbolizer<Feature> symbolizer, FeatureType type, Feature... features) {
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

    private BufferedImage render(TextSymbolizer<Feature> symbolizer, Geometry... geometries) throws RenderingException {
        return render(grid, layer(symbolizer, geometries));
    }

    private static BufferedImage render(GridGeometry grid, MapLayer layer) throws RenderingException {
        return new GraphicsPortrayer().setDomain(grid).portray(layer).getImage();
    }

    private static boolean isPainted(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) != 0;
    }

    /**
     * Returns the bounds of painted pixels, or {@code null} if nothing is painted.
     */
    private static Rectangle paintedBounds(BufferedImage image) {
        Rectangle bounds = null;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isPainted(image, x, y)) {
                    if (bounds == null) bounds = new Rectangle(x, y, 1, 1);
                    else bounds.add(new Rectangle(x, y, 1, 1));
                }
            }
        }
        return bounds;
    }

    private static Rectangle assertPaintedBounds(BufferedImage image) {
        final Rectangle bounds = paintedBounds(image);
        assertNotNull(bounds, "Something should be painted.");
        return bounds;
    }

    /**
     * Counts the painted pixels, or the pixels of the given color if non-null.
     */
    private static int count(BufferedImage image, Color color) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (color != null ? image.getRGB(x, y) == color.getRGB() : isPainted(image, x, y)) count++;
            }
        }
        return count;
    }

    private static boolean sameImage(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
            }
        }
        return true;
    }

    private static boolean columnPainted(BufferedImage image, int x) {
        for (int y = 0; y < image.getHeight(); y++) {
            if (isPainted(image, x, y)) return true;
        }
        return false;
    }

    // ////////////////////////////////////////////////////////////////////////
    // TextSymbolizer parameters //////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the label expression: literal, from an attribute, null or blank.
     */
    @Test
    public void testLabel() throws RenderingException {
        Rectangle bounds = assertPaintedBounds(render(text("HHH", 20), origin()));
        assertTrue(bounds.contains(100, 50), "Label expected centered on the point, found " + bounds);

        final TextSymbolizer<Feature> symbolizer = text("ignored", 20);
        symbolizer.setLabel(FF.property("name", String.class));
        bounds = assertPaintedBounds(render(symbolizer, origin()));
        assertTrue(bounds.contains(100, 50));

        symbolizer.setLabel(null);
        assertNull(paintedBounds(render(symbolizer, origin())));
        assertNull(paintedBounds(render(text("   ", 20), origin())));
    }

    /**
     * Tests the symbolizer geometry expression: only the referenced geometry is labeled.
     */
    @Test
    public void testGeometry() throws RenderingException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        ftb.addAttribute(Geometry.class).setName("geom2");
        final FeatureType type = ftb.build();
        final Feature feature = type.newInstance();
        feature.setPropertyValue("geom",  withCRS(GF.createPoint(new Coordinate(-50, 0)), CRS84));    // Pixel x=50.
        feature.setPropertyValue("geom2", withCRS(GF.createPoint(new Coordinate( 50, 0)), CRS84));    // Pixel x=150.

        final TextSymbolizer<Feature> symbolizer = text("HH", 20);
        Rectangle bounds = assertPaintedBounds(render(grid, layer(symbolizer, type, feature)));
        assertTrue(bounds.contains(50, 50) && !bounds.contains(150, 50), bounds.toString());

        symbolizer.setGeometry(FF.property("geom2"));
        bounds = assertPaintedBounds(render(grid, layer(symbolizer, type, feature)));
        assertTrue(!bounds.contains(50, 50) && bounds.contains(150, 50), bounds.toString());
    }

    /**
     * Tests the unit of measure: a font size of 200 metres on a grid of 10 metres per pixel
     * is rendered like a font size of 20 pixels.
     */
    @Test
    public void testUnitOfMeasure() throws RenderingException {
        final CoordinateReferenceSystem utm = CommonCRS.WGS84.universal(0, 3);
        final GeneralEnvelope env = new GeneralEnvelope(utm);
        env.setRange(0, 500000, 502000);
        env.setRange(1, 0, 1000);
        final GridGeometry metric = new GridGeometry(new GridExtent(200, 100), env, GridOrientation.REFLECTION_Y);
        final Geometry geom = withCRS(GF.createPoint(new Coordinate(501000, 500)), utm);      // Pixel corner (100,50).

        final TextSymbolizer<Feature> symbolizer = text("HHH", 200);
        symbolizer.setUnitOfMeasure(Units.METRE);
        final BufferedImage inMetres = render(metric, layer(symbolizer, geom));
        final BufferedImage inPixels = render(text("HHH", 20), origin());
        final Rectangle b1 = assertPaintedBounds(inMetres);
        final Rectangle b2 = assertPaintedBounds(inPixels);
        assertEquals(b2.width,  b1.width,  2, "Width");
        assertEquals(b2.height, b1.height, 2, "Height");
    }

    // ////////////////////////////////////////////////////////////////////////
    // Font parameters ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the font size.
     */
    @Test
    public void testFontSize() throws RenderingException {
        final Rectangle small = assertPaintedBounds(render(text("HHH", 10), origin()));
        final Rectangle large = assertPaintedBounds(render(text("HHH", 30), origin()));
        assertEquals(3.0, large.width  / (double) small.width,  0.5, "Width ratio");
        assertEquals(3.0, large.height / (double) small.height, 0.6, "Height ratio");
    }

    /**
     * Tests the font family: the first available family in preference order is used.
     */
    @Test
    public void testFontFamily() throws RenderingException {
        final TextSymbolizer<Feature> mono = text("iiiiii", 20);
        mono.getFont().family().set(0, FF.literal("monospace"));
        final TextSymbolizer<Feature> sans = text("iiiiii", 20);
        final Rectangle monoBounds = assertPaintedBounds(render(mono, origin()));
        final Rectangle sansBounds = assertPaintedBounds(render(sans, origin()));
        assertTrue(monoBounds.width > sansBounds.width + 5, "Monospaced letters 'i' expected wider: "
                + monoBounds + " versus " + sansBounds);

        // Unknown family followed by monospace: monospace is used.
        final TextSymbolizer<Feature> fallback = text("iiiiii", 20);
        fallback.getFont().family().set(0, FF.literal("No such font family"));
        fallback.getFont().family().add(FF.literal("monospace"));
        assertTrue(sameImage(render(mono, origin()), render(fallback, origin())));
    }

    /**
     * Tests the font style: italic glyphs are slanted, so the label is wider.
     */
    @Test
    public void testFontStyle() throws RenderingException {
        final TextSymbolizer<Feature> italic = text("HHH", 30);
        italic.getFont().setStyle(FF.literal("italic"));
        final BufferedImage normal  = render(text("HHH", 30), origin());
        final BufferedImage slanted = render(italic, origin());
        assertFalse(sameImage(normal, slanted));
        // The top of the first letter is shifted to the right.
        final Rectangle n = assertPaintedBounds(normal);
        final Rectangle s = assertPaintedBounds(slanted);
        assertTrue(firstPaintedColumn(slanted, s.y + 1) > firstPaintedColumn(normal, n.y + 1),
                "Italic glyph tops expected to be shifted right.");
    }

    private static int firstPaintedColumn(BufferedImage image, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            if (isPainted(image, x, y)) return x;
        }
        return -1;
    }

    /**
     * Tests the font weight: bold glyphs paint more pixels.
     */
    @Test
    public void testFontWeight() throws RenderingException {
        final TextSymbolizer<Feature> bold = text("HHH", 30);
        bold.getFont().setWeight(FF.literal("bold"));
        final int normalCount = count(render(text("HHH", 30), origin()), null);
        final int boldCount   = count(render(bold, origin()), null);
        assertTrue(boldCount > normalCount * 1.2, "Bold expected to paint more pixels: " + boldCount + " versus " + normalCount);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Fill and halo //////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the text fill: default black, color, opacity and graphic fill.
     */
    @Test
    public void testFill() throws RenderingException {
        BufferedImage image = render(text("HHH", 30), origin());
        assertEquals(count(image, null), count(image, Color.BLACK), "Default fill is black.");

        final TextSymbolizer<Feature> symbolizer = text("HHH", 30);
        final Fill<Feature> fill = new Fill<>(FeatureTypeStyle.FACTORY);
        fill.setColor(FF.literal(Color.RED));
        symbolizer.setFill(fill);
        image = render(symbolizer, origin());
        assertEquals(count(image, null), count(image, Color.RED));

        fill.setOpacity(FF.literal(0.5));
        image = render(symbolizer, origin());
        final Rectangle b = assertPaintedBounds(image);
        boolean found = false;
        for (int x = b.x; x < b.x + b.width && !found; x++) {
            final int argb = image.getRGB(x, b.y + b.height / 2);
            if ((argb >>> 24) != 0) {
                assertEquals(128, argb >>> 24, 8, "Alpha");
                found = true;
            }
        }
        assertTrue(found);
    }

    /**
     * Tests the halo: a white border around glyphs by default, with a radius and a fill.
     */
    @Test
    public void testHalo() throws RenderingException {
        final TextSymbolizer<Feature> symbolizer = text("HHH", 30);
        final Rectangle plain = assertPaintedBounds(render(symbolizer, origin()));

        final Halo<Feature> halo = new Halo<>(FeatureTypeStyle.FACTORY);
        halo.setRadius(FF.literal(4.0));
        symbolizer.setHalo(halo);
        BufferedImage image = render(symbolizer, origin());
        final Rectangle withHalo = assertPaintedBounds(image);
        assertEquals(plain.width  + 8, withHalo.width,  2, "Halo width");
        assertEquals(plain.height + 8, withHalo.height, 2, "Halo height");
        assertTrue(count(image, Color.WHITE) > 0, "Default halo fill is white.");
        assertTrue(count(image, Color.BLACK) > 0, "Text painted over the halo.");

        final Fill<Feature> fill = new Fill<>(FeatureTypeStyle.FACTORY);
        fill.setColor(FF.literal(Color.YELLOW));
        halo.setFill(fill);
        image = render(symbolizer, origin());
        assertEquals(0, count(image, Color.WHITE));
        assertTrue(count(image, Color.YELLOW) > 0);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Point placement ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the point placement anchor point: (0,0) puts the lower-left corner of the label box on the point,
     * (1,1) the upper-right corner.
     */
    @Test
    public void testPointPlacementAnchorPoint() throws RenderingException {
        final TextSymbolizer<Feature> symbolizer = text("HHH", 20);
        final PointPlacement<Feature> pp = pointPlacement(symbolizer);
        pp.getAnchorPoint().setAnchorPointX(FF.literal(0.0));
        pp.getAnchorPoint().setAnchorPointY(FF.literal(0.0));
        Rectangle b = assertPaintedBounds(render(symbolizer, origin()));
        assertEquals(100, b.x, 3, "Label expected to start at the point: " + b);
        assertTrue(b.y + b.height <= 50, "Label expected above the point: " + b);

        pp.getAnchorPoint().setAnchorPointX(FF.literal(1.0));
        pp.getAnchorPoint().setAnchorPointY(FF.literal(1.0));
        b = assertPaintedBounds(render(symbolizer, origin()));
        assertEquals(100, b.x + b.width, 3, "Label expected to end at the point: " + b);
        assertTrue(b.y >= 50, "Label expected below the point: " + b);
    }

    /**
     * Tests the point placement displacement: positive X to the right, positive Y upward.
     */
    @Test
    public void testPointPlacementDisplacement() throws RenderingException {
        final Rectangle centered = assertPaintedBounds(render(text("HHH", 20), origin()));
        final TextSymbolizer<Feature> symbolizer = text("HHH", 20);
        final PointPlacement<Feature> pp = pointPlacement(symbolizer);
        pp.getDisplacement().setDisplacementX(FF.literal(30.0));
        pp.getDisplacement().setDisplacementY(FF.literal(20.0));
        final Rectangle moved = assertPaintedBounds(render(symbolizer, origin()));
        assertEquals(centered.x + 30, moved.x, 1);
        assertEquals(centered.y - 20, moved.y, 1);
    }

    /**
     * Tests the point placement rotation: a label rotated by 90° becomes vertical.
     */
    @Test
    public void testPointPlacementRotation() throws RenderingException {
        final Rectangle horizontal = assertPaintedBounds(render(text("HHHH", 20), origin()));
        assertTrue(horizontal.width > horizontal.height * 2);

        final TextSymbolizer<Feature> symbolizer = text("HHHH", 20);
        pointPlacement(symbolizer).setRotation(FF.literal(90.0));
        final Rectangle vertical = assertPaintedBounds(render(symbolizer, origin()));
        assertEquals(horizontal.width,  vertical.height, 2);
        assertEquals(horizontal.height, vertical.width,  2);
        assertTrue(vertical.contains(100, 50));
    }

    // ////////////////////////////////////////////////////////////////////////
    // Line placement /////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the line placement alignment: labels follow the line direction, or stay horizontal.
     * The diagonal line goes from pixel (60,90) to pixel (140,10).
     */
    @Test
    public void testLinePlacementAligned() throws RenderingException {
        final Geometry diagonal = line(-40, -40,  40, 40);
        final TextSymbolizer<Feature> symbolizer = text("HHHHH", 15);
        final LinePlacement<Feature> lp = linePlacement(symbolizer);
        final Rectangle aligned = assertPaintedBounds(render(symbolizer, diagonal));
        assertTrue(aligned.height > 30, "Label expected along the diagonal: " + aligned);

        lp.setAligned(FF.literal(false));
        final Rectangle horizontal = assertPaintedBounds(render(symbolizer, diagonal));
        assertTrue(horizontal.height < 15, "Label expected horizontal: " + horizontal);
        assertTrue(horizontal.contains(100, 50), "Label expected centered on the line: " + horizontal);
    }

    /**
     * Tests the line placement perpendicular offset: positive values to the left of the line, which is above
     * for a line from west to east.
     */
    @Test
    public void testLinePlacementPerpendicularOffset() throws RenderingException {
        final Geometry horizontal = line(-80, 0,  80, 0);       // Pixel row 50.
        final TextSymbolizer<Feature> symbolizer = text("HHH", 10);
        final LinePlacement<Feature> lp = linePlacement(symbolizer);
        Rectangle b = assertPaintedBounds(render(symbolizer, horizontal));
        assertTrue(b.y < 50 && b.y + b.height > 50, "Label expected centered on the line: " + b);

        lp.setPerpendicularOffset(FF.literal(20.0));
        b = assertPaintedBounds(render(symbolizer, horizontal));
        assertEquals(30, b.y + b.height / 2.0, 2, "Label expected 20 pixels above the line: " + b);

        lp.setPerpendicularOffset(FF.literal(-20.0));
        b = assertPaintedBounds(render(symbolizer, horizontal));
        assertEquals(70, b.y + b.height / 2.0, 2, "Label expected 20 pixels below the line: " + b);
    }

    /**
     * Tests the line placement repetition: a single centered label, or labels repeated along the line.
     */
    @Test
    public void testLinePlacementRepeated() throws RenderingException {
        final Geometry horizontal = line(-90, 0,  90, 0);       // Pixels 10 to 190.
        final TextSymbolizer<Feature> symbolizer = text("HH", 10);
        final LinePlacement<Feature> lp = linePlacement(symbolizer);
        final Rectangle single = assertPaintedBounds(render(symbolizer, horizontal));
        assertEquals(100, single.x + single.width / 2.0, 2, "Single label expected at the middle: " + single);

        lp.setRepeated(FF.literal(true));
        lp.setGap(FF.literal(20.0));
        final BufferedImage image = render(symbolizer, horizontal);
        final Rectangle repeated = assertPaintedBounds(image);
        assertTrue(repeated.x < 20 && repeated.x + repeated.width > 150, "Labels expected along the line: " + repeated);
        assertTrue(count(image, null) > 4 * count(render(text("HH", 10), origin()), null), "Expected many labels.");
    }

    /**
     * Tests the line placement initial gap: the first repeated label starts after the gap.
     */
    @Test
    public void testLinePlacementInitialGap() throws RenderingException {
        final Geometry horizontal = line(-90, 0,  90, 0);       // Pixels 10 to 190.
        final TextSymbolizer<Feature> symbolizer = text("HH", 10);
        final LinePlacement<Feature> lp = linePlacement(symbolizer);
        lp.setRepeated(FF.literal(true));
        lp.setGap(FF.literal(10.0));
        assertTrue(assertPaintedBounds(render(symbolizer, horizontal)).x < 15);

        lp.setInitialGap(FF.literal(40.0));
        final Rectangle b = assertPaintedBounds(render(symbolizer, horizontal));
        assertEquals(50, b.x, 3, "First label expected at 40 pixels from the line start: " + b);
    }

    /**
     * Tests the line placement gap: the space between repeated labels.
     */
    @Test
    public void testLinePlacementGap() throws RenderingException {
        final Geometry horizontal = line(-90, 0,  90, 0);       // Pixels 10 to 190.
        final TextSymbolizer<Feature> symbolizer = text("HH", 10);
        final LinePlacement<Feature> lp = linePlacement(symbolizer);
        lp.setRepeated(FF.literal(true));
        final int width = assertPaintedBounds(render(text("HH", 10), origin())).width;

        lp.setGap(FF.literal(40.0));
        final BufferedImage image = render(symbolizer, horizontal);
        // The first label covers about [10 … 10+width], the second one starts 40 pixels later.
        for (int x = 10 + width + 4; x < 10 + width + 36; x++) {
            assertFalse(columnPainted(image, x), "Column " + x + " expected in the gap.");
        }
        boolean second = false;
        for (int x = 10 + width + 40; x < 10 + 2 * width + 44; x++) {
            second |= columnPainted(image, x);
        }
        assertTrue(second, "Second label expected after the gap.");
    }

    /**
     * Tests the line generalization: on a noisy line, the generalized label is straighter.
     */
    @Test
    public void testLinePlacementGeneralizeLine() throws RenderingException {
        final double[] xy = new double[2 * 41];
        for (int i = 0; i <= 40; i++) {
            xy[2*i]     = -80 + i * 4;
            xy[2*i + 1] = (i % 2 == 0) ? -2 : 2;      // Zigzag of 4 pixels amplitude.
        }
        final Geometry noisy = line(xy);
        final TextSymbolizer<Feature> symbolizer = text("HHHHHHHH", 12);
        final LinePlacement<Feature> lp = linePlacement(symbolizer);
        final BufferedImage raw = render(symbolizer, noisy);
        lp.setGeneralizeLine(FF.literal(true));
        final BufferedImage generalized = render(symbolizer, noisy);
        assertFalse(sameImage(raw, generalized));
        assertTrue(assertPaintedBounds(generalized).height < assertPaintedBounds(raw).height,
                "Generalized label expected less tilted.");
    }

    /**
     * Tests that labels along lines are kept upright: a line from east to west is labeled
     * like the same line from west to east.
     */
    @Test
    public void testLinePlacementUpright() throws RenderingException {
        final TextSymbolizer<Feature> symbolizer = text("HHH", 15);
        linePlacement(symbolizer);
        final BufferedImage eastward = render(symbolizer, line(-60, -10,  60, 10));
        final BufferedImage westward = render(symbolizer, line(60, 10,  -60, -10));
        assertTrue(sameImage(eastward, westward));
    }

    /**
     * Tests that aligned labels longer than the line are not rendered.
     */
    @Test
    public void testLinePlacementTooShort() throws RenderingException {
        final TextSymbolizer<Feature> symbolizer = text("HHHHHHHH", 20);
        linePlacement(symbolizer);
        assertNull(paintedBounds(render(symbolizer, line(-5, 0,  5, 0))));
    }

    // ////////////////////////////////////////////////////////////////////////
    // Other behaviors ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests that a polygon with point placement is labeled inside the polygon,
     * and that a point with line placement fallbacks on point placement.
     */
    @Test
    public void testPlacementFallbacks() throws RenderingException {
        final Geometry polygon = withCRS(GF.createPolygon(coordinates(-60, -30,  60, -30,  60, 30,  -60, 30,  -60, -30)), CRS84);
        Rectangle b = assertPaintedBounds(render(text("HHH", 15), polygon));
        assertTrue(new Rectangle(40, 20, 120, 60).contains(b), "Label expected inside the polygon: " + b);

        final TextSymbolizer<Feature> symbolizer = text("HHH", 15);
        linePlacement(symbolizer);
        b = assertPaintedBounds(render(symbolizer, origin()));
        assertTrue(b.contains(100, 50), "Label expected centered on the point: " + b);
    }

    /**
     * Tests the intersection with the label glyphs and halo.
     */
    @Test
    public void testIntersects() throws RenderingException {
        final TextSymbolizer<Feature> symbolizer = text("HHH", 20);
        final Rectangle b = assertPaintedBounds(render(symbolizer, origin()));
        final MapLayer layer = layer(symbolizer, origin());
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, b)) {
            assertEquals(1, result.count());
        }
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(5, 5, 10, 10))) {
            assertEquals(0, result.count());
        }
    }

    /**
     * Tests the rendering of a feature holding a SIS geometry.
     */
    @Test
    public void testSISGeometry() throws RenderingException {
        final var geom = org.apache.sis.geometries.GeometryFactory.DEFAULT.createPoint(CRS84, 50.0, 20.0);   // Pixel (150,30).

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Object.class).setName("geom");
        final Feature feature = ftb.build().newInstance();
        feature.setPropertyValue("geom", geom);

        final TextSymbolizer<Feature> symbolizer = text("HHH", 15);
        symbolizer.setGeometry(FF.property("geom"));

        final BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        final Scene2D scene = new Scene2D(grid, image.createGraphics());
        SymbolizerToScene2D.create(scene, symbolizer).paint(new SEPresentation(null, null, feature, symbolizer), (s) -> {});
        final Rectangle b = assertPaintedBounds(image);
        assertTrue(b.contains(150, 30), b.toString());
    }
}
