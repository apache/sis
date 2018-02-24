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
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.util.iso.Types;


/**
 * Information about the scope of the resource.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_MetadataScope}
 * {@code   └─resourceScope……} Resource scope</div>
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
 * @version 1.0
 * @since   0.5
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@XmlType(name = "MD_MetadataScope_Type", propOrder = {
    "resourceScope",
    "name"
})
@XmlRootElement(name = "MD_MetadataScope")
public class DefaultMetadataScope extends ISOMetadata implements MetadataScope {
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
     * @param resourceScope  code for the scope.
     * @param name           description of the scope, or {@code null} if none.
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
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(MetadataScope)
     */
    public DefaultMetadataScope(final MetadataScope object) {
        super(object);
        if (object != null) {
            resourceScope = object.getResourceScope();
            name          = object.getName();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultMetadataScope}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultMetadataScope} instance is created using the
     *       {@linkplain #DefaultMetadataScope(MetadataScope) copy constructor} and returned.
     *       Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultMetadataScope castOrCopy(final MetadataScope object) {
        if (object == null || object instanceof DefaultMetadataScope) {
            return (DefaultMetadataScope) object;
        }
        return new DefaultMetadataScope(object);
    }

    /**
     * Returns the code for the scope.
     *
     * @return the code for the scope.
     */
    @Override
    @XmlElement(name = "resourceScope", required = true)
    public ScopeCode getResourceScope() {
        return resourceScope;
    }

    /**
     * Sets the code for the scope.
     *
     * @param  newValue  the new code for the scope.
     */
    public void setResourceScope(final ScopeCode newValue) {
        checkWritePermission();
        resourceScope = newValue;
    }

    /**
     * Returns a description of the scope, or {@code null} if none.
     *
     * @return description of the scope, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "name")
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the description of the scope.
     *
     * @param  newValue  the new description of the scope.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }
}
