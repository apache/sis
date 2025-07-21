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

import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArrays;
import java.util.Objects;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Default line implementation.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultPoint extends AbstractGeometry implements Point {

    private final PointSequence points;

    /**
     * @param crs geometry coordinate system, not null.
     */
    public DefaultPoint(CoordinateReferenceSystem crs) {
        points = new ArraySequence(TupleArrays.of(SampleSystem.of(crs), DataType.DOUBLE, 1));
    }

    public DefaultPoint(Tuple position) {
        points = new ArraySequence(TupleArrays.of(position.getSampleSystem(), position.getDataType(), 1));
        points.setPosition(0, position);
    }

    public DefaultPoint(CoordinateReferenceSystem crs, double... position) {
        points = new ArraySequence(TupleArrays.of(crs, position));
    }

    public DefaultPoint(SampleSystem ss, double... position) {
        points = new ArraySequence(TupleArrays.of(ss, position));
    }

    public DefaultPoint(PointSequence ps) {
        if (ps.size() != 1) {
            throw new IllegalArgumentException("Point sequence must contain one point");
        }
        points = ps;
    }

    @Override
    public PointSequence asPointSequence() {
        return points;
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        points.setCoordinateReferenceSystem(cs);
    }

    @Override
    public AttributesType getAttributesType() {
        return points.getAttributesType();
    }

    @Override
    public Tuple getAttribute(String name) {
        return points.getAttribute(0, name);
    }

    @Override
    public void setAttribute(String name, Tuple tuple) {
        points.setAttribute(0, name, tuple);
    }

    @Override
    public Tuple getPosition() {
        return points.getPosition(0);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + Objects.hashCode(this.points);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultPoint other = (DefaultPoint) obj;
        if (!Objects.equals(this.points, other.points)) {
            return false;
        }
        return true;
    }
}
