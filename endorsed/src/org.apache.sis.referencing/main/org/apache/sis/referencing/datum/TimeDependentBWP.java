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

import java.util.Objects;
import java.time.Duration;
import java.time.temporal.Temporal;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.util.internal.shared.Constants;
import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Parameters for a time-dependent geographic transformation between two datum.
 * The {@link #tX tX}, {@link #tY tY}, {@link #tZ tZ}, {@link #rX rX}, {@link #rY rY}, {@link #rZ rZ}
 * and {@link #dS dS} parameters inherited from the parent class are values at a point in time given
 * by {@link #getTimeReference()}. Those values vary at a rate given by the parameters listed in the
 * table below (codes, names and abbreviations are from the EPSG database):
 *
 * <table class="sis">
 *   <caption>Parameters defined by EPSG</caption>
 *   <tr><th>Code</th> <th>Name</th>                                 <th>Abbr.</th></tr>
 *   <tr><td>1040</td> <td>Rate of change of X-axis translation</td> <td>{@link #dtX}</td></tr>
 *   <tr><td>1041</td> <td>Rate of change of Y-axis translation</td> <td>{@link #dtY}</td></tr>
 *   <tr><td>1042</td> <td>Rate of change of Z-axis translation</td> <td>{@link #dtZ}</td></tr>
 *   <tr><td>1043</td> <td>Rate of change of X-axis rotation</td>    <td>{@link #drX}</td></tr>
 *   <tr><td>1044</td> <td>Rate of change of Y-axis rotation</td>    <td>{@link #drY}</td></tr>
 *   <tr><td>1045</td> <td>Rate of change of Z-axis rotation</td>    <td>{@link #drZ}</td></tr>
 *   <tr><td>1046</td> <td>Rate of change of scale difference</td>   <td>{@link #ddS}</td></tr>
 * </table>
 *
 * The numerical fields in this {@code TimeDependentBWP} class uses the EPSG abbreviations
 * with 4 additional constraints compared to the EPSG definitions:
 *
 * <ul>
 *   <li>Unit of {@link #ddS} is fixed to <em>parts per million per year</em>.</li>
 *   <li>Unit of {@link #dtX}, {@link #dtY} and {@link #dtZ} is fixed to <em>millimetres per year</em>.</li>
 *   <li>Unit of {@link #drX}, {@link #drY} and {@link #drZ} is fixed to <em>milli arc-seconds per year</em>.</li>
 *   <li>Sign of rotation terms is fixed to the <em>Position Vector</em> convention (EPSG operation method 1053).
 *       This is the opposite sign than the <cite>Coordinate Frame Rotation</cite> (EPSG operation method 1056.
 *       The Position Vector convention is used by IAG and recommended by ISO 19111.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   0.4
 */
@SuppressWarnings("CloneableImplementsClone")                   // Fields in this class do not need cloning.
public class TimeDependentBWP extends BursaWolfParameters {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8404372938646871943L;

    /**
     * Rate of change of X-axis translation in millimetres per year (EPSG:1040).
     */
    public double dtX;

    /**
     * Rate of change of Y-axis translation in millimetres per year (EPSG:1041).
     */
    public double dtY;

    /**
     * Rate of change of Z-axis translation in millimetres per year (EPSG:1042).
     */
    public double dtZ;

    /**
     * Rate of change of X-axis rotation in milli arc-seconds per year (EPSG:1043),
     * sign following the <i>Position Vector</i> convention.
     */
    public double drX;

    /**
     * Rate of change of Y-axis rotation in milli arc-seconds per year (EPSG:1044),
     * sign following the <i>Position Vector</i> convention.
     */
    public double drY;

    /**
     * Rate of change of Z-axis rotation in milli arc-seconds per year (EPSG:1045),
     * sign following the <i>Position Vector</i> convention.
     */
    public double drZ;

    /**
     * Rate of change of the scale difference in parts per million per year (EPSG:1046).
     */
    public double ddS;

    /**
     * The reference epoch for time-dependent parameters (EPSG:1047).
     */
    @SuppressWarnings("serial")         // Most implementations are serializable.
    private final Temporal timeReference;

    /**
     * Creates a new instance for the given target datum, domain of validity and time reference.
     * All numerical parameters are initialized to 0, which correspond to an identity transform.
     * Callers can assign numerical values to the public fields of interest after construction.
     *
     * @param targetDatum       the target datum (usually WGS 84) for this set of parameters.
     * @param domainOfValidity  area or region in which a coordinate transformation based on those Bursa-Wolf parameters
     *                          is valid, or {@code null} if unspecified.
     * @param timeReference     the reference epoch for time-dependent parameters.
     *
     * @since 1.5
     */
    public TimeDependentBWP(final GeodeticDatum targetDatum, final Extent domainOfValidity, final Temporal timeReference) {
        super(targetDatum, domainOfValidity);
        this.timeReference = Objects.requireNonNull(timeReference);
    }

    /**
     * Verifies parameters validity after initialization.
     */
    @Override
    void verify(final PrimeMeridian pm) throws IllegalArgumentException {
        super.verify(pm);
        ensureFinite("dtX", dtX);
        ensureFinite("dtY", dtY);
        ensureFinite("dtZ", dtZ);
        ensureFinite("drX", drX);
        ensureFinite("drY", drY);
        ensureFinite("drZ", drZ);
    }

    /**
     * Returns the reference epoch for time-dependent parameters.
     *
     * @return the reference epoch for time-dependent parameters.
     */
    public Temporal getTimeReference() {
        return timeReference;
    }

    /**
     * Returns the elapsed time from the reference time to the given date, or {@code null} if none.
     * The reference time is given by {@link #getTimeReference()}.
     *
     * @return fractional number of tropical years since reference time, or {@code null}.
     */
    @Override
    final DoubleDouble period(final Temporal time) {
        if (time != null) {
            final Duration d = Duration.between(timeReference, time);
            if (!d.isZero()) {      // Returns null for 0 as an optimization.
                return DoubleDouble.of(d).divide(Constants.MILLIS_PER_TROPICAL_YEAR * Constants.NANOS_PER_MILLISECOND);
            }
        }
        return null;
    }

    /**
     * Returns the parameter at the given index, taking rate of change in account.
     * The given {@code factor} argument shall contain a conversion factor from mm/year to m/year
     * except for parameter at index 6.
     *
     * @param  index   0 for {@code tX}, 1 for {@code tY}, <i>etc.</i> in {@code TOWGS84[…]} order.
     * @param  factor  factor by which to multiply the rate of change, or {@code null}.
     */
    @Override
    final DoubleDouble param(final int index, final DoubleDouble factor) {
        DoubleDouble p = super.param(index, factor);
        if (factor != null) {
            final double d;
            switch (index) {
                case 0: d = dtX; break;
                case 1: d = dtY; break;
                case 2: d = dtZ; break;
                case 3: d = drX; break;
                case 4: d = drY; break;
                case 5: d = drZ; break;
                case 6: d = ddS; break;
                default: throw new AssertionError(index);
            }
            p = p.add(factor.multiply(d, true));
        }
        return p;
    }

    /**
     * Returns the parameter values. The first 14 elements are always {@link #tX tX}, {@link #tY tY}, {@link #tZ tZ},
     * {@link #rX rX}, {@link #rY rY}, {@link #rZ rZ}, {@link #dS dS}, {@link #dtX}, {@link #dtY}, {@link #dtZ},
     * {@link #drX}, {@link #drY}, {@link #drZ} and {@link #ddS} in that order.
     *
     * @return the parameter values as an array of length 14.
     *
     * @since 0.6
     */
    @Override
    public double[] getValues() {
        return new double[] {tX, tY, tZ, rX, rY, rZ, dS, dtX, dtY, dtZ, drX, drY, drZ, ddS};
    }

    /**
     * Sets the parameters to the given values. The given array can have any length. The first array elements will be
     * assigned to the {@link #tX tX}, {@link #tY tY}, {@link #tZ tZ}, {@link #rX rX}, {@link #rY rY}, {@link #rZ rZ},
     * {@link #dS dS}, {@link #dtX}, {@link #dtY}, {@link #dtZ}, {@link #drX}, {@link #drY}, {@link #drZ} and
     * {@link #ddS} fields in that order.
     *
     * @param  elements  the new parameter values, as an array of any length.
     *
     * @since 0.6
     */
    @Override
    @SuppressWarnings("fallthrough")
    public void setValues(final double... elements) {
        if (elements.length >= 8) {
            switch (elements.length) {
                default:  ddS = elements[13];  // Fallthrough everywhere.
                case 13:  drZ = elements[12];
                case 12:  drY = elements[11];
                case 11:  drX = elements[10];
                case 10:  dtZ = elements[ 9];
                case  9:  dtY = elements[ 8];
                case  8:  dtX = elements[ 7];
            }
        }
        super.setValues(elements);
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public boolean isIdentity() {
        return super.isIdentity() && dtX == 0 && dtY == 0 && dtZ == 0;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public boolean isTranslation() {
        return super.isTranslation() && drX == 0 && drY == 0 && drZ == 0;
    }

    /**
     * Inverts in-place the sign of rotation terms and their derivative.
     * This method can be invoked for converting a <cite>Coordinate Frame Rotation</cite> transformation
     * (EPSG operation method 9607) to a <em>Position Vector</em> transformation (EPSG operation method 9606).
     * The latter convention is used by IAG and recommended by ISO 19111.
     */
    @Override
    public void reverseRotation() {
        super.reverseRotation();
        drX = -drX;
        drY = -drY;
        drZ = -drZ;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object) {
        return super.equals(object) && timeReference == ((TimeDependentBWP) object).timeReference;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public int hashCode() {
        return super.hashCode() ^ timeReference.hashCode();
    }
}
