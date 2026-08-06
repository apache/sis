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
package org.apache.sis.geometries.spherical;

import org.apache.sis.geometries.Sphere;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.ReadOnly;
import org.apache.sis.geometries.math.Vector3D;

/**
 * A polygon on a sphere.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SphericalConvexPolygon {

    /**
     * base sphere.
     */
    private final Sphere sphere;
    /**
     * Serie of points as a unit direction vector from the sphere center.
     * First point equels last point
     */
    private final Array points;

    public SphericalConvexPolygon(Sphere sphere, Array points) {
        this.sphere = sphere;
        this.points = points;
    }

    /**
     * @return the base sphere
     */
    public Sphere getSphere() {
        return sphere;
    }

    /**
     * Test if given vector is contained in this polygon with default epsilon -1e-9.
     *
     * @param vecP vector to test
     * @return true if vector is inside triangle
     */
    public boolean contains(ReadOnly.Vector<?> vecP) {
        return contains(vecP, -1e-9);
    }

    /**
     * Test if given vector is contained in this polygon.
     *
     * @param vecP vector to test
     * @param epsilon edge tolerance, should be a negative value close to zero
     * @return true if vector is inside the polygon
     */
    public boolean contains(ReadOnly.Vector<?> vecP, double epsilon) {
        final Vector3D.Double va = new Vector3D.Double();
        final Vector3D.Double vb = new Vector3D.Double();

        for (long a = 0, n = points.getLength()-1; a < n; a++) {
            points.get(a, va);
            points.get(a + 1, vb);
            if (vecP.dot(va.cross(vb)) < epsilon) {
                return false;
            }
        }
        return true;
    }


}
