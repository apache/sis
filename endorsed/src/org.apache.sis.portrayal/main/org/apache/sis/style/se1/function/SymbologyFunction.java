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

package org.apache.sis.style.se1.function;

import java.awt.Color;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.iso.Names;


/**
 * Base class of the functions defined by Symbology Encoding.
 * Those functions transform values, for example to colors or to formatted strings.
 *
 * @author  Johann Sorel (Geomatys)
 *
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param <V>  the type of values computed by the function.
 */
abstract class SymbologyFunction<R,V> implements Expression<R,V>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2542262069958327049L;

    /**
     * The scope of function names.
     */
    private static final LocalName SCOPE = Names.createLocalName(null, null, "SE");

    /**
     * The function name, without scope.
     */
    private final String name;

    /**
     * The type of values computed by this function.
     */
    private final Class<?> resultType;

    /**
     * Creates a new function.
     *
     * @param  name        the function name, without scope.
     * @param  resultType  the type of values computed by this function.
     */
    SymbologyFunction(final String name, final Class<?> resultType) {
        this.name       = name;
        this.resultType = resultType;
    }

    /**
     * Returns the name of this function in the {@code "SE"} scope.
     *
     * @return the name of this function.
     */
    @Override
    public final ScopedName getFunctionName() {
        return Names.createScopedName(SCOPE, null, name);
    }

    /**
     * Returns the class of resources expected by this function, which is the one of the first parameter.
     *
     * @return type of resources accepted by this function.
     */
    @Override
    public Class<? super R> getResourceClass() {
        return getParameters().get(0).getResourceClass();
    }

    /**
     * Returns all properties of this function, for {@link #equals(Object)} and {@link #hashCode()}.
     */
    abstract Object[] properties();

    /**
     * Returns an expression doing the same operation as this expression, but providing values of the given type.
     * If the values computed by this function are not already of the given type, they are converted.
     *
     * @param  <N>     compile-time value of {@code target} type.
     * @param  target  desired type of expression results.
     * @return expression doing the same operation as this expression but with results of the specified type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <N> Expression<R,N> toValueType(final Class<N> target) {
        if (target.isAssignableFrom(resultType)) {
            return (Expression<R,N>) this;
        }
        return new Converted<>(this, target);
    }

    /**
     * Compares this function with the given object for equality.
     *
     * @param  obj  the object to compare with this function.
     * @return whether the two objects are equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        return (obj != null) && obj.getClass() == getClass()
                && Arrays.equals(properties(), ((SymbologyFunction<?,?>) obj).properties());
    }

    /**
     * Returns a hash code value for this function.
     *
     * @return a hash code value.
     */
    @Override
    public final int hashCode() {
        return name.hashCode() ^ Arrays.hashCode(properties());
    }

    /**
     * Returns a string representation of this function for debugging purposes.
     *
     * @return a string representation of this function.
     */
    @Override
    public final String toString() {
        return name + getParameters();
    }

    /**
     * Converts the given value to a number, or returns NaN if not convertible.
     */
    static double number(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value != null) try {
            final Double n = ObjectConverters.convert(value, Double.class);
            if (n != null) return n;
        } catch (UnconvertibleObjectException e) {
            // Not a number, return NaN.
        }
        return Double.NaN;
    }

    /**
     * Converts the given value to a string, or returns {@code null} if the value is null.
     */
    static String string(final Object value) {
        return (value != null) ? value.toString() : null;
    }

    /**
     * Converts the given value to a color, or returns {@code null} if not convertible.
     * Strings are decoded as hexadecimal {@code #RRGGBB} or {@code #AARRGGBB} colors.
     */
    static Color color(final Object value) {
        if (value instanceof Color) {
            return (Color) value;
        }
        if (value instanceof CharSequence) {
            final String text = value.toString().trim();
            try {
                if (text.startsWith("#") && text.length() == 9) {
                    return new Color((int) Long.parseLong(text.substring(1), 16), true);
                }
                return Color.decode(text);
            } catch (NumberFormatException e) {
                // Not a color, return null.
            }
        }
        return null;
    }

    /**
     * Returns the keyword used in Symbology Encoding for the given enumeration value.
     * For example {@code FRONT_TO_BACK} is mapped to {@code "frontToBack"}.
     */
    static String keyword(final Enum<?> value) {
        final StringBuilder buffer = new StringBuilder();
        boolean upper = false;
        for (final char c : value.name().toLowerCase(Locale.ENGLISH).toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                buffer.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the given expression converted to the given type. Conversions to colors, or conversions that the
     * expression cannot do itself, are done by a wrapper which supports colors encoded as strings.
     */
    @SuppressWarnings("unchecked")
    static <R,N> Expression<R,N> convert(final Expression<R,?> expression, final Class<N> target) {
        if (expression instanceof Converted && ((Converted<?,?>) expression).target == target) {
            return (Expression<R,N>) expression;
        }
        if (target != Color.class) try {            // Apache SIS converters do not support strings such as "#FF0000".
            return expression.toValueType(target);
        } catch (ClassCastException | IllegalArgumentException e) {
            // Fallback on the wrapper below.
        }
        return new Converted<>(expression, target);
    }

    /**
     * An expression converting the values computed by another expression to another type.
     * Colors encoded as strings are supported. Values which cannot be converted are replaced by null.
     */
    private static final class Converted<R,N> implements Expression<R,N>, Serializable {
        private static final long serialVersionUID = 2683539069245106406L;

        /** The expression computing the values to convert. */
        private final Expression<R,?> expression;

        /** The type of values. */
        final Class<N> target;

        Converted(final Expression<R,?> expression, final Class<N> target) {
            this.expression = expression;
            this.target     = Objects.requireNonNull(target);
        }

        @Override public ScopedName            getFunctionName()  {return expression.getFunctionName();}
        @Override public Class<? super R>      getResourceClass() {return expression.getResourceClass();}
        @Override public List<Expression<R,?>> getParameters()    {return expression.getParameters();}
        @Override public <T> Expression<R,T>   toValueType(Class<T> type) {return convert(expression, type);}

        @Override
        public N apply(final R input) {
            final Object value = expression.apply(input);
            if (target == Color.class) {
                return target.cast(color(value));
            }
            try {
                return ObjectConverters.convert(value, target);
            } catch (UnconvertibleObjectException e) {
                return null;
            }
        }

        @Override
        public boolean equals(final Object obj) {
            return (obj instanceof Converted) && expression.equals(((Converted<?,?>) obj).expression)
                    && target == ((Converted<?,?>) obj).target;
        }

        @Override
        public int hashCode() {
            return expression.hashCode() + target.hashCode();
        }

        @Override
        public String toString() {
            return expression + " as " + target.getSimpleName();
        }
    }

    /**
     * A literal for string parameters which need to be returned by {@link #getParameters()},
     * such as the {@code "preceding"} or {@code "cubic"} keywords, or the default {@code "Rasterdata"} lookup value.
     */
    static final class Keyword<R> implements Literal<R,String>, Serializable {
        private static final long serialVersionUID = -7046519389283712245L;

        /** The keyword. */
        private final String value;

        Keyword(final String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public <N> Expression<R,N> toValueType(final Class<N> type) {
            return convert(this, type);
        }

        @Override
        public boolean equals(final Object obj) {
            return (obj instanceof Keyword) && value.equals(((Keyword<?>) obj).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode() ^ 0x6E2A9F31;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
