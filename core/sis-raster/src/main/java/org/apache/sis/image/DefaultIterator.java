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
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.opengis.coverage.grid.SequenceType;


/**
 * An Iterator for traversing anyone rendered Image.
 * <p>
 * Iteration transverse each tiles(raster) from rendered image or raster source one by one in order.
 * Iteration to follow tiles(raster) begin by raster bands, next, raster x coordinates,
 * and to finish raster y coordinates.
 * <p>
 * Iteration follow this scheme :
 * tiles band --&gt; tiles x coordinates --&gt; tiles y coordinates --&gt; next rendered image tiles.
 *
 * Moreover iterator traversing a read-only each rendered image tiles(raster) in top-to-bottom, left-to-right order.
 *
 *
 * /// TODO !!!!!!!!!
 * Code example :
 * {@code
 *                  final PixelIterator dRII = PixelIteratorFactory.create(Iterator(renderedImage, rectangleAreaIterate));
 *                  while (dRII.next()) {
 *                      dRii.getSample();
 *                  }
 * }
 *
 * @author Rémi Marechal       (Geomatys).
 * @author Martin Desruisseaux (Geomatys).
 */
class DefaultIterator extends PixelIterator {

    /**
     * Current X pixel coordinate in this current raster.
     */
    protected int x;

    /**
     * Current Y pixel coordinate in this current raster.
     */
    protected int y;

    /**
     * The X coordinate of the upper-left pixel of this current raster.
     */
    private int minX;

    /**
     * Create raster iterator to follow from minX, minY raster and rectangle intersection coordinate.
     *
     * @param raster will be followed by this iterator.
     * @param subArea {@code Rectangle} which define read iterator area.
     * @throws IllegalArgumentException if subArea don't intersect raster boundary.
     */
    DefaultIterator(final Raster raster, final Rectangle subArea) {
        super(raster, subArea);
        x = minX          = domain.minx;
        y                 = domain.miny;
        currentRasterMaxX = domain.maxX;
        currentRasterMaxY = domain.maxY;
    }

    /**
     * Create default rendered image iterator.
     *
     * @param renderedImage image which will be follow by iterator.
     * @param subArea {@code Rectangle} which represent image sub area iteration.
     * @throws IllegalArgumentException if subArea don't intersect image boundary.
     */
    DefaultIterator(final RenderedImage renderedImage, final Rectangle subArea) {
        super(renderedImage, subArea);
        tileX = timeDomain.minx - 1;
        tileY = timeDomain.miny;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public boolean next() {
        if (++band == numBands) {
            band = 0;
            if (++x == currentRasterMaxX) {
                if (++y == currentRasterMaxY) {
                    if (++tileX == timeDomain.maxX) {
                        tileX = timeDomain.minx;
                        if (++tileY >= timeDomain.maxY) {
                            //-- initialize attribut with expected values to throw exception if another next() is  called.
                            band  = numBands - 1;
                            x     = currentRasterMaxX - 1;
                            y     = currentRasterMaxY - 1;
                            tileX = timeDomain.maxY   - 1;
                            if ((tileY - 1) >= timeDomain.maxY) {
                                //-- at first out tY == tMaxY and with another next() tY = tMaxY + 1.
                                throw new IllegalStateException("Out of raster boundary. Illegal next call, you should rewind iterator first.");
                            }
                            return false;
                        }
                    }
                    //initialize from new tile(raster).
                    updateCurrentRaster(tileX, tileY);
                }
                x = minX;
            }
        }
        return true;
    }

    /**
     * Update current raster from tiles array coordinates.
     *
     * @param tileX current X coordinate from rendered image tiles array.
     * @param tileY current Y coordinate from rendered image tiles array.
     */
    protected void updateCurrentRaster(int tileX, int tileY) {
        //-- update traveled raster
        this.currentRaster = this.image.getTile(tileX, tileY);

        //-- update needed attibut to iter
        final int cRMinX       = this.currentRaster.getMinX();
        final int cRMinY       = this.currentRaster.getMinY();
        this.minX = this.x     = Math.max(domain.minx, cRMinX);
        this.y                 = Math.max(domain.miny, cRMinY);
        this.currentRasterMaxX = Math.min(domain.maxX, cRMinX + tileWidth);
        this.currentRasterMaxY = Math.min(domain.maxY, cRMinY + tileHeight);
        if (currentRaster.getNumBands() != numBands) {
            throw new RasterFormatException("Mismatched number of bands.");
        }
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getX() {
        return x;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getY() {
        return y;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getSample() {
        return currentRaster.getSample(x, y, band);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public float getSampleFloat() {
        return currentRaster.getSampleFloat(x, y, band);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public double getSampleDouble() {
        return currentRaster.getSampleDouble(x, y, band);
    }

    @Override
    public void moveTo(final int x, final int y, final int b) {
        super.moveTo(x, y, b);
        if (image != null) {
            final int riMinX = image.getMinX();
            final int riMinY = image.getMinY();
            final int tmpTX = (x - riMinX) / tileWidth  + image.getMinTileX();
            final int tmpTY = (y - riMinY) / tileHeight + image.getMinTileY();
            if (tmpTX != tileX || tmpTY != tileY) {
                tileX = tmpTX;
                tileY = tmpTY;
                updateCurrentRaster(tileX, tileY);
            }
        }
        this.x = x;
        this.y = y;
        this.band = b;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public SequenceType getIterationDirection() {
        if (image == null) return SequenceType.LINEAR;//1 raster seul
        if (image.getNumXTiles() <=1 && image.getNumYTiles() <= 1)
            return SequenceType.LINEAR;
        return null;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void rewind() {
        if (image == null) {
            band = -1; x = minX; y = domain.miny;
            tileX = tileY = 0;
//          tMaxX = tMaxY = 1;
        } else {
            x    = y    = band    = 0;
            currentRasterMaxX = currentRasterMaxY = 1;
            tileX   = timeDomain.minx - 1;
            tileY   = timeDomain.miny;
        }
    }
}
