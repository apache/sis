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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * A group is an object internal to the file that allows arbitrary nesting of
 * objects within the file (including other groups). A group maps a set of link
 * names in the group to a set of relative file addresses of objects in the file.
 * Certain metadata for an object to which the group points can be cached in the
 * group’s symbol table entry in addition to being in the object’s header.
 * <p>
 * An HDF5 object name space can be stored hierarchically by partitioning the
 * name into components and storing each component as a link in a group. The
 * link for a non-ultimate component points to the group containing the next
 * component. The link for the last component points to the object being named.
 * <p>
 * One implementation of a group is a collection of symbol table nodes indexed
 * by a B-tree. Each symbol table node contains entries for one or more links.
 * If an attempt is made to add a link to an already full symbol table node
 * containing 2K entries, then the node is split and one node contains K symbols
 * and the other contains K+1 symbols.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_symboltable">III.B. Disk Format: Level 1B - Group Symbol Table Nodes</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SymbolTableNode extends IOStructure {

    /**
     * The ASCII character string “SNOD” is used to indicate the beginning of a
     * symbol table node. This gives file consistency checking utilities a
     * better chance of reconstructing a damaged file.
     */
    public static final byte[] SIGNATURE = "SNOD".getBytes(StandardCharsets.US_ASCII);

    /**
     * Although all symbol table nodes have the same length, most contain fewer
     * than the maximum possible number of link entries. This field indicates
     * how many entries contain valid data. The valid entries are packed at the
     * beginning of the symbol table node while the remaining entries contain
     * undefined values.
     */
    private int numberOfEntries;

    /**
     * Each link has an entry in the symbol table node. The format of the entry
     * is described below. There are 2K entries in each group node, where K is
     * the “Group Leaf Node K” value from the superblock.
     */
    public List<SymbolTableEntry> entries;

    /** Creates an empty node, filled by {@link #read(HDF5DataInput)}. */
    public SymbolTableNode() {
    }

    public void read(HDF5DataInput channel) throws IOException {

        channel.ensureSignature(SIGNATURE);
        /*
        The version number for the symbol table node. This document describes
        version 1. (There is no version ‘0’ of the symbol table node)
         */
        channel.ensureVersion(1);
        channel.skipFully(1);
        numberOfEntries = channel.readUnsignedShort();

        entries = new ArrayList<>(numberOfEntries);
        for (int i = 0; i < numberOfEntries;i++) {
            SymbolTableEntry entry  = new SymbolTableEntry();
            entry.read(channel);
            entries.add(entry);
        }
    }

    @Override
    public String toString() {
        return IOStructure.reflectionToString(this);
    }
}
