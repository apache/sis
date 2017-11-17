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
package org.apache.sis.storage;

import org.apache.sis.setup.OptionKey;
import org.apache.sis.feature.FoliationRepresentation;


/**
 * Keys in a map of options for configuring the way data are read or written to a storage.
 * {@code DataOptionKey} extends {@link OptionKey} with options about features, coverages or other kinds of structure
 * in data files. Contrarily to {@code OptionKey}, the options defined in this {@code DataOptionKey} class are usually
 * not applicable to other kinds of file (e.g. configuration or program files).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param <T>  the type of option values.
 *
 * @since 1.0
 * @module
 */
public final class DataOptionKey<T> extends OptionKey<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8927757348322016043L;

    /**
     * Whether to assemble trajectory fragments (distinct CSV lines) into a single {@code Feature} instance
     * forming a foliation. This is ignored if the file does not seem to contain moving features.
     *
     * @since 1.0
     */
    public static final OptionKey<FoliationRepresentation> FOLIATION_REPRESENTATION =
            new DataOptionKey<>("FOLIATION_REPRESENTATION", FoliationRepresentation.class);

    /**
     * Creates a new key of the given name.
     */
    private DataOptionKey(final String name, final Class<T> type) {
        super(name, type);
    }
}
