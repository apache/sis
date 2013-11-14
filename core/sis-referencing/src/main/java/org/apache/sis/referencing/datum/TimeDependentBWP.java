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
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.DoubleDouble;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.internal.referencing.Formulas.JULIAN_YEAR_LENGTH;


/**
 * Parameters for a time-dependent geographic transformation between two datum.
 * The {@link #tX tX}, {@link #tY tY}, {@link #tZ tZ}, {@link #rX rX}, {@link #rY rY}, {@link #rZ rZ}
 * and {@link #dS dS} parameters inherited from the parent class are values at a point in time given
 * by {@link #getTimeReference()}. Those values vary at a rate given by the parameters listed in the
 * table below (codes, names and abbreviations are from the EPSG database):
 *
 * <table class="compact"><tr><td>
 * <table class="sis">
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
 * @since   0.4
 * @version 0.4
 * @module
 */
public class TimeDependentBWP extends BursaWolfParameters {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4628799278259080258L;

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
     * sign following the <cite>Position Vector</cite> convention.
     */
    public double drX;

    /**
     * Rate of change of Y-axis rotation in milli arc-seconds per year (EPSG:1044),
     * sign following the <cite>Position Vector</cite> convention.
     */
    public double drY;

    /**
     * Rate of change of Z-axis rotation in milli arc-seconds per year (EPSG:1045),
     * sign following the <cite>Position Vector</cite> convention.
     */
    public double drZ;

    /**
     * Rate of change of the scale difference in parts per million per year (EPSG:1046).
     */
    public double ddS;

    /**
     * The reference epoch for time-dependent parameters (EPSG:1047).
     */
    private final long timeReference;

    /**
     * Creates a new instance for the given target datum, domain of validity and time reference.
     * All numerical parameters are initialized to 0, which correspond to an identity transform.
     * Callers can assign numerical values to the public fields of interest after construction.
     *
     * @param targetDatum The target datum (usually WGS 84) for this set of parameters.
     * @param domainOfValidity Area or region in which a coordinate transformation based on those Bursa-Wolf parameters
     *        is valid, or {@code null} is unspecified.
     * @param timeReference The reference epoch for time-dependent parameters.
     */
    public TimeDependentBWP(final GeodeticDatum targetDatum, final Extent domainOfValidity, final Date timeReference) {
        super(targetDatum, domainOfValidity);
        ensureNonNull("timeReference", timeReference);
        this.timeReference = timeReference.getTime();
    }

    /**
     * Verifies parameters validity after initialization.
     */
    @Override
    void verify() {
        super.verify();
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
     * @return The reference epoch for time-dependent parameters.
     */
    public Date getTimeReference() {
        return new Date(timeReference);
    }

    /**
     * Returns the elapsed time from the {@linkplain TimeDependentBWP#getTimeReference() reference time}
     * to the given date in millennium, or {@code null} if none.
     */
    @Override
    final DoubleDouble period(final Date time) {
        if (time != null) {
            final long millis = time.getTime() - timeReference;
            if (millis != 0) { // Returns null for 0 as an optimization.
                final DoubleDouble period = new DoubleDouble(millis, 0);
                period.divide(1000 * JULIAN_YEAR_LENGTH, 0);
                return period;
            }
        }
        return null;
    }

    /**
     * Returns the parameter at the given index. If this {@code BursaWolfParameters} is time-dependent,
     * then the returned value shall be corrected for the given period.
     *
     * @param index  0 for {@code tX}, 1 for {@code tY}, <i>etc.</i> in {@code TOWGS84[…]} order.
     * @param period The value computed by {@link #period(Date)}, or {@code null}.
     */
    @Override
    final DoubleDouble param(final int index, final DoubleDouble period) {
        final DoubleDouble p = super.param(index, period);
        if (period != null) {
            final double value = period.value;
            final double error = period.error;
            final double d;
            switch (index) {
                case 0: d = dtX; break;
                case 1: d = dtY; break;
                case 2: d = dtZ; break;
                case 3: d = drX; break;
                case 4: d = drY; break;
                case 5: d = drZ; break;
                case 6: d = ddS; period.multiply(1000, 0); break;
                default: throw new AssertionError(index);
            }
            period.multiply(d);
            p.add(period);
            period.value = value;
            period.error = error;
        }
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIdentity() {
        return super.isIdentity() && dtX == 0 && dtY == 0 && dtZ == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTranslation() {
        return super.isTranslation() && drX == 0 && drY == 0 && drZ == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invert() {
        super.invert();
        dtX = -dtX;
        dtY = -dtY;
        dtZ = -dtZ;
        drX = -drX;
        drY = -drY;
        drZ = -drZ;
        ddS = -ddS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final TimeDependentBWP that = (TimeDependentBWP) object;
            return timeReference == that.timeReference &&
                   Numerics.equals(this.dtX, that.dtX) &&
                   Numerics.equals(this.dtY, that.dtY) &&
                   Numerics.equals(this.dtZ, that.dtZ) &&
                   Numerics.equals(this.drX, that.drX) &&
                   Numerics.equals(this.drY, that.drY) &&
                   Numerics.equals(this.drZ, that.drZ) &&
                   Numerics.equals(this.ddS, that.ddS);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return ((int) serialVersionUID) ^ Arrays.hashCode(new double[] {
            tX, tY, tZ, rX, rY, rZ, dS, dtX, dtY, dtZ, drX, drY, drZ, ddS, timeReference
        });
    }
}
