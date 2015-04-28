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
import org.opengis.util.GenericName;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;


/**
 * An iterator over the {@linkplain IdentifiedObject#getName() name} of an identified object followed by
 * {@linkplain IdentifiedObject#getAlias() aliases} which are instance of {@link ReferenceIdentifier}.
 * This iterator is used for {@link AbstractIdentifiedObject} marshalling because GML merges the name and
 * aliases in a single {@code <gml:name>} property.
 *
 * <p>Note that this iterator is useful only if the aliases are instances of {@link NamedIdentifier},
 * or any other implementation which is both a name and an identifier.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
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
        if (next == null) { // Should never be null in a well-formed IdentifiedObject, but let be safe.
            next();
        }
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
}
