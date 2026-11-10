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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferDouble;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.GraphicsPortrayer;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.ChannelSelection;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.ContrastEnhancement;
import org.apache.sis.style.se1.ContrastMethod;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.se1.function.Interpolate.InterpolationPoint;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.OverlapBehavior;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.RasterSymbolizer;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.SelectedChannel;
import org.apache.sis.style.se1.ShadedRelief;
import org.apache.sis.style.se1.StyleFactory;
import org.apache.sis.style.se1.Symbology;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * Tests {@link RasterToScene2D}, with one test for each {@link RasterSymbolizer} parameter.
 *
 * <p>The rendering grid is 100×100 pixels over [-50 … 50]° in CRS:84, so one pixel is one degree.
 * Most tests use a coverage of 4×4 cells over [-20 … 20]°, so each cell is 10×10 pixels and the cell
 * at column <var>i</var> and row <var>j</var> (row 0 at the top) is centered on pixel (35+10i, 35+10j).
 * Pixels outside [30 … 70) are outside the coverage.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RasterToScene2DTest {

    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    private static final StyleFactory<Feature> SF = FeatureTypeStyle.FACTORY;

    private final GridGeometry grid;

    /**
     * Creates a new test case.
     */
    public RasterToScene2DTest() {
        grid = new GridGeometry(new GridExtent(100, 100), envelope(CRS84, -50, -50, 50, 50), GridOrientation.REFLECTION_Y);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Utilities //////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    private static GeneralEnvelope envelope(CoordinateReferenceSystem crs, double x0, double y0, double x1, double y1) {
        final GeneralEnvelope env = new GeneralEnvelope(crs);
        env.setRange(0, x0, x1);
        env.setRange(1, y0, y1);
        return env;
    }

    /**
     * Creates a coverage of double values over the given envelope in CRS:84.
     * Values are in row-major order, the first row being at the top (north).
     */
    private static GridCoverage coverage(double x0, double y0, double x1, double y1, int width, int height,
            String[] names, double[]... bands)
    {
        final GridGeometry gg = new GridGeometry(new GridExtent(width, height),
                envelope(CRS84, x0, y0, x1, y1), GridOrientation.REFLECTION_Y);
        final SampleDimension[] sds = new SampleDimension[bands.length];
        for (int i = 0; i < sds.length; i++) {
            sds[i] = new SampleDimension.Builder().setName(names[i]).build();
        }
        return new GridCoverageBuilder().setDomain(gg).setRanges(sds)
                .setValues(new DataBufferDouble(bands, width * height), new Dimension(width, height)).build();
    }

    /**
     * Creates the default 4×4 coverage over [-20 … 20]° with the given bands.
     */
    private static GridCoverage coverage(String[] names, double[]... bands) {
        return coverage(-20, -20, 20, 20, 4, 4, names, bands);
    }

    /**
     * Values 0 to 15 in row-major order.
     */
    private static double[] ramp() {
        final double[] values = new double[16];
        for (int i = 0; i < 16; i++) values[i] = i;
        return values;
    }

    /**
     * Returns an array of 16 values all equal to the given value.
     */
    private static double[] constant(double value) {
        final double[] values = new double[16];
        java.util.Arrays.fill(values, value);
        return values;
    }

    /**
     * Creates a coverage from an RGB image, over the given envelope in CRS:84.
     */
    private static GridCoverage rgbCoverage(double x0, double y0, double x1, double y1, Color... colors) {
        final int n = (int) Math.round(Math.sqrt(colors.length));
        final BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_3BYTE_BGR);
        for (int i = 0; i < colors.length; i++) {
            image.setRGB(i % n, i / n, colors[i].getRGB());
        }
        final GridGeometry gg = new GridGeometry(new GridExtent(n, n), envelope(CRS84, x0, y0, x1, y1), GridOrientation.REFLECTION_Y);
        return new GridCoverageBuilder().setDomain(gg).setValues(image).build();
    }

    private static Symbology style(RasterSymbolizer<Feature> symbolizer) {
        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        final Rule<Feature> rule = new Rule<>(SF);
        rule.symbolizers().add(symbolizer);
        fts.rules().add(rule);
        return style;
    }

    private BufferedImage render(RasterSymbolizer<Feature> symbolizer, Resource data) throws RenderingException {
        final MapLayer layer = new MapLayer();
        layer.setData(data);
        layer.setStyle(style(symbolizer));
        return new GraphicsPortrayer().setDomain(grid).portray(layer).getImage();
    }

    private BufferedImage render(RasterSymbolizer<Feature> symbolizer, GridCoverage coverage) throws RenderingException {
        return render(symbolizer, new MemoryGridCoverageResource(coverage));
    }

    /**
     * Returns an aggregate of the given coverages, in the given order.
     */
    private static Aggregate aggregate(GridCoverage... coverages) {
        final List<Resource> components = new java.util.ArrayList<>();
        for (GridCoverage c : coverages) components.add(new MemoryGridCoverageResource(c));
        return new Aggregate() {
            @Override public Collection<? extends Resource> components() {return components;}
            @Override public Metadata getMetadata() {return null;}
        };
    }

    private static RasterSymbolizer<Feature> symbolizer() {
        return new RasterSymbolizer<>(SF);
    }

    private static SelectedChannel<Feature> channel(String name) {
        return SF.createSelectedChannel(name);
    }

    private static ContrastEnhancement<Feature> contrast(ContrastMethod method) {
        final ContrastEnhancement<Feature> ce = SF.createContrastEnhancement();
        ce.setMethod(method);
        return ce;
    }

    private static int gray(BufferedImage image, int x, int y) {
        final int argb = image.getRGB(x, y);
        assertEquals(0xFF, argb >>> 24, "Opaque pixel expected.");
        assertEquals((argb >>> 16) & 0xFF, argb & 0xFF, "Gray pixel expected.");
        return argb & 0xFF;
    }

    private static void assertColor(Color expected, BufferedImage image, int x, int y) {
        assertEquals(Integer.toHexString(expected.getRGB()), Integer.toHexString(image.getRGB(x, y)),
                () -> "Pixel (" + x + "," + y + ")");
    }

    private static void assertTransparent(BufferedImage image, int x, int y) {
        assertEquals(0, image.getRGB(x, y) >>> 24, () -> "Pixel (" + x + "," + y + ") should be transparent.");
    }

    /**
     * Pixel coordinate of the center of the cell at the given column or row.
     */
    private static int cell(int index) {
        return 35 + 10 * index;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Default rendering //////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the default rendering of a single band of values: gray ramp between minimum and maximum,
     * transparent outside the coverage and on NaN values.
     */
    @Test
    public void testDefaultGray() throws RenderingException {
        final double[] values = ramp();
        values[5] = Double.NaN;
        final BufferedImage image = render(symbolizer(), coverage(new String[] {"v"}, values));
        assertEquals(0,   gray(image, cell(0), cell(0)));
        assertEquals(255, gray(image, cell(3), cell(3)));
        assertEquals(Math.round(8 * 255 / 15.0), gray(image, cell(0), cell(2)));
        assertTransparent(image, cell(1), cell(1));         // NaN value.
        assertTransparent(image, 25, 50);                   // Outside the coverage.
        assertTransparent(image, 75, 50);
    }

    /**
     * Tests the default rendering of an RGB image: the image colors are used.
     */
    @Test
    public void testDefaultRGBImage() throws RenderingException {
        final BufferedImage image = render(symbolizer(),
                rgbCoverage(-20, -20, 20, 20, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW));
        // 2×2 cells of 20 pixels.
        assertColor(Color.RED,    image, 40, 40);
        assertColor(Color.GREEN,  image, 60, 40);
        assertColor(Color.BLUE,   image, 40, 60);
        assertColor(Color.YELLOW, image, 60, 60);
        assertTransparent(image, 25, 50);
    }

    /**
     * Tests the rendering of a coverage in another CRS (latitude first), which requires a reprojection.
     */
    @Test
    public void testReprojection() throws RenderingException {
        final CoordinateReferenceSystem latLon = CommonCRS.WGS84.geographic();
        // Envelope in (latitude, longitude) order: latitudes [0 … 20], longitudes [-20 … 0].
        final GridGeometry gg = new GridGeometry(new GridExtent(1, 1), envelope(latLon, 0, -20, 20, 0), GridOrientation.HOMOTHETY);
        final GridCoverage coverage = new GridCoverageBuilder().setDomain(gg)
                .setRanges(new SampleDimension.Builder().setName("v").build())
                .setValues(new DataBufferDouble(new double[] {1}, 1), new Dimension(1, 1)).build();
        final BufferedImage image = render(symbolizer(), coverage);
        // Longitudes [-20 … 0] are pixels 30 to 49, latitudes [0 … 20] are pixels 30 to 49.
        assertEquals(0xFF, image.getRGB(40, 40) >>> 24);
        assertTransparent(image, 60, 40);
        assertTransparent(image, 40, 60);
    }

    // ////////////////////////////////////////////////////////////////////////
    // RasterSymbolizer parameters ////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Tests the geometry expression: the coverage is an attribute of a feature.
     */
    @Test
    public void testGeometry() throws RenderingException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(GridCoverage.class).setName("raster");
        final Feature feature = ftb.build().newInstance();
        feature.setPropertyValue("raster", coverage(new String[] {"v"}, ramp()));

        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        symbolizer.setGeometry(FF.property("raster"));

        final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final Scene2D scene = new Scene2D(grid, image.createGraphics());
        SymbolizerToScene2D.create(scene, symbolizer).paint(new SEPresentation(null, null, feature, symbolizer), (s) -> {});
        assertEquals(255, gray(image, cell(3), cell(3)));
        assertTransparent(image, 25, 50);
    }

    /**
     * Tests the symbolizer opacity.
     */
    @Test
    public void testOpacity() throws RenderingException {
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        symbolizer.setOpacity(FF.literal(0.5));
        final BufferedImage image = render(symbolizer, coverage(new String[] {"v"}, ramp()));
        assertEquals(128, image.getRGB(cell(3), cell(3)) >>> 24, 2);

        symbolizer.setOpacity(FF.literal(0.0));
        assertTransparent(render(symbolizer, coverage(new String[] {"v"}, ramp())), cell(3), cell(3));
    }

    /**
     * Tests the channel selection of red, green and blue channels by band name.
     */
    @Test
    public void testChannelSelectionRGB() throws RenderingException {
        final GridCoverage coverage = coverage(new String[] {"first", "second", "third"},
                constant(10), constant(100), constant(200));
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        final ChannelSelection<Feature> selection = SF.createChannelSelection();
        selection.setChannels(channel("third"), channel("first"), channel("second"));
        symbolizer.setChannelSelection(selection);
        final BufferedImage image = render(symbolizer, coverage);
        assertColor(new Color(200, 10, 100), image, cell(1), cell(1));
    }

    /**
     * Tests the channel selection of a gray channel, by name and by band number.
     */
    @Test
    public void testChannelSelectionGray() throws RenderingException {
        final GridCoverage coverage = coverage(new String[] {"first", "second"}, constant(10), constant(100));
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        final ChannelSelection<Feature> selection = SF.createChannelSelection();
        selection.setChannels(channel("second"));
        symbolizer.setChannelSelection(selection);
        assertEquals(100, gray(render(symbolizer, coverage), cell(1), cell(1)));

        selection.setChannels(channel("1"));            // Band number, starting at 1.
        assertEquals(10, gray(render(symbolizer, coverage), cell(1), cell(1)));
    }

    /**
     * Tests the contrast enhancement of a selected channel, which replaces the global one.
     */
    @Test
    public void testChannelContrastEnhancement() throws RenderingException {
        final double[] values = ramp();
        for (int i = 0; i < values.length; i++) values[i] += 100;      // 100 to 115.
        final GridCoverage coverage = coverage(new String[] {"v"}, values);
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        final ChannelSelection<Feature> selection = SF.createChannelSelection();
        final SelectedChannel<Feature> channel = channel("v");
        selection.setChannels(channel);
        symbolizer.setChannelSelection(selection);
        assertEquals(100, gray(render(symbolizer, coverage), cell(0), cell(0)));       // Values used as-is.

        channel.setContrastEnhancement(contrast(ContrastMethod.NORMALIZE));
        symbolizer.setContrastEnhancement(contrast(ContrastMethod.NONE));
        final BufferedImage image = render(symbolizer, coverage);
        assertEquals(0,   gray(image, cell(0), cell(0)));
        assertEquals(255, gray(image, cell(3), cell(3)));
    }

    /**
     * Tests the normalize contrast enhancement.
     */
    @Test
    public void testContrastEnhancementNormalize() throws RenderingException {
        final double[] values = ramp();
        for (int i = 0; i < values.length; i++) values[i] += 100;      // 100 to 115.
        final GridCoverage coverage = coverage(new String[] {"v"}, values);
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        symbolizer.setContrastEnhancement(contrast(ContrastMethod.NONE));
        BufferedImage image = render(symbolizer, coverage);
        assertEquals(100, gray(image, cell(0), cell(0)));
        assertEquals(115, gray(image, cell(3), cell(3)));

        symbolizer.setContrastEnhancement(contrast(ContrastMethod.NORMALIZE));
        image = render(symbolizer, coverage);
        assertEquals(0,   gray(image, cell(0), cell(0)));
        assertEquals(255, gray(image, cell(3), cell(3)));
    }

    /**
     * Tests the contrast enhancement of a color image: statistics are computed on the pixel brightness
     * and the same stretch is applied on all bands, which preserves the hue.
     * Brightness of (64,32,32) is 41.568 and brightness of (128,64,64) is 83.136.
     */
    @Test
    public void testContrastEnhancementColorImage() throws RenderingException {
        final Color dark = new Color(64, 32, 32), light = new Color(128, 64, 64);
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        symbolizer.setContrastEnhancement(contrast(ContrastMethod.NORMALIZE));
        final BufferedImage image = render(symbolizer, rgbCoverage(-20, -20, 20, 20, dark, light, dark, light));
        assertColor(new Color(138, 0, 0), image, 40, 40);           // (64 - 41.568) / 41.568 × 255 = 137.6
        assertColor(new Color(255, 138, 138), image, 60, 40);
    }

    /**
     * Tests the histogram equalization: most cells have the value 0 and one cell has the value 100,
     * so value 0 becomes bright after equalization while it is black after normalization.
     */
    @Test
    public void testContrastEnhancementHistogram() throws RenderingException {
        final double[] values = constant(0);
        values[15] = 100;
        final GridCoverage coverage = coverage(new String[] {"v"}, values);
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        symbolizer.setContrastEnhancement(contrast(ContrastMethod.NORMALIZE));
        assertEquals(0, gray(render(symbolizer, coverage), cell(0), cell(0)));

        symbolizer.setContrastEnhancement(contrast(ContrastMethod.HISTOGRAM));
        final BufferedImage image = render(symbolizer, coverage);
        assertEquals(Math.round(15 / 16.0 * 255), gray(image, cell(0), cell(0)));
        assertEquals(255, gray(image, cell(3), cell(3)));
    }

    /**
     * Tests the gamma contrast enhancement: values greater than 1 brighten the image.
     */
    @Test
    public void testContrastEnhancementGamma() throws RenderingException {
        final double[] values = constant(64);
        values[15] = 255;
        final GridCoverage coverage = coverage(new String[] {"v"}, values);
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        final ContrastEnhancement<Feature> ce = SF.createContrastEnhancement();
        ce.setGammaValue(FF.literal(2.0));
        symbolizer.setContrastEnhancement(ce);
        assertEquals(ContrastMethod.NONE, ce.getMethod());
        assertEquals(Math.round(Math.sqrt(64 / 255.0) * 255), gray(render(symbolizer, coverage), cell(0), cell(0)));

        ce.setGammaValue(FF.literal(0.5));
        assertEquals(Math.round(Math.pow(64 / 255.0, 2) * 255), gray(render(symbolizer, coverage), cell(0), cell(0)));
    }

    /**
     * Creates a symbolizer with a color map using the given function.
     */
    private static RasterSymbolizer<Feature> colorMap(Expression<Feature,?> function) {
        final ColorMap<Feature> colorMap = SF.createColorMap();
        colorMap.setFunction(function);
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        symbolizer.setColorMap(colorMap);
        return symbolizer;
    }

    /**
     * Creates an interpolation point.
     */
    private static InterpolationPoint<Feature> point(double data, Color color) {
        return new InterpolationPoint<>(FF.literal(data), FF.literal(color));
    }

    /**
     * Tests the color map with a categorize function, with thresholds belonging to the succeeding
     * or preceding interval.
     */
    @Test
    public void testColorMapCategorize() throws RenderingException {
        final var values = List.of(FF.literal(Color.RED), FF.literal(Color.GREEN), FF.literal(Color.BLUE));
        final var thresholds = List.of(FF.literal(5), FF.literal(10));
        final GridCoverage coverage = coverage(new String[] {"v"}, ramp());

        BufferedImage image = render(colorMap(new Categorize<>(null, values, thresholds, null, null)), coverage);
        assertColor(Color.RED,   image, cell(0), cell(1));      // Value 4.
        assertColor(Color.GREEN, image, cell(1), cell(1));      // Value 5.
        assertColor(Color.GREEN, image, cell(1), cell(2));      // Value 9.
        assertColor(Color.BLUE,  image, cell(2), cell(2));      // Value 10.

        image = render(colorMap(new Categorize<>(null, values, thresholds, Categorize.ThresholdsBelongTo.PRECEDING, null)), coverage);
        assertColor(Color.RED,   image, cell(1), cell(1));      // Value 5.
        assertColor(Color.GREEN, image, cell(2), cell(2));      // Value 10.
        assertColor(Color.BLUE,  image, cell(3), cell(2));      // Value 11.
    }

    /**
     * Tests the color map with an interpolate function, in linear, cosine and cubic modes.
     */
    @Test
    public void testColorMapInterpolate() throws RenderingException {
        final var points = List.of(point(3, Color.BLACK), point(13, Color.WHITE));
        final GridCoverage coverage = coverage(new String[] {"v"}, ramp());

        BufferedImage image = render(colorMap(new Interpolate<>(null, points, null, null, null)), coverage);
        assertEquals(0,   gray(image, cell(0), cell(0)));       // Value 0, below the first point.
        assertEquals(0,   gray(image, cell(3), cell(0)));       // Value 3.
        assertEquals(128, gray(image, cell(0), cell(2)));       // Value 8, middle.
        assertEquals(51,  gray(image, cell(1), cell(1)));       // Value 5: 20%.
        assertEquals(255, gray(image, cell(3), cell(3)));       // Value 15, above the last point.

        image = render(colorMap(new Interpolate<>(null, points, Interpolate.Mode.COSINE, null, null)), coverage);
        assertEquals(128, gray(image, cell(0), cell(2)), 1);       // Middle, 127.5 subject to rounding errors.
        assertEquals(Math.round((1 - Math.cos(Math.PI * 0.2)) / 2 * 255), gray(image, cell(1), cell(1)));

        final var three = List.of(point(3, Color.BLACK), point(8, Color.GRAY), point(13, Color.WHITE));
        image = render(colorMap(new Interpolate<>(null, three, Interpolate.Mode.CUBIC, null, null)), coverage);
        assertEquals(128, gray(image, cell(0), cell(2)));       // Value 8 at the middle point.
        assertEquals(0,   gray(image, cell(3), cell(0)));
    }

    /**
     * Tests the color map fallback value, used for NaN values.
     */
    @Test
    public void testColorMapFallback() throws RenderingException {
        final double[] values = ramp();
        values[0] = Double.NaN;
        final var points = List.of(point(0, Color.BLACK), point(15, Color.WHITE));
        final GridCoverage coverage = coverage(new String[] {"v"}, values);
        assertTransparent(render(colorMap(new Interpolate<>(null, points, null, null, null)), coverage), cell(0), cell(0));

        final BufferedImage image = render(colorMap(new Interpolate<>(null, points, null, null, FF.literal(Color.MAGENTA))), coverage);
        assertColor(Color.MAGENTA, image, cell(0), cell(0));
        assertTransparent(image, 25, 50);       // Outside the coverage, not a missing value.
    }

    /**
     * Tests the color map lookup value designating a band by name, and color values given as strings.
     */
    @Test
    public void testColorMapLookupBand() throws RenderingException {
        final GridCoverage coverage = coverage(new String[] {"first", "second"}, constant(0), constant(10));
        final var values = List.of(FF.literal("#FF0000"), FF.literal("#0000FF"));
        final var thresholds = List.of(FF.literal(5));
        assertColor(Color.RED,  render(colorMap(new Categorize<>(null, values, thresholds, null, null)), coverage), cell(1), cell(1));
        assertColor(Color.BLUE, render(colorMap(new Categorize<>(FF.property("second"), values, thresholds, null, null)), coverage), cell(1), cell(1));
        assertColor(Color.BLUE, render(colorMap(new Categorize<>(FF.literal("2"), values, thresholds, null, null)), coverage), cell(1), cell(1));
    }

    /**
     * Tests that unsupported color map functions are ignored: the default rendering is used.
     */
    @Test
    public void testColorMapUnsupported() throws RenderingException {
        final BufferedImage image = render(colorMap(FF.literal(Color.RED)), coverage(new String[] {"v"}, ramp()));
        assertEquals(255, gray(image, cell(3), cell(3)));
    }

    /**
     * Tests the overlap behavior between two overlapping coverages of an aggregate:
     * a red coverage over [-30 … 10]° and a blue coverage over [-10 … 30]° in longitude.
     * The overlap is pixels 40 to 59.
     */
    @Test
    public void testOverlapBehavior() throws RenderingException {
        final GridCoverage red  = rgbCoverage(-30, -10, 10, 10, Color.RED);
        final GridCoverage blue = rgbCoverage(-10, -10, 30, 10, Color.BLUE);
        final RasterSymbolizer<Feature> symbolizer = symbolizer();

        BufferedImage image = render(symbolizer, aggregate(red, blue));
        assertColor(Color.RED,  image, 30, 50);
        assertColor(Color.BLUE, image, 70, 50);
        assertColor(Color.BLUE, image, 50, 50);                 // Default is latest on top.

        symbolizer.setOverlapBehavior(OverlapBehavior.EARLIEST_ON_TOP);
        image = render(symbolizer, aggregate(red, blue));
        assertColor(Color.RED,  image, 50, 50);
        assertColor(Color.BLUE, image, 70, 50);

        symbolizer.setOverlapBehavior(OverlapBehavior.AVERAGE);
        image = render(symbolizer, aggregate(red, blue));
        assertColor(new Color(128, 0, 128), image, 50, 50);
        assertColor(Color.RED,  image, 30, 50);

        symbolizer.setOverlapBehavior(OverlapBehavior.RANDOM);
        image = render(symbolizer, aggregate(red, blue));
        final int argb = image.getRGB(50, 50);
        assertTrue(argb == Color.RED.getRGB() || argb == Color.BLUE.getRGB());
    }

    /**
     * Creates a coverage of one pixel of the given color over [x0 … x1] × [-10 … 10]°,
     * at the given time in milliseconds since epoch.
     */
    private static GridCoverage timedCoverage(double x0, double x1, Color color, long time) throws FactoryException {
        final GeneralEnvelope env = new GeneralEnvelope(CRS.compound(CRS84, CommonCRS.Temporal.JAVA.crs()));
        env.setRange(0, x0, x1);
        env.setRange(1, -10, 10);
        env.setRange(2, time, time + 1000);
        final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        image.setRGB(0, 0, color.getRGB());
        final GridGeometry gg = new GridGeometry(new GridExtent(null, null, new long[] {1, 1, 1}, false),
                                                 env, GridOrientation.REFLECTION_Y);
        return new GridCoverageBuilder().setDomain(gg).setValues(image).build();
    }

    /**
     * Tests the latest on top and earliest on top behaviors with coverages having a time:
     * the capture time is used instead of the order in the aggregate.
     */
    @Test
    public void testOverlapBehaviorTime() throws RenderingException, FactoryException {
        final GridCoverage red  = timedCoverage(-30, 10, Color.RED,  2_000_000);      // Latest.
        final GridCoverage blue = timedCoverage(-10, 30, Color.BLUE, 1_000_000);      // Earliest.
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        BufferedImage image = render(symbolizer, aggregate(red, blue));
        assertColor(Color.BLUE, image, 70, 50);
        assertColor(Color.RED,  image, 50, 50);

        symbolizer.setOverlapBehavior(OverlapBehavior.EARLIEST_ON_TOP);
        image = render(symbolizer, aggregate(red, blue));
        assertColor(Color.BLUE, image, 50, 50);
    }

    /**
     * Creates an elevation model of 40×40 cells over [-20 … 20]° (one cell per display pixel),
     * rising by 1000 metres per cell toward the east in the left half and descending in the right half.
     * The left half faces the light (west) and the right half is in the shadow.
     */
    private static GridCoverage ridge() {
        final double[] z = new double[40 * 40];
        for (int i = 0; i < z.length; i++) {
            final int column = i % 40;
            z[i] = 1000 * ((column < 20) ? column : 39 - column);
        }
        return coverage(-20, -20, 20, 20, 40, 40, new String[] {"elevation"}, z);
    }

    /**
     * Creates a symbolizer painting all values in gray (128) with a shaded relief.
     */
    private static RasterSymbolizer<Feature> shadedGray(boolean brightnessOnly) {
        final RasterSymbolizer<Feature> symbolizer = colorMap(new Categorize<Feature,Color>(null,
                List.of(FF.literal(new Color(128, 128, 128))), List.of(), null, null));
        final ShadedRelief<Feature> relief = new ShadedRelief<>(SF);
        relief.setBrightnessOnly(FF.literal(brightnessOnly));
        symbolizer.setShadedRelief(relief);
        return symbolizer;
    }

    /**
     * Tests the shaded relief applied on the raster colors. With the default relief factor of 55
     * and pixels of 111 km, the slopes are ±0.494, which gives gray levels of about 160 and 75.
     */
    @Test
    public void testShadedRelief() throws RenderingException {
        final int lit = gray(render(shadedGray(false), ridge()), 40, 50);
        final int shadowed = gray(render(shadedGray(false), ridge()), 60, 50);
        assertTrue(lit > 150 && lit < 170, () -> "Slope facing the light: " + lit);
        assertTrue(shadowed > 65 && shadowed < 85, () -> "Slope in the shadow: " + shadowed);

        final double[] flat = new double[40 * 40];
        java.util.Arrays.fill(flat, 1500);
        assertEquals(128, gray(render(shadedGray(false), coverage(-20, -20, 20, 20, 40, 40, new String[] {"z"}, flat)), 50, 50),
                     "Flat areas are not shaded.");

        final RasterSymbolizer<Feature> symbolizer = shadedGray(false);
        symbolizer.getShadedRelief().get().setReliefFactor(FF.literal(0));
        assertEquals(128, gray(render(symbolizer, ridge()), 60, 50), "Relief factor 0 disables the shading.");
        symbolizer.getShadedRelief().get().setReliefFactor(FF.literal(200));
        assertTrue(gray(render(symbolizer, ridge()), 60, 50) < shadowed, "Higher relief factor should give darker shadows.");
    }

    /**
     * Tests the shaded relief applied on the brightness of the canvas: the raster colors are not painted,
     * only the white highlights and black shadows.
     */
    @Test
    public void testShadedReliefBrightnessOnly() throws RenderingException {
        final BufferedImage image = render(shadedGray(true), ridge());
        final int lit = image.getRGB(40, 50), shadowed = image.getRGB(60, 50);
        assertEquals(0xFFFFFF, lit & 0xFFFFFF, "White highlight expected, raster colors shall not be painted.");
        assertEquals(0x000000, shadowed & 0xFFFFFF, "Black shadow expected.");
        assertTrue((lit >>> 24) > 50 && (lit >>> 24) < 80, () -> "Highlight alpha: " + (lit >>> 24));
        assertTrue((shadowed >>> 24) > 90 && (shadowed >>> 24) < 120, () -> "Shadow alpha: " + (shadowed >>> 24));
        assertTransparent(image, 25, 50);
    }

    /**
     * Tests the image outline, with line and polygon symbolizers.
     */
    @Test
    public void testImageOutline() throws RenderingException {
        final LineSymbolizer<Feature> line = new LineSymbolizer<>(SF);
        line.getStroke().setColor(FF.literal(Color.RED));
        line.getStroke().setWidth(FF.literal(4.0));
        final RasterSymbolizer<Feature> symbolizer = symbolizer();
        symbolizer.setImageOutline(line);
        BufferedImage image = render(symbolizer, coverage(new String[] {"v"}, ramp()));
        assertColor(Color.RED, image, 30, 50);      // Left edge.
        assertColor(Color.RED, image, 50, 69);      // Bottom edge.
        assertColor(Color.RED, image, 29, 50);      // Outside the coverage, inside the stroke.
        assertEquals(0xFF, image.getRGB(cell(1), cell(1)) >>> 24);
        assertTrue(gray(image, cell(1), cell(1)) >= 0);

        final PolygonSymbolizer<Feature> polygon = new PolygonSymbolizer<>(SF);
        final Fill<Feature> fill = SF.createFill();
        fill.setColor(FF.literal(Color.BLUE));
        polygon.setFill(fill);
        polygon.setStroke(null);
        symbolizer.setImageOutline(polygon);
        image = render(symbolizer, coverage(new String[] {"v"}, ramp()));
        assertColor(Color.BLUE, image, cell(1), cell(1));       // Polygon drawn over the raster.
        assertTransparent(image, 25, 50);
    }

    /**
     * Tests the intersection with the coverage footprint.
     */
    @Test
    public void testIntersects() throws RenderingException {
        final MapLayer layer = new MapLayer();
        layer.setData(new MemoryGridCoverageResource(coverage(new String[] {"v"}, ramp())));
        layer.setStyle(style(symbolizer()));
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(45, 45, 4, 4))) {
            assertEquals(1, result.count());
        }
        try (Stream<Presentation> result = new GraphicsPortrayer().setDomain(grid).intersects(layer, new Rectangle(5, 5, 4, 4))) {
            assertEquals(0, result.count());
        }
    }
}
