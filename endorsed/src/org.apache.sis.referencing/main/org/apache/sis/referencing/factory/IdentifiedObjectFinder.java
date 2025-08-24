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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Semaphores;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;


/**
 * Searches in an authority factory for objects approximately equal to a given object.
 * This class can be used for fetching a fully defined {@linkplain AbstractIdentifiedObject identified object}
 * from an incomplete one, for example from an object without "{@code ID[…]}" or "{@code AUTHORITY[…]}"
 * element in <i>Well Known Text</i>.
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
 * <h2>Thread safety</h2>
 * {@code IdentifiedObjectFinder} are <strong>not</strong> guaranteed to be thread-safe even if the underlying factory
 * is thread-safe. If concurrent searches are desired, then a new instance should be created for each thread.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see GeodeticAuthorityFactory#newIdentifiedObjectFinder()
 * @see IdentifiedObjects#newFinder(String)
 *
 * @since 0.7
 */
public class IdentifiedObjectFinder {
    /**
     * The domain of the search (for example whether to include deprecated objects in the search).
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.2
     *
     * @see #getSearchDomain()
     *
     * @since 0.7
     */
    public enum Domain {
        /**
         * Fast lookup based only on embedded identifiers and names. If those identification information
         * does not allow to locate an object in the factory, then the search will return an empty set.
         *
         * <h4>Example</h4>
         * If {@link IdentifiedObjectFinder#find(IdentifiedObject)} is invoked with an object having the {@code "4326"}
         * {@linkplain AbstractIdentifiedObject#getIdentifiers() identifier}, then the {@code find(…)} method will invoke
         * <code>factory.{@linkplain GeodeticAuthorityFactory#createGeographicCRS(String) createGeographicCRS}("4326")</code>
         * and compare the object from the factory with the object to search.
         * If the objects do not match, then another attempt will be done using the
         * {@linkplain AbstractIdentifiedObject#getName() object name}. If using name does not work neither,
         * then {@code find(…)} method makes no other attempt and returns an empty set.
         */
        DECLARATION,

        /**
         * Lookup based on valid (non-deprecated) objects known to the factory.
         * First, a fast lookup is performed based on {@link #DECLARATION}.
         * If the fast lookup gave no result, then a more extensive search is performed by scanning the content
         * of the dataset.
         *
         * <h4>Example</h4>
         * If {@link IdentifiedObjectFinder#find(IdentifiedObject)} is invoked with an object equivalent to the
         * {@linkplain org.apache.sis.referencing.CommonCRS#WGS84 WGS84} geographic CRS but does not declare the
         * {@code "4326"} identifier and does not have the <q>WGS 84</q> name, then the search based on
         * {@link #DECLARATION} will give no result. The {@code find(…)} method will then scan the dataset for
         * geographic CRS using equivalent datum and coordinate system. This may be a costly operation.
         *
         * This is the default domain of {@link IdentifiedObjectFinder}.
         */
        VALID_DATASET,

        /**
         * Lookup unconditionally based on all valid (non-deprecated) objects known to the factory.
         * This is similar to {@link #VALID_DATASET} except that the fast {@link #DECLARATION} lookup is skipped.
         * Instead, a potentially costly scan of the database is unconditionally performed
         * (unless the result is already in the cache).
         *
         * <p>This domain can be useful when the search {@linkplain #isIgnoringAxes() ignores axis order}.
         * If axis order is <em>not</em> ignored, then this domain usually has no advantage over {@link #VALID_DATASET}
         * (unless the geodetic dataset contains duplicated entries) to justify the performance cost.</p>
         *
         * <h4>Use case</h4>
         * The EPSG database sometimes contains two definitions for almost identical geographic CRS,
         * one with (<var>latitude</var>, <var>longitude</var>) axis order and one with reverse order
         * (e.g. EPSG::4171 versus EPSG::7084). It is sometimes useful to know all variants of a given CRS.
         * The {@link #VALID_DATASET} domain may not give a complete set because the "fast lookup by identifier"
         * optimization may prevent {@link IdentifiedObjectFinder} to scan the rest of the database.
         * This {@code EXHAUSTIVE_VALID_DATASET} domain forces such scan.
         *
         * @since 1.2
         */
        EXHAUSTIVE_VALID_DATASET,

        /**
         * Lookup based on all objects (both valid and deprecated) known to the factory.
         * This is the same search as {@link #VALID_DATASET} except that deprecated objects
         * are included in the search.
         */
        ALL_DATASET
    }

    /**
     * The criterion for determining if a candidate found by {@code IdentifiedObjectFinder}
     * should be considered equals to the requested object.
     */
    static final ComparisonMode COMPARISON_MODE = ComparisonMode.APPROXIMATE;

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
    private Domain domain;

