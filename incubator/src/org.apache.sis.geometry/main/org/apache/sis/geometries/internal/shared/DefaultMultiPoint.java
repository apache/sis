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

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultMultiPoint extends AbstractGeometry implements MultiPoint<Point> {

    private final PointSequence points;

    public DefaultMultiPoint(PointSequence points) {
        this.points = points;
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
    public Envelope getEnvelope() {
        return points.getEnvelope();
    }

    @Override
    public int getNumGeometries() {
        return points.size();
    }

    @Override
    public Point getGeometryN(int n) {
        return points.getPoint(n);
    }

    @Override
    public AttributesType getAttributesType() {
        return points.getAttributesType();
    }

    @Override
    public PointSequence asPointSequence() {
        return points;
    }

}
