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
 * Mutable implementation of {@link org.opengis.style.Halo}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Halo implements org.opengis.style.Halo {

    private Fill fill;
    private Expression<Feature,? extends Number> radius;

    public Halo() {
        this(new Fill(null, StyleFactory.LITERAL_WHITE, StyleFactory.DEFAULT_FILL_OPACITY),
                StyleFactory.DEFAULT_HALO_RADIUS);
    }

    public Halo(Fill fill, Expression<Feature, ? extends Number> radius) {
        ArgumentChecks.ensureNonNull("fill", fill);
        ArgumentChecks.ensureNonNull("radius", radius);
        this.fill = fill;
        this.radius = radius;
    }

    @Override
    public Fill getFill() {
        return fill;
    }

    public void setFill(Fill fill) {
        ArgumentChecks.ensureNonNull("fill", fill);
        this.fill = fill;
    }

    @Override
    public Expression<Feature,? extends Number> getRadius() {
        return radius;
    }

    public void setRadius(Expression<Feature, ? extends Number> radius) {
        ArgumentChecks.ensureNonNull("radius", radius);
        this.radius = radius;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fill, radius);
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
        final Halo other = (Halo) obj;
        return Objects.equals(this.fill, other.fill)
            && Objects.equals(this.radius, other.radius);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Halo castOrCopy(org.opengis.style.Halo candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Halo) {
            return (Halo) candidate;
        }
        return new Halo(
                Fill.castOrCopy(candidate.getFill()),
                candidate.getRadius());
    }
}
