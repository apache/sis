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
package org.apache.sis.test.foreigner;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;
import org.apache.sis.util.collection.TableColumn;


/**
 * For testing {@link TableColumn} deserialization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 *
 * @param <V>  base type of all values in the column identified by this instance.
 *
 * @since 0.3
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class SerializableTableColumn<V> extends TableColumn<V> implements Serializable {
    /**
     * A constant for column of latitudes as floating point value.
     */
    public static final TableColumn<Float> LATITUDE = new SerializableTableColumn<>("LATITUDE", Float.class, "Latitude");

    /**
     * A constant for column of longitudes as floating point value.
     */
    public static final TableColumn<Float> LONGITUDE = new SerializableTableColumn<>("LONGITUDE", Float.class, "Longitude");

    /**
     * The programmatic name of the static final field holding this constant.
     */
    private final String field;


    /**
     * Creates a new instance for the given type of values.
     *
     * @param  field   the programmatic name of the static final field holding this constant.
     * @param  type    base type of all values in the column identified by this instance.
     * @param  header  the text to display as column header.
     */
    private SerializableTableColumn(final String field, final Class<V> type, final CharSequence header) {
        super(type, header);
        this.field = field;
    }

    /**
     * Invoked on deserialization for resolving this instance to one of the predefined constants.
     *
     * @return one of the predefined constants.
     * @throws InvalidObjectException if this instance can not be resolved.
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            return SerializableTableColumn.class.getField(field).get(null);
        } catch (Exception cause) {                 // Many exceptions, including unchecked ones.
            throw (InvalidObjectException) new InvalidObjectException(cause.toString()).initCause(cause);
        }
    }
}
