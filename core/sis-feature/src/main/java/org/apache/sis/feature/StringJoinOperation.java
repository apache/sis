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

import java.util.Arrays;
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
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


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
     * The character used for escaping occurrences of the delimiter inside a value.
     */
    static final char ESCAPE = '\\';

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
    private final DefaultAttributeType<String> resultType;

    /**
     * The characters to use at the beginning of the concatenated string, or an empty string if none.
     */
    final String prefix;

    /**
     * The characters to use at the end of the concatenated string, or an empty string if none.
     */
    final String suffix;

    /**
     * The characters to use a delimiter between each single attribute value.
     */
    final String delimiter;

    /**
     * Creates a new operation for string concatenations using the given prefix, suffix and delimeter.
     * It is caller's responsibility to ensure that {@code delimiter} and {@code singleAttributes} are not null.
     * This private constructor does not verify that condition on the assumption that the public API did.
     *
     * @see FeatureOperations#compound(Map, String, String, String, PropertyType...)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})                                        // Generic array creation.
    StringJoinOperation(final Map<String,?> identification, final String delimiter,
            final String prefix, final String suffix, final AbstractIdentifiedType[] singleAttributes)
            throws UnconvertibleObjectException
    {
        super(identification);
        attributeNames = new String[singleAttributes.length];
        converters = new ObjectConverter[singleAttributes.length];
        for (int i=0; i < singleAttributes.length; i++) {
            /*
             * Verify the following conditions:
             *   - property types are non-null.
             *   - properties are either attributes, or operations producing attributes.
             *   - attributes contain at most one value (no collections).
             */
            AbstractIdentifiedType attributeType = singleAttributes[i];
            ArgumentChecks.ensureNonNullElement("singleAttributes", i, attributeType);
            final GenericName name = attributeType.getName();
            if (attributeType instanceof AbstractOperation) {
                attributeType = ((AbstractOperation) attributeType).getResult();
            }
            if (!(attributeType instanceof DefaultAttributeType)) {
                throw new IllegalArgumentException(Errors.getResources(identification)
                        .getString(Errors.Keys.IllegalPropertyType_2, name,
                        Classes.getLeafInterfaces(attributeType.getClass(), AbstractIdentifiedType.class)[0]));
            }
            if (((DefaultAttributeType<?>) attributeType).getMaximumOccurs() > 1) {
                throw new IllegalArgumentException(Errors.getResources(identification)
                        .getString(Errors.Keys.NotASingleton_1, name));
            }
            /*
             * StringJoinOperation does not need to keep the AttributeType references.
             * We need only their names and how to convert from String to their values.
             */
            attributeNames[i] = name.toString();
            converters[i] = ObjectConverters.find(String.class, ((DefaultAttributeType<?>) attributeType).getValueClass());
        }
        resultType = FeatureOperations.POOL.unique(new DefaultAttributeType<String>(
                resultIdentification(identification), String.class, 1, 1, null));
        this.delimiter = delimiter;
        this.prefix = (prefix == null) ? "" : prefix;
        this.suffix = (suffix == null) ? "" : suffix;
    }

    /**
     * Returns an empty group of parameters since this operation does not require any parameter.
     *
     * @return empty parameter group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Returns the type of results computed by this operation, which is {@code AttributeType<String>}.
     * The attribute type name depends on the value of {@code "result.*"} properties (if any)
     * given at construction time.
     *
     * @return an {@code AttributeType<String>}.
     */
    @Override
    public AbstractIdentifiedType getResult() {
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
    static <S> Object format(final ObjectConverter<S,?> converter, final Object value) {
        return converter.apply(converter.getSourceClass().cast(value));
    }

    /**
     * Returns the concatenation of property values of the given feature.
     *
     * @param  feature     the feature on which to execute the operation.
     * @param  parameters  ignored (can be {@code null}).
     * @return the concatenation of feature property values.
     */
    @Override
    public Property apply(AbstractFeature feature, ParameterValueGroup parameters) {
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
        private final AbstractFeature feature;

        /**
         * Creates a new attribute for the given feature.
         */
        Result(final AbstractFeature feature) {
            super(resultType);
            this.feature = feature;
        }

        /**
         * Creates a string which is the concatenation of attribute values of all properties
         * specified to the {@link StringJoinOperation} constructor.
         *
         * @return the concatenated string.
         * @throws UnconvertibleObjectException if one of the attribute values is not of the expected type.
         */
        @Override
        public String getValue() throws UnconvertibleObjectException {
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
                    sep = delimiter;
                    if (value != null) {
                        /*
                         * First insert the value, then substitute in-place all occurrences of "\" by "\\"
                         * then all occurence of the delimiter by "\" followed by the delimiter.
                         */
                        final int startAt = sb.length();
                        int j = sb.append(value).length();
                        while (--j >= startAt) {
                            if (sb.charAt(j) == ESCAPE) {
                                sb.insert(j, ESCAPE);
                            }
                        }
                        j = startAt;
                        while ((j = sb.indexOf(sep, j)) >= 0) {
                            sb.insert(j, ESCAPE);
                            j += sep.length() + 1;
                        }
                    }
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
         * Given a concatenated string as produced by {@link #getValue()}, separates the components around
         * the separator and forward the values to the original attributes. If one of the values can not be
         * parsed, then this method does not store any property value ("all or nothing" behavior).
         *
         * @param  value  the concatenated string.
         * @throws InvalidPropertyValueException if one of the attribute values can not be parsed to the expected type.
         */
        @Override
        public void setValue(final String value) throws IllegalArgumentException {
            final int endAt = value.length() - suffix.length();
            final boolean prefixMatches = value.startsWith(prefix);
            if (!prefixMatches || !value.endsWith(suffix)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedCharactersAtBound_4,
                        getName(),
                        prefixMatches ? 1 : 0,              // For "{1,choice,0#begin|1#end}" in message format.
                        prefixMatches ? suffix : prefix,
                        prefixMatches ? value.substring(Math.max(0, endAt)) : CharSequences.token(value, 0)));
            }
            /*
             * We do not use the regex split for avoiding possible reserved regex characters,
             * and also for processing directly escaped delimiters. We convert the values as we
             * read them (no need to store the substrings) but do not store them in the properties
             * before we succeeded to parse all values, so we have a "all or nothing" behavior.
             */
            final Object[] values = new Object[attributeNames.length];
            int lower = prefix.length();
            int upper = lower;
            int count = 0;
            boolean done = false;
            do {
                upper = value.indexOf(delimiter, upper);
                if (upper >= 0 && upper < endAt) {
                    /*
                     * If an odd number of escape characters exist before the delimiter, remove the last
                     * escape character and continue the search for the next delimiter.
                     */
                    int escape = upper;
                    while (escape != 0 && value.charAt(escape - 1) == ESCAPE) {
                        escape--;
                    }
                    if (((upper - escape) & 1) != 0) {
                        upper += delimiter.length() + 1;
                        continue;
                    }
                } else {
                    upper = endAt;
                    done = true;
                }
                /*
                 * Get the value and remove all escape characters. Each escape character is either followed by another
                 * escape character (that we need to keep) or the delimiter. The algorithm used here is inefficient
                 * (we recreate a buffer for each character to remove), but we assume that it should be rarely needed.
                 */
                String element = value.substring(lower, upper);
                for (int i=0; (i = element.indexOf(ESCAPE, i)) >= 0;) {
                    element = new StringBuilder(element.length() - 1)
                            .append(element, 0, i).append(element, i+1, element.length()).toString();
                    if (i < element.length()) {
                        if (element.charAt(i) == ESCAPE) {
                            i++;
                        } else {
                            assert element.regionMatches(i, delimiter, 0, delimiter.length()) : element;
                            i += delimiter.length();
                        }
                    }
                }
                /*
                 * Empty strings are considered as null values for consistency with StringJoinOperation.format(…).
                 * If we have more values than expected, continue the parsing but without storing the values.
                 * The intend is to get the correct count of values for error reporting.
                 */
                if (!element.isEmpty() && count < values.length) try {
                    values[count] = converters[count].apply(element);
                } catch (UnconvertibleObjectException e) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.CanNotAssign_2, attributeNames[count], element), e);
                }
                count++;
                upper += delimiter.length();
                lower = upper;
            } while (!done);
            /*
             * Store the values in the properties only after we successfully converted all of them,
             * in order to have a "all or nothing" behavior.
             */
            if (values.length != count) {
                throw new IllegalArgumentException(
                        Errors.format(Errors.Keys.UnexpectedNumberOfComponents_3, value, values.length, count));
            }
            for (int i=0; i < values.length; i++) {
                feature.setPropertyValue(attributeNames[i], values[i]);
            }
        }
    }

    /**
     * Computes a hash-code value for this operation.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(attributeNames) + 37 * Objects.hash(delimiter, prefix, suffix);
    }

    /**
     * Compares this operation with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj)) {
            // 'this.result' is compared (indirectly) by the super class.
            final StringJoinOperation that = (StringJoinOperation) obj;
            return Arrays.equals(this.attributeNames, that.attributeNames) &&
                   Arrays.equals(this.converters,     that.converters)     &&
                  Objects.equals(this.delimiter,      that.delimiter)      &&
                  Objects.equals(this.prefix,         that.prefix)         &&
                  Objects.equals(this.suffix,         that.suffix);
        }
        return false;
    }
}
