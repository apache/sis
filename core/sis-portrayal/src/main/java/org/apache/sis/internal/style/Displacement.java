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
 * Mutable implementation of {@link org.opengis.style.Displacement}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Displacement implements org.opengis.style.Displacement {

    private Expression<Feature,? extends Number> displacementX;
    private Expression<Feature,? extends Number> displacementY;

    public Displacement() {
        this(StyleFactory.DEFAULT_DISPLACEMENT_X, StyleFactory.DEFAULT_DISPLACEMENT_Y);
    }

    public Displacement(Expression<Feature, ? extends Number> displacementX, Expression<Feature, ? extends Number> displacementY) {
        ArgumentChecks.ensureNonNull("displacementX", displacementX);
        ArgumentChecks.ensureNonNull("displacementY", displacementY);
        this.displacementX = displacementX;
        this.displacementY = displacementY;
    }

    @Override
    public Expression<Feature,? extends Number> getDisplacementX() {
        return displacementX;
    }

    public void setDisplacementX(Expression<Feature, ? extends Number> displacementX) {
        ArgumentChecks.ensureNonNull("displacementX", displacementX);
        this.displacementX = displacementX;
    }

    @Override
    public Expression<Feature,? extends Number> getDisplacementY() {
        return displacementY;
    }

    public void setDisplacementY(Expression<Feature, ? extends Number> displacementY) {
        ArgumentChecks.ensureNonNull("displacementY", displacementY);
        this.displacementY = displacementY;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displacementX, displacementY);
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
        final Displacement other = (Displacement) obj;
        return Objects.equals(this.displacementX, other.displacementX)
            && Objects.equals(this.displacementY, other.displacementY);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Displacement castOrCopy(org.opengis.style.Displacement candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Displacement) {
            return (Displacement) candidate;
        }
        return new Displacement(candidate.getDisplacementX(), candidate.getDisplacementY());
    }
}
