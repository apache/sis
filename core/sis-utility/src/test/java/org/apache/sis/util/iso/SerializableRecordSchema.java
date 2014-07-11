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

import java.io.Serializable;
import java.io.ObjectStreamException;


/**
 * A serializable {@link DefaultRecordSchema} for testing purpose only.
 * On deserialization, the schema is replaced by the {@link #INSTANCE}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@SuppressWarnings("serial")
final class SerializableRecordSchema extends DefaultRecordSchema implements Serializable {
    /**
     * The unique instance for the shema.
     */
    static DefaultRecordSchema INSTANCE;

    /**
     * Construct a new record schema.
     *
     * @param schemaName The name of the new schema.
     */
    SerializableRecordSchema(final String schemaName) {
        super(null, null, schemaName);
    }

    /**
     * On serialization, returns a proxy which will be resolved as {@link #INSTANCE} on deserialization.
     */
    Object writeReplace() throws ObjectStreamException {
        return new Proxy();
    }

    /**
     * The object to serialize instead of {@link DefaultRecordSchema}.
     * This proxy is itself replaced by {@link SerializableRecordSchema#INSTANCE} on deserialization.
     */
    private static final class Proxy implements Serializable {
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
