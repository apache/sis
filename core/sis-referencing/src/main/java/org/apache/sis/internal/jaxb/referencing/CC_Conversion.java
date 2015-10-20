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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.annotation.XmlElement;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.referencing.operation.DefaultConversion;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public final class CC_Conversion extends PropertyType<CC_Conversion, Conversion> {
    /**
     * Temporary storage for the {@code baseCRS} during {@link org.apache.sis.referencing.crs.AbstractDerivedCRS}
     * unmarshalling. A temporary location is needed because {@code AbstractDerivedCRS} does not have any explicit
     * field for {@code baseCRS}.
     *
     * @see #setBaseCRS(Conversion, SingleCRS)
     */
    private SingleCRS baseCRS;

    /**
     * Empty constructor for JAXB only.
     */
    public CC_Conversion() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code Conversion.class}
     */
    @Override
    protected Class<Conversion> getBoundType() {
        return Conversion.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CC_Conversion(final Conversion conversion) {
        super(conversion);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:Conversion>} XML element.
     *
     * @param  conversion The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CC_Conversion wrap(final Conversion conversion) {
        return new CC_Conversion(conversion);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:Conversion>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElement(name = "Conversion")
    public DefaultConversion getElement() {
        return DefaultConversion.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param conversion The unmarshalled element.
     */
    public void setElement(final DefaultConversion conversion) {
        metadata = conversion;
        Context.setWrapper(Context.current(), this);
        if (conversion.getMethod() == null) incomplete("method");
    }

    /**
     * Temporarily stores the {@code baseCRS} associated to the given {@code Conversion}.  This temporary storage is
     * needed because {@code org.apache.sis.referencing.crs.AbstractDerivedCRS} does not have any explicit field for
     * {@code baseCRS}. Instead the base CRS is stored in {@link Conversion#getSourceCRS()}, but we can set this
     * property only after the {@code DerivedCRS} coordinate system has been unmarshalled.
     *
     * See {@code AbstractDerivedCRS.afterUnmarshal(Unmarshaller, Object parent)} for more information.
     *
     * @param  conversion The conversion to which to associate a base CRS.
     * @param  crs The base CRS to associate to the given conversion.
     * @return The previous base CRS, or {@code null} if none.
     */
    public static SingleCRS setBaseCRS(final Conversion conversion, final SingleCRS crs) {
        /*
         * Implementation note: we store the base CRS in the marshalling context because:
         *
         *   - we want to keep each thread isolated (using ThreadLocal), and
         *   - we want to make sure that the reference is disposed even if the unmarshaller throws an exception.
         *     This is guaranteed because the Context is disposed by Apache SIS in "try … finally" constructs.
         */
        final PropertyType<?,?> wrapper = Context.getWrapper(Context.current());
        if (wrapper instanceof CC_Conversion) {
            final CC_Conversion c = (CC_Conversion) wrapper;
            if (c.getElement() == conversion) {  // For making sure that we do not confuse with another conversion.
                final SingleCRS previous = c.baseCRS;
                c.baseCRS = crs;
                return previous;
            }
        }
        return null;
    }
}
