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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.spatial.GeometricObjects;
import org.opengis.metadata.spatial.GeometricObjectType;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Number of objects, listed by geometric object type, used in the dataset.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_GeometricObjects_Type", propOrder = {
    "geometricObjectType",
    "geometricObjectCount"
})
@XmlRootElement(name = "MD_GeometricObjects")
public class DefaultGeometricObjects extends ISOMetadata implements GeometricObjects {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7965994170083468201L;

    /**
     * Total number of the point or vector object type occurring in the dataset.
     */
    private GeometricObjectType geometricObjectType;

    /**
     * Total number of the point or vector object type occurring in the dataset.
     */
    private Integer geometricObjectCount;

    /**
     * Constructs an initially empty geometric objects.
     */
    public DefaultGeometricObjects() {
    }

    /**
     * Creates a geometric object initialized to the given type.
     *
     * @param geometricObjectType Total number of the point or vector
     *          object type occurring in the dataset.
     */
    public DefaultGeometricObjects(final GeometricObjectType geometricObjectType) {
        this.geometricObjectType = geometricObjectType;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(GeometricObjects)
     */
    public DefaultGeometricObjects(final GeometricObjects object) {
        super(object);
        geometricObjectType  = object.getGeometricObjectType();
        geometricObjectCount = object.getGeometricObjectCount();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGeometricObjects}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGeometricObjects} instance is created using the
     *       {@linkplain #DefaultGeometricObjects(GeometricObjects) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGeometricObjects castOrCopy(final GeometricObjects object) {
        if (object == null || object instanceof DefaultGeometricObjects) {
            return (DefaultGeometricObjects) object;
        }
        return new DefaultGeometricObjects(object);
    }

    /**
     * Returns the total number of the point or vector object type occurring in the dataset.
     */
    @Override
    @XmlElement(name = "geometricObjectType", required = true)
    public synchronized GeometricObjectType getGeometricObjectType() {
        return geometricObjectType;
    }

    /**
     * Sets the total number of the point or vector object type occurring in the dataset.
     *
     * @param newValue The new geometric object type.
     */
    public synchronized void setGeometricObjectType(final GeometricObjectType newValue) {
        checkWritePermission();
        geometricObjectType = newValue;
    }

    /**
     * Returns the total number of the point or vector object type occurring in the dataset.
     */
    @Override
    @ValueRange(minimum=0)
    @XmlElement(name = "geometricObjectCount")
    public synchronized Integer getGeometricObjectCount() {
        return geometricObjectCount;
    }

    /**
     * Sets the total number of the point or vector object type occurring in the dataset.
     *
     * @param newValue The geometric object count.
     */
    public synchronized void setGeometricObjectCount(final Integer newValue) {
        checkWritePermission();
        geometricObjectCount = newValue;
    }
}
