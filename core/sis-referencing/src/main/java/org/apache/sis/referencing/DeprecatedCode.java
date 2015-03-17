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
package org.apache.sis.referencing;

import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.resources.Vocabulary;


/**
 * An identifier for a deprecated identifier.
 * This is used mostly for deprecated EPSG codes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class DeprecatedCode extends ImmutableIdentifier {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 357222258307746767L;

    /**
     * Creates a deprecated identifier.
     *
     * @param supersededBy The code that replace this one.
     */
    DeprecatedCode(final Citation authority, final String codeSpace,
            final String code, final String version, final CharSequence supersededBy)
    {
        super(authority, codeSpace, code, version,
                Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, supersededBy));
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
}
