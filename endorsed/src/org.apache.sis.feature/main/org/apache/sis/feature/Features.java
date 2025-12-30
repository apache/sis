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

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.IdentityHashMap;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.pending.jdk.JDK16;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.InvalidPropertyValueException;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;


/**
 * Static methods working on features or attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.6
 * @since   0.5
 */
public final class Features {
    /**
     * Do not allow instantiation of this class.
     */
    private Features() {
    }

    /**
     * Casts the given attribute type to the given parameterized type.
     * An exception is thrown immediately if the given type does not have the expected
     * {@linkplain DefaultAttributeType#getValueClass() value class}.
     *
     * @param  <V>         the expected value class.
     * @param  type        the attribute type to cast, or {@code null}.
     * @param  valueClass  the expected value class.
     * @return the attribute type cast to the given value class, or {@code null} if the given type was null.
     * @throws ClassCastException if the given attribute type does not have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <V> AttributeType<V> cast(final AttributeType<?> type, final Class<V> valueClass)
            throws ClassCastException
    {
        if (type != null) {
            final Class<?> actual = type.getValueClass();
            /*
             * We require a strict equality - not type.isAssignableFrom(actual) - because in
             * the latter case we could have (to be strict) to return a <? extends V> type.
             */
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Resources.format(
                        Resources.Keys.MismatchedValueClass_3,
                        type.getName(), valueClass, actual));
            }
        }
        return (AttributeType<V>) type;
    }

    /**
     * Casts the given attribute instance to the given parameterized type.
     * An exception is thrown immediately if the given instance does not have the expected
     * {@linkplain DefaultAttributeType#getValueClass() value class}.
     *
     * @param  <V>         the expected value class.
     * @param  attribute   the attribute instance to cast, or {@code null}.
     * @param  valueClass  the expected value class.
     * @return the attribute instance cast to the given value class, or {@code null} if the given instance was null.
     * @throws ClassCastException if the given attribute instance does not have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <V> Attribute<V> cast(final Attribute<?> attribute, final Class<V> valueClass)
            throws ClassCastException
    {
        if (attribute != null) {
            final Class<?> actual = attribute.getType().getValueClass();
            /*
             * We require a strict equality - not type.isAssignableFrom(actual) - because in
             * the latter case we could have (to be strict) to return a <? extends V> type.
             */
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Resources.format(
                        Resources.Keys.MismatchedValueClass_3,
                        attribute.getName(), valueClass, actual));
            }
        }
        return (Attribute<V>) attribute;
    }

    /**
     * Returns the given type as an {@link AttributeType} by casting if possible, or by getting the result type
     * of an operation. More specifically this method returns the first of the following types which apply:
     *
     * <ul>
     *   <li>If the given type is an instance of {@link AttributeType}, then it is returned as-is.</li>
     *   <li>If the given type is an instance of {@link Operation} and the {@linkplain Operation#getResult()
     *       result type} is an {@link AttributeType}, then that result type is returned.</li>
     *   <li>If the given type is an instance of {@link Operation} and the {@linkplain Operation#getResult()
     *       result type} is another operation, then the above check is performed recursively.</li>
     * </ul>
     *
     * @param  type  the data type to express as an attribute type, or {@code null}.
     * @return the attribute type, or empty if this method cannot find any.
     *
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public static Optional<AttributeType<?>> toAttribute(IdentifiedType type) {
        return toIdentifiedType(type, (Class) AttributeType.class);
    }

    /**
     * Returns the given type as a {@link FeatureAssociationRole} by casting if possible, or by getting the result type
     * of an operation. More specifically this method returns the first of the following types which apply:
     *
     * <ul>
     *   <li>If the given type is an instance of {@link FeatureAssociationRole}, then it is returned as-is.</li>
     *   <li>If the given type is an instance of {@link Operation} and the {@linkplain Operation#getResult()
     *       result type} is an {@link FeatureAssociationRole}, then that result type is returned.</li>
     *   <li>If the given type is an instance of {@link Operation} and the {@linkplain Operation#getResult()
     *       result type} is another operation, then the above check is performed recursively.</li>
     * </ul>
     *
     * @param  type  the data type to express as an attribute type, or {@code null}.
     * @return the association role, or empty if this method cannot find any.
     *
     * @since 1.4
     */
    public static Optional<FeatureAssociationRole> toAssociation(IdentifiedType type) {
        return toIdentifiedType(type, FeatureAssociationRole.class);
    }

    /**
     * Implementation of {@link #toAttribute(IdentifiedType)} and {@link #toAssociation(IdentifiedType)}.
     */
    private static <T> Optional<T> toIdentifiedType(IdentifiedType type, final Class<T> target) {
        if (!target.isInstance(type)) {
            if (!(type instanceof Operation)) {
                return Optional.empty();
            }
            type = ((Operation) type).getResult();
            if (!target.isInstance(type)) {
                if (!(type instanceof Operation)) {
                    return Optional.empty();
                }
                /*
                 * Operation returns another operation. This case should be rare and should never
                 * contain a cycle. However, given that the consequence of an infinite cycle here
                 * would be thread freeze, we check as a safety.
                 */
                final var done = new IdentityHashMap<IdentifiedType,Boolean>(4);
                while (!target.isInstance(type = ((Operation) type).getResult())) {
                    if (!(type instanceof Operation) || done.put(type, Boolean.TRUE) != null) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.of(target.cast(type));
    }

    /**
     * Finds a feature type common to all given types, or returns {@code null} if none is found.
     * The return value is either one of the given types, or a parent common to all types.
     * A feature <var>F</var> is considered a common parent if <code>F.{@link DefaultFeatureType#isAssignableFrom
     * isAssignableFrom}(type)</code> returns {@code true} for all elements <var>type</var> in the given array.
     *
     * @param  types  types for which to find a common type, or {@code null}.
     * @return a feature type which is assignable from all given types, or {@code null} if none.
     *
     * @see FeatureType#isAssignableFrom(FeatureType)
     *
     * @since 1.0
     */
    @OptionalCandidate
    public static FeatureType findCommonParent(final Iterable<? extends FeatureType> types) {
        return (types != null) ? CommonParentFinder.select(types) : null;
    }

    /**
     * Returns the name of all properties (including inherited properties) of the given feature type.
     *
     * @param  feature  the feature type from which to get the name of all properties.
     * @return name of all properties of the specified feature type.
     *
     * @since 1.6
     */
    public static List<String> getPropertyNames(final FeatureType feature) {
        return JDK16.toList(feature.getProperties(true).stream().map((p) -> p.getName().toString()));
    }

    /**
     * Returns the type of values provided by the given property. For {@linkplain AttributeType attributes}
     * (which is the most common case), the value type is given by {@link AttributeType#getValueClass()}.
     * For {@linkplain FeatureAssociationRole feature associations}, the value type is {@link Feature}.
     * For {@linkplain Operation operations}, the value type is determined recursively from the
     * {@linkplain Operation#getResult() operation result}.
     * If the value type cannot be determined, then this method returns {@code null}.
     *
     * @param  type  the property for which to get the type of values, or {@code null}.
     * @return the type of values provided by the given property, or {@code null} if unknown.
     *
     * @see AttributeType#getValueClass()
     *
     * @since 1.0
     */
    @OptionalCandidate
    public static Class<?> getValueClass(PropertyType type) {
        while (type instanceof Operation) {
            final IdentifiedType result = ((Operation) type).getResult();
            if (result != type && result instanceof PropertyType) {
                type = (PropertyType) result;
            } else if (result instanceof FeatureType) {
                return Feature.class;
            } else {
                break;
            }
        }
        if (type instanceof AttributeType<?>) {
            return ((AttributeType<?>) type).getValueClass();
        } else if (type instanceof FeatureAssociationRole) {
            return Feature.class;
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the type of values that the given property can take.
     * The type of value can be a {@link Class}, a {@link org.opengis.feature.FeatureType}
     * or another {@code PropertyType} depending on given argument:
     *
     * <ul>
     *   <li>If {@code property} is an {@link AttributeType}, then this method gets the
     *       {@linkplain DefaultAttributeType#getValueClass() value class} and
     *       {@linkplain DefaultNameFactory#toTypeName(Class) maps that class to a name}.</li>
     *   <li>If {@code property} is a {@link FeatureAssociationRole}, then this method gets
     *       the name of the {@linkplain DefaultAssociationRole#getValueType() value type}.
     *       This methods can work even if the associated {@code FeatureType} is not yet resolved.</li>
     *   <li>If {@code property} is an {@link Operation}, then this method returns the name of the
     *       {@linkplain AbstractOperation#getResult() result type}.</li>
     * </ul>
     *
     * @param  property  the property for which to get the name of value type.
     * @return the name of value type, or {@code null} if none.
     *
     * @since 0.8
     */
    @OptionalCandidate
    public static GenericName getValueTypeName(final PropertyType property) {
        if (property instanceof FeatureAssociationRole) {
            // Tested first because this is the main interest for this method.
            return DefaultAssociationRole.getValueTypeName((FeatureAssociationRole) property);
        } else if (property instanceof AttributeType<?>) {
            return Names.createTypeName(((AttributeType<?>) property).getValueClass());
        } else if (property instanceof Operation) {
            final IdentifiedType result = ((Operation) property).getResult();
            if (result != null) {
                return result.getName();
            }
        }
        return null;
    }

    /**
     * If the given property is a link, returns the name of the referenced property.
     * A link is an operation created by a call to {@link FeatureOperations#link(Map, PropertyType)},
     * in which case the value returned by this method is the name of the {@link PropertyType} argument
     * which has been given to that {@code link(…)} method.
     *
     * @param  property  the property to test, or {@code null} if none.
     * @return the referenced property name if {@code property} is a link, or an empty value otherwise.
     *
     * @see FeatureOperations#link(Map, PropertyType)
     *
     * @since 1.1
     */
    public static Optional<String> getLinkTarget(final PropertyType property) {
        if (property instanceof LinkOperation) {
            return Optional.of(((LinkOperation) property).referentName);
        }
        return Optional.empty();
    }

    /**
     * If the given property is a link or a compound key, returns the name of the referenced properties.
     * This method is similar to {@link #getLinkTarget(PropertyType)}, except that it recognizes also
     * the operations created by {@link FeatureOperations#compound FeatureOperations.compound(…)}.
     *
     * @param  property  the property to test, or {@code null} if none.
     * @return the referenced property names if {@code property} is a link or a compound key,
     *         or an empty list otherwise.
     *
     * @see FeatureOperations#compound(Map, String, String, String, PropertyType...)
     *
     * @since 1.5
     */
    public static List<String> getLinkTargets(final PropertyType property) {
        return getLinkTarget(property).map(List::of).orElseGet(() -> {
            if (property instanceof StringJoinOperation) {
                return ((StringJoinOperation) property).getAttributeNames();
            }
            return List.of();
        });
    }

    /**
     * Ensures that all characteristics and property values in the given feature are valid.
     * An attribute is valid if it contains a number of values between the
     * {@linkplain DefaultAttributeType#getMinimumOccurs() minimum} and
     * {@linkplain DefaultAttributeType#getMaximumOccurs() maximum number of occurrences} (inclusive),
     * all values are instances of the expected {@linkplain DefaultAttributeType#getValueClass() value class},
     * and the attribute is compliant with any other restriction that the implementation may add.
     *
     * <p>This method gets a quality report as documented in the {@link AbstractFeature#quality()} method
     * and verifies that all {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult#pass()
     * conformance tests pass}. If at least one {@code ConformanceResult.pass} attribute is false, then an
     * {@code InvalidPropertyValueException} is thrown. Otherwise this method returns doing nothing.
     *
     * @param  feature  the feature to validate, or {@code null}.
     * @throws InvalidPropertyValueException if the given feature is non-null and does not pass validation.
     *
     * @since 0.7
     */
    public static void validate(final Feature feature) throws InvalidPropertyValueException {
        if (feature != null) {
            /*
             * Delegate to AbstractFeature.quality() if possible because the user may have overridden the method.
             * Otherwise fallback on the same code as AbstractFeature.quality() default implementation.
             */
            final DataQuality quality;
            if (feature instanceof AbstractFeature) {
                quality = ((AbstractFeature) feature).quality();
            } else {
                final var validator = new Validator(ScopeCode.FEATURE);
                validator.validate(feature.getType(), feature);
                quality = validator.quality;
            }
            /*
             * Loop on quality elements and check conformance results.
             * NOTE: other types of result are ignored for now, since those other
             * types may require threshold and other information to be evaluated.
             */
            for (Element element : quality.getReports()) {
                for (Result result : element.getResults()) {
                    if (result instanceof ConformanceResult) {
                        if (Boolean.FALSE.equals(((ConformanceResult) result).pass())) {
                            final InternationalString message = ((ConformanceResult) result).getExplanation();
                            if (message != null) {
                                throw new InvalidFeatureException(message);
                            }
                        }
                    }
                }
            }
        }
    }
}
