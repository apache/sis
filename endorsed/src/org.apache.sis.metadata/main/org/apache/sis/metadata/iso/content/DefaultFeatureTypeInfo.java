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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.opengis.util.GenericName;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ensurePositive;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.content.FeatureTypeInfo;


/**
 * Information about the occurring feature type.
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see org.apache.sis.storage.FeatureNaming
 * @see org.apache.sis.feature.DefaultFeatureType
 *
 * @since 0.5
 */
@TitleProperty(name = "featureTypeName")
@XmlType(name = "MD_FeatureTypeInfo_Type", propOrder = {
    "featureTypeName",
    "featureInstanceCount"
})
@XmlRootElement(name = "MD_FeatureTypeInfo")
public class DefaultFeatureTypeInfo extends ISOMetadata implements FeatureTypeInfo {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -4103901642915981422L;

    /**
     * Name of the feature type.
     */
    @SuppressWarnings("serial")
    private GenericName featureTypeName;

    /**
     * Number of occurrence of feature instances for this feature types.
     */
    private Integer featureInstanceCount;

    /**
     * Constructs an initially empty feature type info.
     */
    public DefaultFeatureTypeInfo() {
    }

    /**
     * Constructs a feature type info initialized to the specified name.
     *
     * @param  featureTypeName  name of the feature type.
     */
    public DefaultFeatureTypeInfo(final GenericName featureTypeName) {
        this.featureTypeName = featureTypeName;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * <h4>Note on properties validation</h4>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(FeatureTypeInfo)
     */
    public DefaultFeatureTypeInfo(final FeatureTypeInfo object) {
        super(object);
        if (object != null) {
            featureTypeName      = object.getFeatureTypeName();
            featureInstanceCount = object.getFeatureInstanceCount();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultFeatureTypeInfo}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultFeatureTypeInfo} instance is created using the
     *       {@linkplain #DefaultFeatureTypeInfo(FeatureTypeInfo) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultFeatureTypeInfo castOrCopy(final FeatureTypeInfo object) {
        if (object == null || object instanceof DefaultFeatureTypeInfo) {
            return (DefaultFeatureTypeInfo) object;
        }
        return new DefaultFeatureTypeInfo(object);
    }

    /**
     * Returns the name of the feature type.
     *
     * @return name of the feature type.
     *
     * @see org.apache.sis.feature.DefaultFeatureType#getName()
     */
    @Override
    @XmlElement(name = "featureTypeName", required = true)
    public GenericName getFeatureTypeName() {
        return featureTypeName;
    }

    /**
     * Sets the name of the feature type.
     *
     * @param  newValue  the new name.
     */
    public void setFeatureTypeName(final GenericName newValue) {
        checkWritePermission(featureTypeName);
        featureTypeName = newValue;
    }

    /**
     * Returns the number of occurrence of feature instances for this feature types, or {@code null} if none.
     *
     * @return the number of occurrence of feature instances for this feature types, or {@code null} if none.
     */
    @Override
    @ValueRange(minimum = 1)
    @XmlElement(name = "featureInstanceCount")
    public Integer getFeatureInstanceCount() {
        return featureInstanceCount;
    }

    /**
     * Sets a new number of occurrence of feature instances for this feature types.
     *
     * @param  newValue  the new number of occurrence.
     * @throws IllegalArgumentException if the given value is zero or negative.
     */
    public void setFeatureInstanceCount(final Integer newValue) {
        checkWritePermission(featureInstanceCount);
        if (ensurePositive(DefaultFeatureTypeInfo.class, "featureInstanceCount", true, newValue)) {
            featureInstanceCount = newValue;
        }
    }
}
