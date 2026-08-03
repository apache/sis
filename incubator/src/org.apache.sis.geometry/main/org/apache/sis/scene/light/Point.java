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
 * A light emitting uniformly in all directions from a single point.
 *
 * Matches glTF {@code KHR_lights_punctual} type {@code "point"} (luminous
 * intensity in candela, lm/sr, with an optional attenuation {@code range}
 * hint) and Khronos ANARI {@code point} light.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Point extends Light {

    private double intensity = 1.0;
    private Double range;
    private double radius = 0.0;

    public Point() {
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
     * The size of the point light, turning it into a sphere light for soft
     * shadows. Default is 0.0.
     * Not part of glTF {@code KHR_lights_punctual}, ANARI-specific.
     * @return radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @param radius must not be negative
     */
    public void setRadius(double radius) {
        ArgumentChecks.ensurePositive("radius", radius);
        this.radius = radius;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(getColor());
        hash = 67 * hash + Boolean.hashCode(isVisible());
        hash = 67 * hash + Double.hashCode(this.intensity);
        hash = 67 * hash + Objects.hashCode(this.range);
        hash = 67 * hash + Double.hashCode(this.radius);
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
        final Point other = (Point) obj;
        if (Double.compare(this.intensity, other.intensity) != 0) {
            return false;
        }
        if (Double.compare(this.radius, other.radius) != 0) {
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
