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
package org.apache.sis.metadata.iso.content;

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.MemberName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.content.RangeDimension;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.gco.InternationalStringAdapter;

// Specific to the main branch:
import org.opengis.metadata.content.Band;
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information on the range of each dimension of a cell measurement value.
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
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@TitleProperty(name = "sequenceIdentifier")
@XmlType(name = "MD_RangeDimension_Type", propOrder = {
    "sequenceIdentifier",
    "description",          // New in ISO 19115:2014
    "descriptor",           // Legacy ISO 19115:2003
    "name"                  // New in ISO 19115:2014
})
@XmlRootElement(name = "MD_RangeDimension")
@XmlSeeAlso(DefaultBand.class)
public class DefaultRangeDimension extends ISOMetadata implements RangeDimension {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4517148689016920767L;

    /**
     * Unique name or number that identifies attributes included in the coverage.
     */
    @SuppressWarnings("serial")
    private MemberName sequenceIdentifier;

    /**
     * Description of the attribute.
     */
    @SuppressWarnings("serial")
    private InternationalString description;

    /**
     * Identifiers for each attribute included in the resource. These identifiers
     * can be use to provide names for the attribute from a standard set of names.
     */
    @SuppressWarnings("serial")
    private Collection<Identifier> names;

    /**
     * Constructs an initially empty range dimension.
     */
    public DefaultRangeDimension() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(RangeDimension)
     */
    public DefaultRangeDimension(final RangeDimension object) {
        super(object);
        if (object != null) {
            sequenceIdentifier = object.getSequenceIdentifier();
            description        = object.getDescriptor();
            if (object instanceof DefaultRangeDimension) {
                names = copyCollection(((DefaultRangeDimension) object).getNames(), Identifier.class);
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@code SampleDimension}, then this method
     *       delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultRangeDimension}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRangeDimension} instance is created using the
     *       {@linkplain #DefaultRangeDimension(RangeDimension) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRangeDimension castOrCopy(final RangeDimension object) {
        if (object instanceof Band) {
            return DefaultBand.castOrCopy((Band) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultRangeDimension) {
            return (DefaultRangeDimension) object;
        }
        return new DefaultRangeDimension(object);
    }

    /**
     * Returns a unique name or number that identifies attributes included in the coverage.
     *
     * @return unique name or number, or {@code null}.
     */
    @Override
    @XmlElement(name = "sequenceIdentifier")
    public MemberName getSequenceIdentifier() {
        return sequenceIdentifier;
    }

    /**
     * Sets the name or number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     *
     * @param  newValue  the new sequence identifier.
     */
    public void setSequenceIdentifier(final MemberName newValue) {
        checkWritePermission(sequenceIdentifier);
        sequenceIdentifier = newValue;
    }

    /**
     * Returns the description of the attribute.
     *
     * @return description of the attribute, or {@code null}.
     *
     * @since 0.5
     */
    @XmlElement(name = "description")
    @XmlJavaTypeAdapter(InternationalStringAdapter.Since2014.class)
    @UML(identifier="description", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the description of the attribute.
     *
     * @param  newValue  the new description.
     *
     * @since 0.5
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission(description);
        description = newValue;
    }

    /**
     * Returns the description of the range of a cell measurement value.
     * This method fetches the value from the {@linkplain #getDescription() description}.
     *
     * @return description of the range of a cell measurement value, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, renamed {@link #getDescription()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getDescription")
    @XmlElement(name = "descriptor", namespace = LegacyNamespaces.GMD)
    public InternationalString getDescriptor() {
        return FilterByVersion.LEGACY_METADATA.accept() ? getDescription() : null;
    }

    /**
     * Sets the description of the range of a cell measurement value.
     * This method stores the value in the {@linkplain #setDescription(InternationalString) description}.
     *
     * @param  newValue  the new descriptor.
     *
     * @deprecated As of ISO 19115:2014, renamed {@link #setDescription(InternationalString)}.
     */
    @Deprecated(since="1.0")
    public void setDescriptor(final InternationalString newValue) {
        setDescription(newValue);
    }

    /**
     * Returns the identifiers for each attribute included in the resource.
     * These identifiers can be use to provide names for the attribute from a standard set of names.
     *
     * @return identifiers for each attribute included in the resource.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="name", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Identifier> getNames() {
        return names = nonNullCollection(names, Identifier.class);
    }

    /**
     * Sets the identifiers for each attribute included in the resource.
     *
     * @param  newValues  the new identifiers for each attribute.
     *
     * @since 0.5
     */
    public void setNames(final Collection<? extends Identifier> newValues) {
        names = writeCollection(newValues, names, Identifier.class);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "name")
    private Collection<Identifier> getName() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getNames() : null;
    }
}
