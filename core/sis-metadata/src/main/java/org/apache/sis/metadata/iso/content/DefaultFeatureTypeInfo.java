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
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the occurring feature type.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code FeatureTypeInfo} interface.
 * </div>
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
@XmlType(name = "MD_FeatureTypeInfo", propOrder = {
    "featureTypeName",
    "featureInstanceCount"
})
@XmlRootElement(name = "MD_FeatureTypeInfo")
@UML(identifier="MD_FeatureTypeInfo", specification=ISO_19115)
public class DefaultFeatureTypeInfo extends ISOMetadata {
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
     * Constructs a feature type info initialized to the specified name.
     *
     * @param featureTypeName Name of the feature type.
     */
    public DefaultFeatureTypeInfo(final GenericName featureTypeName) {
        this.featureTypeName = featureTypeName;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public DefaultFeatureTypeInfo(final DefaultFeatureTypeInfo object) {
        super(object);
        if (object != null) {
            featureTypeName      = object.getFeatureTypeName();
            featureInstanceCount = object.getFeatureInstanceCount();
        }
    }

    /**
     * Returns the name of the feature type.
     *
     * @return Name of the feature type.
     *
     * @see org.apache.sis.feature.DefaultFeatureType#getName()
     */
    @XmlElement(name = "featureTypeName", required = true)
    @UML(identifier="featureTypeName", obligation=MANDATORY, specification=ISO_19115)
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
    @ValueRange(minimum = 1)
    @XmlElement(name = "featureInstanceCount")
    @UML(identifier="featureInstanceCount", obligation=OPTIONAL, specification=ISO_19115)
    public Integer getFeatureInstanceCount() {
        return featureInstanceCount;
    }

    /**
     * Sets a new number of occurrence of feature instances for this feature types.
     *
     * @param newValue the new number of occurrence.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setFeatureInstanceCount(final Integer newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultFeatureTypeInfo.class, "featureInstanceCount", true, newValue)) {
            featureInstanceCount = newValue;
        }
    }
}
