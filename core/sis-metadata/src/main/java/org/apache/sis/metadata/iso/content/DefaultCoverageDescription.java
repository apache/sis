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
import org.opengis.metadata.Identifier;
import org.opengis.metadata.content.AttributeGroup;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.content.RangeElementDescription;
import org.opengis.util.RecordType;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;
import org.apache.sis.internal.metadata.MetadataUtilities;


/**
 * Information about the content of a grid data cell.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_CoverageDescription_Type", propOrder = {
    "attributeDescription",
    "contentType",
    "dimensions",
    "rangeElementDescriptions"
})
@XmlRootElement(name = "MD_CoverageDescription")
@XmlSeeAlso({
    DefaultImageDescription.class,
    org.apache.sis.internal.jaxb.gmi.MI_CoverageDescription.class
})
public class DefaultCoverageDescription extends AbstractContentInformation implements CoverageDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2161065580202989466L;

    /**
     * Description of the attribute described by the measurement value.
     */
    private RecordType attributeDescription;

    /**
     * Identifier for the level of processing that has been applied to the resource.
     */
    private Identifier processingLevelCode;

    /**
     * Information on attribute groups of the resource.
     */
    private Collection<AttributeGroup> attributeGroups;

    /**
     * Provides the description of the specific range elements of a coverage.
     */
    private Collection<RangeElementDescription> rangeElementDescriptions;

    /**
     * Constructs an empty coverage description.
     */
    public DefaultCoverageDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(CoverageDescription)
     */
    public DefaultCoverageDescription(final CoverageDescription object) {
        super(object);
        if (object != null) {
            attributeDescription     = object.getAttributeDescription();
            processingLevelCode      = object.getProcessingLevelCode();
            attributeGroups          = copyCollection(object.getAttributeGroups(), AttributeGroup.class);
            rangeElementDescriptions = copyCollection(object.getRangeElementDescriptions(), RangeElementDescription.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link ImageDescription}, then this
     *       method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultCoverageDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultCoverageDescription} instance is created using the
     *       {@linkplain #DefaultCoverageDescription(CoverageDescription) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCoverageDescription castOrCopy(final CoverageDescription object) {
        if (object instanceof ImageDescription) {
            return DefaultImageDescription.castOrCopy((ImageDescription) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultCoverageDescription) {
            return (DefaultCoverageDescription) object;
        }
        return new DefaultCoverageDescription(object);
    }

    /**
     * Returns the description of the attribute described by the measurement value.
     *
     * @return Description of the attribute.
     */
    @Override
    @XmlElement(name = "attributeDescription", required = true)
    public RecordType getAttributeDescription() {
        return attributeDescription;
    }

    /**
     * Sets the description of the attribute described by the measurement value.
     *
     * @param newValue The new attribute description.
     */
    public void setAttributeDescription(final RecordType newValue) {
        checkWritePermission();
        attributeDescription = newValue;
    }

    /**
     * Returns an identifier for the level of processing that has been applied to the resource, or {@code null} if none.
     *
     * @return Identifier for the level of processing that has been applied to the resource, or {@code null} if none.
     *
     * @since 0.5
     */
    @Override
/// @XmlElement(name = "processingLevelCode")
    public Identifier getProcessingLevelCode() {
        return processingLevelCode;
    }

    /**
     * Sets the identifier for the level of processing that has been applied to the resource.
     *
     * @param newValue The new identifier for the level of processing.
     *
     * @since 0.5
     */
    public void setProcessingLevelCode(final Identifier newValue) {
        checkWritePermission();
        processingLevelCode = newValue;
    }

    /**
     * Returns information on attribute groups of the resource.
     *
     * @return Information on attribute groups of the resource.
     *
     * @since 0.5
     */
    @Override
/// @XmlElement(name = "attributeGroup")
    public Collection<AttributeGroup> getAttributeGroups() {
        return attributeGroups = nonNullCollection(attributeGroups, AttributeGroup.class);
    }

    /**
     * Sets information on attribute groups of the resource.
     *
     * @param newValues The new information on attribute groups of the resource.
     *
     * @since 0.5
     */
    public void setAttributeGroups(final Collection<? extends AttributeGroup> newValues) {
        attributeGroups = writeCollection(newValues, attributeGroups, AttributeGroup.class);
    }

    /**
     * Returns the type of information represented by the cell value.
     *
     * @return Type of information represented by the cell value, or {@code null}.
     *
     * @deprecated Moved to {@link DefaultAttributeGroup#getContentTypes()} as of ISO 19115:2014.
     */
    @Override
    @Deprecated
    @XmlElement(name = "contentType", required = true)
    public CoverageContentType getContentType() {
        CoverageContentType type = null;
        if (attributeGroups != null) {
            for (final AttributeGroup g : attributeGroups) {
                final Collection<? extends CoverageContentType> contentTypes = g.getContentTypes();
                if (contentTypes != null) { // May be null on marshalling.
                    for (final CoverageContentType t : contentTypes) {
                        if (type == null) {
                            type = t;
                        } else {
                            MetadataUtilities.warning(DefaultCoverageDescription.class, "getContentType",
                                    Messages.Keys.IgnoredPropertiesAfterFirst_1, CoverageContentType.class);
                            break;
                        }
                    }
                }
            }
        }
        return type;
    }

    /**
     * Sets the type of information represented by the cell value.
     *
     * @param newValue The new content type.
     *
     * @deprecated Moved to {@link DefaultAttributeGroup#setContentTypes(Collection)}.
     */
    @Deprecated
    public void setContentType(final CoverageContentType newValue) {
        checkWritePermission();
        final Collection<CoverageContentType> newValues = LegacyPropertyAdapter.asCollection(newValue);
        final Collection<AttributeGroup> groups = getAttributeGroups();
        for (final AttributeGroup group : groups) {
            if (group instanceof DefaultAttributeGroup) {
                ((DefaultAttributeGroup) group).setContentTypes(newValues);
                return;
            }
        }
        final DefaultAttributeGroup group = new DefaultAttributeGroup();
        group.setContentTypes(newValues);
        groups.add(group);
    }

    /**
     * Returns the information on the dimensions of the cell measurement value.
     *
     * @return Dimensions of the cell measurement value.
     *
     * @deprecated Moved to {@link DefaultAttributeGroup#getGroupAttributes()} as of ISO 19115:2014.
     */
    @Override
    @Deprecated
    @XmlElement(name = "dimension")
    public final Collection<RangeDimension> getDimensions() {
        return new LegacyPropertyAdapter<RangeDimension,AttributeGroup>(getAttributeGroups()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected AttributeGroup wrap(final RangeDimension value) {
                final DefaultAttributeGroup container = new DefaultAttributeGroup();
                container.setGroupAttributes(asCollection(value));
                return container;
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected RangeDimension unwrap(final AttributeGroup container) {
                return singleton(container.getGroupAttributes(), RangeDimension.class,
                        DefaultCoverageDescription.class, "getDimensions");
            }

            /** Updates the legacy value in an existing new kind of value. */
            @Override protected boolean update(final AttributeGroup container, final RangeDimension value) {
                if (container instanceof DefaultAttributeGroup) {
                    ((DefaultAttributeGroup) container).setGroupAttributes(asCollection(value));
                    return true;
                }
                return false;
            }
        }.validOrNull();
    }

    /**
     * Sets the information on the dimensions of the cell measurement value.
     *
     * @param newValues The new dimensions.
     *
     * @deprecated Moved to {@link DefaultAttributeGroup#setGroupAttributes(Collection)}.
     */
    @Deprecated
    public void setDimensions(final Collection<? extends RangeDimension> newValues) {
        checkWritePermission();
        ((LegacyPropertyAdapter<RangeDimension,?>) getDimensions()).setValues(newValues);
    }

    /**
     * Provides the description of the specific range elements of a coverage.
     *
     * @return Description of the specific range elements of a coverage.
     */
    @Override
    @XmlElement(name = "rangeElementDescription", namespace = Namespaces.GMI)
    public Collection<RangeElementDescription> getRangeElementDescriptions() {
        return rangeElementDescriptions = nonNullCollection(rangeElementDescriptions, RangeElementDescription.class);
    }

    /**
     * Sets the description of the specific range elements of a coverage.
     *
     * @param newValues The new range element description.
     */
    public void setRangeElementDescriptions(final Collection<? extends RangeElementDescription> newValues) {
        rangeElementDescriptions = writeCollection(newValues, rangeElementDescriptions, RangeElementDescription.class);
    }
}
