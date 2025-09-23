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
package org.apache.sis.referencing.operation.builder;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;
import org.apache.sis.math.Vector;
import static org.apache.sis.referencing.operation.builder.ResidualGrid.SOURCE_DIMENSION;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * Creates an "almost linear" transform mapping the given source points to the given target points.
 * The transform is backed by a <dfn>grid of localization</dfn>, a two-dimensional array of coordinate tuples.
 * Grid size is {@code width} × {@code height} and input coordinates are (<var>i</var>,<var>j</var>) indices in the grid,
 * where <var>i</var> must be in the [0…{@code width}-1] range and <var>j</var> in the [0…{@code height}-1] range inclusive.
 * Output coordinates are the values stored in the grid of localization at the specified index.
 * After a {@code LocalizationGridBuilder} instance has been fully populated (i.e. real world coordinates have been
 * specified for all grid cells), a transformation from grid coordinates to "real world" coordinates can be obtained
 * with the {@link #create(MathTransformFactory)} method. If this transform is close enough to an affine transform,
 * then an instance of {@link LinearTransform} is returned.
 * Otherwise, a transform backed by the localization grid is returned.
 *
 * <p>This builder performs the following steps:</p>
 * <ol>
 *   <li>Compute a linear approximation of the transformation using {@link LinearTransformBuilder}.</li>
 *   <li>Compute {@link DatumShiftGrid} with the residuals.</li>
 *   <li>Create a {@link InterpolatedTransform} with the above shift grid.</li>
 *   <li>If a {@linkplain LinearTransformBuilder#linearizer() linearizer has been applied},
 *       concatenate the inverse transform of that linearizer.</li>
 * </ol>
 *
 * Builders are not thread-safe. Builders can be used only once;
 * points cannot be added or modified after {@link #create(MathTransformFactory)} has been invoked.
 *
 * <h2>Linearizers</h2>
 * If the localization grid is not close enough to a linear transform, {@link InterpolatedTransform} may not converge.
 * To improve the speed and reliability of the transform, a non-linear step can be {@linkplain #addLinearizers specified}.
 * Many candidates can be specified in case the exact form of that non-linear step is unknown;
 * {@code LocalizationGridBuilder} will select the non-linear step that provides the best improvement, if any.
 * See the <cite>Linearizers</cite> section in {@link LinearTransformBuilder} for more discussion.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see InterpolatedTransform
 * @see LinearTransform
 * @see DatumShiftGrid
 *
 * @since 0.8
 */
public class LocalizationGridBuilder extends TransformBuilder {
    /**
     * Tolerance threshold for comparing pixel coordinates relative to integer values.
     */
    private static final double EPS = Numerics.COMPARISON_THRESHOLD;

    /**
     * The transform for the linear part.
     * Always created with a grid size specified to the constructor.
     */
    private final LinearTransformBuilder linearBuilder;

    /**
     * A temporary array for two-dimensional source coordinates.
     * Used for reducing object allocations.
     */
    private final int[] gridCoordinates = new int[SOURCE_DIMENSION];

    /**
     * Conversions from source real-world coordinates to grid indices before interpolation.
     * If there is no such conversion to apply, then this is the identity transform.
     */
    private LinearTransform sourceToGrid;

    /**
     * The desired precision of inverse transformations in unit of source coordinates, or 0 in unspecified.
     * If no {@link #sourceToGrid} transform has been specified, then this is in unit of grid cells
     * (i.e. a value of 1 is the size of one grid cell).
     *
     * @see #setDesiredPrecision(double)
     */
    private double precision;

    /**
     * Arbitrary default {@link #precision} value, in unit of grid cells. Used if no explicit inverse transform
     * precision has been specified. The {@code sourceToGrid} transform shall not be applied on this value.
     * This default precision may change in any future SIS version.
     */
    @Configuration
    private static final double DEFAULT_PRECISION = 1E-7;

    /**
     * The transform created by {@link #create(MathTransformFactory)}.
     */
    private MathTransform transform;

    /**
     * If the coordinates in some dimensions are cyclic, their periods. Otherwise {@code null}.
     * Values are in units of the target CRS. For longitude wraparounds, the period is typically 360°.
     * Array length shall be {@code linearBuilder.getTargetDimensions()} and non-cyclic dimensions shall have
     * a period of zero (not {@link Double#NaN}, because we will use this array as a displacement vector).
     *
     * @see #resolveWraparoundAxis(int, int, double)
     * @see ResidualGrid#periodVector
     */
    private double[] periods;

    /**
     * Creates a new, initially empty, builder for a localization grid of the given size.
     *
     * @param width   the number of columns in the grid of target positions.
     * @param height  the number of rows in the grid of target positions.
     */
    public LocalizationGridBuilder(final int width, final int height) {
        linearBuilder = new LinearTransformBuilder(width, height);
        sourceToGrid  = MathTransforms.identity(SOURCE_DIMENSION);
    }

    /**
     * Creates a new, initially empty, builder for a localization grid of a size inferred from the given points.
     * This constructor uses the given vectors for computing a grid size and the following initial conversion:
     *
     * <blockquote>({@code sourceX}, {@code sourceY}) → ({@code gridX}, {@code gridY})</blockquote>
     *
     * Above conversion can be obtained by {@link #getSourceToGrid()}.
     *
     * <p>Values in the given vectors should be integers, but this constructor is tolerant to non-integer values
     * if they have a constant offset (typically 0.5) relative to integer values. The two vectors do not need to
     * have the same length (i.e. {@code sourceX[i]} are not necessarily related to {@code sourceY[i]}).</p>
     *
     * @param  sourceX  all possible <var>x</var> inputs before conversion to grid coordinates.
     * @param  sourceY  all possible <var>y</var> inputs before conversion to grid coordinates.
     * @throws ArithmeticException if this constructor cannot infer a reasonable grid size from the given vectors.
     */
    public LocalizationGridBuilder(final Vector sourceX, final Vector sourceY) {
        final Matrix fromGrid = new Matrix3();
        final int width  = infer(sourceX, fromGrid, 0);
        final int height = infer(sourceY, fromGrid, 1);
        linearBuilder = new LinearTransformBuilder(width, height);
        try {
            sourceToGrid = MathTransforms.linear(fromGrid).inverse();
        } catch (NoninvertibleTransformException e) {
            // Should not happen because infer(…) verified that the coefficients are okay.
            throw (ArithmeticException) new ArithmeticException(e.getLocalizedMessage()).initCause(e);
        }
    }

    /**
     * Creates a new builder for a localization grid inferred from the given provider of control points.
     * The {@linkplain LinearTransformBuilder#getSourceDimensions() number of source dimensions} in the
     * given {@code localizations} argument shall be 2. The {@code localization} can be used in two ways:
     *
     * <ul class="verbose">
     *   <li>If the {@code localizations} instance has been
     *     {@linkplain LinearTransformBuilder#LinearTransformBuilder(int...) created with a fixed grid size},
     *     then that instance is used as-is — it is not copied. It is okay to specify an empty instance and
     *     to provide control points later by calls to {@link #setControlPoint(int, int, double...)}.</li>
     *   <li>If the {@code localizations} instance has been
     *     {@linkplain LinearTransformBuilder#LinearTransformBuilder() created for a grid of unknown size},
     *     then this constructor tries to infer a grid size by inspection of the control points present in
     *     {@code localizations} at the time this constructor is invoked. Changes in {@code localizations}
     *     after construction will not be reflected in this new builder.</li>
     * </ul>
     *
     * The new builder inherits the {@linkplain LinearTransformBuilder#addLinearizers linearizers}
     * of {@code localizations}.
     *
     * @param  localizations  the provider of control points for which to create a localization grid.
     * @throws ArithmeticException if this constructor cannot infer a reasonable grid size from the given localizations.
     *
     * @since 1.0
     */
    public LocalizationGridBuilder(final LinearTransformBuilder localizations) {
        int n = localizations.getGridDimensions();
        if (n == SOURCE_DIMENSION) {
            linearBuilder = localizations;
            sourceToGrid  = MathTransforms.identity(SOURCE_DIMENSION);
        } else {
            if (n < 0) {
                final Vector[] sources = localizations.sources();
                n = sources.length;
                if (n == SOURCE_DIMENSION) {
                    final Matrix fromGrid = new Matrix3();
                    final int width  = infer(sources[0], fromGrid, 0);
                    final int height = infer(sources[1], fromGrid, 1);
                    linearBuilder = new LinearTransformBuilder(width, height);
                    linearBuilder.setControlPoints(localizations.getControlPoints());
                    try {
                        sourceToGrid = MathTransforms.linear(fromGrid).inverse();
                    } catch (NoninvertibleTransformException e) {
                        throw (ArithmeticException) new ArithmeticException(e.getLocalizedMessage()).initCause(e);
                    }
                    linearBuilder.setLinearizers(localizations);
                    return;
                }
            }
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedTransformDimension_4,
                                               "localizations", 0, SOURCE_DIMENSION, n));
        }
    }

    /**
     * Infers a grid size by searching for the greatest common divisor (GCD) for values in the given vector.
     * The vector values should be integers, but this method is tolerant to constant offsets (typically 0.5).
     * The GCD is taken as a "grid to source" scale factor and the minimal value as the translation term.
     * Those two values are stored in the {@code dim} row of the given matrix.
     *
     * @param  source    the vector of values for which to get the GCD and minimum value.
     * @param  fromGrid  matrix where to store the minimum value and the GCD.
     * @param  dim       index of the matrix row to update.
     * @return grid size.
     */
    private static int infer(final Vector source, final Matrix fromGrid, final int dim) {
        final NumberRange<?> range = source.range();
        final double min  = range.getMinDouble(true);
        final double span = range.getSpan();
        final Number increment = source.increment(EPS * span);
        double inc;
        if (increment != null) {
            inc = increment.doubleValue();
        } else {
            inc = span;
            final int size = source.size();
            for (int i=0; i<size; i++) {
                double v = source.doubleValue(i) - min;
                if (Math.abs(v % inc) > EPS) {
                    do {
                        final double r = (inc % v);     // Both `inc` and `v` are positive, so `r` will be positive too.
                        inc = v;
                        v = r;
                    } while (Math.abs(v) > EPS);
                }
            }
        }
        /*
         * Compute the size from the increment that we found. If the size is larger than the vector length,
         * consider as too large. The rational is that attempt to create a localization grid with that size
         * would fail anyway, because it would contain holes where no value is defined. A limit is important
         * for preventing useless allocation of large arrays - https://issues.apache.org/jira/browse/SIS-407
         */
        fromGrid.setElement(dim, dim, inc);
        fromGrid.setElement(dim, SOURCE_DIMENSION, min);
        final double n = span / inc;
        if (n >= 0.5 && n < source.size() - 0.5) {          // Compare as `double` in case the value is large.
            return ((int) Math.round(n)) + 1;
        }
        throw new ArithmeticException(Resources.format(Resources.Keys.CanNotInferGridSizeFromValues_1, range));
    }

    /**
     * Throws {@link IllegalStateException} if this builder cannot be modified anymore.
     */
    private void ensureModifiable() throws IllegalStateException {
        if (!linearBuilder.isModifiable()) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnmodifiableObject_1, LocalizationGridBuilder.class));
        }
    }

    /**
     * Sets the desired precision of <em>inverse</em> transformations, in units of source coordinates.
     * If a conversion from "real world" to grid coordinates {@linkplain #setSourceToGrid has been specified},
     * then the given precision is in "real world" units. Otherwise the precision is in units of grid cells
     * (i.e. a value of 1 is the size of one grid cell).
     *
     * <div class="note"><b>Note:</b>
     * there is no method for setting the desired target precision because forward transformations <em>precision</em>
     * (not to be confused with <em>accuracy</em>) are limited only by rounding errors. Of course the accuracy of both
     * forward and inverse transformations still limited by the accuracy of given control points and the grid resolution.
     * </div>
     *
     * @param  precision  desired precision of the results of inverse transformations.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     *
     * @see DatumShiftGrid#getCellPrecision()
     */
    public void setDesiredPrecision(final double precision) {
        ensureModifiable();
        ArgumentChecks.ensureStrictlyPositive("precision", precision);
        this.precision = precision;
    }

    /**
     * Returns the desired precision of <em>inverse</em> transformations, in units of source coordinates.
     * This is the precision sets by the last call to {@link #setDesiredPrecision(double)}.
     *
     * @return desired precision of the results of inverse transformations.
     */
    public double getDesiredPrecision() {
        return precision;
    }

    /**
     * Defines relationship between "real-world" source coordinates and grid coordinates.
     * The given transform is usually two-dimensional, in which case conversions from (<var>x</var>,<var>y</var>)
     * source coordinates to ({@code gridX}, {@code gridY}) indices can be done with the following formulas:
     * <ul>
     *   <li><var>gridX</var> = (<var>x</var> - <var>x₀</var>) / <var>Δx</var></li>
     *   <li><var>gridY</var> = (<var>y</var> - <var>y₀</var>) / <var>Δy</var></li>
     * </ul>
     *
     * where:
     * <ul>
     *   <li>(<var>x₀</var>, <var>y₀</var>) is the coordinate of the center of the cell at grid index (0,0).</li>
     *   <li><var>Δx</var> and <var>Δy</var> are the distances between two cells on the <var>x</var> and <var>y</var>
     *       axes respectively, in the same unit of measurement than the one documented in the
     *       {@link #setDesiredPrecision(double)} method.</li>
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
     * If this method is never invoked, then the default conversion is identity.
     * If a {@linkplain #setDesiredPrecision(double) desired precision} has been specified before this method call,
     * it is caller's responsibility to convert that value to new source units if needed.
     *
     * @param  sourceToGrid  conversion from the "real world" source coordinates to grid indices including fractional parts.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     *
     * @see DatumShiftGrid#getCoordinateToGrid()
     */
    public void setSourceToGrid(final LinearTransform sourceToGrid) {
        ensureModifiable();
        int isTarget = 0;
        int dim = sourceToGrid.getSourceDimensions();
        if (dim >= SOURCE_DIMENSION) {
            isTarget = 1;
            dim = sourceToGrid.getTargetDimensions();
            if (dim == SOURCE_DIMENSION) {
                this.sourceToGrid = sourceToGrid;
                return;
            }
        }
        throw new org.opengis.geometry.MismatchedDimensionException(
                Errors.format(Errors.Keys.MismatchedTransformDimension_4,
                "sourceToGrid", isTarget, SOURCE_DIMENSION, dim));
    }

    /**
     * Returns the current relationship between "real-world" source coordinates and grid coordinates.
     * This is the value set by the last call to {@link #setSourceToGrid(LinearTransform)}.
     * If that setter method has never been invoked, then this is an automatically computed transform
     * if the grid coordinates {@linkplain #LocalizationGridBuilder(Vector, Vector) have been specified
     * to the constructor}, or the identity transform {@linkplain #LocalizationGridBuilder(int, int) otherwise}.
     *
     * @return the current relationship between "real-world" source coordinates and grid coordinates.
     */
    public LinearTransform getSourceToGrid() {
        return sourceToGrid;
    }

    /**
     * Returns the envelope of source coordinates. The {@code fullArea} argument control whether
     * the returned envelope shall encompass full surface of every cells or only their centers:
     * <ul>
     *   <li>If {@code true}, then the returned envelope encompasses full cell surfaces,
     *       from lower border to upper border. In other words, the returned envelope encompasses all
     *       {@linkplain org.apache.sis.coverage.grid.PixelInCell#CELL_CORNER cell corners}.</li>
     *   <li>If {@code false}, then the returned envelope encompasses only
     *       {@linkplain org.apache.sis.coverage.grid.PixelInCell#CELL_CENTER cell centers}, inclusive.</li>
     * </ul>
     *
     * This is the envelope of the grid domain (i.e. the ranges of valid {@code gridX} and {@code gridY} argument
     * values in calls to {@code get/setControlPoint(…)} methods) transformed as below:
     * <ol>
     *   <li>expanded by ½ cell on each side if {@code fullArea} is {@code true}</li>
     *   <li>transformed by the inverse of {@linkplain #getSourceToGrid() source to grid} transform.</li>
     * </ol>
     *
     * @param  fullArea  whether the envelope shall encompass the full cell surfaces instead of only their centers.
     * @return the envelope of grid points, from lower corner to upper corner.
     * @throws IllegalStateException if the grid points are not yet known.
     * @throws TransformException if the envelope cannot be calculated.
     *
     * @see LinearTransformBuilder#getSourceEnvelope()
     *
     * @since 1.0
     */
    public Envelope getSourceEnvelope(final boolean fullArea) throws TransformException {
        Envelope envelope = linearBuilder.getSourceEnvelope();
        if (fullArea) {
            for (int i = envelope.getDimension(); --i >= 0;) {
                final GeneralEnvelope ge = GeneralEnvelope.castOrCopy(envelope);
                ge.setRange(i, ge.getLower(i) - 0.5,
                               ge.getUpper(i) + 0.5);
                envelope = ge;
            }
        }
        return Envelopes.transform(sourceToGrid.inverse(), envelope);
    }

    /**
     * Sets all control points. The length of given vectors must be equal to the total number of cells in the grid.
     * The first vector provides the <var>x</var> coordinates; the second vector provides the <var>y</var> coordinates,
     * <i>etc.</i>. Coordinates are stored in row-major order (column index varies faster, followed by row index).
     *
     * @param  coordinates coordinates in each target dimensions, stored in row-major order.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     *
     * @since 1.0
     */
    public void setControlPoints(final Vector... coordinates) {
        ensureModifiable();
        linearBuilder.setControlPoints(Objects.requireNonNull(coordinates));
    }

    /**
     * Sets a single matching control point pair. Source position is assumed precise and target position is assumed uncertain.
     * If the given source position was already associated with another target position, then the old target position is discarded.
     *
     * <p>If a {@linkplain #getSourceToGrid() source to grid} conversion exists, it shall have been applied
     * by the caller for computing the ({@code gridX}, {@code gridY}) coordinates given to this method.</p>
     *
     * @param  gridX   the column index in the grid where to store the given target position.
     * @param  gridY   the row index in the grid where to store the given target position.
     * @param  target  the target coordinates, assumed uncertain.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     * @throws IllegalArgumentException if the {@code x} or {@code y} coordinate value is out of grid range.
     * @throws MismatchedDimensionException if the target position does not have the expected number of dimensions.
     */
    public void setControlPoint(final int gridX, final int gridY, final double... target) {
        ensureModifiable();
        gridCoordinates[0] = gridX;
        gridCoordinates[1] = gridY;
        linearBuilder.setControlPoint(gridCoordinates, target);
    }

    /**
     * Returns a single target coordinate for the given source coordinate, or {@code null} if none.
     * If {@linkplain #addLinearizers linearizers} have been specified and {@link #create create(…)}
     * has already been invoked, then the control points may be projected using one of the linearizers.
     *
     * @param  gridX  the column index in the grid where to read the target position.
     * @param  gridY  the row index in the grid where to read the target position.
     * @return the target coordinates associated to the given source, or {@code null} if none.
     * @throws IllegalArgumentException if the {@code x} or {@code y} coordinate value is out of grid range.
     */
    public double[] getControlPoint(final int gridX, final int gridY) {
        gridCoordinates[0] = gridX;
        gridCoordinates[1] = gridY;
        return linearBuilder.getControlPoint(gridCoordinates);
    }

    /**
     * Returns a row of coordinate values in the given dimension.
     * The returned vector is a view; changes in the returned vector will be reflected in this builder.
     *
     * @param  dimension  the target dimension for which to get coordinate values.
     * @param  row        index of the row to get.
     * @return coordinate values of the specified row in the specified dimension.
     *
     * @since 1.0
     */
    public Vector getRow(final int dimension, final int row) {
        gridCoordinates[0] = 0;
        gridCoordinates[1] = row;
        return linearBuilder.getTransect(dimension, gridCoordinates, 0);
    }

    /**
     * Returns a column of coordinate values in the given dimension.
     * The returned vector is a view; changes in the returned vector will be reflected in this builder.
     *
     * @param  dimension  the target dimension for which to get coordinate values.
     * @param  column     index of the column to get.
     * @return coordinate values of the specified column in the specified dimension.
     *
     * @since 1.0
     */
    public Vector getColumn(final int dimension, final int column) {
        gridCoordinates[0] = column;
        gridCoordinates[1] = 0;
        return linearBuilder.getTransect(dimension, gridCoordinates, 1);
    }

    /**
     * Tries to remove discontinuities in coordinates values caused by anti-meridian crossing.
     * This method can be invoked when the localization grid may cross the anti-meridian,
     * where longitude values may suddenly jump from +180° to -180° or conversely.
     * This method walks through the coordinate values of the given dimension (typically the longitudes dimension)
     * in the given direction (grid rows or grid columns).
     * If a difference greater than {@code period/2} (typically 180°) is found between two consecutive values,
     * then a multiple of {@code period} (typically 360°) is added or subtracted in order to make a value as close
     * as possible from its previous value.
     *
     * <p>This method needs a direction to be specified:</p>
     * <ul>
     *   <li>Direction 0 means that each value is compared with the value in the previous column,
     *       except the value in the first column which is compared to the value in previous row.</li>
     *   <li>Direction 1 means that each value is compared with the value in the previous row,
     *       except the value in the first row which is compared to the value in previous column.</li>
     * </ul>
     * The recommended value is the direction of most stable values. Typically, longitude values increase with column indices
     * and are almost constant when increasing row indices. In such case, the recommended direction is 1 for comparing each
     * value with the value in previous row, since that value should be closer than the value in previous column.
     *
     * <h4>Example</h4>
     * for a grid of (<var>longitude</var>, <var>latitude</var>) values in decimal degrees where longitude values
     * vary (increase or decrease) with increasing column indices and latitude values vary (increase or decrease)
     * with increasing row indices, the following method should be invoked for protecting the grid against
     * discontinuities on anti-meridian:
     *
     * {@snippet lang="java" :
     *     grid.resolveWraparoundAxis(0, 1, 360);
     *     }
     *
     * @param  dimension  the dimension to process.
     *                    This is 0 for longitude dimension in a (<var>longitudes</var>, <var>latitudes</var>) grid.
     * @param  direction  the direction to walk through: 0 for columns or 1 for rows.
     *                    The recommended direction is the direction of most stable values, typically 1 (rows) for longitudes.
     * @param  period     that wraparound range (typically 360° for longitudes). Must be strictly positive.
     * @return the range of coordinate values in the specified dimension after correction for wraparound values.
     * @throws IllegalStateException if this method has already been invoked for the same dimension,
     *         or if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     *
     * @since 1.0
     */
    public NumberRange<Double> resolveWraparoundAxis(final int dimension, final int direction, final double period) {
        ensureModifiable();
        ArgumentChecks.ensureBetween("dimension", 0, linearBuilder.getTargetDimensions() - 1, dimension);
        ArgumentChecks.ensureBetween("direction", 0, linearBuilder.getSourceDimensions() - 1, direction);
        ArgumentChecks.ensureStrictlyPositive("period", period);
        if (periods == null) {
            periods = new double[linearBuilder.getTargetDimensions()];
        }
        if (periods[dimension] != 0) {
            throw new IllegalStateException(Errors.format(
                    Errors.Keys.ValueAlreadyDefined_1, Strings.bracket("periods", dimension)));
        }
        periods[dimension] = period;
        return linearBuilder.resolveWraparoundAxis(dimension, direction, period);
    }

    /**
     * Adds transforms to potentially apply on target control points before to compute the transform.
     * This method can be invoked if the departure from a linear transform is too large, resulting
     * in {@link InterpolatedTransform} to fail with "no convergence error" messages.
     * If linearizers have been specified, then the {@link #create(MathTransformFactory)} method
     * will try to apply each transform on target coordinates and check which one results in the
     * best correlation coefficients. Exactly one of the specified transforms will be selected.
     * If applying no transform is an acceptable solution, then an
     * {@linkplain org.apache.sis.referencing.operation.transform.MathTransforms#identity(int)
     * identity transform} should be included in the given {@code projections} map.
     *
     * <p>The linearizers are specified as {@link MathTransform}s from current {@linkplain #getControlPoint(int, int)
     * target coordinates of control points} to other spaces where <i>sources to new targets</i> transforms may
     * be more linear. The keys in the map are arbitrary identifiers.
     * The {@code projToGrid} argument specifies which control point dimensions to use as {@code projections} source
     * coordinates and can be null or omitted if the projections shall be applied on all target coordinates.
     * It is possible to invoke this method many times with different {@code dimensions} argument values.</p>
     *
     * <p>The {@code compensate} argument tell whether the inverse of specified transform shall be concatenated
     * to the final {@linkplain #create interpolated transform}. If {@code true}, the {@code projection} effect
     * will be cancelled in the final result, i.e. the target coordinates will be approximately the same as if
     * no projection were applied. In such case, the advantage of applying a projection is to improve numerical
     * stability with a better linear approximation in used by the coordinate transformation process.</p>
     *
     * @param  projections  projections from current target coordinates to other spaces which may result in more linear transforms.
     * @param  compensate   whether the inverse of selected projection shall be concatenated to the final interpolated transform.
     * @param  projToGrid   the target dimensions to project, or null or omitted for projecting all target dimensions.
     *                      If non-null and non-empty, then all transforms in the {@code projections} map shall have a
     *                      number of source and target dimensions equals to the length of this array.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     *
     * @see LinearTransformBuilder#addLinearizers(Map, int...)
     *
     * @since 1.1
     */
    public void addLinearizers(final Map<String,MathTransform> projections, final boolean compensate, final int... projToGrid) {
        ensureModifiable();
        linearBuilder.addLinearizers(Objects.requireNonNull(projections), compensate, projToGrid);
    }

    /**
     * Creates a transform from the source points to the target points.
     * This method assumes that source points are precise and all uncertainty is in the target points.
     * If this transform is close enough to an affine transform, then an instance of {@link LinearTransform} is returned.
     *
     * <p>If this method is invoked more than once, the previously created transform instance is returned.</p>
     *
     * @param  factory  the factory to use for creating the transform, or {@code null} for the default factory.
     *                  The {@link MathTransformFactory#createAffineTransform(Matrix)} method of that factory
     *                  shall return {@link LinearTransform} instances.
     * @return the transform from source to target points.
     * @throws FactoryException if the transform cannot be created,
     *         for example because the target points have not be specified.
     */
    @Override
    public MathTransform create(final MathTransformFactory factory) throws FactoryException {
        if (transform == null) {
            final LinearTransform gridToCoord = linearBuilder.create(factory);
            /*
             * Make a first check about whether the result of above LinearTransformBuilder.create() call
             * can be considered a good fit. If true, then we may return the linear transform directly.
             */
            boolean isExact  = true;
            boolean isLinear = true;
            for (final double c : linearBuilder.correlation()) {
                isExact &= (c == 1);
                if (!(c >= 0.9999)) {                           // Empirical threshold (may need to be revisited).
                    isLinear = false;
                    break;
                }
            }
            MathTransform step;
            if (isExact) {
                step = MathTransforms.concatenate(sourceToGrid, gridToCoord);
            } else {
                final int      width    = linearBuilder.gridSize(0);
                final int      height   = linearBuilder.gridSize(1);
                final float[]  residual = new float [SOURCE_DIMENSION * linearBuilder.gridLength];
                final double[] grid     = new double[SOURCE_DIMENSION * width];
                double gridPrecision    = precision;
                try {
                    /*
                     * If the user specified a precision, we need to convert it from source units to grid units.
                     * We convert each dimension separately, then retain the largest magnitude of vector results.
                     */
                    if (gridPrecision > 0 && !sourceToGrid.isIdentity()) {
                        final double[] vector = new double[sourceToGrid.getSourceDimensions()];
                        final double[] offset = new double[sourceToGrid.getTargetDimensions()];
                        double converted = 0;
                        for (int i=0; i<vector.length; i++) {
                            vector[i] = precision;
                            sourceToGrid.deltaTransform(vector, 0, offset, 0, 1);
                            final double length = MathFunctions.magnitude(offset);
                            if (length > converted) converted = length;
                            vector[i] = 0;
                        }
                        gridPrecision = converted;
                    }
                    /*
                     * Compute the residuals, i.e. the differences between the coordinates that we get by a linear
                     * transformation and the coordinates that we want to get. If at least one residual is greater
                     * than the desired precision,  then the returned MathTransform will need to apply corrections
                     * after linear transforms. Those corrections will be done by InterpolatedTransform.
                     */
                    final MathTransform coordToGrid = gridToCoord.inverse();
                    for (int k=0,y=0; y<height; y++) {
                        gridCoordinates[0] = 0;
                        gridCoordinates[1] = y;
                        linearBuilder.getControlRow(gridCoordinates, grid);                 // Expected positions.
                        coordToGrid.transform(grid, 0, grid, 0, width);                     // As grid coordinate.
                        for (int i=0,x=0; x<width; x++) {
                            final double dx = grid[i++] - x;
                            final double dy = grid[i++] - y;
                            isLinear &= (dx <= gridPrecision);
                            isLinear &= (dy <= gridPrecision);
                            residual[k++] = (float) dx;
                            residual[k++] = (float) dy;
                        }
                    }
                    if (isLinear) {
                        step = MathTransforms.concatenate(sourceToGrid, gridToCoord);
                    } else {
                        final ResidualGrid shifts = new ResidualGrid(sourceToGrid, gridToCoord, width, height,
                                residual, (gridPrecision > 0) ? gridPrecision : DEFAULT_PRECISION, periods,
                                linearBuilder.appliedLinearizer());
                        step = InterpolatedTransform.createGeodeticTransformation(nonNull(factory), shifts);
                    }
                } catch (TransformException e) {
                    throw new LocalizationGridException(e);                                 // Should never happen.
                }
            }
            /*
             * At this point we finished to compute the transformation to target coordinates.
             * If those target coordinates have been modified in order to make that step more
             * linear, apply the inverse transformation after the step.
             */
            final ProjectedTransformTry linearizer = linearBuilder.appliedLinearizer();
            if (linearizer != null && linearizer.reverseAfterLinearization) try {
                step = factory.createConcatenatedTransform(step, linearizer.getValue().inverse());
            } catch (NoninvertibleTransformException e) {
                throw new InvalidGeodeticParameterException(Resources.format(
                        Resources.Keys.NonInvertibleOperation_1, linearizer.getKey()), e);
            }
            transform = step;                               // Set only after everything succeeded.
        }
        return transform;
    }

    /**
     * Returns the linearizer applied on target control points.
     * This method returns a non-empty value if {@link #addLinearizers(Map, boolean, int...)} has
     * been invoked with a non-empty map, followed by a {@link #create(MathTransformFactory)} call.
     * In such case, {@link LinearTransformBuilder} selects a linearizer identified by the returned
     * <var>key</var> - <var>value</var> entry. The entry key is one of the keys of the maps given
     * to {@code addLinearizers(…)}. The entry value is the associated {@code MathTransform},
     * possibly modified as described in the <i>axis order</i> section below.
     *
     * <p>All control points returned by {@link #getControlPoint(int, int)} are projected by the selected transform.
     * Consequently, if the target coordinates of original control points are desired, then the transform computed by
     * this builder needs to be concatenated with the {@linkplain MathTransform#inverse() inverse} of the transform
     * returned by this method. This is done automatically in the {@link #create(MathTransformFactory) create(…)}
     * method if the {@code compensate} flag given to {@code addLinearizers(…)} method was {@code true}.
     * Otherwise the compensation, if desired, needs to be done by the caller.</p>
     *
     * <h4>Axis order</h4>
     * The returned transform will contain an operation step performing axis filtering and swapping implied by the
     * {@code projToGrid} argument that was given to the <code>{@linkplain #addLinearizers(Map, boolean, int...)
     * addLinearizers}(…, projToGrid)}</code> method. Consequently, if the {@code projToGrid} argument was not an
     * arithmetic progression, then the transform returned by this method will not be one of the instances given
     * to {@code addLinearizers(…)}.
     *
     * @param  ifNotCompensated  whether to return the transform only if not already compensated by {@code create(…)}.
     *         A value of {@code true} is useful if the caller wants the transform only if it needs to compensate itself.
     * @return the projection applied on target coordinates before to compute a linear transform.
     *
     * @see LinearTransformBuilder#linearizer()
     *
     * @since 1.1
     */
    public Optional<Map.Entry<String,MathTransform>> linearizer(final boolean ifNotCompensated) {
        ProjectedTransformTry linearizer = linearBuilder.appliedLinearizer();
        if (ifNotCompensated && linearizer != null && linearizer.reverseAfterLinearization) {
            linearizer = null;
        }
        return Optional.ofNullable(linearizer);
    }

    /**
     * Returns statistics of differences between values calculated by the transform and actual values.
     * The tested transform is
     * the one computed by {@link #create(MathTransformFactory)} if the {@code linear} argument is {@code false},
     * or the transform computed by {@link LinearTransformBuilder} if the {@code linear} argument is {@code true}.
     * The returned statistics are:
     *
     * <ol class="verbose">
     *   <li>One {@code Statistics} instance for each target dimension, containing statistics about the differences between
     *     coordinates computed by the given transform and expected coordinates. For each (<var>i</var>,<var>j</var>) indices
     *     in this grid, the indices are transformed by a call to {@code mt.transform(…)} and the result is compared with the
     *     coordinates given by <code>{@linkplain #getControlPoint(int, int) getControlPoint}(i,j)</code>.
     *     Those statistics are identified by labels like “Δx” and “Δy”.</li>
     *   <li>One {@code Statistics} instance for each source dimension, containing statistics about the differences between
     *     coordinates computed by the <em>inverse</em> of the transform and expected coordinates.
     *     For each (<var>x</var>,<var>y</var>) control point in this grid, the points are transformed by a call
     *     to {@code mt.inverse().transform(…)} and the result is compared with the pixel indices of that point.
     *     Those statistics are identified by labels like "Δi" and "Δj".</li>
     * </ol>
     *
     * @param  linear  {@code false} for computing errors using the complete transform,
     *                 or {@code true} for using only the linear part.
     * @return statistics of difference between computed values and expected values for each target dimension.
     * @throws IllegalStateException if {@link #create(MathTransformFactory)} has not yet been invoked.
     *
     * @see #toString(boolean, Locale)
     *
     * @since 1.1
     */
    public Statistics[] errors(final boolean linear) {
        final MathTransform mt = linear ? linearBuilder.transform() : transform;
        if (mt == null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.Uninitialized_1, getClass().getSimpleName()));
        }
        final int           tgtDim = mt.getTargetDimensions();
        final double[]      point  = new double[Math.max(tgtDim, SOURCE_DIMENSION)];
        final Statistics[]  stats  = new Statistics[tgtDim + SOURCE_DIMENSION];
        final StringBuilder buffer = new StringBuilder();
        for (int i=0; i<stats.length; i++) {
            buffer.setLength(0);
            buffer.append('Δ');
            if (i < tgtDim) {
                if (i < 3) {
                    buffer.append((char) ('x' + i));
                } else {
                    buffer.append('z').append(i - 1);       // After (x,y,z) continue with z2, z3, z4, etc.
                }
            } else {
                buffer.append((char) ('i' + (i - tgtDim)));
            }
            stats[i] = new Statistics(buffer.toString());
        }
        /*
         * If a linearizer has been applied, all target coordinates in this builder have been projected using
         * that transform. We will need to apply the inverse transform in order to get back the original values.
         * The way that we get the transform below should be the same way as in `create(…)`, except that we
         * apply the inverse transform unconditionally.
         */
        final MathTransform complete, inverse;
        try {
            final ProjectedTransformTry linearizer = linearBuilder.appliedLinearizer();
            complete = (linearizer != null && linearizer.reverseAfterLinearization) ? linearizer.getValue().inverse() : null;
            inverse = mt.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException(e);
        }
        final int width  = linearBuilder.gridSize(0);
        final int height = linearBuilder.gridSize(1);
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                point[0] = gridCoordinates[0] = x;
                point[1] = gridCoordinates[1] = y;
                final double[] expected;
                try {
                    mt.transform(point, 0, point, 0, 1);
                    expected = linearBuilder.getControlPoint(gridCoordinates);
                    if (complete != null) {
                        complete.transform(expected, 0, expected, 0, 1);
                    }
                } catch (TransformException e) {
                    continue;                           // Ignore the points that we fail to transform.
                }
                for (int i=0; i<tgtDim; i++) {
                    stats[i].accept(point[i] - expected[i]);
                }
                /*
                 * Transform the geographic point back to grid indices and check error.
                 */
                try {
                    inverse.transform(expected, 0, expected, 0, 1);
                } catch (TransformException e) {
                    continue;                           // Ignore the points that we fail to transform.
                }
                for (int i=0; i<SOURCE_DIMENSION; i++) {
                    stats[tgtDim + i].accept(expected[i] - gridCoordinates[i]);
                }
            }
        }
        return stats;
    }

    /**
     * Returns a string representation of this builder in the given locale.
     * Current implementation shows the following information:
     *
     * <ul>
     *   <li>Number of points.</li>
     *   <li>Linearizers and their correlation coefficients (if available).</li>
     *   <li>The linear component of the transform.</li>
     *   <li>Error statistics, as documented in the {@link #errors(boolean)} method.</li>
     * </ul>
     *
     * The string representation may change in any future version.
     *
     * @param  linear  {@code false} for errors using the complete transform,
     *                 or {@code true} for using only the linear part.
     * @param  locale  the locale for formatting messages and some numbers, or {@code null} for the default.
     * @return a string representation of this builder.
     *
     * @see #errors(boolean)
     *
     * @since 1.1
     */
    public String toString(final boolean linear, final Locale locale) {
        final StringBuilder buffer = new StringBuilder(400);
        String lineSeparator = null;
        try {
            lineSeparator = linearBuilder.appendTo(buffer, getClass(), locale, Vocabulary.Keys.LinearTransformation);
            if (transform != null) {
                buffer.append(Strings.CONTINUATION_ITEM);
                final Vocabulary vocabulary = Vocabulary.forLocale(locale);
                vocabulary.appendLabel(Vocabulary.Keys.Errors, buffer);
                buffer.append(lineSeparator);
                final StatisticsFormat sf;
                if (locale != null) {
                    sf = StatisticsFormat.getInstance(locale);
                } else {
                    sf = StatisticsFormat.getInstance();
                }
                sf.format(errors(linear), buffer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalStateException e) {
            // Ignore - we will not report error statistics.
        }
        Strings.insertLineInLeftMargin(buffer, lineSeparator);
        return buffer.toString();
    }

    /**
     * Returns a string representation of this builder for debugging purpose.
     * The string representation is for debugging purpose and may change in any future version.
     * The default implementation delegates to {@link #toString(boolean, Locale)} with a null locale.
     *
     * @return a string representation of this builder.
     *
     * @since 1.0
     */
    @Override
    public String toString() {
        return toString(false, null);
    }
}
