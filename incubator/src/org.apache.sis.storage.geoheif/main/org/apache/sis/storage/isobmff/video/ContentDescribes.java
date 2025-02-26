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
package org.apache.sis.storage.isobmff.video;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.base.SingleItemTypeReference;


/**
 * Place holder for future work.
 *
 * @todo Not yet implemented.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ContentDescribes extends SingleItemTypeReference {
    /**
     * Numerical representation of the {@code "cdsc"} box type.
     */
    public static final int BOXTYPE = ((((('c' << 8) | 'd') << 8) | 's') << 8) | 'c';

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
     */
    public ContentDescribes(final Reader reader) throws IOException {
        super(reader, false);
    }
}
