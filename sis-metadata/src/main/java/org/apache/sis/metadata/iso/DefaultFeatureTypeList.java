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
package org.apache.sis.metadata.iso;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.FeatureTypeList;


/**
 * List of names of feature types with the same spatial representation (same as spatial attributes).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(propOrder = {
    "spatialObject",
    "spatialSchemaName"
})
@XmlRootElement(name = "MD_FeatureTypeList")
public class DefaultFeatureTypeList extends ISOMetadata implements FeatureTypeList {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5417914796207743856L;

    /**
     * Instance of a type defined in the spatial schema.
     */
    private String spatialObject;

    /**
     * Name of the spatial schema used.
     */
    private String spatialSchemaName;

    /**
     * Construct an initially empty feature type list.
     */
    public DefaultFeatureTypeList() {
    }

    /**
     * Creates a feature type list initialized to the given values.
     *
     * @param spatialObject The instance of a type defined in the spatial schema, or {@code null} if none.
     * @param spatialSchemaName The name of the spatial schema used, or {@code null} if none.
     */
    public DefaultFeatureTypeList(final String spatialObject, final String spatialSchemaName) {
        this.spatialObject     = spatialObject;
        this.spatialSchemaName = spatialSchemaName;
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
    public static DefaultFeatureTypeList castOrCopy(final FeatureTypeList object) {
        if (object == null || object instanceof DefaultFeatureTypeList) {
            return (DefaultFeatureTypeList) object;
        }
        final DefaultFeatureTypeList copy = new DefaultFeatureTypeList();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Instance of a type defined in the spatial schema.
     */
    @Override
    @XmlElement(name = "spatialObject", required = true)
    public synchronized String getSpatialObject() {
        return spatialObject;
    }

    /**
     * Sets the instance of a type defined in the spatial schema.
     *
     * @param newValue The new spatial object.
     */
    public synchronized void setSpatialObject(final String newValue) {
        checkWritePermission();
        spatialObject = newValue;
    }

    /**
     * Name of the spatial schema used.
     */
    @Override
    @XmlElement(name = "spatialSchemaName", required = true)
    public synchronized String getSpatialSchemaName() {
        return spatialSchemaName;
    }

    /**
     * Sets the name of the spatial schema used.
     *
     * @param newValue The new spatial schema.
     */
    public synchronized void setSpatialSchemaName(final String newValue) {
        checkWritePermission();
        spatialSchemaName = newValue;
    }
}
