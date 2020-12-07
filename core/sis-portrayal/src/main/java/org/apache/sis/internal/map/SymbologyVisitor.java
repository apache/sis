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
            visit(fts);
        }
    }

    public void visit(FeatureTypeStyle candidate) {
        for (Rule rule : candidate.rules()) {
            visit(rule);
        }
    }

    public void visit(Rule candidate) {
        for (Symbolizer symbolizer : candidate.symbolizers()) {
            visit(symbolizer);
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
        visit(candidate.getGeometry());
        visit(candidate.getGraphic());
    }

    public void visit(LineSymbolizer candidate) {
        visit(candidate.getGeometry());
        visit(candidate.getPerpendicularOffset());
        visit(candidate.getStroke());
    }

    public void visit(PolygonSymbolizer candidate) {
        visit(candidate.getDisplacement());
        visit(candidate.getFill());
        visit(candidate.getGeometry());
        visit(candidate.getPerpendicularOffset());
        visit(candidate.getStroke());
    }

    public void visit(TextSymbolizer candidate) {
        visit(candidate.getFill());
        visit(candidate.getFont());
        visit(candidate.getGeometry());
        visit(candidate.getHalo());
        visit(candidate.getLabel());
        visit(candidate.getLabelPlacement());
    }

    public void visit(RasterSymbolizer candidate) {
        visit(candidate.getChannelSelection());
        visit(candidate.getColorMap());
        visit(candidate.getContrastEnhancement());
        visit(candidate.getGeometry());
        visit(candidate.getImageOutline());
        visit(candidate.getOpacity());
        visit(candidate.getShadedRelief());
    }

    public void visit(ExtensionSymbolizer candidate) {
        visit(candidate.getGeometry());
        for (Expression exp : candidate.getParameters().values()) {
            visit(exp);
        }
    }

    public void visit(Graphic candidate) {
        visit(candidate.getAnchorPoint());
        visit(candidate.getDisplacement());
        visit(candidate.getOpacity());
        visit(candidate.getRotation());
        visit(candidate.getSize());

        for (GraphicalSymbol gs : candidate.graphicalSymbols()) {
            visit(gs);
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
        visit(candidate.getExternalMark());
        visit(candidate.getFill());
        visit(candidate.getStroke());
        visit(candidate.getWellKnownName());
    }

    public void visit(ExternalGraphic candidate) {
        for (ColorReplacement cr : candidate.getColorReplacements()) {
            visit(cr);
        }
    }

    public void visit(ExternalMark candidate) {
    }

    public void visit(Stroke candidate) {
        visit(candidate.getColor());
        visit(candidate.getDashOffset());
        visit(candidate.getGraphicFill());
        visit(candidate.getGraphicStroke());
        visit(candidate.getLineCap());
        visit(candidate.getLineJoin());
        visit(candidate.getOpacity());
        visit(candidate.getWidth());
    }

    public void visit(Description candidate) {
    }

    public void visit(Displacement candidate) {
        visit(candidate.getDisplacementX());
        visit(candidate.getDisplacementY());
    }

    public void visit(Fill candidate) {
        visit(candidate.getColor());
        visit(candidate.getGraphicFill());
        visit(candidate.getOpacity());
    }

    public void visit(Font candidate) {
        for (Expression exp : candidate.getFamily()) {
            visit(exp);
        }
        visit(candidate.getSize());
        visit(candidate.getStyle());
        visit(candidate.getWeight());
    }

    public void visit(GraphicFill candidate) {
        visit((Graphic) candidate);
    }

    public void visit(GraphicStroke candidate) {
        visit((Graphic) candidate);
        visit(candidate.getGap());
        visit(candidate.getInitialGap());
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
        visit(candidate.getAnchorPoint());
        visit(candidate.getDisplacement());
        visit(candidate.getRotation());
    }

    public void visit(AnchorPoint candidate) {
        visit(candidate.getAnchorPointX());
        visit(candidate.getAnchorPointY());
    }

    public void visit(LinePlacement candidate) {
        visit(candidate.getGap());
        visit(candidate.getInitialGap());
        visit(candidate.getPerpendicularOffset());
    }

    public void visit(GraphicLegend candidate) {
        visit((Graphic) candidate);
    }

    public void visit(Halo candidate) {
        visit(candidate.getFill());
        visit(candidate.getRadius());
    }

    public void visit(ColorMap candidate) {
        visit(candidate.getFunction());
    }

    public void visit(ColorReplacement candidate) {
        visit(candidate.getRecoding());
    }

    public void visit(ContrastEnhancement candidate) {
        visit(candidate.getGammaValue());
    }

    public void visit(ChannelSelection candidate) {
        visit(candidate.getGrayChannel());
        for (SelectedChannelType sct : candidate.getRGBChannels()) {
            visit(sct);
        }
    }

    public void visit(SelectedChannelType candidate) {
        visit(candidate.getContrastEnhancement());
    }

    public void visit(ShadedRelief candidate) {
        visit(candidate.getReliefFactor());
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

    public void visit(BinaryLogicOperator candidate) {
        for (Filter f : candidate.getChildren()) {
            visit(f);
        }
    }

    public void visit(Not candidate) {
        visit(candidate.getFilter());
    }

    public void visit(PropertyIsBetween candidate) {
        visit(candidate.getExpression());
        visit(candidate.getLowerBoundary());
        visit(candidate.getUpperBoundary());
    }

    public void visit(BinaryComparisonOperator candidate) {
        visit(candidate.getExpression1());
        visit(candidate.getExpression2());
    }

    public void visit(BinarySpatialOperator candidate) {
        visit(candidate.getExpression1());
        visit(candidate.getExpression2());
    }

    public void visit(DistanceBufferOperator candidate) {
        visit(candidate.getExpression1());
        visit(candidate.getExpression2());
    }

    public void visit(BinaryTemporalOperator candidate) {
        visit(candidate.getExpression1());
        visit(candidate.getExpression2());
    }

    public void visit(PropertyIsLike candidate) {
        visit(candidate.getExpression());
    }

    public void visit(PropertyIsNull candidate) {
        visit(candidate.getExpression());
    }

    public void visit(PropertyIsNil candidate) {
        visit(candidate.getExpression());
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

    public void visit(PropertyName candidate) {
    }

    public void visit(Function candidate) {
        for (Expression ex : candidate.getParameters()) {
            visit(ex);
        }
    }

    public void visit(BinaryExpression candidate) {
        visit(candidate.getExpression1());
        visit(candidate.getExpression2());
    }

    public void visit(NilExpression candidate) {
    }

    public void visit(Literal candidate) {
    }

}
