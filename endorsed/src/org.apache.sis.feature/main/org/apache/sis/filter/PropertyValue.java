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

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.apache.sis.feature.Features;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.feature.privy.FeatureProjectionBuilder;
import org.apache.sis.filter.privy.XPath;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.ValueReference;


/**
 * Expression whose value is computed by retrieving the value indicated by the provided name.
 * This expression does not store any value; it acts as an indirection to a property value of
 * the evaluated feature.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <V>  the type of value computed by the expression.
 *
 * @see AssociationValue
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
     * If {@code true}, then {@link #expectedType(FeatureProjectionBuilder)} will not throw an exception when
     * the property is not found.
     */
    protected final boolean isVirtual;

    /**
     * The prefix in a XPath for considering a property as virtual.
     */
    private static final String VIRTUAL_PREFIX = "/*/";

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
    static <V> ValueReference<Feature,V> create(final String xpath, final Class<V> type) {
        final var parsed = new XPath(xpath);
        List<String> path = parsed.path;
        boolean isVirtual = false;
        if (parsed.isAbsolute) {
            /*
             * If the XPath is like "/∗/property" where the root "/" is the feature instance,
             * we interpret that as meaning "property of a feature of any type", which means
             * to relax the restriction about the set of allowed properties.
             */
            isVirtual = (path != null) && path.get(0).equals("*");
            if (!isVirtual) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedXPath_1, xpath));
            }
            path.remove(0);
            if (path.isEmpty()) {
                path = null;
            }
        }
        final PropertyValue<V> tip;
        if (type != Object.class) {
            tip = new Converted<>(type, parsed.tip, isVirtual);
        } else {
            tip = (PropertyValue<V>) new AsObject(parsed.tip, isVirtual);
        }
        return (path != null) ? new AssociationValue<>(path, tip) : tip;
    }

    /**
     * Returns the class of resources expected by this expression.
     */
    @Override
    public final Class<Feature> getResourceClass() {
        return Feature.class;
    }

    /**
     * For {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected final Collection<?> getChildren() {
        return isVirtual ? List.of(name, isVirtual) : List.of(name);
    }

    /**
     * Returns the XPath to the property whose value will be returned by the {@link #apply(Object)} method.
     *
     * @return XPath to the property.
     */
    @Override
    public final String getXPath() {
        return getXPath(null);
    }

    /**
     * Returns the XPath to the property, prefixed by the given path.
     *
     * @param  path  the path to append as a prefix.
     * @return XPath to the property.
     */
    final String getXPath(final String[] path) {
        return XPath.toString(isVirtual ? VIRTUAL_PREFIX : null, path, name);
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
     * Returns the value of {@code expectedType(…)} to return as a fallback when
     * that value cannot be inferred by the analysis of the source feature type.
     * It happens when {@link #isVirtual} is {@code true} and the property to add may be
     * defined in a feature sub-type not known at {@code expectedType(…)} invocation time.
     *
     * @see #expectedType(FeatureProjectionBuilder)
     */
    final FeatureProjectionBuilder.Item defaultType(final FeatureProjectionBuilder addTo) {
        return addTo.addComputedProperty(addTo.addAttribute(getValueClass()).setMinimumOccurs(0).setName(name), true);
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
    @Override
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
     *
     * @param  <V>  the type of value computed by the expression.
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
         * Provides the expected type of values produced by this expression.
         * If the converted {@linkplain #type} is a generalization of the attribute type,
         * the original attribute type is kept unchanged because {@link #apply(Feature)}
         * does not convert those values.
         *
         * @throws UnconvertibleObjectException if the property default value cannot be converted to {@link #type}.
         */
        @Override
        public final FeatureProjectionBuilder.Item expectedType(final FeatureProjectionBuilder addTo) {
            FeatureProjectionBuilder.Item item = super.expectedType(addTo);
            item.replaceValueClass((c) -> type.isAssignableFrom(c) ? c : type);
            return item;
        }
    }

    /**
     * Provides the expected type of values produced by this expression when a feature of the given type is evaluated.
     * The source feature type is specified indirectly by {@link FeatureProjectionBuilder#source()}.
     *
     * <h4>Handling of operations</h4>
     * Properties that are operations are replaced by attributes where the operation result will be stored.
     * An exception to this rule is the links such as {@code "sis:identifier"} and {@code "sis:geometry"},
     * in which case the link operation is kept. It may force {@code FeatureProjectionBuilder} to add also
     * the dependencies (targets) of the link.
     *
     * @param  addTo  where to add the type of properties evaluated by this expression.
     * @return builder of the added property, or {@code null} if this method cannot add a property.
     * @throws PropertyNotFoundException if the property was not found in {@code addTo.source()}.
     */
    @Override
    public FeatureProjectionBuilder.Item expectedType(final FeatureProjectionBuilder addTo) {
        PropertyType type;
        try {
            type = addTo.source().getProperty(name);
        } catch (PropertyNotFoundException e) {
            if (isVirtual) {
                // The property does not exist but may be defined in a yet unknown child type.
                return defaultType(addTo);
            }
            throw e;
        }
        return addTo.addSourceProperty(type, true);
    }




    /**
     * An expression fetching property values as an object of specified type.
     * The value is first cast from {@link Object} to the expected source type,
     * then converted to the specified target type.
     *
     * @param  <S>  the type of source value before conversion.
     * @param  <V>  the type of value computed by the expression.
     */
    private static final class CastedAndConverted<S,V> extends Converted<V> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -58453954752151703L;

        /** The source type before conversion. */
        private final Class<S> source;

        /** The conversion from source type to the type to be returned. */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
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
