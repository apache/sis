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
 * A planar, procedural rectangular area light source lying in the node's
 * local XY plane (width along local X, height along local Y), emitting
 * uniformly toward one or both sides.
 *
 * Has no glTF {@code KHR_lights_punctual} equivalent; added to reach
 * feature parity with the Khronos ANARI {@code quad} light.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Quad extends Light {

    /**
     * Side into which light is emitted.
     */
    public static enum Side {
        FRONT,
        BACK,
        BOTH
    }

    private double width = 1.0;
    private double height = 1.0;
    private double intensity = 1.0;
    private Side side = Side.FRONT;

    public Quad() {
    }

    /**
     * Extent of the quad along the node's local X axis. Default is 1.0.
     * @return width
     */
    public double getWidth() {
        return width;
    }

    /**
     * @param width must be strictly positive
     */
    public void setWidth(double width) {
        ArgumentChecks.ensureStrictlyPositive("width", width);
        this.width = width;
    }

    /**
     * Extent of the quad along the node's local Y axis. Default is 1.0.
     * @return height
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param height must be strictly positive
     */
    public void setHeight(double height) {
        ArgumentChecks.ensureStrictlyPositive("height", height);
        this.height = height;
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

    /**
     * Side into which light is emitted. Default is FRONT.
     * @return side, never null
     */
    public Side getSide() {
        return side;
    }

    /**
     * @param side not null
     */
    public void setSide(Side side) {
        ArgumentChecks.ensureNonNull("side", side);
        this.side = side;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Objects.hashCode(getColor());
        hash = 73 * hash + Boolean.hashCode(isVisible());
        hash = 73 * hash + Double.hashCode(this.width);
        hash = 73 * hash + Double.hashCode(this.height);
        hash = 73 * hash + Double.hashCode(this.intensity);
        hash = 73 * hash + Objects.hashCode(this.side);
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
        final Quad other = (Quad) obj;
        if (Double.compare(this.width, other.width) != 0) {
            return false;
        }
        if (Double.compare(this.height, other.height) != 0) {
            return false;
        }
        if (Double.compare(this.intensity, other.intensity) != 0) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        if (this.side != other.side) {
            return false;
        }
        return Objects.equals(this.getColor(), other.getColor());
    }
}
