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
    private static final long serialVersionUID = 1172943083269243669L;

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
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(FeatureTypeList)
     */
    public DefaultFeatureTypeList(final FeatureTypeList object) {
        super(object);
        if (object != null) {
            spatialObject     = object.getSpatialObject();
            spatialSchemaName = object.getSpatialSchemaName();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultFeatureTypeList}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultFeatureTypeList} instance is created using the
     *       {@linkplain #DefaultFeatureTypeList(FeatureTypeList) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultFeatureTypeList castOrCopy(final FeatureTypeList object) {
        if (object == null || object instanceof DefaultFeatureTypeList) {
            return (DefaultFeatureTypeList) object;
        }
        return new DefaultFeatureTypeList(object);
    }

    /**
     * Instance of a type defined in the spatial schema.
     *
     * @return Instance of a type defined in the spatial schema, or {@code null}.
     */
    @Override
    @XmlElement(name = "spatialObject", required = true)
    public String getSpatialObject() {
        return spatialObject;
    }

    /**
     * Sets the instance of a type defined in the spatial schema.
     *
     * @param newValue The new spatial object.
     */
    public void setSpatialObject(final String newValue) {
        checkWritePermission();
        spatialObject = newValue;
    }

    /**
     * Name of the spatial schema used.
     *
     * @return Name of the spatial schema used, or {@code null}.
     */
    @Override
    @XmlElement(name = "spatialSchemaName", required = true)
    public String getSpatialSchemaName() {
        return spatialSchemaName;
    }

    /**
     * Sets the name of the spatial schema used.
     *
     * @param newValue The new spatial schema.
     */
    public void setSpatialSchemaName(final String newValue) {
        checkWritePermission();
        spatialSchemaName = newValue;
    }
}
