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
package org.apache.sis.internal.metadata;

import org.opengis.metadata.citation.Citation;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.util.iso.SimpleInternationalString;


/**
 * Citations to metadata standards.
 * This class is a complement of {@link org.apache.sis.metadata.iso.citation.Citations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class Standards extends SimpleCitation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8532554725610093472L;

    /**
     * The <cite>ISO 19115-2 Geographic Information — Metadata Part 2: Extensions for imagery and gridded data</cite>
     * standard.
     *
     * @since 0.5
     *
     * @see org.opengis.annotation.Specification#ISO_19115_2
     */
    public static final Citation ISO_19115_2 = new Standards(
            "ISO 19115-2 Geographic Information — Metadata Part 2: Extensions for imagery and gridded data",
            "ISO 19115-2:2009(E)");

    /**
     * The metadata version.
     */
    private final String version;

    /**
     * Creates a new citation for a metadata standard.
     *
     * @param standard The metadata standard.
     * @param version  The metadata version.
     */
    private Standards(final String standard, final String version) {
        super(standard);
        this.version = version;
    }

    /**
     * Returns the metadata version.
     *
     * @return The metadata version.
     */
    @Override
    public InternationalString getEdition() {
        return new SimpleInternationalString(version);
    }
}