    /**
     * {@code true} if the search should ignore coordinate system axes.
     *
     * @see #isIgnoringAxes()
     */
    private boolean ignoreAxes;

    /**
     * Creates a finder using the specified factory.
     *
     * <h4>API note</h4>
     * This constructor is protected because instances of this class should not be created directly.
     * Use {@link GeodeticAuthorityFactory#newIdentifiedObjectFinder()} instead.
     *
     * @param  factory  the factory to scan for the identified objects.
     *
     * @see GeodeticAuthorityFactory#newIdentifiedObjectFinder()
     */
    protected IdentifiedObjectFinder(final AuthorityFactory factory) {
        this.factory = Objects.requireNonNull(factory);
        this.domain  = Domain.VALID_DATASET;
    }

    /**
     * Declares that the given cache or adapter is the wrapper of this finder.
     * This method should be invoked at wrapper construction time.
     * An example of wrapper is {@link ConcurrentAuthorityFactory}'s finder.
     *
     * <p>This method also copies the configuration of the given finder, thus providing a central place
     * where to add calls to setters methods if such methods are added in a future SIS version.</p>
     *
     * @param  other  the cache or the adapter wrapping this finder.
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
     * @return the domain of the search.
     */
    public Domain getSearchDomain() {
        return domain;
    }

    /**
     * Sets the domain of the search (for example whether to include deprecated objects in the search).
     * If this method is never invoked, then the default value is {@link Domain#VALID_DATASET}.
     *
     * @param  domain  the domain of the search.
     */
    public void setSearchDomain(final Domain domain) {
        this.domain = Objects.requireNonNull(domain);
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
     * and dimension. The axis names, orientation and units will be ignored. For example, the {@code find(…)}
     * method may return a Coordinate Reference System object with (<var>latitude</var>, <var>longitude</var>)
     * axes even if the given object had (<var>longitude</var>, <var>latitude</var>) axes.
     *
     * @param  ignore  {@code true} if the search should ignore coordinate system axes.
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
        final ComparisonMode mode = ignoreAxes ? ComparisonMode.ALLOW_VARIANT : COMPARISON_MODE;
        if (Utilities.deepEquals(candidate, object, mode)) {
            return true;
        }
        if (Datum.class.isAssignableFrom(proxy.type)) {
            if (candidate instanceof Datum && object instanceof DatumEnsemble<?>) {
                return DatumOrEnsemble.isLegacyDatum((DatumEnsemble<?>) object, (Datum) candidate, mode);
            }
            if (candidate instanceof DatumEnsemble<?> && object instanceof Datum) {
                return DatumOrEnsemble.isLegacyDatum((DatumEnsemble<?>) candidate, (Datum) object, mode);
            }
        }
        return false;
    }

    /**
     * Returns the cached value for the given object, or {@code null} if none.
     * The returned set (if non-null) should be unmodifiable.
     */
    Set<IdentifiedObject> getFromCache(final IdentifiedObject object) {
        return (wrapper != null) ? wrapper.getFromCache(object) : null;
    }

    /**
     * Stores the given result in the cache, if any. If this method chooses to cache the given set,
     * then it shall wrap or copy the given set in an unmodifiable set and returns the result.
     *
     * @param  result  the search result as a modifiable set.
     * @return a set with the same content as {@code result}.
     */
    Set<IdentifiedObject> cache(final IdentifiedObject object, Set<IdentifiedObject> result) {
        if (wrapper != null) {
            result = wrapper.cache(object, result);
        }
        return result;
    }

