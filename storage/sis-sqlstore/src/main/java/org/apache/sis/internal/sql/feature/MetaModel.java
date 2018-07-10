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
package org.apache.sis.internal.sql.feature;

import java.util.Collection;
import java.sql.DatabaseMetaData;
import org.apache.sis.util.Debug;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;


/**
 * Description about a database entity (schema, table, relation, <i>etc</i>).
 * Information provided by subclasses are inferred from {@link DatabaseMetaData}
 * and stored as {@link org.apache.sis.feature} classes.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class MetaModel {
    /**
     * The entity name (schema, table, <i>etc</i>).
     * May be null, for example if this is the primary key name and that name is unspecified.
     */
    final String name;

    /**
     * Creates a new object describing a database entity (schema, table, <i>etc</i>).
     *
     * @param  name  the database entity name, or {@code null} if unspecified.
     */
    MetaModel(final String name) {
        this.name = name;
    }

    /**
     * Creates a tree representation of this object for debugging purpose.
     * The default implementation adds a single node with the {@link #name} of this entity
     * and returns that node. Subclasses can override this method for appending additional
     * information.
     *
     * @param  parent  the parent node where to add the tree representation.
     * @return the node added by this method.
     */
    @Debug
    TreeTable.Node appendTo(final TreeTable.Node parent) {
        return newChild(parent, name);
    }

    /**
     * Adds a child of the given name to the given parent node.
     * This is a convenience method for {@link #appendTo(TreeTable.Node)} implementations.
     *
     * @param  parent  the node where to add a child.
     * @param  name    the name to assign to the child.
     * @return the child added to the parent.
     */
    @Debug
    static TreeTable.Node newChild(final TreeTable.Node parent, final String name) {
        final TreeTable.Node child = parent.newChild();
        child.setValue(TableColumn.NAME, (name != null) ? name : Vocabulary.format(Vocabulary.Keys.Unnamed));
        return child;
    }

    /**
     * Appends all children to the given parent. The children are added under a node of the given name.
     * If the children collection is empty, then this method does nothing.
     *
     * @param  parent    the node where to add children.
     * @param  name      the name of a node to insert between the parent and the children, or {@code null} if none.
     * @param  children  the children to add, or an empty collection if none.
     */
    @Debug
    static void appendAll(TreeTable.Node parent, final String name, final Collection<? extends MetaModel> children) {
        if (!children.isEmpty()) {
            if (name != null) {
                parent = newChild(parent, name);
            }
            for (final MetaModel child : children) {
                child.appendTo(parent);
            }
        }
    }

    /**
     * Formats a graphical representation of this object for debugging purpose. This representation can
     * be printed to the {@linkplain System#out standard output stream} (for example) if the output device
     * uses a monospaced font and supports Unicode.
     */
    @Override
    public final String toString() {
        final DefaultTreeTable table = new DefaultTreeTable(TableColumn.NAME);
        appendTo(table.getRoot());
        return table.toString();
    }
}
