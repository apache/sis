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
package org.apache.sis.storage.isobmff;

import java.io.IOException;
import org.apache.sis.storage.DataStoreException;


/**
 * The root node of a <abbr>HEIF</abbr> file.
 * This is the starting point for reading boxes.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class Root extends ContainerBox {
    /**
     * Creates the root node and load all children.
     * Boxes of unknown types are ignored and skipped.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the reading failed for another reason.
     */
    public Root(final Reader reader) throws IOException, DataStoreException {
        super(reader, MainBoxRegistry.INSTANCE, false);
    }

    /**
     * Returns 0, since root node has no identifier.
     *
     * @return always 0.
     */
    @Override
    public int type() {
        return 0;
    }
}
