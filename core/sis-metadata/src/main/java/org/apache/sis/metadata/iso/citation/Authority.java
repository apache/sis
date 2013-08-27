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

import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.xml.IdentifierSpace;


/**
 * A citation constant also used as a namespace for identifiers.
 *
 * @param <T> The identifier type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.19)
 * @version 0.4
 * @module
 *
 * @see Citations#EPSG
 */
final class Authority<T> extends SimpleCitation implements IdentifierSpace<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 9049409961960288134L;

    /**
     * The identifier namespace.
     */
    private final String namespace;

    /**
     * Creates a new citation of the given title.
     *
     * @param title The title to be returned by {@link #getTitle()}.
     */
    Authority(final String title, final String namespace) {
        super(title);
        this.namespace = namespace;
    }

    /**
     * Returns the name of this identifier space.
     */
    @Override
    public String getName() {
        return namespace;
    }
}
