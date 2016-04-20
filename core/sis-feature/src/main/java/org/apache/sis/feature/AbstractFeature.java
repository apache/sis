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
package org.apache.sis.feature;

import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.internal.util.CheckedArrayList;


/**
 * An instance of a {@linkplain DefaultFeatureType feature type} containing values for a real-world phenomena.
 * Each feature instance can provide values for the following properties:
 *
 * <ul>
 *   <li>{@linkplain AbstractAttribute   Attributes}</li>
 *   <li>{@linkplain AbstractAssociation Associations to other features}</li>
 *   <li>{@linkplain AbstractOperation   Operations}</li>
 * </ul>
 *
 * {@code AbstractFeature} can be instantiated by calls to {@link DefaultFeatureType#newInstance()}.
 *
 * <div class="section">Simple features</div>
 * A feature is said “simple” if it complies to the following conditions:
 * <ul>
 *   <li>the feature allows only attributes and operations (no associations),</li>
 *   <li>the cardinality of all attributes is constrained to [1 … 1].</li>
 * </ul>
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code AbstractFeature} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> serialized objects of this class are not guaranteed to be compatible with future
 *       versions. Serialization should be used only for short term storage or RMI between applications running
 *       the same SIS version.</li>
 * </ul>
 *
 * @author  Travis L. Pinney
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 *
 * @see DefaultFeatureType#newInstance()
 */
