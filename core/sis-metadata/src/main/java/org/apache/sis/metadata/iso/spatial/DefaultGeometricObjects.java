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

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;


/**
 * Number of objects, listed by geometric object type, used in the dataset.
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
 * @version 0.5
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
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GeometricObjects)
     */
    public DefaultGeometricObjects(final GeometricObjects object) {
        super(object);
        if (object != null) {
            geometricObjectType  = object.getGeometricObjectType();
            geometricObjectCount = object.getGeometricObjectCount();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
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
     *
     * @return Name of spatial objects used to locate spatial locations in the dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "geometricObjectType", required = true)
    public GeometricObjectType getGeometricObjectType() {
        return geometricObjectType;
    }

    /**
     * Sets the total number of the point or vector object type occurring in the dataset.
     *
     * @param newValue The new geometric object type.
     */
    public void setGeometricObjectType(final GeometricObjectType newValue) {
        checkWritePermission();
        geometricObjectType = newValue;
    }

    /**
     * Returns the total number of the point or vector object type occurring in the dataset.
     *
     * @return Total number of the point or vector object type, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 1)
    @XmlElement(name = "geometricObjectCount")
    public Integer getGeometricObjectCount() {
        return geometricObjectCount;
    }

    /**
     * Sets the total number of the point or vector object type occurring in the dataset.
     *
     * @param newValue The geometric object count, or {@code null}.
     * @throws IllegalArgumentException if the given value is zero or negative.
     */
    public void setGeometricObjectCount(final Integer newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultGeometricObjects.class, "geometricObjectCount", true, newValue)) {
            geometricObjectCount = newValue;
        }
    }
}
