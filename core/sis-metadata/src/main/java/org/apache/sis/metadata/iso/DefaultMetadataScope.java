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
import org.opengis.util.InternationalString;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.util.iso.Types;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the scope of the resource.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code MetadataScope} interface.
 * </div>
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_MetadataScope_Type", propOrder = {
    "resourceScope",
    "name"
})
@XmlRootElement(name = "MD_MetadataScope")
@UML(identifier="MD_MetadataScope", specification=ISO_19115)
public class DefaultMetadataScope extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7186722085106176683L;

    /**
     * Code for the scope.
     */
    private ScopeCode resourceScope;

    /**
     * Description of the scope.
     */
    private InternationalString name;

    /**
     * Constructs an initially empty metadata scope.
     */
    public DefaultMetadataScope() {
    }

    /**
     * Constructs a metadata scope initialized to the given value.
     *
     * @param resourceScope code for the scope.
     * @param name Description of the scope, or {@code null} if none.
     */
    public DefaultMetadataScope(final ScopeCode resourceScope, final CharSequence name) {
        this.resourceScope = resourceScope;
        this.name = Types.toInternationalString(name);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public DefaultMetadataScope(final DefaultMetadataScope object) {
        super(object);
        if (object != null) {
            resourceScope = object.getResourceScope();
            name          = object.getName();
        }
    }

    /**
     * Return the code for the scope.
     *
     * @return The ode for the scope.
     */
    @XmlElement(name = "resourceScope", required = true)
    @UML(identifier="resourceScope", obligation=MANDATORY, specification=ISO_19115)
    public ScopeCode getResourceScope() {
        return resourceScope;
    }

    /**
     * Sets the code for the scope.
     *
     * @param newValue The new code for the scope.
     */
    public void setResourceScope(final ScopeCode newValue) {
        checkWritePermission();
        resourceScope = newValue;
    }

    /**
     * Return a description of the scope, or {@code null} if none.
     *
     * @return Description of the scope, or {@code null} if none.
     */
    @XmlElement(name = "name")
    @UML(identifier="name", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the description of the scope.
     *
     * @param newValue The new description of the scope.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }
}
