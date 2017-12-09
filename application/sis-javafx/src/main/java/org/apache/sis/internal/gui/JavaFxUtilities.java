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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import org.apache.sis.gui.Resources;
import org.apache.sis.util.logging.Logging;

/**
 * Internal JavaFx utilities.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class JavaFxUtilities {

    public static final Logger LOGGER = Logging.getLogger("org.apache.sis.gui");

    private JavaFxUtilities(){}

    /**
     * Load an image and resize it to requested size.
     *
     * @param path path to image in the jar
     * @param resize null for no resize
     * @return resized image
     */
    public static Image getImage(final String path, Dimension resize) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(JavaFxUtilities.class.getResourceAsStream(path));

            if (resize!=null) {
                final BufferedImage resized = new BufferedImage(resize.width, resize.height, BufferedImage.TYPE_INT_ARGB);
                final Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(img, 0, 0, resize.width, resize.height, null);
                g.dispose();
                img = resized;
            }

        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        return SwingFXUtils.toFXImage(img, null);
    }

    /**
     * Load and initialize widget from jrxml definition set this module bundle.
     *
     * @param candidate widget to load JRXML in
     * @param cdtClass class as base for resource classloader
     */
    public static void loadJRXML(Parent candidate, Class cdtClass) {
        final String fxmlpath = "/"+cdtClass.getName().replace('.', '/')+".fxml";
        final FXMLLoader loader = new FXMLLoader(cdtClass.getResource(fxmlpath));
        loader.setResources(Resources.forLocale(Locale.getDefault()));
        loader.setController(candidate);
        loader.setRoot(candidate);
        //in special environement like osgi or other, we must use the proper class loaders
        //not necessarly the one who loaded the FXMLLoader class
        loader.setClassLoader(cdtClass.getClassLoader());
        try {
            loader.load();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

}
