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
package org.apache.sis.storage.earthobservation;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.IOException;
import org.opengis.metadata.Metadata;
import org.opengis.util.FactoryException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;


/**
 * Parses Landsat metadata as {@linkplain DefaultMetadata ISO-19115 Metadata} object.
 * Landsat data are distributed as a collection of TIFF files, together with a single
 * text file like below:
 *
 * {@preformat text
 * GROUP = L1_METADATA_FILE
 *   GROUP = METADATA_FILE_INFO
 *     ORIGIN = "Image courtesy of the U.S. Geological Survey"
 *     REQUEST_ID = "0501403126384_00011"
 *     LANDSAT_SCENE_ID = "LC81230522014071LGN00"
 *     FILE_DATE = 2014-03-12T06:06:35Z
 *     STATION_ID = "LGN"
 *     PROCESSING_SOFTWARE_VERSION = "LPGS_2.3.0"
 *   END_GROUP = METADATA_FILE_INFO
 *   GROUP = PRODUCT_METADATA
 *     DATA_TYPE = "L1T"
 *     ELEVATION_SOURCE = "GLS2000"
 *     OUTPUT_FORMAT = "GEOTIFF"
 *     SPACECRAFT_ID = "LANDSAT_8"
 *     SENSOR_ID = "OLI_TIRS"
 *     etc...
 * }
 *
 * This class reads the content from the given input until the first occurrence of the {@code END} keyword.
 * Lines beginning with the {@code #} character (ignoring spaces) are treated as comment lines and ignored.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class LandsatStore extends DataStore {
    /**
     * The file name.
     */
    private final String name;

    /**
     * The reader, or {@code null} if closed.
     */
    private Reader source;

    /**
     * The object returned by {@link #getMetadata()}, created when first needed and cached.
     */
    private Metadata metadata;

    /**
     * Creates a new Landsat store from the given file, URL, stream or character reader.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  connector information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the Landsat file.
     */
    public LandsatStore(final StorageConnector connector) throws DataStoreException {
        super(connector);
        name = connector.getStorageName();
        source = connector.getStorageAs(Reader.class);
        connector.closeAllExcept(source);
        if (source == null) {
            throw new UnsupportedStorageException(errors().getString(Errors.Keys.IllegalInputTypeForReader_2,
                    "Landsat", Classes.getClass(connector.getStorage())));
        }
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object can contain information
     * such as the spatiotemporal extent of the dataset, contact information about the creator or distributor,
     * data quality, usage constraints and more.
     *
     * @return information about the dataset.
     * @throws DataStoreException if an error occurred while reading the data.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null && source != null) try {
            try (BufferedReader reader = (source instanceof BufferedReader) ? (BufferedReader) source : new LineNumberReader(source)) {
                source = null;      // Will be closed at the end of this try-catch block.
                final LandsatReader parser = new LandsatReader(name, listeners);
                parser.read(reader);
                metadata = parser.getMetadata();
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        } catch (FactoryException e) {
            throw new DataStoreReferencingException(e);
        }
        return metadata;
    }

    /**
     * Closes this Landsat store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing the Landsat file.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        metadata = null;
    }

    /**
     * Returns the error resources in the current locale.
     */
    private Errors errors() {
        // Must use "super" because LandsatStore construction may not be finished.
        return Errors.getResources(super.getLocale());
    }

    /**
     * Returns a string representation of this Landsat store for debugging purpose.
     * The content of the string returned by this method may change in any future SIS version.
     *
     * @return a string representation of this datastore for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name + ']';
    }
}
