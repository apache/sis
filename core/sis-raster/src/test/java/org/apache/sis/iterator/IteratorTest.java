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
import java.awt.image.WritableRaster;
import org.apache.sis.test.TestCase;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Reading-only tests from any PixelIterator.
 *
 * @author Rémi Maréchal (Geomatys).
 */
public abstract class IteratorTest extends TestCase{

    /**
     * {@code PixelIterator} which will be tested.
     */
    protected PixelIterator pixIterator;

    /**
     * Raster use to test {@code PixelIterator}.
     */
    protected WritableRaster rasterTest;

    /**
     * Rendered Image use to test {@code PixelIterator}.
     */
    protected IteratorTestImage renderedImage;

    /**
     * X coordinate from upper left corner {@link #rasterTest} or {@link #renderedImage}.
     */
    protected int minx;

    /**
     * Y coordinate from upper left corner {@link #rasterTest} or {@link #renderedImage}.
     */
    protected int miny;

    /**
     * {@link #rasterTest} or {@link #renderedImage} width.
     */
    protected int width;

    /**
     * {@link #rasterTest} or {@link #renderedImage} height.
     */
    protected int height;

    /**
     * {@link #rasterTest} or {@link #renderedImage} band number.
     */
    protected int numBand;

    /**
     * {@link #renderedImage} tiles width.
     */
    protected int tilesWidth;

    /**
     * {@link #renderedImage} tiles height.
     */
    protected int tilesHeight;


    public IteratorTest() {
    }

    /**
     * Affect an appropriate {@code Raster} on {@link #rasterTest} attribute relative to expected test.
     *
     * @param minx    {@link #rasterTest} X coordinate from upper left corner.
     * @param miny    {@link #rasterTest} Y coordinate from upper left corner.
     * @param width   {@link #rasterTest} width.
     * @param height  {@link #rasterTest} height.
     * @param numBand {@link #rasterTest} band number.
     * @param subArea {@code Rectangle} which represent {@link #rasterTest} sub area iteration.
     */
    protected abstract void setRasterTest(int minx, int miny, int width, int height, int numBand, Rectangle subArea);

    /**
     * Affect an appropriate {@code PixelIterator} on {@link #pixIterator} attribute, relative to expected test.
     *
     * @param raster {@code Raster} which will be followed by {@link #pixIterator}.
     */
    protected abstract void setPixelIterator(Raster raster);

    /**
     * Affect an appropriate {@code PixelIterator} on {@link #pixIterator} attribute, relative to expected test.
     *
     * @param renderedImage {@code RenderedImage} which will be followed by {@link #pixIterator}.
     */
    protected abstract void setPixelIterator(RenderedImage renderedImage);

    /**
     * Affect an appropriate {@code PixelIterator} on {@link #pixIterator} attribute, relative to expected test.
     *
     * @param raster  {@code Raster} which will be followed by {@link #pixIterator}.
     * @param subArea {@code Rectangle} which represent {@link #rasterTest} sub area iteration.
     */
    protected abstract void setPixelIterator(final Raster raster, final Rectangle subArea);

    /**
     * Affect an appropriate {@code PixelIterator} on {@link #pixIterator} attribute, relative to expected test.
     *
     * @param renderedImage {@code RenderedImage} which will be followed by {@link #pixIterator}.
     * @param subArea {@code Rectangle} which represent {@link #renderedImage} sub area iteration.
     */
    protected abstract void setPixelIterator(final RenderedImage renderedImage, final Rectangle subArea);

    /**
     * Fill table use to valid tests.
     * Affect value at index in test table.
     *
     * @param index table index.
     * @param value to insert in test table.
     */
    protected abstract void setTabTestValue(final int index, final double value);

    /**
     * Compare two table.
     *
     * Each PixelIterator test have two tables.
     * Table witch contains expected values and another table contains {@code PixelIterator} iteration result.
     * Test is validate if two table contains same values.
     *
     * @return true if these table are equals else false.
     */
    protected abstract boolean compareTab();

