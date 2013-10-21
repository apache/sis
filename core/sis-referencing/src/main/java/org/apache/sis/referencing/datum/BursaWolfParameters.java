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
import java.io.Serializable;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.referencing.IdentifiedObjects;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static org.apache.sis.referencing.operation.matrix.Matrix4.SIZE;

// Related to JDK7
import java.util.Objects;


/**
 * Parameters for a geographic transformation between two datum.
 * For an explanation of Bursa-Wolf parameters purpose, see the <cite>Bursa-Wolf parameters</cite>
 * section of {@link DefaultGeodeticDatum} class javadoc.
 *
 * <p>The Bursa-Wolf parameters shall be applied to geocentric coordinates,
 * where the <var>X</var> axis points towards the Greenwich Prime Meridian,
 * the <var>Y</var> axis points East, and the <var>Z</var> axis points North.</p>
 *
 * {@note The upper case letters are intentional. By convention, (<var>X</var>, <var>Y</var>, <var>Z</var>)
 *        stand for <cite>geocentric</cite> coordinates while (<var>x</var>, <var>y</var>, <var>z</var>)
 *        stand for <cite>projected</cite> coordinates.}
 *
 * The "Bursa-Wolf" formula is expressed with 7 parameters, listed in the table below.
 * The <cite>code</cite>, <cite>name</cite> and <cite>abbreviation</cite> columns list EPSG identifiers,
 * while the <cite>legacy</cite> column lists the identifiers used in the legacy OGC 01-009 specification
 * (still used in some <cite>Well Known Texts</cite>).
 *
 * <table class="compact"><tr><td>
 * <table class="sis">
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
 * <center><font size="-1">(ignoring unit conversions)</font></center>
 *
 * <p><math display="block" alttext="MathML capable browser required">
 *   <mfenced open="[" close="]">
 *     <mtable>
 *       <mtr><mtd><msub><mi>X</mi><mi>t</mi></msub></mtd></mtr>
 *       <mtr><mtd><msub><mi>Y</mi><mi>t</mi></msub></mtd></mtr>
 *       <mtr><mtd><msub><mi>Z</mi><mi>t</mi></msub></mtd></mtr>
 *     </mtable>
 *   </mfenced>
 *   <mo>=</mo>
 *   <mfenced><mn>1</mn><mo>+</mo><mi>dS</mi></mfenced>
 *   <mo>⋅</mo>
 *   <mfenced open="[" close="]">
 *     <mtable>
 *       <mtr>
 *         <mtd><mn>1</mn></mtd>
 *         <mtd><mo>-</mo><msub><mi>r</mi><mi>z</mi></msub></mtd>
 *         <mtd><mo>+</mo><msub><mi>r</mi><mi>y</mi></msub></mtd>
 *       </mtr>
 *       <mtr>
 *         <mtd><mo>+</mo><msub><mi>r</mi><mi>z</mi></msub></mtd>
 *         <mtd><mn>1</mn></mtd>
 *         <mtd><mo>-</mo><msub><mi>r</mi><mi>x</mi></msub></mtd>
 *       </mtr>
 *       <mtr>
 *         <mtd><mo>-</mo><msub><mi>r</mi><mi>y</mi></msub></mtd>
 *         <mtd><mo>+</mo><msub><mi>r</mi><mi>x</mi></msub></mtd>
 *         <mtd><mn>1</mn></mtd>
 *       </mtr>
 *     </mtable>
 *   </mfenced>
 *   <mo>×</mo>
 *   <mfenced open="[" close="]">
 *     <mtable>
 *       <mtr><mtd><msub><mi>X</mi><mi>s</mi></msub></mtd></mtr>
 *       <mtr><mtd><msub><mi>Y</mi><mi>s</mi></msub></mtd></mtr>
 *       <mtr><mtd><msub><mi>Z</mi><mi>s</mi></msub></mtd></mtr>
 *     </mtable>
 *   </mfenced>
 *   <mo>+</mo>
 *   <mfenced open="[" close="]">
 *     <mtable>
 *       <mtr><mtd><msub><mi>t</mi><mi>x</mi></msub></mtd></mtr>
 *       <mtr><mtd><msub><mi>t</mi><mi>y</mi></msub></mtd></mtr>
 *       <mtr><mtd><msub><mi>t</mi><mi>z</mi></msub></mtd></mtr>
 *     </mtable>
 *   </mfenced>
 * </math></p>
 * </tr></td></table>
 *
 * The numerical fields in this {@code BursaWolfParameters} class uses the EPSG abbreviations
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
 * {@section Target datum}
 * The <var>source datum</var> in above coordinates transformation is the {@link DefaultGeodeticDatum} instance
 * that contain this {@code BursaWolfParameters}. It can be any datum, including datum that are valid only locally.
 * But the {@linkplain #targetDatum target datum} is often fixed to WGS 84, since it is the target of the
 * {@code TOWGS84} element in <cite>Well Known Text</cite> (WKT) representations.
 * A different target may be specified at construction time, however users are encouraged to always specify a
 * target datum having a world-wide {@linkplain DefaultGeodeticDatum#getDomainOfValidity() domain of validity}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see DefaultGeodeticDatum#getBursaWolfParameters()
 */
