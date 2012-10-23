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
import java.text.Format;
import java.text.ParseException;
import java.io.Serializable;
import net.jcip.annotations.Immutable;

import org.apache.sis.util.Utilities;


/**
 * An angle in decimal degrees. An angle is the amount of rotation needed to bring one line or
 * plane into coincidence with another, generally measured in degrees, sexagesimal degrees or
 * grads.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 *
 * @see Latitude
 * @see Longitude
 * @see AngleFormat
 */
@Immutable
public class Angle implements Comparable<Angle>, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1158747349433104534L;

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
     * locale. Developers should consider using {@link AngleFormat} for end-user applications
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
             * is formatted in the AngleFormat locale, which is hard-coded to Locale.CANADA in
             * our 'getAngleFormat()' implementation. The getMessage() method uses the system
             * locale, which is what we actually want.
             */
            NumberFormatException e = new NumberFormatException(exception.getMessage());
            e.initCause(exception);
            throw e;
        }
        if (getClass().isAssignableFrom(angle.getClass())) {
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
            return Utilities.equals(θ, ((Angle) object).θ);
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
     * Returns a string representation of this {@code Angle} object.
     * This is a convenience method mostly for debugging purpose, since it uses a fixed locale.
     * Developers should consider using {@link AngleFormat} for end-user applications instead
     * than this method.
     *
     * @see AngleFormat#format(double)
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(16);
        synchronized (Angle.class) {
            buffer = getAngleFormat().format(this, buffer, null);
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
            format = AngleFormat.getInstance(Locale.CANADA);
            // Canada locale is closer to ISO standards than US locale.
        }
        return format;
    }
}
