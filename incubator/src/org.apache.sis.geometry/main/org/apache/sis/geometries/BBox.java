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

import java.util.HashMap;
import java.util.Map;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometry.GeneralEnvelope;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A BBOx geometry defined by lower and upper corners.
 * This is not an axis oriented bounding box, see OBBox for the oriented counterpart.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class BBox extends GeneralEnvelope implements Geometry {

    private Map<String,Object> properties;

    /**
     * @param dimension number of dimensions of the bbox, must be positive.
     */
    public BBox(int dimension) {
        super(dimension);
    }

    /**
     * @param dimension number of dimensions of the bbox, must be positive.
     * @param corners lower corner and higher corner, in crs axis order
     */
    public BBox(int dimension, double ... corners) {
        super(dimension);
        setEnvelope(corners);
    }

    /**
     * @param lower lower corner
     * @param upper upper corner
     */
    public BBox(Tuple lower, Tuple upper) {
        super(Vectors.asDirectPostion(lower), Vectors.asDirectPostion(upper));
    }

    /**
     * @param crs sphere coordinate system, not null.
     */
    public BBox(CoordinateReferenceSystem crs) {
        super(crs);
    }

    /**
     * @param crs sphere coordinate system, not null.
     * @param corners lower corner and higher corner, in crs axis order
     */
    public BBox(CoordinateReferenceSystem crs, double ... corners) {
        super(crs);
        setEnvelope(corners);
    }

    /**
     * @param env Envelope to copy crs and coordinates from.
     */
    public BBox(Envelope env) {
        super(env);
    }

    @Override
    public String getGeometryType() {
        return "POLYGON"; //TODO not in OGC SFA.
    }

    public void add(Tuple<?> position) throws MismatchedDimensionException {
        add(Vectors.asDirectPostion(position));
    }

    /**
     * {@inheritDoc }
     */
    public Tuple<?> getLower() {
        return Vectors.castOrWrap(super.getLowerCorner());
    }

    /**
     * {@inheritDoc }
     */
    public Tuple<?> getUpper() {
        return Vectors.castOrWrap(super.getUpperCorner());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Envelope getEnvelope() {
        return this.clone();
    }

    @Override
    public synchronized Map<String, Object> userProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public AttributesType getAttributesType() {
        return AttributesType.EMPTY;
    }

}
