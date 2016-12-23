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
package org.apache.sis.internal.gpx;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.internal.xml.StaxDataStore;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;
import org.opengis.metadata.Metadata;

// Branch-dependent imports
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.opengis.feature.Feature;


/**
 * A data store backed by GPX files.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class GPXStore extends StaxDataStore {
    /**
     * The "1.0" version.
     */
    static final Version V1_0 = new Version("1.0");

    /**
     * The "1.1" version.
     */
    static final Version V1_1 = new Version("1.1");

    /**
     * The file encoding. Actually used only by the writer; ignored by the reader.
     */
    final Charset encoding;

    /**
     * The metadata, or {@code null} if not yet parsed.
     */
    private transient Metadata metadata;

    /**
     * Iterator over the features.
     */
    private GPXReader reader;

    /**
     * Creates a new GPX store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GPX file.
     */
    public GPXStore(final StorageConnector connector) throws DataStoreException {
        super(connector);
        ArgumentChecks.ensureNonNull("connector", connector);
        final Charset encoding = connector.getOption(OptionKey.ENCODING);
        this.encoding = (encoding != null) ? encoding : StandardCharsets.UTF_8;
    }

    /**
     * Returns the short name (abbreviation) of the format being read or written.
     *
     * @return {@code "GPX"}.
     */
    @Override
    public String getFormatName() {
        return "GPX";
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return null;    // TODO
    }

    /**
     * Returns the stream of features.
     *
     * @return a stream over all features in the CSV file.
     *
     * @todo Needs to reset the position when doing another pass on the features.
     */
    @Override
    public Stream<Feature> getFeatures() {
        return StreamSupport.stream(reader, false);
    }

    final GPXReader reader() throws DataStoreException, XMLStreamException {
        return reader = new GPXReader(this);
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        final GPXReader r = reader;
        reader = null;
        if (r != null) try {
            r.close();
        } catch (XMLStreamException e) {
            final DataStoreException ds = new DataStoreException(e);
            try {
                super.close();
            } catch (DataStoreException s) {
                ds.addSuppressed(s.getCause());
            }
            throw ds;
        }
        super.close();
    }
}
