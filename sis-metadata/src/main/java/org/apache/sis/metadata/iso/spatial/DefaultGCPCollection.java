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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
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
    private static final long serialVersionUID = -5267006706468159746L;

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
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGCPCollection castOrCopy(final GCPCollection object) {
        if (object == null || object instanceof DefaultGCPCollection) {
            return (DefaultGCPCollection) object;
        }
        final DefaultGCPCollection copy = new DefaultGCPCollection();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the identifier of the GCP collection.
     */
    @Override
    @XmlElement(name = "collectionIdentification", namespace = Namespaces.GMI, required = true)
    public synchronized Integer getCollectionIdentification() {
        return collectionIdentification;
    }

    /**
     * Sets the identifier of the GCP collection.
     *
     * @param newValue The new collection identifier value.
     */
    public synchronized void setCollectionIdentification(final Integer newValue) {
        checkWritePermission();
        collectionIdentification = newValue;
    }

    /**
     * Returns the name of the GCP collection.
     */
    @Override
    @XmlElement(name = "collectionName", namespace = Namespaces.GMI, required = true)
    public synchronized InternationalString getCollectionName() {
        return collectionName;
    }

    /**
     * Sets the name of the GCP collection.
     *
     * @param newValue The new collection name.
     */
    public synchronized void setCollectionName(final InternationalString newValue) {
        checkWritePermission();
        collectionName = newValue;
    }

    /**
     * Returns the coordinate system in which the ground control points are defined.
     */
    @Override
    @XmlElement(name = "coordinateReferenceSystem", namespace = Namespaces.GMI, required = true)
    public synchronized ReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

    /**
     * Sets the coordinate system in which the ground control points are defined.
     *
     * @param newValue The new coordinate reference system value.
     */
    public synchronized void setCoordinateReferenceSystem(final ReferenceSystem newValue) {
        checkWritePermission();
        coordinateReferenceSystem = newValue;
    }

    /**
     * Returns the ground control point(s) used in the collection.
     */
    @Override
    @XmlElement(name = "gcp", namespace = Namespaces.GMI, required = true)
    public synchronized Collection<GCP> getGCPs() {
        return GCPs = nonNullCollection(GCPs, GCP.class);
    }

    /**
     * Sets the ground control point(s) used in the collection.
     *
     * @param newValues The new ground control points values.
     */
    public synchronized void setGCPs(final Collection<? extends GCP> newValues) {
        GCPs = writeCollection(newValues, GCPs, GCP.class);
    }
}
