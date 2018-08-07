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
package org.apache.sis.internal.storage.gpx;

import javax.xml.bind.annotation.XmlAttribute;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.internal.storage.xml.GeographicEnvelope;


/**
 * Geographic bounding box encoded in a GPX file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Bounds extends GeographicEnvelope {
    /**
     * The western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    @XmlAttribute(name = Attributes.MIN_X, required = true)
    public double westBoundLongitude = Double.NaN;

    /**
     * The eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    @XmlAttribute(name = Attributes.MAX_X, required = true)
    public double eastBoundLongitude = Double.NaN;

    /**
     * The southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    @XmlAttribute(name = Attributes.MIN_Y, required = true)
    public double southBoundLatitude = Double.NaN;

    /**
     * The northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    @XmlAttribute(name = Attributes.MAX_Y, required = true)
    public double northBoundLatitude = Double.NaN;

    /**
     * Creates an initially empty bounds.
     */
    public Bounds() {
    }

    /**
     * Copies properties from the given ISO 19115 metadata.
     */
    private Bounds(final GeographicBoundingBox box) {
        westBoundLongitude = box.getWestBoundLongitude();
        eastBoundLongitude = box.getEastBoundLongitude();
        northBoundLatitude = box.getNorthBoundLatitude();
        southBoundLatitude = box.getSouthBoundLatitude();
    }

    /**
     * Returns the given ISO 19115 metadata as a {@code Bounds} instance.
     * This method copies the data only if needed.
     *
     * @param  box  the ISO 19115 metadata, or {@code null}.
     * @return the GPX metadata, or {@code null}.
     */
    public static Bounds castOrCopy(final GeographicBoundingBox box) {
        return (box == null || box instanceof Bounds) ? (Bounds) box : new Bounds(box);
    }

    /**
     * Returns the western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     *
     * @return the western-most longitude between -180 and +180°.
     */
    @Override
    public double getWestBoundLongitude() {
        return westBoundLongitude;
    }

    /**
     * Returns the eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     *
     * @return the eastern-most longitude between -180 and +180°.
     */
    @Override
    public double getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    /**
     * Returns the southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return the southern-most latitude between -90 and +90°.
     */
    @Override
    public double getSouthBoundLatitude() {
        return southBoundLatitude;
    }

    /**
     * Returns the northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return the northern-most latitude between -90 and +90°.
     */
    @Override
    public double getNorthBoundLatitude() {
        return northBoundLatitude;
    }
}