    /**
     * Affect an appropriate {@code RenderedImage} on {@link #renderedImage} attribute relative to expected test.
     *
     * @param minx        {@link #renderedImage} X coordinate from upper left corner.
     * @param miny        {@link #renderedImage} Y coordinate from upper left corner.
     * @param width       {@link #renderedImage} width.
     * @param height      {@link #renderedImage} height.
     * @param tilesWidth  {@link #renderedImage} tiles width.
     * @param tilesHeight {@link #renderedImage} tiles height.
     * @param numBand     {@link #renderedImage} band number.
     * @param areaIterate     {@code Rectangle} which represent {@link #renderedImage} sub area iteration.
     */
    protected abstract void setRenderedImgTest(int minx, int miny, int width, int height,
                        int tilesWidth, int tilesHeight, int numBand, Rectangle areaIterate);

    /**
     * Return {@link #renderedImage} or {@link #rasterTest} data type.
     *
     * @return {@link #renderedImage} or {@link #rasterTest} data type.
     */
    protected abstract int getDataBufferType();

    /**
     * Create appropriate table about "writable" tests.
     *
     * @param length tests tables length.
     */
    protected abstract void createTable(int length);

    /**
     * Fill reference table use to valid tests.
     * Affect value at index in test table.
     *
     * @param index table index.
     * @param value to insert in reference table.
     */
    protected abstract void setTabRefValue(int index, double value);

////////////////////////////////////Raster tests/////////////////////////////////
    /**
     * Test if iterator transverse all raster positions with different minX and maxY coordinates.
     * Also test rewind function.
     */
    @Test
    public void differentMinRasterReadTest() {
        width = 16;
        height = 16;
        minx = 0;
        miny = 0;
        numBand = 3;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        minx = 3;
        minx = 5;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        minx = -3;
        miny = 5;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        minx = 3;
        miny = -5;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        minx = -3;
        miny = -5;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        pixIterator.rewind();
        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on upper left raster corner.
     */
    @Test
    @Ignore
    public void rectUpperLeftRasterReadTest() {
        final Rectangle subArea = new Rectangle(4, 6, 5, 4);
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, subArea);
        setPixelIterator(rasterTest, subArea);
        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on upper right raster corner.
     */
    @Test
    @Ignore
    public void rectUpperRightRasterReadTest() {
        final Rectangle subArea = new Rectangle(16, 6, 10, 6);
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, subArea);
        setPixelIterator(rasterTest, subArea);
        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on lower right raster corner.
     */
    @Test
    @Ignore
    public void rectLowerRightRasterReadTest() {
        final Rectangle subArea = new Rectangle(14, 20, 15, 9);
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, subArea);
        setPixelIterator(rasterTest, subArea);
        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on lower left raster corner.
     */
    @Test
    @Ignore
    public void rectLowerLeftRasterReadTest() {
        final Rectangle subArea = new Rectangle(2, 12, 10, 6);
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, subArea);
        setPixelIterator(rasterTest, subArea);
        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is within raster area.
     */
    @Test
    @Ignore
    public void rasterContainsRectReadTest() {
        final Rectangle subArea = new Rectangle(10, 9, 8, 6);
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, subArea);
        setPixelIterator(rasterTest, subArea);
        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        pixIterator.rewind();
        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area contains all raster area.
     */
    @Test
    @Ignore
    public void rectContainsRasterReadTest() {
        final Rectangle subArea = new Rectangle(2, 3, 25, 17);
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, subArea);
        setPixelIterator(rasterTest, subArea);
        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test catching exception with x, y moveTo method coordinates out of raster boundary.
     */
    @Test
    @Ignore
    public void unappropriateMoveToRasterTest() {
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, null);
        setPixelIterator(rasterTest);
        try{
            pixIterator.moveTo(2, 3, 0);
            Assert.fail("test should had failed");
        }catch(Exception e){
            //ok
        }

        try{
            pixIterator.moveTo(9, 10, -1);
            Assert.fail("test should had failed");
        }catch(Exception e){
            //ok
        }

        try{
            pixIterator.moveTo(9, 10, 500);
            Assert.fail("test should had failed");
        }catch(Exception e){
            //ok
        }
    }

