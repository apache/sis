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
package org.apache.sis.storage.isobmff.mpeg;

import java.net.URI;
import java.io.IOException;
import java.awt.image.RasterFormatException;
import org.apache.sis.image.DataType;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.TreeNode;
import org.apache.sis.util.resources.Errors;


/**
 * Size and numerical format of a color component.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 *
 * @see UncompressedFrameConfig#components
 */
public final class Component extends TreeNode {
    /**
     * Default value of {@link #bitDepth}.
     */
    public static final short DEFAULT_BIT_DEPTH = (short) Byte.SIZE;

    /**
     * How pixel data should be displayed, or {@code null} if unknown.
     * The value is an instance of {@link ComponentType}, {@link URI}, {@link String}
     * (when the <abbr>URI</abbr> cannot be parsed) or {@link Integer}, in preference order.
     * The value is determined by one of the following ways:
     *
     * <ul>
     *   <li>{@link ComponentDefinition#componentTypes} at an index read from the container box.</li>
     *   <li>A hard-coded value determined by {@link UncompressedFrameConfig#profile}.</li>
     * </ul>
     *
     * The ISO 23001-17:2024 standard said that when this information appears in the two following boxes,
     * {@link ComponentDefinition} shall precede {@link UncompressedFrameConfig}.
     *
     * @see ComponentDefinition#componentTypes
     */
    public Object type;

    /**
     * Number of bits of the component.
     */
    @Interpretation(Type.UNSIGNED)
    public short bitDepth;

    /**
     * Whether the components are integers, floating points or complex numbers.
     * Values are:
     *
     * <ul>
     *   <li>0: unsigned integer coded on {@link #bitDepth} bits.</li>
     *   <li>1: IEEE 754 floating point coded on {@link #bitDepth} bits: 16, 32, 64, 128 or 256.</li>
     *   <li>2: complex number where the real and imaginary parts are coded on {@link #bitDepth} bits each.</li>
     * </ul>
     *
     * Other values are reserved by ISO/IEC for future definitions.
     *
     * @see #getDataType()
     */
    @Interpretation(Type.UNSIGNED)
    public byte format;

    /**
     * The number of bytes used for coding the components if word-alignment is applied.
     * If zero, components are coded on exactly {@link #bitDepth} bits.
     * Otherwise, components are first aligned on a word boundary,
     * then coded on the less significant bits of words of {@code alignSize} bytes.
     */
    @Interpretation(Type.UNSIGNED)
    public byte alignSize;

    /**
     * Creates a component of the given type with the bit depth of bytes.
     *
     * @param  type  how pixel data should be displayed.
     */
    Component(final ComponentType type) {
        this.type = type;
        bitDepth = DEFAULT_BIT_DEPTH;
    }

    /**
     * Creates a new component and loads the payload from the given reader.
     * Caller shall read the {@code index} field itself and pass it in argument,
     * because the {@code index} size depends on the box. The {@link #alignSize}
     * field shall also be read by the caller if that field exist.
     *
     * @param  reader   the reader from which to read the payload.
     * @param  defs     definition of components, or {@code null} if none.
     * @param  palette  whether the parent is {@link ComponentPalette} (true) or {@link UncompressedFrameConfig} (false).
     * @throws IOException if an error occurred while reading the payload.
     */
    Component(final ChannelDataInput input, final ComponentDefinition defs, final int index) throws IOException {
        if (defs != null) {
            final Object[] ct = defs.componentTypes;
            if (ct != null) {
                type = ct[index];
            }
        }
        bitDepth = (short) (input.readUnsignedByte() + 1);
        format = input.readByte();
    }

    /**
     * Returns the Java2D data type for this component.
     *
     * @throws RasterFormatException if the {@link #format} value is unsupported.
     */
    public DataType getDataType() {
        boolean real = false;
        switch (format) {
            case 0: break;
            case 1: real = true; break;
            default: throw new RasterFormatException(Errors.format(Errors.Keys.UnsupportedFormat_1, format));
        }
        return DataType.forNumberOfBits(Short.toUnsignedInt(bitDepth), real, false);
    }
}
