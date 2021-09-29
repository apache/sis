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

import org.opengis.filter.LogicalOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
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

import static org.apache.sis.internal.util.CollectionsExt.nonNull;


/**
 * Loops on all objects contained in a style.
 * Sub classes are expected to override interested methods to fill their objectives.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public abstract class SymbologyVisitor {
    protected SymbologyVisitor() {
    }

    protected void visit(final Style candidate) {
        if (candidate != null) {
            nonNull(candidate.featureTypeStyles()).forEach(this::visit);
        }
    }

    protected void visit(final FeatureTypeStyle candidate) {
        if (candidate != null) {
            nonNull(candidate.rules()).forEach(this::visit);
        }
    }

    protected void visit(final Rule candidate) {
        if (candidate != null) {
            nonNull(candidate.symbolizers()).forEach(this::visit);
        }
    }

    protected void visit(final Symbolizer candidate) {
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
        } else if (candidate != null) {
            throw new IllegalArgumentException("Unexpected symbolizer " + candidate);
        }
    }

    protected void visit(final PointSymbolizer candidate) {
        if (candidate != null) {
            visit(candidate.getGeometry());
            visit(candidate.getGraphic());
        }
    }

    protected void visit(final LineSymbolizer candidate) {
        if (candidate != null) {
            visit(candidate.getGeometry());
            visit(candidate.getPerpendicularOffset());
            visit(candidate.getStroke());
        }
    }

    protected void visit(final PolygonSymbolizer candidate) {
        if (candidate != null) {
            visit(candidate.getGeometry());
            visit(candidate.getPerpendicularOffset());
            visit(candidate.getDisplacement());
            visit(candidate.getFill());
            visit(candidate.getStroke());
        }
    }

    protected void visit(final TextSymbolizer candidate) {
        if (candidate != null) {
            visit(candidate.getGeometry());
            visit(candidate.getLabel());
            visit(candidate.getFill());
            visit(candidate.getFont());
            visit(candidate.getHalo());
            visit(candidate.getLabelPlacement());
        }
    }

    protected void visit(final RasterSymbolizer candidate) {
        if (candidate != null) {
            visit(candidate.getGeometry());
            visit(candidate.getOpacity());
            visit(candidate.getChannelSelection());
            visit(candidate.getColorMap());
            visit(candidate.getContrastEnhancement());
            visit(candidate.getImageOutline());
            visit(candidate.getShadedRelief());
        }
    }

    protected void visit(final ExtensionSymbolizer candidate) {
        if (candidate != null) {
            visit(candidate.getGeometry());
            candidate.getParameters().values().forEach(this::visit);
        }
    }

    protected void visit(final Graphic candidate) {
        if (candidate != null) {
            visit(candidate.getOpacity());
            visit(candidate.getRotation());
            visit(candidate.getSize());
            visit(candidate.getAnchorPoint());
            visit(candidate.getDisplacement());
            nonNull(candidate.graphicalSymbols()).forEach(this::visit);
        }
    }

    protected void visit(final GraphicalSymbol candidate) {
        if (candidate instanceof Mark) {
            visit((Mark) candidate);
        } else if (candidate instanceof ExternalGraphic) {
            visit((ExternalGraphic) candidate);
        } else if (candidate != null) {
            throw new IllegalArgumentException("Unexpected GraphicalSymbol " + candidate);
        }
    }

    protected void visit(final Mark candidate) {
        if (candidate != null) {
            visit(candidate.getExternalMark());
            visit(candidate.getFill());
            visit(candidate.getStroke());
            visit(candidate.getWellKnownName());
        }
    }

    protected void visit(final ExternalGraphic candidate) {
        nonNull(candidate.getColorReplacements()).forEach(this::visit);
    }

    protected void visit(final ExternalMark candidate) {
    }

    protected void visit(final Stroke candidate) {
        if (candidate != null) {
            visit(candidate.getColor());
            visit(candidate.getDashOffset());
            visit(candidate.getGraphicFill());
            visit(candidate.getGraphicStroke());
            visit(candidate.getLineCap());
            visit(candidate.getLineJoin());
            visit(candidate.getOpacity());
            visit(candidate.getWidth());
        }
    }

    protected void visit(final Description candidate) {
    }

    protected void visit(final Displacement candidate) {
        if (candidate != null) {
            visit(candidate.getDisplacementX());
            visit(candidate.getDisplacementY());
        }
    }

    protected void visit(final Fill candidate) {
        if (candidate != null) {
            visit(candidate.getGraphicFill());
            visit(candidate.getColor());
            visit(candidate.getOpacity());
        }
    }

    protected void visit(final Font candidate) {
        if (candidate != null) {
            candidate.getFamily().forEach(this::visit);
            visit(candidate.getSize());
            visit(candidate.getStyle());
            visit(candidate.getWeight());
        }
    }

    protected void visit(final GraphicFill candidate) {
        visit((Graphic) candidate);
    }

    protected void visit(final GraphicStroke candidate) {
        if (candidate != null) {
            visit((Graphic) candidate);
            visit(candidate.getGap());
            visit(candidate.getInitialGap());
        }
    }

    protected void visit(final LabelPlacement candidate) {
        if (candidate instanceof PointPlacement) {
            visit((PointPlacement) candidate);
        } else if (candidate instanceof LinePlacement) {
            visit((LinePlacement) candidate);
        } else if (candidate != null) {
            throw new IllegalArgumentException("Unexpected Placement " + candidate);
        }
    }

    protected void visit(final PointPlacement candidate) {
        if (candidate != null) {
            visit(candidate.getAnchorPoint());
            visit(candidate.getDisplacement());
            visit(candidate.getRotation());
        }
    }

    protected void visit(final AnchorPoint candidate) {
        if (candidate != null) {
            visit(candidate.getAnchorPointX());
            visit(candidate.getAnchorPointY());
        }
    }

    protected void visit(final LinePlacement candidate) {
        if (candidate != null) {
            visit(candidate.getGap());
            visit(candidate.getInitialGap());
            visit(candidate.getPerpendicularOffset());
        }
    }

    protected void visit(final GraphicLegend candidate) {
        visit((Graphic) candidate);
    }

    protected void visit(final Halo candidate) {
        if (candidate != null) {
            visit(candidate.getFill());
            visit(candidate.getRadius());
        }
    }

    protected void visit(final ColorMap candidate) {
        if (candidate != null) {
            visit(candidate.getFunction());
        }
    }

    protected void visit(final ColorReplacement candidate) {
        if (candidate != null) {
            visit(candidate.getRecoding());
        }
    }

    protected void visit(final ContrastEnhancement candidate) {
        if (candidate != null) {
            visit(candidate.getGammaValue());
        }
    }

    protected void visit(final ChannelSelection candidate) {
        if (candidate != null) {
            visit(candidate.getGrayChannel());
            final SelectedChannelType[] rgbChannels = candidate.getRGBChannels();
            if (rgbChannels != null) {
                for (final SelectedChannelType sct : rgbChannels) {
                    visit(sct);
                }
            }
        }
    }

    protected void visit(final SelectedChannelType candidate) {
        if (candidate != null) {
            visit(candidate.getContrastEnhancement());
        }
    }

    protected void visit(final ShadedRelief candidate) {
        if (candidate != null) {
            visit(candidate.getReliefFactor());
        }
    }

    /**
     * Find all value references and literal in the given expression and its parameters.
     * This method invokes itself recursively.
     *
     * @param  candidate  the filter to examine, or {@code null} if none.
     */
    protected void visit(final Filter<?> candidate) {
        if (candidate != null) {
            if (candidate instanceof LogicalOperator<?>) {
                ((LogicalOperator<?>) candidate).getOperands().forEach(this::visit);
            } else {
                candidate.getExpressions().forEach(this::visit);
            }
        }
    }

    /**
     * Find all value references and literal in the given expression and its parameters.
     * This method invokes itself recursively.
     *
     * @param  candidate  the expression to examine, or {@code null} if none.
     */
    protected void visit(final Expression<?,?> candidate) {
        if (candidate != null) {
            if (candidate instanceof ValueReference<?,?>) {
                visitProperty((ValueReference<?,?>) candidate);
            } else if (candidate instanceof Literal<?,?>) {
                visitLiteral((Literal<?,?>) candidate);
            } else {
                candidate.getParameters().forEach(this::visit);
            }
        }
    }

    /**
     * Invoked by {@link #visit(Expression)} for each value reference.
     *
     * @param  expression  a value reference found in a chain of expressions.
     */
    protected void visitProperty(ValueReference<?,?> expression) {
    }

    /**
     * Invoked by {@link #visit(Expression)} for each literal.
     *
     * @param  expression  a literal found in a chain of expressions.
     */
    protected void visitLiteral(Literal<?,?> expression) {
    }
}
