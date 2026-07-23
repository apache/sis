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
package org.apache.sis.geometries.scene.lights;

import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;

/**
 * A disk or ring shaped area light emitting into a cone of directions along
 * the node's local -Z axis, with a cosine falloff.
 *
 * Has no glTF {@code KHR_lights_punctual} equivalent; added to reach
 * feature parity with the Khronos ANARI {@code ring} light.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Ring extends Light {

    private double radius = 1.0;
    private double innerRadius = 0.0;
    private double openingAngle = Math.PI;
    private double falloffAngle = 0.1;
    private double intensity = 1.0;

    public Ring() {
    }

    /**
     * The outer radius of the ring (a disk when innerRadius is 0). Default is 1.0.
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

    /**
     * Turns the disk into a ring; must be smaller than {@link #getRadius() }.
     * Default is 0.0.
     * @return inner radius
     */
    public double getInnerRadius() {
        return innerRadius;
    }

    /**
     * @param innerRadius must not be negative
     */
    public void setInnerRadius(double innerRadius) {
        ArgumentChecks.ensurePositive("innerRadius", innerRadius);
        this.innerRadius = innerRadius;
    }

    /**
     * Full opening angle (radians) of the cone of directions; outside this
     * cone there is no illumination. Default is PI.
     * @return opening angle in radians
     */
    public double getOpeningAngle() {
        return openingAngle;
    }

    /**
     * @param openingAngle radians, must be in [0, PI]
     */
    public void setOpeningAngle(double openingAngle) {
        ArgumentChecks.ensureBetween("openingAngle", 0.0, Math.PI, openingAngle);
        this.openingAngle = openingAngle;
    }

    /**
     * Size (radians) of the region between the rim of the illumination cone
     * and full intensity; should be smaller than half of the opening angle.
     * Default is 0.1.
     * @return falloff angle in radians
     */
    public double getFalloffAngle() {
        return falloffAngle;
    }

    /**
     * @param falloffAngle must not be negative
     */
    public void setFalloffAngle(double falloffAngle) {
        ArgumentChecks.ensurePositive("falloffAngle", falloffAngle);
        this.falloffAngle = falloffAngle;
    }

    /**
     * The overall amount of light emitted by the light in a direction, in
     * W/sr. Default is 1.0.
     * @return intensity
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * @param intensity must not be negative
     */
    public void setIntensity(double intensity) {
        ArgumentChecks.ensurePositive("intensity", intensity);
        this.intensity = intensity;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(getColor());
        hash = 79 * hash + Boolean.hashCode(isVisible());
        hash = 79 * hash + Double.hashCode(this.radius);
        hash = 79 * hash + Double.hashCode(this.innerRadius);
        hash = 79 * hash + Double.hashCode(this.openingAngle);
        hash = 79 * hash + Double.hashCode(this.falloffAngle);
        hash = 79 * hash + Double.hashCode(this.intensity);
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
        final Ring other = (Ring) obj;
        if (Double.compare(this.radius, other.radius) != 0) {
            return false;
        }
        if (Double.compare(this.innerRadius, other.innerRadius) != 0) {
            return false;
        }
        if (Double.compare(this.openingAngle, other.openingAngle) != 0) {
            return false;
        }
        if (Double.compare(this.falloffAngle, other.falloffAngle) != 0) {
            return false;
        }
        if (Double.compare(this.intensity, other.intensity) != 0) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        return Objects.equals(this.getColor(), other.getColor());
    }
}
