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
package org.apache.sis.internal.coverage.j2d;

import java.util.Arrays;
import java.util.Objects;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import org.apache.sis.util.Workaround;


/**
 * Workaround for broken {@link ColorModel#equals(Object)} in Java 8 and before.
 * This workaround will be removed after upgrade to Java 9.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://bugs.openjdk.java.net/browse/JDK-7107905"></a>
 * @todo Delete after migration to JDK9.
 */
@Workaround(library = "JDK", version = "8")
final class ColorModelPatch<T extends ColorModel> {
    /**
     * The color model to share.
     */
    final T cm;

    /**
     * For internal use only.
     */
    ColorModelPatch(final T cm) {
        this.cm = cm;
    }

    /**
     * Returns {@code true} if the given color models are equal. The {@link ColorModel} class
     * defines an {@code equals} method, but as of Java 6 that method does not compare every
     * attributes. For example it does not compare the color space and the transfer type, so
     * we have to compare them here.
     *
     * @param cm1  the first color model.
     * @param cm2  the second color model.
     * @return {@code true} if the two color models are equal.
     */
    private static boolean equals(final ColorModel cm1, final ColorModel cm2) {
        if (cm1 == cm2) {
            return true;
        }
        if (cm1 != null && cm1.equals(cm2) &&
            cm1.getClass().equals(cm2.getClass()) &&
            cm1.getTransferType() == cm2.getTransferType() &&
            Objects.equals(cm1.getColorSpace(), cm2.getColorSpace()))
        {
            if (cm1 instanceof IndexColorModel) {
                final IndexColorModel icm1 = (IndexColorModel) cm1;
                final IndexColorModel icm2 = (IndexColorModel) cm2;
                final int size = icm1.getMapSize();
                if (icm2.getMapSize() == size &&
                    icm1.getTransparentPixel() == icm2.getTransparentPixel() &&
                    Objects.equals(icm1.getValidPixels(), icm2.getValidPixels()))
                {
                    for (int i=0; i<size; i++) {
                        if (icm1.getRGB(i) != icm2.getRGB(i)) {
                            return false;
                        }
                    }
                }
                if (cm1 instanceof MultiBandsIndexColorModel) {
                    final MultiBandsIndexColorModel micm1 = (MultiBandsIndexColorModel) cm1;
                    final MultiBandsIndexColorModel micm2 = (MultiBandsIndexColorModel) cm2;
                    if (micm1.numBands != micm2.numBands || micm1.visibleBand != micm2.visibleBand) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * For internal use only.
     *
     * @param object object The object to compare to.
     * @return {@code true} if both object are equal.
     */
    @Override
    public boolean equals(final Object object) {
        return (object instanceof ColorModelPatch<?>) && equals(cm, ((ColorModelPatch<?>) object).cm);
    }

    /**
     * For internal use only.
     */
    @Override
    public int hashCode() {
        int code = cm.hashCode() ^ cm.getClass().hashCode();
        if (cm instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel) cm;
            final int[] ARGB = new int[icm.getMapSize()];
            icm.getRGBs(ARGB);
            code ^= Arrays.hashCode(ARGB);
        }
        return code;
    }
}
