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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.feature.Features;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyNotFoundException;
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
 * @see AssociationValue
 *
 * @since 1.1
 * @module
 */
abstract class PropertyValue<V> extends LeafExpression<Feature,V>
        implements ValueReference<Feature,V>, Optimization.OnExpression<Feature,V>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3756361632664536269L;

    /**
     * Name of the property from which to retrieve the value.
     * This is the argument to give in calls to {@link Feature#getProperty(String)}
     */
    protected final String name;

    /**
     * Whether the property to fetch is considered virtual (a property that may be defined only in sub-types).
     * If {@code true}, then {@link #expectedType(FeatureType, FeatureTypeBuilder)} will not throw an exception
     * if the property is not found.
     */
    protected final boolean isVirtual;

    /**
     * The prefix in a x-path for considering a property as virual.
     */
    static final String VIRTUAL_PREFIX = "/*/";

    /**
     * Creates a new expression retrieving values from a property of the given name.
     */
    protected PropertyValue(final String name, final boolean isVirtual) {
        this.name = name;
        this.isVirtual = isVirtual;
    }

    /**
     * Creates a new expression retrieving values from a property of the given path.
     * Simple path expressions of the form "a/b/c" can be used.
     *
     * @param  <V>    compile-time value of {@code type}.
     * @param  xpath  path (usually a single name) of the property to fetch.
     * @param  type   the desired type for the expression result.
     * @return expression retrieving values from a property of the given name.
     * @throws IllegalArgumentException if the given XPath is not supported.
     */
    @SuppressWarnings("unchecked")
    static <V> ValueReference<Feature,V> create(String xpath, final Class<V> type) {
        boolean isVirtual = false;
        List<String> path = XPath.split(xpath);
split:  if (path != null) {
            /*
             * If the XPath is like "/∗/property" where the root "/" is the feature instance,
             * we interpret that as meaning "property of a feature of any type", which means
             * to relax the restriction about the set of allowed properties.
             */
            final String head = path.get(0);                // List and items in the list are guaranteed non-empty.
            isVirtual = head.equals("/*");
            if (isVirtual || head.charAt(0) != XPath.SEPARATOR) {
                final int offset = isVirtual ? 1 : 0;       // Skip the "/*/" component at index 0.
                final int last = path.size() - 1;
                if (last >= offset) {
                    xpath = path.get(last);
                    path  = path.subList(offset, last);
                    break split;                            // Accept the path as valid.
                }
            }
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedXPath_1, xpath));
        }
        /*
         * At this point, `xpath` is the tip of the path (i.e. prefixes have been removed).
         */
        final PropertyValue<V> tip;
        if (type != Object.class) {
            tip = new Converted<>(type, xpath, isVirtual);
        } else {
            tip = (PropertyValue<V>) new AsObject(xpath, isVirtual);
        }
        return (path == null || path.isEmpty()) ? tip : new AssociationValue<>(path, tip);
    }

    /**
     * For {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected final Collection<?> getChildren() {
        return isVirtual ? Arrays.asList(name, isVirtual) : Collections.singleton(name);
    }

    /**
     * Returns the name of the property whose value will be returned by the {@link #apply(Object)} method.
     */
    @Override
    public final String getXPath() {
        return isVirtual ? VIRTUAL_PREFIX.concat(name) : name;
    }

    /**
     * Returns the type of values fetched from {@link Feature} instance.
     * This is the type before conversion to the {@linkplain #getValueClass() target type}.
     * The type is always {@link Object} on newly created expression because the type of feature property
     * values is unknown, but may become a specialized type after {@link Optimization} has been applied.
     */
    protected Class<?> getSourceClass() {
        return Object.class;
    }

    /**
     * Returns the default value of {@link #expectedType(FeatureType, FeatureTypeBuilder)}
     * when it can not be inferred by the analysis of the given {@code FeatureType}.
     */
    final PropertyTypeBuilder expectedType(final FeatureTypeBuilder addTo) {
        return addTo.addAttribute(getValueClass()).setName(name).setMinimumOccurs(0);
    }

    /**
     * Returns an expression that provides values as instances of the specified class.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <N> PropertyValue<N> toValueType(final Class<N> target) {
        if (target.equals(getValueClass())) {
            return (PropertyValue<N>) this;
        }
        final Class<?> source = getSourceClass();
        if (target == Object.class) {
            return (PropertyValue<N>) new AsObject(name, isVirtual);
        } else if (source == Object.class) {
            return new Converted<>(target, name, isVirtual);
        } else {
            return new CastedAndConverted<>(source, target, name, isVirtual);
        }
    }

    /**
     * If the evaluated property is a link, replaces this expression by a more direct reference
     * to the target property. This optimization is important for allowing {@code SQLStore} to
     * put the column name in the SQL {@code WHERE} clause. It makes the difference between
     * using or not the database index.
     */
    public abstract PropertyValue<V> optimize(Optimization optimization);



    /**
     * An expression fetching property values as {@code Object}.
     * This expression does not need to apply any type conversion.
     */
    private static final class AsObject extends PropertyValue<Object> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2854731969723006038L;

        /**
         * Creates a new expression retrieving values from a property of the given name.
         */
        AsObject(final String name, final boolean isVirtual) {
            super(name, isVirtual);
        }

        /**
         * Returns the value of the property of the name given at construction time.
         * If no value is found for the given feature, then this method returns {@code null}.
         */
        @Override
        public Object apply(final Feature instance) {
            return (instance != null) ? instance.getValueOrFallback(name, null) : null;
        }

        /**
         * If the evaluated property is a link, replaces this expression by a more direct reference
         * to the target property. This optimization is important for allowing {@code SQLStore} to
         * put the column name in the SQL {@code WHERE} clause. It makes the difference between
         * using or not the database index.
         */
        @Override
        public PropertyValue<Object> optimize(final Optimization optimization) {
            final FeatureType type = optimization.getFeatureType();
            if (type != null) try {
                return Features.getLinkTarget(type.getProperty(name))
                        .map((rename) -> new AsObject(rename, isVirtual)).orElse(this);
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
    private static class Converted<V> extends PropertyValue<V> {
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
        protected Converted(final Class<V> type, final String xpath, final boolean isVirtual) {
            super(xpath, isVirtual);
            this.type = type;
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
                return ObjectConverters.convert(instance.getValueOrFallback(name, null), type);
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
        public final PropertyValue<V> optimize(final Optimization optimization) {
            final FeatureType featureType = optimization.getFeatureType();
            if (featureType != null) try {
                /*
                 * Resolve link (e.g. "sis:identifier" as a reference to the real identifier property).
                 * This is important for allowing `SQLStore` to use the property in SQL WHERE statements.
                 * If there is no renaming to apply (which is the usual case), then `rename` is null.
                 */
                String rename = name;
                PropertyType property = featureType.getProperty(rename);
                Optional<String> target = Features.getLinkTarget(property);
                if (target.isPresent()) try {
                    rename = target.get();
                    property = featureType.getProperty(rename);
                } catch (PropertyNotFoundException e) {
                    warning(e, true);
                    rename = name;
                }
                /*
                 * At this point we did our best effort for having the property as an attribute,
                 * which allows us to get the expected type. If the type is not `Object`, we can
                 * try to fetch a more specific converter than the default `Converted` one.
                 */
                Class<?> source = getSourceClass();
                final Class<?> original = source;
                if (property instanceof AttributeType<?>) {
                    source = ((AttributeType<?>) property).getValueClass();
                }
                if (!(rename.equals(name) && source.equals(original))) {
                    if (source == Object.class) {
                        return new Converted<>(type, rename, isVirtual);
                    } else {
                        return new CastedAndConverted<>(source, type, rename, isVirtual);
                    }
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
        PropertyType type;
        try {
            type = valueType.getProperty(name);
        } catch (PropertyNotFoundException e) {
            if (isVirtual) {
                // The property does not exist but may be defined on a yet unknown child type.
                return expectedType(addTo);
            }
            throw e;
        }
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
        CastedAndConverted(final Class<S> source, final Class<V> type, final String xpath, final boolean isVirtual) {
            super(type, xpath, isVirtual);
            this.source = source;
            converter = ObjectConverters.find(source, type);
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
                return converter.apply(source.cast(instance.getValueOrFallback(name, null)));
            } catch (ClassCastException | UnconvertibleObjectException e) {
                warning(e, false);
            }
            return null;
        }
    }
}
