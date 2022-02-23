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
 * Mutable implementation of {@link org.opengis.style.LinePlacement}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LinePlacement implements LabelPlacement, org.opengis.style.LinePlacement {

    private Expression<Feature,? extends Number> perpendicularOffset;
    private Expression<Feature,? extends Number> initialGap;
    private Expression<Feature,? extends Number> gap;
    private boolean repeated;
    private boolean aligned;
    private boolean generalizeLine;

    public LinePlacement() {
    }

    public LinePlacement(
            Expression<Feature, Number> perpendicularOffset,
            Expression<Feature, Number> initialGap,
            Expression<Feature, Number> gap,
            boolean repeated, boolean aligned, boolean generalizeLine) {
        this.perpendicularOffset = perpendicularOffset;
        this.initialGap = initialGap;
        this.gap = gap;
        this.repeated = repeated;
        this.aligned = aligned;
        this.generalizeLine = generalizeLine;
    }

    @Override
    public Expression<Feature,? extends Number> getPerpendicularOffset() {
        return perpendicularOffset;
    }

    public void setPerpendicularOffset(Expression<Feature, ? extends Number> perpendicularOffset) {
        this.perpendicularOffset = perpendicularOffset;
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
    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }

    @Override
    public boolean IsAligned() {
        return aligned;
    }

    public void setAligned(boolean aligned) {
        this.aligned = aligned;
    }

    @Override
    public boolean isGeneralizeLine() {
        return generalizeLine;
    }

    public void setGeneralizeLine(boolean generalizeLine) {
        this.generalizeLine = generalizeLine;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perpendicularOffset, initialGap, gap, repeated, aligned, generalizeLine);
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
        final LinePlacement other = (LinePlacement) obj;
        return this.repeated == other.repeated
            && this.aligned == other.aligned
            && this.generalizeLine == other.generalizeLine
            && Objects.equals(this.perpendicularOffset, other.perpendicularOffset)
            && Objects.equals(this.initialGap, other.initialGap)
            && Objects.equals(this.gap, other.gap);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static LinePlacement castOrCopy(org.opengis.style.LinePlacement candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof LinePlacement) {
            return (LinePlacement) candidate;
        }
        return new LinePlacement(
                candidate.getPerpendicularOffset(),
                candidate.getInitialGap(),
                candidate.getGap(),
                candidate.isRepeated(),
                candidate.IsAligned(),
                candidate.isGeneralizeLine());
    }

}
