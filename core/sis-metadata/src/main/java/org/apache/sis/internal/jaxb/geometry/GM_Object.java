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
package org.apache.sis.internal.jaxb.geometry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.geometry.Geometry;
import org.apache.sis.xml.Namespaces;


/**
 * JAXB adapter for {@link Geometry}, in order to integrate the value in an element complying with OGC/ISO standard.
 * The geometry element names are usually prefixed by {@code gml:}.
 *
 * <p>The default implementation does almost nothing. The geometry objects will <strong>not</strong>
 * create the expected {@link JAXBElement} type. This class is only a hook to be extended by more
 * specialized subclasses in GML modules.</p>
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class GM_Object extends XmlAdapter<GM_Object, Geometry> {
    /**
     * The Geometry value covered by a {@code gml:**} element.
     */
    @XmlElementRef(name = "AbstractGeometry", namespace = Namespaces.GML, type = JAXBElement.class)
    protected JAXBElement<? extends Geometry> geometry;

    /**
     * Empty constructor for JAXB and subclasses only.
     */
    public GM_Object() {
    }

    /**
     * Converts an adapter read from an XML stream to the GeoAPI interface which will
     * contains this value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param value The adapter for a geometry value.
     * @return An instance of the GeoAPI interface which represents the geometry value.
     */
    @Override
    public final Geometry unmarshal(final GM_Object value) {
        if (value != null) {
            final JAXBElement<? extends Geometry> g = value.geometry;
            if (g != null) {
                return g.getValue();
            }
        }
        return null;
    }

    /**
     * Converts a GeoAPI interface to the appropriate adapter for the way it will be
     * marshalled into an XML file or stream. JAXB calls automatically this method at
     * marshalling time.
     *
     * @param value The geometry value, here the interface.
     * @return The adapter for the given value.
     */
    @Override
    public final GM_Object marshal(final Geometry value) {
        if (value == null) {
            return null;
        }
        return wrap(value);
    }

    /**
     * Returns the geometry value to be covered by a {@code gml:**} element.
     * The default implementation returns {@code null} if all cases. Subclasses
     * must override this method in order to provide useful marshalling.
     *
     * @param value The value to marshal.
     * @return The adapter which covers the geometry value.
     */
    protected GM_Object wrap(Geometry value) {
        return null;
    }
}
