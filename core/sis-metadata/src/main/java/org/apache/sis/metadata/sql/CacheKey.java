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
package org.apache.sis.metadata.sql;

import java.util.Objects;


/**
 * The key for an entry in the {@link MetadataSource} cache.
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class CacheKey {
    /**
     * The metadata interface to be implemented.
     */
    private final Class<?> type;

    /**
     * The primary key for the entry in the table.
     */
    private final String identifier;

    /**
     * Creates a new key.
     */
    CacheKey(final Class<?> type, final String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    /**
     * Compares the given object with this key for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof CacheKey) {
            final CacheKey that = (CacheKey) other;
            return Objects.equals(this.type,       that.type) &&
                   Objects.equals(this.identifier, that.identifier);
        }
        return false;
    }

    /**
     * Returns a hash code for this key.
     */
    @Override
    public int hashCode() {
        return type.hashCode() ^ identifier.hashCode();
    }
}
