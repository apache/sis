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
package org.apache.sis.storage.isobmff.base;

import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.ContainerBox;


/**
 * Container for user information about the containing box (presentation or track) and its data.
 *
 * <h4>Container</h4>
 * The container can be a {@link Movie} box, a {@link Track} box, a {@code MovieFragment} box
 * or a {@code TrackFragment} box.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public final class UserData extends ContainerBox {
    /**
     * Numerical representation of the {@code "udta"} box type.
     */
    public static final int BOXTYPE = ((((('u' << 8) | 'd') << 8) | 't') << 8) | 'a';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreException if the stream contains inconsistent or unsupported data.
     */
    public UserData(final Reader reader) throws IOException, DataStoreException {
        super(reader, null, false);
    }
}
