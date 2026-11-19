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
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.apache.sis.util.Disposable;


/**
 * An image which wraps an existing image unchanged, except for properties and/or color model.
 * All {@link RenderedImage} methods related to coordinate systems (pixel coordinates or tile indices),
 * and all methods fetching tiles, delegate to the wrapped image.
 *
 * Most non-abstract methods are final because {@link PixelIterator} (among others)
 * relies on the fact that it can unwrap this image and still get the same pixel values.
 *
 * <p>All subclasses shall override {@link #equals(Object)} and {@link #hashCode()}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class TileForwardingImage extends ImageAdapter {
    /**
     * Creates a new wrapper for the given image.
     *
     * @param  source  the image to wrap.
     */
    protected TileForwardingImage(final RenderedImage source) {
        super(source);
    }

    /** Delegates to the wrapped image. */
    @Override public final Raster         getTile(int tx, int ty)    {return source.getTile(tx, ty);}
    @Override public final Raster         getData()                  {return source.getData();}
    @Override public final Raster         getData(Rectangle region)  {return source.getData(region);}
    @Override public final WritableRaster copyData(WritableRaster r) {return source.copyData(r);}

    /**
     * Notifies the source image that tiles will be computed soon in the given region.
     * If the source image is an instance of {@link PlanarImage}, then this method
     * forwards the notification to it. Otherwise default implementation does nothing.
     */
    @Override
    protected Disposable prefetch(final Rectangle tiles) {
        if (source instanceof PlanarImage) {
            /*
             * Forwarding directly is possible because the contract
             * of this class said that tile indices must be the same.
             */
            return ((PlanarImage) source).prefetch(tiles);
        } else {
            return super.prefetch(tiles);
        }
    }
}
