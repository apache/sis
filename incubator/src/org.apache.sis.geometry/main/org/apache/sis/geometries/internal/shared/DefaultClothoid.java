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
package org.apache.sis.geometries.internal.shared;

import java.util.List;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.Clothoid;
import org.apache.sis.geometries.curve.RealFunction;
import org.apache.sis.maths.Vector;


/**
 * A spiral whose curvature varies linearly with arc length, also called a Cornu spiral.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultClothoid extends DefaultSpiral implements Clothoid {

    /**
     * Creates a clothoid from its curvature and the frame at its start point.
     *
     * @param  points      points of the clothoid, the first one being its start point.
     * @param  curvature   curvature as a function of arc length. It shall be linear in the arc
     *                     length measured from the point where the infinite clothoid has zero
     *                     curvature.
     * @param  startFrame  two mutually orthogonal unit vectors forming a right-handed frame at the
     *                     start point, a clothoid being planar.
     */
    public DefaultClothoid(final DataPoints points, final RealFunction curvature, final List<Vector> startFrame) {
        super(points, curvature, null, startFrame);
    }

    /**
     * Returns {@code null}: a clothoid is a planar spiral.
     */
    @Override
    public RealFunction getTorsion() {
        return null;
    }

}
