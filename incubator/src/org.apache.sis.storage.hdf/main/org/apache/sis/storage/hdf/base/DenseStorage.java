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
package org.apache.sis.storage.hdf.base;

import org.apache.sis.storage.hdf.internal.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.btree.BTreeV2;
import org.apache.sis.storage.hdf.btree.BTreeV2_Type5_IndexedGroupLinkName;
import org.apache.sis.storage.hdf.btree.BTreeV2_Type8_IndexedAttributesAttributeName;
import org.apache.sis.storage.hdf.heap.FractalHeap;
import org.apache.sis.storage.hdf.io.HDF5DataInput;
import org.apache.sis.storage.hdf.message.AttributeMessage;
import org.apache.sis.storage.hdf.message.LinkMessage;

/**
 * Reads the links and attributes a group or an object stores densely, that is in
 * a fractal heap indexed by a version 2 B-tree, rather than compactly as object
 * header messages.
 * <p>
 * A group or an object switches to dense storage once it exceeds the phase change
 * thresholds of its creation property list, eight links or attributes by default.
 * The B-tree records hold heap identifiers, and each heap object is the serialized
 * form of the very message that compact storage would have put in the object header.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DenseStorage {

    private DenseStorage() {}

    /**
     * Read every link of a group storing them densely.
     *
     * @param channel channel on the file, its position is restored on return
     * @param fractalHeapAddress address of the fractal heap holding the Link messages
     * @param btreeAddress address of the version 2 B-tree indexing the link names
     * @return the links, never {@code null}
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the heap, the B-tree or a message is invalid or unsupported.
     */
    public static List<LinkMessage> readLinks(HDF5DataInput channel,
            long fractalHeapAddress, long btreeAddress) throws IOException, DataStoreException {
        final List<LinkMessage> links = new ArrayList<>();
        channel.mark();
        try {
            final FractalHeap heap = readHeap(channel, fractalHeapAddress);
            final BTreeV2 btree = readBTree(channel, btreeAddress);
            for (byte[] record : btree.readRecords(channel)) {
                final BTreeV2_Type5_IndexedGroupLinkName key = new BTreeV2_Type5_IndexedGroupLinkName();
                try (HDF5DataInput sub = channel.subChannel(record)) {
                    key.read(sub);
                }
                final byte[] datas = heap.getObject(channel, key.getId());
                final LinkMessage link = new LinkMessage();
                try (HDF5DataInput sub = channel.subChannel(datas)) {
                    link.read(sub);
                }
                links.add(link);
            }
        } finally {
            channel.reset();
        }
        return links;
    }

    /**
     * Read every attribute of an object storing them densely, adding them to the
     * given map.
     *
     * @param channel channel on the file, its position is restored on return
     * @param fractalHeapAddress address of the fractal heap holding the Attribute messages
     * @param btreeAddress address of the version 2 B-tree indexing the attribute names
     * @param target map the attributes are added to
     * @param owner name of the object, used in log messages
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the heap, the B-tree or a message is invalid or unsupported.
     */
    public static void readAttributes(HDF5DataInput channel, long fractalHeapAddress,
            long btreeAddress, Map<String,Object> target, String owner) throws IOException, DataStoreException {
        channel.mark();
        try {
            final FractalHeap heap = readHeap(channel, fractalHeapAddress);
            final BTreeV2 btree = readBTree(channel, btreeAddress);
            for (byte[] record : btree.readRecords(channel)) {
                final BTreeV2_Type8_IndexedAttributesAttributeName key
                        = new BTreeV2_Type8_IndexedAttributesAttributeName();
                try (HDF5DataInput sub = channel.subChannel(record)) {
                    key.read(sub);
                }
                if (key.getMessageFlags() != 0) {
                    /*
                    The datatype or the dataspace of this attribute is shared with
                    another object, which requires resolving a shared message.
                    */
                    Constants.LOGGER.log(Level.FINE, () -> "Skipped a dense attribute of "
                            + owner + " whose datatype or dataspace is shared.");
                    continue;
                }
                final byte[] datas = heap.getObject(channel, key.getHeapId());
                final AttributeMessage attribute = new AttributeMessage();
                /*
                The sub channel delegates global heap access to this channel, which
                a variable length attribute value needs.
                */
                try (HDF5DataInput sub = channel.subChannel(datas)) {
                    attribute.read(sub);
                }
                target.put(attribute.getName(), attribute.getValue());
            }
        } finally {
            channel.reset();
        }
    }

    private static FractalHeap readHeap(HDF5DataInput channel, long address) throws IOException, DataStoreException {
        channel.seek(address);
        final FractalHeap heap = new FractalHeap();
        heap.read(channel);
        return heap;
    }

    private static BTreeV2 readBTree(HDF5DataInput channel, long address) throws IOException, DataStoreException {
        channel.seek(address);
        final BTreeV2 btree = new BTreeV2();
        btree.read(channel);
        return btree;
    }
}
