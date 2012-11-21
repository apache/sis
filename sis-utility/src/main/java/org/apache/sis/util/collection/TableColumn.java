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
package org.apache.sis.util.collection;

import java.util.Map;
import java.util.Collections;
import java.io.Serializable;
import java.io.InvalidObjectException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.type.Types;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Identifies a column in {@link TreeTable.Node} instances.
 * Each {@code TableColumn} instance contains the column header and the type of values
 * for a particular column. {@code TableColumn}s are used for fetching values from nodes
 * as in the following example:
 *
 * {@preformat java
 *     class CityLocation {
 *         private String name;
 *         private float  latitude;
 *         private float  longitude;
 *
 *         CityLocation(TreeTable.Node myNode) {
 *             name      = myNode.getValue(NAME);
 *             latitude  = myNode.getValue(LATITUDE);
 *             longitude = myNode.getValue(LONGITUDE);
 *         }
 *     }
 * }
 *
 * {@section Identity comparisons and serialization}
 * This base class relies on <cite>identity comparisons</cite> instead than defining the
 * {@code equals(Object)} method, because the {@linkplain #getElementType() element type}
 * is not a sufficient criterion for differentiating the columns (many columns have values
 * of the same type) and the {@linkplain #getHeader() header} is arbitrary. Developers who
 * create their own instances are encouraged to declare them as static final constants.
 *
 * <p>This base class is not {@linkplain Serializable serializable}. However the pre-defined
 * constants defined in this class are serializable. Developers who need custom serializable
 * columns are encouraged to create their own subclass and resolve to the singleton instance
 * on deserialization.</p>
 *
 * @param <V> Base type of all values in the column identified by this instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class TableColumn<V> implements CheckedContainer<V> {
    /**
     * Frequently-used constant for a column of object names.
     * The values are typically instances of {@link String} or {@link InternationalString},
     * depending on whether the data provide localization support or not.
     */
    public static final TableColumn<CharSequence> NAME = new Constant<>("NAME",
            CharSequence.class, Vocabulary.Keys.Name);

    /**
     * Frequently-used constant for a column of object types.
     * The values are instances of {@link Class}.
     */
    @SuppressWarnings("unchecked")
    public static final TableColumn<Class<?>> TYPE = new Constant<>("TYPE",
            (Class) Class.class, Vocabulary.Keys.Type);

    /**
     * A map containing only the {@link #NAME} column.
     * This is the default set of columns when parsing a table tree.
     */
    static final Map<TableColumn<?>,Integer> NAME_MAP =
            Collections.<TableColumn<?>,Integer>singletonMap(NAME, 0);

    /**
     * Base type of all values in the column identified by this {@code ColumnTable} instance.
     */
    private final Class<V> type;

    /**
     * The column header, or {@code null} if not yet created.
     */
    CharSequence header;

    /**
     * Implementation of {@link TableColumn} for the pre-defined constants.
     * This implementation differs resource bundle loading until first needed,
     * and resolves deserialized instances to the singleton instances.
     *
     * @param <V> Base type of all values in the column identified by this instance.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private static final class Constant<V> extends TableColumn<V> implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -2486202389234601560L;

        /**
         * The programmatic name of the static final field holding this constant.
         */
        private final String field;

        /**
         * The resource key for the column header.
         */
        private final transient int resourceKey;

        /**
         * Creates a new instance for a build-in constant.
         *
         * @param field  The programmatic name of the static final field holding this constant.
         * @param type   Base type of all values in the column identified by this instance.
         * @param header The resource key for the column header.
         */
        Constant(final String field, final Class<V> type, final int header) {
            super(type);
            this.field       = field;
            this.resourceKey = header;
        }

        /**
         * Returns the text to display as column header.
         */
        @Override
        public synchronized InternationalString getHeader() {
            InternationalString i18n = (InternationalString) header;
            if (i18n == null) {
                header = i18n = Vocabulary.formatInternational(resourceKey);
            }
            return i18n;
        }

        /**
         * Invoked on deserialization for resolving this instance to one of the predefined constants.
         *
         * @return One of the predefined constants.
         * @throws InvalidObjectException If this instance can not be resolved.
         */
        private Object readResolve() throws InvalidObjectException {
            try {
                return TableColumn.class.getField(field).get(null);
            } catch (Exception cause) { // Many exceptions, including unchecked ones.
                InvalidObjectException e = new InvalidObjectException(cause.toString());
                e.initCause(cause);
                throw e;
            }
        }
    }

    /**
     * Invoked on deserialization for creating an initially empty instance.
     * This constructor has {@code protected} visibility only because the Java deserialization
     * mechanism requires so; this constructor shall not be invoked in any other context.
     *
     * <p>Subclasses are responsible for resolving the deserialized instance to a singleton
     * instance. This can be done by the following method, which assume that the subclass
     * declares a public static field named {@code fieldName}:</p>
     *
     * {@preformat java
     *     private Object readResolve() throws InvalidObjectException {
     *         try {
     *             return getClass().getField(fieldName).get(null);
     *         } catch (Exception cause) { // Many exceptions, including unchecked ones.
     *             InvalidObjectException e = new InvalidObjectException(cause.toString());
     *             e.initCause(cause);
     *             throw e;
     *         }
     *     }
     * }
     */
    protected TableColumn() {
        type = null;
    }

    /**
     * Creates a new instance for a build-in constant.
     *
     * @param type Base type of all values in the column identified by this instance.
     */
    TableColumn(final Class<V> type) {
        this.type = type;
    }

    /**
     * Creates a new instance for the given type of values.
     *
     * @param type   Base type of all values in the column identified by this instance.
     * @param header The text to display as column header.
     */
    public TableColumn(final Class<V> type, final CharSequence header) {
        ArgumentChecks.ensureNonNull("type",   this.type   = type);
        ArgumentChecks.ensureNonNull("header", this.header = header);
        this.header = Types.toInternationalString(header);
    }

    /**
     * Returns the text to display as column header.
     *
     * @return The text to display as column header.
     */
    public synchronized InternationalString getHeader() {
        final InternationalString i18n = Types.toInternationalString(header);
        header = i18n;
        return i18n;
    }

    /**
     * Returns the base type of all values in any column identified by this {@code TableColumn}
     * instance.
     */
    @Override
    public final Class<V> getElementType() {
        return type;
    }

    /**
     * Returns a string representation of this table column.
     */
    @Override
    public String toString() {
        return getHeader().toString(null);
    }
}
