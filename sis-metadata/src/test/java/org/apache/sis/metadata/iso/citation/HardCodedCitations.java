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

import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.Static;

import static org.opengis.test.Assert.*;
import static java.util.Collections.singleton;


/**
 * Hard-coded citation constants used for testing purpose only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
public final strictfp class HardCodedCitations extends Static {
    /**
     * The <a href="http://www.iso.org/">International Organization for Standardization</a>
     * organization. An {@linkplain Citation#getAlternateTitles() alternate title} for this
     * citation is "ISO" (according ISO 19115, alternate titles often contain abbreviations).
     */
    public static final Citation ISO;
    static {
        final DefaultCitation c = new DefaultCitation();
        c.setTitle(new SimpleInternationalString("International Organization for Standardization"));
        c.setAlternateTitles(singleton(new SimpleInternationalString("ISO")));
        c.setPresentationForms(singleton(PresentationForm.DOCUMENT_DIGITAL));
        c.freeze();
        ISO = c;
    }

    /**
     * The ISO 19115 standard.
     */
    public static final Citation ISO_19115;
    static {
        final DefaultCitation c = new DefaultCitation();
        c.setTitle(new SimpleInternationalString("ISO 19115"));
        c.setPresentationForms(singleton(PresentationForm.DOCUMENT_DIGITAL));
        c.freeze();
        ISO_19115 = c;
    }

    /**
     * The <a href="http://www.epsg.org">European Petroleum Survey Group</a> authority.
     * An {@linkplain Citation#getAlternateTitles() alternate title} for this citation is
     * "EPSG" (according ISO 19115, alternate titles often contain abbreviations). In
     * addition, this citation contains the "EPSG" {@linkplain Citation#getIdentifiers identifier}.
     */
    public static final Citation EPSG;
    static {
        final DefaultCitation c = new DefaultCitation();
        c.setTitle(new SimpleInternationalString("European Petroleum Survey Group"));
        c.setAlternateTitles(singleton(new SimpleInternationalString("EPSG")));
        c.setPresentationForms(singleton(PresentationForm.TABLE_DIGITAL));
        c.freeze();
        EPSG = c;
    }

    /**
     * The <a href="http://www.remotesensing.org/geotiff/geotiff.html">GeoTIFF</a> specification.
     */
    public static final Citation GEOTIFF;
    static {
        final DefaultCitation c = new DefaultCitation();
        c.setTitle(new SimpleInternationalString("GeoTIFF"));
        c.setPresentationForms(singleton(PresentationForm.DOCUMENT_DIGITAL));
        c.freeze();
        GEOTIFF = c;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedCitations() {
    }

    /**
     * Asserts that the given identifiers are for the {@link HardCodedCitations#EPSG} constant.
     *
     * @param identifiers The collection to validate. Should be a collection of {@link Identifier}.
     */
    public static void assertIdentifiersForEPSG(final Collection<?> identifiers) {
        assertNotNull("identifiers", identifiers);
        for (final Object id : identifiers) {
            assertInstanceOf("identifier", Identifier.class, id);
            // TODO: check for EPSG identifiers here.
        }
    }
}
