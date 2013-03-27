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
import org.opengis.metadata.spatial.GeometricObjects;
import org.opengis.metadata.spatial.TopologyLevel;
import org.opengis.metadata.spatial.VectorSpatialRepresentation;


/**
 * Information about the vector spatial objects in the dataset.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
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
    private static final long serialVersionUID = 5643234643524810592L;

    /**
     * Code which identifies the degree of complexity of the spatial relationships.
    */
    private TopologyLevel topologyLevel;

    /**
     * Information about the geometric objects used in the dataset.
     */
    private Collection<GeometricObjects> geometricObjects;

    /**
     * Constructs an initially empty vector spatial representation.
     */
    public DefaultVectorSpatialRepresentation() {
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
    public static DefaultVectorSpatialRepresentation castOrCopy(final VectorSpatialRepresentation object) {
        if (object == null || object instanceof DefaultVectorSpatialRepresentation) {
            return (DefaultVectorSpatialRepresentation) object;
        }
        final DefaultVectorSpatialRepresentation copy = new DefaultVectorSpatialRepresentation();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the code which identifies the degree of complexity of the spatial relationships.
     */
    @Override
    @XmlElement(name = "topologyLevel")
    public synchronized TopologyLevel getTopologyLevel() {
        return topologyLevel;
    }

    /**
     * Sets the code which identifies the degree of complexity of the spatial relationships.
     *
     * @param newValue The new topology level.
     */
    public synchronized void setTopologyLevel(final TopologyLevel newValue) {
        checkWritePermission();
        topologyLevel = newValue;
    }

    /**
     * Returns information about the geometric objects used in the dataset.
     */
    @Override
    @XmlElement(name = "geometricObjects")
    public synchronized Collection<GeometricObjects> getGeometricObjects() {
        return geometricObjects = nonNullCollection(geometricObjects, GeometricObjects.class);
    }

    /**
     * Sets information about the geometric objects used in the dataset.
     *
     * @param newValues The new geometric objects.
     */
    public synchronized void setGeometricObjects(final Collection<? extends GeometricObjects> newValues) {
        geometricObjects = writeCollection(newValues, geometricObjects, GeometricObjects.class);
    }
}
