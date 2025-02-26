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

import java.io.IOException;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;


/**
 * A box containing a version and a set of flags.
 * Full boxes with an unrecognized version should be ignored and skipped.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class FullBox extends Box {
    /**
     * Right shift to apply to the {@link #flags} bits in order to get the version numbers.
     *
     * @see #version()
     */
    protected static final int VERSION_BIT_SHIFT = 3 * Byte.SIZE;

    /**
     * Version and flags encoded in a single integer.
     * The first 8 bits contains the version, the rest are the flags.
     *
     * @see #version()
     */
    protected final int flags;

    /**
     * Creates a new box. This constructor reads the flags immediately.
     * Subclass constructors should read the <dfn>full box payload</dfn>,
     * defined as all bytes in the box following the flags.
     * It is okay to read the payload only partially.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     */
    protected FullBox(final Reader reader) throws IOException {
        flags = reader.input.readInt();
    }

    /**
     * Returns the version of this format of the box.
     * Typically, version 0 means that fields are 32-bits integers and version 1 means that fields are 64-bits integers.
     *
     * @return the version encoded in the {@linkplain #flags}.
     */
    public final int version() {
        return flags >>> VERSION_BIT_SHIFT;
    }

    /**
     * Verifies that the box version is zero.
     * This is a convenience check for boxes having only one version.
     *
     * @throws UnsupportedVersionException if the box version is not zero.
     */
    protected final void requireVersionZero() throws UnsupportedVersionException {
        final int version = version();
        if (version != 0) {
            throw new UnsupportedVersionException(type(), version);
        }
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
            final int version = version();
            final int options = flags & ((1 << VERSION_BIT_SHIFT) - 1);
            if (version != 0) {
                TreeTable.Node child = target.newChild();
                child.setValue(TableColumn.NAME, "version");
                child.setValue(TableColumn.VALUE, version);
                child.setValue(TableColumn.VALUE_AS_TEXT, String.valueOf(version));
            }
            if (options != 0) {
                TreeTable.Node child = target.newChild();
                child.setValue(TableColumn.NAME, "flags");
                child.setValue(TableColumn.VALUE, options);
                child.setValue(TableColumn.VALUE_AS_TEXT, Integer.toBinaryString(options));
            }
        }
    }
}
