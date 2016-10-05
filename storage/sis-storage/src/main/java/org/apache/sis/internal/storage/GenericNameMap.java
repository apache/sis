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
package org.apache.sis.internal.storage;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ConcurrentModificationException;
import org.opengis.util.GenericName;
import org.opengis.util.ScopedName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.storage.IllegalNameException;


/**
 * Helper class for mapping {@link GenericName} instances and their aliases to arbitrary objects.
 * The objects can be read and written using a {@link String} representation of their name,
 * or an alias when there is no ambiguity.
 *
 * <div class="section">Synchronization</div>
 * This class is not thread-safe. Synchronization, if desired, shall be done by the caller.
 * The caller is typically a {@link org.apache.sis.storage.DataStore} implementation.
 *
 * @param <E> the type of elements associated with the names.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class GenericNameMap<E> {
    /**
     * All aliases found for all names given to the {@link #add(GenericName, Object)} method.
     * Keys are aliases (never the explicitely given names) and values are names for which the key is an alias.
     * Each {@code List<String>} instance contains exactly one name if there is no ambiguity.
     * If the list contains more than one name, this means that the alias is ambiguous.
     *
     * <p>Note that this map always have less entries than {@link #values} map.</p>
     */
    private final Map<String, List<String>> aliases;

    /**
     * The user-specified values associated to names and aliases. If a value is absent, it may means either that the
     * given name does not exist, or that the given name is an ambiguous alias. Those two cases can be distinguished
     * by checking if the key exists in the {@link #aliases} map.
     */
    private final Map<String,E> values;

    /**
     * Creates a new "{@code GenericName} to object" mapping.
     */
    public GenericNameMap() {
        aliases = new HashMap<>();
        values  = new HashMap<>();
    }

    /**
     * Returns the value associated to the given name.
     *
     * @param  name  the name for which to get a value.
     * @return value associated to the given object.
     * @throws IllegalNameException if the given name was not found or is ambiguous.
     */
    public E get(final String name) throws IllegalNameException {
        final E value = values.get(name);
        if (value != null) {
            return value;
        }
        final List<String> nc = aliases.get(name);
        final String message;
        if (nc == null) {
            message = Errors.format(Errors.Keys.ElementNotFound_1, name);
        } else if (nc.size() >= 2) {
            message = Errors.format(Errors.Keys.AmbiguousName_3, nc.get(0), nc.get(1), name);
        } else {
            return null;    // Name was explicitely associated to null value (actually not allowed by current API).
        }
        throw new IllegalNameException(message);
    }

    /**
     * Adds a value for the given name if none exist.
     * If a previous value already exists for the given name, then an exception is thrown.
     *
     * @param  name   the name for which to add a value.
     * @param  value  the value to add (can not be null).
     * @throws IllegalNameException if another element is already registered for the given name.
     */
    public void add(GenericName name, final E value) throws IllegalNameException {
        ArgumentChecks.ensureNonNull("name",  name);
        ArgumentChecks.ensureNonNull("value", value);
        final String key = name.toString();
        if (values.putIfAbsent(key, value) != null) {
            throw new IllegalNameException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, key));
        }
        while (name instanceof ScopedName) {
            name = ((ScopedName) name).tail();
            final String alias = name.toString();
            if (CollectionsExt.addToMultiValuesMap(aliases, alias, key).size() > 1) {
                /*
                 * If there is more than one GenericName for the same alias, we have an ambiguity.
                 * Remove any value associated to that alias. The 'get' method in this class will
                 * know that the value is missing because of ambiguous name.
                 */
                values.remove(alias);
            } else if (values.put(alias, value) != null) {
                /*
                 * If no previous GenericName existed for that alias, then no value should exist for that alias.
                 * If a previous value existed, then (assuming that we do not have a bug in our algorithm) this
                 * object changed in a concurrent thread during this method execution.  We do not try to detect
                 * all such errors, but this check is an easy one to perform opportunistically.
                 */
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Removes the value associated to the given name.
     * If no value is associated to the given name, then this method does nothing.
     *
     * @param  name  the name for which to remove value.
     * @return {@code true} if the value was removed, or {@code false} if no value was defined for the given name.
     * @throws IllegalNameException if inconsistency are found between the given name and the one which was given
     *         to the {@link #add(GenericName, Object)} method. An example of inconsistency is a name having the
     *         same string representation, but for which {@link ScopedName#tail()} returns different values.
     */
    public boolean remove(GenericName name) throws IllegalNameException {
        ArgumentChecks.ensureNonNull("name",  name);
        final String key = name.toString();
        if (values.remove(key) == null) {
            return false;
        }
        boolean error = false;
        while (name instanceof ScopedName) {
            name = ((ScopedName) name).tail();
            final String alias = name.toString();
            final List<String> remaining = CollectionsExt.removeFromMultiValuesMap(aliases, alias, key);
            /*
             * The list of remaining GenericNames may be empty, but should never be null unless the tail is
             * inconsistent with the one found by the 'add(GenericName, Object) method.  Otherwise if there
             * is exactly one remaining GenericName, then the alias is not ambiguous anymore for that name.
             */
            error |= (remaining == null);
            if (remaining == null || remaining.isEmpty()) {
                error |= (values.remove(alias) == null);
            } else if (remaining.size() == 1) {
                final String select = remaining.get(0);
                assert !select.equals(key) : select;     // Should have been removed by removeFromMultiValuesMap(…).
                error |= (values.putIfAbsent(alias, values.get(select)) != null);
            }
        }
        if (error) {
            throw new IllegalNameException(Resources.format(Resources.Keys.InconsistentNameComponents_1, key));
        }
        return true;
    }
}
