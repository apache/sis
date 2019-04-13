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

import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import javax.measure.Unit;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.Units;


/**
 * Small but non-constant translations to apply on coordinates for datum shifts or other transformation process.
 * The main purpose of this class is to encapsulate the data provided by <cite>datum shift grid files</cite>
 * like NTv2, NADCON or RGF93. But this class could also be used for other kind of transformations,
 * provided that the shifts are relatively small (otherwise algorithms may not converge).
 *
 * <p>{@linkplain DefaultGeodeticDatum Geodetic datum} changes can be implemented by translations in geographic
 * or geocentric coordinates. Translations given by {@code DatumShiftGrid} instances are often, but not always,
 * applied directly on geographic coordinates (<var>λ</var>,<var>φ</var>). But some algorithms rather apply the
 * translations in geocentric coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>). This {@code DatumShiftGrid}
 * class can describe both cases, but will be used with different {@code MathTransform} implementations.</p>
 *
 * <p>Steps for calculation of a translation vector:</p>
 * <ol>
 *   <li>Coordinates are given in some "real world" unit.
 *       The expected unit is given by {@link #getCoordinateUnit()}.</li>
 *   <li>Above coordinates are converted to grid indices including fractional parts.
 *       That conversion is given by {@link #getCoordinateToGrid()}.</li>
 *   <li>Translation vectors are interpolated at the position of above grid indices.
 *       That interpolation is done by {@link #interpolateInCell interpolateInCell(…)}.</li>
 *   <li>If the above translations were given as a ratio of the real translation divided by the size of grid cells, apply
 *       the inverse of the conversion given at step 2. This information is given by {@link #isCellValueRatio()}.</li>
 *   <li>The resulting translation vectors are in the unit given by {@link #getTranslationUnit()}.</li>
 * </ol>
 *
 * The {@link #interpolateAt interpolateAt(…)} method performs all those steps.
 * But that method is provided only for convenience; it is not used by Apache SIS.
 * For performance reasons SIS {@code MathTransform} implementations perform all the above-cited steps themselves,
 * and apply the interpolated translations on coordinate values in their own step between above steps 3 and 4.
 *
 * <div class="note"><b>Use cases:</b>
 * <ul class="verbose">
 *   <li><b>Datum shift by geographic translations</b><br>
 *   NADCON and NTv2 grids are defined with longitude (<var>λ</var>) and latitude (<var>φ</var>) inputs in angular
 *   <em>degrees</em> and give (<var>Δλ</var>, <var>Δφ</var>) translations in angular <em>seconds</em>.
 *   However SIS stores the translation values in units of grid cell rather than angular seconds.
 *   The translations will be applied by {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform}
 *   directly on the given (<var>λ</var>,<var>φ</var>) coordinates.
 *   </li>
 *
 *   <li><b>Datum shift by geocentric translations</b><br>
 *   France interpolation grid is defined with longitude (<var>λ</var>) and latitude (<var>φ</var>) inputs in angular
 *   <em>degrees</em> and gives (<var>ΔX</var>, <var>ΔY</var>, <var>ΔZ</var>) geocentric translations in <em>metres</em>.
 *   Those translations will not be added directly to the given (<var>λ</var>,<var>φ</var>) coordinates since there is
 *   a geographic/geocentric conversion in the middle
 *   (see {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform}).
 *   </li>
 *
 *   <li><b>Localization grid of raster data</b><br>
 *   Some remote sensing raster data are provided with a <cite>localization grid</cite> giving pixel coordinates
 *   (e.g. latitude and longitude). This can been seen as a change from {@linkplain DefaultImageDatum image datum}
 *   to {@linkplain DefaultGeodeticDatum geodetic datum}. The coordinate transformation process can sometime be
 *   performed by a mathematical conversion (for example an affine transform) applied as a
 *   {@linkplain org.apache.sis.referencing.operation.builder.LinearTransformBuilder first approximation},
 *   followed by small corrections for the residual part.
 *   {@code DatumShiftGrid} can describe the small corrections part.
 *   </li>
 * </ul></div>
 *
 * Implementations of this class shall be immutable and thread-safe.
 *
 * <div class="section">Number of dimensions</div>
 * Input coordinates and translation vectors can have any number of dimensions. However in the current implementation,
 * only the two first dimensions are used for interpolating the translation vectors. This restriction appears in the
 * following field and method signatures:
 *
 * <ul>
 *   <li>{@link #INTERPOLATED_DIMENSIONS}.</li>
 *   <li>{@link #getCellValue(int, int, int)}
 *       where the two last {@code int} values are (<var>x</var>,<var>y</var>) grid indices.</li>
 *   <li>{@link #interpolateInCell(double, double, double[])}
 *       where the two first {@code double} values are (<var>x</var>,<var>y</var>) grid indices.</li>
 *   <li>{@link #derivativeInCell(double, double)}
 *       where the values are (<var>x</var>,<var>y</var>) grid indices.</li>
 * </ul>
 *
 * Note that the above restriction does not prevent {@code DatumShiftGrid} to interpolate translation vectors
 * in more than two dimensions. See the above <cite>datum shift by geocentric translations</cite> use case for
 * an example.
 *
 * <div class="section">Serialization</div>
 * Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 * Serialization support is appropriate for short term storage or RMI between applications running the
 * same version of Apache SIS. But for long term storage, an established datum shift grid format like
 * NTv2 should be preferred.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param <C>  dimension of the coordinate unit (usually {@link javax.measure.quantity.Angle}).
 * @param <T>  dimension of the translation unit (usually {@link javax.measure.quantity.Angle}
 *             or {@link javax.measure.quantity.Length}).
 *
 * @see org.apache.sis.referencing.operation.transform.DatumShiftTransform
 *
 * @since 0.7
 * @module
 */
