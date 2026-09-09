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
package org.apache.sis.geometries;

import java.util.List;
import javax.measure.quantity.Length;
import org.apache.sis.geometries.curve.ArcByBulge;
import org.apache.sis.geometries.curve.ArcByCenterPoint;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.Conic;
import org.apache.sis.geometries.curve.FunctionCurve;
import org.apache.sis.geometries.curve.Geodesic;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.OffsetCurve;
import org.apache.sis.geometries.curve.ProductCurve;
import org.apache.sis.geometries.curve.Rhumb;
import org.apache.sis.geometries.curve.Spiral;
import org.apache.sis.geometries.internal.shared.DefaultReversedCurve;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;


/**
 * A 1-dimensional geometric primitive, continuous image of an open real interval,
 * usually stored as a sequence of points with the subtype specifying the interpolation between them.
 *
 * <p>A curve is connected and has a measurable length. Its orientation is given by its
 * parameterization, consistently with the tangent which always points in the forward direction.
 * A curve may be built from several curve segments, each segment starting where the previous
 * one ends, possibly with a different interpolation.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The topological dimension is 1.</li>
 *   <li>A curve is always bounded: it has a finite envelope and contains no point at infinity.</li>
 *   <li>Degenerate curves of zero length are not curves but points.</li>
 *   <li>Cutting a curve at any of its interior points gives two curves
 *       whose union is geometrically equal to the original curve.</li>
 * </ul>
 *
 * <p>Note: OGC Simple Feature Access defines only one subtype of curve, {@link LineString},
 * which uses a linear interpolation between points.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.18
 */
