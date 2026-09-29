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
import org.apache.sis.geometries.curve.Arc;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;


/**
 * A chain of circular arcs, each of them centred on a control point and joining two consecutive
 * data points.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultArc extends DefaultConic implements Arc {

    /**
     * Radius vectors giving, together with the two end points, the plane and the rotation direction
     * of each arc.
     */
    protected final List<Vector> radius;

    /**
     * Creates a chain of circular arcs.
     *
     * @param  points         start and end points of the arcs, two consecutive arcs sharing a point.
     *                        There is one more point than there are arcs.
     * @param  controlPoints  centres of the circles carrying the arcs, one per arc.
     * @param  radius         radius vectors of the arcs, possibly empty.
     * @param  cycle          whether this chain closes on itself.
     */
    public DefaultArc(final DataPoints points, final Array controlPoints,
            final List<Vector> radius, final boolean cycle)
    {
        super(points, controlPoints, cycle);
        this.radius = (radius == null) ? List.of() : List.copyOf(radius);
    }

    /**
     * Returns the number of arcs in this chain, which is one less than the number of data points.
     */
    @Override
    public int getNumArc() {
        final int size = points.size();
        return (size == 0) ? 0 : size - 1;
    }

    @Override
    public List<Vector> getRadius() {
        return radius;
    }

}
