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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferDouble;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import javax.imageio.ImageIO;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.RealFunction;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.ChangeCase;
import org.apache.sis.style.se1.function.Concatenate;
import org.apache.sis.style.se1.function.FormatDate;
import org.apache.sis.style.se1.function.FormatNumber;
import org.apache.sis.style.se1.ChannelSelection;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.ContrastEnhancement;
import org.apache.sis.style.se1.ContrastMethod;
import org.apache.sis.style.se1.ColorReplacement;
import org.apache.sis.style.se1.ExternalGraphic;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.GraphicFill;
import org.apache.sis.style.se1.GraphicStroke;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.Mark;
import org.apache.sis.style.se1.Halo;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.se1.function.Interpolate.InterpolationPoint;
import org.apache.sis.style.se1.function.Recode;
import org.apache.sis.style.se1.function.Recode.MapItem;
import org.apache.sis.style.se1.function.StringLength;
import org.apache.sis.style.se1.function.StringPosition;
import org.apache.sis.style.se1.function.Substring;
import org.apache.sis.style.se1.function.Trim;
import org.apache.sis.style.se1.LinePlacement;
import org.apache.sis.style.se1.PointPlacement;
import org.apache.sis.style.se1.PointSymbolizer;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.OverlapBehavior;
import org.apache.sis.style.se1.RasterSymbolizer;
import org.apache.sis.style.se1.SelectedChannel;
import org.apache.sis.style.se1.ShadedRelief;
import org.apache.sis.style.se1.Stroke;
import org.apache.sis.style.se1.StyleFactory;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.TextSymbolizer;


