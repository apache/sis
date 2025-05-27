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
import java.util.Arrays;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Point sequence backed by a list of Point.
 * @author Johann Sorel (Geomatys)
 */
public final class DefaultPointSequence implements PointSequence {

    private final List<Point> points;

    /**
     * @param points list will be used directly.
     */
    public DefaultPointSequence(List<Point> points) {
        ArgumentChecks.ensureNonEmpty("points", points);
        this.points = points;
    }

    public DefaultPointSequence(Point... points) {
        this(Arrays.asList(points));
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.get(0).getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public AttributesType getAttributesType() {
        return points.get(0).getAttributesType();
    }

    @Override
    public int size() {
        return points.size();
    }

    @Override
    public Point getPoint(int index) {
        return points.get(index);
    }

    @Override
    public Tuple getPosition(int index) {
        return points.get(index).getPosition();
    }

    @Override
    public void setPosition(int index, Tuple value) {
        points.get(index).getPosition().set(value);
    }

    @Override
    public Tuple getAttribute(int index, String name) {
        return points.get(index).getAttribute(name);
    }

    @Override
    public void setAttribute(int index, String name, Tuple value) {
        points.get(index).setAttribute(name, value);
    }

}
