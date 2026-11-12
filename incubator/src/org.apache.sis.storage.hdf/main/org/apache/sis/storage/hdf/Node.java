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
package org.apache.sis.storage.hdf;

import java.util.Map;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;


/**
 * An object of an <abbr>HDF5</abbr> file: either a {@link Group} or a {@link Dataset}.
 * Nodes give a raw view of the file content, without interpretation of conventions such as
 * <abbr>CF</abbr>. The interpreted view is given by {@link HDF5Store#components()}.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Node extends Resource {
    /**
     * Returns the group containing this node.
     *
     * @return the parent group, or {@code null} for the root group.
     */
    Group getParent();

    /**
     * Returns the name of this node within its parent group, or the file name for the root group.
     *
     * @return the name of this node within its parent group, or the file name for the root group.
     */
    String getName();

    /**
     * Returns the position in the file of the object header of this node.
     * This is the value identifying the node in an {@link ObjectReference}.
     *
     * @return the position in the file of the object header of this node.
     *
     * @see #findNode(long)
     */
    long getAddress();

    /**
     * Returns the attributes of this node by name. Attribute values are {@link String},
     * {@link Number}, arrays of primitive types or strings, {@code Object[]} for compound values,
     * or {@link ObjectReference} for references to other objects of the file.
     *
     * @return the attributes of this node by name, never {@code null}.
     */
    Map<String,Object> getAttributes();

    /**
     * Searches this node and its descendants for the node having the given object header address.
     * This method is typically invoked on the root group for resolving an {@link ObjectReference}.
     *
     * @param  address  object header address of the searched node.
     * @return the node, or {@code null} if not found.
     * @throws DataStoreException if the children of a group could not be listed.
     *
     * @see ObjectReference#getAddress()
     */
    default Node findNode(final long address) throws DataStoreException {
        if (getAddress() == address) {
            return this;
        }
        if (this instanceof Aggregate agg) {
            for (Resource r : agg.components()) {
                if (r instanceof Node n) {
                    final Node candidate = n.findNode(address);
                    if (candidate != null) return candidate;
                }
            }
        }
        return null;
    }
}