@Immutable
public class BursaWolfParameters extends FormattableObject implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 754825592343010900L;

    /**
     * The conversion factor from <cite>parts per million</cite> to scale minus one.
     */
    private static final double PPM = 1E+6;

    /**
     * The conversion factor from arc-seconds to radians.
     */
    private static final double TO_RADIANS = PI / (180 * 60 * 60);

    /**
     * X-axis translation in metres (EPSG:8605).
     * The legacy OGC parameter name is {@code "dx"}.
     */
    public final double tX;

    /**
     * Y-axis translation in metres (EPSG:8606).
     * The legacy OGC parameter name is {@code "dy"}.
     */
    public final double tY;

    /**
     * Z-axis translation in metres (EPSG:8607).
     * The legacy OGC parameter name is {@code "dz"}.
     */
    public final double tZ;

    /**
     * X-axis rotation in arc seconds (EPSG:8608), sign following the <cite>Position Vector</cite> convention.
     * The legacy OGC parameter name is {@code "ex"}.
     */
    public final double rX;

    /**
     * Y-axis rotation in arc seconds (EPSG:8609), sign following the <cite>Position Vector</cite> convention.
     * The legacy OGC parameter name is {@code "ey"}.
     */
    public final double rY;

    /**
     * Z-axis rotation in arc seconds (EPSG:8610), sign following the <cite>Position Vector</cite> convention.
     * The legacy OGC parameter name is {@code "ez"}.
     */
    public final double rZ;

    /**
     * The scale difference in parts per million (EPSG:8611).
     * The legacy OGC parameter name is {@code "ppm"}.
     *
     * {@example If a distance of 100 km in the source coordinate reference system translates into a distance
     *           of 100.001 km in the target coordinate reference system, the scale difference is 1 ppm
     *           (the ratio being 1.000001).}
     */
    public final double dS;

    /**
     * The target datum for this set of parameters, or {@code null} if unspecified.
     * This is usually the WGS 84 datum, but other targets are allowed. We recommend the target datum
     * to have a world-wide {@linkplain DefaultGeodeticDatum#getDomainOfValidity() domain of validity},
     * but this is not enforced.
     *
     * <p>The source datum is the {@link DefaultGeodeticDatum} that contain this {@code BursaWolfParameters}
     * instance.</p>
     */
    public final GeodeticDatum targetDatum;

    /**
     * Creates a new instance with the given parameters.
     *
     * @param tX X-axis translation in metres.
     * @param tY Y-axis translation in metres.
     * @param tZ Z-axis translation in metres.
     * @param rX X-axis rotation in arc seconds.
     * @param rY Y-axis rotation in arc seconds.
     * @param rZ Z-axis rotation in arc seconds.
     * @param dS The scale difference in parts per million.
     * @param targetDatum The target datum (usually WGS 84) for this set of parameters, or {@code null} if unspecified.
     */
    public BursaWolfParameters(final double tX, final double tY, final double tZ,
                               final double rX, final double rY, final double rZ,
                               final double dS, final GeodeticDatum targetDatum)
    {
        this.tX = tX;
        this.tY = tY;
        this.tZ = tZ;
        this.rX = rX;
        this.rY = rY;
        this.rZ = rZ;
        this.dS = dS;
        this.targetDatum = targetDatum;
    }

    /**
     * Creates Bursa-Wolf parameters from the given matrix.
     * The matrix shall comply to the following constraints:
     *
     * <ul>
     *   <li>The matrix shall be {@linkplain org.apache.sis.referencing.operation.matrix.MatrixSIS#isAffine() affine}.</li>
     *   <li>The sub-matrix defined by {@code matrix} without the last row and last column shall be
     *       <a href="http://en.wikipedia.org/wiki/Skew-symmetric_matrix">skew-symmetric</a> (a.k.a. antisymmetric).</li>
     * </ul>
     *
     * @param  matrix The matrix to fit as a Bursa-Wolf construct.
     * @param  tolerance The tolerance error for the antisymmetric matrix test. Should be a small number like {@code 1E-8}.
     * @param  targetDatum The target datum (usually WGS 84) for this set of parameters, or {@code null} if unspecified.
     * @throws IllegalArgumentException if the specified matrix does not meet the conditions.
     *
     * @see #getPositionVectorTransformation(boolean)
     */
    public BursaWolfParameters(final Matrix matrix, final double tolerance, final GeodeticDatum targetDatum)
            throws IllegalArgumentException
    {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != SIZE || numCol != SIZE) {
            final Integer n = SIZE;
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedMatrixSize_4, n, n, numRow, numCol));
        }
        if (!Matrices.isAffine(matrix)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotAnAffineTransform));
        }
        tX = matrix.getElement(0,3);
        tY = matrix.getElement(1,3);
        tZ = matrix.getElement(2,3);
        final double S = (matrix.getElement(0,0) +
                          matrix.getElement(1,1) +
                          matrix.getElement(2,2)) / 3;
        final double RS = TO_RADIANS * S;
        dS = (S-1) * PPM;
        double rX=0, rY=0, rZ=0;
        for (int j=0; j < SIZE-1; j++) {
            if (!(abs((matrix.getElement(j,j) - 1)*PPM - dS) <= tolerance)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NonUniformScale));
            }
            for (int i = j+1; i < SIZE-1; i++) {
                final double elt1 = matrix.getElement(j,i) / RS;
                final double elt2 = matrix.getElement(i,j) / RS;
                if (!(abs(elt1 + elt2) <= tolerance)) { // We expect elt1 ≈ -elt2
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.NotASkewSymmetricMatrix));
                }
                final double elt = 0.5 * (elt2 - elt1);
                switch (j*SIZE + i) {
                    case 1: rZ =  elt; break;
                    case 2: rY = -elt; break;
                    case 6: rX =  elt; break;
                }
            }
        }
        this.rX = rX;
        this.rY = rY;
        this.rZ = rZ;
        this.targetDatum = targetDatum;
    }

    /**
     * Returns {@code true} if the {@linkplain #targetDatum target datum} is equals (at least on computation purpose)
     * to the WGS84 datum. If the datum is unspecified, then this method returns {@code true} since WGS84 is the only
     * datum supported by the WKT 1 format, and is what users often mean.
     *
     * @return {@code true} if the given datum is equal to WGS84 for computational purpose.
     */
    final boolean isToWGS84() {
        return (targetDatum == null) ||
                (IdentifiedObjects.nameMatches(targetDatum, "WGS 84") ||
                 IdentifiedObjects.nameMatches(targetDatum, "WGS84"));
    }

    /**
     * Returns {@code true} if this Bursa-Wolf parameters performs no operation.
     * This is true when all parameters are set to zero.
     *
     * @return {@code true} if the parameters describe no operation.
     */
    public boolean isIdentity() {
        return tX == 0 && tY == 0 && tZ == 0 &&
               rX == 0 && rY == 0 && rZ == 0 &&
               dS == 0;
    }

    /**
     * Returns {@code true} if this Bursa-Wolf parameters contains only translation terms.
     *
     * @return {@code true} if the parameters describe to a translation only.
     */
    public boolean isTranslation() {
        return rX == 0 && rY == 0 && rZ == 0 && dS == 0;
    }

    /**
     * Returns the position vector transformation (geocentric domain) as an affine transform.
     * The formula is as below, where {@code R} is a conversion factor from arc-seconds to radians:
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
     *
     * {@section Inverse transformation}
     * The inverse transformation can be computed by reversing the sign of the 7 parameters before to use
     * them in the above matrix. Note that both the direct and inverse transformations are approximations.
     * Multiplication of direct and inverse transformation matrices results in a matrix close to the identity,
     * but not necessarily strictly equals.
     *
     * @param  inverse If {@code true}, returns the inverse transformation instead.
     * @return An affine transform in geocentric space created from this Bursa-Wolf parameters.
     *
     * @see DefaultGeodeticDatum#getPositionVectorTransformation(GeodeticDatum)
     */
    public Matrix getPositionVectorTransformation(final boolean inverse) {
        final double sgn = inverse ? -1 : +1;
        final double   S = 1 + sgn*dS / PPM;
        final double  RS = sgn*TO_RADIANS * S;
        return new Matrix4(
                 S,  -rZ*RS,  +rY*RS,  sgn*tX,
            +rZ*RS,       S,  -rX*RS,  sgn*tY,
            -rY*RS,  +rX*RS,       S,  sgn*tZ,
                 0,       0,       0,      1);
    }

    /**
     * Compares the specified object with this object for equality.
     *
     * @param object The object to compare with the parameters.
     * @return {@code true} if the given object is equal to this {@code BursaWolfParameters}.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof BursaWolfParameters) {
            final BursaWolfParameters that = (BursaWolfParameters) object;
            return Numerics.equals(this.tX, that.tX) &&
                   Numerics.equals(this.tY, that.tY) &&
                   Numerics.equals(this.tZ, that.tZ) &&
                   Numerics.equals(this.rX, that.rX) &&
                   Numerics.equals(this.rY, that.rY) &&
                   Numerics.equals(this.rZ, that.rZ) &&
                   Numerics.equals(this.dS, that.dS) &&
                    Objects.equals(this.targetDatum, that.targetDatum);
        }
        return false;
    }

    /**
     * Returns a hash value for this object.
     *
     * @return The hash code value. This value doesn't need to be the same
     *         in past or future versions of this class.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(new double[] {tX, tY, tZ, rX, rY, rZ, dS}) ^ (int) serialVersionUID;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT) element. The WKT contains the
     * parameters in <var>translation</var>, <var>rotation</var>, <var>scale</var> order, like below:
     *
     * <blockquote><code>TOWGS84[{@linkplain #tX}, {@linkplain #tY}, {@linkplain #tZ}, {@linkplain #rX},
     * {@linkplain #rY}, {@linkplain #rZ}, {@linkplain #dS}]</code></blockquote>
     *
     * The element name is {@code "TOWGS84"} in the common case where the {@linkplain #targetDatum target datum}
     * is WGS 84. For other targets, the element name will be derived from the datum name.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, usually {@code "TOWGS84"}.
     */
    @Override
    public String formatTo(final Formatter formatter) {
        formatter.append(tX);
        formatter.append(tY);
        formatter.append(tZ);
        formatter.append(rX);
        formatter.append(rY);
        formatter.append(rZ);
        formatter.append(dS);
        if (isToWGS84()) {
            return "TOWGS84";
        }
        String keyword = super.formatTo(formatter); // Declare the WKT as invalid.
        final String name = IdentifiedObjects.getName(targetDatum, null);
        if (name != null) {
            // We may try to build something better here in future SIS versions, if there is a need for that.
            keyword = "To" + name;
        }
        return keyword;
    }
}
