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
 * Compression configuration box.
 *
 * <p><b>Source:</b> ISO/IEC 23001-17:2024/Amd 2:2025</p>
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class CompressionConfiguration extends FullBox {
    /**
     * Numerical representation of the {@code "cmpC"} box type.
     */
    public static final int BOXTYPE = ((((('c' << 8) | 'm') << 8) | 'p') << 8) | 'C';

    /**
     * The {@code "zlib"} value for {@link #compressionType}.
     */
    public static final int COMPRESSION_ZLIB = ((((('z' << 8) | 'l') << 8) | 'i') << 8) | 'b';

    /**
     * The {@code "defl"} value for {@link #compressionType}.
     */
    public static final int COMPRESSION_DEFLATE = ((((('d' << 8) | 'e') << 8) | 'f') << 8) | 'l';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Identifier of the compression.
     *
     * @see #COMPRESSION_ZLIB
     * @see #COMPRESSION_DEFLATE
     */
    @Interpretation(Type.FOURCC)
    public final int compressionType;

    /**
     * Identifier of which data are in a unit, or {@code null} if unknown.
     */
    public final UnitType unitType;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     */
    public CompressionConfiguration(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        compressionType = input.readInt();
        unitType = UnitType.valueOf(input.readUnsignedByte());
    }
}
