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
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.feature.AttributeConvention;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;


/**
 * Filter features using a set of predefined identifiers and discarding features
 * whose identifier is not in the set.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class FilterByIdentifier extends Node implements Id {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1404452049863376235L;

    /**
     * The identifiers of features to retain. Filtering will use the keys. This map contains also
     * the same identifiers as the original {@link Identifier} objects given at construction time,
     * but those values are not used by this class.
     */
    private final Map<Object,Identifier> identifiers;

    /**
     * Creates a new filter using the given identifiers.
     */
    FilterByIdentifier(final Collection<? extends Identifier> ids) {
        identifiers = new HashMap<>(Containers.hashMapCapacity(ids.size()));
        for (Identifier id : ids) {
            identifiers.put(id.getID(), id);
        }
    }

    /**
     * Returns a name identifying this kind of filter.
     */
    @Override
    protected String name() {
        return "Id";
    }

    /**
     * Returns the identifiers specified at construction time. This is used for {@link #toString()},
     * {@link #hashCode()} and {@link #equals(Object)} implementations. Since all the keys in the
     * {@link #identifiers} map were derived from the values, comparing those values is sufficient
     * for determining if two {@code FilterByIdentifier} instances are equal.
     */
    @Override
    protected Collection<?> getChildren() {
        // Can not return identifiers.values() directly because that collection does not implement equals/hashCode.
        return getIdentifiers();
    }

    /**
     * Returns the identifiers of feature instances to accept.
     */
    @Override
    public Set<Object> getIDs() {
        return Collections.unmodifiableSet(identifiers.keySet());
    }

    /**
     * Same identifiers than {@link #getIDs()} but as the instances specified at construction time.
     * This is not used by this class.
     */
    @Override
    public Set<Identifier> getIdentifiers() {
        return new HashSet<>(identifiers.values());
    }

    /**
     * Returns {@code true} if the given object is a {@link Feature} instance and its identifier
     * is one of the identifier specified at {@code FilterByIdentifier} construction time.
     */
    @Override
    public boolean evaluate(Object object) {
        if (object instanceof Feature) try {
            final Object id = ((Feature) object).getPropertyValue(AttributeConvention.IDENTIFIER);
            if (identifiers.containsKey(id)) {
                return true;
            }
            if (id != null && !(id instanceof String)) {
                /*
                 * Sometime web services specify the identifiers to use for filtering as Strings
                 * while the types stored in the feature instances is different.
                 */
                return identifiers.containsKey(id.toString());
            }
        } catch (PropertyNotFoundException ex) {
            // No identifier property. This is okay.
        }
        return false;
    }

    /**
     * Implementation of the visitor pattern.
     */
    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }
}
