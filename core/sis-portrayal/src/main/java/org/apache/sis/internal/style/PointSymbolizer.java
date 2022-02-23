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
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.PointSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PointSymbolizer extends Symbolizer implements org.opengis.style.PointSymbolizer {

    private Graphic graphic;

    public static PointSymbolizer createDefault() {
        return new PointSymbolizer(
                null,
                null,
                new Description(),
                StyleFactory.DEFAULT_UOM,
                Graphic.createDefault());
    }

    public PointSymbolizer() {
    }

    public PointSymbolizer(String name, Expression geometry, Description description, Unit<Length> unit, Graphic graphic) {
        super(name, geometry, description, unit);
        this.graphic = graphic;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public Graphic getGraphic() {
        return graphic;
    }

    public void setGraphic(Graphic graphic) {
        this.graphic = graphic;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hashCode(this.graphic);
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
        final PointSymbolizer other = (PointSymbolizer) obj;
        return Objects.equals(this.graphic, other.graphic);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static PointSymbolizer castOrCopy(org.opengis.style.PointSymbolizer candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof PointSymbolizer) {
            return (PointSymbolizer) candidate;
        }
        return new PointSymbolizer(
                candidate.getName(),
                candidate.getGeometry(),
                Description.castOrCopy(candidate.getDescription()),
                candidate.getUnitOfMeasure(),
                Graphic.castOrCopy(candidate.getGraphic())
            );
    }
}
