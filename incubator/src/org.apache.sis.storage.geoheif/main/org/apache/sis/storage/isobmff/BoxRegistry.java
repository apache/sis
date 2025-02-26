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

import java.util.UUID;
import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;


/**
 * A factory of <abbr>ISO</abbr> <abbr>BMFF</abbr> boxes for given types or identifiers.
 * Users can register their own factory as a service to be used with {@link java.util.ServiceLoader}.
 *
 * @todo This mechanism makes possible to create custom boxes, but we do not yet have a mechanism for using them.
 *       {@link Reader} will not know what to do with custom boxes.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class BoxRegistry {
    /**
     * Creates a new registry.
     */
    protected BoxRegistry() {
    }

    /**
     * Creates a new box for the given box type.
     * Unknown types should be ignored.
     *
     * <p>The default implementation logs a warning and returns {@code null}.
     * Subclasses should override this method or {@link #create(Reader, UUID)}.</p>
     *
     * @param  reader  the reader from which to read bytes.
     * @param  fourCC  four-character code identifying the box type.
     * @return box, or {@code null} if the given type is not recognized by this registry.
     * @throws IOException if an error occurred while reading the box content.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     * @throws ArithmeticException if an integer overflow occurred for another reason.
     * @throws ArrayStoreException if a child box is not of the type expected by its container box.
     * @throws ArrayIndexOutOfBoundsException if a box does not have the expected number of elements.
     * @throws UnsupportedVersionException if the box declare an unsupported version number.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the box creation failed for another reason.
     *
     * @see Box#formatFourCC(int)
     */
    public Box create(Reader reader, int fourCC) throws IOException, DataStoreException {
        reader.unknownBoxType(fourCC);
        return null;
    }

    /**
     * Creates a new box for the given extension identifier.
     * This method is invoked when the box type is {@link Extension#BOXTYPE}.
     * Unknown identifiers should be ignored.
     *
     * <p>The default implementation logs a warning and returns {@code null}.
     * Subclasses should override this method or {@link #create(Reader, int)}.</p>
     *
     * @param  reader     the reader from which to read bytes.
     * @param  extension  identifier of user-defined box.
     * @return box, or {@code null} if the given identifier is not recognized by this registry.
     * @throws IOException if an error occurred while reading the box content.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     * @throws ArithmeticException if an integer overflow occurred for another reason.
     * @throws ArrayStoreException if a child box is not of the type expected by its container box.
     * @throws ArrayIndexOutOfBoundsException if a box does not have the expected number of elements.
     * @throws UnsupportedVersionException if the box declare an unsupported version number.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the box creation failed for another reason.
     */
    public Box create(Reader reader, UUID extension) throws IOException, DataStoreException {
        reader.unknownBoxType(extension);
        return null;
    }
}
