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
import java.util.Collection;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.filter.internal.Node;
import org.apache.sis.feature.internal.shared.AttributeConvention;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * Filter features using a set of predefined identifiers and discarding features
 * whose identifier is not in the set.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class IdentifierFilter extends Node implements Optimization.OnFilter<AbstractFeature> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1404452049863376235L;

    /**
     * The identifier of features to retain.
     */
    private final String identifier;

    /**
     * Creates a new filter using the given identifier.
     */
    IdentifierFilter(final String identifier) {
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        this.identifier = identifier;
    }

    @Override
    public Enum<?> getOperatorType() {
        return FilterName.RESOURCE_ID;
    }

    /**
     * Nothing to optimize here. The {@code Optimization.OnFilter} interface
     * is implemented for inheriting the AND, OR and NOT methods overriding.
     */
    @Override
    public Filter<AbstractFeature> optimize(Optimization optimization) {
        return this;
    }

    /**
     * Returns the class of resources expected by this expression.
     */
    @Override
    public Class<AbstractFeature> getResourceClass() {
        return AbstractFeature.class;
    }

    /**
     * Returns the identifiers of feature instances to accept.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the parameters of this filter.
     */
    @Override
    public List<Expression<AbstractFeature,?>> getExpressions() {
        return List.of(new LeafExpression.Literal<>(identifier));
    }

    /**
     * Returns the identifiers specified at construction time. This is used for {@link #toString()},
     * {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected Collection<?> getChildren() {
        return List.of(identifier);
    }

    /**
     * Returns {@code true} if the given object is a {@link AbstractFeature} instance and its identifier
     * is one of the identifier specified at {@code IdentifierFilter} construction time.
     */
    @Override
    public boolean test(final AbstractFeature object) {
        if (object == null) {
            return false;
        }
        final Object id = object.getValueOrFallback(AttributeConvention.IDENTIFIER, null);
        return (id != null) && identifier.equals(id.toString());
    }
}
