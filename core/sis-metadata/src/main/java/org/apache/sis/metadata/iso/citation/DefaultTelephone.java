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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.CodeList;
import org.opengis.annotation.UML;
import org.opengis.metadata.citation.Telephone;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.geoapi.evolution.UnsupportedCodeList;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Telephone numbers for contacting the responsible individual or organization.
 *
 * <div class="section">Differences between versions 2003 and 2014 of ISO 19115</div>
 * For any contact having more than one telephone number, the way to organize the information
 * changed significantly between the two versions of ISO standard:
 *
 * <ul>
 *   <li>In ISO 19115:2003, each {@code Contact} had only one {@code Telephone} instance, but that instance
 *       could have an arbitrary amount of "voice" and "facsimile" numbers. The methods (now deprecated) were
 *       {@link DefaultContact#getPhone()}, {@link #getVoices()} and {@link #getFacsimiles()}.</li>
 *   <li>In ISO 19115:2014, each {@code Contact} has an arbitrary amount of {@code Telephone} instances, and
 *       each telephone has exactly one number. The new methods are {@link DefaultContact#getPhones()},
 *       {@link #getNumber()} and {@link #getNumberType()}.</li>
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultContact#getPhones()
 */
@XmlType(name = "CI_Telephone_Type", propOrder = {
    "voices",
    "facsimiles"
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
    CodeList<?> numberType;

    /**
     * Constructs a default telephone.
     */
    public DefaultTelephone() {
    }

    /**
     * Constructs a telephone with the given number and type.
     *
     * @param number     The telephone number, or {@code null}.
     * @param numberType The type of telephone number, or {@code null}.
     *
     * @since 0.5
     */
    DefaultTelephone(final String number, final CodeList<?> numberType) {
        this.number     = number;
        this.numberType = numberType;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Telephone)
     */
    public DefaultTelephone(final Telephone object) {
        super(object);
        if (object != null) {
            if (object instanceof DefaultTelephone) {
                number     = ((DefaultTelephone) object).getNumber();
                numberType = ((DefaultTelephone) object).numberType;
            } else {
                setVoices(object.getVoices());
                setFacsimiles(object.getFacsimiles());
            }
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
     *       {@linkplain #DefaultTelephone(Telephone) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return Telephone number by which individuals can contact responsible organization or individual.
     *
     * @since 0.5
     */
/// @XmlElement(name = "number", required = true)
    @UML(identifier="number", obligation=MANDATORY, specification=ISO_19115)
    public String getNumber() {
        return number;
    }

    /**
     * Sets the telephone number by which individuals can contact responsible organization or individual.
     *
     * @param newValue The new telephone number by which individuals can contact responsible organization or individual.
     *
     * @since 0.5
     */
    public void setNumber(final String newValue) {
        checkWritePermission();
        number = newValue;
    }

    /**
     * Returns the type of telephone number, or {@code null} if none.
     * If non-null, the type can be {@code "VOICE"}, {@code "FACSIMILE"} or {@code "SMS"}.
     *
     * <div class="warning"><b>Upcoming API change — specialization</b><br>
     * The return type will be changed to the {@code TelephoneType} code list
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Type of telephone number, or {@code null} if none.
     *
     * @since 0.5
     */
/// @XmlElement(name = "numberType")
    @UML(identifier="numberType", obligation=OPTIONAL, specification=ISO_19115)
    public CodeList<?> getNumberType() {
        return numberType;
    }

    /**
     * Sets the type of telephone number.
     * If non-null, the type can only be {@code "VOICE"}, {@code "FACSIMILE"} or {@code "SMS"}.
     *
     * <div class="warning"><b>Upcoming API change — specialization</b><br>
     * The argument type will be changed to the {@code TelephoneType} code list when GeoAPI will provide it
     * (tentatively in GeoAPI 3.1). In the meantime, users can define their own code list class as below:
     *
     * {@preformat java
     *   final class UnsupportedCodeList extends CodeList<UnsupportedCodeList> {
     *       private static final List<UnsupportedCodeList> VALUES = new ArrayList<UnsupportedCodeList>();
     *
     *       // Need to declare at least one code list element.
     *       public static final UnsupportedCodeList MY_CODE_LIST = new UnsupportedCodeList("MY_CODE_LIST");
     *
     *       private UnsupportedCodeList(String name) {
     *           super(name, VALUES);
     *       }
     *
     *       public static UnsupportedCodeList valueOf(String code) {
     *           return valueOf(UnsupportedCodeList.class, code);
     *       }
     *
     *       &#64;Override
     *       public UnsupportedCodeList[] family() {
     *           synchronized (VALUES) {
     *               return VALUES.toArray(new UnsupportedCodeList[VALUES.size()]);
     *           }
     *       }
     *   }
     * }
     * </div>
     *
     * @param newValue The new type of telephone number.
     *
     * @since 0.5
     */
    public void setNumberType(final CodeList<?> newValue) {
        checkWritePermission();
        numberType = newValue;
    }

    /**
     * For implementation of {@link #getVoices()} and {@link #getFacsimiles()} deprecated methods.
     * Shall be the telephones list of the enclosing {@link DefaultContact} object.
     *
     * <p>This field references the same collection than {@link DefaultContact#phones} when possible.
     * Note that the link between this collection and {@code DefaultContact.phones} is broken when
     * {@link DefaultContact#freeze()} is invoked, since the {@code Cloner.clone(Object)} method
     * creates a new (unmodifiable) collection.</p>
     *
     * @deprecated This field will be removed after we removed the deprecated public methods.
     */
    @Deprecated
    private Collection<Telephone> owner;

    /**
     * Specifies the collection which contains this telephone number.
     * This method is invoked by {@link DefaultContact#setPhones(Collection)}
     * when the contact "takes possession" of a {@code DefaultTelephone}.
     *
     * <p>This method will be removed after we removed the deprecated public methods.</p>
     *
     * @param  phones The collection which should contains this telephone number.
     * @return {@code this}, or a copy of this instance if we conservatively choose to not modify this instance.
     */
    final DefaultTelephone setOwner(final Collection<Telephone> phones) {
        if (owner != phones) {
            if (owner != null && !CollectionsExt.identityEquals(owner.iterator(), phones.iterator())) {
                final DefaultTelephone copy = new DefaultTelephone(this);
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
           if (isModifiable()) {
               owner = new ArrayList<Telephone>(4);
               owner.add(this);
           } else {
               owner = Collections.<Telephone>singletonList(this);
           }
       }
       return owner;
    }

    /**
     * Returns the telephone numbers by which individuals can speak to the responsible organization or individual.
     * This method searches in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact that own
     * this phone is known.
     *
     * @return Telephone numbers by which individuals can speak to the responsible organization or individual.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #getNumber() number}
     *             with {@link TelephoneType#VOICE}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "voice")
    public final Collection<String> getVoices() {
        return new LegacyTelephones(getOwner(), UnsupportedCodeList.VOICE);
    }

    /**
     * Sets the telephone numbers by which individuals can speak to the responsible organization or individual.
     * This method writes in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact that own
     * this phone is known.
     *
     * @param newValues The new telephone numbers, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #setNumber(String) number}
     *             with {@link TelephoneType#VOICE}.
     */
    @Deprecated
    public void setVoices(final Collection<? extends String> newValues) {
        ((LegacyTelephones) getVoices()).setValues(newValues);
    }

    /**
     * Returns the telephone numbers of a facsimile machine for the responsible organization or individual.
     * This method searches in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact
     * that own this phone is known.
     *
     * @return Telephone numbers of a facsimile machine for the responsible organization or individual.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #getNumber() number}
     *             with {@link TelephoneType#FACSIMILE}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "facsimile")
    public final Collection<String> getFacsimiles() {
        return new LegacyTelephones(getOwner(), UnsupportedCodeList.FACSIMILE);
    }

    /**
     * Sets the telephone number of a facsimile machine for the responsible organization or individual.
     * This method writes in the {@linkplain DefaultContact#getPhones() contact phones}, if the contact
     * that own this phone is known.
     *
     * @param newValues The new telephone number, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by a {@linkplain #setNumber(String) number}
     *             with {@link TelephoneType#FACSIMILE}.
     */
    @Deprecated
    public void setFacsimiles(final Collection<? extends String> newValues) {
        ((LegacyTelephones) getFacsimiles()).setValues(newValues);
    }
}
