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

import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A reference identifier for a deprecated EPSG codes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class DeprecatedCode extends ImmutableIdentifier {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 357222258307746767L;

    /**
     * Creates a deprecated identifier.
     *
     * @param code Identifier code from the EPSG authority.
     * @param supersededBy The code that replace this one.
     */
    public DeprecatedCode(final short code, final short supersededBy) {
        super(Citations.OGP, Constants.EPSG, Short.toString(code).intern(), null, remarks(supersededBy));
    }

    /**
     * Formats a "Superseded by" international string.
     */
    private static InternationalString remarks(final int supersededBy) {
        return Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, supersededBy);
    }

    /**
     * Returns {@code true} since this code is deprecated.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isDeprecated() {
        return true;
    }
}
