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
package org.apache.sis.geometries;

import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Base class of the tests of the {@link Geometry} types.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class GeometryTest {
    /**
     * A two-dimensional reference system in degrees. The axes are
     * (<var>longitude</var>, <var>latitude</var>).
     */
    protected static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.normalizedGeographic();

    /**
     * A three-dimensional reference system, the two first axes of which are in degrees
     * and the third one in metres.
     */
    protected static final CoordinateReferenceSystem CRS_3D = CommonCRS.WGS84.geographic3D();

    /**
     * Tolerance threshold on the measurements and on the coordinates.
     * For rounding errors of the floating point arithmetic.
     */
    protected static final double TOLERANCE = 1E-9;

    protected GeometryTest() {
    }

    /**
     * Returns a view of the given {@code DataPoints} declaring the given attribute interpolations.
     * Attributes absent from the map keep the interpolation declared by the {@code DataPoints}.
     *
     * @param  points   the {@code DataPoints} to wrap.
     * @param  methods  interpolation of the attributes, by attribute name.
     * @return a view of the {@code DataPoints} declaring the given interpolations.
     */
    protected static DataPoints withInterpolations(final DataPoints points, final Map<String,AttributeInterpolation> methods) {
        final DataPointsType base = points.getType();
        final DataPointsType type = new DataPointsType() {
            @Override public List<String> getAttributeNames()            {return base.getAttributeNames();}
            @Override public SampleSystem getAttributeSystem(String name) {return base.getAttributeSystem(name);}
            @Override public DataType     getAttributeType(String name)   {return base.getAttributeType(name);}
            @Override public AttributeInterpolation getAttributeInterpolation(String name) {
                return methods.getOrDefault(name, base.getAttributeInterpolation(name));
            }
        };
        return new DataPoints() {
            @Override public CoordinateReferenceSystem getCoordinateReferenceSystem() {return points.getCoordinateReferenceSystem();}
            @Override public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {points.setCoordinateReferenceSystem(crs);}
            @Override public DataPointsType getType() {return type;}
            @Override public int size() {return points.size();}
            @Override public Point getPoint(int index) {return points.getPoint(index);}
            @Override public Vector<?> getPosition(int index) {return points.getPosition(index);}
            @Override public void setPosition(int index, ReadOnly.Vector<?> value) {points.setPosition(index, value);}
            @Override public Vector<?> getAttribute(int index, String name) {return points.getAttribute(index, name);}
            @Override public void setAttribute(int index, String name, ReadOnly.Vector<?> value) {points.setAttribute(index, name, value);}
        };
    }

    /**
     * Asserts that the given position is the expected one.
     *
     * @param x       the expected value of the first ordinate.
     * @param y       the expected value of the second ordinate.
     * @param actual  the position to verify.
     */
    protected static void assertPositionEquals(final double x, final double y, final Point actual) {
        final double[] position = actual.getPosition().toArrayDouble();
        assertEquals(2, position.length, "Unexpected number of dimensions.");
        assertEquals(x, position[0], TOLERANCE);
        assertEquals(y, position[1], TOLERANCE);
    }
}
