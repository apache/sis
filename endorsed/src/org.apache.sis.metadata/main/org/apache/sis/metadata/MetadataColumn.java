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

import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.sis.xml.NilReason;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TableColumn;


/**
 * A tree table column specific to the metadata module.
 * Defined as a class for allowing serialization.
 *
 * @param <V>  base type of all values in the column identified by this instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see TreeTableView
 */
final class MetadataColumn<V> extends TableColumn<V> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8256073324266678871L;

    /**
     * Table column for the reason why a mandatory property value is absent.
     */
    public static final MetadataColumn<NilReason> NIL_REASON =
            new MetadataColumn<>(NilReason.class, Vocabulary.Keys.NilReason);

    /**
     * Creates a new column header.
     *
     * @param type  base type of all values in the column identified by this instance.
     * @param key   resource key of the localized text to use as the column header.
     */
    private MetadataColumn(final Class<V> type, final short key) {
        super(type, Vocabulary.formatInternational(key));
    }

    /**
     * Invoked on deserialization for resolving this instance to one of the predefined constants.
     *
     * @return one of the predefined constants.
     * @throws InvalidObjectException if this instance cannot be resolved.
     */
    private Object readResolve() throws ObjectStreamException {
        return NIL_REASON;      // For now this is the only column.
    }
}
