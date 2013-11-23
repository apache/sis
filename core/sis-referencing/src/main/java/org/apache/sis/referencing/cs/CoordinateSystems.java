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
import org.apache.sis.measure.Units;
import org.apache.sis.util.Static;
import org.apache.sis.util.Classes;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
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
     * Number of directions from "North", "North-North-East", "North-East", etc.
     */
    static final int COMPASS_DIRECTION_COUNT = 16;

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
     * Returns the arithmetic (counterclockwise) angle from the first axis direction to the second direction,
     * in decimal <strong>degrees</strong>. This method returns a value between -180° and +180°,
     * or {@link Double#NaN NaN} if no angle can be computed.
     *
     * <p>A positive angle denotes a right-handed system, while a negative angle denotes a left-handed system.
     * Example:</p>
     *
     * <ul>
     *   <li>The angle from {@link AxisDirection#EAST EAST} to {@link AxisDirection#NORTH NORTH} is 90°</li>
     *   <li>The angle from {@link AxisDirection#SOUTH SOUTH} to {@link AxisDirection#WEST WEST} is -90°</li>
     *   <li>The angle from "<cite>North along 90° East</cite>" to "<cite>North along 0°</cite>" is 90°.</li>
     * </ul>
     *
     * @param  source The source axis direction.
     * @param  target The target axis direction.
     * @return The arithmetic angle (in degrees) of the rotation to apply on a line pointing toward
     *         the source direction in order to make it point toward the target direction, or
     *         {@link Double#NaN} if this value can not be computed.
     */
    public static double angle(final AxisDirection source, final AxisDirection target) {
        ensureNonNull("source", source);
        ensureNonNull("target", target);
        // Tests for NORTH, SOUTH, EAST, EAST-NORTH-EAST, etc. directions.
        final int compass = getCompassAngle(source, target);
        if (compass != Integer.MIN_VALUE) {
            return compass * (360.0 / COMPASS_DIRECTION_COUNT);
        }
        // Tests for "South along 90 deg East", etc. directions.
        final DirectionAlongMeridian src = DirectionAlongMeridian.parse(source);
        if (src != null) {
            final DirectionAlongMeridian tgt = DirectionAlongMeridian.parse(target);
            if (tgt != null) {
                return src.getAngle(tgt);
            }
        }
        return Double.NaN;
    }

    /**
     * Tests for angle on compass only (do not tests angle between direction along meridians).
     * Returns {@link Integer#MIN_VALUE} if the angle can't be computed.
     */
    static int getCompassAngle(final AxisDirection source, final AxisDirection target) {
        final int base = AxisDirection.NORTH.ordinal();
        final int src  = source.ordinal() - base;
        if (src >= 0 && src < COMPASS_DIRECTION_COUNT) {
            int tgt = target.ordinal() - base;
            if (tgt >= 0 && tgt < COMPASS_DIRECTION_COUNT) {
                tgt = src - tgt;
                if (tgt < -COMPASS_DIRECTION_COUNT/2) {
                    tgt += COMPASS_DIRECTION_COUNT;
                } else if (tgt > COMPASS_DIRECTION_COUNT/2) {
                    tgt -= COMPASS_DIRECTION_COUNT;
                }
                return tgt;
            }
        }
        return Integer.MIN_VALUE;
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
     * The two coordinate systems must implement the same GeoAPI coordinate system interface
     * (for example both of them shall implement {@link org.opengis.referencing.cs.CartesianCS},
     * or both of them shall implement {@link org.opengis.referencing.cs.EllipsoidalCS}).
     * Only units and axes order (e.g. transforming from
     * ({@linkplain AxisDirection#NORTH NORTH},{@linkplain AxisDirection#WEST WEST}) to
     * ({@linkplain AxisDirection#EAST EAST},{@linkplain AxisDirection#NORTH NORTH})
     * are taken in account by this method.
     *
     * <blockquote><font size="-1"><b>Example:</b>
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
     * </font></blockquote>
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
