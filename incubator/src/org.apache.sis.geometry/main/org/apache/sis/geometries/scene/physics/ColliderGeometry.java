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

import org.apache.sis.geometries.Geometry;


/**
 *
 * @author Johann Sorel
 * @spec ISO_12113 KHR_physics_rigid_bodies extension Geometry
 * @see https://github.com/eoineoineoin/glTF_Physics/tree/master/extensions/2.0/Khronos/KHR_physics_rigid_bodies#geometry
 */
public final class ColliderGeometry {

    private Geometry shape;
    private boolean convexHull;

    /**
     * Providing an implicit representation of the geometry.
     *
     * @return the shape
     */
    public Geometry getShape() {
        return shape;
    }

    /**
     * @param shape the shape to set
     */
    public void setShape(Geometry shape) {
        this.shape = shape;
    }

    /**
     * Flag to indicate that the geometry should be a convex hull.
     *
     * @return the convexHull
     */
    public boolean isConvexHull() {
        return convexHull;
    }

    /**
     * @param convexHull the convexHull to set
     */
    public void setConvexHull(boolean convexHull) {
        this.convexHull = convexHull;
    }


}
