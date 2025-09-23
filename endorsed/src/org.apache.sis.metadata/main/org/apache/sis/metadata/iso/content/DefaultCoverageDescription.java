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
import java.util.Collections;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.content.RangeElementDescription;
import org.opengis.util.RecordType;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.metadata.MD_Identifier;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;

// Specific to the main branch:
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the content of a grid data cell.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_CoverageDescription}
 * {@code   └─attributeDescription……} Description of the attribute described by the measurement value.</div>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_CoverageDescription_Type", propOrder = {
    "attributeDescription",
    "processingLevelCode",          // New in ISO 19115:2014
    "attributeGroup",               // Ibid.
    "contentType",                  // Legacy ISO 19115:2003
    "dimensions",                   // Ibid.
    "rangeElementDescriptions"
})
@XmlRootElement(name = "MD_CoverageDescription")
@XmlSeeAlso({
    DefaultImageDescription.class,
    org.apache.sis.xml.bind.gmi.MI_CoverageDescription.class
})
public class DefaultCoverageDescription extends AbstractContentInformation implements CoverageDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2161065580202989466L;

    /**
     * Description of the attribute described by the measurement value.
     */
    @SuppressWarnings("serial")
    private RecordType attributeDescription;

    /**
     * Identifier for the level of processing that has been applied to the resource.
     */
    @SuppressWarnings("serial")
    private Identifier processingLevelCode;

    /**
     * Information on attribute groups of the resource.
     */
    @SuppressWarnings("serial")
    private Collection<DefaultAttributeGroup> attributeGroups;

    /**
     * Provides the description of the specific range elements of a coverage.
     */
    @SuppressWarnings("serial")
    private Collection<RangeElementDescription> rangeElementDescriptions;

    /**
     * Constructs an empty coverage description.
     */
    public DefaultCoverageDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(CoverageDescription)
     */
    public DefaultCoverageDescription(final CoverageDescription object) {
        super(object);
        if (object != null) {
            attributeDescription     = object.getAttributeDescription();
            rangeElementDescriptions = copyCollection(object.getRangeElementDescriptions(), RangeElementDescription.class);
            if (object instanceof DefaultCoverageDescription) {
                processingLevelCode  = ((DefaultCoverageDescription) object).getProcessingLevelCode();
                attributeGroups      = copyCollection(((DefaultCoverageDescription) object).getAttributeGroups(), DefaultAttributeGroup.class);
            }
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
     *       {@linkplain #DefaultCoverageDescription(CoverageDescription) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * @return description of the attribute.
     */
    @Override
    @XmlElement(name = "attributeDescription", required = true)
    public RecordType getAttributeDescription() {
        return attributeDescription;
    }

    /**
     * Sets the description of the attribute described by the measurement value.
     *
     * @param  newValue  the new attribute description.
     */
    public void setAttributeDescription(final RecordType newValue) {
        checkWritePermission(attributeDescription);
        attributeDescription = newValue;
    }

    /**
     * Returns an identifier for the level of processing that has been applied to the resource, or {@code null} if none.
     * For {@linkplain DefaultImageDescription image descriptions}, this is the image distributor's code that identifies
     * the level of radiometric and geometric processing that has been applied.
     *
     * @return identifier for the level of processing that has been applied to the resource, or {@code null} if none.
     *
     * @since 0.5
     */
    @XmlElement(name = "processingLevelCode")
    @XmlJavaTypeAdapter(MD_Identifier.Since2014.class)
    @UML(identifier="processingLevelCode", obligation=OPTIONAL, specification=ISO_19115)
    public Identifier getProcessingLevelCode() {
        return processingLevelCode;
    }

    /**
     * Sets the identifier for the level of processing that has been applied to the resource.
     *
     * @param  newValue  the new identifier for the level of processing.
     *
     * @since 0.5
     */
    public void setProcessingLevelCode(final Identifier newValue) {
        checkWritePermission(processingLevelCode);
        processingLevelCode = newValue;
    }

    /**
     * Returns information on attribute groups of the resource.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code AttributeGroup} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return information on attribute groups of the resource.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="attributeGroup", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultAttributeGroup> getAttributeGroups() {
        return attributeGroups = nonNullCollection(attributeGroups, DefaultAttributeGroup.class);
    }

    /**
     * Sets information on attribute groups of the resource.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code AttributeGroup} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param  newValues  the new information on attribute groups of the resource.
     *
     * @since 0.5
     */
    public void setAttributeGroups(final Collection<? extends DefaultAttributeGroup> newValues) {
        attributeGroups = writeCollection(newValues, attributeGroups, DefaultAttributeGroup.class);
    }

    /**
     * Returns the type of information represented by the cell value.
     * This method fetches the value from the {@linkplain #getAttributeGroups() attribute groups}.
     *
     * @return type of information represented by the cell value, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultAttributeGroup#getContentTypes()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getAttributeGroups")
    @XmlElement(name = "contentType", namespace = LegacyNamespaces.GMD)
    public CoverageContentType getContentType() {
        CoverageContentType type = null;
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Collection<DefaultAttributeGroup> groups = getAttributeGroups();
            if (groups != null) {                                               // May be null on marshalling.
                for (final DefaultAttributeGroup g : groups) {
                    final Collection<? extends CoverageContentType> contentTypes = g.getContentTypes();
                    if (contentTypes != null) {                                 // May be null on marshalling.
                        for (final CoverageContentType t : contentTypes) {
                            if (type == null) {
                                type = t;
                            } else {
                                LegacyPropertyAdapter.warnIgnoredExtraneous(CoverageContentType.class,
                                        DefaultCoverageDescription.class, "getContentType");
                                break;
                            }
                        }
                    }
                }
            }
        }
        return type;
    }

    /**
     * Sets the type of information represented by the cell value.
     * This method stores the value in the first writable {@linkplain #getAttributeGroups() attribute groups}.
     *
     * @param  newValue  the new content type.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultAttributeGroup#setContentTypes(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setContentType(final CoverageContentType newValue) {
        checkWritePermission(valueIfDefined(attributeGroups));
        final Collection<CoverageContentType> newValues = CollectionsExt.singletonOrEmpty(newValue);
        Collection<DefaultAttributeGroup> groups = attributeGroups;
        if (groups != null) {
            for (final DefaultAttributeGroup group : groups) {
                group.setContentTypes(newValues);
                return; // Actually stop at the first instance.
            }
        }
        final DefaultAttributeGroup group = new DefaultAttributeGroup();
        group.setContentTypes(newValues);
        if (groups != null) {
            groups.add(group);
        } else {
            groups = Collections.<DefaultAttributeGroup>singleton(group);
        }
        setAttributeGroups(groups);
    }

    /**
     * Returns the information on the dimensions of the cell measurement value.
     * This method fetches the values from the first {@linkplain #getAttributeGroups() attribute groups}.
     *
     * @return dimensions of the cell measurement value.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultAttributeGroup#getAttributes()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getAttributeGroups")
    @XmlElement(name = "dimension", namespace = LegacyNamespaces.GMD)
    public final Collection<RangeDimension> getDimensions() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new LegacyPropertyAdapter<RangeDimension,DefaultAttributeGroup>(getAttributeGroups()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected DefaultAttributeGroup wrap(final RangeDimension value) {
                final DefaultAttributeGroup container = new DefaultAttributeGroup();
                container.setAttributes(CollectionsExt.singletonOrEmpty(value));
                return container;
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected RangeDimension unwrap(final DefaultAttributeGroup container) {
                return getSingleton(container.getAttributes(), RangeDimension.class,
                        this, DefaultCoverageDescription.class, "getDimensions");
            }

            /** Updates the legacy value in an existing instance of the new kind of value. */
            @Override protected boolean update(final DefaultAttributeGroup container, final RangeDimension value) {
                if (container instanceof DefaultAttributeGroup) {
                    container.setAttributes(CollectionsExt.singletonOrEmpty(value));
                    return true;
                }
                return false;
            }
        }.validOrNull();
    }

    /**
     * Sets the information on the dimensions of the cell measurement value.
     * This method stores the values in the {@linkplain #getAttributeGroups() attribute groups}.
     *
     * @param  newValues  the new dimensions.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultAttributeGroup#setAttributes(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setDimensions(final Collection<? extends RangeDimension> newValues) {
        checkWritePermission(valueIfDefined(attributeGroups));
        ((LegacyPropertyAdapter<RangeDimension,?>) getDimensions()).setValues(newValues);
    }

    /**
     * Provides the description of the specific range elements of a coverage.
     *
     * @return description of the specific range elements of a coverage.
     */
    @Override
    @XmlElement(name = "rangeElementDescription")
    public Collection<RangeElementDescription> getRangeElementDescriptions() {
        return rangeElementDescriptions = nonNullCollection(rangeElementDescriptions, RangeElementDescription.class);
    }

    /**
     * Sets the description of the specific range elements of a coverage.
     *
     * @param  newValues  the new range element description.
     */
    public void setRangeElementDescriptions(final Collection<? extends RangeElementDescription> newValues) {
        rangeElementDescriptions = writeCollection(newValues, rangeElementDescriptions, RangeElementDescription.class);
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
    @XmlElement(name = "attributeGroup")
    private Collection<DefaultAttributeGroup> getAttributeGroup() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getAttributeGroups() : null;
    }
}
