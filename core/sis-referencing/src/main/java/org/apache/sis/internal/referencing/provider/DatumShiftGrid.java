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
package org.apache.sis.internal.referencing.provider;

import java.util.Arrays;
import java.io.Serializable;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.referencing.operation.transform.CoordinateOffsets;


/**
 * Content of datum grid shift files (for example NTv2, NADCON or RGF93).
 * The shift may be in geographic (NTv2 and NADCON) or geocentric (RGF93) domain.
 * All {@code DatumShiftGrid} instances shall be immutable and thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
abstract class DatumShiftGrid implements CoordinateOffsets, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8405276545243175808L;

    /**
     * Coordinate of the center of the cell at index (0,0).
     * This is often the minimal <var>x</var> and <var>y</var> coordinate values,
     * but can be different if the {@link #Δx} or {@link #Δy} increments are negative.
     */
    final double x0, y0;

    /**
     * Coordinate of the center of the cell in the corner opposite to (x₀, y₀).
     * This is often the maximal <var>x</var> and <var>y</var> coordinate values,
     * but can be different if the {@link #Δx} or {@link #Δy} increments are negative.
     */
    final double xf, yf;

    /**
     * The increment between two cells along the <var>x</var> and <var>y</var> axes.
     */
    final double Δx, Δy;

    /**
     * Number of cells along the <var>x</var> and <var>y</var> axes.
     */
    final int width, height;

    /**
     * The grid values for the geodetic offsets.
     * This is an array of length 2 or 3 depending on the domain in which the offset is applied:
     *
     * <ul>
     *   <li>Length of 2 if the grid values are geographic translations among <var>λ</var>, <var>φ</var> axes.</li>
     *   <li>Length of 3 if the grid values are geocentric translations among <var>X</var>, <var>Y</var>, <var>Z</var> axes.</li>
     * </ul>
     */
    final float[][] offsets;

    /**
     * Creates a new grid initialized to the given grid geometry.
     *
     * @param  gridGeometry {<var>x₀</var>, <var>xf</var>, <var>y₀</var>, <var>yf</var>, <var>Δx</var>, <var>Δy</var>}.
     * @throws ArithmeticException if the width or the height exceed the integer capacity.
     */
    DatumShiftGrid(final int dimension, final double[] gridGeometry) {
        x0 = gridGeometry[0];
        xf = gridGeometry[1];
        y0 = gridGeometry[2];
        yf = gridGeometry[3];
        Δx = gridGeometry[4];
        Δy = gridGeometry[5];
        width  = (Δx != 0) ? Math.toIntExact(Math.round((xf - x0) / Δx + 1)) : 0;
        height = (Δy != 0) ? Math.toIntExact(Math.round((yf - y0) / Δy + 1)) : 0;
        final int size = Math.multiplyExact(width, height);
        offsets = new float[dimension][];
        for (int i=0; i<dimension; i++) {
            Arrays.fill(offsets[i] = new float[size], Float.NaN);
        }
    }

    /**
     * Computes the translation to apply for the given coordinate.
     *
     * @param  x The longitude value of the point for which to get the offset.
     * @param  y The latitude value of the point for which to get the offset.
     * @param  offsets A pre-allocated array where to write the offsets, or {@code null}.
     * @return The offset for the given point in the given {@code offsets} array if non-null, or in a new array otherwise.
     */
    @Override
    public final double[] interpolate(double x, double y, double[] dest) {
        final double i = Math.floor((x -= x0) / Δx);
        final double j = Math.floor((y -= y0) / Δy);
        x = x / Δx - i;
        y = y / Δy - j;
        final int p1 = Math.max(0, Math.min(height - 2, (int) j)) * width
                     + Math.max(0, Math.min(width  - 2, (int) i));
        final int p2 = p1 + width;
        if (dest == null) {
            dest = new double[offsets.length];
        }
        for (int k=0; k<offsets.length; k++) {
            final float[] t = offsets[k];
            dest[k] = (1-y) * ((1-x)*t[p1] + x*t[p1+1])
                       + y  * ((1-x)*t[p2] + x*t[p2+1]);
        }
        return dest;
    }

    /**
     * Returns {@code true} if the given grid contains the same data than this grid.
     */
    @Override
    public final boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            final DatumShiftGrid that = (DatumShiftGrid) other;
            return width  == that.width
                && height == that.height
                && Numerics.equals(x0, that.x0)
                && Numerics.equals(xf, that.xf)
                && Numerics.equals(y0, that.y0)
                && Numerics.equals(yf, that.yf)
                && Numerics.equals(Δx, that.Δx)
                && Numerics.equals(Δy, that.Δy)
                && Arrays.deepEquals(offsets, that.offsets);
        }
        return false;
    }

    /**
     * Implemented for consistency with {@link #equals(Object)}, but should not be invoked.
     */
    @Override
    public final int hashCode() {
        return Arrays.deepHashCode(offsets);
    }
}
