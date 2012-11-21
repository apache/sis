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

import java.text.Format;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;


/**
 * Static methods working on {@link TreeTable} objects and their nodes.
 * This class provides convenience factory methods for creating nodes with
 * some frequently-used columns.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class TreeTables extends Static {
    /**
     * Shared {@code TreeTableFormat} instance for {@link #toString()} implementation.
     */
    private static Format format;

    /**
     * Do not allow instantiation of this class.
     */
    private TreeTables() {
    }

    /**
     * Creates a node with a single column for object names.
     * The node will have the following columns:
     *
     * <table class="sis">
     *   <tr><th>Header</th> <th>Type</th>                 <th>Initial value</th></tr>
     *   <tr><td>"Name"</td> <td>{@link CharSequence}</td> <td>{@code name}</td></tr>
     * </table>
     *
     * @param  name The initial value for the "Name" column (can be {@code null}).
     * @return A new node with a name column initialized to the given value.
     */
    public static TreeTable.Node createNode(final CharSequence name) {
        return new DefaultTreeTable.Node(TableColumn.NAME_MAP, (name != null) ? new CharSequence[] {name} : null);
    }

    /**
     * Returns a string representation of the given tree table.
     * The default implementation uses a shared instance of {@link TreeTableFormat}.
     * This is okay for debugging or occasional usages. However for more extensive usages,
     * developers are encouraged to create and configure their own {@code TreeTableFormat}
     * instance.
     *
     * @param  table The tree table to format.
     * @return A string representation of the given tree table.
     */
    public static synchronized String toString(final TreeTable table) {
        ArgumentChecks.ensureNonNull("table", table);
        if (format == null) {
            format = new TreeTableFormat(null, null);
        }
        return format.format(table);
    }
}
