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
import java.io.InvalidObjectException;
import org.apache.sis.util.collection.TableColumn;


/**
 * For testing {@link TableColumn} deserialization.
 *
 * @param <V> Base type of all values in the column identified by this instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class SerializableTableColumn<V> extends TableColumn<V> implements Serializable {
    /**
     * A constant for column of latitudes as floating point value.
     */
    public static final TableColumn<Float> LATITUDE = new SerializableTableColumn<Float>("LATITUDE", Float.class, "Latitude");

    /**
     * A constant for column of longitudes as floating point value.
     */
    public static final TableColumn<Float> LONGITUDE = new SerializableTableColumn<Float>("LONGITUDE", Float.class, "Longitude");

    /**
     * The programmatic name of the static final field holding this constant.
     */
    private final String field;


    /**
     * Creates a new instance for the given type of values.
     *
     * @param field  The programmatic name of the static final field holding this constant.
     * @param type   Base type of all values in the column identified by this instance.
     * @param header The text to display as column header.
     */
    private SerializableTableColumn(final String field, final Class<V> type, final CharSequence header) {
        super(type, header);
        this.field = field;
    }

    /**
     * Invoked on deserialization for resolving this instance to one of the predefined constants.
     *
     * @return One of the predefined constants.
     * @throws InvalidObjectException If this instance can not be resolved.
     */
    private Object readResolve() throws InvalidObjectException {
        try {
            return SerializableTableColumn.class.getField(field).get(null);
        } catch (Exception cause) { // Many exceptions, including unchecked ones.
            InvalidObjectException e = new InvalidObjectException(cause.toString());
            e.initCause(cause);
            throw e;
        }
    }
}
