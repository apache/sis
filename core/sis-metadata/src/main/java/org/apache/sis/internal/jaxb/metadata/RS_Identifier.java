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
package org.apache.sis.internal.jaxb.metadata;

import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping the GeoAPI {@link ReferenceIdentifier} to an implementation class that can
 * be marshalled. See the package documentation for more information about JAXB and interfaces.
 *
 * <p>The XML produced by this adapter shall be compliant to the ISO 19139 syntax.</p>
 *
 * Note that a class of the same name is defined in the {@link org.apache.sis.internal.jaxb.referencing}
 * package, which serves the same purpose (wrapping exactly the same interface) but using the GML syntax
 * instead.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class RS_Identifier extends PropertyType<RS_Identifier, ReferenceIdentifier> {
    /**
     * Empty constructor for JAXB only.
     */
    public RS_Identifier() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code ReferenceIdentifier.class}
     */
    @Override
    protected Class<ReferenceIdentifier> getBoundType() {
        return ReferenceIdentifier.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private RS_Identifier(final ReferenceIdentifier metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <gmd:RS_Identifier>} XML element.
     *
     * @param  metadata The metadata element to marshall.
     * @return A {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected RS_Identifier wrap(ReferenceIdentifier metadata) {
        return new RS_Identifier(metadata);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <gmd:RS_Identifier>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElementRef
    public ImmutableIdentifier getElement() {
        final ReferenceIdentifier metadata = this.metadata;
        if (metadata == null) {
            return null;
        } else if (metadata instanceof ImmutableIdentifier) {
            return (ImmutableIdentifier) metadata;
        } else {
            return new ImmutableIdentifier(metadata);
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setElement(final ImmutableIdentifier metadata) {
        this.metadata = metadata;
    }
}
