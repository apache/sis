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
package org.apache.sis.internal.style;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.swing.Icon;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.measure.Units;
import org.apache.sis.util.SimpleInternationalString;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;
import org.opengis.filter.ResourceId;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.style.ContrastMethod;
import org.opengis.style.ExtensionSymbolizer;
import org.opengis.style.OverlapBehavior;
import org.opengis.style.SemanticType;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;

/**
 * {@link org.opengis.style.StyleFactory} implementation.
 * Created objects are mutable.
 *
 * TODO : A proper review of GeoAPI Style API is required.
 * This class and style objets implementations are drafts used to continue
 * work on the rendering engine.
 *
 * @see StyleFactory
 *
 * @author Johann Sorel (Geomatys)
 */
public class StyleFactory implements org.opengis.style.StyleFactory {

    public static final SimpleInternationalString EMPTY_STRING = new SimpleInternationalString("");
    public static final Literal<Feature,Double> LITERAL_ZERO;
    public static final Literal<Feature,Double> LITERAL_HALF;
    public static final Literal<Feature,Double> LITERAL_ONE;
    public static final Literal<Feature,Color> LITERAL_WHITE;
    public static final Literal<Feature,Color> LITERAL_GRAY;
    public static final Literal<Feature,Color> LITERAL_BLACK;

    public static final Literal<Feature,Double> DEFAULT_ANCHOR_POINT_X;
    public static final Literal<Feature,Double> DEFAULT_ANCHOR_POINT_Y;

    public static final ContrastMethod DEFAULT_CONTRAST_ENHANCEMENT_METHOD;
    public static final Literal<Feature,Double> DEFAULT_CONTRAST_ENHANCEMENT_GAMMA;

    public static final Literal<Feature,Double> DEFAULT_DISPLACEMENT_X;
    public static final Literal<Feature,Double> DEFAULT_DISPLACEMENT_Y;

    public static final Literal<Feature,Color> DEFAULT_FILL_COLOR;
    public static final Literal<Feature,Double> DEFAULT_FILL_OPACITY;

    public static final String STROKE_JOIN_MITRE_STRING = "mitre";
    public static final String STROKE_JOIN_ROUND_STRING = "round";
    public static final String STROKE_JOIN_BEVEL_STRING = "bevel";
    public static final String STROKE_CAP_BUTT_STRING = "butt";
    public static final String STROKE_CAP_ROUND_STRING = "round";
    public static final String STROKE_CAP_SQUARE_STRING = "square";
    public static final Literal<Feature,String> STROKE_JOIN_MITRE;
    public static final Literal<Feature,String> STROKE_JOIN_ROUND;
    public static final Literal<Feature,String> STROKE_JOIN_BEVEL;
    public static final Literal<Feature,String> STROKE_CAP_BUTT;
    public static final Literal<Feature,String> STROKE_CAP_ROUND;
    public static final Literal<Feature,String> STROKE_CAP_SQUARE;
    public static final Literal<Feature,Color> DEFAULT_STROKE_COLOR;
    public static final Literal<Feature,Double> DEFAULT_STROKE_OPACITY;
    public static final Literal<Feature,Double> DEFAULT_STROKE_WIDTH;
    public static final Literal<Feature,String> DEFAULT_STROKE_JOIN;
    public static final Literal<Feature,String> DEFAULT_STROKE_CAP;
    public static final Literal<Feature,Double> DEFAULT_STROKE_OFFSET;

    public static final String FONT_STYLE_NORMAL_STRING = "normal";
    public static final String FONT_STYLE_ITALIC_STRING = "italic";
    public static final String FONT_STYLE_OBLIQUE_STRING = "oblique";
    public static final String FONT_WEIGHT_NORMAL_STRING = "normal";
    public static final String FONT_WEIGHT_BOLD_STRING = "bold";
    public static final Literal<Feature,String> FONT_STYLE_NORMAL;
    public static final Literal<Feature,String> FONT_STYLE_ITALIC;
    public static final Literal<Feature,String> FONT_STYLE_OBLIQUE;
    public static final Literal<Feature,String> FONT_WEIGHT_NORMAL;
    public static final Literal<Feature,String> FONT_WEIGHT_BOLD;
    public static final Literal<Feature,String> DEFAULT_FONT_STYLE;
    public static final Literal<Feature,String> DEFAULT_FONT_WEIGHT;
    public static final Literal<Feature,Double> DEFAULT_FONT_SIZE;

    public static final Literal<Feature,Double> DEFAULT_HALO_RADIUS;

