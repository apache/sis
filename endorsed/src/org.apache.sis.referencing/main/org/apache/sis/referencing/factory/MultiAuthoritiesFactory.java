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

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.Unit;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.AbstractIterator;
import org.apache.sis.util.internal.shared.DefinitionURI;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.collection.SetOfUnknownSize;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.metadata.internal.shared.NameMeaning;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.LazySet;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;


/**
 * A factory that delegates the object creation to another factory determined from the <var>authority</var> part
 * in “<var>authority</var>:<var>code</var>” arguments.
 * The list of factories to use as delegates can be specified at construction time.
 *
 * <p>This factory requires that every codes given to a {@code createFoo(String)} method are prefixed by a namespace,
 * for example {@code "EPSG:4326"} or {@code "EPSG::4326"}.
 * When a {@code createFoo(String)} method is invoked, this class uses the <var>authority</var> part in the
 * “<var>authority</var>:<var>code</var>” argument for locating a factory capable to create a geodetic object
 * for the <var>code</var> part.  If a factory is found in the list of factories given at construction time,
 * then the work is delegated to that factory. Otherwise a {@link NoSuchAuthorityFactoryException} is thrown.</p>
 *
 * <h2>URI syntax</h2>
 * This factory can also parse URNs or URLs of the following forms:
 *
 * <ul>
 *   <li>{@code "urn:ogc:def:}<var>type</var>{@code :}<var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var>{@code "}</li>
 *   <li>{@code "http://www.opengis.net/def/}<var>type</var>{@code /}<var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var>{@code "}</li>
 *   <li>{@code "http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var>{@code "}</li>
 * </ul>
 *
 * In such cases, the <var>type</var> specified in the URN may be used for invoking a more specific method.
 * However, {@code MultiAuthoritiesFactory} uses the type information in the URN only for
 * delegating to a more specific method, never for delegating to a less specific method.
 * An exception will be thrown if the type in the URN is incompatible with the invoked method.
 *
 * <h3>Example</h3>
 * If <code>{@linkplain #createObject(String) createObject}("urn:ogc:def:<b>crs</b>:EPSG::4326")</code> is invoked,
 * then {@code MultiAuthoritiesFactory} will delegate (indirectly, ignoring caching for this example) the object
 * creation to {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createCoordinateReferenceSystem(String)}
 * instead of {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createObject(String)} because of the
 * {@code "crs"} part in the URN. The more specific method gives better performances and avoid ambiguities.
 *
 * <h3>Compound URIs</h3>
 * This class accepts also combined URIs of the following forms
 * (only two components shown, but arbitrary number of components is allowed):
 *
 * <ul>
 *   <li>{@code "urn:ogc:def:}<var>type</var>{@code ,}
 *       <var>type₁</var>{@code :}<var>authority₁</var>{@code :}<var>version₁</var>{@code :}<var>code₁</var>{@code ,}
 *       <var>type₂</var>{@code :}<var>authority₂</var>{@code :}<var>version₂</var>{@code :}<var>code₂</var>{@code "}</li>
 *   <li>{@code  "http://www.opengis.net/def/crs-compound?}<br>
 *       {@code 1=http://www.opengis.net/def/crs/}<var>authority₁</var>{@code /}<var>version₁</var>{@code /}<var>code₁</var>{@code &}<br>
 *       {@code 2=http://www.opengis.net/def/crs/}<var>authority₂</var>{@code /}<var>version₂</var>{@code /}<var>code₂</var>{@code "}</li>
 * </ul>
 *
 * Given such URIs, {@code MultiAuthoritiesFactory} invokes {@link #createObject(String)} for each component
 * and combines the result as described by the {@link CRS#compound(CoordinateReferenceSystem...)} method.
 * URNs (but not URLs) can also combine a
 * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic reference frame} with an
 * {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal coordinate system} for creating a new
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}, or a base geographic CRS with a
 * {@linkplain org.apache.sis.referencing.operation.DefaultConversion conversion} and a
 * {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian coordinate system} for creating a new
 * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected coordinate reference system}, or
 * {@linkplain org.apache.sis.referencing.operation.AbstractCoordinateOperation coordinate operations}
 * for creating a concatenated operation.
 *
 * <h2>Multiple versions for the same authority</h2>
 * {@code MultiAuthoritiesFactory} accepts an arbitrary number of factories for the same authority, provided that
 * those factories have different version numbers. If a {@code createFoo(String)} method is invoked with a URN
 * containing a version number different than zero, then {@code MultiAuthoritiesFactory} will search for a factory
 * with that exact version, or throw a {@link NoSuchAuthorityFactoryException} if no suitable factory is found.
 * If a {@code createFoo(String)} method is invoked with the version number omitted, then {@code MultiAuthoritiesFactory}
 * will use the first factory in iteration order for the requested authority regardless of its version number.
 *
 * <h3>Example</h3>
 * A {@code MultiAuthoritiesFactory} instance could contain two {@code EPSGFactory} instances:
 * one for version 8.2 and another one for version 7.9 of the EPSG dataset.
 * A specific version can be requested in the URN given to {@code createFoo(String)} methods,
 * for example <code>"urn:ogc:def:crs:EPSG:<b>8.2</b>:4326"</code>.
 * If no version is given of if the given version is zero,
 * then the first EPSG factory in iteration order is used regardless of its version number.
 *
 * <h2>Multi-threading</h2>
 * This class is thread-safe if all delegate factories are themselves thread-safe.
 * However, the factory <em>providers</em>, which are given to the constructor as {@link Iterable} instances,
 * do not need to be thread-safe. See constructor Javadoc for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.CRS#getAuthorityFactory(String)
 *
 * @since 0.7
 */
public class MultiAuthoritiesFactory extends GeodeticAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory
{
    /**
     * The factory providers given at construction time. Elements in the map are for {@link CRSAuthorityFactory},
     * {@link CSAuthorityFactory}, {@link DatumAuthorityFactory} and {@link CoordinateOperationAuthorityFactory}
     * in that order. That order is defined by the constant values in {@link AuthorityFactoryIdentifier.Type}.
     * Note that the map should not contain the last {@link AuthorityFactoryIdentifier.Type} values,
     * which are handled in a special way.
     *
     * <p>Content of this map shall be immutable after construction time in order to avoid the need
     * for synchronization. However, usage of an {@code Iterable} element shall be synchronized on
     * that {@code Iterable}.</p>
     */
    private final EnumMap<AuthorityFactoryIdentifier.Type, Iterable<? extends AuthorityFactory>> providers;

    /**
     * The factories obtained from {@link #getAuthorityFactory(Class, String, String)} and similar methods.
     */
    private final ConcurrentMap<AuthorityFactoryIdentifier, AuthorityFactory> factories;

    /**
     * A bit masks identifying which providers have given us all their factories.
     * For each iterator given by {@code providers.get(type).iterator()}, the value
     * {@code (1 << type.ordinal())} is set when {@code MultiAuthoritiesFactory} has
     * iterated until the end of that iterator.
     *
     * <p><b>Design note:</b> this is equivalent to {@link java.util.EnumSet}.
     * But we use a bitmask for the convenience of using {@link AtomicInteger}.</p>
     */
    private final AtomicInteger isIterationCompleted;

    /**
     * The code spaces of all factories given to the constructor, created when first requested.
     *
     * @see #getCodeSpaces()
     */
    private volatile Set<String> codeSpaces;

    /**
     * Whether this factory should relax some rules when processing a given authority code.
     * See {@link #isLenient()} javadoc for a description of relaxed rules.
     *
     * @see #isLenient()
     */
    private volatile boolean isLenient;

    /**
     * The factories for which we have logged a warning. This is used in order to avoid logging the same
     * warnings many time. We do not bother using a concurrent map here since this map should be rarely used.
     */
    private final Map<AuthorityFactoryIdentifier, Boolean> warnings;

