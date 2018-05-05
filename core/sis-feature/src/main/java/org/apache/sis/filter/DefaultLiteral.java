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

import java.util.Collections;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;
import org.opengis.util.LocalName;
import org.opengis.feature.FeatureType;
import org.opengis.feature.AttributeType;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.ExpressionVisitor;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Names;


/**
 * A constant, literal value that can be used in expressions.
 * The {@link #evaluate(Object)} method ignore the argument and always returns {@link #getValue()}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param <T>  the literal value type.
 *
 * @since 1.0
 * @module
 */
final class DefaultLiteral<T> extends AbstractExpression implements Literal, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3240145927452086297L;

    /**
     * The name of attribute types associated with literals.
     */
    private static final LocalName NAME = Names.createLocalName(null, null, "Literal");

    /**
     * A cache of {@link AttributeType} instances for literal classes. Used for avoiding to create many duplicated
     * instances for the common case where the literal is a very common type like {@link String} or {@link Integer}.
     */
    private static final ConcurrentMap<Class<?>, AttributeType<?>> TYPES = new ConcurrentHashMap<>();

    /**
     * The constant value to be returned by {@link #getValue()}.
     */
    private final T value;

    /**
     * Creates a new literal holding the given constant value.
     * It is caller responsibility to ensure that the given argument is non-null.
     */
    DefaultLiteral(final T value) {
        this.value = value;
    }

    /**
     * Returns the constant value held by this object.
     */
    @Override
    public T getValue() {
        return value;
    }

    /**
     * Returns the constant value held by this object.
     */
    @Override
    public T evaluate(Object ignored) {
        return value;
    }

    /**
     * Returns the type of values produced by this expression.
     */
    @Override
    public AttributeType<?> expectedType(FeatureType ignored) {
        final Class<?> valueType = value.getClass();
        AttributeType<?> type = TYPES.get(valueType);
        if (type == null) {
            final Class<?> standardType = Classes.getStandardType(valueType);
            type = TYPES.computeIfAbsent(standardType, DefaultLiteral::newType);
            if (valueType != standardType) {
                TYPES.put(valueType, type);
            }
        }
        return type;
    }

    /**
     * Invoked when a new attribute type need to be created for the given standard type.
     */
    private static <T> AttributeType<T> newType(final Class<T> standardType) {
        return new DefaultAttributeType<>(Collections.singletonMap(DefaultAttributeType.NAME_KEY, NAME),
                                          standardType, 1, 1, null, (AttributeType<?>[]) null);
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(final ExpressionVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }

    /**
     * Compares this literal with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof Literal) && value.equals(((DefaultLiteral) obj).value);
    }

    /**
     * Returns a hash-code value for this literal.
     */
    @Override
    public int hashCode() {
        return value.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this literal.
     */
    @Override
    public String toString() {
        return value.toString();
    }
}
