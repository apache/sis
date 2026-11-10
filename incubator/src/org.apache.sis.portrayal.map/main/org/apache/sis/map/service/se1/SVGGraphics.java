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
package org.apache.sis.map.service.se1;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;


/**
 * An SVG document rendered with Apache Batik.
 * Batik is an optional dependency: this class shall be used only if {@link #isAvailable()} returns {@code true}.
 * Instances are immutable and can be shared between threads.
 *
 * @author Johann Sorel (Geomatys)
 */
final class SVGGraphics {

    /**
     * Whether the Batik library is available.
     */
    private static final boolean AVAILABLE;
    static {
        boolean available;
        try {
            Class.forName("org.apache.batik.bridge.GVTBuilder", false, SVGGraphics.class.getClassLoader());
            available = true;
        } catch (ClassNotFoundException | LinkageError e) {
            available = false;
        }
        AVAILABLE = available;
    }

    /**
     * Height in pixels of documents without inherent size, as specified by Symbology Encoding.
     */
    private static final int DEFAULT_HEIGHT = 16;

    /**
     * The graphic tree built from the SVG document.
     */
    private final GraphicsNode node;

    /**
     * The document size in pixels.
     */
    final double width, height;

    private SVGGraphics(final GraphicsNode node, final double width, final double height) {
        this.node   = node;
        this.width  = width;
        this.height = height;
    }

    /**
     * Returns whether the Batik library is available for rendering SVG documents.
     *
     * @return whether SVG rendering is available.
     */
    static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Parses the given SVG document.
     *
     * @param  uri  URI of the document, used for resolving relative references. May be null.
     * @param  in   the document content.
     * @return the parsed document.
     * @throws IOException if the document cannot be read or parsed.
     */
    static SVGGraphics load(final URI uri, final InputStream in) throws IOException {
        final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
        final Document document = factory.createDocument((uri != null) ? uri.toString() : null, in);
        final Element root = document.getDocumentElement();
        if (root.getAttribute("width").isEmpty() || root.getAttribute("height").isEmpty()) {
            /*
             * No inherent size. Symbology Encoding specifies a height of 16 pixels and the width
             * given by the aspect ratio, taken from the view box if any.
             */
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
        final GraphicsNode node;
        try {
            node = new GVTBuilder().build(context, document);
        } catch (RuntimeException e) {
            throw new IOException("Cannot build the SVG graphic: " + e.getMessage(), e);
        }
        final Dimension2D size = context.getDocumentSize();
        final double width = size.getWidth(), height = size.getHeight();
        if (!(width > 0 && height > 0)) {
            throw new IOException("SVG document without size.");
        }
        return new SVGGraphics(node, width, height);
    }

    /**
     * Renders the document scaled to the given size.
     *
     * @param  targetWidth   width of the image in pixels.
     * @param  targetHeight  height of the image in pixels.
     * @return the rendered image.
     */
    BufferedImage render(final int targetWidth, final int targetHeight) {
        final BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.scale(targetWidth / width, targetHeight / height);
            synchronized (node) {           // GraphicsNode is not documented as thread-safe.
                node.paint(g);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
