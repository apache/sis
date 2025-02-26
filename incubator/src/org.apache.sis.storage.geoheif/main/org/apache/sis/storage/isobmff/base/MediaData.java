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
import org.apache.sis.storage.isobmff.Reader;


/**
 * This box contains the media data.
 * There may be any number of these boxes in the file, including zero.
 *
 * <h4>Container</h4>
 * The container can be the file.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public class MediaData extends ItemData {
    /**
     * Numerical representation of the {@code "mdat"} box type.
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final int BOXTYPE = ((((('m' << 8) | 'd') << 8) | 'a') << 8) | 't';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public int type() {
        return BOXTYPE;
    }

    /**
     * Creates a new box without reading data immediately.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while preparing the box.
     */
    public MediaData(final Reader reader) throws IOException {
        super(reader);
    }
}
