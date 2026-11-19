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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.map.service.common.ColorSubstitution;
import org.apache.sis.map.service.common.ExternalImage;
import org.apache.sis.map.service.common.ResourceLoader;
import org.apache.sis.map.service.common.DrawingShape;
import org.apache.sis.style.se1.ColorReplacement;
import org.apache.sis.style.se1.ExternalGraphic;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.Graphic;
import org.apache.sis.style.se1.GraphicalSymbol;
import org.apache.sis.style.se1.Mark;
import org.apache.sis.style.se1.Stroke;
import org.apache.sis.style.se1.function.Recode;


/**
 * Renders a Symbology Encoding {@link Graphic} to an image.
 * The graphical symbols of the graphic are alternatives in preference order:
 * the first symbol which can be rendered is used.
 *
 * <p>Supported symbols are:</p>
 * <ul>
 *   <li>Marks with a well-known name: {@code square}, {@code circle}, {@code triangle}, {@code star},
 *       {@code cross} and {@code x}. Marks are filled with a color or a graphic fill, and stroked.</li>
 *   <li>Font marks with a well-known name of the form {@code ttf://Font Family#code}, where the code is
 *       a character code in decimal or hexadecimal ({@code 0x}) notation, using an installed font.</li>
 *   <li>Font marks with an online resource referencing a TrueType or OpenType font file,
 *       the glyph being designated by its index in the font with the mark index.</li>
 *   <li>External graphics with an inline icon, or an online resource referencing an image readable by
 *       {@link ImageIO} (PNG, JPEG, GIF, BMP…) or an SVG document (if Apache Batik is available).
 *       Color replacements by {@link Recode} functions are supported.</li>
 * </ul>
 *
 * <p>Marks have a default size of 6 pixels. External graphics have their native size by default
 * (16 pixels height for SVG documents without size), otherwise the graphic size is their height
 * and the width is scaled accordingly.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
final class GraphicRenderer {

    /**
     * Default mark size in pixels, as specified by SE.
     */
    static final double DEFAULT_SIZE = 6;

    /**
     * Maximum graphic size in pixels, for avoiding huge images.
     */
    private static final double MAX_SIZE = 4096;

    /**
     * Transparent margin in pixels around the symbol images, so that antialiased edges
     * are not clipped when images are drawn at fractional positions or rotated.
     */
    static final int PADDING = 1;

    /**
     * Prefix of well-known names designating a glyph of an installed font.
     */
    private static final String FONT_PREFIX = "ttf://";

    /**
     * Context for computing glyph outlines.
     */
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /**
     * Loader and cache of the external resources (images, SVG documents and fonts), shared by all renderers.
     */
    private static final ResourceLoader RESOURCES = new ResourceLoader();

    /**
     * Well-known marks, centered on (0,0) in a 1×1 box, in display orientation (y down).
     */
    private static final Shape SQUARE, CIRCLE, TRIANGLE, STAR, CROSS, X;
    static {
        SQUARE = new Rectangle2D.Double(-0.5, -0.5, 1, 1);
        CIRCLE = new Ellipse2D.Double(-0.5, -0.5, 1, 1);

        final Path2D triangle = new Path2D.Double();
        for (int i = 0; i < 3; i++) {
            final double angle = -Math.PI / 2 + i * 2 * Math.PI / 3;
            final double x = 0.5 * Math.cos(angle);
            final double y = 0.5 * Math.sin(angle);
            if (i == 0) triangle.moveTo(x, y); else triangle.lineTo(x, y);
        }
        triangle.closePath();
        TRIANGLE = recenter(triangle);

        final Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            final double angle = -Math.PI / 2 + i * Math.PI / 5;
            final double radius = (i % 2 == 0) ? 0.5 : 0.2;
            final double x = radius * Math.cos(angle);
            final double y = radius * Math.sin(angle);
            if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
        }
        star.closePath();
        STAR = recenter(star);

        final double hw = 0.125;
        final Area cross = new Area(new Rectangle2D.Double(-0.5, -hw, 1, 2 * hw));
        cross.add(new Area(new Rectangle2D.Double(-hw, -0.5, 2 * hw, 1)));
        CROSS = cross;
        X = AffineTransform.getRotateInstance(Math.PI / 4).createTransformedShape(cross);
    }

    /**
     * Translates the given shape for centering its bounds on (0,0).
     */
    private static Shape recenter(Shape shape) {
        final Rectangle2D bounds = shape.getBounds2D();
        return AffineTransform.getTranslateInstance(-bounds.getCenterX(), -bounds.getCenterY()).createTransformedShape(shape);
    }

    private GraphicRenderer() {
    }

    /**
     * A graphic rendered to an image, with its placement properties.
     */
    static final class Symbol {
        /**
         * The symbol image, centered, with a transparent padding of {@link #PADDING} pixels on each side.
         * Not null.
         */
        final BufferedImage image;
        /**
         * Size in pixels of the graphic box, centered in the image.
         * This is the box used for anchoring the graphic.
         */
        final double width, height;
        /**
         * Graphic rotation in radians, clockwise in display space.
         */
        final double rotation;
        /**
         * Graphic opacity, between 0 exclusive and 1 inclusive.
         */
        final float opacity;
        /**
         * Anchor point relative to the graphic box, (0,0) is the lower-left corner and (1,1) the upper-right corner.
         */
        final double anchorX, anchorY;
        /**
         * Displacement in pixels, positive Y values are upward.
         */
        final double displacementX, displacementY;

        Symbol(BufferedImage image, double width, double height, double rotation, float opacity,
               double anchorX, double anchorY, double displacementX, double displacementY)
        {
            this.image         = image;
            this.width         = width;
            this.height        = height;
            this.rotation      = rotation;
            this.opacity       = opacity;
            this.anchorX       = anchorX;
            this.anchorY       = anchorY;
            this.displacementX = displacementX;
            this.displacementY = displacementY;
        }

        /**
         * Returns a paint repeating this symbol image, for graphic fills.
         * Tiles are anchored on the display origin, so that adjacent features have aligned patterns.
         * The symbol rotation and opacity are not applied.
         *
         * @return the texture paint.
         */
        TexturePaint texture() {
            final BufferedImage tile = image.getSubimage(PADDING, PADDING,
                    image.getWidth() - 2 * PADDING, image.getHeight() - 2 * PADDING);
            return new TexturePaint(tile, new Rectangle2D.Double(0, 0, tile.getWidth(), tile.getHeight()));
        }

        /**
         * Translates the given transform for moving the anchor point of this symbol at the origin.
         * The anchor point is relative to the box of the graphic, centered in the image.
         */
        private void anchor(final AffineTransform t) {
            t.translate(-((image.getWidth()  - width)  / 2 + anchorX * width),
                        -((image.getHeight() - height) / 2 + (1 - anchorY) * height));
        }

        /**
         * Computes the transform from image space to display space for placing this symbol on a point.
         * The symbol is rotated around its anchor point, and the displacement is applied in display axes
         * (not rotated), positive Y values upward.
         *
         * @param x display position
         * @param y display position
         * @return transform from image space to display space.
         */
        AffineTransform placement(double x, double y) {
            final AffineTransform t = AffineTransform.getTranslateInstance(x + displacementX, y - displacementY);
            t.rotate(rotation);
            anchor(t);
            return t;
        }

        /**
         * Computes the transform from image space to display space for placing this symbol
         * along a line at the given display position. The displacement is applied in the
         * rotated frame, so it is relative to the line direction.
         *
         * @param x display position
         * @param y display position
         * @param angle additional rotation in radians, for example the direction of a line.
         * @return transform from image space to display space.
         */
        AffineTransform placement(double x, double y, double angle) {
            final AffineTransform t = AffineTransform.getTranslateInstance(x, y);
            t.rotate(angle + rotation);
            t.translate(displacementX, -displacementY);
            anchor(t);
            return t;
        }
    }

    /**
     * An image with the size of its graphic box.
     */
    private record Drawn(BufferedImage image, double width, double height) {
    }

    /**
     * Renders the given graphic for the given feature.
     *
     * @param  graphic        the graphic to render, not null.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the rendered symbol, or {@code null} if nothing is visible.
     */
    static Symbol render(Graphic<?> graphic, Feature feature, double pixelsPerUnit) {
        final float opacity = (float) StrokeStyle.clamp(SymbolizerToScene2D.evaluateNumber(feature, graphic.getOpacity(), 1.0));
        if (!(opacity > 0)) {
            return null;
        }
        // NaN means the default size: 6 pixels for marks, native size for external graphics.
        final double size = SymbolizerToScene2D.evaluateNumber(feature, graphic.getSize(), Double.NaN) * pixelsPerUnit;
        if (size <= 0 || size > MAX_SIZE) {
            return null;
        }
        List<? extends GraphicalSymbol<?>> symbols = graphic.graphicalSymbols();
        if (symbols.isEmpty()) {
            symbols = List.of(new Mark<>(FeatureTypeStyle.FACTORY));       // Gray square with black outline.
        }
        Drawn drawn = null;
        for (GraphicalSymbol<?> symbol : symbols) {
            if (symbol instanceof Mark<?> mark) {
                drawn = renderMark(mark, feature, pixelsPerUnit, Double.isNaN(size) ? DEFAULT_SIZE * pixelsPerUnit : size);
            } else if (symbol instanceof ExternalGraphic<?> external) {
                drawn = renderExternal(external, feature, size);
            }
            if (drawn != null) break;
        }
        if (drawn == null) {
            return null;
        }
        final double rotation = Math.toRadians(SymbolizerToScene2D.evaluateNumber(feature, graphic.getRotation(), 0.0));
        final var anchor = graphic.getAnchorPoint();
        final var displacement = graphic.getDisplacement();
        return new Symbol(drawn.image, drawn.width, drawn.height,
                Double.isFinite(rotation) ? rotation : 0, opacity,
                SymbolizerToScene2D.evaluateNumber(feature, anchor.getAnchorPointX(), 0.5),
                SymbolizerToScene2D.evaluateNumber(feature, anchor.getAnchorPointY(), 0.5),
                SymbolizerToScene2D.evaluateNumber(feature, displacement.getDisplacementX(), 0.0) * pixelsPerUnit,
                SymbolizerToScene2D.evaluateNumber(feature, displacement.getDisplacementY(), 0.0) * pixelsPerUnit);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Marks //////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Renders a mark with its fill and stroke.
     *
     * @param  size  the mark size in pixels.
     * @return the mark image, or {@code null} if the mark is invisible or its shape is not supported.
     */
    private static Drawn renderMark(Mark<?> mark, Feature feature, double pixelsPerUnit, double size) {
        if (!(size > 0) || size > MAX_SIZE) {
            return null;
        }
        final Shape unitShape = markShape(mark, feature);
        if (unitShape == null) {
            return null;
        }
        final Fill<?> fill = mark.getFill().orElse(null);
        final Stroke<?> stroke = mark.getStroke().orElse(null);
        final StrokeStyle filled  = (fill   != null) ? StrokeStyle.create(fill,   feature, pixelsPerUnit) : null;
        final StrokeStyle outline = (stroke != null) ? StrokeStyle.create(stroke, feature, pixelsPerUnit) : null;
        if (filled == null && outline == null) {
            return null;
        }
        final DrawingShape rs = new DrawingShape();
        if (filled  != null) filled.applyTo(rs);
        if (outline != null) outline.applyTo(rs);
        final double strokeWidth = (outline != null) ? outline.stroke.getLineWidth() : 0;
        final int side = Math.max(1, (int) Math.ceil(size + strokeWidth)) + 2 * PADDING;
        final AffineTransform toImage = AffineTransform.getTranslateInstance(side / 2.0, side / 2.0);
        toImage.scale(size, size);
        rs.setShape(toImage.createTransformedShape(unitShape));
        final BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            rs.paint(g);
        } finally {
            g.dispose();
        }
        return new Drawn(image, size, size);
    }

    /**
     * Returns the unit shape of the given mark, centered on (0,0) in a 1×1 box.
     *
     * @return the shape, or {@code null} if the mark is not supported.
     */
    private static Shape markShape(Mark<?> mark, Feature feature) {
        final OnlineResource resource = mark.getOnlineResource().orElse(null);
        if (resource != null) {
            final URI uri = resource.getLinkage();
            final Font font = (uri != null) ? RESOURCES.font(uri, mark.getFormat().orElse(null)) : null;
            if (font == null) {
                SymbolizerToScene2D.LOGGER.log(Level.FINE, "Unsupported mark resource: {0}", uri);
                return null;
            }
            final double index = mark.getMarkIndex().map((e) -> SymbolizerToScene2D.evaluateNumber(feature, e, Double.NaN)).orElse(Double.NaN);
            if (Double.isNaN(index)) {
                return null;
            }
            return glyph(font.deriveFont(100f).createGlyphVector(FRC, new int[] {(int) index}));
        }
        final String name = SymbolizerToScene2D.evaluateString(feature, mark.getWellKnownName(), "square").trim();
        if (name.regionMatches(true, 0, FONT_PREFIX, 0, FONT_PREFIX.length())) {
            return fontMark(name.substring(FONT_PREFIX.length()));
        }
        return toShape(name);
    }

    /**
     * Returns the unit shape of a glyph designated by {@code "Font Family#code"}.
     *
     * @return the shape, or {@code null} if the font or the character is not available.
     */
    private static Shape fontMark(final String spec) {
        final int s = spec.lastIndexOf('#');
        if (s <= 0 || s == spec.length() - 1) {
            return null;
        }
        final String family = spec.substring(0, s).trim();
        final String code = spec.substring(s + 1).trim();
        final int codePoint;
        try {
            if (code.startsWith("0x") || code.startsWith("0X")) {
                codePoint = Integer.parseInt(code.substring(2), 16);
            } else if (code.chars().allMatch(Character::isDigit)) {
                codePoint = Integer.parseInt(code);
            } else {
                codePoint = code.codePointAt(0);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        final Font font = new Font(family, Font.PLAIN, 100);
        // Java silently replaces unknown fonts by "Dialog", but keeps the requested name in Font.getName().
        if (!font.getFamily(Locale.ROOT).equalsIgnoreCase(family)) {
            SymbolizerToScene2D.LOGGER.log(Level.FINE, "Font family not found: {0}", family);
            return null;
        }
        if (!font.canDisplay(codePoint)) {
            return null;
        }
        return glyph(font.createGlyphVector(FRC, Character.toChars(codePoint)));
    }

    /**
     * Returns the outline of the given glyphs, scaled for fitting in a 1×1 box centered on (0,0).
     *
     * @return the shape, or {@code null} if the glyphs are empty.
     */
    private static Shape glyph(final GlyphVector glyphs) {
        final Shape outline = glyphs.getOutline();
        final Rectangle2D bounds = outline.getBounds2D();
        final double extent = Math.max(bounds.getWidth(), bounds.getHeight());
        if (!(extent > 0)) {
            return null;
        }
        final AffineTransform t = AffineTransform.getScaleInstance(1 / extent, 1 / extent);
        t.translate(-bounds.getCenterX(), -bounds.getCenterY());
        return t.createTransformedShape(outline);
    }

    /**
     * Returns the unit shape for the given well-known name.
     * Unknown names are replaced by a square.
     */
    static Shape toShape(String wellKnownName) {
        switch (wellKnownName.trim().toLowerCase(Locale.ROOT)) {
            case "circle"   : return CIRCLE;
            case "triangle" : return TRIANGLE;
            case "star"     : return STAR;
            case "cross"    : return CROSS;
            case "x"        : return X;
            default         : return SQUARE;
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // External graphics //////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Renders an external graphic.
     *
     * @param  size  the graphic height in pixels, or NaN for the native size.
     * @return the graphic image, or {@code null} if the graphic cannot be loaded.
     */
    private static Drawn renderExternal(ExternalGraphic<?> external, Feature feature, double size) {
        ExternalImage graphic = null;
        final Icon icon = external.getInlineContent().orElse(null);
        if (icon != null) {
            final BufferedImage image = toImage(icon);
            if (image != null) {
                graphic = new ExternalImage.Raster(image);
            }
        } else {
            final OnlineResource resource = external.getOnlineResource().orElse(null);
            final URI uri = (resource != null) ? resource.getLinkage() : null;
            if (uri != null) {
                graphic = RESOURCES.image(uri, external.getFormat().orElse(null));
            }
        }
        if (graphic == null) {
            return null;
        }
        final double nativeWidth  = graphic.getWidth();
        final double nativeHeight = graphic.getHeight();
        final double height = Double.isNaN(size) ? nativeHeight : size;
        final double width  = nativeWidth * height / nativeHeight;
        if (!(width > 0 && height > 0) || width > MAX_SIZE || height > MAX_SIZE) {
            return null;
        }
        final int w = (int) Math.ceil(width);
        final int h = (int) Math.ceil(height);
        final BufferedImage source = replaceColors(graphic.image(w, h), external.colorReplacements(), feature);
        final BufferedImage image = new BufferedImage(w + 2 * PADDING, h + 2 * PADDING, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, PADDING, PADDING, w, h, null);
        } finally {
            g.dispose();
        }
        return new Drawn(image, width, height);
    }

    /**
     * Paints the given icon in an image of the icon size.
     *
     * @return the image, or {@code null} if the icon is empty.
     */
    private static BufferedImage toImage(final Icon icon) {
        final int w = icon.getIconWidth(), h = icon.getIconHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            icon.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Applies the color replacements on a copy of the given image.
     * Each replacement shall be a {@link Recode} function using the pixel color as its lookup value.
     * The first function giving a color for a pixel color is used.
     *
     * @return the image with replaced colors, or the given image if there is no replacement.
     */
    @SuppressWarnings("unchecked")
    private static BufferedImage replaceColors(final BufferedImage source, final List<? extends ColorReplacement<?>> replacements,
                                               final Feature feature)
    {
        final List<Function<Object,Object>> recoders = new ArrayList<>();
        for (ColorReplacement<?> replacement : replacements) {
            final Expression<?,?> fn = replacement.getFunction().orElse(null);
            if (fn instanceof Recode<?> recode) {
                recoders.add(((Recode<Feature>) recode).bind(feature));
            } else if (fn != null) {
                SymbolizerToScene2D.LOGGER.log(Level.FINE, "Unsupported color replacement function, only Recode is supported.");
            }
        }
        if (recoders.isEmpty()) {
            return source;
        }
        return new ColorSubstitution((color) -> {
            for (Function<Object,Object> recoder : recoders) {
                final Color c = SymbolizerToScene2D.toColor(recoder.apply(color), null);
                if (c != null) return c;
            }
            return null;
        }).apply(source);
    }
}
