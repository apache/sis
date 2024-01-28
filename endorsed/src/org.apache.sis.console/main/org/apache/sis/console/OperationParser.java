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
package org.apache.sis.console;

import java.util.Optional;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.PRJDataStore;
import org.opengis.referencing.operation.CoordinateOperation;


/**
 * Reads a coordinate operation in GML or WKT format.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OperationParser extends PRJDataStore {
    /**
     * Creates a new parser for the given path or URL.
     *
     * @param  storage  path or URL (should not be a character string).
     */
    OperationParser(final Object storage) throws DataStoreException {
        super(null, new StorageConnector(storage));
    }

    /**
     * Access to the protected method from {@code PRJDataStore}.
     *
     * @return the coordinate operation, or empty if the file does not exist.
     * @throws DataStoreException if an error occurred while reading the file.
     */
    final Optional<CoordinateOperation> read() throws DataStoreException {
        return readWKT(OperationParser.class, "read", CoordinateOperation.class, null);
    }

    /**
     * Not used.
     */
    @Override
    public Metadata getMetadata() {
        return null;
    }

    /**
     * Nothing to close.
     */
    @Override
    public void close() {
    }
}
