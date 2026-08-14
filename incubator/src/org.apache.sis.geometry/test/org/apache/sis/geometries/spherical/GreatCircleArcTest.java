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
import org.apache.sis.geometries.math.ReadOnly;
import org.apache.sis.geometries.math.Vector3D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link GreatCircleArc}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GreatCircleArcTest {

    private static final double TOLERANCE = 1e-12;

    private final ReadOnly.Vector<?> a = new Vector3D.Double(1, 0, 0);
    private final ReadOnly.Vector<?> b = new Vector3D.Double(0, 1, 0);

    /**
     * Length test.
     * The arc between (1,0,0) and (0,1,0) spans a quarter of a great circle,
     * so its length is PI/2 * radius.
     */
    @Test
    public void getLengthTest() {
        final GreatCircleArc arc = new GreatCircleArc(new Sphere(3), a, b);
        assertEquals(Math.PI / 2, arc.getLength(), TOLERANCE);

        final GreatCircleArc biggerArc = new GreatCircleArc(new Sphere(3, 2.0), a, b);
        assertEquals(Math.PI, biggerArc.getLength(), TOLERANCE);

        // arc between antipodal points spans half a great circle
        final GreatCircleArc halfArc = new GreatCircleArc(new Sphere(3), a, new Vector3D.Double(-1, 0, 0));
        assertEquals(Math.PI, halfArc.getLength(), TOLERANCE);

        // arc between a point and itself has no length
        final GreatCircleArc emptyArc = new GreatCircleArc(new Sphere(3), a, a);
        assertEquals(0, emptyArc.getLength(), TOLERANCE);
    }

}
