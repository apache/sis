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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometry.GeneralEnvelope;


/**
 * A Point is a 0-dimensional geometric object and represents a single location in coordinate space.
 * A Point has an x-coordinate value, a y-coordinate value.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Point", specification=ISO_19107) // section 6.4.13
public interface Point extends Primitive {

    public static final String TYPE = "POINT";

    /**
     * @return point coordinate
     */
    @UML(identifier="position", specification=ISO_19107) // section 6.4.13.2
    Tuple getPosition();

    /**
     * Returns tuple for given name.
     *
     * @param name seached attribute name
     * @return attribute or null.
     */
    Tuple getAttribute(String name);

    void setAttribute(String name, Tuple tuple);

    /**
     * View this point as a single point sequence
     */
    default PointSequence asPointSequence() {
        return new PointSequence() {
            @Override
            public CoordinateReferenceSystem getCoordinateReferenceSystem() {
                return Point.this.getCoordinateReferenceSystem();
            }

            @Override
            public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
                Point.this.setCoordinateReferenceSystem(cs);
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public Point getPoint(int index) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return Point.this;
            }

            @Override
            public Tuple getPosition(int index) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return Point.this.getPosition();
            }

            @Override
            public void setPosition(int index, Tuple value) {
                if (index != 0) throw new IndexOutOfBoundsException();
                Point.this.getPosition().set(value);
            }

            @Override
            public Tuple getAttribute(int index, String name) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return Point.this.getAttribute(name);
            }

            @Override
            public void setAttribute(int index, String name, Tuple value) {
                if (index != 0) throw new IndexOutOfBoundsException();
                Point.this.setAttribute(name, value);
            }

            @Override
            public AttributesType getAttributesType() {
                return Point.this.getAttributesType();
            }
        };
    }

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    default Envelope getEnvelope() {
        final Tuple first = getPosition();
        final BBox env = new BBox(first, first);
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        return env;
    }

    @Override
    default String asText() {
        final Tuple crd = getPosition();
        final StringBuilder sb = new StringBuilder("POINT (");
        AbstractGeometry.toText(sb, crd);
        sb.append(')');
        return sb.toString();
    }

    @UML(identifier="vectorToPoint", specification=ISO_19107) // section 6.4.13.4
    default Vector vectorToPoint(DirectPosition toPoint) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="bearing", specification=ISO_19107) // section 6.4.13.5
    default Bearing bearing(DirectPosition toPoint) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="pointAtDistance", specification=ISO_19107) // section 6.4.13.6
    default DirectPosition pointAtDistance(Vector bearing){
        //TODO
        throw new UnsupportedOperationException();
    }

}
