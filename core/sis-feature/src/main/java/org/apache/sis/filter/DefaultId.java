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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.sis.internal.feature.AttributeConvention;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;

/**
 * Filter features using a list of predefined ids and discarding those not in the list.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
final class DefaultId implements Id, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1404452049863376235L;

    private final DualKeyMap keys = new DualKeyMap();

    public DefaultId( final Set<? extends Identifier> ids ) {
        for (Identifier id : ids) {
            keys.put(id.getID(), id);
        }
    }

    @Override
    public Set<Object> getIDs() {
        return Collections.unmodifiableSet(keys.keySet());
    }

    @Override
    public Set<Identifier> getIdentifiers() {
        return new HashSet<>(keys.values());
    }

    @Override
    public boolean evaluate(Object object) {
        if (object instanceof Feature) {
            final Feature f = (Feature) object;
            try {
                Object id = f.getPropertyValue(AttributeConvention.IDENTIFIER);
                if (id == null) {
                   return false;
                } else if (id instanceof String) {
                    return keys.containsKey(id);
                } else {
                    //it often happens like in web services that keys are sent as Strings
                    //but the real type might be different
                    return keys.containsKey(id) || keys.containsKey(String.valueOf(id));
                }
            } catch (PropertyNotFoundException ex) {
                //normal
            }
        }
        return false;
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultId other = (DefaultId) obj;
        return Objects.equals(this.keys, other.keys);
    }

    @Override
    public int hashCode() {
        return keys.hashCode();
    }

    @Override
    public String toString() {
        return AbstractExpression.toStringTree("Ids", keys.keySet());
    }

    private static class DualKeyMap extends HashMap<Object,Identifier> {

        @Override
        public boolean containsValue(final Object value) {
            if (value instanceof Identifier) {
                Identifier ident = (Identifier) value;
                return containsKey(ident.getID());
            } else {
                return false;
            }
        }
    }
}
