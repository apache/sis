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
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.privy.FeatureProjectionBuilder;
import org.apache.sis.math.FunctionProperty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.Expression;
import org.opengis.filter.ValueReference;


/**
 * Expression whose value is computed by retrieving the value indicated by the provided path.
 * This is used for value reference given by a limited form of XPath such as {@code "a/b/c"}.
 * The last element of the path (the tip) is evaluated by a {@link PropertyValue}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <V>  the type of value computed by the expression.
 *
 * @see PropertyValue
 */
final class AssociationValue<V> extends LeafExpression<Feature, V>
        implements ValueReference<Feature, V>, Optimization.OnExpression<Feature, V>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2082322712413854288L;

    /**
     * Path to the property from which to retrieve the value.
     * Each element in the array is an argument to give in a call to {@link Feature#getProperty(String)}.
     * This array should be considered read-only because it may be shared.
     */
    private final String[] path;

    /**
     * Expression to use for evaluating the property value after the last element of the path.
     */
    private final PropertyValue<V> accessor;

    /**
     * Creates a new expression retrieving values from a property at the given path.
     *
     * @param  path      components of the path before the property evaluated by {@code accessor}.
     * @param  accessor  expression to use for evaluating the property value after the last element of the path.
     */
    AssociationValue(final List<String> path, final PropertyValue<V> accessor) {
        this.path = path.toArray(String[]::new);
        this.accessor = accessor;
    }

    /**
     * Creates a new expression retrieving values from a property at the given path.
     * This constructor is used for creating new expression with the same path as
     * a previous expression but a different accessor.
     *
     * @param  path      components of the path, not cloned (we share arrays).
     * @param  accessor  expression to use for evaluating the property value after the last element of the path.
     */
    private AssociationValue(final String[] path, final PropertyValue<V> accessor) {
        this.path = path;
        this.accessor = accessor;
    }

    /**
     * Returns the class of resources expected by this expression.
     */
    @Override
    public final Class<Feature> getResourceClass() {
        return Feature.class;
    }

    /**
     * Returns the manner in which values are computed from given resources.
     * This method assumes an initially empty set of properties, then adds the transitive properties.
     * This method does not inherit directly the properties of the {@linkplain #accessor} because it
     * does not operate on the same resource, so the non-transitive function properties may not hold.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return transitiveProperties(accessor.getParameters());
    }

    /**
     * For {@link #toString()} implementation.
     */
    @Override
    protected final Collection<?> getChildren() {
        return Set.of(getXPath());
    }

    /**
     * Returns the name of the property whose value will be returned by the {@link #apply(Object)} method.
     */
    @Override
    public final String getXPath() {
        return accessor.getXPath(path);
    }

    /**
     * Returns the value of the property at the path given at construction time.
     * Path components should be feature associations. If this is not the case,
     * this method silently returns {@code null}.
     *
     * @param  feature  the feature from which to get a value, or {@code null}.
     * @return value for the property identified by the XPath (may be {@code null}).
     */
    @Override
    public V apply(Feature instance) {
walk:   if (instance != null) {
            for (final String p : path) {
                final Object value = instance.getPropertyValue(p);
                if (!(value instanceof Feature)) break walk;
                instance = (Feature) value;
            }
            return accessor.apply(instance);
        }
        return null;
    }

    /**
     * If at least one evaluated property is a link, replaces this expression by more direct references
     * to the target properties. This is needed for better SQL WHERE clause in database queries.
     */
    @Override
    public Expression<Feature, V> optimize(final Optimization optimization) {
        final FeatureType specifiedType = optimization.getFeatureType();
walk:   if (specifiedType != null) try {
            FeatureType type = specifiedType;
            String[] direct = path;                 // To be cloned before any modification.
            for (int i=0; i<path.length; i++) {
                PropertyType property = type.getProperty(path[i]);
                Optional<String> link = Features.getLinkTarget(property);
                if (link.isPresent()) {
                    if (direct == path) direct = direct.clone();
                    property = type.getProperty(direct[i] = link.get());
                }
                if (!(property instanceof FeatureAssociationRole)) break walk;
                type = ((FeatureAssociationRole) property).getValueType();
            }
            /*
             * At this point all links have been resolved, up to the final property to evaluate.
             * Delegate the final property optimization to `accessor` which may not only resolve
             * links but also tune the `ObjectConverter`.
             */
            final PropertyValue<V> converted;
            optimization.setFeatureType(type);
            try {
                converted = accessor.optimize(optimization);
            } finally {
                optimization.setFeatureType(specifiedType);
            }
            if (converted != accessor || direct != path) {
                return new AssociationValue<>(direct, converted);
            }
        } catch (PropertyNotFoundException e) {
            warning(e, true);
        }
        return this;
    }

    /**
     * Returns an expression that provides values as instances of the specified class.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <N> Expression<Feature,N> toValueType(final Class<N> target) {
        final PropertyValue<N> converted = accessor.toValueType(target);
        if (converted == accessor) {
            return (AssociationValue<N>) this;
        }
        return new AssociationValue<>(path, converted);
    }

    /**
     * Provides the expected type of values produced by this expression. This method delegates to
     * {@link #accessor} after setting the source feature type to the tip of {@code "a/b/c"} path.
     *
     * @param  addTo  where to add the type of properties evaluated by this expression.
     * @return builder of the added property, or {@code null} if this method cannot add a property.
     * @throws PropertyNotFoundException if the property was not found in {@code addTo.source()}.
     */
    @Override
    public FeatureProjectionBuilder.Item expectedType(final FeatureProjectionBuilder addTo) {
        FeatureType valueType = addTo.source();
        for (final String p : path) {
            final PropertyType type;
            try {
                type = valueType.getProperty(p);
            } catch (PropertyNotFoundException e) {
                if (accessor.isVirtual) {
                    // The association does not exist but may be defined on a yet unknown child type.
                    return accessor.defaultType(addTo);
                }
                throw e;
            }
            if (!(type instanceof FeatureAssociationRole)) {
                return null;
            }
            valueType = ((FeatureAssociationRole) type).getValueType();
        }
        return addTo.using(valueType, accessor);
    }

    /**
     * Returns a hash code value for this association.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(path) + accessor.hashCode();
    }

    /**
     * Compares this value reference with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof AssociationValue<?>) {
            final AssociationValue<?> other = (AssociationValue<?>) obj;
            return Arrays.equals(path, other.path) && accessor.equals(other.accessor);
        }
        return false;
    }
}
