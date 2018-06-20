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
package org.apache.sis.gui.crs;

import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.FactoryException;


/**
 * This element stores the {@linkplain #code code value}.
 * The description name will be fetched when first needed and returned by {@link #toString}.
 *
 * @author Martin Desruisseaux (IRD)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Code {

    /**
     * The authority code.
     */
    public final String code;

    /**
     * The CRS object description for the {@linkplain #code}.
     * Will be extracted only when first needed.
     */
    private String name;

    private String description;

    /**
     * The authority factory to use for fetching the name. Will be set to {@code null} after
     * {@linkplain #name} has been made available, in order to allow the garbage collector
     * to do its work if possible.
     */
    private final AuthorityFactory factory;

    /**
     * Creates a code from the specified value.
     *
     * @param factory The authority factory.
     * @param code The authority code.
     */
    public Code(final AuthorityFactory factory, final String code) {
        ArgumentChecks.ensureNonNull("factory", factory);
        ArgumentChecks.ensureNonNull("code", code);
        this.factory = factory;
        this.code    = code;
    }

    /**
     * Create the Object identified by code.
     *
     * @return IdentifiedObject
     * @throws FactoryException
     */
    public IdentifiedObject createObject() throws FactoryException{
        return factory.createObject(code);
    }

    /**
     * Returns a description of the object.
     *
     * @return
     */
    public String getDescription(){
        if (description == null) try {
            description = factory.getDescriptionText(code).toString();
        } catch (FactoryException e) {
            description = e.getLocalizedMessage();
        }
        return description;
    }

    /**
     * Returns the name for this code.
     *
     * @todo Maybe we should use the widget Locale when invoking InternationalString.toString(...).
     */
    @Override
    public String toString() {
        if (name == null) {
            name = code + " - "+getDescription();
        }
        return name;
    }
}
