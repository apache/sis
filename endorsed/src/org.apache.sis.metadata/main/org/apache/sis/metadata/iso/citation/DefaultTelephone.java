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
package org.apache.sis.metadata.iso.citation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.citation.Telephone;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.gco.StringAdapter;
import org.apache.sis.xml.bind.metadata.code.CI_TelephoneTypeCode;
import org.apache.sis.metadata.internal.Dependencies;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.TelephoneType;


/**
 * Telephone numbers for contacting the responsible individual or organization.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code CI_Telephone}
 * {@code   └─number……} Telephone number by which individuals can contact responsible organisation or individual.</div>
 *
 * <h2>Differences between versions 2003 and 2014 of ISO 19115</h2>
 * For any contact having more than one telephone number, the way to organize the information
 * changed significantly between the two versions of ISO standard:
 *
 * <ul>
 *   <li>In ISO 19115:2003, each {@code Contact} had only one {@code Telephone} instance, but that instance
 *       could have an arbitrary number of "voice" and "facsimile" numbers. The methods (now deprecated) were
 *       {@link DefaultContact#getPhone()}, {@link #getVoices()} and {@link #getFacsimiles()}.</li>
 *   <li>In ISO 19115:2014, each {@code Contact} has an arbitrary number of {@code Telephone} instances, and
 *       each telephone has exactly one number. The new methods are {@link DefaultContact#getPhones()},
 *       {@link #getNumber()} and {@link #getNumberType()}.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 *
 * @see DefaultContact#getPhones()
 *
 * @since 0.5
 */
@XmlType(name = "CI_Telephone_Type", propOrder = {
    "number",           // New in ISO 19115:2014
    "numberType",       // Ibid.
    "voices",           // Legacy ISO 19115:2003
    "facsimiles"        // Ibid.
})
@XmlRootElement(name = "CI_Telephone")
public class DefaultTelephone extends ISOMetadata implements Telephone {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5156405432420742237L;

    /**
     * Telephone number by which individuals can contact responsible organization or individual.
     */
    private String number;

    /**
     * Type of telephone number.
     */
    private TelephoneType numberType;

    /**
     * Constructs a default telephone.
     */
    public DefaultTelephone() {
    }

    /**
     * Constructs a telephone with the given number and type.
     *
     * @param number      the telephone number, or {@code null}.
     * @param numberType  the type of telephone number, or {@code null}.
     *
     * @since 0.5
     */
    public DefaultTelephone(final String number, final TelephoneType numberType) {
        this.number     = number;
        this.numberType = numberType;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Telephone)
     */
    public DefaultTelephone(final Telephone object) {
        super(object);
        if (object != null) {
            number     = object.getNumber();
            numberType = object.getNumberType();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultTelephone}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultTelephone} instance is created using the
     *       {@linkplain #DefaultTelephone(Telephone) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultTelephone castOrCopy(final Telephone object) {
        if (object == null || object instanceof DefaultTelephone) {
            return (DefaultTelephone) object;
        }
        return new DefaultTelephone(object);
    }

    /**
     * Returns the telephone number by which individuals can contact responsible organization or individual.
     *
     * @return telephone number by which individuals can contact responsible organization or individual.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "number", required = true)
    @XmlJavaTypeAdapter(StringAdapter.Since2014.class)
    public String getNumber() {
        return number;
    }

    /**
     * Sets the telephone number by which individuals can contact responsible organization or individual.
     *
     * @param  newValue  the new telephone number by which individuals can contact responsible organization or individual.
     *
     * @since 0.5
     */
    public void setNumber(final String newValue) {
        checkWritePermission(number);
        number = newValue;
    }

    /**
     * Returns the type of telephone number, or {@code null} if none.
     *
     * @return type of telephone number, or {@code null} if none.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "numberType")
    @XmlJavaTypeAdapter(CI_TelephoneTypeCode.Since2014.class)
    public TelephoneType getNumberType() {
        return numberType;
    }

    /**
     * Sets the type of telephone number.
     *
     * @param  newValue  the new type of telephone number.
     *
     * @since 0.5
     */
    public void setNumberType(final TelephoneType newValue) {
        checkWritePermission(numberType);
        numberType = newValue;
    }

