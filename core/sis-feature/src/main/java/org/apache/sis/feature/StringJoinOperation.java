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
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.InvalidPropertyValueException;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;


/**
 * An operation concatenating the string representations of the values of multiple properties.
 * This operation can be used for creating a <cite>compound key</cite> as a {@link String}
 * that consists of two or more attribute values that uniquely identify a feature instance.
 *
 * <p>This operation supports both reading and writing. When setting a value on the attribute
 * created by this operation, the value will be split and forwarded to each single attribute.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
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
     * The parameter descriptor for the "String join" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = LinkOperation.parameters("StringJoin", 1);

    /**
     * The name of the properties (attributes of operations producing attributes)
     * from which to get the values to concatenate.
     */
    private final String[] attributeNames;

    /**
     * Converters for parsing strings as attribute values. Those converters will be used by
     * {@link Result#setValue(String)} while {@link Result#getValue()} will use the inverse
     * of those converters.
     *
     * <p>Note: we store converters from string to value instead than the converse because
     * the inverse conversion is often a simple call to {@link Object#toString()}, so there
     * is a risk that some of the later converters do not bother to remember their inverse.</p>
     */
    private final ObjectConverter<? super String, ?>[] converters;

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
     * It is caller's responsibility to ensure that {@code delimiter} and {@code singleAttributes} are not null.
     * This private constructor does not verify that condition on the assumption that the public API did.
     *
     * @see FeatureOperations#compound(Map, String, String, String, PropertyType...)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})                                        // Generic array creation.
    StringJoinOperation(final Map<String,?> identification, final String delimiter,
            final String prefix, final String suffix, final PropertyType[] singleAttributes)
            throws UnconvertibleObjectException
    {
        super(identification);
        attributeNames = new String[singleAttributes.length];
        converters = new ObjectConverter[singleAttributes.length];
        for (int i=0; i < singleAttributes.length; i++) {
            IdentifiedType attributeType = singleAttributes[i];
            ArgumentChecks.ensureNonNullElement("singleAttributes", i, attributeType);
            final GenericName name = attributeType.getName();
            if (attributeType instanceof Operation) {
                attributeType = ((Operation) attributeType).getResult();
            }
            if (!(attributeType instanceof AttributeType)) {
                throw new IllegalArgumentException(Errors.getResources(identification)
                        .getString(Errors.Keys.IllegalPropertyType_2, name,
                        Classes.getLeafInterfaces(attributeType.getClass(), PropertyType.class)[0]));
            }
            if (((AttributeType<?>) attributeType).getMaximumOccurs() > 1) {
                throw new IllegalArgumentException(Errors.getResources(identification)
                        .getString(Errors.Keys.NotASingleton_1, name));
            }
            converters[i] = ObjectConverters.find(String.class, ((AttributeType<?>) attributeType).getValueClass());
            attributeNames[i] = name.toString();
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
            dependencies = CollectionsExt.immutableSet(true, attributeNames);
        }
        return dependencies;
    }

    /**
     * Formats the given value using the given converter. This method is a workaround for the presence
     * of the first {@code ?} in {@code ObjectConverter<?,?>}: defining a separated method allows us
     * to replace that {@code <?>} by {@code <V>}, thus allowing the compiler to verify consistency.
     *
     * @param converter  the converter to use for formatting the given value.
     * @param value      the value to format, or {@code null}.
     */
    private static <S> Object format(final ObjectConverter<S,?> converter, final Object value) {
        return converter.apply(converter.getSourceClass().cast(value));
    }

    /**
     * Creates a value as the concatenation of the attributes read from the given feature.
     *
     * @param  feature  the feature from which to read the attributes.
     * @return the concatenated string.
     * @throws UnconvertibleObjectException if one of the attribute value is not of the expected type.
     */
    final String join(final Feature feature) throws UnconvertibleObjectException {
        final StringBuilder sb = new StringBuilder();
        String sep = prefix;
        String name  = null;
        Object value = null;
        try {
            for (int i=0; i < attributeNames.length; i++) {
                name  = attributeNames[i];
                value = feature.getPropertyValue(name);                 // Used in 'catch' block in case of exception.
                value = format(converters[i].inverse(), value);
                sb.append(sep);
                if (value != null) {
                    sb.append(value);
                }
                sep = delimiter;
            }
        } catch (ClassCastException e) {
            if (value == null) {
                throw e;
            }
            throw new UnconvertibleObjectException(Errors.format(
                    Errors.Keys.IllegalPropertyValueClass_2, name, value.getClass(), e));
        }
        return sb.append(suffix).toString();
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
            return join(feature);
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
            final String[] values = new String[attributeNames.length];
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
            for (int k=0; k < values.length; k++) {
                final String propName = attributeNames[k];
                final Object val = converters[k].apply(values[k]);
                feature.setPropertyValue(propName, val);
            }
        }
    }
}
