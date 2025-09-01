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

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.privy.FilteredIterator;
import org.apache.sis.referencing.privy.LazySet;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Semaphores;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;


/**
 * Searches in an authority factory for objects approximately equal to a given object.
 * This class can be used for fetching a fully defined {@linkplain IdentifiedObject identified object}
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
     * The domain of the search (for example, whether to include deprecated objects in the search).
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
         * Fast lookup based only on declared identifiers.
         * If those identification information does not allow to locate an object in the factory,
         * then the {@code find(…)} method will return an empty set instead of performing
         * an exhaustive search in the geodetic dataset.
         *
         * <h4>Example</h4>
         * If {@link #find(IdentifiedObject)} is invoked with an object having the {@code "4326"}
         * {@linkplain IdentifiedObject#getIdentifiers() identifier}, then the {@code find(…)} method will invoke
         * <code>factory.{@linkplain GeodeticAuthorityFactory#createGeographicCRS(String) createGeographicCRS}("4326")</code>
         * and compare the result with the object to search.
         * If the two objects do not match, then some implementations may perform another attempt using the
         * {@linkplain IdentifiedObject#getName() object name}. If using the name does not work neither,
         * then {@code find(…)} method makes no other attempt and returns an empty set.
         */
        DECLARATION,

        /**
         * Lookup based on declared identifiers and on non-deprecated objects known to the factory.
         * First, a fast lookup is performed as described in {@link #DECLARATION}.
         * If the last lookup found some matches, those matches are returned without scanning the rest of the database.
         * It may be an incomplete set compared to what {@link #EXHAUSTIVE_VALID_DATASET} would have returned.
         * If the fast lookup gave no result, only then an exhaustive search is performed by scanning
         * the content of the geodetic dataset.
         *
         * <p>This is the default domain of {@link IdentifiedObjectFinder}.</p>
         *
         * <h4>Example</h4>
         * If {@link #find(IdentifiedObject)} is invoked with an object equivalent to the
         * {@linkplain org.apache.sis.referencing.CommonCRS#WGS84 WGS84} geographic <abbr>CRS</abbr>
         * but without declaring the {@code "4326"} identifier and without the <q>WGS 84</q> name,
         * then the initial lookup described in {@link #DECLARATION} will give no result.
         * As a fallback, the {@code find(…)} method scans the geodetic dataset in search
         * for geographic <abbr>CRS</abbr> equivalent to the specified object.
         * It may be a costly operation.
         */
        VALID_DATASET,

        /**
         * Search unconditionally based on all valid (non-deprecated) objects known to the factory.
         * This is similar to {@link #VALID_DATASET} except that the fast {@link #DECLARATION} lookup is skipped.
         * Instead, a potentially costly scan of the geodetic dataset is unconditionally performed
         * (unless the result is already in the cache).
         *
         * <h4>Use case</h4>
         * The <abbr>EPSG</abbr> geodetic dataset sometimes contains two definitions for almost identical
         * geographic <abbr>CRS</abbr>, one with (<var>latitude</var>, <var>longitude</var>) axis order
         * and one with reverse order (e.g. EPSG::4171 versus EPSG::7084). It is sometimes useful to know
         * all variants of a given <abbr>CRS</abbr> in a search {@linkplain #isIgnoringAxes() ignoring axis order}.
         * The {@link #VALID_DATASET} domain may not give a complete set because the "fast lookup by identifiers"
         * optimization may prevent {@link IdentifiedObjectFinder} to scan the rest of the geodetic dataset.
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
     * <h4><abbr>API</abbr> note</h4>
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
     * where to add calls to setter methods if such methods are added in a future <abbr>SIS</abbr> version.</p>
     *
     * @param  other  the cache or the adapter wrapping this finder.
     */
    final void setWrapper(final IdentifiedObjectFinder other) {
        wrapper = other;
        setSearchDomain(other.domain);
        setIgnoringAxes(other.ignoreAxes);
    }

    /**
     * Returns the domain of the search (for example, whether to include deprecated objects in the search).
     * If the domain is {@code DECLARATION}, then the {@code find(…)} method will only perform a fast lookup
     * based on the identifiers and the names of the object to search.
     * Otherwise, an exhaustive scan of the geodetic dataset will be performed (may be slow).
     *
     * <p>The default value is {@link Domain#VALID_DATASET}.</p>
     *
     * @return the domain of the search.
     */
    public Domain getSearchDomain() {
        return domain;
    }

    /**
     * Sets the domain of the search (for example, whether to include deprecated objects in the search).
     * If this method is never invoked, the default value is {@link Domain#VALID_DATASET}.
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
     * Returns the comparison mode to use when comparing a candidate against the object to search.
     */
    private ComparisonMode getComparisonMode() {
        return ignoreAxes ? ComparisonMode.ALLOW_VARIANT : ComparisonMode.APPROXIMATE;
    }

    /**
     * Returns {@code true} if a candidate created by a factory should be considered equal to the object to search.
     * The {@code mode} and {@code proxy} arguments may be snapshots of the {@code IdentifiedObjectFinder}'s state
     * taken at the time when the {@link Instances} iterable has been created.
     *
     * <h4>Implementation note</h4>
     * This method invokes the {@code equals(…)} method on the {@code candidate} argument instead of {@code object}
     * specified by the user on the assumption that implementations coming from the factory are more reliable than
     * user-specified objects.
     *
     * @param  candidate  an object created by an authority factory.
     * @param  object     the user-specified object to search.
     * @param  mode       value of {@link #getComparisonMode()} (may be a snapshot).
     * @param  proxy      value of {@link #proxy} (may be a snapshot).
     * @return whether the given candidate can be considered equal to the object to search.
     *
     * @see #createAndFilter(AuthorityFactory, String, IdentifiedObject)
     */
    private static boolean match(final IdentifiedObject candidate, final IdentifiedObject object,
                                 final ComparisonMode mode, final AuthorityFactoryProxy<?> proxy)
    {
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
     * This is checked by {@link #find(IdentifiedObject)} before actual search.
     * The returned set (if non-null) should be unmodifiable.
     *
     * @param  object  the user-specified object to search.
     * @return the cached result of the find operation, or {@code null} if none.
     */
    Set<IdentifiedObject> getFromCache(final IdentifiedObject object) {
        return (wrapper != null) ? wrapper.getFromCache(object) : null;
    }

    /**
     * Stores the given result in the cache, if any. If this method chooses to cache the given set,
     * then it shall wrap or copy the given set in an unmodifiable set and returns the result.
     *
     * @param  object  the user-specified object which was searched.
     * @param  result  the search result. It will potentially be copied.
     * @return a set with the same content as {@code result}.
     */
    Set<IdentifiedObject> cache(final IdentifiedObject object, Set<IdentifiedObject> result) {
        if (wrapper != null) {
            result = wrapper.cache(object, result);
        }
        return result;
    }

    /**
     * Looks up only one object which is approximately equal to the specified object.
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
     * @throws FactoryException if an error occurred while fetching the authority code candidates.
     */
    @OptionalCandidate
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
                boolean matchAxes = !ignoreAxes || Utilities.deepEquals(candidate, object, ComparisonMode.APPROXIMATE);
                if (result != null) {
                    ambiguous = true;
                    if (sameAxisOrder & matchAxes) {
                        return null;            // Found two matches even when taking in account axis order.
                    }
                }
                result = candidate;
                sameAxisOrder = matchAxes;
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        return (sameAxisOrder || !ambiguous) ? result : null;
    }

    /**
     * Looks up objects which are approximately equal to the specified object.
     * This method tries to instantiate objects identified by the {@linkplain #getCodeCandidates set of candidate codes}
     * using the {@linkplain #factory authority factory} specified at construction time.
     * {@link FactoryException}s thrown during object creations are logged and otherwise ignored.
     * The successfully created objects which are equal to the specified object in the sense of
     * {@link ComparisonMode#APPROXIMATE} or {@link ComparisonMode#ALLOW_VARIANT ALLOW_VARIANT}
     * (depending on whether {@linkplain #isIgnoringAxes() axes are ignored}) are included in the returned set.
     *
     * <h4>Exception handling</h4>
     * This method may return a lazy set, in which case some checked exceptions may occur at iteration time.
     * These exceptions are wrapped in a {@link BackingStoreException}.
     *
     * @param  object  the object looked up.
     * @return the identified objects, or an empty set if not found.
     * @throws FactoryException if an error occurred while fetching the authority code candidates.
     */
    public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
        Set<IdentifiedObject> result = getFromCache(Objects.requireNonNull(object));
        if (result == null) try {
            final AuthorityFactoryProxy<?> previousProxy = proxy;
            proxy = AuthorityFactoryProxy.getInstance(object.getClass());
            try {
                result = createFromCodes(object);
            } finally {
                proxy = previousProxy;
            }
            result = cache(object, result);     // Costly operations (even if the result is empty) are worth to cache.
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        return result;
    }

    /**
     * Creates objects approximately equal to the specified object by iterating over authority code candidates.
     * This method is invoked by {@link #find(IdentifiedObject)} after it has been verified that the result is not in the cache.
     * The default implementation iterates over the {@linkplain #getCodeCandidates(IdentifiedObject) authority code candidates},
     * creates the objects and returns the ones which are approximately equal to the specified object.
     *
     * <h4>Exception handling</h4>
     * This method may return a lazy set, in which case some checked exceptions may occur at iteration time.
     * These exceptions are wrapped in a {@link BackingStoreException}.
     *
     * @param  object  the object looked up.
     * @return the identified objects, or an empty set if not found.
     * @throws FactoryException if an error occurred while fetching the authority code candidates.
     * @throws BackingStoreException allowed for convenience, will be unwrapped by the caller.
     */
    Set<IdentifiedObject> createFromCodes(final IdentifiedObject object) throws FactoryException {
        return new Instances(this, object);
    }

    /**
     * The set of geodetic instances created from the code candidates.
     * This is a lazy set, where each instance is created only when first requested.
     * Checked exceptions are wrapped in {@link BackingStoreException}.
     *
     * <h2>Implementation note</h2>
     * This class should not keep a reference to the enclosing class (it is static for that reason),
     * because some subclasses of {@link IdentifiedObjectFinder} are short-lived data access objects
     * holding resources such as database connections.
     */
    private static final class Instances extends LazySet<IdentifiedObject> implements Function<String, IdentifiedObject> {
        /** Copy of {@link IdentifiedObjectFinder#factory}. */
        private final AuthorityFactory factory;

        /** Snapshot of {@link IdentifiedObjectFinder#proxy}. */
        private final AuthorityFactoryProxy<?> proxy;

        /** The authority codes form which to create object candidates. */
        private final Iterable<String> codes;

        /** The comparison mode for deciding if a candidate is a match. */
        private final ComparisonMode mode;

        /** Previously created objects for removing duplicates. */
        private final Set<IdentifiedObject> existing;

        /** The user-specified object to search. */
        private final IdentifiedObject object;

        /** Whether at least one match has been found. */
        private boolean hasMatches;

        /**
         * Creates a new collection for the given object to search.
         *
         * @param  factory  value of {@link IdentifiedObjectFinder#factory}.
         * @param  object   the user-specified object to search.
         * @throws FactoryException if an error occurred while fetching the authority code candidates.
         */
        Instances(final IdentifiedObjectFinder source, final IdentifiedObject object) throws FactoryException {
            factory  = source.factory;
            proxy    = source.proxy;
            codes    = source.getCodeCandidates(object);
            mode     = source.getComparisonMode();
            existing = new HashSet<>();
            this.object = object;
        }

        /**
         * Creates the iterator which will create objects from authority codes.
         * This method will be invoked only when first needed and at most once, unless {@link #reload()} is invoked.
         *
         * @return iterator over objects created from authority codes.
         */
        @Override
        protected Iterator<IdentifiedObject> createSourceIterator() {
            return new FilteredIterator<>(codes.iterator(), this);
        }

        /**
         * Creates an object from the given code, and verifies if it is considered equals to the object to search.
         * This method is invoked by {@link FilteredIterator}. Checked exceptions are logged. If the object cannot
         * be created or is not approximately equal to the object to search, then this method returns {@code true},
         * which is understood by {@link FilteredIterator} as an instruction to look for the next element.
         *
         * @param  code  one of the authority codes returned by {@link #getCodeCandidates(IdentifiedObject)}.
         * @return object created from the given code if it is approximately equal to the object to search.
         */
        @Override
        public IdentifiedObject apply(final String code) {
            final boolean finer = Semaphores.queryAndSet(Semaphores.FINER_OBJECT_CREATION_LOGS);
            try {
                IdentifiedObject candidate = (IdentifiedObject) proxy.createFromAPI(factory, code);
                if (match(candidate, object, mode, proxy) && existing.add(candidate)) {
                    if (!hasMatches) {
                        hasMatches = true;
                        if (codes instanceof Disposable) {
                            ((Disposable) codes).dispose();   // For stopping iteration after the easy matches.
                        }
                    }
                    return candidate;
                }
            } catch (FactoryException e) {
                exceptionOccurred(e);
            } finally {
                Semaphores.clearIfFalse(Semaphores.FINER_OBJECT_CREATION_LOGS, finer);
            }
            return null;
        }
    }

    /**
     * Creates an object from the given code, and verifies if it is considered equals to the object to search.
     * This is a helper method for subclasses. This method does the same work as {@link Instances}, but allows
     * the subclass to specify an alternative authority factory.
     *
     * @param  factory  the authority factory to use. This is not necessarily {@link #factory}.
     * @param  code     the authority code for which to create an object candidate.
     * @param  object   the user-specified object to search.
     * @return instance for the given code, or {@code null} if not approximately equal to the object to search.
     * @throws NoSuchAuthorityCodeException if no object is found for the given code. It may happen if {@code code}
     *         was a name or alias instead of an identifier and the factory supports only search by identifier.
     * @throws FactoryException if an error occurred while creating the object.
     */
    final IdentifiedObject createAndFilter(final AuthorityFactory factory, final String code, final IdentifiedObject object)
            throws FactoryException
    {
        final boolean finer = Semaphores.queryAndSet(Semaphores.FINER_OBJECT_CREATION_LOGS);
        try {
            final var candidate = (IdentifiedObject) proxy.createFromAPI(factory, code);
            return match(candidate, object, getComparisonMode(), proxy) ? candidate : null;
        } finally {
            Semaphores.clearIfFalse(Semaphores.FINER_OBJECT_CREATION_LOGS, finer);
        }
    }

    /**
     * Returns a set of authority codes that <em>may</em> identify the same object as the specified one.
     * The codes may be determined from object identifiers, names, aliases or extensive search in the geodetic dataset.
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
     * The default implementation returns codes defined from {@code object.getIdentifiers()},
     * or {@code factory.getAuthorityCodes(type)} where {@code type} is derived from {@code object} class,
     * or a combination of both collection, depending on the {@linkplain #getSearchDomain() search domain}.
     * Subclasses should override this method in order to return a smaller set, if they can.
     *
     * @param  object  the object looked up.
     * @return a set of code candidates.
     * @throws FactoryException if an error occurred while fetching the set of code candidates.
     *
     * @see IdentifiedObject#getIdentifiers()
     * @see AuthorityFactory#getAuthorityCodes(Class)
     */
    protected Iterable<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
        /*
         * Undocumented contract: if the Iterable implements `Dispose`, its method will be invoked
         * after the first match has been found. This is interpreted as a hint that the iteration
         * can stop earlier than it would normally do.
         */
        final Class<? extends IdentifiedObject> type = proxy.type.asSubclass(IdentifiedObject.class);
        if (domain == Domain.EXHAUSTIVE_VALID_DATASET) {
            return factory.getAuthorityCodes(type);
        }
        final boolean easy = (domain == Domain.DECLARATION);
        final Set<ReferenceIdentifier> identifiers = object.getIdentifiers();
        if (identifiers.isEmpty()) {
            return easy ? Set.of() : factory.getAuthorityCodes(type);
        }
        return new Codes(factory, identifiers, easy ? null : type);
    }

    /**
     * Union of identifier codes followed by code candidates fetched from the geodetic dataset.
     * The codes returned by this iterable are, in this order:
     *
     * <ol>
     *   <li>{@link IdentifiedObject#getIdentifiers()} (filtered with {@link #apply(ReferenceIdentifier)})</li>
     *   <li>{@link AuthorityFactory#getAuthorityCodes(Class)} (skipped if the class is null)</li>
     * </ol>
     *
     * <h2>Implementation note</h2>
     * This class should not keep a reference to the enclosing class (it is static for that reason),
     * because some subclasses of {@link IdentifiedObjectFinder} are short-lived data access objects
     * holding resources such as database connections.
     */
    private static final class Codes implements Iterable<String>, Function<ReferenceIdentifier, String>, Disposable {
        /** Copy of {@link IdentifiedObjectFinder#factory}. */
        private final AuthorityFactory factory;

        /** The identifiers of the object to search. */
        private final Set<ReferenceIdentifier> identifiers;

        /** Type of objects to request for code candidates, or {@code null} for not requesting code candidates. */
        private Class<? extends IdentifiedObject> type;

        /** Code candidates, created when first needed. This collection may be costly to create and/or to use. */
        private Iterable<String> codes;

        /**
         * Creates a new union of identifier codes and candidate codes.
         *
         * @param factory      value of {@link IdentifiedObjectFinder#factory}.
         * @param identifiers  identifiers of the object to search.
         * @param type         type of objects to request for code candidates, or {@code null}.
         */
        Codes(final AuthorityFactory factory, final Set<ReferenceIdentifier> identifiers, final Class<? extends IdentifiedObject> type) {
            this.factory     = factory;
            this.identifiers = identifiers;
            this.type        = type;
        }

        /**
         * Invoked when the caller requested to stop the iteration after the current group of elements.
         * A group of elements is either the codes specified by the identifiers, or the codes found in
         * the database. We will avoid to stop in the middle of a group.
         *
         * <p>This is an undocumented feature of {@link #createFromCodes(IdentifiedObject)}
         * for stopping an iteration early when at least one match has been found.</p>
         */
        @Override
        public void dispose() {
            type = null;
        }

        /**
         * Converts the given identifier to a code returned by {@link #getIdentifiers()}.
         * We accept only codes with a namespace (e.g. "AUTHORITY:CODE") for avoiding ambiguity.
         * We do not try to check by ourselves if the identifier is in the namespace of the factory,
         * because calling {@code factory.getAuthorityCodes()} or {@code factory.getCodeSpaces()}
         * may be costly for some implementations.
         */
        @Override
        public String apply(final ReferenceIdentifier id) {
            final String code = IdentifiedObjects.toString(id);
            return (code.indexOf(Constants.DEFAULT_SEPARATOR) >= 0) ? code : null;
        }

        /**
         * Returns an iterator over codes of the identifiers of the object to search.
         * The iteration does not include the {@code Identifier} of the name because, at least
         * in Apache <abbr>SIS</abbr> implementations, the factories that accept object names
         * already override {@link #getCodeCandidates(IdentifiedObject)} for including names.
         */
        final Iterator<String> getIdentifiers() {
            return new FilteredIterator<>(identifiers.iterator(), this);
        }

        /**
         * Returns an iterator over the code candidates. This method should be invoked only in last resort.
         *
         * @throws BackingStoreException if an error occurred while fetching the collection of authority codes.
         */
        final Iterator<String> getAuthorityCodes() {
            if (codes == null) {
                if (type == null) {
                    codes = Set.of();
                } else try {
                    codes = factory.getAuthorityCodes(type);
                } catch (FactoryException e) {
                    throw new BackingStoreException(e);
                }
            }
            return codes.iterator();
        }

        /**
         * Returns an iterator over the identifiers followed by the code candidates.
         */
        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                /** The iterator of the code to return. */
                private Iterator<String> codes = getIdentifiers();

                /** Whether {@link #codes} is for code candidates. */
                private boolean isCodeCandidates;

                /** Returns whether there is more codes to return. */
                @Override public boolean hasNext() {
                    if (!isCodeCandidates) {
                        if (codes.hasNext()) return true;
                        codes = getAuthorityCodes();
                        isCodeCandidates = true;
                    }
                    return codes.hasNext();
                }

                /** Returns the next code. */
                @Override public String next() {
                    if (!(isCodeCandidates || codes.hasNext())) {
                        codes = getAuthorityCodes();
                    }
                    return codes.next();
                }
            };
        }
    }

    /**
     * Invoked when an exception occurred during the creation of a candidate from a code.
     */
    static void exceptionOccurred(final FactoryException exception) {
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
         * Looks up objects which are approximately equal to the specified object.
         * The default method implementation delegates the work to the finder specified by {@link #delegate()}.
         */
        @Override
        public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final IdentifiedObjectFinder delegate = delegate();
            return (delegate != this) ? delegate.find(object) : super.find(object);
        }

        /**
         * Looks up only one object which is approximately equal to the specified object.
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
