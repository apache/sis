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
package org.apache.sis.filter.math;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.DoubleUnaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.sql.Types;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.filter.visitor.FunctionIdentifier;
import org.apache.sis.filter.base.Node;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.iso.Names;

// Specific to the main branch:
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.pending.geoapi.filter.AvailableFunction;


/**
 * Descriptions of mathematical operations.
 * These functions are not standard in the ANSI SQL-92 specification,
 * therefore they may or may not be available on a specific database.
 * However, most of them seem available on major database systems.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum Function implements FunctionIdentifier, AvailableFunction {
    /*
     * MIN and MAX are omitted because it needs more generic code working with Comparable.
     * We may need specializations here for the handling of NaN, but this is deferred to a
     * future version.
     */

    /**
     * The absolute value of <var>x</var>.
     */
    ABS(Math::abs),

    /**
     * The sign of input <var>x</var> as -1, 0, or 1.
     */
    SIGN(Math::signum),

    /**
     * The value of <var>x</var> rounded to the nearest whole integer.
     */
    ROUND(Math::rint),

    /**
     * The largest integer value that is less than or equal to <var>x</var>.
     */
    FLOOR(Math::floor),

    /**
     * The smallest integer value that is greater than or equal to <var>x</var>.
     * This is named {@code CEILING} in some databases.
     */
    CEIL(Math::ceil),

    /**
     * The logarithm in base 10 of <var>x</var>.
     */
    LOG10(Math::log10),

    /**
     * The logarithm in base {@linkplain Math#E e} of <var>x</var>.
     * This is named {@code LN} in some databases.
     */
    LOG(Math::log),

    /**
     * The value {@linkplain Math#E e} raised to power <var>x</var>.
     */
    EXP(Math::exp),

    /**
     * The value of <var>x</var> raised to the power of <var>y</var>.
     */
    POWER(null, null, Math::pow),

    /**
     * The square-root value of <var>x</var>.
     */
    SQRT(Math::sqrt),

    /**
     * The cubic-root value of <var>x</var>.
     */
    CBRT(Math::cbrt),

    /**
     * Hypotenuse of <var>x</var> and <var>y</var>.
     */
    HYPOT(null, null, Math::hypot),

    /**
     * The arc sine of <var>x</var>.
     */
    ASIN(Math::asin),

    /**
     * The arc cosine of <var>x</var>.
     */
    ACOS(Math::acos),

    /**
     * The arc tangent of <var>x</var>.
     */
    ATAN(Math::atan),

    /**
     * The arc tangent of <var>y</var>/<var>x</var>.
     * Note that <var>y</var> is the first argument and <var>x</var> is the second argument.
     */
    ATAN2(null, null, Math::atan2),

    /**
     * The hyperbolic sine of <var>x</var>.
     */
    SINH(Math::sinh),

    /**
     * The hyperbolic cosine of <var>x</var>.
     */
    COSH(Math::cosh),

    /**
     * The hyperbolic tangent of <var>x</var>.
     */
    TANH(Math::tanh),

    /**
     * Returns whether the specified number is neither infinite of NaN.
     */
    IS_FINITE(Double::isFinite, null, null),

    /**
     * Returns whether the specified number is positive or negative infinity.
     */
    IS_INFINITE(Double::isInfinite, null, null),

    /**
     * Returns whether the specified number is a Not-a-Number (NaN) value.
     */
    IS_NAN(Double::isNaN, null, null);

    /**
     * Synonymous of some function.
     */
    private static final Map<String, Function> ALIASES = Map.of(
            "CEILING",    CEIL,
            "LN",         LOG,
            "isFinite",   IS_FINITE,
            "isInfinite", IS_INFINITE,
            "isNaN",      IS_NAN);

    /**
     * The mathematical function to invoke if this operation is a predicate, or {@code null}.
     */
    final DoublePredicate filter;

    /**
     * The mathematical function to invoke if this operation is unary, or {@code null}.
     */
    final DoubleUnaryOperator unary;

    /**
     * The mathematical function to invoke if this operation is binary, or {@code null}.
     * Usually, only one of the {@link #unary} and {@code binary} fields is non-null.
     * But we may allow both of them to be non-null if the function if overloaded.
     */
    final DoubleBinaryOperator binary;

    /**
     * The name of this function, created when first needed.
     *
     * @see #getFunctionName()
     */
    private ScopedName name;

    /**
     * Description of the result, created when first needed.
     *
     * @see #getResultType()
     */
    private DefaultAttributeType<?> resultType;

    /**
     * Creates a new function description for a unary operation.
     */
    private Function(final DoubleUnaryOperator math) {
        filter = null;
        unary  = math;
        binary = null;
    }

    /**
     * Creates a new function description.
     */
    private Function(final DoublePredicate filter, final DoubleUnaryOperator unary, final DoubleBinaryOperator binary) {
        this.filter = filter;
        this.unary  = unary;
        this.binary = binary;
    }

    /**
     * Returns the minimum number of parameters expected by this function.
     */
    final int getMinParameterCount() {
        if (unary  != null || filter != null) return 1;
        if (binary != null) return 2;
        return 0;
    }

    /**
     * Returns the maximum number of parameters expected by this function.
     */
    final int getMaxParameterCount() {
        if (binary != null) return 2;
        if (unary  != null || filter != null) return 1;
        return 0;
    }

    /**
     * Returns the function name.
     */
    @Override
    public LocalName getName() {
        return getFunctionName().tip();
    }

    /**
     * Returns the function name returned by the expression.
     *
     * @see FeatureExpression#getFunctionName()
     */
    final synchronized ScopedName getFunctionName() {
        if (name == null) {
            name = Node.createName(name());
        }
        return name;
    }

    /**
     * Returns the attribute type to declare in feature types that store result of this function.
     *
     * @return the attribute type (never {@code null}).
     */
    final synchronized DefaultAttributeType<?> getResultType() {
        if (resultType == null) {
            if (filter == null) {
                resultType = Node.createType(Double.class, name());
            } else {
                resultType = Node.createType(Boolean.class, name());
            }
        }
        return resultType;
    }

    /**
     * Returns the type of return value.
     */
    @Override
    public TypeName getReturnType() {
        return RESULT_TYPE;
    }

    /**
     * The type of values produced by all functions in this enumeration.
     */
    private static final TypeName RESULT_TYPE = Names.createTypeName(Double.class);

    /**
     * Returns the list of arguments expected by the function.
     */
    @Override
    public List<? extends ParameterDescriptor<?>> getArguments() {
        if (unary != null) {
            return List.of(X);
        } else if (this != ATAN2) {
            return List.of(X, Y);
        } else {
            return List.of(Y, X);   // Special case for `atan2(y, x)`.
        }
    }

    /**
     * Description of a function parameter.
     */
    private static final DefaultParameterDescriptor<Number> X = parameter("x"), Y = parameter("y");

    /**
     * Creates a parameter descriptor of the given name.
     */
    private static DefaultParameterDescriptor<Number> parameter(final String name) {
        return new DefaultParameterDescriptor<>(
                Map.of(DefaultParameterDescriptor.NAME_KEY, name),
                1, 1, Number.class, null, null, null);
    }

    /**
     * Returns the types of arguments and the type of the return value of this function.
     * The {@code dataTypes[0]} array element is the data type of the function's return value.
     * All other array elements are the data types of the function's parameters, in order.
     * The values of all array elements are constants of the {@link Types} class.
     *
     * @param  dataTypes  data type of the return value followed by parameters, as {@link java.sql.Types} constants.
     * @return whether the specified return type and argument data types are valid for this function.
     */
    @Override
    public int[] getSignature() {
        final int[] dataTypes = new int[getMaxParameterCount() + 1];
        Arrays.fill(dataTypes, Types.DOUBLE);
        if (filter != null) {
            dataTypes[0] = Types.BOOLEAN;
        }
        return dataTypes;
    }

    /**
     * Returns the names and aliases of all functions.
     */
    static List<String> namesAndAliases() {
        final var names = new ArrayList<String>(Containers.namesOf(Function.class));
        names.addAll(ALIASES.keySet());
        return names;
    }

    /**
     * Returns the enumeration value for the given name.
     * This method accepts synonymous.
     *
     * @param  name  the name or alias of the function.
     * @return function for the given name or alias.
     * @throws IllegalArgumentException if the given name or alias is unknown.
     */
    public static Function of(final String name) {
        Function f = ALIASES.get(name);
        if (f == null) f = valueOf(name);
        return f;
    }
}
