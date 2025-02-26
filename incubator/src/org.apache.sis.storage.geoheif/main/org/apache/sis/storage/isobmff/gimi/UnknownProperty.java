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
package org.apache.sis.storage.isobmff.gimi;

import java.util.UUID;
import java.io.IOException;
import org.apache.sis.storage.isobmff.Extension;
import org.apache.sis.storage.isobmff.Reader;


/**
 * Unknown property observed in GIMI test file.
 * For now, we keep this class only as a reminder for the pattern to use for box by UUID.
 * We will remove or rename this class in the future if we identify a real case of such box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class UnknownProperty extends Extension {    // Could also be `FullBox` if the parent type is `ItemFullProperty`.
    /**
     * The most significant bits of the <abbr>UUID</abbr> as a long integer.
     */
    public static final long UUID_HIGH_BITS = 0x4a66efa7_e541_526cL;

    /**
     * The <abbr>UUID</abbr> that identify this extension.
     *
     * @see #extendedType()
     */
    public static final UUID EXTENDED_TYPE = new UUID(UUID_HIGH_BITS, 0x9427_9e77617feb7dL);

    /**
     * Returns the identifier of this extension.
     * This value is fixed to {@link #EXTENDED_TYPE}.
     */
    @Override
    public final UUID extendedType() {
        return EXTENDED_TYPE;
    }

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     */
    public UnknownProperty(final Reader reader) throws IOException {
    }
}
