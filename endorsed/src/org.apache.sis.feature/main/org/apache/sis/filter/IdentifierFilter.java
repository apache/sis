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
import java.util.HashSet;
import java.util.Collection;
import java.util.Objects;
import org.apache.sis.filter.base.Node;
import org.apache.sis.filter.base.XPathSource;
import org.apache.sis.feature.internal.shared.AttributeConvention;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.Expression;
import org.opengis.filter.ResourceId;
import org.opengis.filter.Filter;


/**
 * Filter features using a set of predefined identifiers.
 * Features without identifiers are discarded.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class IdentifierFilter extends Node
        implements ResourceId<Feature>, XPathSource, Optimization.OnFilter<Feature>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5917022442937908715L;

    /**
     * Name of the property which contains the identifier.
     * The initial value is {@value AttributeConvention#IDENTIFIER},
     * but this name may be replaced by a more direct target if known.
     */
    private final String property;

    /**
     * The identifier of features to retain.
     */
    private final String identifier;

    /**
     * Creates a new filter for filtering features having the given identifier.
     */
    public IdentifierFilter(final String identifier) {
        this.property   = AttributeConvention.IDENTIFIER;
        this.identifier = Objects.requireNonNull(identifier);
    }

    /**
     * Creates a new filter searching for the same identifier than the original filter,
     * but looking in a different property.
     */
    private IdentifierFilter(final IdentifierFilter original, final String property) {
        this.property   = property;
        this.identifier = original.identifier;
    }

    /**
     * If the evaluated property is a link, replaces this filter by a more direct reference to the target property.
     * This optimization helps {@code SQLStore} to put the column name in the <abbr>SQL</abbr> {@code WHERE} clause.
     * It can make the difference between using or not the database index.
     */
    @Override
    public Filter<Feature> optimize(Optimization optimization) {
        final var found = new HashSet<String>();
        try {
            final String preferredName = optimization.getPreferredPropertyName(property, found);
            if (!preferredName.equals(property)) {
                return new IdentifierFilter(this, preferredName);
            }
        } catch (PropertyNotFoundException e) {
            warning(e, true);
            if (found.isEmpty()) {
                return Filter.exclude();    // The property does not exist in any feature type.
            }
        }
        return this;
    }

    /**
     * Returns the class of resources expected by this expression.
     */
    @Override
    public Class<Feature> getResourceClass() {
        return Feature.class;
    }

    /**
     * Returns the path to the property which will be used by the {@code test(R)} method.
     */
    @Override
    public String getXPath() {
        return property;
    }

    /**
     * Returns the identifiers of feature instances to accept.
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the parameters of this filter.
     */
    @Override
    public List<Expression<Feature,?>> getExpressions() {
        return List.of(new LeafExpression.Literal<>(identifier));
    }

    /**
     * Returns the identifiers specified at construction time. This is used for {@link #toString()},
     * {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected Collection<?> getChildren() {
        return List.of(property, identifier);
    }

    /**
     * Returns {@code true} if the given object is a {@link Feature} instance and its identifier
     * is one of the identifier specified at {@code IdentifierFilter} construction time.
     */
    @Override
    public boolean test(final Feature object) {
        if (object != null) try {
            Object id = object.getPropertyValue(property);
            if (id != null) return identifier.equals(id.toString());
        } catch (PropertyNotFoundException e) {
            warning(e, false);
        }
        return false;
    }
}
