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
import java.util.Arrays;
import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.Stroke}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Stroke implements org.opengis.style.Stroke {

    private GraphicFill graphicFill;
    private GraphicStroke graphicStroke;
    private Expression<Feature,Color> color;
    private Expression<Feature,? extends Number> opacity;
    private Expression<Feature,? extends Number> width;
    private Expression<Feature,String> lineJoin;
    private Expression<Feature,String> lineCap;
    private float[] dashArray;
    private Expression<Feature,? extends Number> dashOffset;

    public Stroke() {
        this(null,null,
             StyleFactory.DEFAULT_STROKE_COLOR,
             StyleFactory.DEFAULT_STROKE_OPACITY,
             StyleFactory.DEFAULT_STROKE_WIDTH,
             StyleFactory.DEFAULT_STROKE_JOIN,
             StyleFactory.DEFAULT_STROKE_CAP,
             null,
             StyleFactory.DEFAULT_STROKE_OFFSET);
    }

    public Stroke(GraphicFill graphicFill, GraphicStroke graphicStroke,
            Expression<Feature, Color> color,
            Expression<Feature, ? extends Number> opacity,
            Expression<Feature, ? extends Number> width,
            Expression<Feature, String> lineJoin,
            Expression<Feature, String> lineCap,
            float[] dashArray,
            Expression<Feature, ? extends Number> dashOffset) {
        ArgumentChecks.ensureNonNull("color", color);
        ArgumentChecks.ensureNonNull("opacity", opacity);
        ArgumentChecks.ensureNonNull("width", width);
        ArgumentChecks.ensureNonNull("lineJoin", lineJoin);
        ArgumentChecks.ensureNonNull("lineCap", lineCap);
        ArgumentChecks.ensureNonNull("dashOffset", dashOffset);
        this.graphicFill = graphicFill;
        this.graphicStroke = graphicStroke;
        this.color = color;
        this.opacity = opacity;
        this.width = width;
        this.lineJoin = lineJoin;
        this.lineCap = lineCap;
        this.dashArray = dashArray;
        this.dashOffset = dashOffset;
    }

    @Override
    public GraphicFill getGraphicFill() {
        return graphicFill;
    }

    public void setGraphicFill(GraphicFill graphicFill) {
        this.graphicFill = graphicFill;
    }

    @Override
    public GraphicStroke getGraphicStroke() {
        return graphicStroke;
    }

    public void setGraphicStroke(GraphicStroke graphicStroke) {
        this.graphicStroke = graphicStroke;
    }

    @Override
    public Expression<Feature,Color> getColor() {
        return color;
    }

    public void setColor(Expression<Feature, Color> color) {
        ArgumentChecks.ensureNonNull("color", color);
        this.color = color;
    }

    @Override
    public Expression<Feature,? extends Number> getOpacity() {
        return opacity;
    }

    public void setOpacity(Expression<Feature, ? extends Number> opacity) {
        ArgumentChecks.ensureNonNull("opacity", opacity);
        this.opacity = opacity;
    }

    @Override
    public Expression<Feature,? extends Number> getWidth() {
        return width;
    }

    public void setWidth(Expression<Feature, ? extends Number> width) {
        ArgumentChecks.ensureNonNull("width", width);
        this.width = width;
    }

    @Override
    public Expression<Feature,String> getLineJoin() {
        return lineJoin;
    }

    public void setLineJoin(Expression<Feature, String> lineJoin) {
        ArgumentChecks.ensureNonNull("lineJoin", lineJoin);
        this.lineJoin = lineJoin;
    }

    @Override
    public Expression<Feature,String> getLineCap() {
        return lineCap;
    }

    public void setLineCap(Expression<Feature, String> lineCap) {
        ArgumentChecks.ensureNonNull("lineCap", lineCap);
        this.lineCap = lineCap;
    }

    @Override
    public float[] getDashArray() {
        return dashArray;
    }

    public void setDashArray(float[] dashArray) {
        this.dashArray = dashArray;
    }

    @Override
    public Expression<Feature,? extends Number> getDashOffset() {
        return dashOffset;
    }

    public void setDashOffset(Expression<Feature, ? extends Number> dashOffset) {
        ArgumentChecks.ensureNonNull("dashOffset", dashOffset);
        this.dashOffset = dashOffset;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphicFill, graphicStroke, color, opacity, width, lineJoin, lineCap, dashArray, dashOffset);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Stroke other = (Stroke) obj;
        return Objects.equals(this.graphicFill, other.graphicFill)
            && Objects.equals(this.graphicStroke, other.graphicStroke)
            && Objects.equals(this.color, other.color)
            && Objects.equals(this.opacity, other.opacity)
            && Objects.equals(this.width, other.width)
            && Objects.equals(this.lineJoin, other.lineJoin)
            && Objects.equals(this.lineCap, other.lineCap)
            && Arrays.equals(this.dashArray, other.dashArray)
            && Objects.equals(this.dashOffset, other.dashOffset);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Stroke castOrCopy(org.opengis.style.Stroke candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Stroke) {
            return (Stroke) candidate;
        }
        return new Stroke(
                GraphicFill.castOrCopy(candidate.getGraphicFill()),
                GraphicStroke.castOrCopy(candidate.getGraphicStroke()),
                candidate.getColor(),
                candidate.getOpacity(),
                candidate.getWidth(),
                candidate.getLineJoin(),
                candidate.getLineCap(),
                candidate.getDashArray(),
                candidate.getDashOffset());
    }
}
