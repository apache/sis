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

import java.util.Collections;
import java.util.List;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultPolygon extends AbstractGeometry implements Polygon {

    protected final Curve exterior;
    protected final List<Curve> interiors;

    public DefaultPolygon(LinearRing exterior) {
        this(exterior, null);
    }

    public DefaultPolygon(LinearRing exterior, List<LinearRing> interiors) {
        ArgumentChecks.ensureNonNull("exterior", exterior);
        this.exterior = exterior;
        this.interiors = interiors == null ? Collections.EMPTY_LIST : UnmodifiableArrayList.wrap(interiors.toArray(new LinearRing[0]));

        if (!this.interiors.isEmpty()) {
            for (Curve interior : this.interiors) {
                Geometries.ensureSameAttributes(exterior.getAttributesType(), interior.getAttributesType());
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return exterior.isEmpty();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return exterior.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        exterior.setCoordinateReferenceSystem(cs);
        for (Curve interior : interiors) {
            interior.setCoordinateReferenceSystem(cs);
        }
    }

    @Override
    public Curve getExteriorRing() {
        return exterior;
    }

    @Override
    public List<Curve> getInteriorRings() {
        return interiors;
    }

    @Override
    public Envelope getEnvelope() {
        return getExteriorRing().getEnvelope();
    }

}
