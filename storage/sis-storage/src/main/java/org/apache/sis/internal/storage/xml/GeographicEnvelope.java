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
package org.apache.sis.internal.storage.xml;

import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.sis.geometry.AbstractEnvelope;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Base class of geographic bounding boxes to expose also as an envelope and an ISO 19115 extent.
 * This base class does not contain any field. It is aimed to be sub-classed by data stores which
 * will add their own JAXB annotations. The only methods that subclasses need to implement are:
 *
 * <ul>
 *   <li>{@link #getSouthBoundLatitude()}</li>
 *   <li>{@link #getNorthBoundLatitude()}</li>
 *   <li>{@link #getWestBoundLongitude()}</li>
 *   <li>{@link #getEastBoundLongitude()}</li>
 * </ul>
 *
 * The envelope assumes a two-dimensional WGS84 coordinate reference system with
 * (<var>latitude</var>, <var>longitude</var>) axis order, as defined by EPSG:4326.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@XmlTransient
public abstract class GeographicEnvelope extends AbstractEnvelope implements GeographicBoundingBox, Extent {
    /**
     * For subclass constructors.
     */
    protected GeographicEnvelope() {
    }

    /**
     * Returns the number of dimensions, which is assumed to be 2.
     * The value returned by this method shall be equal to the value returned by
     * {@code getCoordinateReferenceSystem().getCoordinateSystem().getDimension()}.
     *
     * @return the number of dimensions in this envelope.
     */
    @Override
    public int getDimension() {
        return 2;
    }

    /**
     * Returns the coordinate reference system, or {@code null} if unknown.
     * The default implementation returns a two-dimensional WGS84 coordinate reference system
     * with (<var>latitude</var>, <var>longitude</var>) axis order, as defined by EPSG:4326.
     *
     * @return the coordinate reference system, or {@code null}.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return CommonCRS.WGS84.geographic();
    }

    /**
     * Returns the south or west envelope bound.
     *
     * @param  dimension  0 for the south bound, 1 for the west bound.
     * @return the requested envelope bound.
     * @throws IndexOutOfBoundsException if the given index is not a positive number less than the number of dimensions.
     */
    @Override
    public double getLower(final int dimension) {
        switch (dimension) {
            case 0: return getSouthBoundLatitude();
            case 1: return getWestBoundLongitude();
            default: throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Returns the north or east envelope bound.
     *
     * @param  dimension  0 for the north bound, 1 for the east bound.
     * @return the requested envelope bound.
     * @throws IndexOutOfBoundsException if the given index is not a positive number less than the number of dimensions.
     */
    @Override
    public double getUpper(int dimension) {
        switch (dimension) {
            case 0: return getNorthBoundLatitude();
            case 1: return getEastBoundLongitude();
            default: throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Provides geographic component of the extent of the referring object.
     * The default implementation returns a singleton containing only this
     * geographic bounding box.
     *
     * @return the geographic extent, or an empty set if none.
     */
    @Override
    public Collection<? extends GeographicExtent> getGeographicElements() {
        return Collections.singleton(this);
    }

    /**
     * Indication of whether the bounding box encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     * The default implementation unconditionally returns {@link Boolean#TRUE}.
     *
     * @return {@code true} for inclusion, or {@code false} for exclusion.
     */
    @Override
    public Boolean getInclusion() {
        return Boolean.TRUE;
    }
}
