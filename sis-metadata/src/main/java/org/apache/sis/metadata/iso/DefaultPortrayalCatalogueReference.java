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
package org.apache.sis.metadata.iso;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.PortrayalCatalogueReference;


/**
 * Information identifying the portrayal catalogue used.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_PortrayalCatalogueReference_Type")
@XmlRootElement(name = "MD_PortrayalCatalogueReference")
public class DefaultPortrayalCatalogueReference extends ISOMetadata
        implements PortrayalCatalogueReference
{
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -3095277682987563157L;

    /**
     * Bibliographic reference to the portrayal catalogue cited.
     */
    private Collection<Citation> portrayalCatalogueCitations;

    /**
     * Construct an initially empty portrayal catalogue reference.
     */
    public DefaultPortrayalCatalogueReference() {
    }

    /**
     * Creates a portrayal catalogue reference initialized to the given reference.
     *
     * @param portrayalCatalogueCitation The bibliographic reference, or {@code null} if none.
     */
    public DefaultPortrayalCatalogueReference(final Citation portrayalCatalogueCitation) {
        portrayalCatalogueCitations = singleton(Citation.class, portrayalCatalogueCitation);
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultPortrayalCatalogueReference castOrCopy(final PortrayalCatalogueReference object) {
        if (object == null || object instanceof DefaultPortrayalCatalogueReference) {
            return (DefaultPortrayalCatalogueReference) object;
        }
        final DefaultPortrayalCatalogueReference copy = new DefaultPortrayalCatalogueReference();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Bibliographic reference to the portrayal catalogue cited.
     */
    @Override
    @XmlElement(name = "portrayalCatalogueCitation", required = true)
    public synchronized Collection<Citation> getPortrayalCatalogueCitations() {
        return portrayalCatalogueCitations = nonNullCollection(portrayalCatalogueCitations, Citation.class);
    }

    /**
     * Sets bibliographic reference to the portrayal catalogue cited.
     *
     * @param newValues The new portrayal catalogue citations.
     */
    public synchronized void setPortrayalCatalogueCitations(Collection<? extends Citation> newValues) {
        portrayalCatalogueCitations = copyCollection(newValues, portrayalCatalogueCitations, Citation.class);
    }
}
