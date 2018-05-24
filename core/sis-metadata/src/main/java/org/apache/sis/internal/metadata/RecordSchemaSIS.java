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
import java.util.Collections;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.iso.DefaultRecordType;
import org.apache.sis.util.iso.DefaultRecordSchema;
import org.apache.sis.util.resources.Vocabulary;


/**
 * The system-wide schema in the "SIS" namespace.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
@SuppressWarnings("serial")  // serialVersionUID not needed because of writeReplace().
public final class RecordSchemaSIS extends DefaultRecordSchema implements Serializable {
    /**
     * The schema used in SIS for creating records.
     */
    public static final DefaultRecordSchema INSTANCE = new RecordSchemaSIS();

    /**
     * The type of record instances for holding a {@link String} value.
     */
    public static final DefaultRecordType STRING;

    /**
     * The type of record instances for holding a {@link Double} value.
     */
    public static final DefaultRecordType REAL;
    static {
        final InternationalString label = Vocabulary.formatInternational(Vocabulary.Keys.Value);
        STRING = (DefaultRecordType) INSTANCE.createRecordType("CharacterSequence", Collections.singletonMap(label, String.class));
        REAL   = (DefaultRecordType) INSTANCE.createRecordType("Real",              Collections.singletonMap(label, Double.class));
    }

    /**
     * Creates the unique instance.
     */
    private RecordSchemaSIS() {
        super(null, null, Constants.SIS);
    }

    /**
     * On serialization, returns a proxy which will be resolved as {@link #INSTANCE} on deserialization.
     *
     * @return the object to use after deserialization.
     * @throws ObjectStreamException if the serialized object defines an unknown data type.
     */
    protected Object writeReplace() throws ObjectStreamException {
        return new Proxy();
    }

    /**
     * The object to serialize instead of {@link DefaultRecordSchema}.
     * This proxy is itself replaced by {@link RecordSchemaSIS#INSTANCE} on deserialization.
     */
    private static final class Proxy implements Serializable {
        private static final long serialVersionUID = -4381124182735566127L;

        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
