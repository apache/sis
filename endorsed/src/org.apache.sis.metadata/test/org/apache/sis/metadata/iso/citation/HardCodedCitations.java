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
package org.apache.sis.metadata.iso.citation;

import java.net.URI;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.URLs;

// Specific to the main and geoapi-3.1 branches:
import static java.util.Collections.singleton;


/**
 * Hard-coded citation constants used for testing purpose only.
 * We use those hard-coded constants instead of the ones defined in the
 * {@link org.apache.sis.metadata.iso.citation.Citations} class in order
 * to protect the test suite against any change in the definition of the
 * above-cited public constants.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class HardCodedCitations {
    /**
     * The ISO 19111 standard.
     */
    public static final DefaultCitation ISO_19111;
    static {
        final DefaultCitation c = new DefaultCitation("Spatial referencing by coordinates");
        c.getAlternateTitles().add(new SimpleInternationalString("ISO 19111"));
        c.getIdentifiers().add(new DefaultIdentifier("ISO", "19111", null));
        c.getPresentationForms().add(PresentationForm.DOCUMENT_DIGITAL);
        c.transitionTo(DefaultCitation.State.FINAL);
        ISO_19111 = c;
    }

    /**
     * The ISO 19115 standard.
     */
    public static final DefaultCitation ISO_19115;
    static {
        final DefaultCitation c = new DefaultCitation("ISO 19115");
        c.getPresentationForms().add(PresentationForm.DOCUMENT_DIGITAL);
        c.transitionTo(DefaultCitation.State.FINAL);
        ISO_19115 = c;
    }

    /**
     * The <a href="https://epsg.org/">EPSG Geodetic Parameter Dataset</a> authority.
     * This citation contains the "EPSG" {@linkplain Citation#getIdentifiers() identifier}.
     *
     * <p>String representation:</p>
     *
     * <pre class="text">
     *   Citation
     *     ├─Title………………………………………………………… EPSG Geodetic Parameter Dataset
     *     ├─Identifier
     *     │   └─Code………………………………………………… EPSG
     *     ├─Cited responsible party
     *     │   ├─Party
     *     │   │   ├─Name……………………………………… International Association of Oil &amp; Gas Producers
     *     │   │   └─Contact info
     *     │   │       └─Online resource
     *     │   │           ├─Linkage………… https://epsg.org/
     *     │   │           └─Function……… Information
     *     │   └─Role………………………………………………… Principal investigator
     *     └─Presentation form………………………… Table digital</pre>
     */
    public static final DefaultCitation EPSG;
    static {
        final DefaultOnlineResource r = new DefaultOnlineResource(URI.create(URLs.EPSG));
        r.setFunction(OnLineFunction.INFORMATION);

        final DefaultResponsibleParty p = new DefaultResponsibleParty(Role.PRINCIPAL_INVESTIGATOR);
        p.setParties(singleton(new DefaultOrganisation("International Association of Oil & Gas Producers",
                null, null, new DefaultContact(r))));

        final DefaultCitation c = new DefaultCitation("EPSG Geodetic Parameter Dataset");
        c.getPresentationForms().add(PresentationForm.TABLE_DIGITAL);
        c.getIdentifiers().add(new DefaultIdentifier(Constants.EPSG));
        c.getCitedResponsibleParties().add(p);
        c.transitionTo(DefaultCitation.State.FINAL);
        EPSG = c;
    }

    /**
     * Codespace for objects specific to <a href="https://sis.apache.org">Apache SIS</a>.
     */
    public static final DefaultCitation SIS;
    static {
        final DefaultCitation c = new DefaultCitation(Constants.SIS);
        c.transitionTo(DefaultCitation.State.FINAL);
        SIS = c;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedCitations() {
    }
}