    public static final String MARK_SQUARE_STRING = "square";
    public static final String MARK_CIRCLE_STRING = "circle";
    public static final String MARK_TRIANGLE_STRING = "triangle";
    public static final String MARK_STAR_STRING = "star";
    public static final String MARK_CROSS_STRING = "cross";
    public static final String MARK_X_STRING = "x";
    public static final Literal<Feature,String> MARK_SQUARE;
    public static final Literal<Feature,String> MARK_CIRCLE;
    public static final Literal<Feature,String> MARK_TRIANGLE;
    public static final Literal<Feature,String> MARK_STAR;
    public static final Literal<Feature,String> MARK_CROSS;
    public static final Literal<Feature,String> MARK_X;
    public static final Literal<Feature,String> DEFAULT_MARK_WKN;

    public static final Literal<Feature,Double> DEFAULT_GRAPHIC_OPACITY;
    public static final Literal<Feature,Double> DEFAULT_GRAPHIC_ROTATION;
    public static final Literal<Feature,Double> DEFAULT_GRAPHIC_SIZE;
    public static final Literal<Feature,Double> DEFAULT_GRAPHIC_STROKE_INITIAL_GAP;
    public static final Literal<Feature,Double> DEFAULT_GRAPHIC_STROKE_GAP;

    public static final Unit<Length> DEFAULT_UOM;
    public static final String DEFAULT_GEOM;

    public static final Literal<Feature,String> DEFAULT_TEXT_LABEL;

    static {
        final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();
        LITERAL_ZERO = FF.literal(0.0);
        LITERAL_HALF = FF.literal(0.5);
        LITERAL_ONE = FF.literal(1.0);
        LITERAL_WHITE = FF.literal(Color.WHITE);
        LITERAL_GRAY = FF.literal(Color.GRAY);
        LITERAL_BLACK = FF.literal(Color.BLACK);

        DEFAULT_UOM = Units.POINT;
        DEFAULT_GEOM = null;

        DEFAULT_ANCHOR_POINT_X = LITERAL_HALF;
        DEFAULT_ANCHOR_POINT_Y = LITERAL_HALF;

        DEFAULT_CONTRAST_ENHANCEMENT_METHOD = ContrastMethod.NONE;
        DEFAULT_CONTRAST_ENHANCEMENT_GAMMA = LITERAL_ONE;

        DEFAULT_DISPLACEMENT_X = LITERAL_ZERO;
        DEFAULT_DISPLACEMENT_Y = LITERAL_ZERO;

        DEFAULT_FILL_COLOR = FF.literal(Color.GRAY);
        DEFAULT_FILL_OPACITY = LITERAL_ONE;

        STROKE_JOIN_MITRE = FF.literal(STROKE_JOIN_MITRE_STRING);
        STROKE_JOIN_ROUND = FF.literal(STROKE_JOIN_ROUND_STRING);
        STROKE_JOIN_BEVEL = FF.literal(STROKE_JOIN_BEVEL_STRING);
        STROKE_CAP_BUTT = FF.literal(STROKE_CAP_BUTT_STRING);
        STROKE_CAP_ROUND = FF.literal(STROKE_CAP_ROUND_STRING);
        STROKE_CAP_SQUARE = FF.literal(STROKE_CAP_SQUARE_STRING);
        DEFAULT_STROKE_COLOR = FF.literal(Color.BLACK);
        DEFAULT_STROKE_OPACITY = LITERAL_ONE;
        DEFAULT_STROKE_WIDTH = LITERAL_ONE;
        DEFAULT_STROKE_JOIN = STROKE_JOIN_BEVEL;
        DEFAULT_STROKE_CAP = STROKE_CAP_SQUARE;
        DEFAULT_STROKE_OFFSET = LITERAL_ZERO;

        FONT_STYLE_NORMAL = FF.literal(FONT_STYLE_NORMAL_STRING);
        FONT_STYLE_ITALIC = FF.literal(FONT_STYLE_ITALIC_STRING);
        FONT_STYLE_OBLIQUE = FF.literal(FONT_STYLE_OBLIQUE_STRING);
        FONT_WEIGHT_NORMAL = FF.literal(FONT_WEIGHT_NORMAL_STRING);
        FONT_WEIGHT_BOLD = FF.literal(FONT_WEIGHT_BOLD_STRING);
        DEFAULT_FONT_STYLE = FONT_STYLE_NORMAL;
        DEFAULT_FONT_WEIGHT = FONT_WEIGHT_NORMAL;
        DEFAULT_FONT_SIZE = FF.literal(10.0);

        DEFAULT_HALO_RADIUS = LITERAL_ONE;

        MARK_SQUARE = FF.literal(MARK_SQUARE_STRING);
        MARK_CIRCLE = FF.literal(MARK_CIRCLE_STRING);
        MARK_TRIANGLE = FF.literal(MARK_TRIANGLE_STRING);
        MARK_STAR = FF.literal(MARK_STAR_STRING);
        MARK_CROSS = FF.literal(MARK_CROSS_STRING);
        MARK_X = FF.literal(MARK_X_STRING);
        DEFAULT_MARK_WKN = MARK_SQUARE;

        DEFAULT_GRAPHIC_STROKE_INITIAL_GAP = LITERAL_ZERO;
        DEFAULT_GRAPHIC_STROKE_GAP = LITERAL_ZERO;

        DEFAULT_GRAPHIC_OPACITY = LITERAL_ONE;
        DEFAULT_GRAPHIC_ROTATION = LITERAL_ZERO;
        DEFAULT_GRAPHIC_SIZE = FF.literal(6.0);

        DEFAULT_TEXT_LABEL = FF.literal("Label");

    }

