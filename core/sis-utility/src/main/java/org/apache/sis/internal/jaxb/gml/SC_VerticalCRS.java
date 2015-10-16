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

import javax.xml.bind.annotation.XmlAnyElement;
import org.opengis.referencing.crs.VerticalCRS;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * JAXB adapter for {@link VerticalCRS}, in order to integrate the value in an element
 * complying with OGC/ISO standard. Note that the CRS is formatted using the GML schema,
 * not the ISO 19139 one.
 *
 * <p>This wrapper does not declare directly the XML element, because doing so would require
 * the implementation classes in the {@code sis-referencing} module. Instead, this wrapper
 * declares an {@code Object} property annotated with {@code XmlAnyElement}, with a default
 * implementation returning {@code null}. Modules capable to provide an instance shall create
 * a subclass like below:</p>
 *
 * {@preformat java
 *     public final class MyClass extends SC_VerticalCRS implements AdapterReplacement {
 *         &#64;Override
 *         public void register(final Marshaller marshaller) {
 *             marshaller.setAdapter(SC_VerticalCRS.class, this);
 *         }
 *
 *         &#64;Override
 *         public DefaultVerticalCRS getElement() {
 *             return skip() ? null : DefaultVerticalCRS.castOrCopy(metadata);
 *         }
 *     }
 * }
 *
 * Next, the module shall provides the following:
 * <ul>
 *   <li>The path to {@code MyClass} shall be provided in the module
 *       {@code META-INF/services/org.apache.sis.internal.jaxb.AdapterReplacement} file.</li>
 *   <li>The {@code DefaultVerticalCRS} class shall have the
 *       {@code XmlRootElement(name = "VerticalCRS")} annotation.</li>
 *   <li>The {@code DefaultVerticalCRS} class shall be declared by a
 *       {@link org.apache.sis.internal.jaxb.TypeRegistration} implementation provided by the module.</li>
 * </ul>
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.internal.jaxb.AdapterReplacement
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
     * @param metadata The metadata value to marshal.
     */
    protected SC_VerticalCRS(final VerticalCRS metadata) {
        super(metadata);
    }

    /**
     * Returns the Vertical CRS value wrapped by a {@code <gml:VerticalCRS>} element.
     *
     * @param value The value to marshal.
     * @return The adapter which wraps the metadata value.
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
     * {@preformat java
     *   return skip() ? null : DefaultVerticalCRS.castOrCopy(metadata);
     * }
     *
     * @return The metadata to be marshalled.
     */
    @XmlAnyElement(lax = true)
    public Object getElement() {
        Context.warningOccured(Context.current(), SC_VerticalCRS.class, "getElement",
                Errors.class, Errors.Keys.MissingRequiredModule_1, "sis-referencing");
        return null;
    }

    /**
     * Sets the value for the given {@code DefaultVerticalCRS}. If the given value is an instance
     * of {@link VerticalCRS}, then this method assigns that value to the {@link #metadata} field.
     * Otherwise this method does nothing.
     *
     * @param crs The unmarshalled metadata.
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
