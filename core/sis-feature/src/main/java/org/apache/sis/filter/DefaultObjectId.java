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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.feature.AttributeConvention;
import org.opengis.feature.Feature;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.GmlObjectId;


/**
 * Default implementation of a few interfaces from the {@link org.opengis.filter.identity} package.
 * Those objects are used for identifying GML objects or other kind of objects.
 *
 * @deprecated the purpose of {@link org.opengis.filter.identity} is questionable.
 *             See <a href="https://github.com/opengeospatial/geoapi/issues/32">GeoAPI issue #32</a>.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@Deprecated
final class DefaultObjectId implements FeatureId, GmlObjectId, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2877500277700165269L;

    /**
     * The identifier.
     */
    private final String identifier;

    /**
     * Creates a new identifier.
     */
    DefaultObjectId(final String id) {
        ArgumentChecks.ensureNonNull("id", id);
        identifier = id;
    }

    /**
     * Returns the identifier specified at construction time.
     */
    @Override
    public String getID() {
        return identifier;
    }

    /**
     * Returns {@code true} if the given object is a feature with the identifier expected by this class.
     */
    @Override
    public boolean matches(final Object feature) {
        if (feature instanceof Feature) {
            Object value = ((Feature) feature).getValueOrFallback(AttributeConvention.IDENTIFIER, null);
            // Feature does not contain the identifier property if null.
            if (value != null) {
                return identifier.equals(String.valueOf(value));
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given object is an identifier with equals {@link String} code than this object.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DefaultObjectId) {
            return identifier.equals(((DefaultObjectId) obj).identifier);
        }
        return false;
    }

    /**
     * Returns a hash code value based on the identifier specified at construction time.
     */
    @Override
    public int hashCode() {
        return identifier.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this identifier.
     */
    @Override
    public String toString() {
        return "Id:".concat(identifier);
    }
}
