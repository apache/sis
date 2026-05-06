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

import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A Frustum is a delimited space defined by one or many planes.
 * It is often used to clip models outside the camera visible area.
 *
 * @author Johann Sorel
 */
public final class Frustrum extends AbstractGeometry {

    private final Plane[] planes;

    public Frustrum(Plane[] planes) {
        this.planes = planes;
    }

    @Override
    public String getGeometryType() {
        return "FRUSTRUM";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public Plane[] getPlanes() {
        return planes;
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
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
