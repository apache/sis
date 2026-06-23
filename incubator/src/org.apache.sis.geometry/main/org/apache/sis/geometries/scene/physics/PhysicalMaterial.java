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
package org.apache.sis.geometries.scene.physics;


/**
 * Describes how the collider should respond to collisions.
 *
 * @author Johann Sorel
 * @spec ISO_12113 KHR_physics_rigid_bodies extension, Physics Materials
 * @see https://github.com/eoineoineoin/glTF_Physics/tree/master/extensions/2.0/Khronos/KHR_physics_rigid_bodies#physics-materials
 */
public final class PhysicalMaterial {

    public static enum Combine {
        AVERAGE,
        MINIMUM,
        MAXIMUM,
        MULTIPLY
    }

    private double staticFriction;
    private double dynamicFriction;
    private double restitution;
    private Combine frictionCombine;
    private Combine restitutionCombine;

    /**
     * The friction used when an object is laying still on a surface.
     * Typical range from 0 to 1.
     *
     * @return the staticFriction
     */
    public double getStaticFriction() {
        return staticFriction;
    }

    /**
     * @param staticFriction the staticFriction to set
     * @see #getStaticFriction()
     */
    public void setStaticFriction(double staticFriction) {
        this.staticFriction = staticFriction;
    }

    /**
     * The friction used when already moving.
     * Typical range from 0 to 1.
     *
     * @return the dynamicFriction
     */
    public double getDynamicFriction() {
        return dynamicFriction;
    }

    /**
     * @param dynamicFriction the dynamicFriction to set
     * @see #getDynamicFriction()
     */
    public void setDynamicFriction(double dynamicFriction) {
        this.dynamicFriction = dynamicFriction;
    }

    /**
     * Coefficient of restitution.
     * Typical range from 0 to 1.
     *
     * @return the restitution
     */
    public double getRestitution() {
        return restitution;
    }

    /**
     * @param restitution the restitution to set
     * @see #getRestitution()
     */
    public void setRestitution(double restitution) {
        this.restitution = restitution;
    }

    /**
     * How to combine two friction values.
     *
     * @return the frictionCombine
     */
    public Combine getFrictionCombine() {
        return frictionCombine;
    }

    /**
     * @param frictionCombine the frictionCombine to set
     * @see #getFrictionCombine()
     */
    public void setFrictionCombine(Combine frictionCombine) {
        this.frictionCombine = frictionCombine;
    }

    /**
     * How to combine two restitution values.
     *
     * @return the restitutionCombine
     */
    public Combine getRestitutionCombine() {
        return restitutionCombine;
    }

    /**
     * @param restitutionCombine the restitutionCombine to set
     * @see #getRestitutionCombine()
     */
    public void setRestitutionCombine(Combine restitutionCombine) {
        this.restitutionCombine = restitutionCombine;
    }


}
