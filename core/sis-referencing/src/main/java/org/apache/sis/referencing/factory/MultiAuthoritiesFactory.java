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

import java.util.ServiceLoader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.util.FactoryException;
import javax.measure.unit.Unit;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.DefaultNameSpace;


/**
 * A factory that delegates the object creation to another factory determined from the <var>authority</var> part
 * in “<var>authority</var>:<var>code</var>” arguments.
 * The list of factories to use as delegates can be specified at construction time.
 *
 * <p>This factory requires that every codes given to a {@code createFoo(String)} method are prefixed by a namespace,
 * for example {@code "EPSG::4326"}.
 * When a {@code createFoo(String)} method is invoked, this class uses the <var>authority</var> part in the
 * “<var>authority</var>:<var>code</var>” argument for locating a factory capable to create a geodetic object
 * for the <var>code</var> part.  If a factory is found in the list of factories given at construction time,
 * then the work is delegated to that factory. Otherwise a {@link NoSuchAuthorityFactoryException} is thrown.</p>
 *
 * <div class="section">URI syntax</div>
 * This factory can also parse URNs of the
 * {@code "urn:ogc:def:}<var>type</var>{@code :}<var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var>{@code "}
 * form and URLs of the {@code "http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var>{@code "} form.
 * In such cases, the <var>type</var> specified in the URN may be used for invoking a more specific method.
 * However {@code MultiAuthoritiesFactory} uses the type information in the URN only for
 * delegating to a more specific method, never for delegating to a less specific method.
 * An exception will be thrown if the type in the URN is incompatible with the invoked method.
 *
 * <div class="note"><b>Example:</b>
 * if <code>{@linkplain #createObject(String) createObject}("urn:ogc:def:<b>crs</b>:EPSG::4326")</code> is invoked,
 * then {@code MultiAuthoritiesFactory} will delegate (indirectly, ignoring caching for this example) the object
 * creation to {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createCoordinateReferenceSystem(String)}
 * instead of {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createObject(String)} because of the
 * {@code "crs"} part in the URN. The more specific method gives better performances and avoid ambiguities.</div>
 *
 * <div class="section">Multiple versions for the same authority</div>
 * {@code MultiAuthoritiesFactory} accepts an arbitrary amount of factories for the same authority, provided that
 * those factories have different version numbers. If a {@code createFoo(String)} method is invoked with a URN
 * containing a version number different than zero, then {@code MultiAuthoritiesFactory} will search for a factory
 * with that exact version, or throw a {@link NoSuchAuthorityFactoryException} if no suitable factory is found.
 * If a {@code createFoo(String)} method is invoked with the version number omitted, then {@code MultiAuthoritiesFactory}
 * will use the first factory in iteration order for the requested authority regardless of its version number.
 *
 * <div class="note"><b>Example:</b>
 * a {@code MultiAuthoritiesFactory} instance could contain two {@code EPSGFactory} instances:
 * one for version 8.2 and another one for version 7.9 of the EPSG dataset.
 * A specific version can be requested in the URN given to {@code createFoo(String)} methods,
 * for example <code>"urn:ogc:def:crs:EPSG:<b>8.2</b>:4326"</code>.
 * If no version is given (for example {@code "urn:ogc:def:crs:EPSG::4326"}),
 * then the first EPSG factory in iteration order is used regardless of its version number.
 * </div>
 *
 * <div class="section">Multi-threading</div>
 * This class is thread-safe if all delegate factories are themselves thread-safe.
 * However the factory <em>providers</em>, which are given to the constructor as {@link Iterable} instances,
 * do not need to be thread-safe. See constructor Javadoc for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class MultiAuthoritiesFactory extends GeodeticAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory
{
    /**
     * The factory providers given at construction time. Elements in the array are for {@link CRSAuthorityFactory},
     * {@link CSAuthorityFactory}, {@link DatumAuthorityFactory} and {@link CoordinateOperationAuthorityFactory}
     * in that order. That order is defined by the constant values in {@link AuthorityFactoryIdentifier}.
     *
     * <p>Note that this array is shorter than the amount of {@link AuthorityFactoryIdentifier} values.
     * The last {@link AuthorityFactoryIdentifier} values are handled in a special way.</p>
     *
     * <p>The array may contain {@code null} elements when there is no provider for a given type.
     * Content of this array shall be immutable after construction time in order to avoid the need
     * for synchronization when reading the array. However usage of an {@code Iterable} element
     * shall be synchronized on that {@code Iterable}.</p>
     */
    private final Iterable<? extends AuthorityFactory>[] providers;

    /**
     * The factories obtained from the {@link #iterators}.
     */
    private final ConcurrentMap<AuthorityFactoryIdentifier, AuthorityFactory> factories;

    /**
     * A bit masks identifying which providers have given us all their factories.
     */
    private final AtomicInteger iterationCompleted;

    /**
     * The code spaces of all factories given to the constructor, created when first requested.
     *
     * @see #getCodeSpaces()
     */
    private volatile Set<String> codeSpaces;

    /**
     * Creates a new multi-factories instance using the given lists of factories.
     * Calls to {@code createFoo(String)} methods will scan the supplied factories in their iteration order when first needed.
     * The first factory having the requested {@linkplain GeodeticAuthorityFactory#getCodeSpaces() namespace} will be used.
     *
     * <div class="section">Requirements</div>
     * {@code MultiAuthoritiesFactory} may iterate over the same {@code Iterable} more than once.
     * Each iteration <strong>shall</strong> return the same instances than previous iterations,
     * unless {@link #reload()} has been invoked.
     *
     * <p>The {@code Iterable}s do not need to be thread-safe.
     * {@code MultiAuthoritiesFactory} will use them only in blocks synchronized on the {@code Iterable} instance.
     * For example all usages of {@code crsFactory} will be done inside a {@code synchronized(crsFactory)} block.</p>
     *
     * <div class="section">Name collision</div>
     * If an {@code Iterable} contains more than one factory for the same namespace and version,
     * then only the first occurrence will be used. All additional factories for the same namespace
     * and version will be ignored, after a warning has been logged.
     *
     * <div class="section">Caching</div>
     * {@code MultiAuthoritiesFactory} caches the factories found from the given {@code Iterable}s,
     * but does not cache the objects created by those factories.
     * This constructor assumes that the given factories already do their own caching.
     *
     * @param crsFactories   The factories for creating {@link CoordinateReferenceSystem} objects, or null if none.
     * @param csFactories    The factories for creating {@link CoordinateSystem} objects, or null if none.
     * @param datumFactories The factories for creating {@link Datum} objects, or null if none.
     * @param copFactories   The factories for creating {@link CoordinateOperation} objects, or null if none.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "empty-statement"})    // Generic array creation.
    public MultiAuthoritiesFactory(final Iterable<? extends CRSAuthorityFactory> crsFactories,
                                   final Iterable<? extends CSAuthorityFactory> csFactories,
                                   final Iterable<? extends DatumAuthorityFactory> datumFactories,
                                   final Iterable<? extends CoordinateOperationAuthorityFactory> copFactories)
    {
        final Iterable<? extends AuthorityFactory>[] p = new Iterable[4];
        p[AuthorityFactoryIdentifier.CRS]       = crsFactories;
        p[AuthorityFactoryIdentifier.CS]        = csFactories;
        p[AuthorityFactoryIdentifier.DATUM]     = datumFactories;
        p[AuthorityFactoryIdentifier.OPERATION] = copFactories;
        /*
         * Mark null Iterables as if we already iterated over all their elements.
         * Opportunistically reduce the array size by trimming trailing null elements.
         * The memory gain is negligible, but this will reduce the number of iterations in loops.
         */
        int length = 0, nullMask = 0;
        for (int i=0; i < p.length; i++) {
            if (p[i] != null) length = i+1;
            else nullMask |= (1 << i);
        }
        providers = ArraysExt.resize(p, length);
        factories = new ConcurrentHashMap<>();
        iterationCompleted = new AtomicInteger(nullMask);
    }

    /**
     * Returns the organization or party responsible for definition and maintenance of the database.
     * The default implementation returns {@code null} since {@code MultiAuthoritiesFactory} is not
     * about a particular authority.
     */
    @Override
    public Citation getAuthority() {
        return null;
    }

    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
        throw new UnsupportedOperationException("Not supported yet.");      // TODO
    }

    /**
     * Returns the code spaces of all factories given to the constructor.
     * This method may be relatively costly since it implies instantiation of all factories.
     *
     * @return The code spaces of all factories.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<String> getCodeSpaces() {
        Set<String> union = codeSpaces;
        if (union == null) {
            union = new LinkedHashSet<>();
            for (final Iterable<? extends AuthorityFactory> provider : providers) {
                if (provider != null) synchronized (provider) {
                    for (final AuthorityFactory factory : provider) {
                        union.addAll(getCodeSpaces(factory));
                    }
                }
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
    private static Set<String> getCodeSpaces(final AuthorityFactory factory) {
        if (factory instanceof GeodeticAuthorityFactory) {
            return ((GeodeticAuthorityFactory) factory).getCodeSpaces();
        } else {
            final String authority = Citations.getCodeSpace(factory.getAuthority());
            return (authority != null) ? Collections.singleton(authority) : Collections.emptySet();
        }
    }

    /**
     * Caches the given factory, but without replacing existing instance if any.
     * This method returns the factory that we should use, either the given instance of the cached one.
     *
     * @param  identifier The type, authority and version of the factory to cache.
     * @param  factory The factory to cache.
     * @return The given {@code factory} if no previous instance was cached, or the existing instance otherwise.
     */
    private AuthorityFactory cache(final AuthorityFactoryIdentifier identifier, final AuthorityFactory factory) {
        final AuthorityFactory existing = factories.putIfAbsent(identifier.intern(), factory);
        return (existing != null) ? existing : factory;
    }

    /**
     * Returns the factory identified by the given type, authority and version.
     *
     * @param  <T>        The compile-time value of {@code type}.
     * @param  type       The type of the desired factory as one of the {@link CRSAuthorityFactory}, {@link CSAuthorityFactory},
     *                    {@link DatumAuthorityFactory} or {@link CoordinateOperationFactory} interfaces.
     * @param  authority  The namespace or authority identifier of the desired factory.
     *                    Examples: {@code "EPSG"}, {@code "CRS"} or {@code "AUTO2"}.
     * @param  version    The version of the desired factory, or {@code null} for the default version.
     * @return The factory for the given type, authority and version.
     * @throws NoSuchAuthorityFactoryException if no suitable factory has been found.
     */
    /*
     * This method is declared final for avoiding the false impression than overriding this method would change
     * the behavior of MultiAuthoritiesFactory. It would not because the 'create(…)' method invokes the private
     * 'getAuthorityFactory(…)' instead of the public one.
     */
    public final <T extends AuthorityFactory> T getAuthorityFactory(final Class<T> type,
            final String authority, final String version) throws NoSuchAuthorityFactoryException
    {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonNull("authority", authority);
        return type.cast(getAuthorityFactory(AuthorityFactoryIdentifier.create(type, authority, version)));
    }

    /**
     * Returns the factory identified by the given type, authority and version. If no such factory is found in
     * the cache, then this method iterates over the factories created by the providers given at construction time.
     *
     * @param  request The type, authority and version of the desired factory.
     * @return The factory for the given type, authority and version.
     * @throws NoSuchAuthorityFactoryException if no suitable factory has been found.
     */
    private AuthorityFactory getAuthorityFactory(final AuthorityFactoryIdentifier request)
            throws NoSuchAuthorityFactoryException
    {
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
            if (factory != null && request.versionOf(factory.getAuthority()) == request) {
                // Default factory is for the version that user requested. Cache that finding.
                return cache(request, factory);
            }
        }
        /*
         * At this point we know that there is no factory in the cache for the requested type, authority and version.
         * Create new factories with the Iterables given at construction time. If we already started an iteration in
         * a previous call to this getAuthorityFactory(…) method, we will continue the search after skipping already
         * cached instances.
         */
        int doneMask = iterationCompleted.get();
        final int type = request.type;
        if ((doneMask & (1 << type)) == 0) {
            if (type >= 0 && type < providers.length) {
                final Iterable<? extends AuthorityFactory> provider = providers[type];
                synchronized (provider) {   // Should never be null because of the 'doneMask' check.
                    /*
                     * Search for a factory for the given authority. Caches all factories that we find
                     * during the iteration process. Some factories may already be cached as a result
                     * of a partial iteration in a previous call to getAuthorityFactory(…).
                     */
                    for (final Iterator<? extends AuthorityFactory> it = provider.iterator(); it.hasNext();) {
                        factory = it.next();
                        for (final String namespace : getCodeSpaces(factory)) {
                            final AuthorityFactoryIdentifier unversioned = request.unversioned(namespace);
                            AuthorityFactory cached = cache(unversioned, factory);
                            final AuthorityFactory found = request.equals(unversioned) ? cached : null;
                            /*
                             * Only if we have no choice, ask to the factory what is its version number.
                             * We have no choice when ignoring the version number causes a conflict, or
                             * when the user asked for a specific version.
                             */
                            if (factory != cached || (request.hasVersion() && request.getAuthority().equals(unversioned.getAuthority()))) {
                                final AuthorityFactoryIdentifier versioned = unversioned.versionOf(factory.getAuthority());
                                if (versioned != unversioned) {
                                    /*
                                     * Before to cache the factory with a key containing the factory version, make sure
                                     * that we took in account the version of the default factory. This will prevent the
                                     * call to 'cache(versioned, factory)' to overwrite the default factory.
                                     */
                                    if (factory != cached) {
                                        cache(unversioned.versionOf(cached.getAuthority()), cached);
                                    }
                                    cached = cache(versioned, factory);
                                }
                                /*
                                 * If there is a conflict, log a warning provided that we did not already reported
                                 * that conflict. The flag telling us if we already logged a warning is in the key,
                                 * so we have to find that key in the loop below. This is inefficient, but conflict
                                 * should not happen in a sane environment.
                                 */
                                if (factory != cached) {
                                    for (final AuthorityFactoryIdentifier identifier : factories.keySet()) {
                                        if (identifier.equals(versioned)) {
                                            identifier.logConflictWarning(cached);
                                            break;
                                        }
                                    }
                                }
                                if (request.equals(versioned)) {
                                    return cached;
                                }
                            }
                            if (found != null) {
                                return found;
                            }
                        }
                    }
                }
            } else if (type >= AuthorityFactoryIdentifier.GEODETIC) {
                /*
                 * Special cases: if the requested factory is ANY, take the first factory that we can found
                 * regardless of its type. We will try CRS, CS, DATUM and OPERATION factories in that order.
                 * The GEODETIC type is like ANY except for the additional restriction that the factory shall
                 * be an instance of the SIS-specific GeodeticAuthorityFactory class.
                 */
                assert providers.length < Math.min(type, Byte.MAX_VALUE) : type;
                for (byte i=0; i < providers.length; i++) {
                    factory = getAuthorityFactory(request.newType(i));
                    switch (type) {
                        case AuthorityFactoryIdentifier.ANY: {
                            return factory;
                        }
                        case AuthorityFactoryIdentifier.GEODETIC: {
                            if (factory instanceof GeodeticAuthorityFactory) {
                                return factory;
                            }
                        }
                    }
                }
            }
            /*
             * Remember that we have iterated over all elements of this provider, so we will not try again.
             * Note that the mask values may also be modified in other threads for other providers, so we
             * need to atomically verify that the current value has not been modified before to set it.
             */
            while (!iterationCompleted.compareAndSet(doneMask, doneMask | (1 << type))) {
                doneMask = iterationCompleted.get();
            }
        }
        final String authority = request.getAuthority();
        throw new NoSuchAuthorityFactoryException(Errors.format(Errors.Keys.UnknownAuthority_1, authority), authority);
    }

    /**
     * Creates an object from a code using the given proxy.
     *
     * @param  <T>   The type of the object to be returned.
     * @param  proxy The proxy to use for creating the object.
     * @param  code  The code of the object to create.
     * @return The object from one of the authority factory specified at construction time.
     * @throws FactoryException If an error occurred while creating the object.
     */
    private <T> T create(final AuthorityFactoryProxy<T> proxy, final String code) throws FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        final String authority, version;
        final DefinitionURI uri = DefinitionURI.parse(code);
        if (uri != null) {
            authority = uri.authority;
            version = uri.version;
        } else {
            /*
             * Usages of CharSequences.skipLeadingWhitespaces(…) and skipTrailingWhitespaces(…)
             * below will work even if code.indexOf(…) returned -1.
             */
            int afterAuthority = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
            int end = CharSequences.skipTrailingWhitespaces(code, 0, afterAuthority);
            int start = CharSequences.skipLeadingWhitespaces(code, 0, end);
            if (start >= end) {
                throw new NoSuchAuthorityFactoryException(Errors.format(Errors.Keys.MissingAuthority_1, code), null);
            }
            authority = code.substring(start, end);
            int afterVersion = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR, ++afterAuthority);
            start = CharSequences.skipLeadingWhitespaces(code, afterAuthority, afterVersion);
            end = CharSequences.skipTrailingWhitespaces(code, start, afterVersion);
            if (start < end) {
                version = code.substring(start, end);
                afterVersion++;
            } else {
                version = null;
                afterVersion = afterAuthority;
            }
        }
        return proxy.createFromAPI(getAuthorityFactory(
                AuthorityFactoryIdentifier.create(proxy.factoryType, authority, version)), code);
    }

    /**
     * Creates an arbitrary object from a code.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var> — note that this form is ambiguous</li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var> — note that this form is ambiguous</li>
     *   <li>{@code urn:ogc:def:}<var>type</var>{@code :}<var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * The two first formats are ambiguous when used with this {@code createObject(String)} because different kinds
     * of objects can have the same code.
     *
     * @return The object for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
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
     *   <li>{@code http://www.opengis.net/gml/srs/}<var>authority</var>{@code .xml#}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
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
     * </ul>
     *
     * @return The datum for the given code.
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
     * </ul>
     *
     * @return The datum for the given code.
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
     * </ul>
     *
     * @return The datum for the given code.
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
     * </ul>
     *
     * @return The datum for the given code.
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
     * </ul>
     *
     * @return The datum for the given code.
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
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
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
     * </ul>
     *
     * @return The ellipsoid for the given code.
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
     * </ul>
     *
     * @return The prime meridian for the given code.
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
     * @return The extent for the given code.
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
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateSystem createCoordinateSystem(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.COORDINATE_SYSTEM, code);
    }

    /**
     * Creates a 2- or 3-dimensional coordinate system for geodetic latitude and longitude, sometime with ellipsoidal height.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>cs</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     * </ul>
     *
     * @return The coordinate system for the given code.
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
     * </ul>
     *
     * @return The coordinate system for the given code.
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
     * </ul>
     *
     * @return The coordinate system for the given code.
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
     * </ul>
     *
     * @return The coordinate system for the given code.
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
     * </ul>
     *
     * @return The coordinate system for the given code.
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
     * </ul>
     *
     * @return The coordinate system for the given code.
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
     * </ul>
     *
     * @return The coordinate system for the given code.
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
     * </ul>
     *
     * @return The axis for the given code.
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
     * </ul>
     *
     * @return The unit of measurement for the given code.
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
     * </ul>
     *
     * @return The parameter descriptor for the given code.
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
     * </ul>
     *
     * @return The operation method for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public OperationMethod createOperationMethod(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.METHOD, code);
    }

    /**
     * Creates an operation for transforming coordinates in the source CRS to coordinates in the target CRS.
     * The given code can use any of the following patterns, where <var>version</var> is optional:
     * <ul>
     *   <li><var>authority</var>{@code :}<var>code</var></li>
     *   <li><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     *   <li><code>urn:ogc:def:<b>coordinateOperation</b>:</code><var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var></li>
     * </ul>
     *
     * @return The operation for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateOperation createCoordinateOperation(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.OPERATION, code);
    }

    /**
     * Clears the cache and notifies all factories need to be fetched again from the providers given at
     * construction time. All providers that are instances of {@link ServiceLoader} will have their
     * {@link ServiceLoader#reload() reload()} method invoked.
     *
     * <p>This method is intended for use in situations in which new factories can be installed into a running
     * Java virtual machine. This method invocation will happen automatically if Apache SIS is running in a
     * servlet or OSGi container.</p>
     */
    public void reload() {
        for (int type=0; type < providers.length; type++) {
            final Iterable<?> provider = providers[type];
            if (provider != null) {
                synchronized (provider) {
                    if (provider instanceof ServiceLoader<?>) {
                        ((ServiceLoader<?>) provider).reload();
                    }
                    /*
                     * Clear the 'iterationCompleted' bit before to clear the cache so that if another thread
                     * invokes 'getAuthorityFactory(…)', it will block on the synchronized(provider) statement
                     * until we finished the cleanup.
                     */
                    applyAndMask(~(1 << type));
                    /*
                     * Clear the cache on a provider-by-provider basis, not by a call to factories.clear().
                     * This is needed because this MultiAuthoritiesFactory instance may be used concurrently
                     * by other threads, and we have no global lock for the whole factory.
                     */
                    final Iterator<AuthorityFactoryIdentifier> it = factories.keySet().iterator();
                    while (it.hasNext()) {
                        if (it.next().type == type) {
                            it.remove();
                        }
                    }
                }
            }
        }
        applyAndMask(providers.length - 1);     // Clears all bits other than the bits for providers.
    }

    /**
     * Sets {@link #iterationCompleted} to {@code iterationCompleted & mask}.
     * This is used by {@link #reload()} for clearing bits.
     */
    private void applyAndMask(final int mask) {
        int value;
        do value = iterationCompleted.get();
        while (!iterationCompleted.compareAndSet(value, value & mask));
    }
}
