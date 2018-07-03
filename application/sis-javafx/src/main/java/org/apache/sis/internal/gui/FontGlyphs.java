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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.sis.util.ArgumentChecks;


/**
 * Internal image tool to generate icons for javafx widgets.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 * @deprecated This introduces a dependency to AWT, which we wish to avoid at this early stage.
 */
@Deprecated
public final class FontGlyphs {

    private static Font FONT;

    static {
        try {
            InputStream is = FontGlyphs.class.getResourceAsStream("/META-INF/resources/webjars/material-design-icons/3.0.1/MaterialIcons-Regular.ttf");
            FONT = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Font not loaded.  Using serif font.");
            FONT = new Font("serif", Font.PLAIN, 24);
        }
    }

    private FontGlyphs() {
    }

    /**
     * Create a image icon from glyph code and color.
     *
     * @param text glyph codes
     * @param size output image size
     * @param textColor glyph color
     * @return glyph image
     */
    public static Image createImage(String text, float size, Color textColor) {
        return SwingFXUtils.toFXImage(createImage(text, textColor, FONT.deriveFont(size), null, null, true, false),null);
    }

    private static BufferedImage createImage(String text, Color textColor, Font font, Color bgColor, Insets insets, final boolean squareWanted, final boolean removeLeading) {
        ArgumentChecks.ensureNonEmpty("Text to draw", text);
        ArgumentChecks.ensureNonNull("Font to use", text);
        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }

        final int border = 0;
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        final FontMetrics fm = g.getFontMetrics(font);
        final int textSize = fm.stringWidth(text);

        int width = textSize + border * 2 + insets.left + insets.right;
        int height = fm.getHeight() + border * 2 + insets.top + insets.bottom;
        if (removeLeading) {
            height -= fm.getLeading();
        }

        // We want a square. We compute additional margin to draw icon and text in center of thee square.
        final int additionalLeftInset;
        final int additionalTopInset;
        if (squareWanted) {
            final int tmpWidth = width;
            width = Math.max(width, height);
            additionalLeftInset = (width - tmpWidth) / 2;

            final int tmpHeight = height;
            height = Math.max(width, height);
            additionalTopInset = (height - tmpHeight) / 2;
        } else {
            additionalLeftInset = 0;
            additionalTopInset = 0;
        }

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final RoundRectangle2D rect = new RoundRectangle2D.Double(0, 0, width - 1, img.getHeight() - 1, border, border);

        if (bgColor != null) {
            final Color brighter = new Color(
                    Math.min(255, bgColor.getRed() + 100),
                    Math.min(255, bgColor.getGreen() + 100),
                    Math.min(255, bgColor.getBlue() + 100));

            final LinearGradientPaint gradiant = new LinearGradientPaint(0, 0, 0, height, new float[]{0, 1}, new Color[]{brighter, bgColor});

            g.setPaint(gradiant);
            g.fill(rect);
        }
        int x = border + insets.left + additionalLeftInset;

        //draw text
        if (textColor != null) {
            g.setColor(textColor);
        }
        g.setFont(font);
        g.drawString(text, x, fm.getAscent() + border + insets.top + additionalTopInset);

        if (bgColor != null) {
            //draw border
            g.setColor(Color.BLACK);
            g.draw(rect);
        }
        return img;
    }
}
