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
package org.apache.sis.xml.bind;

import java.util.logging.Level;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * An identifier in the scope of a type, for use as keys in an hash map.
 * This object is for handing ISO 19111 identifiers, because they may have
 * the same value for different types. The type is usually a GeoAPI interface.
 *
 * @param  <T>  base type of the identified object.
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ScopedIdentifier<T> {
    /**
     * Sentinel value for meaning that an identifier is used many times.
     */
    private static final Object DUPLICATED = Void.TYPE;

    /**
     * The identifier scope, usually a GeoAPI interface.
     */
    private final Class<? extends T> scope;

    /**
     * The identifier of the object to associate to this key.
     */
    private final String identifier;

    /**
     * Creates a new key.
     *
     * @param scope       the identifier scope, usually a GeoAPI interface.
     * @param identifier  the identifier of the object to associate to this key.
     */
    public ScopedIdentifier(final Class<? extends T> scope, final String identifier) {
        this.scope      = scope;
        this.identifier = identifier;
    }

    /**
     * Returns an identifier with the same scope as this identifier by a different character string.
     *
     * @param  alt  the new identifier.
     * @return the new identifier, or {@code this} if no change.
     */
    public ScopedIdentifier<T> rename(final String alt) {
        return alt.equals(identifier) ? this : new ScopedIdentifier<>(scope, alt);
    }

    /**
     * Stores an identified object for this identifier.
     * The identifier is typically {@link org.opengis.referencing.IdentifiedObject#getIdentifiers()}.
     * If the given identifier is already associated to another identified object, a warning is logged.
     * This method can be invoked many times if an object has many identifiers.
     *
     * @param  base    limit to follow when storing the object for parent interfaces.
     * @param  object  the identified object to store. Shall be an instance of {@code T}.
     * @param  caller  the class to declare as the source if a warning is logged, or {@code null} for no warning.
     * @param  method  the name of the method to declare as the source if a warning is logged, or {@code null}.
     */
    public void store(final Class<T> base, final T object, final Class<?> caller, final String method) {
        final Context context = Context.current();
        if (context != null) {
            boolean warn = (caller != null);
            ScopedIdentifier<?> key = this;
            final Class<?>[] parents = Classes.getAllInterfaces(scope);
            for (int index = -1 ;;) {
                final Object previous = context.identifiedObjects.putIfAbsent(key, object);
                if (previous != null && previous != object && previous != DUPLICATED) {
                    context.identifiedObjects.put(key, DUPLICATED);
                    if (warn) {
                        warn = false;           // Report warning only once for this identifier.
                        Context.warningOccured(context, (key == this) ? Level.WARNING : Level.FINE,
                                caller, method, null, Errors.class, Errors.Keys.DuplicatedIdentifier_1, identifier);
                    }
                }
                Class<?> parent;
                do if (++index >= parents.length) return;
                while (!base.isAssignableFrom(parent = parents[index]));
                key = new ScopedIdentifier<>(parent, identifier);
            }
        }
    }

    /**
     * Returns the object associated to this scoped identifier.
     *
     * @param  context  the unmarshalling context. Shall not be null.
     * @return the object associated to this scoped identifier, or {@code null} if none.
     */
    public T get(final Context context) {
        final Object value = context.identifiedObjects.get(this);
        return (value != DUPLICATED) ? scope.cast(value) : null;
    }

    /**
     * Returns an hash code value for this key.
     */
    @Override
    public int hashCode() {
        return scope.hashCode() + identifier.hashCode();
    }

    /**
     * Tests this key with the given object for equality.
     *
     * @param  obj  the object to compare with this key.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScopedIdentifier) {
            final var other = (ScopedIdentifier<?>) obj;
            return scope.equals(other.scope) && identifier.equals(other.identifier);
        }
        return false;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return scope.getSimpleName() + ':' + identifier;
    }
}
