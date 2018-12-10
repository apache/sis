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
package org.apache.sis.internal.raster;

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;


/**
 * Creates color models from given properties.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class ColorModelFactory {
    /**
     * Do not allow instantiation of this class.
     */
    private ColorModelFactory() {
    }

    /**
     * Creates for image of the given type.
     *
     * @param  type  one of {@link DataBuffer} constant.
     * @return color model.
     *
     * @todo need much improvement.
     */
    public static ColorModel create(final int type) {
        final int bits = DataBuffer.getDataTypeSize(type);
        final int[] ARGB = new int[1 << bits];
        final float scale = 255f / ARGB.length;
        for (int i=0; i<ARGB.length; i++) {
            int c = Math.round(scale * i);
            c |= (c << 8) | (c << 16);
            ARGB[i] = c;
        }
        return new IndexColorModel(bits, ARGB.length, ARGB, 0, false, -1, DataBuffer.TYPE_USHORT);
    }
}
