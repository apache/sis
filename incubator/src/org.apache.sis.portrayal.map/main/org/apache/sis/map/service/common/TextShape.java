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
package org.apache.sis.map.service.common;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;


/**
 * The glyphs of a text in a font, as shapes for labels.
 * The text can be written straight around an anchor point, or along a line with each glyph following the line.
 * Coordinates are in pixels with <var>y</var> down.
 *
 * <ul>
 *   <li>Line breaks start new lines in straight text. Along a line, they are replaced by spaces.</li>
 *   <li>The box of a text extends from the top of the ascenders of the first line
 *       to the bottom of the descenders of the last line.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TextShape {
    /**
     * Context for computing the glyph outlines, with fractional metrics for accurate placement.
     */
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /**
     * The layout of each line of the straight text, or {@code null} for empty lines.
     */
    private final TextLayout[] lines;

    /**
     * The glyphs of the text written on a single line, for placement along a line.
     */
    private final GlyphVector glyphs;

    /**
     * Distance from the baseline to the top of the ascenders, in pixels.
     */
    private final double ascent;

    /**
     * Distance from the baseline to the bottom of the descenders, in pixels.
     */
    private final double descent;

    /**
     * Distance between the bottom of the descenders of a line and the top of the ascenders of the next line.
     */
    private final double leading;

    /**
     * Creates the glyphs of the given text.
     *
     * @param  text  the text, which should not be blank.
     * @param  font  the font, with its size in pixels.
     */
    public TextShape(final String text, final Font font) {
        final String[] split = text.split("\\R");
        lines = new TextLayout[split.length];
        for (int i = 0; i < split.length; i++) {
            if (!split[i].isEmpty()) {
                lines[i] = new TextLayout(split[i], font, FRC);
            }
        }
        final char[] chars = text.replaceAll("\\R", " ").toCharArray();
        glyphs = font.layoutGlyphVector(FRC, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);
        final LineMetrics metrics = font.getLineMetrics(text, FRC);
        ascent  = metrics.getAscent();
        descent = metrics.getDescent();
        leading = metrics.getLeading();
    }

    /**
     * Returns the height of the straight text, from the top of the first line to the bottom of the last line.
     *
     * @return the height in pixels.
     */
    public double getHeight() {
        return lines.length * (ascent + descent + leading) - leading;
    }

    /**
     * Returns the length of the text written along a line.
     *
     * @return the advance of the text on a single line, in pixels.
     */
    public double getAdvance() {
        return glyphs.getLogicalBounds().getWidth();
    }

    /**
     * Returns the outline of the straight text, aligned relative to the anchor at (0,0).
     * The alignment is the position of the anchor in the box of the text, each line being aligned horizontally:
     *
     * <ul>
     *   <li>horizontally, 0 for the anchor at the start of the lines and 1 at their end;</li>
     *   <li>vertically, 0 for the anchor at the bottom of the box and 1 at its top.</li>
     * </ul>
     *
     * @param  anchorX  horizontal position of the anchor in the box, usually between 0 and 1.
     * @param  anchorY  vertical position of the anchor in the box, usually between 0 and 1.
     * @return the glyph outlines relative to the anchor.
     */
    public Shape outline(final double anchorX, final double anchorY) {
        final double lineHeight = ascent + descent + leading;
        final double top = -(1 - anchorY) * getHeight();
        final Path2D outline = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int i = 0; i < lines.length; i++) {
            final TextLayout line = lines[i];
            if (line != null) {
                final double x = -anchorX * line.getAdvance();
                outline.append(line.getOutline(AffineTransform.getTranslateInstance(x, top + i * lineHeight + ascent)), false);
            }
        }
        return outline;
    }

    /**
     * Returns the outline of the text written along a line, starting at the given distance from the line start.
     * Each glyph is rotated by the line direction at the glyph center, and vertically centered on the line.
     *
     * @param  walker  the line along which to write the text.
     * @param  start   distance along the line where the text starts, in pixels.
     * @return the glyph outlines in the coordinates of the line.
     */
    public Shape alongLine(final LineWalker walker, final double start) {
        final Path2D outline = new Path2D.Double(Path2D.WIND_NON_ZERO);
        final double baseline = (ascent - descent) / 2;
        for (int i = 0, n = glyphs.getNumGlyphs(); i < n; i++) {
            final double center = glyphCenter(i);
            final GraphicPlacement p = walker.position(start + center);
            final AffineTransform t = AffineTransform.getTranslateInstance(p.x(), p.y());
            t.rotate(p.rotation());
            t.translate(-center, baseline);
            outline.append(t.createTransformedShape(glyphs.getGlyphOutline(i)), false);
        }
        return outline;
    }

    /**
     * Returns the boxes occupied by the glyphs of the text written along a line.
     * The text is placed as in {@link #alongLine(LineWalker, double)}.
     *
     * @param  walker   the line along which to write the text.
     * @param  start    distance along the line where the text starts, in pixels.
     * @param  padding  margin added on each side of the glyph boxes, in pixels.
     * @return one box per glyph, in the coordinates of the line.
     */
    public List<LabelBox> boxesAlongLine(final LineWalker walker, final double start, final double padding) {
        final int n = glyphs.getNumGlyphs();
        final List<LabelBox> boxes = new ArrayList<>(n);
        final double height = ascent + descent;
        for (int i = 0; i < n; i++) {
            final double advance = glyphs.getGlyphMetrics(i).getAdvance();
            final GraphicPlacement p = walker.position(start + glyphCenter(i));
            final Rectangle2D bounds = new Rectangle2D.Double(-advance / 2, -height / 2, advance, height);
            boxes.add(new LabelBox(bounds, p.x(), p.y(), p.rotation()).expand(padding));
        }
        return boxes;
    }

    /**
     * Returns the distance from the start of the text to the center of the given glyph.
     */
    private double glyphCenter(final int i) {
        return glyphs.getGlyphPosition(i).getX() + glyphs.getGlyphMetrics(i).getAdvance() / 2;
    }
}
