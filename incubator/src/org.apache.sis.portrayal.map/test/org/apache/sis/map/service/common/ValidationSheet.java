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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;


/**
 * An image showing the rendering of many validation cases, grouped in sections, for visual validation of a painter.
 * Each case is rendered in a tile of {@value #TILE_SIZE}×{@value #TILE_SIZE} pixels covering [-70 … 70]° in CRS:84,
 * so one pixel is one degree and the origin is at the tile center. The description of each case is written below
 * its tile, followed in red by the warnings and the rendering errors.
 *
 * <ul>
 *   <li>Sections start on a new row, with their title.</li>
 *   <li>Each row has up to {@value #COLUMNS} tiles.</li>
 *   <li>Descriptions and warnings longer than two lines are truncated.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ValidationSheet {
    /**
     * Size in pixels of the rendered area of each tile.
     */
    public static final int TILE_SIZE = 140;

    /**
     * Maximal number of tiles per row.
     */
    public static final int COLUMNS = 6;

    /**
     * Space in pixels around each tile.
     */
    private static final int MARGIN = 8;

    /**
     * Height in pixels of the description area below each tile.
     */
    private static final int TEXT_HEIGHT = 58;

    /**
     * Height in pixels of the title area.
     */
    private static final int HEADER_HEIGHT = 48;

    /**
     * Height in pixels of a section title.
     */
    private static final int SECTION_HEIGHT = 34;

    /**
     * Maximal number of lines of the description and of the warnings.
     */
    private static final int MAX_LINES = 2;

    /**
     * Explanation written below the title.
     */
    private static final String LEGEND = "Magenta: source geometries and anchor points. "
            + "One pixel is one degree in CRS:84, with the origin at the tile center.";

    /**
     * Title of the image.
     */
    private final String title;

    /**
     * The cases, by section title.
     */
    private final Map<String, List<ValidationCase>> sections;

    /**
     * The rendering grid of each tile.
     */
    private final GridGeometry grid;

    /**
     * Creates an empty sheet.
     *
     * @param  title  title of the image.
     */
    public ValidationSheet(final String title) {
        this.title = Objects.requireNonNull(title, "title");
        sections = new LinkedHashMap<>();
        grid = new GridGeometry(new GridExtent(TILE_SIZE, TILE_SIZE),
                new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), -70, -70, 140, 140), GridOrientation.REFLECTION_Y);
    }

    /**
     * Adds a section of cases.
     *
     * @param  section  title of the section.
     * @param  cases    the cases of the section.
     */
    public void add(final String section, final List<? extends ValidationCase> cases) {
        sections.put(Objects.requireNonNull(section, "section"), List.copyOf(cases));
    }

    /**
     * Returns the number of cases in this sheet.
     *
     * @return the number of cases.
     */
    public int size() {
        return sections.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Renders all cases in a single image.
     *
     * @return the validation image.
     */
    public BufferedImage render() {
        final int cellWidth  = TILE_SIZE + 2 * MARGIN;
        final int cellHeight = TILE_SIZE + 2 * MARGIN + TEXT_HEIGHT;
        int columns = 1;
        int height = HEADER_HEIGHT;
        for (final List<ValidationCase> cases : sections.values()) {
            columns = Math.max(columns, Math.min(COLUMNS, cases.size()));
        }
        for (final List<ValidationCase> cases : sections.values()) {
            height += SECTION_HEIGHT + ((cases.size() + columns - 1) / columns) * cellHeight;
        }
        final BufferedImage image = new BufferedImage(columns * cellWidth, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            g.drawString(title, MARGIN, 30);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.drawString(LEGEND, MARGIN, 44);
            int top = HEADER_HEIGHT;
            for (final Map.Entry<String, List<ValidationCase>> section : sections.entrySet()) {
                g.setColor(new Color(50, 80, 120));
                g.fillRect(0, top + 4, image.getWidth(), SECTION_HEIGHT - 8);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                g.drawString(section.getKey(), MARGIN, top + SECTION_HEIGHT - 12);
                top += SECTION_HEIGHT;
                final List<ValidationCase> cases = section.getValue();
                for (int i = 0; i < cases.size(); i++) {
                    final int x = (i % columns) * cellWidth + MARGIN;
                    final int y = top + (i / columns) * cellHeight + MARGIN;
                    drawCase(g, cases.get(i), x, y, cellWidth);
                }
                top += ((cases.size() + columns - 1) / columns) * cellHeight;
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Renders all cases and writes the image in PNG format.
     *
     * @param  output  the file to write.
     * @throws IOException if the image cannot be written.
     */
    public void write(final Path output) throws IOException {
        if (!ImageIO.write(render(), "png", output.toFile())) {
            throw new IOException("No PNG writer available.");
        }
    }

    /**
     * Draws a case: its tile and its description.
     */
    private void drawCase(final Graphics2D g, final ValidationCase c, final int x, final int y, final int cellWidth) {
        g.setColor(new Color(240, 240, 240));
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(225, 225, 225));
        g.drawLine(x + TILE_SIZE / 2, y, x + TILE_SIZE / 2, y + TILE_SIZE - 1);
        g.drawLine(x, y + TILE_SIZE / 2, x + TILE_SIZE - 1, y + TILE_SIZE / 2);
        final List<String> problems = new ArrayList<>();
        g.drawImage(renderTile(c, problems), x, y, null);
        g.setColor(Color.GRAY);
        g.drawRect(x - 1, y - 1, TILE_SIZE + 1, TILE_SIZE + 1);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        final FontMetrics fm = g.getFontMetrics();
        int ty = y + TILE_SIZE + fm.getAscent() + 3;
        g.setColor(Color.BLACK);
        for (final String line : wrap(c.description(), fm, cellWidth - 4)) {
            g.drawString(line, x - MARGIN + 2, ty);
            ty += fm.getHeight();
        }
        if (!problems.isEmpty()) {
            g.setColor(Color.RED);
            for (final String line : wrap(String.join(" ", problems), fm, cellWidth - 4)) {
                g.drawString(line, x - MARGIN + 2, ty);
                ty += fm.getHeight();
            }
        }
    }

    /**
     * Renders a case in a transparent tile.
     *
     * @param  c         the case to render.
     * @param  problems  where to add the warnings and errors.
     * @return the rendered tile.
     */
    private BufferedImage renderTile(final ValidationCase c, final List<String> problems) {
        final BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = tile.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            c.paint(g, grid, problems);
        } catch (IOException | DataStoreException | RenderingException | RuntimeException e) {
            problems.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            g.dispose();
        }
        return tile;
    }

    /**
     * Splits the given text in lines fitting in the given width, truncated after the maximal number of lines.
     */
    private List<String> wrap(final String text, final FontMetrics fm, final int width) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        boolean truncated = false;
        for (final String word : text.split("\\s+")) {
            if (line.length() != 0 && fm.stringWidth(line + " " + word) > width) {
                if (lines.size() == MAX_LINES - 1) {
                    truncated = true;
                    break;
                }
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() != 0) line.append(' ');
            line.append(word);
        }
        if (line.length() != 0) {
            lines.add(truncated ? line + "…" : line.toString());
        }
        return lines;
    }
}
