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
import java.util.Objects;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.GmlObjectId;

/**
 * Filter feature id.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
final class DefaultFeatureId implements FeatureId, GmlObjectId, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2877500277700165269L;

    private final String id;

    public DefaultFeatureId(String id) {
        ArgumentChecks.ensureNonNull("id", id);
        this.id = id;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public boolean matches(Object feature) {
        if (feature instanceof Feature) {
            final Feature f = (Feature) feature;
            try {
                Object id = f.getPropertyValue(AttributeConvention.IDENTIFIER);
                return this.id.equals(String.valueOf(id));
            } catch (PropertyNotFoundException ex) {
                //normal
            }
        }
        return false;
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
        final DefaultFeatureId other = (DefaultFeatureId) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode();
    }

    @Override
    public String toString() {
        return "Id:" + id;
    }
}
