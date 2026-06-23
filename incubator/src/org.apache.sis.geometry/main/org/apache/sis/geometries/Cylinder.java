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

import static org.opengis.annotation.Specification.ISO_12113;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.internal.shared.AbstractOrientedGeometry;


/**
 * A cylinder centered at the origin and aligned along the Y axis in local space, with potentially different radii at each end.
 * <p>
 * A cone is a special case of cylinder when one of the radii is zero.
 * <p>
 * Synonym : conical frustrum is a volume defined by 2 circles aligned on the same axis
 *
 * ISO 19107 : classified as a conic surface
 *
 * @author Johann Sorel (Geomatys)
 * @spec ISO_19107 section 8.5.4  Cylinder
 * @spec ISO_19107 section 8.5.3  Cone
 * @spec ISO_12113 KHR_implicit_shapes extension Cylinder
 */
@UML(identifier="Cylinder", specification=ISO_12113)
public final class Cylinder extends AbstractOrientedGeometry {

    private double height = 1.0;
    private double radiusTop = 1.0;
    private double radiusBottom = 1.0;

    public Cylinder() {
    }

    @Override
    public String getGeometryType() {
        return "CYLINDER";
    }

    /**
     * Height is along the Y axis, right handed as defined in GLTF.
     * @return the cylinder height
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param height new cylinder height
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * @return cylinder top circle radius
     */
    public double getRadiusTop() {
        return radiusTop;
    }

    /**
     * @param radius new cylinder top radius
     */
    public void setRadiusTop(double radius) {
        this.radiusTop = radius;
    }

    /**
     * @return cylinder bottom circle radius
     */
    public double getRadiusBottom() {
        return radiusBottom;
    }

    /**
     * @param radius new cylinder bottom radius
     */
    public void setRadiusBottom(double radius) {
        this.radiusBottom = radius;
    }

    /**
     * A cylinder becomes a cone when the top or bottom radius is set to 0.0.
     *
     * @return true if cylinder is a cone, top or bottom radius is 0.0.
     */
    public boolean isCone() {
        return radiusBottom == 0.0 || radiusTop == 0.0;
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
