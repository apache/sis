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
 * A light which is thought to be infinitely far away, so its rays arrive
 * (almost) parallel, such as sunlight.
 *
 * Matches glTF {@code KHR_lights_punctual} type {@code "directional"}
 * (illuminance in lux, lm/m2) and Khronos ANARI {@code directional} light.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Directional extends Light {

    private double intensity = 1.0;
    private double angularDiameter = 0.0;

    public Directional() {
    }

    /**
     * Illuminance arriving at a surface facing the light, in lux (lm/m2).
     * Default is 1.0.
     * @return intensity
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * @param intensity illuminance in lux, must not be negative
     */
    public void setIntensity(double intensity) {
        ArgumentChecks.ensurePositive("intensity", intensity);
        this.intensity = intensity;
    }

    /**
     * Apparent size (angle in radians) of the light. A value greater than
     * zero enables soft shadows. Default is 0.0.
     * Not part of glTF {@code KHR_lights_punctual}, ANARI-specific.
     * @return angular diameter in radians
     */
    public double getAngularDiameter() {
        return angularDiameter;
    }

    /**
     * @param angularDiameter angle in radians, must not be negative
     */
    public void setAngularDiameter(double angularDiameter) {
        ArgumentChecks.ensurePositive("angularDiameter", angularDiameter);
        this.angularDiameter = angularDiameter;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(getColor());
        hash = 61 * hash + Boolean.hashCode(isVisible());
        hash = 61 * hash + Double.hashCode(this.intensity);
        hash = 61 * hash + Double.hashCode(this.angularDiameter);
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
        final Directional other = (Directional) obj;
        if (Double.compare(this.intensity, other.intensity) != 0) {
            return false;
        }
        if (Double.compare(this.angularDiameter, other.angularDiameter) != 0) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        return Objects.equals(this.getColor(), other.getColor());
    }
}
