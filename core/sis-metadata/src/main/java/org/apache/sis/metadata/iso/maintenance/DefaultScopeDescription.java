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
import org.opengis.metadata.maintenance.ScopeDescription;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.metadata.ExcludedSet;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Messages;

import static org.apache.sis.internal.jaxb.Context.isMarshalling;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


/**
 * Description of the class of information covered by the information.
 *
 * {@section Relationship between properties}
 * ISO 19115 defines {@code ScopeDescription} as an <cite>union</cite> (in the C/C++ sense):
 * only one of the properties in this class can be set to a non-empty value.
 * Setting any property to a non-empty value discard all the other ones.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
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
    private static final byte ATTRIBUTES=1, FEATURES=2, FEATURE_INSTANCES=3, ATTRIBUTE_INSTANCES=4, DATASET=5, OTHER=6;

    /**
     * The names of the mutually exclusive properties. The index of each name shall be the
     * value of the above {@code byte} constants minus one.
     */
    private static final String[] NAMES = {
        "attributes",
        "features",
        "featureInstances",
        "attributeInstances",
        "dataset",
        "other"
    };

    /**
     * The names of the setter methods, for logging purpose only.
     */
    private static final String[] SETTERS = {
        "setAttributes",
        "setFeatures",
        "setFeatureInstances",
        "setAttributeInstances",
        "setDataset",
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
     *   <li>{@code Set<CharSequence>} for the {@code attributes} property</li>
     *   <li>{@code Set<CharSequence>} for the {@code features} property</li>
     *   <li>{@code Set<CharSequence>} for the {@code featureInstances} property</li>
     *   <li>{@code Set<CharSequence>} for the {@code attributeInstances} property</li>
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
     * following list has precedence: {@linkplain #getAttributes() attributes},
     * {@linkplain #getFeatures() features}, {@linkplain #getFeatureInstances() feature instances},
     * {@linkplain #getAttributeInstances() attribute instances}, {@linkplain #getDataset() dataset}
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
            for (byte i=ATTRIBUTES; i<=OTHER; i++) {
                Collection<? extends CharSequence> props = null;
                Object value = null;
                switch (i) {
                    case ATTRIBUTES:          props = object.getAttributes();         break;
                    case FEATURES:            props = object.getFeatures();           break;
                    case FEATURE_INSTANCES:   props = object.getFeatureInstances();   break;
                    case ATTRIBUTE_INSTANCES: props = object.getAttributeInstances(); break;
                    case DATASET:             value = object.getDataset();            break;
                    case OTHER:               value = object.getOther();              break;
                    default: throw new AssertionError(i);
                }
                if (props != null) {
                    value = copySet(props, CharSequence.class);
                }
                if (value != null) {
                    this.value = value;
                    this.property = i;
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
     * Returns the given value casted to a {@code Set<CharSequence>}.
     */
    @SuppressWarnings("unchecked")
    private static Set<CharSequence> cast(final Object value) {
        assert ((CheckedContainer<?>) value).getElementType() == CharSequence.class;
        return (Set<CharSequence>) value;
    }

    /**
     * Returns the set of properties identified by the {@code code} argument,
     * or an unmodifiable empty set if another value is defined.
     */
    private Set<CharSequence> getProperty(final byte code) {
        final Object value = this.value;
        if (value != null) {
            if (property == code) {
                return cast(value);
            } else if (!(value instanceof Set) || !((Set<?>) value).isEmpty()) {
                return isMarshalling() ? null : new ExcludedSet<>(NAMES[code-1], NAMES[property-1]);
            }
        }
        // Unconditionally create a new set, because the
        // user may hold a reference to the previous one.
        final Set<CharSequence> c = nonNullSet(null, CharSequence.class);
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
    private void setProperty(final Set<? extends CharSequence> newValue, final byte code) {
        Set<CharSequence> c = null;
        if (property == code) {
            c = cast(value);
        } else if (isNullOrEmpty(newValue)) {
            return;
        } else {
            warningOnOverwrite(code);
            property = code;
        }
        value = writeSet(newValue, c, CharSequence.class);
    }

    /**
     * Sends a warning if setting the value for the given property would overwrite an existing property.
     *
     * @param code The property which is going to be set.
     */
    private void warningOnOverwrite(final byte code) {
        if (value != null && property != code) {
            MetadataUtilities.warning(DefaultScopeDescription.class, SETTERS[code-1],
                    Messages.Keys.DiscardedExclusiveProperty_2, NAMES[property-1], NAMES[code-1]);
        }
    }

    /**
     * Returns the attributes to which the information applies.
     *
     * @return Attributes to which the information applies.
     *
     * {@section Conditions}
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     */
    @Override
    public Set<CharSequence> getAttributes() {
        return getProperty(ATTRIBUTES);
    }

    /**
     * Sets the attributes to which the information applies.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * @param newValues The new attributes.
     */
    public void setAttributes(final Set<? extends CharSequence> newValues) {
        setProperty(newValues, ATTRIBUTES);
    }

    /**
     * Returns the features to which the information applies.
     *
     * @return Features to which the information applies.
     *
     * {@section Conditions}
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     */
    @Override
    public Set<CharSequence> getFeatures() {
        return getProperty(FEATURES);
    }

    /**
     * Sets the features to which the information applies.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * @param newValues The new features.
     */
    public void setFeatures(final Set<? extends CharSequence> newValues) {
        setProperty(newValues, FEATURES);
    }

    /**
     * Returns the feature instances to which the information applies.
     *
     * @return Feature instances to which the information applies.
     *
     * {@section Conditions}
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     */
    @Override
    public Set<CharSequence> getFeatureInstances() {
        return getProperty(FEATURE_INSTANCES);
    }

    /**
     * Sets the feature instances to which the information applies.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * @param newValues The new feature instances.
     */
    public void setFeatureInstances(final Set<? extends CharSequence> newValues) {
        setProperty(newValues, FEATURE_INSTANCES);
    }

    /**
     * Returns the attribute instances to which the information applies.
     *
     * @return Attribute instances to which the information applies.
     *
     * {@section Conditions}
     * This method returns a modifiable collection only if no other property is set.
     * Otherwise, this method returns an unmodifiable empty collection.
     */
    @Override
    public Set<CharSequence> getAttributeInstances() {
        return getProperty(ATTRIBUTE_INSTANCES);
    }

    /**
     * Sets the attribute instances to which the information applies.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards all other properties.
     *
     * @param newValues The new attribute instances.
     */
    public void setAttributeInstances(final Set<? extends CharSequence> newValues) {
        setProperty(newValues, ATTRIBUTE_INSTANCES);
    }

    /**
     * Returns the dataset to which the information applies.
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
     * {@section Effect on other properties}
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
     * Returns the class of information that does not fall into the other categories to which the information applies.
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
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
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