/**
 * Generates a single image showing the rendering of many symbolizers, for visual validation.
 * Cases are grouped in sections, one per symbolizer type.
 * Each {@link Case} is rendered in a small tile, with its description below.
 * The source geometry is drawn on top of each rendering as a thin magenta line,
 * which helps checking offsets, caps, gaps and other placement parameters.
 *
 * <p>Usage: {@code SEPainterValidation [output.png]}.
 * The default output file is {@code SEPainterValidation.png} in the current directory.</p>
 *
 * <p>Each tile is rendered on a grid of {@value #TILE_SIZE}×{@value #TILE_SIZE} pixels
 * covering [-70 … 70]° in CRS:84, so one pixel is one degree.
 * Features store SIS geometries in the {@code "geom"} and {@code "geom2"} attributes,
 * and optional {@code "color"}, {@code "width"} and {@code "name"} attributes for testing expressions.
 * Raster cases store a coverage, or an aggregate of coverages, in the {@code "raster"} attribute.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SEPainterValidation {

    /**
     * A symbolizer to validate, with the feature to render.
     *
     * @param feature      the feature to render, not null.
     * @param symbolizer   the symbolizer to validate, not null.
     * @param description  short description of what is validated, not null.
     */
    public record Case(Feature feature, Symbolizer<Feature> symbolizer, String description) {
        public Case {
            Objects.requireNonNull(feature, "feature");
            Objects.requireNonNull(symbolizer, "symbolizer");
            Objects.requireNonNull(description, "description");
        }
    }

    /**
     * Size in pixels of the rendered area of each tile.
     */
    private static final int TILE_SIZE = 140;

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
     * Number of tiles per row.
     */
    private static final int COLUMNS = 6;

    /**
     * Color of the source geometry drawn over each rendering.
     */
    private static final Color REFERENCE_COLOR = new Color(255, 0, 255, 180);

    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    private static final GeometryFactory GF = GeometryFactory.DEFAULT;

    /**
     * Rendering grid of each tile.
     */
    private static final GridGeometry GRID;

    /**
     * Type of all features.
     */
    private static final FeatureType TYPE;
    static {
        final GeneralEnvelope env = new GeneralEnvelope(CRS84);
        env.setRange(0, -70, 70);
        env.setRange(1, -70, 70);
        GRID = new GridGeometry(new GridExtent(TILE_SIZE, TILE_SIZE), env, GridOrientation.REFLECTION_Y);

        /*
         * SIS geometries cannot be declared as the default geometry of a feature type
         * (they are not yet known to the generic geometry wrappers). Symbolizers must
         * therefore reference the geometry attribute explicitly.
         */
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("validation");
        ftb.addAttribute(Geometry.class).setName("geom");
        ftb.addAttribute(Geometry.class).setName("geom2");
        ftb.addAttribute(Color.class).setName("color");
        ftb.addAttribute(Double.class).setName("width");
        ftb.addAttribute(String.class).setName("name");
        ftb.addAttribute(Object.class).setName("raster");
        TYPE = ftb.build();
    }

    private SEPainterValidation() {
    }

    /**
     * Generates the validation image.
     *
     * @param  args  optional path of the PNG file to write.
     * @throws IOException if the image cannot be written.
     */
    public static void main(String[] args) throws IOException {
        final Path output = Path.of(args.length > 0 ? args[0] : "SEPainterValidation.png");
        final Map<String, List<Case>> sections = new LinkedHashMap<>();
        sections.put("LineSymbolizer", lineCases());
        sections.put("LineSymbolizer on curve types", curveCases());
        sections.put("PolygonSymbolizer", polygonCases());
        sections.put("PointSymbolizer", pointCases());
        sections.put("External graphics and font marks", externalCases());
        sections.put("TextSymbolizer", textCases());
        sections.put("RasterSymbolizer", rasterCases());
        final BufferedImage image = render("Symbology Encoding validation", sections);
        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IOException("No PNG writer available.");
        }
        System.out.println("Validation image written to " + output.toAbsolutePath());
    }

    // ////////////////////////////////////////////////////////////////////////
    // Validation cases ///////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns the cases validating all {@link LineSymbolizer} parameters.
     *
     * @return line symbolizer validation cases.
     */
    public static List<Case> lineCases() {
        final List<Case> cases = new ArrayList<>();

        // ---- Stroke color, opacity and width ----
        cases.add(new Case(feature(straight()), symbolizer(), "Default stroke: black, 1px"));
        {
            final var s = symbolizer(); s.getStroke().setColor(FF.literal(Color.RED)); s.getStroke().setWidth(FF.literal(4.0));
            cases.add(new Case(feature(straight()), s, "Color: red, width 4"));
        }
        {
            final var f = feature(curve()); f.setPropertyValue("color", Color.BLUE); f.setPropertyValue("width", 6.0);
            final var s = symbolizer(); s.getStroke().setColor(FF.property("color", Color.class)); s.getStroke().setWidth(FF.property("width", Double.class));
            cases.add(new Case(f, s, "Color and width from attributes: blue, 6"));
        }
        for (double opacity : new double[] {1, 0.5, 0.2}) {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(12.0)); s.getStroke().setOpacity(FF.literal(opacity));
            cases.add(new Case(feature(zigzag()), s, "Opacity " + opacity + ", width 12"));
        }
        for (double width : new double[] {2, 8, 20}) {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(width));
            cases.add(new Case(feature(straight()), s, "Width " + width));
        }

        // ---- Line join and cap ----
        for (String join : new String[] {"mitre", "round", "bevel"}) {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(14.0)); s.getStroke().setLineJoin(FF.literal(join));
            s.getStroke().setColor(FF.literal(Color.DARK_GRAY));
            cases.add(new Case(feature(zigzag()), s, "Line join: " + join + ", width 14"));
        }
        for (String cap : new String[] {"butt", "round", "square"}) {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(20.0)); s.getStroke().setLineCap(FF.literal(cap));
            s.getStroke().setColor(FF.literal(Color.DARK_GRAY));
            cases.add(new Case(feature(line(-40, 0, 40, 0)), s, "Line cap: " + cap + ", width 20"));
        }

        // ---- Dashes ----
        {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(4.0)); s.getStroke().setDashArray(FF.literal(new float[] {10, 5}));
            cases.add(new Case(feature(curve()), s, "Dash array [10 5], width 4"));
        }
        {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(4.0)); s.getStroke().setDashArray(FF.literal(new float[] {20, 5, 2, 5}));
            cases.add(new Case(feature(curve()), s, "Dash array [20 5 2 5], width 4"));
        }
        for (int offset : new int[] {0, 10}) {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(6.0)); s.getStroke().setLineCap(FF.literal("butt"));
            s.getStroke().setDashArray(FF.literal(new float[] {20, 10}));
            s.getStroke().setDashOffset(FF.literal(offset));
            cases.add(new Case(feature(straight()), s, "Dash [20 10], dash offset " + offset + ", butt cap"));
        }

        // ---- Graphic fill ----
        {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(20.0));
            s.getStroke().setGraphicFill(graphicFill(mark("cross", Color.RED, null), 8));
            cases.add(new Case(feature(zigzag()), s, "Graphic fill: red cross 8px, width 20"));
        }
        {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(24.0));
            s.getStroke().setGraphicFill(graphicFill(mark("circle", Color.ORANGE, Color.BLACK), 10));
            cases.add(new Case(feature(curve()), s, "Graphic fill: outlined orange circle 10px, width 24"));
        }

        // ---- Graphic stroke ----
        {
            final var s = symbolizer(); s.getStroke().setGraphicStroke(new GraphicStroke<>(FeatureTypeStyle.FACTORY));
            cases.add(new Case(feature(curve()), s, "Graphic stroke: default graphic (gray square 6px)"));
        }
        for (String wkn : new String[] {"square", "circle", "triangle", "star", "cross", "x"}) {
            final var s = symbolizer(); s.getStroke().setGraphicStroke(graphicStroke(mark(wkn, Color.GREEN.darker(), Color.BLACK), 12));
            cases.add(new Case(feature(curve()), s, "Graphic stroke: " + wkn + " 12px, follows line direction"));
        }
        {
            final var gs = graphicStroke(mark("triangle", Color.RED, Color.BLACK), 10); gs.setGap(FF.literal(10.0));
            final var s = symbolizer(); s.getStroke().setGraphicStroke(gs);
            cases.add(new Case(feature(zigzag()), s, "Graphic stroke gap 10, triangle 10px"));
        }
        {
            final var gs = graphicStroke(mark("square", Color.RED, Color.BLACK), 10); gs.setGap(FF.literal(10.0)); gs.setInitialGap(FF.literal(40.0));
            final var s = symbolizer(); s.getStroke().setGraphicStroke(gs);
            cases.add(new Case(feature(straight()), s, "Graphic stroke initial gap 40, gap 10"));
        }
        {
            final var gs = graphicStroke(mark("square", Color.CYAN, Color.BLACK), 10); gs.setGap(FF.literal(6.0));
            gs.getGraphic().setRotation(FF.literal(45.0));
            final var s = symbolizer(); s.getStroke().setGraphicStroke(gs);
            cases.add(new Case(feature(straight()), s, "Graphic stroke, graphic rotation 45°"));
        }
        {
            final var gs = graphicStroke(mark("circle", Color.BLUE, null), 14);
            gs.getGraphic().setOpacity(FF.literal(0.4));
            final var s = symbolizer(); s.getStroke().setGraphicStroke(gs);
            cases.add(new Case(feature(curve()), s, "Graphic stroke, graphic opacity 0.4 (overlaps darker)"));
        }
        {
            final var gs = graphicStroke(mark("triangle", Color.RED, Color.BLACK), 12); gs.setGap(FF.literal(8.0));
            gs.getGraphic().getAnchorPoint().setAnchorPointY(FF.literal(0.0));
            final var s = symbolizer(); s.getStroke().setGraphicStroke(gs);
            cases.add(new Case(feature(straight()), s, "Graphic stroke, anchor point Y 0 (graphics above line)"));
        }
        {
            final var gs = graphicStroke(mark("circle", Color.RED, Color.BLACK), 8); gs.setGap(FF.literal(8.0));
            gs.getGraphic().getDisplacement().setDisplacementY(FF.literal(-10.0));
            final var s = symbolizer(); s.getStroke().setGraphicStroke(gs);
            cases.add(new Case(feature(straight()), s, "Graphic stroke, displacement Y -10 (graphics below line)"));
        }

        // ---- Perpendicular offset ----
        for (double offset : new double[] {10, -10}) {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(3.0)); s.getStroke().setColor(FF.literal(Color.BLUE));
            s.setPerpendicularOffset(FF.literal(offset));
            cases.add(new Case(feature(zigzag()), s, "Perpendicular offset " + offset + " (positive = left)"));
        }
        {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(3.0)); s.getStroke().setColor(FF.literal(Color.BLUE));
            s.setPerpendicularOffset(FF.literal(8.0));
            cases.add(new Case(feature(polygon()), s, "Perpendicular offset 8 on polygon rings"));
        }

        // ---- Unit of measure ----
        {
            final var s = symbolizer(); s.setUnitOfMeasure(Units.METRE);
            s.getStroke().setWidth(FF.literal(1000_000.0)); s.getStroke().setColor(FF.literal(Color.DARK_GRAY));
            cases.add(new Case(feature(straight()), s, "UOM metre: width 1000 km (~9px at equator)"));
        }
        {
            final var s = symbolizer(); s.setUnitOfMeasure(Units.METRE);
            s.getStroke().setWidth(FF.literal(200_000.0)); s.getStroke().setColor(FF.literal(Color.BLUE));
            s.setPerpendicularOffset(FF.literal(2000_000.0));
            cases.add(new Case(feature(straight()), s, "UOM metre: width 200 km, offset 2000 km (~18px)"));
        }
        {
            final var s = symbolizer(); s.setUnitOfMeasure(Units.METRE);
            s.getStroke().setWidth(FF.literal(300_000.0)); s.getStroke().setLineCap(FF.literal("butt"));
            s.getStroke().setDashArray(FF.literal(new float[] {2000_000, 1000_000}));
            cases.add(new Case(feature(straight()), s, "UOM metre: dashes [2000 km 1000 km]"));
        }

        // ---- Geometry ----
        {
            final var f = feature(straight()); f.setPropertyValue("geom2", zigzag());
            final var s = symbolizer(); s.setGeometry(FF.property("geom2")); s.getStroke().setWidth(FF.literal(3.0));
            cases.add(new Case(f, s, "Geometry expression: \"geom2\" (zigzag, not the straight line)"));
        }
        {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(3.0)); s.getStroke().setColor(FF.literal(Color.RED));
            cases.add(new Case(feature(polygon()), s, "Polygon with hole: both rings stroked"));
        }
        {
            final var s = symbolizer(); s.getStroke().setWidth(FF.literal(3.0));
            cases.add(new Case(feature(GF.createMultiLineString(CRS84, line(-50, 30, 50, 30), line(-50, -30, 50, -30))), s,
                    "Multi line string: two lines"));
        }
        return cases;
    }

    /**
     * Returns one case for each type of curve, rendered by a line symbolizer.
     * Curves which cannot be linearized are reported in red below their tile.
     *
     * @return curve validation cases.
     */
    public static List<Case> curveCases() {
        final List<Case> cases = new ArrayList<>();
        cases.add(new Case(feature(line(-50, -30,  -10, 30,  50, -30)), curveStyle(), "LineString"));
        cases.add(new Case(feature(GF.createLinearRing(points(-40, -40,  40, -40,  0, 40,  -40, -40))), curveStyle(), "LinearRing"));
        cases.add(new Case(feature(GF.createCircularString(points(-50, 0,  -25, 30,  0, 0,  25, -30,  50, 0))),
                curveStyle(), "CircularString, two arcs"));
        cases.add(new Case(feature(compoundCurve()), curveStyle(), "CompoundCurve: line, circular arc, line"));
        cases.add(new Case(feature(GF.createArcByCenterPoint(GF.createPoint(CRS84, 0, -20), 45, null, 300, 60)),
                curveStyle(), "ArcByCenterPoint: center (0,-20), radius 45, bearings 300° to 60°"));
        cases.add(new Case(feature(GF.createArcByBulge(points(-45, -10,  45, -10), 35, vector(0, 1))),
                curveStyle(), "ArcByBulge: chord (-45,-10) (45,-10), bulge 35 north"));
        cases.add(new Case(feature(GF.createArc(points(-45, 0,  0, 45,  45, 0), NDArrays.of(CRS84, new double[] {0, 0,  0, 0}), null, false)),
                curveStyle(), "Arc: two arcs centered on (0,0), radius 45"));
        cases.add(new Case(feature(GF.createCircle(points(40, 0,  0, 40,  -40, 0,  0, -40,  40, 0),
                NDArrays.of(CRS84, new double[] {0, 0,  0, 0,  0, 0,  0, 0}), List.of())),
                curveStyle(), "Circle: center (0,0), radius 40"));
        cases.add(new Case(feature(GF.createConic(points(ellipse(-0.3, 0.4, 1.2, 2.0, 2.6)), null, false)),
                curveStyle(), "Conic: 5 points on ellipse 50×30"));
        cases.add(new Case(feature(GF.createEllipticArc(points(ellipse(-0.5, 0.8, 1.8, 2.9)), null, false)),
                curveStyle(), "EllipticArc: 4 points on ellipse 50×30"));
        cases.add(new Case(feature(GF.createBezier(points(-50, -20,  50, 20),
                NDArrays.of(CRS84, new double[] {-50, -20,  -20, 50,  20, -50,  50, 20}), new double[] {0, 1}, 3, null, null, null, null)),
                curveStyle(), "Bezier: cubic, 4 control points"));
        cases.add(new Case(feature(GF.createBSplineCurve(points(-50, -30,  -30, 30,  -10, -30,  10, 30,  30, -30,  50, 30),
                null, null, 3, null, null, false)),
                curveStyle(), "BSplineCurve: degree 3, 6 control points, uniform knots"));
        cases.add(new Case(feature(GF.createNurbCurve(points(25, -25,  25, 25,  -25, 25),
                new double[] {1, Math.sqrt(0.5), 1}, new double[] {0, 0, 0, 1, 1, 1}, 2)),
                curveStyle(), "NurbCurve: rational quarter circle, radius 50"));
        cases.add(new Case(feature(GF.createPolynomialSpline(points(-50, -20,  -10, 30,  20, -30,  50, 20),
                null, null, 3, null, null, null, null, 0)),
                curveStyle(), "PolynomialSpline: degree 3 through 4 points"));
        cases.add(new Case(feature(GF.createCubicSpline(points(-50, -20,  -10, 30,  20, -30,  50, 20),
                null, null, null, null, null, null)),
                curveStyle(), "CubicSpline through 4 points"));
        cases.add(new Case(feature(GF.createGeodesic(points(-50, -30,  50, 40))), curveStyle(), "Geodesic (-50,-30) to (50,40)"));
        cases.add(new Case(feature(GF.createRhumb(points(-50, -30,  50, 40))), curveStyle(), "Rhumb (-50,-30) to (50,40)"));
        cases.add(new Case(feature(GF.createSpiral(points(-40, -30), function(r -> 0.05), null, null)),
                curveStyle(), "Spiral: constant curvature 0.05"));
        cases.add(new Case(feature(GF.createClothoid(points(-40, -30), function(r -> 0.001 * r), null)),
                curveStyle(), "Clothoid: curvature 0.001 × arc length"));
        cases.add(new Case(feature(GF.createRay(CRS84, vector(-40, -20), vector(1, 0.5))),
                curveStyle(), "Ray from (-40,-20) toward (1,0.5)"));
        cases.add(new Case(feature(GF.createProductCurve(null, null, line(-50, 0, 50, 0))),
                curveStyle(), "ProductCurve of a single segment"));
        cases.add(new Case(feature(GF.createOffsetCurve(zigzag(), Quantities.create(10, Units.UNITY), null)),
                curveStyle(), "OffsetCurve of zigzag, distance 10 (left)"));
        cases.add(new Case(feature(GF.createOffsetCurve(GF.createCircularString(points(-45, -20,  0, 25,  45, -20)),
                Quantities.create(10, Units.UNITY), null)),
                curveStyle(), "OffsetCurve of circular string, distance 10"));
        cases.add(new Case(feature(compoundCurve()), arrowStyle(), "CompoundCurve with direction arrows"));
        cases.add(new Case(feature(GF.createReversed(compoundCurve())), arrowStyle(), "Reversed CompoundCurve: arrows point west"));
        cases.add(new Case(feature(GF.createMultiCurve(CRS84,
                GF.createCircularString(points(-50, 20,  0, 45,  50, 20)),
                GF.createCircularString(points(-50, -40,  0, -15,  50, -40)))),
                curveStyle(), "MultiCurve: two circular strings"));
        cases.add(new Case(feature(GF.createCurvePolygon(GF.createCircularString(points(-45, 0,  45, 0,  -45, 0)),
                List.of(GF.createLinearRing(points(-20, -20,  -20, 20,  20, 20,  20, -20,  -20, -20))))),
                curveStyle(), "CurvePolygon: circle exterior, square hole"));
        return cases;
    }

    /**
     * Returns the cases validating all {@link PolygonSymbolizer} parameters.
     *
     * @return polygon symbolizer validation cases.
     */
    public static List<Case> polygonCases() {
        final List<Case> cases = new ArrayList<>();

        // ---- Fill ----
        cases.add(new Case(feature(square(40)), polygonSymbolizer(), "Default: gray fill, black 1px stroke"));
        cases.add(new Case(feature(square(40)), fillOnly(Color.RED), "Fill color red, no stroke"));
        {
            final var f = feature(square(40)); f.setPropertyValue("color", Color.BLUE);
            final var s = fillOnly(Color.RED); s.getFill().get().setColor(FF.property("color", Color.class));
            cases.add(new Case(f, s, "Fill color from attribute: blue"));
        }
        for (double opacity : new double[] {1, 0.5, 0.2}) {
            final var s = fillOnly(Color.RED); s.getFill().get().setOpacity(FF.literal(opacity));
            cases.add(new Case(feature(polygon()), s, "Fill opacity " + opacity));
        }
        {
            final var s = fillOnly(Color.RED); s.getFill().get().setGraphicFill(graphicFill(mark("cross", Color.RED, null), 8));
            cases.add(new Case(feature(polygon()), s, "Fill graphic fill: red cross 8px"));
        }
        {
            final var s = fillOnly(Color.RED); s.getFill().get().setGraphicFill(graphicFill(mark("circle", Color.ORANGE, Color.BLACK), 12));
            cases.add(new Case(feature(star()), s, "Fill graphic fill: outlined orange circle 12px"));
        }
        {
            final var s = fillOnly(Color.RED); s.getFill().get().setGraphicFill(graphicFill(mark("star", Color.YELLOW, Color.DARK_GRAY), 14));
            s.getFill().get().setOpacity(FF.literal(0.5));
            cases.add(new Case(feature(square(40)), s, "Fill graphic fill: star 14px, fill opacity 0.5"));
        }

        // ---- Stroke ----
        {
            final var s = polygonSymbolizer(); s.setFill(null); s.getStroke().get().setColor(FF.literal(Color.BLUE));
            s.getStroke().get().setWidth(FF.literal(3.0));
            cases.add(new Case(feature(polygon()), s, "No fill, blue stroke 3px on both rings"));
        }
        {
            final var s = fillOnly(Color.LIGHT_GRAY); s.setStroke(stroke(Color.DARK_GRAY, 10));
            s.getStroke().get().setLineJoin(FF.literal("round"));
            cases.add(new Case(feature(star()), s, "Stroke 10px, round join, over fill"));
        }
        {
            final var s = fillOnly(Color.LIGHT_GRAY); s.setStroke(stroke(Color.RED, 10));
            s.getStroke().get().setOpacity(FF.literal(0.5));
            cases.add(new Case(feature(square(40)), s, "Stroke 10px, opacity 0.5 over fill"));
        }
        {
            final var s = fillOnly(Color.LIGHT_GRAY); s.setStroke(stroke(Color.BLUE, 3));
            s.getStroke().get().setDashArray(FF.literal(new float[] {10, 5}));
            cases.add(new Case(feature(polygon()), s, "Stroke dash array [10 5]"));
        }
        {
            final var s = fillOnly(Color.LIGHT_GRAY); s.setStroke(stroke(Color.BLACK, 1));
            final var gs = graphicStroke(mark("triangle", Color.RED, Color.BLACK), 10); gs.setGap(FF.literal(6.0));
            gs.getGraphic().setRotation(FF.literal(90.0));
            s.getStroke().get().setGraphicStroke(gs);
            cases.add(new Case(feature(polygon()), s, "Stroke graphic stroke: arrows show ring direction"));
        }
        {
            final var s = fillOnly(Color.LIGHT_GRAY); s.setStroke(stroke(Color.BLACK, 12));
            s.getStroke().get().setGraphicFill(graphicFill(mark("x", Color.BLUE, null), 6));
            cases.add(new Case(feature(square(40)), s, "Stroke graphic fill: blue x 6px, width 12"));
        }

        // ---- Symbology functions on features ----
        for (double width : new double[] {2, 8}) {
            final var f = feature(square(40)); f.setPropertyValue("width", width);
            final var s = fillOnly(Color.RED);
            s.getFill().get().setColor(new Categorize<Feature,Color>(FF.property("width"),
                    List.of(FF.literal(Color.GREEN), FF.literal(Color.ORANGE)), List.of(FF.literal(5)), null, null));
            cases.add(new Case(f, s, "Fill = Categorize(width, green, 5, orange), width " + width));
        }
        {
            final var f = feature(square(40)); f.setPropertyValue("width", 7.5);
            final var s = fillOnly(Color.RED);
            s.getFill().get().setColor(new Interpolate<Feature>(FF.property("width"),
                    List.of(point(0, Color.BLUE), point(10, Color.RED)), null, null, null).toValueType(Color.class));
            cases.add(new Case(f, s, "Fill = Interpolate(width, 0 blue, 10 red), width 7.5"));
        }

        // ---- Displacement ----
        {
            final var s = fillOnly(Color.RED);
            s.getDisplacement().setDisplacementX(FF.literal(15.0)); s.getDisplacement().setDisplacementY(FF.literal(15.0));
            cases.add(new Case(feature(square(30)), s, "Displacement (15, 15): right and up"));
        }
        {
            final var s = fillOnly(Color.RED);
            s.getDisplacement().setDisplacementX(FF.literal(-20.0));
            cases.add(new Case(feature(square(30)), s, "Displacement (-20, 0): left"));
        }

        // ---- Perpendicular offset ----
        for (double offset : new double[] {10, -10}) {
            final var s = fillOnly(Color.RED); s.setStroke(stroke(Color.BLACK, 1));
            s.setPerpendicularOffset(FF.literal(offset));
            cases.add(new Case(feature(polygon()), s, "Perpendicular offset " + offset + " (positive = larger)"));
        }
        {
            final var s = fillOnly(Color.RED); s.setStroke(stroke(Color.BLACK, 1));
            s.setPerpendicularOffset(FF.literal(8.0));
            cases.add(new Case(feature(lShape()), s, "Perpendicular offset 8 on concave polygon"));
        }
        {
            final var s = fillOnly(Color.RED); s.setStroke(stroke(Color.BLACK, 1));
            s.setPerpendicularOffset(FF.literal(-12.0));
            cases.add(new Case(feature(lShape()), s, "Perpendicular offset -12 on concave polygon"));
        }
        {
            final var s = fillOnly(Color.RED);
            s.setPerpendicularOffset(FF.literal(-45.0));
            cases.add(new Case(feature(square(40)), s, "Perpendicular offset -45: polygon collapses, nothing drawn"));
        }

        // ---- Unit of measure ----
        {
            final var s = fillOnly(Color.RED); s.setUnitOfMeasure(Units.METRE);
            s.getDisplacement().setDisplacementX(FF.literal(2000_000.0));
            cases.add(new Case(feature(square(30)), s, "UOM metre: displacement 2000 km (~18px right)"));
        }
        {
            final var s = fillOnly(Color.RED); s.setUnitOfMeasure(Units.METRE); s.setStroke(stroke(Color.BLACK, 200_000));
            s.setPerpendicularOffset(FF.literal(1000_000.0));
            cases.add(new Case(feature(square(30)), s, "UOM metre: offset 1000 km, stroke 200 km"));
        }

        // ---- Geometry ----
        {
            final var f = feature(square(40)); f.setPropertyValue("geom2", star());
            final var s = fillOnly(Color.RED); s.setGeometry(FF.property("geom2"));
            cases.add(new Case(f, s, "Geometry expression: \"geom2\" (star, not the square)"));
        }
        cases.add(new Case(feature(GF.createMultiPolygon(CRS84,
                squareAt(-30, 30, 20), squareAt(30, -30, 20))),
                polygonSymbolizer(), "MultiPolygon: two squares"));
        cases.add(new Case(feature(GF.createCurvePolygon(GF.createCircularString(points(-45, 0,  45, 0,  -45, 0)),
                List.of(GF.createCircularString(points(-20, 0,  20, 0,  -20, 0))))),
                polygonSymbolizer(), "CurvePolygon: circle with circular hole"));
        cases.add(new Case(feature(line(-40, -30,  40, -30,  0, 40)), polygonSymbolizer(), "LineString: closed implicitly"));
        cases.add(new Case(feature(GF.createCircularString(points(-45, -20,  0, 40,  45, -20))), polygonSymbolizer(),
                "CircularString: closed implicitly"));
        cases.add(new Case(feature(GF.createPoint(CRS84, 0, 0)), polygonSymbolizer(), "Point: small square (6 px)"));
        return cases;
    }

    /**
     * Returns the cases validating all {@link PointSymbolizer} parameters.
     *
     * @return point symbolizer validation cases.
     */
    public static List<Case> pointCases() {
        final List<Case> cases = new ArrayList<>();
        final Geometry origin = GF.createPoint(CRS84, 0, 0);

        // ---- Marks ----
        cases.add(new Case(feature(origin), pointSymbolizer(), "Default graphic: gray square 6px, black outline"));
        for (String wkn : new String[] {"square", "circle", "triangle", "star", "cross", "x"}) {
            cases.add(new Case(feature(origin), point(mark(wkn, Color.GREEN.darker(), Color.BLACK), 40), "Mark " + wkn + ", size 40"));
        }
        cases.add(new Case(feature(origin), point(mark("circle", Color.RED, null), 40), "Mark fill red, no outline"));
        cases.add(new Case(feature(origin), point(mark("circle", null, Color.BLUE), 40), "Mark outline blue, no fill"));
        {
            final var m = mark("square", Color.RED, Color.BLACK); m.getFill().get().setOpacity(FF.literal(0.4));
            cases.add(new Case(feature(origin), point(m, 40), "Mark fill opacity 0.4, opaque outline"));
        }
        {
            final var m = mark("square", Color.YELLOW, Color.BLACK); m.getStroke().get().setWidth(FF.literal(6.0));
            m.getStroke().get().setLineJoin(FF.literal("round"));
            cases.add(new Case(feature(origin), point(m, 40), "Mark outline width 6, round join"));
        }
        {
            final var m = mark("circle", Color.WHITE, Color.BLACK); m.getStroke().get().setWidth(FF.literal(3.0));
            m.getStroke().get().setDashArray(FF.literal(new float[] {6, 4}));
            cases.add(new Case(feature(origin), point(m, 40), "Mark outline dash array [6 4]"));
        }
        {
            final var f = feature(origin); f.setPropertyValue("color", Color.ORANGE); f.setPropertyValue("width", 30.0);
            final var m = mark("star", Color.RED, Color.BLACK); m.getFill().get().setColor(FF.property("color", Color.class));
            final var s = point(m, 10); s.getGraphic().setSize(FF.property("width", Double.class));
            cases.add(new Case(f, s, "Fill color and size from attributes: orange, 30"));
        }

        // ---- Graphic ----
        {
            final var s = point(mark("square", Color.RED, Color.BLACK), 40); s.getGraphic().setOpacity(FF.literal(0.4));
            cases.add(new Case(feature(origin), s, "Graphic opacity 0.4"));
        }
        for (double size : new double[] {10, 30, 60}) {
            cases.add(new Case(feature(origin), point(mark("circle", Color.CYAN, Color.BLACK), size), "Graphic size " + size));
        }
        for (double rotation : new double[] {30, 90, -45}) {
            final var s = point(mark("triangle", Color.CYAN, Color.BLACK), 40);
            s.getGraphic().setRotation(FF.literal(rotation));
            cases.add(new Case(feature(origin), s, "Graphic rotation " + rotation + "° (clockwise)"));
        }
        for (double[] anchor : new double[][] {{0, 0}, {1, 1}, {0.5, 0}}) {
            final var s = point(mark("square", Color.ORANGE, Color.BLACK), 30);
            s.getGraphic().getAnchorPoint().setAnchorPointX(FF.literal(anchor[0]));
            s.getGraphic().getAnchorPoint().setAnchorPointY(FF.literal(anchor[1]));
            cases.add(new Case(feature(origin), s, "Anchor point (" + anchor[0] + ", " + anchor[1] + ")"));
        }
        {
            final var s = point(mark("triangle", Color.ORANGE, Color.BLACK), 30);
            s.getGraphic().getDisplacement().setDisplacementX(FF.literal(25.0));
            s.getGraphic().getDisplacement().setDisplacementY(FF.literal(25.0));
            cases.add(new Case(feature(origin), s, "Displacement (25, 25): right and up"));
        }
        {
            final var s = point(mark("triangle", Color.ORANGE, Color.BLACK), 30);
            s.getGraphic().getDisplacement().setDisplacementX(FF.literal(25.0));
            s.getGraphic().setRotation(FF.literal(90.0));
            cases.add(new Case(feature(origin), s, "Displacement (25, 0) with rotation 90°: not rotated"));
        }
        {
            final var s = point(mark("triangle", Color.ORANGE, Color.BLACK), 30);
            s.getGraphic().getAnchorPoint().setAnchorPointY(FF.literal(0.0));
            s.getGraphic().setRotation(FF.literal(90.0));
            cases.add(new Case(feature(origin), s, "Anchor (0.5, 0) with rotation 90°: rotates around anchor"));
        }

        // ---- Unit of measure ----
        {
            final var s = point(mark("circle", Color.RED, Color.BLACK), 3000_000); s.setUnitOfMeasure(Units.METRE);
            cases.add(new Case(feature(origin), s, "UOM metre: size 3000 km (~27px)"));
        }
        {
            final var s = point(mark("circle", Color.RED, Color.BLACK), 2000_000); s.setUnitOfMeasure(Units.METRE);
            s.getGraphic().getDisplacement().setDisplacementX(FF.literal(3000_000.0));
            cases.add(new Case(feature(origin), s, "UOM metre: size 2000 km, displacement 3000 km"));
        }

        // ---- Geometry ----
        {
            final var f = feature(origin); f.setPropertyValue("geom2", GF.createPoint(CRS84, 30, 30));
            final var s = point(mark("star", Color.RED, Color.BLACK), 30); s.setGeometry(FF.property("geom2"));
            cases.add(new Case(f, s, "Geometry expression: \"geom2\" at (30, 30)"));
        }
        cases.add(new Case(feature(GF.createMultiPoint(CRS84, GF.createPoint(CRS84, -40, -30), GF.createPoint(CRS84, 0, 30),
                GF.createPoint(CRS84, 40, -10))), point(mark("circle", Color.RED, Color.BLACK), 14), "MultiPoint: one graphic per point"));
        cases.add(new Case(feature(lShape()), point(mark("star", Color.RED, Color.BLACK), 20), "Polygon: graphic on a point inside (concave)"));
        cases.add(new Case(feature(zigzag()), point(mark("star", Color.RED, Color.BLACK), 20), "LineString: graphic at middle of length"));
        cases.add(new Case(feature(GF.createCircularString(points(-45, -20,  0, 40,  45, -20))),
                point(mark("star", Color.RED, Color.BLACK), 20), "CircularString: graphic at middle of length"));
        {
            final var s = pointSymbolizer();
            s.getGraphic().graphicalSymbols().add(new ExternalGraphic<>(FeatureTypeStyle.FACTORY));
            cases.add(new Case(feature(origin), s, "ExternalGraphic without resource: nothing drawn"));
        }
        {
            final var s = point(mark("star", Color.RED, Color.BLACK), 30);
            s.getGraphic().graphicalSymbols().add(0, new ExternalGraphic<>(FeatureTypeStyle.FACTORY));
            cases.add(new Case(feature(origin), s, "ExternalGraphic without resource, then Mark: uses the mark"));
        }
        return cases;
    }

    /**
     * Returns the cases validating all {@link TextSymbolizer} parameters.
     *
     * @return text symbolizer validation cases.
     */
    public static List<Case> textCases() {
        final List<Case> cases = new ArrayList<>();
        final Geometry origin = GF.createPoint(CRS84, 0, 0);

        // ---- Label and font ----
        {
            final var s = new TextSymbolizer<Feature>(FeatureTypeStyle.FACTORY);
            s.setGeometry(FF.property("geom")); s.setLabel(FF.literal("Label"));
            cases.add(new Case(feature(origin), s, "Default: font 10, black, centered on point"));
        }
        {
            final var f = feature(origin); f.setPropertyValue("name", "Paris");
            final var s = text("ignored", 20); s.setLabel(FF.property("name", String.class));
            cases.add(new Case(f, s, "Label from attribute: \"Paris\""));
        }
        for (double size : new double[] {10, 20, 32}) {
            cases.add(new Case(feature(origin), text("Size", size), "Font size " + size));
        }
        for (String family : new String[] {"serif", "sans-serif", "monospace"}) {
            final var s = text("Family", 20); s.getFont().family().set(0, FF.literal(family));
            cases.add(new Case(feature(origin), s, "Font family " + family));
        }
        {
            final var s = text("Family", 20); s.getFont().family().set(0, FF.literal("No such font"));
            s.getFont().family().add(FF.literal("monospace"));
            cases.add(new Case(feature(origin), s, "Font family fallback: unknown, then monospace"));
        }
        {
            final var s = text("Italic", 24); s.getFont().setStyle(FF.literal("italic"));
            cases.add(new Case(feature(origin), s, "Font style italic"));
        }
        {
            final var s = text("Bold", 24); s.getFont().setWeight(FF.literal("bold"));
            cases.add(new Case(feature(origin), s, "Font weight bold"));
        }
        {
            final var s = text("Both", 24); s.getFont().setWeight(FF.literal("bold")); s.getFont().setStyle(FF.literal("italic"));
            cases.add(new Case(feature(origin), s, "Font bold italic"));
        }

        // ---- Fill and halo ----
        {
            final var s = text("Red", 28); s.setFill(fill(Color.RED));
            cases.add(new Case(feature(origin), s, "Fill color red"));
        }
        {
            final var s = text("Faded", 28); s.setFill(fill(Color.BLUE)); s.getFill().setOpacity(FF.literal(0.4));
            cases.add(new Case(feature(origin), s, "Fill opacity 0.4"));
        }
        {
            final var s = text("Fill", 44); s.getFont().setWeight(FF.literal("bold"));
            s.getFill().setGraphicFill(graphicFill(mark("circle", Color.RED, null), 5));
            cases.add(new Case(feature(origin), s, "Fill graphic fill: red dots"));
        }
        {
            final var s = text("Halo", 24); s.setHalo(new Halo<>(FeatureTypeStyle.FACTORY));
            cases.add(new Case(feature(origin), s, "Halo default: radius 1, white"));
        }
        {
            final var s = text("Halo", 24); s.setHalo(halo(Color.YELLOW, 3));
            cases.add(new Case(feature(origin), s, "Halo radius 3, yellow"));
        }
        {
            final var s = text("Halo", 24); s.setFill(fill(Color.WHITE)); s.setHalo(halo(Color.BLACK, 2));
            cases.add(new Case(feature(origin), s, "White text, black halo radius 2"));
        }
        {
            final var s = text("Halo", 24); s.setHalo(halo(Color.BLUE, 5)); s.getHalo().get().getFill().setOpacity(FF.literal(0.3));
            cases.add(new Case(feature(origin), s, "Halo radius 5, blue, opacity 0.3"));
        }

        // ---- Point placement ----
        for (double[] anchor : new double[][] {{0, 0}, {1, 1}, {0, 0.5}, {0.5, 0}}) {
            final var s = text("Anchor", 20); final var pp = pointPlacement(s);
            pp.getAnchorPoint().setAnchorPointX(FF.literal(anchor[0]));
            pp.getAnchorPoint().setAnchorPointY(FF.literal(anchor[1]));
            cases.add(new Case(feature(origin), s, "Anchor point (" + anchor[0] + ", " + anchor[1] + ")"));
        }
        {
            final var s = text("Moved", 20); final var pp = pointPlacement(s);
            pp.getDisplacement().setDisplacementX(FF.literal(20.0)); pp.getDisplacement().setDisplacementY(FF.literal(30.0));
            cases.add(new Case(feature(origin), s, "Displacement (20, 30): right and up"));
        }
        for (double rotation : new double[] {30, 90, -45}) {
            final var s = text("Rotated", 20); pointPlacement(s).setRotation(FF.literal(rotation));
            cases.add(new Case(feature(origin), s, "Rotation " + rotation + "° (clockwise)"));
        }
        {
            final var s = text("Rotated", 20); final var pp = pointPlacement(s);
            pp.getAnchorPoint().setAnchorPointX(FF.literal(0.0));
            pp.setRotation(FF.literal(-45.0));
            cases.add(new Case(feature(origin), s, "Anchor (0, 0.5), rotation -45°: rotates around anchor"));
        }

        // ---- Line placement ----
        {
            final var s = text("Along the curve", 14); linePlacement(s);
            cases.add(new Case(feature(curve()), s, "Line placement: aligned, centered"));
        }
        for (double offset : new double[] {12, -12}) {
            final var s = text("Offset", 14); linePlacement(s).setPerpendicularOffset(FF.literal(offset));
            cases.add(new Case(feature(curve()), s, "Line placement offset " + offset + " (positive = left)"));
        }
        {
            final var s = text("Horizontal", 14); linePlacement(s).setAligned(FF.literal(false));
            cases.add(new Case(feature(curve()), s, "Line placement not aligned"));
        }
        {
            final var s = text("Rep", 12); final var lp = linePlacement(s);
            lp.setRepeated(FF.literal(true)); lp.setGap(FF.literal(10.0));
            cases.add(new Case(feature(curve()), s, "Repeated, gap 10"));
        }
        {
            final var s = text("Rep", 12); final var lp = linePlacement(s);
            lp.setRepeated(FF.literal(true)); lp.setGap(FF.literal(10.0)); lp.setInitialGap(FF.literal(50.0));
            cases.add(new Case(feature(curve()), s, "Repeated, gap 10, initial gap 50"));
        }
        {
            final var s = text("Noisy line label", 13); linePlacement(s);
            cases.add(new Case(feature(noisyLine()), s, "Noisy line, not generalized"));
        }
        {
            final var s = text("Noisy line label", 13); linePlacement(s).setGeneralizeLine(FF.literal(true));
            cases.add(new Case(feature(noisyLine()), s, "Noisy line, generalized"));
        }
        {
            final var s = text("Upright", 16); linePlacement(s);
            cases.add(new Case(feature(line(50, 30,  -50, -30)), s, "Line from east to west: text kept upright"));
        }
        {
            final var s = text("Much too long label", 16); linePlacement(s);
            cases.add(new Case(feature(line(-15, 0,  15, 0)), s, "Label longer than line: not rendered"));
        }
        {
            final var s = text("Rings", 12); final var lp = linePlacement(s);
            lp.setRepeated(FF.literal(true)); lp.setGap(FF.literal(15.0));
            cases.add(new Case(feature(polygon()), s, "Line placement on polygon rings, repeated"));
        }
        {
            final var s = text("Halo", 16); linePlacement(s); s.setHalo(halo(Color.WHITE, 2));
            cases.add(new Case(feature(zigzag()), s, "Line placement with halo on zigzag"));
        }

        // ---- Unit of measure ----
        {
            final var s = text("UOM", 10); s.setUnitOfMeasure(Units.METRE);
            s.getFont().setSize(FF.literal(2500_000.0)); s.setHalo(halo(Color.YELLOW, 300_000));
            cases.add(new Case(feature(origin), s, "UOM metre: size 2500 km, halo 300 km"));
        }
        {
            final var s = text("UOM", 10); s.setUnitOfMeasure(Units.METRE);
            s.getFont().setSize(FF.literal(1500_000.0));
            linePlacement(s).setPerpendicularOffset(FF.literal(2000_000.0));
            cases.add(new Case(feature(straight()), s, "UOM metre: size 1500 km, offset 2000 km"));
        }

        // ---- Geometry ----
        {
            final var f = feature(origin); f.setPropertyValue("geom2", GF.createPoint(CRS84, 25, 35));
            final var s = text("geom2", 16); s.setGeometry(FF.property("geom2"));
            cases.add(new Case(f, s, "Geometry expression: \"geom2\" at (25, 35)"));
        }
        cases.add(new Case(feature(GF.createMultiPoint(CRS84, GF.createPoint(CRS84, -35, -30), GF.createPoint(CRS84, 0, 30),
                GF.createPoint(CRS84, 35, -10))), text("Pt", 14), "MultiPoint: one label per point"));
        cases.add(new Case(feature(lShape()), text("Inside", 14), "Polygon, point placement: label inside (concave)"));
        cases.add(new Case(feature(zigzag()), text("Middle", 14), "LineString, point placement: label at middle"));
        {
            final var s = text("Point", 16); linePlacement(s);
            cases.add(new Case(feature(origin), s, "Point with line placement: fallback on point"));
        }

        // ---- Labels computed by Symbology Encoding functions ----
        final Feature city = feature(origin);
        city.setPropertyValue("name", "  saint-étienne  ");
        city.setPropertyValue("width", 172565.0);
        final Expression<Feature,?> name = new Trim<>(FF.property("name"), Trim.StripOffPosition.BOTH, null);
        cases.add(new Case(city, label(new FormatNumber<>(FF.property("width"), FF.literal("#,##0"), null, null, null), 16),
                "FormatNumber(172565, \"#,##0\")"));
        cases.add(new Case(city, label(new FormatNumber<>(FF.property("width"), FF.literal("# ##0,0"), null,
                FF.literal(","), FF.literal(" ")), 16), "FormatNumber, decimal \",\" and grouping \" \""));
        cases.add(new Case(city, label(new FormatDate<>(FF.literal("2006-02-20T13:05:00Z"), FF.literal("DD.MM.YYYY")), 16),
                "FormatDate(2006-02-20, \"DD.MM.YYYY\")"));
        cases.add(new Case(city, label(new FormatDate<>(FF.literal("2006-02-20T13:05:00Z"), FF.literal("D MMMMMde YYYY h:mm a")), 9),
                "FormatDate, \"D MMMMMde YYYY h:mm a\""));
        cases.add(new Case(city, label(new ChangeCase<>(name, null), 14), "ChangeCase(Trim(name, both))"));
        cases.add(new Case(city, label(new Substring<>(name, FF.literal(7), null), 16), "Substring(name, 7)"));
        cases.add(new Case(city, label(new Concatenate<>(List.of(FF.literal("length "), new StringLength<>(name),
                FF.literal(", '-' at "), new StringPosition<>(FF.literal("-"), name, null))), 14),
                "StringLength and StringPosition of \"-\""));
        cases.add(new Case(city, label(new Concatenate<>(List.of(new ChangeCase<>(name, null), FF.literal(": "),
                new FormatNumber<>(FF.property("width"), FF.literal("#.##0"), null, FF.literal(","), FF.literal(".")))), 9),
                "Concatenate of functions, created in code"));
        cases.add(new Case(city, label(FF.function("Concatenate", new Expression[] {
                FF.function("Substring", new Expression[] {FF.function("ChangeCase", new Expression[] {name}), FF.literal(1), FF.literal(5)}),
                FF.literal("…")}), 16), "Functions created by name in a filter factory"));
        return cases;
    }

    /**
     * Returns the cases validating all {@link RasterSymbolizer} parameters.
     *
     * @return raster symbolizer validation cases.
     */
    public static List<Case> rasterCases() {
        final List<Case> cases = new ArrayList<>();
        final GridCoverage field = field(new String[] {"elevation"}, (x, y) -> wave(x, y));
        final GridCoverage rgbField = field(new String[] {"red", "green", "blue"},
                (x, y) -> 128 + 127 * Math.sin(x / 15), (x, y) -> 128 + 127 * Math.cos(y / 12), (x, y) -> 128 + 127 * Math.sin((x + y) / 20));

        // ---- Default rendering ----
        cases.add(new Case(raster(field), rasterSymbolizer(), "Default, 1 band: gray, min/max normalized"));
        cases.add(new Case(raster(rgbField), rasterSymbolizer(), "Default, 3 bands: RGB, normalized"));
        cases.add(new Case(raster(imageCoverage()), rasterSymbolizer(), "Default, RGB image: own colors"));
        cases.add(new Case(raster(field(new String[] {"v"}, (x, y) -> (Math.hypot(x, y) < 20) ? Double.NaN : wave(x, y))),
                rasterSymbolizer(), "NaN values: transparent hole"));
        cases.add(new Case(raster(latLonCoverage()), rasterSymbolizer(), "Coverage in (lat, lon) CRS: reprojected"));

        // ---- Opacity ----
        {
            final var s = rasterSymbolizer(); s.setOpacity(FF.literal(0.4));
            cases.add(new Case(raster(rgbField), s, "Opacity 0.4"));
        }

        // ---- Channel selection ----
        {
            final var s = rasterSymbolizer(); s.setChannelSelection(channels("blue", "green", "red"));
            cases.add(new Case(raster(rgbField), s, "Channels RGB = blue, green, red (swapped)"));
        }
        {
            final var s = rasterSymbolizer(); s.setChannelSelection(channels("green"));
            cases.add(new Case(raster(rgbField), s, "Gray channel = green (by name)"));
        }
        {
            final var s = rasterSymbolizer(); s.setChannelSelection(channels("3"));
            cases.add(new Case(raster(rgbField), s, "Gray channel = 3 (band number)"));
        }
        {
            final var s = rasterSymbolizer(); final var cs = channels("red", "green", "blue");
            cs.getChannels()[0].setContrastEnhancement(contrast(ContrastMethod.HISTOGRAM, 1));
            s.setChannelSelection(cs);
            cases.add(new Case(raster(lowContrast(3)), s, "Low contrast RGB, histogram on red channel only"));
        }

        // ---- Contrast enhancement ----
        final GridCoverage dull = lowContrast(1);
        cases.add(new Case(raster(dull), contrastSymbolizer(contrast(ContrastMethod.NONE, 1)), "Low contrast values 100-140, no enhancement"));
        cases.add(new Case(raster(dull), contrastSymbolizer(contrast(ContrastMethod.NORMALIZE, 1)), "Contrast normalize"));
        cases.add(new Case(raster(dull), contrastSymbolizer(contrast(ContrastMethod.HISTOGRAM, 1)), "Contrast histogram equalization"));
        cases.add(new Case(raster(dull), contrastSymbolizer(contrast(ContrastMethod.NORMALIZE, 2)), "Contrast normalize, gamma 2 (brighter)"));
        cases.add(new Case(raster(dull), contrastSymbolizer(contrast(ContrastMethod.NORMALIZE, 0.5)), "Contrast normalize, gamma 0.5 (darker)"));
        cases.add(new Case(raster(lowContrast(3)), contrastSymbolizer(contrast(ContrastMethod.NORMALIZE, 1)), "Low contrast RGB, normalize all"));

        // ---- Color map ----
        {
            final var c = new Categorize<Feature,Color>(null,
                    List.of(FF.literal(new Color(0, 0, 160)), FF.literal(new Color(80, 160, 255)),
                            FF.literal(new Color(60, 170, 60)), FF.literal(new Color(150, 110, 60))),
                    List.of(FF.literal(-0.5), FF.literal(0.0), FF.literal(0.5)), null, null);
            cases.add(new Case(raster(field), colorMapSymbolizer(c), "Categorize: 4 classes, thresholds -0.5, 0, 0.5"));
        }
        {
            final var c = new Categorize<Feature,Color>(null, List.of(FF.literal(Color.YELLOW), FF.literal(Color.RED)),
                    List.of(FF.literal(0.0)), null, FF.literal(Color.MAGENTA));
            cases.add(new Case(raster(field(new String[] {"v"}, (x, y) -> (Math.hypot(x, y) < 20) ? Double.NaN : wave(x, y))),
                    colorMapSymbolizer(c), "Categorize with fallback (magenta) for NaN"));
        }
        {
            final Expression<Feature,?> c = FF.function("Categorize", new Expression[] {FF.literal("Rasterdata"),
                    FF.literal("#203060"), FF.literal(0.0), FF.literal("#E0C080")});
            cases.add(new Case(raster(field), colorMapSymbolizer(c), "Categorize created by name, colors as strings"));
        }
        for (Interpolate.Mode mode : Interpolate.Mode.values()) {
            final var i = new Interpolate<Feature>(null, List.of(point(-1, Color.BLUE), point(0, Color.WHITE), point(1, Color.RED)),
                    mode, null, null);
            cases.add(new Case(raster(field), colorMapSymbolizer(i), "Interpolate " + mode.name().toLowerCase() + ": blue, white, red"));
        }
        {
            final var i = new Interpolate<Feature>(null, List.of(point(-0.3, Color.BLACK), point(0.3, Color.GREEN)), null, null, null);
            cases.add(new Case(raster(field), colorMapSymbolizer(i), "Interpolate [-0.3, 0.3]: clamped outside"));
        }
        {
            final var i = new Interpolate<Feature>(null, List.of(point(0, Color.BLACK), point(255, Color.ORANGE)), null, null, null);
            final var s = colorMapSymbolizer(i); s.setChannelSelection(channels("blue"));
            cases.add(new Case(raster(rgbField), s, "Interpolate on gray channel \"blue\""));
        }
        {
            final var i = new Interpolate<Feature>(FF.property("green"), List.of(point(0, Color.BLACK), point(255, Color.CYAN)), null, null, null);
            cases.add(new Case(raster(rgbField), colorMapSymbolizer(i), "Interpolate with lookup value \"green\" band"));
        }

        // ---- Overlap behavior ----
        for (OverlapBehavior behavior : OverlapBehavior.values()) {
            final var s = rasterSymbolizer(); s.setOverlapBehavior(behavior);
            cases.add(new Case(raster(overlapping()), s, "Overlap " + behavior.name().toLowerCase().replace('_', ' ')
                    + ": red first, blue second"));
        }

        // ---- Image outline ----
        {
            final var s = rasterSymbolizer(); final var line = new LineSymbolizer<>(SF);
            line.getStroke().setColor(FF.literal(Color.RED)); line.getStroke().setWidth(FF.literal(3.0));
            s.setImageOutline(line);
            cases.add(new Case(raster(field), s, "Image outline: red line 3px"));
        }
        {
            final var s = rasterSymbolizer(); final var polygon = new PolygonSymbolizer<>(SF);
            polygon.setFill(fill(new Color(255, 255, 0, 90))); polygon.setStroke(stroke(Color.BLACK, 2));
            s.setImageOutline(polygon);
            cases.add(new Case(raster(field), s, "Image outline: translucent yellow polygon"));
        }
        {
            final var s = rasterSymbolizer(); final var line = new LineSymbolizer<>(SF);
            line.getStroke().setColor(FF.literal(Color.BLACK)); line.getStroke().setWidth(FF.literal(2.0));
            s.setImageOutline(line);
            cases.add(new Case(raster(latLonCoverage()), s, "Image outline of reprojected coverage"));
        }

        // ---- Shaded relief ----
        final GridCoverage dem = field(new String[] {"elevation"}, (x, y) -> 4000 * (1 + Math.sin(x / 4) * Math.cos(y / 5)));
        final var terrain = new Interpolate<Feature>(null, List.of(point(0, new Color(60, 140, 60)),
                point(4000, new Color(210, 190, 120)), point(8000, Color.WHITE)), null, null, null);
        cases.add(new Case(raster(dem), shadedRelief(null, 55, false), "Shaded relief on default gray, factor 55"));
        cases.add(new Case(raster(dem), shadedRelief(terrain, 55, false), "Shaded relief on color map, factor 55"));
        cases.add(new Case(raster(dem), shadedRelief(terrain, 10, false), "Shaded relief factor 10 (flatter)"));
        cases.add(new Case(raster(dem), shadedRelief(terrain, 200, false), "Shaded relief factor 200 (steeper)"));
        cases.add(new Case(raster(dem), shadedRelief(null, 55, true), "Brightness only: shading of the tile background"));
        return cases;
    }

    /**
     * Returns the cases validating external graphics (images, SVG, inline content, color replacement)
     * and font marks. The image files are generated in a temporary directory.
     *
     * @return external graphic validation cases.
     */
    public static List<Case> externalCases() {
        final List<Case> cases = new ArrayList<>();
        final Geometry origin = GF.createPoint(CRS84, 0, 0);
        final URI png, svg, missing;
        try {
            final Path dir = Files.createTempDirectory("sis-validation");
            dir.toFile().deleteOnExit();
            png = writePin(dir.resolve("pin.png"));
            svg = writeHouse(dir.resolve("house.svg"));
            missing = dir.resolve("missing.png").toUri();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // ---- Raster images ----
        cases.add(new Case(feature(origin), point(external(png, "image/png"), null), "PNG image, native size (32×48)"));
        cases.add(new Case(feature(origin), point(external(png, "image/png"), 24.0), "PNG image, size 24 (height)"));
        cases.add(new Case(feature(origin), point(external(png, "image/png"), 80.0), "PNG image, size 80 (upscaled)"));
        {
            final var s = point(external(png, "image/png"), null); s.getGraphic().setRotation(FF.literal(30.0));
            cases.add(new Case(feature(origin), s, "PNG image, rotation 30°"));
        }
        {
            final var s = point(external(png, "image/png"), null); s.getGraphic().setOpacity(FF.literal(0.4));
            cases.add(new Case(feature(origin), s, "PNG image, opacity 0.4"));
        }
        {
            final var s = point(external(png, "image/png"), null);
            s.getGraphic().getAnchorPoint().setAnchorPointY(FF.literal(0.0));
            cases.add(new Case(feature(origin), s, "PNG image, anchor (0.5, 0): pin tip on point"));
        }
        {
            final var e = external(png, "image/png");
            final ColorReplacement<Feature> replacement = new ColorReplacement<>(SF);
            replacement.setFunction(new Recode<>(null, List.of(
                    new MapItem<>(FF.literal("#E02020"), FF.literal(new Color(30, 160, 30))),
                    new MapItem<>(FF.literal("#FFFFFF"), FF.literal(Color.YELLOW))), null));
            e.colorReplacements().add(replacement);
            cases.add(new Case(feature(origin), point(e, null), "Color replacement: red → green, white → yellow"));
        }

        // ---- SVG and inline content ----
        cases.add(new Case(feature(origin), point(external(svg, "image/svg+xml"), null), "SVG document, native size (40×40)"));
        cases.add(new Case(feature(origin), point(external(svg, "image/svg+xml"), 100.0), "SVG document, size 100 (crisp)"));
        {
            final ExternalGraphic<Feature> e = new ExternalGraphic<>(SF);
            final BufferedImage icon = new BufferedImage(30, 20, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = icon.createGraphics();
            g.setColor(Color.ORANGE); g.fillRoundRect(0, 0, 30, 20, 8, 8);
            g.setColor(Color.BLACK);  g.drawRoundRect(0, 0, 29, 19, 8, 8);
            g.dispose();
            e.setInlineContent(new javax.swing.ImageIcon(icon));
            cases.add(new Case(feature(origin), point(e, null), "Inline content (Icon 30×20)"));
        }
        {
            final var s = pointSymbolizer();
            s.getGraphic().graphicalSymbols().add(external(missing, "image/png"));
            s.getGraphic().graphicalSymbols().add(mark("star", Color.RED, Color.BLACK));
            s.getGraphic().setSize(FF.literal(30.0));
            cases.add(new Case(feature(origin), s, "Missing image, then star mark: uses the mark"));
        }
        cases.add(new Case(feature(origin), point(external(missing, "image/png"), null), "Missing image only: nothing drawn"));

        // ---- External graphics in strokes and fills ----
        {
            final var s = symbolizer();
            final GraphicStroke<Feature> gs = new GraphicStroke<>(SF);
            gs.getGraphic().graphicalSymbols().add(external(png, "image/png"));
            gs.getGraphic().setSize(FF.literal(18.0));
            gs.getGraphic().setRotation(FF.literal(90.0));
            gs.setGap(FF.literal(4.0));
            s.getStroke().setGraphicStroke(gs);
            cases.add(new Case(feature(curve()), s, "Graphic stroke with PNG image, size 18"));
        }
        {
            final var s = fillOnly(Color.RED);
            final GraphicFill<Feature> gf = new GraphicFill<>(SF);
            gf.getGraphic().graphicalSymbols().add(external(svg, "image/svg+xml"));
            gf.getGraphic().setSize(FF.literal(20.0));
            s.getFill().get().setGraphicFill(gf);
            cases.add(new Case(feature(polygon()), s, "Polygon graphic fill with SVG, size 20"));
        }

        // ---- Font marks ----
        for (String wkn : new String[] {"ttf://SansSerif#0x41", "ttf://Serif#0x26", "ttf://SansSerif#0x2605", "ttf://Monospaced#64"}) {
            cases.add(new Case(feature(origin), point(mark(wkn, new Color(40, 90, 200), Color.BLACK), 50),
                    "Font mark " + wkn.substring(6)));
        }
        {
            final var s = point(mark("ttf://SansSerif#0x2665", Color.RED, null), 40);
            s.getGraphic().setRotation(FF.literal(20.0));
            cases.add(new Case(feature(origin), s, "Font mark SansSerif#0x2665 (heart), rotation 20°"));
        }
        cases.add(new Case(feature(origin), point(mark("ttf://No such font#0x41", Color.RED, Color.BLACK), 50),
                "Font mark with unknown font: nothing drawn"));
        final URI fontFile = findFontFile();
        if (fontFile != null) {
            for (int index : new int[] {36, 68}) {
                final var m = mark("square", new Color(150, 60, 150), Color.BLACK);
                m.setOnlineResource(new DefaultOnlineResource(fontFile));
                m.setFormat("ttf");
                m.setMarkIndex(FF.literal(index));
                final String name = Path.of(fontFile).getFileName().toString();
                cases.add(new Case(feature(origin), point(m, 50), "Font file " + name + ", glyph index " + index));
            }
        }
        return cases;
    }

    /**
     * Creates a point symbolizer with the given external graphic and size (null for native size).
     */
    private static PointSymbolizer<Feature> point(ExternalGraphic<Feature> external, Double size) {
        final PointSymbolizer<Feature> symbolizer = pointSymbolizer();
        symbolizer.getGraphic().graphicalSymbols().add(external);
        if (size != null) symbolizer.getGraphic().setSize(FF.literal(size));
        return symbolizer;
    }

    /**
     * Creates an external graphic referencing the given resource.
     */
    private static ExternalGraphic<Feature> external(URI uri, String format) {
        final ExternalGraphic<Feature> external = new ExternalGraphic<>(SF);
        external.setOnlineResource(new DefaultOnlineResource(uri));
        external.setFormat(format);
        return external;
    }

    /**
     * Writes a 32×48 PNG image of a red map pin with a white center, the tip at the bottom.
     */
    private static URI writePin(Path file) throws IOException {
        final BufferedImage image = new BufferedImage(32, 48, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        final java.awt.geom.Path2D pin = new java.awt.geom.Path2D.Double();
        pin.moveTo(16, 47);
        pin.curveTo(4, 30, 1, 24, 1, 16);
        pin.curveTo(1, 7, 8, 1, 16, 1);
        pin.curveTo(24, 1, 31, 7, 31, 16);
        pin.curveTo(31, 24, 28, 30, 16, 47);
        pin.closePath();
        g.setColor(new Color(0xE0, 0x20, 0x20));
        g.fill(pin);
        g.setColor(Color.BLACK);
        g.draw(pin);
        g.setColor(Color.WHITE);
        g.fillOval(9, 9, 14, 14);
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
        file.toFile().deleteOnExit();
        return file.toUri();
    }

    /**
     * Writes a 40×40 SVG document of a house.
     */
    private static URI writeHouse(Path file) throws IOException {
        Files.writeString(file, "<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40'>"
                + "<polygon points='20,2 38,18 32,18 32,38 8,38 8,18 2,18' fill='#F4E2C0' stroke='#603010' stroke-width='2'/>"
                + "<rect x='16' y='24' width='8' height='14' fill='#A05020'/>"
                + "<rect x='11' y='20' width='6' height='5' fill='#60A0E0'/>"
                + "</svg>", java.nio.charset.StandardCharsets.UTF_8);
        file.toFile().deleteOnExit();
        return file.toUri();
    }

    /**
     * Searches a TrueType font file in common system directories.
     *
     * @return URI of a font file, or {@code null} if none.
     */
    private static URI findFontFile() {
        for (String dir : new String[] {"/usr/share/fonts", "/usr/local/share/fonts", "/Library/Fonts", "C:\\Windows\\Fonts"}) {
            final Path path = Path.of(dir);
            if (Files.isDirectory(path)) {
                try (java.util.stream.Stream<Path> files = Files.walk(path)) {
                    final var font = files.filter((p) -> p.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ttf")).sorted().findFirst();
                    if (font.isPresent()) return font.get().toUri();
                } catch (IOException | UncheckedIOException e) {
                    // Try next directory.
                }
            }
        }
        return null;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Rendering //////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Renders all given cases in a single image, with their descriptions.
     *
     * @param  title  title written at the top of the image.
     * @param  cases  the cases to render.
     * @return the aggregated image.
     */
    public static BufferedImage render(String title, List<Case> cases) {
        final Map<String, List<Case>> sections = new LinkedHashMap<>();
        sections.put(null, cases);
        return render(title, sections);
    }

    /**
     * Renders all given cases in a single image, grouped in sections.
     * Each section starts on a new row with its title.
     *
     * @param  title     title written at the top of the image.
     * @param  sections  the cases to render, by section title. A null section title is not written.
     * @return the aggregated image.
     */
    public static BufferedImage render(String title, Map<String, List<Case>> sections) {
        final int cellWidth  = TILE_SIZE + 2 * MARGIN;
        final int cellHeight = TILE_SIZE + TEXT_HEIGHT + 2 * MARGIN;
        int columns = 1;
        for (List<Case> cases : sections.values()) {
            columns = Math.max(columns, Math.min(COLUMNS, cases.size()));
        }
        int height = HEADER_HEIGHT;
        for (Map.Entry<String, List<Case>> section : sections.entrySet()) {
            if (section.getKey() != null) height += SECTION_HEIGHT;
            height += ((section.getValue().size() + columns - 1) / columns) * cellHeight;
        }
        final BufferedImage image = new BufferedImage(columns * cellWidth, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            g.drawString(title, MARGIN, 22);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.setColor(Color.DARK_GRAY);
            g.drawString("Magenta line: source geometry. Magenta cross: point position. Tile grid: 1 pixel = 1° in CRS:84, origin at tile center.", MARGIN, 40);

            final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
            final Font sectionFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
            int top = HEADER_HEIGHT;
            for (Map.Entry<String, List<Case>> section : sections.entrySet()) {
                if (section.getKey() != null) {
                    g.setColor(new Color(60, 60, 60));
                    g.fillRect(0, top + 4, image.getWidth(), SECTION_HEIGHT - 8);
                    g.setColor(Color.WHITE);
                    g.setFont(sectionFont);
                    g.drawString(section.getKey(), MARGIN, top + SECTION_HEIGHT / 2 + 5);
                    top += SECTION_HEIGHT;
                }
                final List<Case> cases = section.getValue();
                for (int i = 0; i < cases.size(); i++) {
                    final Case c = cases.get(i);
                    final int x = (i % columns) * cellWidth + MARGIN;
                    final int y = top + (i / columns) * cellHeight + MARGIN;

                    // Tile background and frame.
                    g.setColor(new Color(240, 240, 240));
                    g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    g.setColor(new Color(225, 225, 225));
                    g.drawLine(x + TILE_SIZE / 2, y, x + TILE_SIZE / 2, y + TILE_SIZE - 1);
                    g.drawLine(x, y + TILE_SIZE / 2, x + TILE_SIZE - 1, y + TILE_SIZE / 2);

                    String error = null;
                    try {
                        g.drawImage(renderTile(c), x, y, null);
                        error = linearizationProblems(c);
                    } catch (RenderingException | RuntimeException ex) {
                        error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    }
                    g.setColor(Color.GRAY);
                    g.drawRect(x - 1, y - 1, TILE_SIZE + 1, TILE_SIZE + 1);

                    // Description.
                    g.setFont(font);
                    final FontMetrics fm = g.getFontMetrics();
                    int ty = y + TILE_SIZE + fm.getAscent() + 3;
                    g.setColor(Color.BLACK);
                    for (String line : wrap(c.description(), fm, cellWidth - 4, 2)) {
                        g.drawString(line, x - MARGIN + 2, ty);
                        ty += fm.getHeight();
                    }
                    if (error != null) {
                        g.setColor(Color.RED);
                        for (String line : wrap(error, fm, cellWidth - 4, 2)) {
                            g.drawString(line, x - MARGIN + 2, ty);
                            ty += fm.getHeight();
                        }
                    }
                }
                top += ((cases.size() + columns - 1) / columns) * cellHeight;
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Renders a single case in a transparent tile, with the source geometry drawn on top.
     *
     * @param  c  the case to render.
     * @return the rendered tile.
     * @throws RenderingException if the rendering failed.
     */
    public static BufferedImage renderTile(Case c) throws RenderingException {
        final BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = tile.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            final Scene2D scene = new Scene2D(GRID, g);
            paint(scene, c.feature(), c.symbolizer());

            final LineSymbolizer<Feature> reference = new LineSymbolizer<>(FeatureTypeStyle.FACTORY);
            reference.setGeometry(c.symbolizer().getGeometry());
            reference.getStroke().setColor(FF.literal(REFERENCE_COLOR));
            reference.getStroke().setWidth(FF.literal(1.0));
            paint(scene, c.feature(), reference);
            if (c.symbolizer() instanceof PointSymbolizer<?> || (c.symbolizer() instanceof TextSymbolizer<?> ts
                    && !(ts.getLabelPlacement() instanceof LinePlacement<?>)))
            {
                // Show where the graphics are anchored.
                final PointSymbolizer<Feature> anchor = new PointSymbolizer<>(FeatureTypeStyle.FACTORY);
                anchor.setGeometry(c.symbolizer().getGeometry());
                anchor.getGraphic().graphicalSymbols().add(mark("cross", REFERENCE_COLOR, null));
                anchor.getGraphic().setSize(FF.literal(7.0));
                paint(scene, c.feature(), anchor);
            }
        } finally {
            g.dispose();
        }
        return tile;
    }

    /**
     * Returns a description of the curves of the case geometry which cannot be linearized,
     * and therefore are not rendered.
     *
     * @return the problems, or {@code null} if none.
     */
    private static String linearizationProblems(Case c) {
        final Geometry geometry = GeometryResolver.resolve(SymbolizerToScene2D.evaluate(c.feature(), c.symbolizer().getGeometry()));
        final StringBuilder problems = new StringBuilder();
        GeometryResolver.linearComponents(geometry, new ArrayList<>(), (curve, ex) -> {
            if (problems.length() != 0) problems.append(' ');
            problems.append("Not rendered: ").append(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        });
        return (problems.length() != 0) ? problems.toString() : null;
    }

    /**
     * Paints the given feature with the given symbolizer.
     */
    private static void paint(Scene2D scene, Feature feature, Symbolizer<Feature> symbolizer) throws RenderingException {
        final SymbolizerToScene2D<?> renderer = SymbolizerToScene2D.create(scene, symbolizer);
        if (renderer == null) {
            throw new RenderingException("No renderer for " + symbolizer.getClass().getSimpleName());
        }
        renderer.paint(new SEPresentation(null, null, feature, symbolizer), (presentations) -> presentations.close());
    }

    /**
     * Splits the given text in lines fitting in the given width.
     */
    private static List<String> wrap(String text, FontMetrics fm, int width, int maxLines) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        boolean truncated = false;
        for (String word : text.split("\\s+")) {
            if (line.length() != 0 && fm.stringWidth(line + " " + word) > width) {
                if (lines.size() == maxLines - 1) {
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

    // ////////////////////////////////////////////////////////////////////////
    // Features and styles ////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Creates a feature with the given geometry in the {@code "geom"} attribute.
     */
    private static Feature feature(Geometry geom) {
        final Feature feature = TYPE.newInstance();
        feature.setPropertyValue("geom", geom);
        return feature;
    }

    /**
     * Creates a line string in CRS:84 from (x,y) tuples.
     */
    private static LineString line(double... xy) {
        return GF.createLineString(GF.createDataPoints(CRS84, xy));
    }

    /**
     * Horizontal line from west to east.
     */
    private static LineString straight() {
        return line(-50, 0, 50, 0);
    }

    /**
     * Line with sharp angles, for joins and offsets.
     */
    private static LineString zigzag() {
        return line(-50, -20,  -20, 30,  10, -25,  50, 25);
    }

    /**
     * Smooth line made of many points, for dashes and graphic strokes.
     */
    private static LineString curve() {
        final double[] xy = new double[2 * 41];
        for (int i = 0; i <= 40; i++) {
            final double x = -50 + i * 2.5;
            xy[i * 2]     = x;
            xy[i * 2 + 1] = 30 * Math.sin(x / 50 * Math.PI);
        }
        return line(xy);
    }

    /**
     * Creates data points in CRS:84 from (x,y) tuples.
     */
    private static DataPoints points(double... xy) {
        return GF.createDataPoints(CRS84, xy);
    }

    /**
     * Creates a vector of double values.
     */
    private static Vector<?> vector(double... values) {
        return Vectors.create(values, DataType.DOUBLE);
    }

    /**
     * Coordinates of the points of the ellipse (x/50)² + (y/30)² = 1 at the given parameters in radians.
     */
    private static double[] ellipse(double... t) {
        final double[] xy = new double[t.length * 2];
        for (int i = 0; i < t.length; i++) {
            xy[2*i]     = 50 * Math.cos(t[i]);
            xy[2*i + 1] = 30 * Math.sin(t[i]);
        }
        return xy;
    }

    /**
     * Creates a real function from the given operator.
     */
    @SuppressWarnings("rawtypes")
    private static RealFunction function(DoubleUnaryOperator op) {
        return new RealFunction() {
            @Override public GenericName getName()       {return null;}
            @Override public Range       getDomain()     {return null;}
            @Override public List<URI>   getMetadata()   {return List.of();}
            @Override public double      value(double r) {return op.applyAsDouble(r);}
        };
    }

    /**
     * Compound curve made of a line, a circular arc and a line, from west to east.
     */
    private static CompoundCurve compoundCurve() {
        return GF.createCompoundCurve(CRS84,
                line(-50, -30,  -20, -30),
                GF.createCircularString(points(-20, -30,  0, 10,  20, -30)),
                line(20, -30,  50, 30));
    }

    /**
     * Style used for curve cases: black stroke of 3 pixels.
     */
    private static LineSymbolizer<Feature> curveStyle() {
        final LineSymbolizer<Feature> s = symbolizer();
        s.getStroke().setWidth(FF.literal(3.0));
        return s;
    }

    /**
     * Style showing the curve direction with triangles pointing forward.
     */
    private static LineSymbolizer<Feature> arrowStyle() {
        final GraphicStroke<Feature> gs = graphicStroke(mark("triangle", Color.RED, Color.BLACK), 10);
        gs.setGap(FF.literal(8.0));
        gs.getGraphic().setRotation(FF.literal(90.0));
        final LineSymbolizer<Feature> s = symbolizer();
        s.getStroke().setGraphicStroke(gs);
        return s;
    }

    /**
     * Square polygon with a square hole.
     */
    private static Geometry polygon() {
        final LinearRing exterior = GF.createLinearRing(GF.createDataPoints(CRS84,
                -45.0, -45.0,  45.0, -45.0,  45.0, 45.0,  -45.0, 45.0,  -45.0, -45.0));
        final LinearRing hole = GF.createLinearRing(GF.createDataPoints(CRS84,
                -20.0, -20.0,  -20.0, 20.0,  20.0, 20.0,  20.0, -20.0,  -20.0, -20.0));
        return GF.createPolygon(exterior, List.of(hole));
    }

    /**
     * Square polygon centered on (0,0) with the given half size.
     */
    private static Geometry square(double half) {
        return squareAt(0, 0, half);
    }

    /**
     * Square polygon centered on (cx,cy) with the given half size.
     */
    private static Polygon squareAt(double cx, double cy, double half) {
        return GF.createPolygon(GF.createLinearRing(points(
                cx - half, cy - half,  cx + half, cy - half,  cx + half, cy + half,  cx - half, cy + half,  cx - half, cy - half)), List.of());
    }

    /**
     * Concave L-shaped polygon.
     */
    private static Geometry lShape() {
        return GF.createPolygon(GF.createLinearRing(points(
                -45, -45,  45, -45,  45, -10,  -10, -10,  -10, 45,  -45, 45,  -45, -45)), List.of());
    }

    /**
     * Five branches star polygon, concave.
     */
    private static Geometry star() {
        final double[] xy = new double[2 * 11];
        for (int i = 0; i <= 10; i++) {
            final double angle = Math.PI / 2 + i * Math.PI / 5;
            final double radius = (i % 2 == 0) ? 50 : 22;
            xy[2*i]     = radius * Math.cos(angle);
            xy[2*i + 1] = radius * Math.sin(angle) - 5;
        }
        return GF.createPolygon(GF.createLinearRing(points(xy)), List.of());
    }

    /**
     * Creates a polygon symbolizer rendering the {@code "geom"} attribute, with default fill and stroke.
     */
    private static PolygonSymbolizer<Feature> polygonSymbolizer() {
        final PolygonSymbolizer<Feature> symbolizer = new PolygonSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.setGeometry(FF.property("geom"));
        return symbolizer;
    }

    /**
     * Creates a polygon symbolizer with a fill of the given color and no stroke.
     */
    private static PolygonSymbolizer<Feature> fillOnly(Color color) {
        final PolygonSymbolizer<Feature> symbolizer = polygonSymbolizer();
        final Fill<Feature> fill = new Fill<>(FeatureTypeStyle.FACTORY);
        fill.setColor(FF.literal(color));
        symbolizer.setFill(fill);
        symbolizer.setStroke(null);
        return symbolizer;
    }

    /**
     * Creates a stroke of the given color and width.
     */
    private static Stroke<Feature> stroke(Color color, double width) {
        final Stroke<Feature> stroke = new Stroke<>(FeatureTypeStyle.FACTORY);
        stroke.setColor(FF.literal(color));
        stroke.setWidth(FF.literal(width));
        return stroke;
    }

    /**
     * Creates a point symbolizer rendering the {@code "geom"} attribute, with the default graphic.
     */
    private static PointSymbolizer<Feature> pointSymbolizer() {
        final PointSymbolizer<Feature> symbolizer = new PointSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.setGeometry(FF.property("geom"));
        return symbolizer;
    }

    /**
     * Creates a point symbolizer drawing the given mark with the given size.
     */
    private static PointSymbolizer<Feature> point(Mark<Feature> mark, double size) {
        final PointSymbolizer<Feature> symbolizer = pointSymbolizer();
        symbolizer.getGraphic().graphicalSymbols().add(mark);
        symbolizer.getGraphic().setSize(FF.literal(size));
        return symbolizer;
    }

    /**
     * Creates a text symbolizer rendering the {@code "geom"} attribute with the given label and sans-serif font size.
     */
    private static TextSymbolizer<Feature> text(String label, double size) {
        final TextSymbolizer<Feature> symbolizer = new TextSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.setGeometry(FF.property("geom"));
        symbolizer.setLabel(FF.literal(label));
        symbolizer.getFont().family().add(FF.literal("sans-serif"));
        symbolizer.getFont().setSize(FF.literal(size));
        return symbolizer;
    }

    /**
     * Creates a text symbolizer with the given label expression and font size.
     */
    @SuppressWarnings("unchecked")
    private static TextSymbolizer<Feature> label(Expression<Feature,?> label, double size) {
        final TextSymbolizer<Feature> symbolizer = text("", size);
        symbolizer.setLabel((Expression<Feature,String>) label);
        return symbolizer;
    }

    private static PointPlacement<Feature> pointPlacement(TextSymbolizer<Feature> symbolizer) {
        final PointPlacement<Feature> placement = new PointPlacement<>(FeatureTypeStyle.FACTORY);
        symbolizer.setLabelPlacement(placement);
        return placement;
    }

    private static LinePlacement<Feature> linePlacement(TextSymbolizer<Feature> symbolizer) {
        final LinePlacement<Feature> placement = new LinePlacement<>(FeatureTypeStyle.FACTORY);
        symbolizer.setLabelPlacement(placement);
        return placement;
    }

    private static Fill<Feature> fill(Color color) {
        final Fill<Feature> fill = new Fill<>(FeatureTypeStyle.FACTORY);
        fill.setColor(FF.literal(color));
        return fill;
    }

    private static Halo<Feature> halo(Color color, double radius) {
        final Halo<Feature> halo = new Halo<>(FeatureTypeStyle.FACTORY);
        halo.setFill(fill(color));
        halo.setRadius(FF.literal(radius));
        return halo;
    }

    /**
     * Horizontal line with a small zigzag noise.
     */
    private static LineString noisyLine() {
        final double[] xy = new double[2 * 31];
        for (int i = 0; i <= 30; i++) {
            xy[2*i]     = -60 + i * 4;
            xy[2*i + 1] = (i % 2 == 0) ? -2 : 2;
        }
        return line(xy);
    }

    /**
     * Shared style factory.
     */
    private static final StyleFactory<Feature> SF = FeatureTypeStyle.FACTORY;

    /**
     * A smooth field of values in [-1 … 1] used for raster cases.
     */
    private static double wave(double x, double y) {
        return Math.sin(x / 12) * Math.cos(y / 15);
    }

    /**
     * Creates a feature holding the given raster (coverage or aggregate) in the {@code "raster"} attribute.
     */
    private static Feature raster(Object raster) {
        final Feature feature = TYPE.newInstance();
        feature.setPropertyValue("raster", raster);
        return feature;
    }

    /**
     * Creates a coverage of 40×40 cells over [-50 … 50]° with one band per function of (λ,φ).
     */
    private static GridCoverage field(String[] names, java.util.function.DoubleBinaryOperator... functions) {
        final int n = 40;
        final double[][] bands = new double[functions.length][n * n];
        for (int j = 0; j < n; j++) {
            final double y = 50 - (j + 0.5) * 100.0 / n;
            for (int i = 0; i < n; i++) {
                final double x = -50 + (i + 0.5) * 100.0 / n;
                for (int b = 0; b < functions.length; b++) {
                    bands[b][j * n + i] = functions[b].applyAsDouble(x, y);
                }
            }
        }
        final SampleDimension[] sds = new SampleDimension[bands.length];
        for (int b = 0; b < sds.length; b++) {
            sds[b] = new SampleDimension.Builder().setName(names[b]).build();
        }
        final GridGeometry gg = new GridGeometry(new GridExtent(n, n), envelope(-50, -50, 50, 50), GridOrientation.REFLECTION_Y);
        return new GridCoverageBuilder().setDomain(gg).setRanges(sds)
                .setValues(new DataBufferDouble(bands, n * n), new Dimension(n, n)).build();
    }

    /**
     * A field with low contrast values between 100 and 140, mostly near 100, in the given number of bands.
     */
    private static GridCoverage lowContrast(int numBands) {
        final String[] names = {"red", "green", "blue"};
        final java.util.function.DoubleBinaryOperator[] f = new java.util.function.DoubleBinaryOperator[numBands];
        for (int b = 0; b < numBands; b++) {
            final int shift = b * 15;
            f[b] = (x, y) -> 100 + 40 * Math.pow((Math.sin((x + shift) / 12) * Math.cos(y / 15) + 1) / 2, 3);
        }
        return field(java.util.Arrays.copyOf(names, numBands), f);
    }

    /**
     * Creates a 2×2 RGB image coverage over [-40 … 40]°.
     */
    private static GridCoverage imageCoverage() {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
        image.setRGB(0, 0, Color.RED.getRGB());
        image.setRGB(1, 0, Color.GREEN.getRGB());
        image.setRGB(0, 1, Color.BLUE.getRGB());
        image.setRGB(1, 1, Color.YELLOW.getRGB());
        final GridGeometry gg = new GridGeometry(new GridExtent(2, 2), envelope(-40, -40, 40, 40), GridOrientation.REFLECTION_Y);
        return new GridCoverageBuilder().setDomain(gg).setValues(image).build();
    }

    /**
     * Creates a coverage in a (latitude, longitude) CRS over latitudes [-10 … 40] and longitudes [-40 … 20].
     */
    private static GridCoverage latLonCoverage() {
        final CoordinateReferenceSystem latLon = CommonCRS.WGS84.geographic();
        final GeneralEnvelope env = new GeneralEnvelope(latLon);
        env.setRange(0, -10, 40);
        env.setRange(1, -40, 20);
        final int n = 10;
        final double[] values = new double[n * n];
        for (int i = 0; i < values.length; i++) values[i] = (i % n) + (i / n);
        final GridGeometry gg = new GridGeometry(new GridExtent(n, n), env, GridOrientation.HOMOTHETY);
        return new GridCoverageBuilder().setDomain(gg).setRanges(new SampleDimension.Builder().setName("v").build())
                .setValues(new DataBufferDouble(values, values.length), new Dimension(n, n)).build();
    }

    /**
     * An aggregate of two overlapping coverages: red over [-50 … 20]° and blue over [-20 … 50]° in longitude.
     */
    private static Aggregate overlapping() {
        final List<Resource> components = List.of(
                new MemoryGridCoverageResource(solid(-50, -30, 20, 30, Color.RED)),
                new MemoryGridCoverageResource(solid(-20, -30, 50, 30, Color.BLUE)));
        return new Aggregate() {
            @Override public java.util.Collection<? extends Resource> components() {return components;}
            @Override public org.opengis.metadata.Metadata getMetadata() {return null;}
        };
    }

    /**
     * Creates a coverage of a single color over the given envelope.
     */
    private static GridCoverage solid(double x0, double y0, double x1, double y1, Color color) {
        final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        image.setRGB(0, 0, color.getRGB());
        final GridGeometry gg = new GridGeometry(new GridExtent(1, 1), envelope(x0, y0, x1, y1), GridOrientation.REFLECTION_Y);
        return new GridCoverageBuilder().setDomain(gg).setValues(image).build();
    }

    private static GeneralEnvelope envelope(double x0, double y0, double x1, double y1) {
        final GeneralEnvelope env = new GeneralEnvelope(CRS84);
        env.setRange(0, x0, x1);
        env.setRange(1, y0, y1);
        return env;
    }

    /**
     * Creates a raster symbolizer rendering the {@code "raster"} attribute.
     */
    private static RasterSymbolizer<Feature> rasterSymbolizer() {
        final RasterSymbolizer<Feature> symbolizer = new RasterSymbolizer<>(SF);
        symbolizer.setGeometry(FF.property("raster"));
        return symbolizer;
    }

    private static RasterSymbolizer<Feature> contrastSymbolizer(ContrastEnhancement<Feature> ce) {
        final RasterSymbolizer<Feature> symbolizer = rasterSymbolizer();
        symbolizer.setContrastEnhancement(ce);
        return symbolizer;
    }

    private static RasterSymbolizer<Feature> shadedRelief(Expression<Feature,?> colorMap, double factor, boolean brightnessOnly) {
        final RasterSymbolizer<Feature> s = (colorMap != null) ? colorMapSymbolizer(colorMap) : rasterSymbolizer();
        final ShadedRelief<Feature> relief = new ShadedRelief<>(SF);
        relief.setReliefFactor(FF.literal(factor));
        relief.setBrightnessOnly(FF.literal(brightnessOnly));
        s.setShadedRelief(relief);
        return s;
    }

    private static RasterSymbolizer<Feature> colorMapSymbolizer(Expression<Feature,?> function) {
        final RasterSymbolizer<Feature> symbolizer = rasterSymbolizer();
        final ColorMap<Feature> colorMap = SF.createColorMap();
        colorMap.setFunction(function);
        symbolizer.setColorMap(colorMap);
        return symbolizer;
    }

    /**
     * Creates an interpolation point associating the given data value to the given color.
     */
    private static InterpolationPoint<Feature> point(double data, Color color) {
        return new InterpolationPoint<>(FF.literal(data), FF.literal(color));
    }

    private static ChannelSelection<Feature> channels(String... names) {
        final ChannelSelection<Feature> selection = SF.createChannelSelection();
        final SelectedChannel<Feature>[] channels = new SelectedChannel[names.length];
        for (int i = 0; i < names.length; i++) {
            channels[i] = SF.createSelectedChannel(names[i]);
        }
        selection.setChannels(channels);
        return selection;
    }

    private static ContrastEnhancement<Feature> contrast(ContrastMethod method, double gamma) {
        final ContrastEnhancement<Feature> ce = SF.createContrastEnhancement();
        ce.setMethod(method);
        if (gamma != 1) ce.setGammaValue(FF.literal(gamma));
        return ce;
    }

    /**
     * Creates a line symbolizer rendering the {@code "geom"} attribute.
     */
    private static LineSymbolizer<Feature> symbolizer() {
        final LineSymbolizer<Feature> symbolizer = new LineSymbolizer<>(FeatureTypeStyle.FACTORY);
        symbolizer.setGeometry(FF.property("geom"));
        return symbolizer;
    }

    /**
     * Creates a well-known mark.
     *
     * @param wkn well-known name.
     * @param fill fill color, or {@code null} for no fill.
     * @param outline outline color, or {@code null} for no outline.
     */
    private static Mark<Feature> mark(String wkn, Color fill, Color outline) {
        final Mark<Feature> mark = new Mark<>(FeatureTypeStyle.FACTORY);
        mark.setWellKnownName(FF.literal(wkn));
        if (fill != null) {
            final Fill<Feature> f = new Fill<>(FeatureTypeStyle.FACTORY);
            f.setColor(FF.literal(fill));
            mark.setFill(f);
        } else {
            mark.setFill(null);
        }
        if (outline != null) {
            final Stroke<Feature> s = new Stroke<>(FeatureTypeStyle.FACTORY);
            s.setColor(FF.literal(outline));
            s.setWidth(FF.literal(1.0));
            mark.setStroke(s);
        } else {
            mark.setStroke(null);
        }
        return mark;
    }

    private static GraphicFill<Feature> graphicFill(Mark<Feature> mark, double size) {
        final GraphicFill<Feature> gf = new GraphicFill<>(FeatureTypeStyle.FACTORY);
        gf.getGraphic().graphicalSymbols().add(mark);
        gf.getGraphic().setSize(FF.literal(size));
        return gf;
    }

    private static GraphicStroke<Feature> graphicStroke(Mark<Feature> mark, double size) {
        final GraphicStroke<Feature> gs = new GraphicStroke<>(FeatureTypeStyle.FACTORY);
        gs.getGraphic().graphicalSymbols().add(mark);
        gs.getGraphic().setSize(FF.literal(size));
        return gs;
    }
}
