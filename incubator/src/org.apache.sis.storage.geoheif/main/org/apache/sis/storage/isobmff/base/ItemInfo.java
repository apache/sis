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

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.BoxRegistry;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;
import org.apache.sis.util.collection.TreeTable;


/**
 * Extra information about selected items.
 *
 * <h4>Container</h4>
 * The container can be the file segment, or a {@link Movie}, {@link Track},
 * {@code MovieFragment} or {@code TrackFragment} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ItemInfo extends FullBox {
    /**
     * Numerical representation of the {@code "iinf"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'i') << 8) | 'n') << 8) | 'f';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * All entries in this information box.
     */
    public final ItemInfoEntry[] entries;

    /**
     * A registry accepting only children of types that are legal for the container.
     * All other box types will be ignored with a warning sent to the listeners.
     */
    private static final BoxRegistry FILTER = new BoxRegistry() {
        @Override public Box create(final Reader reader, final int fourCC) throws IOException, DataStoreException {
            switch (fourCC) {
                case ItemInfoEntry.BOXTYPE: return new ItemInfoEntry(reader);
                default: reader.unexpectedChildType(BOXTYPE, fourCC); return null;
            }
        }
    };

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     * @throws DataStoreException if the stream contains inconsistent or unsupported data.
     */
    public ItemInfo(final Reader reader) throws IOException, DataStoreException {
        super(reader);
        final ChannelDataInput input = reader.input;
        final int count;
        switch (version()) {
            case 0: count = input.readUnsignedShort(); break;
            case 1: count = input.readInt(); break;
            default: throw new UnsupportedVersionException(BOXTYPE, version());
        }
        entries = new ItemInfoEntry[count];
        for (int i=0; i<count; i++) {
            entries[i] = (ItemInfoEntry) reader.readBox(FILTER);
        }
    }

    /**
     * Collects information about all items in this box.
     * It will be used by {@link ItemInfo} for more human-readable output.
     *
     * @param  context  the tree being formatted. Can be used for fetching contextual information.
     * @param  target   the node where to add properties.
     * @param  after    {@code false} for the first nodes, or {@code true} for the last nodes.
     */
    @Override
    protected void appendTreeNodes(final Tree context, final TreeTable.Node target, final boolean after) {
        super.appendTreeNodes(context, target, after);
        if (!after) {
            for (ItemInfoEntry entry : entries) {
                context.names.put(entry.itemID, entry);       // In case of conflict, keep the most recent item.
            }
        }
    }
}
