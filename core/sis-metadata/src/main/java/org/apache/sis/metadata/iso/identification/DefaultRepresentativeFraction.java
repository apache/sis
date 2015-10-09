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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.apache.sis.metadata.UnmodifiableMetadataException;
import org.apache.sis.internal.jaxb.ModifiableIdentifierMap;
import org.apache.sis.internal.jaxb.IdentifierMapAdapter;
import org.apache.sis.internal.jaxb.gco.GO_Integer64;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.util.CheckedArrayList;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;


/**
 * A scale defined as the inverse of a denominator.
 * Scale is defined as a kind of {@link Number}.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #setScale(double)} for computing the denominator from a scale value.</li>
 * </ul>
 *
 * <div class="section">Limitations</div>
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
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see DefaultResolution#getEquivalentScale()
 */
@XmlType(name = "MD_RepresentativeFraction_Type")
@XmlRootElement(name = "MD_RepresentativeFraction")
public class DefaultRepresentativeFraction extends Number implements RepresentativeFraction, IdentifiedObject, Emptiable, Cloneable {
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
     * {@code true} if this representative fraction has been made unmodifiable.
     */
    private transient boolean isUnmodifiable;

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
     * @throws IllegalArgumentException If the given value is negative.
     */
    public DefaultRepresentativeFraction(final long denominator) {
        ArgumentChecks.ensurePositive("denominator", denominator);
        this.denominator = denominator;
    }

    /**
     * Constructs a new representative fraction initialized to the value of the given object.
     *
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public DefaultRepresentativeFraction(final RepresentativeFraction object) {
        if (object != null) {
            denominator = object.getDenominator();
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
    @ValueRange(minimum = 0)
    @XmlJavaTypeAdapter(value = GO_Integer64.class, type = long.class)
    @XmlElement(name = "denominator", required = true)
    public long getDenominator() {
        return denominator;
    }

    /**
     * Sets the denominator value.
     *
     * @param  denominator The new denominator value, or 0 if none.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setDenominator(final long denominator) {
        if (isUnmodifiable) {
            throw new UnmodifiableMetadataException(Errors.format(Errors.Keys.UnmodifiableMetadata));
        }
        if (ensurePositive(DefaultRepresentativeFraction.class, "denominator", false, denominator)) {
            this.denominator = denominator;
        }
    }

    /**
     * Sets the denominator from a scale in the (0 … 1] range.
     * The denominator is computed by {@code round(1 / scale)}.
     *
     * <p>The equivalent of a {@code getScale()} method is {@link #doubleValue()}.</p>
     *
     * @param  scale The scale as a number between 0 exclusive and 1 inclusive, or NaN.
     * @throws IllegalArgumentException if the given scale is our of range.
     */
    public void setScale(final double scale) {
        if (isUnmodifiable) {
            throw new UnmodifiableMetadataException(Errors.format(Errors.Keys.UnmodifiableMetadata));
        }
        /*
         * For the following argument check, we do not need to use a Metadatautility method because
         * 'setScale' is never invoked at (un)marshalling time. Note also that we accept NaN values
         * since round(NaN) == 0, which is the desired value.
         */
        if (scale <= 0 || scale > 1) {
            throw new IllegalArgumentException((scale <= 0)
                    ? Errors.format(Errors.Keys.ValueNotGreaterThanZero_2, "scale", scale)
                    : Errors.format(Errors.Keys.ValueOutOfRange_4, "scale", 0, 1, scale));
        }
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
     * Returns {@code true} if no scale is defined.
     * The following relationship shall hold:
     *
     * {@preformat java
     *   assert isEmpty() == Double.isNaN(doubleValue());
     * }
     *
     * @return {@code true} if no scale is defined.
     *
     * @see #doubleValue()
     * @see #floatValue()
     *
     * @since 0.6
     */
    @Override
    public boolean isEmpty() {
        return (denominator == 0);
    }

    /**
     * Makes this representative fraction unmodifiable. After invocation to this method,
     * any call to a setter method will throw an {@link UnmodifiableMetadataException}.
     *
     * @since 0.7
     *
     * @see org.apache.sis.metadata.ModifiableMetadata#freeze()
     */
    public void freeze() {
        isUnmodifiable = true;
    }

    /**
     * Returns a modifiable copy of this representative fraction.
     *
     * @return A modifiable copy of this representative fraction.
     */
    @Override
    public DefaultRepresentativeFraction clone() {
        final DefaultRepresentativeFraction c;
        try {
            c = (DefaultRepresentativeFraction) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);    // Should never happen since we are cloneable.
        }
        c.isUnmodifiable = false;
        return c;
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

    /**
     * Returns a string representation of this scale, or {@code NaN} if undefined.
     * If defined, the string representation uses the colon as in "1:20000".
     *
     * @return A string representation of this scale.
     */
    @Override
    public String toString() {
        return (denominator != 0) ? "1:" + denominator : "NaN";
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
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<Identifier> getIdentifiers() {
        if (identifiers == null) {
            identifiers = new CheckedArrayList<Identifier>(Identifier.class);
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
        final Collection<Identifier> identifiers = getIdentifiers();
        return isUnmodifiable ? new IdentifierMapAdapter(identifiers)
                              : new ModifiableIdentifierMap(identifiers);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Invoked by JAXB for fetching the unique identifier unique for the XML document.
     *
     * @see org.apache.sis.metadata.iso.ISOMetadata#getID()
     */
    @XmlID
    @XmlAttribute  // Defined in "gco" as unqualified attribute.
    @XmlSchemaType(name = "ID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getID() {
        return isNullOrEmpty(identifiers) ? null : MetadataUtilities.getObjectID(this);
    }

    /**
     * Invoked by JAXB for specifying the unique identifier.
     *
     * @see org.apache.sis.metadata.iso.ISOMetadata#setID(String)
     */
    private void setID(String id) {
        MetadataUtilities.setObjectID(this, id);
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
