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
import java.awt.Transparency;
import java.awt.image.IndexColorModel;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.base.ArrayOfLongs;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;
import org.apache.sis.storage.isobmff.base.ItemPropertyContainer;


/**
 * Describes image data coded through a palette. Shall be present if and only if a
 * {@link ComponentDefinition} box is present and contains {@link ComponentType#PALETTE}.
 *
 * <h4>Container</h4>
 * The container can be a {@link ItemPropertyContainer} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ComponentPalette extends FullBox {
    /**
     * Numerical representation of the {@code "cpal"} box type.
     */
    public static final int BOXTYPE = ((((('c' << 8) | 'p') << 8) | 'a') << 8) | 'l';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The components (bands) of the palette.
     */
    public final Component[] components;

    /**
     * Values in each component (band) for each palette index. Should not be modified,
     * as the same sub-array instances may be shared when the same color is repeated.
     */
    public final long[][] values;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @param  defs    definition of components, or {@code null} if none.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public ComponentPalette(final Reader reader, final ComponentDefinition defs)
            throws IOException, UnsupportedVersionException
    {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        final int numBands = input.readUnsignedShort();
        components = new Component[numBands];
        for (int i=0; i<numBands; i++) {
            final int index = input.readInt();
            components[i] = new Component(input, defs, index);
        }
        final int length = input.readInt();
        values = new long[length][];
        for (int i=0; i<length; i++) {
            final long[] color = new long[numBands];
            for (int band=0; band<numBands; band++) {
                color[band] = input.readBits(components[band].bitDepth);
                input.skipRemainingBits();
            }
            values[i] = new ArrayOfLongs(color).unique(reader.sharedObjects);
        }
    }

    /**
     * Creates a color model with the ARGB values defined by this palette.
     * This method recognizes only the red, green, blue and alpha components.
     * If a component uses more than 8 bits, the lowest bits are dropped.
     *
     * @param  dataType     type of sample values.
     * @param  numBits      the number of bits per sample value, or 0 for automatic.
     * @param  numBands     the number of bands of the sample modle. This is usually 1.
     * @param  visibleBand  the band to display. This is usually 0.
     * @return the color model, or {@code null} if no red, green or blue component has been found.
     */
    public IndexColorModel toARGB(final DataType dataType, final int numBits, final int numBands, final int visibleBand) {
        /*
         * The following information, repeated for each of Red, Green, Blue, Alpha bands.
         *   - Index of the component.
         *   - The number of right-most bits to discard for keeping at most 8 bits.
         *   - The shift to apply to the left including padding for getting 8 bits.
         */
        int length = 0, colorMask = 0;
        final int n = components.length;
        final var filter = new int[n * 3];
        for (int band=0; band<n; band++) {
            final Component c = components[band];
            final Object type = c.type;
            if (type instanceof ComponentType ct) {
                final int shift;
                switch (ct) {
                    case ALPHA: shift = 3; break;
                    case RED:   shift = 2; break;
                    case GREEN: shift = 1; break;
                    case BLUE:  shift = 0; break;
                    default: continue;
                }
                final int extraSize = c.bitDepth - Byte.SIZE;
                filter[length++] = band;
                filter[length++] = Math.max( extraSize, 0);
                filter[length++] = Math.max(-extraSize, 0) + shift * Byte.SIZE;
                colorMask |= (1 << shift);
            }
        }
        if ((colorMask & 0b111) == 0) {
            return null;    // No RGB component, ignoring alpha.
        }
        final var ARGB = new int[values.length];
        for (int i=0; i<ARGB.length; i++) {
            final long[] v = values[i];
            int color=0, ct=0;
            do {
                long c;
                c  = v[filter[ct++]];   // The red, green, blue or alpha value.
                c >>>= filter[ct++];    // If there is more than 8 bits, discard the lowest ones.
                c  <<= filter[ct++];    // Move the value to its location in the ARGB code.
                color |= (int) c;
            } while (ct < length);
            ARGB[i] = color;
        }
        final boolean hasAlpha = (colorMask & 0b1000) != 0;
        return ColorModelFactory.createIndexColorModel(dataType, numBits, numBands, visibleBand, ARGB,
                hasAlpha, hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }
}
