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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.ElevationAngle;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Static;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * Utility methods working on {@link CoordinateSystem} objects and their axes.
 * Those methods allow for example to {@linkplain #angle estimate an angle between two axes}
 * or {@linkplain #swapAndScaleAxes determining the change of axis directions and units}
 * between two coordinate systems.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.4
 */
public final class CoordinateSystems extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CoordinateSystems() {
    }

    /**
     * Returns whether the given coordinate system can be associated to a {@link org.opengis.referencing.crs.GeodeticCRS}.
     * This is true for instances of {@link EllipsoidalCS}, {@link CartesianCS} and {@link SphericalCS},
     * and false for all other types of coordinate system.
     *
     * @param  cs  the coordinate system to test (can be {@code null}).
     * @return whether the given coordinate system can be associated to a geodetic CRS.
     *
     * @see #getSingleComponents(CoordinateSystem)
     * @since 1.3
     */
    public static boolean isGeodetic(final CoordinateSystem cs) {
        return (cs instanceof EllipsoidalCS) || (cs instanceof CartesianCS) || (cs instanceof SphericalCS);
    }

    /**
     * Returns an axis direction code from the given direction name.
     * Names are case-insensitive. They may be:
     *
     * <ul>
     *   <li>Cardinal directions like <q>north</q> and <q>east</q>.</li>
     *   <li>Inter-cardinal directions <q>north-east</q> and <q>south-south-east</q>,
     *       using either {@code '-'}, {@code '_'} or spaces as separator between the cardinal points.</li>
     *   <li>Directions from a pole like <q>South along 180 deg</q> and <q>South along 90° East</q>,
     *       using either the {@code "deg"} or {@code "°"} symbol. Note that the meridian is not necessarily relative
     *       to Greenwich (see {@link #directionAlongMeridian directionAlongMeridian(…)} for more information).</li>
     * </ul>
     *
     * @param  name  the direction name (e.g. "north", "north-east", <i>etc.</i>).
     * @return the axis direction for the given name.
     * @throws IllegalArgumentException if the given name is not a known axis direction.
     */
    public static AxisDirection parseAxisDirection(String name) throws IllegalArgumentException {
        ArgumentChecks.ensureNonEmpty("name", name);
        name = name.strip();
        AxisDirection candidate = AxisDirections.valueOf(name);
        if (candidate != null) {
            return candidate;
        }
        /*
         * Some EPSG direction names are of the form "South along 180 deg". We check that the
         * direction before "along" is valid and create a new axis direction if it is. We can
         * not just replace "South along 180 deg" by "South" because the same CRS may use two
         * of those directions. For example, EPSG:32661 has the following axis direction:
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
        throw new IllegalArgumentException(Resources.format(Resources.Keys.UnknownAxisDirection_1, name));
    }

    /**
     * Returns an axis direction from a pole along a meridian.
     * The given meridian is usually, but not necessarily, relative to the Greenwich meridian.
     *
     * <h4>Reference meridian</h4>
     * The reference meridian depends on the context. It is usually the prime meridian of the
     * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic reference frame} of the
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS} instance
     * that contains (through its coordinate system) the axes having those directions.
     * This policy is consistent with <abbr>WKT</abbr> 2 specification.
     *
     * <h4>Example</h4>
     * {@code directionAlongMeridian(AxisDirection.SOUTH, -90)} returns an axis direction for
     * <q>South along 90°W</q>.
     *
     * @param  baseDirection  the base direction, which must be {@link AxisDirection#NORTH} or {@link AxisDirection#SOUTH}.
     * @param  meridian       the meridian in degrees, relative to a unspecified (usually Greenwich) prime meridian.
     *         Meridians in the East hemisphere are positive and meridians in the West hemisphere are negative.
     * @return the axis direction along the given meridian.
     *
     * @since 0.6
     */
    public static AxisDirection directionAlongMeridian(final AxisDirection baseDirection, final double meridian) {
        return new DirectionAlongMeridian(baseDirection, meridian).getDirection();
    }

    /**
     * Returns {@code true} if the given axis direction seems to be a direction along a meridian.
     *
     * <table class="sis">
     *   <caption>Examples</caption>
     *   <tr><th>Axis name</th>               <th>Return value</th></tr>
     *   <tr><td>North along 90 deg East</td> <td>{@code true}</td></tr>
     *   <tr><td>South along 90 deg East</td> <td>{@code true}</td></tr>
     *   <tr><td>South</td>                   <td>{@code false}</td></tr>
     *   <tr><td>East</td>                    <td>{@code false}</td></tr>
     * </table>
     *
     * Note that {@code true} is not a guarantee that {@link #parseAxisDirection(String)} will succeed.
     * But it means that there is reasonable chances of success based on brief inspection of axis name.
     *
     * @param  direction  the direction to test. Can be null.
     * @return if the given direction is non-null and seems to be a direction along a meridian.
     *
     * @since 1.2
     */
    public static boolean isAlongMeridian(final AxisDirection direction) {
        return AxisDirections.isUserDefined(direction) && DirectionAlongMeridian.matches(direction.name());
    }

    /**
     * Returns the arithmetic (counterclockwise) angle from the first axis direction to the second direction.
     * This method returns a value between -180° and +180°, or {@code null} if no angle can be computed.
     *
     * <h4>Horizontal directions</h4>
     * For any pair of compass directions which are not opposite directions, a positive angle denotes
     * a right-handed system while a negative angle denotes a left-handed system. Examples:
     *
     * <ul>
     *   <li>The angle from {@link AxisDirection#EAST EAST} to {@link AxisDirection#NORTH NORTH} is 90°</li>
     *   <li>The angle from {@link AxisDirection#SOUTH SOUTH} to {@link AxisDirection#WEST WEST} is -90°</li>
     *   <li>The angle from <q>North along 90° East</q> to <q>North along 0°</q> is 90°.</li>
     * </ul>
     *
     * In the case of directions like <q>South along 90°W</q>, the caller is responsible to make sure
     * that the meridians are relative to the same prime meridian. This is the case if the axes are part of
     * the same {@code CoordinateSystem} instance.
     *
     * <h4>Horizontal and vertical directions</h4>
     * By convention this method defines the angle from any compass direction to the {@link AxisDirection#UP UP}
     * vertical direction as 90°, and the angle of any compass direction to the {@link AxisDirection#DOWN DOWN}
     * vertical direction as -90°. The sign of those angles gives no indication about whether the coordinate system
     * is right-handed or left-handed. Those angles are returned as instances of {@link ElevationAngle}.
     *
     * <p>All angles are approximations since this method does not take the Earth ellipsoidal or geoidal shape in
     * account.</p>
     *
     * <h4>Invariants</h4>
     * For any non-null return value:
     * <ul>
     *   <li>{@code angle(A, A) = 0°}</li>
     *   <li>{@code angle(A, opposite(A)) = ±180°}</li>
     *   <li>{@code angle(A, B) = -angle(B, A)}</li>
     * </ul>
     *
     * @param  source  the source axis direction.
     * @param  target  the target axis direction.
     * @return the arithmetic angle (in degrees) of the rotation to apply on a line pointing toward
     *         the source direction in order to make it point toward the target direction, or
     *         {@code null} if this value cannot be computed.
     */
    public static Angle angle(final AxisDirection source, final AxisDirection target) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
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
         * Check for FORWARD, AFT, PORT, STARBOARD.
         */
        c = AxisDirections.angleForVehicle(source, target);
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
         * avoiding class initialization of the latter in the common case where we do not need it).
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
     * Returns {@code true} if all {@code CoordinateSystem} interfaces of {@code targetCS} have a counterpart in
     * {@code sourceCS}. This method is equivalent to {@link Classes#implementSameInterfaces(Class, Class, Class)}
     * except that it decomposes {@link DefaultCompoundCS} in its components before to check the two collections
     * of interfaces.
     *
     * <h4>Example</h4>
     * If {@code sourceCS} is a {@link DefaultCompoundCS} containing {@link EllipsoidalCS} and a vertical or temporal
     * coordinate system and {@code targetCS} is an {@link EllipsoidalCS} only, then this method returns {@code true}.
     * But if {@code targetCS} is a {@link CartesianCS} or contains any other CS which is not a component of source CS,
     * then this method returns {@code false}.
     */
    static boolean hasAllTargetTypes(final CoordinateSystem sourceCS, final CoordinateSystem targetCS) {
        final List<CoordinateSystem> sources = getSingleComponents(sourceCS);
        final List<CoordinateSystem> targets = getSingleComponents(targetCS);
next:   for (final CoordinateSystem cs : targets) {
            for (int i=0; i<sources.size(); i++) {
                if (Classes.implementSameInterfaces(sources.get(i).getClass(), cs.getClass(), CoordinateSystem.class)) {
                    sources.remove(i);
                    continue next;
                }
            }
            return false;           // Found no `sourceCS` component for at least one of the `targetCS` components.
        }
        return true;
    }

    /**
     * Returns an affine transform between two coordinate systems.
     * Only units and axes order (e.g. transforming from
     * ({@linkplain AxisDirection#NORTH North}, {@linkplain AxisDirection#WEST West}) to
     * ({@linkplain AxisDirection#EAST East}, {@linkplain AxisDirection#NORTH North})
     * are taken in account by this method.
     *
     * <h4>Conditions</h4>
     * The two coordinate systems must implement the same GeoAPI coordinate system interface.
     * For example if {@code sourceCS} is a {@link org.opengis.referencing.cs.CartesianCS},
     * then {@code targetCS} must be a {@code CartesianCS} too.
     *
     * <h4>Example</h4>
     * If coordinates in {@code sourceCS} are (<var>x</var>,<var>y</var>) tuples in metres
     * and coordinates in {@code targetCS} are (<var>-y</var>,<var>x</var>) tuples in centimetres,
     * then the transformation can be performed as below:
     *
     * <pre class="math">
     *     ┌      ┐   ┌                ┐ ┌     ┐
     *     │-y(cm)│   │   0  -100    0 │ │ x(m)│
     *     │ x(cm)│ = │ 100     0    0 │ │ y(m)│
     *     │ 1    │   │   0     0    1 │ │ 1   │
     *     └      ┘   └                ┘ └     ┘</pre>
     *
     * @param  sourceCS  the source coordinate system.
     * @param  targetCS  the target coordinate system.
     * @return the conversion from {@code sourceCS} to {@code targetCS} as an affine transform.
     *         Only axis direction and units are taken in account.
     * @throws IllegalArgumentException if the CS are not of the same type, or axes do not match.
     * @throws IncommensurableException if the units are not compatible, or the conversion is non-linear.
     *
     * @see Matrices#createTransform(AxisDirection[], AxisDirection[])
     */
    @SuppressWarnings("fallthrough")
    public static Matrix swapAndScaleAxes(final CoordinateSystem sourceCS,
                                          final CoordinateSystem targetCS)
            throws IllegalArgumentException, IncommensurableException
    {
        if (sourceCS == targetCS) {     // Quick optimization for a common case.
            return Matrices.createIdentity(sourceCS.getDimension() + 1);
        }
        if (!Classes.implementSameInterfaces(sourceCS.getClass(), targetCS.getClass(), CoordinateSystem.class)) {
            // Above line was a relatively cheap test. Try the more expensive test below only if necessary.
            if (!hasAllTargetTypes(sourceCS, targetCS)) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.IncompatibleCoordinateSystemTypes));
            }
        }
        final AxisDirection[] srcAxes = getAxisDirections(sourceCS);
        final AxisDirection[] dstAxes = getAxisDirections(targetCS);
        final MatrixSIS matrix = Matrices.createTransform(srcAxes, dstAxes);
        assert Arrays.equals(srcAxes, dstAxes) == matrix.isIdentity() : matrix;
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
        final int sourceDim = matrix.getNumCol() - 1;                       // == sourceCS.getDimension()
        final int targetDim = matrix.getNumRow() - 1;                       // == targetCS.getDimension()
        for (int j=0; j<targetDim; j++) {
            final Unit<?> targetUnit = targetCS.getAxis(j).getUnit();
            for (int i=0; i<sourceDim; i++) {
                if (matrix.getElement(j,i) == 0) {
                    // There are no dependencies between source[i] and target[j]
                    // (i.e. axes are orthogonal).
                    continue;
                }
                final Unit<?> sourceUnit = sourceCS.getAxis(i).getUnit();
                if (Objects.equals(sourceUnit, targetUnit)) {
                    // There are no units conversion to apply
                    // between source[i] and target[j].
                    continue;
                }
                Number scale  = 1;
                Number offset = 0;
                final Number[] coefficients = Units.coefficients(sourceUnit.getConverterToAny(targetUnit));
                switch (coefficients != null ? coefficients.length : -1) {
                    case 2:  scale  = coefficients[1];       // Fall through
                    case 1:  offset = coefficients[0];       // Fall through
                    case 0:  break;
                    default: throw new IncommensurableException(Resources.format(
                                Resources.Keys.NonLinearUnitConversion_2, sourceUnit, targetUnit));
                }
                final boolean decimal = true;   // Whether values were intended to be exact in base 10.
                final var shift  = DoubleDouble.of(matrix.getNumber(j, sourceDim), decimal);
                final var factor = DoubleDouble.of(matrix.getNumber(j, i), decimal);
                matrix.setNumber(j, i,         factor.multiply(scale,  decimal));
                matrix.setNumber(j, sourceDim, factor.multiply(offset, decimal).add(shift));
            }
        }
        return matrix;
    }

    /**
     * Returns a coordinate system derived from the given one but with a modified list of axes.
     * The axes may be filtered (excluding some axes), reordered or have their unit and direction modified.
     *
     * <h4>Example</h4>
     * for replacing all angular units of a coordinate system to degrees (regardless what the original
     * angular units were) while leaving other kinds of units unchanged, one can write:
     *
     * {@snippet lang="java" :
     *     CoordinateSystem cs = ...;
     *     cs = CoordinateSystems.replaceAxes(cs, new AxisFilter() {
     *         @Override
     *         public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
     *             if (Units.isAngular(unit)) {
     *                 unit = Units.DEGREE;
     *             }
     *             return unit;
     *         }
     *     });
     *     }
     *
     * <h4>Coordinate system normalization</h4>
     * This method is often used together with {@link #swapAndScaleAxes swapAndScaleAxes(…)} for normalizing the
     * coordinate values given to a {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform
     * math transform}. For example:
     *
     * {@snippet lang="java" :
     *     CoordinateSystem sourceCS = ...;
     *     CoordinateSystem targetCS = ...;
     *     Matrix step1 = swapAndScaleAxes(sourceCS, replaceAxes(sourceCS, AxisConvention.NORMALIZED));
     *     Matrix step2 = ...; // some transform working on coordinates with standard axis order and unit.
     *     Matrix step3 = swapAndScaleAxes(replaceAxes(targetCS, AxisConvention.NORMALIZED), targetCS);
     *     }
     *
     * @param  cs      the coordinate system, or {@code null}.
     * @param  filter  the modifications to apply on coordinate system axes.
     * @return the modified coordinate system as a new instance,
     *         or {@code cs} if the given coordinate system was null or does not need any change.
     * @throws IllegalArgumentException if the specified coordinate system cannot be filtered.
     *         It may be because the coordinate system would contain an illegal number of axes,
     *         or because an axis would have an unexpected direction or unexpected unit of measurement.
     *
     * @see AxesConvention#NORMALIZED
     *
     * @since 0.6
     */
    public static CoordinateSystem replaceAxes(final CoordinateSystem cs, final AxisFilter filter) {
        ArgumentChecks.ensureNonNull("filter", filter);
        if (cs != null) {
            final CoordinateSystem newCS;
            if (filter instanceof AxesConvention) {
                if (cs instanceof AbstractCS) {
                    // User may have overridden the 'forConvention' method.
                    return ((AbstractCS) cs).forConvention((AxesConvention) filter);
                } else {
                    newCS = Normalizer.forConvention(cs, (AxesConvention) filter);
                }
            } else {
                newCS = Normalizer.normalize(cs, filter, false);
            }
            if (newCS != null) {
                return newCS;
            }
        }
        return cs;
    }

    /**
     * Returns a coordinate system derived from the given one but with all linear units replaced by the given unit.
     * Non-linear units (e.g. angular or scale units) are left unchanged.
     *
     * <p>This convenience method is equivalent to the following code:</p>
     * {@snippet lang="java" :
     *     return CoordinateSystems.replaceAxes(cs, new AxisFilter() {
     *         @Override public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
     *             return Units.isLinear(unit) ? newUnit : unit;
     *         }
     *     });
     *     }
     *
     * @param  cs       the coordinate system in which to replace linear units, or {@code null}.
     * @param  newUnit  the new linear unit.
     * @return the modified coordinate system as a new instance, or {@code null} if the given {@code cs} was null,
     *         or {@code cs} if all linear units were already equal to the given one.
     *
     * @see Units#isLinear(Unit)
     *
     * @since 0.7
     */
    public static CoordinateSystem replaceLinearUnit(final CoordinateSystem cs, final Unit<Length> newUnit) {
        ArgumentChecks.ensureNonNull("newUnit", newUnit);
        return replaceAxes(cs, new AxisFilter() {
            @Override public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
                return Units.isLinear(unit) ? newUnit : unit;
            }
        });
    }

    /**
     * Returns a coordinate system derived from the given one but with all angular units replaced by the given unit.
     * Non-angular units (e.g. linear or scale units) are left unchanged.
     *
     * <p>This convenience method is equivalent to the following code:</p>
     * {@snippet lang="java" :
     *     return CoordinateSystems.replaceAxes(cs, new AxisFilter() {
     *         @Override public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
     *             return Units.isAngular(unit) ? newUnit : unit;
     *         }
     *     });
     *     }
     *
     * @param  cs       the coordinate system in which to replace angular units, or {@code null}.
     * @param  newUnit  the new angular unit.
     * @return the modified coordinate system as a new instance, or {@code null} if the given {@code cs} was null,
     *         or {@code cs} if all angular units were already equal to the given one.
     *
     * @see Units#isAngular(Unit)
     *
     * @since 0.7
     */
    public static CoordinateSystem replaceAngularUnit(final CoordinateSystem cs, final Unit<javax.measure.quantity.Angle> newUnit) {
        ArgumentChecks.ensureNonNull("newUnit", newUnit);
        return replaceAxes(cs, new AxisFilter() {
            @Override public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
                return Units.isAngular(unit) ? newUnit : unit;
            }
        });
    }

    /**
     * Returns all components of the given coordinate system.
     * If the given coordinate system (<abbr>CS</abbr>) is null, then this method returns an empty list.
     * Otherwise, if the given <abbr>CS</abbr> is <em>not</em> an instance of {@link DefaultCompoundCS},
     * then this method adds that <abbr>CS</abbr> as the singleton element of the returned list.
     * Otherwise, this method returns all {@linkplain DefaultCompoundCS#getComponents() components}.
     * If a component is itself a {@link DefaultCompoundCS}, its is recursively decomposed into its
     * singleton component.
     *
     * <h4>Implementation note</h4>
     * This method always returns a modifiable list.
     * Callers can freely modify that list without impacting the coordinate system.
     *
     * @param  cs  the coordinate system to decompose into singleton components, or {@code null}.
     * @return the components, or an empty list if {@code cs} is null.
     *
     * @see #isGeodetic(CoordinateSystem)
     * @since 1.5
     */
    public static List<CoordinateSystem> getSingleComponents(final CoordinateSystem cs) {
        final var addTo = new ArrayList<CoordinateSystem>(3);
        getSingleComponents(cs, addTo);
        return addTo;
    }

    /**
     * Recursively adds all single coordinate systems in the given list.
     */
    private static void getSingleComponents(final CoordinateSystem cs, final List<CoordinateSystem> addTo) {
        if (cs != null) {
            if (cs instanceof DefaultCompoundCS) {
                for (final CoordinateSystem c : ((DefaultCompoundCS) cs).getComponents()) {
                    getSingleComponents(c, addTo);
                }
            } else {
                addTo.add(cs);
            }
        }
    }

    /**
     * Returns the axis directions for the specified coordinate system.
     * This method guarantees that the returned array is non-null and does not contain any null direction.
     *
     * @param  cs  the coordinate system.
     * @return the axis directions for the specified coordinate system.
     * @throws NullPointerException if {@code cs} is null, or one of its axes is null,
     *         or a value returned by {@link CoordinateSystemAxis#getDirection()} is null.
     *
     * @since 0.8
     */
    public static AxisDirection[] getAxisDirections(final CoordinateSystem cs) {
        final var directions = new AxisDirection[cs.getDimension()];
        for (int i=0; i<directions.length; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            ArgumentChecks.ensureNonNullElement("cs", i, cs);
            ArgumentChecks.ensureNonNullElement("cs[#].direction", i, directions[i] = axis.getDirection());
        }
        return directions;
    }

    /**
     * Returns the axis directions, replacing "North/South along meridian" by a cardinal direction.
     * When a {@linkplain #isAlongMeridian direction along a meridian} is detected,
     * this method uses the axis abbreviation for the direction by East or North.
     *
     * <h4>Example</h4>
     * The <q>WGS 84 / UPS South (E,N)</q> coordinate reference system has two axis
     * oriented toward North: <q>North along 90°E</q> and <q>North along 0°E</q>.
     * Those axes are conventionally named <q>Easting (E)</q> and <q>Northing (N)</q>.
     * This method uses those conventional names for returning (east, north) directions.
     *
     * @param  cs  the coordinate system.
     * @return the simple axis directions for the specified coordinate system.
     * @throws NullPointerException if {@code cs} is null, or one of its axes or directions is null.
     *
     * @since 1.5
     */
    public static AxisDirection[] getSimpleAxisDirections(final CoordinateSystem cs) {
        final var directions = getAxisDirections(cs);
        for (int i=0; i<directions.length; i++) {
            if (isAlongMeridian(directions[i])) {
                final String abbreviation = cs.getAxis(i).getAbbreviation();
                if (abbreviation != null && abbreviation.length() == 1) {
                    AxisDirection r = AxisDirections.fromAbbreviation(abbreviation.charAt(0));
                    if (r != null) directions[i] = r;
                }
            }
        }
        return directions;
    }

    /**
     * Returns a short (if possible) localized name for the given axis. This method replaces
     * names such as "Geodetic latitude" or "Geocentric latitude" by a simple "Latitude" word.
     * This method can be used for example in column or row headers when the context is known
     * and the space is rare.
     *
     * @param  axis    the axis for which to get a short label.
     * @param  locale  desired locale for the label, or {@code null} for the default.
     * @return a relatively short axis label, in the desired locale if possible.
     *
     * @since 1.3
     */
    public static String getShortName(final CoordinateSystemAxis axis, final Locale locale) {
        return AxisName.find(Objects.requireNonNull(axis), locale);
    }

    /**
     * Returns the EPSG code of a coordinate system using the units and directions of given axes.
     * This method ignores axis metadata (names, abbreviation, identifiers, remarks, <i>etc.</i>).
     * The axis minimum and maximum values are checked only if the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() range meaning} is "wraparound".
     * If no suitable coordinate system is known to Apache SIS, then this method returns {@code null}.
     *
     * <p>Current implementation uses a hard-coded list of known coordinate systems;
     * it does not yet scan the EPSG database (this may change in future Apache SIS version).
     * The current list of known coordinate systems is given below.</p>
     *
     * <table class="sis">
     *   <caption>Known coordinate systems (CS)</caption>
     *   <tr><th>EPSG</th> <th>CS type</th> <th colspan="3">Axis directions</th> <th>Horizontal unit</th></tr>
     *   <tr><td>6424</td> <td>Ellipsoidal</td> <td>east</td>  <td>north</td> <td></td>   <td>degree</td></tr>
     *   <tr><td>6422</td> <td>Ellipsoidal</td> <td>north</td> <td>east</td>  <td></td>   <td>degree</td></tr>
     *   <tr><td>6425</td> <td>Ellipsoidal</td> <td>east</td>  <td>north</td> <td></td>   <td>grads</td></tr>
     *   <tr><td>6403</td> <td>Ellipsoidal</td> <td>north</td> <td>east</td>  <td></td>   <td>grads</td></tr>
     *   <tr><td>6429</td> <td>Ellipsoidal</td> <td>east</td>  <td>north</td> <td></td>   <td>radian</td></tr>
     *   <tr><td>6428</td> <td>Ellipsoidal</td> <td>north</td> <td>east</td>  <td></td>   <td>radian</td></tr>
     *   <tr><td>6426</td> <td>Ellipsoidal</td> <td>east</td>  <td>north</td> <td>up</td> <td>degree</td></tr>
     *   <tr><td>6423</td> <td>Ellipsoidal</td> <td>north</td> <td>east</td>  <td>up</td> <td>degree</td></tr>
     *   <tr><td>6427</td> <td>Ellipsoidal</td> <td>east</td>  <td>north</td> <td>up</td> <td>grads</td></tr>
     *   <tr><td>6421</td> <td>Ellipsoidal</td> <td>north</td> <td>east</td>  <td>up</td> <td>grads</td></tr>
     *   <tr><td>6431</td> <td>Ellipsoidal</td> <td>east</td>  <td>north</td> <td>up</td> <td>radian</td></tr>
     *   <tr><td>6430</td> <td>Ellipsoidal</td> <td>north</td> <td>east</td>  <td>up</td> <td>radian</td></tr>
     *   <tr><td>4400</td> <td>Cartesian</td>   <td>east</td>  <td>north</td> <td></td>   <td>metre</td></tr>
     *   <tr><td>4500</td> <td>Cartesian</td>   <td>north</td> <td>east</td>  <td></td>   <td>metre</td></tr>
     *   <tr><td>4491</td> <td>Cartesian</td>   <td>west</td>  <td>north</td> <td></td>   <td>metre</td></tr>
     *   <tr><td>4501</td> <td>Cartesian</td>   <td>north</td> <td>west</td>  <td></td>   <td>metre</td></tr>
     *   <tr><td>6503</td> <td>Cartesian</td>   <td>west</td>  <td>south</td> <td></td>   <td>metre</td></tr>
     *   <tr><td>6501</td> <td>Cartesian</td>   <td>south</td> <td>west</td>  <td></td>   <td>metre</td></tr>
     *   <tr><td>1039</td> <td>Cartesian</td>   <td>east</td>  <td>north</td> <td></td>   <td>foot</td></tr>
     *   <tr><td>1029</td> <td>Cartesian</td>   <td>north</td> <td>east</td>  <td></td>   <td>foot</td></tr>
     *   <tr><td>4403</td> <td>Cartesian</td>   <td>east</td>  <td>north</td> <td></td>   <td>Clarke’s foot</td></tr>
     *   <tr><td>4502</td> <td>Cartesian</td>   <td>north</td> <td>east</td>  <td></td>   <td>Clarke’s foot</td></tr>
     *   <tr><td>4497</td> <td>Cartesian</td>   <td>east</td>  <td>north</td> <td></td>   <td>US survey foot</td></tr>
     * </table>
     *
     * @param  type  the type of coordinate system for which an EPSG code is desired, as a GeoAPI interface.
     * @param  axes  axes for which a coordinate system EPSG code is desired.
     * @return EPSG codes for a coordinate system using the given axes (ignoring metadata), or {@code null} if unknown
     *         to this method. Note that a null value does not mean that a more  extensive search in the EPSG database
     *         would not find a matching coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createCoordinateSystem(String)
     *
     * @since 1.0
     */
    @SuppressWarnings("fallthrough")
    public static Integer getEpsgCode(final Class<? extends CoordinateSystem> type, final CoordinateSystemAxis... axes) {
        ArgumentChecks.ensureNonNull("type", type);
forDim: switch (axes.length) {
            case 3: {
                if (!Units.METRE.equals(axes[2].getUnit())) break;      // Restriction in our hard-coded list of codes.
                // Fall through
            }
            case 2: {
                final Unit<?> unit = axes[0].getUnit();
                if (unit != null && unit.equals(axes[1].getUnit())) {
                    final boolean isAngular = Units.isAngular(unit);
                    if ((isAngular && type.isAssignableFrom(EllipsoidalCS.class)) ||
                         Units.isLinear(unit) && type.isAssignableFrom(CartesianCS.class))
                    {
                        /*
                         * Current implementation defines EPSG codes for EllipsoidalCS and CartesianCS only.
                         * Those two coordinate system types can be differentiated by the unit of the two first axes.
                         * If a future implementation supports more CS types, above condition will need to be updated.
                         */
                        final AxisDirection[] directions = new AxisDirection[axes.length];
                        for (int i=0; i<directions.length; i++) {
                            final CoordinateSystemAxis axis = axes[i];
                            ArgumentChecks.ensureNonNullElement("axes", i, axis);
                            directions[i] = axis.getDirection();
                            if (isAngular && axis.getRangeMeaning() == RangeMeaning.WRAPAROUND) try {
                                final UnitConverter uc = unit.getConverterToAny(Units.DEGREE);
                                final double min = uc.convert(axis.getMinimumValue());
                                final double max = uc.convert(axis.getMaximumValue());
                                if ((min > Double.NEGATIVE_INFINITY && Math.abs(min - Longitude.MIN_VALUE) > Formulas.ANGULAR_TOLERANCE) ||
                                    (max < Double.POSITIVE_INFINITY && Math.abs(max - Longitude.MAX_VALUE) > Formulas.ANGULAR_TOLERANCE))
                                {
                                    break forDim;
                                }
                            } catch (IncommensurableException e) {      // Should never happen since we checked that units are angular.
                                Logging.unexpectedException(AbstractCS.LOGGER, CoordinateSystems.class, "getEpsgCode", e);
                                break forDim;
                            }
                        }
                        return getEpsgCode(unit, directions);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the EPSG code of a coordinate system using the given unit and axis directions.
     * This convenience method performs the work documented in {@link #getEpsgCode(Class, CoordinateSystemAxis...)},
     * but requiring only a frequently used subset of information.
     * If no suitable coordinate system is known to Apache SIS, then this method returns {@code null}.
     *
     * <p>Current implementation uses a hard-coded list of known coordinate systems;
     * it does not yet scan the EPSG database (this may change in future Apache SIS version).
     * The current list of known coordinate systems is documented {@linkplain #getEpsgCode(Class,
     * CoordinateSystemAxis...) above}.</p>
     *
     * @param  unit        desired unit of measurement. For three-dimensional ellipsoidal coordinate system,
     *                     this is the unit for the horizontal axes only; the vertical axis is in metres.
     * @param  directions  desired axis directions.
     * @return EPSG codes for a coordinate system using the given axis directions and unit of measurement,
     *         or {@code null} if unknown to this method. Note that a null value does not mean that a more
     *         extensive search in the EPSG database would not find a matching coordinate system.
     *
     * @see Units#getEpsgCode(Unit, boolean)
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createCoordinateSystem(String)
     *
     * @since 0.8
     */
    public static Integer getEpsgCode(final Unit<?> unit, final AxisDirection... directions) {
        ArgumentChecks.ensureNonNull("unit", unit);
        ArgumentChecks.ensureNonNull("directions", directions);
        final int code = Codes.lookup(unit, directions);
        return (code != 0) ? code : null;
    }
}