    public StyleFactory(){}

    @Override
    public AnchorPoint anchorPoint(Expression x, Expression y) {
        return new org.apache.sis.internal.style.AnchorPoint(x, y);
    }

    @Override
    public ChannelSelection channelSelection(org.opengis.style.SelectedChannelType gray) {
        return new ChannelSelection(SelectedChannelType.castOrCopy(gray));
    }

    @Override
    public ChannelSelection channelSelection(
            org.opengis.style.SelectedChannelType red,
            org.opengis.style.SelectedChannelType green,
            org.opengis.style.SelectedChannelType blue) {
        return new ChannelSelection(
                SelectedChannelType.castOrCopy(red),
                SelectedChannelType.castOrCopy(green),
                SelectedChannelType.castOrCopy(blue));
    }

    @Override
    public ColorMap colorMap(Expression propertyName, Expression... mapping) {
        return new ColorMap(propertyName);
    }

    @Override
    public ColorReplacement colorReplacement(Expression propertyName, Expression... mapping) {
        return new ColorReplacement(propertyName);
    }

    @Override
    public ContrastEnhancement contrastEnhancement(Expression gamma, ContrastMethod method) {
        return new ContrastEnhancement(method, gamma);
    }

    @Override
    public Description description(InternationalString title, InternationalString description) {
        return new Description(title, description);
    }

    @Override
    public Displacement displacement(Expression dx, Expression dy) {
        return new Displacement(dx, dy);
    }

