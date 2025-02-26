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

import java.awt.color.ColorSpace;
import org.apache.sis.util.CharSequences;
import org.apache.sis.storage.UnsupportedEncodingException;


/**
 * Type of color component. This reflect only how pixel data should be displayed.
 * This enumeration does not contain information about band bounds, for example.
 *
 * The ordinal values of this enumeration are the values specified by <abbr>ISO</abbr> 23001-17:2024 table 1.
 * Values equal or greater then {@code 0x8000} are used-defined component types.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public enum ComponentType {
    /**
     * Monochrome component.
     */
    MONOCHROME(ColorSpace.TYPE_GRAY),

    /**
     * Luma component (Y).
     */
    LUMA_Y(ColorSpace.TYPE_YCbCr),

    /**
     * Chroma component (Cb / U).
     */
    CHROMA_CB_U(ColorSpace.TYPE_YCbCr),

    /**
     * Chroma component (Cr / V).
     */
    CHROMA_CR_V(ColorSpace.TYPE_YCbCr),

    /**
     * Red component (R).
     */
    RED(ColorSpace.TYPE_RGB),

    /**
     * Green component (G).
     */
    GREEN(ColorSpace.TYPE_RGB),

    /**
     * Blue component (B).
     */
    BLUE(ColorSpace.TYPE_RGB),

    /**
     * Alpha/transparency component (A).
     * Arbitrarily associated to <abbr>RGB</abbr> color space.
     */
    ALPHA(ColorSpace.TYPE_RGB),

    /**
     * Depth component (D).
     */
    DEPTH(-1),

    /**
     * Disparity component (Disp).
     */
    DISPARITY(-1),

    /**
     * Palette component (P).
     * The {@link Component#format} value for this component shall be 0.
     *
     * @see ComponentPalette
     */
    PALETTE(-1),

    /**
     * Filter Array (FA) component such as Bayer, RGBW, etc.
     */
    FILTER(-1),

    /**
     * Padded component (unused bits/bytes).
     */
    PADDED(-1),

    /**
     * Cyan component (C).
     */
    CYAN(ColorSpace.TYPE_CMY),

    /**
     * Magenta component (M).
     */
    MAGENTA(ColorSpace.TYPE_CMY),

    /**
     * Yellow component (Y).
     */
    YELLOW(ColorSpace.TYPE_CMY),

    /**
     * Key (black) component (K).
     */
    KEY(ColorSpace.TYPE_CMYK);

    /**
     * The Java2D color space as one of the {@code TYPE_*} constants in {@link ColorSpace}, or -1 if none.
     */
    public final int colorSpace;

    /**
     * Creates a new enumeration value.
     *
     * @param colorSpace the Java2D {@code ColorSpace.TYPE_*} constant value, or -1 if none.
     */
    private ComponentType(final int colorSpace) {
        this.colorSpace = colorSpace;
    }

    /**
     * Returns the value for the given <abbr>ISO</abbr> code.
     *
     * @param  ordinal  the <abbr>ISO</abbr> code
     * @return the enumeration value for the given code.
     * @throws UnsupportedEncodingException if the given code is unknown to this enumeration.
     */
    static ComponentType valueOf(final int ordinal) throws UnsupportedEncodingException {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        }
        throw new UnsupportedEncodingException("Unknown interleaving mode: " + ordinal);
    }

    /**
     * All values indexed by <abbr>ISO</abbr> value.
     */
    private static final ComponentType[] VALUES = values();

    /**
     * The components of the {@code "rgb"} predefined profile.
     *
     * @see UncompressedFrameConfig#RGB
     */
    static final ComponentType[] RGB = {RED, GREEN, BLUE};

    /**
     * The components of the {@code "rgba"} predefined profile.
     *
     * @see UncompressedFrameConfig#RGBA
     */
    static final ComponentType[] RGBA = {RED, GREEN, BLUE, ALPHA};

    /**
     * The components of the {@code "abgr"} predefined profile.
     *
     * @see UncompressedFrameConfig#ABGR
     */
    static final ComponentType[] ABGR = {ALPHA, BLUE, GREEN, RED};

    /**
     * Returns a human-readable string representation of this enumeration value.
     * This string representation will be used as sample dimension name.
     *
     * @return human-readable string representation.
     */
    @Override
    public String toString() {
        return CharSequences.upperCaseToSentence(name()).toString();
    }
}
