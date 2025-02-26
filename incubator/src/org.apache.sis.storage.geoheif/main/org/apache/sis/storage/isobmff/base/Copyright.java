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

import java.util.Locale;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * Copyright declaration which applies to the entire presentation, or to the entire track.
 *
 * <h4>Container</h4>
 * The container can be an {@link UserData} box.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public final class Copyright extends FullBox {
    /**
     * Numerical representation of the {@code "cprt"} box type.
     */
    public static final int BOXTYPE = ((((('c' << 8) | 'p') << 8) | 'r') << 8) | 't';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Language code.
     */
    public final Locale language;

    /**
     * The copyright notice, or {@code null} if none.
     */
    public final String notice;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public Copyright(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        int packed = input.readShort();
        final byte[] code = new byte[3];
        for (int i=code.length; --i >= 0;) {
            code[i] = (byte) ((packed & 0b111) + 0x60);
            packed >>>= 3;
        }
        language = Locale.of(new String(code, StandardCharsets.US_ASCII));
        notice = reader.readNullTerminatedString(true);
    }

    /**
     * Converts node properties to <abbr>ISO</abbr> 19115 metadata.
     *
     * @param  builder  the builder where to set metadata information.
     */
    @Override
    public void metadata(final MetadataBuilder builder) {
        builder.parseLegalNotice(language, notice);
    }
}
