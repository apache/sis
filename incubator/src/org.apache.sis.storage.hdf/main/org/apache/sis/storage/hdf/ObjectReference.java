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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;


/**
 * A reference to another object of an <abbr>HDF5</abbr> file, or to a region of a dataset.
 * Such references are found in attribute or dataset values. The referenced node can be found
 * with {@link Node#findNode(long)} invoked on the root group with the {@linkplain #getAddress() address}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ObjectReference {
    /**
     * Address of the referenced object header, or -1 if unknown.
     */
    private final long address;

    /**
     * Serialized dataspace selection of a region reference, or {@code null} if none.
     */
    private final byte[] selection;

    /**
     * Whether this is a reference to a region of a dataset.
     */
    private final boolean region;

    /**
     * Creates a reference to an object.
     *
     * @param  address  address of the referenced object header, or -1 if unknown.
     */
    public ObjectReference(final long address) {
        this.address   = address;
        this.selection = null;
        this.region    = false;
    }

    /**
     * Creates a reference to a region of a dataset.
     *
     * @param  address    address of the object header of the dataset, or -1 if unknown.
     * @param  selection  serialized dataspace selection (not cloned), or {@code null} if unknown.
     */
    public ObjectReference(final long address, final byte[] selection) {
        this.address   = address;
        this.selection = selection;
        this.region    = true;
    }

    /**
     * Returns the address of the object header of the referenced object.
     *
     * @return address of the referenced object header, or -1 if unknown.
     *
     * @see Node#getAddress()
     * @see Node#findNode(long)
     */
    public long getAddress() {
        return address;
    }

    /**
     * Returns the serialized dataspace selection if this is a reference to a region of a dataset.
     * The selection encoding is defined by the <abbr>HDF5</abbr> specification and is not decoded.
     *
     * @return read-only view of the serialized selection, or empty if this is not a region reference
     *         or if the selection is unknown.
     */
    public Optional<ByteBuffer> getRegion() {
        return (selection != null) ? Optional.of(ByteBuffer.wrap(selection).asReadOnlyBuffer()) : Optional.empty();
    }

    /**
     * Compares this reference with the given object for equality.
     *
     * @param  other  the object to compare with this reference.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof ObjectReference that) && address == that.address
                && region == that.region && Arrays.equals(selection, that.selection);
    }

    /**
     * Returns a hash code value for this reference.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(address) + 31 * Arrays.hashCode(selection) + (region ? 7 : 0);
    }

    /**
     * Returns a string representation of this reference for debugging purposes.
     */
    @Override
    public String toString() {
        return (region ? "Reference to a region of " : "Reference to ") + address;
    }
}
