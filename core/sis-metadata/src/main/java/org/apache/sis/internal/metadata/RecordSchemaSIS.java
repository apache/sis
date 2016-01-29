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
package org.apache.sis.internal.metadata;

import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.iso.DefaultRecordSchema;


/**
 * The system-wide schema in the "SIS" namespace.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class RecordSchemaSIS extends DefaultRecordSchema implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2708181165532467516L;

    /**
     * The schema used in SIS for creating records.
     */
    static final DefaultRecordSchema INSTANCE = new RecordSchemaSIS();

    /**
     * Creates the unique instance.
     */
    private RecordSchemaSIS() {
        super(null, null, Constants.SIS);
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
        private static final long serialVersionUID = -4381124182735566127L;

        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
