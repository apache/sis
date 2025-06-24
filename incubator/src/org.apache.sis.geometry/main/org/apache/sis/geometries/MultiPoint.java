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

import org.apache.sis.geometries.math.Tuple;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A MultiPoint is a 0-dimensional GeometryCollection.
 * The elements of a MultiPoint are restricted to Points.
 * The Points are not connected or ordered in any semantically important way (see the discussion at GeometryCollection).
 * A MultiPoint is simple if no two Points in the MultiPoint are equal (have identical coordinate values in X and Y).
 * Every MultiPoint is spatially equal under the definition in Clause 6.1.15.3 to a simple Multipoint.
 * The boundary of a MultiPoint is the empty set.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface MultiPoint<T extends Point> extends GeometryCollection<T>{

    public static final String TYPE = "MULTIPOINT";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * View this multipoint as a point sequence
     */
    default PointSequence asPointSequence() {
        return new PointSequence() {
            @Override
            public CoordinateReferenceSystem getCoordinateReferenceSystem() {
                return MultiPoint.this.getCoordinateReferenceSystem();
            }

            @Override
            public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
                MultiPoint.this.setCoordinateReferenceSystem(cs);
            }

            @Override
            public int size() {
                return getNumGeometries();
            }

            @Override
            public Point getPoint(int index) {
                return MultiPoint.this.getGeometryN(index);
            }

            @Override
            public Tuple getPosition(int index) {
                return MultiPoint.this.getGeometryN(index).getPosition();
            }

            @Override
            public void setPosition(int index, Tuple value) {
                MultiPoint.this.getGeometryN(index).getPosition().set(value);
            }

            @Override
            public Tuple getAttribute(int index, String name) {
                return MultiPoint.this.getGeometryN(index).getAttribute(name);
            }

            @Override
            public void setAttribute(int index, String name, Tuple value) {
                MultiPoint.this.getGeometryN(index).setAttribute(name, value);
            }

            @Override
            public AttributesType getAttributesType() {
                return MultiPoint.this.getAttributesType();
            }
        };
    }

    @Override
    default String asText() {        int dimension = getDimension();
        final StringBuilder sb = new StringBuilder(TYPE);
        sb.append('(');
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            T point = getGeometryN(i);
            if (i > 0) sb.append(',');
            AbstractGeometry.toText(sb, point.getPosition());
        }
        sb.append(')');
        return sb.toString();
    }

}
