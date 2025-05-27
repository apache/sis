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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TupleArraysTest {

    /**
     * Test integer type packing.
     */
    @Test
    public void testPackIntegerDataType() {

        final int[] datas = new int[]{0,1,2,3};
        TupleArray array = TupleArrays.of(2, datas);

        //test ubyte packing
        TupleArray packed = TupleArrays.packIntegerDataType(array);
        assertEquals(DataType.UBYTE, packed.getDataType());
        assertArrayEquals(datas, packed.toArrayInt());

        //test byte packing
        datas[1] = -2;
        packed = TupleArrays.packIntegerDataType(array);
        assertEquals(DataType.BYTE, packed.getDataType());
        assertArrayEquals(datas, packed.toArrayInt());

        //test short packing
        datas[1] = -1000;
        packed = TupleArrays.packIntegerDataType(array);
        assertEquals(DataType.SHORT, packed.getDataType());
        assertArrayEquals(datas, packed.toArrayInt());

        //test ushort packing
        datas[1] = 10000;
        packed = TupleArrays.packIntegerDataType(array);
        assertEquals(DataType.USHORT, packed.getDataType());
        assertArrayEquals(datas, packed.toArrayInt());

        //test uint packaging
        datas[1] = 65535 + 20;
        packed = TupleArrays.packIntegerDataType(array);
        assertEquals(DataType.UINT, packed.getDataType());
        assertArrayEquals(datas, packed.toArrayInt());

        //test no change
        datas[1] = - (65535 + 20);
        packed = TupleArrays.packIntegerDataType(array);
        assertEquals(DataType.INT, packed.getDataType());
        assertArrayEquals(datas, packed.toArrayInt());
    }

    /**
     * Test array grouping.
     */
    @Test
    public void testGroup() throws FactoryException {

        final TupleArray array1 = TupleArrays.of(1, new double[]{ 0, 1, 2, 3});
        final TupleArray array2 = TupleArrays.of(2, new double[]{10,11,12,13,14,15,16,17});
        final TupleArray array3 = TupleArrays.of(1, new double[]{20,21,22,23});

        final TupleArray group = TupleArrays.group(array1, array2, array3);
        assertEquals(null, group.getCoordinateReferenceSystem());
        assertEquals(4, group.getSampleSystem().getSize());
        assertEquals(4, group.getLength());

        assertArrayEquals(new double[]{
            0,10,11,20,
            1,12,13,21,
            2,14,15,22,
            3,16,17,23
            },
            group.toArrayDouble(), 0.0);
    }

    /**
     * Test array group size different
     */
    @Test
    public void testGroupWrongSize() throws FactoryException {
        final TupleArray array1 = TupleArrays.of(1, new double[]{ 0, 1, 2, 3});
        final TupleArray array2 = TupleArrays.of(2, new double[]{10,11,12,13,14,15});

        assertThrows(IllegalArgumentException.class, ()->{
            final TupleArray group = TupleArrays.group(array1, array2);
        });
    }

    /**
     * Test stream over a TupleArray
     */
    @Test
    public void testStream() {

        final double[] arr = new double[100];
        for(int i=0;i<100;i++) {
            arr[i] = i;
        }
        final TupleArray array = TupleArrays.of(SampleSystem.ofSize(1), arr);

        final Set<Tuple> distinct = Collections.synchronizedSet(new HashSet<>());
        Stream<Tuple> stream = array.stream(true);
        stream.forEach((Tuple t) -> {
            distinct.add(new Vector1D.Double(t));
        });

        assertEquals(100, distinct.size());
    }
}
