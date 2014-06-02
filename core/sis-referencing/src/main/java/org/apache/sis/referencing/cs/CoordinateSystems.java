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
package org.apache.sis.referencing.cs;

import java.util.Arrays;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.LinearConverter;
import javax.measure.converter.ConversionException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.ElevationAngle;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Static;
import org.apache.sis.util.Classes;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import java.util.Objects;


/**
 * Utility methods working on {@link CoordinateSystem} objects and their axes.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public final class CoordinateSystems extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CoordinateSystems() {
    }

    /**
     * Returns an axis direction code from the given direction name.
     * Names are case-insensitive. They may be:
     *
     * <ul>
     *   <li>Cardinal directions like "<cite>north</cite>" and "<cite>east</cite>".</li>
     *   <li>Inter-cardinal directions "<cite>north-east</cite>" and "<cite>south-south-east</cite>",
     *       using either {@code '-'}, {@code '_'} or spaces as separator between the cardinal points.</li>
     *   <li>Directions from a pole like "<cite>South along 180 deg</cite>" and "<cite>South along 90° East</cite>",
     *       using either the {@code "deg"} or {@code "°"} symbol.</li>
     * </ul>
     *
     * @param  name The direction name (e.g. "north", "north-east", <i>etc.</i>).
     * @return The axis direction for the given name.
     * @throws IllegalArgumentException if the given name is not a known axis direction.
     */
    public static AxisDirection parseAxisDirection(String name) throws IllegalArgumentException {
        ensureNonNull("name", name);
        name = CharSequences.trimWhitespaces(name);
        AxisDirection candidate = AxisDirections.valueOf(name);
        if (candidate != null) {
            return candidate;
        }
        /*
         * Some EPSG direction names are of the form "South along 180 deg". We check that the
         * direction before "along" is valid and create a new axis direction if it is. We can
         * not just replace "South along 180 deg" by "South" because the same CRS may use two
         * of those directions. For example EPSG:32661 has the following axis direction:
         *
         * South along 180 deg
         * South along 90 deg East
         */
        final DirectionAlongMeridian meridian = DirectionAlongMeridian.parse(name);
        if (meridian != null) {
            candidate = meridian.getDirection();
            assert candidate == AxisDirections.valueOf(meridian.toString());
            return candidate;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownAxisDirection_1, name));
    }

    /**
     * Returns the arithmetic (counterclockwise) angle from the first axis direction to the second direction.
     * This method returns a value between -180° and +180°, or {@code null} if no angle can be computed.
     *
     * {@section Horizontal directions}
     * For any pair of compass directions which are not opposite directions, a positive angle denotes
     * a right-handed system while a negative angle denotes a left-handed system. Examples:
     *
     * <ul>
     *   <li>The angle from {@link AxisDirection#EAST EAST} to {@link AxisDirection#NORTH NORTH} is 90°</li>
     *   <li>The angle from {@link AxisDirection#SOUTH SOUTH} to {@link AxisDirection#WEST WEST} is -90°</li>
     *   <li>The angle from "<cite>North along 90° East</cite>" to "<cite>North along 0°</cite>" is 90°.</li>
     * </ul>
     *
     * {@section Horizontal and vertical directions}
     * By convention this method defines the angle from any compass direction to the {@link AxisDirection#UP UP}
     * vertical direction as 90°, and the angle of any compass direction to the {@link AxisDirection#DOWN DOWN}
     * vertical direction as -90°. The sign of those angles gives no indication about whether the coordinate system
     * is right-handed or left-handed. Those angles are returned as instances of {@link ElevationAngle}.
     *
     * <p>All angles are approximative since this method does not take the Earth ellipsoidal or geoidal shape in
     * account.</p>
     *
     * {@section Invariants}
     * For any non-null return value:
     * <ul>
     *   <li>{@code angle(A, A) = 0°}</li>
     *   <li>{@code angle(A, opposite(A)) = ±180°}</li>
     *   <li>{@code angle(A, B) = -angle(B, A)}</li>
     * </ul>
     *
     * @param  source The source axis direction.
     * @param  target The target axis direction.
     * @return The arithmetic angle (in degrees) of the rotation to apply on a line pointing toward
     *         the source direction in order to make it point toward the target direction, or
     *         {@code null} if this value can not be computed.
     */
    public static Angle angle(final AxisDirection source, final AxisDirection target) {
        ensureNonNull("source", source);
        ensureNonNull("target", target);
        /*
         * Check for NORTH, SOUTH, EAST, EAST-NORTH-EAST, etc.
         * Checked first because this is the most common case.
         */
        int c = AxisDirections.angleForCompass(source, target);
        if (c != Integer.MIN_VALUE) {
            return new Angle(c * (360.0 / AxisDirections.COMPASS_COUNT));
        }
        /*
         * Check for GEOCENTRIC_X, GEOCENTRIC_Y, GEOCENTRIC_Z.
         */
        c = AxisDirections.angleForGeocentric(source, target);
        if (c != Integer.MIN_VALUE) {
            return new Angle(c * 90);
        }
        /*
         * Check for DISPLAY_UP, DISPLAY_DOWN, etc. assuming a flat screen.
         * Note that we do not check for grid directions (COLUMN_POSITIVE,
         * ROW_POSITIVE, etc.) because the grid geometry may be anything.
         */
        c = AxisDirections.angleForDisplay(source, target);
        if (c != Integer.MIN_VALUE) {
            return new Angle(c * (360 / AxisDirections.DISPLAY_COUNT));
        }
        /*
         * Check for "South along 90° East", etc. directions. Note that this
         * check may perform a relatively costly parsing of axis direction name.
         * (NOTE: the check for 'isUserDefined' is performed outside DirectionAlongMeridian for
         * avoiding class initialization of the later in the common case where we do not need it).
         */
        final DirectionAlongMeridian srcMeridian, tgtMeridian;
        srcMeridian = AxisDirections.isUserDefined(source) ? DirectionAlongMeridian.parse(source) : null;
        tgtMeridian = AxisDirections.isUserDefined(target) ? DirectionAlongMeridian.parse(target) : null;
        if (srcMeridian != null && tgtMeridian != null) {
            return new Angle(srcMeridian.angle(tgtMeridian));
        }
        /*
         * Check for UP and DOWN, with special case if one of the direction is horizontal
         * (either a compass direction of a direction along a meridian).
         */
        final boolean srcVrt = AxisDirections.isVertical(source);
        final boolean tgtVrt = AxisDirections.isVertical(target);
        if (tgtVrt) {
            if (srcVrt) {
                return new Angle(source.equals(target) ? 0 : target.equals(AxisDirection.UP) ? 180 : -180);
            } else if (AxisDirections.isCompass(source) || srcMeridian != null) {
                return target.equals(AxisDirection.UP) ? ElevationAngle.ZENITH : ElevationAngle.NADIR;
            }
        } else if (srcVrt) {
            if (AxisDirections.isCompass(target) || tgtMeridian != null) {
                return source.equals(AxisDirection.UP) ? ElevationAngle.NADIR : ElevationAngle.ZENITH;
            }
        }
        return null;
    }

    /**
     * Returns the axis direction for the specified coordinate system.
     *
     * @param  cs The coordinate system.
     * @return The axis directions for the specified coordinate system.
     */
    private static AxisDirection[] getAxisDirections(final CoordinateSystem cs) {
        final AxisDirection[] directions = new AxisDirection[cs.getDimension()];
        for (int i=0; i<directions.length; i++) {
            directions[i] = cs.getAxis(i).getDirection();
        }
        return directions;
    }

    /**
     * Returns an affine transform between two coordinate systems.
     * The two coordinate systems must implement the same GeoAPI coordinate system interface.
     * For example if {@code sourceCRS} is a {@link org.opengis.referencing.cs.CartesianCS},
     * then {@code targetCRS} must be a {@code CartesianCS} too.
     * Only units and axes order (e.g. transforming from
     * ({@linkplain AxisDirection#NORTH North}, {@linkplain AxisDirection#WEST West}) to
     * ({@linkplain AxisDirection#EAST East}, {@linkplain AxisDirection#NORTH North})
     * are taken in account by this method.
     *
     * <div class="note"><b>Example:</b>
     * If coordinates in {@code sourceCS} are (<var>x</var>,<var>y</var>) tuples in metres
     * and coordinates in {@code targetCS} are (<var>-y</var>,<var>x</var>) tuples in centimetres,
     * then the transformation can be performed as below:
     *
     * {@preformat math
     *     ┌      ┐   ┌                ┐ ┌     ┐
     *     │-y(cm)│   │   0  -100    0 │ │ x(m)│
     *     │ x(cm)│ = │ 100     0    0 │ │ y(m)│
     *     │ 1    │   │   0     0    1 │ │ 1   │
     *     └      ┘   └                ┘ └     ┘
     * }
     * </div>
     *
     * @param  sourceCS The source coordinate system.
     * @param  targetCS The target coordinate system.
     * @return The conversion from {@code sourceCS} to {@code targetCS} as an affine transform.
     *         Only axis direction and units are taken in account.
     * @throws IllegalArgumentException if the CS are not of the same type, or axes do not match.
     * @throws ConversionException if the units are not compatible, or the conversion is non-linear.
     *
     * @see Matrices#createTransform(AxisDirection[], AxisDirection[])
     */
    public static Matrix swapAndScaleAxes(final CoordinateSystem sourceCS,
                                          final CoordinateSystem targetCS)
            throws IllegalArgumentException, ConversionException
    {
        if (!Classes.implementSameInterfaces(sourceCS.getClass(), targetCS.getClass(), CoordinateSystem.class)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleCoordinateSystemTypes));
        }
        final AxisDirection[] sourceAxis = getAxisDirections(sourceCS);
        final AxisDirection[] targetAxis = getAxisDirections(targetCS);
        final MatrixSIS matrix = Matrices.createTransform(sourceAxis, targetAxis);
        assert Arrays.equals(sourceAxis, targetAxis) == matrix.isIdentity() : matrix;
        /*
         * The previous code computed a matrix for swapping axes. Usually, this
         * matrix contains only 0 and 1 values with only one "1" value by row.
         * For example, the matrix operation for swapping x and y axes is:
         *          ┌ ┐   ┌         ┐ ┌ ┐
         *          │y│   │ 0  1  0 │ │x│
         *          │x│ = │ 1  0  0 │ │y│
         *          │1│   │ 0  0  1 │ │1│
         *          └ ┘   └         ┘ └ ┘
         * Now, take in account units conversions. Each matrix's element (j,i)
         * is multiplied by the conversion factor from sourceCS.getUnit(i) to
         * targetCS.getUnit(j). This is an element-by-element multiplication,
         * not a matrix multiplication. The last column is processed in a special
         * way, since it contains the offset values.
         */
        final int sourceDim = matrix.getNumCol() - 1;  // == sourceCS.getDimension()
        final int targetDim = matrix.getNumRow() - 1;  // == targetCS.getDimension()
        for (int j=0; j<targetDim; j++) {
            final Unit<?> targetUnit = targetCS.getAxis(j).getUnit();
            for (int i=0; i<sourceDim; i++) {
                final double element = matrix.getElement(j,i);
                if (element == 0) {
                    // There is no dependency between source[i] and target[j]
                    // (i.e. axes are orthogonal).
                    continue;
                }
                final Unit<?> sourceUnit = sourceCS.getAxis(i).getUnit();
                if (Objects.equals(sourceUnit, targetUnit)) {
                    // There is no units conversion to apply
                    // between source[i] and target[j].
                    continue;
                }
                final UnitConverter converter = sourceUnit.getConverterToAny(targetUnit);
                if (!(converter instanceof LinearConverter)) {
                    throw new ConversionException(Errors.format(
                              Errors.Keys.NonLinearUnitConversion_2, sourceUnit, targetUnit));
                }
                final double offset = converter.convert(0);
                final double scale  = Units.derivative(converter, 0);
                matrix.setElement(j, i, element*scale);
                matrix.setElement(j, sourceDim, matrix.getElement(j, sourceDim) + element*offset);
            }
        }
        return matrix;
    }
}
