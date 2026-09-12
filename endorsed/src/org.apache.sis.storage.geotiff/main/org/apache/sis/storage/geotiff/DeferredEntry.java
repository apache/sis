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
package org.apache.sis.storage.geotiff;

import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.geotiff.base.Tags;
import org.apache.sis.storage.geotiff.reader.Type;
import org.apache.sis.util.resources.Errors;


/**
 * Offset to a TIFF tag entry that has not yet been read.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DeferredEntry implements Comparable<DeferredEntry> {
    /**
     * Where to add the entry value after it has been read.
     */
    final ImageFileDirectory owner;

    /**
     * The GeoTIFF tag to decode, as an <strong>unsigned</strong> short.
     */
    final short tag;

    /**
     * The GeoTIFF type of the value to read.
     */
    private final Type type;

    /**
     * The number of values to read.
     * This value come from a 32-bits unsigned integer. Therefore, it should never be negative.
     */
    private final long count;

    /**
     * Offset from beginning of TIFF file where the values are stored.
     */
    final long offset;

    /**
     * Creates a new deferred entry.
     */
    DeferredEntry(final ImageFileDirectory owner, final short tag, final Type type, final long count, final long offset) {
        this.owner  = owner;
        this.tag    = tag;
        this.type   = type;
        this.count  = count;
        this.offset = offset;
    }

    /**
     * Returns +1 if this entry is located after the given entry in the TIFF file,
     * or -1 if it is located before. A valid TIFF file should not have two entries
     * at the same location, but it is okay for Apache SIS implementation if such
     * case nevertheless happen.
     */
    @Override
    public int compareTo(final DeferredEntry other) {
        return Long.signum(offset - other.offset);
    }

    /**
     * Ensures that the number of elements to read is not too large.
     *
     * @param remaining  number of bytes remaining in the stream to read.
     */
    final void ensureReasonableCount(final long remaining) throws DataStoreContentException {
        if (count * type.size > remaining) {
            throw new DataStoreContentException(owner.reader.errors().getString(Errors.Keys.ExcessiveListSize_2, Tags.name(tag), count));
        }
    }

    /**
     * Adds the value read from the current position in the given stream for this entry.
     *
     * @return {@code null} on success, or the unrecognized value otherwise.
     * @throws Exception if an error occurred while reading the entry.
     */
    final Object addDeferredEntry() throws Exception {
        return owner.addEntry(tag, type, count);
    }
}
