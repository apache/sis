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
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.4
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
    private static final long serialVersionUID = 4819196764221609265L;

    /**
     * Indication of whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     *
     * <p>Implementation note: we need to store the reference to the {@code Boolean} instance instead
     * than using bitmask because {@link org.apache.sis.internal.jaxb.PrimitiveTypeProperties} may
     * associate some properties to that particular instance.</p>
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
        this.inclusion = inclusion;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GeographicExtent)
     */
    public AbstractGeographicExtent(final GeographicExtent object) {
        super(object);
        if (object != null) {
            inclusion = object.getInclusion();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link BoundingPolygon},
     *       {@link GeographicBoundingBox} or {@link GeographicDescription}, then this method
     *       delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractGeographicExtent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractGeographicExtent} instance is created using the
     *       {@linkplain #AbstractGeographicExtent(GeographicExtent) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
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
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractGeographicExtent) {
            return (AbstractGeographicExtent) object;
        }
        return new AbstractGeographicExtent(object);
    }

    /**
     * Indication of whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     *
     * @return {@code true} for inclusion, or {@code false} for exclusion.
     */
    @Override
    @XmlElement(name = "extentTypeCode")
    public Boolean getInclusion() {
        return inclusion;
    }

    /**
     * Sets whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     *
     * @param newValue {@code true} if the bounding polygon encompasses an area covered by the data.
     */
    public void setInclusion(final Boolean newValue) {
        checkWritePermission();
        inclusion = newValue;
    }
}
