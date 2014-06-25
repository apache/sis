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
 * @author  Remi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_AttributeGroup_Type", propOrder = {
/// "contentType",
/// "groupAttribute"
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
    private Collection<RangeDimension> groupAttributes;

    /**
     * Constructs an initially empty attribute group.
     */
    public DefaultAttributeGroup(){
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
            contentTypes    = copyCollection(object.getContentTypes(), CoverageContentType.class);
            groupAttributes = copyCollection(object.getGroupAttributes(), RangeDimension.class);
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
/// @XmlElement(name = "contentType", required = true)
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
     * Returns the content types for attributes groups for a {@code RangeDimension}, or {@code null} if none.
     *
     * @return The content types for attributes groups for a {@code RangeDimension}, or {@code null} if none.
     */
    @Override
/// @XmlElement(name = "groupAttribute")
    public Collection<RangeDimension> getGroupAttributes() {
        return groupAttributes = nonNullCollection(groupAttributes, RangeDimension.class);
    }

    /**
     * Sets the content types for attributes groups for a {@code RangeDimension}.
     *
     * @param newValues The new content types for attributes groups for a {@code RangeDimension}.
     */
    public void setGroupAttributes(final Collection<? extends RangeDimension> newValues) {
        groupAttributes = writeCollection(newValues, groupAttributes, RangeDimension.class);
    }
}
