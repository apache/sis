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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.unit.Unit;
import org.opengis.util.MemberName;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.*; // We use almost all types from this package.
import org.apache.sis.internal.jaxb.metadata.replace.ServiceParameter;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Debug;

import static org.apache.sis.referencing.IdentifiedObjects.isHeuristicMatchForName;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Convenience methods for fetching parameter values despite the variations in parameter names, value types and units.
 * See {@link DefaultParameterValueGroup} javadoc for a description of the standard way to get and set a particular
 * parameter in a group. The remaining of this javadoc is specific to Apache SIS.
 *
 * <div class="section">Convenience static methods</div>
 * This class provides the following convenience static methods:
 * <ul>
 *   <li>{@link #cast(ParameterValue, Class) cast(…, Class)} for type safety with parameterized types.</li>
 *   <li>{@link #getMemberName(ParameterDescriptor)} for inter-operability between ISO 19111 and ISO 19115.</li>
 *   <li>{@link #getValueDomain(ParameterDescriptor)} for information purpose.</li>
 *   <li>{@link #copy(ParameterValueGroup, ParameterValueGroup)} for copying values into an existing instance.</li>
 * </ul>
 *
 *
 * <div class="section">Fetching parameter values despite different names, types or units</div>
 * The common way to get a parameter is to invoke the {@link #parameter(String)} method.
 * This {@code Parameters} class provides an alternative way, using a {@link ParameterDescriptor} argument
 * instead than a {@code String}. The methods in this class use the additional information provided by the
 * descriptor for choosing a {@code String} argument that the above-cited {@code parameter(String)} method
 * is more likely to know (by giving preference to a {@linkplain DefaultParameterDescriptor#getName() name}
 * or {@linkplain DefaultParameterDescriptor#getAlias() alias} defined by a common
 * {@linkplain org.apache.sis.metadata.iso.ImmutableIdentifier#getAuthority() authority}),
 * and for applying type and unit conversions.
 *
 * <div class="note"><b>Example:</b>
 * The same parameter may be known under different names. For example the
 * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getSemiMajorAxis()
 * length of the semi-major axis of the ellipsoid} is commonly known as {@code "semi_major"}.
 * But that parameter can also be named {@code "semi_major_axis"}, {@code "earth_radius"} or simply {@code "a"}
 * in other libraries. When fetching parameter values, we do not always know in advance which of the above-cited
 * names is recognized by an arbitrary {@code ParameterValueGroup} implementation.
 *
 * <p>This uncertainty is mitigated with the Apache SIS implementation since
 * {@link DefaultParameterValueGroup#parameter(String)} compares the given {@code String} argument
 * against all parameter's {@linkplain DefaultParameterDescriptor#getAlias() aliases} in addition
 * to the {@linkplain DefaultParameterDescriptor#getName() name}.
 * However we do not have the guarantee that all implementations do that.</p></div>
 *
 * The method names in this class follow the names of methods provided by the {@link ParameterValue} interface.
 * Those methods are themselves inspired by JDK methods:
 *
 * <table class="sis">
 *   <caption>Methods fetching parameter value</caption>
 *   <tr><th>{@code Parameters} method</th>                     <th>{@code ParameterValue} method</th>                                     <th>JDK methods</th></tr>
 *   <tr><td>{@link #getValue(ParameterDescriptor)}</td>        <td>{@link DefaultParameterValue#getValue()        getValue()}</td>        <td></td></tr>
 *   <tr><td>{@link #booleanValue(ParameterDescriptor)}</td>    <td>{@link DefaultParameterValue#booleanValue()    booleanValue()}</td>    <td>{@link Boolean#booleanValue()}</td></tr>
 *   <tr><td>{@link #intValue(ParameterDescriptor)}</td>        <td>{@link DefaultParameterValue#intValue()        intValue()}</td>        <td>{@link Number#intValue()}</td></tr>
 *   <tr><td>{@link #intValueList(ParameterDescriptor)}</td>    <td>{@link DefaultParameterValue#intValueList()    intValueList()}</td>    <td></td></tr>
 *   <tr><td>{@link #doubleValue(ParameterDescriptor)}</td>     <td>{@link DefaultParameterValue#doubleValue()     doubleValue()}</td>     <td>{@link Number#doubleValue()}</td></tr>
 *   <tr><td>{@link #doubleValueList(ParameterDescriptor)}</td> <td>{@link DefaultParameterValue#doubleValueList() doubleValueList()}</td> <td></td></tr>
 *   <tr><td>{@link #stringValue(ParameterDescriptor)}</td>     <td>{@link DefaultParameterValue#stringValue()     stringValue()}</td>     <td></td></tr>
 * </table>
 *
 *
 * <div class="section">Note for subclass implementors</div>
 * All methods in this class get their information from the {@link ParameterValueGroup} methods.
 * In addition, each method in this class is isolated from all others: overriding one method has
 * no impact on other methods.
 *
 * <div class="note"><b>Note on this class name:</b>
 * Despite implementing the {@link ParameterValueGroup} interface, this class is not named
 * {@code AbstractParameterValueGroup} because it does not implement any method from the interface.
 * Extending this class or extending {@link Object} make almost no difference for implementors.
 * The intend of this {@code Parameters} class is rather to extend the API with methods
 * that are convenient for the way Apache SIS uses parameters.
 * In other words, this class is intended for users rather than implementors.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlTransient
public abstract class Parameters implements ParameterValueGroup, Cloneable {
    /**
     * For subclass constructors only.
     */
    protected Parameters() {
    }

    /**
     * Returns the given parameter value group as an unmodifiable {@code Parameters} instance.
     * If the given parameters is already an unmodifiable instance of {@code Parameters},
     * then it is returned as-is. Otherwise this method copies all parameter values in a new,
     * unmodifiable, parameter group instance.
     *
     * @param  parameters The parameters to make unmodifiable, or {@code null}.
     * @return An unmodifiable group with the same parameters than the given group,
     *         or {@code null} if the given argument was null.
     *
     * @since 0.7
     *
     * @see DefaultParameterValue#unmodifiable(ParameterValue)
     */
    public static Parameters unmodifiable(final ParameterValueGroup parameters) {
        return UnmodifiableParameterValueGroup.create(parameters);
    }

    /**
     * Returns the given parameter value group as a {@code Parameters} instance.
     * If the given parameters is already an instance of {@code Parameters}, then it is returned as-is.
     * Otherwise this method returns a wrapper which delegate all method invocations to the given instance.
     *
     * <p>This method provides a way to get access to the non-static {@code Parameters} methods, like
     * {@link #getValue(ParameterDescriptor)}, for an arbitrary {@code ParameterValueGroup} instance.</p>
     *
     * @param  parameters The object to cast or wrap, or {@code null}.
     * @return The given argument as an instance of {@code Parameters} (may be the same reference),
     *         or {@code null} if the given argument was null.
     */
    public static Parameters castOrWrap(final ParameterValueGroup parameters) {
        if (parameters == null || parameters instanceof Parameters) {
            return (Parameters) parameters;
        } else {
            return new Wrapper(parameters);
        }
    }

    /** Wrappers used as a fallback by {@link Parameters#castOrWrap(ParameterValueGroup)}. */
    @SuppressWarnings("CloneDoesntCallSuperClone")
    private static final class Wrapper extends Parameters implements Serializable {
        private static final long serialVersionUID = -5491790565456920471L;
        private final ParameterValueGroup delegate;
        Wrapper(final ParameterValueGroup delegate) {this.delegate = delegate;}

        @Override public ParameterDescriptorGroup    getDescriptor()        {return delegate.getDescriptor();}
        @Override public List<GeneralParameterValue> values()               {return delegate.values();}
        @Override public ParameterValue<?>           parameter(String name) {return delegate.parameter(name);}
        @Override public List<ParameterValueGroup>   groups   (String name) {return delegate.groups(name);}
        @Override public ParameterValueGroup         addGroup (String name) {return delegate.addGroup(name);}
        @Override public Parameters                  clone()                {return new Wrapper(delegate.clone());}
    }

    /**
     * Casts the given parameter descriptor to the given type.
     * An exception is thrown immediately if the parameter does not have the expected
     * {@linkplain DefaultParameterDescriptor#getValueClass() value class}.
     *
     * @param  <T>        The expected value class.
     * @param  descriptor The descriptor to cast, or {@code null}.
     * @param  valueClass The expected value class.
     * @return The descriptor casted to the given value class, or {@code null} if the given descriptor was null.
     * @throws ClassCastException if the given descriptor does not have the expected value class.
     *
     * @see Class#cast(Object)
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterDescriptor<T> cast(final ParameterDescriptor<?> descriptor, final Class<T> valueClass)
            throws ClassCastException
    {
        ArgumentChecks.ensureNonNull("valueClass", valueClass);
        if (descriptor != null) {
            final Class<?> actual = descriptor.getValueClass();
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends T> type.
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        Verifier.getDisplayName(descriptor), actual));
            }
        }
        return (ParameterDescriptor<T>) descriptor;
    }

    /**
     * Casts the given parameter value to the given type.
     * An exception is thrown immediately if the parameter does not have the expected value class.
     *
     * @param  <T>        The expected value class.
     * @param  parameter  The parameter to cast, or {@code null}.
     * @param  valueClass The expected value class.
     * @return The value casted to the given type, or {@code null} if the given value was null.
     * @throws ClassCastException if the given value doesn't have the expected value class.
     *
     * @see Class#cast(Object)
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterValue<T> cast(final ParameterValue<?> parameter, final Class<T> valueClass)
            throws ClassCastException
    {
        ArgumentChecks.ensureNonNull("valueClass", valueClass);
        if (parameter != null) {
            final ParameterDescriptor<?> descriptor = parameter.getDescriptor();
            final Class<?> actual = descriptor.getValueClass();
            if (!valueClass.equals(actual)) {   // Same comment than cast(ParameterDescriptor).
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        Verifier.getDisplayName(descriptor), actual));
            }
        }
        return (ParameterValue<T>) parameter;
    }

    /**
     * Returns the descriptors of the given parameters, in the same order.
     * Special cases:
     *
     * <ul>
     *   <li>If the given array is {@code null}, then this method returns {@code null}.
     *   <li>If an element of the given array is {@code null}, then the corresponding
     *       element of the returned array is also {@code null}.</li>
     * </ul>
     *
     * @param  parameters The parameter values from which to get the descriptors, or {@code null}.
     * @return The descriptors of the given parameter values, or {@code null} if the {@code parameters} argument was null.
     *
     * @since 0.6
     */
    public static GeneralParameterDescriptor[] getDescriptors(final GeneralParameterValue... parameters) {
        if (parameters == null) {
            return null;
        }
        final GeneralParameterDescriptor[] descriptors = new GeneralParameterDescriptor[parameters.length];
        for (int i=0; i<parameters.length; i++) {
            final GeneralParameterValue p = parameters[i];
            if (p != null) {
                descriptors[i] = p.getDescriptor();
            }
        }
        return descriptors;
    }

    /**
     * Gets the parameter name as an instance of {@code MemberName}.
     * This method performs the following checks:
     *
     * <ul>
     *   <li>If the {@linkplain DefaultParameterDescriptor#getName() primary name} is an instance of {@code MemberName},
     *       returns that primary name.</li>
     *   <li>Otherwise this method searches for the first {@linkplain DefaultParameterDescriptor#getAlias() alias}
     *       which is an instance of {@code MemberName}. If found, that alias is returned.</li>
     *   <li>If no alias is found, then this method tries to build a member name from the primary name and the
     *       {@linkplain DefaultParameterDescriptor#getValueClass() value class}, using the mapping defined in
     *       {@link org.apache.sis.util.iso.DefaultTypeName} javadoc.</li>
     * </ul>
     *
     * This method can be used as a bridge between the parameter object
     * defined by ISO 19111 (namely {@code CC_OperationParameter}) and the one
     * defined by ISO 19115 (namely {@code SV_Parameter}).
     *
     * @param  parameter The parameter from which to get the name (may be {@code null}).
     * @return The member name, or {@code null} if none.
     *
     * @see org.apache.sis.util.iso.Names#createMemberName(CharSequence, String, CharSequence, Class)
     *
     * @since 0.5
     */
    public static MemberName getMemberName(final ParameterDescriptor<?> parameter) {
        return ServiceParameter.getMemberName(parameter);
    }

    /**
     * Returns the domain of valid values defined by the given descriptor, or {@code null} if none.
     * This method performs the following operations:
     *
     * <ul>
     *   <li>If the given parameter is an instance of {@code DefaultParameterDescriptor},
     *       delegate to {@link DefaultParameterDescriptor#getValueDomain()}.</li>
     *   <li>Otherwise builds the range from the {@linkplain DefaultParameterDescriptor#getMinimumValue() minimum value},
     *       {@linkplain DefaultParameterDescriptor#getMaximumValue() maximum value} and, if the values are numeric, from
     *       the {@linkplain DefaultParameterDescriptor#getUnit() unit}.</li>
     * </ul>
     *
     * @param  descriptor The parameter descriptor, or {@code null}.
     * @return The domain of valid values, or {@code null} if none.
     *
     * @see DefaultParameterDescriptor#getValueDomain()
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Range<?> getValueDomain(final ParameterDescriptor<?> descriptor) {
        if (descriptor != null) {
            if (descriptor instanceof DefaultParameterDescriptor<?>) {
                return ((DefaultParameterDescriptor<?>) descriptor).getValueDomain();
            }
            final Class<?> valueClass = descriptor.getValueClass();
            final Comparable<?> minimumValue = descriptor.getMinimumValue();
            final Comparable<?> maximumValue = descriptor.getMaximumValue();
            if ((minimumValue == null || valueClass.isInstance(minimumValue)) &&
                (maximumValue == null || valueClass.isInstance(maximumValue)))
            {
                if (Number.class.isAssignableFrom(valueClass)) {
                    final Unit<?> unit = descriptor.getUnit();
                    if (unit != null) {
                        return new MeasurementRange((Class) valueClass,
                                (Number) minimumValue, true, (Number) maximumValue, true, unit);
                    } else if (minimumValue != null || maximumValue != null) {
                        return new NumberRange((Class) valueClass,
                                (Number) minimumValue, true, (Number) maximumValue, true);
                    }
                } else if (minimumValue != null || maximumValue != null) {
                    return new Range(valueClass, minimumValue, true, maximumValue, true);
                }
            }
        }
        return null;
    }

    /**
     * Returns the name or alias of the given parameter for the authority code space expected by this group.
     * If no name or alias for this group's authority can be found, then the primary name will be returned.
     *
     * @param  source The parameter for which the name is wanted.
     * @return The name of the given parameter.
     */
    private String getName(final GeneralParameterDescriptor source) {
        final ParameterDescriptorGroup descriptor = getDescriptor();
        if (descriptor != null) {   // Paranoiac check (should never be null)
            final Identifier group = descriptor.getName();
            if (group != null) {    // Paranoiac check (should never be null)
                final Citation authority = group.getAuthority();
                final String name = IdentifiedObjects.getName(source, authority);
                if (name != null || authority == null) {
                    return name;
                }
            }
        }
        return IdentifiedObjects.getName(source, null);
    }

    /**
     * Returns the parameter of the given name, or {@code null} if it does not exist.
     * The default implementation iterates over the {@link #values()} and compares the descriptor names.
     * The {@link DefaultParameterValueGroup} subclass will override this method with a more efficient
     * implementation which avoid creating some deferred parameters.
     */
    @SuppressWarnings("null")
    ParameterValue<?> parameterIfExist(final String name) throws ParameterNotFoundException {
        ParameterValue<?> fallback  = null;
        ParameterValue<?> ambiguity = null;
        for (final GeneralParameterValue value : values()) {
            if (value instanceof ParameterValue<?>) {
                final ParameterValue<?> param = (ParameterValue<?>) value;
                final ParameterDescriptor<?> descriptor = param.getDescriptor();
                if (name.equals(descriptor.getName().toString())) {
                    return param;
                }
                if (isHeuristicMatchForName(descriptor, name)) {
                    if (fallback == null) {
                        fallback = param;
                    } else {
                        ambiguity = param;
                    }
                }
            }
        }
        if (ambiguity != null) {
            throw new ParameterNotFoundException(Errors.format(Errors.Keys.AmbiguousName_3,
                    IdentifiedObjects.toString(fallback .getDescriptor().getName()),
                    IdentifiedObjects.toString(ambiguity.getDescriptor().getName()), name), name);
        }
        return fallback;
    }

    /**
     * Returns the parameter value for the specified operation parameter.
     * This method tries to do the same work than {@link #parameter(String)} but without
     * instantiating optional parameters if that parameter was not already instantiated.
     *
     * @param  parameter The parameter to search.
     * @return The requested parameter value, or {@code null} if none.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     */
    private ParameterValue<?> getParameter(final ParameterDescriptor<?> parameter) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("parameter", parameter);
        /*
         * Search for an identifier matching this group's authority. For example if this ParameterValueGroup
         * was created from an EPSG database, then we want to use the EPSG names instead than the OGC names.
         */
        final String name = getName(parameter);
        /*
         * We do not want to invoke 'parameter(name)' because we do not want to create a new parameter
         * if the user did not supplied one.  We search the parameter ourself (so we don't create any)
         * and return null if we do not find any.
         *
         * If we find a parameter, we can return it directly only if this object is an instance of a known
         * implementation (currently DefaultParameterValueGroup and MapProjectionParameters), otherwise we
         * do not know if the user overrode the 'parameter' method in a way incompatible with this method.
         * We do not use Class.getMethod(…).getDeclaringClass() because it is presumed not worth the cost.
         * In case of doubt, we delegate to 'parameter(name)'.
         */
        final ParameterValue<?> value = parameterIfExist(name);
        if (value == null || isKnownImplementation()) {
            return value;
        } else {
            return parameter(name);
        }
    }

    /**
     * Returns {@code true} if this class is an implementation of an instance which is known to not override
     * {@link #parameter(String)} in a way incompatible with {@link #parameterIfExist(String)}.
     * The {@link DefaultParameterValueGroup} class needs to override this method.
     */
    boolean isKnownImplementation() {
        return false;
    }

    /**
     * Returns the value of the parameter identified by the given descriptor, or {@code null} if none.
     * This method uses the following information from the given {@code parameter} descriptor:
     *
     * <ul>
     *   <li>The most appropriate {@linkplain DefaultParameterDescriptor#getName() name} or
     *       {@linkplain DefaultParameterDescriptor#getAlias() alias} to use for searching
     *       in this {@code ParameterValueGroup}, chosen as below:
     *     <ul>
     *       <li>a name or alias defined by the same
     *           {@linkplain org.apache.sis.metadata.iso.ImmutableIdentifier#getAuthority() authority}, if any;</li>
     *       <li>an arbitrary name or alias otherwise.</li>
     *     </ul>
     *   </li>
     *   <li>The {@linkplain DefaultParameterDescriptor#getDefaultValue() default value}
     *       to return if there is no value associated to the above-cited name or alias.</li>
     *   <li>The {@linkplain DefaultParameterDescriptor#getUnit() unit of measurement}
     *       (if any) of numerical value to return.</li>
     *   <li>The {@linkplain DefaultParameterDescriptor#getValueClass() type} of value to return.</li>
     * </ul>
     *
     * This method can be useful when the {@code ParameterDescriptor} are known in advance, for example in the
     * implementation of some {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod coordinate
     * operation method}. If the caller has no such {@code ParameterDescriptor} at hand, then the
     * {@link DefaultParameterValueGroup#parameter(String) parameter(String)} method is probably more convenient.
     *
     * @param  <T> The type of the parameter value.
     * @param  parameter The name or alias of the parameter to look for, together with the desired type and unit of value.
     * @return The requested parameter value if it exists, or the {@linkplain DefaultParameterDescriptor#getDefaultValue()
     *         default value} otherwise (which may be {@code null}).
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     *
     * @see DefaultParameterValueGroup#parameter(String)
     * @see DefaultParameterValue#getValue()
     *
     * @since 0.6
     */
    public <T> T getValue(final ParameterDescriptor<T> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> p = getParameter(parameter);
        if (p != null) {
            final Object value;
            final Class<T> type = parameter.getValueClass();
            final Unit<?>  unit = parameter.getUnit();
            if (unit == null) {
                value = p.getValue();
            } else if (type.isArray()) {
                value = p.doubleValueList(unit);
            } else {
                value = p.doubleValue(unit);
            }
            if (value != null) {
                return ObjectConverters.convert(value, type);
            }
        }
        return parameter.getDefaultValue();     // Returning null is allowed here.
    }

    /**
     * Returns the value of the parameter identified by the given descriptor, or throws an exception if none.
     * The default implementation invokes {@link #getValue(ParameterDescriptor)} and verifies that the returned
     * value is non-null.
     *
     * @param  <T> The type of the parameter value.
     * @param  parameter The name or alias of the parameter to look for, together with the desired type and unit of value.
     * @return The requested parameter value if it exists, or the {@linkplain DefaultParameterDescriptor#getDefaultValue()
     *         default value} otherwise provided that it is not {@code null}.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @since 0.7
     */
    public <T> T getMandatoryValue(final ParameterDescriptor<T> parameter) throws ParameterNotFoundException {
        final T value = getValue(parameter);
        if (value != null) {
            return value;
        } else {
            throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForParameter_1,
                    Verifier.getDisplayName(parameter)));
        }
    }

    /**
     * Returns the default value of the given descriptor, or throws an exception if the
     * descriptor does not define a default value. This check should be kept consistent
     * with the {@link DefaultParameterValue#incompatibleValue(Object)} check.
     */
    private static <T> T defaultValue(final ParameterDescriptor<T> parameter) throws IllegalStateException {
        final T value = parameter.getDefaultValue();
        if (value != null) {
            return value;
        } else {
            throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForParameter_1,
                    Verifier.getDisplayName(parameter)));
        }
    }

    /**
     * Returns the boolean value of the parameter identified by the given descriptor.
     * See {@link #getValue(ParameterDescriptor)} for more information about how this
     * method uses the given {@code parameter} argument.
     *
     * @param  parameter The name or alias of the parameter to look for.
     * @return The requested parameter value if it exists, or the <strong>non-null</strong>
     *         {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} otherwise.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see DefaultParameterValue#booleanValue()
     *
     * @since 0.6
     */
    public boolean booleanValue(final ParameterDescriptor<Boolean> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> value = getParameter(parameter);
        return (value != null) ? value.booleanValue() : defaultValue(parameter);
    }

    /**
     * Returns the integer value of the parameter identified by the given descriptor.
     * See {@link #getValue(ParameterDescriptor)} for more information about how this
     * method uses the given {@code parameter} argument.
     *
     * @param  parameter The name or alias of the parameter to look for.
     * @return The requested parameter value if it exists, or the <strong>non-null</strong>
     *         {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} otherwise.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see DefaultParameterValue#intValue()
     *
     * @since 0.6
     */
    public int intValue(final ParameterDescriptor<? extends Number> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> value = getParameter(parameter);
        return (value != null) ? value.intValue() : defaultValue(parameter).intValue();
    }

    /**
     * Returns the integer values of the parameter identified by the given descriptor.
     * See {@link #getValue(ParameterDescriptor)} for more information about how this
     * method uses the given {@code parameter} argument.
     *
     * @param  parameter The name or alias of the parameter to look for.
     * @return The requested parameter values if they exist, or the <strong>non-null</strong>
     *         {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} otherwise.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see DefaultParameterValue#intValueList()
     *
     * @since 0.6
     */
    public int[] intValueList(final ParameterDescriptor<int[]> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> value = getParameter(parameter);
        return (value != null) ? value.intValueList() : defaultValue(parameter);
    }

    /**
     * Returns the floating point value of the parameter identified by the given descriptor.
     * See {@link #getValue(ParameterDescriptor)} for more information about how this method
     * uses the given {@code parameter} argument.
     *
     * <p>If the given descriptor supplies a {@linkplain DefaultParameterDescriptor#getUnit()
     * unit of measurement}, then the returned value will be converted into that unit.</p>
     *
     * @param  parameter The name or alias of the parameter to look for.
     * @return The requested parameter value if it exists, or the <strong>non-null</strong>
     *         {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} otherwise.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see DefaultParameterValue#doubleValue(Unit)
     *
     * @since 0.6
     */
    public double doubleValue(final ParameterDescriptor<? extends Number> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> value = getParameter(parameter);
        if (value != null) {
            final Unit<?> unit = parameter.getUnit();
            return (unit != null) ? value.doubleValue(unit) : value.doubleValue();
        } else {
            return defaultValue(parameter).doubleValue();
        }
    }

    /**
     * Returns the floating point values of the parameter identified by the given descriptor.
     * See {@link #getValue(ParameterDescriptor)} for more information about how this method
     * uses the given {@code parameter} argument.
     *
     * <p>If the given descriptor supplies a {@linkplain DefaultParameterDescriptor#getUnit()
     * unit of measurement}, then the returned values will be converted into that unit.</p>
     *
     * @param  parameter The name or alias of the parameter to look for.
     * @return The requested parameter values if they exists, or the <strong>non-null</strong>
     *         {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} otherwise.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see DefaultParameterValue#doubleValueList(Unit)
     *
     * @since 0.6
     */
    public double[] doubleValueList(final ParameterDescriptor<double[]> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> value = getParameter(parameter);
        if (value != null) {
            final Unit<?> unit = parameter.getUnit();
            return (unit != null) ? value.doubleValueList(unit) : value.doubleValueList();
        } else {
            return defaultValue(parameter);
        }
    }

    /**
     * Returns the string value of the parameter identified by the given descriptor.
     * See {@link #getValue(ParameterDescriptor)} for more information about how this
     * method uses the given {@code parameter} argument.
     *
     * @param  parameter The name or alias of the parameter to look for.
     * @return The requested parameter value if it exists, or the <strong>non-null</strong>
     *         {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} otherwise.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see DefaultParameterValue#stringValue()
     *
     * @since 0.6
     */
    public String stringValue(final ParameterDescriptor<? extends CharSequence> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> value = getParameter(parameter);
        return (value != null) ? value.stringValue() : defaultValue(parameter).toString();
    }

    /**
     * Returns the parameter identified by the given descriptor.
     * If the identified parameter is optional and not yet created, then it will be created now.
     *
     * <p>The default implementation is equivalent to:</p>
     *
     * {@preformat java
     *     return cast(parameter(name), parameter.getValueClass());
     * }
     *
     * where {@code name} is a {@code parameter} {@linkplain DefaultParameterDescriptor#getName() name}
     * or {@linkplain DefaultParameterDescriptor#getAlias() alias} chosen by the same algorithm than
     * {@link #getValue(ParameterDescriptor)}.
     *
     * @param  <T> The type of the parameter value.
     * @param  parameter The parameter to look for.
     * @return The requested parameter instance.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     *
     * @see DefaultParameterValueGroup#parameter(String)
     *
     * @since 0.6
     */
    public <T> ParameterValue<T> getOrCreate(final ParameterDescriptor<T> parameter) throws ParameterNotFoundException {
        return cast(parameter(getName(parameter)), parameter.getValueClass());
    }

    /**
     * Returns a copy of this group of parameter values.
     * The default implementation performs a <em>shallow</em> copy,
     * but subclasses are encouraged to perform a <em>deep</em> copy.
     *
     * @return A copy of this group of parameter values.
     *
     * @see #copy(ParameterValueGroup, ParameterValueGroup)
     */
    @Override
    public Parameters clone() {
        try {
            return (Parameters) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);   // Should never happen since we are Cloneable
        }
    }

    /**
     * Copies the values of a parameter group into another parameter group.
     * All values in the {@code source} group shall be valid for the {@code destination} group,
     * but the {@code destination} may have more parameters.
     * Sub-groups are copied recursively.
     *
     * <p>A typical usage of this method is for transferring values from an arbitrary implementation
     * to some specific implementation, or to a parameter group using a different but compatible
     * {@linkplain DefaultParameterValueGroup#getDescriptor() descriptor}.</p>
     *
     * @param  values The parameters values to copy.
     * @param  destination Where to copy the values.
     * @throws InvalidParameterNameException if a {@code source} parameter name is unknown to the {@code destination}.
     * @throws InvalidParameterValueException if the value of a {@code source} parameter is invalid for the {@code destination}.
     *
     * @see #clone()
     *
     * @since 0.5
     */
    public static void copy(final ParameterValueGroup values, final ParameterValueGroup destination)
            throws InvalidParameterNameException, InvalidParameterValueException
    {
        final Integer ZERO = 0;
        final Map<String,Integer> occurrences = new HashMap<String,Integer>();
        for (final GeneralParameterValue value : values.values()) {
            final String name = value.getDescriptor().getName().getCode();
            final int occurrence = JDK8.getOrDefault(occurrences, name, ZERO);
            if (value instanceof ParameterValueGroup) {
                /*
                 * Contains sub-group - invokes 'copy' recursively.
                 * The target group may exist, but not necessarily.
                 */
                final List<ParameterValueGroup> groups = destination.groups(name);
                copy((ParameterValueGroup) value, (occurrence < groups.size())
                        ? groups.get(occurrence) : destination.addGroup(name));
            } else {
                /*
                 * Single parameter - copy the value, with special care for value with units
                 * and for multi-occurrences. Not that the later is not allowed by ISO 19111
                 * but supported by SIS implementation.
                 */
                final ParameterValue<?> source = (ParameterValue<?>) value;
                final ParameterValue<?> target;
                if (occurrence == 0) {
                    try {
                        target = destination.parameter(name);
                    } catch (ParameterNotFoundException cause) {
                        throw (InvalidParameterNameException) new InvalidParameterNameException(Errors.format(
                                    Errors.Keys.UnexpectedParameter_1, name), name).initCause(cause);
                    }
                } else {
                    target = (ParameterValue<?>) getOrCreate(destination, name, occurrence);
                }
                final Object  v    = source.getValue();
                final Unit<?> unit = source.getUnit();
                if (unit == null) {
                    target.setValue(v);
                } else if (v instanceof Number) {
                    target.setValue(((Number) v).doubleValue(), unit);
                } else if (v instanceof double[]) {
                    target.setValue((double[]) v, unit);
                } else if (v != target.getValue()) {    // Accept null value if the target value is already null.
                    throw new InvalidParameterValueException(Errors.format(
                            Errors.Keys.IllegalArgumentValue_2, name, v), name, v);
                }
            }
            occurrences.put(name, occurrence + 1);
        }
    }

    /**
     * Returns the <var>n</var>th occurrence of the parameter of the given name.
     * This method is not public because ISO 19111 does not allow multi-occurrences of parameter values
     * (this is a SIS-specific flexibility). Current implementation is not very efficient, but it should
     * not be an issue if this method is rarely invoked.
     *
     * @param  values The group from which to get or create a value
     * @param  name   The name of the parameter to fetch. An exact match will be required.
     * @param  n      Number of occurrences to skip before to return or create the parameter.
     * @return The <var>n</var>th occurrence (zero-based) of the parameter of the given name.
     * @throws IndexOutOfBoundsException if {@code n} is greater than the current number of
     *         parameters of the given name.
     */
    private static GeneralParameterValue getOrCreate(final ParameterValueGroup values, final String name, int n) {
        for (final GeneralParameterValue value : values.values()) {
            if (name.equals(value.getDescriptor().getName().getCode())) {
                if (--n < 0) {
                    return value;
                }
            }
        }
        if (n == 0) {
            final GeneralParameterValue value = values.getDescriptor().descriptor(name).createValue();
            values.values().add(value);
            return value;
        } else {
            // We do not botter formatting a good error message for now, because
            // this method is currently invoked only with increasing index values.
            throw new IndexOutOfBoundsException(name);
        }
    }

    /**
     * Returns a string representation of this group.
     * The default implementation delegates to {@link ParameterFormat}.
     *
     * <p>This method is for information purpose only and may change in future SIS version.</p>
     *
     * @since 0.7
     */
    @Debug
    @Override
    public String toString() {
        return ParameterFormat.sharedFormat(this);
    }

    /**
     * Prints a string representation of this group to the {@linkplain System#out standard output stream}.
     * If a {@linkplain java.io.Console console} is attached to the running JVM (i.e. if the application
     * is run from the command-line and the output is not redirected to a file) and if Apache SIS thinks
     * that the console supports the ANSI escape codes (a.k.a. X3.64), then a syntax coloring will be applied.
     *
     * <p>This is a convenience method for debugging purpose and for console applications.</p>
     *
     * @since 0.7
     */
    @Debug
    public void print() {
        ParameterFormat.print(this);
    }
}
