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
import java.util.Vector;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.ColorModel;
import org.apache.sis.util.ArgumentChecks;


/**
 * An image which is computed on-the-fly, usually from other images.
 * Computations are performed on a tile-by-tile basis and the result
 * is stored in a cache shared by all images on the platform.
 *
 * @todo Add an API providing operation parameters.
 *
 * <p>Subclasses need to implement the following methods:</p>
 * <ul>
 *   <li>{@link #computeTile(int, int)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class ImageOperation extends CachedImage {
    /**
     * The sources of this image. Never null and does not contain any null element.
     */
    private final RenderedImage[] sources;

    /**
     * The color model associated to this image, or {@code null} if unspecified.
     */
    private final ColorModel colorModel;

    /**
     * Image size in pixels.
     */
    private final int width, height;

    /**
     * Coordinate of the pixel in upper-left corner.
     */
    private final int minX, minY;

    /**
     * Coordinate of the tile in the upper-left corner.
     */
    private final int minTileX, minTileY;

    /**
     * Creates a new operation with a single image as the main source.
     * This {@code ImageOperation} will use the same color model, data type,
     * tile size, image size and minimum coordinates than the given image.
     *
     * @param  image   the main source of this operation.
     * @param  others  additional sources, or {@code null} if none.
     */
    protected ImageOperation(final RenderedImage image, final RenderedImage... others) {
        super(image);
        if (others == null) {
            sources = new RenderedImage[] {image};
        } else {
            sources = new RenderedImage[others.length + 1];
            sources[0] = image;
            System.arraycopy(others, 0, sources, 1, others.length);
            for (int i=1; i<sources.length; i++) {
                ArgumentChecks.ensureNonNullElement("others", i-1, sources[i]);
            }
        }
        colorModel = image.getColorModel();
        width      = image.getWidth();
        height     = image.getHeight();
        minX       = image.getMinX();
        minY       = image.getMinY();
        minTileX   = image.getMinTileX();
        minTileY   = image.getMinTileY();
    }

    /**
     * Creates a new operation with an arbitrary amount of images as the sources.
     * The tile size will be the width and height of the given sample model.
     *
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  colorModel   the color model for all rasters, or {@code null} if unspecified.
     * @param  width        the image width  in pixels, as a strictly positive number.
     * @param  height       the image height in pixels, as a strictly positive number.
     * @param  minX         <var>x</var> coordinate (column) of the pixel in upper-left corner.
     * @param  minY         <var>y</var> coordinate (row) of the pixel in upper-left corner.
     * @param  minTileX     <var>x</var> index of the tile in upper-left corner.
     * @param  minTileY     <var>y</var> index of the tile in upper-left corner.
     * @param  sources      all sources of this image. May be an empty array.
     */
    protected ImageOperation(final SampleModel sampleModel, final ColorModel colorModel,
                             final int width, final int height, final int minX, final int minY,
                             final int minTileX, final int minTileY, RenderedImage... sources)
    {
        super(sampleModel);
        this.colorModel = colorModel;
        this.width      = width;
        this.height     = height;
        this.minX       = minX;
        this.minY       = minY;
        this.minTileX   = minTileX;
        this.minTileY   = minTileY;
        ArgumentChecks.ensureStrictlyPositive("width",  width);
        ArgumentChecks.ensureStrictlyPositive("height", height);
        ArgumentChecks.ensureNonNull("sources", sources);
        sources = sources.clone();
        this.sources = sources;
        for (int i=0; i<sources.length; i++) {
            ArgumentChecks.ensureNonNullElement("sources", i, sources[i]);
        }
    }

    /**
     * Returns the source at the given index.
     *
     * @param  index  index of the desired source.
     * @return source at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    protected final RenderedImage getSource(final int index) {
        return sources[index];
    }

    /**
     * Returns the immediate sources of image data for this image.
     * This method returns the source specified at construction time.
     *
     * @return the immediate sources, or an empty vector is none.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        return new Vector<>(Arrays.asList(sources));
    }

    /**
     * Returns the color model associated with this image (may be null).
     * All rasters returned from this image will have this color model.
     *
     * @return the color model of this image, or {@code null} if unspecified.
     */
    @Override
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Returns the width of this image in pixels.
     * This value is set at construction time.
     *
     * @return the width of this image.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this image in pixels.
     * This value is set at construction time.
     *
     * @return the height of this image.
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     * This value is set at construction time.
     *
     * @return the minimum <var>x</var> coordinate (column) of this image.
     */
    @Override
    public int getMinX() {
        return minX;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     * This value is set at construction time.
     *
     * @return the minimum <var>y</var> coordinate (row) of this image.
     */
    @Override
    public int getMinY() {
        return minY;
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction.
     * This value is set at construction time.
     *
     * @return the minimum tile index in the <var>x</var> direction.
     */
    @Override
    public int getMinTileX() {
        return minTileX;
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction.
     * This value is set at construction time.
     *
     * @return the minimum tile index in the <var>y</var> direction.
     */
    @Override
    public int getMinTileY() {
        return minTileY;
    }
}
