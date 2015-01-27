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
import org.opengis.metadata.lineage.Source;
import org.apache.sis.metadata.iso.lineage.DefaultSource;


/**
 * A wrapper for a metadata using the {@code "gmi"} namespace.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "LE_Source_Type")
@XmlRootElement(name = "LE_Source")
public class LE_Source extends DefaultSource {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4284654104287846003L;

    /**
     * Creates an initially empty metadata.
     * This is also the default constructor used by JAXB.
     */
    public LE_Source() {
    }

    /**
     * Creates a new metadata as a copy of the given one.
     * This is a shallow copy constructor.
     *
     * @param original The original metadata to copy.
     */
    public LE_Source(final Source original) {
        super(original);
    }

    /**
     * Wraps the given metadata into a SIS implementation that can be marshalled,
     * using the {@code "gmi"} namespace if necessary.
     *
     * @param  original The original metadata provided by the user.
     * @return The metadata to marshall.
     */
    public static DefaultSource castOrCopy(final Source original) {
        if (original != null && !(original instanceof LE_Source)) {
            if (original.getProcessedLevel() != null || original.getResolution() != null) {
                return new LE_Source(original);
            }
        }
        return DefaultSource.castOrCopy(original);
    }
}
