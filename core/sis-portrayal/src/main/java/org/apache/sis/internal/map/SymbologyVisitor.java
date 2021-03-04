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
package org.apache.sis.internal.map;

import java.util.Collection;
import java.util.List;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.BinaryExpression;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.DistanceBufferOperator;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.style.AnchorPoint;
import org.opengis.style.ChannelSelection;
import org.opengis.style.ColorMap;
import org.opengis.style.ColorReplacement;
import org.opengis.style.ContrastEnhancement;
import org.opengis.style.Description;
import org.opengis.style.Displacement;
import org.opengis.style.ExtensionSymbolizer;
import org.opengis.style.ExternalGraphic;
import org.opengis.style.ExternalMark;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Fill;
import org.opengis.style.Font;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicFill;
import org.opengis.style.GraphicLegend;
import org.opengis.style.GraphicStroke;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.Halo;
import org.opengis.style.LabelPlacement;
import org.opengis.style.LinePlacement;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.Mark;
import org.opengis.style.PointPlacement;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.PolygonSymbolizer;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.Rule;
import org.opengis.style.SelectedChannelType;
import org.opengis.style.ShadedRelief;
import org.opengis.style.Stroke;
import org.opengis.style.Style;
import org.opengis.style.Symbolizer;
import org.opengis.style.TextSymbolizer;

