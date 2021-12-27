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
package org.apache.sis.internal.feature.j2d;

import java.awt.geom.PathIterator;


/**
 * A path iterator with applies on-the-fly decimation for faster drawing.
 * The decimation algorithm is based on a simple distance calculation on
 * each axis (this is not a Douglas-Peucker algorithm).
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class DecimatedPathIterator implements PathIterator {
    /**
     * The source of line segments.
     */
    private final PathIterator source;

    /**
     * The desired resolution on each axis.
     */
    private final double xRes, yRes;

    /**
     * Previous coordinates, or NaN if none.
     */
    private double px, py;

    /**
     * Creates a new iterator.
     */
    DecimatedPathIterator(final PathIterator source, final double xRes, final double yRes) {
        this.source = source;
        this.xRes   = xRes;
        this.yRes   = yRes;
        px = py = Double.NaN;
    }

    /**
     * Moves the iterator to the next segment.
     */
    @Override
    public void next() {
        source.next();
    }

    /**
     * Returns {@code true} if iteration is finished.
     */
    @Override
    public boolean isDone() {
        return source.isDone();
    }

    /**
     * Returns the winding rule for determining the interior of the path.
     */
    @Override
    public int getWindingRule() {
        return source.getWindingRule();
    }

    /**
     * Returns the coordinates and type of the current path segment in the iteration.
     * This method has a fallback for quadratic and cubic curves, but this fallback
     * is not very good. This iterator should be used for flat shapes only.
     *
     * @param  coords an array where to store the data returned from this method.
     * @return the path-segment type of the current path segment.
     */
    @Override
    public int currentSegment(final double[] coords) {
        do {
            final int type = source.currentSegment(coords);
            switch (type) {
                default: {
                    px = py = Double.NaN;
                    return type;
                }
                case SEG_MOVETO: {
                    px = coords[0];
                    py = coords[1];
                    return SEG_MOVETO;
                }
                case SEG_LINETO: {
                    if (include(coords[0], coords[1])) {
                        return SEG_LINETO;
                    }
                    break;
                }
            }
            source.next();
        } while (!source.isDone());
        coords[0] = px;
        coords[1] = py;
        return SEG_LINETO;
    }

    /**
     * Returns the coordinates and type of the current path segment in the iteration.
     * This is a copy of {@link #currentSegment(double[])} with only the type changed.
     *
     * @param  coords an array where to store the data returned from this method.
     * @return the path-segment type of the current path segment.
     */
    @Override
    public int currentSegment(final float[] coords) {
        do {
            final int type = source.currentSegment(coords);
            switch (type) {
                default: {
                    px = py = Double.NaN;
                    return type;
                }
                case SEG_MOVETO: {
                    px = coords[0];
                    py = coords[1];
                    return SEG_MOVETO;
                }
                case SEG_LINETO: {
                    if (include(coords[0], coords[1])) {
                        return SEG_LINETO;
                    }
                    break;
                }
            }
            source.next();
        } while (!source.isDone());
        coords[0] = (float) px;
        coords[1] = (float) py;
        return SEG_LINETO;
    }

    /**
     * Returns whether the given point should be returned in a {@link #SEG_LINETO} segment.
     */
    private boolean include(final double x, final double y) {
        if (Math.abs(px - x) < xRes && Math.abs(py - y) < yRes) {
            return false;
        } else {
            px = x;
            py = y;
            return true;
        }
    }
}
