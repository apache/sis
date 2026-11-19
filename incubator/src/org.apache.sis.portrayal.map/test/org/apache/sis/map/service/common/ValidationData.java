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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferDouble;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import javax.imageio.ImageIO;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.aggregate.CoverageAggregator;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Geometries, coverages and an icon rendered by the validation sheets.
 * Geometries are SIS geometries in CRS:84 fitting in the [-70 … 70]° tiles of a {@link ValidationSheet}.
 * Coverages cover [-50 … 50]°.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ValidationData {
    /**
     * Number of cells along each axis of the {@linkplain #field fields}.
     */
    public static final int CELLS = 40;

    /**
     * The CRS of all geometries and coverages.
     */
    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    /**
     * Factory of the SIS geometries.
     */
    private final GeometryFactory factory;

    /**
     * The icon, created when first requested.
     */
    private Path icon;

    /**
     * Creates the factory of the validation data.
     */
    public ValidationData() {
        factory = GeometryFactory.DEFAULT;
    }

    /**
     * Creates a point.
     *
     * @param  x  longitude in degrees.
     * @param  y  latitude in degrees.
     * @return the point.
     */
    public Geometry point(final double x, final double y) {
        return factory.createPoint(CRS84, x, y);
    }

    /**
     * Creates a line string from (x,y) tuples.
     *
     * @param  xy  the coordinates.
     * @return the line string.
     */
    public LineString line(final double... xy) {
        return factory.createLineString(factory.createDataPoints(CRS84, xy));
    }

    /**
     * Returns a horizontal line from west to east.
     *
     * @return the line.
     */
    public LineString straight() {
        return line(-50, 0, 50, 0);
    }

    /**
     * Returns a line with sharp angles, for joins, offsets and markers.
     *
     * @return the line.
     */
    public LineString zigzag() {
        return line(-50, -20, -20, 30, 10, -25, 50, 25);
    }

    /**
     * Returns a smooth line made of many points, for dashes, patterns and labels.
     *
     * @return the line.
     */
    public LineString curve() {
        final double[] xy = new double[2 * 41];
        for (int i = 0; i <= 40; i++) {
            final double x = -55 + i * 2.75;
            xy[i * 2]     = x;
            xy[i * 2 + 1] = 30 * Math.sin(x / 55 * Math.PI);
        }
        return line(xy);
    }

    /**
     * Returns a square centered on (cx,cy) with the given half size.
     *
     * @param  cx    longitude of the center.
     * @param  cy    latitude of the center.
     * @param  half  half of the size.
     * @return the polygon.
     */
    public Polygon square(final double cx, final double cy, final double half) {
        return factory.createPolygon(factory.createLinearRing(factory.createDataPoints(CRS84,
                cx - half, cy - half,  cx + half, cy - half,  cx + half, cy + half,  cx - half, cy + half,  cx - half, cy - half)),
                List.of());
    }

    /**
     * Returns a square with a square hole.
     *
     * @return the polygon.
     */
    public Polygon donut() {
        return factory.createPolygon(
                factory.createLinearRing(factory.createDataPoints(CRS84, -45.0, -45.0, 45.0, -45.0, 45.0, 45.0, -45.0, 45.0, -45.0, -45.0)),
                List.of(factory.createLinearRing(factory.createDataPoints(CRS84, -20.0, -20.0, -20.0, 20.0, 20.0, 20.0, 20.0, -20.0, -20.0, -20.0))));
    }

    /**
     * Returns a concave star with five branches.
     *
     * @return the polygon.
     */
    public Polygon star() {
        final double[] xy = new double[2 * 11];
        for (int i = 0; i <= 10; i++) {
            final double angle  = Math.PI / 2 + i * Math.PI / 5;
            final double radius = (i % 2 == 0) ? 55 : 24;
            xy[2*i]     = radius * Math.cos(angle);
            xy[2*i + 1] = radius * Math.sin(angle) - 5;
        }
        return factory.createPolygon(factory.createLinearRing(factory.createDataPoints(CRS84, xy)), List.of());
    }

    /**
     * Returns a coverage of {@value #CELLS}×{@value #CELLS} cells with one band per function of (λ,φ).
     *
     * @param  names      names of the bands.
     * @param  functions  the function computing the values of each band.
     * @return the coverage.
     */
    public GridCoverage field(final String[] names, final DoubleBinaryOperator... functions) {
        return coverage(CELLS, names, functions);
    }

    /**
     * Returns a coverage of the given size over [-50 … 50]° with one band per function of (λ,φ).
     * The functions are evaluated at the center of each cell.
     *
     * @param  size       number of cells along each axis.
     * @param  names      names of the bands.
     * @param  functions  the function computing the values of each band.
     * @return the coverage.
     */
    public GridCoverage coverage(final int size, final String[] names, final DoubleBinaryOperator... functions) {
        final double[][] bands = new double[functions.length][size * size];
        for (int j = 0; j < size; j++) {
            final double y = 50 - (j + 0.5) * 100.0 / size;
            for (int i = 0; i < size; i++) {
                final double x = -50 + (i + 0.5) * 100.0 / size;
                for (int b = 0; b < functions.length; b++) {
                    bands[b][j * size + i] = functions[b].applyAsDouble(x, y);
                }
            }
        }
        final SampleDimension[] ranges = new SampleDimension[bands.length];
        for (int b = 0; b < ranges.length; b++) {
            ranges[b] = new SampleDimension.Builder().setName(names[b]).build();
        }
        final GridGeometry domain = new GridGeometry(new GridExtent(size, size),
                new Envelope2D(CRS84, -50, -50, 100, 100), GridOrientation.REFLECTION_Y);
        return new GridCoverageBuilder().setDomain(domain).setRanges(ranges)
                .setValues(new DataBufferDouble(bands, size * size), new Dimension(size, size)).build();
    }

    /**
     * Returns a coverage with a time dimension, one {@linkplain #field field} per function, one day apart.
     *
     * @param  name    name of the band.
     * @param  slices  the function computing the values of each slice.
     * @return the coverage resource.
     */
    public Resource series(final String name, final DoubleBinaryOperator... slices) {
        final CoverageAggregator aggregator = new CoverageAggregator();
        final Instant start = Instant.parse("2020-01-01T00:00:00Z");
        for (int i=0; i<slices.length; i++) {
            aggregator.add(field(new String[] {name}, slices[i]), start.plus(Duration.ofDays(i)), Duration.ofDays(1));
        }
        return aggregator.build(null);
    }

    /**
     * Returns the path to an icon of 24×24 pixels: a black circle on a white square with a transparent border.
     * The icon is written in a temporary file deleted when the virtual machine exits.
     *
     * @return path to the icon file.
     * @throws IOException if the icon cannot be written.
     */
    public Path icon() throws IOException {
        if (icon == null) {
            final BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRect(2, 2, 20, 20);
                g.setColor(Color.BLACK);
                g.fillOval(6, 6, 12, 12);
                g.drawRect(2, 2, 19, 19);
            } finally {
                g.dispose();
            }
            final Path file = Files.createTempFile("validation-icon", ".png");
            file.toFile().deleteOnExit();
            ImageIO.write(image, "png", file.toFile());
            icon = file;
        }
        return icon;
    }
}
