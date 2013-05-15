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
package org.apache.sis.internal.netcdf.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;


/**
 * Attribute found in a NetCDF file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class Attribute {
    /**
     * The attribute name.
     */
    final String name;

    /**
     * The value, either as a {@link String} or as an array of primitive type.
     */
    final Object value;

    /**
     * Creates a new attribute of the given name and value.
     */
    Attribute(final String name, final Object value) {
        this.name  = name;
        this.value = value;
    }

    /**
     * Creates a (<cite>name</cite>, <cite>attribute</cite>) mapping for the given array of attributes.
     *
     * @throws DataStoreException If an attribute is defined twice.
     */
    static Map<String,Attribute> toMap(final Attribute[] attributes) throws DataStoreException {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        final Map<String,Attribute> map = new HashMap<>(Containers.hashMapCapacity(attributes.length));
        for (final Attribute attribute : attributes) {
            if (map.put(attribute.name, attribute) != null) {
                throw new DataStoreException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, attribute.name));
            }
        }
        return map;
    }
}
