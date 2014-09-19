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

import javax.measure.unit.Unit;
import org.opengis.util.MemberName;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.internal.jaxb.metadata.replace.ServiceParameter;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Static;


/**
 * Static methods working on parameters and their descriptors.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.5
 * @module
 */
public final class Parameters extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Parameters() {
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
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterDescriptor<T> cast(final ParameterDescriptor<?> descriptor, final Class<T> valueClass)
            throws ClassCastException
    {
        if (descriptor != null) {
            final Class<?> actual = descriptor.getValueClass();
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends T> type.
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        descriptor.getName().getCode(), actual));
            }
        }
        return (ParameterDescriptor<T>) descriptor;
    }

    /**
     * Casts the given parameter value to the given type.
     * An exception is thrown immediately if the parameter does not have the expected value class.
     *
     * @param  <T>   The expected value class.
     * @param  value The value to cast, or {@code null}.
     * @param  type  The expected value class.
     * @return The value casted to the given type, or {@code null} if the given value was null.
     * @throws ClassCastException if the given value doesn't have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterValue<T> cast(final ParameterValue<?> value, final Class<T> type)
            throws ClassCastException
    {
        if (value != null) {
            final ParameterDescriptor<?> descriptor = value.getDescriptor();
            final Class<?> actual = descriptor.getValueClass();
            if (!type.equals(actual)) { // Same comment than cast(ParameterDescriptor)...
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        descriptor.getName().getCode(), actual));
            }
        }
        return (ParameterValue<T>) value;
    }

    /**
     * Returns the domain of valid values defined by the given descriptor, or {@code null} if none.
     * This method builds the range from the {@linkplain DefaultParameterDescriptor#getMinimumValue() minimum value},
     * {@linkplain DefaultParameterDescriptor#getMaximumValue() maximum value} and, if the values are numeric, from
     * the {@linkplain DefaultParameterDescriptor#getUnit() unit}.
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
}
