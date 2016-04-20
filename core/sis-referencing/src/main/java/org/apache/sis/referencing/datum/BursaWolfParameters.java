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

import java.util.Date;
import java.util.Arrays;
import java.io.Serializable;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;

import static java.lang.Math.abs;
import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.referencing.operation.matrix.Matrix4.SIZE;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Parameters for a geographic transformation between two datum having the same prime meridian.
 * Bursa-Wolf parameters are also known as <cite>Helmert transformation parameters</cite>.
 * For an explanation of their purpose, see the <cite>Bursa-Wolf parameters</cite> section
 * of {@link DefaultGeodeticDatum} class javadoc.
 *
 * <p>The Bursa-Wolf parameters shall be applied to geocentric coordinates,
 * where the <var>X</var> axis points towards the Prime Meridian (usually Greenwich),
 * the <var>Y</var> axis points East, and the <var>Z</var> axis points North.</p>
 *
 * <div class="note"><b>Note:</b>
 * The upper case letters are intentional. By convention, (<var>X</var>, <var>Y</var>, <var>Z</var>)
 * stand for <cite>geocentric</cite> coordinates while (<var>x</var>, <var>y</var>, <var>z</var>)
 * stand for <cite>projected</cite> coordinates.</div>
 *
 * The "Bursa-Wolf" formula is expressed with 7 parameters, listed in the table below.
 * The <cite>code</cite>, <cite>name</cite> and <cite>abbreviation</cite> columns list EPSG identifiers,
 * while the <cite>legacy</cite> column lists the identifiers used in the legacy OGC 01-009 specification
 * (still used in some <cite>Well Known Texts</cite>).
 *
 * <table summary="Parameters and formula"><tr><td>
 * <table class="sis">
 *   <caption>Parameters defined by EPSG</caption>
 *   <tr><th>Code</th> <th>Name</th>               <th>Abbr.</th>       <th>Legacy</th></tr>
 *   <tr><td>8605</td> <td>X-axis translation</td> <td>{@link #tX}</td> <td>{@code dx}</td></tr>
 *   <tr><td>8606</td> <td>Y-axis translation</td> <td>{@link #tY}</td> <td>{@code dy}</td></tr>
 *   <tr><td>8607</td> <td>Z-axis translation</td> <td>{@link #tZ}</td> <td>{@code dz}</td></tr>
 *   <tr><td>8608</td> <td>X-axis rotation</td>    <td>{@link #rX}</td> <td>{@code ex}</td></tr>
 *   <tr><td>8609</td> <td>Y-axis rotation</td>    <td>{@link #rY}</td> <td>{@code ey}</td></tr>
 *   <tr><td>8610</td> <td>Z-axis rotation</td>    <td>{@link #rZ}</td> <td>{@code ez}</td></tr>
 *   <tr><td>8611</td> <td>Scale difference</td>   <td>{@link #dS}</td> <td>{@code ppm}</td></tr>
 * </table>
 *
 * </td><td style="padding-left: 40pt; white-space: nowrap">
 * <center><b>Geocentric coordinates transformation</b></center>
 * <center>from (<var>X</var><sub>s</sub>, <var>Y</var><sub>s</sub>, <var>Z</var><sub>s</sub>)
 *           to (<var>X</var><sub>t</sub>, <var>Y</var><sub>t</sub>, <var>Z</var><sub>t</sub>)</center>
 * <center style="font-size: small">(ignoring unit conversions)</center>
 *
 * <p>{@include formulas.html#Bursa-Wolf}</p>
 * </td></tr></table>
 *
 * The numerical fields in this {@code BursaWolfParameters} class use the EPSG abbreviations
 * with 4 additional constraints compared to the EPSG definitions:
 *
 * <ul>
 *   <li>Unit of scale difference ({@link #dS}) is fixed to <em>parts per million</em>.</li>
 *   <li>Unit of translation terms ({@link #tX}, {@link #tY}, {@link #tZ}) is fixed to <em>metres</em>.</li>
 *   <li>Unit of rotation terms ({@link #rX}, {@link #rY}, {@link #rZ}) is fixed to <em>arc-seconds</em>.</li>
 *   <li>Sign of rotation terms is fixed to the <em>Position Vector</em> convention (EPSG operation method 9606).
 *       This is the opposite sign than the <cite>Coordinate Frame Rotation</cite> (EPSG operation method 9607).
 *       The Position Vector convention is used by IAG and recommended by ISO 19111.</li>
 * </ul>
 *
 * <div class="section">Source and target geodetic datum</div>
 * The <var>source datum</var> in above coordinates transformation is the {@link DefaultGeodeticDatum} instance
 * that contain this {@code BursaWolfParameters}. It can be any datum, including datum that are valid only locally.
 * The <var>{@linkplain #getTargetDatum() target datum}</var> is specified at construction time and is often,
 * but not necessarily, the <cite>World Geodetic System 1984</cite> (WGS 84) datum.
 *
 * <p>If the source and target datum does not have the same {@linkplain DefaultGeodeticDatum#getPrimeMeridian()
 * prime meridian}, then it is user's responsibility to apply longitude rotation before to use the Bursa-Wolf
 * parameters.</p>
 *
 * <div class="section">When Bursa-Wolf parameters are used</div>
 * {@code BursaWolfParameters} are used in three contexts:
 * <ol>
 *   <li>Created as a step while creating a {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperation
 *       coordinate operation} from the EPSG database.</li>
 *   <li>Associated to a {@link DefaultGeodeticDatum} with the WGS 84 {@linkplain #getTargetDatum() target datum} for
 *       providing the parameter values to display in the {@code TOWGS84[…]} element of <cite>Well Known Text</cite>
 *       (WKT) version 1. Note that WKT version 2 does not have {@code TOWGS84[…]} element anymore.</li>
 *   <li>Specified at {@code DefaultGeodeticDatum} construction time for arbitrary target datum.
 *       Apache SIS will ignore those Bursa-Wolf parameters, except as a fallback if no parameters
 *       can been found in the EPSG database for a given pair of source and target CRS.</li>
 * </ol>
 *
 * <div class="note"><b>Note:</b>
 * In EPSG terminology, Apache SIS gives precedence to the <cite>late-binding</cite> approach
 * (case 1 above) over the <cite>early-binding</cite> approach (case 3 above).</div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see DefaultGeodeticDatum#getBursaWolfParameters()
 * @see <a href="http://en.wikipedia.org/wiki/Helmert_transformation">Wikipedia: Helmert transformation</a>
 */
public class BursaWolfParameters extends FormattableObject implements Cloneable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 754825592343010900L;

    /**
     * The conversion factor from <cite>parts per million</cite> to scale minus one.
     */
    static final double PPM = 1E+6;

    /**
     * X-axis translation in metres (EPSG:8605).
     * The legacy OGC parameter name is {@code "dx"}.
     */
    public double tX;

    /**
     * Y-axis translation in metres (EPSG:8606).
     * The legacy OGC parameter name is {@code "dy"}.
     */
    public double tY;

    /**
     * Z-axis translation in metres (EPSG:8607).
     * The legacy OGC parameter name is {@code "dz"}.
     */
    public double tZ;

    /**
     * X-axis rotation in arc-seconds (EPSG:8608), sign following the <cite>Position Vector</cite> convention.
     * The legacy OGC parameter name is {@code "ex"}.
     */
    public double rX;

    /**
     * Y-axis rotation in arc-seconds (EPSG:8609), sign following the <cite>Position Vector</cite> convention.
     * The legacy OGC parameter name is {@code "ey"}.
     */
    public double rY;

    /**
     * Z-axis rotation in arc-seconds (EPSG:8610), sign following the <cite>Position Vector</cite> convention.
     * The legacy OGC parameter name is {@code "ez"}.
     */
    public double rZ;

    /**
     * The scale difference in parts per million (EPSG:8611).
     * The legacy OGC parameter name is {@code "ppm"}.
     *
     * <div class="note"><b>Example:</b>
     * If a distance of 100 km in the source coordinate reference system translates into a distance of 100.001 km
     * in the target coordinate reference system, the scale difference is 1 ppm (the ratio being 1.000001).</div>
     */
    public double dS;

    /**
     * The target datum for this set of parameters, or {@code null} if unknown.
     * This is usually the WGS 84 datum, but other targets are allowed.
     *
     * <p>The source datum is the {@link DefaultGeodeticDatum} that contain this {@code BursaWolfParameters}
     * instance.</p>
     *
     * @see #getTargetDatum()
     */
    private final GeodeticDatum targetDatum;

    /**
     * Region or timeframe in which a coordinate transformation based on those Bursa-Wolf parameters is valid,
     * or {@code null} if unspecified.
     *
     * @see #getDomainOfValidity()
     */
    private final Extent domainOfValidity;

    /**
     * Creates a new instance for the given target datum and domain of validity.
     * All numerical parameters are initialized to 0, which correspond to an identity transform.
     * Callers can assign numerical values to the public fields of interest after construction.
     * For example, many coordinate transformations will provide values only for the translation
     * terms ({@link #tX}, {@link #tY}, {@link #tZ}).
     *
     * <p>Alternatively, numerical fields can also be initialized by a call to
     * {@link #setPositionVectorTransformation(Matrix, double)}.</p>
     *
     * @param targetDatum The target datum (usually WGS 84) for this set of parameters, or {@code null} if unknown.
     * @param domainOfValidity Area or region in which a coordinate transformation based on those Bursa-Wolf parameters
     *        is valid, or {@code null} is unspecified.
     */
    public BursaWolfParameters(final GeodeticDatum targetDatum, final Extent domainOfValidity) {
        this.targetDatum = targetDatum;
        this.domainOfValidity = domainOfValidity;
    }

    /**
     * Verifies parameters validity after initialization of {@link DefaultGeodeticDatum}.
     * This method requires that the prime meridian of the target datum is either the same
     * than the enclosing {@code GeodeticDatum}, or Greenwich. We put this restriction for
     * avoiding ambiguity about whether the longitude rotation should be applied before or
     * after the datum shift.
     *
     * <p>If the target prime meridian is Greenwich, then SIS will assume that the datum shift
     * needs to be applied in a coordinate system having Greenwich as the prime meridian.</p>
     *
     * <p><b>Maintenance note:</b>
     * if the above policy regarding prime meridians is modified, then some {@code createOperationStep(…)} method
     * implementations in {@link org.apache.sis.referencing.operation.CoordinateOperationFinder} may need to be
     * revisited. See especially the methods creating a transformation between a pair of {@code GeocentricCRS} or
     * between a pair of {@code GeographicCRS} (tip: search for {@code DefaultGeodeticDatum}).</p>
     *
     * @param pm The prime meridian of the enclosing {@code GeodeticDatum}.
     */
    void verify(final PrimeMeridian pm) throws IllegalArgumentException {
        if (targetDatum != null) {
            final PrimeMeridian actual = targetDatum.getPrimeMeridian();
            if (actual.getGreenwichLongitude() != 0 && !Utilities.equalsIgnoreMetadata(pm, actual)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedPrimeMeridian_2,
                        IdentifiedObjects.getName(pm, null), IdentifiedObjects.getName(actual, null)));
            }
        }
        ensureFinite("tX", tX);
        ensureFinite("tY", tY);
        ensureFinite("tZ", tZ);
        ensureFinite("rX", rX);
        ensureFinite("rY", rY);
        ensureFinite("rZ", rZ);
        ensureBetween("dS", -PPM, PPM, dS);     // For preventing zero or negative value on the matrix diagonal.
    }

    /**
     * Returns the target datum for this set of parameters, or {@code null} if unknown.
     * This is usually the WGS 84 datum, but other targets are allowed.
     *
     * <p>The source datum is the {@link DefaultGeodeticDatum} that contain this {@code BursaWolfParameters}
     * instance.</p>
     *
     * @return The target datum for this set of parameters, or {@code null} if unknown.
     */
    public GeodeticDatum getTargetDatum() {
        return targetDatum;
    }

    /**
     * Returns the parameter values. The length of the returned array depends on the values:
     *
     * <ul>
     *   <li>If this instance is an {@link TimeDependentBWP}, then the array length will be 14.</li>
     *   <li>Otherwise if this instance contains a non-zero {@link #dS} value, then the array length will be 7 with
     *       {@link #tX}, {@link #tY}, {@link #tZ}, {@link #rX}, {@link #rY}, {@link #rZ} and {@link #dS} values
     *       in that order.</li>
     *   <li>Otherwise if this instance contains non-zero rotation terms,
     *       then this method returns the first 6 of the above-cited values.</li>
     *   <li>Otherwise (i.e. this instance {@linkplain #isTranslation() is a translation}),
     *       this method returns only the first 3 of the above-cited values.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * the rules about the arrays of length 3, 6 or 7 are derived from the <cite>Well Known Text</cite> (WKT)
     * version 1 specification. The rule about the array of length 14 is an extension.</div>
     *
     * @return The parameter values as an array of length 3, 6, 7 or 14.
     *
     * @since 0.6
     */
    @SuppressWarnings("fallthrough")
    public double[] getValues() {
        final double[] elements = new double[(dS != 0) ? 7 : (rZ != 0 || rY != 0 || rX != 0) ? 6 : 3];
        switch (elements.length) {
            default: elements[6] = dS;      // Fallthrough everywhere.
            case 6:  elements[5] = rZ;
                     elements[4] = rY;
                     elements[3] = rX;
            case 3:  elements[2] = tZ;
                     elements[1] = tY;
                     elements[0] = tX;
        }
        return elements;
    }

    /**
     * Sets the parameters to the given values. The given array can have any length. The first array elements will be
     * assigned to the {@link #tX}, {@link #tY}, {@link #tZ}, {@link #rX}, {@link #rY}, {@link #rZ} and {@link #dS}
     * fields in that order.
     *
     * <ul>
     *   <li>If the length of the given array is not sufficient for assigning a value to every fields,
     *       then the remaining fields are left unchanged (they are <strong>not</strong> reset to zero,
     *       but this is not a problem if this {@code BursaWolfParameters} is a new instance).</li>
     *   <li>If the length of the given array is greater than necessary, then extra elements are ignored by this base
     *       class. Note however that those extra elements may be used by subclasses like {@link TimeDependentBWP}.</li>
     * </ul>
     *
     * @param elements The new parameter values, as an array of any length.
     *
     * @since 0.6
     */
    @SuppressWarnings("fallthrough")
    public void setValues(final double... elements) {
        switch (elements.length) {
            default: dS = elements[6];      // Fallthrough everywhere.
            case 6:  rZ = elements[5];
            case 5:  rY = elements[4];
            case 4:  rX = elements[3];
            case 3:  tZ = elements[2];
            case 2:  tY = elements[1];
            case 1:  tX = elements[0];
            case 0:  break;
         }
    }

    /**
     * Returns {@code true} if the {@linkplain #targetDatum target datum} is equals (at least on computation purpose)
     * to the WGS84 datum. If the datum is unspecified, then this method returns {@code true} since WGS84 is the only
     * datum supported by the WKT 1 format, and is what users often mean.
     *
     * @return {@code true} if the given datum is equal to WGS84 for computational purpose.
     */
    final boolean isToWGS84() {
        return (targetDatum != null) && IdentifiedObjects.isHeuristicMatchForName(targetDatum, "WGS84");
    }

    /**
     * Returns {@code true} if a transformation built from this set of parameters would perform no operation.
     * This is true when the value of all parameters is zero.
     *
     * @return {@code true} if the parameters describe no operation.
     */
    public boolean isIdentity() {
        return tX == 0 && tY == 0 && tZ == 0 && isTranslation();
    }

    /**
     * Returns {@code true} if a transformation built from this set of parameters would perform only a translation.
     *
     * @return {@code true} if the parameters describe a translation only.
     */
    public boolean isTranslation() {
        return rX == 0 && rY == 0 && rZ == 0 && dS == 0;
    }

    /**
     * Inverts in-place the sign of rotation terms ({@link #rX}, {@link #rY}, {@link #rZ}).
     * This method can be invoked for converting a <cite>Coordinate Frame Rotation</cite> transformation
     * (EPSG operation method 9607) to a <em>Position Vector</em> transformation (EPSG operation method 9606).
     * The later convention is used by IAG and recommended by ISO 19111.
     */
    public void reverseRotation() {
        rX = -rX;
        rY = -rY;
        rZ = -rZ;
    }

    /**
     * Inverts in-place the transformation by inverting the sign of all numerical parameters.
     * The {@linkplain #getPositionVectorTransformation(Date) position vector transformation} matrix
     * created from inverted Bursa-Wolf parameters will be <strong>approximatively</strong> equals
     * to the {@linkplain org.apache.sis.referencing.operation.matrix.MatrixSIS#inverse() inverse}
     * of the matrix created from the original parameters. The equality holds approximatively only
     * because the parameter values are very small (parts per millions and arc-seconds).
     */
    public void invert() {
        final double[] values = getValues();
        for (int i=0; i<values.length; i++) {
            values[i] = -values[i];
        }
        setValues(values);
    }

    /**
     * Returns the elapsed time from the {@linkplain TimeDependentBWP#getTimeReference() reference time}
     * to the given date, in millennium. If this {@code BursaWolfParameters} is not time-dependent, then
     * returns {@code null}.
     */
    DoubleDouble period(final Date time) {
        return null;
    }

    /**
     * Returns the parameter at the given index. If this {@code BursaWolfParameters} is time-dependent,
     * then the returned value shall be corrected for the given period.
     *
     * @param index  0 for {@code tX}, 1 for {@code tY}, <i>etc.</i> in {@code TOWGS84[…]} order.
     * @param period The value computed by {@link #period(Date)}, or {@code null}.
     */
    DoubleDouble param(final int index, final DoubleDouble period) {
        final double p;
        switch (index) {
            case 0: p = tX; break;
            case 1: p = tY; break;
            case 2: p = tZ; break;
            case 3: p = rX; break;
            case 4: p = rY; break;
            case 5: p = rZ; break;
            case 6: p = dS; break;
            default: throw new AssertionError(index);
        }
        return new DoubleDouble(p);
    }

    /**
     * Returns the position vector transformation (geocentric domain) as an affine transform.
     * For transformations that do not depend on time, the formula is as below where {@code R}
     * is a conversion factor from arc-seconds to radians:
     *
     * <blockquote><pre> R = toRadians(1″)
     * S = 1 + {@linkplain #dS}/1000000
     * ┌    ┐    ┌                               ┐  ┌   ┐
     * │ X' │    │      S   -{@linkplain #rZ}*RS   +{@linkplain #rY}*RS   {@linkplain #tX} │  │ X │
     * │ Y' │  = │ +{@linkplain #rZ}*RS        S   -{@linkplain #rX}*RS   {@linkplain #tY} │  │ Y │
     * │ Z' │    │ -{@linkplain #rY}*RS   +{@linkplain #rX}*RS        S   {@linkplain #tZ} │  │ Z │
     * │ 1  │    │      0        0        0    1 │  │ 1 │
     * └    ┘    └                               ┘  └   ┘</pre></blockquote>
     *
     * This affine transform can be applied on <strong>geocentric</strong> coordinates.
     * This is identified as operation method 1033 in the EPSG database.
     * Those geocentric coordinates are typically converted from geographic coordinates
     * in the region or timeframe given by {@link #getDomainOfValidity()}.
     *
     * <p>If the source datum and the {@linkplain #getTargetDatum() target datum} do not use the same
     * {@linkplain DefaultGeodeticDatum#getPrimeMeridian() prime meridian}, then it is caller's responsibility
     * to apply longitude rotation before to use the matrix returned by this method.</p>
     *
     * <div class="section">Time-dependent transformation</div>
     * Some transformations use parameters that vary with time (e.g. operation method EPSG:1053).
     * Users can optionally specify a date for which the transformation is desired.
     * For transformations that do not depends on time, this date is ignored and can be null.
     * For time-dependent transformations, {@code null} values default to the transformation's
     * {@linkplain TimeDependentBWP#getTimeReference() reference time}.
     *
     * <div class="section">Inverse transformation</div>
     * The inverse transformation can be approximated by reversing the sign of the 7 parameters before to use them
     * in the above matrix. This is often considered sufficient since <cite>position vector transformations</cite>
     * are themselves approximations. However Apache SIS will rather use
     * {@link org.apache.sis.referencing.operation.matrix.MatrixSIS#inverse()} in order to increase the chances
     * that concatenation of transformations <var>A</var> → <var>B</var> followed by <var>B</var> → <var>A</var>
     * gives back the identity transform.
     *
     * @param  time Date for which the transformation is desired, or {@code null} for the transformation's reference time.
     * @return An affine transform in geocentric space created from this Bursa-Wolf parameters and the given time.
     *
     * @see DefaultGeodeticDatum#getPositionVectorTransformation(GeodeticDatum, Extent)
     */
    public Matrix getPositionVectorTransformation(final Date time) {
        final DoubleDouble period = period(time);
        if (period == null && isTranslation()) {
            final Matrix4 matrix = new Matrix4();
            matrix.m03 = tX;
            matrix.m13 = tY;
            matrix.m23 = tZ;
            return matrix;
        }
        /*
         * Above was an optimization for the common case where the Bursa-Wolf parameters contain only
         * translation terms. If we have rotation or scale terms, then use double-double arithmetic.
         */
        final DoubleDouble RS = DoubleDouble.createSecondsToRadians();
        final DoubleDouble S = param(6, period);
        S.divide(PPM, 0);
        S.add(1, 0);        // S = 1 + dS / PPM;
        RS.multiply(S);     // RS = toRadians(1″) * S;
        final DoubleDouble  X = param(3, period); X.multiply(RS);
        final DoubleDouble  Y = param(4, period); Y.multiply(RS);
        final DoubleDouble  Z = param(5, period); Z.multiply(RS);
        final DoubleDouble mX = new DoubleDouble(X); mX.negate();
        final DoubleDouble mY = new DoubleDouble(Y); mY.negate();
        final DoubleDouble mZ = new DoubleDouble(Z); mZ.negate();
        final Integer       O = 0; // Fetch Integer instance only once.
        return Matrices.create(4, 4, new Number[] {
                 S,  mZ,   Y,  param(0, period),
                 Z,   S,  mX,  param(1, period),
                mY,   X,   S,  param(2, period),
                 O,   O,   O,  1});
    }

    /**
     * Sets all Bursa-Wolf parameters from the given <cite>Position Vector transformation</cite> matrix.
     * The matrix shall comply to the following constraints:
     *
     * <ul>
     *   <li>The matrix shall be {@linkplain org.apache.sis.referencing.operation.matrix.MatrixSIS#isAffine() affine}.</li>
     *   <li>The sub-matrix defined by {@code matrix} without the last row and last column shall be
     *       <a href="http://en.wikipedia.org/wiki/Skew-symmetric_matrix">skew-symmetric</a> (a.k.a. antisymmetric).</li>
     * </ul>
     *
     * @param  matrix The matrix from which to get Bursa-Wolf parameters.
     * @param  tolerance The tolerance error for the skew-symmetric matrix test, in units of PPM or arc-seconds (e.g. 1E-8).
     * @throws IllegalArgumentException if the specified matrix does not meet the conditions.
     *
     * @see #getPositionVectorTransformation(Date)
     */
    public void setPositionVectorTransformation(final Matrix matrix, final double tolerance) throws IllegalArgumentException {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != SIZE || numCol != SIZE) {
            final Integer n = SIZE;
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedMatrixSize_4, n, n, numRow, numCol));
        }
        if (!Matrices.isAffine(matrix)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotAnAffineTransform));
        }
        /*
         * Translation terms, taken "as-is".
         * If the matrix contains only translation terms (which is often the case), we are done.
         */
        tX = matrix.getElement(0,3);
        tY = matrix.getElement(1,3);
        tZ = matrix.getElement(2,3);
        if (Matrices.isTranslation(matrix)) {   // Optimization for a common case.
            return;
        }
        /*
         * Scale factor: take the average of elements on the diagonal. All those
         * elements should have the same value, but we tolerate slight deviation
         * (this will be verified later).
         */
        final DoubleDouble S = new DoubleDouble(getNumber(matrix, 0,0));
        S.add(getNumber(matrix, 1,1));
        S.add(getNumber(matrix, 2,2));
        S.divide(3, 0);
        /*
         * Computes: RS = S * toRadians(1″)
         *           dS = (S-1) * PPM
         */
        final DoubleDouble RS = DoubleDouble.createSecondsToRadians();
        RS.multiply(S);
        S.add(-1, 0);
        S.multiply(PPM, 0);
        dS = S.value;
        /*
         * Rotation terms. Each rotation terms appear twice, with one value being the negative of the other value.
         * We verify this skew symmetric aspect in the loop. We also opportunistically verify that the scale terms
         * are uniform.
         */
        for (int j=0; j < SIZE-1; j++) {
            if (!(abs((matrix.getElement(j,j) - 1)*PPM - dS) <= tolerance)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NonUniformScale));
            }
            for (int i = j+1; i < SIZE-1; i++) {
                S.setFrom(RS);
                S.inverseDivide(getNumber(matrix, j,i));        // Negative rotation term.
                double value = S.value;
                double error = S.error;
                S.setFrom(RS);
                S.inverseDivide(getNumber(matrix, i,j));        // Positive rotation term.
                if (!(abs(value + S.value) <= tolerance)) {     // We expect r1 ≈ -r2
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.NotASkewSymmetricMatrix));
                }
                S.subtract(value, error);
                S.multiply(0.5, 0);
                value = S.value;                                // Average of the two rotation terms.
                switch (j*SIZE + i) {
                    case 1: rZ =  value; break;
                    case 2: rY = -value; break;
                    case 6: rX =  value; break;
                }
            }
        }
    }

    /**
     * Retrieves the value at the specified row and column of the given matrix, wrapped in a {@code Number}.
     * The {@code Number} type depends on the matrix accuracy.
     *
     * @param matrix The matrix from which to get the number.
     * @param row    The row index, from 0 inclusive to {@link Matrix#getNumRow()} exclusive.
     * @param column The column index, from 0 inclusive to {@link Matrix#getNumCol()} exclusive.
     * @return       The current value at the given row and column.
     */
    private static Number getNumber(final Matrix matrix, final int row, final int column) {
        if (matrix instanceof MatrixSIS) {
            return ((MatrixSIS) matrix).getNumber(row, column);
        } else {
            return matrix.getElement(row, column);
        }
    }

    /**
     * Returns the region or timeframe in which a coordinate transformation based on those Bursa-Wolf parameters is
     * valid, or {@code null} if unspecified. If an extent was specified at construction time, then that extent is
     * returned. Otherwise the datum domain of validity (which may be {@code null}) is returned.
     *
     * @return Area or region or timeframe in which the coordinate transformation is valid, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    public Extent getDomainOfValidity() {
        if (domainOfValidity == null && targetDatum != null) {
            return targetDatum.getDomainOfValidity();
        }
        return domainOfValidity;
    }

    /**
     * Returns a copy of this object.
     *
     * @return A copy of all parameters.
     */
    @Override
    public BursaWolfParameters clone() {
        try {
            return (BursaWolfParameters) super.clone();
        }  catch (CloneNotSupportedException exception) {
            // Should not happen, since we are cloneable.
            throw new AssertionError(exception);
        }
    }

    /**
     * Compares the specified object with this object for equality.
     *
     * @param object The object to compare with the parameters.
     * @return {@code true} if the given object is equal to this {@code BursaWolfParameters}.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final BursaWolfParameters that = (BursaWolfParameters) object;
            return Arrays.equals(this.getValues(),      that.getValues()) &&
                  Objects.equals(this.targetDatum,      that.targetDatum) &&
                  Objects.equals(this.domainOfValidity, that.domainOfValidity);
        }
        return false;
    }

    /**
     * Returns a hash value for this object.
     *
     * @return The hash code value. This value does not need to be the same in past or future versions of this class.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(getValues()) ^ (int) serialVersionUID;
    }

    /**
     * Formats this object as a <cite>Well Known Text</cite> {@code ToWGS84[…]} element.
     * The WKT contains the parameters in <var>translation</var>, <var>rotation</var>, <var>scale</var> order,
     * like below:
     *
     * <blockquote><code>TOWGS84[{@linkplain #tX}, {@linkplain #tY}, {@linkplain #tZ}, {@linkplain #rX},
     * {@linkplain #rY}, {@linkplain #rZ}, {@linkplain #dS}]</code></blockquote>
     *
     * <div class="note"><b>Compatibility note:</b>
     * {@code TOWGS84} is defined in the WKT 1 specification only.</div>
     *
     * The element name is {@code "ToWGS84"} in the common case where the {@linkplain #getTargetDatum() target datum}
     * is WGS 84. For other targets, the element name will be derived from the datum name.
     *
     * @param  formatter The formatter where to format the inner content of this WKT element.
     * @return Usually {@code "ToWGS84"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final double[] values = getValues();
        for (final double value : values) {
            formatter.append(value);
        }
        if (isToWGS84()) {
            if (values.length > 7) {
                formatter.setInvalidWKT(BursaWolfParameters.class, null);
            }
            return WKTKeywords.ToWGS84;
        }
        formatter.setInvalidWKT(BursaWolfParameters.class, null);
        String name = IdentifiedObjects.getUnicodeIdentifier(getTargetDatum());
        if (name == null) {
            name = "Unknown";
        }
        // We may try to build something better here in future SIS versions, if there is a need for that.
        return "To" + name;
    }
}
