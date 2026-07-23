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
import org.apache.sis.geometries.scene.Texture;
import org.apache.sis.util.ArgumentChecks;


/**
 * A material is a set of properties defining a visual representation.
 * Materials on there own do not suffice to obtain the visual aspect
 * of a model, they must be combined with a rendering technique.
 *
 * This base class holds the properties common to all shading techniques,
 * regardless of the shading model used. See {@link BlinnPhong}, {@link PBR}
 * and {@link PBRSG} for the technique-specific properties.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract sealed class Material permits BlinnPhong, PBR, PBRSG {

    public static final String ALPHA_MODE_OPAQUE = "OPAQUE";
    public static final String ALPHA_MODE_MASK = "MASK";
    public static final String ALPHA_MODE_BLEND = "BLEND";

    private String id;
    private boolean doubleSided;
    private String alphaMode = ALPHA_MODE_OPAQUE;
    private double alphaCutoff = 0.5;
    private Color emissiveFactor = Color.BLACK;
    private Texture emissiveTexture;
    private Texture occlusionTexture;
    private double occlusionStrength = 1.0;
    private Texture normalTexture;
    private double normalScale = 1.0;
    private boolean unlit;

    Material() {
    }

    /**
     * @return material identifier
     */
    public String getIdentifier() {
        return id;
    }

    /**
     * @param id set material identifier
     */
    public void setIdentifier(String id) {
        this.id = id;
    }

    /**
     * Default is OPAQUE
     * @return alphaMode
     */
    public String getAlphaMode() {
        return alphaMode;
    }

    /**
     * Default is OPAQUE
     * @param alphaMode
     */
    public void setAlphaMode(String alphaMode) {
        this.alphaMode = alphaMode;
    }

    /**
     * Default value is 0.5
     * @return alpha cutoff
     */
    public double getAlphaCutoff() {
        return alphaCutoff;
    }

    /**
     * Default value is 0.5
     * @param cutoff
     */
    public void setAlphaCutoff(double cutoff) {
        ArgumentChecks.ensureBetween("alpha cutoff", 0.0, 1.0, cutoff);
        this.alphaCutoff = cutoff;
    }

    /**
     * Default value is false
     * @return double sided.
     */
    public boolean isDoubleSided() {
        return doubleSided;
    }

    /**
     * Default value is false
     * @param doublesided
     */
    public void setDoubleSided(boolean doublesided) {
        this.doubleSided = doublesided;
    }

    /**
     * Default is BLACK (0,0,0)
     * @return emmisive factor
     */
    public Color getEmissiveFactor() {
        return emissiveFactor;
    }

    /**
     * Default is BLACK (0,0,0)
     * @param color emmisive factor
     */
    public void setEmissiveFactor(Color color) {
        this.emissiveFactor = color;
    }

    public Texture getEmissiveTexture() {
        return emissiveTexture;
    }

    public void setEmissiveTexture(Texture texture) {
        this.emissiveTexture = texture;
    }

    public Texture getOcclusionTexture() {
        return occlusionTexture;
    }

    public void setOcclusionTexture(Texture texture) {
        this.occlusionTexture = texture;
    }

    /**
     * Default is 1.0
     * @return occlusion strength
     */
    public double getOcclusionStrength() {
        return occlusionStrength;
    }

    /**
     * Default is 1.0
     * @param strength occlusion strength between 0.0 and 1.0
     */
    public void setOcclusionStrength(double strength) {
        ArgumentChecks.ensureBetween("occlusion strength", 0.0, 1.0, strength);
        this.occlusionStrength = strength;
    }

    public Texture getNormalTexture() {
        return normalTexture;
    }

    public void setNormalTexture(Texture texture) {
        this.normalTexture = texture;
    }

    /**
     * Default is 1.0
     * @return normal scale
     */
    public double getNormalScale() {
        return normalScale;
    }

    /**
     * Default is 1.0
     * @param scale normal scale
     */
    public void setNormalScale(double scale) {
        ArgumentChecks.ensureBetween("normal scale", 0.0, 1.0, scale);
        this.normalScale = scale;
    }

    /**
     * Default value is false
     * @return true if lights should be disabled for this material.
     */
    public boolean isUnlit() {
        return unlit;
    }

    /**
     * Default value is false
     * @param unlit if lights should be disabled for this material.
     */
    public void setUnlit(boolean unlit) {
        this.unlit = unlit;
    }

}
