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

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.opengis.metadata.spatial.VectorSpatialRepresentation;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.bind.metadata.MD_Scope;

// Specific to the main branch:
import org.opengis.metadata.quality.Scope;


/**
 * Method used to represent geographic information in the dataset.
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
@XmlType(name = "AbstractMD_SpatialRepresentation_Type")
@XmlRootElement(name = "AbstractMD_SpatialRepresentation")
@XmlSeeAlso({
    DefaultGridSpatialRepresentation.class,
    DefaultVectorSpatialRepresentation.class
})
public class AbstractSpatialRepresentation extends ISOMetadata implements SpatialRepresentation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2238840586154687777L;

    /**
     * Level and extent of the spatial representation.
     */
    @SuppressWarnings("serial")
    private Scope scope;

    /**
     * Constructs an initially empty spatial representation.
     */
    public AbstractSpatialRepresentation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(SpatialRepresentation)
     */
    public AbstractSpatialRepresentation(final SpatialRepresentation object) {
        super(object);
        if (object instanceof AbstractSpatialRepresentation) {
            scope = ((AbstractSpatialRepresentation) object).getScope();
        }
    }

    /**
     * Returns the level and extent of the spatial representation.
     *
     * @return level and extent of the spatial representation, or {@code null} if none.
     *
     * @since 1.3
     */
    @XmlElement(name = "scope")
    @XmlJavaTypeAdapter(MD_Scope.Since2014.class)
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the level and extent of the spatial representation.
     *
     * @param  newValue  the new type of resource.
     *
     * @since 1.3
     */
    public void setScope(final Scope newValue) {
        checkWritePermission(scope);
        scope = newValue;
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link VectorSpatialRepresentation} or
     *       {@link GridSpatialRepresentation}, then this method delegates to the {@code castOrCopy(…)}
     *       method of the corresponding SIS subclass. Note that if the given object implements
     *       more than one of the above-cited interfaces, then the {@code castOrCopy(…)} method
     *       to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractSpatialRepresentation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractSpatialRepresentation} instance is created using the
     *       {@linkplain #AbstractSpatialRepresentation(SpatialRepresentation) copy constructor}
     *       and returned. Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractSpatialRepresentation castOrCopy(final SpatialRepresentation object) {
        if (object instanceof GridSpatialRepresentation) {
            return DefaultGridSpatialRepresentation.castOrCopy((GridSpatialRepresentation) object);
        }
        if (object instanceof VectorSpatialRepresentation) {
            return DefaultVectorSpatialRepresentation.castOrCopy((VectorSpatialRepresentation) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractSpatialRepresentation) {
            return (AbstractSpatialRepresentation) object;
        }
        return new AbstractSpatialRepresentation(object);
    }
}
