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
package org.apache.sis.measure;

import java.math.BigDecimal;
import java.math.MathContext;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.converter.UnitConverter;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.PatchedUnitFormat;

import static org.apache.sis.math.MathFunctions.truncate;


/**
 * A converter from fractional degrees to sexagesimal degrees. Sexagesimal degrees are pseudo-unit
 * in the <cite>sign - degrees - decimal point - minutes (two digits) - integer seconds (two digits) -
 * fraction of seconds (any precision)</cite> format.
 *
 * <p>When possible, Apache SIS always handles angles in radians, decimal degrees or any other
 * proportional units. Sexagesimal angles are considered a string representation issue (handled
 * by {@link AngleFormat}) rather than a unit issue. Unfortunately, this pseudo-unit is extensively
 * used in the EPSG database, so we have to support it.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
class SexagesimalConverter extends UnitConverter { // Intentionally not final.
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -2119974989555436361L;

    /**
     * Small tolerance factor when comparing numbers close to 1.
     * For comparing numbers other than 1, multiply by the number magnitude.
     */
    static final double EPS = 1E-10;

    /**
     * Pseudo-unit for sexagesimal degree. Numbers in this pseudo-unit have the following format:
     *
     * <cite>sign - degrees - decimal point - minutes (two digits) - fraction of minutes (any precision)</cite>.
     *
     * Using this unit is loosely equivalent to formatting decimal degrees with the
     * {@code "D.MMm"} {@link AngleFormat} pattern.
     *
     * <p>This unit is non-linear and not practical for computation. Consequently, it should be
     * avoided as much as possible. This pseudo-unit is defined only because used in the EPSG
     * database (code 9111).</p>
     *
     * <p>This unit does not have an easily readable symbol because of the
     * <a href="http://kenai.com/jira/browse/JSR_275-41">JSR-275 bug</a>.</p>
     */
    static final Unit<Angle> DM = NonSI.DEGREE_ANGLE.transform(
            new SexagesimalConverter(false, 100).inverse()).asType(Angle.class);//.alternate("D.M");

    /**
     * Pseudo-unit for sexagesimal degree. Numbers in this pseudo-unit have the following format:
     *
     * <cite>sign - degrees - decimal point - minutes (two digits) - integer seconds (two digits) -
     * fraction of seconds (any precision)</cite>.
     *
     * Using this unit is loosely equivalent to formatting decimal degrees with the
     * {@code "D.MMSSs"} {@link AngleFormat} pattern.
     *
     * <p>This unit is non-linear and not practical for computation. Consequently, it should be
     * avoided as much as possible. This pseudo-unit is defined only because extensively used in
     * the EPSG database (code 9110).</p>
     *
     * <p>This unit does not have an easily readable symbol because of the
     * <a href="http://kenai.com/jira/browse/JSR_275-41">JSR-275 bug</a>.</p>
     */
    static final Unit<Angle> DMS = NonSI.DEGREE_ANGLE.transform(
            new SexagesimalConverter(true, 10000).inverse()).asType(Angle.class);//.alternate("D.MS");

    /**
     * Pseudo-unit for degree - minute - second.
     * Numbers in this pseudo-unit have the following format:
     *
     * <cite>signed degrees (integer) - arc-minutes (integer) - arc-seconds
     * (real, any precision)</cite>.
     *
     * Using this unit is loosely equivalent to formatting decimal degrees with the
     * {@code "DMMSS.s"} {@link AngleFormat} pattern.
     *
     * <p>This unit is non-linear and not practical for computation. Consequently, it should be
     * avoided as much as possible. This pseudo-unit is defined only because extensively used in
     * EPSG database (code 9107).</p>
     *
     * <p>This unit does not have an easily readable symbol because of the
     * <a href="http://kenai.com/jira/browse/JSR_275-41">JSR-275 bug</a>.</p>
     */
    static final Unit<Angle> DMS_SCALED = NonSI.DEGREE_ANGLE.transform(
            new SexagesimalConverter(true, 1).inverse()).asType(Angle.class);//.alternate("DMS");

    /*
     * Declares the units that we were not able to declare in calls to Unit.alternate(String).
     */
    static {
        PatchedUnitFormat.init(DM, "D.M", DMS, "D.MS", DMS_SCALED, "DMS");
    }

    /**
     * {@code true} if the seconds field is present.
     */
    final boolean hasSeconds;

    /**
     * The value to divide DMS unit by.
     * For "degree minute second" (EPSG code 9107), this is 1.
     * For "sexagesimal degree" (EPSG code 9110), this is 10000.
     */
    final double divider;

    /**
     * The inverse of this converter.
     */
    private final UnitConverter inverse;

    /**
     * Constructs a converter for sexagesimal units.
     *
     * @param hasSeconds {@code true} if the seconds field is present.
     * @param divider The value to divide DMS unit by.
     *        For "degree minute second" (EPSG code 9107), this is 1.
     *        For "sexagesimal degree" (EPSG code 9110), this is 10000.
     */
    private SexagesimalConverter(final boolean hasSeconds, final double divider) {
        this.hasSeconds = hasSeconds;
        this.divider    = divider;
        this.inverse    = new Inverse(this);
    }

    /**
     * Constructs a converter for sexagesimal units.
     * This constructor is for {@link Inverse} usage only.
     */
    private SexagesimalConverter(final SexagesimalConverter inverse) {
        this.hasSeconds = inverse.hasSeconds;
        this.divider    = inverse.divider;
        this.inverse    = inverse;
    }

    /**
     * Returns the inverse of this converter.
     */
    @Override
    public final UnitConverter inverse() {
        return inverse;
    }

    /**
     * Performs a conversion from fractional degrees to sexagesimal degrees.
     */
    @Override
    public double convert(double angle) {
        final double deg = truncate(angle);
        angle = (angle - deg) * 60;
        if (hasSeconds) {
            final double min = truncate(angle);
            angle  = (angle - min) * 60; // Secondes
            angle += (deg*100 + min)*100;
        } else {
            angle += deg * 100;
        }
        return angle / divider;
    }

    /**
     * Performs a conversion from fractional degrees to sexagesimal degrees.
     * This method delegates to the version working on {@code double} primitive type,
     * so it does not provide the accuracy normally required by this method contract.
     */
    @Override
    public final BigDecimal convert(final BigDecimal value, final MathContext context) {
        return new BigDecimal(convert(value.doubleValue()), context);
    }

    /**
     * Compares this converter with the specified object.
     */
    @Override
    public final boolean equals(final Object object) {
        return object != null && object.getClass() == getClass() &&
                ((SexagesimalConverter) object).divider == divider;
    }

    /**
     * Returns a hash value for this converter.
     */
    @Override
    public final int hashCode() {
        return ((int) divider) ^ getClass().hashCode();
    }

    /**
     * The inverse of {@link SexagesimalConverter}.
     */
    private static final class Inverse extends SexagesimalConverter {
        /**
         * Serial number for compatibility with different versions.
         */
        private static final long serialVersionUID = -1928146841653975281L;

        /**
         * Constructs a converter.
         */
        public Inverse(final SexagesimalConverter inverse) {
            super(inverse);
        }

        /**
         * Performs a conversion from sexagesimal degrees to fractional degrees.
         *
         * @throws IllegalArgumentException If the given angle can not be converted.
         */
        @Override
        public double convert(final double angle) throws IllegalArgumentException {
            double deg,min,sec;
            if (hasSeconds) {
                sec = angle * divider;
                deg = truncate(sec/10000); sec -= 10000*deg;
                min = truncate(sec/  100); sec -=   100*min;
            } else {
                sec = 0;
                min = angle * divider;
                deg = truncate(min / 100);
                min -= deg * 100;
            }
            if (min <= -60 || min >= 60) {  // Do not enter for NaN
                if (Math.abs(Math.abs(min) - 100) <= (EPS * 100)) {
                    if (min >= 0) deg++; else deg--;
                    min = 0;
                } else {
                    throw illegalField(angle, min, Vocabulary.Keys.AngularMinutes);
                }
            }
            if (sec <= -60 || sec >= 60) { // Do not enter for NaN
                if (Math.abs(Math.abs(sec) - 100) <= (EPS * 100)) {
                    if (sec >= 0) min++; else min--;
                    sec = 0;
                } else {
                    throw illegalField(angle, sec, Vocabulary.Keys.AngularSeconds);
                }
            }
            return (sec/60 + min)/60 + deg;
        }

        /**
         * Creates an exception for an illegal field.
         *
         * @param  value The user-supplied angle value.
         * @param  field The value of the illegal field.
         * @param  unit  The vocabulary key for the field (minutes or seconds).
         * @return The exception to throw.
         */
        private static IllegalArgumentException illegalField(final double value, final double field, final short unit) {
            return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentField_4,
                    "angle", value, Vocabulary.format(unit), field));
        }
    }
}
