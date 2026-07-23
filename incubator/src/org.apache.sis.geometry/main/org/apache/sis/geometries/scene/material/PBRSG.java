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
import org.apache.sis.util.ArgumentChecks;


/**
 * Specular-glossiness material model from Physically-Based Rendering (PBR).
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PBRSG extends Material {

    private Color diffuseFactor = Color.WHITE;
    private Texture diffuseTexture;
    private Color specularFactor = Color.WHITE;
    private double glossinessFactor = 1.0;
    private Texture specularGlossinessTexture;

    public PBRSG() {
    }

    /**
     * Default is WHITE (1,1,1,1)
     * @return diffuse factor
     */
    public Color getDiffuseFactor() {
        return diffuseFactor;
    }

    /**
     * Default is WHITE (1,1,1,1)
     * @param color diffuse factor
     */
    public void setDiffuseFactor(Color color) {
        this.diffuseFactor = color;
    }

    /**
     * Default is null
     * @return diffuse texture
     */
    public Texture getDiffuseTexture() {
        return diffuseTexture;
    }

    /**
     * Default is null
     * @param texture diffuse
     */
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

    /**
     * Default is 1.0
     * @return between 0.0 and 1.0
     */
    public double getGlossinessFactor() {
        return glossinessFactor;
    }

    /**
     * Default is 1.0
     * @param factor between 0.0 and 1.0
     */
    public void setGlossinessFactor(double factor) {
        ArgumentChecks.ensureBetween("pbrsg glossiness factor", 0.0, 1.0, factor);
        this.glossinessFactor = factor;
    }

    /**
     * Default is null
     * @return specular glossiness texture
     */
    public Texture getSpecularGlossinessTexture() {
        return specularGlossinessTexture;
    }

    /**
     * Default is null
     * @param texture specular glossiness
     */
    public void setSpecularGlossinessTexture(Texture texture) {
        this.specularGlossinessTexture = texture;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(getIdentifier());
        hash = 43 * hash + Boolean.hashCode(isDoubleSided());
        hash = 43 * hash + Objects.hashCode(getAlphaMode());
        hash = 43 * hash + Double.hashCode(getAlphaCutoff());
        hash = 43 * hash + Objects.hashCode(getEmissiveFactor());
        hash = 43 * hash + Objects.hashCode(getEmissiveTexture());
        hash = 43 * hash + Objects.hashCode(getOcclusionTexture());
        hash = 43 * hash + Double.hashCode(getOcclusionStrength());
        hash = 43 * hash + Objects.hashCode(getNormalTexture());
        hash = 43 * hash + Double.hashCode(getNormalScale());
        hash = 43 * hash + Boolean.hashCode(isUnlit());
        hash = 43 * hash + Objects.hashCode(this.diffuseFactor);
        hash = 43 * hash + Objects.hashCode(this.diffuseTexture);
        hash = 43 * hash + Objects.hashCode(this.specularFactor);
        hash = 43 * hash + Double.hashCode(this.glossinessFactor);
        hash = 43 * hash + Objects.hashCode(this.specularGlossinessTexture);
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
        final PBRSG other = (PBRSG) obj;
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
        if (Double.compare(this.glossinessFactor, other.glossinessFactor) != 0) {
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
        if (!Objects.equals(this.diffuseFactor, other.diffuseFactor)) {
            return false;
        }
        if (!Objects.equals(this.diffuseTexture, other.diffuseTexture)) {
            return false;
        }
        if (!Objects.equals(this.specularFactor, other.specularFactor)) {
            return false;
        }
        return Objects.equals(this.specularGlossinessTexture, other.specularGlossinessTexture);
    }
}
