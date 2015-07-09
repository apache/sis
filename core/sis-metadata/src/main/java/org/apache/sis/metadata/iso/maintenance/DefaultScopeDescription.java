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
import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.metadata.ExcludedSet;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.system.Semaphores;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Messages;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;

// Branch-dependent imports
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;


/**
 * Description of the class of information covered by the information.
 *
 * <div class="section">Relationship between properties</div>
 * ISO 19115 defines {@code ScopeDescription} as an <cite>union</cite> (in the C/C++ sense):
 * only one of the properties in this class can be set to a non-empty value.
 * Setting any property to a non-empty value discard all the other ones.
 *
 * <div class="section">Limitations</div>
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
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_ScopeDescription_Type") // No need for propOrder since this structure is a union (see javadoc).
@XmlRootElement(name = "MD_ScopeDescription")
public class DefaultScopeDescription extends ISOMetadata implements ScopeDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2029119689389845656L;

    /**
     * Enumeration of possible values for {@link #property}.
     */
    private static final byte DATASET=1, FEATURES=2, ATTRIBUTES=3, FEATURE_INSTANCES=4, ATTRIBUTE_INSTANCES=5, OTHER=6;

    /**
     * The names of the mutually exclusive properties. The index of each name shall be the
     * value of the above {@code byte} constants minus one.
     */
    private static final String[] NAMES = {
        "dataset",
        "features",
        "attributes",
        "featureInstances",
        "attributeInstances",
        "other"
    };

    /**
     * The names of the setter methods, for logging purpose only.
     */
    private static final String[] SETTERS = {
        "setDataset",
        "setFeatures",
        "setAttributes",
        "setFeatureInstances",
        "setAttributeInstances",
        "setOther"
    };

    /**
     * Specifies which property is set, or 0 if none.
     */
    private byte property;

    /**
     * The value, as one of the following types:
     *
     * <ul>
     *   <li>{@code Set<FeatureType>}   for the {@code features} property</li>
     *   <li>{@code Set<AttributeType>} for the {@code attributes} property</li>
     *   <li>{@code Set<FeatureType>}   for the {@code featureInstances} property</li>
     *   <li>{@code Set<AttributeType>} for the {@code attributeInstances} property</li>
     *   <li>{@code String} for the {@code dataset} property</li>
     *   <li>{@code String} for the {@code other} property</li>
     * </ul>
     */
    private Object value;

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
     * <p>If the given object contains more than one value, then the first non-null element in the
     * following list has precedence (from wider scope to smaller scope):
     * {@linkplain #getDataset() dataset},
     * {@linkplain #getFeatures() features},
     * {@linkplain #getAttributes() attributes},
     * {@linkplain #getFeatureInstances() feature instances},
     * {@linkplain #getAttributeInstances() attribute instances}
     * and {@linkplain #getOther() other}.</p>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ScopeDescription)
     */
    @SuppressWarnings("unchecked")
    public DefaultScopeDescription(final ScopeDescription object) {
        super(object);
        if (object != null) {
            for (byte i=DATASET; i<=OTHER; i++) {
                Object candidate;
                switch (i) {
                    case DATASET:             candidate = object.getDataset();            break;
                    case FEATURES:            candidate = object.getFeatures();           break;
                    case ATTRIBUTES:          candidate = object.getAttributes();         break;
                    case FEATURE_INSTANCES:   candidate = object.getFeatureInstances();   break;
                    case ATTRIBUTE_INSTANCES: candidate = object.getAttributeInstances(); break;
                    case OTHER:               candidate = object.getOther();              break;
                    default: throw new AssertionError(i);
                }
                if (candidate != null) {
                    switch (i) {
                        case ATTRIBUTES:
                        case ATTRIBUTE_INSTANCES: {
                            candidate = copySet((Collection<AttributeType>) candidate, AttributeType.class);
                            break;
                        }
                        case FEATURES:
                        case FEATURE_INSTANCES: {
                            candidate = copySet((Collection<FeatureType>) candidate, FeatureType.class);
                            break;
                        }
                    }
                    value = candidate;
                    property = i;
                    break;
                }
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
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
     * Returns the given value casted to a {@code Set} of elements of the given type.
     * It is caller responsibility to ensure that the cast is valid, as element type
     * is verified only when assertions are enabled.
     */
    @SuppressWarnings("unchecked")
    private static <E> Set<E> cast(final Object value, final Class<E> type) {
        assert ((CheckedContainer<?>) value).getElementType() == type;
        return (Set<E>) value;
    }

    /**
     * Returns the set of properties identified by the {@code code} argument,
     * or an unmodifiable empty set if another value is defined.
     */
    private <E> Set<E> getProperty(final Class<E> type, final byte code) {
        final Object value = this.value;
        if (value != null) {
            if (property == code) {
                return cast(value, type);
            } else if (!(value instanceof Set) || !((Set<?>) value).isEmpty()) {
                return Semaphores.query(Semaphores.NULL_COLLECTION)
                       ? null : new ExcludedSet<E>(NAMES[code-1], NAMES[property-1]);
            }
        }
        // Unconditionally create a new set, because the
        // user may hold a reference to the previous one.
        final Set<E> c = nonNullSet(null, type);
        property = code;
        this.value = c;
        return c;
    }

    /**
     * Sets the properties identified by the {@code code} argument, if non-null and non-empty.
     * This discards any other properties.
     *
     * @param caller The caller method, for logging purpose.
     * @param code   The property which is going to be set.
     */
    private <E> void setProperty(final Set<? extends E> newValue, final Class<E> type, final byte code) {
        Set<E> c = null;
        if (property == code) {
            c = cast(value, type);
        } else if (isNullOrEmpty(newValue)) {
            return;
        } else {
            warningOnOverwrite(code);
            property = code;
        }
        value = writeSet(newValue, c, type);
    }

    /**
     * Sends a warning if setting the value for the given property would overwrite an existing property.
     *
     * @param code The property which is going to be set.
     */
    private void warningOnOverwrite(final byte code) {
        if (value != null && property != code) {
            Context.warningOccured(Context.current(), DefaultScopeDescription.class, SETTERS[code-1],
                    Messages.class, Messages.Keys.DiscardedExclusiveProperty_2, NAMES[property-1], NAMES[code-1]);
        }
    }

    /**
     * Returns the dataset to which the information applies.
     *
     * <div class="note"><b>Example:</b>
     * If a geographic data provider is generating vector mapping for thee administrative areas
     * and if the data were processed in the same way, then the provider could record the bulk
     * of initial data at {@link ScopeCode#DATASET} level with a
     * “<cite>Administrative area A, B &amp; C</cite>” description.
     * </div>
     *
     * @return Dataset to which the information applies, or {@code null}.
     */
    @Override
    @XmlElement(name = "dataset")
    public String getDataset() {
        return (property == DATASET) ? (String) value : null;
    }

    /**
     * Sets the dataset to which the information applies.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
     *
     * @param newValue The new dataset.
     */
    public void setDataset(final String newValue) {
        checkWritePermission();
        if (newValue != null || property == DATASET) {
            warningOnOverwrite(DATASET);
            property = DATASET;
            value = newValue;
        }
    }

    /**
     * Returns the feature types to which the information applies.
     *
     * <div class="note"><b>Example:</b>
     * if an administrative area performs a complete re-survey of the road network,
     * the change can be recorded at {@link ScopeCode#FEATURE_TYPE} level with a
     * “<cite>Administrative area A — Road network</cite>” description.
     * </div>
     *
     * <div class="section">Conditions</div>
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @return Feature types to which the information applies.
     */
    @Override
    public Set<FeatureType> getFeatures() {
        return getProperty(FeatureType.class, FEATURES);
    }

    /**
     * Sets the feature types to which the information applies.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @param newValues The new feature types.
     */
    public void setFeatures(final Set<? extends FeatureType> newValues) {
        setProperty(newValues, FeatureType.class, FEATURES);
    }

    /**
     * Returns the attribute types to which the information applies.
     *
     * <div class="note"><b>Example:</b>
     * if an administrative area detects an anomaly in all overhead clearance of the road survey,
     * the correction can be recorded at {@link ScopeCode#ATTRIBUTE_TYPE} level with a
     * “<cite>Administrative area A — Overhead clearance</cite>” description.
     * </div>
     *
     * <div class="section">Conditions</div>
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @return Attribute types to which the information applies.
     */
    @Override
    public Set<AttributeType> getAttributes() {
        return getProperty(AttributeType.class, ATTRIBUTES);
    }

    /**
     * Sets the attribute types to which the information applies.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @param newValues The new attribute types.
     */
    public void setAttributes(final Set<? extends AttributeType> newValues) {
        setProperty(newValues, AttributeType.class, ATTRIBUTES);
    }

    /**
     * Returns the feature instances to which the information applies.
     *
     * <div class="note"><b>Example:</b>
     * If a new bridge is constructed in a road network,
     * the change can be recorded at {@link ScopeCode#FEATURE} level with a
     * “<cite>Administrative area A — New bridge</cite>” description.
     * </div>
     *
     * <div class="section">Conditions</div>
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @return Feature instances to which the information applies.
     */
    @Override
    public Set<FeatureType> getFeatureInstances() {
        return getProperty(FeatureType.class, FEATURE_INSTANCES);
    }

    /**
     * Sets the feature instances to which the information applies.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @param newValues The new feature instances.
     */
    public void setFeatureInstances(final Set<? extends FeatureType> newValues) {
        setProperty(newValues, FeatureType.class, FEATURE_INSTANCES);
    }

    /**
     * Returns the attribute instances to which the information applies.
     *
     * <div class="note"><b>Example:</b>
     * If the overhead clearance of a new bridge was wrongly recorded,
     * the correction can be recorded at {@link ScopeCode#ATTRIBUTE} level with a
     * “<cite>Administrative area A — New bridge — Overhead clearance</cite>” description.
     * </div>
     *
     * <div class="section">Conditions</div>
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @return Attribute instances to which the information applies.
     */
    @Override
    public Set<AttributeType> getAttributeInstances() {
        return getProperty(AttributeType.class, ATTRIBUTE_INSTANCES);
    }

    /**
     * Sets the attribute instances to which the information applies.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@code Set<CharSequence>} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-238">GEO-238</a> for more information.</div>
     *
     * @param newValues The new attribute instances.
     */
    public void setAttributeInstances(final Set<? extends AttributeType> newValues) {
        setProperty(newValues, AttributeType.class, ATTRIBUTE_INSTANCES);
    }

    /**
     * Returns the class of information that does not fall into the other categories to which the information applies.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@link InternationalString} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-221">GEO-221</a> for more information.</div>
     *
     * @return Class of information that does not fall into the other categories, or {@code null}.
     */
    @Override
    @XmlElement(name = "other")
    public String getOther() {
        return (property == OTHER) ? (String) value : null;
    }

    /**
     * Sets the class of information that does not fall into the other categories to
     * which the information applies.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The type of this property may be changed to {@link InternationalString} for ISO 19115:2014 conformance.
     * See <a href="http://jira.codehaus.org/browse/GEO-221">GEO-221</a> for more information.</div>
     *
     * @param newValue Other class of information.
     */
    public void setOther(final String newValue) {
        checkWritePermission();
        if (newValue != null || property == OTHER) {
            warningOnOverwrite(OTHER);
            property = OTHER;
            value = newValue;
        }
    }
}
