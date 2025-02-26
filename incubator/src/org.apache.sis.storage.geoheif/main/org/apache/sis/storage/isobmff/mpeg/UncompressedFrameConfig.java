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
import java.awt.color.ColorSpace;
import java.awt.image.SinglePixelPackedSampleModel;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;
import org.apache.sis.storage.isobmff.base.ItemPropertyContainer;


/**
 * Uncompressed frames that are composed of one or more components.
 * Examples: <abbr>RGB</abbr> or <abbr>YUV</abbr> formats.
 * Component values may be absolute values or indexes into a color palette.
 *
 * <h4>Container</h4>
 * The container can be a {@link ItemPropertyContainer} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class UncompressedFrameConfig extends FullBox {
    /**
     * Numerical representation of the {@code "uncC"} box type.
     */
    public static final int BOXTYPE = ((((('u' << 8) | 'n') << 8) | 'c') << 8) | 'C';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Numerical representation of the {@code "rgb3"} profile.
     *
     * @see ComponentType#RGB
     */
    static final int RGB = ((((('r' << 8) | 'g') << 8) | 'b') << 8) | '3';

    /**
     * Numerical representation of the {@code "rgba"} profile.
     *
     * @see ComponentType#RGBA
     */
    static final int RGBA = ((((('r' << 8) | 'g') << 8) | 'b') << 8) | 'a';

    /**
     * Numerical representation of the {@code "abgr"} profile.
     *
     * @see ComponentType#ABGR
     */
    static final int ABGR = ((((('a' << 8) | 'b') << 8) | 'g') << 8) | 'r';

    /**
     * Predefined configuration as a code such as {@link #RGB}, {@link #RGBA} or {@link #ARGB}.
     * Value 0 means no profile.
     */
    @Interpretation(Type.FOURCC)
    public final int profile;

    /**
     * The components (bands) of this frame.
     */
    public final Component[] components;

    /**
     * Whether components use predefined sampling mode.
     * <ul>
     *   <li>0: No sub-sampling. All components have the same width and height as the frame.</li>
     *   <li>1: YCbCr 4:2:2 sub-sampling. Requires {@link ComponentType#colorSpace} = {@link ColorSpace#TYPE_YCbCr}.</li>
     *   <li>2: YCbCr 4:2:0 sub-sampling. Requires {@link ComponentType#colorSpace} = {@link ColorSpace#TYPE_YCbCr}.</li>
     *   <li>3: YCbCr 4:1:1 sub-sampling. Requires {@link ComponentType#colorSpace} = {@link ColorSpace#TYPE_YCbCr}.</li>
     * </ul>
     *
     * Other values are reserved by ISO/IEC for future definitions.
     * Apache <abbr>SIS</abbr> currently supports only the type 0 (no sub-sampling).
     */
    @Interpretation(Type.UNSIGNED)
    public byte samplingType;

    /**
     * Interleaving mode of pixels within a tile.
     */
    public final InterleavingMode interleaveType;

    /**
     * Size in bytes of blocks in which one or more component values are stored.
     * A value of 0 means that blocking is not used within sample data.
     * If non-zero, shall be ≥ {@link Component#alignSize}.
     *
     * <p>This property maps to Java2D {@link SinglePixelPackedSampleModel}
     * with unused bits in the least significant bits of the data elements.</p>
     *
     * @see #blockLittleEndian
     * @see #blockReversed
     * @see #blockPadLSB
     * @see #padUnknown
     */
    @Interpretation(Type.UNSIGNED)
    public byte blockSize;

    /**
     * Whether components are stored in little endian byte order.
     */
    public boolean componentsLittleEndian;

    /**
     * Location of the padding bits in a block.
     * If {@code true}, padding bits are located in the least significant bits of the block.
     * If {@code false} padding bits are located in the most significant bits of the block.
     * Should be ignored when {@link #blockSize} is zero.
     */
    public boolean blockPadLSB;

    /**
     * Whether blocks a stored in little endian byte order.
     * Should be ignored when {@link #blockSize} is zero.
     */
    public boolean blockLittleEndian;

    /**
     * Whether the component order within each block shall be inverted, without changing the location of padding bits.
     * Should be ignored when {@link #blockSize} is zero.
     */
    public boolean blockReversed;

    /**
     * Whether padding bits are unrestricted. If {@code false}, all padding bits shall be zero.
     * Should be ignored when {@link #blockSize} is zero.
     */
    public boolean padUnknown;

    /**
     * Total number of bytes required to contain component values of a single pixel, including padding.
     * A value of 0 means that no additional padding is present after each pixel.
     * This property maps to Java2D <i>pixel stride</i>.
     */
    @Interpretation(Type.UNSIGNED)
    public int pixelSize;

    /**
     * Total number of bytes required for all values of a row, including padding.
     * A value of 0 means that no additional padding is present at the end of rows of tiles.
     * This property maps to Java2D <i>scan line stride</i>.
     */
    @Interpretation(Type.UNSIGNED)
    public int rowAlignSize;

    /**
     * Total number of bytes required for all values of a tile, including padding.
     * A value of 0 means that no additional padding is present at the end of tiles.
     */
    @Interpretation(Type.UNSIGNED)
    public int tileAlignSize;

    /**
     * Horizontal number of tiles in the frame.
     */
    @Interpretation(Type.UNSIGNED)
    public final int numTileCols;

    /**
     * Vertical number of tiles in the frame.
     */
    @Interpretation(Type.UNSIGNED)
    public final int numTileRows;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @param  defs    definition of bands, or {@code null} if none.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     * @throws UnsupportedEncodingException if the format, interleaving mode or other option is not supported.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     */
    public UncompressedFrameConfig(final Reader reader, final ComponentDefinition defs)
            throws IOException, UnsupportedEncodingException
    {
        super(reader);
        final ChannelDataInput input = reader.input;
        final ComponentType[] predefined;
        profile = input.readInt();
        /*
         * Field values predefined by the profile from table 5 in ISO 23001-17:2024.
         * This switch statement supports only the few cases that we can map to RGBA.
         * Unsupported profiles are variants of YUV (Luma, Chroma Cb/U, Chroma Cr/V).
         */
        switch (profile) {
            case RGB:  predefined = ComponentType.RGB;  break;
            case RGBA: predefined = ComponentType.RGBA; break;
            case ABGR: predefined = ComponentType.ABGR; break;
            default:   predefined = null;
        }
        switch (version()) {
            /*
             * Configuration specified by explicit field values. Those values should be consistent with
             * the values inferred from the profile in above switch cases, but this is not verified.
             */
            case 0: {
                final int count = input.readInt();
                components = new Component[count];
                for (int i=0; i<count; i++) {
                    final int index = input.readUnsignedShort();
                    var c = new Component(input, defs, index);
                    c.alignSize = input.readByte();
                    if (c.type == null && predefined != null && i < predefined.length) {
                        c.type = predefined[i];
                    }
                    components[i] = c;
                }
                samplingType   = input.readByte();
                interleaveType = InterleavingMode.valueOf(input.readUnsignedByte());
                blockSize      = input.readByte();

                final int options = input.readUnsignedByte();
                componentsLittleEndian = (options & 0b10000000) != 0;
                blockPadLSB            = (options & 0b01000000) != 0;
                blockLittleEndian      = (options & 0b00100000) != 0;
                blockReversed          = (options & 0b00010000) != 0;
                padUnknown             = (options & 0b00001000) != 0;
                pixelSize     = input.readInt();
                rowAlignSize  = input.readInt();
                tileAlignSize = input.readInt();
                numTileCols   = Math.toIntExact(input.readUnsignedInt() + 1);
                numTileRows   = Math.toIntExact(input.readUnsignedInt() + 1);
                break;
            }
            case 1: {
                if (predefined == null) {
                    throw new UnsupportedEncodingException("Unsupported profile: " + formatFourCC(profile));
                }
                numTileCols    = 1;
                numTileRows    = 1;
                interleaveType = InterleavingMode.COMPONENT;
                components     = new Component[predefined.length];
                for (int i=0; i<predefined.length; i++) {
                    components[i] = new Component(predefined[i]);
                }
                break;
            }
            default: throw new UnsupportedVersionException(BOXTYPE, version());
        }
    }
}
