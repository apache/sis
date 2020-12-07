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

import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.filter.Node;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;


/**
 * Expressions that do not depend on any other expression.
 * Those expression may read value from a feature property, or return a constant value.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class LeafExpression extends Node implements Expression, FeatureExpression {
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
     * Evaluates the expression for producing a result of the given type.
     * If this method can not produce a value of the given type, then it returns {@code null}.
     * This implementation evaluates the expression {@linkplain #evaluate(Object) in the default way},
     * then tries to convert the result to the target type.
     *
     * @param  feature  to feature to evaluate with this expression.
     * @param  target   the desired type for the expression result.
     * @return the result, or {@code null} if it can not be of the specified type.
     */
    @Override
    public final <T> T evaluate(final Object feature, final Class<T> target) {
        ArgumentChecks.ensureNonNull("target", target);
        final Object value = evaluate(feature);
        try {
            return ObjectConverters.convert(value, target);
        } catch (UnconvertibleObjectException e) {
            warning(e);
            return null;                    // As per method contract.
        }
    }




    /**
     * Expression whose value is computed by retrieving the value indicated by the provided name.
     * A property name does not store any value; it acts as an indirection to a property value of
     * the evaluated feature.
     */
    static final class Property extends LeafExpression implements org.opengis.filter.expression.PropertyName {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 3417789380239058201L;
        private static final Object MISSING = new Object();

        /** Name of the property from which to retrieve the value. */
        private final String name;

        /** Creates a new expression retrieving values from a property of the given name. */
        Property(final String name) {
            ArgumentChecks.ensureNonNull("name", name);
            this.name = name;
        }

        /** Identification of this expression. */
        @Override public String getName() {
            return "PropertyName";
        }

        /** For {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations. */
        @Override protected Collection<?> getChildren() {
            return Collections.singleton(name);
        }

        /** Returns the name of the property whose value will be returned by the {@link #evaluate(Object)} method. */
        @Override public String getPropertyName() {
            return name;
        }

        /**
         * Returns the value of the property of the given name.
         * The {@code candidate} object can be any of the following type:
         *
         * <ul>
         *   <li>A {@link Feature}, in which case {@link Feature#getPropertyValue(String)} will be invoked.</li>
         *   <li>A {@link Map}, in which case {@link Map#get(Object)} will be invoked.</li>
         * </ul>
         *
         * If no value is found for the given feature, then this method returns {@code null}.
         */
        @Override
        public Object evaluate(final Object candidate) {
            if (candidate instanceof Feature) {
                Object value = ((Feature) candidate).getValueOrFallback(name, MISSING);
                if (value == MISSING) {
                    warning("Property " + name + " undefined on type " + ((Feature) candidate).getType().getName());
                    // Null will be returned below.
                } else {
                    return value;
                }
            } else if (candidate instanceof Map<?,?>) {
                return ((Map<?,?>) candidate).get(name);
            }
            return null;
        }

        /**
         * Provides the expected type of values produced by this expression when a feature of the given type is evaluated.
         *
         * @param  valueType  the type of features to be evaluated by the given expression.
         * @param  addTo      where to add the type of properties evaluated by the given expression.
         * @return builder of the added property, or {@code null} if this method can not add a property.
         * @throws IllegalArgumentException if this method can not determine the property type for the given feature type.
         */
        @Override
        public PropertyTypeBuilder expectedType(final FeatureType valueType, final FeatureTypeBuilder addTo) {
            PropertyType type = valueType.getProperty(name);        // May throw IllegalArgumentException.
            while (type instanceof Operation) {
                final IdentifiedType result = ((Operation) type).getResult();
                if (result != type && result instanceof PropertyType) {
                    type = (PropertyType) result;
                } else if (result instanceof FeatureType) {
                    return addTo.addAssociation((FeatureType) result).setName(name);
                } else {
                    return null;
                }
            }
            return addTo.addProperty(type);
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(final ExpressionVisitor visitor, final Object extraData) {
            return visitor.visit(this, extraData);
        }
    }




    /**
     * A constant, literal value that can be used in expressions.
     * The {@link #evaluate(Object)} method ignores the argument and always returns {@link #getValue()}.
     */
    static final class Literal extends LeafExpression implements org.opengis.filter.expression.Literal {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -8383113218490957822L;

        /** The constant value to be returned by {@link #getValue()}. */
        private final Object value;

        /** Creates a new literal holding the given constant value. */
        Literal(final Object value) {
            ArgumentChecks.ensureNonNull("value", value);
            this.value = value;
        }

        /** Identification of this expression. */
        @Override public String getName() {
            return "Literal";
        }

        /** For {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations. */
        @Override protected Collection<?> getChildren() {
            return Collections.singleton(value);
        }

        /** Returns the constant value held by this object. */
        @Override public Object getValue() {
            return value;
        }

        /** Expression evaluation, which just returns the constant value. */
        @Override public Object evaluate(Object ignored) {
            return value;
        }

        /**
         * Provides the type of values returned by {@link #evaluate(Object)}
         * wrapped in an {@link AttributeType} named "Literal".
         *
         * @param  addTo  where to add the type of properties evaluated by the given expression.
         * @return builder of the added property.
         */
        @Override
        public PropertyTypeBuilder expectedType(FeatureType ignored, final FeatureTypeBuilder addTo) {
            final Class<?> valueType = value.getClass();
            AttributeType<?> propertyType = TYPES.get(valueType);
            if (propertyType == null) {
                final Class<?> standardType = Classes.getStandardType(valueType);
                propertyType = TYPES.computeIfAbsent(standardType, Literal::newType);
                if (valueType != standardType) {
                    TYPES.put(valueType, propertyType);
                }
            }
            return addTo.addProperty(propertyType);
        }

        /**
         * A cache of {@link AttributeType} instances for literal classes. Used for avoiding to create
         * duplicated instances when the literal is a common type like {@link String} or {@link Integer}.
         */
        @SuppressWarnings("unchecked")
        private static final WeakValueHashMap<Class<?>, AttributeType<?>> TYPES = new WeakValueHashMap<>((Class) Class.class);

        /**
         * Invoked when a new attribute type need to be created for the given standard type.
         * The given standard type should be a GeoAPI interface, not the implementation class.
         */
        private static <T> AttributeType<T> newType(final Class<T> standardType) {
            return createType(standardType, Names.createLocalName(null, null, "Literal"));
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(final ExpressionVisitor visitor, final Object extraData) {
            return visitor.visit(this, extraData);
        }
    }
}