    /**
     * Test catching exception with rectangle which don't intersect raster area.
     */
    @Test
    public void unappropriateRectRasterTest() {
        final Rectangle subArea = new Rectangle(-17, -20, 5, 15);
        numBand = 3;
        width = 16;
        height = 16;
        minx = 5;
        miny = 7;
        setRasterTest(minx, miny, width, height, numBand, subArea);
        try{
            setPixelIterator(rasterTest, subArea);
            Assert.fail("test should had failed");
        }catch(Exception e){
            //ok
        }
    }


//////////////////////////////Rendered image tests/////////////////////////////////


    /**
     * Test if iterator transverse all raster positions with different minX and maxY coordinates.
     * Also test rewind function.
     */
    @Test
    @Ignore
    public void transversingAllReadTest() {
        minx = 0;
        miny = 0;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, null);
        setPixelIterator(renderedImage);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        minx = 1;
        miny = -50;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, null);
        setPixelIterator(renderedImage);

        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());

        pixIterator.rewind();
        comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined within upper left renderedImage tile.
     */
    @Test
    @Ignore
    public void rectUpperLeftWithinTileTest() {
        final Rectangle rect = new Rectangle(-10, -20, 10, 30);
        minx = -5;
        miny = 5;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined within upper right renderedImage tile.
     */
    @Test
    @Ignore
    public void rectUpperRightWithinTileTest() {
        final Rectangle rect = new Rectangle(90, -20, 30, 31);
        minx = -5;
        miny = 7;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined within lower right renderedImage tile.
     */
    @Test
    @Ignore
    public void rectLowerRightWithinTileTest() {
        final Rectangle rect = new Rectangle(97, 40, 50, 50);
        minx = 5;
        miny = -7;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined within lower left renderedImage tile.
     */
    @Test
    @Ignore
    public void rectLowerLeftWithinTileTest() {
        final Rectangle rect = new Rectangle(0, 34, 5, 50);
        minx = 2;
        miny = -15;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is within image tile.
     */
    @Test
    @Ignore
    public void imageContainsRectWithinTileTest() {
        final Rectangle rect = new Rectangle(16, 18, 8, 3);
        minx = -5;
        miny = 7;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on upper left rendered image corner.
     */
    @Test
    @Ignore
    public void rectUpperLeftTest() {
        final Rectangle rect = new Rectangle(-10, -20, 40, 30);
        minx = -5;
        miny = 5;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on upper right rendered image corner.
     */
    @Test
    @Ignore
    public void rectUpperRightTest() {
        final Rectangle rect = new Rectangle(80, -20, 30, 50);
        minx = 0;
        miny = 0;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on lower right rendered image corner.
     */
    @Test
    @Ignore
    public void rectLowerRightTest() {
        final Rectangle rect = new Rectangle(80, 30, 50, 50);
        minx = 0;
        miny = 0;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is defined on lower left rendered image corner.
     */
    @Test
    @Ignore
    public void rectLowerLeftTest() {
        final Rectangle rect = new Rectangle(-20, 30, 50, 50);
        minx = 0;
        miny = 0;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area is within image area.
     */
    @Test
    @Ignore
    public void imageContainsRectTest() {
        final Rectangle rect = new Rectangle(20, 10, 10, 10);
        minx = -5;
        miny = 7;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test if iterator transverse expected value in define area.
     * Area contains all image area.
     */
    @Test
    @Ignore
    public void rectContainsImageTest() {
        final Rectangle rect = new Rectangle(-10, -10, 150, 80);
        minx = 0;
        miny = 0;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        setPixelIterator(renderedImage, rect);

        int comp = 0;
        while (pixIterator.next()) {
            setTabTestValue(comp++, pixIterator.getSampleDouble());
        }
        assertTrue(compareTab());
    }

    /**
     * Test catching exception with rectangle which don't intersect raster area.
     */
    @Test
    @Ignore
    public void unappropriateRectRITest() {
        final Rectangle rect = new Rectangle(-100, -50, 5, 17);
        minx = 0;
        miny = 0;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, 3, rect);
        try{
            setPixelIterator(renderedImage, rect);
            Assert.fail("test should had failed");
        }catch(IllegalArgumentException e){
            //ok
        }
    }

    /**
     * Test catching exception with x, y moveTo method coordinates out of raster boundary.
     */
    @Test
    @Ignore
    public void unappropriateMoveToRITest() {
        minx = 0;
        miny = 0;
        width = 100;
        height = 50;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        try{
            pixIterator.moveTo(102, 53, 0);
            Assert.fail("test should had failed");
        }catch(IllegalArgumentException e){
            //ok
        }
    }

    /**
     * Test catching exception with another next() method call, after have already traveled all the image.
     */
    @Test
    @Ignore
    public void unappropriateMultiNextCallTest() {
        minx = 0;
        miny = 0;
        width = 40;
        height = 20;
        tilesWidth = 10;
        tilesHeight = 5;
        numBand = 3;
        setRenderedImgTest(minx, miny, width, height, tilesWidth, tilesHeight, numBand, null);
        setPixelIterator(renderedImage);
        while (pixIterator.next()) {
            //-- do nothing, we want go to the end of the iteration
        }
        try {
            pixIterator.next();
            Assert.fail("test should had failed");
        } catch (IllegalStateException e) {
            //ok
        }
    }



    /**
     * Test about getNumBands methods
     */
    public void numBandsTest() {
        assertTrue(pixIterator.getNumBands() == 1);
    }

    /**
     * Compare 2 integer table.
     *
     * @param tabA table resulting raster iterate.
     * @param tabB table resulting raster iterate.
     * @return true if tables are identical.
     */
    protected boolean compareTab(int[] tabA, int[] tabB) {
        int length = tabA.length;
        if (length != tabB.length) return false;
        for (int i = 0; i<length; i++) {
            if (tabA[i] != tabB[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare 2 double table.
     *
     * @param tabA table resulting raster iterate.
     * @param tabB table resulting raster iterate.
     * @return true if tables are identical.
     */
    protected boolean compareTab(double[] tabA, double[] tabB) {
        int length = tabA.length;
        if (length != tabB.length) return false;
        for (int i = 0; i<length; i++) {
            if (tabA[i] != tabB[i]) return false;
        }
        return true;
    }

    /**
     * Compare 2 float table.
     *
     * @param tabA table resulting raster iterate.
     * @param tabB table resulting raster iterate.
     * @return true if tables are identical.
     */
    protected boolean compareTab(float[] tabA, float[] tabB) {
        int length = tabA.length;
        if (length != tabB.length) return false;
        for (int i = 0; i<length; i++) {
            if (tabA[i] != tabB[i]) return false;
        }
        return true;
    }

    /**
     * Compare 2 short table.
     *
     * @param tabA table resulting raster iterate.
     * @param tabB table resulting raster iterate.
     * @return true if tables are identical.
     */
    protected boolean compareTab(short[] tabA, short[] tabB) {
        int length = tabA.length;
        if (length != tabB.length) return false;
        for (int i = 0; i<length; i++) {
            if (tabA[i] != tabB[i]) return false;
        }
        return true;
    }

    /**
     * Compare 2 byte table.
     *
     * @param tabA table resulting raster iterate.
     * @param tabB table resulting raster iterate.
     * @return true if tables are identical.
     */
    protected boolean compareTab(byte[] tabA, byte[] tabB) {
        int length = tabA.length;
        if (length != tabB.length) return false;
        for (int i = 0; i<length; i++) {
            if (tabA[i] != tabB[i]) return false;
        }
        return true;
    }
}
