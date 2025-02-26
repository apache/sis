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
package org.apache.sis.storage.isobmff;

import java.util.UUID;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;


/**
 * A box identified by an <abbr>UUID</abbr>.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class Extension extends Box {
    /**
     * Numerical representation of the {@code "uuid"} box type.
     */
    public static final int BOXTYPE = ((((('u' << 8) | 'u') << 8) | 'i') << 8) | 'd';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Creates a new box. Subclass constructors should read the payload.
     */
    protected Extension() {
    }

    /**
     * Returns the identifier of this extension.
     * This is the value of the {@code EXTENDED_TYPE} constant defined in each sub-class.
     *
     * @return the user-extension identifier.
     */
    @Override
    public abstract UUID extendedType();

    /**
     * Returns a unique key for the type of this box.
     */
    @Override
    public final Object typeKey() {
        return extendedType();
    }

    /**
     * Appends properties other than the ones defined by public fields.
     * Those properties will be shown first in the tree.
     *
     * @param  context  the tree being formatted. Can be used for fetching contextual information.
     * @param  target   the node where to add properties.
     * @param  after    {@code false} for the first nodes, or {@code true} for the last nodes.
     */
    @Override
    protected void appendTreeNodes(final Tree context, final TreeTable.Node target, final boolean after) {
        super.appendTreeNodes(context, target, after);
        if (!after) {
            final UUID value = extendedType();
            if (value != null) {
                final TreeTable.Node child = target.newChild();
                child.setValue(TableColumn.NAME, "extendedType");
                child.setValue(TableColumn.VALUE, value);
                child.setValue(TableColumn.VALUE_AS_TEXT, value.toString());
            }
        }
    }
}
