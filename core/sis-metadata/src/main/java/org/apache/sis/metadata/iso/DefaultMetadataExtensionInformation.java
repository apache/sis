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
package org.apache.sis.metadata.iso;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.MetadataExtensionInformation;


/**
 * Information describing metadata extensions.
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_MetadataExtensionInformation_Type", propOrder = {
    "extensionOnLineResource",
    "extendedElementInformation"
})
@XmlRootElement(name = "MD_MetadataExtensionInformation")
public class DefaultMetadataExtensionInformation extends ISOMetadata
        implements MetadataExtensionInformation
{
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 1471486028607039929L;

    /**
     * Information about on-line sources containing the community profile name and
     * the extended metadata elements. Information for all new metadata elements.
     */
    private OnlineResource extensionOnLineResource;

    /**
     * Provides information about a new metadata element, not found in ISO 19115, which is
     * required to describe geographic data.
     */
    private Collection<ExtendedElementInformation> extendedElementInformation;

    /**
     * Construct an initially empty metadata extension information.
     */
    public DefaultMetadataExtensionInformation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(MetadataExtensionInformation)
     */
    public DefaultMetadataExtensionInformation(final MetadataExtensionInformation object) {
        super(object);
        if (object != null) {
            extensionOnLineResource    = object.getExtensionOnLineResource();
            extendedElementInformation = copyCollection(object.getExtendedElementInformation(), ExtendedElementInformation.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultMetadataExtensionInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultMetadataExtensionInformation} instance is created using the
     *       {@linkplain #DefaultMetadataExtensionInformation(MetadataExtensionInformation) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultMetadataExtensionInformation castOrCopy(final MetadataExtensionInformation object) {
        if (object == null || object instanceof DefaultMetadataExtensionInformation) {
            return (DefaultMetadataExtensionInformation) object;
        }
        return new DefaultMetadataExtensionInformation(object);
    }

    /**
     * Information about on-line sources containing the community profile name and
     * the extended metadata elements and information for all new metadata elements.
     *
     * @return Online sources to community profile name and extended metadata elements, or {@code null}.
     */
    @Override
    @XmlElement(name = "extensionOnLineResource")
    public OnlineResource getExtensionOnLineResource() {
        return extensionOnLineResource;
    }

    /**
     * Sets information about on-line sources.
     *
     * @param newValue The new extension online resource.
     */
    public void setExtensionOnLineResource(final OnlineResource newValue) {
        checkWritePermission();
        this.extensionOnLineResource = newValue;
    }

    /**
     * Provides information about a new metadata element, not found in ISO 19115,
     * which is required to describe resource.
     *
     * @return New metadata elements not found in ISO 19115.
     */
    @Override
    @XmlElement(name = "extendedElementInformation")
    public Collection<ExtendedElementInformation> getExtendedElementInformation() {
        return extendedElementInformation = nonNullCollection(extendedElementInformation, ExtendedElementInformation.class);
    }

    /**
     * Sets information about a new metadata element.
     *
     * @param newValues The new extended element information.
     */
    public void setExtendedElementInformation(final Collection<? extends ExtendedElementInformation> newValues) {
        extendedElementInformation = writeCollection(newValues, extendedElementInformation, ExtendedElementInformation.class);
    }
}
