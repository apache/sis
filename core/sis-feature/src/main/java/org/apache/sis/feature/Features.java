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

import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;


/**
 * Static methods working on features or attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.5
 * @version 0.7
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
     * @param  <V>        The expected value class.
     * @param  type       The attribute type to cast, or {@code null}.
     * @param  valueClass The expected value class.
     * @return The attribute type casted to the given value class, or {@code null} if the given type was null.
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
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends V> type.
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.MismatchedValueClass_3,
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
     * @param  <V>        The expected value class.
     * @param  attribute  The attribute instance to cast, or {@code null}.
     * @param  valueClass The expected value class.
     * @return The attribute instance casted to the given value class, or {@code null} if the given instance was null.
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
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends V> type.
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.MismatchedValueClass_3,
                        attribute.getName(), valueClass, actual));
            }
        }
        return (AbstractAttribute<V>) attribute;
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
             * types may require threshold and other informations to be evaluated.
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
