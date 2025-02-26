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
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;


/**
 * Linking of one item to others via typed references.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ItemReference extends FullBox {
    /**
     * Numerical representation of the {@code "iref"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'r') << 8) | 'e') << 8) | 'f';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * All the references. All elements are non-null, but the array index may not
     * correspond to the index in the file because of the exclusion of null values.
     */
    public final SingleItemTypeReference[] references;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     * @throws ArrayStoreException if an item is not an instance of {@link SingleItemTypeReference}.
     * @throws DataStoreException if another logical error occurred.
     */
    public ItemReference(final Reader reader) throws IOException, DataStoreException {
        super(reader);
        requireVersionZero();
        references = reader.readChildren(null, false).toArray(SingleItemTypeReference[]::new);
    }

    /**
     * Converts node properties of all children to <abbr>ISO</abbr> 19115 metadata.
     *
     * @param  builder  the builder where to set metadata information.
     */
    @Override
    public void metadata(final MetadataBuilder builder) {
        for (final SingleItemTypeReference child : references) {
            child.metadata(builder);
        }
    }
}
