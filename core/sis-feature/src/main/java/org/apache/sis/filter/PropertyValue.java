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

import java.util.Optional;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.feature.Features;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.Expression;
import org.opengis.filter.ValueReference;


/**
 * Expression whose value is computed by retrieving the value indicated by the provided name.
 * This expression does not store any value; it acts as an indirection to a property value of
 * the evaluated feature.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @param  <V>  the type of value computed by the expression.
 *
 * @since 1.1
 * @module
 */
abstract class PropertyValue<V> extends LeafExpression<Feature,V> implements ValueReference<Feature,V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3756361632664536269L;

    /**
     * Name of the property from which to retrieve the value.
     */
    protected final String name;

    /**
     * Creates a new expression retrieving values from a property of the given name.
     */
    protected PropertyValue(final String name) {
        ArgumentChecks.ensureNonNull("name", name);
        this.name = name;
    }

    /**
     * Creates a new expression retrieving values from a property of the given name.
     *
     * @param  <V>   compile-time value of {@code type}.
     * @param  name  the name of the property to fetch.
     * @param  type  the desired type for the expression result.
     * @return expression retrieving values from a property of the given name.
     */
    @SuppressWarnings("unchecked")
    static <V> PropertyValue<V> create(final String name, final Class<V> type) {
        ArgumentChecks.ensureNonNull("type", type);
        if (type == Object.class) {
            return (PropertyValue<V>) new AsObject(name);
        } else {
            return new Converted<>(type, name);
        }
    }

    /**
     * For {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected final Collection<?> getChildren() {
        return Collections.singleton(name);
    }

    /**
     * Returns the name of the property whose value will be returned by the {@link #apply(Object)} method.
     */
    @Override
    public final String getXPath() {
        return name;
    }

    /**
     * Returns the type of values fetched from {@link Feature} instance.
     * This is the type before conversion to the {@linkplain #getValueClass() target type}.
     */
    protected Class<?> getSourceClass() {
        return Object.class;
    }

    /**
     * Returns an expression that provides values as instances of the specified class.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <N> Expression<Feature,N> toValueType(final Class<N> type) {
        if (type.isAssignableFrom(getValueClass())) {
            return (PropertyValue<N>) this;
        }
        final Class<?> source = getSourceClass();
        if (source != Object.class) {
            return new CastedAndConverted<>(source, type, name);
        }
        return create(name, type);
    }




    /**
     * An expression fetching property values as {@code Object}.
     * This expression does not need to apply any type conversion.
     */
    private static final class AsObject extends PropertyValue<Object>
            implements Optimization.OnExpression<Feature,Object>
    {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2854731969723006038L;

        /**
         * Creates a new expression retrieving values from a property of the given name.
         */
        AsObject(final String name) {
            super(name);
        }

        /**
         * Returns the value of the property of the given name.
         * If no value is found for the given feature, then this method returns {@code null}.
         */
        @Override
        public Object apply(final Feature instance) {
            if (instance != null) try {
                return instance.getPropertyValue(name);
            } catch (PropertyNotFoundException e) {
                warning(e, true);
                // Null will be returned below.
            }
            return null;
        }

        /**
         * If the evaluated property is a link, replaces this expression by a more direct reference
         * to the target property. This optimization is important for allowing {@code SQLStore} to
         * put the column name in the SQL {@code WHERE} clause. It makes the difference between
         * using or not the database index.
         */
        @Override
        public Expression<Feature,?> optimize(final Optimization optimization) {
            final FeatureType type = optimization.getFeatureType();
            if (type != null) try {
                return Features.getLinkTarget(type.getProperty(name)).map(AsObject::new).orElse(this);
            } catch (PropertyNotFoundException e) {
                warning(e, true);
            }
            return this;
        }
    }




    /**
     * An expression fetching property values as an object of specified type.
     * The value is converted from {@link Object} to the specified type.
     */
    private static class Converted<V> extends PropertyValue<V> implements Optimization.OnExpression<Feature,V> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -1436865010478207066L;

        /** The desired type of values. */
        protected final Class<V> type;

        /**
         * Creates a new expression retrieving values from a property of the given name.
         *
         * @param  type  the desired type for the expression result.
         * @param  name  the name of the property to fetch.
         */
        protected Converted(final Class<V> type, final String name) {
            super(name);
            this.type = type;
        }

        /**
         * Creates a new {@code Converted} fetching values for a property of different name.
         * The given name should be the target of a link that the caller has resolved.
         */
        protected Converted<V> rename(final String target) {
            return new Converted<>(type, target);
        }

        /**
         * Returns the type of values computed by this expression.
         */
        @Override
        public final Class<V> getValueClass() {
            return type;
        }

        /**
         * Returns the value of the property of the given name.
         * If no value is found for the given feature, then this method returns {@code null}.
         */
        @Override
        public V apply(final Feature instance) {
            if (instance != null) try {
                return ObjectConverters.convert(instance.getPropertyValue(name), type);
            } catch (PropertyNotFoundException e) {
                warning(e, true);
                // Null will be returned below.
            } catch (UnconvertibleObjectException e) {
                warning(e, false);
            }
            return null;
        }

        /**
         * Tries to optimize this expression. If an {@link ObjectConverter} can be determined in advance
         * for the {@linkplain Optimization#getFeatureType() feature type for which to optimize},
         * then a specialized expression is returned. Otherwise this method returns {@code this}.
         */
        @Override
        public final Expression<Feature, ? extends V> optimize(final Optimization optimization) {
            final FeatureType featureType = optimization.getFeatureType();
            if (featureType != null) try {
                String targetName = name;
                PropertyType property = featureType.getProperty(targetName);
                Optional<String> target = Features.getLinkTarget(property);
                if (target.isPresent()) try {
                    targetName = target.get();
                    property = featureType.getProperty(targetName);
                } catch (PropertyNotFoundException e) {
                    targetName = name;
                    warning(e, true);
                }
                if (property instanceof AttributeType<?>) {
                    final Class<?> source = ((AttributeType<?>) property).getValueClass();
                    if (source != null && source != Object.class && !source.isAssignableFrom(getSourceClass())) {
                        return new CastedAndConverted<>(source, type, targetName);
                    }
                }
                if (!targetName.equals(name)) {
                    return rename(targetName);
                }
            } catch (PropertyNotFoundException e) {
                warning(e, true);
            }
            return this;
        }

        /**
         * Provides the expected type of values produced by this expression
         * when a feature of the given type is evaluated.
         */
        @Override
        public final PropertyTypeBuilder expectedType(final FeatureType valueType, final FeatureTypeBuilder addTo) {
            final PropertyTypeBuilder p = super.expectedType(valueType, addTo);
            if (p instanceof AttributeTypeBuilder<?>) {
                final AttributeTypeBuilder<?> a = (AttributeTypeBuilder<?>) p;
                if (!type.isAssignableFrom(a.getValueClass())) {
                    return a.setValueClass(type);
                }
            }
            return p;
        }
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




    /**
     * An expression fetching property values as an object of specified type.
     * The value is first casted from {@link Object} to the expected source type,
     * then converted to the specified target type.
     */
    private static final class CastedAndConverted<S,V> extends Converted<V> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -58453954752151703L;

        /** The source type before conversion. */
        private final Class<S> source;

        /** The conversion from source type to the type to be returned. */
        private final ObjectConverter<? super S, ? extends V> converter;

        /** Creates a new expression retrieving values from a property of the given name. */
        CastedAndConverted(final Class<S> source, final Class<V> type, final String name) {
            super(type, name);
            this.source = source;
            converter = ObjectConverters.find(source, type);
        }

        /** Creates a new expression derived from an existing one except for the target name. */
        private CastedAndConverted(final CastedAndConverted<S,V> other, final String name) {
            super(other.type, name);
            source = other.source;
            converter = other.converter;
        }

        /**
         * Creates a new {@code CastedAndConverted} fetching values for a property of different name.
         * The given name should be the target of a link that the caller has resolved.
         */
        @Override
        protected Converted<V> rename(final String target) {
            return new CastedAndConverted<>(this, target);
        }

        /**
         * Returns the type of values fetched from {@link Feature} instance.
         */
        @Override
        protected Class<S> getSourceClass() {
            return source;
        }

        /**
         * Returns the value of the property of the given name.
         * If no value is found for the given feature, then this method returns {@code null}.
         */
        @Override
        public V apply(final Feature instance) {
            if (instance != null) try {
                return converter.apply(source.cast(instance.getPropertyValue(name)));
            } catch (PropertyNotFoundException e) {
                warning(e, true);
            } catch (ClassCastException | UnconvertibleObjectException e) {
                warning(e, false);
            }
            return null;
        }
    }
}
