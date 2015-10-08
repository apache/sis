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
package org.apache.sis.internal.jaxb.gml;

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;


/**
 * Base class for GML objects that are wrappers around a GeoAPI implementation.
 * Every GML object to be marshalled have an ID attribute, which is mandatory.
 * If no ID is explicitely set, a default one will be created from the wrapped object.
 *
 * <div class="note"><b>Note:</b>
 * This class is somewhat temporary. It assigns the ID to the <em>wrapped</em> object.
 * In a future SIS version, we should assign the ID to the object itself.</div>
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@XmlTransient
public abstract class GMLAdapter {
    /**
     * The period identifier, or {@code null} if undefined.
     * This element is part of GML 3.1.1 specification.
     *
     * <div class="section">Difference between {@code gmd:uuid} and {@code gml:id}</div>
     * <ul>
     *   <li>{@code id} is a standard <strong>GML</strong> attribute available on every
     *       object-with-identity. It has type={@code "xs:ID"} - i.e. it is a fragment
     *       identifier, unique within document scope only, for internal cross-references.
     *       It is not useful by itself as a persistent unique identifier.</li>
     *   <li>{@code uuid} is an optional attribute available on every object-with-identity,
     *       provided in the <strong>GMD</strong> schemas that implement ISO 19115 in XML.
     *       May be used as a persistent unique identifier, but only available within GMD
     *       context.</li>
     * </ul>
     *
     * @see <a href="https://www.seegrid.csiro.au/wiki/bin/view/AppSchemas/GmlIdentifiers">GML identifiers</a>
     * @see org.apache.sis.internal.jaxb.gco.PropertyType#getUUIDREF()
     */
    @XmlID
    @XmlAttribute(namespace = Namespaces.GML, required = true)
    private String id;

    /**
     * Creates a new GML object with no ID.
     * This constructor is typically invoked at unmarshalling time.
     * The {@link #id} value will then be set by JAXB.
     *
     * @see #copyIdTo(Object)
     */
    protected GMLAdapter() {
    }

    /**
     * Creates a new GML object wrapping the given GeoAPI implementation.
     * The ID will be determined from the given object.
     *
     * <p>This constructor is typically invoked at marshalling time. The {@link #id}
     * value set by this constructor will be used by JAXB for producing the XML.</p>
     *
     * @param wrapped An instance of a GeoAPI interface to be wrapped.
     */
    protected GMLAdapter(final Object wrapped) {
        if (wrapped instanceof IdentifiedObject) {
            final IdentifierMap map = ((IdentifiedObject) wrapped).getIdentifierMap();
            if (map != null) { // Should not be null, but let be safe.
                id = map.get(IdentifierSpace.ID);
            }
        }
    }

    /**
     * Assigns the {@link #id} value (if non-null) to the given object. This method
     * is typically invoked at unmarshalling time in order to assign the ID of this
     * temporary wrapper to the "real" GeoAPI implementation instance.
     *
     * @param wrapped The GeoAPI implementation for which to assign the ID.
     */
    public final void copyIdTo(final Object wrapped) {
        if (id != null && wrapped instanceof IdentifiedObject) {
            final IdentifierMap map = ((IdentifiedObject) wrapped).getIdentifierMap();
            if (map != null) { // Should not be null, but let be safe.
                map.put(IdentifierSpace.ID, id);
            }
        }
    }
}
