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
import java.util.Optional;
import java.util.function.Predicate;
import java.net.URI;
import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import org.opengis.util.MemberName;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.*;                                         // We use almost all types from this package.
import org.apache.sis.xml.bind.metadata.replace.ServiceParameter;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Printable;
import org.apache.sis.util.resources.Errors;


/**
 * Convenience methods for fetching parameter values despite the variations in parameter names, value types and units.
 * See {@link DefaultParameterValueGroup} javadoc for a description of the standard way to get and set a particular
 * parameter in a group. The remaining of this javadoc is specific to Apache SIS.
 *
 * <h2>Convenience methods</h2>
 * This class provides the following convenience static methods:
 * <ul>
 *   <li>{@link #cast(ParameterValue, Class) cast(…, Class)} for type safety with parameterized types.</li>
 *   <li>{@link #getMemberName(ParameterDescriptor)} for inter-operability between ISO 19111 and ISO 19115.</li>
 *   <li>{@link #getValueDomain(ParameterDescriptor)} for information purpose.</li>
 *   <li>{@link #copy(ParameterValueGroup, ParameterValueGroup)} for copying values into an existing instance.</li>
 * </ul>
 *
 * Most instance methods in this class follow the same naming pattern
 * than the methods provided by the {@link ParameterValue} interface.
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
 * <h2>Fetching parameter values despite different names, types or units</h2>
 * The common way to get a parameter is to invoke the {@link #parameter(String)} method.
 * This {@code Parameters} class provides alternative ways, using a {@link ParameterDescriptor} argument
 * instead of a {@code String} argument. Those descriptors provide additional information like the various
 * {@linkplain DefaultParameterDescriptor#getAlias() aliases} under which the same parameter may be known.
 * By using this information, {@code Parameters} can choose the most appropriate parameter name or alias
 * (by searching for a common {@linkplain org.apache.sis.referencing.ImmutableIdentifier#getAuthority() authority})
 * when it delegates its work to the {@code parameter(String)} method.
 *
 * <h3>Example</h3>
 * The same parameter may be known under different names. For example, the
 * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getSemiMajorAxis()
 * length of the semi-major axis of the ellipsoid} is commonly known as {@code "semi_major"}.
 * But that parameter can also be named {@code "semi_major_axis"}, {@code "earth_radius"} or simply {@code "a"}
 * in other libraries. When fetching parameter values, we do not always know in advance which of the above-cited
 * names is recognized by an arbitrary {@code ParameterValueGroup} instance.
 *
 * <p>{@code Parameters} uses also the descriptor information for applying type and unit conversions
 * (i.e. returned values are converted to the units of measurement specified by the given parameter descriptor).</p>
 *
 *
 * <h2>Note for subclass implementers</h2>
 * This class does not implement any method from the {@link ParameterValueGroup} interface
 * (this class is not named “{@code AbstractParameterValueGroup}” for that reason).
 * Extending this class or extending {@link Object} make almost no difference for implementers;
 * {@code Parameters} purpose is mostly to extend the API for users convenience.
 * All methods in this class get their information from the {@link ParameterValueGroup} methods.
 * In addition, unless otherwise specified, methods in this class is isolated from all others:
 * overriding one method has no impact on other methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.4
 */
@XmlTransient
public abstract class Parameters implements ParameterValueGroup, Cloneable, Printable {
    /**
     * For subclass constructors only.
     */
    protected Parameters() {
    }

    /**
     * Returns {@code true} if the given parameter group is a non-null instance created by {@code unmodifiable(…)}.
     *
     * @param  parameters  the parameter group to test. Can be {@code null}.
     * @return whether the given parameters are non-null and unmodifiable.
     *
     * @since 1.3
     */
    public static boolean isUnmodifiable(final ParameterValueGroup parameters) {
        return (parameters instanceof UnmodifiableParameterValueGroup);
    }

