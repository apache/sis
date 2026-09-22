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
package org.apache.sis.geometries.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Geometries shared by the test cases of the {@link GeometryProcessor} operations.
 * They are immutable, which allows the test classes to declare them once and to
 * reuse the same instances in every test case.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TestData {
    /**
     * The coordinate reference system of all the geometries declared in this class.
     */
    public static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.geographic();

    /**
     * An empty geometry. Together with {@link #EMPTY_2}, it allows to verify that the result of an
     * operation on the empty set depends on the emptiness of the operands, not on their identity.
     */
    public static final Empty EMPTY_1 = GeometryFactory.createEmpty(CRS_2D);

    /**
     * Another empty geometry, distinct from {@link #EMPTY_1} but equal to it as a set of positions.
     */
    public static final Empty EMPTY_2 = GeometryFactory.createEmpty(CRS_2D);

    /**
     * An arbitrary geometry which is not empty, used as the other operand of the operations
     * tested against the empty set.
     */
    public static final Point NON_EMPTY = GeometryFactory.createPoint(CRS_2D, 10.0, 5.0);

    /**
     * An arbitrary point. Together with {@link #POINT_A_BIS}, it allows to verify that the result
     * of an operation on two points depends on their positions, not on their identity.
     */
    public static final Point POINT_A = GeometryFactory.createPoint(CRS_2D, 10.0, 5.0);

    /**
     * Another point, distinct from {@link #POINT_A} but at the same position.
     */
    public static final Point POINT_A_BIS = GeometryFactory.createPoint(CRS_2D, 10.0, 5.0);

    /**
     * A point at a position different than {@link #POINT_A}.
     */
    public static final Point POINT_B = GeometryFactory.createPoint(CRS_2D, 20.0, 15.0);

    /**
     * The empty geometry expected as the result of an operation which found no position,
     * for example the intersection of {@link #POINT_A} with {@link #POINT_B}.
     * It is a distinct instance from {@link #EMPTY_1} on purpose: an operation builds its
     * result rather than returning an operand when neither operand is empty.
     */
    public static final Empty EMPTY_RESULT = GeometryFactory.createEmpty(CRS_2D);

    /**
     * Do not allow instantiation of this class.
     */
    private TestData() {
    }

    /**
     * Asserts that the given collection contains exactly the positions of the given points,
     * in any order. This is used for the results of the operations which are specified as a
     * set of positions, the order of which is left to the implementation.
     *
     * @param  actual    the collection of points to verify.
     * @param  expected  the points which shall be in the given collection, in any order.
     */
    public static void assertPositionsEqual(final MultiPoint<?> actual, final Point... expected) {
        final List<String> remaining = new ArrayList<>(expected.length);
        for (final Point point : expected) {
            remaining.add(Arrays.toString(point.getPosition().toArrayDouble()));
        }
        for (int i = 0; i < actual.getNumGeometries(); i++) {
            final String position = Arrays.toString(actual.getGeometryN(i).getPosition().toArrayDouble());
            assertTrue(remaining.remove(position), () -> "Unexpected position " + position + '.');
        }
        assertTrue(remaining.isEmpty(), () -> "Missing positions " + remaining + '.');
    }
}
