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
 * distributed under the License is distributed on an "AS IS" BASIS,z
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.isobmff.mpeg;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * From ISO/IEC 23001-17:2024 amendment 1.
 *
 * @todo Verify box structure and document.
 * The specification was not yet published at the time of writing this class.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TAITimeStamp extends FullBox {
    /**
     * Numerical representation of the {@code "itai"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 't') << 8) | 'a') << 8) | 'i';

    @Interpretation(Type.UNSIGNED)
    public final long TAITimestamp;

    public final boolean synchronizationState;

    public final boolean timestampGenerationFailure;

    public final boolean timestampIsModified;

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
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public TAITimeStamp(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        TAITimestamp = input.readLong();
        final byte f = input.readByte();
        synchronizationState       = (f & 0b10000000) != 0;
        timestampGenerationFailure = (f & 0x01000000) != 0;
        timestampIsModified        = (f & 0x00100000) != 0;
    }
}
