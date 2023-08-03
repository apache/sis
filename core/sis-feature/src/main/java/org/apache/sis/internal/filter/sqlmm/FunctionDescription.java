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
package org.apache.sis.internal.filter.sqlmm;

import java.util.List;
import org.opengis.util.LocalName;
import org.opengis.util.TypeName;
import org.apache.sis.util.iso.Names;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryType;

// Branch-dependent imports
import org.opengis.filter.capability.Argument;
import org.opengis.filter.capability.AvailableFunction;


/**
 * Description of a SQLMM function with its parameters.
 *
 * @todo Argument descriptions are incomplete. They have no good names,
 *       and the types are missing (they are {@code null}) except for geometry types.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see SQLMM#description(Geometries)
 *
 * @since 1.4
 */
final class FunctionDescription implements AvailableFunction {
    /**
     * The function name in SQLMM namespace.
     *
     * @see #getName()
     */
    private final LocalName name;

    /**
     * The name and type of each argument expected by this function.
     * This list is unmodifiable.
     *
     * @see #getArguments()
     */
    private final List<Argument> arguments;

    /**
     * The type of return value.
     *
     * @see #getReturnType()
     */
    private final TypeName result;

    /**
     * Creates a new description for the given SQLMM function.
     *
     * @param  function  the SQLMM function for which to create a description.
     * @param  library   the geometry library implementation in use.
     */
    FunctionDescription(final SQLMM function, final Geometries<?> library) {
        name = createLocalName(function.name());
        final Arg[] args = new Arg[function.maxParamCount];
        for (int i=0; i<args.length; i++) {
            final GeometryType type;
            switch (i) {
                case 0:  type = function.geometryType1; break;
                case 1:  type = function.geometryType2; break;
                default: type = null; break;
            }
            args[i] = new Arg(i, (type != null) ? type.getTypeName(library) : null);
        }
        arguments = List.of(args);
        result = function.getGeometryType().map((t) -> t.getTypeName(library))
                         .orElseGet(() -> Names.createTypeName(function.getReturnType(library)));
    }

    /**
     * Creates a name which is local in SQLMM namespace.
     *
     * @param  name  text of the name to create.
     * @return name which is local in SQLMM namespace.
     */
    private static LocalName createLocalName(final String name) {
        return Names.createLocalName("SQLMM", null, name);
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
    public List<Argument> getArguments() {
        return arguments;
    }

    /**
     * {@return the type of return value}.
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
     */
    private static final class Arg implements Argument {
        /**
         * The argument name.
         */
        private final LocalName name;

        /**
         * The value type, or {@code null} if unknown.
         */
        private final TypeName type;

        /**
         * Creates a new argument description.
         *
         * @param  i     index of the argument being created.
         * @param  type  the argument type, or {@code null} if unknown.
         */
        Arg(final int i, final TypeName type) {
            this.name = createLocalName("arg" + (i+1));
            this.type = type;
        }

        /**
         * {@return the name of the argument}.
         */
        @Override
        public LocalName getName() {
            return name;
        }

        /**
         * {@return the name of the type of the argument}.
         */
        @Override
        public TypeName getValueType() {
            return type;
        }

        /**
         * Tests whether the given object is equal to this argument description.
         *
         * @param  obj  the object to test for equality.
         * @return whether the given object describes the same argument than this.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Arg) {
                final var other = (Arg) obj;
                return name.equals(other.name)
                    && type.equals(other.type);
            }
            return false;
        }

        /**
         * {@return a hash-code value for this argument description}.
         */
        @Override
        public int hashCode() {
            return name.hashCode() + type.hashCode();
        }

        /**
         * {@return a string representation of this argument}.
         * Current version includes the name and the type.
         * Should be used only for debugging purposes.
         */
        @Override
        public String toString() {
            final var sb = new StringBuilder(20);
            addType(sb.append(name), type);
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
     * @return whether the given object describes the same function than this.
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
     * {@return a hash-code value for this function description}.
     */
    @Override
    public int hashCode() {
        return name.hashCode() + arguments.hashCode() + result.hashCode();
    }

    /**
     * {@return a string representation of this function with its argument}.
     * Should be used only for debugging purposes.
     */
    @Override
    public String toString() {
        final var sb = new StringBuilder(40).append(name).append('(');
        boolean isMore = false;
        for (final Argument arg : getArguments()) {
            if (isMore) sb.append(", ");
            addType(sb.append(arg.getName()), arg.getValueType());
            isMore = true;
        }
        addType(sb.append(')'), getReturnType());
        return sb.toString();
    }
}
