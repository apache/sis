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
 * A plane centered at the origin in local space, with normal along the +Y axis in local space,
 * optionally with finite extents and may be double or single-sided.
 * <p>
 * Synonym : Quad , for a plane with finite extents
 * <p>
 * Synonym : Sheet , for a double-sided plane
 *
 *
 * @author Johann Sorel (Geomatys)
 * @spec ISO_12113 KHR_implicit_shapes extension Plane
 */
@UML(identifier="Plane", specification=ISO_12113)
public final class Plane extends AbstractOrientedGeometry {

    private double sizeX = Double.NaN;
    private double sizeZ = Double.NaN;
    private boolean doubleSided = false;

    public Plane() {
    }

    @Override
    public String getGeometryType() {
        return "PLANE";
    }

    /**
     * @return the plane X size
     */
    public double getSizeX() {
        return sizeX;
    }

    /**
     * @param size new plane X size
     */
    public void setSizeX(double size) {
        this.sizeX = size;
    }

    /**
     * @return the plane Z size
     */
    public double getSizeZ() {
        return sizeZ;
    }

    /**
     * @param size new plane Z size
     */
    public void setSizeZ(double size) {
        this.sizeZ = size;
    }

    /**
     * @return true if double sided
     */
    public boolean isDoubleSided() {
        return doubleSided;
    }

    /**
     * @param doubleSided true if double sided
     */
    public void setDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * A plane becomes a quad when the sizes are set.
     *
     * @return true if plane is a quad
     */
    public boolean isQuad() {
        return Double.isFinite(sizeX) && Double.isFinite(sizeZ);
    }

    /**
     * A plane becomes a sheet when it is double sided.
     *
     * @return true if plane is a sheet
     */
    public boolean isSheet() {
        return doubleSided;
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