public abstract class DatumShiftGrid<C extends Quantity<C>, T extends Quantity<T>> implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8405276545243175808L;

    /**
     * Number of source dimensions in which interpolations are applied. The grids may have more dimensions,
     * but only this number of dimensions will be used in interpolations. The value of this field is set to
     * {@value}. That value is hard-coded not only in this field, but also in signature of various methods
     * expecting a two-dimensional (<var>x</var>, <var>y</var>) position:
     * <code>{@linkplain #getCellValue(int, int, int) getCellValue}(…, x, y)</code>,
     * <code>{@linkplain #interpolateInCell(double, double, double[]) interpolateInCell}(x, y, …)</code>,
     * <code>{@linkplain #derivativeInCell(double, double) derivativeInCell}(x, y)</code>.
     *
     * <div class="note"><b>Future evolution:</b>
     * if this class is generalized to more source dimensions in a future Apache SIS version, then this field
     * may be deprecated or its value changed. That change would be accompanied by new methods with different
     * signature. This field can be used as a way to detect that such change occurred.</div>
     *
     * @since 1.0
     */
    protected static final int INTERPOLATED_DIMENSIONS = 2;

    /**
     * The unit of measurements of input values, before conversion to grid indices by {@link #coordinateToGrid}.
     * The coordinate unit is typically {@link org.apache.sis.measure.Units#DEGREE}.
     *
     * @see #getCoordinateUnit()
     */
    private final Unit<C> coordinateUnit;

    /**
     * Conversion from the "real world" coordinates to grid indices including fractional parts.
     * This is the conversion that needs to be applied before to interpolate.
     *
     * @see #getCoordinateToGrid()
     */
    private final LinearTransform coordinateToGrid;

    /**
     * The unit of measurement of output values, as interpolated by the {@link #interpolateAt} method.
     *
     * @see #getTranslationUnit()
     */
    private final Unit<T> translationUnit;

    /**
     * {@code true} if the translation interpolated by {@link #interpolateInCell interpolateInCell(…)}
     * are divided by grid cell size. If {@code true}, then the inverse of {@link #coordinateToGrid}
     * needs to be applied on the interpolated values as a delta transform.
     * Such conversion is applied (if needed) by the {@link #interpolateAt} method.
     *
     * @see #isCellValueRatio()
     */
    private final boolean isCellValueRatio;

    /**
     * Number of grid cells along each dimension. This is usually an array of length {@value #INTERPOLATED_DIMENSIONS}
     * containing the number of grid cells along the <var>x</var> and <var>y</var> axes.
     */
    private final int[] gridSize;

    /**
     * Creates a new datum shift grid for the given size and units.
     * The actual cell values need to be provided by subclasses.
     *
     * <p>Meaning of argument values is documented more extensively in {@link #getCoordinateUnit()},
     * {@link #getCoordinateToGrid()}, {@link #isCellValueRatio()} and {@link #getTranslationUnit()}
     * methods. The argument order is roughly the order in which they are used in the process of
     * interpolating translation vectors.</p>
     *
     * @param  coordinateUnit    the unit of measurement of input values, before conversion to grid indices by {@code coordinateToGrid}.
     * @param  coordinateToGrid  conversion from the "real world" coordinates to grid indices including fractional parts.
     * @param  gridSize          number of cells along each axis in the grid. The length of this array shall be equal to {@code coordinateToGrid} target dimensions.
     * @param  isCellValueRatio  {@code true} if results of {@link #interpolateInCell interpolateInCell(…)} are divided by grid cell size.
     * @param  translationUnit   the unit of measurement of output values.
     */
    protected DatumShiftGrid(final Unit<C> coordinateUnit, final LinearTransform coordinateToGrid,
            int[] gridSize, final boolean isCellValueRatio, final Unit<T> translationUnit)
    {
        ArgumentChecks.ensureNonNull("coordinateUnit",   coordinateUnit);
        ArgumentChecks.ensureNonNull("coordinateToGrid", coordinateToGrid);
        ArgumentChecks.ensureNonNull("gridSize",         gridSize);
        ArgumentChecks.ensureNonNull("translationUnit",  translationUnit);
        int n = coordinateToGrid.getTargetDimensions();
        ArgumentChecks.ensureDimensionMatches("gridSize", n, gridSize);
        this.coordinateUnit   = coordinateUnit;
        this.coordinateToGrid = coordinateToGrid;
        this.isCellValueRatio = isCellValueRatio;
        this.translationUnit  = translationUnit;
        this.gridSize = gridSize = gridSize.clone();
        for (int i=0; i<gridSize.length; i++) {
            if ((n = gridSize[i]) < 2) {
                throw new IllegalArgumentException(Errors.format(n <= 0
                        ? Errors.Keys.ValueNotGreaterThanZero_2
                        : Errors.Keys.IllegalArgumentValue_2, Strings.toIndexed("gridSize", i), n));
            }
        }
    }

    /**
     * Creates a new datum shift grid with the same grid geometry (size and units) than the given grid.
     *
     * @param  other  the other datum shift grid from which to copy the grid geometry.
     */
    protected DatumShiftGrid(final DatumShiftGrid<C,T> other) {
        ArgumentChecks.ensureNonNull("other", other);
        coordinateUnit   = other.coordinateUnit;
        coordinateToGrid = other.coordinateToGrid;
        isCellValueRatio = other.isCellValueRatio;
        translationUnit  = other.translationUnit;
        gridSize         = other.gridSize;
    }

    /**
     * Returns the number of cells along each axis in the grid.
     * The length of this array is the number of grid dimensions, which is typically {@value #INTERPOLATED_DIMENSIONS}.
     * The grid dimensions shall be equal to {@link #getCoordinateToGrid() coordinateToGrid} target dimensions.
     *
     * <div class="note"><b>Note:</b> the number of grid dimensions is not necessarily equal to the
     * {@linkplain #getTranslationDimensions() number of dimension of the translation vectors}.</div>
     *
     * @return the number of cells along each axis in the grid.
     */
    public int[] getGridSize() {
        return gridSize.clone();
    }

    /**
     * Returns the domain of validity of input coordinates that can be specified to the
     * {@link #interpolateAt interpolateAt(…)} method. Coordinates outside that domain of
     * validity will still be accepted, but the extrapolated results may be very wrong.
     *
     * <p>The unit of measurement for the coordinate values in the returned envelope is
     * given by {@link #getCoordinateUnit()}. The envelope CRS is undefined.</p>
     *
     * @return the domain covered by this grid.
     * @throws TransformException if an error occurred while computing the envelope.
     */
    public Envelope getDomainOfValidity() throws TransformException {
        final GeneralEnvelope env = new GeneralEnvelope(gridSize.length);
        for (int i=0; i<gridSize.length; i++) {
            env.setRange(i, -0.5, gridSize[i] - 0.5);
        }
        return Envelopes.transform(getCoordinateToGrid().inverse(), env);
    }

    /**
     * Returns the unit of measurement of input values, before conversion to grid indices.
     * The coordinate unit is usually {@link Units#DEGREE}, but other units are allowed.
     *
     * @return the unit of measurement of input values before conversion to grid indices.
     *
     * @see #getTranslationUnit()
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation#getInterpolationCRS()
     */
    public Unit<C> getCoordinateUnit() {
        return coordinateUnit;
    }

    /**
     * Returns the conversion from the source coordinates (in "real world" units) to grid indices.
     * The input coordinates given to the {@link LinearTransform} shall be in the unit of measurement
     * given by {@link #getCoordinateUnit()}. The output coordinates are grid indices as real numbers
     * (i.e. can have a fractional part). Integer grid indices are located in the center of grid cells,
     * i.e. the transform uses {@link org.opengis.referencing.datum.PixelInCell#CELL_CENTER} convention.
     *
     * <p>This transform is usually two-dimensional, in which case conversions from (<var>x</var>,<var>y</var>)
     * coordinates to ({@code gridX}, {@code gridY}) indices can be done with the following formulas:</p>
     * <ul>
     *   <li><var>gridX</var> = (<var>x</var> - <var>x₀</var>) / <var>Δx</var></li>
     *   <li><var>gridY</var> = (<var>y</var> - <var>y₀</var>) / <var>Δy</var></li>
     * </ul>
     *
     * where:
     * <ul>
     *   <li>(<var>x₀</var>, <var>y₀</var>) is the coordinate of the center of the cell at grid index (0,0).</li>
     *   <li><var>Δx</var> and <var>Δy</var> are the distances between two cells on the <var>x</var> and <var>y</var>
     *       axes respectively, in the unit of measurement given by {@link #getCoordinateUnit()}.</li>
     * </ul>
     *
     * The {@code coordinateToGrid} transform for the above formulas can be represented by the following matrix:
     *
     * {@preformat math
     *   ┌                      ┐
     *   │ 1/Δx      0   -x₀/Δx │
     *   │    0   1/Δy   -y₀/Δy │
     *   │    0      0        1 │
     *   └                      ┘
     * }
     *
     * @return conversion from the "real world" coordinates to grid indices including fractional parts.
     */
    public LinearTransform getCoordinateToGrid() {
        return coordinateToGrid;
    }

    /**
     * Returns the number of dimensions of the translation vectors interpolated by this datum shift grid.
     * This number of dimensions is not necessarily equals to the number of source or target dimensions
     * of the "{@linkplain #getCoordinateToGrid() coordinate to grid}" transform.
     * The number of translation dimensions is usually 2 or 3, but other values are allowed.
     *
     * @return number of dimensions of translation vectors.
     */
    public abstract int getTranslationDimensions();

    /**
     * Returns the unit of measurement of output values, as interpolated by the {@code interpolateAt(…)} method.
     * Apache SIS {@code MathTransform} implementations restrict the translation units to the following values:
     *
     * <ul>
     *   <li>For {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform}, the translation
     *       unit shall be the same than the {@linkplain #getCoordinateUnit() coordinate unit}.</li>
     *   <li>For {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform},
     *       the translation unit shall be the same than the unit of source ellipsoid axis lengths.</li>
     * </ul>
     *
     * @return the unit of measurement of output values interpolated by {@code interpolateAt(…)}.
     *
     * @see #getCoordinateUnit()
     * @see #interpolateAt
     */
    public Unit<T> getTranslationUnit() {
        return translationUnit;
    }

    /**
     * Interpolates the translation to apply for the given coordinates.
     * The input values are in the unit given by {@link #getCoordinateUnit()}.
     * The output values are in the unit given by {@link #getTranslationUnit()}.
     * The length of the returned array is given by {@link #getTranslationDimensions()}.
     *
     * <div class="section">Default implementation</div>
     * The default implementation performs the following steps:
     * <ol>
     *   <li>Convert the given coordinate into grid indices using the transform given by {@link #getCoordinateToGrid()}.</li>
     *   <li>Interpolate the translation vector at the above grid indices with a call to {@link #interpolateInCell}.</li>
     *   <li>If {@link #isCellValueRatio()} returns {@code true}, {@linkplain LinearTransform#deltaTransform delta transform}
     *       the translation vector by the inverse of the conversion given at step 1.</li>
     * </ol>
     *
     * If the give point is outside this grid, then this method returns the vector at the closest position in the grid
     * as documented in {@link #interpolateInCell(double, double, double[])}.
     *
     * @param  coordinates  the "real world" coordinate (often longitude and latitude, but not necessarily)
     *                      of the point for which to get the translation.
     * @return the translation vector at the given position.
     * @throws TransformException if an error occurred while computing the translation vector.
     */
    public double[] interpolateAt(final double... coordinates) throws TransformException {
        final LinearTransform c = getCoordinateToGrid();
        ArgumentChecks.ensureDimensionMatches("coordinates", c.getSourceDimensions(), coordinates);
        final int dim = getTranslationDimensions();
        double[] vector = new double[Math.max(dim, c.getTargetDimensions())];
        c.transform(coordinates, 0, vector, 0, 1);
        interpolateInCell(vector[0], vector[1], vector);
        if (isCellValueRatio()) {
            c.inverse().deltaTransform(vector, 0, vector, 0, 1);
        }
        if (vector.length != dim) {
            vector = Arrays.copyOf(vector, dim);
        }
        return vector;
    }

    /**
     * Interpolates the translation to apply for the given two-dimensional grid indices. The result is stored in
     * the given {@code vector} array, which shall have a length of at least {@link #getTranslationDimensions()}.
     * The output unit of measurement is the same than the one documented in {@link #getCellValue(int, int, int)}.
     *
     * <div class="section">Extrapolations</div>
     * If the given coordinates are outside this grid, then this method computes the translation vector at the
     * closest position in the grid. Applying translations on points outside the grid is a kind of extrapolation,
     * but some amount of extrapolations are necessary for operations like transforming an envelope before to compute
     * its intersection with another envelope.
     *
     * <div class="section">Derivative (Jacobian matrix)</div>
     * If the length of the given array is at least <var>n</var> + 4 where <var>n</var> = {@link #getTranslationDimensions()},
     * then this method appends the derivative (approximated) at the given grid indices. This is the same derivative than the
     * one computed by {@link #derivativeInCell(double, double)}, opportunistically computed here for performance reasons.
     * The matrix layout is as below, where <var>u</var> and <var>v</var> are the coordinates after translation.
     *
     * {@preformat math
     *   ┌                 ┐         ┌                             ┐
     *   │  ∂u/∂x   ∂u/∂y  │    =    │  vector[n+0]   vector[n+1]  │
     *   │  ∂v/∂x   ∂v/∂y  │         │  vector[n+2]   vector[n+3]  │
     *   └                 ┘         └                             ┘
     * }
     *
     * <div class="section">Default implementation</div>
     * The default implementation performs the following steps for each dimension <var>dim</var>,
     * where the number of dimension is determined by {@link #getTranslationDimensions()}.
     *
     * <ol>
     *   <li>Clamp the {@code gridX} index into the [0 … {@code gridSize[0]} - 1] range, inclusive.</li>
     *   <li>Clamp the {@code gridY} index into the [0 … {@code gridSize[1]} - 1] range, inclusive.</li>
     *   <li>Using {@link #getCellValue}, get the cell values around the given indices.</li>
     *   <li>Apply a bilinear interpolation and store the result in {@code vector[dim]}.</li>
     *   <li>Appends Jacobian matrix coefficients if the {@code vector} length is sufficient.</li>
     * </ol>
     *
     * @param  gridX   first grid coordinate of the point for which to get the translation.
     * @param  gridY   second grid coordinate of the point for which to get the translation.
     * @param  vector  a pre-allocated array where to write the translation vector.
     *
     * @see #isCellInGrid(double, double)
     */
    public void interpolateInCell(double gridX, double gridY, final double[] vector) {
        if (gridX < 0) gridX = 0;
        if (gridY < 0) gridY = 0;
        int ix = (int) gridX;  gridX -= ix;
        int iy = (int) gridY;  gridY -= iy;
        int n;
        if (ix > (n = gridSize[0] - 2)) {
            ix = n;
            gridX = 1;
        }
        if (iy > (n = gridSize[1] - 2)) {
            iy = n;
            gridY = 1;
        }
        n = getTranslationDimensions();
        boolean derivative = (vector.length >= n + INTERPOLATED_DIMENSIONS * INTERPOLATED_DIMENSIONS);
        for (int dim = 0; dim < n; dim++) {
            double dx;
            final double r00 = getCellValue(dim, ix,   iy  );
            final double r01 = getCellValue(dim, ix+1, iy  );       // Naming convention: ryx (row index first, like matrix).
            final double r10 = getCellValue(dim, ix,   iy+1);
            final double r11 = getCellValue(dim, ix+1, iy+1);
            final double r0x = r00 + gridX * (dx = r01 - r00);                          // TODO: use Math.fma on JDK9.
            final double r1x = r10 + gridX * (     r11 - r10);
            vector[dim] = gridY * (r1x - r0x) + r0x;
            if (derivative) {
                /*
                 * Following code appends the same values than the ones computed by derivativeInCell(gridX, gridY),
                 * but reusing some of the values that we already fetched for computing the interpolation.
                 */
                double dy = r10 - r00;
                int i = n;
                if (dim == 0) {
                    dx++;
                } else {
                    dy++;
                    i += INTERPOLATED_DIMENSIONS;
                    derivative = false;
                }
                vector[i  ] = dx;
                vector[i+1] = dy;
            }
        }
    }

    /**
     * Returns the derivative at the given grid indices.
     * If the given coordinates is outside the grid, then this method returns the derivative at the closest cell.
     *
     * <div class="section">Default implementation</div>
     * The current implementation assumes that the derivative is constant everywhere in the cell
     * at the given indices. It does not yet take in account the fractional part of {@code gridX}
     * and {@code gridY}, because empirical tests suggest that the accuracy of such interpolation
     * is uncertain.
     *
     * @param  gridX  first grid coordinate of the point for which to get the translation.
     * @param  gridY  second grid coordinate of the point for which to get the translation.
     * @return the derivative at the given location.
     *
     * @see #isCellInGrid(double, double)
     * @see #interpolateInCell(double, double, double[])
     */
    public Matrix derivativeInCell(final double gridX, final double gridY) {
        final int ix = Math.max(0, Math.min(gridSize[0] - 2, (int) gridX));
        final int iy = Math.max(0, Math.min(gridSize[1] - 2, (int) gridY));
        final Matrix derivative = Matrices.createDiagonal(getTranslationDimensions(), gridSize.length);
        for (int j=derivative.getNumRow(); --j>=0;) {
            final double r00 = getCellValue(j, ix,   iy  );
            final double dx  = getCellValue(j, ix+1, iy  ) - r00;
            final double dy  = getCellValue(j, ix,   iy+1) - r00;
            derivative.setElement(j, 0, derivative.getElement(j, 0) + dx);
            derivative.setElement(j, 1, derivative.getElement(j, 1) + dy);
        }
        return derivative;
    }

    /**
     * Returns the translation stored at the given two-dimensional grid indices for the given dimension.
     * The returned value is considered representative of the value in the center of the grid cell.
     * The output unit of measurement depends on the {@link #isCellValueRatio()} boolean:
     *
     * <ul>
     *   <li>If {@code false}, the value returned by this method shall be in the unit of measurement
     *       given by {@link #getTranslationUnit()}.</li>
     *   <li>If {@code true}, the value returned by this method is the ratio of the translation divided by the
     *       distance between grid cells in the <var>dim</var> dimension (<var>Δx</var> or <var>Δy</var> in the
     *       {@linkplain #DatumShiftGrid(Unit, LinearTransform, int[], boolean, Unit) constructor javadoc}).</li>
     * </ul>
     *
     * @param  dim    the dimension of the translation vector component to get,
     *                from 0 inclusive to {@link #getTranslationDimensions()} exclusive.
     * @param  gridX  the grid index on the <var>x</var> axis, from 0 inclusive to {@code gridSize[0]} exclusive.
     * @param  gridY  the grid index on the <var>y</var> axis, from 0 inclusive to {@code gridSize[1]} exclusive.
     * @return the translation for the given dimension in the grid cell at the given index.
     */
    public abstract double getCellValue(int dim, int gridX, int gridY);

    /**
     * Returns an average translation value for the given dimension.
     * Those average values shall provide a good "first guess" before to interpolate the actual translation value
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
     * @param  dim  the dimension for which to get an average translation value,
     *              from 0 inclusive to {@link #getTranslationDimensions()} exclusive.
     * @return a translation value close to the average for the given dimension.
     */
    public double getCellMean(final int dim) {
        final DoubleDouble sum = new DoubleDouble();
        final int nx = gridSize[0];
        final int ny = gridSize[1];
        for (int gridY=0; gridY<ny; gridY++) {
            for (int gridX=0; gridX<nx; gridX++) {
                sum.addKahan(getCellValue(dim, gridX, gridY));
            }
        }
        return sum.doubleValue() / (nx * ny);
    }

    /**
     * Returns an estimation of cell value precision (not to be confused with accuracy).
     * This information can be determined in different ways:
     *
     * <ul>
     *   <li>If the data are read from an ASCII file with a fixed number of digits, then a suggested value is half
     *       the precision of the last digit (i.e. 0.5 × 10⁻ⁿ where <var>n</var> is the number of digits after the
     *       comma).</li>
     *   <li>If there is no indication about precision, then this method should return a value smaller than the
     *       best accuracy found in the grid. Accuracy are often specified on a cell-by-cell basis in grid files.</li>
     * </ul>
     *
     * The output unit of measurement is the same than the one documented in {@link #getCellValue}.
     * In particular if {@link #isCellValueRatio()} returns {@code true}, then the accuracy is in
     * units of grid cell size.
     *
     * <p>This information is used for determining a tolerance threshold in iterative calculation.</p>
     *
     * @return an estimation of cell value precision.
     */
    public abstract double getCellPrecision();

    /**
     * Returns {@code true} if the translation values in the cells are divided by the cell size.
     * If {@code true}, then the values returned by {@link #getCellValue getCellValue(…)},
     * {@link #getCellMean getCellMean(…)} and {@link #interpolateInCell interpolateInCell(…)} methods
     * are the ratio of the translation divided by the distance between grid cells in the requested
     * dimension (<var>Δx</var> or <var>Δy</var> in the {@linkplain #DatumShiftGrid(Unit, LinearTransform,
     * int[], boolean, Unit) constructor javadoc}).
     *
     * @return {@code true} if the translation values in the cells are divided by the cell size.
     */
    public boolean isCellValueRatio() {
        return isCellValueRatio;
    }

    /**
     * Returns {@code true} if the given grid coordinates is inside this grid.
     *
     * @param  gridX   first grid coordinate of the point to test.
     * @param  gridY   second grid coordinate of the point to test.
     * @return whether the given point is inside this grid.
     *
     * @see #interpolateInCell(double, double, double[])
     *
     * @since 1.0
     */
    public boolean isCellInGrid(final double gridX, final double gridY) {
        return gridX >= 0 && gridY >= 0 && gridX <= gridSize[0] - 1 && gridY <= gridSize[1] - 1;
    }

    /**
     * Returns a description of the values in this grid. Grid values may be given directly as matrices or tensors,
     * or indirectly as name of file from which data were loaded. If grid values are given directly, then:
     *
     * <ul>
     *   <li>The number of {@linkplain #getGridSize() grid} dimensions determines the parameter type:
     *       one-dimensional grids are represented by {@link org.apache.sis.math.Vector} instances,
     *       two-dimensional grids are represented by {@link Matrix} instances,
     *       and grids with more than {@value #INTERPOLATED_DIMENSIONS} are represented by tensors.</li>
     *   <li>The {@linkplain #getTranslationDimensions() number of dimensions of translation vectors}
     *       determines how many matrix or tensor parameters appear.</li>
     * </ul>
     *
     * <div class="note"><b>Example 1:</b>
     * if this {@code DatumShiftGrid} instance has been created for performing NADCON datum shifts,
     * then this method returns a group named "NADCON" with two parameters:
     * <ul>
     *   <li>A parameter of type {@link java.nio.file.Path} named “Latitude difference file”.</li>
     *   <li>A parameter of type {@link java.nio.file.Path} named “Longitude difference file”.</li>
     * </ul></div>
     *
     * <div class="note"><b>Example 2:</b>
     * if this {@code DatumShiftGrid} instance has been created by
     * {@link org.apache.sis.referencing.operation.builder.LocalizationGridBuilder},
     * then this method returns a group named "Localization grid" with four parameters:
     * <ul>
     *   <li>A parameter of type {@link Integer} named “num_row” for the number of rows in each matrix.</li>
     *   <li>A parameter of type {@link Integer} named “num_col” for the number of columns in each matrix.</li>
     *   <li>A parameter of type {@link Matrix} named “grid_x”.</li>
     *   <li>A parameter of type {@link Matrix} named “grid_y”.</li>
     * </ul></div>
     *
     * @return a description of the values in this grid.
     *
     * @since 1.0
     */
    public abstract ParameterDescriptorGroup getParameterDescriptors();

    /**
     * Gets the parameter values for the grids and stores them in the provided {@code parameters} group.
     * The given {@code parameters} must have the descriptor returned by {@link #getParameterDescriptors()}.
     * The matrices, tensors or file names are stored in the given {@code parameters} instance.
     *
     * <div class="note"><b>Implementation note:</b>
     * this method is invoked by {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform}
     * and other transforms for initializing the values of their parameter group.</div>
     *
     * @param  parameters  the parameter group where to set the values.
     *
     * @since 1.0
     */
    public abstract void getParameterValues(Parameters parameters);

    /**
     * Returns a string representation of this {@code DatumShiftGrid}. The default implementation
     * formats the {@linkplain #getParameterValues(Parameters) parameter values}.
     *
     * @return a string representation of the grid parameters.
     *
     * @since 1.0
     */
    @Override
    public String toString() {
        final Parameters p = Parameters.castOrWrap(getParameterDescriptors().createValue());
        getParameterValues(p);
        return p.toString();
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     * Default implementation compares only the properties known to this abstract class like
     * {@linkplain #getGridSize() grid size}, {@linkplain #getCoordinateUnit() coordinate unit}, <i>etc.</i>
     * Subclasses need to override for adding comparison of the actual values.
     *
     * @param  other  the other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, of the same class than this {@code DatumShiftGrid}
     *         and contains the same data.
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            final DatumShiftGrid<?,?> that = (DatumShiftGrid<?,?>) other;
            return Arrays .equals(gridSize,         that.gridSize)     // Test first the value that are most likely to differ
                && Objects.equals(coordinateToGrid, that.coordinateToGrid)
                && Objects.equals(coordinateUnit,   that.coordinateUnit)
                && Objects.equals(translationUnit,  that.translationUnit)
                && isCellValueRatio == that.isCellValueRatio;
        }
        return false;
    }

    /**
     * Returns a hash code value for this datum shift grid.
     * This method does not need to compute a hash code from all grid values.
     * Comparing some metadata like the grid filename is considered sufficient
     *
     * @return a hash code based on metadata.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(coordinateToGrid) + 37 * Arrays.hashCode(gridSize);
    }
}
