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
package org.apache.sis.internal.referencing;

import org.apache.sis.referencing.NamedIdentifier;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.Deprecable;


/**
 * A deprecated name.
 * This is used mostly for names which were used in legacy versions of the EPSG database.
 *
 * <div class="note"><b>Implementation note:</b>
 * this class opportunistically recycles the {@linkplain #getDescription() description} property into a
 * {@linkplain #getRemarks() remarks} property. This is a lazy way to inherit {@link #equals(Object)}
 * and {@link #hashCode()} implementations without adding code in this class for taking in account a
 * new field.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public final class DeprecatedName extends NamedIdentifier implements Deprecable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1792369861343798471L;

    /**
     * Creates a new deprecated EPSG name.
     *
     * @param authority  Organization or party responsible for definition and maintenance of the name.
     * @param codeSpace  Name or identifier of the person or organization responsible for namespace.
     * @param code       Name, optionally from a controlled list or pattern defined by a namespace.
     * @param version    The version of the associated namespace or name as specified by the code authority, or {@code null} if none.
     * @param remarks    Comments on or information about why this name is deprecated, or {@code null} if none.
     */
    public DeprecatedName(final Citation authority, final String codeSpace,
            final CharSequence code, final String version, final InternationalString remarks)
    {
        super(authority, codeSpace, code, version, remarks);
    }

    /**
     * Returns {@code true} since this name is deprecated.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isDeprecated() {
        return true;
    }

    /**
     * Information about the replacement for this name.
     *
     * <div class="note"><b>Example:</b> "superseded by code XYZ".</div>
     *
     * @return Information about the replacement for this name, or {@code null} if none.
     */
    @Override
    public InternationalString getRemarks() {
        return super.getDescription();
    }

    /**
     * Returns {@code null}, since we used the description for the superseded information.
     * See <cite>"Implementation note"</cite> in class javadoc.
     *
     * @return {@code null}.
     */
    @Override
    public InternationalString getDescription() {
        return null;
    }
}
