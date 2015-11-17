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
package org.apache.sis.referencing.datum;

import java.io.Serializable;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ArgumentChecks;


/**
 * Small but non-constant translations to apply on coordinates for datum shifts or other transformation process.
 * The main purpose of this class is to encapsulate the data provided by <cite>datum shift grid files</cite>
 * like NTv2, NADCON or RGF93. But this class could also be used for other kind of transformations,
 * provided that the shifts are <strong>small</strong> (otherwise algorithms may not converge).
 *
 * <p>{@linkplain DefaultGeodeticDatum Geodetic datum} changes impact directly two kinds of coordinates:
 * geographic and geocentric. Translations given by {@code DatumShiftGrid} instances are often, but not always,
 * applied directly on geographic coordinates (<var>λ</var>,<var>φ</var>). But some algorithms rather apply the
 * translations in geocentric coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>). This {@code DatumShiftGrid}
 * class can describe both cases, but will be used with different {@code MathTransform} implementations.</p>
 *
 * <div class="note"><b>Use cases:</b><ul>
 *   <li><p><b>Datum shift by geographic translations</b><br>
 *   For datum shifts using NADCON or NTv2 grids, the (<var>x</var>,<var>y</var>) arguments are longitude (λ)
 *   and latitude (φ) in angular <em>degrees</em> and the translations are (<var>Δλ</var>, <var>Δφ</var>)
 *   offsets in angular <em>seconds</em>, converted to degrees for Apache SIS needs. Those offsets will be
 *   added or subtracted by {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform}
 *   directly on the given (<var>λ</var>,<var>φ</var>) coordinates.
 *   </p></li>
 *
 *   <li><p><b>Datum shift by geocentric translations</b><br>
 *   For datum shifts in France, the (<var>x</var>,<var>y</var>) arguments are longitude and latitude in angular degrees
 *   but the translations are (<var>ΔX</var>, <var>ΔY</var>, <var>ΔZ</var>) geocentric offsets in <em>metres</em>.
 *   Those offsets will not be added directly to the given (<var>λ</var>,<var>φ</var>) coordinates since there is
 *   a Geographic/Geocentric conversion in the middle
 *   (see {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform}).
 *   </p></li>
 *
 *   <li><p><b>Localization grid of raster data</b><br>
 *   Some remote sensing raster data are provided with a <cite>localization grid</cite> giving pixel coordinates
 *   (e.g. latitude and longitude). This can been seen as a change from {@linkplain DefaultImageDatum image datum}
 *   to {@linkplain DefaultGeodeticDatum geodetic datum}. The coordinate transformation process can sometime be
 *   performed by a mathematical conversion (for example an affine transform) applied as a first approximation,
 *   followed by small corrections for the residual part. {@code DatumShiftGrid} can describe the small corrections part.
 *   </p></li>
 * </ul></div>
 *
 * <p>Implementations of this class shall be immutable and thread-safe.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class DatumShiftGrid implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8405276545243175808L;

    /**
     * Coordinate of the center of the cell at grid index (0,0).
     * The (<var>x₀</var>, <var>y₀</var>) coordinate is often the minimal (<var>x</var>,<var>y</var>) value
     * of the grid, but not necessarily if the <var>Δx</var> or <var>Δy</var> increments are negative.
     */
    protected final double x0, y0;

    /**
     * Multiplication factor for converting a coordinate into grid index.
     * The (<var>gx</var>, <var>gy</var>) index of a cell in the grid is given by:
     *
     * <ul>
     *   <li><var>gx</var> = (<var>x</var> - <var>x₀</var>) × {@code scaleX}</li>
     *   <li><var>gy</var> = (<var>y</var> - <var>y₀</var>) × {@code scaleY}</li>
     * </ul>
     */
    protected final double scaleX, scaleY;

    /**
     * Number of cells in the grid along the <var>x</var> and <var>y</var> axes.
     */
    protected final int nx, ny;

    /**
     * Creates a new grid.
     *
     * @param x0  First ordinate (often longitude) of the center of the cell at grid index (0,0).
     * @param y0  Second ordinate (often latitude) of the center of the cell at grid index (0,0).
     * @param Δx  Increment in <var>x</var> value between cells at index <var>gridX</var> and <var>gridX</var> + 1.
     * @param Δy  Increment in <var>y</var> value between cells at index <var>gridY</var> and <var>gridY</var> + 1.
     * @param nx  Number of cells along the <var>x</var> axis in the grid.
     * @param ny  Number of cells along the <var>y</var> axis in the grid.
     */
    protected DatumShiftGrid(final double x0, final double y0,
                             final double Δx, final double Δy,
                             final int    nx, final int    ny)
    {
        this.x0 = x0;
        this.y0 = y0;
        this.nx = nx;
        this.ny = ny;
        ArgumentChecks.ensureStrictlyPositive("nx", nx);
        ArgumentChecks.ensureStrictlyPositive("ny", ny);
        /*
         * The intend of DoubleDouble arithmetic here is to avoid rounding errors on the assumption that
         * the Δx and Δy values are defined in base 10 in the grid. For example 1 / 1E-5 gives 99999.9…,
         * while DoubleDouble below gives the expected result.  Since we use Math.floor(double) for grid
         * indices computation, the difference is significant.
         */
        final DoubleDouble dd = new DoubleDouble(Δx);
        dd.inverseDivide(1, 0);
        scaleX = dd.value;
        dd.value = Δy;
        dd.error = DoubleDouble.errorForWellKnownValue(Δy);
        dd.inverseDivide(1, 0);
        scaleY = dd.value;
    }

    /**
     * Returns the domain of validity of the (<var>x</var>,<var>y</var>) coordinates that can be specified to the
     * {@link #offsetAt(double, double, double[])} method. Coordinates outside that domain of validity will still
     * be accepted, but the result of offset computation may be very wrong.
     *
     * @return The domain covered by this grid.
     */
    public Envelope getDomainOfValidity() {
        final Envelope2D domain = new Envelope2D(null, x0 - 0.5/scaleX, y0 - 0.5/scaleY, nx/scaleX, ny/scaleY);
        if (domain.width < 0) {
            domain.width = -domain.width;
            domain.x += domain.width;
        }
        if (domain.height < 0) {
            domain.height = -domain.height;
            domain.y += domain.height;
        }
        return domain;
    }

    /**
     * Interpolates the translation to apply for the given coordinate.
     * This method usually returns an array of length 2 or 3, but it could be of any length
     * (provided that this length never change). The values in the returned array are often
     * for the same dimensions than <var>x</var> and <var>y</var>, but not necessarily.
     * See the class javadoc for use cases.
     *
     * <div class="section">Default implementation</div>
     * The default implementation performs the following steps for each dimension {@code dim},
     * where the number of dimension is determined by the length of the {@code offsets} array:
     * <ol>
     *   <li>Convert the given (<var>x</var>,<var>y</var>) coordinate into grid coordinate
     *       using the formula documented in {@link #scaleX} and {@link #scaleY} fields.<li>
     *   <li>Clamp the grid coordinate into the ([0 … {@link #nx}-2], [0 … {@link #ny}-2]) range inclusive.</li>
     *   <li>Using {@link #getCellValue(int, int, int)}, get the four cell values around the coordinate.</li>
     *   <li>Apply a bilinear interpolation and store the result in {@code offset[dim]}.</li>
     * </ol>
     *
     * @param x       First ordinate (often longitude, but not necessarily) of the point for which to get the offset.
     * @param y       Second ordinate (often latitude, but not necessarily) of the point for which to get the offset.
     * @param offset  A pre-allocated array where to write the offset vector.
     */
    public void offsetAt(double x, double y, double[] offset) {
        final int gridX = Math.max(0, Math.min(nx - 2, (int) Math.floor(x = (x - x0) * scaleX)));
        final int gridY = Math.max(0, Math.min(ny - 2, (int) Math.floor(y = (y - y0) * scaleY)));
        x -= gridX;
        y -= gridY;
        for (int dim=0; dim<offset.length; dim++) {
            offset[dim] = (1-y) * ((1-x) * getCellValue(dim, gridX, gridY  ) + x*getCellValue(dim, gridX+1, gridY  ))
                           + y  * ((1-x) * getCellValue(dim, gridX, gridY+1) + x*getCellValue(dim, gridX+1, gridY+1));
        }
    }

    /**
     * Returns the offset stored at the given grid indices along the given dimension.
     *
     * @param dim    The dimension of the offset component to get.
     * @param gridX  The grid index along the <var>x</var> axis, from 0 inclusive to {@link #nx} exclusive.
     * @param gridY  The grid index along the <var>y</var> axis, from 0 inclusive to {@link #ny} exclusive.
     * @return The offset at the given dimension in the grid cell at the given index.
     */
    protected abstract double getCellValue(int dim, int gridX, int gridY);

    /**
     * Returns {@code true} if the given grid contains the same data than this grid.
     *
     * @param  other The other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, of the same class than this {@code DatumShiftGrid}
     *         and contains the same data.
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            final DatumShiftGrid that = (DatumShiftGrid) other;
            return nx == that.nx
                && ny == that.ny
                && Numerics.equals(x0,     that.x0)
                && Numerics.equals(y0,     that.y0)
                && Numerics.equals(scaleX, that.scaleX)
                && Numerics.equals(scaleY, that.scaleY);
        }
        return false;
    }

    /**
     * Returns a hash code value for this datum shift grid.
     */
    @Override
    public int hashCode() {
        return Numerics.hashCode(
                 Double.doubleToLongBits(x0) + 31*
                (Double.doubleToLongBits(y0) + 31*
                (Double.doubleToLongBits(scaleX) + 31*
                 Double.doubleToLongBits(scaleY))));
    }
}
