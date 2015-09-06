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
package org.apache.sis.parameter;

import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.measure.unit.Unit;
import org.opengis.util.CodeList;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.jaxb.referencing.CC_OperationParameter;
import org.apache.sis.referencing.IdentifiedObjects;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureCanCast;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * The definition of a single parameter used by an operation method.
 * For {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems}
 * most parameter values are numeric, but other types of parameter values are possible.
 *
 * <p>A parameter descriptor contains the following properties:</p>
 * <ul>
 *   <li>The parameter {@linkplain #getName() name}.</li>
 *   <li>The {@linkplain #getValueClass() class of values}. This is usually {@link Double}, {@code double[]},
 *       {@link Integer}, {@code int[]}, {@link Boolean}, {@link String} or {@link java.net.URI},
 *       but other types are allowed as well.</li>
 *   <li>Whether this parameter is optional or mandatory. This is specified by the {@linkplain #getMinimumOccurs()
 *       minimum occurences} number, which can be 0 or 1 respectively.</li>
 *   <li>The domain of values, as a {@linkplain #getMinimumValue() minimum value}, {@linkplain #getMaximumValue()
 *       maximum value} or an enumeration of {@linkplain #getValidValues() valid values}.</li>
 *   <li>The {@linkplain #getDefaultValue() default value}.</li>
 *   <li>The {@linkplain #getUnit() unit of measurement}.</li>
 * </ul>
 *
 * @param <T> The type of elements to be returned by {@link DefaultParameterValue#getValue()}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 *
 * @see DefaultParameterValue
 * @see DefaultParameterDescriptorGroup
 */
@XmlType(name = "OperationParameterType")
@XmlRootElement(name = "OperationParameter")
public class DefaultParameterDescriptor<T> extends AbstractParameterDescriptor implements ParameterDescriptor<T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1978932430298071693L;

    /**
     * The class that describe the type of parameter values.
     *
     * @see #getValueClass()
     */
    private final Class<T> valueClass;

    /**
     * A set of valid values (usually from a {@linkplain CodeList code list})
     * or {@code null} if it does not apply. This set is immutable.
     *
     * @see #getValidValues()
     */
    private final Set<T> validValues;

    /**
     * The minimum and maximum parameter value with their unit of measurement, or {@code null} if none.
     * If this field is non-null, then <code>valueDomain.{@linkplain Range#getElementType() getElementType()}</code>
     * shall be one of the following:
     *
     * <ul>
     *   <li>If {@link #valueClass} is not an array, then the range element type shall be the same class.</li>
     *   <li>If {@code valueClass} is an array, then the range element type shall be the wrapper of
     *       <code>valueClass.{@linkplain Class#getComponentType() getComponentType()}</code>.</li>
     * </ul>
     *
     * @see #getValueDomain()
     */
    private final Range<?> valueDomain;

    /**
     * The default value for the parameter, or {@code null}.
     *
     * @see #getDefaultValue()
     */
    private final T defaultValue;

    /**
     * Constructs a descriptor from the given properties. The properties map is given unchanged to the
     * {@linkplain AbstractParameterDescriptor#AbstractParameterDescriptor(Map, int, int) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#DESCRIPTION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * The {@code valueDomain} argument combines the {@linkplain #getMinimumValue() minimum value},
     * {@linkplain #getMaximumValue() maximum value}, {@linkplain #getUnit() unit of measurement}
     * (if any) and information about whether the bounds are inclusive or exclusive.
     * If this argument is non-null, then it shall comply to the following conditions:
     *
     * <ul>
     *   <li>The range shall be non-{@linkplain Range#isEmpty() empty}.</li>
     *   <li><code>valueDomain.{@linkplain Range#getElementType() getElementType()}</code> shall be equals
     *       to one of the following:
     *     <ul>
     *       <li>to {@code valueClass} if the later is not an array,</li>
     *       <li>or to <code>{@linkplain Numbers#primitiveToWrapper(Class)
     *           primitiveToWrapper}(valueClass.{@linkplain Class#getComponentType() getComponentType()})</code>
     *           if {@code valueClass} is an array.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * If both {@code valueDomain} and {@code validValues} are non-null, then all valid values shall be contained
     * in the value domain.
     *
     * @param properties    The properties to be given to the identified object.
     * @param minimumOccurs The {@linkplain #getMinimumOccurs() minimum number of times} that values
     *                      for this parameter group are required, or 0 if no restriction.
     * @param maximumOccurs The {@linkplain #getMaximumOccurs() maximum number of times} that values
     *                      for this parameter group are required, or {@link Integer#MAX_VALUE} if no restriction.
     * @param valueClass    The class that describes the type of the parameter value.
     * @param valueDomain   The minimum value, maximum value and unit of measurement, or {@code null} if none.
     * @param validValues   The list of valid values, or {@code null} if there is no restriction.
     *                      This property is mostly for restricting values to a {@linkplain CodeList code list}
     *                      or enumeration subset. It is not necessary to provide this property when all values
     *                      from the code list or enumeration are valid.
     * @param defaultValue  The default value for the parameter, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public DefaultParameterDescriptor(final Map<String,?> properties,
                                      final int           minimumOccurs,
                                      final int           maximumOccurs,
                                      final Class<T>      valueClass,
                                      final Range<?>      valueDomain,
                                      final T[]           validValues,
                                      final T             defaultValue)
    {
        super(properties, minimumOccurs, maximumOccurs);
        ensureNonNull("valueClass",   valueClass);
        ensureCanCast("defaultValue", valueClass, defaultValue);
        if (valueDomain != null) {
            Class<?> componentType = valueClass.getComponentType();
            if (componentType != null) {
                componentType = Numbers.primitiveToWrapper(componentType);
            } else {
                componentType = valueClass;
            }
            final Class<?> elementType = valueDomain.getElementType();
            if (elementType != componentType) {
                throw new IllegalArgumentException(Errors.getResources(properties).getString(
                        Errors.Keys.IllegalArgumentClass_2, "valueDomain",
                        "Range<" + Classes.getShortName(elementType) + '>'));
            }
            if (valueDomain.isEmpty()) {
                throw new IllegalArgumentException(Errors.getResources(properties)
                        .getString(Errors.Keys.IllegalRange_2, valueDomain.getMinValue(), valueDomain.getMaxValue()));
            }
        }
        this.valueClass   = valueClass;
        this.valueDomain  = valueDomain;
        this.defaultValue = Numerics.cached(defaultValue);
        /*
         * If the caller specified a set of valid values, then copy the values in
         * a new set and verify their type and inclusion in the [min … max] range.
         */
        if (validValues != null) {
            final Set<T> valids = CollectionsExt.createSetForType(valueClass, validValues.length);
            for (T value : validValues) {
                if (value != null) {
                    value = Numerics.cached(value);
                    final Verifier error = Verifier.ensureValidValue(valueClass, null, valueDomain, value);
                    if (error != null) {
                        throw new IllegalArgumentException(error.message(properties, super.getName().getCode(), value));
                    }
                    valids.add(value);
                }
            }
            this.validValues = CollectionsExt.unmodifiableOrCopy(valids);
        } else {
            this.validValues = null;
        }
        /*
         * Finally, verify the default value if any.
         */
        if (defaultValue != null) {
            final Verifier error = Verifier.ensureValidValue(valueClass, this.validValues, valueDomain, defaultValue);
            if (error != null) {
                throw new IllegalArgumentException(error.message(properties, super.getName().getCode(), defaultValue));
            }
        }
    }

    /**
     * Creates a new descriptor with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param descriptor The descriptor to shallow copy.
     *
     * @see #castOrCopy(ParameterDescriptor)
     */
    @SuppressWarnings("unchecked")
    protected DefaultParameterDescriptor(final ParameterDescriptor<T> descriptor) {
        super(descriptor);
        valueClass   = descriptor.getValueClass();
        validValues  = descriptor.getValidValues();
        defaultValue = descriptor.getDefaultValue();
        valueDomain  = Parameters.getValueDomain(descriptor);
    }

    /**
     * Returns a SIS parameter implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the values of the given object.
     *
     * @param  <T> The type of values.
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static <T> DefaultParameterDescriptor<T> castOrCopy(final ParameterDescriptor<T> object) {
        return (object == null) || (object instanceof DefaultParameterDescriptor<?>)
                ? (DefaultParameterDescriptor<T>) object : new DefaultParameterDescriptor<T>(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ParameterDescriptor.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ParameterDescriptor}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their own
     * set of interfaces.</div>
     *
     * @return {@code ParameterDescriptor.class} or a user-defined sub-interface.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends ParameterDescriptor<T>> getInterface() {
        return (Class) ParameterDescriptor.class;
    }

    /**
     * Returns the class that describe the type of the parameter.
     *
     * @return The parameter value class.
     */
    @Override
    public final Class<T> getValueClass() {
        return valueClass;
    }

    /**
     * If this parameter allows only a finite set of values, returns that set.
     * The set of valid values is usually a {@linkplain CodeList code list} or enumeration.
     * This method returns {@code null} if this parameter does not limit values to a finite set.
     *
     * @return A finite set of valid values (usually from a {@linkplain CodeList code list}),
     *         or {@code null} if it does not apply or if there is no restriction.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<T> getValidValues() {
        return validValues;   // Null or unmodifiable
    }

    /**
     * Returns the domain of values with their unit of measurement (if any), or {@code null} if none.
     * The {@code Range} object combines the {@linkplain #getValueClass() value class},
     * {@linkplain #getMinimumValue() minimum value}, {@linkplain #getMaximumValue() maximum value}
     * and whether these values are inclusive or inclusive. If the range is an instance of
     * {@link MeasurementRange}, then it contains also the {@linkplain #getUnit() unit of measurement}.
     *
     * <div class="note"><b>API note:</b> If this method returns a non-null value, then its type is either exactly
     * {@code Range<T>}, or {@code Range<E>} where {@code <E>} is the {@linkplain Class#getComponentType() component
     * type} of {@code <T>} (using wrapper classes for primitive types).</div>
     *
     * @return The domain of values, or {@code null}.
     *
     * @see Parameters#getValueDomain(ParameterDescriptor)
     */
    /* Implementation note: this method is final because the constructor performs various checks on range validity,
     * and we can not express those rules in the method signature. The 'Verifier.ensureValidValue(…)' method needs
     * some guarantees about range validity, so we can not let users override this method with a range that may
     * break them.
     */
    public final Range<?> getValueDomain() {
        return valueDomain;
    }

    /**
     * Returns the minimum parameter value. If there is no minimum value, or if minimum
     * value is inappropriate for the {@linkplain #getValueClass() value class}, then
     * this method returns {@code null}.
     *
     * <p>This is a convenience method for
     * <code>{@linkplain #getValueDomain()}.{@linkplain Range#getMinValue() getMinValue()}</code>.
     * Note that this method said nothing about whether the value is {@linkplain Range#isMinIncluded() inclusive}.</p>
     *
     * @return The minimum parameter value (often an instance of {@link Double}), or {@code null} if unbounded.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Comparable<T> getMinimumValue() {
        return (valueDomain != null && valueDomain.getElementType() == valueClass)
               ? (Comparable<T>) valueDomain.getMinValue() : null;
    }

    /**
     * Returns the maximum parameter value. If there is no maximum value, or if maximum
     * value is inappropriate for the {@linkplain #getValueClass() value type}, then
     * this method returns {@code null}.
     *
     * <p>This is a convenience method for
     * <code>{@linkplain #getValueDomain()}.{@linkplain Range#getMaxValue() getMaxValue()}</code>.
     * Note that this method said nothing about whether the value is {@linkplain Range#isMaxIncluded() inclusive}.</p>
     *
     * @return The minimum parameter value (often an instance of {@link Double}), or {@code null} if unbounded.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Comparable<T> getMaximumValue() {
        return (valueDomain != null && valueDomain.getElementType() == valueClass)
               ? (Comparable<T>) valueDomain.getMaxValue() : null;
    }

    /**
     * Returns the default value for the parameter. The return type can be any type
     * including a {@link Number} or a {@link String}. If there is no default value,
     * then this method returns {@code null}.
     *
     * @return The default value, or {@code null} in none.
     */
    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the unit of measurement for the
     * {@linkplain #getMinimumValue() minimum},
     * {@linkplain #getMaximumValue() maximum} and
     * {@linkplain #getDefaultValue() default} values.
     * This attribute apply only if the values is of numeric type (usually an instance of {@link Double}).
     *
     * <p>This is a convenience method for
     * <code>{@linkplain #getValueDomain()}.{@linkplain MeasurementRange#unit() unit()}</code>.</p>
     *
     * @return The unit for numeric value, or {@code null} if it doesn't apply to the value type.
     */
    @Override
    public Unit<?> getUnit() {
        return (valueDomain instanceof MeasurementRange<?>) ? ((MeasurementRange<?>) valueDomain).unit() : null;
    }

    /**
     * Creates a new parameter value instance initialized with the {@linkplain #getDefaultValue() default value}.
     * The {@linkplain DefaultParameterDescriptor parameter descriptor} for the created parameter value will be
     * {@code this} object.
     *
     * @return A parameter initialized to the default value.
     */
    @Override
    public ParameterValue<T> createValue() {
        return new DefaultParameterValue<T>(this);
    }

    /**
     * Compares the specified object with this parameter for equality.
     * The strictness level is controlled by the second argument.
     * This method compares the following properties in every cases:
     *
     * <ul>
     *   <li>{@link #getName()}, compared {@linkplain #isHeuristicMatchForName(String) heuristically}
     *       in {@code IGNORE_METADATA} or less strict mode.</li>
     *   <li>{@link #getValueClass()}</li>
     *   <li>{@link #getDefaultValue()}</li>
     *   <li>{@link #getUnit()}</li>
     * </ul>
     *
     * All other properties (minimum and maximum occurrences, minimum, maximum and valid values)
     * are compared only for modes stricter than {@link ComparisonMode#IGNORE_METADATA}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                default: {
                    /*
                     * Tests for name, since parameters with different name have completely different meaning.
                     * For example there is no difference between "semi_major" and "semi_minor" parameters
                     * except the name. We do not perform this comparison if the user asked for metadata
                     * comparison, because in such case the names have already been compared by the super-class.
                     */
                    final ParameterDescriptor<?> that = (ParameterDescriptor<?>) object;
                    return getValueClass() == that.getValueClass() &&
                           Objects.deepEquals(getDefaultValue(), that.getDefaultValue()) &&
                           Objects.equals(getUnit(), that.getUnit()) &&
                           (isHeuristicMatchForName(that.getName().getCode()) ||
                            IdentifiedObjects.isHeuristicMatchForName(that, getName().getCode()));
                }
                case BY_CONTRACT: {
                    final ParameterDescriptor<?> that = (ParameterDescriptor<?>) object;
                    return getMinimumOccurs() == that.getMinimumOccurs() &&
                           getMaximumOccurs() == that.getMaximumOccurs() &&
                           getValueClass()    == that.getValueClass()    &&
                           Objects.    equals(getValidValues(),  that.getValidValues())  &&
                           Objects.    equals(getMinimumValue(), that.getMinimumValue()) &&
                           Objects.    equals(getMaximumValue(), that.getMaximumValue()) &&
                           Objects.deepEquals(getDefaultValue(), that.getDefaultValue()) &&
                           Objects.    equals(getUnit(),         that.getUnit());
                }
                case STRICT: {
                    final DefaultParameterDescriptor<?> that = (DefaultParameterDescriptor<?>) object;
                    return                    this.valueClass == that.valueClass   &&
                           Objects.    equals(this.validValues,  that.validValues) &&
                           Objects.    equals(this.valueDomain,  that.valueDomain) &&
                           Objects.deepEquals(this.defaultValue, that.defaultValue);
                }
            }
        }
        return false;
    }

    /**
     * Invoked by {@link #hashCode()} for computing the hash code when first needed.
     *
     * @return {@inheritDoc}
     */
    @Override
    protected long computeHashCode() {
        return Arrays.deepHashCode(new Object[] {valueClass, valueDomain, defaultValue}) + super.computeHashCode();
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     *
     * <p>This constructor fetches the value class and the unit of measurement from the enclosing
     * {@link DefaultParameterValue}, if presents, because those information are not presents in GML.
     * They are GeoAPI additions.</p>
     */
    @SuppressWarnings("unchecked")
    private DefaultParameterDescriptor() {
        final PropertyType<?,?> wrapper = Context.getWrapper(Context.current());
        if (wrapper instanceof CC_OperationParameter) {
            final CC_OperationParameter param = (CC_OperationParameter) wrapper;
            /*
             * This unsafe cast would be forbidden if this constructor was public or used in any context where the
             * user can choose the value of <T>. But this constructor should be invoked only during unmarshalling,
             * after the creation of the ParameterValue (this is the reverse creation order than what we normally
             * do through the public API). The 'valueClass' should be compatible with DefaultParameterValue.value,
             * and the parameterized type visible to the user should be only <?>.
             */
            valueClass  = (Class) param.valueClass;
            valueDomain = param.valueDomain;
        } else {
            valueClass  = null;
            valueDomain = null;
        }
        validValues  = null;
        defaultValue = null;
    }
}
