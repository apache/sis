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
package org.apache.sis.metadata.iso.maintenance;

import java.util.Set;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Description of the class of information covered by the information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_ScopeDescription_Type", propOrder = {
    "dataset",
    "other"
})
@XmlRootElement(name = "MD_ScopeDescription")
public class DefaultScopeDescription extends ISOMetadata implements ScopeDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5671299759930976286L;

    /**
     * The attributes to which the information applies.
     */
    private Set<AttributeType> attributes;

    /**
     * The features to which the information applies.
     */
    private Set<FeatureType> features;

    /**
     * The feature instances to which the information applies.
     */
    private Set<FeatureType> featureInstances;

    /**
     * The attribute instances to which the information applies.
     */
    private Set<AttributeType> attributeInstances;

    /**
     * Dataset to which the information applies.
     */
    private String dataset;

    /**
     * Class of information that does not fall into the other categories to
     * which the information applies.
     */
    private String other;

    /**
     * Creates an initially empty scope description.
     */
    public DefaultScopeDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(ScopeDescription)
     */
    public DefaultScopeDescription(final ScopeDescription object) {
        super(object);
        dataset            = object.getDataset();
        other              = object.getOther();
        attributeInstances = copySet(object.getAttributeInstances(), AttributeType.class);
        attributes         = copySet(object.getAttributes(), AttributeType.class);
        featureInstances   = copySet(object.getFeatureInstances(), FeatureType.class);
        features           = copySet(object.getFeatures(), FeatureType.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultScopeDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultScopeDescription} instance is created using the
     *       {@linkplain #DefaultScopeDescription(ScopeDescription) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultScopeDescription castOrCopy(final ScopeDescription object) {
        if (object == null || object instanceof DefaultScopeDescription) {
            return (DefaultScopeDescription) object;
        }
        return new DefaultScopeDescription(object);
    }

    /**
     * Returns the attributes to which the information applies.
     */
    @Override
    public synchronized Set<AttributeType> getAttributes() {
        return attributes = nonNullSet(attributes, AttributeType.class);
    }

    /**
     * Sets the attributes to which the information applies.
     *
     * @param newValues The new attributes.
     */
    public synchronized void setAttributes(final Set<? extends AttributeType> newValues) {
        attributes = writeSet(newValues, attributes, AttributeType.class);
    }

    /**
     * Returns the features to which the information applies.
     */
    @Override
    public synchronized Set<FeatureType> getFeatures() {
        return features = nonNullSet(features, FeatureType.class);
    }

    /**
     * Sets the features to which the information applies.
     *
     * @param newValues The new features.
     */
    public synchronized void setFeatures(final Set<? extends FeatureType> newValues) {
        features = writeSet(newValues, features, FeatureType.class);
    }

    /**
     * Returns the feature instances to which the information applies.
     */
    @Override
    public synchronized Set<FeatureType> getFeatureInstances() {
        return featureInstances = nonNullSet(featureInstances, FeatureType.class);
    }

    /**
     * Sets the feature instances to which the information applies.
     *
     * @param newValues The new feature instances.
     */
    public synchronized void setFeatureInstances(final Set<? extends FeatureType> newValues) {
        featureInstances = writeSet(newValues, featureInstances, FeatureType.class);
    }

    /**
     * Returns the attribute instances to which the information applies.
     */
    @Override
    public synchronized Set<AttributeType> getAttributeInstances() {
        return attributeInstances = nonNullSet(attributeInstances, AttributeType.class);
    }

    /**
     * Sets the attribute instances to which the information applies.
     *
     * @param newValues The new attribute instances.
     */
    public synchronized void setAttributeInstances(final Set<? extends AttributeType> newValues) {
        attributeInstances = writeSet(newValues, attributeInstances, AttributeType.class);
    }

    /**
     * Returns the dataset to which the information applies.
     */
    @Override
    @XmlElement(name = "dataset")
    public synchronized String getDataset() {
        return dataset;
    }

    /**
     * Sets the dataset to which the information applies.
     *
     * @param newValue The new dataset.
     */
    public synchronized void setDataset(final String newValue) {
        checkWritePermission();
        dataset = newValue;
    }

    /**
     * Returns the class of information that does not fall into the other categories to
     * which the information applies.
     */
    @Override
    @XmlElement(name = "other")
    public synchronized String getOther() {
        return other;
    }

    /**
     * Sets the class of information that does not fall into the other categories to
     * which the information applies.
     *
     * @param newValue Other class of information.
     */
    public synchronized void setOther(final String newValue) {
        checkWritePermission();
        other = newValue;
    }
}
