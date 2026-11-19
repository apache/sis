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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.feature.Feature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.image.Interpolation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.common.ContrastStretch;
import org.apache.sis.map.service.common.DrawingComposite;
import org.apache.sis.map.service.common.CoverageSampler;
import org.apache.sis.map.service.common.CoverageSlices;
import org.apache.sis.map.service.common.HillShader;
import org.apache.sis.map.service.common.Drawing;
import org.apache.sis.map.service.common.DrawingImage;
import org.apache.sis.map.service.common.SampleTable;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.Resource;
import org.apache.sis.style.se1.ChannelSelection;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.ContrastEnhancement;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.OverlapBehavior;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.RasterSymbolizer;
import org.apache.sis.style.se1.SelectedChannel;
import org.apache.sis.style.se1.ShadedRelief;
import org.apache.sis.style.se1.Symbolizer;


/**
 * Support for RasterSymbolizer rendering.
 *
 * <p>The following {@link RasterSymbolizer} parameters are supported:</p>
 * <ul>
 *   <li>Geometry: for features, the geometry expression is evaluated on the feature and shall return
 *       a {@link GridCoverage} or a {@link GridCoverageResource}. For coverage resources, the resource is read.</li>
 *   <li>Opacity: applied on the whole rendered raster.</li>
 *   <li>ChannelSelection: red, green and blue channels, or a gray channel. Channels are identified by
 *       sample dimension name, or otherwise by band number starting at 1. Each channel may have its own
 *       contrast enhancement, which replaces the symbolizer one for that channel.</li>
 *   <li>ContrastEnhancement: normalize, histogram equalization and gamma, see {@link ContrastStretch}.
 *       Statistics are computed on the visible pixels. For a color image without channel selection,
 *       statistics are computed on the brightness of pixels and the same stretch is applied on all bands.</li>
 *   <li>ColorMap: {@link org.apache.sis.style.se1.function.Categorize} and {@link org.apache.sis.style.se1.function.Interpolate}
 *       functions, applied on the gray channel if selected, otherwise on the band designated by the lookup value
 *       ({@code "Rasterdata"} for the first band, or a literal or property name designating a band by name or number).
 *       Other kinds of expressions are not supported. Contrast enhancement is not applied when a color map is used.</li>
 *   <li>OverlapBehavior: for an aggregate of coverages, latest on top, earliest on top,
 *       average of overlapping pixels, or random order. Latest and earliest refer to the start time
 *       of the coverages if all of them have a temporal extent, or to the rendering order otherwise.</li>
 *   <li>ImageOutline: the footprint of each coverage is drawn with the given line or polygon symbolizer.</li>
 *   <li>ShadedRelief: hill shading of the elevations given by the gray channel, the color map band or the first band,
 *       see {@link HillShader}. The relief factor multiplies the slopes, and elevations are converted to metres if their
 *       unit is linear (metres are assumed otherwise). The colors of the raster are shaded, or if brightness only
 *       is true, the raster colors are not painted and the shading is applied on the canvas painted so far.</li>
 * </ul>
 *
 * <p>Without channel selection, color map or contrast enhancement, coverages with an indexed or 8 bits
 * color model are drawn with their own colors. Other coverages are drawn in gray (one or two bands)
 * or RGB (three bands or more) using the first bands, with values normalized between their minimum
 * and maximum. When color settings are specified, channels without contrast enhancement use the values
 * as-is if they are in the [0 … 255] range.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class RasterToScene2D extends SymbolizerToScene2D<RasterSymbolizer<?>> {

    /**
     * Number of points on each edge of the coverage footprint, for curved edges after reprojection.
     */
    private static final int FOOTPRINT_POINTS = 16;

    /**
     * The lookup value designating the first band in color maps.
     */
    private static final String RASTER_DATA = "rasterdata";

    RasterToScene2D(Scene2D state, RasterSymbolizer<?> symbolizer) {
        super(state, symbolizer);
    }

    /**
     * The rendering of the coverages of a presentation.
     *
     * @param drawing  the image of the colorized coverages, or {@code null} if nothing is visible.
     * @param rasters  the coverages resampled on the display grid, for drawing their outlines.
     */
    private record Rendering(Drawing drawing, List<DisplayRaster> rasters) {
    }

    /**
     * Returns the image of the colorized coverages followed by their outlines, used for painting and for hit tests.
     * Hit tests consider only the pixels of the image which are not fully transparent, and the outlines.
     *
     * @param  presentation  the presentation to draw.
     * @return the drawing of the coverages, or {@code null} if nothing is visible.
     * @throws RenderingException if the coverages cannot be read.
     */
    @Override
    protected Drawing drawing(SEPresentation presentation) throws RenderingException {
        final Rendering rendering = render(presentation);
        if (rendering == null) {
            return null;
        }
        final List<Drawing> drawings = new ArrayList<>();
        if (rendering.drawing() != null) {
            drawings.add(rendering.drawing());
        }
        addOutlines(presentation, rendering.rasters(), drawings);
        return drawings.isEmpty() ? null : new DrawingComposite(drawings);
    }

    /**
     * Resamples, colorizes and shades the coverages of the given presentation.
     *
     * @return the rendering, or {@code null} if the presentation has no coverage.
     */
    private Rendering render(SEPresentation presentation) throws RenderingException {
        final Feature feature = presentation.getCandidate();
        final List<GridCoverage> coverages = coverages(presentation);
        if (coverages.isEmpty()) {
            return null;
        }
        final double reliefFactor = reliefFactor(feature);
        final List<DisplayRaster> rasters = new ArrayList<>(coverages.size());
        for (GridCoverage coverage : coverages) {
            final DisplayRaster raster = resample(coverage, feature, reliefFactor);
            if (raster != null) rasters.add(raster);
        }
        colorize(rasters, feature);
        BufferedImage image = compose(rasters);
        if (reliefFactor > 0) {
            image = shade(image, rasters, feature, reliefFactor);
        }
        Drawing drawing = null;
        if (image != null) {
            final double opacity = StrokeStyle.clamp(evaluateNumber(feature, symbolizer.getOpacity(), 1.0));
            if (opacity > 0) {
                final GridExtent extent = state.grid.getExtent();
                drawing = new DrawingImage(image, (float) opacity)
                        .add(AffineTransform.getTranslateInstance(extent.getLow(0), extent.getLow(1)));
            }
        }
        return new Rendering(drawing, rasters);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Coverage reading ///////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns the coverages to render for the given presentation, in rendering order.
     */
    private List<GridCoverage> coverages(SEPresentation presentation) throws RenderingException {
        final List<GridCoverage> coverages = new ArrayList<>();
        final Feature feature = presentation.getCandidate();
        if (feature != null) {
            addCoverages(evaluate(feature, symbolizer.getGeometry()), coverages);
        } else {
            addCoverages(presentation.getResource(), coverages);
        }
        return coverages;
    }

    /**
     * Adds the coverages of the given value, which may be a coverage, a coverage resource or an aggregate.
     */
    private void addCoverages(Object value, List<GridCoverage> coverages) throws RenderingException {
        try {
            if (value instanceof GridCoverage coverage) {
                coverages.add(coverage);
            } else if (value instanceof GridCoverageResource resource) {
                coverages.add(resource.read(state.grid));
            } else if (value instanceof Aggregate aggregate) {
                for (Resource component : aggregate.components()) {
                    addCoverages(component, coverages);
                }
            }
        } catch (NoSuchDataException | DisjointExtentException ex) {
            // The coverage does not intersect the rendering area.
        } catch (DataStoreException ex) {
            throw new RenderingException(ex);
        }
    }

    /**
     * A coverage resampled on the display grid, with either its original colors
     * or the sample values of the bands to style.
     */
    private static final class DisplayRaster {
        /** Footprint of the coverage in its own CRS. */
        final Polygon footprint;
        /** Start time of the coverage, or {@code null} if unknown. */
        final Instant time;
        /** Image with the coverage colors, or {@code null} if the coverage needs to be styled. */
        RenderedImage colored;
        /** Sample values of the bands to style, one band per channel, or {@code null} if the coverage colors are used. */
        SampleTable samples;
        /** Elevations in metres, or {@code null} if no relief shading. */
        double[] elevations;
        /** The colorized image of the display size, computed by {@code colorize(…)}. */
        BufferedImage image;

        DisplayRaster(Polygon footprint, GridCoverage coverage) {
            this.footprint = footprint;
            final Instant[] extent = coverage.getGridGeometry().getTemporalExtent();
            time = (extent.length != 0) ? extent[0] : null;
        }
    }

    /**
     * How the bands of the coverages are converted to colors.
     */
    private enum Style {
        /** Coverage colors are used. */ DIRECT,
        /** A color map is applied on one band. */ COLOR_MAP,
        /** One band is rendered in gray. */ GRAY,
        /** Three bands are rendered as red, green and blue. */ RGB
    }

    /**
     * Returns whether the symbolizer has explicit color settings.
     */
    private boolean isStyled() {
        return symbolizer.getChannelSelection().isPresent()
            || symbolizer.getContrastEnhancement().isPresent()
            || ColorMapFunction.isSupported(symbolizer.getColorMap().orElse(null));
    }

    /**
     * Resamples the given coverage on the display grid.
     *
     * @param  reliefFactor  the relief shading exaggeration, or 0 if no relief shading.
     * @return the resampled raster, or {@code null} if the coverage cannot be rendered.
     */
    private DisplayRaster resample(GridCoverage coverage, Feature feature, double reliefFactor) throws RenderingException {
        final GridCoverage coverage2D;
        final DisplayRaster raster;
        try {
            coverage2D = new CoverageSlices(coverage).middle();
            raster = new DisplayRaster(footprint(coverage2D), coverage);
            final CoverageSampler sampler = new CoverageSampler(state.grid);
            if (!isStyled()) {
                final RenderedImage image = sampler.resample(coverage2D.forConvertedValues(false), Interpolation.NEAREST);
                if (isDisplayable(image.getColorModel())) {
                    raster.colored = image;
                }
            }
            if (raster.colored != null && !(reliefFactor > 0)) {
                return raster;
            }
            final List<SampleDimension> sampleDimensions = coverage2D.getSampleDimensions();
            final int[] bands = bands(sampleDimensions, feature);
            if (raster.colored == null) {
                raster.samples = sampler.sample(coverage2D, Interpolation.NEAREST, bands);
            }
            if (reliefFactor > 0) {
                // Elevations are interpolated for avoiding terraces in the shading.
                final double[] z = sampler.sample(coverage2D, Interpolation.BILINEAR, new int[] {bands[0]}).band(0);
                final double scale = toMetres(sampleDimensions.get(bands[0]));
                for (int i = 0; i < z.length; i++) z[i] *= scale;
                raster.elevations = z;
            }
        } catch (DisjointExtentException ex) {
            return null;        // The coverage does not intersect the rendering area.
        } catch (TransformException | IllegalArgumentException | IllegalStateException ex) {
            LOGGER.log(Level.WARNING, "Cannot resample coverage for rendering.", ex);
            return null;
        }
        return raster;
    }

    /**
     * Returns whether the given color model can be drawn as-is: indexed, or 8 bits per component.
     */
    private static boolean isDisplayable(ColorModel cm) {
        if (cm instanceof IndexColorModel) {
            return true;
        }
        if (cm == null) {
            return false;
        }
        for (int i = 0; i < cm.getNumComponents(); i++) {
            if (cm.getComponentSize(i) != 8) return false;
        }
        final int n = cm.getNumColorComponents();
        return n == 1 || n == 3;
    }

    /**
     * Returns the style to apply on the coverages.
     */
    private Style style() {
        final ColorMap<?> colorMap = symbolizer.getColorMap().orElse(null);
        if (ColorMapFunction.isSupported(colorMap)) {
            return Style.COLOR_MAP;
        }
        if (colorMap != null && colorMap.getFunction().isPresent()) {
            LOGGER.log(Level.FINE, "Unsupported color map function, only Categorize and Interpolate are supported.");
        }
        final SelectedChannel<?>[] channels = symbolizer.getChannelSelection().map(ChannelSelection::getChannels).orElse(null);
        if (channels != null) {
            return (channels.length == 3) ? Style.RGB : Style.GRAY;
        }
        return null;    // Depends on the number of bands.
    }

    /**
     * Returns the indices of the bands to style: one band for gray and color map, three bands for RGB.
     */
    private int[] bands(List<SampleDimension> sampleDimensions, Feature feature) throws RenderingException {
        final SelectedChannel<?>[] channels = symbolizer.getChannelSelection().map(ChannelSelection::getChannels).orElse(null);
        final Style style = style();
        if (style == Style.COLOR_MAP) {
            if (channels != null && channels.length == 1) {
                return new int[] {band(channels[0], sampleDimensions, feature)};
            }
            final String lookup = ColorMapFunction.lookupBand(symbolizer.getColorMap().get());
            if (lookup == null || lookup.trim().toLowerCase(Locale.ROOT).equals(RASTER_DATA)) {
                return new int[] {0};
            }
            return new int[] {band(lookup, sampleDimensions)};
        }
        if (channels != null) {
            final int[] bands = new int[channels.length];
            for (int i = 0; i < bands.length; i++) {
                bands[i] = band(channels[i], sampleDimensions, feature);
            }
            return bands;
        }
        return (sampleDimensions.size() >= 3) ? new int[] {0, 1, 2} : new int[] {0};
    }

    /**
     * Returns the index of the band of the given selected channel.
     */
    private static int band(SelectedChannel<?> channel, List<SampleDimension> sampleDimensions, Feature feature)
            throws RenderingException
    {
        final String name = evaluateString(feature, channel.getSourceChannelName(), null);
        if (name == null) {
            throw new RenderingException("Selected channel without source channel name.");
        }
        return band(name, sampleDimensions);
    }

    /**
     * Returns the index of the band identified by the given name, or by the given number starting at 1.
     */
    private static int band(String name, List<SampleDimension> sampleDimensions) throws RenderingException {
        final String key = name.trim();
        for (int i = 0; i < sampleDimensions.size(); i++) {
            final var sdName = sampleDimensions.get(i).getName();
            if (sdName != null && (key.equalsIgnoreCase(sdName.toString()) || key.equalsIgnoreCase(sdName.tip().toString()))) {
                return i;
            }
        }
        try {
            final int index = Integer.parseInt(key) - 1;
            if (index >= 0 && index < sampleDimensions.size()) {
                return index;
            }
        } catch (NumberFormatException ex) {
            // Not a band number.
        }
        throw new RenderingException("No band named \"" + name + "\" in the coverage.");
    }

    // ////////////////////////////////////////////////////////////////////////
    // Colorization ///////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Converts each resampled raster to an ARGB image of the display size, stored in {@link DisplayRaster#image}.
     * Contrast enhancement statistics are computed on all rasters together.
     */
    private void colorize(List<DisplayRaster> rasters, Feature feature) {
        final GridExtent extent = state.grid.getExtent();
        final int width  = (int) extent.getSize(0);
        final int height = (int) extent.getSize(1);
        /*
         * Prepare the conversion of sample values to colors, shared by all rasters.
         */
        int numChannels = 0;
        for (DisplayRaster r : rasters) {
            if (r.samples != null) numChannels = Math.max(numChannels, r.samples.getNumBands());
        }
        Style style = style();
        if (style == null) {
            style = (numChannels >= 3) ? Style.RGB : Style.GRAY;
        }
        ColorMapFunction colorMap = null;
        final ContrastStretch[] stretches = new ContrastStretch[numChannels];
        if (style == Style.COLOR_MAP) {
            colorMap = ColorMapFunction.create(symbolizer.getColorMap().get(), feature);
        } else {
            final SelectedChannel<?>[] channels = symbolizer.getChannelSelection().map(ChannelSelection::getChannels).orElse(null);
            final ContrastEnhancement<?> global = symbolizer.getContrastEnhancement().orElse(null);
            final boolean implicit = !isStyled();
            if (!implicit && channels == null && style == Style.RGB && global != null) {
                // SE: in the case of a color image, the relative grayscale brightness of a pixel color is used.
                Arrays.fill(stretches, stretch(global, feature, brightness(rasters)));
            } else for (int c = 0; c < numChannels; c++) {
                if (implicit) {
                    // No color settings: values are stretched for making them visible.
                    stretches[c] = new ContrastStretch(ContrastStretch.Method.NORMALIZE).fit(concatenate(rasters, c));
                    continue;
                }
                ContrastEnhancement<?> ce = global;
                if (channels != null && c < channels.length && channels[c].getContrastEnhancement().isPresent()) {
                    ce = channels[c].getContrastEnhancement().get();
                }
                stretches[c] = stretch(ce, feature, concatenate(rasters, c));
            }
        }
        /*
         * Convert each raster.
         */
        for (DisplayRaster raster : rasters) {
            final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (raster.colored != null) {
                drawColored(raster, image);
            } else {
                final int[] argb = new int[width * height];
                final SampleTable v = raster.samples;
                for (int i = 0; i < argb.length; i++) {
                    if (!v.isInside(i)) {
                        continue;       // Transparent, otherwise color maps would use their fallback color.
                    }
                    if (colorMap != null) {
                        argb[i] = colorMap.argb(v.value(0, i));
                    } else if (style == Style.RGB && v.getNumBands() >= 3) {
                        final int r = intensity(stretches[0], v.value(0, i));
                        final int g = intensity(stretches[1], v.value(1, i));
                        final int b = intensity(stretches[2], v.value(2, i));
                        argb[i] = (r < 0 || g < 0 || b < 0) ? ColorMapFunction.TRANSPARENT
                                : 0xFF000000 | (r << 16) | (g << 8) | b;
                    } else {
                        final int k = intensity(stretches[0], v.value(0, i));
                        argb[i] = (k < 0) ? ColorMapFunction.TRANSPARENT : 0xFF000000 | (k << 16) | (k << 8) | k;
                    }
                }
                image.setRGB(0, 0, width, height, argb, 0, width);
            }
            raster.image = image;
        }
    }

    /**
     * Creates the contrast stretch of a channel for a Symbology Encoding contrast enhancement.
     * Values are used as-is in the [0 … 255] range when there is no enhancement method.
     *
     * @param  enhancement  the contrast enhancement, or {@code null} if none.
     * @param  feature      feature for evaluating expressions, may be null.
     * @param  values       the sample values of the channel, NaN values are ignored.
     * @return the stretch fitted on the given values.
     */
    private ContrastStretch stretch(ContrastEnhancement<?> enhancement, Feature feature, double[] values) {
        ContrastStretch.Method method = ContrastStretch.Method.NONE;
        double gamma = 1;
        if (enhancement != null) {
            switch (enhancement.getMethod()) {
                case NORMALIZE: method = ContrastStretch.Method.NORMALIZE; break;
                case HISTOGRAM: method = ContrastStretch.Method.HISTOGRAM; break;
                default: break;
            }
            gamma = evaluateNumber(feature, enhancement.getGammaValue(), 1.0);
        }
        final ContrastStretch stretch = new ContrastStretch(method).setNativeRange(0, 255);
        if (gamma > 0 && gamma < Double.POSITIVE_INFINITY) {
            stretch.setGamma(gamma);
        }
        return stretch.fit(values);
    }

    /**
     * Returns the intensity in [0 … 255] of the given value, or -1 if the value is NaN.
     */
    private int intensity(ContrastStretch stretch, double value) {
        final double n = stretch.apply(value);
        return Double.isNaN(n) ? -1 : (int) Math.round(n * 255);
    }

    /**
     * Computes the brightness (luma) of the pixels of all rasters, from their three first channels.
     */
    private static double[] brightness(List<DisplayRaster> rasters) {
        final double[] r = concatenate(rasters, 0), g = concatenate(rasters, 1), b = concatenate(rasters, 2);
        final int n = Math.min(r.length, Math.min(g.length, b.length));
        for (int i = 0; i < n; i++) {
            r[i] = 0.299 * r[i] + 0.587 * g[i] + 0.114 * b[i];
        }
        return Arrays.copyOf(r, n);
    }

    /**
     * Concatenates the values of the given channel of all rasters, for computing statistics.
     */
    private static double[] concatenate(List<DisplayRaster> rasters, int channel) {
        int length = 0;
        for (DisplayRaster r : rasters) {
            if (r.samples != null && channel < r.samples.getNumBands()) length += r.samples.size();
        }
        final double[] all = new double[length];
        int offset = 0;
        for (DisplayRaster r : rasters) {
            if (r.samples != null && channel < r.samples.getNumBands()) {
                for (int i = 0, n = r.samples.size(); i < n; i++) {
                    all[offset++] = r.samples.value(channel, i);
                }
            }
        }
        return all;
    }

    /**
     * Draws a coverage with its own colors, clipped to its footprint for hiding the fill values.
     */
    private void drawColored(DisplayRaster raster, BufferedImage target) {
        final Graphics2D g = target.createGraphics();
        try {
            final Shape footprint = displayFootprint(raster.footprint);
            if (footprint != null) {
                final GridExtent extent = state.grid.getExtent();
                g.clip(AffineTransform.getTranslateInstance(-extent.getLow(0), -extent.getLow(1)).createTransformedShape(footprint));
            }
            g.drawRenderedImage(raster.colored, AffineTransform.getTranslateInstance(raster.colored.getMinX(), raster.colored.getMinY()));
        } finally {
            g.dispose();
        }
    }

    /**
     * Combines the colorized rasters according the overlap behavior.
     *
     * @return the combined image, or {@code null} if there is no raster.
     */
    private BufferedImage compose(List<DisplayRaster> rasters) {
        if (rasters.isEmpty()) {
            return null;
        }
        if (rasters.size() == 1) {
            return rasters.get(0).image;
        }
        final OverlapBehavior behavior = symbolizer.getOverlapBehavior();
        final List<DisplayRaster> ordered = new ArrayList<>(rasters);
        switch (behavior) {
            case AVERAGE: return average(rasters);
            case RANDOM:  Collections.shuffle(ordered); break;
            default: {
                // Latest or earliest capture time if known for all rasters, rendering order otherwise.
                if (ordered.stream().allMatch((r) -> r.time != null)) {
                    ordered.sort(Comparator.comparing((DisplayRaster r) -> r.time));
                }
                if (behavior == OverlapBehavior.EARLIEST_ON_TOP) {
                    Collections.reverse(ordered);
                }
                break;
            }
        }
        final BufferedImage result = ordered.get(0).image;
        final Graphics2D g = result.createGraphics();
        try {
            for (int i = 1; i < ordered.size(); i++) {
                g.drawImage(ordered.get(i).image, 0, 0, null);
            }
        } finally {
            g.dispose();
        }
        return result;
    }

    /**
     * Averages the colors of overlapping pixels, weighted by their alpha.
     */
    private static BufferedImage average(List<DisplayRaster> rasters) {
        final int width  = rasters.get(0).image.getWidth();
        final int height = rasters.get(0).image.getHeight();
        final int[][] pixels = new int[rasters.size()][];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = rasters.get(i).image.getRGB(0, 0, width, height, null, 0, width);
        }
        final int[] result = new int[width * height];
        for (int p = 0; p < result.length; p++) {
            double sa = 0, sr = 0, sg = 0, sb = 0;
            int count = 0;
            for (int[] px : pixels) {
                final int c = px[p];
                final int a = c >>> 24;
                if (a == 0) continue;
                sa += a;
                sr += a * ((c >>> 16) & 0xFF);
                sg += a * ((c >>>  8) & 0xFF);
                sb += a * ( c         & 0xFF);
                count++;
            }
            if (count != 0) {
                result[p] = ((int) Math.round(sa / count) << 24)
                          | ((int) Math.round(sr / sa) << 16)
                          | ((int) Math.round(sg / sa) << 8)
                          |  (int) Math.round(sb / sa);
            }
        }
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, result, 0, width);
        return image;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Relief shading /////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns the relief shading exaggeration, or 0 if there is no relief shading.
     */
    private double reliefFactor(Feature feature) {
        final ShadedRelief<?> relief = symbolizer.getShadedRelief().orElse(null);
        if (relief == null) {
            return 0;
        }
        final double factor = evaluateNumber(feature, relief.getReliefFactor(), 55);
        return (factor > 0 && Double.isFinite(factor)) ? factor : 0;
    }

    /**
     * Returns the factor converting the values of the given band to metres.
     * If the band has no linear unit, the values are assumed in metres.
     */
    private static double toMetres(SampleDimension band) {
        final Unit<?> unit = band.getUnits().orElse(null);
        if (unit != null && Units.isLinear(unit)) {
            return unit.asType(Length.class).getConverterTo(Units.METRE).convert(1);
        }
        return 1;
    }

    /**
     * Applies the relief shading on the colors of the given image. If the shading shall be applied
     * on the brightness of the canvas painted so far by other layers, returns the shading alone.
     *
     * @param  image         the colorized rasters, or {@code null} if none.
     * @param  reliefFactor  the relief shading exaggeration.
     * @return the image to paint, or {@code null} if none.
     */
    private BufferedImage shade(BufferedImage image, List<DisplayRaster> rasters, Feature feature, double reliefFactor) {
        double[] elevations = null;
        for (DisplayRaster raster : rasters) {          // Rasters painted later overwrite the previous ones.
            final double[] z = raster.elevations;
            if (z == null) continue;
            if (elevations == null) {
                elevations = z;
            } else for (int i = 0; i < z.length; i++) {
                if (!Double.isNaN(z[i])) elevations[i] = z[i];
            }
        }
        if (elevations == null) {
            return image;
        }
        final GridExtent extent = state.grid.getExtent();
        final BufferedImage overlay = new HillShader(elevations, (int) extent.getSize(0), (int) extent.getSize(1))
                .setGrid(state.grid).setFactor(reliefFactor).createOverlay();
        if (evaluateBoolean(feature, symbolizer.getShadedRelief().get().isBrightnessOnly(), false)) {
            return overlay;
        }
        if (image != null) {
            final Graphics2D g = image.createGraphics();
            try {
                g.setComposite(AlphaComposite.SrcAtop);         // Shade only the painted pixels.
                g.drawImage(overlay, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
        return image;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Footprint and outline //////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns the footprint of the given two-dimensional coverage in its CRS,
     * with intermediate points along the edges.
     */
    private static Polygon footprint(GridCoverage coverage2D) {
        final GridGeometry gg = coverage2D.getGridGeometry();
        final GridExtent extent = gg.getExtent();
        final double x0 = extent.getLow(0), x1 = extent.getHigh(0) + 1.0;
        final double y0 = extent.getLow(1), y1 = extent.getHigh(1) + 1.0;
        final int n = FOOTPRINT_POINTS;
        final double[] grid = new double[2 * (4 * n + 1)];
        int k = 0;
        for (int i = 0; i < n; i++) { grid[k++] = x0 + (x1 - x0) * i / n; grid[k++] = y0; }
        for (int i = 0; i < n; i++) { grid[k++] = x1; grid[k++] = y0 + (y1 - y0) * i / n; }
        for (int i = 0; i < n; i++) { grid[k++] = x1 - (x1 - x0) * i / n; grid[k++] = y1; }
        for (int i = 0; i < n; i++) { grid[k++] = x0; grid[k++] = y1 - (y1 - y0) * i / n; }
        grid[k++] = x0; grid[k] = y0;
        try {
            final MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CORNER);
            final double[] crs = new double[grid.length];
            gridToCRS.transform(grid, 0, crs, 0, grid.length / 2);
            final CoordinateReferenceSystem crs2D = gg.getCoordinateReferenceSystem();
            return GeometryFactory.DEFAULT.createPolygon(
                    GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(crs2D, crs)), List.of());
        } catch (TransformException | RuntimeException ex) {
            LOGGER.log(Level.FINE, "Cannot compute the coverage footprint.", ex);
            return null;
        }
    }

    /**
     * Returns the given footprint in display space, or {@code null} if it cannot be computed.
     */
    private Shape displayFootprint(Polygon footprint) {
        if (footprint == null) return null;
        try {
            final LineString ring = GeometryResolver.toDisplay(footprint.getExteriorRing(),
                    state.getDataToDisplay(footprint.getCoordinateReferenceSystem()));
            if (!(ring instanceof LinearRing linear)) {
                return null;
            }
            // A polygon rather than its ring, for area semantic in Shape.intersects(…) and contains(…).
            return Geometries.asShape(GeometryFactory.DEFAULT.createPolygon(linear, List.of()));
        } catch (RenderingException | OperationException ex) {
            LOGGER.log(Level.FINE, "Cannot project the coverage footprint.", ex);
            return null;
        }
    }

    /**
     * Adds the drawing of the image outline of each raster, if an outline symbolizer is defined.
     */
    private void addOutlines(SEPresentation presentation, List<DisplayRaster> rasters, List<Drawing> target)
            throws RenderingException
    {
        final Symbolizer<?> outline = symbolizer.getImageOutline().orElse(null);
        if (outline == null) {
            return;
        }
        for (DisplayRaster raster : rasters) {
            if (raster.footprint == null) continue;
            final Symbolizer<?> copy = withGeometry(outline, raster.footprint);
            if (copy == null) {
                LOGGER.log(Level.FINE, "Image outline shall be a line or polygon symbolizer.");
                return;
            }
            final SymbolizerToScene2D<?> renderer = create(state, copy);
            if (renderer != null) {
                final Drawing drawing = renderer.drawing(new SEPresentation(presentation.getLayer(), presentation.getResource(), null, copy));
                if (drawing != null) {
                    target.add(drawing);
                }
            }
        }
    }

    /**
     * Returns a copy of the given line or polygon symbolizer rendering the given geometry.
     *
     * @return the copy, or {@code null} if the symbolizer is neither a line or polygon symbolizer.
     */
    private static <R> Symbolizer<R> withGeometry(Symbolizer<R> symbolizer, Object geometry) {
        if (symbolizer instanceof LineSymbolizer<R> ls) {
            final var copy = new LineSymbolizer<>(ls);
            copy.setGeometry(copy.literal(geometry));
            return copy;
        }
        if (symbolizer instanceof PolygonSymbolizer<R> ps) {
            final var copy = new PolygonSymbolizer<>(ps);
            copy.setGeometry(copy.literal(geometry));
            return copy;
        }
        return null;
    }
}
