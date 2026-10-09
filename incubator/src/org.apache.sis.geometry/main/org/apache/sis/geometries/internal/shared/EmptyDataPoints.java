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
package org.apache.sis.geometries.internal.shared;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;


/**
 * A sequence holding no position. This is what a geometry which has no position of its own
 * returns, so that asking any geometry for its positions answers an empty sequence rather
 * than failing.
 *
 * <p>Every accessor reports the sequence as empty: there is no index to read from or to write
 * to, so this class is modifiable in the same way an empty list is.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class EmptyDataPoints implements DataPoints {

    private final CoordinateReferenceSystem crs;
    private final DataPointsType type;

    /**
     * Creates an empty sequence described by the given attributes.
     *
     * @param  crs   the reference system the absent positions would be expressed in, not null.
     * @param  type  the attributes the absent positions would carry, not null.
     */
    public EmptyDataPoints(final CoordinateReferenceSystem crs, final DataPointsType type) {
        this.crs = crs;
        this.type = type;
    }

    /**
     * Creates an empty sequence in the given reference system, carrying no attribute.
     *
     * @param  crs  the reference system the absent positions would be expressed in, not null.
     */
    public EmptyDataPoints(final CoordinateReferenceSystem crs) {
        this(crs, DataPointsType.EMPTY);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("The reference system of an empty sequence cannot be changed.");
    }

    @Override
    public DataPointsType getType() {
        return type;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Point getPoint(int index) {
        throw new IndexOutOfBoundsException("This sequence holds no position.");
    }

    @Override
    public Vector<?> getPosition(int index) {
        throw new IndexOutOfBoundsException("This sequence holds no position.");
    }

    @Override
    public void setPosition(int index, ReadOnly.Vector<?> value) {
        throw new IndexOutOfBoundsException("This sequence holds no position.");
    }

    @Override
    public Vector<?> getAttribute(int index, String name) {
        throw new IndexOutOfBoundsException("This sequence holds no position.");
    }

    @Override
    public void setAttribute(int index, String name, ReadOnly.Vector<?> value) {
        throw new IndexOutOfBoundsException("This sequence holds no position.");
    }

    @Override
    public int hashCode() {
        return DataPoints.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return DataPoints.equals(this, obj);
    }
}
