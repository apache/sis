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
package org.apache.sis.internal.referencing.j2d;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;


/**
 * An affine transform which is translated relative to an original transform.
 * The translation terms are stored separately without modifying the transform.
 * This class if for internal use by {@link TileOrganizer} only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
final class TileTranslation {
    /**
     * The translated "grid to real world" transform, as an immutable instance.
     */
    private final AffineTransform gridToCRS;

    /**
     * The translation in "absolute units". This is the same units than for tiles at subsampling (1,1).
     */
    private final int dx, dy;

    /**
     * Creates a new translated transform. The translation is specified in "absolute units",
     * i.e. in the same units than for tiles at subsampling (1,1).
     *
     * @param  subsampling  the {@linkplain Tile#getSubsampling() tile subsampling}.
     * @param  reference    the "grid to real world" transform at subsampling (1,1).
     * @param  dx           the translation along <var>x</var> axis in "absolute units".
     * @param  dy           the translation along <var>y</var> axis in "absolute units".
     */
    TileTranslation(final Dimension subsampling, AffineTransform reference, int dx, int dy) {
        this.dx = dx / subsampling.width;                           // It is okay to round toward zero.
        this.dy = dy / subsampling.height;
        dx %= subsampling.width;
        dy %= subsampling.height;
        reference = new AffineTransform(reference);
        reference.scale(subsampling.width, subsampling.height);
        reference.translate(dx, dy);                                // Correction for non-integer division of (dx,dy).
        gridToCRS = new ImmutableAffineTransform(reference);
    }

    /**
     * Applies the translation and the new "grid to CRS" transform on the given tile.
     *
     * @param  tile  the tile on which to apply the translation.
     */
    final void applyTo(final Tile tile) {
        synchronized (tile) {
            tile.translate(dx, dy);
            tile.setGridToCRS(gridToCRS);
        }
    }
}