/**
 * Loops on all objects contained in a style.
 * Sub classes are expected to override interested methods to fill their objectives.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public abstract class SymbologyVisitor {

    public void visit(Style candidate) {
        for (FeatureTypeStyle fts : candidate.featureTypeStyles()) {
            if (fts != null) visit(fts);
        }
    }

    public void visit(FeatureTypeStyle candidate) {
        for (Rule rule : candidate.rules()) {
            if (rule != null) visit(rule);
        }
    }

    public void visit(Rule candidate) {
        for (Symbolizer symbolizer : candidate.symbolizers()) {
            if (symbolizer != null) visit(symbolizer);
        }
    }

    public void visit(Symbolizer candidate) {
        if (candidate instanceof PointSymbolizer) {
            visit((PointSymbolizer) candidate);
        } else if (candidate instanceof LineSymbolizer) {
            visit((LineSymbolizer) candidate);
        } else if (candidate instanceof PolygonSymbolizer) {
            visit((PolygonSymbolizer) candidate);
        } else if (candidate instanceof TextSymbolizer) {
            visit((TextSymbolizer) candidate);
        } else if (candidate instanceof RasterSymbolizer) {
            visit((RasterSymbolizer) candidate);
        } else if (candidate instanceof ExtensionSymbolizer) {
            visit((ExtensionSymbolizer) candidate);
        } else {
            throw new IllegalArgumentException("Unexpected symbolizer " + candidate);
        }
    }

    public void visit(PointSymbolizer candidate) {
        visitNoNull(candidate.getGeometry());
        final Graphic graphic = candidate.getGraphic();
        if (graphic != null) visit(graphic);
    }

    public void visit(LineSymbolizer candidate) {
        visitNoNull(candidate.getGeometry());
        visitNoNull(candidate.getPerpendicularOffset());
        final Stroke stroke = candidate.getStroke();
        if (stroke != null) visit(stroke);
    }

    public void visit(PolygonSymbolizer candidate) {
        visitNoNull(candidate.getGeometry());
        visitNoNull(candidate.getPerpendicularOffset());

        final Displacement displacement = candidate.getDisplacement();
        final Fill fill = candidate.getFill();
        final Stroke stroke = candidate.getStroke();
        if (displacement != null) visit(displacement);
        if (fill != null) visit(fill);
        if (stroke != null) visit(stroke);
    }

    public void visit(TextSymbolizer candidate) {
        visitNoNull(candidate.getGeometry());
        visitNoNull(candidate.getLabel());
        final Fill fill = candidate.getFill();
        final Font font = candidate.getFont();
        final Halo halo = candidate.getHalo();
        final LabelPlacement labelPlacement = candidate.getLabelPlacement();
        if (fill != null) visit(fill);
        if (font != null) visit(font);
        if (halo != null) visit(halo);
        if (labelPlacement != null) visit(labelPlacement);
    }

    public void visit(RasterSymbolizer candidate) {
        visitNoNull(candidate.getGeometry());
        visitNoNull(candidate.getOpacity());
        final ChannelSelection channelSelection = candidate.getChannelSelection();
        final ColorMap colorMap = candidate.getColorMap();
        final ContrastEnhancement contrastEnhancement = candidate.getContrastEnhancement();
        final Symbolizer imageOutline = candidate.getImageOutline();
        final ShadedRelief shadedRelief = candidate.getShadedRelief();
        if (channelSelection != null) visit(channelSelection);
        if (colorMap != null) visit(colorMap);
        if (contrastEnhancement != null) visit(contrastEnhancement);
        if (imageOutline != null) visit(imageOutline);
        if (shadedRelief != null) visit(shadedRelief);
    }

    public void visit(ExtensionSymbolizer candidate) {
        visitNoNull(candidate.getGeometry());
        for (Expression exp : candidate.getParameters().values()) {
            visitNoNull(exp);
        }
    }

    public void visit(Graphic candidate) {
        visitNoNull(candidate.getOpacity());
        visitNoNull(candidate.getRotation());
        visitNoNull(candidate.getSize());

        final AnchorPoint anchorPoint = candidate.getAnchorPoint();
        final Displacement displacement = candidate.getDisplacement();
        final List<GraphicalSymbol> graphicalSymbols = candidate.graphicalSymbols();
        if (anchorPoint != null) visit(anchorPoint);
        if (displacement != null) visit(displacement);
        if (graphicalSymbols != null) {
            for (GraphicalSymbol gs : graphicalSymbols) {
                visit(gs);
            }
        }
    }

    public void visit(GraphicalSymbol candidate) {
        if (candidate instanceof Mark) {
            visit((Mark) candidate);
        } else if (candidate instanceof ExternalGraphic) {
            visit((ExternalGraphic) candidate);
        } else {
            throw new IllegalArgumentException("Unexpected GraphicalSymbol " + candidate);
        }
    }

    public void visit(Mark candidate) {
        final ExternalMark externalMark = candidate.getExternalMark();
        final Fill fill = candidate.getFill();
        final Stroke stroke = candidate.getStroke();
        if (externalMark != null) visit(externalMark);
        if (fill != null) visit(fill);
        if (stroke != null) visit(stroke);
        visitNoNull(candidate.getWellKnownName());
    }

    public void visit(ExternalGraphic candidate) {
        final Collection<ColorReplacement> colorReplacements = candidate.getColorReplacements();
        if (colorReplacements != null) {
            for (ColorReplacement cr : colorReplacements) {
                visit(cr);
            }
        }
    }

    public void visit(ExternalMark candidate) {
    }

    public void visit(Stroke candidate) {
        final GraphicFill graphicFill = candidate.getGraphicFill();
        final GraphicStroke graphicStroke = candidate.getGraphicStroke();
        visitNoNull(candidate.getColor());
        visitNoNull(candidate.getDashOffset());
        if (graphicFill != null) visit(graphicFill);
        if (graphicStroke != null) visit(graphicStroke);
        visitNoNull(candidate.getLineCap());
        visitNoNull(candidate.getLineJoin());
        visitNoNull(candidate.getOpacity());
        visitNoNull(candidate.getWidth());
    }

    public void visit(Description candidate) {
    }

    public void visit(Displacement candidate) {
        visitNoNull(candidate.getDisplacementX());
        visitNoNull(candidate.getDisplacementY());
    }

    public void visit(Fill candidate) {
        final GraphicFill graphicFill = candidate.getGraphicFill();
        if (graphicFill != null) visit(graphicFill);
        visitNoNull(candidate.getColor());
        visitNoNull(candidate.getOpacity());
    }

    public void visit(Font candidate) {
        for (Expression exp : candidate.getFamily()) {
            visitNoNull(exp);
        }
        visitNoNull(candidate.getSize());
        visitNoNull(candidate.getStyle());
        visitNoNull(candidate.getWeight());
    }

    public void visit(GraphicFill candidate) {
        visit((Graphic) candidate);
    }

    public void visit(GraphicStroke candidate) {
        visit((Graphic) candidate);
        visitNoNull(candidate.getGap());
        visitNoNull(candidate.getInitialGap());
    }

    public void visit(LabelPlacement candidate) {
        if (candidate instanceof PointPlacement) {
            visit((PointPlacement) candidate);
        } else if (candidate instanceof LinePlacement) {
            visit((LinePlacement) candidate);
        } else {
            throw new IllegalArgumentException("Unexpected Placement " + candidate);
        }
    }

    public void visit(PointPlacement candidate) {
        final AnchorPoint anchorPoint = candidate.getAnchorPoint();
        final Displacement displacement = candidate.getDisplacement();
        if (anchorPoint != null) visit(anchorPoint);
        if (displacement != null) visit(displacement);
        visitNoNull(candidate.getRotation());
    }

    public void visit(AnchorPoint candidate) {
        visitNoNull(candidate.getAnchorPointX());
        visitNoNull(candidate.getAnchorPointY());
    }

    public void visit(LinePlacement candidate) {
        visitNoNull(candidate.getGap());
        visitNoNull(candidate.getInitialGap());
        visitNoNull(candidate.getPerpendicularOffset());
    }

    public void visit(GraphicLegend candidate) {
        visit((Graphic) candidate);
    }

    public void visit(Halo candidate) {
        final Fill fill = candidate.getFill();
        if (fill != null) visit(fill);
        visitNoNull(candidate.getRadius());
    }

    public void visit(ColorMap candidate) {
        visitNoNull(candidate.getFunction());
    }

    public void visit(ColorReplacement candidate) {
        visitNoNull(candidate.getRecoding());
    }

    public void visit(ContrastEnhancement candidate) {
        visitNoNull(candidate.getGammaValue());
    }

    public void visit(ChannelSelection candidate) {
        final SelectedChannelType grayChannel = candidate.getGrayChannel();
        final SelectedChannelType[] rgbChannels = candidate.getRGBChannels();
        if (grayChannel != null) visit(grayChannel);
        if (rgbChannels != null) {
            for (SelectedChannelType sct : rgbChannels) {
                visit(sct);
            }
        }
    }

    public void visit(SelectedChannelType candidate) {
        if (candidate.getContrastEnhancement() != null) visit(candidate.getContrastEnhancement());
    }

    public void visit(ShadedRelief candidate) {
        visitNoNull(candidate.getReliefFactor());
    }

    public void visit(Filter candidate) {
        if (candidate instanceof BinaryLogicOperator) {
            visit( (BinaryLogicOperator) candidate);
        } else if (candidate instanceof Not) {
            visit( (Not) candidate);
        } else if (candidate instanceof PropertyIsBetween) {
            visit( (PropertyIsBetween) candidate);
        } else if (candidate instanceof BinaryComparisonOperator) {
            visit( (BinaryComparisonOperator) candidate);
        } else if (candidate instanceof BinarySpatialOperator) {
            visit( (BinarySpatialOperator) candidate);
        } else if (candidate instanceof DistanceBufferOperator) {
            visit( (DistanceBufferOperator) candidate);
        } else if (candidate instanceof BinaryTemporalOperator) {
            visit( (BinaryTemporalOperator) candidate);
        } else if (candidate instanceof PropertyIsLike) {
            visit( (PropertyIsLike) candidate);
        } else if (candidate instanceof PropertyIsNull) {
            visit( (PropertyIsNull) candidate);
        } else if (candidate instanceof PropertyIsNil) {
            visit( (PropertyIsNil) candidate);
        } else if (candidate instanceof ExcludeFilter) {
            visit((ExcludeFilter) candidate);
        } else if (candidate instanceof IncludeFilter) {
            visit((IncludeFilter) candidate);
        } else if (candidate instanceof Id) {
            visit((Id) candidate);
        }
    }

    protected final void visitNoNull(Filter candidate) {
        if (candidate != null) visit(candidate);
    }

    public void visit(BinaryLogicOperator candidate) {
        for (Filter f : candidate.getChildren()) {
            visitNoNull(f);
        }
    }

    public void visit(Not candidate) {
        visitNoNull(candidate.getFilter());
    }

    public void visit(PropertyIsBetween candidate) {
        visitNoNull(candidate.getExpression());
        visitNoNull(candidate.getLowerBoundary());
        visitNoNull(candidate.getUpperBoundary());
    }

    public void visit(BinaryComparisonOperator candidate) {
        visitNoNull(candidate.getExpression1());
        visitNoNull(candidate.getExpression2());
    }

    public void visit(BinarySpatialOperator candidate) {
        visitNoNull(candidate.getExpression1());
        visitNoNull(candidate.getExpression2());
    }

    public void visit(DistanceBufferOperator candidate) {
        visitNoNull(candidate.getExpression1());
        visitNoNull(candidate.getExpression2());
    }

    public void visit(BinaryTemporalOperator candidate) {
        visitNoNull(candidate.getExpression1());
        visitNoNull(candidate.getExpression2());
    }

    public void visit(PropertyIsLike candidate) {
        visitNoNull(candidate.getExpression());
    }

    public void visit(PropertyIsNull candidate) {
        visitNoNull(candidate.getExpression());
    }

    public void visit(PropertyIsNil candidate) {
        visitNoNull(candidate.getExpression());
    }

    public void visit(ExcludeFilter candidate) {
    }

    public void visit(IncludeFilter candidate) {
    }

    public void visit(Id candidate) {
    }

    public void visit(Expression candidate) {
        if (candidate instanceof PropertyName) {
            visit((PropertyName) candidate);
        } else if (candidate instanceof Function) {
            visit((Function) candidate);
        } else if (candidate instanceof BinaryExpression) {
            visit((BinaryExpression) candidate);
        } else if (candidate instanceof NilExpression) {
            visit((NilExpression) candidate);
        } else if (candidate instanceof Literal) {
            visit((Literal) candidate);
        } else {
            throw new IllegalArgumentException("Unexpected expression");
        }
    }

    protected final void visitNoNull(Expression candidate) {
        if (candidate != null) visit(candidate);
    }

    public void visit(PropertyName candidate) {
    }

    public void visit(Function candidate) {
        for (Expression ex : candidate.getParameters()) {
            visitNoNull(ex);
        }
    }

    public void visit(BinaryExpression candidate) {
        visitNoNull(candidate.getExpression1());
        visitNoNull(candidate.getExpression2());
    }

    public void visit(NilExpression candidate) {
    }

    public void visit(Literal candidate) {
    }

}
