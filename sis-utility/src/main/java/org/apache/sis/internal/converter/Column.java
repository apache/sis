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
package org.apache.sis.internal.converter;

import java.io.Serializable;
import java.io.InvalidObjectException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Debug;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;


/**
 * Columns in the string representation of converter chains.
 * This is used mostly for debugging purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class Column extends TableColumn<Class<?>> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -828524683891584679L;

    /**
     * The column for the source type.
     */
    static final Column SOURCE = new Column(false);

    /**
     * The column for the target type.
     */
    static final Column TARGET = new Column(true);

    /**
     * {@code false} for the source, or {@code true} for the target.
     */
    private final boolean target;

    /**
     * Creates a new column.
     */
    @SuppressWarnings("unchecked")
    private Column(final boolean target) {
        super((Class) Class.class, target ? "target" : "source");
        this.target = target;
    }

    /**
     * Returns the header label.
     */
    @Override
    public InternationalString getHeader() {
        return Vocabulary.formatInternational(target ? Vocabulary.Keys.Destination : Vocabulary.Keys.Source);
    }

    /**
     * Resources to the singleton instance on deserialization.
     */
    private Object readResolve() throws InvalidObjectException {
        return target ? TARGET : SOURCE;
    }

    /**
     * Formats the given tree table. This method is used for the implementation of
     * {@link FallbackConverter#toString()} and {@link ConverterRegistry#toString()}
     * methods. Since they are mostly for debugging purpose, we do not bother to cache
     * the {@link TreeTableFormat} instance.
     */
    @Debug
    static String format(final TreeTable table) {
        final TreeTableFormat format = new TreeTableFormat(null, null);
        format.setColumnSeparatorPattern("[ ] ⇨ ");
        return format.format(table);
    }
}
