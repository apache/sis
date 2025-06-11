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
import java.time.temporal.Temporal;
import java.io.Serializable;
import static java.lang.Math.abs;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.internal.Resources;
import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.referencing.operation.matrix.Matrix4.SIZE;


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
 * stand for <i>geocentric</i> coordinates while (<var>x</var>, <var>y</var>, <var>z</var>)
 * stand for <i>projected</i> coordinates.</div>
 *
 * The "Bursa-Wolf" formula is expressed with 7 parameters, listed in the table below.
 * The <i>code</i>, <i>name</i> and <i>abbreviation</i> columns list EPSG identifiers,
 * while the <i>legacy</i> column lists the identifiers used in the legacy OGC 01-009 specification
 * (still used in some <i>Well Known Texts</i>).
 *
 * <div class="horizontal-flow">
 * <div><table class="sis">
 *   <caption>Parameters defined by EPSG</caption>
 *   <tr><th>Code</th> <th>Name</th>               <th>Abbr.</th>       <th>Legacy</th></tr>
 *   <tr><td>8605</td> <td>X-axis translation</td> <td>{@link #tX}</td> <td>{@code dx}</td></tr>
 *   <tr><td>8606</td> <td>Y-axis translation</td> <td>{@link #tY}</td> <td>{@code dy}</td></tr>
 *   <tr><td>8607</td> <td>Z-axis translation</td> <td>{@link #tZ}</td> <td>{@code dz}</td></tr>
 *   <tr><td>8608</td> <td>X-axis rotation</td>    <td>{@link #rX}</td> <td>{@code ex}</td></tr>
 *   <tr><td>8609</td> <td>Y-axis rotation</td>    <td>{@link #rY}</td> <td>{@code ey}</td></tr>
 *   <tr><td>8610</td> <td>Z-axis rotation</td>    <td>{@link #rZ}</td> <td>{@code ez}</td></tr>
 *   <tr><td>8611</td> <td>Scale difference</td>   <td>{@link #dS}</td> <td>{@code ppm}</td></tr>
 * </table></div>
 * <div><p><b>Geocentric coordinates transformation</b></p>
 * <p>from (<var>X</var><sub>s</sub>, <var>Y</var><sub>s</sub>, <var>Z</var><sub>s</sub>)
 *      to (<var>X</var><sub>t</sub>, <var>Y</var><sub>t</sub>, <var>Z</var><sub>t</sub>)
 * <br><span style="font-size:small">(ignoring unit conversions)</span></p>
 *
 * <p>{@include formulas.html#Bursa-Wolf}</p>
 * </div></div>
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
 * <h2>Source and target geodetic reference frames</h2>
 * The <var>source datum</var> in above coordinates transformation is the {@link DefaultGeodeticDatum} instance
 * that contain this {@code BursaWolfParameters}. It can be any datum, including datum that are valid only locally.
 * The <var>{@linkplain #getTargetDatum() target datum}</var> is specified at construction time and is often,
 * but not necessarily, the <cite>World Geodetic System 1984</cite> (WGS 84) datum.
 *
 * <p>If the source and target datum does not have the same {@linkplain DefaultGeodeticDatum#getPrimeMeridian()
 * prime meridian}, then it is user's responsibility to apply longitude rotation before to use the Bursa-Wolf
 * parameters.</p>
 *
 * <h2>When Bursa-Wolf parameters are used</h2>
 * {@code BursaWolfParameters} are used in three contexts:
 * <ol>
 *   <li>Created as a step while creating a {@linkplain org.apache.sis.referencing.operation.AbstractCoordinateOperation
 *       coordinate operation} from the EPSG database.</li>
 *   <li>Associated to a {@link DefaultGeodeticDatum} with the WGS 84 {@linkplain #getTargetDatum() target datum} for
 *       providing the parameter values to display in the {@code TOWGS84[…]} element of <i>Well Known Text</i>
 *       (WKT) version 1. Note that WKT version 2 does not have {@code TOWGS84[…]} element anymore.</li>
 *   <li>Specified at {@code DefaultGeodeticDatum} construction time for arbitrary target datum.
 *       Apache SIS will ignore those Bursa-Wolf parameters, except as a fallback if no parameters
 *       can be found in the EPSG database for a given pair of source and target CRS.</li>
 * </ol>
 *
 * In EPSG terminology, Apache SIS gives precedence to the <i>late-binding</i> approach
 * (case 1 above) over the <i>early-binding</i> approach (case 3 above).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see DefaultGeodeticDatum#getBursaWolfParameters()
 * @see <a href="https://en.wikipedia.org/wiki/Helmert_transformation">Wikipedia: Helmert transformation</a>
 *
 * @since 0.4
 */
public class BursaWolfParameters extends FormattableObject implements Cloneable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 754825592343010900L;

    /**
     * The conversion factor from <i>parts per million</i> to scale minus one.
     */
    static final int PPM = 1000000;

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
     * X-axis rotation in arc-seconds (EPSG:8608), sign following the <i>Position Vector</i> convention.
     * The legacy OGC parameter name is {@code "ex"}.
     */
    public double rX;

    /**
     * Y-axis rotation in arc-seconds (EPSG:8609), sign following the <i>Position Vector</i> convention.
     * The legacy OGC parameter name is {@code "ey"}.
     */
    public double rY;

    /**
     * Z-axis rotation in arc-seconds (EPSG:8610), sign following the <i>Position Vector</i> convention.
     * The legacy OGC parameter name is {@code "ez"}.
     */
    public double rZ;

    /**
     * The scale difference in parts per million (EPSG:8611).
     * The legacy OGC parameter name is {@code "ppm"}.
     *
     * <h4>Example</h4>
     * If a distance of 100 km in the source coordinate reference system translates into a distance of 100.001 km
     * in the target coordinate reference system, the scale difference is 1 ppm (the ratio being 1.000001).
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
    @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
    private final GeodeticDatum targetDatum;

    /**
     * Region or timeframe in which a coordinate transformation based on those Bursa-Wolf parameters is valid,
     * or {@code null} if unspecified.
     *
     * @see #getDomainOfValidity()
     */
    @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
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
     * @param targetDatum       the target datum (usually WGS 84) for this set of parameters, or {@code null} if unknown.
     * @param domainOfValidity  area or region in which a coordinate transformation based on those Bursa-Wolf parameters
     *                          is valid, or {@code null} if unspecified.
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
     * @param  pm  the prime meridian of the enclosing {@code GeodeticDatum}.
     */
    void verify(final PrimeMeridian pm) throws IllegalArgumentException {
        if (targetDatum != null) {
            final PrimeMeridian actual = targetDatum.getPrimeMeridian();
            if (actual.getGreenwichLongitude() != 0 && !Utilities.equalsIgnoreMetadata(pm, actual)) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedPrimeMeridian_2,
                        IdentifiedObjects.getDisplayName(pm, null),
                        IdentifiedObjects.getDisplayName(actual, null)));
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
     * @return the target datum for this set of parameters, or {@code null} if unknown.
     */
    @OptionalCandidate
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
     * <h4>Compatibility note</h4>
     * The rules about the arrays of length 3, 6 or 7 are derived from the <cite>Well Known Text</cite> (WKT)
     * version 1 specification. The rule about the array of length 14 is an extension.
     *
     * @return the parameter values as an array of length 3, 6, 7 or 14.
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
     * @param  elements  the new parameter values, as an array of any length.
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
     * Returns {@code true} if the {@linkplain #targetDatum target datum} is equal (at least on computation purpose)
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
     * The latter convention is used by IAG and recommended by ISO 19111.
     */
    public void reverseRotation() {
        rX = -rX;
        rY = -rY;
        rZ = -rZ;
    }

    /**
     * Inverts in-place the transformation by inverting the sign of all numerical parameters.
     * The {@linkplain #getPositionVectorTransformation(Temporal) position vector transformation} matrix
     * created from inverted Bursa-Wolf parameters will be <strong>approximately</strong> equals
     * to the {@linkplain org.apache.sis.referencing.operation.matrix.MatrixSIS#inverse() inverse}
     * of the matrix created from the original parameters. The equality holds approximately only
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
     * Returns the elapsed time from the reference time to the given date, or {@code null} if none.
     * If this {@code BursaWolfParameters} is not time-dependent, then returns {@code null}.
     *
     * @return fractional number of tropical years since reference time, or {@code null}.
     */
    DoubleDouble period(final Temporal time) {
        return null;
    }

    /**
     * Returns the parameter at the given index. If this {@code BursaWolfParameters} is time-dependent,
     * then the returned value shall be corrected for the time elapsed since the reference time.
     *
     * The {@code factor} argument shall be the value computed by {@link #period(Temporal)},
     * multiplied by 1000 for all {@code index} values except 6.
     * The 1000 factor is for conversion mm/year to m/year or milli-arc-seconds to arc-seconds.
     *
     * @param  index   0 for {@code tX}, 1 for {@code tY}, <i>etc.</i> in {@code TOWGS84[…]} order.
     * @param  factor  factor by which to multiply the rate of change, or {@code null}.
     */
    DoubleDouble param(final int index, final DoubleDouble factor) {
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
        return DoubleDouble.of(p, true);
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
     * <h4>Time-dependent transformation</h4>
     * Some transformations use parameters that vary with time (e.g. operation method EPSG:1053).
     * Users can optionally specify a date for which the transformation is desired.
     * For transformations that do not depends on time, this date is ignored and can be null.
     * For time-dependent transformations, {@code null} values default to the transformation's
     * {@linkplain TimeDependentBWP#getTimeReference() reference time}.
     *
     * <h4>Inverse transformation</h4>
     * The inverse transformation can be approximated by reversing the sign of the 7 parameters before to use them
     * in the above matrix. This is often considered sufficient since <cite>position vector transformations</cite>
     * are themselves approximations. However, Apache SIS will rather use
     * {@link org.apache.sis.referencing.operation.matrix.MatrixSIS#inverse()} in order to increase the chances
     * that concatenation of transformations <var>A</var> → <var>B</var> followed by <var>B</var> → <var>A</var>
     * gives back the identity transform.
     *
     * @param  time  date for which the transformation is desired, or {@code null} for the transformation's reference time.
     * @return an affine transform in geocentric space created from this Bursa-Wolf parameters and the given time.
     *
     * @see DefaultGeodeticDatum#getPositionVectorTransformation(GeodeticDatum, Extent)
     *
     * @since 1.5
     */
    public Matrix getPositionVectorTransformation(final Temporal time) {
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
        DoubleDouble mp = (period != null) ? period.divide(1000) : null;    // Convert millimetre to metre.
        DoubleDouble RS = DoubleDouble.SECONDS_TO_RADIANS;
        DoubleDouble S = param(6, period).divide(PPM).add(1);       // S = 1 + dS / PPM;
        RS = RS.multiply(S);                                        // RS = toRadians(1″) * S;
        final DoubleDouble pX = param(3, mp).multiply(RS);
        final DoubleDouble pY = param(4, mp).multiply(RS);
        final DoubleDouble pZ = param(5, mp).multiply(RS);
        final DoubleDouble mX = pX.negate();
        final DoubleDouble mY = pY.negate();
        final DoubleDouble mZ = pZ.negate();
        final Integer       O = 0;                                  // Fetch Integer instance only once.
        return Matrices.create(4, 4, new Number[] {
                 S,  mZ,  pY,  param(0, mp),
                pZ,   S,  mX,  param(1, mp),
                mY,  pX,   S,  param(2, mp),
                 O,   O,   O,  1});
    }

    /**
     * Sets all Bursa-Wolf parameters from the given <cite>Position Vector transformation</cite> matrix.
     * The matrix shall comply to the following constraints:
     *
     * <ul>
     *   <li>The matrix shall be {@linkplain org.apache.sis.referencing.operation.matrix.MatrixSIS#isAffine() affine}.</li>
     *   <li>The sub-matrix defined by {@code matrix} without the last row and last column shall be
     *       <a href="https://en.wikipedia.org/wiki/Skew-symmetric_matrix">skew-symmetric</a> (a.k.a. antisymmetric).</li>
     * </ul>
     *
     * @param  matrix     the matrix from which to get Bursa-Wolf parameters.
     * @param  tolerance  the tolerance error for the skew-symmetric matrix test, in units of PPM or arc-seconds (e.g. 1E-8).
     * @throws IllegalArgumentException if the specified matrix does not met the conditions.
     *
     * @see #getPositionVectorTransformation(Temporal)
     */
    public void setPositionVectorTransformation(final Matrix matrix, final double tolerance) throws IllegalArgumentException {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != SIZE || numCol != SIZE) {
            final Integer n = SIZE;
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedMatrixSize_4, n, n, numRow, numCol));
        }
        if (!Matrices.isAffine(matrix)) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.NotAnAffineTransform));
        }
        /*
         * Translation terms, taken "as-is".
         * If the matrix contains only translation terms (which is often the case), we are done.
         */
        tX = matrix.getElement(0,3);
        tY = matrix.getElement(1,3);
        tZ = matrix.getElement(2,3);
        if (Matrices.isTranslation(matrix)) {                   // Optimization for a common case.
            return;
        }
        /*
         * Scale factor: take the average of elements on the diagonal. All those
         * elements should have the same value, but we tolerate slight deviation
         * (this will be verified later).
         */
        DoubleDouble RS = DoubleDouble.of(getNumber(matrix, 0, 0), true)
                                     .add(getNumber(matrix, 1, 1), true)
                                     .add(getNumber(matrix, 2, 2), true).divide(3);
        /*
         * Computes: RS = S * toRadians(1″)
         *           dS = (S-1) * PPM
         */
        dS = RS.add(-1).multiply(PPM).doubleValue();
        RS = RS.multiply(DoubleDouble.SECONDS_TO_RADIANS);
        /*
         * Rotation terms. Each rotation terms appear twice, with one value being the negative of the other value.
         * We verify this skew symmetric aspect in the loop. We also opportunistically verify that the scale terms
         * are uniform.
         */
        for (int j=0; j < SIZE-1; j++) {
            if (!(abs((matrix.getElement(j,j) - 1)*PPM - dS) <= tolerance)) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.NonUniformScale));
            }
            for (int i = j+1; i < SIZE-1; i++) {
                final DoubleDouble mr = DoubleDouble.of(getNumber(matrix, j, i), true).divide(RS);    // Negative rotation term.
                final DoubleDouble pr = DoubleDouble.of(getNumber(matrix, i, j), true).divide(RS);    // Positive rotation term.
                if (!(abs(pr.value + mr.value) <= 2*tolerance)) {                                     // We expect mr ≈ -pr.
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.NotASkewSymmetricMatrix));
                }
                final double value = pr.subtract(mr).scalb(-1).doubleValue();   // Average of the two rotation terms.
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
     * @param  matrix  the matrix from which to get the number.
     * @param  row     the row index, from 0 inclusive to {@link Matrix#getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link Matrix#getNumCol()} exclusive.
     * @return the current value at the given row and column.
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
     * @return area or region or timeframe in which the coordinate transformation is valid, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    @OptionalCandidate
    public Extent getDomainOfValidity() {
        if (domainOfValidity == null && targetDatum != null) {
            return IdentifiedObjects.getDomainOfValidity(targetDatum).orElse(null);
        }
        return domainOfValidity;
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of all parameters.
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
     * @param  object  the object to compare with the parameters.
     * @return {@code true} if the given object is equal to this {@code BursaWolfParameters}.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final var that = (BursaWolfParameters) object;
            return Arrays.equals(this.getValues(),      that.getValues()) &&
                  Objects.equals(this.targetDatum,      that.targetDatum) &&
                  Objects.equals(this.domainOfValidity, that.domainOfValidity);
        }
        return false;
    }

    /**
     * Returns a hash value for this object.
     *
     * @return the hash code value. This value does not need to be the same in past or future versions of this class.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(getValues()) ^ (int) serialVersionUID;
    }

    /**
     * Formats this object as a <i>Well Known Text</i> {@code ToWGS84[…]} element.
     * The WKT contains the parameters in <var>translation</var>, <var>rotation</var>, <var>scale</var> order,
     * like below:
     *
     * <blockquote><code>TOWGS84[{@linkplain #tX}, {@linkplain #tY}, {@linkplain #tZ}, {@linkplain #rX},
     * {@linkplain #rY}, {@linkplain #rZ}, {@linkplain #dS}]</code></blockquote>
     *
     * The element name is {@code "ToWGS84"} in the common case where the {@linkplain #getTargetDatum() target datum}
     * is WGS 84. For other targets, the element name will be derived from the datum name.
     *
     * <h4>Compatibility note</h4>
     * {@code TOWGS84} is defined in the WKT 1 specification only.
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
        String name = IdentifiedObjects.getSimpleNameOrIdentifier(getTargetDatum());
        if (name == null) {
            name = "Unknown";
        }
        // We may try to build something better here in future SIS versions, if there is a need for that.
        return "To" + name;
    }
}
