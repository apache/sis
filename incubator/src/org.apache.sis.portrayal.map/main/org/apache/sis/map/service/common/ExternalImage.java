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
package org.apache.sis.map.service.common;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * An image used as a graphic: a raster image or a vector graphic such as an SVG document.
 * Instances are immutable and can be shared between threads.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ResourceLoader#image(java.net.URI, String)
 */
public sealed interface ExternalImage permits ExternalImage.Raster, ExternalImage.SVG {
    /**
     * Returns the native width of the image.
     *
     * @return the width in pixels.
     */
    double getWidth();

    /**
     * Returns the native height of the image.
     *
     * @return the height in pixels.
     */
    double getHeight();

    /**
     * Returns an image to draw scaled to the given size.
     * Raster images are returned at their native resolution, to be scaled by the caller when drawn.
     * Vector graphics are rendered at the given size, so that no resolution is lost.
     * The returned image shall not be modified.
     *
     * @param  width   width at which the image will be drawn, in pixels.
     * @param  height  height at which the image will be drawn, in pixels.
     * @return the image to draw.
     */
    BufferedImage image(int width, int height);

    /**
     * A raster image used as a graphic. The native size is the size of the raster.
     *
     * @param raster  the image, which shall not be modified.
     */
    public static record Raster(BufferedImage raster) implements ExternalImage {
        /**
         * Creates a graphic for the given raster.
         *
         * @throws NullPointerException if the raster is null.
         */
        public Raster {
            Objects.requireNonNull(raster, "raster");
        }

        @Override
        public double getWidth() {
            return raster.getWidth();
        }

        @Override
        public double getHeight() {
            return raster.getHeight();
        }

        /**
         * Returns the raster at its native resolution, regardless of the requested size.
         *
         * @param  width   ignored.
         * @param  height  ignored.
         * @return the raster.
         */
        @Override
        public BufferedImage image(final int width, final int height) {
            return raster;
        }
    }

    /**
     * An SVG document used as a graphic, rendered with Apache Batik.
     * Batik is an optional dependency: this class shall be used only if
     * {@link ResourceLoader#isSVGSupported()} returns {@code true}.
     *
     * <ul>
     *   <li>A document without inherent size has a height of 16 pixels, as specified by Symbology Encoding,
     *       and the width given by the aspect ratio of its view box.</li>
     *   <li>The last rendered image is kept for reuse when the same size is requested again.</li>
     * </ul>
     */
    static final class SVG implements ExternalImage {
        /**
         * Height in pixels of documents without inherent size.
         */
        private static final int DEFAULT_HEIGHT = 16;

        /**
         * The graphic tree built from the SVG document. Also used as the lock for painting.
         */
        private final GraphicsNode node;

        /**
         * The document size in pixels.
         */
        private final double width, height;

        /**
         * The last rendered image, or {@code null} if none. Accessed in a block synchronized on {@link #node}.
         */
        private BufferedImage rendered;

        /**
         * Parses the given SVG document.
         *
         * @param  uri  URI of the document, used for resolving relative references. May be null.
         * @param  in   the document content.
         * @throws IOException if the document cannot be read or parsed.
         */
        SVG(final URI uri, final InputStream in) throws IOException {
            final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
            final Document document = factory.createDocument((uri != null) ? uri.toString() : null, in);
            final Element root = document.getDocumentElement();
            if (root.getAttribute("width").isEmpty() || root.getAttribute("height").isEmpty()) {
                double aspect = 1;
                final String[] box = root.getAttribute("viewBox").trim().split("[\\s,]+");
                if (box.length == 4) try {
                    aspect = Double.parseDouble(box[2]) / Double.parseDouble(box[3]);
                } catch (NumberFormatException e) {
                    // Keep the square aspect ratio.
                }
                if (!(aspect > 0 && aspect < Double.POSITIVE_INFINITY)) aspect = 1;
                root.setAttribute("width",  Double.toString(DEFAULT_HEIGHT * aspect));
                root.setAttribute("height", Integer.toString(DEFAULT_HEIGHT));
            }
            final UserAgent agent = new UserAgentAdapter();
            final BridgeContext context = new BridgeContext(agent, new DocumentLoader(agent));
            context.setDynamicState(BridgeContext.STATIC);
            try {
                node = new GVTBuilder().build(context, document);
            } catch (RuntimeException e) {
                throw new IOException("Cannot build the SVG graphic: " + e.getMessage(), e);
            }
            final Dimension2D size = context.getDocumentSize();
            width  = size.getWidth();
            height = size.getHeight();
            if (!(width > 0 && height > 0)) {
                throw new IOException("SVG document without size.");
            }
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        /**
         * Renders the document scaled to the given size.
         *
         * @param  targetWidth   width of the image in pixels.
         * @param  targetHeight  height of the image in pixels.
         * @return the rendered image.
         */
        @Override
        public BufferedImage image(final int targetWidth, final int targetHeight) {
            synchronized (node) {           // GraphicsNode is not documented as thread-safe.
                if (rendered == null || rendered.getWidth() != targetWidth || rendered.getHeight() != targetHeight) {
                    final BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                    final Graphics2D g = image.createGraphics();
                    try {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g.scale(targetWidth / width, targetHeight / height);
                        node.paint(g);
                    } finally {
                        g.dispose();
                    }
                    rendered = image;
                }
                return rendered;
            }
        }
    }


}
