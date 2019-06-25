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
package org.apache.sis.internal.simple;

import java.util.Collection;
import java.util.Collections;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.internal.util.Strings;


/**
 * A trivial implementation of {@link Format} containing only the format name.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class SimpleFormat extends SimpleCitation implements Format {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2673642991031129520L;

    /**
     * Creates a new format of the given short name or abbreviation.
     *
     * @param  name  a short name or abbreviation for the format.
     */
    public SimpleFormat(final String name) {
        super(name);
    }

    /**
     * Returns the format specification title, which is {@code null} by default.
     *
     * @return the format specification.
     */
    @Override
    public InternationalString getTitle() {
        return null;
    }

    /**
     * Returns the format name given at construction time.
     *
     * @return the name given at construction time.
     */
    @Override
    public Collection<InternationalString> getAlternateTitles() {
        return Collections.singletonList(super.getTitle());
    }

    /**
     * Citation / URL of the specification format.
     *
     * @return citation / URL of the specification format.
     */
    @Override
    public Citation getFormatSpecificationCitation() {
        return this;
    }

    /**
     * Returns a string representation of this format for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.bracket("Format", title);
    }
}
