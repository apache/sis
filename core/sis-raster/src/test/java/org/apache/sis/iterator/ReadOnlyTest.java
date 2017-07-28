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

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opengis.coverage.grid.SequenceType;
import static org.apache.sis.iterator.IteratorTestUtilities.*;
import org.junit.Ignore;

/**
 * Some tests only for Default type iterator.
 *
 * @author Rémi Marechal (Geomatys).
 */
abstract class ReadOnlyTest extends IteratorTest{

    protected ReadOnlyTest() {
    }

    /**
     * Affect expected values in reference table implementation.
     *
     * @param indexCut starting position in {@code tabRef} array
     * @param length new {@code tabRef} length.
     */
    protected abstract void setMoveToRITabs(final int indexCut, final int length);

     /**
     * {@inheritDoc }.
     */
    @Override
    protected void setPixelIterator(Raster raster) {
        pixIterator = PixelIteratorFactory.createReadOnlyIterator(raster);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    protected void setPixelIterator(RenderedImage renderedImage) {
        pixIterator = PixelIteratorFactory.createReadOnlyIterator(renderedImage);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    protected void setPixelIterator(final Raster raster, final Rectangle subArea) {
        pixIterator = PixelIteratorFactory.createReadOnlyIterator(raster, subArea);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    protected void setPixelIterator(RenderedImage renderedImage, Rectangle subArea) {
        pixIterator = PixelIteratorFactory.createReadOnlyIterator(renderedImage, subArea);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    protected void setRasterTest(int minx, int miny, int width, int height, int numband, Rectangle subArea) {
        initTestForRaster(this, minx, miny, width, height, numband, subArea);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    protected void setRenderedImgTest(int minx, int miny, int width, int height, int tilesWidth, int tilesHeight, int numBand, Rectangle areaIterate) {
        initTestForRenderedImage(this, minx, miny, width, height, tilesWidth, tilesHeight, numBand, areaIterate);
    }

    ///////////////////////////////Raster Tests/////////////////////////////////
    /**
     * Test sequence iteration direction.
     */
    @Test
    @Ignore
    public void getIterationDirectionRasterTest() {
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        assertTrue(SequenceType.LINEAR.equals(pixIterator.getIterationDirection()));
    }

    /**
     * Test if getX() getY() iterator methods are conform from raster.
     */
    @Test
    @Ignore
    public void getXYRasterTest() {
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        for (int y = miny; y<miny + height; y++) {
            for (int x = minx; x<minx + width; x++) {
                pixIterator.next();
                assertTrue(pixIterator.getX() == x);
                assertTrue(pixIterator.getY() == y);
                for (int b = 0; b<numBand-1; b++) {
                    pixIterator.next();
                }
            }
        }
    }

    /**
     * Test if iterator transverse expected values from x y coordinates define by moveTo method.
     */
    @Test
    @Ignore
    public void moveToRasterTest() {
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        final int mX = 17;
        final int mY = 15;
        pixIterator.moveTo(mX, mY, 0);
        final int indexCut = ((mY-miny)*width + mX - minx)*numBand;
        final int lenght = width*height*numBand - indexCut;
        setMoveToRITabs(indexCut, lenght);
        int comp = 0;
        do {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        } while (pixIterator.next());
        assertTrue(compareTab());
    }


    ///////////////////////////Rendered Image Tests//////////////////////////////

    /**
     * Test sequence iteration direction in image which contain only one raster.
     */
    @Test
    @Ignore
    public void getIterationDirectionOneRasterTest() {
        minx = 56;
        miny = 1;
        width = 100;
        height = 50;
        tilesWidth = 100;
        tilesHeight = 50;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        assertTrue(SequenceType.LINEAR.equals(pixIterator.getIterationDirection()));
    }

    /**
     * Test sequence iteration direction in image which contain only one raster.
     */
    @Test
    @Ignore
    public void getIterationDirectionMultiRasterTest() {
        minx = 56;
        miny = 1;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        assertTrue(pixIterator.getIterationDirection() == null);
    }

    /**
     * Test if getX() getY() iterator methods are conform from rendered image.
     */
    @Test
    @Ignore
    public void getXYImageTest() {
        minx = 56;
        miny = 1;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        for (int ty = 0; ty<height/tilesHeight; ty++) {
            for (int tx = 0; tx<width/tilesWidth; tx++) {
                for (int y = 0; y<tilesHeight; y++) {
                    for (int x = 0; x<tilesWidth; x++) {
                        pixIterator.next();
                        assertTrue(pixIterator.getX() == tx*tilesWidth+x+minx);
                        assertTrue(pixIterator.getY() == ty*tilesHeight+y+miny);
                        for (int b = 0; b<numBand-1; b++) {
                            pixIterator.next();
                        }
                    }
                }
            }
        }
    }

    /**
     * Test if iterator transverse expected values from x y coordinates define by moveTo method.
     */
    @Test
    @Ignore
    public void moveToRITest() {
        minx = -1;
        miny = 3;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        final int mX = 17;
        final int mY = 15;
        final int ity = (mY-miny) / tilesHeight;
        final int itx = (mX-minx) / tilesWidth;
        pixIterator.moveTo(mX, mY,0);
        final int indexCut = ((((ity*((width/tilesWidth)-1))+itx)*tilesHeight+mY-miny-itx)*tilesWidth + mX-minx)*numBand;
        final int lenght = width*height*numBand - indexCut;
        setMoveToRITabs(indexCut, lenght);
        int comp = 0;
        do {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        } while (pixIterator.next());
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected values from x y coordinates define by moveTo method.
     */
    @Test
    @Ignore
    public void moveToRIUpperLeftTest() {
        minx = -1;
        miny = 3;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        final int mX = -1;
        final int mY = 3;
        final int ity = (mY-miny) / tilesHeight;
        final int itx = (mX-minx) / tilesWidth;
        pixIterator.moveTo(mX, mY, 0);
        final int indexCut = ((((ity*((width/tilesWidth)-1))+itx)*tilesHeight+mY-miny-itx)*tilesWidth + mX-minx)*numBand;
        final int lenght = width*height*numBand - indexCut;
        setMoveToRITabs(indexCut, lenght);
        int comp = 0;
        do {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        } while (pixIterator.next());
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected values from x y coordinates define by moveTo method.
     */
    @Test
    @Ignore
    public void moveToRIUpperRightTest() {
        minx = -1;
        miny = 3;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        final int mX = 98;
        final int mY = 3;
        final int ity = (mY-miny) / tilesHeight;
        final int itx = (mX-minx) / tilesWidth;
        pixIterator.moveTo(mX, mY, 0);
        final int indexCut = ((((ity*((width/tilesWidth)-1))+itx)*tilesHeight+mY-miny-itx)*tilesWidth + mX-minx)*numBand;
        final int lenght = width*height*numBand - indexCut;
        setMoveToRITabs(indexCut, lenght);
        int comp = 0;
        do {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        } while (pixIterator.next());
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected values from x y coordinates define by moveTo method.
     */
    @Test
    @Ignore
    public void moveToRIlowerRightTest() {
        minx = -1;
        miny = 3;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        final int mX = 98;
        final int mY = 52;
        final int ity = (mY-miny) / tilesHeight;
        final int itx = (mX-minx) / tilesWidth;
        pixIterator.moveTo(mX, mY, 0);
        final int indexCut = ((((ity*((width/tilesWidth)-1))+itx)*tilesHeight+mY-miny-itx)*tilesWidth + mX-minx)*numBand;
        final int lenght = width*height*numBand - indexCut;
        setMoveToRITabs(indexCut, lenght);
        int comp = 0;
        do {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        } while (pixIterator.next());
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected values from x y coordinates define by moveTo method.
     */
    @Test
    @Ignore
    public void moveToRIlowerLeftTest() {
        minx = -1;
        miny = 3;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        final int mX = -1;
        final int mY = 52;
        final int ity = (mY-miny) / tilesHeight;
        final int itx = (mX-minx) / tilesWidth;
        pixIterator.moveTo(mX, mY, 0);
        final int indexCut = ((((ity*((width/tilesWidth)-1))+itx)*tilesHeight+mY-miny-itx)*tilesWidth + mX-minx)*numBand;
        final int lenght = width*height*numBand - indexCut;
        setMoveToRITabs(indexCut, lenght);
        int comp = 0;
        do {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        } while (pixIterator.next());
        assertTrue(compareTab());
    }
}

