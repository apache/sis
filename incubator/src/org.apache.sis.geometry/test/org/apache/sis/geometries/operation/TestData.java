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

import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


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
     * Do not allow instantiation of this class.
     */
    private TestData() {
    }
}
