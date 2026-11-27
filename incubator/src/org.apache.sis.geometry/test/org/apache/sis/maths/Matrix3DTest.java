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
package org.apache.sis.maths;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link Matrix3D}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Matrix3DTest {

    /**
     * Rotations of ±90° around Y whose value at row 2, column 0 drifted one ulp past ±1,
     * as after repeated multiplications, are in gimbal lock and must not give NaN angles.
     */
    @Test
    public void testToEulerGimbalLockDrift() {
        final Matrix3D up = new Matrix3D(
                 0, 0, 1,
                 0, 1, 0,
                -1.0000000000000002, 0, 0);
        assertArrayEquals(new double[] {0, 1.5707963267948966, 0}, up.toEuler().toArrayDouble(), 0.0);

        final Matrix3D down = new Matrix3D(
                0, 0, -1,
                0, 1,  0,
                1.0000000000000002, 0, 0);
        assertArrayEquals(new double[] {0, -1.5707963267948966, 0}, down.toEuler().toArrayDouble(), 0.0);
    }
}
