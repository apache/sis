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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.opengis.metadata.spatial.GeometricObjects;
import org.opengis.metadata.spatial.TopologyLevel;
import org.opengis.metadata.spatial.VectorSpatialRepresentation;


/**
 * Information about the vector spatial objects in the dataset.
 *
 * <h2>Limitations</h2>
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
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_VectorSpatialRepresentation_Type", propOrder = {
    "topologyLevel",
    "geometricObjects"
})
@XmlRootElement(name = "MD_VectorSpatialRepresentation")
public class DefaultVectorSpatialRepresentation extends AbstractSpatialRepresentation
        implements VectorSpatialRepresentation
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5891825325520101913L;

    /**
     * Code which identifies the degree of complexity of the spatial relationships.
    */
    private TopologyLevel topologyLevel;

    /**
     * Information about the geometric objects used in the dataset.
     */
    @SuppressWarnings("serial")
    private Collection<GeometricObjects> geometricObjects;

    /**
     * Constructs an initially empty vector spatial representation.
     */
    public DefaultVectorSpatialRepresentation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(VectorSpatialRepresentation)
     */
    public DefaultVectorSpatialRepresentation(final VectorSpatialRepresentation object) {
        super(object);
        if (object != null) {
            topologyLevel    = object.getTopologyLevel();
            geometricObjects = copyCollection(object.getGeometricObjects(), GeometricObjects.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultVectorSpatialRepresentation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultVectorSpatialRepresentation} instance is created using the
     *       {@linkplain #DefaultVectorSpatialRepresentation(VectorSpatialRepresentation) copy constructor}
     *       and returned. Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultVectorSpatialRepresentation castOrCopy(final VectorSpatialRepresentation object) {
        if (object == null || object instanceof DefaultVectorSpatialRepresentation) {
            return (DefaultVectorSpatialRepresentation) object;
        }
        return new DefaultVectorSpatialRepresentation(object);
    }

    /**
     * Returns the code which identifies the degree of complexity of the spatial relationships.
     *
     * @return the degree of complexity of the spatial relationships, or {@code null}.
     */
    @Override
    @XmlElement(name = "topologyLevel")
    public TopologyLevel getTopologyLevel() {
        return topologyLevel;
    }

    /**
     * Sets the code which identifies the degree of complexity of the spatial relationships.
     *
     * @param  newValue  the new topology level.
     */
    public void setTopologyLevel(final TopologyLevel newValue) {
        checkWritePermission(topologyLevel);
        topologyLevel = newValue;
    }

    /**
     * Returns information about the geometric objects used in the dataset.
     *
     * @return information about the geometric objects used in the dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "geometricObjects")
    public Collection<GeometricObjects> getGeometricObjects() {
        return geometricObjects = nonNullCollection(geometricObjects, GeometricObjects.class);
    }

    /**
     * Sets information about the geometric objects used in the dataset.
     *
     * @param  newValues  the new geometric objects.
     */
    public void setGeometricObjects(final Collection<? extends GeometricObjects> newValues) {
        geometricObjects = writeCollection(newValues, geometricObjects, GeometricObjects.class);
    }
}
