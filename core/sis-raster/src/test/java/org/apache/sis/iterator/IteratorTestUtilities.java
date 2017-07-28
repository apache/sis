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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;

/**
 *
 * @author rmarechal
 */
class IteratorTestUtilities {


    static void initTestForRenderedImage(IteratorTest test, int minx, int miny, int width, int height, int tilesWidth, int tilesHeight, int numBand, Rectangle areaIterate) {
        final int dataType = test.getDataBufferType();
        final SampleModel sampleM = new PixelInterleavedSampleModel(dataType, tilesWidth, tilesHeight,numBand,tilesWidth*numBand, new int[]{0,1,2});
        test.renderedImage = new IteratorTestImage(minx, miny, width, height,tilesWidth, tilesHeight,  minx+tilesWidth, miny+tilesHeight, sampleM);
        double comp;
        int nbrTX = width/tilesWidth;
        int nbrTY = height/tilesHeight;
        double valueRef = (dataType == DataBuffer.TYPE_FLOAT) ? -200.5 : 0;
        comp = valueRef;
        for(int j = 0;j<nbrTY;j++){
            for(int i = 0; i<nbrTX;i++){
                for (int y = miny+j*tilesHeight, ly = y+tilesHeight; y<ly; y++) {
                    for (int x = minx+i*tilesWidth, lx = x + tilesWidth; x<lx; x++) {
                        for (int b = 0; b<numBand; b++) {
                            test.renderedImage.setSample(x, y, b, comp++);
                        }
                    }
                }
            }
        }

        ////////////////////remplir tabRef/////////////////

        int tX, tY, tMaxX, tMaxY;
        int areaMinX = 0, areaMinY = 0, areaMaxX = 0, areaMaxY = 0;
        int tabLength;

        if (areaIterate != null) {
            //iteration area boundary
            areaMinX = Math.max(minx, areaIterate.x);
            areaMinY = Math.max(miny, areaIterate.y);
            areaMaxX = Math.min(minx+width, areaIterate.x+areaIterate.width);
            areaMaxY = Math.min(miny+height, areaIterate.y+areaIterate.height);
            tabLength = (areaMaxX - areaMinX) * (areaMaxY - areaMinY) * numBand;

            //iteration tiles index
            tX = (areaMinX-minx)/tilesWidth;
            tY = (areaMinY-miny)/tilesHeight;
            tMaxX = (areaMaxX-minx)/tilesWidth;
            tMaxY = (areaMaxY-miny)/tilesHeight;
            if (tMaxX == width/tilesWidth) tMaxX--;
            if (tMaxY == height/tilesHeight) tMaxY--;
        } else {
            tX = tY = 0;
            tMaxX = width/tilesWidth - 1;
            tMaxY = height/tilesHeight - 1;
            tabLength = width * height * numBand;
        }

        //test table creation
        test.createTable(tabLength);

        int rasterMinX, rasterMinY, rasterMaxX, rasterMaxY, depX, depY, endX, endY;

        comp = 0;
        for (;tY <= tMaxY; tY++) {
            for (int tx = tX;tx<=tMaxX; tx++) {

                //iteration area from each tile
                rasterMinX = minx+tx*tilesWidth;
                rasterMinY = miny+tY*tilesHeight;
                rasterMaxX = rasterMinX + tilesWidth;
                rasterMaxY = rasterMinY + tilesHeight;

                //iteration area
                if (areaIterate != null) {
                    depX = Math.max(rasterMinX, areaMinX);
                    depY = Math.max(rasterMinY, areaMinY);
                    endX = Math.min(rasterMaxX, areaMaxX);
                    endY = Math.min(rasterMaxY, areaMaxY);
                } else {
                    depX = rasterMinX;
                    depY = rasterMinY;
                    endX = rasterMaxX;
                    endY = rasterMaxY;
                }


                for (;depY < endY; depY++) {
                    for (int x = depX; x < endX; x++) {
                        for (int b = 0;b<numBand;b++) {
                            test.setTabRefValue((int)comp++, valueRef + b + (x-rasterMinX)*numBand + (depY-rasterMinY)*tilesWidth*numBand + tx*tilesHeight*tilesWidth*numBand + tY*(width/tilesWidth)*tilesHeight*tilesWidth*numBand);
                        }
                    }
                }
            }
        }
    }

    /**
     * Initialize given Iterator test for Raster test.
     * This method initialize Internal tested {@linkplain IteratorTest#rasterTest raster} and also test table reference.
     *
     * @param test
     * @param minx
     * @param miny
     * @param width
     * @param height
     * @param numband
     * @param subArea
     */
    static void initTestForRaster(IteratorTest test, int minx, int miny, int width, int height, int numband, Rectangle subArea) {
        final int dataType = test.getDataBufferType();
        double valueRef;
        switch (dataType) {
            case DataBuffer.TYPE_FLOAT : valueRef = -2000.5;break;
            default : valueRef = 0;break;
        }
        SampleModel sampleM = new PixelInterleavedSampleModel(dataType, width, width, numband, width*numband, new int[]{0, 1, 2});
        test.rasterTest = Raster.createWritableRaster(sampleM, new Point(minx, miny));
        double comp = valueRef;
        for (int y = miny; y<miny + height; y++) {
            for (int x = minx; x<minx + width; x++) {
                for (int b = 0; b<numband; b++) {
                    test.rasterTest.setSample(x, y, b, comp++);
                }
            }
        }
        int mx, my, w,h;
        if (subArea == null) {
            mx = minx;
            my = miny;
            w = width;
            h = height;

        } else {
            mx = Math.max(minx, subArea.x);
            my = Math.max(miny, subArea.y);
            w  = Math.min(minx + width, subArea.x + subArea.width) - mx;
            h  = Math.min(miny + height, subArea.y + subArea.height) - my;
        }
        final int length = w * h * numband;
        test.createTable(length);
        comp = 0;
        for (int y = my; y<my + h; y++) {
            for (int x = mx; x<mx + w; x++) {
                for (int b = 0; b<numband; b++) {
                    test.setTabRefValue((int) comp++,  b + numband * ((x-minx) + (y-miny) * width) + valueRef);
                }
            }
        }
    }
}
