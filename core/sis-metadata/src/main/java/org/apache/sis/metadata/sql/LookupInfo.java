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

import java.util.Map;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.MetadataStandard;


/**
 * Information about the last used metadata type. Those information are cached on the assumption
 * that the same maps will be used more than once before to move to another metadata object.
 *
 * <p>Each thread shall have its own {@code LastUsedInfo} instance.
 * Consequently there is no need for synchronization in this class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class LookupInfo {
    /**
     * The type of metadata objects for which the {@link #names} and {@link #indices} maps are built.
     * Must be the interface type when such interface exists. This is mapped to the table name in the database.
     */
    private Class<?> type;

    /**
     * The last "method name to column name" map returned by {@link #asNameMap(MetadataStandard)}.
     * Cached on assumption that the same map will be used more than once before to move to another metadata object.
     */
    private Map<String,String> names;

    /**
     * The last used "method name to property indices" map returned by {@link #asIndexMap(MetadataStandard)}.
     * Cached on assumption that the same map will be used more than once before to move to another metadata object.
     */
    private Map<String,Integer> indices;

    /**
     * The last converter used. This field exists only for performance purposes, on
     * the assumption that the last used converter has good chances to be used again.
     */
    private ObjectConverter<?,?> converter;

    /**
     * Creates a new cache.
     */
    LookupInfo() {
    }

    /**
     * Returns the type of metadata objects for which {@link #asNameMap(MetadataStandard)} or
     * {@link #asIndexMap(MetadataStandard)} will return "name to name" or "name to index" mappings.
     */
    final Class<?> getMetadataType() {
        return type;
    }

    /**
     * Sets the type of metadata objects for which {@link #asNameMap(MetadataStandard)} or
     * {@link #asIndexMap(MetadataStandard)} will return "name to name" or "name to index" mappings.
     *
     * @param  t  the type of metadata object for which to get column names or property indices.
     */
    final void setMetadataType(final Class<?> t) {
        if (type != t) {
            type    = t;
            names   = null;
            indices = null;
        }
    }

    /**
     * Maps method names to the name of columns in the database table corresponding to the current
     * {@linkplain #getMetadataType() metadata type}. The values in the returned map must be the
     * same than the keys in the map returned by {@link MetadataSource#asValueMap(Object)}.
     *
     * @throws ClassCastException if the metadata object type does not extend a metadata interface
     *         of the expected package.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<String,String> asNameMap(final MetadataStandard standard) {
        if (names == null) {
            names = standard.asNameMap(type, KeyNamePolicy.METHOD_NAME, MetadataSource.NAME_POLICY);
        }
        return names;
    }

    /**
     * Maps method names to property indices for the current {@linkplain #getMetadataType() metadata type}.
     *
     * @return a map from method names to property indices.
     * @throws ClassCastException if the metadata object type does not extend a metadata interface
     *         of the expected package.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<String,Integer> asIndexMap(final MetadataStandard standard) {
        if (indices == null) {
            indices = standard.asIndexMap(type, KeyNamePolicy.METHOD_NAME);
        }
        return indices;
    }

    /**
     * Converts the specified non-metadata value into an object of the expected type.
     * The expected value is an instance of a class outside the metadata package, for example
     * {@link String}, {@link org.opengis.util.InternationalString}, {@link java.net.URI}, <i>etc.</i>
     *
     * @throws UnconvertibleObjectException if the value can not be converter.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    final Object convert(final Class<?> targetType, Object value) {
        final Class<?> sourceType = value.getClass();
        if (!targetType.isAssignableFrom(sourceType)) {
            if (converter == null || !converter.getSourceClass().isAssignableFrom(sourceType) ||
                                     !targetType.isAssignableFrom(converter.getTargetClass()))
            {
                converter = ObjectConverters.find(sourceType, targetType);
            }
            value = ((ObjectConverter) converter).apply(value);
        }
        return value;
    }
}
