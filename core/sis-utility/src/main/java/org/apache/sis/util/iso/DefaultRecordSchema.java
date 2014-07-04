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
package org.apache.sis.util.iso;

import java.util.Map;
import java.util.Collections;
import java.io.Serializable;
import org.opengis.util.LocalName;
import org.opengis.util.TypeName;
import org.opengis.util.RecordType;
import org.opengis.util.RecordSchema;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.WeakValueHashMap;


/**
 * A collection of record types in a given namespace.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class DefaultRecordSchema implements RecordSchema, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7508781073649388985L;

    /**
     * The schema name given at construction time.
     */
    private final LocalName schemaName;

    /**
     * The record types in the namespace of this schema.
     */
    final Map<TypeName,RecordType> description;

    /**
     * Creates a new schema of the given name.
     *
     * @param schemaName The name of the new schema.
     */
    DefaultRecordSchema(final LocalName schemaName) {
        this.schemaName = schemaName;
        description = new WeakValueHashMap<>(TypeName.class);
    }

    /**
     * Returns the schema name.
     *
     * @return The schema name.
     */
    @Override
    public LocalName getSchemaName() {
        return schemaName;
    }

    /**
     * Returns the dictionary of all (<var>name</var>, <var>record type</var>) pairs in this schema.
     *
     * @return All (<var>name</var>, <var>record type</var>) pairs in this schema.
     */
    @Override
    public Map<TypeName, RecordType> getDescription() {
        return Collections.unmodifiableMap(description);
    }

    /**
     * Returns the record type for the given name.
     * If the type name is not defined within this schema, then this method returns {@code null}.
     *
     * @param  name The name of the type to lookup.
     * @return The type for the given name, or {@code null} if none.
     */
    @Override
    public RecordType locate(final TypeName name) {
        return description.get(name);
    }

    /**
     * Returns a string representation of this schema for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        return "RecordSchema[“" + schemaName + "”]";
    }
}
