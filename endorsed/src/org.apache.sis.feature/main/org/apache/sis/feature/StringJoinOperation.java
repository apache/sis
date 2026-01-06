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
import java.util.Objects;
import java.util.List;
import java.io.IOException;
import java.io.Serializable;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.converter.SurjectiveConverter;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.feature.internal.shared.AttributeConvention;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.InvalidPropertyValueException;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;
import org.opengis.feature.PropertyNotFoundException;


/**
 * An operation concatenating the string representations of the values of multiple properties.
 * This operation can be used for creating a <dfn>compound key</dfn> as a {@link String}
 * that consists of two or more attribute values that uniquely identify a feature instance.
 *
 * <p>This operation supports both reading and writing. When setting a value on the attribute
 * created by this operation, the value will be split and forwarded to each single attribute.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
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
    private static final ParameterDescriptorGroup EMPTY_PARAMS = parameters("StringJoin");

    /**
     * A pseudo-converter returning the identifier of a feature. This pseudo-converter is used in place
     * of "real" converters in the {@link StringJoinOperation#converters} array when the property is an
     * association to a feature instead of an attribute. This pseudo-converters is used as below:
     *
     * <ul>
     *   <li>{@link Result#getValue()} gets this converter by a call to {@code converters[i].inverse()}.
     *       This works provided that {@link #inverse()} returns {@code this} (see comment below).</li>
     *   <li>{@link Result#setValue(String)} needs to perform a special case for this class.</li>
     * </ul>
     *
     * This is not a well-formed converter since its {@link #inverse()} method does not fulfill the required
     * semantic of {@link ObjectConverter#inverse()}, but this is okay for {@link StringJoinOperation} needs.
     * This converter should never be accessible to users however.
     */
    private static final class ForFeature extends SurjectiveConverter<Object, Object> implements Serializable {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2208230611402221572L;

        /**
         * The "real" converter which would have been stored in the {@link StringJoinOperation#converters}
         * array if the property was an attribute instead of an association. For formatting the feature
         * identifier, we need to use the inverse of that converter.
         */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        final ObjectConverter<? super String, ?> converter;

        /** Creates a new wrapper over the given converter. */
        ForFeature(final ObjectConverter<? super String, ?> converter) {
            this.converter = converter;
        }

        /**
         * Returns {@code this} for allowing {@link Result#getValue()} to get this pseudo-converter.
         * This is a violation of {@link ObjectConverter} contract since this pseudo-converter is not
         * an identity converter. Direct uses of this pseudo-converter will need a {@code instanceof}
         * check instead.
         */
        @Override public ObjectConverter<Object,Object> inverse()        {return this;}
        @Override public Class<Object>                  getSourceClass() {return Object.class;}
        @Override public Class<Object>                  getTargetClass() {return Object.class;}
        @Override public Object apply(final Object f) {
            return (f != null) ? format(converter.inverse(),
                    ((Feature) f).getPropertyValue(AttributeConvention.IDENTIFIER)) : null;
        }
    }

    /**
     * The name of the properties (attributes of operations producing attributes)
     * from which to get the values to concatenate.
     *
     * @see #getAttributeNames()
     * @see #getDependencies()
     */
    private final String[] attributeNames;

    /**
     * Converters for parsing strings as attribute values. Those converters will be used by
     * {@link Result#setValue(String)} while {@link Result#getValue()} will use the inverse
     * of those converters.
     *
     * <p>Note: we store converters from string to value instead of the converse because
     * the inverse conversion is often a simple call to {@link Object#toString()}, so there
     * is a risk that some of the latter converters do not bother to remember their inverse.</p>
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final ObjectConverter<? super String, ?>[] converters;

    /**
     * The property names as an unmodifiable set, created when first needed.
     * This is simply {@link #attributeNames} copied in an unmodifiable set.
     *
     * @see #getDependencies()
     */
    private transient volatile Set<String> dependencies;

    /**
     * The type of the result returned by the string concatenation operation.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final AttributeType<String> resultType;

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
     * @param  identification    the name and other information to be given to this operation.
     * @param  delimiter         the characters to use as delimiter between each single property value.
     * @param  prefix            characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix            characters to use at the end of the concatenated string, or {@code null} if none.
     * @param  singleAttributes  identification of the single attributes (or operations producing attributes) to concatenate.
     * @param  inheritFrom       existing operation from which to inherit null attributes, or {@code null} if none.
     * @throws UnconvertibleObjectException if at least one attributes is not convertible from a string.
     * @throws IllegalArgumentException if the operation failed for another reason.
     *
     * @see FeatureOperations#compound(Map, String, String, String, PropertyType...)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})                                        // Generic array creation.
    StringJoinOperation(final Map<String,?> identification, final String delimiter,
            final String prefix, final String suffix, final PropertyType[] singleAttributes,
            final StringJoinOperation inheritFrom)
    {
        super(identification);
        attributeNames = new String[singleAttributes.length];
        converters = new ObjectConverter[singleAttributes.length];
        for (int i=0; i < singleAttributes.length; i++) {
            /*
             * Verify the following conditions:
             *   - property types are non-null.
             *   - properties are either attributes, or operations producing attributes,
             *     or association to features having an "sis:identifier" property.
             *   - attributes contain at most one value (no collections).
             *
             * We test FeatureAssociationRole, Operation and AttributeType in that order
             * because the "sis:identifier" property of FeatureType may be an Operation,
             * which may in turn produce an AttributeType. We do not accept more complex
             * combinations (e.g. operation producing an association).
             */
            IdentifiedType propertyType = singleAttributes[i];
            if (inheritFrom == null) {
                ArgumentChecks.ensureNonNullElement("singleAttributes", i, propertyType);
            } else if (propertyType == null) {
                attributeNames[i] = inheritFrom.attributeNames[i];
                converters[i] = inheritFrom.converters[i];
                continue;
            }
            final GenericName name = propertyType.getName();
            int maximumOccurs = 0;                              // May be a bitwise combination; need only to know if > 1.
            PropertyNotFoundException cause = null;             // In case of failure to find "sis:identifier" property.
            final boolean isAssociation = (propertyType instanceof FeatureAssociationRole);
            if (isAssociation) {
                final var role = (FeatureAssociationRole) propertyType;
                final FeatureType ft = role.getValueType();
                maximumOccurs = role.getMaximumOccurs();
                try {
                    propertyType = ft.getProperty(AttributeConvention.IDENTIFIER);
                } catch (PropertyNotFoundException e) {
                    cause = e;
                }
            }
            if (propertyType instanceof Operation) {
                propertyType = ((Operation) propertyType).getResult();
            }
            if (propertyType instanceof AttributeType) {
                maximumOccurs |= ((AttributeType<?>) propertyType).getMaximumOccurs();
            } else {
                final Class<?>[] inf = Classes.getLeafInterfaces(Classes.getClass(propertyType), PropertyType.class);
                throw new IllegalArgumentException(Resources.forProperties(identification)
                        .getString(Resources.Keys.IllegalPropertyType_2, name, (inf.length != 0) ? inf[0] : null), cause);
            }
            if (maximumOccurs > 1) {
                throw new IllegalArgumentException(Resources.forProperties(identification)
                        .getString(Resources.Keys.NotASingleton_1, name));
            }
            /*
             * StringJoinOperation does not need to keep the AttributeType references.
             * We need only their names and how to convert from String to their values.
             */
            attributeNames[i] = name.toString();
            final Class<?> valueClass = ((AttributeType<?>) propertyType).getValueClass();
            ObjectConverter<? super String, ?> converter;
            try {
                converter = ObjectConverters.find(String.class, valueClass);
            } catch (UnconvertibleObjectException e) {
                throw new UnconvertibleObjectException(Resources.forProperties(identification)
                        .getString(Resources.Keys.IllegalPropertyType_2, name, valueClass), e);
            }
            if (isAssociation) {
                converter = new ForFeature(converter);
            }
            converters[i] = converter;
        }
        if (inheritFrom != null) {
            resultType = inheritFrom.resultType;
        } else {
            resultType = FeatureOperations.POOL.unique(new DefaultAttributeType<>(
                    resultIdentification(identification), String.class, 1, 1, null));
        }
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
    public IdentifiedType getResult() {
        return resultType;
    }

    /**
     * Returns the name of the properties from which to get the values to concatenate.
     * This is the same information as {@link #getDependencies()}, only in a different
     * kind of collection.
     */
    final List<String> getAttributeNames() {
        return Containers.viewAsUnmodifiableList(attributeNames);
    }

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<String> getDependencies() {
        Set<String> cached = dependencies;
        if (cached == null) {
            // Not really a problem if computed twice concurrently.
            dependencies = cached = Containers.copyToImmutableSetIgnoreNull(attributeNames);
        }
        return cached;
    }

    /**
     * Returns the same operation but using different properties as inputs.
     *
     * @param  dependencies  the new properties to use as operation inputs.
     * @return the new operation, or {@code this} if unchanged.
     */
    @Override
    public Operation updateDependencies(final Map<String, PropertyType> dependencies) {
        boolean hasNonNull = false;
        final var singleAttributes = new PropertyType[attributeNames.length];
        for (int i=0; i < singleAttributes.length; i++) {
            hasNonNull |= (singleAttributes[i] = dependencies.get(attributeNames[i])) != null;
        }
        if (hasNonNull) {
            final var op = new StringJoinOperation(inherit(), delimiter, prefix, suffix, singleAttributes, this);
            if (!(Arrays.equals(op.attributeNames, attributeNames) && Arrays.equals(op.converters, converters))) {
                return FeatureOperations.POOL.unique(op);
            }
        }
        return this;
    }

    /**
     * Formats the given value using the given converter. This method is a workaround for the presence
     * of the first {@code ?} in {@code ObjectConverter<?,?>}: defining a separated method allows us
     * to replace that {@code <?>} by {@code <S>}, thus allowing the compiler to verify consistency.
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
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        return new Result(Objects.requireNonNull(feature));
    }




    /**
     * The attributes that contains the result of concatenating the string representation of other attributes.
     * Value is calculated each time it is accessed.
     */
    private final class Result extends OperationResult<String> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -555025854115540108L;

        /**
         * Creates a new attribute for the given feature.
         */
        Result(final Feature feature) {
            super(resultType, feature);
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
            final var sb = new StringBuilder();
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
                        Errors.Keys.IncompatiblePropertyValue_1, name), e);
            }
            return sb.append(suffix).toString();
        }

        /**
         * Given a concatenated string as produced by {@link #getValue()}, separates the components around
         * the separator and forward the values to the original attributes. If one of the values cannot be
         * parsed, then this method does not store any property value ("all or nothing" behavior).
         *
         * @param  value  the concatenated string.
         * @throws InvalidPropertyValueException if one of the attribute values cannot be parsed to the expected type.
         */
        @Override
        public void setValue(final String value) throws InvalidPropertyValueException {
            final int endAt = value.length() - suffix.length();
            final boolean prefixMatches = value.startsWith(prefix);
            if (!prefixMatches || !value.endsWith(suffix)) {
                throw new InvalidPropertyValueException(Errors.format(Errors.Keys.UnexpectedCharactersAtBound_4,
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
            final var values = new Object[attributeNames.length];
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
                            assert element.startsWith(delimiter, i) : element;
                            i += delimiter.length();
                        }
                    }
                }
                /*
                 * Empty strings are considered as null values for consistency with StringJoinOperation.format(…).
                 * If we have more values than expected, continue the parsing but without storing the values.
                 * The intent is to get the correct count of values for error reporting.
                 */
                if (!element.isEmpty() && count < values.length) {
                    ObjectConverter<? super String, ?> converter = converters[count];
                    if (converter instanceof ForFeature) {
                        converter = ((ForFeature) converter).converter;
                    }
                    try {
                        values[count] = converter.apply(element);
                    } catch (UnconvertibleObjectException e) {
                        throw new InvalidPropertyValueException(Errors.format(
                                Errors.Keys.CanNotAssign_2, attributeNames[count], element), e);
                    }
                }
                count++;
                upper += delimiter.length();
                lower = upper;
            } while (!done);
            /*
             * Store the values in the properties only after we successfully converted all of them,
             * in order to have a "all or nothing" behavior (assuming that calls to Feature methods
             * below do not fail).
             */
            if (values.length != count) {
                throw new InvalidPropertyValueException(Resources.format(
                        Resources.Keys.UnexpectedNumberOfComponents_4, getName(), value, values.length, count));
            }
            for (int i=0; i < values.length; i++) {
                Feature f   = feature;
                String name = attributeNames[i];
                if (converters[i] instanceof ForFeature) {
                    f = (Feature) f.getPropertyValue(name);
                    name = AttributeConvention.IDENTIFIER;
                }
                f.setPropertyValue(name, values[i]);
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
            final var that = (StringJoinOperation) obj;
            return Arrays.equals(this.attributeNames, that.attributeNames) &&
                   Arrays.equals(this.converters,     that.converters)     &&
                  Objects.equals(this.delimiter,      that.delimiter)      &&
                  Objects.equals(this.prefix,         that.prefix)         &&
                  Objects.equals(this.suffix,         that.suffix);
        }
        return false;
    }

    /**
     * Appends a string representation of the "formula" used for computing the result.
     *
     * @param  buffer  where to format the "formula".
     */
    @Override
    void formatResultFormula(final Appendable buffer) throws IOException {
        final String escape = ESCAPE + delimiter;
        if (prefix != null) buffer.append(prefix);
        for (int i=0; i<attributeNames.length; i++) {
            if (i != 0) buffer.append(delimiter);
            buffer.append(attributeNames[i].replace(delimiter, escape));
        }
        if (suffix != null) buffer.append(suffix);
    }
}
