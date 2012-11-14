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
package org.apache.sis.internal.util;

import java.io.Serializable;
import java.io.InvalidObjectException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TableColumn;


/**
 * {@link TableColumn} constants used in the SIS library.
 *
 * @param <T> Base type of all values in the column identified by this instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class ColumnConstant<T> implements TableColumn<T>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8638750288322500203L;

    /**
     * Frequently-used constant for a column of object names.
     * The values are typically instances of {@link String} or
     * {@link org.opengis.util.InternationalString}, depending
     * if the data provide localization support or not.
     */
    public static final TableColumn<CharSequence> NAME = new ColumnConstant<>("NAME",
            CharSequence.class, Vocabulary.Keys.Name);

    /**
     * Frequently-used constant for a column of object types.
     * The values are instances of {@link Class}.
     */
    @SuppressWarnings("unchecked")
    public static final TableColumn<Class<?>> TYPE = new ColumnConstant<>("TYPE",
            (Class) Class.class, Vocabulary.Keys.Type);

    /**
     * The programmatic name of the static final field holding this constant.
     */
    private final String name;

    /**
     * Base type of all values in the column identified by this {@code ColumnConstant} instance.
     */
    private final transient Class<T> type;

    /**
     * The resource key for the column header.
     */
    private final transient int resourceKey;

    /**
     * Creates a new instance for the given type of values.
     *
     * @param name The programmatic name of the static final field holding this constant.
     * @param type Base type of all values in the column identified by this instance.
     * @param resourceKey The resource key for the column header.
     */
    private ColumnConstant(final String name, final Class<T> type, final int resourceKey) {
        this.name        = name;
        this.type        = type;
        this.resourceKey = resourceKey;
    }

    /**
     * Returns the text to display as column header.
     */
    @Override
    public InternationalString getHeader() {
        return Vocabulary.formatInternational(resourceKey);
    }

    /**
     * Returns the base type of all values in any column identified by this {@code TableConstant}.
     */
    @Override
    public final Class<T> getElementType() {
        return type;
    }

    /**
     * Returns the name of the field declaring this constant.
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Invoked on deserialization for resolving this instance to one of the predefined constants.
     *
     * @return One of the predefined constants.
     * @throws InvalidObjectException If this instance can not be resolved.
     */
    protected Object readResolve() throws InvalidObjectException {
        try {
            return getClass().getField(name).get(null);
        } catch (Exception cause) { // Many exceptions, including unchecked ones.
            InvalidObjectException e = new InvalidObjectException(cause.toString());
            e.initCause(cause);
            throw e;
        }
    }
}
