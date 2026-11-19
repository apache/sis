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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.service.GraphicsPortrayer;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.MemoryFeatureSet;
import org.apache.sis.style.se1.ColorReplacement;
import org.apache.sis.style.se1.ExternalGraphic;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.Graphic;
import org.apache.sis.style.se1.GraphicFill;
import org.apache.sis.style.se1.Mark;
import org.apache.sis.style.se1.PointSymbolizer;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.style.se1.function.Recode;
import org.apache.sis.style.se1.function.Recode.MapItem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * Tests {@link GraphicRenderer} with external graphics (images, SVG documents, inline icons),
 * color replacements and font marks.
 *
 * <p>Most tests use a 20×10 PNG image whose left half is red and right half is blue.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GraphicRendererTest {

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    /**
     * Directory where to write the test images.
     */
    private final Path directory;

    /**
     * Creates a new test case.
     *
     * @throws IOException if the temporary directory cannot be created.
     */
    public GraphicRendererTest() throws IOException {
        directory = Files.createTempDirectory("sis-graphics");
    }

    /**
     * Deletes the temporary directory and its content.
     *
     * @throws IOException if a file cannot be deleted.
     */
    @AfterAll
    public void deleteDirectory() throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : (Iterable<Path>) files.sorted(java.util.Comparator.reverseOrder())::iterator) {
                Files.delete(file);
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Utilities //////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Writes a PNG image of the given size with a red left half and a blue right half.
     */
    private URI png(String name, int width, int height) throws IOException {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, (x < width / 2) ? Color.RED.getRGB() : Color.BLUE.getRGB());
            }
        }
        final Path file = directory.resolve(name);
        assertTrue(ImageIO.write(image, "png", file.toFile()));
        return file.toUri();
    }

    /**
     * Writes an SVG document of the given size filled with green.
     */
    private URI svg(String name, int width, int height) throws IOException {
        final Path file = directory.resolve(name);
        Files.writeString(file, "<svg xmlns='http://www.w3.org/2000/svg' width='" + width + "' height='" + height + "'>"
                + "<rect width='" + width + "' height='" + height + "' fill='#00FF00'/></svg>", StandardCharsets.UTF_8);
        return file.toUri();
    }

    /**
     * Creates an external graphic referencing the given resource.
     */
    private static ExternalGraphic<Feature> external(URI uri, String format) {
        final ExternalGraphic<Feature> external = new ExternalGraphic<>(FeatureTypeStyle.FACTORY);
        external.setOnlineResource(new DefaultOnlineResource(uri));
        external.setFormat(format);
        return external;
    }

    /**
     * Creates a graphic with the given symbols and size (null for the default size).
     */
    private static Graphic<Feature> graphic(Double size, Object... symbols) {
        final Graphic<Feature> graphic = new Graphic<>(FeatureTypeStyle.FACTORY);
        for (Object symbol : symbols) {
            graphic.graphicalSymbols().add((org.apache.sis.style.se1.GraphicalSymbol<Feature>) symbol);
        }
        if (size != null) graphic.setSize(FF.literal(size));
        return graphic;
    }

    /**
     * Creates a mark with the given well-known name, filled in black without outline.
     */
    private static Mark<Feature> mark(String wellKnownName) {
        final Mark<Feature> mark = new Mark<>(FeatureTypeStyle.FACTORY);
        mark.setWellKnownName(FF.literal(wellKnownName));
        final Fill<Feature> fill = new Fill<>(FeatureTypeStyle.FACTORY);
        fill.setColor(FF.literal(Color.BLACK));
        mark.setFill(fill);
        mark.setStroke(null);
        return mark;
    }

    /**
     * Returns the color of the pixel at the given position in the graphic box (excluding padding).
     */
    private static Color pixel(GraphicRenderer.Symbol symbol, int x, int y) {
        return new Color(symbol.image.getRGB(x + GraphicRenderer.PADDING, y + GraphicRenderer.PADDING), true);
    }

    /**
     * Returns the fraction of painted pixels in the symbol image.
     */
    private static double paintedRatio(GraphicRenderer.Symbol symbol) {
        int count = 0;
        final BufferedImage image = symbol.image;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) count++;
            }
        }
        return count / (double) (image.getWidth() * image.getHeight());
    }

    // ////////////////////////////////////////////////////////////////////////
    // External graphics //////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests an external PNG image without size: the native size is used.
     */
    @Test
    public void testImageNativeSize() throws IOException {
        final var symbol = GraphicRenderer.render(graphic(null, external(png("native.png", 20, 10), "image/png")), null, 1);
        assertNotNull(symbol);
        assertEquals(20, symbol.width);
        assertEquals(10, symbol.height);
        assertEquals(Color.RED,  pixel(symbol, 3, 5));
        assertEquals(Color.BLUE, pixel(symbol, 16, 5));
    }

    /**
     * Tests an external PNG image with a size: the size is the height and the aspect ratio is preserved.
     */
    @Test
    public void testImageScaled() throws IOException {
        final var symbol = GraphicRenderer.render(graphic(30.0, external(png("scaled.png", 20, 10), "image/png")), null, 1);
        assertNotNull(symbol);
        assertEquals(60, symbol.width);
        assertEquals(30, symbol.height);
        assertEquals(Color.RED,  pixel(symbol, 10, 15));
        assertEquals(Color.BLUE, pixel(symbol, 50, 15));
    }

    /**
     * Tests an external SVG document, rendered at its native size and at a given size.
     */
    @Test
    public void testSVG() throws IOException {
        assumeTrue(SVGGraphics.isAvailable(), "Apache Batik is not available.");
        final URI uri = svg("graphic.svg", 20, 10);
        var symbol = GraphicRenderer.render(graphic(null, external(uri, "image/svg+xml")), null, 1);
        assertNotNull(symbol);
        assertEquals(20, symbol.width);
        assertEquals(10, symbol.height);
        assertEquals(Color.GREEN, pixel(symbol, 10, 5));

        symbol = GraphicRenderer.render(graphic(40.0, external(uri, null)), null, 1);        // Format from file extension.
        assertEquals(80, symbol.width);
        assertEquals(40, symbol.height);
        assertEquals(Color.GREEN, pixel(symbol, 40, 20));
    }

    /**
     * Tests an SVG document without inherent size: SE specifies a default height of 16 pixels,
     * the width being given by the aspect ratio of the view box.
     */
    @Test
    public void testSVGWithoutSize() throws IOException {
        assumeTrue(SVGGraphics.isAvailable(), "Apache Batik is not available.");
        final Path file = directory.resolve("unsized.svg");
        Files.writeString(file, "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 10'>"
                + "<rect width='20' height='10' fill='#00FF00'/></svg>", StandardCharsets.UTF_8);
        final var symbol = GraphicRenderer.render(graphic(null, external(file.toUri(), "image/svg+xml")), null, 1);
        assertNotNull(symbol);
        assertEquals(32, symbol.width);
        assertEquals(16, symbol.height);
        assertEquals(Color.GREEN, pixel(symbol, 16, 8));
        assertEquals(Color.GREEN, pixel(symbol, 30, 14));
    }

    /**
     * Tests a mark filled with a graphic fill: a large square filled with a pattern of small red squares.
     */
    @Test
    public void testMarkGraphicFill() {
        final Mark<Feature> pattern = mark("square");
        pattern.getFill().get().setColor(FF.literal(Color.RED));
        final GraphicFill<Feature> gf = new GraphicFill<>(FeatureTypeStyle.FACTORY);
        gf.getGraphic().graphicalSymbols().add(pattern);
        gf.getGraphic().setSize(FF.literal(4.0));
        final Mark<Feature> mark = mark("square");
        mark.getFill().get().setGraphicFill(gf);
        final var symbol = GraphicRenderer.render(graphic(20.0, mark), null, 1);
        assertNotNull(symbol);
        assertEquals(Color.RED, pixel(symbol, 10, 10));
    }

    /**
     * Tests an external graphic with inline content.
     */
    @Test
    public void testInlineContent() {
        final BufferedImage image = new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 8; x++) image.setRGB(x, y, Color.YELLOW.getRGB());
        final ExternalGraphic<Feature> external = new ExternalGraphic<>(FeatureTypeStyle.FACTORY);
        external.setInlineContent(new ImageIcon(image));
        final var symbol = GraphicRenderer.render(graphic(null, external), null, 1);
        assertNotNull(symbol);
        assertEquals(8, symbol.width);
        assertEquals(4, symbol.height);
        assertEquals(Color.YELLOW, pixel(symbol, 4, 2));
    }

    /**
     * Tests color replacement: red replaced by green, blue unchanged.
     */
    @Test
    public void testColorReplacement() throws IOException {
        final ExternalGraphic<Feature> external = external(png("replaced.png", 20, 10), "image/png");
        final ColorReplacement<Feature> replacement = new ColorReplacement<>(FeatureTypeStyle.FACTORY);
        replacement.setFunction(new Recode<>(null, List.of(new MapItem<>(FF.literal("#FF0000"), FF.literal(Color.GREEN))), null));
        external.colorReplacements().add(replacement);
        final var symbol = GraphicRenderer.render(graphic(null, external), null, 1);
        assertEquals(Color.GREEN, pixel(symbol, 3, 5));
        assertEquals(Color.BLUE,  pixel(symbol, 16, 5));
    }

    /**
     * Tests that graphical symbols are alternatives: a missing image is replaced by the next symbol.
     */
    @Disabled //produce an annoying log, TODO find how to use sis test logging helper
    @Test
    public void testAlternatives() {
        final URI missing = directory.resolve("missing.png").toUri();
        final var symbol = GraphicRenderer.render(graphic(null, external(missing, "image/png"), mark("circle")), null, 1);
        assertNotNull(symbol, "The circle mark should be used.");
        assertEquals(6, symbol.width);        // Default mark size.
        assertNull(GraphicRenderer.render(graphic(null, external(missing, "image/png")), null, 1));
    }

    // ////////////////////////////////////////////////////////////////////////
    // Font marks /////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests a font mark designated by a well-known name of the form {@code ttf://Family#code}.
     * The letter "L" covers a small fraction of its box.
     */
    @Test
    public void testFontMarkWellKnownName() {
        final var symbol = GraphicRenderer.render(graphic(40.0, mark("ttf://SansSerif#0x4C")), null, 1);
        assertNotNull(symbol);
        assertEquals(40, symbol.width);
        final double ratio = paintedRatio(symbol);
        assertTrue(ratio > 0.05 && ratio < 0.5, "Letter L expected, painted ratio is " + ratio);
        // Decimal code gives the same glyph.
        final var decimal = GraphicRenderer.render(graphic(40.0, mark("ttf://SansSerif#76")), null, 1);
        assertEquals(ratio, paintedRatio(decimal), 1E-9);
        // Unknown font: not rendered.
        assertNull(GraphicRenderer.render(graphic(40.0, mark("ttf://No such font#0x4C")), null, 1));
    }

    /**
     * Tests a font mark designated by an online resource to a font file and a glyph index.
     * This test requires a TrueType font installed on the system.
     */
    @Test
    public void testFontMarkResource() throws IOException {
        final Optional<Path> font = findFont();
        assumeTrue(font.isPresent(), "No TrueType font found on this system.");
        final Mark<Feature> mark = mark("square");
        mark.setOnlineResource(new DefaultOnlineResource(font.get().toUri()));
        mark.setFormat("ttf");
        mark.setMarkIndex(FF.literal(36));
        final var symbol = GraphicRenderer.render(graphic(40.0, mark), null, 1);
        assertNotNull(symbol);
        final double ratio = paintedRatio(symbol);
        assertTrue(ratio > 0.02 && ratio < 0.9, "Glyph expected, painted ratio is " + ratio);
    }

    /**
     * Searches a TrueType font file in common system directories.
     */
    private static Optional<Path> findFont() throws IOException {
        for (String dir : new String[] {"/usr/share/fonts", "/usr/local/share/fonts", "/Library/Fonts", "C:\\Windows\\Fonts"}) {
            final Path path = Path.of(dir);
            if (Files.isDirectory(path)) {
                try (Stream<Path> files = Files.walk(path)) {
                    final Optional<Path> font = files.filter((p) -> p.toString().toLowerCase().endsWith(".ttf")).findFirst();
                    if (font.isPresent()) return font;
                }
            }
        }
        return Optional.empty();
    }

    // ////////////////////////////////////////////////////////////////////////
    // Rendering //////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the rendering of a point symbolizer with an external image, centered on the point.
     * The grid is 100×100 pixels over [-50 … 50]° and the point is at (0,0), which is pixel (50,50).
     */
    @Test
    public void testPointSymbolizer() throws IOException, RenderingException {
        final PointSymbolizer<Feature> symbolizer = new PointSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.getGraphic().graphicalSymbols().add(external(png("point.png", 20, 10), "image/png"));

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType type = ftb.build();
        final Feature feature = type.newInstance();
        final Geometry point = new GeometryFactory().createPoint(new Coordinate(0, 0));
        point.setUserData(CommonCRS.WGS84.normalizedGeographic());
        feature.setPropertyValue("geom", point);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        final Rule<Feature> rule = new Rule<>(FeatureTypeStyle.FACTORY);
        rule.symbolizers().add(symbolizer);
        fts.rules().add(rule);
        final MapLayer layer = new MapLayer();
        layer.setData(new MemoryFeatureSet(null, type, List.of(feature)));
        layer.setStyle(style);

        final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, -50, 50);
        env.setRange(1, -50, 50);
        final GridGeometry grid = new GridGeometry(new GridExtent(100, 100), env, GridOrientation.REFLECTION_Y);
        final BufferedImage image = new GraphicsPortrayer().setDomain(grid).portray(layer).getImage();
        // The image covers pixels 40 to 59 horizontally and 45 to 54 vertically.
        assertEquals(Color.RED.getRGB(),  image.getRGB(43, 50));
        assertEquals(Color.BLUE.getRGB(), image.getRGB(56, 50));
        assertEquals(0, image.getRGB(50, 42) >>> 24);
        assertEquals(0, image.getRGB(37, 50) >>> 24);
    }
}
