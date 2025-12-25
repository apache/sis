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
import org.apache.sis.util.Workaround;
import org.apache.sis.storage.isobmff.Reader;


/**
 * Same as {@code MediaData}, but with the addition of an identifier.
 *
 * <h4>Container</h4>
 * The container can be the file.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class IdentifiedMediaData extends MediaData {
    /**
     * Numerical representation of the {@code "imda"} box type.
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final int BOXTYPE = ((((('i' << 8) | 'm') << 8) | 'd') << 8) | 'a';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * An identifier used in setting up data references to the contained media data.
     * Shall be unique within all {@code IdentifiedMediaData} boxes of the file.
     */
    @Interpretation(Type.UNSIGNED)
    public final int identifier;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     */
    public IdentifiedMediaData(final Reader reader) throws IOException {
        this(reader, reader.input.readInt());
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     * The identifier needs to be read before the call to `super(reader)`.
     */
    @Workaround(library="JDK", version="7", fixed="25")
    private IdentifiedMediaData(final Reader reader, final int id) throws IOException {
        super(reader);
        identifier = id;
    }
}
