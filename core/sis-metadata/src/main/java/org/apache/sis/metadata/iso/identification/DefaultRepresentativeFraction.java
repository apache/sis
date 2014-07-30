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

import java.util.Collection;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.apache.sis.internal.jaxb.IdentifierMapWithSpecialCases;
import org.apache.sis.internal.jaxb.gco.GO_Integer64;
import org.apache.sis.internal.util.CheckedArrayList;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


/**
 * A scale defined as the inverse of a denominator.
 * Scale is defined as a kind of {@link Number}.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #setScale(double)} for computing the denominator from a scale value.</li>
 * </ul>
 *
 * {@section Limitations}
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.4
 * @module
 */
@XmlType(name = "MD_RepresentativeFraction_Type")
@XmlRootElement(name = "MD_RepresentativeFraction")
public class DefaultRepresentativeFraction extends Number implements RepresentativeFraction, IdentifiedObject {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -6043871487256529207L;

    /**
     * The number below the line in a vulgar fraction, or 0 if undefined.
     */
    private long denominator;

    /**
     * All identifiers associated with this metadata, or {@code null} if none.
     * This field is initialized to a non-null value when first needed.
     */
    private Collection<Identifier> identifiers;

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
     * @param  object The metadata to copy values from, or {@code null} if none.
     * @throws IllegalArgumentException If the denominator of the given source is negative.
     */
    public DefaultRepresentativeFraction(final RepresentativeFraction object) throws IllegalArgumentException {
        if (object != null) {
            denominator = object.getDenominator();
            ArgumentChecks.ensurePositive("object", denominator);
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
     *
     * @return The denominator.
     */
    @Override
    @XmlJavaTypeAdapter(value = GO_Integer64.class, type = long.class)
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
     *
     * @return The scale.
     */
    @Override
    public float floatValue() {
        return (denominator != 0) ? (1.0f / (float) denominator) : Float.NaN;
    }

    /**
     * Returns 1 if the {@linkplain #getDenominator() denominator} is equals to 1, or 0 otherwise.
     *
     * <div class="note"><b>Rational:</b>
     * This method is defined that way because scales smaller than 1 can
     * only be casted to 0, and NaN values are also represented by 0.</div>
     *
     * @return 1 if the denominator is 1, or 0 otherwise.
     */
    @Override
    public long longValue() {
        return (denominator == 1) ? 1 : 0;
    }

    /**
     * Returns 1 if the {@linkplain #getDenominator() denominator} is equals to 1, or 0 otherwise.
     *
     * <div class="note"><b>Rational:</b>
     * This method is defined that way because scales smaller than 1 can
     * only be casted to 0, and NaN values are also represented by 0.</div>
     *
     * @return 1 if the denominator is 1, or 0 otherwise.
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




    // --------------------------------------------------------------------------------------
    // Code below this point is basically a copy-and-paste of ISOMetadata, with some edition.
    // The JAXB attributes defined here shall be the same than the ISOMetadata ones.
    // --------------------------------------------------------------------------------------

    /**
     * Returns all identifiers associated to this object, or an empty collection if none.
     * Those identifiers are marshalled in XML as {@code id} or {@code uuid} attributes.
     */
    @Override
    public Collection<Identifier> getIdentifiers() {
        if (identifiers == null) {
            identifiers = new CheckedArrayList<>(Identifier.class);
        }
        return identifiers;
    }

    /**
     * Returns a map view of the {@linkplain #getIdentifiers() identifiers} collection as (<var>authority</var>,
     * <var>code</var>) entries. That map is <cite>live</cite>: changes in the identifiers list will be reflected
     * in the map, and conversely.
     */
    @Override
    public IdentifierMap getIdentifierMap() {
        return new IdentifierMapWithSpecialCases(getIdentifiers());
    }

    /**
     * Invoked by JAXB for fetching the unique identifier unique for the XML document.
     *
     * @see org.apache.sis.metadata.iso.ISOMetadata#getID()
     */
    @XmlID
    @XmlAttribute  // Defined in "gco" as unqualified attribute.
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getID() {
        return isNullOrEmpty(identifiers) ? null : getIdentifierMap().getSpecialized(IdentifierSpace.ID);
    }

    /**
     * Invoked by JAXB for specifying the unique identifier.
     *
     * @see org.apache.sis.metadata.iso.ISOMetadata#setID(String)
     */
    private void setID(String id) {
        id = CharSequences.trimWhitespaces(id);
        if (id != null && !id.isEmpty()) {
            getIdentifierMap().putSpecialized(IdentifierSpace.ID, id);
        }
    }

    /**
     * Invoked by JAXB for fetching the unique identifier unique "worldwide".
     *
     * @see org.apache.sis.metadata.iso.ISOMetadata#getUUID()
     */
    @XmlAttribute  // Defined in "gco" as unqualified attribute.
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getUUID() {
        return isNullOrEmpty(identifiers) ? null : getIdentifierMap().get(IdentifierSpace.UUID);
    }

    /**
     * Invoked by JAXB for specifying the unique identifier.
     *
     * @see org.apache.sis.metadata.iso.ISOMetadata#setUUID(String)
     */
    private void setUUID(final String id) {
        getIdentifierMap().put(IdentifierSpace.UUID, id);
    }
}
