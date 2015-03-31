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

import java.util.Locale;
import java.util.Formatter;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.text.Format;
import java.text.ParseException;
import java.io.Serializable;
import org.apache.sis.internal.util.Utilities;

import static java.lang.Double.doubleToLongBits;
import static org.apache.sis.math.MathFunctions.isNegative;


/**
 * An angle in decimal degrees. An angle is the amount of rotation needed to bring one line or plane
 * into coincidence with another. Various kind of angles are used in geographic information systems,
 * some of them having a specialized class in Apache SIS:
 *
 * <ul>
 *   <li>{@linkplain Latitude} is an angle ranging from 0° at the equator to 90° at the poles.</li>
 *   <li>{@linkplain Longitude} is an angle measured east-west from a prime meridian (usually Greenwich, but not necessarily).</li>
 *   <li><cite>Azimuth</cite> is a direction given by an angle between 0° and 360° measured clockwise from North.</li>
 *   <li><cite>Bearing</cite> is a direction given by an angle between 0° and 90° in a quadrant defined by a cardinal direction.</li>
 *   <li><cite>Bearing</cite> is also sometime used in navigation for an angle relative to the vessel forward direction.</li>
 *   <li><cite>Deflection angle</cite> is the angle between a line and the prolongation of a preceding line.</li>
 *   <li><cite>Interior angle</cite> is an angle measured between two lines of sight.</li>
 *   <li>{@linkplain ElevationAngle Elevation angle} is the angular height from the horizontal plane to an object above the horizon.</li>
 * </ul>
 *
 * <div class="section">Formatting angles</div>
 * The recommended way to format angles is to instantiate an {@link AngleFormat} once, then to
 * reuse it many times. As a convenience, {@code Angle} objects can also be formatted by the
 * {@code "%s"} conversion specifier of {@link Formatter}, but this is less efficient for this
 * class.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class and the {@link Latitude} / {@link Longitude} subclasses are immutable, and thus
 * inherently thread-safe. Other subclasses may or may not be immutable, at implementation choice
 * (see {@link java.lang.Number} for an example of a similar in purpose class having mutable subclasses).
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see Latitude
 * @see Longitude
 * @see AngleFormat
 */
