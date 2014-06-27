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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.MemberName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.RangeDimension;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information on the range of each dimension of a cell measurement value.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_RangeDimension_Type", propOrder = {
    "sequenceIdentifier",
    "descriptor",
/// "names"
})
@XmlRootElement(name = "MD_RangeDimension")
@XmlSeeAlso(DefaultBand.class)
public class DefaultRangeDimension extends ISOMetadata implements RangeDimension {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4517148689016920767L;

    /**
     * Number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     */
    private MemberName sequenceIdentifier;

    /**
     * Description of the attribute.
     */
    private InternationalString description;

    /**
     * Identifiers for each attribute included in the resource. These identifiers
     * can be use to provide names for the attribute from a standard set of names.
     */
    private Collection<Identifier> names;

    /**
     * Constructs an initially empty range dimension.
     */
    public DefaultRangeDimension() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(RangeDimension)
     */
    public DefaultRangeDimension(final RangeDimension object) {
        super(object);
        if (object != null) {
            sequenceIdentifier = object.getSequenceIdentifier();
            description        = object.getDescriptor();
///         names              = copyCollection(object.getNames(), Identifier.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link Band}, then this method
     *       delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultRangeDimension}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRangeDimension} instance is created using the
     *       {@linkplain #DefaultRangeDimension(RangeDimension) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * Returns the number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     *
     * @return Identifier of bands on which a sensor operates, or {@code null}.
     */
    @Override
    @XmlElement(name = "sequenceIdentifier")
    public MemberName getSequenceIdentifier() {
        return sequenceIdentifier;
    }

    /**
     * Sets the number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     *
     * @param newValue The new sequence identifier.
     */
    public void setSequenceIdentifier(final MemberName newValue) {
        checkWritePermission();
        sequenceIdentifier = newValue;
    }

    /**
     * Returns the description of the attribute.
     *
     * @return Description of the attribute, or {@code null}.
     *
     * @since 0.5
     */
/// @Override
/// @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the description of the attribute.
     *
     * @param newValue The new description.
     *
     * @since 0.5
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the description of the range of a cell measurement value.
     * This method fetches the value from the {@linkplain #getDescription() description}.
     *
     * @return Description of the range of a cell measurement value, or {@code null}.
     *
     * @deprecated Renamed {@link #getDescription()} as of ISO 19115:2014.
     */
    @Override
    @Deprecated
    @XmlElement(name = "descriptor")
    public final InternationalString getDescriptor() {
        return getDescription();
    }

    /**
     * Sets the description of the range of a cell measurement value.
     * This method stores the value in the {@linkplain #setDescription(InternationalString) description}.
     *
     * @param newValue The new descriptor.
     *
     * @deprecated Renamed {@link #setDescription(InternationalString)}.
     */
    @Deprecated
    public final void setDescriptor(final InternationalString newValue) {
        setDescription(newValue);
    }

    /**
     * Returns the identifiers for each attribute included in the resource.
     * These identifiers can be use to provide names for the attribute from a standard set of names.
     *
     * @return Identifiers for each attribute included in the resource.
     *
     * @since 0.5
     */
/// @Override
/// @XmlElement(name = "name")
    public Collection<Identifier> getNames() {
        return names = nonNullCollection(names, Identifier.class);
    }

    /**
     * Sets the identifiers for each attribute included in the resource.
     *
     * @param newValues The new identifiers for each attribute.
     *
     * @since 0.5
     */
    public void setNames(final Collection<? extends Identifier> newValues) {
        names = writeCollection(newValues, names, Identifier.class);
    }
}
