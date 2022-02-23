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
 * Mutable implementation of {@link org.opengis.style.LineSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LineSymbolizer extends Symbolizer implements org.opengis.style.LineSymbolizer {

    private Expression<Feature,? extends Number> perpendicularOffset;
    private Stroke stroke;

    public static LineSymbolizer createDefault() {
        return new LineSymbolizer(null, null, new Description(),
                StyleFactory.DEFAULT_UOM, new Stroke(), StyleFactory.LITERAL_ZERO);
    }

    public LineSymbolizer() {
        super();
    }

    public LineSymbolizer(String name, Expression geometry, Description description,
            Unit<Length> unit, Stroke stroke, Expression offset) {
        super(name, geometry, description, unit);
        this.stroke = stroke;
        this.perpendicularOffset = offset;
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    @Override
    public Expression<Feature,? extends Number> getPerpendicularOffset() {
        return perpendicularOffset;
    }

    public void setPerpendicularOffset(Expression<Feature, ? extends Number> perpendicularOffset) {
        this.perpendicularOffset = perpendicularOffset;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(perpendicularOffset, stroke);
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
        final LineSymbolizer other = (LineSymbolizer) obj;
        return Objects.equals(this.perpendicularOffset, other.perpendicularOffset)
            && Objects.equals(this.stroke, other.stroke);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static LineSymbolizer castOrCopy(org.opengis.style.LineSymbolizer candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof LineSymbolizer) {
            return (LineSymbolizer) candidate;
        }
        return new LineSymbolizer(
                candidate.getName(),
                candidate.getGeometry(),
                Description.castOrCopy(candidate.getDescription()),
                candidate.getUnitOfMeasure(),
                Stroke.castOrCopy(candidate.getStroke()),
                candidate.getPerpendicularOffset());
    }


}