public class Angle implements Comparable<Angle>, Formattable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3701568577051191744L;

    /**
     * A shared instance of {@link AngleFormat}.
     *
     * @see #getAngleFormat()
     */
    private static Format format;

    /**
     * Angle value in decimal degrees. We use decimal degrees as the storage unit
     * instead than radians in order to avoid rounding errors, since there is no
     * way to represent 30°, 45°, 90°, 180°, <i>etc.</i> in radians without errors.
     */
    private final double θ;

    /**
     * Constructs a new angle with the specified value in decimal degrees.
     *
     * @param θ Angle in decimal degrees.
     */
    public Angle(final double θ) {
        this.θ = θ;
    }

    /**
     * Constructs a newly allocated {@code Angle} object that contain the angular value
     * represented by the string. The string should represent an angle in either fractional
     * degrees (e.g. 45.5°) or degrees with minutes and seconds (e.g. 45°30').
     *
     * <p>This is a convenience constructor mostly for testing purpose, since it uses a fixed
     * locale. Developers should consider using {@link AngleFormat} for end-user applications
     * instead than this constructor.</p>
     *
     * @param  string A string to be converted to an {@code Angle}.
     * @throws NumberFormatException if the string does not contain a parsable angle.
     *
     * @see AngleFormat#parse(String)
     */
    public Angle(final String string) throws NumberFormatException {
        final Object angle;
        try {
            synchronized (Angle.class) {
                angle = getAngleFormat().parseObject(string);
            }
        } catch (ParseException exception) {
            /*
             * Use Exception.getMessage() instead than getLocalizedMessage() because the later
             * is formatted in the AngleFormat locale, which is hard-coded to Locale.ROOT in our
             * 'getAngleFormat()' implementation. The getMessage() method uses the system locale,
             * which is what we actually want.
             */
            NumberFormatException e = new NumberFormatException(exception.getMessage());
            e.initCause(exception);
            throw e;
        }
        final Class<?> type = angle.getClass();
        if (type == Angle.class || getClass().isAssignableFrom(type)) {
            this.θ = ((Angle) angle).θ;
        } else {
            throw new NumberFormatException(string);
        }
    }

    /**
     * Returns the angle value in decimal degrees.
     *
     * @return The angle value in decimal degrees.
     */
    public double degrees() {
        return θ;
    }

    /**
     * Returns the angle value in radians.
     *
     * @return The angle value in radians.
     */
    public double radians() {
        return Math.toRadians(θ);
    }

    /**
     * Returns a hash code for this {@code Angle} object.
     */
    @Override
    public int hashCode() {
        final long code = Double.doubleToLongBits(θ);
        return (int) code ^ (int) (code >>> 32) ^ (int) serialVersionUID;
    }

    /**
     * Compares the specified object with this angle for equality.
     *
     * @param object The object to compare with this angle for equality.
     * @return {@code true} if the given object is equal to this angle.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && getClass() == object.getClass()) {
            return doubleToLongBits(θ) == doubleToLongBits(((Angle) object).θ);
        }
        return false;
    }

    /**
     * Compares two {@code Angle} objects numerically. The comparison
     * is done as if by the {@link Double#compare(double, double)} method.
     *
     * @param that The angle to compare with this object for order.
     * @return -1 if this angle is smaller than the given one, +1 if greater or 0 if equals.
     */
    @Override
    public int compareTo(final Angle that) {
        return Double.compare(this.θ, that.θ);
    }

    /**
     * Upper threshold before to format an angle as an ordinary number.
     * This is set to 90° in the case of latitude numbers.
     */
    double maximum() {
        return 360;
    }

    /**
     * Returns the hemisphere character for an angle of the given sign.
     * This is used only by {@link #toString()}, not by {@link AngleFormat}.
     */
    char hemisphere(final boolean negative) {
        return 0;
    }

    /**
     * Returns a string representation of this {@code Angle} object.
     * This is a convenience method mostly for debugging purpose, since it uses a fixed locale.
     * Developers should consider using {@link AngleFormat} for end-user applications instead
     * than this method.
     *
     * @see AngleFormat#format(double)
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        double m = Math.abs(θ);
        final boolean isSmall = m <= (1 / 3600E+3); // 1E-3 arc-second.
        if (isSmall || m > maximum()) {
            final char h = hemisphere(isNegative(θ));
            if (h == 0) {
                m = θ;  // Restore the sign.
            }
            char symbol = '°';
            if (isSmall) {
                symbol = '″';
                m *= 3600;
            }
            buffer.append(m).append(symbol);
            if (h != 0) {
                buffer.append(h);
            }
        } else {
            synchronized (Angle.class) {
                buffer = getAngleFormat().format(this, buffer, null);
            }
        }
        return buffer.toString();
    }

    /**
     * Returns a shared instance of {@link AngleFormat}. The return type is
     * {@link Format} in order to avoid class loading before necessary.
     *
     * <p>This method must be invoked in a {@code synchronized(Angle.class)} block. We use
     * synchronization instead than static class initialization because {@code AngleFormat}
     * is not thread-safe, so it needs to be used in a synchronized block anyway. We could
     * avoid synchronization by using {@link ThreadLocal}, but this brings other issues in
     * OSGi context. Given that our Javadoc said that {@link #Angle(String)} and {@link #toString()}
     * should be used mostly for debugging purpose, we consider not worth to ensure high
     * concurrency capability here.</p>
     */
    private static Format getAngleFormat() {
        assert Thread.holdsLock(Angle.class);
        if (format == null) {
            format = new AngleFormat(Locale.ROOT);
        }
        return format;
    }

    /**
     * Formats this angle using the provider formatter. This method is invoked when an
     * {@code Angle} object is formatted using the {@code "%s"} conversion specifier of
     * {@link Formatter}. Users don't need to invoke this method explicitely.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If the precision is 0, then this method formats an empty string.</li>
     *   <li>If the precision is 1 and this angle is a {@link Latitude} or {@link Longitude},
     *       then this method formats only the hemisphere symbol.</li>
     *   <li>Otherwise the precision, if positive, is given to {@link AngleFormat#setMaximumWidth(int)}.</li>
     * </ul>
     *
     * @param formatter The formatter in which to format this angle.
     * @param flags     {@link FormattableFlags#LEFT_JUSTIFY} for left alignment, or 0 for right alignment.
     * @param width     Minimal number of characters to write, padding with {@code ' '} if necessary.
     * @param precision Maximal number of characters to write, or -1 if no limit.
     */
    @Override
    public void formatTo(final Formatter formatter, final int flags, final int width, final int precision) {
        final String value;
        if (precision == 0) {
            value = "";
        } else {
            final char h;
            int w = precision; // To be decremented only if we may truncate and an hemisphere symbol exist.
            if (w > 0 && (h = hemisphere(isNegative(θ))) != 0 && --w == 0) {
                value = Character.toString(h);
            } else {
                final AngleFormat format = new AngleFormat(formatter.locale());
                if (w > 0) {
                    format.setMaximumWidth(w);
                }
                value = format.format(this, new StringBuffer(), null).toString();
            }
        }
        Utilities.formatTo(formatter, flags, width, precision, value);
    }
}
