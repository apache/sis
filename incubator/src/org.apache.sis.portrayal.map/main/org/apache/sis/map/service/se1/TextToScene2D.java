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

import java.awt.BasicStroke;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.DouglasPeucker;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.style.se1.Font;
import org.apache.sis.style.se1.Halo;
import org.apache.sis.style.se1.LabelPlacement;
import org.apache.sis.style.se1.LinePlacement;
import org.apache.sis.style.se1.PointPlacement;
import org.apache.sis.style.se1.TextSymbolizer;


/**
 * Support for TextSymbolizer rendering.
 *
 * <p>All the {@link TextSymbolizer} parameters are supported:</p>
 * <ul>
 *   <li>Geometry: the geometry expression is evaluated on the feature. JTS geometries are viewed as SIS geometries.</li>
 *   <li>Unit of measure: pixel or ground linear units (metre, foot, …), applied on the font size, halo radius,
 *       displacement, perpendicular offset and gaps.</li>
 *   <li>Label: the text, labels are not rendered if the text is null or blank.</li>
 *   <li>Font: family (first installed family in preference order, generic names {@code serif}, {@code sans-serif}
 *       and {@code monospace} are supported), style ({@code normal}, {@code italic}, {@code oblique}),
 *       weight ({@code normal}, {@code bold}) and size.</li>
 *   <li>Point placement: anchor point, displacement (display axes, positive Y upward) and rotation (clockwise).
 *       Points and multi-points get a label on each point, other geometries on their representative point.</li>
 *   <li>Line placement: perpendicular offset (positive to the left), repeated labels with initial gap and gap,
 *       aligned labels following the line (otherwise horizontal), line generalization.
 *       Labels are kept upright and labels longer than the line are not rendered.
 *       Geometries without linear components fallback on a centered point placement.</li>
 *   <li>Halo: radius and fill.</li>
 *   <li>Fill: color, opacity and graphic fill.</li>
 * </ul>
 *
 * <p>Labels are painted immediately, without conflict resolution between overlapping labels.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TextToScene2D extends SymbolizerToScene2D<TextSymbolizer<?>> {

    /**
     * Context for computing glyph outlines, with fractional metrics for accurate placement.
     */
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /**
     * Maximum font size in pixels, for avoiding huge outlines.
     */
    private static final double MAX_FONT_SIZE = 2000;

    /**
     * Generic font family names, mapped to Java logical fonts.
     */
    private static final Map<String,String> LOGICAL_FONTS = Map.of(
            "serif",       java.awt.Font.SERIF,
            "sans-serif",  java.awt.Font.SANS_SERIF,
            "sansserif",   java.awt.Font.SANS_SERIF,
            "monospace",   java.awt.Font.MONOSPACED,
            "monospaced",  java.awt.Font.MONOSPACED,
            "dialog",      java.awt.Font.DIALOG,
            "dialoginput", java.awt.Font.DIALOG_INPUT);

    /**
     * Installed font families, by lower case name. Loaded when first needed.
     */
    private static final class InstalledFonts {
        static final Map<String,String> FAMILIES = new HashMap<>();
        static {
            try {
                for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)) {
                    FAMILIES.put(name.toLowerCase(Locale.ROOT), name);
                }
            } catch (RuntimeException | Error ex) {
                LOGGER.log(Level.WARNING, "Cannot list installed fonts.", ex);
            }
        }
    }

    private TextToScene2D(Scene2D state, TextSymbolizer<?> symbolizer) {
        super(state, symbolizer);
    }

    @Override
    public void paint(SEPresentation presentation, Consumer<Stream<Presentation>> callback) throws RenderingException {
        RenderedVisual.paint(createVisuals(presentation), state.getGraphics());
    }

    @Override
    public boolean intersects(SEPresentation presentation, Shape mask, Consumer<Stream<Presentation>> callback) throws RenderingException {
        return RenderedVisual.intersects(createVisuals(presentation), mask);
    }

    /**
     * Creates the visual elements for the given presentation: the halo first, then the text.
     *
     * @return visual elements, may be empty but never null.
     */
    private List<RenderedVisual> createVisuals(SEPresentation presentation) throws RenderingException {
        final Feature feature = presentation.getCandidate();
        final String text = evaluateString(feature, symbolizer.getLabel(), null);
        if (text == null || text.isBlank()) {
            return List.of();
        }
        final Geometry geometry = GeometryResolver.resolve(evaluate(feature, symbolizer.getGeometry()));
        if (geometry == null) {
            return List.of();
        }
        final double pixelsPerUnit = state.getPixelsPerUnit(symbolizer.getUnitOfMeasure());
        final java.awt.Font font = createFont(symbolizer.getFont(), feature, pixelsPerUnit);
        if (font == null) {
            return List.of();
        }
        final Label label = new Label(text.strip(), font);
        final Path2D outlines = new Path2D.Double(Path2D.WIND_NON_ZERO);
        final LabelPlacement<?> placement = symbolizer.getLabelPlacement();
        boolean placed = false;
        if (placement instanceof LinePlacement<?> lp) {
            placed = placeOnLines(geometry, lp, label, feature, pixelsPerUnit, outlines);
        }
        if (!placed) {
            final PointPlacement<?> pp = (placement instanceof PointPlacement<?> p) ? p : null;
            placeOnPoints(geometry, pp, label, feature, pixelsPerUnit, outlines);
        }
        if (outlines.getCurrentPoint() == null) {
            return List.of();
        }
        final List<RenderedVisual> visuals = new ArrayList<>(2);
        final Halo<?> halo = symbolizer.getHalo().orElse(null);
        if (halo != null) {
            final double radius = evaluateNumber(feature, halo.getRadius(), 1.0) * pixelsPerUnit;
            if (radius > 0 && Double.isFinite(radius)) {
                final Area area = new Area(new BasicStroke((float) (2 * radius),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(outlines));
                area.add(new Area(outlines));
                final RenderedShape rs = FillStyle.create(area, halo.getFill(), feature, pixelsPerUnit);
                if (rs != null) visuals.add(rs);
            }
        }
        final RenderedShape rs = FillStyle.create(outlines, symbolizer.getFill(), feature, pixelsPerUnit);
        if (rs != null) visuals.add(rs);
        return visuals;
    }

    /**
     * Creates the Java2D font for the given SE font.
     *
     * @return the font, or {@code null} if the size is invalid.
     */
    static java.awt.Font createFont(Font<?> se, Feature feature, double pixelsPerUnit) {
        final double size = evaluateNumber(feature, se.getSize(), 10.0) * pixelsPerUnit;
        if (!(size > 0) || size > MAX_FONT_SIZE) {
            return null;
        }
        String family = null;
        for (Expression<?,String> exp : se.family()) {
            family = resolveFamily(evaluateString(feature, exp, null));
            if (family != null) break;
        }
        if (family == null) {
            family = java.awt.Font.SANS_SERIF;
        }
        int style = java.awt.Font.PLAIN;
        final String fontStyle = evaluateString(feature, se.getStyle(), "normal").trim().toLowerCase(Locale.ROOT);
        if (fontStyle.equals("italic") || fontStyle.equals("oblique")) {
            style |= java.awt.Font.ITALIC;
        }
        final String weight = evaluateString(feature, se.getWeight(), "normal").trim().toLowerCase(Locale.ROOT);
        if (weight.equals("bold") || weight.equals("bolder")) {
            style |= java.awt.Font.BOLD;
        }
        return new java.awt.Font(family, style, 1).deriveFont((float) size);
    }

    /**
     * Returns the Java font family for the given name, or {@code null} if not available.
     */
    private static String resolveFamily(String name) {
        if (name == null) return null;
        final String key = name.trim().toLowerCase(Locale.ROOT);
        final String logical = LOGICAL_FONTS.get(key);
        return (logical != null) ? logical : InstalledFonts.FAMILIES.get(key);
    }

    /**
     * Glyphs and metrics of a label text.
     */
    private static final class Label {
        final GlyphVector glyphs;
        final double width, ascent, descent;

        Label(String text, java.awt.Font font) {
            final char[] chars = text.toCharArray();
            glyphs = font.layoutGlyphVector(FRC, chars, 0, chars.length, java.awt.Font.LAYOUT_LEFT_TO_RIGHT);
            final LineMetrics metrics = font.getLineMetrics(text, FRC);
            width   = glyphs.getLogicalBounds().getWidth();
            ascent  = metrics.getAscent();
            descent = metrics.getDescent();
        }

        /**
         * Height of the label box, from the bottom of descenders to the top of ascenders.
         */
        double height() {
            return ascent + descent;
        }

        /**
         * Vertical translation to apply on the baseline for centering the label box on y = 0.
         */
        double centerBaseline() {
            return (ascent - descent) / 2;
        }
    }

    /**
     * Places a label on each anchor point of the geometry.
     *
     * @param pp  the point placement, or {@code null} for default values (label centered on the point).
     */
    private void placeOnPoints(Geometry geometry, PointPlacement<?> pp, Label label, Feature feature,
            double pixelsPerUnit, Path2D outlines) throws RenderingException
    {
        double ax = 0.5, ay = 0.5, dx = 0, dy = 0, rotation = 0;
        if (pp != null) {
            ax = evaluateNumber(feature, pp.getAnchorPoint().getAnchorPointX(), 0.5);
            ay = evaluateNumber(feature, pp.getAnchorPoint().getAnchorPointY(), 0.5);
            dx = evaluateNumber(feature, pp.getDisplacement().getDisplacementX(), 0.0) * pixelsPerUnit;
            dy = evaluateNumber(feature, pp.getDisplacement().getDisplacementY(), 0.0) * pixelsPerUnit;
            rotation = Math.toRadians(evaluateNumber(feature, pp.getRotation(), 0.0));
            if (!Double.isFinite(dx)) dx = 0;
            if (!Double.isFinite(dy)) dy = 0;
            if (!Double.isFinite(rotation)) rotation = 0;
        }
        final Shape outline = label.glyphs.getOutline();
        final List<Point> points = new ArrayList<>();
        GeometryResolver.anchorPoints(geometry, points);
        for (Point point : points) {
            final double[] xy = GeometryResolver.toDisplay(point, state.getDataToDisplay(point.getCoordinateReferenceSystem()));
            if (xy == null) continue;
            /*
             * The anchor point is relative to the label box: (0,0) is the lower-left corner,
             * at the descender line, and (1,1) the upper-right corner, at the ascender line.
             * The baseline is at y = 0 in glyph coordinates, with y axis oriented down.
             */
            final AffineTransform t = AffineTransform.getTranslateInstance(xy[0] + dx, xy[1] - dy);
            t.rotate(rotation);
            t.translate(-ax * label.width, ay * label.height() - label.descent);
            outlines.append(t.createTransformedShape(outline), false);
        }
    }

    /**
     * Places labels along the linear components of the geometry.
     *
     * @return {@code false} if the geometry has no linear component, in which case point placement should be used.
     */
    private boolean placeOnLines(Geometry geometry, LinePlacement<?> lp, Label label, Feature feature,
            double pixelsPerUnit, Path2D outlines) throws RenderingException
    {
        final List<LineString> components = new ArrayList<>();
        GeometryResolver.linearComponents(geometry, components);
        if (components.isEmpty()) {
            return false;
        }
        final double  offset     = evaluateNumber(feature, lp.getPerpendicularOffset(), 0.0) * pixelsPerUnit;
        final boolean repeated   = evaluateBoolean(feature, lp.isRepeated(), false);
        final boolean aligned    = evaluateBoolean(feature, lp.isAligned(), true);
        final boolean generalize = evaluateBoolean(feature, lp.getGeneralizeLine(), false);
        double initialGap = evaluateNumber(feature, lp.getInitialGap(), 0.0) * pixelsPerUnit;
        double gap        = evaluateNumber(feature, lp.getGap(),        0.0) * pixelsPerUnit;
        if (!(initialGap >= 0) || Double.isInfinite(initialGap)) initialGap = 0;
        if (!(gap        >= 0) || Double.isInfinite(gap))        gap        = 0;

        final double[] position = new double[3];
        for (LineString line : components) {
            if (line.getNumPoints() < 2) continue;
            LineString display;
            try {
                display = GeometryResolver.toDisplay(line, state.getDataToDisplay(line.getCoordinateReferenceSystem()));
            } catch (OperationException ex) {
                throw new RenderingException(ex);
            }
            final LineString shifted = StrokeVisuals.offset(display, offset);
            if (shifted != null) display = shifted;
            if (generalize) {
                display = DouglasPeucker.simplify(display, label.height() / 2);
            }
            final PolylineWalker walker = PolylineWalker.of(display);
            final double length = walker.length();
            if (!(length > 0) || (aligned && label.width > length)) {
                continue;
            }
            /*
             * Start positions of the labels along the line.
             * A single label is centered on the line; repeated labels start at the initial gap.
             */
            final List<Double> starts = new ArrayList<>();
            if (repeated) {
                // At least one pixel between label starts, for avoiding an excessive number of labels.
                final double step = Math.max(1, label.width + gap);
                for (double s = initialGap; s + label.width <= length; s += step) {
                    starts.add(s);
                }
            } else {
                starts.add((length - label.width) / 2);
            }
            PolylineWalker reversed = null;
            for (double start : starts) {
                if (aligned) {
                    /*
                     * Keep the text upright: if the part of the line covered by this label goes from right
                     * to left on screen (or downward for vertical parts), place the label on the reversed line.
                     */
                    walker.position(start, position);
                    final double x0 = position[0], y0 = position[1];
                    walker.position(start + label.width, position);
                    final double dx = position[0] - x0, dy = position[1] - y0;
                    if (dx < 0 || (dx == 0 && dy > 0)) {
                        if (reversed == null) reversed = walker.reverse();
                        alignedLabel(reversed, length - start - label.width, label, position, outlines);
                    } else {
                        alignedLabel(walker, start, label, position, outlines);
                    }
                } else {
                    walker.position(start + label.width / 2, position);
                    final AffineTransform t = AffineTransform.getTranslateInstance(position[0], position[1]);
                    t.translate(-label.width / 2, label.centerBaseline());
                    outlines.append(t.createTransformedShape(label.glyphs.getOutline()), false);
                }
            }
        }
        return true;
    }

    /**
     * Places each glyph of the label along the line, rotated by the line direction at the glyph center.
     * The label is vertically centered on the line.
     */
    private static void alignedLabel(PolylineWalker walker, double start, Label label, double[] position, Path2D outlines) {
        final GlyphVector glyphs = label.glyphs;
        for (int i = 0, n = glyphs.getNumGlyphs(); i < n; i++) {
            final double gx  = glyphs.getGlyphPosition(i).getX();
            final double adv = glyphs.getGlyphMetrics(i).getAdvance();
            final double center = gx + adv / 2;
            walker.position(start + center, position);
            final AffineTransform t = AffineTransform.getTranslateInstance(position[0], position[1]);
            t.rotate(position[2]);
            t.translate(-center, label.centerBaseline());
            outlines.append(t.createTransformedShape(glyphs.getGlyphOutline(i)), false);
        }
    }

    public static final class Spi implements SymbolizerToScene2D.Spi<TextSymbolizer> {

        @Override
        public Class<TextSymbolizer> getSymbolizerType() {
            return TextSymbolizer.class;
        }

        @Override
        public SymbolizerToScene2D create(Scene2D state, TextSymbolizer symbolizer) throws RenderingException {
            return new TextToScene2D(state, symbolizer);
        }
    }
}
