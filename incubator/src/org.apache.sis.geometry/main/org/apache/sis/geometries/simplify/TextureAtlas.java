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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.util.ArgumentChecks;


/**
 *
 * Creation algorithm from :
 * http://www.blackpawn.com/texts/lightmaps/
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TextureAtlas {

    private final int pageWidth;
    private final int pageHeight;
    private final int padding;
    private final List<Page> pages = new ArrayList<>();

    public TextureAtlas(int pageWidth, int pageHeight, int padding) {
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.padding = padding;
    }

    /**
     * @return Pages in the atlas.
     */
    public List<Page> getPages() {
        return Collections.unmodifiableList(pages);
    }

    /**
     * Get page where image is stored.
     * @param name searched image name, not null
     * @return Page or null if not found
     */
    public Page getPage(String name) throws IllegalArgumentException{
        return pages.get(getPageIndex(name));
    }

    /**
     * Get page where image is stored.
     * @param name searched image name, not null
     * @return Page or null if not found
     */
    public int getPageIndex(String name) throws IllegalArgumentException{
        for (int i = 0, n = pages.size(); i < n; i++) {
            if (pages.get(i).index.containsKey(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Image " + name + " is not stored in this atlas.");
    }

    /**
     * Insert image in the atlas.
     * If the image is larger then atlas pages,
     * a new page with the image size will be created.
     *
     * @param name user defined name or null, if null a new one will be created
     * @param image not null
     * @return image name
     */
    public String add(String name, RenderedImage image) {
        ArgumentChecks.ensureNonNull("image", image);
        if (name == null) name = UUID.randomUUID().toString();

        //ensure name is not used
        for (Page p : pages) {
            if (p.index.containsKey(name)) {
                throw new IllegalArgumentException("Name " + name + " is already used in the atlas");
            }
        }

        if (image.getWidth() > pageWidth || image.getHeight() > pageHeight) {
            //big image are added to their own page
            final Page page = new Page(name, image);
            pages.add(page);
            return name;
        } else {
            //search a page where to store the image
            for (Page page : pages) {
                if (page.tryInsert(name, image)) {
                    return name;
                }
            }

            //create a new page
            final Page page = new Page();
            page.tryInsert(name, image);
            pages.add(page);
            return name;
        }
    }

    /**
     * Atlas page.
     */
    public final class Page {
        private final RenderedImage image;
        private final Node tree;
        private final Map<String,GridExtent> index = new HashMap<>();

        Page() {
            this.image = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_ARGB);
            this.tree = new Node(0, 0, pageWidth, pageHeight);
        }

        /**
         * Single image page.
         * @param name
         * @param image
         */
        Page(String name, RenderedImage image) {
            this.image = image;
            this.tree = new Node(name, image);
            this.index.put(name, tree.extent);
        }

        public Map<String,GridExtent> index() {
            return Collections.unmodifiableMap(index);
        }

        public RenderedImage image() {
            return image;
        }

        private boolean tryInsert(String name, RenderedImage image) {
            final Node node = tree.tryInsert(name, image);

            if (node == null) {
                return false;
            }

            final Graphics2D graphics = ((BufferedImage) this.image).createGraphics();
            graphics.drawRenderedImage(image, new AffineTransform(1, 0, 0, 1, node.extent.getLow(0), node.extent.getLow(1)));
            graphics.dispose();
            index.put(name, node.extent);
            return true;
        }
    }

    private final class Node {

        public final GridExtent extent;
        public Node child0;
        public Node child1;
        public String name;

        public Node(long x, long y, long width, long height) {
            extent = new GridExtent(null, new long[]{x,y}, new long[]{x+width, y+height}, false);
        }

        /**
         * Single image node.
         */
        public Node(String name, RenderedImage image) {
            this.extent = new GridExtent(image.getWidth(), image.getHeight());
            this.name = name;
        }

        public Node tryInsert(String name, RenderedImage image) {
            if (child0 != null || child1 != null) {
                // try inserting into first child
                Node newNode = child0.tryInsert(name, image);
                if (newNode != null) {
                    return newNode;
                }
                // no room, insert into second
                return child1.tryInsert(name, image);
            } else {
                final int imgWidth = image.getWidth();
                final int imgHeight = image.getHeight();
                final long x = extent.getLow(0);
                final long y = extent.getLow(1);
                final long width = extent.getSize(0);
                final long height = extent.getSize(1);
                if (this.name != null) {
                    // if there's already a image here, return
                    return null;
                } else if (imgWidth > width || imgHeight > height) {
                    // if we're too small, return
                    return null;
                } else if (imgWidth == width && imgHeight == height) {
                    // if we're just right, accept
                    this.name = name;
                    return this;
                } else {
                    // otherwise, gotta split this node and create some kids
                    // decide which way to split
                    long dw = width - imgWidth;
                    long dh = height - imgHeight;

                    if (dw > dh) {
                        //vertical split
                        child0 = new Node(
                                x,
                                y,
                                imgWidth,
                                height);
                        child1 = new Node(
                                x + imgWidth + padding,
                                y,
                                width - imgWidth - padding,
                                height);
                    } else {
                        //horizontal split
                        child0 = new Node(
                                x,
                                y,
                                width,
                                imgHeight);
                        child1 = new Node(
                                x,
                                y + imgHeight + padding,
                                width,
                                height - imgHeight - padding);
                    }
                    // insert into first child we created
                    return child0.tryInsert(name, image);
                }
            }
        }
    }
}
