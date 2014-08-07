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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.content.AttributeGroup;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.RangeDimension;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information about content type for groups of attributes for a specific
 * {@linkplain DefaultRangeDimension range dimension}.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_AttributeGroup_Type", propOrder = {
    "contentType",
    "attribute"
})
@XmlRootElement(name = "MD_AttributeGroup")
public class DefaultAttributeGroup extends ISOMetadata implements AttributeGroup {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -2198484393626051875L;

    /**
     * Type of information represented by the value(s).
     */
    private Collection<CoverageContentType> contentTypes;

    /**
     * Content type for attributes groups for a {@link RangeDimension}.
     */
    private Collection<RangeDimension> attributes;

    /**
     * Constructs an initially empty attribute group.
     */
    public DefaultAttributeGroup() {
    }

    /**
     * Constructs an attribute group initialized to the given values.
     *
     * @param contentType Type of information represented by the value, or {@code null}.
     * @param attribute   The attribute, or {@code null}.
     */
    public DefaultAttributeGroup(final CoverageContentType contentType, final RangeDimension attribute) {
        contentTypes = singleton(contentType, CoverageContentType.class);
        attributes   = singleton(attribute, RangeDimension.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(AttributeGroup)
     */
    public DefaultAttributeGroup(final AttributeGroup object) {
        super(object);
        if (object != null) {
            contentTypes = copyCollection(object.getContentTypes(), CoverageContentType.class);
            attributes   = copyCollection(object.getAttributes(), RangeDimension.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultAttributeGroup}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultAttributeGroup} instance is created using the
     *       {@linkplain #DefaultAttributeGroup(AttributeGroup) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAttributeGroup castOrCopy(final AttributeGroup object) {
        if (object == null || object instanceof DefaultAttributeGroup) {
            return (DefaultAttributeGroup) object;
        }
        return new DefaultAttributeGroup(object);
    }

    /**
     * Returns the types of information represented by the value(s).
     *
     * @return The types of information represented by the value(s).
     */
    @Override
    @XmlElement(name = "contentType", required = true)
    public Collection<CoverageContentType> getContentTypes() {
        return contentTypes = nonNullCollection(contentTypes, CoverageContentType.class);
    }

    /**
     * Sets the types of information represented by the value(s).
     *
     * @param newValues The new types of information.
     */
    public void setContentTypes(final Collection<? extends CoverageContentType> newValues) {
        contentTypes = writeCollection(newValues, contentTypes, CoverageContentType.class);
    }

    /**
     * Returns information on an attribute of the resource.
     *
     * @return Information on an attribute of the resource.
     */
    @Override
    @XmlElement(name = "attribute")
    public Collection<RangeDimension> getAttributes() {
        return attributes = nonNullCollection(attributes, RangeDimension.class);
    }

    /**
     * Sets information on an attribute of the resource.
     *
     * @param newValues The new attributes.
     */
    public void setAttributes(final Collection<? extends RangeDimension> newValues) {
        attributes = writeCollection(newValues, attributes, RangeDimension.class);
    }
}