    @Override
    public ExternalGraphic externalGraphic(OnlineResource resource, String format,
            Collection<org.opengis.style.ColorReplacement> replacements) {

        List<ColorReplacement> cs = null;
        if (replacements != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.ColorReplacement cr : replacements) {
                cs.add(ColorReplacement.castOrCopy(cr));
            }
        }
        return new ExternalGraphic(resource, null, format, cs);
    }

    @Override
    public ExternalGraphic externalGraphic(Icon inline,
            Collection<org.opengis.style.ColorReplacement> replacements) {
        List<ColorReplacement> cs = null;
        if (replacements != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.ColorReplacement cr : replacements) {
                cs.add(ColorReplacement.castOrCopy(cr));
            }
        }
        return new ExternalGraphic(null, inline, null, cs);
    }

    @Override
    public ExternalMark externalMark(OnlineResource resource, String format, int markIndex) {
        return new ExternalMark(resource, null, format, markIndex);
    }

    @Override
    public ExternalMark externalMark(Icon inline) {
        return new ExternalMark(null, inline, null, 0);
    }

    @Override
    public FeatureTypeStyle featureTypeStyle(String name,
            org.opengis.style.Description description,
            ResourceId definedFor, Set<GenericName> featureTypeNames,
            Set<SemanticType> types, List<org.opengis.style.Rule> rules) {
        List<Rule> cs = null;
        if (rules != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.Rule cr : rules) {
                cs.add(Rule.castOrCopy(cr));
            }
        }
        return new FeatureTypeStyle(name, Description.castOrCopy(description),
                definedFor, featureTypeNames, types, cs, null);
    }

    @Override
    public Fill fill(org.opengis.style.GraphicFill fill, Expression color, Expression opacity) {
        return new Fill(GraphicFill.castOrCopy(fill), color, opacity);
    }

    @Override
    public Font font(List<Expression> family, Expression style, Expression weight, Expression size) {
        return new Font(family, style, weight, size);
    }

    @Override
    public Graphic graphic( List<org.opengis.style.GraphicalSymbol> symbols,
            Expression opacity, Expression size, Expression rotation,
            org.opengis.style.AnchorPoint anchor, org.opengis.style.Displacement disp) {
        List<GraphicalSymbol> cs = null;
        if (symbols != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.GraphicalSymbol cr : symbols) {
                cs.add(GraphicalSymbol.castOrCopy(cr));
            }
        }
        return new Graphic(cs, opacity, size, rotation,
                AnchorPoint.castOrCopy(anchor),
                Displacement.castOrCopy(disp));
    }

    @Override
    public GraphicFill graphicFill(List<org.opengis.style.GraphicalSymbol> symbols,
            Expression opacity, Expression size, Expression rotation,
            org.opengis.style.AnchorPoint anchor, org.opengis.style.Displacement disp) {
        List<GraphicalSymbol> cs = null;
        if (symbols != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.GraphicalSymbol cr : symbols) {
                cs.add(GraphicalSymbol.castOrCopy(cr));
            }
        }
        return new GraphicFill(cs, opacity, size, rotation,
                AnchorPoint.castOrCopy(anchor),
                Displacement.castOrCopy(disp));
    }

    @Override
    public GraphicLegend graphicLegend(List<org.opengis.style.GraphicalSymbol> symbols,
            Expression opacity, Expression size, Expression rotation,
            org.opengis.style.AnchorPoint anchor, org.opengis.style.Displacement disp) {
        List<GraphicalSymbol> cs = null;
        if (symbols != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.GraphicalSymbol cr : symbols) {
                cs.add(GraphicalSymbol.castOrCopy(cr));
            }
        }
        return new GraphicLegend(cs, opacity, size, rotation,
                AnchorPoint.castOrCopy(anchor),
                Displacement.castOrCopy(disp));
    }

    @Override
    public GraphicStroke graphicStroke(List<org.opengis.style.GraphicalSymbol> symbols,
            Expression opacity, Expression size, Expression rotation,
            org.opengis.style.AnchorPoint anchor, org.opengis.style.Displacement disp,
            Expression initialGap, Expression gap) {
        List<GraphicalSymbol> cs = null;
        if (symbols != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.GraphicalSymbol cr : symbols) {
                cs.add(GraphicalSymbol.castOrCopy(cr));
            }
        }
        return new GraphicStroke(cs, opacity, size, rotation,
                AnchorPoint.castOrCopy(anchor),
                Displacement.castOrCopy(disp),
                initialGap, gap);
    }

    @Override
    public Halo halo(org.opengis.style.Fill fill, Expression radius) {
        return new Halo(Fill.castOrCopy(fill), radius);
    }

    @Override
    public LinePlacement linePlacement(Expression offset, Expression initialGap,
            Expression gap, boolean repeated, boolean aligned, boolean generalizedLine) {
        return new LinePlacement(offset, initialGap, gap, repeated, aligned, generalizedLine);
    }

    @Override
    public LineSymbolizer lineSymbolizer(String name, Expression geometry,
            org.opengis.style.Description description, Unit<?> unit,
            org.opengis.style.Stroke stroke, Expression offset) {
        return new LineSymbolizer(name, geometry,
                Description.castOrCopy(description),
                (Unit<Length>) unit,
                Stroke.castOrCopy(stroke), offset);
    }

    @Override
    public Mark mark(Expression wellKnownName, org.opengis.style.Fill fill,
            org.opengis.style.Stroke stroke) {
        return new Mark(wellKnownName, null,
                Fill.castOrCopy(fill),
                Stroke.castOrCopy(stroke));
    }

    @Override
    public Mark mark(org.opengis.style.ExternalMark externalMark,
            org.opengis.style.Fill fill, org.opengis.style.Stroke stroke) {
        return new Mark(null, ExternalMark.castOrCopy(externalMark),
                Fill.castOrCopy(fill),
                Stroke.castOrCopy(stroke));
    }

    @Override
    public PointPlacement pointPlacement(org.opengis.style.AnchorPoint anchor,
            org.opengis.style.Displacement displacement, Expression rotation) {
        return new PointPlacement(
                AnchorPoint.castOrCopy(anchor),
                Displacement.castOrCopy(displacement),
                rotation);
    }

    @Override
    public PointSymbolizer pointSymbolizer(String name, Expression geometry,
            org.opengis.style.Description description, Unit<?> unit,
            org.opengis.style.Graphic graphic) {
        return new PointSymbolizer(name, geometry,
                Description.castOrCopy(description), (Unit<Length>) unit,
                Graphic.castOrCopy(graphic));
    }

    @Override
    public PolygonSymbolizer polygonSymbolizer(String name, Expression geometry,
            org.opengis.style.Description description, Unit<?> unit,
            org.opengis.style.Stroke stroke, org.opengis.style.Fill fill,
            org.opengis.style.Displacement displacement, Expression offset) {
        return new PolygonSymbolizer(name, geometry,
                Description.castOrCopy(description), (Unit<Length>) unit,
                Stroke.castOrCopy(stroke),
                Fill.castOrCopy(fill),
                Displacement.castOrCopy(displacement), offset);
    }

    @Override
    public RasterSymbolizer rasterSymbolizer(String name, Expression geometry,
            org.opengis.style.Description description, Unit<?> unit,
            Expression opacity, org.opengis.style.ChannelSelection channelSelection,
            OverlapBehavior overlapsBehaviour,
            org.opengis.style.ColorMap colorMap,
            org.opengis.style.ContrastEnhancement contrast,
            org.opengis.style.ShadedRelief shaded,
            org.opengis.style.Symbolizer outline) {
        return new RasterSymbolizer(name, geometry,
                Description.castOrCopy(description),
                (Unit<Length>) unit, opacity,
                ChannelSelection.castOrCopy(channelSelection),
                overlapsBehaviour,
                ColorMap.castOrCopy(colorMap),
                ContrastEnhancement.castOrCopy(contrast),
                ShadedRelief.castOrCopy(shaded),
                outline);
    }

    @Override
    public ExtensionSymbolizer extensionSymbolizer(String name, String geometry,
            org.opengis.style.Description description, Unit<?> unit, String extensionName,
            Map<String, Expression> parameters) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ExtensionSymbolizer extensionSymbolizer(String name, Expression geometry,
            org.opengis.style.Description description, Unit<?> unit, String extensionName,
            Map<String, Expression> parameters) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Rule rule(String name, org.opengis.style.Description description,
            org.opengis.style.GraphicLegend legend, double min, double max,
            List<org.opengis.style.Symbolizer> symbolizers, Filter filter) {
        return new Rule(name,
                Description.castOrCopy(description),
                GraphicLegend.castOrCopy(legend),
                filter, false, min, max, symbolizers, null);
    }

    @Override
    public SelectedChannelType selectedChannelType(String channelName,
            org.opengis.style.ContrastEnhancement contrastEnhancement) {
        return new SelectedChannelType(channelName,
                ContrastEnhancement.castOrCopy(contrastEnhancement));
    }

    @Override
    public ShadedRelief shadedRelief(Expression reliefFactor, boolean brightnessOnly) {
        return new ShadedRelief(brightnessOnly, reliefFactor);
    }

    @Override
    public Stroke stroke(Expression color, Expression opacity, Expression width,
            Expression join, Expression cap, float[] dashes, Expression offset) {
        return new Stroke(null, null, color, opacity, width, join, cap, dashes, offset);
    }

    @Override
    public Stroke stroke(org.opengis.style.GraphicFill fill, Expression color,
            Expression opacity, Expression width, Expression join, Expression cap,
            float[] dashes, Expression offset) {
        return new Stroke(GraphicFill.castOrCopy(fill),
                null, color, opacity, width, join, cap, dashes, offset);
    }

    @Override
    public Stroke stroke(org.opengis.style.GraphicStroke stroke,
            Expression color, Expression opacity, Expression width,
            Expression join, Expression cap, float[] dashes, Expression offset) {
        return new Stroke(null, GraphicStroke.castOrCopy(stroke),
                color, opacity, width, join, cap, dashes, offset);
    }

    @Override
    public Style style(String name, org.opengis.style.Description description,
            boolean isDefault, List<org.opengis.style.FeatureTypeStyle> featureTypeStyles,
            org.opengis.style.Symbolizer defaultSymbolizer) {
        List<FeatureTypeStyle> cs = null;
        if (featureTypeStyles != null) {
            cs = new ArrayList<>();
            for (org.opengis.style.FeatureTypeStyle cr : featureTypeStyles) {
                cs.add(FeatureTypeStyle.castOrCopy(cr));
            }
        }
        return new Style(
                name,
                Description.castOrCopy(description),
                isDefault,
                cs,
                defaultSymbolizer);
    }

    @Override
    public TextSymbolizer textSymbolizer(String name,Expression geometry,
            org.opengis.style.Description description, Unit<?> unit,
            Expression label, org.opengis.style.Font font,
            org.opengis.style.LabelPlacement placement,
            org.opengis.style.Halo halo,
            org.opengis.style.Fill fill) {
        return new TextSymbolizer(name, geometry,
                Description.castOrCopy(description),
                (Unit<Length>) unit, label,
                Font.castOrCopy(font),
                LabelPlacement.castOrCopy(placement),
                Halo.castOrCopy(halo),
                Fill.castOrCopy(fill));
    }

}
