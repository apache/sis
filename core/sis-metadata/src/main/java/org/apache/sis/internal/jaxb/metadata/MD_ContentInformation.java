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
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.ImageDescription;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.jaxb.gmi.MI_ImageDescription;
import org.apache.sis.internal.jaxb.gmi.MI_CoverageDescription;
import org.apache.sis.metadata.iso.content.AbstractContentInformation;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
public final class MD_ContentInformation extends
        PropertyType<MD_ContentInformation, ContentInformation>
{
    /**
     * Empty constructor for JAXB only.
     */
    public MD_ContentInformation() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code ContentInformation.class}
     */
    @Override
    protected Class<ContentInformation> getBoundType() {
        return ContentInformation.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private MD_ContentInformation(final ContentInformation value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <mrc:MD_ContentInformation>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected MD_ContentInformation wrap(final ContentInformation value) {
        return new MD_ContentInformation(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <mrc:MD_ContentInformation>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public AbstractContentInformation getElement() {
        final ContentInformation metadata = this.metadata;
        if (metadata instanceof ImageDescription) {
            return MI_ImageDescription.castOrCopy((ImageDescription) metadata);
        }
        if (metadata instanceof CoverageDescription) {
            return MI_CoverageDescription.castOrCopy((CoverageDescription) metadata);
        }
        return AbstractContentInformation.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public void setElement(final AbstractContentInformation value) {
        metadata = value;
    }
}
