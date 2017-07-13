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
package org.apache.sis.storage.gdal;

import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.simple.SimpleIdentifier;


/**
 * Base class of implementations in this Proj4 wrappers package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class PJObject implements IdentifiedObject {
    /**
     * The name of this referencing object, or {@code null} if none.
     */
    final ReferenceIdentifier name;

    /**
     * The aliases, or an empty list if none.
     */
    final Collection<GenericName> aliases;

    /**
     * Creates a new identified object of the given name.
     *
     * @param name  the name of the new object, or {@code null} if none.
     */
    PJObject(final ReferenceIdentifier name) {
        this.name    = name;
        this.aliases = Collections.emptyList();
    }

    /**
     * Creates a new identified object of the given name and aliases.
     *
     * @param name  the name of the new object, or {@code null} if none.
     */
    PJObject(final ReferenceIdentifier name, final Collection<GenericName> aliases) {
        this.name    = name;
        this.aliases = aliases;
    }

    /**
     * Creates a new identified object as a copy of the given one.
     */
    PJObject(final IdentifiedObject object) {
        name    = object.getName();
        aliases = object.getAlias();
    }

    /**
     * Returns the value of the given parameter as an identifier, or {@code null} if none.
     * The given parameter key shall include the {@code '+'} prefix and {@code '='} suffix,
     * for example {@code "+proj="}. This is a helper method for providing the {@code name}
     * argument in constructors.
     *
     * @param  definition  the Proj.4 definition string to parse.
     * @param  key         the parameter name.
     * @return the parameter value as an identifier.
     */
    static ReferenceIdentifier identifier(final String definition, final String key) {
        int i = definition.indexOf(key);
        if (i >= 0) {
            i += key.length();
            final int stop = definition.indexOf(' ', i);
            String value = (stop >= 0) ? definition.substring(i, stop) : definition.substring(i);
            if (!(value = value.trim()).isEmpty()) {
                return new SimpleIdentifier(null, value, false);
            }
        }
        return null;
    }

    /**
     * Returns the name of this referencing object, or {@code null} if none.
     * Note that this attribute is mandatory according ISO 19111, but this
     * simple Proj.4 wrapper is lenient about that.
     */
    @Override
    public ReferenceIdentifier getName() {
        return name;
    }

    /**
     * Returns the name in a singleton set, since we don't have anything better for identifiers.
     * The objects created by {@link Proj4} will use {@code "EPSG:xxxx"} identifiers, so this is
     * rather the name which is quite inaccurate.
     */
    @Override
    public Set<ReferenceIdentifier> getIdentifiers() {
        return Collections.singleton(name);
    }

    /**
     * Returns the aliases, or an empty set if none. The aliases are determined from
     * the {@code "parameter-names.txt"} or {@code "projection-names.txt"} resources
     * files.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<GenericName> getAlias() {
        return aliases;
    }

    /**
     * Returns {@code null} since we do not define any scope in our Proj4 wrappers.
     * Note that this method is not inherited from {@link IdentifiedObject}, but is
     * defined in sub-interfaces like {@link org.opengis.referencing.crs.SingleCRS}.
     */
    public InternationalString getScope() {
        return null;
    }

    /**
     * Returns {@code null} since we do not define any domain of validity.
     * Note that this method is not inherited from {@link IdentifiedObject}, but is
     * defined in sub-interfaces like {@link org.opengis.referencing.crs.SingleCRS}.
     */
    public Extent getDomainOfValidity() {
        return null;
    }

    /**
     * Returns {@code null} since there is no remarks associated with our Proj4 wrappers.
     */
    @Override
    public InternationalString getRemarks() {
        return null;
    }

    /**
     * Throws unconditionally the exception since we do not support WKT formatting.
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a string representation of this object, mostly for debugging purpose.
     * This string representation may change in any future version.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name + ']';
    }
}
