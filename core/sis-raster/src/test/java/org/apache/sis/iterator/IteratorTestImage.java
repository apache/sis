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
package org.apache.sis.iterator;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.util.Vector;

/**
 *
 * @author Remi Marechal (Geomatys).
 */
public class IteratorTestImage implements WritableRenderedImage {

    private final int minx;
    private final int miny;
//    private final int maxX;
//    private final int maxY;
    private final int width;
    private final int height;
    private final Dimension tileSize;
    private final SampleModel sm;
    private final int numxtile;
    private final int numytile;
    private final int tgXo;
    private final int tgYo;
    private final int minTx;
    private final int minTy;

    private final WritableRaster[] tiles;

    public IteratorTestImage(final int minx, final int miny, final int width, final int height,
            final int tilesWidth, final int tilesHeight, final int tileGridXOffset, final int tileGridYOffset,
            final SampleModel sampleM) {

        this.minx = minx;
        this.miny = miny;
        this.width = width;
        this.height = height;
        this.tileSize = new Dimension(tilesWidth, tilesHeight);
        numxtile = (int) Math.ceil(width / (double) tilesWidth);
        numytile = (int) Math.ceil(height / (double) tilesHeight);
        sm = sampleM;
        tgXo = tileGridXOffset;
        tgYo = tileGridYOffset;
        tiles = new WritableRaster[numxtile * numytile];
        minTx = (int) Math.floor((tgXo - minx) / (double)tileSize.width);
        minTy = (int) Math.floor((tgYo - miny) / (double)tileSize.height);
    }



    @Override
    public ColorModel getColorModel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SampleModel getSampleModel() {
        return sm;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getMinX() {
        return minx;
    }

    @Override
    public int getMinY() {
        return miny;
    }

    @Override
    public int getNumXTiles() {
        return tileSize.width;
    }

    @Override
    public int getNumYTiles() {
        return numxtile;
    }

    @Override
    public int getMinTileX() {
        return minTx;
    }

    @Override
    public int getMinTileY() {
        return minTy;
    }

    @Override
    public int getTileWidth() {
        return tileSize.width;
    }

    @Override
    public int getTileHeight() {
        return tileSize.height;
    }

    @Override
    public int getTileGridXOffset() {
        return tgXo;
    }

    @Override
    public int getTileGridYOffset() {
        return tgYo;
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        return getWritableTile(tileX, tileY);
    }

    @Override
    public Raster getData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Raster getData(Rectangle rect) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WritableRaster getWritableTile(int tileX, int tileY) {
        final int id = (tileY - minTy) * numxtile + tileX - minTx;
        if (id == -22) {
            System.out.println("");
        }
        if (tiles[id] == null)
            tiles[id] = Raster.createInterleavedRaster(sm.getDataType(),
                    tileSize.width, tileSize.height,
                    sm.getNumBands(),
                    new Point(tgXo + tileX * tileSize.width, tgYo + tileY * tileSize.height));
        return tiles[id];
    }

    @Override
    public void releaseWritableTile(int tileX, int tileY) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isTileWritable(int tileX, int tileY) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Point[] getWritableTileIndices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasTileWriters() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setData(Raster r) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<RenderedImage> getSources() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getProperty(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] getPropertyNames() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WritableRaster copyData(WritableRaster raster) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public void addTileObserver(TileObserver to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeTileObserver(TileObserver to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setSample(final int x, final int y, final int b, final double value) {
        final WritableRaster wr = getTileFromPixCoords(x, y);
        wr.setSample(x, y, b, value);
    }

    private WritableRaster getTileFromPixCoords(final int x, final int y) {
        final int tx = Math.floorDiv((x - tgXo), tileSize.width);
        final int ty = Math.floorDiv((y - tgYo) , tileSize.height);
        return getWritableTile(tx, ty);
    }
}
