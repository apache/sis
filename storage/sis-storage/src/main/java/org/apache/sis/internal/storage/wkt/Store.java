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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.Reader;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.referencing.CRS;


/**
 * A data store which creates data objects from a WKT definition.
 * This {@code DataStore} implementation is basically a facade for the {@link CRS#fromWKT(String)} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
final class Store extends DataStore {
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
        objects = new ArrayList<>();
        source  = connector.getStorageAs(Reader.class);
        connector.closeAllExcept(source);
        if (source == null) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, super.getDisplayName()));
        }
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
            final ParsePosition pos = new ParsePosition(0);
            final WKTFormat parser = new WKTFormat(null, null);
            do {
                objects.add(parser.parse(wkt, pos));
                pos.setIndex(CharSequences.skipLeadingWhitespaces(wkt, pos.getIndex(), wkt.length()));
                final Warnings warnings = parser.getWarnings();
                if (warnings != null) {
                    final LogRecord record = new LogRecord(Level.WARNING, warnings.toString());
                    record.setSourceClassName(Store.class.getName());
                    record.setSourceMethodName("getMetadata");          // Public facade for this method.
                    listeners.warning(record);
                }
            } while (pos.getIndex() < wkt.length());
        } catch (ParseException e) {
            throw new DataStoreContentException(getLocale(), "WKT", getDisplayName(), in).initCause(e);
        } catch (IOException e) {
            throw new DataStoreException(getLocale(), "WKT", getDisplayName(), in).initCause(e);
        }
    }

    /**
     * Returns the metadata associated to the parsed objects, or {@code null} if none.
     * The current implementation retains only instances of {@link ReferenceSystem}
     * and ignore other cases.
     *
     * @return the metadata associated to the parsed object, or {@code null} if none.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            parse();
            DefaultMetadata md = null;
            for (final Object object : objects) {
                if (object instanceof ReferenceSystem) {
                    if (md == null) md = new DefaultMetadata();
                    md.getReferenceSystemInfo().add((ReferenceSystem) object);
                }
            }
            metadata = md;
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
