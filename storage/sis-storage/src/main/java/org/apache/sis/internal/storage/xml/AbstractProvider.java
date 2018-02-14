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
package org.apache.sis.internal.storage.xml;

import java.util.Map;
import java.util.HashMap;
import java.io.Reader;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.storage.DocumentedStoreProvider;


/**
 * Base class for providers of {@link DataStore} implementations for XML files.
 * This base class does not assume that the data store will use any particular framework
 * (JAXB, StAX, <i>etc</i>).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class AbstractProvider extends DocumentedStoreProvider {
    /**
     * The {@value} MIME type, used only if {@link #probeContent(StorageConnector)} can not determine
     * a more accurate type.
     */
    public static final String MIME_TYPE = "application/xml";

    /**
     * The read-ahead limit when reading the XML document from a {@link Reader}.
     */
    private static final int READ_AHEAD_LIMIT = 2048;

    /**
     * The expected XML header. According XML specification, this declaration is required to appear
     * at the document beginning (no space allowed before the declaration).
     */
    private static final byte[] HEADER = {'<','?','x','m','l',' '};

    /**
     * The mapping from XML namespace to MIME type. This map shall be populated by subclasses
     * at construction time, then never modified anymore since we do not synchronize it.
     *
     * <div class="note"><b>Example</b>
     * public MyDataStore() {
     *     types.put("http://www.opengis.net/gml/3.2",        "application/gml+xml");
     *     types.put("http://www.isotc211.org/2005/gmd",      "application/vnd.iso.19139+xml");
     *     types.put("http://www.opengis.net/cat/csw/2.0.2",  "application/vnd.ogc.csw_xml");
     * }</div>
     */
    protected final Map<String,String> types;

    /**
     * Creates a new provider. Subclasses shall populate the {@link #types} map with a mapping
     * from their namespace to the MIME type to declare.
     *
     * @param  name  the primary key to use for searching in the {@code MD_Format} table, or {@code null} if none.
     * @param  initialCapacity  initial capacity of the hash map to create.
     */
    protected AbstractProvider(final String name, final int initialCapacity) {
        super(name);
        types = new HashMap<>(initialCapacity);
        suffix.add("xml");
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by the data store.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the storage
     * header.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as a XML file.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        /*
         * Usual case. This includes InputStream, DataInput, File, Path, URL, URI.
         */
        final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
        if (buffer != null) {
            if (buffer.remaining() < HEADER.length) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            // Quick check for "<?xml " header.
            for (int i=0; i<HEADER.length; i++) {
                if (buffer.get(i) != HEADER[i]) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
            // Now check for a more accurate MIME type.
            buffer.position(HEADER.length);
            final ProbeResult result = new MimeTypeDetector(types) {
                @Override int read() {
                    if (buffer.hasRemaining()) {
                        return buffer.get();
                    }
                    insufficientBytes = (buffer.limit() != buffer.capacity());
                    return -1;
                }
            }.probeContent();
            buffer.position(0);
            return result;
        }
        /*
         * We should enter in this block only if the user gave us explicitely a Reader.
         * A common case is a StringReader wrapping a String object.
         */
        final Reader reader = connector.getStorageAs(Reader.class);
        if (reader != null) try {
            // Quick check for "<?xml " header.
            reader.mark(HEADER.length + READ_AHEAD_LIMIT);
            for (int i=0; i<HEADER.length; i++) {
                if (reader.read() != HEADER[i]) {
                    reader.reset();
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
            // Now check for a more accurate MIME type.
            final ProbeResult result = new MimeTypeDetector(types) {
                private int remaining = READ_AHEAD_LIMIT;
                @Override int read() throws IOException {
                    return (--remaining >= 0) ? IOUtilities.readCodePoint(reader) : -1;
                }
            }.probeContent();
            reader.reset();
            return result;
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }
}