    /**
     * Creates a new multi-factories instance using the given lists of factories.
     * Calls to {@code createFoo(String)} methods will scan the supplied factories in their iteration order when first needed.
     * The first factory having the requested {@linkplain GeodeticAuthorityFactory#getCodeSpaces() namespace} will be used.
     *
     * <h4>Requirements</h4>
     * {@code MultiAuthoritiesFactory} may iterate over the same {@code Iterable} more than once.
     * Each iteration <strong>shall</strong> return the same instances as previous iterations,
     * unless {@link #reload()} has been invoked.
     *
     * <p>The {@code Iterable}s do not need to be thread-safe.
     * {@code MultiAuthoritiesFactory} will use them only in blocks synchronized on the {@code Iterable} instance.
     * For example, all usages of {@code crsFactory} will be done inside a {@code synchronized(crsFactory)} block.</p>
     *
     * <h4>Name collision</h4>
     * If an {@code Iterable} contains more than one factory for the same namespace and version,
     * then only the first occurrence will be used. All additional factories for the same namespace
     * and version will be ignored, after a warning has been logged.
     *
     * <h4>Caching</h4>
     * {@code MultiAuthoritiesFactory} caches the factories found from the given {@code Iterable}s,
     * but does not cache the objects created by those factories.
     * This constructor assumes that the given factories already do their own caching.
     *
     * @param crsFactories    the factories for creating {@link CoordinateReferenceSystem} objects, or null if none.
     * @param csFactories     the factories for creating {@link CoordinateSystem} objects, or null if none.
     * @param datumFactories  the factories for creating {@link Datum} objects, or null if none.
     * @param copFactories    the factories for creating {@link CoordinateOperation} objects, or null if none.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "empty-statement"})    // Generic array creation.
    public MultiAuthoritiesFactory(final Iterable<? extends CRSAuthorityFactory> crsFactories,
                                   final Iterable<? extends CSAuthorityFactory> csFactories,
                                   final Iterable<? extends DatumAuthorityFactory> datumFactories,
                                   final Iterable<? extends CoordinateOperationAuthorityFactory> copFactories)
    {
        providers = new EnumMap<>(AuthorityFactoryIdentifier.Type.class);
        providers.put(AuthorityFactoryIdentifier.Type.CRS,       crsFactories);
        providers.put(AuthorityFactoryIdentifier.Type.CS,        csFactories);
        providers.put(AuthorityFactoryIdentifier.Type.DATUM,     datumFactories);
        providers.put(AuthorityFactoryIdentifier.Type.OPERATION, copFactories);
        /*
         * Mark null Iterables as if we already iterated over all their elements.
         */
        int nullMask = 0;
        for (final var it = providers.entrySet().iterator(); it.hasNext();) {
            final var entry = it.next();
            if (entry.getValue() == null) {
                nullMask |= (1 << entry.getKey().ordinal());
                it.remove();
            }
        }
        isIterationCompleted = new AtomicInteger(nullMask);
        factories = new ConcurrentHashMap<>();
        warnings  = new HashMap<>();
    }

    /**
     * Returns whether this factory should relax some rules when processing a given authority code.
     * If this value is {@code true}, then the behavior of this {@code MultiAuthoritiesFactory}
     * is changed as below:
     *
     * <ul>
     *   <li>If a version is specified in a URN but there is no factory for that specific version,
     *       then fallback on a factory for the same authority but the default version.</li>
     * </ul>
     *
     * The default value is {@code false}, which means that an exception will be thrown
     * if there is no factory specifically for the requested version.
     *
     * @return whether this factory should relax some rules when processing a given authority code.
     */
    public boolean isLenient() {
        return isLenient;
    }

    /**
     * Sets whether this factory should relax some rules when processing a given code.
     *
     * @param lenient whether this factory should relax some rules when processing a given authority code.
     */
    public void setLenient(final boolean lenient) {
        isLenient = lenient;
    }

    /**
     * Returns the database or specification that defines the codes recognized by this factory.
     * The default implementation returns {@code null} because {@code MultiAuthoritiesFactory}
     * is not about a particular authority.
     *
     * @return the organization responsible for definitions in the registry, or {@code null} if none or many.
     */
    @Override
    public Citation getAuthority() {
        return null;
    }

    /**
     * Returns the set of authority codes for objects of the given type.
     * This method returns the union of codes returned by all factories specified at construction time.
     *
     * <p>The {@link Set#contains(Object)} method of the returned set is lenient:
     * it accepts various ways to format a code even if the iterator returns only one form.
     * For example, the {@code contains(Object)} method may return {@code true} for {@code "EPSG:4326"},
     * {@code "EPSG::4326"}, {@code "urn:ogc:def:crs:EPSG::4326"}, <i>etc.</i> even if
     * the iterator returns only {@code "EPSG:4326"}.</p>
     *
     * <p><b>Warnings:</b></p>
     * <ul>
     *   <li>Callers should not retain a reference to the returned collection for a long time,
     *       since it may be backed by database connections (depending on the factory implementations).</li>
     *   <li>The returned set is not thread-safe. Each thread should ask its own instance and let
     *       the garbage collector disposes it as soon as the collection is not needed anymore.</li>
     *   <li>Call to the {@link Set#size()} method on the returned collection should be avoided
     *       since it may be costly.</li>
     * </ul>
     *
     * @param  type  the spatial reference objects type.
     * @return the set of authority codes for spatial reference objects of the given type.
     * @throws FactoryException if access to an underlying factory failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        return new SetOfUnknownSize<String>() {
            /**
             * Returns an iterator over all authority codes.
             * Codes are fetched on-the-fly.
             */
            @Override
            public Iterator<String> iterator() {
                return new AbstractIterator<String>() {
                    /** An iterator over the factories for which to return codes. */
                    private final Iterator<AuthorityFactory> factories = getAllFactories();

                    /** An iterator over the codes of the current factory. */
                    private Iterator<String> codes = Collections.emptyIterator();

                    /** The prefix to prepend before codes, or {@code null} if none. */
                    private String prefix;

                    /** For filtering duplicated codes when there is many versions of the same authority. */
                    private final Set<String> done = new HashSet<>();

                    /** Tests if there is more codes to return. */
                    @Override public boolean hasNext() {
                        while (next == null) {
                            while (!codes.hasNext()) {
                                do {
                                    if (!factories.hasNext()) {
                                        return false;
                                    }
                                    final AuthorityFactory factory = factories.next();
                                    codes = getAuthorityCodes(factory).iterator();
                                    prefix = getCodeSpace(factory);
                                } while (!done.add(prefix));
                            }
                            next = codes.next();
                        }
                        return true;
                    }

                    /** Returns the next element, with namespace inserted before the code if needed. */
                    @Override public String next() {
                        String code = super.next();
                        if (prefix != null && code.indexOf(Constants.DEFAULT_SEPARATOR) < 0) {
                            code = prefix + Constants.DEFAULT_SEPARATOR + code;
                        }
                        return code;
                    }
                };
            }

            /**
             * The cache of values returned by {@link #getAuthorityCodes(AuthorityFactory)}.
             */
            private final Map<AuthorityFactory, Set<String>> cache = new IdentityHashMap<>();

            /**
             * Returns the authority codes for the given factory.
             * This method invokes {@link AuthorityFactory#getAuthorityCodes(Class)}
             * only once per factory and caches the returned {@code Set<String>}.
             */
            final Set<String> getAuthorityCodes(final AuthorityFactory factory) {
                Set<String> codes = cache.get(factory);
                if (codes == null) {
                    try {
                        codes = factory.getAuthorityCodes(type);
                    } catch (FactoryException e) {
                        throw new BackingStoreException(e);
                    }
                    if (cache.put(factory, codes) != null) {
                        throw new ConcurrentModificationException();
                    }
                }
                return codes;
            }

            /** Declares that this set excludes null. */
            @Override protected int characteristics() {
                return Spliterator.DISTINCT | Spliterator.NONNULL;
            }

            /**
             * The collection size, or a negative value if we have not yet computed the size.
             * A negative value different than -1 means that we have not counted all elements,
             * but we have determined that the set is not empty.
             */
            private int size = -1;

            /**
             * Returns the {@link #size()} value if cheap.
             */
            @Override
            protected OptionalInt sizeIfKnown() {
                return size >= 0 ? OptionalInt.of(size) : OptionalInt.empty();
            }

            /**
             * Returns the number of elements in this set (costly operation).
             */
            @Override
            public int size() {
                if (size < 0) {
                    int n = 0;
                    final Set<String> done = new HashSet<>();
                    for (final Iterator<AuthorityFactory> it = getAllFactories(); it.hasNext();) {
                        final AuthorityFactory factory = it.next();
                        if (done.add(getCodeSpace(factory))) {
                            n += getAuthorityCodes(factory).size();
                        }
                    }
                    size = n;
                }
                return size;
            }

            /**
             * Returns {@code true} if the set does not contain any element.
             * This method is much more efficient than testing {@code size() != 0}
             * since it will stop iteration as soon as an element is found.
             */
            @Override
            public boolean isEmpty() {
                if (size == -1) {
                    for (final Iterator<AuthorityFactory> it = getAllFactories(); it.hasNext();) {
                        if (!getAuthorityCodes(it.next()).isEmpty()) {
                            size = -2;      // Size still unknown, but we know that the set is not empty.
                            return false;
                        }
                    }
                    size = 0;
                }
                return size == 0;
            }

            /**
             * The proxy for the {@code GeodeticAuthorityFactory.getAuthorityCodes(type).contains(String)}.
             * Used by {@link #contains(Object)} for delegating its work to the most appropriate factory.
             */
            private final AuthorityFactoryProxy<Boolean> contains =
                new AuthorityFactoryProxy<Boolean>(Boolean.class, AuthorityFactoryIdentifier.Type.ANY) {
                    @Override Boolean createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                        try {
                            return getAuthorityCodes(factory).contains(code);
                        } catch (BackingStoreException e) {
                            throw e.unwrapOrRethrow(FactoryException.class);
                        }
                    }
                    @Override AuthorityFactoryProxy<Boolean> specialize(String typeName) {
                        return this;
                    }
                };

            /**
             * Returns {@code true} if the factory contains the given code.
             */
            @Override
            public boolean contains(final Object code) {
                if (code instanceof String) try {
                    return create(contains, (String) code);
                } catch (NoSuchAuthorityCodeException e) {
                    // Ignore - will return false.
                } catch (FactoryException e) {
                    throw new BackingStoreException(e);
                }
                return false;
            }

            /** Declared as unsupported operation for preventing a call to {@link #size()}. */
            @Override public boolean removeAll(Collection<?> c) {throw new UnsupportedOperationException();}
            @Override public boolean retainAll(Collection<?> c) {throw new UnsupportedOperationException();}
            @Override public boolean remove   (Object o)        {throw new UnsupportedOperationException();}
        };
    }

    /**
     * Returns the code spaces of all factories given to the constructor.
     *
     * <h4>Performance note</h4>
     * The current implementation may be relatively costly because it implies instantiation of all factories.
     *
     * @return the code spaces of all factories.
     * @throws FactoryException if an error occurred while listing the code spaces managed by this factory.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<String> getCodeSpaces() throws FactoryException {
        Set<String> union = codeSpaces;
        if (union == null) {
            union = new LinkedHashSet<>();
            for (final Iterator<AuthorityFactory> it = getAllFactories(); it.hasNext();) {
                union.addAll(getCodeSpaces(it.next()));
            }
            codeSpaces = union = CollectionsExt.unmodifiableOrCopy(union);
        }
        return union;
    }

    /**
     * Returns the code spaces for the given factory.
     * This method delegates to {@link GeodeticAuthorityFactory#getCodeSpaces()} if possible,
     * or reproduces its default implementation otherwise.
     */
    private static Set<String> getCodeSpaces(final AuthorityFactory factory) throws FactoryException {
        if (factory instanceof GeodeticAuthorityFactory) {
            return ((GeodeticAuthorityFactory) factory).getCodeSpaces();
        } else try {
            final String authority = Citations.toCodeSpace(factory.getAuthority());
            return (authority != null) ? Set.of(authority) : Set.of();
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
    }

    /**
     * Returns the "main" namespace of the given factory, or {@code null} if none.
     * The purpose of this method is to get a unique identifier of a factory, ignoring version number.
     * Current implementation returns the first namespace, but this may be changed in any future SIS version.
     *
     * @param  factory  the factory for which to get the main code space.
     * @return the main code space of the given factory, or {@code null} if none.
     * @throws BackingStoreException if an error occurred while listing the code spaces managed by this factory.
     */
    static String getCodeSpace(final AuthorityFactory factory) {
        try {
            return CollectionsExt.first(getCodeSpaces(factory));
        } catch (FactoryException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Caches the given factory, but without replacing existing instance if any.
     * This method returns the factory that we should use, either the given instance of the cached one.
     *
     * @param  identifier  the type, authority and version of the factory to cache.
     * @param  factory     the factory to cache.
     * @return the given {@code factory} if no previous instance was cached, or the existing instance otherwise.
     */
    private AuthorityFactory cache(final AuthorityFactoryIdentifier identifier, final AuthorityFactory factory) {
        final AuthorityFactory existing = factories.putIfAbsent(identifier.intern(), factory);
        return (existing != null) ? existing : factory;
    }

    /**
     * Returns an iterator over all factories in this {@link MultiAuthoritiesFactory}.
     * Note that the same factory instance may be returned more than once if it implements more than one
     * of the {@link CRSAuthorityFactory}, {@link CSAuthorityFactory}, {@link DatumAuthorityFactory} or
     * {@link CoordinateOperationAuthorityFactory} interfaces.
     *
     * <p>This iterator takes care of synchronization on the {@code Iterable<AuthorityFactory>} instances.
     * Note that despite the above-cited synchronization, the returned iterator is <strong>not</strong>
     * thread-safe: each thread needs to use its own iterator instance. However, provided that the above
     * condition is met, threads can safely use their iterators concurrently.</p>
     */
    private Iterator<AuthorityFactory> getAllFactories() {
        return new LazySynchronizedIterator<>(providers.values().iterator());
    }

    /**
     * Returns the factory identified by the given type, authority and version.
     *
     * @param  <T>        the compile-time value of {@code type}.
     * @param  type       the type of the desired factory as one of the {@link CRSAuthorityFactory}, {@link CSAuthorityFactory},
     *                    {@link DatumAuthorityFactory} or {@link CoordinateOperationFactory} interfaces.
     * @param  authority  the namespace or authority identifier of the desired factory.
     *                    Examples: {@code "EPSG"}, {@code "CRS"} or {@code "AUTO2"}.
     * @param  version    the version of the desired factory, or {@code null} for the default version.
     * @return the factory for the given type, authority and version.
     * @throws NoSuchAuthorityFactoryException if no suitable factory has been found.
     * @throws FactoryException if an error occurred while getting the authority or code spaces managed by this factory.
     */
    /*
     * This method is declared final for avoiding the false impression than overriding this method would change
     * the behavior of MultiAuthoritiesFactory. It would not because the `create(…)` method invokes the private
     * `getAuthorityFactory(…)` instead of the public one.
     */
    public final <T extends AuthorityFactory> T getAuthorityFactory(final Class<T> type,
            final String authority, final String version) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonNull("authority", authority);
        return type.cast(getAuthorityFactory(AuthorityFactoryIdentifier.create(type, authority, version)));
    }

    /**
     * Returns the factory identified by the given type, authority and version. If no such factory is found in
     * the cache, then this method iterates over the factories created by the providers given at construction time.
     *
     * @param  request  the type, authority and version of the desired factory.
     * @return the factory for the given type, authority and version.
     * @throws NoSuchAuthorityFactoryException if no suitable factory has been found.
     * @throws FactoryException if an error occurred while getting the authority or code spaces managed by this factory.
     */
    private AuthorityFactory getAuthorityFactory(final AuthorityFactoryIdentifier request) throws FactoryException {
        AuthorityFactory factory = factories.get(request);
        if (factory != null) {
            return factory;
        }
        /*
         * If there is no factory in the cache for the given type, authority and version, then check if the
         * default factory (when no version is specified) is actually the factory for the requested version.
         * The reason why we have to do this check is because we do not ask the version of a factory before
         * we really need to do so, since fetching this information is relatively costly for some factories
         * (e.g. EPSGFactory needs to look in the "Version History" table of the dataset) and rarely needed.
         */
        if (request.hasVersion()) {
            factory = factories.get(request.versionOf(null));
            if (factory != null) try {
                if (request.versionOf(factory.getAuthority()) == request) {
                    // Default factory is for the version that user requested. Cache that finding.
                    return cache(request, factory);
                }
                factory = null;
            } catch (BackingStoreException e) {
                throw e.unwrapOrRethrow(FactoryException.class);
            }
        }
        /*
         * At this point we know that there is no factory in the cache for the requested type, authority and version.
         * Create new factories with the Iterables given at construction time. If we already started an iteration in
         * a previous call to this getAuthorityFactory(…) method, we will continue the search after skipping already
         * cached instances.
         */
        int doneMask = isIterationCompleted.get();
        final int bitmask = (1 << request.type.ordinal());
        if ((doneMask & bitmask) == 0) {
            final Iterable<? extends AuthorityFactory> provider = providers.get(request.type);
            if (provider != null) {
                final Iterator<? extends AuthorityFactory> it;
                synchronized (provider) {               // Should never be null because of the `doneMask` check.
                    it = provider.iterator();
                    while (it.hasNext()) {
                        factory = it.next();
                        if (factory != null) break;     // Paranoiac check against null factories.
                    }
                }
                /*
                 * Search for a factory for the given authority. Caches all factories that we find
                 * during the iteration process. Some factories may already be cached as a result
                 * of a partial iteration in a previous call to getAuthorityFactory(…).
                 */
                while (factory != null) {
                    for (final String namespace : getCodeSpaces(factory)) {
                        final AuthorityFactoryIdentifier unversioned = request.unversioned(namespace);
                        AuthorityFactory cached = cache(unversioned, factory);
                        final AuthorityFactory found = request.equals(unversioned) ? cached : null;
                        /*
                         * Only if we have no choice, ask to the factory what is its version number.
                         * We have no choice when ignoring the version number causes a conflict, or
                         * when the user asked for a specific version.
                         */
                        if (factory != cached || (request.hasVersion() && request.isSameAuthority(unversioned))) try {
                            final AuthorityFactoryIdentifier versioned = unversioned.versionOf(factory.getAuthority());
                            if (versioned != unversioned) {
                                /*
                                 * Before to cache the factory with a key containing the factory version, make sure
                                 * that we took in account the version of the default factory. This will prevent the
                                 * call to `cache(versioned, factory)` to overwrite the default factory.
                                 */
                                if (factory != cached) {
                                    cache(unversioned.versionOf(cached.getAuthority()), cached);
                                }
                                cached = cache(versioned, factory);
                            }
                            /*
                             * If there is a conflict, log a warning provided that we did not already reported
                             * that conflict.
                             */
                            if (factory != cached && canLog(versioned)) {
                                versioned.logConflict(cached);
                            }
                            if (request.equals(versioned)) {
                                return cached;
                            }
                        } catch (BackingStoreException e) {
                            throw e.unwrapOrRethrow(FactoryException.class);
                        }
                        if (found != null) {
                            return found;
                        }
                    }
                    factory = null;
                    synchronized (provider) {
                        while (it.hasNext()) {
                            factory = it.next();
                            if (factory != null) break;         // Paranoiac check against null factories.
                        }
                    }
                }
            } else if (request.type.isGeneric()) {
                /*
                 * Special cases: if the requested factory is ANY, take the first factory that we can find
                 * regardless of its type. We will try CRS, CS, DATUM and OPERATION factories in that order.
                 * The GEODETIC type is like ANY except for the additional restriction that the factory shall
                 * be an instance of the SIS-specific GeodeticAuthorityFactory class.
                 */
                for (final var entry : providers.entrySet()) {
                    factory = getAuthorityFactory(request.newType(entry.getKey()));
                    if (request.type.api.isInstance(factory)) {
                        return factory;
                    }
                }
            }
            /*
             * Remember that we have iterated over all elements of this provider, so we will not try again.
             * Note that the mask values may also be modified in other threads for other providers, so we
             * need to atomically verify that the current value has not been modified before to set it.
             */
            while (!isIterationCompleted.compareAndSet(doneMask, doneMask | bitmask)) {
                doneMask = isIterationCompleted.get();
            }
        }
        /*
         * No factory found. Before to fail, search for a factory for the default version if we are allowed to.
         */
        if (request.hasVersion() && isLenient) {
            factory = getAuthorityFactory(request.versionOf(null));
            if (canLog(request)) {
                request.logFallback();
            }
            return factory;
        }
        final String authority = request.getAuthorityAndVersion().toString();
        throw new NoSuchAuthorityFactoryException(Resources.format(Resources.Keys.UnknownAuthority_1, authority), authority);
    }

    /**
     * Returns {@code true} if this {@code MultiAuthoritiesFactory} can log a warning for the given factory.
     */
    private boolean canLog(AuthorityFactoryIdentifier identifier) {
        synchronized (warnings) {
            if (warnings.containsKey(identifier)) {
                return false;
            }
            // Invoke identifier.intern() only if needed.
            return warnings.putIfAbsent(identifier.intern(), Boolean.TRUE) == null;
        }
    }

    /**
     * Creates an object from a code using the given proxy.
     *
     * @param  <T>    the type of the object to be returned.
     * @param  proxy  the proxy to use for creating the object.
     * @param  code   the code of the object to create.
     * @return the object from one of the authority factory specified at construction time.
     * @throws FactoryException if an error occurred while creating the object.
     */
    private <T> T create(AuthorityFactoryProxy<? extends T> proxy, String code) throws FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        final String authority, version;
        final String[] parameters;
        final DefinitionURI uri = DefinitionURI.parse(code);
        if (uri != null) {
            Class<? extends T> type = proxy.type;
            proxy = proxy.specialize(uri.type);
            /*
             * If the URN or URL contains combined references for compound coordinate reference systems,
             * create the components. First we verify that all component references have been parsed
             * before to start creating any object.
             */
            if (uri.code == null) {
                final DefinitionURI[] components = uri.components;
                if (components != null) {
                    for (int i=0; i < components.length; i++) {
                        if (components[i] == null) {
                            throw new NoSuchAuthorityCodeException(Resources.format(
                                    Resources.Keys.CanNotParseCombinedReference_2, i+1, uri.isHTTP ? 1 : 0),
                                    uri.authority, null, uri.toString());
                        }
                    }
                    if (proxy != null) type = proxy.type;       // Use the more specific type declared in the URN.
                    return combine(type, components, uri.isHTTP);
                }
            }
            /*
             * At this point we determined that the URN or URL references a single instance (not combined references).
             * Example: "urn:ogc:def:crs:EPSG:9.1:4326". Verifies that the object type is recognized and that a code
             * is present. The remainder steps are the same as if the user gave a simple code (e.g. "EPSG:4326").
             */
            if (uri.authority == null) {
                // We want this check before the `code` value is modified below.
                throw new NoSuchAuthorityCodeException(
                        Resources.format(Resources.Keys.MissingAuthority_1, code), null, uri.code, code);
            }
            authority  = uri.authority;
            version    = uri.version;
            code       = uri.code;
            parameters = uri.parameters;
            if (code == null || proxy == null) {
                final String s = uri.toString();
                final String message;
                if (code == null) {
                    message = Errors.format(Errors.Keys.MissingComponentInElement_2, s, "code");
                } else {
                    message = Resources.format(Resources.Keys.CanNotCreateObjectAsInstanceOf_2, type,
                            DefinitionURI.PREFIX + DefinitionURI.SEPARATOR + uri.type);
                }
                throw new NoSuchAuthorityCodeException(message, authority, code, s);
            }
        } else {
            /*
             * Separate the authority from the rest of the code. The authority is mandatory; if missing,
             * an exception will be thrown. Note that the CharSequences.skipLeading/TrailingWhitespaces(…)
             * methods are robust to negative index, so the code will work even if code.indexOf(…) returned -1.
             */
            int afterAuthority = code.indexOf(Constants.DEFAULT_SEPARATOR);
            int end = CharSequences.skipTrailingWhitespaces(code, 0, afterAuthority);
            int start = CharSequences.skipLeadingWhitespaces(code, 0, end);
            if (start >= end) {
                throw new NoSuchAuthorityCodeException(Resources.format(Resources.Keys.MissingAuthority_1, code), null, code);
            }
            authority = code.substring(start, end);
            /*
             * Separate the version from the rest of the code. The version is optional. The code may have no room
             * for version (e.g. "EPSG:4326"), or specify an empty version (e.g. "EPSG::4326"). If the version is
             * equals to an empty string or to the "0" string, it will be considered as no version. Usage of 0 as
             * a pseudo-version is a practice commonly found in other software products.
             */
            int afterVersion = code.indexOf(Constants.DEFAULT_SEPARATOR, ++afterAuthority);
            start = CharSequences.skipLeadingWhitespaces(code, afterAuthority, afterVersion);
            end = CharSequences.skipTrailingWhitespaces(code, start, afterVersion);
            version = (start < end && !code.startsWith(DefinitionURI.NO_VERSION, start)) ? code.substring(start, end) : null;
            if (version != null && !Character.isUnicodeIdentifierPart(version.codePointAt(0))) {
                throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.InvalidVersionIdentifier_1, version), authority, code);
            }
            /*
             * Separate the code from the authority and the version.
             */
            code = CharSequences.trimWhitespaces(code, Math.max(afterAuthority, afterVersion + 1), code.length()).toString();
            parameters = null;
        }
        /*
         * At this point we have the code without the authority and version parts.
         * Push back the authority part if the factory may need it. For now we do that only if the code has
         * parameters, since interpretation of the unit parameter in "AUTO(2):42001,unit,longitude,latitude"
         * depends on whether the authority is "AUTO" or "AUTO2". This works for now, but we may need a more
         * rigorous approach in a future SIS version.
         */
        if (parameters != null || code.indexOf(CommonAuthorityCode.SEPARATOR) >= 0) {
            final var buffer = new StringBuilder(authority.length() + code.length() + 1)
                    .append(authority).append(Constants.DEFAULT_SEPARATOR).append(code);
            if (parameters != null) {
                for (final String p : parameters) {
                    buffer.append(CommonAuthorityCode.SEPARATOR).append(p);
                }
            }
            code = buffer.toString();
        }
        return proxy.createFromAPI(getAuthorityFactory(AuthorityFactoryIdentifier.create(proxy.factoryType, authority, version)), code);
    }

    /**
     * Returns a description of the object corresponding to a code.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li>{@code urn:ogc:def:}<var>type</var>{@code :}<var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/def/}<var>type</var>{@code /}<var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @param  type  the type of object for which to get a description.
     * @param  code  value allocated by authority.
     * @return a description of the object, or empty if the object corresponding to the specified code has no description.
     * @throws FactoryException if an error occurred while fetching the description.
     *
     * @since 1.5
     */
    @Override
    public Optional<InternationalString> getDescriptionText(Class<? extends IdentifiedObject> type, String code)
            throws FactoryException
    {
        return Optional.ofNullable(create(new AuthorityFactoryProxy.Description(type), code));
    }

    /**
     * Creates an arbitrary object from a code.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var> — note that this form is ambiguous</li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var> — note that this form is ambiguous</li>
     *   <li>{@code urn:ogc:def:}<var>type</var>{@code :}<var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/def/}<var>type</var>{@code /}<var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * The two first formats are ambiguous when used with this {@code createObject(String)} method
     * because different kinds of objects can have the same code.
     *
     * @return the object for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    @SuppressWarnings("removal")
    public IdentifiedObject createObject(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.OBJECT, code);
    }

    /**
     * Creates an arbitrary coordinate reference system from a code.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CRS, code);
    }

    /**
     * Creates a 2- or 3-dimensional coordinate reference system based on an ellipsoidal approximation of the geoid.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeographicCRS createGeographicCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEOGRAPHIC_CRS, code);
    }

    /**
     * Creates a 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.5
     */
    @Override
    public GeodeticCRS createGeodeticCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEODETIC_CRS, code);
    }

    /**
     * Creates a 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @deprecated ISO 19111:2019 does not define an explicit class for geocentric CRS.
     * Use {@link #createGeodeticCRS(String)} instead.
     */
    @Override
    @Deprecated(since = "2.0")  // Temporary version number until this branch is released.
    public GeocentricCRS createGeocentricCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEOCENTRIC_CRS, code);
    }

    /**
     * Creates a 2-dimensional coordinate reference system used to approximate the shape of the earth on a planar surface.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ProjectedCRS createProjectedCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PROJECTED_CRS, code);
    }

    /**
     * Creates a 1-dimensional coordinate reference system used for recording heights or depths.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalCRS createVerticalCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_CRS, code);
    }

    /**
     * Creates a 1-dimensional coordinate reference system used for the recording of time.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TemporalCRS createTemporalCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TEMPORAL_CRS, code);
    }

    /**
     * Creates a CRS describing the position of points through two or more independent coordinate reference systems.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CompoundCRS createCompoundCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.COMPOUND_CRS, code);
    }

    /**
     * Creates a CRS that is defined by its coordinate conversion from another CRS (not by a datum).
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public DerivedCRS createDerivedCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.DERIVED_CRS, code);
    }

    /**
     * Creates a 1-, 2- or 3-dimensional contextually local coordinate reference system.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EngineeringCRS createEngineeringCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ENGINEERING_CRS, code);
    }

    /**
     * Creates a 2-dimensional engineering coordinate reference system applied to locations in images.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>crs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>crs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @deprecated The {@code ImageCRS} class has been removed in ISO 19111:2019.
     *             It is replaced by {@code EngineeringCRS}.
     */
    @Override
    @Deprecated(since = "1.5")
    public ImageCRS createImageCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.IMAGE_CRS, code);
    }

    /**
     * Creates an arbitrary datum from a code. The returned object will typically be an
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>datum</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>datum</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Datum createDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.DATUM, code);
    }

    /**
     * Creates a datum defining the location and orientation of an ellipsoid that approximates the shape of the earth.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>datum</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>datum</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeodeticDatum createGeodeticDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEODETIC_DATUM, code);
    }

    /**
     * Creates a datum identifying a particular reference level surface used as a zero-height surface.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>datum</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>datum</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalDatum createVerticalDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_DATUM, code);
    }

    /**
     * Creates a datum defining the origin of a temporal coordinate reference system.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>datum</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>datum</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TemporalDatum createTemporalDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TEMPORAL_DATUM, code);
    }

    /**
     * Creates a datum defining the origin of an engineering coordinate reference system.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>datum</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>datum</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EngineeringDatum createEngineeringDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ENGINEERING_DATUM, code);
    }

    /**
     * Creates a datum defining the origin of an image coordinate reference system.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>datum</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>datum</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @deprecated The {@code ImageDatum} class has been removed in ISO 19111:2019.
     *             It is replaced by {@code EngineeringDatum}.
     */
    @Override
    @Deprecated(since = "1.5")
    public ImageDatum createImageDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.IMAGE_DATUM, code);
    }

    /**
     * Creates a geometric figure that can be used to describe the approximate shape of the earth.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>ellipsoid</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>ellipsoid</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the ellipsoid for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Ellipsoid createEllipsoid(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ELLIPSOID, code);
    }

    /**
     * Creates a prime meridian defining the origin from which longitude values are determined.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>meridian</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>meridian</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the prime meridian for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PRIME_MERIDIAN, code);
    }

    /**
     * Creates information about spatial, vertical, and temporal extent (usually a domain of validity) from a code.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     * </ul>
     *
     * @return the extent for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Extent createExtent(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.EXTENT, code);
    }

    /**
     * Creates an arbitrary coordinate system from a code.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateSystem createCoordinateSystem(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.COORDINATE_SYSTEM, code);
    }

    /**
     * Creates a 2- or 3-dimensional coordinate system for geodetic latitude and longitude, sometimes with ellipsoidal height.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ELLIPSOIDAL_CS, code);
    }

    /**
     * Creates a 1-dimensional coordinate system for heights or depths of points.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalCS createVerticalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_CS, code);
    }

    /**
     * Creates a 1-dimensional coordinate system for heights or depths of points.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TimeCS createTimeCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TIME_CS, code);
    }

    /**
     * Creates a 2- or 3-dimensional Cartesian coordinate system made of straight orthogonal axes.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CartesianCS createCartesianCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CARTESIAN_CS, code);
    }

    /**
     * Creates a 3-dimensional coordinate system with one distance measured from the origin and two angular coordinates.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public SphericalCS createSphericalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.SPHERICAL_CS, code);
    }

    /**
     * Creates a 3-dimensional coordinate system made of a polar coordinate system
     * extended by a straight perpendicular axis.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CylindricalCS createCylindricalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CYLINDRICAL_CS, code);
    }

    /**
     * Creates a 2-dimensional coordinate system for coordinates represented by a distance from the origin
     * and an angle from a fixed direction.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>cs</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public PolarCS createPolarCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.POLAR_CS, code);
    }

    /**
     * Creates a coordinate system axis with name, direction, unit and range of values.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>axis</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>axis</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the axis for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.AXIS, code);
    }

    /**
     * Creates an unit of measurement from a code.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>uom</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>uom</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the unit of measurement for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Unit<?> createUnit(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.UNIT, code);
    }

    /**
     * Creates a definition of a single parameter used by an operation method.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>parameter</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>parameter</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the parameter descriptor for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ParameterDescriptor<?> createParameterDescriptor(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PARAMETER, code);
    }

    /**
     * Creates a description of the algorithm and parameters used to perform a coordinate operation.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>method</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>method</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * If the given code is not found in the geodetic registry, then this method searches also among
     * the {@linkplain DefaultMathTransformFactory#getOperationMethod(String) build-in methods}.
     *
     * @return the operation method for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public OperationMethod createOperationMethod(final String code) throws FactoryException {
        try {
            return create(AuthorityFactoryProxy.METHOD, code);
        } catch (NoSuchAuthorityCodeException e) {
            try {
                return DefaultMathTransformFactory.provider().getOperationMethod(code);
            } catch (NoSuchIdentifierException s) {
                e.addSuppressed(s);
                throw e;
            }
        }
    }

    /**
     * Creates an operation for transforming coordinates in the source CRS to coordinates in the target CRS.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>coordinateOperation</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>http://www.opengis.net/def/<b>coordinateOperation</b>/</code><var>authority</var>{@code /}<var>version</var>{@code /}<var>code</var></li>
     * </ul>
     *
     * @return the operation for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateOperation createCoordinateOperation(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.OPERATION, code);
    }

    /**
     * Creates operations from source and target coordinate reference system codes.
     * If the authority for the two given CRS is handled by the same factory, then
     * this method delegates to that factory. Otherwise this method returns an empty set.
     *
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(
            final String sourceCRS, final String targetCRS) throws FactoryException
    {
        final var deferred = new Deferred();
        final CoordinateOperationAuthorityFactory factory = create(deferred, sourceCRS);
        final String source = deferred.code;
        if (create(deferred, targetCRS) == factory) {
            return factory.createFromCoordinateReferenceSystemCodes(source, deferred.code);
        }
        /*
         * No coordinate operation because of mismatched factories. This is not illegal (the result is an empty set)
         * but it is worth to notify the user because this case has some chances to be a user error.
         */
        final LogRecord record = Resources.forLocale(null).createLogRecord(Level.WARNING,
                Resources.Keys.MismatchedOperationFactories_2, sourceCRS, targetCRS);
        Logging.completeAndLog(LOGGER, MultiAuthoritiesFactory.class, "createFromCoordinateReferenceSystemCodes", record);
        return super.createFromCoordinateReferenceSystemCodes(sourceCRS, targetCRS);
    }

    /**
     * A proxy that does not execute immediately the {@code create} method on a factory,
     * but instead stores information for later execution.
     */
    private static final class Deferred extends AuthorityFactoryProxy<CoordinateOperationAuthorityFactory> {
        Deferred() {
            super(CoordinateOperationAuthorityFactory.class, AuthorityFactoryIdentifier.Type.OPERATION);
        }

        /** The authority code saved by the {@code createFromAPI(…)} method. */
        String code;

        /**
         * Saves the given code in the {@link #code} field and returns the given factory unchanged.
         * @throws FactoryException if the given factory is not an instance of {@link CoordinateOperationAuthorityFactory}.
         */
        @Override
        CoordinateOperationAuthorityFactory createFromAPI(final AuthorityFactory factory, final String code)
                throws FactoryException
        {
            this.code = code;
            return opFactory(factory);
        }
    }

    /**
     * Invoked when a {@code createFoo(…)} method is given a combined URI.
     * A combined URI is a URN or URL referencing other components. For example if the given URI
     * is {@code "urn:ogc:def:crs, crs:EPSG::27700, crs:EPSG::5701"}, then the components are:
     * <ol>
     *   <li>{@code "urn:ogc:def:crs:EPSG:9.1:27700"}</li>
     *   <li>{@code "urn:ogc:def:crs:EPSG:9.1:5701"}</li>
     * </ol>
     *
     * We do not require the components to be instance of CRS, since the "Definition identifier URNs in
     * OGC namespace" best practice paper allows other kinds of combination (e.g. of coordinate operations).
     *
     * @param  <T>         compile-time value of {@code type} argument.
     * @param  type        type of object to create.
     * @param  references  parsed URI of the components.
     * @param  isHTTP      whether the user URI is an URL (i.e. {@code "http://something"}) instead of a URN.
     * @return the combined object.
     * @throws FactoryException if an error occurred while creating the combined object.
     */
    private <T> T combine(final Class<T> type, final DefinitionURI[] references, final boolean isHTTP) throws FactoryException {
        /*
         * Identify the type requested by the user and create all components with the assumption that they will
         * be of that type. This is the most common case. If during iteration we find an object of another kind,
         * then the array type will be downgraded to IdentifiedObject[]. The `componentType` variable will keep
         * its non-null value only if the array stay of the expected sub-type.
         */
        final AuthorityFactoryIdentifier.Type requestedType;
        IdentifiedObject[] components;
        Class<? extends IdentifiedObject> componentType;
        if (CoordinateReferenceSystem.class.isAssignableFrom(type)) {
            requestedType = AuthorityFactoryIdentifier.Type.CRS;
            componentType = CoordinateReferenceSystem.class;
            components    = new CoordinateReferenceSystem[references.length];       // Intentional covariance.
        } else if (CoordinateOperation.class.isAssignableFrom(type)) {
            requestedType = AuthorityFactoryIdentifier.Type.OPERATION;
            componentType = CoordinateOperation.class;
            components    = new CoordinateOperation[references.length];             // Intentional covariance.
        } else {
            throw new FactoryException(Resources.format(Resources.Keys.CanNotCombineUriAsType_1, type));
        }
        final String expected = NameMeaning.toObjectType(componentType);    // Note: "compound-crs" ⟶ "crs".
        for (int i=0; i<references.length; i++) {
            final DefinitionURI ref = references[i];
            final IdentifiedObject component = createObject(ref.toString());
            if (componentType != null && (!componentType.isInstance(component) || !expected.equalsIgnoreCase(ref.type))) {
                componentType = null;
                components = Arrays.copyOf(components, components.length, IdentifiedObject[].class);
            }
            components[i] = component;
        }
        /*
         * At this point we have successfully created all components. The way to interpret those components
         * depends mostly on the type of object requested by the user. For a given requested type, different
         * rules apply depending on the type of components. Those rules are described in OGC 07-092r1 (2007):
         * "Definition identifier URNs in OGC namespace".
         */
        IdentifiedObject combined = null;
        switch (requestedType) {
            case OPERATION: {
                if (componentType != null) {
                    /*
                     * URN combined references for concatenated operations. We build an operation name from
                     * the operation identifiers (rather than CRS identifiers) because this is what the user
                     * gave to us, and because source/target CRS are not guaranteed to be defined. We do not
                     * yet support swapping roles of source and target CRS if an implied-reverse coordinate
                     * operation is included.
                     */
                    final CoordinateOperation[] steps = (CoordinateOperation[]) components;
                    String name = IdentifiedObjects.getIdentifierOrName(steps[0]) + " ⟶ "
                                + IdentifiedObjects.getIdentifierOrName(steps[steps.length - 1]);
                    combined = DefaultCoordinateOperationFactory.provider()
                            .createConcatenatedOperation(
                                    Map.of(CoordinateOperation.NAME_KEY, name),
                                    steps);
                }
                break;
            }
            case CRS: {
                if (componentType != null) {
                    /*
                     * URN combined references for compound coordinate reference systems.
                     * The URNs of the individual well-known CRSs are listed in the same order in which the
                     * individual coordinate tuples are combined to form the CompoundCRS coordinate tuple.
                     */
                    combined = CRS.compound((CoordinateReferenceSystem[]) components);
                } else if (!isHTTP) {
                    final CoordinateSystem cs = remove(references, components, CoordinateSystem.class);
                    if (cs != null) {
                        final Datum datum = remove(references, components, Datum.class);
                        if (datum != null) {
                            /*
                             * URN combined references for datum and coordinate system. In this case, the URN shall
                             * concatenate the URNs of one well-known datum and one well-known coordinate system.
                             */
                            if (ArraysExt.allEquals(references, null)) {
                                combined = combine((GeodeticDatum) datum, cs);
                            }
                        } else {
                            /*
                             * URN combined references for projected or derived CRSs. In this case, the URN shall
                             * concatenate the URNs of the one well-known CRS, one well-known Conversion, and one
                             * well-known CartesianCS. Similar action can be taken for derived CRS.
                             */
                            CoordinateReferenceSystem baseCRS = remove(references, components, CoordinateReferenceSystem.class);
                            CoordinateOperation op = remove(references, components, CoordinateOperation.class);
                            if (ArraysExt.allEquals(references, null) && op instanceof Conversion) {
                                combined = combine(baseCRS, (Conversion) op, cs);
                            }
                        }
                    }
                }
                break;
            }
        }
        /*
         * At this point the combined object has been created if we know how to create it.
         * Maybe the result matches the definition of an existing object in the database,
         * in which case we will use the existing definition for better metadata.
         */
        if (combined == null) {
            throw new FactoryException(Resources.format(Resources.Keys.UnexpectedComponentInURI));
        }
        final IdentifiedObject existing = newIdentifiedObjectFinder().findSingleton(combined);
        return type.cast(existing != null ? existing : combined);
    }

    /**
     * If the given {@code type} is found in the given {@code references}, sets that reference element to {@code null}
     * and returns the corresponding {@code components} element. Otherwise returns {@code null}. This is equivalent to
     * {@link Map#remove(Object, Object)} where {@code references} are the keys and {@code components} are the values.
     * We do not bother building that map because the arrays are very short (2 or 3 elements).
     */
    private static <T> T remove(final DefinitionURI[] references, final IdentifiedObject[] components, final Class<T> type) {
        final String expected = NameMeaning.toObjectType(type);
        for (int i=0; i<references.length; i++) {
            final DefinitionURI ref = references[i];
            if (ref != null && expected.equalsIgnoreCase(ref.type)) {
                references[i] = null;
                return type.cast(components[i]);
            }
        }
        return null;
    }

    /**
     * Invoked when a {@code createFoo(…)} method is given a combined URI containing a datum and a coordinate system.
     * If the given information are not sufficient or not applicable, then this method returns {@code null}.
     *
     * @param  datum  the datum, or {@code null} if missing.
     * @param  cs     the coordinate system (never null).
     * @return the combined CRS, or {@code null} if the given information are not sufficient.
     * @throws FactoryException if an error occurred while creating the combined CRS.
     *
     * @todo Handle {@link DatumEnsemble}.
     */
    private static GeodeticCRS combine(final GeodeticDatum datum, final CoordinateSystem cs) throws FactoryException {
        final Map<String,?> properties = IdentifiedObjects.getProperties(datum, Datum.IDENTIFIERS_KEY);
        final CRSFactory factory = GeodeticObjectFactory.provider();
        if (datum instanceof GeodeticDatum) {
            if (cs instanceof EllipsoidalCS) {
                return factory.createGeographicCRS(properties, datum, (EllipsoidalCS) cs);
            } else if (cs instanceof SphericalCS) {
                return factory.createGeodeticCRS(properties, datum, null, (SphericalCS) cs);
            }
        }
        return null;
    }

    /**
     * Invoked when a {@code createFoo(…)} method is given a combined URI containing a conversion and a coordinate
     * system. If the given information are not sufficient or not applicable, then this method returns {@code null}.
     *
     * @param  baseCRS   the CRS on which the derived CRS will be based on, or {@code null} if missing.
     * @param  fromBase  the conversion from {@code baseCRS} to the CRS to be created by this method.
     * @param  cs        the coordinate system (never null).
     * @return the combined CRS, or {@code null} if the given information are not sufficient.
     * @throws FactoryException if an error occurred while creating the combined CRS.
     */
    private static DerivedCRS combine(final CoordinateReferenceSystem baseCRS, final Conversion fromBase,
            final CoordinateSystem cs) throws FactoryException
    {
        if (baseCRS != null && fromBase.getSourceCRS() == null && fromBase.getTargetCRS() == null) {
            final Map<String,?> properties = IdentifiedObjects.getProperties(fromBase, Datum.IDENTIFIERS_KEY);
            final CRSFactory factory = GeodeticObjectFactory.provider();
            if (baseCRS instanceof GeographicCRS && cs instanceof CartesianCS) {
                return factory.createProjectedCRS(properties, (GeographicCRS) baseCRS, fromBase, (CartesianCS) cs);
            } else {
                return factory.createDerivedCRS(properties, baseCRS, fromBase, cs);
            }
        }
        return null;
    }

    /**
     * Creates a finder which can be used for looking up unidentified objects.
     * The default implementation delegates the lookups to the underlying factories.
     *
     * @return a finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder cannot be created.
     */
    @Override
    public IdentifiedObjectFinder newIdentifiedObjectFinder() throws FactoryException {
        return new Finder(this);
    }

    /**
     * A {@link IdentifiedObjectFinder} which tests every factories declared in the
     * {@linkplain MultiAuthoritiesFactory#getAllFactories() collection of factories}.
     */
    private static final class Finder extends IdentifiedObjectFinder {
        /**
         * The finders of all factories, or {@code null} if not yet fetched.
         * We will create this array only when first needed in order to avoid instantiating the factories
         * before needed (for example we may be able to find an object using only its code). However,
         * if we need to create this array, then we will create it fully (for all factories at once).
         */
        private IdentifiedObjectFinder[] finders;

        /**
         * Creates a new finder.
         */
        protected Finder(final MultiAuthoritiesFactory factory) throws FactoryException {
            super(factory);
        }

        /**
         * Sets the domain of the search (for example whether to include deprecated objects in the search).
         */
        @Override
        public void setSearchDomain(final Domain domain) {
            super.setSearchDomain(domain);
            if (finders != null) {
                for (final IdentifiedObjectFinder finder : finders) {
                    finder.setSearchDomain(domain);
                }
            }
        }

        /**
         * Sets whether the search should ignore coordinate system axes.
         */
        @Override
        public void setIgnoringAxes(final boolean ignore) {
            super.setIgnoringAxes(ignore);
            if (finders != null) {
                for (final IdentifiedObjectFinder finder : finders) {
                    finder.setIgnoringAxes(ignore);
                }
            }
        }

        /**
         * Creates objects equal (optionally ignoring metadata) to the specified object using the given identifiers.
         * This method may be used in order to get a fully identified object from a partially identified one.
         *
         * @param  type         the type of the factory which is needed.
         * @param  object       the user-specified object to search.
         * @param  identifiers  {@code object.getIdentifiers()} or {@code object.getName()}.
         * @param  result       the collection where to add the object instantiated from the identifiers.
         * @throws FactoryException if an error occurred while creating an object.
         */
        private void createFromIdentifiers(final AuthorityFactoryIdentifier.Type type, final IdentifiedObject object,
                final Iterable<ReferenceIdentifier> identifiers, final Set<IdentifiedObject> result) throws FactoryException
        {
            for (final ReferenceIdentifier identifier : identifiers) {
                final String authority = identifier.getCodeSpace();
                if (authority != null) {
                    @SuppressWarnings("LocalVariableHidesMemberVariable")
                    final var factory = (MultiAuthoritiesFactory) this.factory;
                    final IdentifiedObject candidate;
                    try {
                        final var fid = AuthorityFactoryIdentifier.create(type, authority, identifier.getVersion());
                        candidate = createAndFilter(factory.getAuthorityFactory(fid), identifier.getCode(), object);
                    } catch (NoSuchAuthorityCodeException e) {
                        // The identifier was not recognized. No problem, let's go on.
                        exceptionOccurred(e);
                        continue;
                    }
                    if (candidate != null) {
                        result.add(candidate);
                    }
                }
            }
        }

        /**
         * Creates objects approximately equal to the specified object by iterating over authority code candidates.
         * This method is invoked by {@link #find(IdentifiedObject)} when the result was not already in the cache.
         * First, this method tries to delegate to the factory specified by the name space of each identifier.
         * If this quick search using declared identifiers did not worked, then this method delegates to every
         * factories registered in the enclosing {@link MultiAuthoritiesFactory}, in iteration order.
         *
         * <p>This method shall <strong>not</strong> delegate the job to the parent class, as the default
         * implementation in the parent class is very inefficient. We need to delegate to the finders of
         * all factories, so we can leverage their potentially more efficient algorithms.</p>
         *
         * @param  object  the object looked up.
         * @return the identified objects, or an empty set if not found.
         * @throws FactoryException if an error occurred while fetching the authority code candidates.
         * @throws BackingStoreException allowed for convenience, will be unwrapped by the caller.
         */
        @Override
        final Set<IdentifiedObject> createFromCodes(final IdentifiedObject object) throws FactoryException {
            if (getSearchDomain() != Domain.EXHAUSTIVE_VALID_DATASET) {
                for (AuthorityFactoryIdentifier.Type type : AuthorityFactoryIdentifier.Type.values()) {
                    if (type.isFactoryOf(object)) {
                        final var result = new LinkedHashSet<IdentifiedObject>();
                        createFromIdentifiers(type, object, object.getIdentifiers(), result);
                        if (result.isEmpty()) {
                            createFromIdentifiers(type, object, CollectionsExt.singletonOrEmpty(object.getName()), result);
                            if (result.isEmpty()) {
                                break;
                            }
                        }
                        return result;
                    }
                }
            }
            /*
             * No object created from the identifiers or the name.
             * Prepare finders for each factory in iteration order.
             */
            if (finders == null) {
                final var list = new ArrayList<IdentifiedObjectFinder>();
                final var unique = new IdentityHashMap<AuthorityFactory,Boolean>();
                final Iterator<AuthorityFactory> it = ((MultiAuthoritiesFactory) factory).getAllFactories();
                while (it.hasNext()) {
                    final AuthorityFactory candidate = it.next();
                    if (candidate instanceof GeodeticAuthorityFactory && unique.put(candidate, Boolean.TRUE) == null) {
                        IdentifiedObjectFinder finder = ((GeodeticAuthorityFactory) candidate).newIdentifiedObjectFinder();
                        if (finder != null) {   // Should never be null according method contract, but we are paranoiac.
                            list.add(finder);
                        }
                    }
                }
                finders = list.toArray(IdentifiedObjectFinder[]::new);
            }
            /*
             * If only one finder returns a non-empty set, we return that set without copying
             * the elements in a `LinkedHashSet` because it may a lazy set. We merge the sets
             * only if really necessary.
             */
            Set<IdentifiedObject> merged = null, result = null;
            for (final IdentifiedObjectFinder finder : finders) {
                finder.setWrapper(this);    // Also copy the configuration of this finder.
                final Set<IdentifiedObject> codes = finder.find(object);
                if (!codes.isEmpty()) {
                    if (result == null) {
                        result = codes;
                    } else {
                        if (merged == null) {
                            merged = new LinkedHashSet<>(result);
                        }
                        if (merged.addAll(codes)) {
                            result = merged;
                        }
                    }
                }
            }
            return (result != null) ? result : Set.of();
        }
    }

    /**
     * Clears the cache and notifies this {@code MultiAuthoritiesFactory} that all factories will need to
     * be fetched again from the providers given at construction time. In addition, all providers that are
     * instances of {@link ServiceLoader} will have their {@link ServiceLoader#reload() reload()} method invoked.
     *
     * <p>This method is intended for use in situations in which new factories can be installed into a running
     * Java virtual machine.</p>
     */
    public void reload() {
        for (final var entry : providers.entrySet()) {
            final Iterable<?> provider = entry.getValue();
            synchronized (provider) {
                if (provider instanceof LazySet<?>) {
                    ((LazySet<?>) provider).reload();
                } else if (provider instanceof ServiceLoader<?>) {
                    ((ServiceLoader<?>) provider).reload();
                }
                /*
                 * Clear the `iterationCompleted` bit before to clear the cache so that if another thread
                 * invokes `getAuthorityFactory(…)`, it will block on the synchronized(provider) statement
                 * until we finished the cleanup.
                 */
                final int type = entry.getKey().ordinal();
                applyAndMask(~(1 << type));
                /*
                 * Clear the cache on a provider-by-provider basis, not by a call to factories.clear().
                 * This is needed because this MultiAuthoritiesFactory instance may be used concurrently
                 * by other threads, and we have no global lock for the whole factory.
                 */
                final Iterator<AuthorityFactoryIdentifier> it = factories.keySet().iterator();
                while (it.hasNext()) {
                    if (it.next().type.ordinal() == type) {
                        it.remove();
                    }
                }
            }
        }
        // Clears all bits other than the bits for providers.
        applyAndMask((1 << AuthorityFactoryIdentifier.Type.GEODETIC.ordinal()) - 1);
    }

    /**
     * Sets {@link #isIterationCompleted} to {@code iterationCompleted & mask}.
     * This is used by {@link #reload()} for clearing bits.
     */
    private void applyAndMask(final int mask) {
        int value;
        do value = isIterationCompleted.get();
        while (!isIterationCompleted.compareAndSet(value, value & mask));
    }
}
