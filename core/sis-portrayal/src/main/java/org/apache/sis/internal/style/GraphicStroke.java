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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.GraphicStroke}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GraphicStroke extends Graphic implements org.opengis.style.GraphicStroke {

    private Expression<Feature,? extends Number> initialGap;
    private Expression<Feature,? extends Number> gap;

    public GraphicStroke() {
    }

    public GraphicStroke(List<GraphicalSymbol> graphicalSymbols,
            Expression<Feature, ? extends Number> opacity,
            Expression<Feature, ? extends Number> size,
            Expression<Feature, ? extends Number> rotation,
            AnchorPoint anchorPoint,
            Displacement displacement,
            Expression<Feature, ? extends Number> initialGap,
            Expression<Feature, ? extends Number> gap) {
        super(graphicalSymbols, opacity, size, rotation, anchorPoint, displacement);
        this.initialGap = initialGap;
        this.gap = gap;
    }

    @Override
    public Expression<Feature,? extends Number> getInitialGap() {
        return initialGap;
    }

    public void setInitialGap(Expression<Feature, ? extends Number> initialGap) {
        this.initialGap = initialGap;
    }

    @Override
    public Expression<Feature,? extends Number> getGap() {
        return gap;
    }

    public void setGap(Expression<Feature, ? extends Number> gap) {
        this.gap = gap;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(initialGap, gap);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof GraphicStroke)) {
            return false;
        }
        final GraphicStroke other = (GraphicStroke) obj;
        return Objects.equals(this.initialGap, other.initialGap)
            && Objects.equals(this.gap, other.gap);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static GraphicStroke castOrCopy(org.opengis.style.GraphicStroke candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof GraphicStroke) {
            return (GraphicStroke) candidate;
        }
        final List<GraphicalSymbol> cs = new ArrayList<>();
        for (org.opengis.style.GraphicalSymbol cr : candidate.graphicalSymbols()) {
            cs.add(GraphicalSymbol.castOrCopy(cr));
        }
        return new GraphicStroke(
                cs,
                candidate.getOpacity(),
                candidate.getSize(),
                candidate.getRotation(),
                AnchorPoint.castOrCopy(candidate.getAnchorPoint()),
                Displacement.castOrCopy(candidate.getDisplacement()),
                candidate.getInitialGap(),
                candidate.getGap());
    }
}
