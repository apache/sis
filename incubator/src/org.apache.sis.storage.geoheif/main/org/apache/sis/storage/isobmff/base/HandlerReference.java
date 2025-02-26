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
 * Media type of the track. For example, a video would be stored in a video track,
 * identified by being handled by a video handler.
 *
 * <h4>Container</h4>
 * The container can be a {@code Media} box or a {@link Meta}.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class HandlerReference extends FullBox {
    /**
     * Numerical representation of the {@code "hdlr"} box type.
     */
    public static final int BOXTYPE = ((((('h' << 8) | 'd') << 8) | 'l') << 8) | 'r';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The format of the {@code Meta} box content.
     * The value {@code "null"} indicates that this box is merely used to hold resources.
     */
    @Interpretation(Type.FOURCC)
    public final int handlerType;

    /**
     * Human-readable name for the track type, or {@code null} if none.
     * This is for debugging and inspection purposes.
     */
    public final String name;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public HandlerReference(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        handlerType = (int) input.readLong();       // The high bits are currently reserved and not used.
        input.skipNBytes(3 * Integer.BYTES);
        name = reader.readNullTerminatedString(false);
    }
}
