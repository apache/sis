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

import java.util.Arrays;
import java.util.TreeSet;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.Vector;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * An indexed point in the {@code DataPoints}
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Vertex implements Point {

    private final DataPoints parent;
    private final int index;

    public Vertex(DataPoints geometry, long index) {
        this.parent = geometry;
        this.index = Math.toIntExact(index);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Vector getPosition() {
        return parent.getPosition(index);
    }

    /**
     * @return index in the parent mesh.
     */
    public long getIndex() {
        return index;
    }

    @Override
    public Vector getAttribute(String key) {
        return parent.getAttribute(index, key);
    }

    @Override
    public void setAttribute(String name, Vector tuple) {
        parent.setAttribute(index, name, tuple);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return parent.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DataPointsType getDataPointsType() {
        return parent.getType();
    }

    /**
     * Returns the index followed by the values of all attributes, sorted by name.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("V:");
        sb.append(getIndex());
        final TreeSet<String> properties = new TreeSet<>(getDataPointsType().getAttributeNames());
        for (String name : properties) {
            sb.append(' ');
            sb.append(name);
            sb.append(Arrays.toString(getAttribute(name).toArrayDouble()));
        }
        return sb.toString();
    }
}
