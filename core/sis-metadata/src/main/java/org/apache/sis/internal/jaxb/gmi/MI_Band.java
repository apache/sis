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
package org.apache.sis.internal.jaxb.gmi;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.content.Band;
import org.apache.sis.metadata.iso.content.DefaultBand;
import org.apache.sis.xml.Namespaces;


/**
 * A wrapper for a metadata using the {@code "gmi"} namespace.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@XmlType(name = "MI_Band_Type", namespace = Namespaces.MRC)
@XmlRootElement(name = "MI_Band", namespace = Namespaces.MRC)
public class MI_Band extends DefaultBand {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6839213923457158942L;

    /**
     * Creates an initially empty metadata.
     * This is also the default constructor used by JAXB.
     */
    public MI_Band() {
    }

    /**
     * Creates a new metadata as a copy of the given one.
     * This is a shallow copy constructor.
     *
     * @param original  the original metadata to copy.
     */
    public MI_Band(final Band original) {
        super(original);
    }

    /**
     * Wraps the given metadata into a SIS implementation that can be marshalled,
     * using the {@code "gmi"} namespace if necessary.
     *
     * @param  original  the original metadata provided by the user.
     * @return the metadata to marshal.
     */
    public static DefaultBand castOrCopy(final Band original) {
        if (original != null && !(original instanceof MI_Band)) {
            if (original.getBandBoundaryDefinition()   != null ||
                original.getNominalSpatialResolution() != null ||
                original.getTransferFunctionType()     != null ||
                original.getTransmittedPolarisation()  != null ||
                original.getDetectedPolarisation()     != null)
            {
                return new MI_Band(original);
            }
        }
        return DefaultBand.castOrCopy(original);
    }
}
