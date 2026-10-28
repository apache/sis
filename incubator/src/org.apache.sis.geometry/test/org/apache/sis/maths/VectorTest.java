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
 * Tests for the {@link Vector} static factory methods and the array constructors.
 *
 * @author Johann Sorel (Geomatys)
 */
public class VectorTest {

    private static final double TOLERANCE = 0.0;

    /**
     * The vector class must be selected by the dimension and the primitive type of the arguments.
     */
    @Test
    public void testOfPrimitiveTypes() {
        assertInstanceOf(Vector1D.Byte.class,   Vector.of((byte) 1));
        assertInstanceOf(Vector2D.Short.class,  Vector.of((short) 1, (short) 2));
        assertInstanceOf(Vector3D.Int.class,    Vector.of(1, 2, 3));
        assertInstanceOf(Vector4D.Long.class,   Vector.of(1L, 2L, 3L, 4L));
        assertInstanceOf(Vector2D.Float.class,  Vector.of(1f, 2f));
        assertInstanceOf(Vector3D.Double.class, Vector.of(1.0, 2.0, 3.0));
        assertInstanceOf(Vector1D.Double.class, Vector.of(1.0));
        //mixed types use the widest type
        assertInstanceOf(Vector2D.Long.class,   Vector.of(1, 2L));
        assertInstanceOf(Vector2D.Double.class, Vector.of(1, 2.5));

        assertArrayEquals(new double[] {1, 2, 3, 4}, Vector.of(1.0, 2.0, 3.0, 4.0).toArrayDouble(), TOLERANCE);
        assertArrayEquals(new double[] {1, 2, 3}, Vector.of(1, 2, 3).toArrayDouble(), TOLERANCE);
    }

    /**
     * Factories for any dimension and data type.
     */
    @Test
    public void testOfArray() {
        assertInstanceOf(Vector3D.Double.class, Vector.of(new double[] {1, 2, 3}));
        final Vector<?> nd = Vector.of(1.0, 2.0, 3.0, 4.0, 5.0);
        assertInstanceOf(VectorND.Double.class, nd);
        assertArrayEquals(new double[] {1, 2, 3, 4, 5}, nd.toArrayDouble(), TOLERANCE);

        final Vector<?> unsigned = Vector.of(DataType.USHORT, 40000, 2);
        assertInstanceOf(Vector2D.UShort.class, unsigned);
        assertArrayEquals(new double[] {40000, 2}, unsigned.toArrayDouble(), TOLERANCE);
    }

    /**
     * Array constructors of fixed dimension vectors must reject arrays of another length.
     */
    @Test
    public void testArrayLength() {
        assertArrayEquals(new double[] {1, 2}, new Vector2D.Double(new double[] {1, 2}).toArrayDouble(), TOLERANCE);
        assertThrows(IllegalArgumentException.class, () -> new Vector2D.Double(new double[] {1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> new Vector3D.Double(new double[] {1, 2}));
        assertThrows(IllegalArgumentException.class, () -> new Vector3D.Double(new float[] {1, 2}));
        assertThrows(IllegalArgumentException.class, () -> new Vector4D.Int(new int[] {1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> new Vector1D.UByte(new byte[0]));
    }

    /**
     * Methods with the {@code Accurate} suffix must keep their accuracy when terms nearly cancel,
     * while the plain methods lose all significant digits in the same cases.
     */
    @Test
    public void testAccuracy() {
        //dot product, exact value is 1
        final Vector3D.Double u = Vector.of(1e8, 1.0, -1e8);
        final Vector3D.Double v = Vector.of(1e8, 1.0, 1e8);
        assertEquals(1.0, u.dotAccurate(v), 0.0);
        assertEquals(1.0, Vector.of(1e8, 1.0, -1e8, 0.0, 0.0).dotAccurate(Vector.of(1e8, 1.0, 1e8, 0.0, 0.0)), 0.0);
        assertEquals(1.0, Vectors.unmodifiable(u).dotAccurate(v), 0.0);
        assertEquals(0.0, u.dot(v), 0.0, "plain arithmetic loses all digits");

        //cross product of nearly parallel vectors
        final Vector3D.Double a = Vector.of(0.0, 101394113.74178699, 101394113.80164877);
        final Vector3D.Double b = Vector.of(0.0, 101394123.88119836, 101394123.94106016);
        assertEquals(0.9039268278985468, a.crossAccurate(b).x, 1e-15);
        assertEquals(0.9039268278985468, Vectors.unmodifiable(a).crossAccurate(b).get(0), 1e-15);
        assertEquals(0.0, a.cross(b).x, 0.0, "plain arithmetic loses all digits");
    }

    /**
     * Both linear interpolations are exact at both ends and keep equal vectors unchanged.
     */
    @Test
    public void testLerp() {
        final Vector3D.Double p = Vector.of(0.1, -7.3, 1e-3);
        final Vector3D.Double q = Vector.of(-198.53016738247243, 3.0, 2.0);
        assertEquals(p, p.lerp(q, 0));
        assertEquals(q, p.lerp(q, 1));
        assertEquals(p, p.lerp(p, 0.3));
        assertEquals(p, p.lerpAccurate(q, 0));
        assertEquals(q, p.lerpAccurate(q, 1));
        assertEquals(p, p.lerpAccurate(p, 0.3));

        //same with the default implementation and with VectorND
        assertEquals(q, Vectors.unmodifiable(p).lerp(q, 1));
        final Vector<?> nd = Vector.of(0.1, -7.3, 1e-3, 4.0, 5.0);
        assertEquals(nd, nd.lerp(nd, 0.3));
        assertArrayEquals(new double[] {0.5, 0.5, 0.5, 4.5, 5.0},
                Vector.of(0.0, 0.0, 0.0, 4.0, 5.0).lerp(Vector.of(1.0, 1.0, 1.0, 5.0, 5.0), 0.5).toArrayDouble(), 0.0);
    }
}
