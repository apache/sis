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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.HashMap;
import org.opengis.metadata.Identifier;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.NamedIdentifier;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;


/**
 * Placeholder for the name of an operation method or a parameter defined in the EPSG database.
 * We use this placeholder for remembering where to manage {@code IdentifiedObject} version.
 * Future implementation may also be able to read the remarks from the database if requested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class EPSGName {  // TODO: consider extending NamedIdentifier if we implement deferred reading of remarks.
    /**
     * Version of the operation method, or {@code null} if unknown.
     *
     * <p>This is unspecified in current Apache SIS implementation.
     * However future SIS implementations may fetch this information from the EPSG database.
     * In the meantime, we use this constant as a way to track the places in Apache SIS code
     * base where this information is desired.</p>
     */
    private static final String VERSION = null;

    /**
     * Placeholder for what may be (in a future Apache SIS version) an implementation
     * capable to fetch the remarks from the database using the given identifier code.
     *
     * <p>This is unspecified in current Apache SIS implementation.
     * However future SIS implementations may fetch this information from the EPSG database.
     * In the meantime, we use this constant as a way to track the places in Apache SIS code
     * base where this information is desired.</p>
     */
    private static final InternationalString REMARKS = null;

    /**
     * Do not allow (for now) instantiation of this class.
     */
    private EPSGName() {
    }

    /**
     * Creates an EPSG name or alias.
     *
     * @param code The EPSG name to be returned by {@link NamedIdentifier#getCode()}.
     * @return An EPSG name or alias for the given string.
     */
    public static NamedIdentifier create(final String code) {
        return new NamedIdentifier(Citations.EPSG, Constants.EPSG, code, VERSION, REMARKS);
    }

    /**
     * Creates an EPSG identifier.
     *
     * @param  code The EPSG code.
     * @return The EPSG identifier for the given numerical value.
     */
    public static Identifier identifier(final int code) {
        return new ImmutableIdentifier(Citations.EPSG, Constants.EPSG, String.valueOf(code).intern(), VERSION, REMARKS);
    }

    /**
     * Creates a map of properties to be given to the construction of an operation method.
     * The returned map is modifiable - callers can add or remove entries after this method call.
     *
     * @param  identifier The EPSG code.
     * @param  name       The EPSG name.
     * @param  nameOGC    The OGC name, or {@code null} if none.
     * @return A map of properties for building the operation method.
     */
    public static Map<String,Object> properties(final int identifier, final String name, final String nameOGC) {
        return properties(identifier, name, (nameOGC == null) ? null :
            // Version and remarks are intentionally null here, since they are not EPSG version or remarks.
            new NamedIdentifier(Citations.OGC, Constants.OGC, nameOGC, null, null));
    }

    /**
     * Creates a map of properties to be given to the construction of an operation method.
     * The returned map is modifiable - callers can add or remove entries after this method call.
     *
     * @param  identifier The EPSG code.
     * @param  name       The EPSG name.
     * @param  nameOGC    The OGC name, or {@code null} if none.
     * @return A map of properties for building the operation method.
     */
    public static Map<String,Object> properties(final int identifier, final String name, final GenericName nameOGC) {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(IDENTIFIERS_KEY, identifier(identifier));
        properties.put(NAME_KEY, create(name));
        if (nameOGC != null) {
            properties.put(ALIAS_KEY, nameOGC);
        }
        return properties;
    }
}
