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
package org.apache.sis.gui.referencing;

import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.FactoryException;


/**
 * Stores the code of a coordinate reference system (CRS) together with its description.
 * The description will be fetched when first needed and returned by {@link #toString()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Code {
    /**
     * The CRS code. Usually defined by EPSG, but other authorities are allowed.
     */
    final String code;

    /**
     * The CRS object description for the {@linkplain #code}, fetched when first needed.
     * In Apache SIS implementation of EPSG factory, this is the CRS name.
     */
    private String name;

    /**
     * The authority factory to use for fetching the name. Will be set to {@code null} after
     * {@linkplain #name} has been made available, in order to allow the garbage collector
     * to do its work if possible.
     */
    private final AuthorityFactory factory;

    /**
     * Creates a code from the specified value.
     */
    Code(final AuthorityFactory factory, final String code) {
        this.factory = factory;
        this.code    = code;
    }

    /**
     * Create the Object identified by code.
     */
    IdentifiedObject createObject() throws FactoryException{
        return factory.createObject(code);
    }

    /**
     * Returns a description of the object.
     */
    public String getDescription() {
        if (name == null) try {
            name = factory.getDescriptionText(code).toString();
        } catch (FactoryException e) {
            name = e.getLocalizedMessage();
        }
        return name;
    }

    /**
     * Returns the name for this code.
     *
     * @todo Maybe we should use the widget Locale when invoking InternationalString.toString(...).
     */
    @Override
    public String toString() {
        return code + " - " + getDescription();
    }
}
