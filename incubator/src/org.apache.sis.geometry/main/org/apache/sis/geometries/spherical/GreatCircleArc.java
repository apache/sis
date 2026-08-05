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
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.ReadOnly;

/**
 * A great circle arc on a sphere.
 *
 * @see https://mathworld.wolfram.com/GreatCircle.html
 * @author Johann Sorel (Geomatys)
 */
public final class GreatCircleArc {

    /**
     * base sphere.
     */
    private final Sphere sphere;
    /**
     * first arc point, as a unit direction vector from the sphere center
     */
    private final ReadOnly.Vector<?> vecA;
    /**
     * second arc point, as a unit direction vector from the sphere center
     */
    private final ReadOnly.Vector<?> vecB;

    public GreatCircleArc(Sphere sphere, ReadOnly.Vector<?> vecA, ReadOnly.Vector<?> vecB) {
        this.sphere = sphere;
        this.vecA = vecA;
        this.vecB = vecB;
    }

    /**
     * @return the base sphere
     */
    public Sphere getSphere() {
        return sphere;
    }

    /**
     * @return first arc point, as a unit direction vector from the sphere center
     */
    public ReadOnly.Vector<?> getA() {
        return vecA;
    }

    /**
     * @return second arc point, as a unit direction vector from the sphere center
     */
    public ReadOnly.Vector<?> getB() {
        return vecB;
    }

    /**
     * @return arc length, using the sphere radius
     */
    public double getLength() {
        final double cosAngle = Maths.clamp(vecA.dot(vecB),-1,1);
        return sphere.getRadius() * Math.acos(cosAngle);
    }

}
