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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.BoxRegistry;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * Extra information about selected items.
 *
 * <h4>Container</h4>
 * The container can be a {@link ItemInfo}.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ItemInfoEntry extends FullBox {
    /**
     * Numerical representation of the {@code "infe"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'n') << 8) | 'f') << 8) | 'e';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * 0 for the primary resource, or the identifier of the item for which the information are defined.
     *
     * <h4>Implementation note</h4>
     * This field is not annotated with {@link Type#IDENTIFIER} because it
     * would cause this field to repeat the value shown in {@link #itemName}.
     */
    @Interpretation(Type.UNSIGNED)
    public final int itemID;

    /**
     * 0 for an unprotected item, or the 1-based index of the {@code ItemProtection} entry defining
     * the protection applied to this item.
     */
    @Interpretation(Type.UNSIGNED)
    public final short itemProtectionIndex;

    /**
     * A valid value for {@link #itemType}.
     */
    public static final int MIME = ((((('m' << 8) | 'i') << 8) | 'm') << 8) | 'e',
                            URI  = ((((('u' << 8) | 'r') << 8) | 'i') << 8) | ' ',
                            UNCI = ((((('u' << 8) | 'n') << 8) | 'c') << 8) | 'i',
                            JPEG = ((((('j' << 8) | 'p') << 8) | 'e') << 8) | 'g',
                            GRID = ((((('g' << 8) | 'r') << 8) | 'i') << 8) | 'd';

    /**
     * Item type indicator such as {@link #MIME} or {@link #URI}.
     */
    @Interpretation(Type.FOURCC)
    public final int itemType;

    /**
     * Symbolic name of the item (source file), or {@code null} if none.
     */
    public final String itemName;

    /**
     * The <abbr>MIME</abbr> type of the item, or {@code null} if none.
     * If the content is encoded, then this is the <abbr>MIME</abbr> type after decoding.
     */
    public String contentType;

    /**
     * The encoding, or {@code null} if none.
     * Examples: {@code "gzip"}, {@code "compress"}, {@code "deflate"}.
     */
    public String contentEncoding;

    /**
     * Identifier of the extension fields of version 1 compared to version 0.
     */
    @Interpretation(Type.FOURCC)
    public int extensionType;

    /**
     * A registry accepting only children of types that are legal for the container.
     * All other box types will be ignored with a warning sent to the listeners.
     */
    private static final BoxRegistry FILTER = new BoxRegistry() {
        @Override public Box create(final Reader reader, final int fourCC) throws IOException {
            switch (fourCC) {
                case FDItemInfoExtension.BOXTYPE: return new FDItemInfoExtension(reader);
                default: reader.unexpectedChildType(BOXTYPE, fourCC); return null;
            }
        }
    };

    /**
     * The extension fields, or {@code null} if none.
     * Contains content length (in bytes) and MD5 sum.
     */
    public FDItemInfoExtension extension;

    /**
     * An absolute URI used as a type indicator.
     */
    public URI itemUriType;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     * @throws DataStoreException if the stream contains inconsistent or unsupported data.
     */
    public ItemInfoEntry(final Reader reader) throws IOException, DataStoreException {
        super(reader);
        final ChannelDataInput input = reader.input;
        final int version = version();
        if (version < 0 || version > 3) {
            throw new UnsupportedVersionException(BOXTYPE, version);
        }
        itemID              = (version >= 3) ? input.readInt() : input.readUnsignedShort();
        itemProtectionIndex = input.readShort();
        itemType            = (version >= 2) ? input.readInt() : MIME;
        itemName            = reader.readNullTerminatedString(false);
        switch (itemType) {
            case MIME: {
                contentType     = reader.readNullTerminatedString(false);
                contentEncoding = reader.readNullTerminatedString(false);
                break;
            }
            case URI: {
                itemUriType = reader.readURI();
                break;
            }
        }
        if (version == 1) {
            extensionType = input.readInt();
            extension = (FDItemInfoExtension) reader.readBox(FILTER);
        }
    }

    /**
     * Returns the item name. If {@link #itemName} is null, then {@link #itemID} is used.
     * This is used for formatting error messages and should not be used as real identifier.
     *
     * @return a non-null item name.
     */
    public String itemName() {
        return (itemName != null) ? itemName : Integer.toUnsignedString(itemID);
    }

    /**
     * Converts node properties to <abbr>ISO</abbr> 19115 metadata.
     *
     * @param  builder  the builder where to set metadata information.
     */
    @Override
    public void metadata(final MetadataBuilder builder) {
        // TODO: DefaultDataType.setFileType(contentType);
        builder.addCompression(contentEncoding);
        if (extension != null) {
            extension.metadata(builder);
        }
    }
}
