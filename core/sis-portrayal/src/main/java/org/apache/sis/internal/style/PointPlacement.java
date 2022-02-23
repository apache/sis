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
 * Mutable implementation of {@link org.opengis.style.PointPlacement}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PointPlacement implements LabelPlacement, org.opengis.style.PointPlacement {

    private AnchorPoint anchorPoint;
    private Displacement displacement;
    private Expression<Feature,? extends Number> rotation;

    public PointPlacement() {
    }

    public PointPlacement(
            AnchorPoint anchorPoint,
            Displacement displacement,
            Expression<Feature, ? extends Number> rotation) {
        this.anchorPoint = anchorPoint;
        this.displacement = displacement;
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
    public Expression<Feature,? extends Number> getRotation() {
        return rotation;
    }

    public void setRotation(Expression<Feature, ? extends Number> rotation) {
        this.rotation = rotation;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anchorPoint, displacement, rotation);
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
        final PointPlacement other = (PointPlacement) obj;
        return Objects.equals(this.anchorPoint, other.anchorPoint)
            && Objects.equals(this.displacement, other.displacement)
            && Objects.equals(this.rotation, other.rotation);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static PointPlacement castOrCopy(org.opengis.style.PointPlacement candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof PointPlacement) {
            return (PointPlacement) candidate;
        }
        return new PointPlacement(
                AnchorPoint.castOrCopy(candidate.getAnchorPoint()),
                Displacement.castOrCopy(candidate.getDisplacement()),
                candidate.getRotation());
    }
}
