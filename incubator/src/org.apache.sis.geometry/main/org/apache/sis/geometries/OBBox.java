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

import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometry.GeneralEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * An oriented BBox geometry defined by a center and half length axis.
 * This is an axis oriented bounding box, see OBBox for the oriented counterpart.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class OBBox extends AbstractGeometry {

    private Vector<?> center;
    private final Vector<?> xAxis;
    private final Vector<?> yAxis;
    private final Vector<?> zAxis;

    /**
     * @param dimension number of dimensions of the bbox, must be 3.
     */
    public OBBox(int dimension) {
        this(Geometries.getUndefinedCRS(dimension));
    }

    /**
     * @param crs obbox coordinate system with three dimensions, not null.
     */
    public OBBox(CoordinateReferenceSystem crs) {
        if (crs.getCoordinateSystem().getDimension() != 3) {
            throw new IllegalArgumentException("Oriented BBox only support 3 dimensions.");
        }
        center = Vectors.createDouble(crs);
        xAxis = Vectors.createDouble(3);
        yAxis = Vectors.createDouble(3);
        zAxis = Vectors.createDouble(3);
    }

    @Override
    public String getGeometryType() {
        return "BBOX"; //TODO not in OGC SFA.
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return center.getCoordinateReferenceSystem();
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
    public AttributesType getAttributesType() {
        return AttributesType.EMPTY;
    }

    /**
     * @return center of the bounding box.
     */
    public Tuple getCenter() {
        return center;
    }

    /**
     * @return half length vector on X axis.
     */
    public Tuple getXAxis() {
        return xAxis;
    }

    /**
     * @return half length vector on Y axis.
     */
    public Tuple getYAxis() {
        return yAxis;
    }

    /**
     * @return half length vector on Z axis.
     */
    public Tuple getZAxis() {
        return zAxis;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Envelope getEnvelope() {
        final Tuple<?> nxAxis = xAxis.copy().scale(-1);
        final Tuple<?> nyAxis = yAxis.copy().scale(-1);
        final Tuple<?> nzAxis = zAxis.copy().scale(-1);
        final GeneralEnvelope env = new GeneralEnvelope(center,center);
        final Vector<?> corner = center.copy();
        env.add(corner.set(center).add( xAxis).add( yAxis).add( zAxis));
        env.add(corner.set(center).add( xAxis).add( yAxis).add(nzAxis));
        env.add(corner.set(center).add( xAxis).add(nyAxis).add( zAxis));
        env.add(corner.set(center).add( xAxis).add(nyAxis).add(nzAxis));
        env.add(corner.set(center).add(nxAxis).add( yAxis).add( zAxis));
        env.add(corner.set(center).add(nxAxis).add( yAxis).add(nzAxis));
        env.add(corner.set(center).add(nxAxis).add(nyAxis).add( zAxis));
        env.add(corner.set(center).add(nxAxis).add(nyAxis).add(nzAxis));
        return env;
    }

    /**
     * Build oriented bbox from given envelope.
     * @param env
     */
    public void fromEnvelope(Envelope env) {
        setCoordinateReferenceSystem(env.getCoordinateReferenceSystem());
        center.set(0, env.getMedian(0));
        center.set(1, env.getMedian(1));
        center.set(2, env.getMedian(2));
        xAxis.setAll(0.0);
        yAxis.setAll(0.0);
        zAxis.setAll(0.0);
        xAxis.set(0, env.getSpan(0) / 2.0);
        yAxis.set(1, env.getSpan(1) / 2.0);
        zAxis.set(2, env.getSpan(2) / 2.0);
    }
}
