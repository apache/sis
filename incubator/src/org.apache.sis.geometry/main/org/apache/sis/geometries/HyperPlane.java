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

import org.apache.sis.geometries.internal.shared.AbstractOrientedGeometry;
import static org.opengis.annotation.Specification.ISO_12113;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A hyperplane centered at the origin in local space, with normal along the +Y axis in local space.
 * The hyper plane divised the geometric space in two.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Plane", specification=ISO_12113)
public final class HyperPlane extends AbstractOrientedGeometry {

    public HyperPlane() {
    }

    @Override
    public String getGeometryType() {
        return "HYPERPLANE";
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
    public Envelope getUnorientedEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