    /**
     * Lookups objects which are approximately equal to the specified object.
     * This method tries to instantiate objects identified by the {@linkplain #getCodeCandidates set of candidate codes}
     * with the authority factory specified at construction time.
     * The created objects which are equal to the specified object in the
     * the sense of {@link ComparisonMode#APPROXIMATE} are returned.
     *
     * @param  object  the object looked up.
     * @return the identified objects, or an empty set if not found.
     * @throws FactoryException if an error occurred while creating an object.
     */
    public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
        Set<IdentifiedObject> result = getFromCache(Objects.requireNonNull(object));
        if (result == null) {
            final AuthorityFactoryProxy<?> previousProxy = proxy;
            proxy = AuthorityFactoryProxy.getInstance(object.getClass());
            try {
                if (domain != Domain.EXHAUSTIVE_VALID_DATASET) {
                    /*
                     * First check if one of the identifiers can be used to find directly an identified object.
                     * Verify that the object that we found is actually equal to given one; we do not blindly
                     * trust the identifiers in the user object.
                     */
                    IdentifiedObject candidate = createFromIdentifiers(object);
                    if (candidate != null) {
                        result = Set.of(candidate);
                    }
                }
                /*
                 * Here we exhausted the quick paths.
                 * Perform a full scan (costly) if we are allowed to, otherwise abandon.
                 */
                if (result == null) {
                    if (domain == Domain.DECLARATION) {
                        result = Set.of();
                    } else {
                        result = createFromCodes(object);
                    }
                }
            } finally {
                proxy = previousProxy;
            }
            result = cache(object, result);     // Costly operations (even if the result is empty) are worth to cache.
        }
        return result;
    }

    /**
     * Lookups only one object which is approximately equal to the specified object.
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
     * @param  object  the object looked up.
     * @return the identified object, or {@code null} if none or ambiguous.
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
                final boolean equalsIncludingAxes = !ignoreAxes || Utilities.deepEquals(candidate, object, COMPARISON_MODE);
                if (result != null) {
                    ambiguous = true;
                    if (sameAxisOrder & equalsIncludingAxes) {
                        return null;            // Found two matches even when taking in account axis order.
                    }
                }
                result = candidate;
                sameAxisOrder = equalsIncludingAxes;
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
     * @param  object  the object looked up.
     * @return the identified object, or {@code null} if not found.
     * @throws FactoryException if an error occurred while creating an object.
     *
     * @see #createFromCodes(IdentifiedObject)
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
            if (code.indexOf(Constants.DEFAULT_SEPARATOR) >= 0) {
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
     * Creates an object equals (optionally ignoring metadata), to the specified object.
     * This method scans the {@linkplain #getCodeCandidates(IdentifiedObject) authority codes},
     * creates the objects and returns the first one which is equal to the specified object in
     * the sense of {@link Utilities#deepEquals(Object, Object, ComparisonMode)}.
     *
     * <p>This method may be used in order to get a fully {@linkplain AbstractIdentifiedObject identified object}
     * from an object without {@linkplain AbstractIdentifiedObject#getIdentifiers() identifiers}.</p>
     *
     * @param  object  the object looked up.
     * @return the identified object, or {@code null} if not found.
     * @throws FactoryException if an error occurred while scanning through authority codes.
     *
     * @see #createFromIdentifiers(IdentifiedObject)
     */
    Set<IdentifiedObject> createFromCodes(final IdentifiedObject object) throws FactoryException {
        final var result = new LinkedHashSet<IdentifiedObject>();     // We need to preserve order.
        final boolean finer = Semaphores.queryAndSet(Semaphores.FINER_OBJECT_CREATION_LOGS);
        try {
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
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        } finally {
            Semaphores.clearIfFalse(Semaphores.FINER_OBJECT_CREATION_LOGS, finer);
        }
        return result;
    }

    /**
     * Creates an object for the given identifier, name or alias. This method is invoked by the default
     * {@link #find(IdentifiedObject)} method implementation with the following argument values, in order
     * (from less expensive to most expensive search operation):
     *
     * <ol>
     *   <li>All {@linkplain AbstractIdentifiedObject#getIdentifier() identifiers} of the object to search,
     *       formatted in an {@linkplain IdentifiedObjects#toString(Identifier) "AUTHORITY:CODE"} pattern.</li>
     *   <li>The {@linkplain AbstractIdentifiedObject#getName() name} of the object to search,
     *       {@linkplain org.apache.sis.referencing.NamedIdentifier#getCode() without authority}.</li>
     *   <li>All {@linkplain AbstractIdentifiedObject#getAlias() aliases} of the object to search.</li>
     *   <li>Each code returned by the {@link #getCodeCandidates(IdentifiedObject)} method, in iteration order.</li>
     * </ol>
     *
     * @param  code  the authority code for which to create an object.
     * @return the identified object for the given code, or {@code null} to stop attempts.
     * @throws NoSuchAuthorityCodeException if no object is found for the given code. It may happen if {@code code}
     *         was a name or alias instead of an identifier and the factory supports only search by identifier.
     * @throws FactoryException if an error occurred while creating the object.
     */
    private IdentifiedObject create(final String code) throws FactoryException {
        return (IdentifiedObject) proxy.createFromAPI(factory, code);
    }

    /**
     * Returns a set of authority codes that <em>may</em> identify the same object as the specified one.
     * The elements may be determined from object identifiers, from object names, or from a more extensive search in the database.
     * The effort in populating the returned set is specified by the {@linkplain #getSearchDomain() search domain}.
     * The returned set should contain at least the codes of every objects in the search domain
     * that are {@linkplain ComparisonMode#APPROXIMATE approximately equal} to the specified object.
     * However, the set may conservatively contain the codes for more objects if an exact search is too expensive.
     *
     * <p>This method is invoked by the default {@link #find(IdentifiedObject)} method implementation.
     * The caller iterates through the returned codes, instantiates the objects and compares them with
     * the specified object in order to determine which codes are really matching.
     * The iteration order should be the preference order.</p>
     *
     * <h4>Exceptions during iteration</h4>
     * An unchecked {@link BackingStoreException} may be thrown during the iteration if the implementation
     * fetches the codes lazily (when first needed) from the authority factory, and that action failed.
     * The exception cause is often the checked {@link FactoryException}.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns the same set as
     * <code>{@linkplain GeodeticAuthorityFactory#getAuthorityCodes(Class) getAuthorityCodes}(type)</code>
     * where {@code type} is the interface specified at construction type.
     * Subclasses should override this method in order to return a smaller set, if they can.
     *
     * @param  object  the object looked up.
     * @return a set of code candidates.
     * @throws FactoryException if an error occurred while fetching the set of code candidates.
     */
    protected Iterable<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
        return factory.getAuthorityCodes(proxy.type.asSubclass(IdentifiedObject.class));
    }

    /**
     * Invoked when an exception occurred during the creation of a candidate from a code.
     */
    private static void exceptionOccurred(final FactoryException exception) {
        if (GeodeticAuthorityFactory.LOGGER.isLoggable(Level.FINER)) {
            /*
             * use `getMessage()` instead of `getLocalizedMessage()` for
             * giving preference to the locale of system administrator.
             */
            Logging.completeAndLog(GeodeticAuthorityFactory.LOGGER, IdentifiedObjectFinder.class,
                                   "find", new LogRecord(Level.FINER, exception.getMessage()));
        }
    }

    /**
     * An object finder which delegates some or all work to another object finder.
     * The default implementation of all {@code Wrapper} methods delegates the work to the object finder
     * specified at construction time or in the last call to {@link #delegate(IdentifiedObjectFinder)}.
     * Subclasses can override methods for modifying some find operations.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.5
     * @since   1.5
     */
    public static abstract class Wrapper extends IdentifiedObjectFinder {
        /**
         * The finder doing the actual work, or {@code this} for using the
         * default method implementation provided by the parent class.
         */
        private IdentifiedObjectFinder delegate;

        /**
         * Creates a new object finder which will delegate the actual work to the given finder.
         *
         * @param  finder  the object finder to which to delegate the work.
         */
        protected Wrapper(final IdentifiedObjectFinder finder) {
            super(finder.factory);
            delegate = finder;
        }

        /**
         * Sets a new finder to which to delegate the actual search operations.
         * If the specified finder is {@code this}, then this class will delegate to the default
         * method implementations provided by the {@code IdentifiedObjectFinder} parent class.
         * Otherwise, the specified finder should be a new instance not in use by other code.
         *
         * @param  finder  the object finder to which to delegate the work. Can be {@code this}.
         * @throws FactoryException if the delegate cannot be set.
         */
        protected void delegate(final IdentifiedObjectFinder finder) throws FactoryException {
            if (delegate != null) {
                delegate.wrapper = null;
            }
            delegate = Objects.requireNonNull(finder);
        }

        /**
         * Returns the object finder to which to delegate the actual search operations.
         * If this method returns {@code this}, then the search operations will be delegated
         * to the default methods provided by the {@code IdentifiedObjectFinder} parent class.
         *
         * @return the object finder to which to delegate the work. May be {@code this}.
         * @throws FactoryException if the delegate cannot be created.
         */
        protected IdentifiedObjectFinder delegate() throws FactoryException {
            if (delegate != this) {
                delegate.setWrapper(this);  // Done on each call because it also copies the configuration.
            }
            return delegate;
        }

        /**
         * Lookups objects which are approximately equal to the specified object.
         * The default method implementation delegates the work to the finder specified by {@link #delegate()}.
         */
        @Override
        public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final IdentifiedObjectFinder delegate = delegate();
            return (delegate != this) ? delegate.find(object) : super.find(object);
        }

        /**
         * Lookups only one object which is approximately equal to the specified object.
         * The default method implementation delegates the work to the finder specified by {@link #delegate()}.
         */
        @Override
        public IdentifiedObject findSingleton(final IdentifiedObject object) throws FactoryException {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final IdentifiedObjectFinder delegate = delegate();
            return (delegate != this) ? delegate.findSingleton(object) : super.findSingleton(object);
        }

        /**
         * Creates an object equals (optionally ignoring metadata), to the specified object.
         * The default method implementation delegates the work to the finder specified by {@link #delegate()}.
         */
        @Override
        Set<IdentifiedObject> createFromCodes(final IdentifiedObject object) throws FactoryException {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final IdentifiedObjectFinder delegate = delegate();
            return (delegate != this) ? delegate.createFromCodes(object) : super.createFromCodes(object);
        }

        /**
         * Returns a set of authority codes that <em>may</em> identify the same object as the specified one.
         * The default method implementation delegates the work to the finder specified by {@link #delegate()}.
         */
        @Override
        protected Iterable<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final IdentifiedObjectFinder delegate = delegate();
            return (delegate != this) ? delegate.getCodeCandidates(object) : super.getCodeCandidates(object);
        }
    }
}
