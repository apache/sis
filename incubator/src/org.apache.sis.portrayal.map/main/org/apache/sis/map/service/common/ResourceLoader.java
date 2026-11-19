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

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.apache.sis.map.service.Scene2D;


/**
 * Loads the external resources of graphics, with a cache: raster images, SVG documents and fonts.
 * The kind of a resource is given by its format (a media type such as {@code "image/svg+xml"}),
 * or else by the extension of its file name:
 *
 * <ul>
 *   <li>TrueType and OpenType fonts: {@code ttf}, {@code otf} or a {@code font/…} media type.</li>
 *   <li>SVG documents: {@code svg}. They are rendered with Apache Batik, which is an optional dependency.</li>
 *   <li>Other resources are raster images in a format known to {@link ImageIO} (PNG, JPEG, GIF, BMP…).</li>
 * </ul>
 *
 * The locations shall be absolute URIs. The resources are softly referenced for allowing the garbage collector
 * to release them. Resources which cannot be loaded are reported once, then ignored.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ResourceLoader {
    /**
     * Whether the Apache Batik library is available for rendering SVG documents.
     */
    private static final boolean SVG_SUPPORTED;
    static {
        boolean available;
        try {
            Class.forName("org.apache.batik.bridge.GVTBuilder", false, ResourceLoader.class.getClassLoader());
            available = true;
        } catch (ClassNotFoundException | LinkageError e) {
            available = false;
        }
        SVG_SUPPORTED = available;
    }

    /**
     * Value stored in the cache for resources which cannot be loaded.
     */
    private static final Object FAILED = new Object();

    /**
     * The loaded resources by location: {@link ExternalImage}, {@link Font} or {@link #FAILED}.
     */
    private final Map<URI, SoftReference<Object>> cache;

    /**
     * Creates a loader with an empty cache.
     */
    public ResourceLoader() {
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Returns whether SVG documents can be loaded, which requires Apache Batik.
     *
     * @return whether SVG documents are supported.
     */
    public boolean isSVGSupported() {
        return SVG_SUPPORTED;
    }

    /**
     * Loads the raster image or SVG document at the given location.
     *
     * @param  location  absolute location of the image.
     * @param  format    media type of the image, or {@code null} for deciding from the file extension.
     * @return the image, or {@code null} if the resource is a font or cannot be loaded.
     */
    public ExternalImage image(final URI location, final String format) {
        if (isFont(location, format)) {
            return null;
        }
        final Object value = load(location, format);
        return (value instanceof ExternalImage image) ? image : null;
    }

    /**
     * Loads the TrueType or OpenType font at the given location.
     *
     * @param  location  absolute location of the font file.
     * @param  format    media type of the font, or {@code null} for deciding from the file extension.
     * @return the font, or {@code null} if the resource is not a font or cannot be loaded.
     */
    public Font font(final URI location, final String format) {
        if (!isFont(location, format)) {
            return null;
        }
        final Object value = load(location, format);
        return (value instanceof Font font) ? font : null;
    }

    /**
     * Returns whether the given resource is a font, according its format or its file extension.
     */
    private boolean isFont(final URI location, final String format) {
        final String f = (format != null) ? format.toLowerCase(Locale.ROOT) : "";
        if (f.contains("ttf") || f.contains("otf") || f.contains("truetype") || f.contains("opentype") || f.startsWith("font/")) {
            return true;
        }
        final String path = String.valueOf(location.getPath()).toLowerCase(Locale.ROOT);
        return path.endsWith(".ttf") || path.endsWith(".otf");
    }

    /**
     * Returns whether the given resource is an SVG document, according its format or its file extension.
     */
    private boolean isSVG(final URI location, final String format) {
        if (format != null && format.toLowerCase(Locale.ROOT).contains("svg")) {
            return true;
        }
        final String path = String.valueOf(location.getPath()).toLowerCase(Locale.ROOT);
        return path.endsWith(".svg");
    }

    /**
     * Returns the resource at the given location, loading it if it is not in the cache.
     *
     * @return the resource, or {@link #FAILED} if it cannot be loaded.
     */
    private Object load(final URI location, final String format) {
        final SoftReference<Object> reference = cache.get(location);
        Object value = (reference != null) ? reference.get() : null;
        if (value == null) {
            try {
                value = read(location, format);
            } catch (IOException | FontFormatException | RuntimeException e) {
                Scene2D.LOGGER.log(Level.WARNING, "Cannot load the graphic resource at " + location, e);
                value = FAILED;
            }
            cache.put(location, new SoftReference<>(value));
        }
        return value;
    }

    /**
     * Reads the resource at the given location.
     *
     * @return the image or the font.
     */
    private Object read(final URI location, final String format) throws IOException, FontFormatException {
        if (!location.isAbsolute()) {
            throw new IOException("Relative URI cannot be resolved: " + location);
        }
        final boolean font = isFont(location, format);
        final boolean svg  = !font && isSVG(location, format);
        if (svg && !SVG_SUPPORTED) {
            throw new IOException("Apache Batik is required for rendering SVG graphics.");
        }
        try (InputStream in = location.toURL().openStream()) {
            if (font) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
            if (svg) {
                return new ExternalImage.SVG(location, in);
            }
            final BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Unsupported image format.");
            }
            return new ExternalImage.Raster(image);
        }
    }
}
