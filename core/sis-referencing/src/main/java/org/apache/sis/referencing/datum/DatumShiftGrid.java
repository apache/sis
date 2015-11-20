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
 * <div class="note"><b>Use cases:</b>
 * NADCON, NTv2 and other grids are used in the contexts described below. But be aware of units of measurement!
 * In particular, input geographic coordinates (λ,φ) need to be in <strong>radians</strong> for use with SIS
 * {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform} implementation.
 * <ul>
 *   <li><p><b>Datum shift by geographic translations</b><br>
 *   NADCON and NTv2 grids are defined with longitude (λ) and latitude (φ) inputs in angular <em>degrees</em>
 *   and give (<var>Δλ</var>, <var>Δφ</var>) translations in angular <em>seconds</em>.
 *   However Apache SIS needs all those values to be converted to <strong>radians</strong>.
 *   The translations will be applied by {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform}
 *   directly on the given (<var>λ</var>,<var>φ</var>) coordinates.
 *   </p></li>
 *
 *   <li><p><b>Datum shift by geocentric translations</b><br>
 *   France interpolation grid is defined with longitude (λ) and latitude (φ) inputs in angular <em>degrees</em>
 *   and gives (<var>ΔX</var>, <var>ΔY</var>, <var>ΔZ</var>) geocentric translations in <em>metres</em>.
 *   Those offsets will not be added directly to the given (<var>λ</var>,<var>φ</var>) coordinates since there is
 *   a geographic/geocentric conversion in the middle
 *   (see {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform}).
 *   </p></li>
 *
 *   <li><p><b>Localization grid of raster data</b><br>
 *   Some remote sensing raster data are provided with a <cite>localization grid</cite> giving pixel coordinates
 *   (e.g. latitude and longitude). This can been seen as a change from {@linkplain DefaultImageDatum image datum}
 *   to {@linkplain DefaultGeodeticDatum geodetic datum}. The coordinate transformation process can sometime be
 *   performed by a mathematical conversion (for example an affine transform) applied as a
 *   {@linkplain org.apache.sis.referencing.operation.builder.LinearTransformBuilder first approximation},
 *   followed by small corrections for the residual part.
 *   {@code DatumShiftGrid} can describe the small corrections part.
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
     * This is usually a geographic coordinate (<var>λ₀</var>,<var>φ₀</var>) in <strong>radians</strong>,
     * but other usages are allowed for users who instantiate
     * {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform} themselves.
     * The (<var>x₀</var>, <var>y₀</var>) coordinate is often the minimal (<var>x</var>,<var>y</var>) value
     * of the grid, but not necessarily if the <var>Δx</var> or <var>Δy</var> increments are negative.
     */
    protected final double x0, y0;

    /**
     * Multiplication factor for converting a coordinate into grid index.
     * The (<var>gridX</var>, <var>gridY</var>) indices of a cell in the grid are given by:
     *
     * <ul>
     *   <li><var>gridX</var> = (<var>x</var> - <var>x₀</var>) ⋅ {@code scaleX}</li>
     *   <li><var>gridY</var> = (<var>y</var> - <var>y₀</var>) ⋅ {@code scaleY}</li>
     * </ul>
     */
    protected final double scaleX, scaleY;

    /**
     * Number of cells in the grid along the <var>x</var> and <var>y</var> axes.
     */
    protected final int nx, ny;

    /**
     * Creates a new datum shift grid for the given grid geometry.
     * The actual offset values need to be provided by subclasses.
     *
     * @param x0  First ordinate (often longitude in radians) of the center of the cell at grid index (0,0).
     * @param y0  Second ordinate (often latitude in radians) of the center of the cell at grid index (0,0).
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
        scaleX = 1 / Δx;
        scaleY = 1 / Δy;
    }

    /**
     * Creates a new datum shift grid with the same grid geometry than the given grid.
     *
     * @param other The other datum shift grid from which to copy the grid geometry.
     */
    protected DatumShiftGrid(final DatumShiftGrid other) {
        x0 = other.x0;
        y0 = other.y0;
        nx = other.nx;
        ny = other.ny;
        scaleX = other.scaleX;
        scaleY = other.scaleY;
    }

    /**
     * Returns the number of dimensions of the translation vectors interpolated by this datum shift grid.
     * The number of dimension is usually 2 or 3, but other values are allowed.
     *
     * @return Number of dimensions of translation vectors.
     */
    public abstract int getShiftDimensions();

    /**
     * Returns the domain of validity of the (<var>x</var>,<var>y</var>) coordinate values that can be specified
     * to the {@link #offsetAt offsetAt(x, y, …)} method. Coordinates outside that domain of validity will still
     * be accepted, but the result of offset computation may be very wrong.
     *
     * <p>In datum shift grids used by {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform},
     * the domain of validity is always expressed as longitudes and latitudes in <strong>radians</strong>.
     * The envelope is usually in radians for simpler (non-geocentric) interpolations too, for consistency reasons.</p>
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
     * Returns an average offset value for the given dimension.
     * Those average values shall provide a good "first guess" before to interpolate the actual offset value
     * at the (<var>x</var>,<var>y</var>) coordinate. This "first guess" is needed for inverse transform.
     *
     * <div class="section">Default implementation</div>
     * The default implementation computes the average of all values returned by
     * {@link #getCellValue getCellValue(dim, …)}, but subclasses may override with more specific values.
     *
     * <div class="note"><b>Example:</b>
     * In the <cite>"France geocentric interpolation"</cite> (ESPG:9655) operation method, those "average" values
     * are fixed by definition to -168, -60 and +320 metres for dimensions 0, 1 and 2 respectively
     * (geocentric <var>X</var>, <var>Y</var> and <var>Z</var>).</div>
     *
     * @param dim  The dimension for which to get an average value,
     *             from 0 inclusive to {@link #getShiftDimensions()} exclusive.
     * @return A value close to the average for the given dimension.
     */
    public double getAverageOffset(final int dim) {
        final DoubleDouble sum = new DoubleDouble();
        for (int gridY=0; gridY<ny; gridY++) {
            for (int gridX=0; gridX<nx; gridX++) {
                sum.add(getCellValue(dim, gridX, gridY));
            }
        }
        return sum.value / (nx * ny);
    }

    /**
     * Interpolates the translation to apply for the given coordinate. The result is stored in the
     * given {@code offsets} array, which shall have a length of at least {@link #getShiftDimensions()}.
     * The computed translation values are often for the same dimensions than the given <var>x</var> and <var>y</var>
     * values, but not necessarily.
     * See the class javadoc for use cases.
     *
     * <div class="section">Default implementation</div>
     * The default implementation performs the following steps for each dimension {@code dim},
     * where the number of dimension is determined by {@link #getShiftDimensions()}.
     * <ol>
     *   <li>Convert the given (<var>x</var>,<var>y</var>) coordinate into grid coordinate
     *       using the formula documented in {@link #scaleX} and {@link #scaleY} fields.</li>
     *   <li>Clamp the grid coordinate into the [0 … {@link #nx} - 2] and [0 … {@link #ny} - 2] ranges, inclusive.</li>
     *   <li>Using {@link #getCellValue(int, int, int)}, get the four cell values around the coordinate.</li>
     *   <li>Apply a bilinear interpolation and store the result in {@code offsets[dim]}.</li>
     * </ol>
     *
     * @param x        First ordinate (often longitude, but not necessarily) of the point for which to get the offset.
     * @param y        Second ordinate (often latitude, but not necessarily) of the point for which to get the offset.
     * @param offsets  A pre-allocated array where to write the translation vector.
     */
    public void offsetAt(double x, double y, double[] offsets) {
        final int gridX = Math.max(0, Math.min(nx - 2, (int) Math.floor(x = (x - x0) * scaleX)));
        final int gridY = Math.max(0, Math.min(ny - 2, (int) Math.floor(y = (y - y0) * scaleY)));
        x -= gridX;
        y -= gridY;
        final int n = getShiftDimensions();
        for (int dim = 0; dim < n; dim++) {
            offsets[dim] = (1-y) * ((1-x)*getCellValue(dim, gridX, gridY  ) + x*getCellValue(dim, gridX+1, gridY  ))
                            + y  * ((1-x)*getCellValue(dim, gridX, gridY+1) + x*getCellValue(dim, gridX+1, gridY+1));
        }
    }

    /**
     * Returns the offset stored at the given grid indices along the given dimension.
     *
     * @param dim    The dimension of the offset component to get,
     *               from 0 inclusive to {@link #getShiftDimensions()} exclusive.
     * @param gridX  The grid index along the <var>x</var> axis, from 0 inclusive to {@link #nx} exclusive.
     * @param gridY  The grid index along the <var>y</var> axis, from 0 inclusive to {@link #ny} exclusive.
     * @return The offset at the given dimension in the grid cell at the given index.
     */
    protected abstract double getCellValue(int dim, int gridX, int gridY);

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
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
     * This method does not need to compute a hash code from all grid values.
     * Comparing some metadata like the grid filename is considered sufficient
     *
     * @return A hash code based on metadata.
     */
    @Override
    public int hashCode() {
        return Numerics.hashCode(Double.doubleToLongBits(x0)
                         + 31 * (Double.doubleToLongBits(y0)
                         + 31 * (Double.doubleToLongBits(scaleX)
                         + 31 *  Double.doubleToLongBits(scaleY))))
                         + 37 * nx + ny;
    }
}
