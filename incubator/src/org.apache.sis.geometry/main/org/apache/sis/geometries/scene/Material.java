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
package org.apache.sis.geometries.scene;

import java.awt.Color;
import java.util.HashMap;
import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;

/**
 * A material is a set of properties defining a visual representation.
 * Materials on there own do not suffice to obtain the visual aspect
 * of a model, they must be combined with a rendering technique.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Material extends HashMap<String,Object> {

    public static final String ALPHA_MODE_OPAQUE = "OPAQUE";
    public static final String ALPHA_MODE_MASK = "MASK";
    public static final String ALPHA_MODE_BLEND = "BLEND";

    // PBR rendering technique /////////////////////////////////////////////////

    /** Boolean, default is false */
    public static final String DOUBLESIDED = "doubleSided";
    /** Double, The alpha cutoff value of the material. default is 0.5 */
    public static final String ALPHACUTOFF = "alphaCutoff";
    /** String alpha mode. default is opaque*/
    public static final String ALPHAMODE = "alphaMode";
    /** Color */
    public static final String EMISSIVEFACTOR = "emissiveFactor";
    /** Texture */
    public static final String EMISSIVETEXTURE = "emissiveTexture";
    /** Texture */
    public static final String OCCLUSIONTEXTURE = "occlusionTexture";
    public static final String OCCLUSIONSTRENGTH = "occlusionStrength";
    /** Texture */
    public static final String NORMALTEXTURE = "normalTexture";
    public static final String NORMALSCALE = "normalScale";


    /** Color */
    public static final String PBR_BASECOLORFACTOR = "baseColorFactor";
    /** Texture */
    public static final String PBR_BASECOLORTEXTURE = "baseColorTexture";
    /** Number */
    public static final String PBR_METALLICFACTOR = "metallicFactor";
    /** Number */
    public static final String PBR_ROUGHNESSFACTOR = "roughnessFactor";
    /** Texture */
    public static final String PBR_METALLICROUGHNESSTEXTURE = "metallicRoughnessTexture";

    // PBR rendering technique /////////////////////////////////////////////////

    /** Color */
    public static final String PBRSG_DIFFUSEFACTOR = "diffuseFactor";
    /** Texture */
    public static final String PBRSG_DIFFUSETEXTURE = "diffuseTexture";
    /** Color */
    public static final String PBRSG_SPECULARFACTOR = "specularFactor";
    /** Scalar */
    public static final String PBRSG_GLOSSINESSFACTOR = "glossinessFactor";
    /** Texture */
    public static final String PBRSG_SPECULAR_GLOSSINESS_TEXTURE = "specularGlossinessTexture";


    // Blinn-Phong rendering technique /////////////////////////////////////////

    /** Color */
    public static final String BP_AMBIANTFACTOR = "ambiantFactor";
    /** Texture */
    public static final String BP_AMBIANTTEXTURE = "ambiantTexture";
    /** Color */
    public static final String BP_DIFFUSEFACTOR = "diffuseFactor";
    /** Texture */
    public static final String BP_DIFFUSETEXTURE = "diffuseTexture";
    /** Color */
    public static final String BP_SPECULARFACTOR = "specularFactor";
    /** Texture */
    public static final String BP_SPECULARTEXTURE = "specularTexture";

    // Unlit rendering technique /////////////////////////////////////////
    // See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_unlit/README.md

    /** Boolean */
    public static final String LIGHTS_UNLIT = "unlit";

    // See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_ior/README.md
    /** Double */
    public static final String IOR = "ior";

    // See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_transmission/README.md
    /** Double */
    public static final String TRANSMISSIONFACTOR = "transmissionFactor";
    /** Texture */
    public static final String TRANSMISSIONTEXTURE = "transmissionTexture";

    // See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_volume/README.md
    /** Double */
    public static final String THICKNESSFACTOR = "thicknessFactor";
    /** Texture */
    public static final String THICKNESSTEXTURE = "thicknessTexture";
    /** Double */
    public static final String ATTENUATIONDISTANCE = "attenuationDistance";
    /** Color */
    public static final String ATTENUATIONCOLOR = "attenuationColor";


    private String id;

    public Material() {

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
        String str = (String) get(ALPHAMODE);
        return (str == null) ? ALPHA_MODE_OPAQUE : str;
    }

    /**
     * Default is OPAQUE
     * @param alphaMode
     */
    public void setAlphaMode(String alphaMode) {
        put(ALPHAMODE, alphaMode);
    }

    /**
     * Default value is 0.5
     * @return alpha cutoff
     */
    public double getAlphaCutoff() {
        Number ac = (Number) get(ALPHACUTOFF);
        return (ac == null) ? 0.5 : ac.doubleValue();
    }

    /**
     * Default value is 0.5
     * @param cutoff
     */
    public void setAlphaCutoff(double cutoff) {
        ArgumentChecks.ensureBetween("alpha cutoff", 0.0, 1.0, cutoff);
        put(ALPHACUTOFF, cutoff);
    }

    /**
     * Default value is false
     * @return double sided.
     */
    public boolean isDoubleSided() {
        return Boolean.TRUE.equals(get(DOUBLESIDED));
    }

    /**
     * Default value is false
     * @param doublesided
     */
    public void setDoubleSided(boolean doublesided) {
        put(DOUBLESIDED, doublesided);
    }

    /**
     * Default is BLACK (0,0,0)
     * @return emmisive factor
     */
    public Color getEmissiveFactor() {
        Color color = (Color) get(EMISSIVEFACTOR);
        return (color == null) ? Color.BLACK : color;
    }

    /**
     * Default is BLACK (0,0,0)
     * @param color emmisive factor
     */
    public void setEmissiveFactor(Color color) {
        put(EMISSIVEFACTOR, color);
    }

    public Texture getEmissiveTexture() {
        return (Texture) get(EMISSIVETEXTURE);
    }

    public void setEmissiveTexture(Texture texture) {
        put(EMISSIVETEXTURE, texture);
    }

    public Texture getOcclusionTexture() {
        return (Texture) get(OCCLUSIONTEXTURE);
    }

    public void setOcclusionTexture(Texture texture) {
        put(OCCLUSIONTEXTURE, texture);
    }

    /**
     * Default is 1.0
     * @return occlusion strength
     */
    public double getOcclusionStrength() {
        Number strength = (Number) get(OCCLUSIONSTRENGTH);
        return (strength == null) ? 1.0 : strength.doubleValue();
    }

    /**
     * Default is 1.0
     * @param strength occlusion strength between 0.0 and 1.0
     */
    public void setOcclusionStrength(double strength) {
        ArgumentChecks.ensureBetween("occlusion strength", 0.0, 1.0, strength);
        put(OCCLUSIONSTRENGTH, strength);
    }

    public Texture getNormalTexture() {
        return (Texture) get(NORMALTEXTURE);
    }

    public void setNormalTexture(Texture texture) {
        put(NORMALTEXTURE, texture);
    }

    /**
     * Default is 1.0
     * @return normal scale
     */
    public double getNormalScale() {
        Number scale = (Number) get(NORMALSCALE);
        return (scale == null) ? 1.0 : scale.doubleValue();
    }

    /**
     * Default is 1.0
     * @param scale normal scale
     */
    public void setNormalScale(double scale) {
        ArgumentChecks.ensureBetween("normal scale", 0.0, 1.0, scale);
        put(NORMALSCALE, scale);
    }

    /**
     * Default is WHITE (1,1,1)
     * @return pbr base color factor
     */
    public Color getPBRBaseColorFactor() {
        Color color = (Color) get(PBR_BASECOLORFACTOR);
        return (color == null) ? Color.WHITE : color;
    }

    /**
     * Default is WHITE (1,1,1)
     * @param color pbr base color factor
     */
    public void setPBRBaseColorFactor(Color color) {
        put(PBR_BASECOLORFACTOR, color);
    }

    public Texture getPBRBaseColorTexture() {
        return (Texture) get(PBR_BASECOLORTEXTURE);
    }

    public void setPBRBaseColorTexture(Texture texture) {
        put(PBR_BASECOLORTEXTURE, texture);
    }

    /**
     * Default is 1.0
     * @return between 0.0 and 1.0
     */
    public double getPBRMetallicFactor() {
        Number factor = (Number) get(PBR_METALLICFACTOR);
        return (factor == null) ? 1.0 : factor.doubleValue();
    }

    /**
     * Default is 1.0
     * @param factor between 0.0 and 1.0
     */
    public void setPBRMetallicFactor(double factor) {
        ArgumentChecks.ensureBetween("pbr metallic factor", 0.0, 1.0, factor);
        put(PBR_METALLICFACTOR, factor);
    }

    /**
     * Default is 1.0
     * @return between 0.0 and 1.0
     */
    public double getPBRRoughnessFactor() {
        Number roughness = (Number) get(PBR_ROUGHNESSFACTOR);
        return (roughness == null) ? 1.0 : roughness.doubleValue();
    }

    /**
     * Default is 1.0
     * @param roughness  between 0.0 and 1.0
     */
    public void setPBRRoughnessFactor(double roughness) {
        ArgumentChecks.ensureBetween("PBR roughness factor", 0.0, 1.0, roughness);
        put(PBR_ROUGHNESSFACTOR, roughness);
    }

    public Texture getPBRMetallicRoughnessTexture() {
        return (Texture) get(PBR_METALLICROUGHNESSTEXTURE);
    }

    public void setPBRMetallicRoughnessTexture(Texture texture) {
        put(PBR_METALLICROUGHNESSTEXTURE, texture);
    }

    /**
     * Default is WHITE (1,1,1,1)
     * @return diffuse factor
     */
    public Color getPBRSGDiffuseFactor() {
        Color color = (Color) get(PBRSG_DIFFUSEFACTOR);
        return (color == null) ? Color.WHITE : color;
    }

    /**
     * Default is WHITE (1,1,1,1)
     * @param color diffuse factor
     */
    public void setPBRSGDiffuseFactor(Color color) {
        put(PBRSG_DIFFUSEFACTOR, color);
    }

    /**
     * Default is null
     * @return diffuse texture
     */
    public Texture getPBRSGDiffuseTexture() {
        return (Texture) get(PBRSG_DIFFUSETEXTURE);
    }

    /**
     * Default is null
     * @param texture diffuse
     */
    public void setPBRSGDiffuseTexture(Texture texture) {
        put(PBRSG_DIFFUSETEXTURE, texture);
    }

    /**
     * Default is 1.0
     * @return between 0.0 and 1.0
     */
    public double getPBRSGGlossinessFactor() {
        Number factor = (Number) get(PBRSG_GLOSSINESSFACTOR);
        return (factor == null) ? 1.0 : factor.doubleValue();
    }

    /**
     * Default is 1.0
     * @param factor between 0.0 and 1.0
     */
    public void setPBRSGGlossinessFactor(double factor) {
        ArgumentChecks.ensureBetween("pbrsg glossiness factor", 0.0, 1.0, factor);
        put(PBRSG_GLOSSINESSFACTOR, factor);
    }

    /**
     * Default is WHITE (1,1,1)
     * @return specular factor
     */
    public Color getPBRSGSpecularFactor() {
        Color color = (Color) get(PBRSG_SPECULARFACTOR);
        return (color == null) ? Color.WHITE : color;
    }

    /**
     * Default is WHITE (1,1,1)
     * @param color specular factor
     */
    public void setPBRSGSpecularFactor(Color color) {
        put(PBRSG_SPECULARFACTOR, color);
    }

    /**
     * Default is null
     * @return specular glossiness texture
     */
    public Texture getPBRSGSpecularGlossinessTexture() {
        return (Texture) get(PBRSG_SPECULAR_GLOSSINESS_TEXTURE);
    }

    /**
     * Default is null
     * @param texture specular glossiness
     */
    public void setPBRSGSpecularGlossinessTexture(Texture texture) {
        put(PBRSG_SPECULAR_GLOSSINESS_TEXTURE, texture);
    }

    /**
     * Default value is false
     * @return true if lights should be disabled for this material.
     */
    public boolean isUnlit() {
        return Boolean.TRUE.equals(get(LIGHTS_UNLIT));
    }

    /**
     * Default value is false
     * @param unlit if lights should be disabled for this material.
     */
    public void setUnlit(boolean unlit) {
        put(LIGHTS_UNLIT, unlit);
    }

    /**
     * Default value is 1.5
     * @return index of refraction
     */
    public double getIOR() {
        Number ac = (Number) get(IOR);
        return (ac == null) ? 1.5 : ac.doubleValue();
    }

    /**
     * Default value is 1.5
     * @param ior
     */
    public void setIOR(double ior) {
        ArgumentChecks.ensureBetween("ior", 1.0, 100.0, ior);
        put(IOR, ior);
    }

    /**
     * Default value is 0.0
     * @return transmission factor
     */
    public double getTransmissionFactor() {
        Number ac = (Number) get(TRANSMISSIONFACTOR);
        return (ac == null) ? 0.0 : ac.doubleValue();
    }

    /**
     * Default value is 0.0
     * @param tf
     */
    public void setTransmissionFactor(double tf) {
        put(TRANSMISSIONFACTOR, tf);
    }

    /**
     * Default is null
     * @return transmission texture
     */
    public Texture getTransmissionTexture() {
        return (Texture) get(TRANSMISSIONTEXTURE);
    }

    /**
     * Default is null
     * @param texture transmission
     */
    public void setTransmissionTexture(Texture texture) {
        put(TRANSMISSIONTEXTURE, texture);
    }

    /**
     * Default value is 0.0
     * @return thickness factor
     */
    public double getThicknessFactor() {
        Number ac = (Number) get(THICKNESSFACTOR);
        return (ac == null) ? 0.0 : ac.doubleValue();
    }

    /**
     * Default value is 0.0
     * @param tf
     */
    public void setThicknessFactor(double tf) {
        put(THICKNESSFACTOR, tf);
    }

    /**
     * Default is null
     * @return thickness texture
     */
    public Texture getThicknessTexture() {
        return (Texture) get(THICKNESSTEXTURE);
    }

    /**
     * Default is null
     * @param texture thickness
     */
    public void setThicknessTexture(Texture texture) {
        put(THICKNESSTEXTURE, texture);
    }

    /**
     * Default value is +Infinity
     * @return volume attenuation distance
     */
    public double getAttenuationDistance() {
        Number ac = (Number) get(ATTENUATIONDISTANCE);
        return (ac == null) ? Double.POSITIVE_INFINITY : ac.doubleValue();
    }

    /**
     * Default value is +Infinity
     * @param ad volume attenuation distance
     */
    public void setAttenuationDistance(double ad) {
        put(ATTENUATIONDISTANCE, ad);
    }

    /**
     * Default is WHITE (1,1,1)
     * @return volume attenuation color
     */
    public Color getAttenuationColor() {
        Color color = (Color) get(ATTENUATIONCOLOR);
        return (color == null) ? Color.WHITE : color;
    }

    /**
     * Default is WHITE (1,1,1)
     * @param color volume attenuation color
     */
    public void setAttenuationColor(Color color) {
        put(ATTENUATIONCOLOR, color);
    }

    public final class Matte extends Material {
        //todo separate parameters to different type of materials
    }

    public final class BlinnPhong extends Material {
        //todo separate parameters to different type of materials
    }

    public final class PhysicallyBased extends Material {
        //todo separate parameters to different type of materials
    }


    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.id);
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
        final Material other = (Material) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return super.equals(obj);
    }

}
