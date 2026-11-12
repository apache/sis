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
package org.apache.sis.storage.hdf.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Names;
import org.apache.sis.storage.hdf.Node;
import org.apache.sis.storage.hdf.base.ObjectHeader;
import org.apache.sis.storage.hdf.io.Connector;
import org.apache.sis.storage.hdf.io.HDF5DataInput;
import org.apache.sis.storage.hdf.message.DataspaceMessage;
import org.apache.sis.storage.hdf.message.DatatypeMessage;
import org.apache.sis.storage.hdf.message.Message;
import org.opengis.util.GenericName;


/**
 * Static methods for reading the object headers and creating the {@link GroupNode} and {@link DatasetNode} instances.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Nodes {
    /**
     * Do not allow instantiation of this class.
     */
    private Nodes() {
    }

    /**
     * Read the object header located at the given address.
     * <p>
     * The header version is deduced from its first byte : version 1 headers start
     * with the version number itself, version 2 headers start with the {@code OHDR}
     * signature whose first character is {@code 'O'} (79).
     *
     * @param channel channel to read from, its position is restored on return
     * @param objectHeaderAddress address of the object header
     * @return the decoded header, never {@code null}
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the header is invalid or unsupported.
     */
    public static ObjectHeader readHeader(HDF5DataInput channel, long objectHeaderAddress) throws IOException, DataStoreException {
        channel.mark();
        try {
            channel.seek(objectHeaderAddress);
            final int version = channel.readUnsignedByte();
            final ObjectHeader header = ObjectHeader.forVersion(version);
            channel.seek(objectHeaderAddress);
            header.read(channel);
            return header;
        } finally {
            channel.reset();
        }
    }

    /**
     * Read the object header located at the given address, using a channel of its own.
     *
     * @param connector file connector.
     * @param objectHeaderAddress address of the object header
     * @return the decoded header, never {@code null}
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the header is invalid or unsupported.
     * @see #readHeader(HDF5DataInput, long)
     */
    public static ObjectHeader readHeader(Connector connector, long objectHeaderAddress) throws IOException, DataStoreException {
        try (HDF5DataInput channel = connector.createChannel()) {
            return readHeader(channel, objectHeaderAddress);
        }
    }

    /**
     * Create the node whose object header is at the given address.
     * <p>
     * The kind of node is decided from the header content rather than from any
     * external hint : an object carrying both a datatype and a dataspace is a
     * dataset, anything else is a group. This is the only classification that
     * works for both the symbol table based groups of the 1.6 era and the link
     * based groups introduced in 1.8. In particular it avoids relying on the
     * symbol table entry cache type, whose value 0 only means "nothing cached"
     * and legitimately occurs for groups whose hard link count exceeds one.
     *
     * @param parent parent group, {@code null} for the root group
     * @param connector file connector, not {@code null}
     * @param objectHeaderAddress address of the node object header
     * @param name name of the node within its parent
     * @return a {@link GroupNode} or a {@link DatasetNode}, never {@code null}
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the node content is invalid or unsupported.
     */
    public static Node create(GroupNode parent, Connector connector, long objectHeaderAddress, String name)
            throws IOException, DataStoreException
    {
        final ObjectHeader header = readHeader(connector, objectHeaderAddress);

        boolean hasDatatype = false;
        boolean hasDataspace = false;
        for (Message message : header.getMessages()) {
            if (message instanceof DatatypeMessage) {
                hasDatatype = true;
            } else if (message instanceof DataspaceMessage) {
                hasDataspace = true;
            }
        }

        if (hasDatatype && hasDataspace) {
            return new DatasetNode(parent, connector, objectHeaderAddress, name, header);
        }
        if (hasDatatype) {
            /*
            A datatype without a dataspace is a committed (named) datatype. It is
            neither a group nor a dataset, expose it as an empty group so that the
            resource tree stays navigable.
            */
            Constants.LOGGER.log(Level.FINE, () -> "Object " + name
                    + " is a committed datatype, exposed as an empty group.");
        }
        return new GroupNode(parent, connector, objectHeaderAddress, name, header);
    }

    /**
     * Returns the name of the given node.
     * This is a local name for the root group, otherwise a scoped name made of the names
     * of the node and its ancestors, excluding the root group.
     *
     * @param node node to name.
     * @return the name of the given node
     */
    public static GenericName createName(Node node) {
        if (node.getParent() == null) {
            //root node
            return Names.createLocalName(null, null, node.getName());
        }

        final List<String> parts = new ArrayList<>();
        for (Node n = node; n != null && n.getParent() != null; n = n.getParent()) {
            parts.add(0, n.getName());
        }
        return Names.createGenericName(null, null, parts.toArray(String[]::new));
    }
}
