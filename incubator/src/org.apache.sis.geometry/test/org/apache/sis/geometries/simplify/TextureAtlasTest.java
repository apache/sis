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
package org.apache.sis.geometries.simplify;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.grid.GridExtent;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TextureAtlasTest {

    /**
     * Test atlas image creation.
     */
    @Test
    public void atlasCreationTest() {

        final TextureAtlas atlas = new TextureAtlas(10, 10, 0);

        { //add image too big for atlas
            final BufferedImage image1 = createImage(30, 5, Color.BLACK);
            assertEquals("image1", atlas.add("image1", image1));
            assertEquals(1, atlas.getPages().size());
            assertEquals(0, atlas.getPageIndex("image1"));
            GridExtent extent = atlas.getPages().get(0).index().get("image1");
            assertEquals(new GridExtent(30, 5), extent);
            assertEquals(atlas.getPages().get(0).image(), image1);
        }

        { //add small images
            final BufferedImage image2 = createImage(5, 4, Color.BLUE);
            assertEquals("image2", atlas.add("image2", image2));
            assertEquals(2, atlas.getPages().size());
            assertEquals(1, atlas.getPageIndex("image2"));
            GridExtent extent = atlas.getPages().get(1).index().get("image2");
            assertEquals(new GridExtent(null, new long[]{0,0}, new long[]{5,4}, false), extent);

            //image3 must be placed under image2
            final BufferedImage image3 = createImage(7, 3, Color.YELLOW);
            assertEquals("image3", atlas.add("image3", image3));
            assertEquals(2, atlas.getPages().size());
            assertEquals(1, atlas.getPageIndex("image3"));
            extent = atlas.getPages().get(1).index().get("image3");
            assertEquals(new GridExtent(null, new long[]{0,4}, new long[]{7,4+3}, false), extent);

            //image4 must be placed on the right of image2
            final BufferedImage image4 = createImage(4, 2, Color.GREEN);
            assertEquals("image4", atlas.add("image4", image4));
            assertEquals(2, atlas.getPages().size());
            assertEquals(1, atlas.getPageIndex("image4"));
            extent = atlas.getPages().get(1).index().get("image4");
            assertEquals(new GridExtent(null, new long[]{5,0}, new long[]{5+4,2}, false), extent);

            //image5 must be placed on a new page
            final BufferedImage image5 = createImage(1, 9, Color.RED);
            assertEquals("image5", atlas.add("image5", image5));
            assertEquals(3, atlas.getPages().size());
            assertEquals(2, atlas.getPageIndex("image5"));
            extent = atlas.getPages().get(2).index().get("image5");
            assertEquals(new GridExtent(null, new long[]{0,0}, new long[]{1,9}, false), extent);

            //check images
            final RenderedImage page1 = atlas.getPages().get(1).image();
            final RenderedImage page2 = atlas.getPages().get(2).image();

            assertEquals(Color.BLUE, getPixel(page1, 0, 0));
            assertEquals(Color.BLUE, getPixel(page1, 4, 3));
            assertEquals(Color.YELLOW, getPixel(page1, 0, 4));
            assertEquals(Color.YELLOW, getPixel(page1, 6, 6));
            assertEquals(Color.GREEN, getPixel(page1, 5, 0));
            assertEquals(Color.GREEN, getPixel(page1, 8, 1));
            assertEquals(Color.RED, getPixel(page2, 0, 0));
            assertEquals(Color.RED, getPixel(page2, 0, 8));
        }

        { //add image with al existing name, must raise an exception
            final BufferedImage image4 = createImage(8, 5, Color.WHITE);
            try {
                atlas.add("image4", image4);
                fail("image4 already exist, should have caused an exception");
            } catch (IllegalArgumentException ex) {
                //ok
            }
        }
    }

    private static BufferedImage createImage(int width, int height, Color color) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private static Color getPixel(RenderedImage image, int x, int y) {
        return new Color(((BufferedImage) image).getRGB(x, y));
    }

}
