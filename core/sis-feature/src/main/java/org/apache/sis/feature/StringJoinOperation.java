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
import java.util.Set;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.InvalidPropertyValueException;
import org.opengis.feature.Property;


/**
 * An operation concatenating the string representations of the values of multiple properties.
 * This operation can be used for creating a <cite>compound key</cite> as a {@link String}
 * that consists of two or more attribute values that uniquely identify a feature instance.
 *
 * <p>This operation supports both reading and writing. When setting a value on the attribute
 * created by this operation, the value will be split and forwarded to each single attribute.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see <a href="https://en.wikipedia.org/wiki/Compound_key">Compound key on Wikipedia</a>
 */
final class StringJoinOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2303047827010821381L;

    /**
     * The parameter descriptor for the "Aggregate" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = LinkOperation.parameters("StringJoin", 1);

    /**
     * The name of the properties from which to get the values to concatenate.
     */
    private final String[] propertyNames;

    /**
     * The property names as an unmodifiable set, created when first needed.
     */
    private transient Set<String> dependencies;

    /**
     * The type of the result returned by the string concatenation operation.
     */
    private final AttributeType<String> resultType;

    /**
     * The characters to use at the beginning of the concatenated string, or an empty string if none.
     */
    private final String prefix;

    /**
     * The characters to use at the end of the concatenated string, or an empty string if none.
     */
    private final String suffix;

    /**
     * The characters to use a delimiter between each single attribute value.
     */
    private final String delimiter;

    /**
     * Creates a new operation for string concatenations using the given prefix, suffix and delimeter.
     * It is caller's responsibility to ensure that {@code delimiter} and {@code singleProperties} are not null.
     * This private constructor does not verify that condition on the assumption that the public API did.
     */
    StringJoinOperation(final Map<String,?> identification, final String delimiter,
            final String prefix, final String suffix, final GenericName[] singleProperties)
    {
        super(identification);
        propertyNames = new String[singleProperties.length];
        for (int i=0; i < singleProperties.length; i++) {
            ArgumentChecks.ensureNonNullElement("singleProperties", i, singleProperties);
            propertyNames[i] = singleProperties[i].toString();
        }
        resultType = new DefaultAttributeType<>(resultIdentification(identification), String.class, 1, 1, null);
        this.delimiter = delimiter;
        this.prefix = (prefix == null) ? "" : prefix;
        this.suffix = (suffix == null) ? "" : suffix;
    }

    /**
     * Concatenate operation do not require any parameter.
     *
     * @return empty parameter group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Concatenate operation generates a {@link String} result.
     *
     * @return concatenated string type.
     */
    @Override
    public IdentifiedType getResult() {
        return resultType;
    }

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Set<String> getDependencies() {
        if (dependencies == null) {
            dependencies = CollectionsExt.immutableSet(true, propertyNames);
        }
        return dependencies;
    }

    /**
     * Returns the concatenation of property values of the given feature.
     *
     * @param  feature     the feature on which to execute the operation.
     * @param  parameters  ignored (can be {@code null}).
     * @return the concatenation of feature property values.
     */
    @Override
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        ArgumentChecks.ensureNonNull("feature", feature);
        return new Result(feature);
    }

    /**
     * The attributes that contains the result of concatenating the string representation of other attributes.
     * Value is calculated each time it is accessed.
     */
    private final class Result extends AbstractAttribute<String> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -8435975199763452547L;

        /**
         * The feature specified to the {@link StringJoinOperation#apply(Feature, ParameterValueGroup)} method.
         */
        private final Feature feature;

        /**
         * Creates a new attribute for the given feature.
         */
        Result(final Feature feature) {
            super(resultType);
            this.feature = feature;
        }

        /**
         * Creates a string which is the concatenation of attribute values of all properties
         * specified to the {@link StringJoinOperation} constructor.
         */
        @Override
        public String getValue() {
            final StringBuilder sb = new StringBuilder();
            String sep = prefix;
            for (final String name : propertyNames) {
                final Object value = feature.getPropertyValue(name);
                sb.append(sep);
                if (value != null) {
                    sb.append(value);
                }
                sep = delimiter;
            }
            return sb.append(suffix).toString();
        }

        /**
         * Given a concatenated string as produced by {@link #getValue()}, separates the components around
         * the separator and forward the values to the original attributes.
         */
        @Override
        public void setValue(String value) {
            //check prefix
            if (!value.startsWith(prefix)) {
                throw new InvalidPropertyValueException("Unvalid string, does not start with "+prefix);
            }
            if (!value.endsWith(suffix)) {
                throw new InvalidPropertyValueException("Unvalid string, does not end with "+suffix);
            }

            //split values, we don't use the regex split to avoid possible reserverd regex characters
            final Object[] values = new Object[propertyNames.length];
            int i = 0;
            int offset = 0;
            //remove prefix and suffix
            value = value.substring(prefix.length(), value.length() - suffix.length());
            while (true) {
                if (i >= values.length) {
                    throw new InvalidPropertyValueException("Unvalid string, expected "+values.length+" values, but found more");
                }
                final int idx = value.indexOf(delimiter, offset);
                if (idx < 0) {
                    //last element
                    values[i++] = value.substring(offset);
                    break;
                } else {
                    values[i++] = value.substring(offset, idx);
                    offset = (idx + delimiter.length());
                }
            }

            if (i != values.length) {
                throw new InvalidPropertyValueException("Unvalid string, number of values do not match, found "+(i)+" but expected "+values.length);
            }

            //set values, convert them if necessary
            final FeatureType type = feature.getType();
            for (int k=0; k < values.length; k++) {
                final String propName = propertyNames[k];
                final AttributeType<?> pt = (AttributeType<?>) type.getProperty(propName);
                final Object val = ObjectConverters.convert(values[k], pt.getValueClass());
                feature.setPropertyValue(propName, val);
            }
        }
    }
}
