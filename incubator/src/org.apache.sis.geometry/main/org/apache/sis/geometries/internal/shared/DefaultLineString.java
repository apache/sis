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

import javax.measure.Quantity;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.measure.Quantities;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultLineString extends AbstractGeometry implements LineString {

    private final DataPoints points;

    public DefaultLineString(DataPoints points) {
        this.points = points;
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public Quantity<?> getLength() {
        //TODO : fallback on JTS until implemented
        return Quantities.create(asJTS().getLength(), Geometries.getLinearUnit(this));
    }

    @Override
    public Point getCentroid() {
        if (isEmpty()) {
            // The centroid of the empty set is undefined.
            return null;
        }
        //TODO : fallback on JTS until implemented
        return (Point) fromJTS(asJTS().getCentroid());
    }

    @Override
    public Point getRepresentativePoint() {
        if (isEmpty()) {
            // The empty set has no interior position.
            return null;
        }
        //TODO : fallback on JTS until implemented
        return (Point) fromJTS(asJTS().getInteriorPoint());
    }

    @Override
    public boolean isSimple() {
        //TODO : fallback on JTS until implemented
        return asJTS().isSimple();
    }

    @Override
    public boolean isValid() {
        //TODO : fallback on JTS until implemented
        return asJTS().isValid();
    }

}
