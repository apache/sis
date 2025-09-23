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
package org.apache.sis.metadata.internal.shared;

import java.util.Map;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.opengis.util.TypeName;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.internal.Resources;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.iso.DefaultRecordType;
import org.apache.sis.util.iso.DefaultRecordSchema;
import org.apache.sis.util.resources.Vocabulary;


/**
 * The system-wide schema in the "SIS" namespace.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings({"serial", "removal"})  // serialVersionUID not needed because of writeReplace().
public final class RecordSchemaSIS extends DefaultRecordSchema implements Serializable {
    /**
     * The schema used in SIS for creating records.
     */
    public static final DefaultRecordSchema INSTANCE = new RecordSchemaSIS();

    /**
     * The type name for a record having an unknown number of fields.
     * This is used at {@code <gco:RecordType>} unmarshalling time,
     * where the type is not well defined, by assuming one field per line.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-419">SIS-419</a>
     */
    public static final TypeName MULTILINE = INSTANCE.createRecordTypeName(
            Resources.formatInternational(Resources.Keys.MultilineRecord));

    /**
     * The type of record instances for holding a single {@link String} value.
     */
    public static final DefaultRecordType STRING;

    /**
     * The type of record instances for holding a single {@link Double} value.
     */
    public static final DefaultRecordType REAL;
    static {
        final InternationalString field = Vocabulary.formatInternational(Vocabulary.Keys.Value);
        STRING = singleton(Resources.Keys.SingleText,   field, String.class);
        REAL   = singleton(Resources.Keys.SingleNumber, field, Double.class);
    }

    /**
     * Creates the unique instance.
     */
    private RecordSchemaSIS() {
        super(null, null, Constants.SIS);
    }

    /**
     * Creates a new record type of the given name, which will contain the given field.
     *
     * @param  typeName    the record type name as a {@link Resources.Keys} code.
     * @param  field       the name of the singleton record field.
     * @param  valueClass  the expected value type for the singleton field.
     * @return a record type of the given name and field.
     */
    private static DefaultRecordType singleton(final short typeName, final InternationalString field, final Class<?> valueClass) {
        return (DefaultRecordType) INSTANCE.createRecordType(Resources.formatInternational(typeName), Map.of(field, valueClass));
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
