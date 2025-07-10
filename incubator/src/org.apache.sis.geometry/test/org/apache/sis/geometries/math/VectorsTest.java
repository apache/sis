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

import org.apache.sis.geometry.GeneralEnvelope;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class VectorsTest {

    @Test
    public void octEncodingTest(){

        Vector3D.Float normal = new Vector3D.Float(1,0,0);
        byte[] bytes = Vectors.toOctByte(normal);
        Vector3D.Float res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,1,0);
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,0,1);
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0.5f,0,0.5f);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(-1,0,0);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,-1,0);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,0,-1);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,-0.5f,0.5f);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .02f);
    }

    @Test
    public void quantizeTransformTupleTest() throws MismatchedDimensionException, TransformException {

        final GeneralEnvelope quantizeBox = new GeneralEnvelope(3);
        quantizeBox.setRange(0, 0, 100);
        quantizeBox.setRange(1, 100, 300);
        quantizeBox.setRange(2, -300, -100);

        final Vector3D.Double coord1 = new Vector3D.Double(0,100,-300);
        final Vector3D.Double coord2 = new Vector3D.Double(100,300,-100);

        final Tuple dp1 = Vectors.toQuantizedEncoding(coord1, quantizeBox, 32767, null);
        final Tuple dp2 = Vectors.toQuantizedEncoding(coord2, quantizeBox, 32767, null);

        assertEquals(new Vector3D.Double(0, 0, 0), dp1);
        assertEquals(new Vector3D.Double(32767, 32767, 32767), dp2);
        assertEquals(DataType.USHORT, dp1.getDataType());
        assertEquals(DataType.USHORT, dp2.getDataType());

    }

    private static final double DELTA = 1e-8;

    @Test
    public void quantizeTransformTest() throws MismatchedDimensionException, TransformException {

        final GeneralEnvelope quantizeBox = new GeneralEnvelope(3);
        quantizeBox.setRange(0, 0, 100);
        quantizeBox.setRange(1, 100, 300);
        quantizeBox.setRange(2, -300, -100);

        MathTransform trs = Vectors.quantizedTransform(quantizeBox, 32767);

        final Vector3D.Double coord1 = new Vector3D.Double(0,100,-300);
        final Vector3D.Double coord2 = new Vector3D.Double(100,300,-100);

        coord1.transform(trs);
        coord2.transform(trs);

        assertArrayEquals(new double[] {0, 0, 0}, coord1.toArrayDouble(), DELTA);
        assertArrayEquals(new double[] {32767, 32767, 32767}, coord2.toArrayDouble(), DELTA);

    }
}
