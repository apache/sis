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
package org.apache.sis.internal.storage.wkt;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.io.Reader;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.CharSequences;


/**
 * A data store which creates data objects from a WKT definition.
 *
 * <div class="note"><b>Note:</b>
 * this class differs from {@link org.apache.sis.internal.storage.PRJDataStore} in that
 * the file containing WKT definition is the main file, not an auxiliary file.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.7
 * @module
 */
final class Store extends URIDataStore {
    /**
     * Arbitrary size limit. Files that big are likely to be something else than WKT,
     * so this limit allows earlier error reporting than loading huge amount of data
     * before to detect that those data are not what we taught they are.
     */
    private static final int SIZE_LIMIT = 1000000;

    /**
     * The reader, set by the constructor and cleared when no longer needed.
     */
    private Reader source;

    /**
     * The locale for {@link org.opengis.util.InternationalString} localization
     * or {@code null} for {@link Locale#ROOT} (usually English).
     * This locale is <strong>not</strong> used for parsing numbers or dates.
     */
    private final Locale locale;

    /**
     * Timezone for dates, or {@code null} for UTC.
     */
    private final TimeZone timezone;

    /**
     * The geometry library, or {@code null} for the default.
     */
    private final GeometryLibrary library;

    /**
     * The parsed objects, filled only when first needed.
     * May still be empty if the parsing failed.
     */
    private final List<Object> objects;

    /**
     * The metadata object, created when first needed.
     */
    private Metadata metadata;

    /**
     * Creates a new WKT store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    public Store(final StoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        objects  = new ArrayList<>();
        locale   = connector.getOption(OptionKey.LOCALE);       // For `InternationalString`, not for numbers.
        timezone = connector.getOption(OptionKey.TIMEZONE);
        library  = connector.getOption(OptionKey.GEOMETRY_LIBRARY);
        source   = connector.commit(Reader.class, StoreProvider.NAME);
        listeners.useReadOnlyEvents();
    }

    /**
     * Parses the objects, if not already done. Note that {@link #objects} may still be empty
     * if an exception has been thrown at this invocation time or in previous invocation.
     *
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    private void parse() throws DataStoreException {
        final Reader in = source;
        if (in != null) try {
            source = null;                                                  // Cleared first in case of error.
            final String wkt;
            try {
                char[] buffer = new char[FirstKeywordPeek.READ_AHEAD_LIMIT];
                int length = 0;
                int n;
                while ((n = in.read(buffer, length, buffer.length - length)) >= 0) {
                    if ((length += n) >= buffer.length) {
                        if (n >= SIZE_LIMIT) {
                            throw new DataStoreContentException(Resources.format(
                                    Resources.Keys.ExcessiveStringSize_3, getDisplayName(), SIZE_LIMIT, n));
                        }
                        buffer = Arrays.copyOf(buffer, n << 1);
                    }
                }
                wkt = String.valueOf(buffer, 0, length);
            } finally {
                in.close();
            }
            /*
             * At this point we copied all the file content into a String. This string usually contain exactly
             * one WKT definitions, but this DataStore nevertheless allows an arbitrary number of consecutive
             * definitions.
             */
            final ParsePosition pos = new ParsePosition(0);
            final StoreFormat parser = new StoreFormat(locale, timezone, library, listeners);
            do {
                final Object obj = parser.parse(wkt, pos);
                objects.add(obj);
                pos.setIndex(CharSequences.skipLeadingWhitespaces(wkt, pos.getIndex(), wkt.length()));
                parser.validate(obj);
            } while (pos.getIndex() < wkt.length());
        } catch (ParseException e) {
            throw new DataStoreContentException(getLocale(), StoreProvider.NAME, getDisplayName(), in).initCause(e);
        } catch (IOException e) {
            throw new DataStoreException(getLocale(), StoreProvider.NAME, getDisplayName(), in).initCause(e);
        }
    }

    /**
     * Returns the metadata associated to the parsed objects, or {@code null} if none.
     * The current implementation retains only instances of {@link ReferenceSystem}
     * and ignore other objects. The identification information {@code Citation} is
     * set to the CRS name and identifier, unless there is ambiguity.
     *
     * @return the metadata associated to the parsed object, or {@code null} if none.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            parse();
            final MetadataBuilder builder = new MetadataBuilder();
            String name = null;
            int count = 0;
            for (final Object object : objects) {
                if (object instanceof ReferenceSystem) {
                    final ReferenceSystem rs = (ReferenceSystem) object;
                    builder.addReferenceSystem(rs);
                    name = IdentifiedObjects.getDisplayName(rs, getLocale());
                    count++;
                    builder.addIdentifier(IdentifiedObjects.getIdentifier(rs, null), MetadataBuilder.Scope.RESOURCE);
                }
            }
            if (count == 1) {                   // Set the citation title only if non-ambiguous.
                builder.addTitle(name);
            } else {
                addTitleOrIdentifier(builder);
            }
            metadata = builder.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        listeners.close();              // Should never fail.
        final Reader s = source;
        source = null;                  // Cleared first in case of failure.
        objects.clear();
        if (s != null) try {
            s.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }
}
