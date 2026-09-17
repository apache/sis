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
import org.apache.sis.geometries.curve.Circle;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;


/**
 * A complete circle, i.e. a chain of circular arcs sharing a single centre and closing on itself.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultCircle extends DefaultArc implements Circle {

    /**
     * Creates a complete circle.
     *
     * @param  points         points of the circle, all at the same distance from the centre,
     *                        the first and the last ones being equal.
     * @param  controlPoints  the centre of the circle, repeated once per arc. Because a single arc
     *                        must stay below a full turn, at least two of them are needed.
     * @param  radius         radius vectors of the arcs, possibly empty.
     */
    public DefaultCircle(final DataPoints points, final Array controlPoints, final List<Vector> radius) {
        super(points, controlPoints, radius, true);
    }

    /**
     * Returns {@code true}: a circle is a complete curve closing on itself.
     */
    @Override
    public boolean isCycle() {
        return true;
    }

}