public abstract class AbstractFeature implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5637918246427380190L;

    /**
     * Information about the feature (name, characteristics, <i>etc.</i>).
     */
    final DefaultFeatureType type;

    /**
     * Creates a new feature of the given type.
     *
     * @param type Information about the feature (name, characteristics, <i>etc.</i>).
     *
     * @see DefaultFeatureType#newInstance()
     */
    protected AbstractFeature(final DefaultFeatureType type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
    }

    /**
     * Return the {@linkplain #type} name as a non-null string. This is used mostly for formatting error message.
     * This method shall not be invoked when a null name should be considered as an error.
     */
    final String getName() {
        return String.valueOf(type.getName());
    }

    /**
     * Returns information about the feature (name, characteristics, <i>etc.</i>).
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.FeatureType}. This change is pending GeoAPI revision.</div>
     *
     * @return Information about the feature.
     */
    public DefaultFeatureType getType() {
        return type;
    }

    /**
     * Returns the property (attribute, feature association or operation result) of the given name.
     * If the property type is a parameterless {@linkplain AbstractOperation operation}, then this
     * method may return the result of {@linkplain AbstractOperation#apply executing} the operation
     * on this feature, at implementation choice.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.Property}. This change is pending GeoAPI revision.</div>
     *
     * <div class="note"><b>Tip:</b> This method returns the property <em>instance</em>. If only the property
     * <em>value</em> is desired, then {@link #getPropertyValue(String)} is preferred since it gives to SIS a
     * chance to avoid the creation of {@link AbstractAttribute} or {@link AbstractAssociation} instances.</div>
     *
     * @param  name The property name.
     * @return The property of the given name (never {@code null}).
     * @throws IllegalArgumentException if the given argument is not a property name of this feature.
     *
     * @see #getPropertyValue(String)
     * @see DefaultFeatureType#getProperty(String)
     */
    public abstract Object getProperty(final String name) throws IllegalArgumentException;

    /**
     * Sets the property (attribute or feature association).
     * The given property shall comply to the following conditions:
     *
     * <ul>
     *   <li>It must be non-null.</li>
     *   <li>Its {@linkplain Property#getName() name} shall be the name of the property to set in this feature.</li>
     *   <li>Its type shall be the same instance than the {@linkplain DefaultFeatureType#getProperty(String)
     *       property type} defined by the feature type for the above name.
     *       In other words, the following condition shall hold:</li>
     * </ul>
     *
     * {@preformat java
     *     assert property.getType() == getType().getProperty(property.getName());
     * }
     *
     * <div class="note"><b>Note:</b> This method is useful for storing non-default {@code Attribute} or
     * {@code FeatureAssociation} implementations in this feature. When default implementations are sufficient,
     * the {@link #setPropertyValue(String, Object)} method is preferred.</div>
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the argument may be changed
     * to {@code org.opengis.feature.Property}. This change is pending GeoAPI revision.</div>
     *
     * @param  property The property to set.
     * @throws IllegalArgumentException if the name of the given property is not a property name of this feature.
     * @throws IllegalArgumentException if the value of the given property is not valid.
     * @throws IllegalArgumentException if the property can not be set for another reason.
     *
     * @see #setPropertyValue(String, Object)
     */
    public abstract void setProperty(final Object property) throws IllegalArgumentException;

    /**
     * Wraps the given value in a {@link Property} object. This method is invoked only by
     * {@link #getProperty(String)} when it needs to converts its {@code properties} data.
     *
     * @param  name  The name of the property to create.
     * @param  value The value to wrap.
     * @return A {@code Property} wrapping the given value.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    final Property createProperty(final String name, final Object value) {
        final AbstractIdentifiedType pt = type.getProperty(name);
        if (pt instanceof DefaultAttributeType<?>) {
            return AbstractAttribute.create((DefaultAttributeType<?>) pt, value);
        } else if (pt instanceof DefaultAssociationRole) {
            return AbstractAssociation.create((DefaultAssociationRole) pt, value);
        } else {
            // Should never happen, unless the user gave us some mutable FeatureType.
            throw new CorruptedObjectException(Errors.format(Errors.Keys.UnknownType_1, pt));
        }
    }

    /**
     * Creates a new property initialized to its default value.
     *
     * @param  name The name of the property to create.
     * @return A {@code Property} of the given name.
     * @throws IllegalArgumentException if the given argument is not the name of an attribute or
     *         feature association of this feature.
     */
    final Property createProperty(final String name) throws IllegalArgumentException {
        final AbstractIdentifiedType pt = type.getProperty(name);
        if (pt instanceof DefaultAttributeType<?>) {
            return ((DefaultAttributeType<?>) pt).newInstance();
        } else if (pt instanceof DefaultAssociationRole) {
            return ((DefaultAssociationRole) pt).newInstance();
        } else {
            throw unsupportedPropertyType(pt.getName());
        }
    }

    /**
     * Executes the parameterless operation of the given name and returns its result.
     */
    final Object getOperationResult(final String name) {
        /*
         * The (Operation) cast below should never fail (unless the DefaultFeatureType in not really immutable,
         * which would be a contract violation) because all callers shall ensure that this method is invoked in
         * a context where the following assertion holds.
         */
        assert DefaultFeatureType.OPERATION_INDEX.equals(type.indices().get(name)) : name;
        return ((AbstractOperation) type.getProperty(name)).apply(this, null);
    }

    /**
     * Executes the parameterless operation of the given name and returns the value of its result.
     */
    final Object getOperationValue(final String name) {
        final AbstractOperation operation = (AbstractOperation) type.getProperty(name);
        if (operation instanceof LinkOperation) {
            return getPropertyValue(((LinkOperation) operation).referentName);
        }
        final Object result = operation.apply(this, null);
        if (result instanceof AbstractAttribute<?>) {
            return getAttributeValue((AbstractAttribute<?>) result);
        } else if (result instanceof AbstractAssociation) {
            return getAssociationValue((AbstractAssociation) result);
        } else {
            return null;
        }
    }

    /**
     * Executes the parameterless operation of the given name and sets the value of its result.
     */
    final void setOperationValue(final String name, final Object value) {
        final AbstractOperation operation = (AbstractOperation) type.getProperty(name);
        if (operation instanceof LinkOperation) {
            setPropertyValue(((LinkOperation) operation).referentName, value);
        } else {
            final Object result = operation.apply(this, null);
            if (result instanceof Property) {
                setPropertyValue((Property) result, value);
            } else {
                throw new IllegalStateException(Errors.format(Errors.Keys.CanNotSetPropertyValue_1, name));
            }
        }
    }

    /**
     * Returns the default value to be returned by {@link #getPropertyValue(String)}
     * for the property of the given name.
     *
     * @param  name The name of the property for which to get the default value.
     * @return The default value for the {@code Property} of the given name.
     * @throws IllegalArgumentException if the given argument is not an attribute or association name of this feature.
     */
    final Object getDefaultValue(final String name) throws IllegalArgumentException {
        final AbstractIdentifiedType pt = type.getProperty(name);
        if (pt instanceof DefaultAttributeType<?>) {
            return getDefaultValue((DefaultAttributeType<?>) pt);
        } else if (pt instanceof DefaultAssociationRole) {
            final int maximumOccurs = ((DefaultAssociationRole) pt).getMaximumOccurs();
            return maximumOccurs > 1 ? Collections.EMPTY_LIST : null;       // No default value for associations.
        } else {
            throw unsupportedPropertyType(pt.getName());
        }
    }

    /**
     * Returns the default value to be returned by {@link #getPropertyValue(String)} for the given attribute type.
     */
    private static <V> Object getDefaultValue(final DefaultAttributeType<V> attribute) {
        final V defaultValue = attribute.getDefaultValue();
        if (Field.isSingleton(attribute.getMaximumOccurs())) {
            return defaultValue;
        } else {
            // Following is for compliance with getPropertyValue(String) method contract - see its javadoc.
            return (defaultValue != null) ? Collections.singletonList(defaultValue) : Collections.emptyList();
        }
    }

    /**
     * Returns the value for the property of the given name.
     * This convenience method is equivalent to invoking {@link #getProperty(String)} for the given name,
     * then to perform one of the following actions depending on the property type and the cardinality:
     *
     * <table class="sis">
     *   <caption>Class of returned value</caption>
     *   <tr><th>Property type</th>                  <th>max. occurs</th> <th>Method invoked</th>                         <th>Return type</th></tr>
     *   <tr><td>{@link AttributeType}</td>          <td>0 or 1</td>      <td>{@link Attribute#getValue()}</td>           <td>{@link Object}</td></tr>
     *   <tr><td>{@code AttributeType}</td>          <td>2 or more</td>   <td>{@link Attribute#getValues()}</td>          <td>{@code Collection<?>}</td></tr>
     *   <tr><td>{@link FeatureAssociationRole}</td> <td>0 or 1</td>      <td>{@link FeatureAssociation#getValue()}</td>  <td>{@link Feature}</td></tr>
     *   <tr><td>{@code FeatureAssociationRole}</td> <td>2 or more</td>   <td>{@link FeatureAssociation#getValues()}</td> <td>{@code Collection<Feature>}</td></tr>
     * </table>
     *
     * <div class="note"><b>Note:</b> “max. occurs” is the {@linkplain DefaultAttributeType#getMaximumOccurs() maximum
     * number of occurrences} and does not depend on the actual number of values. If an attribute allows more than one
     * value, then this method will always return a collection for that attribute even if the collection is empty.</div>
     *
     * @param  name The property name.
     * @return The value for the given property, or {@code null} if none.
     * @throws IllegalArgumentException if the given argument is not an attribute or association name of this feature.
     *
     * @see AbstractAttribute#getValue()
     */
    public abstract Object getPropertyValue(final String name) throws IllegalArgumentException;

    /**
     * Sets the value for the property of the given name.
     *
     * <div class="section">Validation</div>
     * The amount of validation performed by this method is implementation dependent.
     * Usually, only the most basic constraints are verified. This is so for performance reasons
     * and also because some rules may be temporarily broken while constructing a feature.
     * A more exhaustive verification can be performed by invoking the {@link #quality()} method.
     *
     * @param  name  The attribute name.
     * @param  value The new value for the given attribute (may be {@code null}).
     * @throws IllegalArgumentException if the given name is not an attribute or association name of this feature.
     * @throws ClassCastException if the value is not assignable to the expected value class.
     * @throws IllegalArgumentException if the given value is not valid for a reason other than its type.
     *
     * @see AbstractAttribute#setValue(Object)
     */
    public abstract void setPropertyValue(final String name, final Object value) throws IllegalArgumentException;

    /**
     * Returns the value of the given attribute, as a singleton or as a collection depending
     * on the maximum number of occurrences.
     */
    static Object getAttributeValue(final AbstractAttribute<?> property) {
        return Field.isSingleton(property.getType().getMaximumOccurs()) ? property.getValue() : property.getValues();
    }

    /**
     * Returns the value of the given association, as a singleton or as a collection depending
     * on the maximum number of occurrences.
     */
    static Object getAssociationValue(final AbstractAssociation property) {
        return Field.isSingleton(property.getRole().getMaximumOccurs()) ? property.getValue() : property.getValues();
    }

    /**
     * Sets the value of the given property, with some minimal checks.
     */
    static void setPropertyValue(final Property property, final Object value) {
        if (property instanceof AbstractAttribute<?>) {
            setAttributeValue((AbstractAttribute<?>) property, value);
        } else if (property instanceof AbstractAssociation) {
            setAssociationValue((AbstractAssociation) property, value);
        } else {
            throw unsupportedPropertyType(property.getName());
        }
    }

    /**
     * Sets the attribute value after verification of its type. This method is invoked only for checking
     * that we are not violating the Java parameterized type contract. For a more exhaustive validation,
     * use {@link Validator} instead.
     */
    @SuppressWarnings("unchecked")
    private static <V> void setAttributeValue(final AbstractAttribute<V> attribute, final Object value) {
        if (value != null) {
            final DefaultAttributeType<V> pt = attribute.getType();
            final Class<?> base = pt.getValueClass();
            if (!base.isInstance(value)) {
                Object element = value;
                if (value instanceof Collection<?>) {
                    /*
                     * If the given value is a collection, verify the class of all values
                     * before to delegate to Attribute.setValues(Collection<? extends V>).
                     */
                    final Iterator<?> it = ((Collection<?>) value).iterator();
                    do if (!it.hasNext()) {
                        ((AbstractAttribute) attribute).setValues((Collection) value);
                        return;
                    } while ((element = it.next()) == null || base.isInstance(element));
                    // Found an illegal value. Exeption is thrown below.
                }
                throw illegalValueClass(attribute.getName(), element); // 'element' can not be null here.
            }
        }
        ((AbstractAttribute) attribute).setValue(value);
    }

    /**
     * Sets the association value after verification of its type.
     * For a more exhaustive validation, use {@link Validator} instead.
     */
    @SuppressWarnings("unchecked")
    private static void setAssociationValue(final AbstractAssociation association, final Object value) {
        if (value != null) {
            final DefaultAssociationRole role = association.getRole();
            final DefaultFeatureType base = role.getValueType();
            if (value instanceof AbstractFeature) {
                final DefaultFeatureType actual = ((AbstractFeature) value).getType();
                if (base != actual && !DefaultFeatureType.maybeAssignableFrom(base, actual)) {
                    throw illegalPropertyType(role.getName(), actual.getName());
                }
            } else if (value instanceof Collection<?>) {
                verifyAssociationValues(role, (Collection<?>) value);
                association.setValues((Collection<? extends AbstractFeature>) value);
                return; // Skip the setter at the end of this method.
            } else {
                throw illegalValueClass(association.getName(), value);
            }
        }
        association.setValue((AbstractFeature) value);
    }

    /**
     * Returns {@code true} if the caller can skip the call to {@link #verifyPropertyValue(String, Object)}.
     * This is a slight optimization for the case when we replaced an attribute value by a new value of
     * the same class. Since the type check has already been done by the previous assignation, we do not
     * need to perform it again.
     *
     * @param previous The previous value, or {@code null}.
     * @param value    The new value, or {@code null}.
     * @return         {@code true} if the caller can skip the verification performed by {@code verifyPropertyValue}.
     */
    static boolean canSkipVerification(final Object previous, final Object value) {
        if (previous != null) {
            if (value == null) {
                return true;
            }
            if (previous.getClass() == value.getClass() && !(value instanceof AbstractFeature)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies if the given property can be assigned to this feature.
     *
     * @param name Shall be {@code property.getName().toString()}.
     * @param property The property to verify.
     */
    final void verifyPropertyType(final String name, final Property property) {
        final AbstractIdentifiedType pt, base = type.getProperty(name);
        if (property instanceof AbstractAttribute<?>) {
            pt = ((AbstractAttribute<?>) property).getType();
        } else if (property instanceof AbstractAssociation) {
            pt = ((AbstractAssociation) property).getRole();
        } else {
            throw illegalPropertyType(base.getName(), property.getClass());
        }
        if (pt != base) {
            if (base == null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, getName(), name));
            } else {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedPropertyType_1, name));
            }
        }
    }

    /**
     * Verifies the validity of the given value for the property of the given name, then returns the value
     * to store. The returned value is usually the same than the given one, except in the case of collections.
     */
    final Object verifyPropertyValue(final String name, final Object value) {
        final AbstractIdentifiedType pt = type.getProperty(name);
        if (pt instanceof DefaultAttributeType<?>) {
            if (value != null) {
                return verifyAttributeValue((DefaultAttributeType<?>) pt, value);
            }
        } else if (pt instanceof DefaultAssociationRole) {
            if (value != null) {
                return verifyAssociationValue((DefaultAssociationRole) pt, value);
            }
        } else {
            throw unsupportedPropertyType(pt.getName());
        }
        return value;
    }

    /**
     * Verifies the validity of the given attribute value, and returns the value to store in the feature.
     * An attribute:
     * <ul>
     *   <li>May be a singleton,  in which case the value class is verified.</li>
     *   <li>May be a collection, in which case the class each elements in the collection is verified.</li>
     * </ul>
     *
     * @param value The value, which shall be non-null.
     */
    private static <T> Object verifyAttributeValue(final DefaultAttributeType<T> type, final Object value) {
        final Class<T> valueClass = type.getValueClass();
        final boolean isSingleton = Field.isSingleton(type.getMaximumOccurs());
        if (valueClass.isInstance(value)) {
            return isSingleton ? value : singletonList(valueClass, type.getMinimumOccurs(), value);
        } else if (!isSingleton && value instanceof Collection<?>) {
            return CheckedArrayList.castOrCopy((Collection<?>) value, valueClass);
        } else {
            throw illegalValueClass(type.getName(), value);
        }
    }

    /**
     * Verifies the validity of the given association value, and returns the value to store in the feature.
     * An association:
     * <ul>
     *   <li>May be a singleton,  in which case the feature type is verified.</li>
     *   <li>May be a collection, in which case the class each elements in the collection is verified.</li>
     * </ul>
     *
     * @param value The value, which shall be non-null.
     */
    private static Object verifyAssociationValue(final DefaultAssociationRole role, final Object value) {
        final boolean isSingleton = Field.isSingleton(role.getMaximumOccurs());
        if (value instanceof AbstractFeature) {
            /*
             * If the user gave us a single value, first verify its validity.
             * Then wrap it in a list of 1 element if this property is multi-valued.
             */
            final DefaultFeatureType valueType = ((AbstractFeature) value).getType();
            final DefaultFeatureType base = role.getValueType();
            if (base == valueType || DefaultFeatureType.maybeAssignableFrom(base, valueType)) {
                return isSingleton ? value : singletonList(AbstractFeature.class, role.getMinimumOccurs(), value);
            } else {
                throw illegalPropertyType(role.getName(), valueType.getName());
            }
        } else if (!isSingleton && value instanceof Collection<?>) {
            verifyAssociationValues(role, (Collection<?>) value);
            return CheckedArrayList.castOrCopy((Collection<?>) value, AbstractFeature.class);
        } else {
            throw illegalValueClass(role.getName(), value);
        }
    }

    /**
     * Verifies if all values in the given collection are valid instances of feature for the given association role.
     */
    private static void verifyAssociationValues(final DefaultAssociationRole role, final Collection<?> values) {
        final DefaultFeatureType base = role.getValueType();
        int index = 0;
        for (final Object value : values) {
            ArgumentChecks.ensureNonNullElement("values", index, value);
            if (!(value instanceof AbstractFeature)) {
                throw illegalValueClass(role.getName(), value);
            }
            final DefaultFeatureType type = ((AbstractFeature) value).getType();
            if (base != type && !DefaultFeatureType.maybeAssignableFrom(base, type)) {
                throw illegalPropertyType(role.getName(), type.getName());
            }
            index++;
        }
    }

    /**
     * Creates a collection which will initially contain only the given value.
     * At the difference of {@link Collections#singletonList(Object)}, this method returns a modifiable list.
     */
    @SuppressWarnings("unchecked")
    private static <V> Collection<V> singletonList(final Class<V> valueClass, final int minimumOccurs, final Object value) {
        final CheckedArrayList<V> values = new CheckedArrayList<V>(valueClass, Math.max(minimumOccurs, 4));
        values.add((V) value); // Type will be checked by CheckedArrayList.
        return values;
    }

    /**
     * Returns the exception for a property type which is neither an attribute or an association.
     * This method is invoked after a {@link PropertyType} has been found for the user-supplied name,
     * but that property can not be stored in or extracted from a {@link Property} instance.
     */
    static IllegalArgumentException unsupportedPropertyType(final GenericName name) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.CanNotInstantiate_1, name));
    }

    /**
     * Returns the exception for a property value of wrong Java class.
     *
     * @param value The value, which shall be non-null.
     */
    private static ClassCastException illegalValueClass(final GenericName name, final Object value) {
        return new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyValueClass_2, name, value.getClass()));
    }

    /**
     * Returns the exception for a property value (usually a feature) of wrong type.
     */
    private static IllegalArgumentException illegalPropertyType(final GenericName name, final Object value) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyValueClass_2, name, value));
    }

    /**
     * Evaluates the quality of this feature at this method invocation time. The data quality reports
     * may include information about whether the property values met the constraints defined by the
     * property types, or any other criterion at implementation choice.
     *
     * <p>The default implementation reports data quality with at least the following information:</p>
     * <ul>
     *   <li>
     *     The {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getScope() scope}
     *     {@linkplain org.apache.sis.metadata.iso.quality.DefaultScope#getLevel() level} is set to
     *     {@link org.opengis.metadata.maintenance.ScopeCode#FEATURE}.
     *   </li><li>
     *     The {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getReports() reports} list contains
     *     at most one {@linkplain org.apache.sis.metadata.iso.quality.DefaultDomainConsistency domain consistency}
     *     element per property. Implementations are free to omit element for properties having nothing to report.
     *   </li><li>
     *     Each report may have one or more {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult
     *     conformance result}, as documented on {@link AbstractAttribute#quality()} javadoc.
     *   </li>
     * </ul>
     *
     * This feature is valid if this method does not report any
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult conformance result} having a
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult#pass() pass} value of {@code false}.
     *
     * <div class="note"><b>Example:</b> given a feature with an attribute named “population”.
     * If this attribute is mandatory ([1 … 1] cardinality) but no value has been assigned to it,
     * then this {@code quality()} method will return the following data quality report:
     *
     * {@preformat text
     *   Data quality
     *     ├─Scope
     *     │   └─Level………………………………………………… Feature
     *     └─Report
     *         ├─Measure identification
     *         │   └─Code………………………………………… population
     *         ├─Evaluation method type…… Direct internal
     *         └─Result
     *             ├─Explanation……………………… Missing value for “population” property.
     *             └─Pass………………………………………… false
     * }
     * </div>
     *
     * @return Reports on all constraint violations found.
     *
     * @see AbstractAttribute#quality()
     * @see AbstractAssociation#quality()
     */
    public DataQuality quality() {
        final Validator v = new Validator(ScopeCode.FEATURE);
        v.validate(type, this);
        return v.quality;
    }

    /**
     * Formats this feature in a tabular format.
     *
     * @return A string representation of this feature in a tabular format.
     *
     * @see FeatureFormat
     */
    @Override
    public String toString() {
        return FeatureFormat.sharedFormat(this);
    }
}
