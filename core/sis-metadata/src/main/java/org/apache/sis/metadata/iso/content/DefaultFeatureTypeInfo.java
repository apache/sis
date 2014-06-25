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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.GenericName;
import org.opengis.metadata.content.FeatureTypeInfo;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.ArgumentChecks;


/**
 * Information about the occurring feature type.
 *
 * @author  Remi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_FeatureTypeInfo", propOrder = {
/// "featureTypeName",
/// "featureInstanceCount"
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
     * Constructs a feature type info initialized to the specified value.
     *
     * @param featureTypeName Name of the feature type.
     * @param featureInstanceCount Number of occurrence of feature instances for this feature types, or {@code null}.
     */
    public DefaultFeatureTypeInfo(final GenericName featureTypeName, final Integer featureInstanceCount) {
        if (featureInstanceCount != null) {
            ArgumentChecks.ensurePositive("featureInstanceCount", featureInstanceCount);
        }
        this.featureTypeName      = featureTypeName;
        this.featureInstanceCount = featureInstanceCount;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
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
     *       {@linkplain #DefaultFeatureTypeInfo(FeatureTypeInfo) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return Name of the feature type.
     */
    @Override
/// @XmlElement(name = "featureTypeName", required = true)
    public GenericName getFeatureTypeName() {
        return featureTypeName;
    }

    /**
     * Sets the name of the feature type.
     *
     * @param newValue The new name.
     */
    public void setFeatureTypeName(final GenericName newValue) {
        checkWritePermission();
        featureTypeName = newValue;
    }

    /**
     * Returns the number of occurrence of feature instances for this feature types, or {@code null} if none.
     *
     * @return The number of occurrence of feature instances for this feature types, or {@code null} if none.
     */
    @Override
    @ValueRange(minimum = 1)
/// @XmlElement(name = "featureInstanceCount")
    public Integer getFeatureInstanceCount() {
        return featureInstanceCount;
    }

    /**
     * Set a new number of occurrence of feature instances for this feature types.
     *
     * @param newValue the new number of occurrence.
     */
    public void setFeatureInstanceCount(final Integer newValue) {
        checkWritePermission();
        if (newValue != null) {
            ArgumentChecks.ensurePositive("featureInstanceCount", newValue);
        }
        featureInstanceCount = newValue;
    }
}
