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
package org.apache.sis.storage.wkt;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.Reader;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.metadata.Metadata;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.CharSequences;


/**
 * A data store which creates data objects from a WKT definition.
 *
 * <h4>Design note</h4>
 * this class differs from {@link org.apache.sis.storage.base.PRJDataStore} in that
 * the file containing WKT definition is the main file, not an auxiliary file.
 *
 * @author  Martin Desruisseaux (Geomatys)
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
    private volatile Reader source;

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
                source = null;
                in.close();
            }
            /*
             * At this point we copied all the file content into a String. This string usually contain exactly
             * one WKT definitions, but this DataStore nevertheless allows an arbitrary number of consecutive
             * definitions.
             */
            final ParsePosition pos = new ParsePosition(0);
            final StoreFormat parser = new StoreFormat(dataLocale, timezone, library, listeners);
            do {
                final Object obj = parser.parse(wkt, pos);
                objects.add(obj);
                pos.setIndex(CharSequences.skipLeadingWhitespaces(wkt, pos.getIndex(), wkt.length()));
                parser.validate(null, Store.class, "getMetadata", obj);
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
            final var builder = new MetadataBuilder();
            InternationalString name = null;
            int count = 0;
            for (final Object object : objects) {
                if (object instanceof ReferenceSystem) {
                    final var rs = (ReferenceSystem) object;
                    builder.addReferenceSystem(rs);
                    name = IdentifiedObjects.getDisplayName(rs);
                    count++;
                    builder.addIdentifier(IdentifiedObjects.getIdentifier(rs, null), MetadataBuilder.Scope.RESOURCE);
                }
            }
            if (count == 1) {                   // Set the citation title only if non-ambiguous.
                builder.addTitle(name);
                mergeAuxiliaryMetadata(Store.class, builder);
            } else {
                mergeAuxiliaryMetadata(Store.class, builder);
                builder.addTitleOrIdentifier(getFilename(), MetadataBuilder.Scope.ALL);
            }
            metadata = builder.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Closes this data store and releases any underlying resources.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public void close() throws DataStoreException {
        try {
            listeners.close();              // Should never fail.
            final Reader s = source;
            if (s != null) s.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        } finally {
            synchronized (this) {
                objects.clear();
                metadata = null;
                source   = null;
            }
        }
    }
}
