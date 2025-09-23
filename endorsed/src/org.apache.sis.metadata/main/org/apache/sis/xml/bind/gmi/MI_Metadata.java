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
package org.apache.sis.xml.bind.gmi;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Metadata;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


/**
 * A wrapper for a metadata using the {@code "gmi"} namespace.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlType(name = "MI_Metadata_Type", namespace = LegacyNamespaces.GMI)
@XmlRootElement(name = "MI_Metadata", namespace = LegacyNamespaces.GMI)
public class MI_Metadata extends DefaultMetadata {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2785777463171443407L;

    /**
     * Creates an initially empty metadata.
     * This is also the default constructor used by JAXB.
     */
    public MI_Metadata() {
    }

    /**
     * Creates a new metadata as a copy of the given one.
     * This is a shallow copy constructor.
     *
     * @param  original  the original metadata to copy.
     */
    public MI_Metadata(final Metadata original) {
        super(original);
    }

    /**
     * Wraps the given metadata into a SIS implementation that can be marshalled,
     * using the {@code "gmi"} namespace if necessary.
     *
     * @param  original  the original metadata provided by the user.
     * @return the metadata to marshal.
     */
    public static DefaultMetadata castOrCopy(final Metadata original) {
        if (original != null && !(original instanceof MI_Metadata)) {
            if (!isNullOrEmpty(original.getAcquisitionInformation())) {
                return new MI_Metadata(original);
            }
        }
        return DefaultMetadata.castOrCopy(original);
    }
}
