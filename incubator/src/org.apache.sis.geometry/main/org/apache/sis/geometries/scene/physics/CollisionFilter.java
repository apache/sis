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

import java.util.Set;


/**
 * Describes a filter which determines if this collider should perform collision detection
 * against another collider.
 *
 * @author Johann Sorel
 * @spec ISO_12113 KHR_physics_rigid_bodies extension, Collision Filtering
 * @see https://github.com/eoineoineoin/glTF_Physics/tree/master/extensions/2.0/Khronos/KHR_physics_rigid_bodies#collision-filtering
 */
public final class CollisionFilter {

    private Set<String> collisionSystems;
    private Set<String> notCollideWithSystems;
    private Set<String> collideWithSystems;

    /**
     * An array of arbitrary strings indicating the "system" a node is a member of.
     *
     * @return the collisionSystems
     */
    public Set<String> getCollisionSystems() {
        return collisionSystems;
    }

    /**
     * @param collisionSystems the collisionSystems to set
     * @see #getCollisionSystems()
     */
    public void setCollisionSystems(Set<String> collisionSystems) {
        this.collisionSystems = collisionSystems;
    }

    /**
     * An array of strings representing the systems which this node can not collide with
     *
     * @return the notCollideWithSystems
     */
    public Set<String> getNotCollideWithSystems() {
        return notCollideWithSystems;
    }

    /**
     * @param notCollideWithSystems the notCollideWithSystems to set
     * @see #getNotCollideWithSystems()
     */
    public void setNotCollideWithSystems(Set<String> notCollideWithSystems) {
        this.notCollideWithSystems = notCollideWithSystems;
    }

    /**
     * An array of strings representing the systems which this node can collide with
     *
     * @return the collideWithSystems
     */
    public Set<String> getCollideWithSystems() {
        return collideWithSystems;
    }

    /**
     * @param collideWithSystems the collideWithSystems to set
     * @see #getCollideWithSystems()
     */
    public void setCollideWithSystems(Set<String> collideWithSystems) {
        this.collideWithSystems = collideWithSystems;
    }
}
