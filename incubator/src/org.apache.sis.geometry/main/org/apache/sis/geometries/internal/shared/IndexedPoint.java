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

import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.Tuple;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A point viewing a single index of a DataPoints.
 * All operations are delegated to the parent DataPoints, this class holds no* coordinate of its own. 
 * The coordinate reference system cannot be modified through this view.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class IndexedPoint implements Point {

    private final DataPoints parent;
    private final int index;

    public IndexedPoint(DataPoints parent, int index) {
        this.parent = parent;
        this.index = index;
    }

    /**
     * @return index in the parent point sequence.
     */
    public int getIndex() {
        return index;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Tuple getPosition() {
        return parent.getPosition(index);
    }

    @Override
    public Tuple getAttribute(String name) {
        return parent.getAttribute(index, name);
    }

    @Override
    public void setAttribute(String name, Tuple tuple) {
        parent.setAttribute(index, name, tuple);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return parent.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public AttributesType getAttributesType() {
        return parent.getAttributesType();
    }
}
