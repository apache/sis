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
 *
 * This package contains documentation from OGC specifications.
 * Open Geospatial Consortium's work is fully acknowledged here.
 */
package org.apache.sis.metadata.iso.extent;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;
import org.opengis.metadata.extent.BoundingPolygon;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Base class for geographic area of the dataset.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "AbstractEX_GeographicExtent_Type")
@XmlRootElement(name = "EX_GeographicExtent")
@XmlSeeAlso({
    DefaultGeographicBoundingBox.class,
    DefaultBoundingPolygon.class,
    DefaultGeographicDescription.class
})
public class AbstractGeographicExtent extends ISOMetadata implements GeographicExtent {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8844015895495563161L;

    /**
     * Indication of whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     */
    private Boolean inclusion;

    /**
     * Constructs an initially empty geographic extent.
     */
    public AbstractGeographicExtent() {
    }

    /**
     * Constructs a geographic extent initialized with the specified inclusion value.
     *
     * @param inclusion Whether the bounding polygon encompasses an area covered by the data.
     */
    public AbstractGeographicExtent(final boolean inclusion) {
        this.inclusion = Boolean.valueOf(inclusion);
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * <p>This method checks for the {@link BoundingPolygon}, {@link GeographicBoundingBox} and
     * {@link GeographicDescription} sub-interfaces. If one of those interfaces is found, then
     * this method delegates to the corresponding {@code castOrCopy} static method. If the given
     * object implements more than one of the above-cited interfaces, then the {@code castOrCopy}
     * method to be used is unspecified.</p>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractGeographicExtent castOrCopy(final GeographicExtent object) {
        if (object instanceof BoundingPolygon) {
            return DefaultBoundingPolygon.castOrCopy((BoundingPolygon) object);
        }
        if (object instanceof GeographicBoundingBox) {
            return DefaultGeographicBoundingBox.castOrCopy((GeographicBoundingBox) object);
        }
        if (object instanceof GeographicDescription) {
            return DefaultGeographicDescription.castOrCopy((GeographicDescription) object);
        }
        if (object == null || object instanceof AbstractGeographicExtent) {
            return (AbstractGeographicExtent) object;
        }
        final AbstractGeographicExtent copy = new AbstractGeographicExtent();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Indication of whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     *
     * @return {@code true} for inclusion, or {@code false} for exclusion.
     */
    @Override
    @XmlElement(name = "extentTypeCode")
    public synchronized Boolean getInclusion() {
        return inclusion;
    }

    /**
     * Sets whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     *
     * @param newValue {@code true} if the bounding polygon encompasses an area covered by the data.
     */
    public synchronized void setInclusion(final Boolean newValue) {
        checkWritePermission();
        inclusion = newValue;
    }
}
