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
package org.apache.sis.scene.material;

import java.awt.Color;
import java.util.Objects;
import org.apache.sis.scene.Texture;
import org.apache.sis.util.ArgumentChecks;


/**
 * Physically-Based Rendering material using the metallic-roughness workflow.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PBR extends Material {

    private Color baseColorFactor = Color.WHITE;
    private Texture baseColorTexture;
    private double metallicFactor = 1.0;
    private double roughnessFactor = 1.0;
    private Texture metallicRoughnessTexture;

    // See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_ior/README.md
    private double ior = 1.5;

    // See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_transmission/README.md
    private double transmissionFactor = 0.0;
    private Texture transmissionTexture;

    // See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_volume/README.md
    private double thicknessFactor = 0.0;
    private Texture thicknessTexture;
    private double attenuationDistance = Double.POSITIVE_INFINITY;
    private Color attenuationColor = Color.WHITE;

    public PBR() {
    }

    /**
     * Default is WHITE (1,1,1)
     * @return pbr base color factor
     */
    public Color getBaseColorFactor() {
        return baseColorFactor;
    }

    /**
     * Default is WHITE (1,1,1)
     * @param color pbr base color factor
     */
    public void setBaseColorFactor(Color color) {
        this.baseColorFactor = color;
    }

    public Texture getBaseColorTexture() {
        return baseColorTexture;
    }

    public void setBaseColorTexture(Texture texture) {
        this.baseColorTexture = texture;
    }

    /**
     * Default is 1.0
     * @return between 0.0 and 1.0
     */
    public double getMetallicFactor() {
        return metallicFactor;
    }

    /**
     * Default is 1.0
     * @param factor between 0.0 and 1.0
     */
    public void setMetallicFactor(double factor) {
        ArgumentChecks.ensureBetween("pbr metallic factor", 0.0, 1.0, factor);
        this.metallicFactor = factor;
    }

    /**
     * Default is 1.0
     * @return between 0.0 and 1.0
     */
    public double getRoughnessFactor() {
        return roughnessFactor;
    }

    /**
     * Default is 1.0
     * @param roughness  between 0.0 and 1.0
     */
    public void setRoughnessFactor(double roughness) {
        ArgumentChecks.ensureBetween("PBR roughness factor", 0.0, 1.0, roughness);
        this.roughnessFactor = roughness;
    }

    public Texture getMetallicRoughnessTexture() {
        return metallicRoughnessTexture;
    }

    public void setMetallicRoughnessTexture(Texture texture) {
        this.metallicRoughnessTexture = texture;
    }

    /**
     * Default value is 1.5
     * @return index of refraction
     */
    public double getIOR() {
        return ior;
    }

    /**
     * Default value is 1.5
     * @param ior
     */
    public void setIOR(double ior) {
        ArgumentChecks.ensureBetween("ior", 1.0, 100.0, ior);
        this.ior = ior;
    }

    /**
     * Default value is 0.0
     * @return transmission factor
     */
    public double getTransmissionFactor() {
        return transmissionFactor;
    }

    /**
     * Default value is 0.0
     * @param tf
     */
    public void setTransmissionFactor(double tf) {
        this.transmissionFactor = tf;
    }

    /**
     * Default is null
     * @return transmission texture
     */
    public Texture getTransmissionTexture() {
        return transmissionTexture;
    }

    /**
     * Default is null
     * @param texture transmission
     */
    public void setTransmissionTexture(Texture texture) {
        this.transmissionTexture = texture;
    }

    /**
     * Default value is 0.0
     * @return thickness factor
     */
    public double getThicknessFactor() {
        return thicknessFactor;
    }

    /**
     * Default value is 0.0
     * @param tf
     */
    public void setThicknessFactor(double tf) {
        this.thicknessFactor = tf;
    }

    /**
     * Default is null
     * @return thickness texture
     */
    public Texture getThicknessTexture() {
        return thicknessTexture;
    }

    /**
     * Default is null
     * @param texture thickness
     */
    public void setThicknessTexture(Texture texture) {
        this.thicknessTexture = texture;
    }

    /**
     * Default value is +Infinity
     * @return volume attenuation distance
     */
    public double getAttenuationDistance() {
        return attenuationDistance;
    }

    /**
     * Default value is +Infinity
     * @param ad volume attenuation distance
     */
    public void setAttenuationDistance(double ad) {
        this.attenuationDistance = ad;
    }

    /**
     * Default is WHITE (1,1,1)
     * @return volume attenuation color
     */
    public Color getAttenuationColor() {
        return attenuationColor;
    }

    /**
     * Default is WHITE (1,1,1)
     * @param color volume attenuation color
     */
    public void setAttenuationColor(Color color) {
        this.attenuationColor = color;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(getIdentifier());
        hash = 41 * hash + Boolean.hashCode(isDoubleSided());
        hash = 41 * hash + Objects.hashCode(getAlphaMode());
        hash = 41 * hash + Double.hashCode(getAlphaCutoff());
        hash = 41 * hash + Objects.hashCode(getEmissiveFactor());
        hash = 41 * hash + Objects.hashCode(getEmissiveTexture());
        hash = 41 * hash + Objects.hashCode(getOcclusionTexture());
        hash = 41 * hash + Double.hashCode(getOcclusionStrength());
        hash = 41 * hash + Objects.hashCode(getNormalTexture());
        hash = 41 * hash + Double.hashCode(getNormalScale());
        hash = 41 * hash + Boolean.hashCode(isUnlit());
        hash = 41 * hash + Objects.hashCode(this.baseColorFactor);
        hash = 41 * hash + Objects.hashCode(this.baseColorTexture);
        hash = 41 * hash + Double.hashCode(this.metallicFactor);
        hash = 41 * hash + Double.hashCode(this.roughnessFactor);
        hash = 41 * hash + Objects.hashCode(this.metallicRoughnessTexture);
        hash = 41 * hash + Double.hashCode(this.ior);
        hash = 41 * hash + Double.hashCode(this.transmissionFactor);
        hash = 41 * hash + Objects.hashCode(this.transmissionTexture);
        hash = 41 * hash + Double.hashCode(this.thicknessFactor);
        hash = 41 * hash + Objects.hashCode(this.thicknessTexture);
        hash = 41 * hash + Double.hashCode(this.attenuationDistance);
        hash = 41 * hash + Objects.hashCode(this.attenuationColor);
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
        final PBR other = (PBR) obj;
        if (Double.compare(this.getAlphaCutoff(), other.getAlphaCutoff()) != 0) {
            return false;
        }
        if (Double.compare(this.getOcclusionStrength(), other.getOcclusionStrength()) != 0) {
            return false;
        }
        if (Double.compare(this.getNormalScale(), other.getNormalScale()) != 0) {
            return false;
        }
        if (this.isDoubleSided() != other.isDoubleSided()) {
            return false;
        }
        if (this.isUnlit() != other.isUnlit()) {
            return false;
        }
        if (Double.compare(this.metallicFactor, other.metallicFactor) != 0) {
            return false;
        }
        if (Double.compare(this.roughnessFactor, other.roughnessFactor) != 0) {
            return false;
        }
        if (Double.compare(this.ior, other.ior) != 0) {
            return false;
        }
        if (Double.compare(this.transmissionFactor, other.transmissionFactor) != 0) {
            return false;
        }
        if (Double.compare(this.thicknessFactor, other.thicknessFactor) != 0) {
            return false;
        }
        if (Double.compare(this.attenuationDistance, other.attenuationDistance) != 0) {
            return false;
        }
        if (!Objects.equals(this.getIdentifier(), other.getIdentifier())) {
            return false;
        }
        if (!Objects.equals(this.getAlphaMode(), other.getAlphaMode())) {
            return false;
        }
        if (!Objects.equals(this.getEmissiveFactor(), other.getEmissiveFactor())) {
            return false;
        }
        if (!Objects.equals(this.getEmissiveTexture(), other.getEmissiveTexture())) {
            return false;
        }
        if (!Objects.equals(this.getOcclusionTexture(), other.getOcclusionTexture())) {
            return false;
        }
        if (!Objects.equals(this.getNormalTexture(), other.getNormalTexture())) {
            return false;
        }
        if (!Objects.equals(this.baseColorFactor, other.baseColorFactor)) {
            return false;
        }
        if (!Objects.equals(this.baseColorTexture, other.baseColorTexture)) {
            return false;
        }
        if (!Objects.equals(this.metallicRoughnessTexture, other.metallicRoughnessTexture)) {
            return false;
        }
        if (!Objects.equals(this.transmissionTexture, other.transmissionTexture)) {
            return false;
        }
        if (!Objects.equals(this.thicknessTexture, other.thicknessTexture)) {
            return false;
        }
        return Objects.equals(this.attenuationColor, other.attenuationColor);
    }
}
