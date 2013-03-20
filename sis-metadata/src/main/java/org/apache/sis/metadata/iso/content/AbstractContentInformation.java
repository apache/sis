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
 *
 * This package contains documentation from OGC specifications.
 * Open Geospatial Consortium's work is fully acknowledged here.
 */
package org.apache.sis.metadata.iso.content;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Description of the content of a dataset.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "AbstractMD_ContentInformation_Type")
@XmlRootElement(name = "MD_ContentInformation")
@XmlSeeAlso({
    DefaultCoverageDescription.class,
    DefaultFeatureCatalogueDescription.class
})
public class AbstractContentInformation extends ISOMetadata implements ContentInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1609535650982322560L;

    /**
     * Constructs an initially empty content information.
     */
    public AbstractContentInformation() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * <p>This method checks for the {@link CoverageDescription} and {@link FeatureCatalogueDescription}
     * sub-interfaces. If one of those interfaces is found, then this method delegates to
     * the corresponding {@code castOrCopy} static method. If the given object implements more
     * than one of the above-cited interfaces, then the {@code castOrCopy} method to be used is
     * unspecified.</p>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractContentInformation castOrCopy(final ContentInformation object) {
        if (object instanceof CoverageDescription) {
            return DefaultCoverageDescription.castOrCopy((CoverageDescription) object);
        }
        if (object instanceof FeatureCatalogueDescription) {
            return DefaultFeatureCatalogueDescription.castOrCopy((FeatureCatalogueDescription) object);
        }
        if (object == null || object instanceof AbstractContentInformation) {
            return (AbstractContentInformation) object;
        }
        final AbstractContentInformation copy = new AbstractContentInformation();
        copy.shallowCopy(object);
        return copy;
    }
}
