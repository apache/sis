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
package org.apache.sis.metadata.iso.identification;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A scale defined as the inverse of a denominator.
 * Scale is defined as a kind of {@link Number}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_RepresentativeFraction_Type")
@XmlRootElement(name = "MD_RepresentativeFraction")
public class DefaultRepresentativeFraction extends Number implements RepresentativeFraction {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -715235893904309869L;

    /**
     * The number below the line in a vulgar fraction, or 0 if undefined.
     */
    private long denominator;

    /**
     * Creates a uninitialized representative fraction.
     * The {@linkplain #getDenominator() denominator} is initially zero
     * and the {@linkplain #doubleValue() double value} is NaN.
     */
    public DefaultRepresentativeFraction() {
    }

    /**
     * Creates a new representative fraction from the specified denominator.
     *
     * @param  denominator The denominator as a positive number, or 0 if unspecified.
     * @throws IllegalArgumentException If the given value is not a positive number or zero.
     */
    public DefaultRepresentativeFraction(final long denominator) throws IllegalArgumentException {
        ArgumentChecks.ensurePositive("denominator", denominator);
        this.denominator = denominator;
    }

    /**
     * Constructs a new representative fraction initialized to the value of the given object.
     *
     * @param  source The representative fraction to copy, or {@code null} if none.
     * @throws IllegalArgumentException If the given source is non-null and its denominator
     *         is not a positive number or zero.
     */
    public DefaultRepresentativeFraction(final RepresentativeFraction source) throws IllegalArgumentException {
        if (source != null) {
            denominator = source.getDenominator();
            ArgumentChecks.ensurePositive("source", denominator);
        }
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRepresentativeFraction castOrCopy(final RepresentativeFraction object) {
        return (object == null) || (object instanceof DefaultRepresentativeFraction)
                ? (DefaultRepresentativeFraction) object : new DefaultRepresentativeFraction(object);
    }

    /**
     * Returns the denominator of this representative fraction.
     */
    @Override
    @XmlElement(name = "denominator", required = true)
    public long getDenominator() {
        return denominator;
    }

    /**
     * Sets the denominator value.
     *
     * @param  denominator The new denominator value, or 0 if none.
     * @throws IllegalArgumentException If the given value is not a positive number or zero.
     */
    public void setDenominator(final long denominator) throws IllegalArgumentException {
        ArgumentChecks.ensurePositive("denominator", denominator);
        this.denominator = denominator;
    }

    /**
     * Sets the denominator from a scale in the [-1 … +1] range.
     * The denominator is computed by {@code round(1 / scale)}.
     *
     * @param  scale The scale as a number between -1 and +1 inclusive, or NaN.
     * @throws IllegalArgumentException if the given scale is our of range.
     */
    public void setScale(final double scale) throws IllegalArgumentException {
        if (Math.abs(scale) > 1) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueOutOfRange_4, "scale", -1, +1, scale));
        }
        // round(NaN) == 0, which is the desired value.
        setDenominator(Math.round(1.0 / scale));
    }

    /**
     * Returns the scale value of this representative fraction.
     * This method is the converse of {@link #setScale(double)}.
     *
     * @return The scale value of this representative fraction, or NaN if none.
     */
    @Override
    public double doubleValue() {
        return (denominator != 0) ? (1.0 / (double) denominator) : Double.NaN;
    }

    /**
     * Returns the scale as a {@code float} type.
     */
    @Override
    public float floatValue() {
        return (denominator != 0) ? (1.0f / (float) denominator) : Float.NaN;
    }

    /**
     * Returns 1 if the {@linkplain #getDenominator() denominator} is equals to 1, or 0 otherwise.
     *
     * {@note This method is defined that way because scales smaller than 1 can
     *        only be casted to 0, and NaN values are also represented by 0.}
     */
    @Override
    public long longValue() {
        return (denominator == 1) ? 1 : 0;
    }

    /**
     * Returns 1 if the {@linkplain #getDenominator() denominator} is equals to 1, or 0 otherwise.
     *
     * {@note This method is defined that way because scales smaller than 1 can
     *        only be casted to 0, and NaN values are also represented by 0.}
     */
    @Override
    public int intValue() {
        return (denominator == 1) ? 1 : 0;
    }

    /**
     * Compares this object with the specified value for equality.
     *
     * @param object The object to compare with.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(Object object) {
        /*
         * Note: 'equals(Object)' and 'hashCode()' implementations are defined in the interface,
         * in order to ensure that the following requirements hold:
         *
         * - a.equals(b) == b.equals(a)   (reflexivity)
         * - a.equals(b) implies (a.hashCode() == b.hashCode())
         */
        if (object instanceof RepresentativeFraction) {
            return ((RepresentativeFraction) object).getDenominator() == denominator;
        }
        return false;
    }

    /**
     * Returns a hash value for this representative fraction.
     */
    @Override
    public int hashCode() {
        return (int) denominator;
    }
}
