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
import java.util.ArrayList;
import java.util.BitSet;
import java.io.IOException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.ContainerBox;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.TreeTable;


/**
 * Association of any item with an ordered set of item properties.
 * Item properties are small data records.
 *
 * <h4>Container</h4>
 * The container can be a {@link Meta} box.
 *
 * <h4>Content</h4>
 * The first child shall be an {@link ItemPropertyContainer} instance.
 * All other children should be {@link ItemPropertyAssociation} instances.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ItemProperties extends ContainerBox {
    /**
     * Numerical representation of the {@code "iprp"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'p') << 8) | 'r') << 8) | 'p';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreException if the stream contains inconsistent or unsupported data.
     */
    public ItemProperties(final Reader reader) throws IOException, DataStoreException {
        super(reader, null, false);
    }

    /**
     * All properties associated to the same item identifier. Instances of this class are created
     * by {@link #collect(Map)} and should be short lived, only the time needed by the caller for
     * processing the properties.
     */
    @SuppressWarnings("serial")
    public static final class ForID extends ArrayList<Box> {
        /**
         * Whether the index at a given index is considered essential.
         */
        private final BitSet essentials;

        /**
         * Whether at least one item is missing, and whether at least one of the missing item is essential.
         */
        public boolean missingItem, missingEssential;

        /**
         * Creates an initially empty list of boxes.
         *
         * @param  itemID  identifier of the item for which properties are collected.
         */
        private ForID(final Integer itemID) {
            essentials = new BitSet();
        }

        /**
         * Adds the item fetched from the given array at the given index, if that item exists.
         *
         * @param  items      the item to from the {@link ItemPropertyContainer} box.
         * @param  index      index of the item to take in the {@code items} array.
         * @param  essential  whether the item is considered essential.
         */
        final void add(final Box[] items, final int index, final boolean essential) {
            if (index >= 0 && index < items.length) {
                final Box item = items[index];
                if (item != null) {
                    if (essential) {
                        essentials.set(size());     // Must be before `add(item)`.
                    }
                    add(item);
                    return;
                }
            }
            missingItem = true;
            missingEssential |= essential;
        }

        /**
         * Returns whether the property at the given index is essential.
         * This method is invoked when the caller does not know how to interpret an item.
         *
         * @param  index  index of the property.
         * @return whether the property at the given index is essential.
         */
        public boolean essential(final int index) {
            return essentials.get(index);
        }

        /**
         * Tests whether the two objects contain the same information.
         * This is used for detecting {@link ItemPropertyAssociation} instances that reference the same boxes.
         * Such detection avoids to recreate the same {@link java.awt.image.SampleModel} for every tiles of an
         * image.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ForID other) {
                return super.equals(other)
                        && essentials.equals(other.essentials)
                        && missingItem == other.missingItem
                        && missingEssential == other.missingEssential;
            }
            return false;
        }

        /**
         * Returns a hash code value for this list of properties.
         */
        @Override
        public int hashCode() {
            return super.hashCode() + 37 * essentials.hashCode();
        }
    }

    /**
     * Gets all properties of each item, grouped by item identifier.
     * Keys are {@link ItemPropertyAssociation.Entry#itemID} values.
     *
     * @param  dest  where to store the properties.
     */
    public final void collect(final Map<Integer, ForID> dest) {
        ItemPropertyContainer   ipco = null;
        ItemPropertyAssociation ipma = null;
        for (final Box box : children) {
            switch (box.type()) {
                case ItemPropertyContainer.BOXTYPE:   ipco = (ItemPropertyContainer)   box; break;
                case ItemPropertyAssociation.BOXTYPE: ipma = (ItemPropertyAssociation) box; break;
                default: continue;
            }
            if (ipco != null && ipma != null) {
                for (ItemPropertyAssociation.Entry entry : ipma.entries) {
                    final ForID properties = dest.computeIfAbsent(entry.itemID, ForID::new);
                    final int n = entry.count();
                    for (int i=0; i<n; i++) {
                        properties.add(ipco.children, entry.propertyIndex(i), entry.essential(i));
                    }
                }
                ipma = null;
                // Do not clear `ipco`, as we may have others `impa` which need to same `ipco`.
            }
        }
    }

    /**
     * Opportunistically saves contextual information before to format the tree.
     * {@link ItemPropertyAssociation.Entry#appendTreeNodes(Tree, TreeTable.Node, boolean)}
     * will need the children of the property container.
     *
     * @param  context  the tree being formatted. Can be used for fetching contextual information.
     * @param  target   the node where to add properties.
     * @param  after    {@code false} for the first nodes, or {@code true} for the last nodes.
     */
    @Override
    protected void appendTreeNodes(final Tree context, final TreeTable.Node target, final boolean after) {
        Box[] properties = null;
        if (!after) {
            for (final Box box : children) {
                if (box.type() == ItemPropertyContainer.BOXTYPE) {
                    properties = ((ItemPropertyContainer) box).children;
                    break;
                }
            }
        }
        context.setContext(Box[].class, properties);
        super.appendTreeNodes(context, target, after);
    }
}
