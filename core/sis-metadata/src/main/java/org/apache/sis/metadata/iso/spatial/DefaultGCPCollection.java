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
package org.apache.sis.metadata.iso.spatial;

import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.spatial.GCP;
import org.opengis.metadata.spatial.GCPCollection;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.Namespaces;


/**
 * Information about a control point collection.
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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MI_GCPCollection_Type", propOrder = {
    "collectionIdentification",
    "collectionName",
    "coordinateReferenceSystem",
    "GCPs"
})
@XmlRootElement(name = "MI_GCPCollection", namespace = Namespaces.GMI)
public class DefaultGCPCollection extends AbstractGeolocationInformation implements GCPCollection {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2757911443659529373L;

    /**
     * Identifier of the GCP collection.
     */
    private Integer collectionIdentification;

    /**
     * Name of the GCP collection.
     */
    private InternationalString collectionName;

    /**
     * Coordinate system in which the ground control points are defined.
     */
    private ReferenceSystem coordinateReferenceSystem;

    /**
     * Ground control point(s) used in the collection.
     */
    private Collection<GCP> GCPs;

    /**
     * Constructs an initially empty ground control point collection.
     */
    public DefaultGCPCollection() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GCPCollection)
     */
    public DefaultGCPCollection(final GCPCollection object) {
        super(object);
        if (object != null) {
            collectionIdentification  = object.getCollectionIdentification();
            collectionName            = object.getCollectionName();
            coordinateReferenceSystem = object.getCoordinateReferenceSystem();
            GCPs                      = copyCollection(object.getGCPs(), GCP.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGCPCollection}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGCPCollection} instance is created using the
     *       {@linkplain #DefaultGCPCollection(GCPCollection) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGCPCollection castOrCopy(final GCPCollection object) {
        if (object == null || object instanceof DefaultGCPCollection) {
            return (DefaultGCPCollection) object;
        }
        return new DefaultGCPCollection(object);
    }

    /**
     * Returns the identifier of the GCP collection.
     *
     * @return The identifier, or {@code null}.
     */
    @Override
    @XmlElement(name = "collectionIdentification", namespace = Namespaces.GMI, required = true)
    public Integer getCollectionIdentification() {
        return collectionIdentification;
    }

    /**
     * Sets the identifier of the GCP collection.
     *
     * @param newValue The new collection identifier value.
     */
    public void setCollectionIdentification(final Integer newValue) {
        checkWritePermission();
        collectionIdentification = newValue;
    }

    /**
     * Returns the name of the GCP collection.
     *
     * @return Name of the GCP collection, or {@code null}.
     */
    @Override
    @XmlElement(name = "collectionName", namespace = Namespaces.GMI, required = true)
    public InternationalString getCollectionName() {
        return collectionName;
    }

    /**
     * Sets the name of the GCP collection.
     *
     * @param newValue The new collection name.
     */
    public void setCollectionName(final InternationalString newValue) {
        checkWritePermission();
        collectionName = newValue;
    }

    /**
     * Returns the coordinate system in which the ground control points are defined.
     *
     * @return Coordinate system in which the ground control points are defined, or {@code null}.
     */
    @Override
    @XmlElement(name = "coordinateReferenceSystem", namespace = Namespaces.GMI, required = true)
    public ReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

    /**
     * Sets the coordinate system in which the ground control points are defined.
     *
     * @param newValue The new coordinate reference system value.
     */
    public void setCoordinateReferenceSystem(final ReferenceSystem newValue) {
        checkWritePermission();
        coordinateReferenceSystem = newValue;
    }

    /**
     * Returns the ground control point(s) used in the collection.
     *
     * @return Ground control point(s).
     */
    @Override
    @XmlElement(name = "gcp", namespace = Namespaces.GMI, required = true)
    public Collection<GCP> getGCPs() {
        return GCPs = nonNullCollection(GCPs, GCP.class);
    }

    /**
     * Sets the ground control point(s) used in the collection.
     *
     * @param newValues The new ground control points values.
     */
    public void setGCPs(final Collection<? extends GCP> newValues) {
        GCPs = writeCollection(newValues, GCPs, GCP.class);
    }
}
