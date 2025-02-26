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
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;


/**
 * Rendering and color conversion.
 *
 * <h4>Container</h4>
 * The container can be a {@code VisualSampleEntry} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ColourInformation extends Box {
    /**
     * Numerical representation of the {@code "colr"} box type.
     */
    public static final int BOXTYPE = ((((('c' << 8) | 'o') << 8) | 'l') << 8) | 'r';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Numerical representation of the {@code "nclx"} color type.
     * This is one of the possible values of {@link #colourType}.
     * Used for on-screen colors.
     */
    public static final int NCLX = ((((('n' << 8) | 'c') << 8) | 'l') << 8) | 'x';

    /**
     * Numerical representation of the {@code "rICC"} color type.
     * This is one of the possible values of {@link #colourType}.
     */
    public static final int RICC = ((((('r' << 8) | 'I') << 8) | 'C') << 8) | 'C';

    /**
     * Numerical representation of the {@code "prof"} color type.
     * This is one of the possible values of {@link #colourType}.
     */
    public static final int PROF = ((((('p' << 8) | 'r') << 8) | 'o') << 8) | 'f';

    /**
     * Type of color information supplied.
     * Can be {@link #NCLX}, {@link #RICC} or {@link #PROF}.
     */
    @Interpretation(Type.FOURCC)
    public final int colourType;

    /**
     * A color primary value as defined in ISO/IEC 23091-2.
     * Provided only with {@link #NCLX} color type.
     */
    public final short colourPrimaries;

    /**
     * Transfer characteristic value as defined in ISO/IEC 23091-2.
     * Provided only with {@link #NCLX} color type.
     */
    public final short transferCharacteristics;

    /**
     * Matrix coefficient value as defined in ISO/IEC 23091-2.
     * Provided only with {@link #NCLX} color type.
     */
    public final short matrixCoefficients;

    /**
     * Video full range flag as defined in ISO/IEC 23091-2.
     * Provided only with {@link #NCLX} color type.
     */
    public final boolean fullRangeFlag;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     */
    public ColourInformation(final Reader reader) throws IOException {
        final ChannelDataInput input = reader.input;
        colourType = input.readInt();
        switch (colourType) {
            case NCLX: {
                colourPrimaries = input.readShort();
                transferCharacteristics = input.readShort();
                matrixCoefficients = input.readShort();
                fullRangeFlag = (input.readByte() & 0x80) != 0;
                break;
            }
            default: {
                colourPrimaries = 0;
                transferCharacteristics = 0;
                matrixCoefficients = 0;
                fullRangeFlag = false;
            }
        }
    }
}
