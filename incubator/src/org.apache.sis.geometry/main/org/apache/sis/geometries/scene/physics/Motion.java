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

import org.apache.sis.geometries.math.Quaternion;
import org.apache.sis.geometries.math.Vector3D;

/**
 * Allows the simulation to move this node, describing parameters for that motion.
 *
 * @author Johann Sorel
 * @spec ISO_12113 KHR_physics_rigid_bodies extension Motion
 * @see https://github.com/eoineoineoin/glTF_Physics/tree/master/extensions/2.0/Khronos/KHR_physics_rigid_bodies#motions
 */
public final class Motion {

    private boolean isKinematic = false;
    private double mass = 1.0;
    private Vector3D.Double centerOfMass = new Vector3D.Double();
    private Vector3D.Double inertialDiagonal = new Vector3D.Double();
    private Quaternion inertiaOrientation = new Quaternion();
    private Vector3D.Double linearVelocity = new Vector3D.Double();
    private Vector3D.Double angularVelocity = new Vector3D.Double();
    private double gravityFactor = 1.0;

    /**
     * When true, treat the rigid body as having infinite mass.
     * Its velocity will be constant during simulation.
     *
     * @return the isKinematic
     */
    public boolean isIsKinematic() {
        return isKinematic;
    }

    /**
     * @param isKinematic the isKinematic to set
     * @see #isKinematic()
     */
    public void setIsKinematic(boolean isKinematic) {
        this.isKinematic = isKinematic;
    }

    /**
     * The mass of the rigid body. Larger values imply the rigid body is harder to move.
     * In Kilograms (kg).
     *
     * @return the mass
     */
    public double getMass() {
        return mass;
    }

    /**
     * @param mass the mass to set
     * @see #getMass()
     */
    public void setMass(double mass) {
        this.mass = mass;
    }

    /**
     * Center of mass of the rigid body in node space.
     *
     * @return the centerOfMass
     */
    public Vector3D.Double getCenterOfMass() {
        return centerOfMass;
    }

    /**
     * @param centerOfMass the centerOfMass to set
     * @see #getCenterOfMass()
     */
    public void setCenterOfMass(Vector3D.Double centerOfMass) {
        this.centerOfMass = centerOfMass;
    }

    /**
     * The principal moments of inertia. Larger values imply the rigid body is harder to rotate.
     * In Kilogram meter squared (kg·m2).
     *
     * @return the inertialDiagonal
     */
    public Vector3D.Double getInertialDiagonal() {
        return inertialDiagonal;
    }

    /**
     * @param inertialDiagonal the inertialDiagonal to set
     * @see #getInertialDiagonal()
     */
    public void setInertialDiagonal(Vector3D.Double inertialDiagonal) {
        this.inertialDiagonal = inertialDiagonal;
    }

    /**
     * The quaternion rotating from inertia major axis space to node space.
     *
     * @return the inertiaOrientation
     */
    public Quaternion getInertiaOrientation() {
        return inertiaOrientation;
    }

    /**
     * @param inertiaOrientation the inertiaOrientation to set
     * @see #getInertiaOrientation()
     */
    public void setInertiaOrientation(Quaternion inertiaOrientation) {
        this.inertiaOrientation = inertiaOrientation;
    }

    /**
     * Initial linear velocity of the rigid body in node space.
     * In Meter per second (m·s-1).
     *
     * @return the linearVelocity
     */
    public Vector3D.Double getLinearVelocity() {
        return linearVelocity;
    }

    /**
     * @param linearVelocity the linearVelocity to set
     * @see #getLinearVelocity()
     */
    public void setLinearVelocity(Vector3D.Double linearVelocity) {
        this.linearVelocity = linearVelocity;
    }

    /**
     * Initial angular velocity of the rigid body in node space.
     * In Radian per second (rad·s-1).
     *
     * @return the angularVelocity
     */
    public Vector3D.Double getAngularVelocity() {
        return angularVelocity;
    }

    /**
     * @param angularVelocity the angularVelocity to set
     * @see #getAngularVelocity()
     */
    public void setAngularVelocity(Vector3D.Double angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    /**
     * Scalar value used to modify the effect of gravity on this motion
     *
     * @return the gravityFactor
     */
    public double getGravityFactor() {
        return gravityFactor;
    }

    /**
     * @param gravityFactor the gravityFactor to set
     * @see #getGravityFactor()
     */
    public void setGravityFactor(double gravityFactor) {
        this.gravityFactor = gravityFactor;
    }
}
