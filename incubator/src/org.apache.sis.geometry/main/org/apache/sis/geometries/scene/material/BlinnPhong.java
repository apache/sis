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
package org.apache.sis.geometries.scene.material;

import java.awt.Color;
import java.util.Objects;
import org.apache.sis.geometries.scene.Texture;


/**
 * Blinn-Phong material, the classic ambient/diffuse/specular shading model.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class BlinnPhong extends Material {

    private Color ambientFactor = Color.BLACK;
    private Texture ambientTexture;
    private Color diffuseFactor = Color.WHITE;
    private Texture diffuseTexture;
    private Color specularFactor = Color.WHITE;
    private Texture specularTexture;

    public BlinnPhong() {
    }

    /**
     * Default is BLACK (0,0,0)
     * @return ambient factor
     */
    public Color getAmbientFactor() {
        return ambientFactor;
    }

    /**
     * Default is BLACK (0,0,0)
     * @param color ambient factor
     */
    public void setAmbientFactor(Color color) {
        this.ambientFactor = color;
    }

    public Texture getAmbientTexture() {
        return ambientTexture;
    }

    public void setAmbientTexture(Texture texture) {
        this.ambientTexture = texture;
    }

    /**
     * Default is WHITE (1,1,1)
     * @return diffuse factor
     */
    public Color getDiffuseFactor() {
        return diffuseFactor;
    }

    /**
     * Default is WHITE (1,1,1)
     * @param color diffuse factor
     */
    public void setDiffuseFactor(Color color) {
        this.diffuseFactor = color;
    }

    public Texture getDiffuseTexture() {
        return diffuseTexture;
    }

    public void setDiffuseTexture(Texture texture) {
        this.diffuseTexture = texture;
    }

    /**
     * Default is WHITE (1,1,1)
     * @return specular factor
     */
    public Color getSpecularFactor() {
        return specularFactor;
    }

    /**
     * Default is WHITE (1,1,1)
     * @param color specular factor
     */
    public void setSpecularFactor(Color color) {
        this.specularFactor = color;
    }

    public Texture getSpecularTexture() {
        return specularTexture;
    }

    public void setSpecularTexture(Texture texture) {
        this.specularTexture = texture;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(getIdentifier());
        hash = 37 * hash + Boolean.hashCode(isDoubleSided());
        hash = 37 * hash + Objects.hashCode(getAlphaMode());
        hash = 37 * hash + Double.hashCode(getAlphaCutoff());
        hash = 37 * hash + Objects.hashCode(getEmissiveFactor());
        hash = 37 * hash + Objects.hashCode(getEmissiveTexture());
        hash = 37 * hash + Objects.hashCode(getOcclusionTexture());
        hash = 37 * hash + Double.hashCode(getOcclusionStrength());
        hash = 37 * hash + Objects.hashCode(getNormalTexture());
        hash = 37 * hash + Double.hashCode(getNormalScale());
        hash = 37 * hash + Boolean.hashCode(isUnlit());
        hash = 37 * hash + Objects.hashCode(this.ambientFactor);
        hash = 37 * hash + Objects.hashCode(this.ambientTexture);
        hash = 37 * hash + Objects.hashCode(this.diffuseFactor);
        hash = 37 * hash + Objects.hashCode(this.diffuseTexture);
        hash = 37 * hash + Objects.hashCode(this.specularFactor);
        hash = 37 * hash + Objects.hashCode(this.specularTexture);
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
        final BlinnPhong other = (BlinnPhong) obj;
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
        if (!Objects.equals(this.ambientFactor, other.ambientFactor)) {
            return false;
        }
        if (!Objects.equals(this.ambientTexture, other.ambientTexture)) {
            return false;
        }
        if (!Objects.equals(this.diffuseFactor, other.diffuseFactor)) {
            return false;
        }
        if (!Objects.equals(this.diffuseTexture, other.diffuseTexture)) {
            return false;
        }
        if (!Objects.equals(this.specularFactor, other.specularFactor)) {
            return false;
        }
        return Objects.equals(this.specularTexture, other.specularTexture);
    }
}
