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
package org.apache.sis.referencing.factory;

import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashSet;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;


/**
 * Searches in an authority factory for objects approximatively equal to a given object.
 * This class can be used for fetching a fully defined {@linkplain AbstractIdentifiedObject identified object}
 * from an incomplete one, for example from an object without "{@code ID[…]}" or "{@code AUTHORITY[…]}"
 * element in <cite>Well Known Text</cite>.
 *
 * <p>The steps for using {@code IdentifiedObjectFinder} are:</p>
 * <ol>
 *   <li>Get a new instance by calling
 *       {@link GeodeticAuthorityFactory#newIdentifiedObjectFinder()}.</li>
 *   <li>Optionally configure that instance by calling its setter methods.</li>
 *   <li>Perform a search by invoking the {@link #find(IdentifiedObject)} or {@link #findSingleton(IdentifiedObject)} method.</li>
 *   <li>The same {@code IdentifiedObjectFinder} instance can be reused for consecutive searches.</li>
 * </ol>
 *
 * <div class="section">Thread safety</div>
 * {@code IdentifiedObjectFinder} are <strong>not</strong> guaranteed to be thread-safe even if the underlying factory
 * is thread-safe. If concurrent searches are desired, then a new instance should be created for each thread.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see GeodeticAuthorityFactory#newIdentifiedObjectFinder()
 * @see IdentifiedObjects#newFinder(String)
 */
public class IdentifiedObjectFinder {
    /**
     * The domain of the search (for example whether to include deprecated objects in the search).
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    public static enum Domain {
        /**
         * Fast lookup based only on embedded identifiers and names. If those identification information
         * does not allow to locate an object in the factory, then the search will return an empty set.
         *
         * <div class="note"><b>Example:</b>
         * if {@link IdentifiedObjectFinder#find(IdentifiedObject)} is invoked with an object having the {@code "4326"}
         * {@linkplain AbstractIdentifiedObject#getIdentifiers() identifier}, then the {@code find(…)} method will invoke
         * <code>factory.{@linkplain GeodeticAuthorityFactory#createGeographicCRS(String) createGeographicCRS}("4326")</code>
         * and compare the object from the factory with the object to search.
         * If the objects do not match, then another attempt will be done using the
         * {@linkplain AbstractIdentifiedObject#getName() object name}. If using name does not work neither,
         * then {@code find(…)} method makes no other attempt and returns an empty set.
         * </div>
         */
        DECLARATION,

        /**
         * Lookup based on valid (non-deprecated) objects known to the factory.
         * First, a fast lookup is performed based on {@link #DECLARATION}.
         * If the fast lookup gave no result, then a more extensive search is performed by scanning the content
         * of the dataset.
         *
         * <div class="note"><b>Example:</b>
         * if {@link IdentifiedObjectFinder#find(IdentifiedObject)} is invoked with an object equivalent to the
         * {@linkplain org.apache.sis.referencing.CommonCRS#WGS84 WGS84} geographic CRS but does not declare the
         * {@code "4326"} identifier and does not have the <cite>"WGS 84"</cite> name, then the search based on
         * {@link #DECLARATION} will give no result. The {@code find(…)} method will then scan the dataset for
         * geographic CRS using equivalent datum and coordinate system. This may be a costly operation.
         * </div>
         */
        VALID_DATASET,

