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
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * Identifies the primary item of a container.
 *
 * <h4>Container</h4>
 * The container can be a {@link Meta} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class PrimaryItem extends FullBox {
    /**
     * Numerical representation of the {@code "pitm"} box type.
     */
    public static final int BOXTYPE = ((((('p' << 8) | 'i') << 8) | 't') << 8) | 'm';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The identifier of the primary item. Shall be the identifier on an item in the enclosing {@link Meta} box.
     */
    @Interpretation(Type.IDENTIFIER)
    public final int itemID;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public PrimaryItem(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        final ChannelDataInput input = reader.input;
        switch (version()) {
            case 0: itemID = input.readUnsignedShort(); break;
            case 1: itemID = input.readInt(); break;
            default: throw new UnsupportedVersionException(BOXTYPE, version());
        }
    }
}
