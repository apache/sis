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
package org.apache.sis.internal.gui;

import java.util.Locale;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;


/**
 * JavaFX utilities for internal purpose only.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class FXUtilities {
    /**
     * Do not allow instantiation of this class.
     */
    private FXUtilities() {
    }

    /**
     * Loads an image and resizes it to requested size.
     *
     * @param  loader  the class to use for loading the image.
     * @param  path    path to image in the jar, relative to the {@code loader} class.
     * @param  resize  the desired size, or {@code null} for no resizing.
     * @return image resized to the given dimension.
     * @throws IOException if the image can not be loaded.
     *
     * @deprecated we need a mechanism without dependency to AWT.
     */
    @Deprecated
    public static Image getImage(final Class<?> loader, final String path, final Dimension resize) throws IOException {
        BufferedImage img = ImageIO.read(loader.getResourceAsStream(path));
        if (resize != null) {
            final BufferedImage resized = new BufferedImage(resize.width, resize.height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img, 0, 0, resize.width, resize.height, null);
            g.dispose();
            img = resized;
        }
        return SwingFXUtils.toFXImage(img, null);
    }

    /**
     * Loads and initializes widget from JRXML definition provided in this module.
     * The JRXML file shall be in the same package than the given {@code loader} class
     * and have the same simple name followed by the {@code ".fxml"} extension.
     *
     * @param  target  the widget for which to load the JRXML file.
     * @param  loader  the class to use for loading the file.
     * @param  locale  the locale for the resources.
     * @throws IOException if an error occurred while loading the JRXML file.
     */
    public static void loadJRXML(final Parent target, final Class<?> loader, final Locale locale) throws IOException {
        final FXMLLoader fxl = new FXMLLoader(loader.getResource(loader.getSimpleName() + ".fxml"),
                                              Resources.forLocale(locale));
        fxl.setRoot(target);
        fxl.setController(target);
        /*
         * In some environements like OSGi, we must use the class loader of the widget
         * (not the class loader of FXMLLoader).
         */
        fxl.setClassLoader(loader.getClassLoader());
        fxl.load();
    }
}
