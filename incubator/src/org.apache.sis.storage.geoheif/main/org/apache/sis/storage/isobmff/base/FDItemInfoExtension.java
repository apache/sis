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

import java.net.URI;
import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.base.MetadataBuilder;


/**
 * Information added by version 1 of {@link ItemInfo} compared to version 0.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class FDItemInfoExtension extends Box {
    /**
     * Numerical representation of the {@code "fdel"} box type.
     */
    public static final int BOXTYPE = ((((('f' << 8) | 'd') << 8) | 'e') << 8) | 'l';

    /**
     * Identifies the extension fields of version 1 with respect to version 0 of the item information entry.
     */
    @Interpretation(Type.FOURCC)
    public final int extensionType;

    /**
     * URL of the file, or {@code null} if none.
     */
    public final URI contentLocation;

    /**
     * MD5 digest of the file, or {@code null} if none.
     */
    public final String contentMD5;

    /**
     * Total length in bytes of the decoded file.
     */
    @Interpretation(Type.UNSIGNED)
    public final long contentLength;

    /**
     * Total length in bytes of the encoded file.
     * This is equal to {@link #contentLength} if no encoding is applied.
     */
    @Interpretation(Type.UNSIGNED)
    public final long transferLength;

    /**
     * File groups to which the file item (source file) belongs.
     */
    @Interpretation(Type.IDENTIFIER)
    public final int[] groupID;

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
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     */
    public FDItemInfoExtension(final Reader reader) throws IOException {
        final ChannelDataInput input = reader.input;
        extensionType   = input.readInt();
        contentLocation = reader.readURI();
        contentMD5      = reader.readNullTerminatedString(false);
        contentLength   = input.readLong();
        transferLength  = input.readLong();
        groupID         = input.readInts(input.readUnsignedByte());
    }

    /**
     * Converts node properties to <abbr>ISO</abbr> 19115 metadata.
     *
     * @param  builder  the builder where to set metadata information.
     */
    @Override
    public void metadata(final MetadataBuilder builder) {
        // TODO: DefaultDigitalTransferOptions.setTransferSize(transferLength / 1E+6);
        // TODO: Linkage to contentLocation
    }
}
