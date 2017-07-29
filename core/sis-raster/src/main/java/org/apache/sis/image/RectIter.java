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
package org.apache.sis.image;

import java.awt.Rectangle;

/**
 * To avoid unneccessary recomputing of method {@link Rectangle#getMaxX() }
 * or {@link Rectangle#getMaxY() } do a new integer Rectangle implementation only used by {@link PixelIterator}
 *
 * @author Remi Marechal (Geomatys).
 */
class RectIter {

    final int minx;
    final int miny;
    final int maxX;
    final int maxY;
    final int width;
    final int height;

    /**
     * Create integer Rectangle with no boundary coordinate recomputing.
     *
     * @param rect origin coordinates references.
     */
    RectIter(final Rectangle rect) {
        this.minx   = rect.x;
        this.miny   = rect.y;
        this.width  = rect.width;
        this.height = rect.height;
        this.maxX   = minx + width;
        this.maxY   = miny + height;
    }

    /**
     * Create integer Rectangle with no boundary coordinate recomputing.
     *
     * @param minx minimum lower left corner in X direction.
     * @param miny minimum lower left corner in Y direction.
     * @param width width in X direction.
     * @param height height in Y direction.
     */
    RectIter(final int minx, final int miny, final int width, final int height) {
        this.minx   = minx;
        this.miny   = miny;
        this.width  = width;
        this.height = height;
        this.maxX   = this.minx + width;
        this.maxY   = this.miny + height;
    }

    boolean isEmpty() {
        return (maxX <= minx || maxY <= miny);
    }

    Rectangle toRectangle() {
        return new Rectangle(minx, miny, width, height);
    }
}
