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

import java.awt.image.RenderedImage;
import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;


/**
 * A textured light source surrounding the scene, illuminating it from
 * infinity. The environment map orientation follows the node's local axes
 * (local -Z is the direction the center of the texture is mapped to, local
 * +Y is up), the same convention as {@link Point}/{@link Spot} placement.
 *
 * Has no glTF {@code KHR_lights_punctual} equivalent; added to reach
 * feature parity with the Khronos ANARI {@code hdri} light.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HDRI extends Light {

    private RenderedImage radiance;
    private double scale = 1.0;
    private String layout = "equirectangular";

    public HDRI() {
    }

    /**
     * Environment map, typically HDR with values &gt; 1: the amount of
     * light emitted by a point on the light source in a direction, in W/sr/m2.
     * @return radiance image, may be null
     */
    public RenderedImage getRadiance() {
        return radiance;
    }

    /**
     * @param radiance environment map, may be null
     */
    public void setRadiance(RenderedImage radiance) {
        this.radiance = radiance;
    }

    /**
     * Scale factor applied to the radiance image values. Default is 1.0.
     * @return scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * @param scale scale factor
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     * Pixel layout of the environment map. Default is "equirectangular",
     * currently the only value supported by ANARI.
     * @return layout, never null
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @param layout not null
     */
    public void setLayout(String layout) {
        ArgumentChecks.ensureNonNull("layout", layout);
        this.layout = layout;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(getColor());
        hash = 83 * hash + Boolean.hashCode(isVisible());
        hash = 83 * hash + Objects.hashCode(this.radiance);
        hash = 83 * hash + Double.hashCode(this.scale);
        hash = 83 * hash + Objects.hashCode(this.layout);
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
        final HDRI other = (HDRI) obj;
        if (Double.compare(this.scale, other.scale) != 0) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        if (!Objects.equals(this.layout, other.layout)) {
            return false;
        }
        if (!Objects.equals(this.radiance, other.radiance)) {
            return false;
        }
        return Objects.equals(this.getColor(), other.getColor());
    }
}
