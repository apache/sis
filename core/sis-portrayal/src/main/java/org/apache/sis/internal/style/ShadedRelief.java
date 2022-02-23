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
 * Mutable implementation of {@link org.opengis.style.ShadedRelief}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ShadedRelief implements org.opengis.style.ShadedRelief {

    private boolean brightnessOnly;
    private Expression<Feature,? extends Number> reliefFactor;

    public ShadedRelief() {
    }

    public ShadedRelief(boolean brightnessOnly, Expression<Feature, ? extends Number> reliefFactor) {
        this.brightnessOnly = brightnessOnly;
        this.reliefFactor = reliefFactor;
    }

    @Override
    public boolean isBrightnessOnly() {
        return brightnessOnly;
    }

    public void setBrightnessOnly(boolean brightnessOnly) {
        this.brightnessOnly = brightnessOnly;
    }

    @Override
    public Expression<Feature, ? extends Number> getReliefFactor() {
        return reliefFactor;
    }

    public void setReliefFactor(Expression<Feature, ? extends Number> reliefFactor) {
        this.reliefFactor = reliefFactor;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brightnessOnly, reliefFactor);
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
        final ShadedRelief other = (ShadedRelief) obj;
        return this.brightnessOnly == other.brightnessOnly
            && Objects.equals(this.reliefFactor, other.reliefFactor);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static ShadedRelief castOrCopy(org.opengis.style.ShadedRelief candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof ShadedRelief) {
            return (ShadedRelief) candidate;
        }
        return new ShadedRelief(candidate.isBrightnessOnly(), candidate.getReliefFactor());
    }
}
