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
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Identifies a column in {@link TreeTable.Node} instances.
 * Each {@code TableColumn} instance contains the column header and the type of values
 * for a particular column. {@code TableColumn}s are used for fetching values from nodes
 * as in the following example:
 *
 * {@preformat java
 *     public class CityLocation {
 *         public static final ColumnTable<String> CITY_NAME = new ColumnTable<>(String.class, "City name");
 *         public static final ColumnTable<Float>  LATITUDE  = new ColumnTable<>(Float.class,  "Latitude");
 *         public static final ColumnTable<Float>  LONGITUDE = new ColumnTable<>(Float.class,  "Longitude");
 *
 *         private String name;
 *         private float  latitude;
 *         private float  longitude;
 *
 *         CityLocation(TreeTable.Node myNode) {
 *             name      = myNode.getValue(CITY_NAME);
 *             latitude  = myNode.getValue(LATITUDE);
 *             longitude = myNode.getValue(LONGITUDE);
 *         }
 *     }
 * }
 *
 * <div class="section">Identity comparisons and serialization</div>
 * This base class relies on <cite>identity comparisons</cite> instead than defining the
 * {@code equals(Object)} method, because the {@linkplain #getElementType() element type}
 * is not a sufficient criterion for differentiating the columns (many columns have values
 * of the same type) and the {@linkplain #getHeader() header} is arbitrary. Consequently
 * developers who create their own instances are encouraged to declare them as static final
 * constants as in the above example, and use those constants consistently.
 *
 * <p>This base class is not serializable because the default deserialization mechanism does
 * not resolve automatically the deserialized instances to the above-cited singleton instances.
 * Developers who need serialization support for their own instances have to resolve them in
 * their own subclass. The following example is one possible way to achieve that goal:</p>
 *
 * {@preformat java
 *     public class CityLocation {
 *         public static final ColumnTable<String> CITY_NAME = new Column<>("CITY_NAME", String.class, "City name");
 *         public static final ColumnTable<Float>  LATITUDE  = new Column<>("LATITUDE",  Float.class,  "Latitude");
 *         public static final ColumnTable<Float>  LONGITUDE = new Column<>("LONGITUDE", Float.class,  "Longitude");
 *
 *         private static final class Column<V> extends TableColumn<V> implements Serializable {
 *             private final String field;
 *
 *             private Column(String field, Class<V> type, CharSequence header) {
 *                 super(type, header);
 *                 this.field = field;
 *             }
 *
 *             private Object readResolve() throws ObjectStreamException {
 *                 try {
 *                     return CityLocation.class.getField(field).get(null);
 *                 } catch (Exception cause) { // Many exceptions, including unchecked ones.
 *                     throw new InvalidObjectException(cause.toString());
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * The constants defined in this class use a similar approach for providing serialization support.
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
     * The column {@linkplain #getHeader() header} is <cite>"Name"</cite> (eventually localized) and
     * the column elements are typically instances of {@link String} or {@link InternationalString},
     * depending on whether the data provide localization support or not.
     */
    public static final TableColumn<CharSequence> NAME = new Constant<CharSequence>("NAME",
            CharSequence.class, Vocabulary.Keys.Name);

    /**
     * Frequently-used constant for a column of object identifiers.
     * The column {@linkplain #getHeader() header} is <cite>"Identifier"</cite> (eventually localized)
     * and the column elements are instances of {@link String}.
     */
    public static final TableColumn<String> IDENTIFIER = new Constant<String>("IDENTIFIER",
            String.class, Vocabulary.Keys.Identifier);

    /**
     * Frequently-used constant for a column of index values.
     * The column {@linkplain #getHeader() header} is <cite>"Index"</cite> (eventually localized)
     * and the column elements are instances of {@link Integer}.
     */
    public static final TableColumn<Integer> INDEX = new Constant<Integer>("INDEX",
            Integer.class, Vocabulary.Keys.Index);

    /**
     * Frequently-used constant for a column of object types.
     * The column {@linkplain #getHeader() header} is <cite>"Type"</cite> (eventually localized).
     */
    @SuppressWarnings("unchecked")
    public static final TableColumn<Class<?>> TYPE = new Constant<Class<?>>("TYPE",
            (Class) Class.class, Vocabulary.Keys.Type);

    /**
     * Frequently-used constant for a column of object values.
     * The column {@linkplain #getHeader() header} is <cite>"Value"</cite> (eventually localized) and
     * the column elements can be instance of any kind of objects.
     *
     * @see #VALUE_AS_TEXT
     * @see #VALUE_AS_NUMBER
     */
    public static final TableColumn<Object> VALUE = new Constant<Object>("VALUE",
            Object.class, Vocabulary.Keys.Value);

    /**
     * Frequently-used constant for a column of object textual values.
     * The column {@linkplain #getHeader() header} is <cite>"Value"</cite> (eventually localized) and
     * the column elements are typically instances of {@link String} or {@link InternationalString},
     * depending on whether the data provide localization support or not.
     */
    public static final TableColumn<CharSequence> VALUE_AS_TEXT = new Constant<CharSequence>("VALUE_AS_TEXT",
            CharSequence.class, Vocabulary.Keys.Value);

    /**
     * Frequently-used constant for a column of object numerical values.
     * The column {@linkplain #getHeader() header} is <cite>"Value"</cite> (eventually localized).
     */
    public static final TableColumn<Number> VALUE_AS_NUMBER = new Constant<Number>("VALUE_AS_NUMBER",
            Number.class, Vocabulary.Keys.Value);

    /**
     * A map containing only the {@link #NAME} column.
     * This is the default set of columns when parsing a tree table.
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
        private static final long serialVersionUID = -3460868641711391888L;

        /**
         * The programmatic name of the static final field holding this constant.
         */
        private final String field;

        /**
         * The resource key for the column header.
         */
        private final transient short resourceKey;

        /**
         * Creates a new instance for a build-in constant.
         *
         * @param field  The programmatic name of the static final field holding this constant.
         * @param type   Base type of all values in the column identified by this instance.
         * @param header The resource key for the column header.
         */
        Constant(final String field, final Class<V> type, final short header) {
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
            } catch (Exception cause) { // (ReflectiveOperationException) on JDK7 branch.
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
     * See the <cite>Identity comparisons and serialization</cite> section in the class
     * javadoc for more information.
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
     * The default implementation returns the {@linkplain #getHeader() header}
     * in its default locale.
     */
    @Override
    public String toString() {
        return String.valueOf(getHeader());
    }
}
