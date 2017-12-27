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
import javax.xml.bind.annotation.XmlElementRefs;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public class MD_Identifier extends PropertyType<MD_Identifier, Identifier> {
    /**
     * Empty constructor for JAXB only.
     */
    MD_Identifier() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code Identifier.class}
     */
    @Override
    protected final Class<Identifier> getBoundType() {
        return Identifier.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private MD_Identifier(final Identifier metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <gmd:MD_Identifier>} XML element.
     *
     * @param  metadata  the metadata element to marshall.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected MD_Identifier wrap(final Identifier metadata) {
        return new MD_Identifier(metadata);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <gmd:MD_Identifier>} or {@code RS_Identifier} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRefs({
        @XmlElementRef(type = DefaultIdentifier.class),
        @XmlElementRef(type = ImmutableIdentifier.class)
    })
    @SuppressWarnings("deprecation")
    public final Identifier getElement() {
        if (metadata instanceof ImmutableIdentifier) {
            return (ImmutableIdentifier) metadata;
        }
        if (metadata instanceof ReferenceIdentifier) {
            return ImmutableIdentifier.castOrCopy(metadata);
        }
        return DefaultIdentifier.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled metadata.
     */
    public final void setElement(final Identifier metadata) {
        this.metadata = metadata;
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends MD_Identifier {
        /** Empty constructor used only by JAXB. */
        private Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected MD_Identifier wrap(final Identifier value) {
            return accept2014() ? super.wrap(value) : null;
        }
    }
}
