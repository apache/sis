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

import java.util.Objects;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A ray is a one direction infinite line.
 * It is build from a position and a direction.
 *
 * @author Johann Sorel
 */
public final class Ray extends AbstractGeometry{

    private Tuple<?> position;
    private Vector<?> direction;

    public Ray(int dimension) {
        this(Vectors.createDouble(dimension),Vectors.createDouble(dimension));
    }

    public Ray(Tuple<?> position, Vector<?> direction) {
        this.position = position;
        this.direction = direction;
    }

    @Override
    public String getGeometryType() {
        return "RAY";
    }

    public Tuple<?> getPosition() {
        return position;
    }

    public void setPosition(Tuple<?> position) {
        this.position = position;
    }

    public Vector<?> getDirection() {
        return direction;
    }

    public void setDirection(Vector<?> direction) {
        this.direction = direction;
    }

    @Override
    public BBox getEnvelope() {
        final int dim = direction.getDimension();
        final BBox bbox = new BBox(dim);
        for (int i=0;i<dim;i++){
            double d = direction.get(i);
            if (d<0){
                bbox.setRange(i, Double.NEGATIVE_INFINITY, position.get(i));
            } else if (d>0){
                bbox.setRange(i, position.get(i), Double.POSITIVE_INFINITY);
            } else {
                bbox.setRange(i, position.get(i), position.get(i));
            }
        }
        return bbox;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AttributesType getAttributesType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Ray other = (Ray) obj;
        if (this.position != other.position && (this.position == null || !this.position.equals(other.position))) {
            return false;
        }
        return this.direction == other.direction || (this.direction != null && this.direction.equals(other.direction));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.position);
        hash = 89 * hash + Objects.hashCode(this.direction);
        return hash;
    }
}
