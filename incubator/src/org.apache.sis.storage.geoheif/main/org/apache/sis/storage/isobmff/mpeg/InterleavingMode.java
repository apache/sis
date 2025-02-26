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

import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.util.CharSequences;


/**
 * Interleaving mode of pixels within a tile.
 * The ordinal values of this enumeration are the values specified by <abbr>ISO</abbr> 23001-17:2024 table 4.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public enum InterleavingMode {
    /**
     * Component interleaving.
     * Map to Java2D {@link BandedSampleModel}.
     */
    COMPONENT,

    /**
     * Pixel interleaving.
     * Map to Java2D {@link PixelInterleavedSampleModel}.
     */
    PIXEL,

    /**
     * Mixed interleaving.
     * Allowed only when {@link UncompressedFrameConfig#samplingType} is not zero.
     */
    MIXED,

    /**
     * Row interleaving.
     * Map to Java2D {@link ComponentSampleModel} with a pixel stride equals to tile width.
     */
    ROW,

    /**
     * Tile-component interleaving.
     * Similar to Java2D {@link BandedSampleModel}, except that for each band,
     * the values of all tiles are written sequentially before to move to the next band.
     */
    TILE,

    /**
     * Multi-Y pixel interleaving.
     * For use with non-zero {@link UncompressedFrameConfig#samplingType}.
     */
    MULTI_Y_PIXEL;

    /**
     * All values indexed by <abbr>ISO</abbr> value.
     */
    private static final InterleavingMode[] VALUES = values();

    /**
     * Returns the value for the given <abbr>ISO</abbr> code.
     *
     * @param  ordinal  the <abbr>ISO</abbr> code
     * @return the enumeration value for the given code.
     * @throws UnsupportedEncodingException if the given code is unknown to this enumeration.
     */
    static InterleavingMode valueOf(final int ordinal) throws UnsupportedEncodingException {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        }
        throw new UnsupportedEncodingException("Unknown interleaving mode: " + ordinal);
    }

    /**
     * Returns a human-readable string representation of this enumeration value,
     * together with the <abbr>ISO</abbr> numerical value.
     *
     * @return human-readable string representation.
     */
    @Override
    public String toString() {
        var buffer = (StringBuilder) CharSequences.upperCaseToSentence(name());
        return buffer.append(" (").append(ordinal()).append(')').toString();
    }
}
