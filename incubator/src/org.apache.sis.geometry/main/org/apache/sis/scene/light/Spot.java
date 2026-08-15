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
package org.apache.sis.scene.light;

import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;


/**
 * A light emitting into a cone of directions along the node's local -Z axis.
 *
 * Matches glTF {@code KHR_lights_punctual} type {@code "spot"} (luminous
 * intensity in candela, lm/sr, with {@code innerConeAngle}/
 * {@code outerConeAngle} and an optional attenuation {@code range} hint)
 * and Khronos ANARI {@code spot} light.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Spot extends Light {

    private double intensity = 1.0;
    private Double range;
    private double innerConeAngle = 0.0;
    private double outerConeAngle = Math.PI / 4.0;

    public Spot() {
    }

    /**
     * Luminous intensity emitted by the light, in candela (lm/sr).
     * Default is 1.0.
     * @return intensity
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * @param intensity candela, must not be negative
     */
    public void setIntensity(double intensity) {
        ArgumentChecks.ensurePositive("intensity", intensity);
        this.intensity = intensity;
    }

    /**
     * Hint for the distance cutoff at which the light's intensity may be
     * considered to be zero. Not physically based. Default is null (no cutoff).
     * @return range, or null if not set
     */
    public Double getRange() {
        return range;
    }

    /**
     * @param range distance cutoff hint, must be positive if not null
     */
    public void setRange(Double range) {
        if (range != null) {
            ArgumentChecks.ensureStrictlyPositive("range", range);
        }
        this.range = range;
    }

    /**
     * Angle (radians) from the spot's central axis where the intensity
     * begins to fall off toward {@link #getOuterConeAngle() }.
     * Default is 0.0. Must be in [0, outerConeAngle].
     * @return inner cone angle in radians
     */
    public double getInnerConeAngle() {
        return innerConeAngle;
    }

    /**
     * @param innerConeAngle radians, must be in [0, outerConeAngle]
     */
    public void setInnerConeAngle(double innerConeAngle) {
        ArgumentChecks.ensureBetween("innerConeAngle", 0.0, outerConeAngle, innerConeAngle);
        this.innerConeAngle = innerConeAngle;
    }

    /**
     * Angle (radians) from the spot's central axis where the intensity
     * reaches zero. Default is PI/4. Must be in [innerConeAngle, PI/2].
     * @return outer cone angle in radians
     */
    public double getOuterConeAngle() {
        return outerConeAngle;
    }

    /**
     * @param outerConeAngle radians, must be in [innerConeAngle, PI/2]
     */
    public void setOuterConeAngle(double outerConeAngle) {
        ArgumentChecks.ensureBetween("outerConeAngle", innerConeAngle, Math.PI / 2.0, outerConeAngle);
        this.outerConeAngle = outerConeAngle;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(getColor());
        hash = 71 * hash + Boolean.hashCode(isVisible());
        hash = 71 * hash + Double.hashCode(this.intensity);
        hash = 71 * hash + Objects.hashCode(this.range);
        hash = 71 * hash + Double.hashCode(this.innerConeAngle);
        hash = 71 * hash + Double.hashCode(this.outerConeAngle);
        return hash;
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
        final Spot other = (Spot) obj;
        if (Double.compare(this.intensity, other.intensity) != 0) {
            return false;
        }
        if (Double.compare(this.innerConeAngle, other.innerConeAngle) != 0) {
            return false;
        }
        if (Double.compare(this.outerConeAngle, other.outerConeAngle) != 0) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        if (!Objects.equals(this.range, other.range)) {
            return false;
        }
        return Objects.equals(this.getColor(), other.getColor());
    }
}
