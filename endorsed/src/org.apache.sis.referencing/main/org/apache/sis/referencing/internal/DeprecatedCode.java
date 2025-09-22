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
package org.apache.sis.referencing.internal;

import java.util.Objects;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.util.Deprecable;


/**
 * An identifier which should not be used anymore.
 * This is used mostly for deprecated <abbr>EPSG</abbr> codes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DeprecatedCode extends ImmutableIdentifier implements Deprecable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8186104136524932846L;

    /**
     * The replacement for the deprecated object, or {@code null} if none.
     */
    public final String replacedBy;

    /**
     * Comments on or information about why this identifier is deprecated, or {@code null} if none.
     */
    @SuppressWarnings("serial")     // Most SIS implementations are serializable.
    private final InternationalString remarks;

    /**
     * Creates a deprecated identifier.
     *
     * @param authority   organization or party responsible for definition and maintenance of the code space or code.
     * @param codeSpace   name or identifier of the person or organization responsible for namespace.
     * @param code        identifier code or name, optionally from a controlled list or pattern defined by a code space.
     * @param version     the version of the associated code space or code as specified by the code authority, or {@code null} if none.
     * @param description a description associated with the identifier. May be {@code null}.
     * @param replacedBy  the replacement for the deprecated object, or {@code null} if none.
     * @param remarks     comments on or information about why this identifier is deprecated, or {@code null} if none.
     */
    public DeprecatedCode(final Citation authority, final String codeSpace,
            final String code, final String version, final InternationalString description,
            final String replacedBy, final InternationalString remarks)
    {
        super(authority, codeSpace, code, version, remarks);
        this.replacedBy = replacedBy;
        this.remarks = remarks;
    }

    /**
     * Returns {@code true} since this identifier is deprecated.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isDeprecated() {
        return true;
    }

    /**
     * Information about the replacement for this identifier.
     *
     * <div class="note"><b>Example:</b> "superseded by code XYZ".</div>
     *
     * @return information about the replacement for this identifier, or {@code null} if none.
     */
    @Override
    public InternationalString getRemarks() {
        return remarks;
    }

    /**
     * Returns an hash code value for this identifier.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 37 * Objects.hash(replacedBy, remarks);
    }

    /**
     * Tests whether this object is equal to the given object.
     */
    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj)) {
            final var other = (DeprecatedCode) obj;
            return Objects.equals(replacedBy, other.replacedBy) && Objects.equals(remarks, other.remarks);
        }
        return false;
    }
}
