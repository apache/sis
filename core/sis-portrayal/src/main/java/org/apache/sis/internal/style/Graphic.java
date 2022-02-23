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
 * Mutable implementation of {@link org.opengis.style.Graphic}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Graphic implements org.opengis.style.Graphic {

    private final List<GraphicalSymbol> graphicalSymbols = new ArrayList<>();
    private Expression<Feature,? extends Number> opacity;
    private Expression<Feature,? extends Number> size;
    private Expression<Feature,? extends Number> rotation;
    private AnchorPoint anchorPoint;
    private Displacement displacement;

    public static Graphic createDefault() {
        final List<GraphicalSymbol> symbols = new ArrayList<>();
        symbols.add(new Mark());
        return new Graphic(symbols,
                StyleFactory.DEFAULT_GRAPHIC_OPACITY,
                StyleFactory.DEFAULT_GRAPHIC_SIZE,
                StyleFactory.DEFAULT_GRAPHIC_ROTATION,
                new AnchorPoint(),
                new Displacement());
    }

    public Graphic() {

    }

    public Graphic(List<GraphicalSymbol> graphicalSymbols,
            Expression<Feature, ? extends Number> opacity,
            Expression<Feature, ? extends Number> size,
            Expression<Feature, ? extends Number> rotation,
            AnchorPoint anchorPoint,
            Displacement displacement) {
        if (graphicalSymbols!= null) this.graphicalSymbols.addAll(graphicalSymbols);
        this.opacity = opacity;
        this.size = size;
        this.rotation = rotation;
        this.anchorPoint = anchorPoint;
        this.displacement = displacement;
    }

    @Override
    public List<org.opengis.style.GraphicalSymbol> graphicalSymbols() {
        return (List) graphicalSymbols;
    }

    @Override
    public Expression<Feature,? extends Number> getOpacity() {
        return opacity;
    }

    public void setOpacity(Expression<Feature, ? extends Number> opacity) {
        this.opacity = opacity;
    }

    @Override
    public Expression<Feature,? extends Number> getSize() {
        return size;
    }

    public void setSize(Expression<Feature, ? extends Number> size) {
        this.size = size;
    }

    @Override
    public Expression<Feature,? extends Number> getRotation() {
        return rotation;
    }

    public void setRotation(Expression<Feature, ? extends Number> rotation) {
        this.rotation = rotation;
    }

    @Override
    public AnchorPoint getAnchorPoint() {
        return anchorPoint;
    }

    public void setAnchorPoint(AnchorPoint anchorPoint) {
        this.anchorPoint = anchorPoint;
    }

    @Override
    public Displacement getDisplacement() {
        return displacement;
    }

    public void setDisplacement(Displacement displacement) {
        this.displacement = displacement;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphicalSymbols, opacity, size, rotation, anchorPoint, displacement);
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
        final Graphic other = (Graphic) obj;
        return Objects.equals(this.graphicalSymbols, other.graphicalSymbols)
            && Objects.equals(this.opacity, other.opacity)
            && Objects.equals(this.size, other.size)
            && Objects.equals(this.rotation, other.rotation)
            && Objects.equals(this.anchorPoint, other.anchorPoint)
            && Objects.equals(this.displacement, other.displacement);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Graphic castOrCopy(org.opengis.style.Graphic candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Graphic) {
            return (Graphic) candidate;
        }

        final List<GraphicalSymbol> cs = new ArrayList<>();
        for (org.opengis.style.GraphicalSymbol cr : candidate.graphicalSymbols()) {
            cs.add(GraphicalSymbol.castOrCopy(cr));
        }
        return new Graphic(
                cs,
                candidate.getOpacity(),
                candidate.getSize(),
                candidate.getRotation(),
                AnchorPoint.castOrCopy(candidate.getAnchorPoint()),
                Displacement.castOrCopy(candidate.getDisplacement()));
    }
}
