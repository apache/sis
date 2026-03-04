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
package org.apache.sis.geometries.math;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * Original code from Unlicense.science
 *
 * @author Johann Sorel
 */
public class QuaternionTest {

    public static final double DELTA = 0.00000001;

    @Test
    public void testToMatrix3(){

        //test identity
        Quaternion qt = new Quaternion();
        Matrix3D result = qt.toMatrix3();
        assertEquals(new Matrix3D( 1,  0,  0,
                             0,  1,  0,
                             0,  0,  1),
                result);


        qt = new Quaternion(1, 0, 0, 0);
        result = qt.toMatrix3();
        assertEquals(new Matrix3D( 1,  0,  0,
                             0, -1,  0,
                             0,  0, -1),
                result);

        qt = new Quaternion(0, 1, 0, 0);
        result = qt.toMatrix3();
        assertEquals(new Matrix3D(-1,  0,  0,
                             0,  1,  0,
                             0,  0, -1),
                result);

        qt = new Quaternion(0, 0, 1, 0);
        result = qt.toMatrix3();
        assertEquals(new Matrix3D(-1,  0,  0,
                             0, -1,  0,
                             0,  0,  1),
                result);
    }

    @Test
    public void testFromMatrix(){
        final double angle = 12.37;

        final Matrix4D m = new Matrix4D(new double[][]{
            {1,    0,                  0,                  0},
            {0,    Math.cos(angle),    -Math.sin(angle),   0},
            {0,    Math.sin(angle),    Math.cos(angle),    0},
            {0,    0,                  0,                  1}
            });

        Quaternion q = new Quaternion();
        q.setFromMatrix(m);

        assertArrayEquals(m.toArrayDoubleColOrder(),q.toMatrix4().toArrayDoubleColOrder(), DELTA);
    }

    @Test
    public void testFromEuler(){

        Vector euler = new Vector3D.Double(0, 0, 0);
        Quaternion q = new Quaternion().setFromEuler(euler);
        assertArrayEquals(new double[]{0, 0, 0, 1}, q.values, DELTA);

        euler = new Vector3D.Double(Math.PI, 0, 0);
        q = new Quaternion().setFromEuler(euler);
        assertArrayEquals(new double[]{0, 0, 1, 0}, q.values, DELTA);

        euler = new Vector3D.Double(0, Math.PI / 2.0, 0);
        q = new Quaternion().setFromEuler(euler);
        assertArrayEquals(new double[]{0, Math.cos(Math.PI / 4.0), 0, Math.cos(Math.PI / 4.0)}, q.values, DELTA);
        euler = q.toEuler();
        assertArrayEquals(new double[]{0, Math.PI / 2.0, 0}, euler.toArrayDouble(), DELTA);

        euler = new Vector3D.Double(0, 0, Math.PI);
        q = new Quaternion().setFromEuler(euler);
        assertArrayEquals(new double[]{1, 0, 0, 0}, q.values, DELTA);

    }

    @Test
    public void testToEuler(){

        Quaternion q = new Quaternion(0, 0, 0, 1);
        Vector<?> euler = q.toEuler();
        assertArrayEquals(new double[]{0, 0, 0}, euler.toArrayDouble(), DELTA);

        q = new Quaternion(0, 0, 1, 0);
        euler = q.toEuler();
        assertArrayEquals(new double[]{Math.PI, 0, 0}, euler.toArrayDouble(), DELTA);

        q = new Quaternion(1, 0, 0, 0);
        euler = q.toEuler();
        assertArrayEquals(new double[]{0, 0, Math.PI}, euler.toArrayDouble(), DELTA);
    }


}