    /**
     * Returns the given parameter value group as an unmodifiable {@code Parameters} instance.
     * If the given parameters is already an unmodifiable instance of {@code Parameters},
     * then it is returned as-is. Otherwise this method copies all parameter values in a new,
     * unmodifiable, parameter group instance.
     *
     * @param  parameters  the parameters to make unmodifiable, or {@code null}.
     * @return an unmodifiable group with the same parameters as the given group,
     *         or {@code null} if the given argument was null.
     *
     * @see DefaultParameterValue#unmodifiable(ParameterValue)
     *
     * @since 0.7
     */
    public static Parameters unmodifiable(final ParameterValueGroup parameters) {
        return UnmodifiableParameterValueGroup.create(parameters);
    }

    /**
     * Returns the given parameter value group as an unmodifiable {@code Parameters} instance
     * with some parameters hidden. The hidden parameters are excluded from the list returned
     * by {@link Parameters#values()}, but are otherwise still accessible when the hidden
     * parameters is explicitly named in a call to {@link #parameter(String)}.
     *
     * <h4>Use case</h4>
     * This method is used for hiding parameters that should be inferred from the context.
     * For example, the {@code "semi_major"} and {@code "semi_minor"} parameters are included
     * in the list of {@link org.opengis.referencing.operation.MathTransform} parameters
     * because that class has no way to know the values if they are not explicitly provided.
     * But those semi-axis length parameters should not be included in the list of
     * {@link org.opengis.referencing.operation.CoordinateOperation} parameters
     * because they are inferred from the context (the source and target CRS).
     *
     * @param  parameters  the parameters to make unmodifiable, or {@code null}.
     * @param  filter      specifies which source parameters to keep visible, or {@code null} if no filtering.
     * @return an unmodifiable group with the parameters of the given group to keep visible,
     *         or {@code null} if the {@code parameters} argument was null.
     *
     * @since 1.3
     */
    public static Parameters unmodifiable(final ParameterValueGroup parameters,
            final Predicate<? super GeneralParameterDescriptor> filter)
    {
        return FilteredParameters.create(UnmodifiableParameterValueGroup.create(parameters), filter);
    }

