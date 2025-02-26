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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.base.MetadataBuilder;


/**
 * Container for other boxes providing untimed metadata.
 * The full set of metadata is the union of the track metadata with the enclosing movie metadata.
 *
 * <h4>Container</h4>
 * The container can be the file segment, or a {@code Segment} box, {@link Movie} box,
 * {@link Track} box, {@code MovieFragment} box, or {@code TrackFragment} box.
 *
 * <h4>Content</h4>
 * The {@code Meta} should contains the following boxes:
 * <ul>
 *   <li>{@link HandlerReference} (mandatory)</li>
 *   <li>{@link PrimaryItem}      (optional)</li>
 *   <li>{@code DataInformation}  (optional)</li>
 *   <li>{@link ItemLocation}     (optional)</li>
 *   <li>{@code ItemProtection}   (optional)</li>
 *   <li>{@link ItemInfo}         (optional)</li>
 *   <li>{@code IPMPControl}      (optional)</li>
 *   <li>{@link ItemReference}    (optional)</li>
 *   <li>{@link ItemData}         (optional)</li>
 *   <li>Other boxes              (optional)</li>
 * </ul>
 *
 * <b>Note:</b> the {@code Meta} box is unusual in that it is a container box yet
 * extends {@link FullBox}, not {@link org.apache.sis.storage.isobmff.ContainerBox}.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class Meta extends FullBox {
    /**
     * Numerical representation of the {@code "meta"} box type.
     */
    public static final int BOXTYPE = ((((('m' << 8) | 'e') << 8) | 't') << 8) | 'a';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * All children boxes in this metadata box, excluding unrecognized box types.
     * All elements are non-null, but array index may not correspond to the index
     * in the file (because of the exclusion of null values).
     */
    public final Box[] children;

    /**
     * Creates a new box. This constructor reads the children immediately.
     *
     * @param  reader  the reader from which to read the fields.
     * @throws IOException if an error occurred while reading the fields.
     * @throws DataStoreException if the stream contains inconsistent or unsupported data.
     */
    public Meta(final Reader reader) throws IOException, DataStoreException {
        super(reader);
        requireVersionZero();
        children = reader.readChildren(null, false).toArray(Box[]::new);
    }

    /**
     * Converts node properties of all children to <abbr>ISO</abbr> 19115 metadata.
     *
     * @param  builder  the builder where to set metadata information.
     */
    @Override
    public void metadata(final MetadataBuilder builder) {
        for (final Box child : children) {
            child.metadata(builder);
        }
    }
}
