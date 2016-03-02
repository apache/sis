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

import java.util.Iterator;
import java.util.Collection;
import org.opengis.util.GenericName;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.metadata.NameMeaning;
import org.apache.sis.internal.referencing.NilReferencingObject;

import static org.apache.sis.internal.util.Utilities.appendUnicodeIdentifier;


/**
 * An iterator over the {@linkplain IdentifiedObject#getName() name} of an identified object followed by
 * {@linkplain IdentifiedObject#getAlias() aliases} which are instance of {@link ReferenceIdentifier}.
 * This iterator is used for {@link AbstractIdentifiedObject} XML marshalling because GML merges the name
 * and aliases in a single {@code <gml:name>} property. However this iterator is useful only if the aliases
 * are instances of {@link NamedIdentifier}, or any other implementation which is both a name and an identifier.
 *
 * <p>This class also opportunistically provide helper methods for {@link AbstractIdentifiedObject} marshalling.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
final class NameIterator implements Iterator<ReferenceIdentifier> {
    /**
     * The next element to return, or {@code null} if we reached the end of iteration.
     */
    private ReferenceIdentifier next;

    /**
     * An iterator over the aliases.
     */
    private final Iterator<GenericName> alias;

    /**
     * Creates a new iterator over the name and aliases of the given object.
     */
    NameIterator(final IdentifiedObject object) {
        alias = object.getAlias().iterator();
        next = object.getName();
        // Should never be null in a well-formed IdentifiedObject, but let be safe.
        if (isUnnamed(next)) {
            next();
        }
    }

    /**
     * Returns {@code true} if the given identifier is null or the {@link NilReferencingObject#UNNAMED} instance.
     */
    static boolean isUnnamed(final ReferenceIdentifier name) {
        return (name == null) || (name == NilReferencingObject.UNNAMED);
    }

    /**
     * Returns {@code true} if there is an other name or alias to return.
     */
    @Override
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns the next name or alias in the iteration.
     *
     * Note: we do not bother checking for {@code NoSuchElementException} because this iterator
     * will be used only by JAXB, which is presumed checking for {@link #hasNext()} correctly.
     */
    @Override
    public ReferenceIdentifier next() {
        final ReferenceIdentifier n = next;
        while (alias.hasNext()) {
            final GenericName c = alias.next();
            if (c instanceof ReferenceIdentifier) {
                next = (ReferenceIdentifier) c;
                return n;
            }
        }
        next = null;
        return n;
    }

    /**
     * Unsupported operation since this iterator is read-only.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the number of name and aliases in the given object.
     */
    public static int count(final IdentifiedObject object) {
        int c = 0;
        final NameIterator it = new NameIterator(object);
        while (it.hasNext()) {
            it.next();
            c++;
        }
        return c;
    }

    /**
     * Implementation of {@link AbstractIdentifiedObject#getID()}, provided here for reducing the amount of code
     * to load in the common case where XML support is not needed.
     *
     * <p>The current implementation searches for the first identifier, regardless its authority.
     * If no identifier is found, then the name and aliases are used.
     * Then, this method returns the concatenation of the following elements separated by hyphens:</p>
     * <ul>
     *   <li>The code space in lower case, retaining only characters that are valid for Unicode identifiers.</li>
     *   <li>The object type as defined in OGC's URN (see {@link org.apache.sis.internal.util.DefinitionURI})</li>
     *   <li>The object code, retaining only characters that are valid for Unicode identifiers.</li>
     * </ul>
     *
     * Example: {@code "epsg-crs-4326"}.
     *
     * <p>The returned ID needs to be unique only in the XML document being marshalled.
     * Consecutive invocations of this method do not need to return the same value,
     * since it may depends on the marshalling context.</p>
     *
     * @param  context     The (un)marshalling context.
     * @param  object      The object for which to get a {@code gml:id}.
     * @param  name        The identified object name, or {@code null} if none.
     * @param  alias       The identified object aliases, or {@code null} if none.
     * @param  identifiers The identifiers, or {@code null} if none.
     * @return Proposed value for {@code gml:id} attribute, or {@code null} if none.
     */
    static String getID(final Context context, final IdentifiedObject object, final ReferenceIdentifier name,
            final Collection<? extends GenericName> alias, final Collection<? extends ReferenceIdentifier> identifiers)
    {
        String candidate = Context.getObjectID(context, object);
        if (candidate == null) {
            final StringBuilder id = new StringBuilder();
            /*
             * We will iterate over the identifiers first. Only after the iteration is over,
             * if we found no suitable ID, then we will use the primary name as a last resort.
             */
            if (identifiers != null) {
                for (final ReferenceIdentifier identifier : identifiers) {
                    if (appendUnicodeIdentifier(id, '-', identifier.getCodeSpace(), "", true) |    // Really |, not ||
                        appendUnicodeIdentifier(id, '-', NameMeaning.toObjectType(object.getClass()), "", false) |
                        appendUnicodeIdentifier(id, '-', identifier.getCode(), "", true))
                    {
                        /*
                         * Check for ID uniqueness. If the ID is rejected, then we just need to clear
                         * the buffer and let the iteration continue the search for another ID.
                         */
                        candidate = id.toString();
                        if (Context.setObjectForID(context, object, candidate)) {
                            return candidate;
                        }
                    }
                    id.setLength(0);    // Clear the buffer for another try.
                }
            }
            /*
             * In last ressort, use the name or an alias. The name will be used without codespace since
             * names are often verbose. If that name is also used, append a number until we find a free ID.
             */
            if (isUnnamed(name) || !appendUnicodeIdentifier(id, '-', name.getCode(), "", false)) {
                if (alias != null) {
                    for (final GenericName a : alias) {
                        if (appendUnicodeIdentifier(id, '-', a.toString(), "", false)) {
                            break;
                        }
                    }
                }
            }
            if (id.length() != 0) {
                candidate = id.toString();
                if (!Context.setObjectForID(context, object, candidate)) {
                    final int s = id.append('-').length();
                    int n = 0;
                    do {
                        if (++n == 100) return null;    //  Arbitrary limit.
                        candidate = id.append(n).toString();
                        id.setLength(s);
                    } while (!Context.setObjectForID(context, object, candidate));
                }
            }
        }
        return candidate;
    }
}
