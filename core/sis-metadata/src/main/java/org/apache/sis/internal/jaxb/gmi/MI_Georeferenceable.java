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
import org.opengis.metadata.spatial.Georeferenceable;
import org.apache.sis.metadata.iso.spatial.DefaultGeoreferenceable;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


/**
 * A wrapper for a metadata using the {@code "gmi"} namespace.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MI_Georeferenceable_Type")
@XmlRootElement(name = "MI_Georeferenceable")
public class MI_Georeferenceable  extends DefaultGeoreferenceable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4709694415908530851L;

    /**
     * Creates an initially empty metadata.
     * This is also the default constructor used by JAXB.
     */
    public MI_Georeferenceable() {
    }

    /**
     * Creates a new metadata as a copy of the given one.
     * This is a shallow copy constructor.
     *
     * @param original The original metadata to copy.
     */
    public MI_Georeferenceable(final Georeferenceable original) {
        super(original);
    }

    /**
     * Wraps the given metadata into a SIS implementation that can be marshalled,
     * using the {@code "gmi"} namespace if necessary.
     *
     * @param  original The original metadata provided by the user.
     * @return The metadata to marshall.
     */
    public static DefaultGeoreferenceable castOrCopy(final Georeferenceable original) {
        if (original != null && !(original instanceof MI_Georeferenceable)) {
            if (!isNullOrEmpty(original.getGeolocationInformation())) {
                return new MI_Georeferenceable(original);
            }
        }
        return DefaultGeoreferenceable.castOrCopy(original);
    }
}
