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
package org.apache.sis.storage.isobmff.base;

import java.util.Map;
import java.io.IOException;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.TreeNode;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * Associates items with item properties. Each item property is a {@link Box} or {@link FullBox}.
 * Each association may be marked as either essential or non-essential. If any essential property
 * is not recognized by the reader, then the reader should not process that item.
 *
 * <h4>Container</h4>
 * The container can be an {@link ItemProperties} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ItemPropertyAssociation extends FullBox {
    /**
     * Numerical representation of the {@code "ipma"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'p') << 8) | 'm') << 8) | 'a';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * An entry in the enclosing {@link ItemPropertyAssociation}.
     */
    public static final class Entry extends TreeNode {
        /**
         * Identifies the item with which properties are associated.
         */
        @Interpretation(Type.IDENTIFIER)
        public final int itemID;

        /**
         * 1-based index of the associated property box, together with whether the property is essential.
         * The first bit is the essential bit flag. All remaining bits are the 1-based index.
         */
        private final short[] propertyIndex;

        /**
         * Creates a new entry and loads the payload from the given reader.
         *
         * @param  reader  the reader from which to read the payload.
         * @param  flags   value of {@link ItemPropertyAssociation#flags}.
         * @throws IOException if an error occurred while reading the payload.
         */
        Entry(final ChannelDataInput input, final int flags) throws IOException {
            itemID = (flags & ~((1 << VERSION_BIT_SHIFT) - 1)) == 0 ? input.readUnsignedShort() : input.readInt();
            final int n = input.readUnsignedByte();
            if ((flags & 1) != 0) {
                propertyIndex = input.readShorts(n);
            } else {
                propertyIndex = new short[n];
                for (int i=0; i<n; i++) {
                    propertyIndex[i] = (short) (input.readByte() & 0x807F);
                }
            }
        }

        /**
         * Returns the number of property indices.
         *
         * @return number of property indices.
         */
        public int count() {
            return propertyIndex.length;
        }

        /**
         * Returns whether the associated property is essential to the item.
         *
         * @param  i  0-based index of the property to query.
         * @return whether the property is essential.
         */
        public boolean essential(final int i) {
            return propertyIndex[i] < 0;
        }

        /**
         * Returns either 0 to indicate that no property is associated, or the 0-based index of the
         * associated property box. The index counts all boxes, including {@link FreeSpace} boxes.
         *
         * @param  i  0-based index of the property to query.
         * @return 0-based index of the associated box.
         */
        public int propertyIndex(final int i) {
            return (propertyIndex[i] & Short.MAX_VALUE) - 1;
        }

        /**
         * Appends properties other than the ones defined by public fields.
         * Those properties will be shown last in the tree.
         *
         * @param  context  the tree being formatted. Can be used for fetching contextual information.
         * @param  target   the node where to add properties.
         * @param  after    {@code false} for the first nodes, or {@code true} for the last nodes.
         *
         * @see ItemProperties#collect(Map)
         */
        @Override
        protected void appendTreeNodes(final Tree context, final TreeTable.Node target, final boolean after) {
            super.appendTreeNodes(context, target, after);
            if (after) {
                final Box[] properties = context.getContext(Box[].class);
                final TreeTable.Node indexes = target.newChild();
                indexes.setValue(TableColumn.NAME, "property index");
                for (int i : propertyIndex) {
                    int index = i & Short.MAX_VALUE;
                    String name = String.valueOf(index);
                    if (i < 0) name += " (essential)";
                    TreeTable.Node child = indexes.newChild();
                    child.setValue(TableColumn.NAME, name);
                    child.setValue(TableColumn.VALUE, i);
                    if (properties != null && --index >= 0 && index < properties.length) {
                        final Box property = properties[index];
                        if (property != null) {
                            child.setValue(TableColumn.VALUE_AS_TEXT, property.typeName());
                        }
                    }
                }
            }
        }
    }

    /**
     * All entries in this {@code ItemPropertyAssociation}.
     */
    public final Entry[] entries;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     */
    public ItemPropertyAssociation(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        entries = new Entry[input.readInt()];
        for (int i=0; i<entries.length; i++) {
            entries[i] = new Entry(input, flags);
        }
    }
}
