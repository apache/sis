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
package org.apache.sis.geometries.privy;

import java.util.HashMap;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.math.Vector2D;
import org.apache.sis.geometries.math.Vector3D;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ArraySequenceTest {

    /**
     * Failing test, positions must have a crs.
     */
    @Test
    public void constructorNoCrsTest() {
        final TupleArray positions = TupleArrays.of(2, new double[]{0,0, 1,0, 0,1, 0,0});
        assertThrows(NullPointerException.class, ()->{
            ArraySequence array = new ArraySequence(positions);
        });
    }

    /**
     * Valid construction, positions have a crs.
     */
    @Test
    public void constructorCrsTest() {
        final TupleArray positions = TupleArrays.of(CommonCRS.WGS84.normalizedGeographic(), new double[]{0,0, 1,0, 0,1, 0,0});
        ArraySequence array = new ArraySequence(positions);
    }

    /**
     * Failing test, no positions.
     */
    @Test
    public void constructorAttributesNoPositionTest() {
        final TupleArray normals = TupleArrays.of(3, new double[]{1,0,0, 0,1,0, 0,0,1, 1,0,0});
        final Map<String,TupleArray> attributes = new HashMap<>();
        attributes.put(AttributesType.ATT_NORMAL, normals);
        assertThrows(NullPointerException.class, ()->{
            ArraySequence array = new ArraySequence(attributes);
        });
    }

    /**
     * Valid construction, positions have a crs.
     */
    @Test
    public void constructorAttributesTest() {
        final TupleArray positions = TupleArrays.of(CommonCRS.WGS84.normalizedGeographic(), new double[]{0,0, 1,0, 0,1, 0,0});
        final TupleArray normals = TupleArrays.of(3, new double[]{1,0,0, 0,1,0, 0,0,1, 1,0,0});
        final Map<String,TupleArray> attributes = new HashMap<>();
        attributes.put(AttributesType.ATT_POSITION, positions);
        attributes.put(AttributesType.ATT_NORMAL, normals);
        ArraySequence array = new ArraySequence(attributes);
    }

    /**
     * Test point attributes.
     */
    @Test
    public void pointsTest() {

        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        final TupleArray positions = TupleArrays.of(crs, new double[]{0,0, 1,0, 0,1, 0,0});
        final TupleArray normals = TupleArrays.of(3, new double[]{1,0,0, 0,1,0, 0,0,1, 1,0,0});
        final Map<String,TupleArray> attributes = new HashMap<>();
        attributes.put(AttributesType.ATT_POSITION, positions);
        attributes.put(AttributesType.ATT_NORMAL, normals);
        final ArraySequence array = new ArraySequence(attributes);

        assertEquals(CommonCRS.WGS84.normalizedGeographic(), array.getCoordinateReferenceSystem());
        assertEquals(2, array.getDimension());
        assertEquals(positions, array.getAttribute(AttributesType.ATT_POSITION));
        assertEquals(normals, array.getAttribute(AttributesType.ATT_NORMAL));
        assertEquals(2, array.getAttributeNames().size());

        final SampleSystem ss = SampleSystem.of(crs);
        assertEquals(new Vector2D.Double(ss, 0, 0), array.getAttribute(0, AttributesType.ATT_POSITION));
        assertEquals(new Vector2D.Double(ss, 1, 0), array.getAttribute(1, AttributesType.ATT_POSITION));
        assertEquals(new Vector2D.Double(ss, 0, 1), array.getAttribute(2, AttributesType.ATT_POSITION));
        assertEquals(new Vector2D.Double(ss, 0, 0), array.getAttribute(3, AttributesType.ATT_POSITION));
        assertEquals(new Vector2D.Double(ss, 0, 0), array.getPosition(0));
        assertEquals(new Vector2D.Double(ss, 1, 0), array.getPosition(1));
        assertEquals(new Vector2D.Double(ss, 0, 1), array.getPosition(2));
        assertEquals(new Vector2D.Double(ss, 0, 0), array.getPosition(3));
        assertEquals(new Vector3D.Double(1, 0, 0), array.getAttribute(0, AttributesType.ATT_NORMAL));
        assertEquals(new Vector3D.Double(0, 1, 0), array.getAttribute(1, AttributesType.ATT_NORMAL));
        assertEquals(new Vector3D.Double(0, 0, 1), array.getAttribute(2, AttributesType.ATT_NORMAL));
        assertEquals(new Vector3D.Double(1, 0, 0), array.getAttribute(3, AttributesType.ATT_NORMAL));

        Point pt0 = array.getPoint(0);
        Point pt1 = array.getPoint(1);
        Point pt2 = array.getPoint(2);
        Point pt3 = array.getPoint(3);
        assertEquals(new Vector2D.Double(ss, 0, 0), pt0.getPosition());
        assertEquals(new Vector2D.Double(ss, 1, 0), pt1.getPosition());
        assertEquals(new Vector2D.Double(ss, 0, 1), pt2.getPosition());
        assertEquals(new Vector2D.Double(ss, 0, 0), pt3.getPosition());
        assertEquals(new Vector3D.Double(1, 0, 0), pt0.getAttribute(AttributesType.ATT_NORMAL));
        assertEquals(new Vector3D.Double(0, 1, 0), pt1.getAttribute(AttributesType.ATT_NORMAL));
        assertEquals(new Vector3D.Double(0, 0, 1), pt2.getAttribute(AttributesType.ATT_NORMAL));
        assertEquals(new Vector3D.Double(1, 0, 0), pt3.getAttribute(AttributesType.ATT_NORMAL));

    }

}
