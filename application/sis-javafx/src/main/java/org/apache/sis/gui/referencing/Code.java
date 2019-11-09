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

import java.util.Locale;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.Exceptions;


/**
 * Stores the code of a coordinate reference system (CRS) together with its description.
 * The description will be fetched when first needed and returned by {@link #toString()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class Code {
    /**
     * The CRS code. Usually defined by EPSG, but other authorities are allowed.
     */
    final String code;

    /**
     * The CRS object name for the {@linkplain #code}, fetched when first needed.
     * In Apache SIS implementation of EPSG factory, this is the CRS name.
     */
    private String name;

    /**
     * The object returned by {@link #crs()}, cached for reuse.
     */
    private CoordinateReferenceSystem crs;

    /**
     * The authority factory to use for fetching the name.
     */
    private final CRSAuthorityFactory factory;

    /**
     * Creates a code from the specified value.
     */
    Code(final CRSAuthorityFactory factory, final String code) {
        this.factory = factory;
        this.code    = code;
    }

    /**
     * Creates the object identified by code.
     */
    CoordinateReferenceSystem crs() throws FactoryException {
        if (crs == null) {
            crs = factory.createCoordinateReferenceSystem(code);
        }
        return crs;
    }

    /**
     * Returns a description of the object. This method fetches the description when first needed.
     * If the operation fails, the exception message will be used as a description.
     *
     * @param  locale  the desired locale, or {@code null} for the default locale.
     * @return the object name in the given locale if possible.
     */
    String name(final Locale locale) {
        if (name == null) try {
            name = factory.getDescriptionText(code).toString(locale);
        } catch (FactoryException e) {
            name = Exceptions.getLocalizedMessage(e, locale);
        }
        return name;
    }
}