@UML(identifier="Curve", specification=ISO_19107)
public sealed interface Curve extends Orientable
        permits ArcByBulge,
                ArcByCenterPoint,
                CircularString,
                CompoundCurve,
                Conic,
                FunctionCurve,
                Geodesic,
                LineString,
                OffsetCurve,
                ProductCurve,
                Rhumb,
                Spiral,
                DefaultReversedCurve
{

    /**
     * Points lying on this curve, the first one being the start point and the last one the end point.
     *
     * <p>Difference with ISO 19107: the type has been changed from a list of direct positions
     * to {@link DataPoints}. This change allows to accommodate additional attributes like in
     * GLTF or GPU models.</p>
     *
     * @return curve data points.
     *
     * @see ISO 19107:2019 - 6.4.18.3
     */
    @UML(identifier="dataPoint", specification=ISO_19107)
    DataPoints getDataPoints();

    /**
     * Length of this curve in its associated spatial reference system.
     * The arc length parameterization of this curve therefore spans the [0 … length] interval.
     *
     * <p>Difference with OGC Simple Feature Access, which returns a {@code double}:
     * the length is returned as a {@link Length} quantity in order to carry its unit of measurement.</p>
     *
     * @return length of the curve.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @see ISO 19107:2019 - 6.4.18.9
     */
    @UML(identifier="length", specification=ISO_19107)
    default Length getLength() {
        throw new UnsupportedOperationException();
    }

    /**
     * Position where this curve begins.
     *
     * @return start point of this curve, or {@code null} if this curve is empty.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @see ISO 19107:2019 - 6.4.18.6
     */
    @UML(identifier="startPoint", specification=ISO_19107)
    default Point getStartPoint() {
        final DataPoints points = getDataPoints();
        if (points.isEmpty()) return null;
        return points.getPoint(0);
    }

    /**
     * Position where this curve ends.
     *
     * @return end point of this curve, or {@code null} if this curve is empty.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @see ISO 19107:2019 - 6.4.18.7
     */
    @UML(identifier="endPoint", specification=ISO_19107)
    default Point getEndPoint() {
        final DataPoints points = getDataPoints();
        if (points.isEmpty()) return null;
        return points.getPoint(points.size()-1);
    }

    /**
     * Returns whether this curve is closed, i.e. whether its start point equals its end point.
     * For a simple curve, this is equivalent to {@link Geometry#isCycle()}.
     *
     * @return {@code true} if this curve is closed.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @see ISO 19107:2019 - 6.4.4.14
     */
    default boolean isClosed() {
        final DataPoints points = getDataPoints();
        final int size = points.size();
        if (size == 0) {
            return false;
        }
        return points.getPosition(0).equals(points.getPosition(size - 1), 0);
    }

    /**
     * Returns whether this curve is both closed and simple,
     * in which case it is a valid boundary component of a surface.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@code isRing()} is {@code true} if and only if both {@link Geometry#isCycle()}
     *       and {@link Geometry#isSimple()} are {@code true}.</li>
     * </ul>
     *
     * @return {@code true} if this curve is a ring.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @see ISO 19107:2019 - 6.4.18.8
     */
    @UML(identifier="isRing", specification=ISO_19107)
    default boolean isRing() {
        throw new UnsupportedOperationException();
    }

    /**
     * Positions used to build the geometry of this curve, the way they are used depending on the interpolation.
     *
     * <p>Control points do not necessarily lie on the curve. They use the same reference system as
     * this curve, but may carry additional parameter columns used to compute the data points.
     * For example the homogeneous weight of a rational spline, or the bearing and the distance
     * from the center of a circular arc to its data points.</p>
     *
     * @return curve control points, possibly empty.
     *
     * @see ISO 19107:2019 - 6.4.18.2
     */
    @UML(identifier="controlPoint", specification=ISO_19107)
    Array getControlPoints();

    /**
     * Construction parameter values matching the {@linkplain #getDataPoints() data points},
     * so that {@code constrParam(knot[i])} is the data point at index <var>i</var>.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Values are monotonic and there are as many knots as data points.</li>
     *   <li>The first and last values are the {@linkplain #getStartConstrParam() start}
     *       and {@linkplain #getEndConstrParam() end} construction parameters.</li>
     *   <li>In a composite curve, the last knot of a segment is the first knot of the next segment.</li>
     * </ul>
     *
     * @return knot values in the construction (knot) space.
     *
     * @see ISO 19107:2019 - 6.4.18.4
     */
    @UML(identifier="knot", specification=ISO_19107)
    default double[] getKnots() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Interpolation mechanism used between the data points of this curve.
     *
     * <p>The interpolation combines the {@linkplain #getDataPoints() data points},
     * {@linkplain #getControlPoints() control points} and {@linkplain #getKnots() knots}
     * to determine the positions of this curve. Segments of a composite curve may use
     * different interpolations. The default is a linear interpolation in the coordinate
     * system of this curve.</p>
     *
     * @return interpolation used by this curve.
     *
     * @see ISO 19107:2019 - 6.4.18.5
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    default CurveInterpolation getInterpolation() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Construction parameter of the {@linkplain #getStartPoint() start point}, in knot space.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@link #constrParam(double)} applied to this value gives the start point.</li>
     *   <li>The construction parameterization is strictly monotonic, but this value is not
     *       necessarily smaller than the {@linkplain #getEndConstrParam() end parameter}.</li>
     * </ul>
     *
     * @return construction parameter of the start point.
     *
     * @see ISO 19107:2019 - 6.4.18.15
     */
    @UML(identifier="startConstrParam", specification=ISO_19107)
    default double getStartConstrParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Construction parameter of the {@linkplain #getEndPoint() end point}, in knot space.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@link #constrParam(double)} applied to this value gives the end point.</li>
     *   <li>The construction parameterization is strictly monotonic, but this value is not
     *       necessarily greater than the {@linkplain #getStartConstrParam() start parameter}.</li>
     * </ul>
     *
     * @return construction parameter of the end point.
     *
     * @see ISO 19107:2019 - 6.4.18.15
     */
    @UML(identifier="endConstrParam", specification=ISO_19107)
    default double getEndConstrParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Arc length parameter of the {@linkplain #getStartPoint() start point}.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@link #param(Length)} applied to this value gives the start point.</li>
     *   <li>For a curve which is not a segment of another curve, this value is usually zero.</li>
     *   <li>For a segment other than the first one, this value is the
     *       {@linkplain #getEndParam() end parameter} of the preceding segment.</li>
     * </ul>
     *
     * @return arc length parameter of the start point.
     *
     * @see ISO 19107:2019 - 6.4.18.13
     */
    @UML(identifier="startParam", specification=ISO_19107)
    default Length getStartParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Arc length parameter of the {@linkplain #getEndPoint() end point}.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@link #param(Length)} applied to this value gives the end point.</li>
     *   <li>The difference with the {@linkplain #getStartParam() start parameter} is the
     *       {@linkplain #getLength() length} of this curve.</li>
     * </ul>
     *
     * @return arc length parameter of the end point.
     *
     * @see ISO 19107:2019 - 6.4.18.13
     */
    @UML(identifier="endParam", specification=ISO_19107)
    default Length getEndParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Number of continuous derivatives guaranteed between the first and the last knot.
     * The default value 0 means simple continuity (C⁰), while a value <var>n</var> means
     * that this curve and its <var>n</var> first derivatives are continuous (Cⁿ).
     *
     * @return number of continuous derivatives in the interior of this curve, or {@code null} if unspecified.
     *
     * @see ISO 19107:2019 - 6.4.18.10
     */
    @UML(identifier="numDerivativesInterior", specification=ISO_19107)
    default Integer getNumDerivativesInterior() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Number of continuous derivatives guaranteed at the {@linkplain #getStartPoint() start point},
     * therefore the continuity between this curve and its predecessor in a composite curve.
     * The default value 0 means simple continuity (C⁰).
     *
     * @return number of continuous derivatives at the start point, or {@code null} if unspecified.
     *
     * @see ISO 19107:2019 - 6.4.18.11
     */
    @UML(identifier="numDerivativesStart", specification=ISO_19107)
    default Integer getNumDerivativesStart() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Number of continuous derivatives guaranteed at the {@linkplain #getEndPoint() end point},
     * therefore the continuity between this curve and its successor in a composite curve.
     * The default value 0 means simple continuity (C⁰).
     *
     * @return number of continuous derivatives at the end point, or {@code null} if unspecified.
     *
     * @see ISO 19107:2019 - 6.4.18.12
     */
    @UML(identifier="numDerivativesEnd", specification=ISO_19107)
    default Integer getNumDerivativesEnd() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a curve geometrically equal to this curve but with the opposite orientation.
     * Reversing a curve reverses the parameter space of its segments and the order of those segments,
     * so the start and end points are exchanged.
     *
     * @return this curve with reversed orientation.
     *
     * @see ISO 19107:2019 - 6.4.18.14
     */
    @UML(identifier="reverse", specification=ISO_19107)
    @Override
    default Curve getReverse() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Approximates this curve by a polyline whose points lie on this curve.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>All the points of the returned line lie on this curve, within the accuracy of the
     *       numerical representation of the positions.</li>
     *   <li>If {@code spacing} is non-zero, the distance along this curve between two consecutive
     *       points of the returned line is at most {@code spacing}.</li>
     *   <li>If {@code offset} is non-zero, the distance between this curve and the returned line
     *       is at most {@code offset}.</li>
     *   <li>If both arguments are non-zero, both criteria are met.</li>
     *   <li>If both arguments are zero, the implementation may apply its own precision limit, and
     *       the returned line contains at least all the {@linkplain #getDataPoints() data points}
     *       of this curve.</li>
     *   <li>{@linkplain #getControlPoints() control points} of this curve which lie on this curve
     *       are among the control points of the returned line.</li>
     * </ul>
     *
     * @param  spacing  maximal distance along this curve between two points of the line, or zero for no limit.
     * @param  offset   maximal distance between this curve and the line, or zero for no limit.
     * @return a linear approximation of this curve.
     *
     * @see ISO 19107:2019 - 6.4.18.17
     */
    @UML(identifier="asLine", specification=ISO_19107)
    default LineString asLine(Length spacing, Length offset) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the position on this curve at the given construction parameter.
     * The construction parameterization is chosen for computational convenience and usually has
     * no simple relationship with arc lengths. By default, the knot space is the sequence of
     * integers starting at 0.
     *
     * @param  cp  construction parameter, between the {@linkplain #getStartConstrParam() start}
     *             and the {@linkplain #getEndConstrParam() end} construction parameters.
     * @return position on this curve at the given construction parameter.
     *
     * @see ISO 19107:2019 - 6.4.18.18
     */
    @UML(identifier="constrParam", specification=ISO_19107)
    default DirectPosition constrParam(double cp) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the length of this curve between two positions on this curve.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>A position which is not on this curve is projected on the nearest position of this curve.</li>
     *   <li>If this curve is not simple and passes several times through one of those positions,
     *       the smallest length is returned.</li>
     * </ul>
     *
     * @param  point1  first position, by default the {@linkplain #getStartPoint() start point}.
     * @param  point2  second position, by default the {@linkplain #getEndPoint() end point}.
     * @return length of this curve between the two given positions.
     *
     * @see ISO 19107:2019 - 6.4.18.19
     */
    @UML(identifier="length", specification=ISO_19107)
    default Length getLength(DirectPosition point1, DirectPosition point2) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the length of this curve between two construction parameters.
     * This variant works directly in the construction parameter space, and therefore also
     * converts a construction parameter into an arc length parameter.
     *
     * @param  cparam1  first construction parameter, by default the {@linkplain #getStartConstrParam() start} one.
     * @param  cparam2  second construction parameter, by default the {@linkplain #getEndConstrParam() end} one.
     * @return length of this curve between the two given construction parameters.
     *
     * @see ISO 19107:2019 - 6.4.18.19
     */
    @UML(identifier="length", specification=ISO_19107)
    default Length getLength(double cparam1, double cparam2) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the position on this curve at the given arc length parameter,
     * i.e. at the given distance measured along this curve from the
     * {@linkplain #getStartParam() start parameter}.
     *
     * @param  s  arc length parameter, between the {@linkplain #getStartParam() start}
     *            and the {@linkplain #getEndParam() end} parameters.
     * @return position on this curve at the given arc length parameter.
     *
     * @see ISO 19107:2019 - 6.4.18.20
     */
    @UML(identifier="param", specification=ISO_19107)
    default DirectPosition param(Length s) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the arc length parameters at which this curve passes through the given position.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>A position which is not on this curve is replaced by the nearest position on this curve.</li>
     *   <li>For any returned value <var>d</var>, {@link #param(Length)} applied to <var>d</var>
     *       gives the given position.</li>
     *   <li>More than one value is returned only if this curve is not simple.</li>
     *   <li>If several positions of this curve are at the same minimal distance from the given
     *       position, the choice among them is arbitrary.</li>
     * </ul>
     *
     * @param  p  position for which to compute the arc length parameters.
     * @return arc length parameters of the given position, possibly empty.
     *
     * @see ISO 19107:2019 - 6.4.18.21
     */
    @UML(identifier="paramForPoint", specification=ISO_19107)
    default List<Length> paramForPoint(DirectPosition p) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the tangent vector at the given arc length parameter.
     * The vector coordinates are the differentials of the coordinates of the direct positions.
     * Being taken with respect to arc length, the returned vector is a unit vector.
     *
     * @param  s  arc length parameter, between the {@linkplain #getStartParam() start}
     *            and the {@linkplain #getEndParam() end} parameters.
     * @return unit tangent vector at the given arc length parameter.
     *
     * @see ISO 19107:2019 - 6.4.18.22
     */
    @UML(identifier="tangent", specification=ISO_19107)
    default Vector tangent(Length s) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the tangent vector at the given construction parameter.
     * The vector coordinates are the differentials of the coordinates of the direct positions.
     * Its direction is the direction of this curve, but its magnitude depends on the construction
     * parameterization; {@link #tangent(Length)} returns the collinear unit vector.
     *
     * @param  knotParameter  construction parameter, between the {@linkplain #getStartConstrParam() start}
     *                        and the {@linkplain #getEndConstrParam() end} construction parameters.
     * @return tangent vector at the given construction parameter.
     *
     * @see ISO 19107:2019 - 6.4.18.22
     */
    @UML(identifier="tangent", specification=ISO_19107)
    default Vector tangent(double knotParameter) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Extracts the portion of this curve between two construction parameters.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>If the first parameter is smaller than the second one, the returned curve has the same
     *       orientation as this curve, otherwise the orientation is reversed.</li>
     *   <li>Both parameters shall be between the {@linkplain #getStartConstrParam() start}
     *       and the {@linkplain #getEndConstrParam() end} construction parameters.</li>
     * </ul>
     *
     * <p>Difference with ISO 19107: declared as a copy constructor in the standard,
     * which an interface cannot express in Java.</p>
     *
     * @param  cparam1  construction parameter where the returned curve begins.
     * @param  cparam2  construction parameter where the returned curve ends.
     * @return the portion of this curve between the two given construction parameters.
     *
     * @see ISO 19107:2019 - 6.4.18.22
     */
    @UML(identifier="Curve", specification=ISO_19107)
    default Curve subCurve(double cparam1, double cparam2) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Extracts the portion of this curve between two arc length parameters.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>If the first parameter is smaller than the second one, the returned curve has the same
     *       orientation as this curve, otherwise the orientation is reversed.</li>
     *   <li>Both parameters shall be between the {@linkplain #getStartParam() start}
     *       and the {@linkplain #getEndParam() end} parameters.</li>
     * </ul>
     *
     * <p>Difference with ISO 19107: declared as a copy constructor in the standard,
     * which an interface cannot express in Java.</p>
     *
     * @param  dist1  arc length parameter where the returned curve begins.
     * @param  dist2  arc length parameter where the returned curve ends.
     * @return the portion of this curve between the two given arc length parameters.
     *
     * @see ISO 19107:2019 - 6.4.18.22
     */
    @UML(identifier="Curve", specification=ISO_19107)
    default Curve subCurve(Length dist1, Length dist2) {
        //TODO
        throw new UnsupportedOperationException();
    }
}
