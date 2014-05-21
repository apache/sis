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

import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CorruptedObjectException;


/**
 * An instance of a {@linkplain DefaultFeatureType feature type} containing values for a real-world phenomena.
 * Each feature instance can provide values for the following properties:
 *
 * <ul>
 *   <li>{@linkplain DefaultAttribute   Attributes}</li>
 *   <li>{@linkplain DefaultAssociation Associations to other features}</li>
 *   <li>{@linkplain DefaultOperation   Operations}</li>
 * </ul>
 *
 * {@code AbstractFeature} can be instantiated by calls to {@link DefaultFeatureType#newInstance()}.
 *
 * {@section Simple features}
 * A feature is said “simple” if it complies to the following conditions:
 * <ul>
 *   <li>the feature allows only attributes and operations (no associations),</li>
 *   <li>the cardinality of all attributes is constrained to [1 … 1].</li>
 * </ul>
 *
 * {@section Limitations}
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
 * @version 0.5
 * @module
 *
 * @see DefaultFeatureType#newInstance()
 */
public abstract class AbstractFeature implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5637918246427380190L;

    /**
     * Information about the feature (name, characteristics, <i>etc.</i>).
     */
    private final DefaultFeatureType type;

    /**
     * Creates a new feature of the given type.
     *
     * @param type Information about the feature (name, characteristics, <i>etc.</i>).
     */
    public AbstractFeature(final DefaultFeatureType type) {
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
     * Returns the type for the property of the given name.
     *
     * @param  name The property name.
     * @return The type for the property of the given name (never {@code null}).
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    final PropertyType getPropertyType(final String name) throws IllegalArgumentException {
        final PropertyType pt = type.getProperty(name);
        if (pt != null) {
            return pt;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, getName(), name));
    }

    /**
     * Returns the property (attribute, operation or association) of the given name.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.Property}. This change is pending GeoAPI revision.</div>
     *
     * @param  name The property name.
     * @return The property of the given name.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    public abstract Object getProperty(final String name) throws IllegalArgumentException;

    /**
     * Wraps the given value in a {@link Property} object. This method is invoked only by
     * {@link #getProperty(String)} when it needs to converts its {@code properties} data.
     *
     * @param  name  The name of the property to create.
     * @param  value The value to wrap.
     * @return A {@code Property} wrapping the given value.
     */
    final Property createProperty(final String name, final Object value) {
        final PropertyType pt = getPropertyType(name);
        if (pt instanceof DefaultAttributeType<?>) {
            return new DefaultAttribute<>((DefaultAttributeType<?>) pt, value);
        } else if (pt instanceof DefaultAssociationRole) {
            return new DefaultAssociation((DefaultAssociationRole) pt, (AbstractFeature) value);
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
     * @throws IllegalArgumentException If the given argument is not an attribute or association name of this feature.
     */
    final Property createProperty(final String name) throws IllegalArgumentException {
        final PropertyType pt = getPropertyType(name);
        if (pt instanceof DefaultAttributeType<?>) {
            return new DefaultAttribute<>((DefaultAttributeType<?>) pt);
        } else if (pt instanceof DefaultAssociationRole) {
            return new DefaultAssociation((DefaultAssociationRole) pt);
        } else {
            throw new IllegalArgumentException(unsupportedPropertyType(pt.getName()));
        }
    }

    /**
     * Returns the default value for the property of the given name.
     *
     * @param  name The name of the property for which to get the default value.
     * @return The default value for the {@code Property} of the given name.
     * @throws IllegalArgumentException If the given argument is not an attribute or association name of this feature.
     */
    final Object getDefaultValue(final String name) throws IllegalArgumentException {
        final PropertyType pt = getPropertyType(name);
        if (pt instanceof DefaultAttributeType<?>) {
            return ((DefaultAttributeType<?>) pt).getDefaultValue();
        } else if (pt instanceof DefaultAssociationRole) {
            return null; // No default value for associations.
        } else {
            throw new IllegalArgumentException(unsupportedPropertyType(pt.getName()));
        }
    }

    /**
     * Returns the value for the property of the given name.
     * This convenience method is equivalent to the following steps:
     *
     * <ul>
     *   <li>Get the property of the given name.</li>
     *   <li>Delegates to {@link DefaultAttribute#getValue()} or {@link DefaultAssociation#getValue()},
     *       depending on the property type.
     * </ul>
     *
     * @param  name The property name.
     * @return The value for the given property, or {@code null} if none.
     * @throws IllegalArgumentException If the given argument is not an attribute or association name of this feature.
     *
     * @see DefaultAttribute#getValue()
     */
    public abstract Object getPropertyValue(final String name) throws IllegalArgumentException;

    /**
     * Sets the value for the property of the given name.
     *
     * {@section Validation}
     * The amount of validation performed by this method is implementation dependent.
     * Usually, only the most basic constraints are verified. This is so for performance reasons
     * and also because some rules may be temporarily broken while constructing a feature.
     * A more exhaustive verification can be performed by invoking the {@link #quality()} method.
     *
     * @param  name  The attribute name.
     * @param  value The new value for the given attribute (may be {@code null}).
     * @throws ClassCastException If the value is not assignable to the expected value class.
     * @throws IllegalArgumentException If the given value can not be assigned for an other reason.
     *
     * @see DefaultAttribute#setValue(Object)
     */
    public abstract void setPropertyValue(final String name, final Object value) throws IllegalArgumentException;

    /**
     * Sets the value of the given property, with some minimal checks.
     */
    static void setPropertyValue(final Property property, final Object value) {
        if (property instanceof DefaultAttribute<?>) {
            setAttributeValue((DefaultAttribute<?>) property, value);
        } else if (property instanceof DefaultAssociation) {
            ArgumentChecks.ensureCanCast("value", AbstractFeature.class, value);
            setAssociationValue((DefaultAssociation) property, (AbstractFeature) value);
        } else {
            throw new IllegalArgumentException(unsupportedPropertyType(property.getName()));
        }
    }

    /**
     * Sets the attribute value after verification of its type. This method is invoked only for checking
     * that we are not violating the Java parameterized type contract. For a more exhaustive validation,
     * use {@link Validator} instead.
     */
    @SuppressWarnings("unchecked")
    private static <T> void setAttributeValue(final DefaultAttribute<T> attribute, final Object value) {
        if (value != null) {
            final DefaultAttributeType<T> pt = attribute.getType();
            final Class<?> base = pt.getValueClass();
            if (!base.isInstance(value)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyClass_2,
                        pt.getName(), value.getClass()));
            }
        }
        ((DefaultAttribute) attribute).setValue(value);
    }

    /**
     * Sets the association value after verification of its type.
     * For a more exhaustive validation, use {@link Validator} instead.
     */
    private static <T> void setAssociationValue(final DefaultAssociation association, final AbstractFeature value) {
        if (value != null) {
            final DefaultAssociationRole pt = association.getRole();
            final DefaultFeatureType base = pt.getValueType();
            final DefaultFeatureType actual = value.getType();
            if (!base.maybeAssignableFrom(actual)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyClass_2,
                        pt.getName(), actual.getName()));
            }
        }
        association.setValue(value);
    }

    /**
     * Verifies the validity of the given value for the property of the given name. If a check failed,
     * returns the exception to throw. Otherwise returns {@code null}. This method does not throw the
     * exception immediately in order to give to the caller a chance to perform cleanup operation first.
     */
    final RuntimeException verifyValueType(final String name, final Object value) {
        final PropertyType pt = getPropertyType(name);
        if (pt instanceof DefaultAttributeType<?>) {
            if (value == null || ((DefaultAttributeType<?>) pt).getValueClass().isInstance(value)) {
                return null;
            }
        } else if (pt instanceof DefaultAssociationRole) {
            if (value == null) {
                return null;
            }
            if (value instanceof AbstractFeature) {
                final DefaultFeatureType valueType = ((AbstractFeature) value).getType();
                if (((DefaultAssociationRole) pt).getValueType().maybeAssignableFrom(valueType)) {
                    return null;
                }
                return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyClass_2,
                        name, valueType.getName()));
            }
        } else {
            return new IllegalArgumentException(unsupportedPropertyType(pt.getName()));
        }
        return new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyClass_2,
                name, value.getClass())); // 'value' should not be null at this point.
    }

    /**
     * Returns the exception message for a property type which neither an attribute or an association.
     */
    static String unsupportedPropertyType(final GenericName name) {
        return Errors.format(Errors.Keys.CanNotInstantiate_1, name);
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
     *     conformance result}, as documented on {@link DefaultAttribute#quality()} javadoc.
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
     * @see DefaultAttribute#quality()
     * @see DefaultAssociation#quality()
     */
    public DataQuality quality() {
        final Validator v = new Validator(ScopeCode.FEATURE);
        for (final String name : type.indices().keySet()) {
            final Property property = (Property) getProperty(name);
            final DataQuality quality;
            if (property instanceof DefaultAttribute<?>) {
                quality = ((DefaultAttribute<?>) property).quality();
            } else if (property instanceof DefaultAssociation) {
                quality = ((DefaultAssociation) property).quality();
            } else {
                continue;
            }
            if (quality != null) { // Should not be null, but let be safe.
                v.quality.getReports().addAll(quality.getReports());
            }
        }
        return v.quality;
    }

    /**
     * Returns a copy of this feature
     * This method clones also all {@linkplain Cloneable cloneable} property instances in this feature,
     * but not necessarily property values. Whether the property values are cloned or not (i.e. whether
     * the clone operation is <cite>deep</cite> or <cite>shallow</cite>) depends on the behavior or
     * property {@code clone()} methods (see for example {@link DefaultAttribute#clone()}).
     *
     * @return A clone of this attribute.
     * @throws CloneNotSupportedException if this feature can not be cloned, typically because
     *         {@code clone()} on a property instance failed.
     */
    @Override
    public AbstractFeature clone() throws CloneNotSupportedException {
        return (AbstractFeature) super.clone();
    }

    /**
     * Returns a hash code value for this feature.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return type.hashCode();
    }

    /**
     * Compares this feature with the given object for equality.
     *
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            return type.equals(((AbstractFeature) obj).type);
        }
        return false;
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
