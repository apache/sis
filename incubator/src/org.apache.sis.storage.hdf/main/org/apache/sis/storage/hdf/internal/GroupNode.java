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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.Group;
import org.apache.sis.storage.hdf.Node;
import org.apache.sis.storage.hdf.ObjectReference;
import org.apache.sis.storage.hdf.base.DenseStorage;
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.base.ObjectHeader;
import org.apache.sis.storage.hdf.base.SymbolTableEntry;
import org.apache.sis.storage.hdf.base.SymbolTableNode;
import org.apache.sis.storage.hdf.btree.BTreeV1;
import org.apache.sis.storage.hdf.heap.LocalHeap;
import org.apache.sis.storage.hdf.io.Connector;
import org.apache.sis.storage.hdf.io.HDF5DataInput;
import org.apache.sis.storage.hdf.message.AttributeInfoMessage;
import org.apache.sis.storage.hdf.message.AttributeMessage;
import org.apache.sis.storage.hdf.message.LinkInfo;
import org.apache.sis.storage.hdf.message.LinkMessage;
import org.apache.sis.storage.hdf.message.Message;
import org.apache.sis.storage.hdf.message.SymbolTableMessage;
import org.opengis.util.GenericName;

/**
 * A group is an object header that contains a message that points to a local
 * heap (for storing the links to objects in the group) and to a B-tree
 * (which indexes the links).
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GroupNode extends AbstractResource implements Group {

    private final GroupNode parent;
    private final Connector connector;

    private final long address;
    private final String name;
    private final GenericName genericName;
    private final List<Node> components = new ArrayList<>();

    //parsed values
    private final Map<String,Object> attributes = new HashMap<>();
    /**
     * Soft links of this group, by link name, the value being the path of the
     * link target. Soft links are exposed as is, they are not resolved.
     */
    private final Map<String,String> softLinks = new LinkedHashMap<>();
    /**
     * External links of this group, by link name. Following one would mean
     * opening another file, which is not done.
     */
    private final Map<String,LinkMessage.External> externalLinks = new LinkedHashMap<>();

    /**
     * Create a group from the address of its object header.
     *
     * @param parent parent group, {@code null} for the root group
     * @param connector file connector, not {@code null}
     * @param objectHeaderAddress address of this group object header
     * @param name name of this group within its parent
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the object header or a child is invalid or unsupported.
     */
    public GroupNode(GroupNode parent, Connector connector, long objectHeaderAddress, String name) throws IOException, DataStoreException {
        this(parent, connector, objectHeaderAddress, name, Nodes.readHeader(connector, objectHeaderAddress));
    }

    /**
     * Create a group from an already decoded object header, avoiding a second read.
     *
     * @param parent parent group, {@code null} for the root group
     * @param connector file connector, not {@code null}
     * @param objectHeaderAddress address of this group object header
     * @param name name of this group within its parent
     * @param header decoded object header of this group
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a link or a child is invalid or unsupported.
     * @see Nodes#create(GroupNode, Connector, long, String)
     */
    GroupNode(GroupNode parent, Connector connector, long objectHeaderAddress, String name, ObjectHeader header) throws IOException, DataStoreException {
        super(null, false);
        this.parent = parent;
        this.name = name;
        this.connector = connector;
        this.genericName = Nodes.createName(this);
        this.address = objectHeaderAddress;

        try (final HDF5DataInput channel = connector.createChannel()) {
            /*
            A "new style" group stores its links either compactly, as Link messages
            in this header, or densely, in a fractal heap referenced by the Link
            Info message. Collect them first, the dense case needs the Link Info.
            */
            final List<LinkMessage> links = new ArrayList<>();
            LinkInfo linkInfo = null;
            AttributeInfoMessage attributeInfo = null;

            //extract properties
            for (Message message : header.getMessages()) {
                if (message instanceof AttributeMessage cdt) {
                    attributes.put(cdt.getName(), cdt.getValue());
                } else if (message instanceof AttributeInfoMessage cdt) {
                    attributeInfo = cdt;
                } else if (message instanceof SymbolTableMessage stm) {
                    buildComponents(channel, stm);
                } else if (message instanceof LinkMessage cdt) {
                    links.add(cdt);
                } else if (message instanceof LinkInfo cdt) {
                    linkInfo = cdt;
                } else {
                    /*
                    Unknown or merely informative messages are ignored : refusing to
                    open a file because of a single unrecognized optional message
                    would be worse than reading what we do understand.
                    */
                    Constants.LOGGER.log(Level.FINE, () -> "Ignored message "
                            + message.getClass().getSimpleName() + " in group " + name);
                }
            }

            if (linkInfo != null && channel.isDefinedOffset(linkInfo.getFractalHeapAddress())) {
                /*
                Dense link storage : the Link messages live in a fractal heap
                indexed by a version 2 B-tree rather than in this object header.
                */
                links.addAll(DenseStorage.readLinks(channel,
                        linkInfo.getFractalHeapAddress(), linkInfo.getV2BtreeNameIndexAddress()));
            }

            for (LinkMessage link : links) {
                addLink(link);
            }

            if (attributeInfo != null && channel.isDefinedOffset(attributeInfo.getFractalHeapAddress())) {
                DenseStorage.readAttributes(channel, attributeInfo.getFractalHeapAddress(),
                        attributeInfo.getAttributeNameV2btreeAddress(), attributes, name);
            }
        }
    }

    /**
     * Returns whether the given object header address is this group or one of its ancestors.
     * Hard links may point back at an enclosing group, which the HDF5 library
     * allows, so following them blindly would recurse forever.
     *
     * @param objectHeaderAddress object header address to test.
     * @return whether the given object header address is this group or one of its ancestors
     */
    private boolean isAncestor(long objectHeaderAddress) {
        for (GroupNode group = this; group != null; group = group.getParent()) {
            if (group.getAddress() == objectHeaderAddress) return true;
        }
        return false;
    }

    /**
     * Add the component or link described by the given Link message.
     *
     * @param link link to add, hard links become child nodes.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the linked object is invalid or unsupported.
     */
    private void addLink(LinkMessage link) throws IOException, DataStoreException {
        final String linkName = link.getName();
        final LinkMessage.LinkInformation info = link.getLinkInformation();
        if (info instanceof LinkMessage.Hard hard) {
            if (isAncestor(hard.objectHeaderAddress())) {
                Constants.LOGGER.log(Level.FINE, () -> "Skipped link " + linkName
                        + " of group " + name + ", it points back at an enclosing group.");
                return;
            }
            components.add(Nodes.create(this, connector, hard.objectHeaderAddress(), linkName));
        } else if (info instanceof LinkMessage.Soft soft) {
            softLinks.put(linkName, soft.value());
        } else if (info instanceof LinkMessage.External external) {
            externalLinks.put(linkName, external);
        } else {
            Constants.LOGGER.log(Level.FINE, () -> "Ignored user defined link " + linkName);
        }
    }

    /**
     * Returns the soft links of this group by link name, never {@code null}.
     * The value is the path of the target, which is not resolved.
     *
     * @return the soft links of this group by link name, never {@code null}
     */
    @Override
    public Map<String,String> getSoftLinks() {
        return Collections.unmodifiableMap(softLinks);
    }

    /**
     * Returns the external links of this group by link name, never {@code null}.
     * Targets live in another file and are not opened.
     *
     * @return the external links of this group by link name, never {@code null}
     */
    public Map<String,LinkMessage.External> getExternalLinks() {
        return Collections.unmodifiableMap(externalLinks);
    }

    @Override
    public GroupNode getParent() {
        return parent;
    }

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(genericName);
    }

    private void buildComponents(final HDF5DataInput channel, SymbolTableMessage stm) throws IOException, DataStoreException {
        channel.mark();
        channel.seek(stm.localHeapAddress);
        final LocalHeap localHeap = (LocalHeap) IOStructure.loadIdentifiedObject(channel);

        channel.seek(stm.v1BtreeAddress);
        final BTreeV1 btree = (BTreeV1) IOStructure.loadIdentifiedObject(channel);
        channel.reset();

        buildComponents(channel, localHeap, btree);
    }

    private void buildComponents(final HDF5DataInput channel, LocalHeap localHeap, BTreeV1 btree) throws IOException, DataStoreException {
        final BTreeV1.GroupNode node = (BTreeV1.GroupNode) btree.root;

        for (long adr : node.groupChildAddresses) {
            channel.seek(adr);
            IOStructure struct = IOStructure.loadIdentifiedObject(channel);

            if (btree.header.nodeLevel == 0) {
                //leaf group
                if (struct instanceof SymbolTableNode stn) {
                    buildComponents(channel, localHeap, stn);
                } else {
                    throw new IOException("Unexpected " + struct);
                }
            } else {
                //group in group
                if (struct instanceof BTreeV1 subtree) {
                    buildComponents(channel, localHeap, subtree);
                } else {
                    throw new IOException("Unexpected " + struct);
                }
            }
        }
    }

    private void buildComponents(final HDF5DataInput channel, LocalHeap localHeap, SymbolTableNode stn) throws IOException, DataStoreException {
        for (SymbolTableEntry entry : stn.entries) {
            final String componentName = entry.getName(channel, localHeap).orElse("");
            final int cacheType = entry.getCacheType();
            if (cacheType == 2) {
                /*
                Symbolic (soft) link. The target path is stored in the local heap
                of the group holding the link, not in the entry itself.
                */
                final String target = localHeap == null ? null
                        : localHeap.getString(channel, entry.offsetToLinkValue);
                softLinks.put(componentName, target);
            } else if (isAncestor(entry.getObjectHeaderAddress())) {
                Constants.LOGGER.log(Level.FINE, () -> "Skipped entry " + componentName
                        + " of group " + name + ", it points back at an enclosing group.");
            } else {
                /*
                Cache types 0 and 1 only say whether the group metadata happened to
                be cached in the scratch pad; type 0 also occurs for groups whose
                hard link count exceeds one. The object header itself is what tells
                a group from a dataset.
                */
                components.add(Nodes.create(this, connector, entry.getObjectHeaderAddress(), componentName));
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Find node for given path.
     *
     * @param path {@code '/'} separated path, relative to this group or absolute if starting with {@code '/'}.
     * @return the node, or {@code null} if a path component is not found or is not a group.
     */
    @Override
    public Node resolve(String path) {
        if (path.startsWith("/")) {
            GroupNode parent = this;
            while (parent.getParent() != null) {
                parent = parent.getParent();
            }
            return parent.resolve(path.substring(1));
        }
        final int split = path.indexOf('/');
        if (split == -1) {
            return getComponent(path);
        } else {
            final Node child = getComponent(path.substring(0, split));
            return (child instanceof GroupNode group) ? group.resolve(path.substring(split+1)) : null;
        }
    }

    /**
     * Returns the direct child with the given name, or {@code null} if none.
     *
     * @param name child name.
     * @return the direct child with the given name, or {@code null} if none
     */
    public Node getComponent(String name) {
        for (Node n : components()) {
            if (name.equals(n.getName())) {
                return n;
            }
        }
        return null;
    }

    @Override
    public List<Node> components() {
        return Collections.unmodifiableList(components);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String toString() {
        final String name = getName();
        final StringBuilder sb = new StringBuilder(name);
        sb.append("[HDF5-Group]");
        for (Map.Entry entry : getAttributes().entrySet()) {
            Object v = prettyPrint(this, entry.getValue());
            sb.append('\n').append("@").append(entry.getKey()).append(" = ").append(v);
        }
        return StringUtilities.toStringTree(sb.toString(), components());
    }

    /**
     * Returns a representation of the given attribute value for display purposes.
     * References are replaced by the identifier of the referenced node.
     *
     * @param base node holding the attribute, used for finding the root group.
     * @param v attribute value.
     * @return value to display.
     */
    static Object prettyPrint(Node base, Object v) {
        if (v instanceof ObjectReference ref) {
            //get the root node
            Node root = base;
            while (root != null && root.getParent() != null) {
                root = root.getParent();
            }

            try {
                Node candidate = root.findNode(ref.getAddress());
                if (candidate != null) {
                    v = candidate.getIdentifier().orElse(null);
                } else {
                    v = "Reference not found for address " + ref.getAddress();
                }
            } catch (DataStoreException ex) {
                v = ex.getMessage();
            }

        } else if (v instanceof CharSequence) {
            v = "\"" + v + "\"";
            v = ((String)v).replace('\n', ' ');
        } else if (v instanceof Collection c) {
            final List<Object> lst = new ArrayList<>();
            for (Object o : c) {
                lst.add(prettyPrint(base, o));
            }
            v = Arrays.toString(lst.toArray());
        } else if (v != null && v.getClass().isArray()) {
            int l = Array.getLength(v);
            final List<Object> lst = new ArrayList<>(l);
            for (int i = 0; i < l; i++) {
                lst.add(prettyPrint(base, Array.get(v, i)));
            }
            v = Arrays.toString(lst.toArray());
        }
        return v;
    }
}
