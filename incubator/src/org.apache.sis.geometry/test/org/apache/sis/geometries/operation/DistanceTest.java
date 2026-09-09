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

import javax.measure.quantity.Length;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DistanceTest {

    private static final SampleSystem CRS2D = SampleSystem.of(CommonCRS.WGS84.geographic());

    /**
     * Test point to point distance.
     */
    @Test
    public void PointPoint() {

        { //different CRS
            final Point point1 = GeometryFactory.createPoint(CommonCRS.WGS84.geographic());
            final Point point2 = GeometryFactory.createPoint(CommonCRS.WGS84.normalizedGeographic());
            try {
                new GeometryProcessor().distance(point1, point2);
                fail("evaluation should fail");
            } catch (OperationException ex) {
                //ok
            }
        }

        { //at same position
            final Point point1 = GeometryFactory.createPoint(CRS2D, 10.0, 5.0);
            final Point point2 = GeometryFactory.createPoint(CRS2D, 10.0, 5.0);
            final Length distance = new GeometryProcessor().distance(point1, point2);
            assertEquals(Units.METRE, distance.getUnit());
            assertEquals(0.0, distance.getValue().doubleValue(), 0.0);
        }

        { //at 1.0 of distance
            final Point point1 = GeometryFactory.createPoint(CRS2D, 10, 5);
            final Point point2 = GeometryFactory.createPoint(CRS2D, 10, 6);
            final Length distance = new GeometryProcessor().distance(point1, point2);
            assertEquals(Units.METRE, distance.getUnit());
            assertEquals(1.0, distance.getValue().doubleValue(), 0.0);
        }
    }

}
