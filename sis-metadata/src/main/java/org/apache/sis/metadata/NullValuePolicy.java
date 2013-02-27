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
package org.apache.sis.metadata;


/**
 * Whatever {@link MetadataStandard#asMap MetadataStandard.asMap(…)} shall contain entries
 * for null values or empty collections. By default the map does not provide
 * {@linkplain java.util.Map.Entry entries} for {@code null} metadata attributes or
 * {@linkplain java.util.Collection#isEmpty() empty} collections.
 * This enumeration allows control on this behavior.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 *
 * @see MetadataStandard#asMap(Object, KeyNamePolicy, NullValuePolicy)
 */
public enum NullValuePolicy {
    /**
     * Includes all entries in the map, including those having a null value or an
     * empty collection.
     */
    ALL,

    /**
     * Includes only the non-null attributes.
     * Collections are included no matter if they are empty or not.
     */
    NON_NULL,

    /**
     * Includes only the attributes that are non-null and, in the case of collections,
     * non-{@linkplain java.util.Collection#isEmpty() empty}.
     * This is the default behavior of {@link AbstractMetadata#asMap()}.
     */
    NON_EMPTY
}
