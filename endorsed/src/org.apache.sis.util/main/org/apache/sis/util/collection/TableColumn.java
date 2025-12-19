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
import java.util.Objects;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;
import org.opengis.annotation.Obligation;
import org.opengis.util.InternationalString;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Identifies a column in {@link TreeTable.Node} instances.
 * Each {@code TableColumn} instance contains the column header and the type of values
 * for a particular column. {@code TableColumn}s are used for fetching values from nodes
 * as in the following example:
 *
 * {@snippet lang="java" :
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
 *     }
 *
 * <h2>Identity comparisons and serialization</h2>
 * This base class relies on <em>identity comparisons</em> instead of defining the
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
 * {@snippet lang="java" :
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
 *     }
 *
 * The constants defined in this class use a similar approach for providing serialization support.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <V>  base type of all values in the column identified by this instance.
 *
 * @since 0.3
 */
public class TableColumn<V> implements CheckedContainer<V> {
    /**
     * Predefined constant for a column of object names.
     * The column {@linkplain #getHeader() header} is <q>Name</q> (eventually localized) and
     * the column elements are typically instances of {@link String} or {@link InternationalString},
     * depending on whether the data provide localization support or not.
     */
    public static final TableColumn<CharSequence> NAME = new Constant<>("NAME",
            CharSequence.class, Vocabulary.Keys.Name);

    /**
     * Predefined constant for a column of object identifiers.
     * The column {@linkplain #getHeader() header} is <q>Identifier</q> (eventually localized)
     * and the column elements are instances of {@link String}.
     */
    public static final TableColumn<String> IDENTIFIER = new Constant<>("IDENTIFIER",
            String.class, Vocabulary.Keys.Identifier);

    /**
     * Predefined constant for a column of index values.
     * The column {@linkplain #getHeader() header} is <q>Index</q> (eventually localized)
     * and the column elements are instances of {@link Integer}.
     */
    public static final TableColumn<Integer> INDEX = new Constant<>("INDEX",
            Integer.class, Vocabulary.Keys.Index);

    /**
     * Predefined constant for a column of object types.
     * The column {@linkplain #getHeader() header} is <q>Type</q> (eventually localized).
     */
    @SuppressWarnings("unchecked")
    public static final TableColumn<Class<?>> TYPE = new Constant<>("TYPE",
            (Class) Class.class, Vocabulary.Keys.Type);

    /**
     * Predefined constant for a column of obligation (mandatory, optional, conditional).
     * The column {@linkplain #getHeader() header} is <q>Obligation</q> (eventually localized)
     * and the column elements are instances of {@link Obligation}.
     *
     * @since 1.5
     */
    public static final TableColumn<Obligation> OBLIGATION = new Constant<>("OBLIGATION",
            Obligation.class, Vocabulary.Keys.Obligation);

    /**
     * Predefined constant for a column of object values.
     * The column {@linkplain #getHeader() header} is <q>Value</q> (eventually localized) and
     * the column elements can be instance of any kind of objects.
     *
     * @see #VALUE_AS_TEXT
     * @see #VALUE_AS_NUMBER
     */
    public static final TableColumn<Object> VALUE = new Constant<>("VALUE",
            Object.class, Vocabulary.Keys.Value);

    /**
     * Predefined constant for a column of object textual values.
     * The column {@linkplain #getHeader() header} is <q>Value</q> (eventually localized) and
     * the column elements are typically instances of {@link String} or {@link InternationalString},
     * depending on whether the data provide localization support or not.
     */
    public static final TableColumn<CharSequence> VALUE_AS_TEXT = new Constant<>("VALUE_AS_TEXT",
            CharSequence.class, Vocabulary.Keys.Value);

    /**
     * Predefined constant for a column of object numerical values.
     * The column {@linkplain #getHeader() header} is <q>Value</q> (eventually localized).
     */
    public static final TableColumn<Number> VALUE_AS_NUMBER = new Constant<>("VALUE_AS_NUMBER",
            Number.class, Vocabulary.Keys.Value);

    /**
     * Predefined constant for a column of remarks.
     * The column {@linkplain #getHeader() header} is <q>Remarks</q> (eventually localized) and
     * the column elements are typically instances of {@link String} or {@link InternationalString},
     * depending on whether the data provide localization support or not.
     *
     * @since 1.0
     */
    public static final TableColumn<CharSequence> REMARKS = new Constant<>("REMARKS",
            CharSequence.class, Vocabulary.Keys.Remarks);

    /**
     * A map containing only the {@link #NAME} column.
     * This is the default set of columns when parsing a tree table.
     */
    static final Map<TableColumn<?>,Integer> NAME_MAP = Map.of(NAME, 0);

    /**
     * Base type of all values in the column identified by this {@code ColumnTable} instance.
     */
    private final Class<V> type;

    /**
     * The column header, or {@code null} if not yet created.
     */
    CharSequence header;

    /**
     * Implementation of {@link TableColumn} for the predefined constants.
     * This implementation differs resource bundle loading until first needed,
     * and resolves deserialized instances to the singleton instances.
     *
     * @param  <V>  base type of all values in the column identified by this instance.
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
         * @param field   the programmatic name of the static final field holding this constant.
         * @param type    base type of all values in the column identified by this instance.
         * @param header  the resource key for the column header.
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
         * @return one of the predefined constants.
         * @throws InvalidObjectException if this instance cannot be resolved.
         */
        private Object readResolve() throws ObjectStreamException {
            try {
                return TableColumn.class.getField(field).get(null);
            } catch (ReflectiveOperationException cause) {
                throw (InvalidObjectException) new InvalidObjectException(cause.toString()).initCause(cause);
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
     * @param type  base type of all values in the column identified by this instance.
     */
    TableColumn(final Class<V> type) {
        this.type = type;
    }

    /**
     * Creates a new instance for the given type of values.
     *
     * @param type    base type of all values in the column identified by this instance.
     * @param header  the text to display as column header.
     */
    public TableColumn(final Class<V> type, final CharSequence header) {
        this.type   = Objects.requireNonNull(type);
        this.header = Objects.requireNonNull(header);
    }

    /**
     * Returns the text to display as column header.
     *
     * @return the text to display as column header.
     */
    public synchronized InternationalString getHeader() {
        CharSequence t = header;
        if (t == null || t instanceof InternationalString) {
            return (InternationalString) t;
        }
        final InternationalString i18n = new SimpleInternationalString(t.toString());
        header = i18n;
        return i18n;
    }

    /**
     * Returns the base type of all values in any column identified by this {@code TableColumn} instance.
     */
    @Override
    public final Class<V> getElementType() {
        return type;
    }

    /**
     * Indicates that a table header is an immutable object (at least by default).
     *
     * @return {@link Mutability#IMMUTABLE} by default.
     * @since 1.6
     */
    @Override
    public Mutability getMutability() {
        return Mutability.IMMUTABLE;
    }

    /**
     * Returns a string representation of this table column.
     * The default implementation returns the {@linkplain #getHeader() header} in its default locale.
     *
     * @return a string representation of this table column.
     */
    @Override
    public String toString() {
        return String.valueOf(getHeader());
    }
}
