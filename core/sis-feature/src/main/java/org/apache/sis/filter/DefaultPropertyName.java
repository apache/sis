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
import java.io.Serializable;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.feature.DefaultAssociationRole;

import static java.util.Collections.singletonMap;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.PropertyName;


/**
 * Expression whose value is computed by retrieving the value indicated by the provided name.
 * A property name does not store any value; it acts as an indirection to a property value of
 * the evaluated feature.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class DefaultPropertyName extends AbstractExpression implements PropertyName, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8474562134021521300L;

    /**
     * Name of the property from which to retrieve the value.
     */
    private final String name;

    /**
     * Creates a new expression retrieving values from a property of the given name.
     * It is caller responsibility to ensure that the given name is non-null.
     *
     * @param  name  name of the property (usually a feature attribute).
     */
    DefaultPropertyName(final String name) {
        this.name = name;
    }

    /**
     * Returns the name of the property whose value will be returned by the {@link #evaluate evaluate} method.
     */
    @Override
    public String getPropertyName() {
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
     * If no value is found for the given property, then this method returns {@code null}.
     */
    @Override
    public Object evaluate(final Object candidate) {
        if (candidate instanceof Feature) {
            try {
                return ((Feature) candidate).getPropertyValue(name);
            } catch (PropertyNotFoundException ex) {
                // Null will be returned below.
                // TODO: report a warning somewhere?
            }
        } else if (candidate instanceof Map<?,?>) {
            return ((Map<?,?>) candidate).get(name);
        }
        return null;
    }

    /**
     * Returns the expected type of values produced by this expression when a feature of the given type is evaluated.
     *
     * @throws IllegalArgumentException if this method can not determine the property type for the given feature type.
     */
    @Override
    public PropertyType expectedType(final FeatureType type) {
        PropertyType propertyType = type.getProperty(name);         // May throw IllegalArgumentException.
        while (propertyType instanceof Operation) {
            final IdentifiedType it = ((Operation) propertyType).getResult();
            if (it instanceof PropertyType) {
                propertyType = (PropertyType) it;
            } else if (it instanceof FeatureType) {
                propertyType = new DefaultAssociationRole(singletonMap(DefaultAssociationRole.NAME_KEY, name), type, 1, 1);
            } else {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                            name, PropertyType.class, Classes.getStandardType(Classes.getClass(it))));
            }
        }
        return propertyType;
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(final ExpressionVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }

    /**
     * Returns a hash-code value for this expression.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof DefaultPropertyName) && name.equals(((DefaultPropertyName) obj).name);
    }

    /**
     * Returns a hash-code value for this expression.
     */
    @Override
    public int hashCode() {
        return name.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this expression.
     */
    @Override
    public String toString() {
        return '{' + name + '}';
    }
}
