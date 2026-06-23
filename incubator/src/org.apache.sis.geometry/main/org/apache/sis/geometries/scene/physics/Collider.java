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
 * Describes the physical representation of a node's shape for collision detection.
 *
 * @author Johann Sorel
 * @spec ISO_12113 KHR_physics_rigid_bodies extension Collider
 * @see https://github.com/eoineoineoin/glTF_Physics/tree/master/extensions/2.0/Khronos/KHR_physics_rigid_bodies#colliders
 */
public final class Collider {

    private ColliderGeometry geometry;
    private PhysicalMaterial material;
    private CollisionFilter collisionFilter;

    /**
     * An object describing the geometrical representation of this collider.
     *
     * @return the geometry
     */
    public ColliderGeometry getGeometry() {
        return geometry;
    }

    /**
     * @param geometry the geometry to set
     * @see #getGeometry()
     */
    public void setGeometry(ColliderGeometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Describes how the collider should respond to collisions.
     *
     * @return the material
     */
    public PhysicalMaterial getMaterial() {
        return material;
    }

    /**
     * @param material the material to set
     * @see #getMaterial()
     */
    public void setMaterial(PhysicalMaterial material) {
        this.material = material;
    }

    /**
     * Describes a filter which determines if this collider should perform collision detection
     * against another collider.
     *
     * @return the collisionfilter
     */
    public CollisionFilter getCollisionFilter() {
        return collisionFilter;
    }

    /**
     * @param collisionfilter the collisionfilter to set
     * @see #getCollisionFilter()
     */
    public void setCollisionFilter(CollisionFilter collisionfilter) {
        this.collisionFilter = collisionfilter;
    }
}
