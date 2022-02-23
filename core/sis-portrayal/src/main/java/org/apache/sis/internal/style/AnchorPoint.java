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
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.AnchorPoint}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class AnchorPoint implements org.opengis.style.AnchorPoint {

    private Expression<Feature,? extends Number> x;
    private Expression<Feature,? extends Number> y;

    public AnchorPoint() {
        this(StyleFactory.DEFAULT_ANCHOR_POINT_X, StyleFactory.DEFAULT_ANCHOR_POINT_Y);
    }

    public AnchorPoint(Expression<Feature,? extends Number> x, Expression<Feature,? extends Number> y) {
        ArgumentChecks.ensureNonNull("x", x);
        ArgumentChecks.ensureNonNull("y", y);
        this.x = x;
        this.y = y;
    }

    @Override
    public Expression<Feature,? extends Number> getAnchorPointX() {
        return x;
    }

    public void setAnchorPointX(Expression<Feature,? extends Number> x) {
        ArgumentChecks.ensureNonNull("x", x);
        this.x = x;
    }

    @Override
    public Expression<Feature,? extends Number> getAnchorPointY() {
        return y;
    }

    public void setAnchorPointY(Expression<Feature,? extends Number> y) {
        ArgumentChecks.ensureNonNull("y", y);
        this.y = y;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
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
        final AnchorPoint other = (AnchorPoint) obj;
        return Objects.equals(this.x, other.x)
            && Objects.equals(this.y, other.y);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static AnchorPoint castOrCopy(org.opengis.style.AnchorPoint candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof AnchorPoint) {
            return (AnchorPoint) candidate;
        }
        return new AnchorPoint(candidate.getAnchorPointX(), candidate.getAnchorPointY());
    }
}
