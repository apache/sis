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
import javax.measure.UnitConverter;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Units;


/**
 * Small but non-constant translations to apply on coordinates for datum shifts or other transformation process.
 * The main purpose of this class is to encapsulate the data provided by <i>datum shift grid files</i>
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
 * <h2>Use cases</h2>
 * <ul class="verbose">
 *   <li><b>Datum shift by geographic translations</b><br>
 *   NADCON and NTv2 grids are defined with longitude (<var>λ</var>) and latitude (<var>φ</var>) inputs in angular
 *   <em>degrees</em> and give (<var>Δλ</var>, <var>Δφ</var>) translations in angular <em>seconds</em>.
 *   However, SIS stores the translation values in units of grid cell rather than angular seconds.
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
 *   Some remote sensing raster data are provided with a <i>localization grid</i> giving pixel coordinates
 *   (e.g. latitude and longitude). This can be seen as a change from {@linkplain DefaultEngineeringDatum image datum}
 *   to {@linkplain DefaultGeodeticDatum geodetic reference frame}. The coordinate transformation process
 *   can sometimes be performed by a mathematical conversion (for example an affine transform) applied as a
 *   {@linkplain org.apache.sis.referencing.operation.builder.LinearTransformBuilder first approximation},
 *   followed by small corrections for the residual part.
 *   {@code DatumShiftGrid} can describe the small corrections part.
 *   </li>
 * </ul>
 *
 * <h2>Number of dimensions</h2>
 * Input coordinates and translation vectors can have any number of dimensions. However, in the current implementation,
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
 * <h2>Longitude wraparound</h2>
 * Some grids are defined over an area beyond the [−180° … +180°] range of longitudes.
 * For example, NADCON grid for Alaska is defined in a [−194° … −127.875°] range,
 * in which case a longitude of 170° needs to be replaced by −190° before it can be processed by the grid.
 * The default {@code DatumShiftGrid} class does not apply longitude wraparound automatically
 * (it does not even know which axis, if any, is longitude),
 * but subclasses can add this support by overriding the {@link #replaceOutsideGridCoordinates(double[])} method.
 *
 * <h2>Sub-grids</h2>
 * Some datum shift grid files provide a grid valid on a wide region, refined with denser sub-grids in smaller regions.
 * For each point to transform, the {@link org.opengis.referencing.operation.MathTransform} should search and use the
 * densest sub-grid containing the point. This functionality is not supported directly by {@code DatumShiftGrid},
 * but can be achieved by organizing many transforms in a tree. The first step is to create an instance of
 * {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform} for each {@code DatumShiftGrid}.
 * Then, those transforms with their domain of validity can be given to
 * {@link org.apache.sis.referencing.operation.transform.MathTransforms#specialize MathTransforms.specialize(…)}.
 *
 * <h2>Serialization</h2>
 * Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 * Serialization support is appropriate for short term storage or RMI between applications running the
 * same version of Apache SIS. But for long term storage, an established datum shift grid format like
 * NTv2 should be preferred.
 *
 * <h2>Multi-threading</h2>
 * Implementations of this class shall be immutable and thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param <C>  dimension of the coordinate unit (usually {@link javax.measure.quantity.Angle}).
 * @param <T>  dimension of the translation unit (usually {@link javax.measure.quantity.Angle}
 *             or {@link javax.measure.quantity.Length}).
 *
 * @see org.apache.sis.referencing.operation.transform.DatumShiftTransform
 *
 * @since 0.7
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
     * <h4>Future evolution</h4>
     * If this class is generalized to more source dimensions in a future Apache SIS version, then this field
     * may be deprecated or its value changed. That change would be accompanied by new methods with different
     * signature. This field can be used as a way to detect that such change occurred.
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
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private final Unit<C> coordinateUnit;

    /**
     * Conversion from the "real world" coordinates to grid indices including fractional parts.
     * This is the conversion that needs to be applied before to interpolate.
     *
     * @see #getCoordinateToGrid()
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private final LinearTransform coordinateToGrid;

    /**
     * The unit of measurement of output values, as interpolated by the {@link #interpolateAt} method.
     *
     * @see #getTranslationUnit()
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
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
     *
     * @see #getGridSize()
     * @see #getGridSize(int)
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
     * That number of grid dimensions is not necessarily equal to the
     * {@linkplain #getTranslationDimensions() number of dimension of the translation vectors}.
     *
     * @return the number of cells along each axis in the grid.
     */
    public int[] getGridSize() {
        return gridSize.clone();
    }

    /**
     * Returns the number of cells in the specified dimension.
     * Invoking this method is equivalent to {@code getGridSize()[dimension]}.
     *
     * @param  dimension  the dimension for which to get the grid size.
     * @return the number of grid cells in the specified dimension.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     *
     * @since 1.1
     */
    public int getGridSize(final int dimension) {
        return gridSize[dimension];
    }

    /**
     * Returns the domain of validity of input coordinates that can be specified to the
     * {@link #interpolateAt interpolateAt(…)} method. Coordinates outside that domain
     * will still be accepted, but results may be extrapolations far from reality.
     * This method does not take in account longitude wraparound
     * (i.e. the returned envelope may cross the ±180° meridian).
     *
     * <p>The envelope coordinates are computed at cell centers; the envelope does not contain
     * the margin of 0.5 cell between cell center and cell border at the edges of the envelope.
     * The unit of measurement for the coordinate values in the returned envelope is given by
     * {@link #getCoordinateUnit()}. The envelope CRS is not set, but its value is implicitly
     * the CRS of grid input coordinates.</p>
     *
     * @return the domain covered by this grid.
     * @throws TransformException if an error occurred while computing the envelope.
     */
    public Envelope getDomainOfValidity() throws TransformException {
        final GeneralEnvelope env = new GeneralEnvelope(gridSize.length);
        for (int i=0; i<gridSize.length; i++) {
            /*
             * Note: a previous version was using the following code in an attempt to encompass
             * fully all cells (keeping in mind that the `coordinatetoGrid` maps cell centers):
             *
             *    env.setRange(i, -0.5, gridSize[i] - 0.5);
             *
             * However, it was causing spurious overlaps when two grids are side-by-side
             * (no overlapping) but one grid has larger cells than the other other grid.
             * The 0.5 cell expansion caused the grid with larger cells to overlap the
             * grid with smaller cells. This case happens with NTv2 datum shift grid.
             */
            env.setRange(i, 0, gridSize[i] - 1);
        }
        return Envelopes.transform(getCoordinateToGrid().inverse(), env);
    }

    /**
     * Returns the domain of validity converted to the specified unit of measurement.
     * A common use case for this method is for converting the domain of a NADCON or
     * NTv2 datum shift grid file, which are expressed in {@link Units#ARC_SECOND},
     * to {@link Units#DEGREE}.
     *
     * @param  unit  the desired unit of measurement.
     * @return the domain covered by this grid, converted to the given unit of measurement.
     * @throws TransformException if an error occurred while computing the envelope.
     *
     * @since 1.1
     */
    public Envelope getDomainOfValidity(final Unit<C> unit) throws TransformException {
        final UnitConverter uc = getCoordinateUnit().getConverterTo(unit);
        if (uc.isIdentity()) {
            return getDomainOfValidity();
        }
        final GeneralEnvelope domain = GeneralEnvelope.castOrCopy(getDomainOfValidity());
        for (int i=domain.getDimension(); --i >= 0;) {
            domain.setRange(i, uc.convert(domain.getLower(i)), uc.convert(domain.getUpper(i)));
        }
        return domain;
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
     * i.e. the transform uses {@link org.apache.sis.coverage.grid.PixelInCell#CELL_CENTER} convention.
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
     * <pre class="math">
     *   ┌                      ┐
     *   │ 1/Δx      0   -x₀/Δx │
     *   │    0   1/Δy   -y₀/Δy │
     *   │    0      0        1 │
     *   └                      ┘</pre>
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
     *       unit shall be the same as the {@linkplain #getCoordinateUnit() coordinate unit}.</li>
     *   <li>For {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform},
     *       the translation unit shall be the same as the unit of source ellipsoid axis lengths.</li>
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
     * <h4>Default implementation</h4>
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
     * The output unit of measurement is the same as the one documented in {@link #getCellValue(int, int, int)}.
     *
     * <h4>Extrapolations</h4>
     * If the given coordinates are outside this grid, then this method computes the translation vector at the
     * closest position in the grid. Applying translations on points outside the grid is a kind of extrapolation,
     * but some extrapolations are necessary for operations like transforming an envelope before to compute its
     * intersection with another envelope.
     *
     * <h4>Derivative (Jacobian matrix)</h4>
     * If the length of the given array is at least <var>n</var> + 4 where <var>n</var> = {@link #getTranslationDimensions()},
     * then this method appends the derivative (approximated) at the given grid indices. This is the same derivative as the
     * one computed by {@link #derivativeInCell(double, double)}, opportunistically computed here for performance reasons.
     * The matrix layout is as below, where <var>t₀</var> and <var>t₁</var> are the coordinates after translation.
     *
     * <pre class="math">
     *   ┌                   ┐         ┌                             ┐
     *   │  ∂t₀/∂x   ∂t₀/∂y  │    =    │  vector[n+0]   vector[n+1]  │
     *   │  ∂t₁/∂x   ∂t₁/∂y  │         │  vector[n+2]   vector[n+3]  │
     *   └                   ┘         └                             ┘</pre>
     *
     * <h4>Default implementation</h4>
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
        final int xmax = gridSize[0] - 2;
        final int ymax = gridSize[1] - 2;
        int ix = (int) gridX;                               // Really want rounding toward zero (not floor).
        int iy = (int) gridY;
        if (ix < 0 || ix > xmax || iy < 0 || iy > ymax) {
            final double[] gridCoordinates = {gridX, gridY};
            replaceOutsideGridCoordinates(gridCoordinates);
            gridX = gridCoordinates[0];
            gridY = gridCoordinates[1];
            ix = Math.max(0, Math.min(xmax, (int) gridX));
            iy = Math.max(0, Math.min(ymax, (int) gridY));
        }
        gridX -= ix;                                        // If was negative, will continue to be negative.
        gridY -= iy;
        /*
         * The `skipX` and `skipY` flags tell us whether a coordinate is outside the grid,
         * in which case we will need to skip derivative calculation for component outside.
         * More specifically, the Jacobian in dimensions outside the grid must be identity.
         * Note that we want the `false` value if gridX == (gridSize[0] - 1) == (xmax + 1)
         * if which case we have gridX = 1 (in all other cases, gridX < 1). Same for y.
         */
        boolean skipX = (gridX < 0); if (skipX) gridX = 0;
        boolean skipY = (gridY < 0); if (skipY) gridY = 0;
        if (gridX > 1)  {gridX = 1; skipX = true;}
        if (gridY > 1)  {gridY = 1; skipY = true;}
        final int n = getTranslationDimensions();
        boolean derivative = (vector.length >= n + INTERPOLATED_DIMENSIONS * INTERPOLATED_DIMENSIONS);
        for (int dim = 0; dim < n; dim++) {
            double dx, dy;
            final double r00 = getCellValue(dim, ix,   iy  );
            final double r01 = getCellValue(dim, ix+1, iy  );       // Naming convention: ryx (row index first, like matrix).
            final double r10 = getCellValue(dim, ix,   iy+1);
            final double r11 = getCellValue(dim, ix+1, iy+1);
            final double r0x = Math.fma(gridX, dx = r01 - r00, r00);
            final double r1x = Math.fma(gridX, dy = r11 - r10, r10);    // Not really "dy" measurement yet, will become dy later.
            vector[dim] = gridY * (r1x - r0x) + r0x;
            if (derivative) {
                /*
                 * Following code appends the same values as the ones computed by derivativeInCell(gridX, gridY),
                 * but reusing some of the values that we already fetched for computing the interpolation.
                 */
                if (skipX) {
                    dx = 0;
                } else {
                    dx += (dy - dx) * gridX;
                }
                if (skipY) {
                    dy = 0;
                } else {
                    dy  =  r10 - r00;
                    dy += (r11 - r01 - dy) * gridY;
                }
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
     * Estimates the derivative at the given grid indices. Derivatives must be consistent with values given by
     * {@link #interpolateInCell(double, double, double[])} at adjacent positions. For a two-dimensional grid,
     * {@code tₐ(x,y)} an abbreviation for {@code interpolateInCell(gridX, gridY, …)[a]} and for <var>x</var>
     * and <var>y</var> integers, the derivative is:
     *
     * <pre class="math">
     *   ┌                   ┐         ┌                                                        ┐
     *   │  ∂t₀/∂x   ∂t₀/∂y  │    =    │  t₀(x+1,y) - t₀(x,y) + 1      t₀(x,y+1) - t₀(x,y)      │
     *   │  ∂t₁/∂x   ∂t₁/∂y  │         │  t₁(x+1,y) - t₁(x,y)          t₁(x,y+1) - t₁(x,y) + 1  │
     *   └                   ┘         └                                                        ┘</pre>
     *
     * <h4>Extrapolations</h4>
     * Derivatives must be consistent with {@link #interpolateInCell(double, double, double[])} even when the
     * given coordinates are outside the grid. The {@code interpolateInCell(…)} contract in such cases is to
     * compute the translation vector at the closest position in the grid. A consequence of this contract is
     * that translation vectors stay constant when moving along at least one direction outside the grid.
     * Consequences on the derivative matrix are as below:
     *
     * <ul>
     *   <li>If both {@code gridX} and {@code gridY} are outside the grid, then the derivative is the identity matrix.</li>
     *   <li>If only {@code gridX} is outside the grid, then only the first column is set to [1, 0, …].
     *       The second column is set to the derivative of the closest cell at {@code gridY} position.</li>
     *   <li>If only {@code gridY} is outside the grid, then only the second column is set to [0, 1, …].
     *       The first column is set to the derivative of the closest cell at {@code gridX} position.</li>
     * </ul>
     *
     * @param  gridX  first grid coordinate of the point for which to get the translation.
     * @param  gridY  second grid coordinate of the point for which to get the translation.
     * @return the derivative at the given location.
     *
     * @see #isCellInGrid(double, double)
     * @see #interpolateInCell(double, double, double[])
     */
    public Matrix derivativeInCell(double gridX, double gridY) {
        final int xmax = gridSize[0] - 2;
        final int ymax = gridSize[1] - 2;
        int ix = (int) gridX;
        int iy = (int) gridY;
        if (ix < 0 || ix > xmax || iy < 0 || iy > ymax) {
            final double[] gridCoordinates = {gridX, gridY};
            replaceOutsideGridCoordinates(gridCoordinates);
            gridX = gridCoordinates[0];
            gridY = gridCoordinates[1];
            ix = Math.max(0, Math.min(xmax, (int) gridX));
            iy = Math.max(0, Math.min(ymax, (int) gridY));
        }
        gridX -= ix;
        gridY -= iy;
        final boolean skipX = (gridX < 0 || gridX > 1);
        final boolean skipY = (gridY < 0 || gridY > 1);
        final Matrix derivative = Matrices.createDiagonal(getTranslationDimensions(), gridSize.length);
        for (int j=derivative.getNumRow(); --j>=0;) {
            final double r00 = getCellValue(j, ix,   iy  );
            final double r01 = getCellValue(j, ix+1, iy  );       // Naming convention: ryx (row index first, like matrix).
            final double r10 = getCellValue(j, ix,   iy+1);
            final double r11 = getCellValue(j, ix+1, iy+1);
            if (!skipX) {
                double dx = r01 - r00;
                dx += (r11 - r10 - dx) * gridX;
                derivative.setElement(j, 0, derivative.getElement(j, 0) + dx);
            }
            if (!skipY) {
                double dy = r10 - r00;
                dy += (r11 - r01 - dy) * gridY;
                derivative.setElement(j, 1, derivative.getElement(j, 1) + dy);
            }
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
     * Caller must ensure that all arguments given to this method are in their expected ranges.
     * The behavior of this method is undefined if any argument value is out-of-range.
     * (this method is not required to validate arguments, for performance reasons).
     *
     * @param  dim    the dimension of the translation vector component to get,
     *                from 0 inclusive to {@link #getTranslationDimensions()} exclusive.
     * @param  gridX  the grid index on the <var>x</var> axis, from 0 inclusive to {@code gridSize[0]} exclusive.
     * @param  gridY  the grid index on the <var>y</var> axis, from 0 inclusive to {@code gridSize[1]} exclusive.
     * @return the translation for the given dimension in the grid cell at the given index.
     * @throws IndexOutOfBoundsException may be thrown (but is not guaranteed to be throw) if an argument is out of range.
     */
    public abstract double getCellValue(int dim, int gridX, int gridY);

    /**
     * Returns an average translation value for the given dimension.
     * Those average values shall provide a good "first guess" before to interpolate the actual translation value
     * at the (<var>x</var>,<var>y</var>) coordinate. This "first guess" is needed for inverse transform.
     *
     * <h4>Default implementation</h4>
     * The default implementation computes the average of all values returned by
     * {@link #getCellValue getCellValue(dim, …)}, but subclasses may override with more specific values.
     *
     * <h4>Example</h4>
     * In the <q>France geocentric interpolation</q> (ESPG:9655) operation method, those "average" values
     * are fixed by definition to -168, -60 and +320 metres for dimensions 0, 1 and 2 respectively
     * (geocentric <var>X</var>, <var>Y</var> and <var>Z</var>).
     *
     * @param  dim  the dimension for which to get an average translation value,
     *              from 0 inclusive to {@link #getTranslationDimensions()} exclusive.
     * @return a translation value close to the average for the given dimension.
     */
    public double getCellMean(final int dim) {
        DoubleDouble sum = DoubleDouble.ZERO;
        final int nx = gridSize[0];
        final int ny = gridSize[1];
        for (int gridY=0; gridY<ny; gridY++) {
            for (int gridX=0; gridX<nx; gridX++) {
                sum = sum.add(getCellValue(dim, gridX, gridY), false);
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
     * The output unit of measurement is the same as the one documented in {@link #getCellValue}.
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
    public boolean isCellInGrid(double gridX, double gridY) {
        final double xmax = gridSize[0] - 1;
        final double ymax = gridSize[1] - 1;
        if (gridX >= 0 && gridX <= xmax && gridY >= 0 && gridY <= ymax) {
            return true;
        }
        final double[] gridCoordinates = {gridX, gridY};
        replaceOutsideGridCoordinates(gridCoordinates);
        gridX = gridCoordinates[0];
        gridY = gridCoordinates[1];
        return (gridX >= 0 && gridX <= xmax && gridY >= 0 && gridY <= ymax);
    }

    /**
     * Invoked when a {@code gridX} or {@code gridY} coordinate is outside the range of valid grid coordinates.
     * This method can replace the invalid coordinate by a valid one. The main purpose is to handle datum shift
     * grids crossing the anti-meridian. For example, NADCON grid for Alaska is defined in a [−194° … −127.875°]
     * longitude range, so a longitude of 170° needs to be converted to a longitude of −190° before it can be
     * processed by that grid.
     *
     * <p>The default implementation does nothing. Subclasses need to override this method if they want to handle
     * longitude wraparounds. Note that the coordinate values are grid indices, not longitude or latitude values.
     * So the period to add or remove is the number of cells that the grid would have if it was spanning 360° of
     * longitude.</p>
     *
     * <h4>Example</h4>
     * If longitude values are mapped to {@code gridX} coordinates (in dimension 0), and if a shift of 360° in
     * longitude values is equivalent to a shift of {@code periodX} cells in the grid, then this method can be
     * implemented as below:
     *
     * {@snippet lang="java" :
     *     private final double periodX = ...;      // Number of grid cells in 360° of longitude.
     *
     *     @Override
     *     protected void replaceOutsideGridCoordinates(double[] gridCoordinates) {
     *         gridCoordinates[0] = Math.IEEEremainder(gridCoordinates[0], periodX);
     *     }
     * }
     *
     * This method receives all grid coordinates in the {@code gridCoordinates} argument and can modify any
     * of them, possibly many at once. The reason is because a shift of 360° of longitude (for example) may
     * correspond to an offset in both {@code gridX} and {@code gridY} indices if the grid is inclined.
     * The {@code gridCoordinates} array length is the number of grid dimensions,
     * typically {@value #INTERPOLATED_DIMENSIONS}.
     *
     * @param  gridCoordinates  on input, the cell indices of the point which is outside the grid.
     *         On output, the cell indices of an equivalent point inside the grid if possible.
     *         Coordinate values are modified in-place.
     *
     * @see Math#IEEEremainder(double, double)
     *
     * @since 1.1
     */
    protected void replaceOutsideGridCoordinates(double[] gridCoordinates) {
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
     * <h4>Example 1</h4>
     * if this {@code DatumShiftGrid} instance has been created for performing NADCON datum shifts,
     * then this method returns a group named "NADCON" with two parameters:
     * <ul>
     *   <li>A parameter of type {@link java.nio.file.Path} named “Latitude difference file”.</li>
     *   <li>A parameter of type {@link java.nio.file.Path} named “Longitude difference file”.</li>
     * </ul>
     *
     * <h4>Example 2</h4>
     * if this {@code DatumShiftGrid} instance has been created by
     * {@link org.apache.sis.referencing.operation.builder.LocalizationGridBuilder},
     * then this method returns a group named "Localization grid" with four parameters:
     * <ul>
     *   <li>A parameter of type {@link Integer} named “num_row” for the number of rows in each matrix.</li>
     *   <li>A parameter of type {@link Integer} named “num_col” for the number of columns in each matrix.</li>
     *   <li>A parameter of type {@link Matrix} named “grid_x”.</li>
     *   <li>A parameter of type {@link Matrix} named “grid_y”.</li>
     * </ul>
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
     * <h4>Implementation note</h4>
     * This method is invoked by {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform}
     * and other transforms for initializing the values of their parameter group.
     *
     * @param  parameters  the parameter group where to set the values.
     *
     * @since 1.0
     */
    public abstract void getParameterValues(Parameters parameters);

    /**
     * Returns a string representation of this {@code DatumShiftGrid} for debugging purposes.
     *
     * @return a string representation of this datum shift grid.
     *
     * @since 1.0
     */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer("DatumShift[");
        for (int i=0; i<gridSize.length; i++) {
            if (i != 0) buffer.append(" × ");
            buffer.append(gridSize[i]);
        }
        String s = String.valueOf(coordinateUnit);  if (s.isEmpty()) s = "1";
        String t = String.valueOf(translationUnit); if (t.isEmpty()) t = "1";
        buffer.append(" cells; units = ").append(s).append(" → ").append(t);
        if (isCellValueRatio) {
            buffer.append("∕cellSize");
        }
        return buffer.append(']').toString();
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data as this grid.
     * Default implementation compares only the properties known to this abstract class like
     * {@linkplain #getGridSize() grid size}, {@linkplain #getCoordinateUnit() coordinate unit}, <i>etc.</i>
     * Subclasses need to override for adding comparison of the actual values.
     *
     * @param  other  the other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, of the same class as this {@code DatumShiftGrid}
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
