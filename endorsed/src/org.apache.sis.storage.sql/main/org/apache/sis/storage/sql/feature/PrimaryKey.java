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
package org.apache.sis.storage.sql.feature;

import java.util.List;
import java.util.Collection;
import java.util.StringJoiner;
import org.apache.sis.util.Classes;
import org.apache.sis.util.collection.Containers;


/**
 * Represents SQL primary key constraint.
 * It contains the list of columns composing the key.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class PrimaryKey {
    /**
     * The class of primary key values. If the primary key use more than one column,
     * then this field is the class of an array; it may be an array of primitive type.
     */
    final Class<?> valueClass;

    /**
     * For sub-class constructors only.
     */
    PrimaryKey(final Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    /**
     * Creates a new key for the given columns, or returns {@code null} if none.
     *
     * @param  columns  the columns composing the primary key. May be empty.
     * @return the primary key, or {@code null} if the given list is empty.
     */
    static PrimaryKey create(final Class<?> valueClass, final Collection<String> columns) {
        if (columns.isEmpty()) {
            return null;
        }
        final String c = Containers.peekIfSingleton(columns);
        return (c != null) ? new Single(valueClass, c) : new Composite(valueClass, columns);
    }

    /**
     * Returns the list of column names composing the key.
     * Shall never be null nor empty.
     *
     * @return column names composing the key. Contains at least one element.
     */
    public abstract List<String> getColumns();

    /**
     * A primary key composed of exactly one column.
     */
    private static final class Single extends PrimaryKey {
        /** The single column name. */
        private final String column;

        /** Creates a new primary key composed of the given column. */
        Single(final Class<?> valueClass, final String column) {
            super(valueClass);
            this.column = column;
        }

        /** Returns the single column composing this primary key. */
        @Override
        public List<String> getColumns() {
            return List.of(column);
        }
    }

    /**
     * A primary key composed of two or more columns.
     */
    private static final class Composite extends PrimaryKey {
        /** Name of columns composing the primary key. */
        private final List<String> columns;

        /** Creates a new primary key composed of the given columns. */
        Composite(final Class<?> valueClass, final Collection<String> columns) {
            super(valueClass);
            this.columns = Containers.copyToImmutableList(columns, String.class);
        }

        /** Returns all columns composing this primary key. */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public List<String> getColumns() {
            return columns;
        }
    }

    /**
     * Returns a string representation of this primary key for debugging purposes.
     */
    @Override
    public String toString() {
        var buffer = new StringJoiner(", ", "(", ")");
        getColumns().forEach(buffer::add);
        return buffer + " as " + Classes.getShortName(valueClass);
    }
}
