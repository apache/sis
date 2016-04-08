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

import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.InvalidPropertyValueException;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;


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
    public static <V> AttributeType<V> cast(final AttributeType<?> type, final Class<V> valueClass)
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
        return (AttributeType<V>) type;
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
    public static <V> Attribute<V> cast(final Attribute<?> attribute, final Class<V> valueClass)
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
        return (Attribute<V>) attribute;
    }


    /**
     * Validate feature state.
     * <br>
     * This method is a shortcut to loop on feature data quality results.
     * <br>
     * If one ConformanceResult is false then an IllegalArgumentException is throw,
     * otherwise the function return doing nothing.
     *
     * @param feature tested feature.
     * @throws InvalidPropertyValueException if feature do not pass validation
     */
    public static void validate(Feature feature) throws InvalidPropertyValueException {

        //Get data quality of the feature
        final DataQuality quality;
        if(feature instanceof AbstractFeature){
            quality = ((AbstractFeature)feature).quality();
        }else{
            //use default validator
            final Validator v = new Validator(ScopeCode.FEATURE);
            final FeatureType type = feature.getType();
            for (final PropertyType pt : type.getProperties(true)) {
                final Property property = feature.getProperty(pt.getName().toString());
                final DataQuality pq;
                if (property instanceof AbstractAttribute<?>) {
                    pq = ((AbstractAttribute<?>) property).quality();
                } else if (property instanceof AbstractAssociation) {
                    pq = ((AbstractAssociation) property).quality();
                } else {
                    continue;
                }
                if (pq != null) { // Should not be null, but let be safe.
                    v.quality.getReports().addAll(pq.getReports());
                }
            }
            quality = v.quality;
        }

        //loop on quality elements and check conformance results
        boolean valid = true;
        search:
        for(Element element : quality.getReports()){
            for(Result result : element.getResults()){
                //NOTE : other type of result are ignored for now
                // other results may requiere threshold and other informations
                // to be evaluated
                if(result instanceof ConformanceResult){
                    final Boolean pass = ((ConformanceResult)result).pass();
                    if(Boolean.FALSE.equals(pass)){
                        valid = false;
                        break search;
                    }
                }
            }
        }

        if(!valid){
            throw new InvalidPropertyValueException(quality.toString());
        }
    }

}
