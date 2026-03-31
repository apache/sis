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

import java.util.List;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.Identifier;


/**
 * A sphere geometry defined by a center and a radius.
 * Even if it is called a Sphere this class can handle 2 to N dimensions.
 *
 * ISO 19107 : classified as a conic surface
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Sphere", specification=ISO_19107) // section 8.5.2
public final class Sphere extends AbstractGeometry implements ParametricCurveSurface {

    private Tuple center;
    private double radius = 1.0;

    /**
     * @param dimension number of dimensions of the sphere, must be positive.
     */
    public Sphere(int dimension) {
        this(Geometries.getUndefinedCRS(dimension));
    }

    /**
     * @param crs sphere coordinate system, not null.
     */
    public Sphere(CoordinateReferenceSystem crs) {
        center = Vectors.createDouble(crs);
    }

    @Override
    public String getGeometryType() {
        return "SPHERE";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        if (cs.getCoordinateSystem().getDimension() != getCoordinateReferenceSystem().getCoordinateSystem().getDimension()) {
            throw new IllegalArgumentException("New CRS dimension must be the same as previous CRS");
        }
        Vector v = Vectors.create(cs, center.getDataType());
        v.set(center);
        center = v;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return center.getCoordinateReferenceSystem();
    }

    @Override
    public AttributesType getAttributesType() {
        return AttributesType.EMPTY;
    }

    /**
     * @return radius of the sphere.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @param radius new sphere radius, must be positive.
     */
    public void setRadius(double radius) {
        ArgumentChecks.ensurePositive("radius", radius);
        this.radius = radius;
    }

    /**
     * @return sphere center, modifiable.
     */
    public Tuple getCenter() {
        return center;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Envelope getEnvelope() {
        final Tuple center = getCenter();
        final BBox env = new BBox(center, center);
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        if (radius > 0) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                double c = center.get(i);
                env.setRange(0, c-radius, c+radius);
            }
        }
        return env;
    }

    // methods from ParametricCurveSurface

    @Override
    public int getRows() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getColumns() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DirectPosition> getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DirectPosition> getDataPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryType getHorizontalCurveType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryType getVerticalCurveType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Knot> getKnots() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Curve getHorizontalCurve(double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Curve getVerticalCurve(double u) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectPosition getSurface(double u, double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
