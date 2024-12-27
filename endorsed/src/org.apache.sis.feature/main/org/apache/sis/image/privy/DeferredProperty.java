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
package org.apache.sis.image.privy;

import java.util.Map;
import java.util.function.Function;
import java.awt.image.RenderedImage;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;


/**
 * An image property for which the computation is differed.
 * This special kind of properties is recognized by the following methods:
 *
 * <ul>
 *   <li>{@link TiledImage#getProperty(String)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DeferredProperty {
    /**
     * The value, or {@code null} if not yet computed.
     */
    private Object value;

    /**
     * The function computing the {@linkplain #value}. This function is reset to {@code null}
     * after the value has been computed for allowing the garbage collector to do its work.
     */
    private Function<RenderedImage, ?> provider;

    /**
     * Creates a new deferred property.
     *
     * @param  provider  function computing the {@linkplain #value}.
     */
    public DeferredProperty(final Function<RenderedImage, ?> provider) {
        this.provider = provider;
    }

    /**
     * Returns the property value, which is computed when this method is first invoked.
     *
     * @param  image  the image for which to compute the property value.
     * @return the property value, or {@code null} if it cannot be computed.
     */
    final synchronized Object compute(final RenderedImage image) {
        if (value == null) {
            final Function<RenderedImage, ?> p = provider;
            if (p != null) {
                provider = null;            // Clear first in case an exception is thrown below.
                value = p.apply(image);
            }
        }
        return value;
    }

    /**
     * Creates a deferred property for computing the value of {@link PlanarImage#GRID_GEOMETRY_KEY}.
     *
     * @param  grid        the grid geometry of the grid coverage rendered as an image.
     * @param  dimensions  the dimensions to keep from the coverage grid geometry.
     * @return a deferred property for computing the grid geometry of an image.
     */
    public static Map<String,Object> forGridGeometry(final GridGeometry grid, final int[] dimensions) {
        return Map.of(PlanarImage.GRID_GEOMETRY_KEY, new DeferredProperty(new ImageGeometry(grid, dimensions)));
    }

    /**
     * A deferred property for computing the value of {@link PlanarImage#GRID_GEOMETRY_KEY}.
     */
    private static final class ImageGeometry implements Function<RenderedImage, GridGeometry> {
        /** The grid geometry of the grid coverage rendered as an image. */
        private final GridGeometry grid;

        /** The dimensions to keep from the coverage grid geometry. */
        private final int dimX, dimY;

        /**
         * Creates a deferred property for an image grid geometry.
         *
         * @param grid        the grid geometry of the grid coverage rendered as an image.
         * @param dimensions  the dimensions to keep from the coverage grid geometry.
         */
        public ImageGeometry(final GridGeometry grid, final int[] dimensions) {
            this.grid = grid;
            this.dimX = dimensions[0];
            this.dimY = dimensions[1];
        }

        /**
         * Invoked when the {@link PlanarImage#GRID_GEOMETRY_KEY} value needs to be computed.
         * The image should have been rendered from a grid coverage having the grid geometry
         * given at construction time.
         *
         * @param  image  the image for which to compute the property.
         * @return the grid geometry property computed for the given image.
         */
        @Override
        public GridGeometry apply(final RenderedImage image) {
            final GridExtent extent = grid.getExtent();
            return grid.selectDimensions(dimX, dimY).shiftGrid(
                    Math.subtractExact(image.getMinX(), extent.getLow(dimX)),
                    Math.subtractExact(image.getMinY(), extent.getLow(dimY)));
        }
    }
}
