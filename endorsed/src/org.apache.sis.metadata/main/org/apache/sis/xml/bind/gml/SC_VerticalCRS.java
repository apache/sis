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
package org.apache.sis.xml.bind.gml;

import jakarta.xml.bind.annotation.XmlAnyElement;
import org.opengis.referencing.crs.VerticalCRS;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gco.PropertyType;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * JAXB adapter for {@link VerticalCRS}, in order to integrate the value in an element
 * complying with OGC/ISO standard. Note that the CRS is formatted using the GML schema,
 * not the ISO 19139:2007 one.
 *
 * <p>This wrapper does not declare directly the XML element, because doing so would
 * require the implementation classes in the {@code org.apache.sis.referencing} module.
 * Instead, this wrapper declares an {@code Object} property annotated with {@code XmlAnyElement},
 * with a default implementation returning {@code null}.
 * Modules capable to provide an instance shall create a subclass like below:</p>
 *
 * {@snippet lang="java" :
 * public final class MyClass extends SC_VerticalCRS implements AdapterReplacement {
 *     @Override
 *     public void register(final Marshaller marshaller) {
 *         marshaller.setAdapter(SC_VerticalCRS.class, this);
 *     }
 *
 *     @Override
 *     public DefaultVerticalCRS getElement() {
 *         return skip() ? null : DefaultVerticalCRS.castOrCopy(metadata);
 *     }
 * }
 * }
 *
 * Next, the module shall provide the following:
 * <ul>
 *   <li>The path to {@code MyClass} shall be provided in the {@code module-info.java} file
 *       as a {@code org.apache.sis.xml.bind.AdapterReplacement} service.</li>
 *   <li>The {@code DefaultVerticalCRS} class shall have the
 *       {@code XmlRootElement(name = "VerticalCRS")} annotation.</li>
 *   <li>The {@code DefaultVerticalCRS} class shall be declared by a
 *       {@link org.apache.sis.xml.bind.TypeRegistration} implementation provided by the module.</li>
 * </ul>
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.xml.bind.AdapterReplacement
 */
public class SC_VerticalCRS extends PropertyType<SC_VerticalCRS, VerticalCRS> {
    /**
     * Empty constructor for JAXB only.
     */
    public SC_VerticalCRS() {
    }

    /**
     * Wraps a Vertical CRS value with a {@code <gml:VerticalCRS>} element at marshalling-time.
     *
     * @param  metadata  the metadata value to marshal.
     */
    protected SC_VerticalCRS(final VerticalCRS metadata) {
        super(metadata);
    }

    /**
     * Returns the Vertical CRS value wrapped by a {@code <gml:VerticalCRS>} element.
     *
     * @param  value  the value to marshal.
     * @return the adapter which wraps the metadata value.
     */
    @Override
    protected SC_VerticalCRS wrap(final VerticalCRS value) {
        return new SC_VerticalCRS(value);
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     *
     * @return {@code VerticalCRS.class}
     */
    @Override
    protected final Class<VerticalCRS> getBoundType() {
        return VerticalCRS.class;
    }

    /**
     * Returns the {@code DefaultVerticalCRS} generated from the metadata value.
     * The default implementation returns {@code null}. Subclasses shall override
     * this method like below:
     *
     * {@snippet lang="java" :
     *     return skip() ? null : DefaultVerticalCRS.castOrCopy(metadata);
     *     }
     *
     * @return the metadata to be marshalled.
     */
    @XmlAnyElement(lax = true)
    public Object getElement() {
        Context.warningOccured(Context.current(), SC_VerticalCRS.class, "getElement",
                Errors.class, Errors.Keys.MissingRequiredModule_1, Modules.REFERENCING);
        return null;
    }

    /**
     * Sets the value for the given {@code DefaultVerticalCRS}. If the given value is an instance
     * of {@link VerticalCRS}, then this method assigns that value to the {@link #metadata} field.
     * Otherwise this method does nothing.
     *
     * @param  crs  the unmarshalled metadata.
     */
    public final void setElement(final Object crs) {
        if (crs instanceof VerticalCRS) {
            metadata = (VerticalCRS) crs;
            if (metadata.getCoordinateSystem() == null) incomplete("coordinateSystem");
            if (metadata.getDatum()            == null) incomplete("verticalDatum");
        } else {
            Context.warningOccured(Context.current(), SC_VerticalCRS.class, "setElement", Errors.class,
                    Errors.Keys.UnexpectedValueInElement_2, "verticalCRS", Classes.getShortClassName(crs));
        }
    }
}