        /**
         * Lookup based on all objects (both valid and deprecated) known to the factory.
         * This is the same search than {@link #VALID_DATASET} except that deprecated objects
         * are included in the search.
         */
        ALL_DATASET
    }

    /**
     * The criterion for determining if a candidate found by {@code IdentifiedObjectFinder}
     * should be considered equals to the requested object.
     */
    static final ComparisonMode COMPARISON_MODE = ComparisonMode.APPROXIMATIVE;

    /**
     * The factory to use for creating objects. This is the factory specified at construction time.
     */
    protected final AuthorityFactory factory;

    /**
     * The proxy for objects creation. This is updated before every object to search.
     */
    private transient AuthorityFactoryProxy<?> proxy;

    /**
     * The cache or the adapter which is wrapping this finder, or {@code null} if none.
     * An example of wrapper is {@link ConcurrentAuthorityFactory}'s finder.
     *
     * @see #setWrapper(IdentifiedObjectFinder)
     */
    private IdentifiedObjectFinder wrapper;

    /**
     * The domain of the search (for example whether to include deprecated objects in the search).
     *
     * @see #getSearchDomain()
     */
    private Domain domain = Domain.VALID_DATASET;

    /**
     * {@code true} if the search should ignore coordinate system axes.
     *
     * @see #isIgnoringAxes()
     */
    private boolean ignoreAxes;

    /**
     * {@code true} if the search should ignore names, aliases and identifiers. This is set to {@code true}
     * only when this finder is wrapped by a {@link MultiAuthoritiesFactory} finder, which performs it own
     * search based on identifiers.
     */
    boolean ignoreIdentifiers;

    /**
     * Creates a finder using the specified factory.
     *
     * <div class="note"><b>Design note:</b>
     * this constructor is protected because instances of this class should not be created directly.
     * Use {@link GeodeticAuthorityFactory#newIdentifiedObjectFinder()} instead.</div>
     *
     * @param factory The factory to scan for the identified objects.
     *
     * @see GeodeticAuthorityFactory#newIdentifiedObjectFinder()
     */
    protected IdentifiedObjectFinder(final AuthorityFactory factory) {
        ArgumentChecks.ensureNonNull("factory", factory);
        this.factory = factory;
    }

    /**
     * Declares that the given cache or adapter is the wrapper of this finder.
     * This method should be invoked at wrapper construction time.
     * An example of wrapper is {@link ConcurrentAuthorityFactory}'s finder.
     *
     * <p>This method also copies the configuration of the given finder, thus providing a central place
     * where to add calls to setters methods if such methods are added in a future SIS version.</p>
     *
     * @param other The cache or the adapter wrapping this finder.
     */
    final void setWrapper(final IdentifiedObjectFinder other) {
        wrapper = other;
        setSearchDomain(other.domain);
        setIgnoringAxes(other.ignoreAxes);
    }

    /**
     * Returns the domain of the search (for example whether to include deprecated objects in the search).
     * If {@code DECLARATION}, only a fast lookup based on embedded identifiers and names will be performed.
     * Otherwise an exhaustive full scan against all registered objects will be performed (may be slow).
     *
     * <p>The default value is {@link Domain#VALID_DATASET}.</p>
     *
     * @return The domain of the search.
     */
    public Domain getSearchDomain() {
        return domain;
    }

    /**
     * Sets the domain of the search (for example whether to include deprecated objects in the search).
     * If this method is never invoked, then the default value is {@link Domain#VALID_DATASET}.
     *
     * @param domain The domain of the search.
     */
    public void setSearchDomain(final Domain domain) {
        ArgumentChecks.ensureNonNull("domain", domain);
        this.domain = domain;
    }

    /**
     * Returns {@code true} if the search should ignore coordinate system axes.
     * The default value is {@code false}.
     *
     * @return {@code true} if the search should ignore coordinate system axes.
     */
    public boolean isIgnoringAxes() {
        return ignoreAxes;
    }

    /**
     * Sets whether the search should ignore coordinate system axes.
     * If this property is set to {@code true}, then the search will compare only the coordinate system type
     * and dimension. The axis names, orientation and units will be ignored. For example the {@code find(…)}
     * method may return a Coordinate Reference System object with (<var>latitude</var>, <var>longitude</var>)
     * axes even if the given object had (<var>longitude</var>, <var>latitude</var>) axes.
     *
     * @param ignore {@code true} if the search should ignore coordinate system axes.
     */
    public void setIgnoringAxes(final boolean ignore) {
        ignoreAxes = ignore;
    }

    /**
     * Returns {@code true} if a candidate found by {@code IdentifiedObjectFinder} should be considered equals to the
     * requested object. This method invokes the {@code equals(…)} method on the {@code candidate} argument instead
     * than on the user-specified {@code object} on the assumption that implementations coming from the factory are
     * more reliable than user-specified objects.
     */
    private boolean match(final IdentifiedObject candidate, final IdentifiedObject object) {
        return Utilities.deepEquals(candidate, object, ignoreAxes ? ComparisonMode.ALLOW_VARIANT : COMPARISON_MODE);
    }

    /**
     * Returns the cached value for the given object, or {@code null} if none.
     */
    Set<IdentifiedObject> getFromCache(final IdentifiedObject object) {
        return (wrapper != null) ? wrapper.getFromCache(object) : null;
    }

    /**
     * Stores the given result in the cache, if any.
     * This method will be invoked by {@link #find(IdentifiedObject)}
     * only if {@link #getSearchDomain()} is not {@link Domain#DECLARATION}.
     *
     * @return The given {@code result}, or another set equal to the result if it has been computed
     *         concurrently in another thread.
     */
    Set<IdentifiedObject> cache(final IdentifiedObject object, Set<IdentifiedObject> result) {
        if (wrapper != null) {
            result = wrapper.cache(object, result);
        }
        return result;
    }

    /**
     * Lookups objects which are approximatively equal to the specified object.
     * The default implementation tries to instantiate some {@linkplain AbstractIdentifiedObject identified objects}
     * from the authority factory specified at construction time, in the following order:
     *
     * <ul>
     *   <li>If the specified object contains {@linkplain AbstractIdentifiedObject#getIdentifiers() identifiers}
     *       associated to the same authority than the factory, then those identifiers are used for
     *       {@linkplain GeodeticAuthorityFactory#createObject(String) creating objects} to be tested.</li>
     *   <li>If the authority factory can create objects from their {@linkplain AbstractIdentifiedObject#getName() name}
     *       in addition of identifiers, then the name and {@linkplain AbstractIdentifiedObject#getAlias() aliases} are
     *       used for creating objects to be tested.</li>
     *   <li>If a full scan of the dataset is allowed, then full {@linkplain #getCodeCandidates set of candidate codes}
     *       is used for creating objects to be tested.</li>
     * </ul>
     *
     * The created objects which are equal to the specified object in the
     * the sense of {@link ComparisonMode#APPROXIMATIVE} are returned.
     *
     * @param  object The object looked up.
     * @return The identified objects, or an empty set if not found.
     * @throws FactoryException if an error occurred while creating an object.
     */
    public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
        ArgumentChecks.ensureNonNull("object", object);
        Set<IdentifiedObject> result = getFromCache(object);
        if (result == null) {
            final AuthorityFactoryProxy<?> previous = proxy;
            proxy = AuthorityFactoryProxy.getInstance(object.getClass());
            try {
                if (!ignoreIdentifiers) {
                    /*
                     * First check if one of the identifiers can be used to find directly an identified object.
                     * Verify that the object that we found is actually equal to given one; we do not blindly
                     * trust the identifiers in the user object.
                     */
                    IdentifiedObject candidate = createFromIdentifiers(object);
                    if (candidate != null) {
                        return Collections.singleton(candidate);    // Not worth to cache.
                    }
                    /*
                     * We are unable to find the object from its identifiers. Try a quick name lookup.
                     * Some implementations like the one backed by the EPSG database are capable to find
                     * an object from its name.
                     */
                    candidate = createFromNames(object);
                    if (candidate != null) {
                        return Collections.singleton(candidate);    // Not worth to cache.
                    }
                }
                /*
                 * Here we exhausted the quick paths.
                 * Perform a full scan (costly) if we are allowed to, otherwise abandon.
                 */
                if (domain == Domain.DECLARATION) {
                    return Collections.emptySet();              // Do NOT cache.
                }
                result = createFromCodes(object);
            } finally {
                proxy = previous;
            }
            result = cache(object, result);     // Costly operation (even if the result is empty) worth to cache.
        }
        return result;
    }

    /**
     * Lookups only one object which is approximatively equal to the specified object.
     * This method invokes {@link #find(IdentifiedObject)}, then examine the returned {@code Set} as below:
     *
     * <ul>
     *   <li>If the set is empty, then this method returns {@code null}.</li>
     *   <li>If the set contains exactly one element, then this method returns that element.</li>
     *   <li>If the set contains more than one element, but only one element has the same axis order
     *       than {@code object} and all other elements have different axis order,
     *       then this method returns the single element having the same axis order.</li>
     *   <li>Otherwise this method considers that there is ambiguity and returns {@code null}.</li>
     * </ul>
     *
     * @param  object The object looked up.
     * @return The identified object, or {@code null} if none or ambiguous.
     * @throws FactoryException if an error occurred while creating an object.
     */
    public IdentifiedObject findSingleton(final IdentifiedObject object) throws FactoryException {
        /*
         * Do not invoke Set.size() because it may be a costly operation if the subclass
         * implements a mechanism that create IdentifiedObject instances only on demand.
         */
        IdentifiedObject result = null;
        boolean sameAxisOrder = false;
        boolean ambiguous = false;
        try {
            for (final IdentifiedObject candidate : find(object)) {
                final boolean so = !ignoreAxes || Utilities.deepEquals(candidate, object, COMPARISON_MODE);
                if (result != null) {
                    ambiguous = true;
                    if (sameAxisOrder && so) {
                        return null;            // Found two matches even when taking in account axis order.
                    }
                }
                result = candidate;
                sameAxisOrder = so;
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        return (sameAxisOrder || !ambiguous) ? result : null;
    }

    /**
     * Creates an object equals (optionally ignoring metadata), to the specified object
     * using only the {@linkplain AbstractIdentifiedObject#getIdentifiers identifiers}.
     * If no such object is found, returns {@code null}.
     *
     * <p>This method may be used in order to get a fully identified object from a partially identified one.</p>
     *
     * @param  object The object looked up.
     * @return The identified object, or {@code null} if not found.
     * @throws FactoryException if an error occurred while creating an object.
     *
     * @see #createFromCodes(IdentifiedObject)
     * @see #createFromNames(IdentifiedObject)
     */
    private IdentifiedObject createFromIdentifiers(final IdentifiedObject object) throws FactoryException {
        for (final Identifier id : object.getIdentifiers()) {
            final String code = IdentifiedObjects.toString(id);
            /*
             * We will process only codes with a namespace (e.g. "AUTHORITY:CODE") for avoiding ambiguity.
             * We do not try to check by ourselves if the identifier is in the namespace of the factory,
             * because calling factory.getAuthorityCodes() or factory.getCodeSpaces() may be costly for
             * some implementations.
             */
            if (code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR) >= 0) {
                final IdentifiedObject candidate;
                try {
                    candidate = create(code);
                } catch (NoSuchAuthorityCodeException e) {
                    // The identifier was not recognized. No problem, let's go on.
                    exceptionOccurred(e);
                    continue;
                }
                if (match(candidate, object)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Creates an object equals (optionally ignoring metadata), to the specified object using only the
     * {@linkplain AbstractIdentifiedObject#getName name} and {@linkplain AbstractIdentifiedObject#getAlias aliases}.
     * If no such object is found, returns {@code null}.
     *
     * <p>This method may be used with some {@linkplain GeodeticAuthorityFactory authority factory}
     * implementations like the one backed by the EPSG database, which are capable to find an object
     * from its name when the identifier is unknown.</p>
     *
     * @param  object The object looked up.
     * @return The identified object, or {@code null} if not found.
     * @throws FactoryException if an error occurred while creating an object.
     *
     * @see #createFromCodes(IdentifiedObject)
     * @see #createFromIdentifiers(IdentifiedObject)
     */
    private IdentifiedObject createFromNames(final IdentifiedObject object) throws FactoryException {
        String code = object.getName().getCode();
        IdentifiedObject candidate;
        try {
            candidate = create(code);
        } catch (FactoryException e) {
            /*
             * The identifier was not recognized. We will continue later will aliases.
             * Note: we catch a more generic exception than NoSuchAuthorityCodeException because
             *       this attempt may fail for various reasons (character string not supported
             *       by the underlying database for primary key, duplicated name found, etc.).
             */
            exceptionOccurred(e);
            candidate = null;
        }
        if (match(candidate, object)) {
            return candidate;
        }
        for (final GenericName id : object.getAlias()) {
            code = id.toString();
            try {
                candidate = create(code);
            } catch (FactoryException e) {
                // The name was not recognized. No problem, let's go on.
                exceptionOccurred(e);
                continue;
            }
            if (match(candidate, object)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Creates an object equals (optionally ignoring metadata), to the specified object.
     * This method scans the {@linkplain #getCodeCandidates(IdentifiedObject) authority codes},
     * creates the objects and returns the first one which is equal to the specified object in
     * the sense of {@link Utilities#deepEquals(Object, Object, ComparisonMode)}.
     *
     * <p>This method may be used in order to get a fully {@linkplain AbstractIdentifiedObject identified object}
     * from an object without {@linkplain AbstractIdentifiedObject#getIdentifiers() identifiers}.</p>
     *
     * <p>Scanning the whole set of authority codes may be slow. Users should try
     * <code>{@linkplain #createFromIdentifiers createFromIdentifiers}(object)</code> and/or
     * <code>{@linkplain #createFromNames createFromNames}(object)</code> before to fallback
     * on this method.</p>
     *
     * @param  object The object looked up.
     * @return The identified object, or {@code null} if not found.
     * @throws FactoryException if an error occurred while scanning through authority codes.
     *
     * @see #createFromIdentifiers(IdentifiedObject)
     * @see #createFromNames(IdentifiedObject)
     */
    Set<IdentifiedObject> createFromCodes(final IdentifiedObject object) throws FactoryException {
        final Set<IdentifiedObject> result = new LinkedHashSet<IdentifiedObject>();     // We need to preserve order.
        for (final String code : getCodeCandidates(object)) {
            final IdentifiedObject candidate;
            try {
                candidate = create(code);
            } catch (FactoryException e) {
                exceptionOccurred(e);
                continue;
            }
            if (match(candidate, object)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Creates an object for the given code. This method is invoked by the default {@link #find(IdentifiedObject)}
     * method implementation of for each code returned by the {@link #getCodeCandidates(IdentifiedObject)} method,
     * in iteration order.
     *
     * @param  code The authority code for which to create an object.
     * @return The identified object for the given code, or {@code null} to stop attempts.
     * @throws FactoryException if an error occurred while creating the object.
     */
    private IdentifiedObject create(final String code) throws FactoryException {
        return (IdentifiedObject) proxy.createFromAPI(factory, code);
    }

    /**
     * Returns a set of authority codes that <strong>may</strong> identify the same object than the specified one.
     * The returned set must contains <em>at least</em> the code of every objects that are
     * {@linkplain ComparisonMode#APPROXIMATIVE approximatively equal} to the specified one.
     * However the set may conservatively contains the code for more objects if an exact search is too expensive.
     *
     * <p>This method is invoked by the default {@link #find(IdentifiedObject)} method implementation.
     * The caller iterates through the returned codes, instantiate the objects and compare them with
     * the specified one in order to determine which codes are really applicable.
     * The iteration stops as soon as a match is found (in other words, if more than one object is equals
     * to the specified one, then the {@code find(…)} method selects the first one in iteration order).</p>
     *
     * <div class="section">Default implementation</div>
     * The default implementation returns the same set than
     * <code>{@linkplain GeodeticAuthorityFactory#getAuthorityCodes(Class) getAuthorityCodes}(type)</code>
     * where {@code type} is the interface specified at construction type.
     * Subclasses should override this method in order to return a smaller set, if they can.
     *
     * @param  object The object looked up.
     * @return A set of code candidates.
     * @throws FactoryException if an error occurred while fetching the set of code candidates.
     */
    protected Set<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
        return factory.getAuthorityCodes(proxy.type.asSubclass(IdentifiedObject.class));
    }

    /**
     * Invoked when an exception occurred during the creation of a candidate from a code.
     */
    private static void exceptionOccurred(final FactoryException exception) {
        Logging.recoverableException(Logging.getLogger(Loggers.CRS_FACTORY), IdentifiedObjectFinder.class, "find", exception);
    }
}
