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

import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.apache.sis.util.Static;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.feature.Resources;


/**
 * Static methods working on features or attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.5
 * @module
 */
public final class Features extends Static {
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
     * @return the attribute type casted to the given value class, or {@code null} if the given type was null.
     * @throws ClassCastException if the given attribute type does not have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <V> DefaultAttributeType<V> cast(final DefaultAttributeType<?> type, final Class<V> valueClass)
            throws ClassCastException
    {
        if (type != null) {
            final Class<?> actual = type.getValueClass();
            /*
             * We require a strict equality - not type.isAssignableFrom(actual) - because in
             * the later case we could have (to be strict) to return a <? extends V> type.
             */
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Resources.format(Resources.Keys.MismatchedValueClass_3,
                        type.getName(), valueClass, actual));
            }
        }
        return (DefaultAttributeType<V>) type;
    }

    /**
     * Casts the given attribute instance to the given parameterized type.
     * An exception is thrown immediately if the given instance does not have the expected
     * {@linkplain DefaultAttributeType#getValueClass() value class}.
     *
     * @param  <V>         the expected value class.
     * @param  attribute   the attribute instance to cast, or {@code null}.
     * @param  valueClass  the expected value class.
     * @return the attribute instance casted to the given value class, or {@code null} if the given instance was null.
     * @throws ClassCastException if the given attribute instance does not have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <V> AbstractAttribute<V> cast(final AbstractAttribute<?> attribute, final Class<V> valueClass)
            throws ClassCastException
    {
        if (attribute != null) {
            final Class<?> actual = attribute.getType().getValueClass();
            /*
             * We require a strict equality - not type.isAssignableFrom(actual) - because in
             * the later case we could have (to be strict) to return a <? extends V> type.
             */
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Resources.format(Resources.Keys.MismatchedValueClass_3,
                        attribute.getName(), valueClass, actual));
            }
        }
        return (AbstractAttribute<V>) attribute;
    }

    /**
     * Returns the name of the type of values that the given property can take.
     * The type of value can be a {@link Class}, a {@code FeatureType}
     * or another {@code PropertyType} depending on given argument:
     *
     * <ul>
     *   <li>If {@code property} is an {@code AttributeType}, then this method gets the
     *       {@linkplain DefaultAttributeType#getValueClass() value class} and
     *       {@linkplain DefaultNameFactory#toTypeName(Class) maps that class to a name}.</li>
     *   <li>If {@code property} is a {@code FeatureAssociationRole}, then this method gets
     *       the name of the {@linkplain DefaultAssociationRole#getValueType() value type}.
     *       This methods can work even if the associated {@code FeatureType} is not yet resolved.</li>
     *   <li>If {@code property} is an {@code Operation}, then this method returns the name of the
     *       {@linkplain AbstractOperation#getResult() result type}.</li>
     * </ul>
     *
     * @param  property  the property for which to get the name of value type.
     * @return the name of value type, or {@code null} if none.
     *
     * @since 0.8
     */
    public static GenericName getValueTypeName(final AbstractIdentifiedType property) {
        if (property instanceof DefaultAssociationRole) {
            // Tested first because this is the main interest for this method.
            return DefaultAssociationRole.getValueTypeName((DefaultAssociationRole) property);
        } else if (property instanceof DefaultAttributeType<?>) {
            final DefaultNameFactory factory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
            return factory.toTypeName(((DefaultAttributeType<?>) property).getValueClass());
        } else if (property instanceof AbstractOperation) {
            final AbstractIdentifiedType result = ((AbstractOperation) property).getResult();
            if (result != null) {
                return result.getName();
            }
        }
        return null;
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
     * @throws IllegalArgumentException if the given feature is non-null and does not pass validation.
     *
     * @since 0.7
     */
    public static void validate(final AbstractFeature feature) throws IllegalArgumentException {
        if (feature != null) {
            final DataQuality quality = feature.quality();
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
