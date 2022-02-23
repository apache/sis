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

import java.util.Objects;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.PolygonSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PolygonSymbolizer extends Symbolizer implements org.opengis.style.PolygonSymbolizer {

    private Stroke stroke;
    private Fill fill;
    private Displacement displacement;
    private Expression<Feature,? extends Number> offset;

    public static PolygonSymbolizer createDefault() {
        return new PolygonSymbolizer(null,null, new Description(),
                StyleFactory.DEFAULT_UOM, new Stroke(), new Fill(),
                new Displacement(), StyleFactory.LITERAL_ZERO);
    }

    public PolygonSymbolizer() {
    }

    public PolygonSymbolizer(String name, Expression geometry, Description description, Unit<Length> unit, Stroke stroke, Fill fill, Displacement displacement, Expression offset) {
        super(name, geometry, description, unit);
        this.stroke = stroke;
        this.fill = fill;
        this.displacement = displacement;
        this.offset = offset;
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public Fill getFill() {
        return fill;
    }

    public void setFill(Fill fill) {
        this.fill = fill;
    }

    @Override
    public Displacement getDisplacement() {
        return displacement;
    }

    public void setDisplacement(Displacement displacement) {
        this.displacement = displacement;
    }

    @Override
    public Expression<Feature,? extends Number> getPerpendicularOffset() {
        return offset;
    }

    public void setPerpendicularOffset(Expression<Feature,? extends Number> offset) {
        this.offset = offset;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(stroke, fill, displacement, offset);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PolygonSymbolizer other = (PolygonSymbolizer) obj;
        return Objects.equals(this.stroke, other.stroke)
            && Objects.equals(this.fill, other.fill)
            && Objects.equals(this.displacement, other.displacement)
            && Objects.equals(this.offset, other.offset);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static PolygonSymbolizer castOrCopy(org.opengis.style.PolygonSymbolizer candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof PolygonSymbolizer) {
            return (PolygonSymbolizer) candidate;
        }
        return new PolygonSymbolizer(
                candidate.getName(),
                candidate.getGeometry(),
                Description.castOrCopy(candidate.getDescription()),
                candidate.getUnitOfMeasure(),
                Stroke.castOrCopy(candidate.getStroke()),
                Fill.castOrCopy(candidate.getFill()),
                Displacement.castOrCopy(candidate.getDisplacement()),
                candidate.getPerpendicularOffset()
            );
    }

}
