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

import java.io.IOException;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.DataStoreException;


/**
 * A world file store exposing in the public API the fact that it is an aggregate.
 * This class is used for image formats that may store many images per file.
 * Examples: TIFF and GIF image formats.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MultiImageStore extends WorldFileStore implements Aggregate {
    /**
     * Creates a new store from the given file, URL or stream.
     *
     * @param  format  information about the storage (URL, stream, <i>etc</i>) and the reader/writer to use.
     * @throws DataStoreException if an error occurred while opening the stream.
     * @throws IOException if an error occurred while creating the image reader instance.
     */
    MultiImageStore(final FormatFinder format) throws DataStoreException, IOException {
        super(format);
    }

    /**
     * The writable variant of {@link MultiImageStore}.
     */
    static final class Writable extends WritableStore implements WritableAggregate {
        /**
         * Creates a new store from the given file, URL or stream.
         *
         * @param  format  information about the storage (URL, stream, <i>etc</i>) and the reader/writer to use.
         * @throws DataStoreException if an error occurred while opening the stream.
         * @throws IOException if an error occurred while creating the image reader instance.
         */
        Writable(final FormatFinder format) throws DataStoreException, IOException {
            super(format);
        }
    }
}
