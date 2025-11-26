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
package org.apache.sis.filter.sqlmm;

import java.util.List;
import java.io.Serializable;
import java.lang.reflect.Type;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.pending.jdk.Record;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.Names;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.metadata.simple.SimpleIdentifiedObject;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.referencing.NamedIdentifier;

// Specific to the main branch:
import java.util.Set;
import javax.measure.Unit;
import org.apache.sis.util.iso.DefaultTypeName;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.pending.geoapi.filter.AvailableFunction;


/**
 * Description of a <abbr>SQLMM</abbr> function with its parameters.
 *
 * @todo Argument descriptions are incomplete. They have no good names,
 *       and the types are missing (they are {@code null}) except for geometry types.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see SQLMM#description(Geometries)
 */
final class FunctionDescription extends Record implements AvailableFunction, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -264656649425360058L;

    /**
     * The name space for function descriptions.
     */
    private static final String NAMESPACE = "SQLMM";

    /**
     * The function name in SQLMM namespace.
     *
     * @see #getName()
     */
    @SuppressWarnings("serial")     // The implementation used here is serializable.
    private final LocalName name;

    /**
     * The name and type of each argument expected by this function.
     * This list is unmodifiable.
     *
     * @see #getArguments()
     */
    @SuppressWarnings("serial")     // The implementation used here is serializable.
    private final List<ParameterDescriptor<?>> arguments;

    /**
     * The type of return value.
     *
     * @see #getReturnType()
     */
    @SuppressWarnings("serial")     // The implementation used here is serializable.
    private final TypeName result;

    /**
     * Creates a new description for the given SQLMM function.
     *
     * @param  function  the SQLMM function for which to create a description.
     * @param  library   the geometry library implementation in use.
     */
    FunctionDescription(final SQLMM function, final Geometries<?> library) {
        name = Names.createLocalName(NAMESPACE, null, function.name());
        final var args = new Argument<?>[function.maxParamCount];
        for (int i=0; i<args.length; i++) {
            final GeometryType gt;
            switch (i) {
                case 0:  gt = function.geometryType1; break;
                case 1:  gt = function.geometryType2; break;
                default: gt = null; break;
            }
            final TypeName type = (gt != null) ? gt.getTypeName(library) : null;
            args[i] = new Argument<>("arg" + (i+1), type, Argument.getValueClass(type), true);
        }
        arguments = List.of(args);
        result = function.getGeometryType().map((t) -> t.getTypeName(library))
                         .orElseGet(() -> Names.createTypeName(function.getReturnType(library)));
    }

    /**
     * Returns the function name in SQLMM namespace. This is the {@linkplain SQLMM#name() name}
     * of the enumeration value, but wrapped in a {@link LocalName} with the "SQLMM" namespace.
     *
     * @return the function name.
     */
    @Override
    public LocalName getName() {
        return name;
    }

    /**
     * Returns the name and type of each argument expected by this function.
     *
     * @return arguments that the function accepts.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<ParameterDescriptor<?>> getArguments() {
        return arguments;
    }

    /**
     * Returns the type of return value.
     */
    @Override
    public TypeName getReturnType() {
        return result;
    }

    /**
     * Description of an argument of a SQLMM function.
     *
     * @todo Argument names are not informative.
     * @todo Argument types are unknown if not geometric.
     *
     * @param  <T>  the type of the argument value.
     */
    @SuppressWarnings("EqualsAndHashcode")
    private static final class Argument<T> extends SimpleIdentifiedObject implements ParameterDescriptor<T> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1607271450895713628L;

        /**
         * Java type of the argument value, or {@code Object.class} if unknown.
         */
        private final Class<T> valueClass;

        /**
         * The value type in OGC namespace, or {@code null} if unknown.
         */
        @SuppressWarnings("serial")     // Most Apache SIS implementations are serializable.
        private final TypeName type;

        /**
         * Whether this argument is mandatory.
         */
        private final boolean mandatory;

        /**
         * Creates a new argument description.
         *
         * @param  name        name of the argument being created.
         * @param  type        the {@code valueClass} name in OGC namespace, or {@code null} if unknown.
         * @param  valueClass  Java type of the argument value, or {@code Object.class} if unknown.
         * @param  mandatory   whether this argument is mandatory.
         */
        Argument(final String name, final TypeName type, final Class<T> valueClass, final boolean mandatory) {
            this.name       = new NamedIdentifier(null, NAMESPACE, name, null, null);
            this.type       = type;
            this.valueClass = valueClass;
            this.mandatory  = mandatory;
        }

        /**
         * Returns the Java type for the specified type name, or {@code Object.class} if none.
         * This method is for computing the value returned by {@link #getValueClass()}.
         * It cannot be inlined because of parameterized type.
         *
         * @param  type  type for which to get the Java class.
         * @return the Java class for the specified type name.
         */
        static Class<?> getValueClass(final TypeName type) {
            if (type instanceof DefaultTypeName) {
                final Type t = ((DefaultTypeName) type).toJavaType().orElse(null);
                if (t instanceof Class<?>) {
                    return (Class<?>) t;
                }
            }
            return Object.class;
        }

        /**
         * Returns the Java type of the argument value, or {@code Object.class} if none.
         * This is the Java equivalent of {@link #getValueType()}.
         *
         * @return the Java type of the argument value.
         */
        @Override
        public Class<T> getValueClass() {
            return valueClass;
        }

        /**
         * Creates a new instance of parameter value initialized with the default value.
         *
         * @return a new parameter value initialized to the default value.
         */
        @Override
        public ParameterValue<T> createValue() {
            return new DefaultParameterValue<>(this);
        }

        /**
         * Returns the maximum number of times that values for this argument can be provided.
         * This is 0 for optional argument and 1 for mandatory argument.
         *
         * @return the minimum occurrence.
         */
        @Override
        public int getMinimumOccurs() {
            return mandatory ? 1 : 0;
        }

        /**
         * Returns the maximum number of times that values for this argument can be provided.
         *
         * @return the maximum occurrence.
         */
        @Override
        public int getMaximumOccurs() {
            return 1;
        }

        @Override public Set<T>        getValidValues()  {return null;}
        @Override public Comparable<T> getMinimumValue() {return null;}
        @Override public Comparable<T> getMaximumValue() {return null;}
        @Override public T             getDefaultValue() {return null;}
        @Override public Unit<?>       getUnit()         {return null;}

        /**
         * Tests whether the given object is equal to this argument description.
         *
         * @param  obj   the object to test for equality.
         * @param  mode  the strictness level of the comparison.
         * @return whether the given object describes the same argument as this.
         */
        @Override
        public boolean equals(final Object obj, final ComparisonMode mode) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Argument) {
                final var other = (Argument) obj;
                return Utilities.deepEquals(name, other.name, mode)
                    && Utilities.deepEquals(type, other.type, mode);
            }
            return false;
        }

        /**
         * Returns a hash-code value for this argument description.
         */
        @Override
        public int hashCode() {
            return name.hashCode() + type.hashCode();
        }

        /**
         * Returns a string representation of this argument.
         * Current version includes the name and the type.
         * Should be used only for debugging purposes.
         *
         * @return a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            final var sb = new StringBuilder(20);
            addType(sb.append(name.getCode()), type);
            return sb.toString();
        }
    }

    /**
     * Appends the given type name if non-null.
     *
     * @param sb    where to append the type name.
     * @param type  the type name to add, or {@code null} if none.
     */
    private static void addType(final StringBuilder sb, final TypeName type) {
        if (type != null) {
            sb.append(" : ").append(type);
        }
    }

    /**
     * Tests whether the given object is equal to this function description.
     *
     * @param  obj  the object to test for equality.
     * @return whether the given object describes the same function as this.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FunctionDescription) {
            final var other = (FunctionDescription) obj;
            return name.equals(other.name)
                && result.equals(other.result)
                && arguments.equals(other.arguments);
        }
        return false;
    }

    /**
     * Returns a hash-code value for this function description.
     */
    @Override
    public int hashCode() {
        return name.hashCode() + arguments.hashCode() + result.hashCode();
    }

    /**
     * Returns a string representation of this function with its argument.
     * Should be used only for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        final var sb = new StringBuilder(40).append(name).append('(');
        boolean isMore = false;
        for (final ParameterDescriptor<?> arg : getArguments()) {
            if (isMore) sb.append(", ");
            TypeName type = null;
            if (arg instanceof DefaultParameterDescriptor<?>) {
                type = ((DefaultParameterDescriptor<?>) arg).getValueType();
            }
            addType(sb.append(arg.getName().getCode()), type);
            isMore = true;
        }
        addType(sb.append(')'), getReturnType());
        return sb.toString();
    }
}
