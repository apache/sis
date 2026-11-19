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

import java.awt.BasicStroke;


/**
 * Builder of the Java2D strokes of lines, from values already evaluated and converted to pixels.
 * The default is a solid line of 1 pixel with butt caps and miter joins.
 *
 * {@snippet lang="java" :
 *     BasicStroke stroke = new StrokeBuilder()
 *             .setWidth(3)
 *             .setCap(BasicStroke.CAP_ROUND)
 *             .setDashes(new double[] {10, 5}, 0)
 *             .build();
 *     }
 *
 * <ul>
 *   <li>Negative and NaN widths are replaced by 0.</li>
 *   <li>Negative and NaN dash lengths are replaced by 0. A dash pattern whose total length is 0 or infinite is ignored.</li>
 *   <li>An odd number of dash lengths is repeated for alternating dashes and gaps.</li>
 *   <li>The dash offset is reduced to the period of the dash pattern, negative offsets included.</li>
 *   <li>The miter limit is 10 times the line width.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class StrokeBuilder {
    /**
     * Limit of the miter joins, as a ratio of the stroke width.
     */
    private static final float MITER_LIMIT = 10;

    /**
     * Width of the line, in pixels.
     */
    private double width;

    /**
     * Shape at the ends of the line, as a {@link BasicStroke} constant.
     */
    private int cap;

    /**
     * Shape at the corners of the line, as a {@link BasicStroke} constant.
     */
    private int join;

    /**
     * Lengths of the dashes and gaps in pixels, or {@code null} for a solid line.
     */
    private float[] dashes;

    /**
     * Offset of the dash pattern, in pixels between 0 inclusive and the period exclusive.
     */
    private float phase;

    /**
     * Creates a builder of solid lines of 1 pixel with butt caps and miter joins.
     */
    public StrokeBuilder() {
        width = 1;
        cap   = BasicStroke.CAP_BUTT;
        join  = BasicStroke.JOIN_MITER;
    }

    /**
     * Sets the width of the line.
     *
     * @param  width  the width in pixels.
     * @return this builder.
     */
    public StrokeBuilder setWidth(final double width) {
        this.width = (width > 0) ? width : 0;
        return this;
    }

    /**
     * Returns the width of the line.
     *
     * @return the width in pixels, never negative.
     */
    public double getWidth() {
        return width;
    }

    /**
     * Sets the shape at the ends of the line.
     *
     * @param  cap  {@link BasicStroke#CAP_BUTT}, {@link BasicStroke#CAP_ROUND} or {@link BasicStroke#CAP_SQUARE}.
     * @return this builder.
     */
    public StrokeBuilder setCap(final int cap) {
        this.cap = cap;
        return this;
    }

    /**
     * Sets the shape at the corners of the line.
     *
     * @param  join  {@link BasicStroke#JOIN_MITER}, {@link BasicStroke#JOIN_ROUND} or {@link BasicStroke#JOIN_BEVEL}.
     * @return this builder.
     */
    public StrokeBuilder setJoin(final int join) {
        this.join = join;
        return this;
    }

    /**
     * Sets the dash pattern of the line.
     *
     * @param  lengths  lengths of the dashes and gaps in pixels, or {@code null} for a solid line.
     * @param  offset   distance in the pattern at which the line starts, in pixels.
     * @return this builder.
     */
    public StrokeBuilder setDashes(final double[] lengths, final double offset) {
        dashes = null;
        phase  = 0;
        if (lengths != null && lengths.length != 0) {
            final float[] pattern = new float[lengths.length];
            double period = 0;
            for (int i = 0; i < pattern.length; i++) {
                pattern[i] = (lengths[i] > 0) ? (float) lengths[i] : 0;
                period += pattern[i];
            }
            if (period > 0 && Double.isFinite(period)) {
                if ((pattern.length & 1) != 0) {
                    period *= 2;
                }
                double p = offset % period;
                if (p < 0) {
                    p += period;
                } else if (!(p >= 0)) {
                    p = 0;
                }
                dashes = pattern;
                phase  = (float) p;
            }
        }
        return this;
    }

    /**
     * Creates the stroke of the line, dashed if a dash pattern has been set.
     *
     * @return the stroke.
     */
    public BasicStroke build() {
        if (dashes != null) {
            return new BasicStroke((float) width, cap, join, MITER_LIMIT, dashes, phase);
        }
        return buildSolid(width);
    }

    /**
     * Creates a solid stroke of the given width with the cap and join of the line.
     * This is used for lines drawn along the main line, such as casings.
     *
     * @param  lineWidth  the width in pixels. Negative and NaN values are replaced by 0.
     * @return the stroke, without dashes.
     */
    public BasicStroke buildSolid(final double lineWidth) {
        return new BasicStroke((lineWidth > 0) ? (float) lineWidth : 0, cap, join, MITER_LIMIT);
    }
}
