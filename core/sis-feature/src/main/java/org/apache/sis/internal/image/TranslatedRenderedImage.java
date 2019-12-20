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

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Vector;


/**
 * Translated RenderedImage implementation.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class TranslatedRenderedImage extends AbstractRenderedImage {

    private final RenderedImage image;
    private final int offsetX;
    private final int offsetY;

    public TranslatedRenderedImage(RenderedImage image, int offsetX, int offsetY) {
        this.image = image;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public Vector<RenderedImage> getSources() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(image);
        return sources;
    }

    @Override
    public Object getProperty(String name) {
        return image.getProperty(name);
    }

    @Override
    public String[] getPropertyNames() {
        return image.getPropertyNames();
    }

    @Override
    public ColorModel getColorModel() {
        return image.getColorModel();
    }

    @Override
    public SampleModel getSampleModel() {
        return image.getSampleModel();
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public int getMinX() {
        return Math.addExact(image.getMinX(), offsetX);
    }

    @Override
    public int getMinY() {
        return Math.addExact(image.getMinY(), offsetY);
    }

    @Override
    public int getNumXTiles() {
        return image.getNumXTiles();
    }

    @Override
    public int getNumYTiles() {
        return image.getNumYTiles();
    }

    @Override
    public int getMinTileX() {
        return image.getMinTileX();
    }

    @Override
    public int getMinTileY() {
        return image.getMinTileY();
    }

    @Override
    public int getTileWidth() {
        return image.getTileWidth();
    }

    @Override
    public int getTileHeight() {
        return image.getTileHeight();
    }

    @Override
    public int getTileGridXOffset() {
        return Math.addExact(image.getTileGridXOffset(), offsetX);
    }

    @Override
    public int getTileGridYOffset() {
        return Math.addExact(image.getTileGridYOffset(), offsetY);
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        final Raster tile = image.getTile(tileX, tileY);
        return tile.createTranslatedChild(
                Math.addExact(tile.getMinX(), offsetX),
                Math.addExact(tile.getMinY(), offsetY));
    }
}
