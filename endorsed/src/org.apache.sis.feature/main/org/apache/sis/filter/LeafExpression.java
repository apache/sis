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
package org.apache.sis.filter;

import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.feature.privy.FeatureExpression;
import org.apache.sis.feature.privy.FeatureProjectionBuilder;
import org.apache.sis.filter.internal.Node;
import org.apache.sis.math.FunctionProperty;

// Specific to the main branch:
import org.apache.sis.feature.DefaultAttributeType;


/**
 * Expressions that do not depend on any other expression.
 * Those expression may read value from a feature property, or return a constant value.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@code Feature}) used as inputs.
 * @param  <V>  the type of value computed by the expression.
 */
abstract class LeafExpression<R,V> extends Node implements FeatureExpression<R,V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4262341851590811918L;

    /**
     * Creates a new property reader.
     */
    LeafExpression() {
    }

    /**
     * Returns the expression used as parameters for this function,
     * which is an empty list.
     */
    @Override
    public final List<Expression<R,?>> getParameters() {
        return List.of();
    }

    /**
     * Returns the manner in which values are computed from given resources.
     * Since leaf expressions have no parameters, the only properties to return are the intrinsic properties
     * of this function. The default implementation assumes that there is none, but subclasses may override.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return Set.of();
    }




    /**
     * A constant, literal value that can be used in expressions.
     * The {@link #apply(Object)} method ignores the argument and always returns {@link #getValue()}.
     *
     * @param  <R>  the type of resources used as inputs.
     * @param  <V>  the type of value computed by the expression.
     */
    static class Literal<R,V> extends LeafExpression<R,V> implements org.apache.sis.pending.geoapi.filter.Literal<R,V> {
        /** The properties of this function, which returns constants. */
        private static final Set<FunctionProperty> CONSTANT =
                Set.of(FunctionProperty.ORDER_PRESERVING, FunctionProperty.ORDER_REVERSING);

        /** For cross-version compatibility. */
        private static final long serialVersionUID = -8383113218490957822L;

        /** The constant value to be returned by {@link #getValue()}. */
        @SuppressWarnings("serial")         // Not statically typed as Serializable.
        protected final V value;

        /** Creates a new literal holding the given constant value. */
        Literal(final V value) {
            this.value = value;             // Null is accepted.
        }

        @Override public Class<? super R> getResourceClass() {
            return Object.class;
        }

        /** For {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations. */
        @Override protected Collection<?> getChildren() {
            // Not `List.of(…)` because value may be null.
            return Collections.singleton(value);
        }

        /** Returns the constant value held by this object. */
        @Override public V getValue() {
            return value;
        }

        /** Returns the type of values computed by this expression. */
        @Override public Class<?> getValueClass() {
            return (value != null) ? value.getClass() : Object.class;
        }

        /** Expression evaluation, which just returns the constant value. */
        @Override public V apply(Object ignored) {
            return value;
        }

        /** Notifies that results are constant. */
        @SuppressWarnings("ReturnOfCollectionOrArrayField")             // Because immutable.
        @Override public Set<FunctionProperty> properties() {
            return CONSTANT;
        }

        /**
         * Returns an expression that provides values as instances of the specified class.
         *
         * @throws ClassCastException if values cannot be provided as instances of the specified class.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <N> Expression<R,N> toValueType(final Class<N> target) {
            try {
                final N c = ObjectConverters.convert(value, target);
                return (c != value) ? new Literal<>(c) : (Literal<R,N>) this;
            } catch (UnconvertibleObjectException e) {
                throw (ClassCastException) new ClassCastException(Errors.format(
                        Errors.Keys.CanNotConvertValue_2, getFunctionName(), target)).initCause(e);
            }
        }

        /**
         * Provides the type of values returned by {@link #apply(Object)}.
         * The returned item wraps an {@code AttributeType} named "Literal".
         * The attribute type is determined by the class of the {@linkplain #value}.
         *
         * @param  addTo  where to add the type of properties evaluated by the given expression.
         * @return handler for the added property.
         */
        @Override
        public FeatureProjectionBuilder.Item expectedType(final FeatureProjectionBuilder addTo) {
            final Class<?> valueType = getValueClass();
            DefaultAttributeType<?> propertyType;
            synchronized (TYPES) {
                propertyType = TYPES.get(valueType);
                if (propertyType == null) {
                    final Class<?> standardType = Classes.getStandardType(valueType);
                    propertyType = TYPES.computeIfAbsent(standardType, Literal::newType);
                    if (valueType != standardType) {
                        TYPES.put(valueType, propertyType);
                    }
                }
            }
            return addTo.addSourceProperty(propertyType, true);
        }

        /**
         * A cache of {@link DefaultAttributeType} instances for literal classes. Used for avoiding to create
         * duplicated instances when the literal is a common type like {@link String} or {@link Integer}.
         */
        @SuppressWarnings("unchecked")
        private static final WeakValueHashMap<Class<?>, DefaultAttributeType<?>> TYPES = new WeakValueHashMap<>((Class) Class.class);

        /**
         * Invoked when a new attribute type need to be created for the given standard type.
         * The given standard type should be a GeoAPI interface, not the implementation class.
         */
        private static <R> DefaultAttributeType<R> newType(final Class<R> standardType) {
            return createType(standardType, Names.createLocalName(null, null, "Literal"));
        }
    }




    /**
     * A literal value which is the result of transforming another literal.
     * This is used for example when a geometry is transformed to a different CRS for computational purposes.
     * This transformation should be invisible to users, so we need to provide the original expressions when needed.
     *
     * @param  <R>  the type of resources used as inputs.
     * @param  <V>  the type of value computed by the expression.
     *
     * @see BinaryGeometryFilter#original(Expression)
     */
    static final class Transformed<R,V> extends Literal<R,V> implements Optimization.OnExpression<R,V> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -5120203649333919221L;

        /** The original expression. */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        final Expression<R,?> original;

        /** Creates a new literal holding the given constant value. */
        Transformed(final V value, final Expression<R,?> original) {
            super(value);
            this.original = original;
        }

        /**
         * Returns the same literal without the reference to the original expression.
         * Since this {@code Transformed} instance will not longer be unwrapped,
         * the transformed value will become visible to users.
         */
        @Override
        public Expression<R, ? extends V> optimize(final Optimization optimization) {
            return Optimization.literal(getValue());
        }

        /**
         * Converts the transformed value if possible, or the original value as a fallback.
         *
         * @throws ClassCastException if values cannot be provided as instances of the specified class.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <N> Expression<R,N> toValueType(final Class<N> target) {
            // Same implementation as `super.toValueType(type)` except for exception handling.
            try {
                final N c = ObjectConverters.convert(value, target);
                return (c != value) ? new Literal<>(c) : (Literal<R,N>) this;
            } catch (UnconvertibleObjectException e) {
                try {
                    return original.toValueType(target);
                } catch (RuntimeException bis) {
                    final var c = new ClassCastException(Errors.format(
                            Errors.Keys.CanNotConvertValue_2, getFunctionName(), target));
                    c.initCause(e);
                    c.addSuppressed(bis);
                    throw c;
                }
            }
        }
    }
}
