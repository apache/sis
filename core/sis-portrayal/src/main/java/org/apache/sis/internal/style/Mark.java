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
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.Mark}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Mark implements org.opengis.style.Mark, GraphicalSymbol {

    private Expression<Feature,String> wellKnownName;
    private ExternalMark externalMark;
    private Fill fill;
    private Stroke stroke;

    public Mark() {
        this(StyleFactory.MARK_SQUARE, null, new Fill(), new Stroke());
    }

    public Mark(Expression<Feature, String> wellKnownName,
            ExternalMark externalMark,
            Fill fill,
            Stroke stroke) {
        this.wellKnownName = wellKnownName;
        this.externalMark = externalMark;
        this.fill = fill;
        this.stroke = stroke;
    }

    @Override
    public Expression getWellKnownName() {
        return wellKnownName;
    }

    public void setWellKnownName(Expression<Feature, String> wellKnownName) {
        this.wellKnownName = wellKnownName;
    }

    @Override
    public ExternalMark getExternalMark() {
        return externalMark;
    }

    public void setExternalMark(ExternalMark externalMark) {
        this.externalMark = externalMark;
    }

    @Override
    public Fill getFill() {
        return fill;
    }

    public void setFill(Fill fill) {
        this.fill = fill;
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
    public int hashCode() {
        return Objects.hash(wellKnownName, externalMark, fill, stroke);
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
        final Mark other = (Mark) obj;
        return Objects.equals(this.wellKnownName, other.wellKnownName)
            && Objects.equals(this.externalMark, other.externalMark)
            && Objects.equals(this.fill, other.fill)
            && Objects.equals(this.stroke, other.stroke);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Mark castOrCopy(org.opengis.style.Mark candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Mark) {
            return (Mark) candidate;
        }
        return new Mark(
                candidate.getWellKnownName(),
                ExternalMark.castOrCopy(candidate.getExternalMark()),
                Fill.castOrCopy(candidate.getFill()),
                Stroke.castOrCopy(candidate.getStroke()));
    }
}