    /**
     * For implementation of {@link #getVoices()} and {@link #getFacsimiles()} deprecated methods.
     * Shall be the telephones list of the enclosing {@link DefaultContact} object.
     *
     * <p>This field references the same collection as {@link DefaultContact#phones} when possible.
     * Note that the link between this collection and {@code DefaultContact.phones} is broken when
     * {@link DefaultContact} is copied by {@link org.apache.sis.metadata.MetadataCopier}, since the
     * {@code Cloner.clone(Object)} method creates a new (unmodifiable) collection.</p>
     *
     * @deprecated This field will be removed after we removed the deprecated public methods.
     */
    @Deprecated(since="1.0")
    @SuppressWarnings("serial")
    private Collection<Telephone> owner;

    /**
     * Specifies the collection which contains this telephone number.
     * This method is invoked by {@link DefaultContact#setPhones(Collection)}
     * when the contact "takes possession" of a {@code DefaultTelephone}.
     *
     * <p>This method will be removed after we removed the deprecated public methods.</p>
     *
     * @param  phones  the collection which should contain this telephone number.
     * @return {@code this}, or a copy of this instance if we conservatively choose to not modify this instance.
     */
    final DefaultTelephone setOwner(final Collection<Telephone> phones) {
        if (owner != phones) {
            if (owner != null && !CollectionsExt.identityEquals(owner.iterator(), phones.iterator())) {
                final var copy = new DefaultTelephone(this);
                copy.owner = phones;
                return copy;
            }
            owner = phones;
        }
        return this;
    }

    /**
     * Returns the collection that own this telephone number, or create a new collection.
     * Creating a new collection is needed when this phone number has not yet been given
     * to a {@link DefaultContact}.
     *
     * <p>This method will be removed after we removed the deprecated public methods.</p>
     */
    final Collection<Telephone> getOwner() {
       if (owner == null) {
           if (super.state() != State.FINAL) {
               owner = new ArrayList<>(4);
               owner.add(this);
           } else {
               owner = Collections.singletonList(this);
           }
       }
       return owner;
    }

    /**
     * Returns the telephone numbers by which individuals can speak to the responsible organization or individual.
     * This method searches in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact that own
     * this phone is known.
     *
     * @return telephone numbers by which individuals can speak to the responsible organization or individual.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #getNumber() number}
     *             with {@link TelephoneType#VOICE}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies({"getNumber", "getNumberType"})
    @XmlElement(name = "voice", namespace = LegacyNamespaces.GMD)
    public final Collection<String> getVoices() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return new LegacyTelephones(getOwner(), TelephoneType.VOICE);
        }
        return null;                    // Marshalling newer ISO 19115-3 document.
    }

    /**
     * Sets the telephone numbers by which individuals can speak to the responsible organization or individual.
     * This method writes in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact that own
     * this phone is known.
     *
     * @param  newValues  the new telephone numbers, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #setNumber(String) number}
     *             with {@link TelephoneType#VOICE}.
     */
    @Deprecated(since="1.0")
    public void setVoices(final Collection<? extends String> newValues) {
        ((LegacyTelephones) getVoices()).setValues(newValues);
    }

    /**
     * Returns the telephone numbers of a facsimile machine for the responsible organization or individual.
     * This method searches in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact
     * that own this phone is known.
     *
     * @return telephone numbers of a facsimile machine for the responsible organization or individual.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #getNumber() number}
     *             with {@link TelephoneType#FACSIMILE}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies({"getNumber", "getNumberType"})
    @XmlElement(name = "facsimile", namespace = LegacyNamespaces.GMD)
    public final Collection<String> getFacsimiles() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return new LegacyTelephones(getOwner(), TelephoneType.FACSIMILE);
        }
        return null;                    // Marshalling newer ISO 19115-3 document.
    }

    /**
     * Sets the telephone number of a facsimile machine for the responsible organization or individual.
     * This method writes in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact
     * that own this phone is known.
     *
     * @param  newValues  the new telephone number, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #setNumber(String) number}
     *             with {@link TelephoneType#FACSIMILE}.
     */
    @Deprecated(since="1.0")
    public void setFacsimiles(final Collection<? extends String> newValues) {
        ((LegacyTelephones) getFacsimiles()).setValues(newValues);
    }
}
