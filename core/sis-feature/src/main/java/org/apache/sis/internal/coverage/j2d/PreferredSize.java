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

import java.awt.Dimension;
import java.awt.Rectangle;


/**
 * Specifies an image size which can be modified by {@link ImageLayout} if needed. Changes are applied only if
 * an image can not be tiled because {@link ImageLayout} can not find a tile size close to the desired size.
 * For example if the image width is a prime number, there is no way to divide the image horizontally with
 * an integer number of tiles. The only way to get an integer number of tiles is to change the image size.
 * The use of this class is understood by {@link ImageLayout} as a permission to do so.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@SuppressWarnings("serial")     // Not intended to be serialized.
public final class PreferredSize extends Rectangle {
    /**
     * Creates a new rectangle.
     */
    public PreferredSize() {
    }

    /**
     * Adjusts the bounds for making it divisible by the given tile size.
     */
    final void makeDivisible(final Dimension tileSize) {
        if (!isEmpty()) {
            final int sx = sizeToAdd(width,   tileSize.width);
            final int sy = sizeToAdd(height,  tileSize.height);
            if ((width  += sx) < 0) width  -= tileSize.width;       // if (overflow) reduce to valid range.
            if ((height += sy) < 0) height -= tileSize.height;
            if (x < (x -= sx/2)) x = Integer.MIN_VALUE;             // if (overflow) set to minimal value.
            if (y < (y -= sy/2)) y = Integer.MIN_VALUE;
        }
    }

    /**
     * Computes the size to add to the width or height for making it divisible by the given tile size.
     */
    private static int sizeToAdd(int size, final int tileSize) {
        size %= tileSize;
        if (size != 0) {
            size = tileSize - size;
        }
        return size;
    }
}
