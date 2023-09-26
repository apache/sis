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
package org.apache.sis.storage.image;

import java.util.function.Predicate;
import javax.imageio.ImageIO;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.base.StoreUtilities;


/**
 * A filter for data store providers with special handling for world files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see org.apache.sis.io.stream.InternalOptionKey#PREFERRED_PROVIDERS
 *
 * @since 1.4
 */
public final class DataStoreFilter implements Predicate<DataStoreProvider> {
    /**
     * Short name of the data store to search.
     * May also be an Image I/O format name.
     *
     * @see DataStoreProvider#getShortName()
     */
    final String preferred;

    /**
     * Whether to search among writers instead of readers.
     */
    private final boolean writer;

    /**
     * Creates a new filter for the given data store name.
     *
     * @param  preferred  name of the data store to search, or Image I/O format name.
     * @param  writer     whether to search among writers intead of readers.
     */
    public DataStoreFilter(final String preferred, final boolean writer) {
        this.preferred = preferred;
        this.writer = writer;
    }

    /**
     * Returns {@code true} if the specified store has the name that this filter is looking for.
     * Name comparison is case-insensitive and ignores characters that are not part of Unicode
     * identifier (e.g. white spaces).
     *
     * @param  candidate  the provider to test.
     * @return whether the given provider has the desired name.
     */
    @Override
    public boolean test(final DataStoreProvider candidate) {
        final String formatName = StoreUtilities.getFormatName(candidate);
        if (CharSequences.equalsFiltered(formatName, preferred, Characters.Filter.UNICODE_IDENTIFIER, true)) {
            return true;
        }
        if (WorldFileStoreProvider.NAME.equals(formatName)) {
            String[] formats = writer ? ImageIO.getWriterFormatNames() : ImageIO.getReaderFormatNames();
            return ArraysExt.containsIgnoreCase(formats, preferred);
        }
        return false;
    }
}
