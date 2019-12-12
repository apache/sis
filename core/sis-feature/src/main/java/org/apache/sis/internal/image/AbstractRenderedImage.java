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
package org.apache.sis.internal.image;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public abstract class AbstractRenderedImage implements RenderedImage {

    @Override
    public Vector<RenderedImage> getSources() {
        return new Vector<>();
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public String[] getPropertyNames() {
        return new String[0];
    }

    @Override
    public Raster getData() {
        final SampleModel sm = getSampleModel().createCompatibleSampleModel(getWidth(), getHeight());
        final Raster rasterOut = Raster.createWritableRaster(sm, new Point(getMinX(), getMinY()));

        // Clear dataBuffer to 0 value for all bank
        for (int s = 0; s < rasterOut.getDataBuffer().getSize(); s++) {
            for (int b = 0; b < rasterOut.getDataBuffer().getNumBanks(); b++) {
                rasterOut.getDataBuffer().setElem(b, s, 0);
            }
        }

        for (int y = 0, yn = this.getNumYTiles(); y < yn; y++) {
            for (int x = 0, xn = this.getNumXTiles(); x < xn; x++) {
                final Raster rasterIn = getTile(x, y);
                rasterOut.getSampleModel()
                        .setDataElements(
                                x * this.getTileWidth(),
                                y * this.getTileHeight(),
                                this.getTileWidth(),
                                this.getTileHeight(),
                                rasterIn.getSampleModel().getDataElements(0, 0, this.getTileWidth(), this.getTileHeight(), null, rasterIn.getDataBuffer()),
                                rasterOut.getDataBuffer());
            }
        }

        return rasterOut;
    }

    @Override
    public Raster getData(Rectangle rect) {
        final SampleModel sm = getSampleModel().createCompatibleSampleModel(rect.width, rect.height);
        Raster rasterOut = Raster.createWritableRaster(sm, null);

        // Clear dataBuffer to 0 value for all bank
        for (int s = 0; s < rasterOut.getDataBuffer().getSize(); s++) {
            for (int b = 0; b < rasterOut.getDataBuffer().getNumBanks(); b++) {
                rasterOut.getDataBuffer().setElem(b, s, 0);
            }
        }

        final Point upperLeftPosition = this.getPositionOf(rect.x, rect.y);
        final Point lowerRightPosition = this.getPositionOf(rect.x+rect.width-1, rect.y+rect.height-1);

        for (int y = Math.max(upperLeftPosition.y,0); y < Math.min(lowerRightPosition.y + 1,this.getNumYTiles()); y++) {
            for (int x = Math.max(upperLeftPosition.x,0); x < Math.min(lowerRightPosition.x + 1, this.getNumXTiles()); x++) {
                final Rectangle tileRect = new Rectangle(x * this.getTileWidth(), y * this.getTileHeight(), this.getTileWidth(), this.getTileHeight());

                final int minX, maxX, minY, maxY;
                minX = clamp(rect.x,               tileRect.x, tileRect.x + tileRect.width);
                maxX = clamp(rect.x + rect.width,  tileRect.x, tileRect.x + tileRect.width);
                minY = clamp(rect.y,               tileRect.y, tileRect.y + tileRect.height);
                maxY = clamp(rect.y + rect.height, tileRect.y, tileRect.y + tileRect.height);

                final Rectangle rectIn = new Rectangle(minX, minY, maxX-minX, maxY-minY);
                rectIn.translate(-tileRect.x, -tileRect.y);
                final Rectangle rectOut = new Rectangle(minX, minY, maxX-minX, maxY-minY);
                rectOut.translate(-rect.x, -rect.y);

                if (rectIn.width <= 0 || rectIn.height <= 0 || rectOut.width <= 0 || rectOut.height <= 0){
                    continue;
                }

                final Raster rasterIn = getTile(x, y);

                rasterOut.getSampleModel().setDataElements(rectOut.x, rectOut.y, rectOut.width, rectOut.height,
                        rasterIn.getSampleModel().getDataElements(rectIn.x, rectIn.y, rectIn.width, rectIn.height, null, rasterIn.getDataBuffer()),
                        rasterOut.getDataBuffer());
            }
        }

        if (rect.x != 0 && rect.y != 0) {
            rasterOut = rasterOut.createTranslatedChild(rect.x, rect.y);
        }
        return rasterOut;
    }


    @Override
    public WritableRaster copyData(WritableRaster raster) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Get the tile column and row position for a pixel.
     * Return value can be out of the gridSize
     */
    protected Point getPositionOf(int x, int y){
        final int posX = (int) (Math.floor(x / this.getTileWidth()));
        final int posY = (int) (Math.floor(y / this.getTileHeight()));
        return new Point(posX, posY);
    }

    /**
     * Clamps a value between min value and max value.
     *
     * @param val the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    private static int clamp(int val, int min, int max) {
        return Math.min(Math.max(val, min), max);
    }
}
