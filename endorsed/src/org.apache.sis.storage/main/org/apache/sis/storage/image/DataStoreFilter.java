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

import java.util.Objects;
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
 *
 * @see org.apache.sis.io.stream.InternalOptionKey#PREFERRED_PROVIDERS
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
     * If there is another condition to apply, the other condition. Otherwise {@code null}.
     * This is used for allowing {@code (foo instanceof DataStoreFilter)} to continue to work
     * on the result of an {@code and} operation.
     */
    private final Predicate<? super DataStoreProvider> other;

    /**
     * Creates a new filter for the given data store name.
     *
     * @param  preferred  name of the data store to search, or Image I/O format name.
     * @param  writer     whether to search among writers instead of readers.
     */
    public DataStoreFilter(final String preferred, final boolean writer) {
        this.preferred = preferred;
        this.writer    = writer;
        this.other     = null;
    }

    /**
     * Creates a new filter which is the result of a {@code AND} operation between the specified filters.
     */
    private DataStoreFilter(final DataStoreFilter first, final Predicate<? super DataStoreProvider> other) {
        this.preferred = first.preferred;
        this.writer    = first.writer;
        this.other     = other;
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
        if (other == null || other.test(candidate)) {
            final String formatName = StoreUtilities.getFormatName(candidate);
            if (CharSequences.equalsFiltered(formatName, preferred, Characters.Filter.UNICODE_IDENTIFIER, true)) {
                return true;
            }
            if (WorldFileStoreProvider.NAME.equals(formatName)) {
                String[] formats = writer ? ImageIO.getWriterFormatNames() : ImageIO.getReaderFormatNames();
                return ArraysExt.containsIgnoreCase(formats, preferred);
            }
        }
        return false;
    }

    /**
     * Returns a filter which is the result of a AND operation between this filter and the other filter.
     * The combined filter is still an instance of {@code DataStoreFilter}, so {@code instanceof} checks
     * are still possible.
     */
    @Override
    public Predicate<DataStoreProvider> and(Predicate<? super DataStoreProvider> other) {
        return new DataStoreFilter(this, Objects.requireNonNull(other));
    }
}
