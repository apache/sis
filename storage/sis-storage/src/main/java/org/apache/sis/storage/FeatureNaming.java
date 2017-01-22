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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ConcurrentModificationException;
import java.util.Locale;
import org.opengis.util.GenericName;
import org.opengis.util.ScopedName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.storage.Resources;


/**
 * Helper class for mapping {@link GenericName} instances and their shortened names to features.
 * The features are typically represented by instances of {@link org.opengis.feature.FeatureType}
 * or {@link org.opengis.coverage.Coverage} (sometime seen as a kind of features), but this class
 * actually puts no restriction on the kind of object associated to {@code GenericName}s;
 * {@link DataStore} implementations are free to choose their internal object.
 * Those objects can be stored and fetched using the {@code String} representation of their name
 * as given by {@link GenericName#toString()}, or a shortened name when there is no ambiguity.
 *
 * <div class="note"><b>Example:</b>
 * a data store may contain a {@code FeatureType} named {@code "foo:bar"}.
 * If that feature type has been binded like below:
 *
 * {@preformat java
 *     FeatureNaming<FeatureType> binding = new FeatureNaming<>();
 *     FeatureType myFooBar = ...;                                  // Some type named "foo:bar" for this example.
 *     binding.add(null, myFooBar.getName(), myFooBar);
 * }
 *
 * Then the two following lines return the same instance:
 *
 * {@preformat java
 *     assert binding.get(null, "foo:bar") == myFooBar;
 *     assert binding.get(null,     "bar") == myFooBar;    // Allowed only if there is no ambiguity.
 * }
 * </div>
 *
 * Note that contrarily to the standard {@link java.util.Map#get(Object)} method contract, the {@link #get get(…)}
 * method defined in this class throws an exception instead than returning {@code null} if no unambiguous mapping
 * can be established for the given name. This behavior allows {@code FeatureNaming} to produce an error message
 * telling why the operation can not succeed.
 *
 * <div class="section">Managing the list of generic names</div>
 * This class does not memorize the list of {@linkplain #add added} {@code GenericName} instances. Instead this
 * class memorizes only their string representations, thus protecting the binding from any change in the original
 * {@code GenericName} instances. The list of feature names should instead be included in the ISO 19115 metadata
 * returned by {@link DataStore#getMetadata()}. The path to feature names is:
 *
 * <blockquote>{@code metadata} /
 * {@link org.apache.sis.metadata.iso.DefaultMetadata#getContentInfo() contentInfo} /
 * {@link org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription#getFeatureTypeInfo() featureTypes} /
 * {@link org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo#getFeatureTypeName() featureTypeName}
 * </blockquote>
 *
 * Note that above metadata information are not necessarily structured as a flat list; a {@code DataStore} may group
 * some feature information in different {@link org.opengis.metadata.content.FeatureCatalogueDescription} instances.
 * This is one reason why we let the data store manages {@code GenericName} lists itself.
 *
 * <div class="section">Thread safety</div>
 * A {@code FeatureNaming} instance is thread-safe only if constructed once and never modified after publication.
 * For example it is safe to initialize a {@code FeatureNaming} in a {@link DataStore} or {@link DataStoreProvider}
 * constructor if the result is stored in a private final field with no public accessor
 * and no call to {@link #add add(…)} or {@link #remove remove(…)} methods after construction.
 * If this condition does not hold, then synchronization (if desired) is caller's responsibility.
 * The caller is typically the {@code DataStore} implementation which contains this {@code FeatureNaming} instance.
 *
 * @param <E> the type of elements associated with the names.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see org.apache.sis.util.iso.AbstractName
 * @see org.apache.sis.feature.DefaultFeatureType
 * @see org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo
 */
public class FeatureNaming<E> {
    /**
     * All aliases found for all names given to the {@link #add(DataStore, GenericName, Object)} method.
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
    public FeatureNaming() {
        aliases = new HashMap<>();
        values  = new HashMap<>();
    }

    /**
     * Returns the display name for the given data store, or the localizable "unnamed" string
     * if the given data store is null or does not have a display name.
     */
    private static CharSequence name(final DataStore store) {
        if (store != null) {
            final String name = store.getDisplayName();
            if (name != null) {
                return name;
            }
        }
        return Vocabulary.formatInternational(Vocabulary.Keys.Unnamed);
    }

    /**
     * Returns the locale for the given store, or {@code null} if none.
     */
    private static Locale locale(final DataStore store) {
        return (store != null) ? store.getLocale() : null;
    }

    /**
     * Returns the value associated to the given name.
     *
     * @param  store  the data store for which to get a value, or {@code null} if unknown.
     * @param  name   the name for which to get a value.
     * @return value associated to the given object.
     * @throws IllegalNameException if the given name was not found or is ambiguous.
     */
    public E get(final DataStore store, final String name) throws IllegalNameException {
        final E value = values.get(name);
        if (value != null) {
            return value;
        }
        final short key;
        final Object[] params;
        final List<String> nc = aliases.get(name);
        if (nc == null) {
            key = Resources.Keys.FeatureNotFound_2;
            params = new CharSequence[] {name(store), name};
        } else if (nc.size() >= 2) {
            key = Resources.Keys.AmbiguousName_4;
            params = new CharSequence[] {name(store), nc.get(0), nc.get(1), name};
        } else {
            return null;    // Name was explicitely associated to null value (actually not allowed by current API).
        }
        throw new IllegalNameException(locale(store), key, params);
    }

    /**
     * Adds a value for the given name if none exist.
     * If a previous value already exists for the given name, then an exception is thrown.
     *
     * @param  store  the data store for which to add a value, or {@code null} if unknown.
     * @param  name   the name for which to add a value.
     * @param  value  the value to add (can not be null).
     * @throws IllegalNameException if another element is already registered for the given name.
     */
    public void add(final DataStore store, GenericName name, final E value) throws IllegalNameException {
        ArgumentChecks.ensureNonNull("name",  name);
        ArgumentChecks.ensureNonNull("value", value);
        final String key = name.toString();
        if (values.putIfAbsent(key, value) != null) {
            throw new IllegalNameException(locale(store), Resources.Keys.FeatureAlreadyPresent_2, name(store), key);
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
                throw new ConcurrentModificationException(name(store).toString());
            }
        }
    }

    /**
     * Removes the value associated to the given name.
     * If no value is associated to the given name, then this method does nothing.
     *
     * @param  store  the data store for which to remove a value, or {@code null} if unknown.
     * @param  name   the name for which to remove value.
     * @return {@code true} if the value was removed, or {@code false} if no value was defined for the given name.
     * @throws IllegalNameException if inconsistency are found between the given name and the one which was given
     *         to the {@link #add add(…)} method. An example of inconsistency is a name having the same string
     *         representation, but for which {@link ScopedName#tail()} returns different values.
     */
    public boolean remove(final DataStore store, GenericName name) throws IllegalNameException {
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
             * The list of remaining GenericNames may be empty but should never be null unless the tail
             * is inconsistent with the one found by the 'add(…) method.  Otherwise if there is exactly
             * one remaining GenericName, then the alias is not ambiguous anymore for that name.
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
            throw new IllegalNameException(locale(store), Resources.Keys.InconsistentNameComponents_2, name(store), key);
        }
        return true;
    }
}