    /**
     * Returns the given parameter value group as a {@code Parameters} instance.
     * If the given parameters is already an instance of {@code Parameters}, then it is returned as-is.
     * Otherwise this method returns a wrapper which delegate all method invocations to the given instance.
     *
     * <p>This method provides a way to get access to the non-static {@code Parameters} methods, like
     * {@link #getValue(ParameterDescriptor)}, for an arbitrary {@code ParameterValueGroup} instance.</p>
     *
     * @param  parameters  the object to cast or wrap, or {@code null}.
     * @return the given argument as an instance of {@code Parameters} (may be the same reference),
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

        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
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
     * @param  <T>         the expected value class.
     * @param  descriptor  the descriptor to cast, or {@code null}.
     * @param  valueClass  the expected value class.
     * @return the descriptor cast to the given value class, or {@code null} if the given descriptor was null.
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
            /*
             * We require a strict equality - not type.isAssignableFrom(actual) - because in
             * the latter case we could have (to be strict) to return a <? extends T> type.
             */
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Resources.format(Resources.Keys.IllegalParameterType_2,
                        Verifier.getDisplayName(descriptor), actual));
            }
        }
        return (ParameterDescriptor<T>) descriptor;
    }

    /**
     * Casts the given parameter value to the given type.
     * An exception is thrown immediately if the parameter does not have the expected value class.
     *
     * @param  <T>         the expected value class.
     * @param  parameter   the parameter to cast, or {@code null}.
     * @param  valueClass  the expected value class.
     * @return the value cast to the given type, or {@code null} if the given value was null.
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
            if (!valueClass.equals(actual)) {       // Same comment as cast(ParameterDescriptor).
                throw new ClassCastException(Resources.format(Resources.Keys.IllegalParameterType_2,
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
     * @param  parameters  the parameter values from which to get the descriptors, or {@code null}.
     * @return the descriptors of the given parameter values, or {@code null} if the {@code parameters} argument was null.
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
     *   <li>If the {@linkplain DefaultParameterDescriptor#getName() primary name} is an instance of {@code MemberName}
     *       (for example a {@link org.apache.sis.referencing.NamedIdentifier} subclass), returns that primary name.</li>
     *   <li>Otherwise this method searches for the first {@linkplain DefaultParameterDescriptor#getAlias() alias}
     *       which is an instance of {@code MemberName} (a subtype of aliases type).
     *       If found, that alias is returned.</li>
     *   <li>If no alias is found, then this method tries to build a {@code MemberName} from the primary name and the
     *       {@linkplain DefaultParameterDescriptor#getValueClass() value class}, using the mapping defined in
     *       {@link org.apache.sis.util.iso.DefaultTypeName} javadoc.</li>
     * </ul>
     *
     * This method can be used as a bridge between the parameter object
     * defined by ISO 19111 (namely {@code CC_OperationParameter}) and the one
     * defined by ISO 19115 (namely {@code SV_Parameter}).
     *
     * @param  parameter  the parameter from which to get the name (may be {@code null}).
     * @return the member name, or {@code null} if none.
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
     * @param  descriptor  the parameter descriptor, or {@code null}.
     * @return the domain of valid values, or {@code null} if none.
     *
     * @see DefaultParameterDescriptor#getValueDomain()
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Range<?> getValueDomain(final ParameterDescriptor<?> descriptor) {
        if (descriptor != null) {
            if (descriptor instanceof DefaultParameterDescriptor<?>) {
                return ((DefaultParameterDescriptor<?>) descriptor).getValueDomain();
            }
            Class<?> valueClass = descriptor.getValueClass();
            final Comparable<?> minimumValue = descriptor.getMinimumValue();
            final Comparable<?> maximumValue = descriptor.getMaximumValue();
            if (valueClass == null) {       // Should never be null, but invalid objects exist.
                valueClass = Classes.findCommonClass(Classes.getClass(minimumValue), Classes.getClass(maximumValue));
                if (valueClass == null) {
                    valueClass = Object.class;
                }
            }
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
     * @param  source  the parameter for which the name is wanted.
     * @return the name of the given parameter. May be {@code null} if there is no name at all,
     *         but such nameless descriptors are not legal.
     */
    private String getName(final GeneralParameterDescriptor source) {
        final ParameterDescriptorGroup descriptor = getDescriptor();
        if (descriptor != null) {                                   // Paranoiac check (should never be null)
            final Identifier group = descriptor.getName();
            if (group != null) {                                    // Paranoiac check (should never be null)
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
    ParameterValue<?> parameterIfExist(final String name) throws ParameterNotFoundException {
        int i1 = 0, i2 = 0;
        ParameterValue<?> first     = null;
        ParameterValue<?> ambiguity = null;
        final List<GeneralParameterValue> values = values();
        final int size = values.size();
        for (int i=0; i<size; i++) {
            final GeneralParameterValue value = values.get(i);
            if (value instanceof ParameterValue<?>) {
                final ParameterValue<?> param = (ParameterValue<?>) value;
                if (IdentifiedObjects.isHeuristicMatchForName(param.getDescriptor(), name)) {
                    if (first == null) {
                        first = param;
                        i1 = i;
                    } else {
                        ambiguity = param;
                        i2 = i;
                    }
                }
            }
        }
        /*
         * If there is no ambiguity, we are done. In case of ambiguity we should throw an exception.
         * However, we will not throw the exception if this method is invoked from the getParameter(…)
         * method of a Parameters instance wrapping a non-SIS implementation. The reason is that for
         * foreigner implementations, the package-private getParameter(…) method will conservatively
         * delegate to the public parameter(…) method, in case the implementer overrides it. But for
         * Apache SIS implementations in this package, we rely on the exception being thrown.
         *
         * Note that all classes in this package except UnmodifiableParameterValueGroup override this
         * method in a way that unconditionally throw the exception.  UnmodifiableParameterValueGroup
         * is the class that needs the exception to be thrown.
         */
        if (ambiguity == null || !isKnownImplementation()) {
            return first;
        }
        final GeneralParameterDescriptor d1 = first    .getDescriptor();
        final GeneralParameterDescriptor d2 = ambiguity.getDescriptor();
        final String message;
        if (d1 == d2) {
            message = Errors.format(Errors.Keys.MultiOccurenceValueAtIndices_3, name, i1, i2);
        } else {
            message = Errors.format(Errors.Keys.AmbiguousName_3,
                        IdentifiedObjects.toString(d1.getName()),
                        IdentifiedObjects.toString(d2.getName()), name);
        }
        throw new ParameterNotFoundException(message, name);
    }

    /**
     * Returns the parameter value for the specified operation parameter.
     * This method tries to do the same work as {@link #parameter(String)} but without
     * instantiating optional parameters if that parameter was not already instantiated.
     *
     * <h4>Performance note</h4>
     * Profiling shows that this method is costly. To mitigate the problem, {@link DefaultParameterValueGroup}
     * overrides this method with a quick comparisons of descriptor references before to fallback on this more
     * generic implementation.
     *
     * @param  parameter  the parameter to search.
     * @return the requested parameter value, or {@code null} if none.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     */
    ParameterValue<?> getParameter(final ParameterDescriptor<?> parameter) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("parameter", parameter);
        /*
         * Search for an identifier matching this group's authority. For example if this ParameterValueGroup
         * was created from an EPSG database, then we want to use the EPSG names instead of the OGC names.
         */
        final String name = getName(parameter);
        /*
         * We do not want to invoke `parameter(name)` because we do not want to create a new parameter
         * if the user did not supplied one.  We search the parameter ourself (so we don't create any)
         * and return null if we do not find any.
         *
         * If we find a parameter, we can return it directly only if this object is an instance of a known
         * implementation (currently DefaultParameterValueGroup and MapProjectionParameters), otherwise we
         * do not know if the user overrode the `parameter` method in a way incompatible with this method.
         * We do not use Class.getMethod(…).getDeclaringClass() because it is presumed not worth the cost.
         * In case of doubt, we delegate to `parameter(name)`.
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
     * Returns the <abbr>URI</abbr> of the <abbr>GML</abbr> document
     * or <abbr>WKT</abbr> file from which the parameter values are read.
     * This information can be used together with {@code getValue(ParameterDescriptor<URI>)} for
     * resolving a parameter value as a path relative to the GML or WKT file declaring the parameter.
     * Note that the source file is not necessarily the same for all parameters in a group, because a GML
     * document could define parameters in files referenced by different {@code xlink:href} attribute values.
     *
     * @return the <abbr>URI</abbr> of the document from which the parameter values are read.
     *
     * @see DefaultParameterValue#getSourceFile()
     * @see org.apache.sis.io.wkt.WKTFormat#getSourceFile()
     * @see org.apache.sis.xml.MarshalContext#getDocumentURI()
     * @see URI#resolve(URI)
     *
     * @since 1.5
     */
    public Optional<URI> getSourceFile(final ParameterDescriptor<?> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> p = getParameter(parameter);
        if (p instanceof DefaultParameterValue<?>) {
            return ((DefaultParameterValue<?>) p).getSourceFile();
        }
        return Optional.empty();
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
     *           {@linkplain org.apache.sis.referencing.ImmutableIdentifier#getAuthority() authority}, if any;</li>
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
     * @param  <T>        the type of the parameter value.
     * @param  parameter  the name or alias of the parameter to look for, together with the desired type and unit of value.
     * @return the requested parameter value if it exists, or the {@linkplain DefaultParameterDescriptor#getDefaultValue()
     *         default value} otherwise (which may be {@code null}).
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws UnconvertibleObjectException if the parameter value cannot be converted to the expected type.
     *
     * @see #getMandatoryValue(ParameterDescriptor)
     * @see #getOrCreate(ParameterDescriptor)
     * @see DefaultParameterValueGroup#parameter(String)
     * @see DefaultParameterValue#getValue()
     *
     * @since 0.6
     */
    public <T> T getValue(final ParameterDescriptor<T> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> p = getParameter(parameter);
        if (p != null) {
            final Class<T> type = parameter.getValueClass();
            final Unit<?>  unit = parameter.getUnit();
            Object value = p.getValue();
            if (value != null) {                // Tested first for avoiding IllegalStateException in call to doubleValue().
                if (unit != null) {
                    if (type.isArray()) {
                        value = p.doubleValueList(unit);
                    } else {
                        value = p.doubleValue(unit);
                    }
                }
                return ObjectConverters.convert(value, type);
            }
        }
        return parameter.getDefaultValue();     // Returning null is allowed here.
    }

    /**
     * Returns the value of the parameter identified by the given descriptor, or throws an exception if none.
     * The default implementation performs the same work as {@link #getValue(ParameterDescriptor)} and verifies
     * that the returned value is non-null.
     *
     * @param  <T>        the type of the parameter value.
     * @param  parameter  the name or alias of the parameter to look for, together with the desired type and unit of value.
     * @return the requested parameter value if it exists, or the {@linkplain DefaultParameterDescriptor#getDefaultValue()
     *         default value} otherwise provided that it is not {@code null}.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getValue(ParameterDescriptor)
     * @see #getOrCreate(ParameterDescriptor)
     *
     * @since 0.7
     */
    public <T> T getMandatoryValue(final ParameterDescriptor<T> parameter) throws ParameterNotFoundException {
        final ParameterValue<?> p = getParameter(parameter);
        if (p != null) {
            final Class<T> type = parameter.getValueClass();
            final Unit<?>  unit = parameter.getUnit();
            final Object value;
            if (unit == null) {
                value = p.getValue();
            } else if (type.isArray()) {
                value = p.doubleValueList(unit);
            } else {
                value = p.doubleValue(unit);
            }
            final T result;
            if (value != null) {
                result = ObjectConverters.convert(value, type);
            } else {
                result = parameter.getDefaultValue();
            }
            if (result != null) {
                return result;
            }
        }
        throw new IllegalStateException(Resources.format(Resources.Keys.MissingValueForParameter_1,
                Verifier.getDisplayName(parameter)));
    }

    /**
     * Returns the default value of the given descriptor, or throws an exception if the
     * descriptor does not define a default value. This check should be kept consistent
     * with the {@link DefaultParameterValue#missingOrIncompatibleValue(Object)} check.
     */
    private static <T> T defaultValue(final ParameterDescriptor<T> parameter) throws IllegalStateException {
        final T value = parameter.getDefaultValue();
        if (value != null) {
            return value;
        } else {
            throw new IllegalStateException(Resources.format(Resources.Keys.MissingValueForParameter_1,
                    Verifier.getDisplayName(parameter)));
        }
    }

    /**
     * Returns the boolean value of the parameter identified by the given descriptor.
     * See {@link #getValue(ParameterDescriptor)} for more information about how this
     * method uses the given {@code parameter} argument.
     *
     * @param  parameter  the name or alias of the parameter to look for.
     * @return the requested parameter value if it exists, or the <strong>non-null</strong>
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
     * @param  parameter  the name or alias of the parameter to look for.
     * @return the requested parameter value if it exists, or the <strong>non-null</strong>
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
     * @param  parameter  the name or alias of the parameter to look for.
     * @return the requested parameter values if they exist, or the <strong>non-null</strong>
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
     * @param  parameter  the name or alias of the parameter to look for.
     * @return the requested parameter value if it exists, or the <strong>non-null</strong>
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
     * Returns the floating point value of the parameter identified by the given descriptor,
     * converted to the given unit of measurement. See {@link #getValue(ParameterDescriptor)}
     * for more information about how this method uses the given {@code parameter} argument.
     *
     * @param  parameter  the name or alias of the parameter to look for.
     * @param  unit       the desired unit of measurement.
     * @return the requested parameter value if it exists, or the <strong>non-null</strong>
     *         {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} otherwise.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     * @throws IllegalArgumentException if the specified unit is invalid for the parameter.
     *
     * @see DefaultParameterValue#doubleValue(Unit)
     *
     * @since 1.3
     */
    public double doubleValue(final ParameterDescriptor<? extends Number> parameter, final Unit<?> unit) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("unit", unit);
        final ParameterValue<?> value = getParameter(parameter);
        if (value != null) {
            return value.doubleValue(unit);
        } else {
            double d = defaultValue(parameter).doubleValue();
            final Unit<?> source = parameter.getUnit();
            if (source != null) try {
                d = source.getConverterToAny(unit).convert(d);
            } catch (IncommensurableException e) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2, source, unit), e);
            }
            return d;
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
     * @param  parameter  the name or alias of the parameter to look for.
     * @return the requested parameter values if they exists, or the <strong>non-null</strong>
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
     * @param  parameter  the name or alias of the parameter to look for.
     * @return the requested parameter value if it exists, or the <strong>non-null</strong>
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
     * {@snippet lang="java" :
     *     return cast(parameter(name), parameter.getValueClass());
     *     }
     *
     * where {@code name} is a {@code parameter} {@linkplain DefaultParameterDescriptor#getName() name}
     * or {@linkplain DefaultParameterDescriptor#getAlias() alias} chosen by the same algorithm as
     * {@link #getValue(ParameterDescriptor)}.
     *
     * @param  <T>        the type of the parameter value.
     * @param  parameter  the parameter to look for.
     * @return the requested parameter instance.
     * @throws ParameterNotFoundException if the given {@code parameter} name or alias is not legal for this group.
     *
     * @see #getValue(ParameterDescriptor)
     * @see #getMandatoryValue(ParameterDescriptor)
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
     * @return a copy of this group of parameter values.
     *
     * @see #copy(ParameterValueGroup, ParameterValueGroup)
     */
    @Override
    public Parameters clone() {
        try {
            return (Parameters) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);            // Should never happen since we are Cloneable.
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
     * @param  values       the parameter values to copy.
     * @param  destination  where to copy the values.
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
        final Map<String,Integer> occurrences = new HashMap<>();
        for (final GeneralParameterValue value : values.values()) {
            final String name = value.getDescriptor().getName().getCode();
            final int occurrence = occurrences.getOrDefault(name, ZERO);
            if (value instanceof ParameterValueGroup) {
                /*
                 * Contains sub-group - invokes `copy` recursively.
                 * The target group may exist, but not necessarily.
                 */
                final List<ParameterValueGroup> groups = destination.groups(name);
                copy((ParameterValueGroup) value, (occurrence < groups.size())
                        ? groups.get(occurrence) : destination.addGroup(name));
            } else {
                /*
                 * Single parameter - copy the value, with special care for value with units
                 * and for multi-occurrences. Note that the latter is not allowed by ISO 19111
                 * but supported by SIS implementation.
                 */
                final ParameterValue<?> source = (ParameterValue<?>) value;
                final ParameterValue<?> target;
                if (occurrence == 0) {
                    try {
                        target = destination.parameter(name);
                    } catch (ParameterNotFoundException cause) {
                        throw new InvalidParameterNameException(Errors.format(
                                    Errors.Keys.UnexpectedParameter_1, name), cause, name);
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
                if (source instanceof DefaultParameterValue<?> && target instanceof DefaultParameterValue<?>) {
                    ((DefaultParameterValue<?>) source).getSourceFile().ifPresent((file) ->
                            ((DefaultParameterValue<?>) target).setSourceFile(file));
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
     * @param  values  the group from which to get or create a value
     * @param  name    the name of the parameter to fetch. An exact match will be required.
     * @param  n       number of occurrences to skip before to return or create the parameter.
     * @return the <var>n</var>th occurrence (zero-based) of the parameter of the given name.
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
            /*
             * We do not botter formatting a good error message for now, because
             * this method is currently invoked only with increasing index values.
             */
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
    @Override
    public void print() {
        ParameterFormat.print(this);
    }
}
