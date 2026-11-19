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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.DouglasPeucker;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.common.DrawingComposite;
import org.apache.sis.map.service.common.Drawing;
import org.apache.sis.map.service.common.FontFamily;
import org.apache.sis.map.service.common.GraphicPlacement;
import org.apache.sis.map.service.common.LabelBox;
import org.apache.sis.map.service.common.LabelCandidate;
import org.apache.sis.map.service.common.LineWalker;
import org.apache.sis.map.service.common.DrawingShape;
import org.apache.sis.map.service.common.ShapeHalo;
import org.apache.sis.map.service.common.TextShape;
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
 *   <li>Label: the text, labels are not rendered if the text is null or blank.
 *       Line breaks start new lines, each one aligned horizontally with the anchor point.</li>
 *   <li>Font: family (first installed family in preference order, generic names {@code serif}, {@code sans-serif}
 *       and {@code monospace} are supported), style ({@code normal}, {@code italic}, {@code oblique}),
 *       weight ({@code normal}, {@code bold}) and size.</li>
 *   <li>Point placement: anchor point, displacement (display axes, positive Y upward) and rotation (clockwise).
 *       Points and multi-points get a label on each point, other geometries on their representative point.</li>
 *   <li>Line placement: perpendicular offset (positive to the left), repeated labels with initial gap and gap,
 *       aligned labels following the line (otherwise horizontal), line generalization.
 *       Labels are kept upright and labels longer than the line are not rendered.
 *       Line breaks in the text are replaced by spaces.
 *       Geometries without linear components fallback on a centered point placement.</li>
 *   <li>Halo: radius and fill.</li>
 *   <li>Fill: color, opacity and graphic fill.</li>
 * </ul>
 *
 * <p>Labels are drawn after all other presentations, in the order of the presentations.
 * A label is omitted if it overlaps a label of another presentation already drawn.
 * The halo is part of the label for the overlap tests. The labels of a same presentation,
 * for example the repetitions along a line, do not exclude each other.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TextToScene2D extends SymbolizerToScene2D<TextSymbolizer<?>> {

    /**
     * Maximum font size in pixels, for avoiding huge outlines.
     */
    private static final double MAX_FONT_SIZE = 2000;

    /**
     * A label at one position, before styling.
     *
     * @param outline    the glyph outlines in display coordinates.
     * @param footprint  the area occupied by the label, including the halo.
     */
    private record Placement(Shape outline, List<LabelBox> footprint) {
    }

    TextToScene2D(Scene2D state, TextSymbolizer<?> symbolizer) {
        super(state, symbolizer);
    }

    /**
     * Returns the drawing of all labels of the given presentation, without overlap tests.
     *
     * @param  presentation  the presentation to draw.
     * @return the drawing, possibly empty.
     * @throws RenderingException if the drawing cannot be computed.
     */
    @Override
    protected Drawing drawing(SEPresentation presentation) throws RenderingException {
        final List<Drawing> drawings = new ArrayList<>();
        for (LabelCandidate label : labels(presentation)) {
            drawings.add(label.drawing());
        }
        return new DrawingComposite(drawings);
    }

    /**
     * Returns the labels of the given presentation, each one with its halo beneath the text.
     *
     * @param  presentation  the presentation to label.
     * @return the labels, may be empty but never null.
     * @throws RenderingException if the labels cannot be computed.
     */
    @Override
    protected List<LabelCandidate> labels(SEPresentation presentation) throws RenderingException {
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
        final Halo<?> halo = symbolizer.getHalo().orElse(null);
        final ShapeHalo haloShape = new ShapeHalo((halo != null) ? evaluateNumber(feature, halo.getRadius(), 1.0) * pixelsPerUnit : 0);
        final double margin = haloShape.isEmpty() ? 0 : haloShape.size();
        final TextShape label = new TextShape(text.strip(), font);
        final List<Placement> placements = new ArrayList<>();
        final LabelPlacement<?> placement = symbolizer.getLabelPlacement();
        boolean placed = false;
        if (placement instanceof LinePlacement<?> lp) {
            placed = placeOnLines(geometry, lp, label, feature, pixelsPerUnit, margin, placements);
        }
        if (!placed) {
            final PointPlacement<?> pp = (placement instanceof PointPlacement<?> p) ? p : null;
            placeOnPoints(geometry, pp, label, feature, pixelsPerUnit, margin, placements);
        }
        if (placements.isEmpty()) {
            return List.of();
        }
        final StrokeStyle haloFill = (margin > 0) ? StrokeStyle.create(halo.getFill(), feature, pixelsPerUnit) : null;
        final StrokeStyle textFill = StrokeStyle.create(symbolizer.getFill(), feature, pixelsPerUnit);
        final List<LabelCandidate> labels = new ArrayList<>(placements.size());
        for (Placement p : placements) {
            final Drawing beneath = (haloFill != null) ? haloFill.applyTo(new DrawingShape(haloShape.apply(p.outline()))) : null;
            final Drawing body    = (textFill != null) ? textFill.applyTo(new DrawingShape(p.outline())) : null;
            final Drawing drawing = DrawingComposite.of(beneath, body);
            if (drawing != null) {
                labels.add(new LabelCandidate(drawing, p.footprint(), 0));
            }
        }
        return labels;
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
            family = new FontFamily(evaluateString(feature, exp, null)).javaName();
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
     * Places a label on each anchor point of the geometry.
     *
     * @param pp  the point placement, or {@code null} for default values (label centered on the point).
     */
    private void placeOnPoints(Geometry geometry, PointPlacement<?> pp, TextShape label, Feature feature,
            double pixelsPerUnit, double margin, List<Placement> placements) throws RenderingException
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
        /*
         * The anchor point is relative to the label box: (0,0) is the lower-left corner,
         * at the descender line, and (1,1) the upper-right corner, at the ascender line.
         */
        final Shape outline = label.outline(ax, ay);
        final Rectangle2D bounds = outline.getBounds2D();
        final List<Point> points = new ArrayList<>();
        GeometryResolver.anchorPoints(geometry, points);
        for (Point point : points) {
            final double[] xy = GeometryResolver.toDisplay(point, state.getDataToDisplay(point.getCoordinateReferenceSystem()));
            if (xy == null) continue;
            final double x = xy[0] + dx;
            final double y = xy[1] - dy;
            final AffineTransform t = AffineTransform.getTranslateInstance(x, y);
            t.rotate(rotation);
            placements.add(new Placement(t.createTransformedShape(outline), List.of(new LabelBox(bounds, x, y, rotation).expand(margin))));
        }
    }

    /**
     * Places labels along the linear components of the geometry.
     *
     * @return {@code false} if the geometry has no linear component, in which case point placement should be used.
     */
    private boolean placeOnLines(Geometry geometry, LinePlacement<?> lp, TextShape label, Feature feature,
            double pixelsPerUnit, double margin, List<Placement> placements) throws RenderingException
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

        for (LineString line : components) {
            if (line.getNumPoints() < 2) continue;
            LineString display;
            try {
                display = GeometryResolver.toDisplay(line, state.getDataToDisplay(line.getCoordinateReferenceSystem()));
            } catch (OperationException ex) {
                throw new RenderingException(ex);
            }
            final LineString shifted = GeometryResolver.offset(display, offset);
            if (shifted != null) display = shifted;
            if (generalize) {
                display = DouglasPeucker.simplify(display, label.getHeight() / 2);
            }
            final LineWalker walker = new LineWalker(display);
            final double length = walker.length();
            final double width  = label.getAdvance();
            if (!(length > 0) || (aligned && width > length)) {
                continue;
            }
            /*
             * Start positions of the labels along the line.
             * A single label is centered on the line; repeated labels start at the initial gap.
             */
            final List<Double> starts = new ArrayList<>();
            if (repeated) {
                // At least one pixel between label starts, for avoiding an excessive number of labels.
                final double step = Math.max(1, width + gap);
                for (double s = initialGap; s + width <= length; s += step) {
                    starts.add(s);
                }
            } else {
                starts.add((length - width) / 2);
            }
            LineWalker reversed = null;
            for (double start : starts) {
                if (aligned) {
                    /*
                     * Keep the text upright: if the part of the line covered by this label goes from right
                     * to left on screen (or downward for vertical parts), place the label on the reversed line.
                     */
                    LineWalker path = walker;
                    double from = start;
                    if (walker.isUpsideDown(start, start + width)) {
                        if (reversed == null) reversed = walker.reverse();
                        path = reversed;
                        from = length - start - width;
                    }
                    placements.add(new Placement(label.alongLine(path, from), label.boxesAlongLine(path, from, margin)));
                } else {
                    final GraphicPlacement center = walker.position(start + width / 2);
                    final Shape outline = label.outline(0.5, 0.5);
                    final AffineTransform t = AffineTransform.getTranslateInstance(center.x(), center.y());
                    placements.add(new Placement(t.createTransformedShape(outline),
                            List.of(new LabelBox(outline.getBounds2D(), center.x(), center.y(), 0).expand(margin))));
                }
            }
        }
        return true;
    }
}
