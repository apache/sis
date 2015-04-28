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
import org.opengis.metadata.spatial.Georectified;
import org.apache.sis.metadata.iso.spatial.DefaultGeorectified;

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
@XmlType(name = "MI_Georectified_Type")
@XmlRootElement(name = "MI_Georectified")
public class MI_Georectified extends DefaultGeorectified {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7518820483760523541L;

    /**
     * Creates an initially empty metadata.
     * This is also the default constructor used by JAXB.
     */
    public MI_Georectified() {
    }

    /**
     * Creates a new metadata as a copy of the given one.
     * This is a shallow copy constructor.
     *
     * @param original The original metadata to copy.
     */
    public MI_Georectified(final Georectified original) {
        super(original);
    }

    /**
     * Wraps the given metadata into a SIS implementation that can be marshalled,
     * using the {@code "gmi"} namespace if necessary.
     *
     * @param  original The original metadata provided by the user.
     * @return The metadata to marshall.
     */
    public static DefaultGeorectified castOrCopy(final Georectified original) {
        if (original != null && !(original instanceof MI_Georectified)) {
            if (!isNullOrEmpty(original.getCheckPoints())) {
                return new MI_Georectified(original);
            }
        }
        return DefaultGeorectified.castOrCopy(original);
    }
}
