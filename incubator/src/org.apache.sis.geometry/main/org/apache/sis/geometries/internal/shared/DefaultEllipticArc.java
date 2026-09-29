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

import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.EllipticArc;
import org.apache.sis.maths.Array;


/**
 * A conic without a cross term, therefore an arc of ellipse, each arc being determined by four data
 * points instead of the five needed by a general conic.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultEllipticArc extends DefaultConic implements EllipticArc {

    /**
     * Creates an elliptic arc from the given data points and control points.
     *
     * @param  points         points lying on the ellipse. Four of them determine each arc.
     * @param  controlPoints  centers of the ellipses of the arcs, or {@code null} if none.
     * @param  cycle          whether this arc closes on itself, making it a complete ellipse.
     */
    public DefaultEllipticArc(final DataPoints points, final Array controlPoints, final boolean cycle) {
        super(points, controlPoints, cycle);
    }

}
