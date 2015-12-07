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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;


/**
 * Search in an authority factory for objects equal, ignoring metadata, to a given object.
 * This class can be used for fetching a fully {@linkplain AbstractIdentifiedObject identified object}
 * from an incomplete one, for example from an object without "{@code ID[…]}" or "{@code AUTHORITY[…]}"
 * element in <cite>Well Known Text</cite>.
 *
 * <p>The steps for using {@code IdentifiedObjectFinder} are:</p>
 * <ol>
 *   <li>Get a new instance by calling
 *       {@link GeodeticAuthorityFactory#createIdentifiedObjectFinder(Class)}.</li>
 *   <li>Optionally configure that instance by calling its setter methods.</li>
 *   <li>Perform a search by invoking the {@link #find(IdentifiedObject)} or
 *       {@link #findIdentifier(IdentifiedObject)} methods.</li>
 *   <li>Reuse the same {@code IdentifiedObjectFinder} instance for consecutive searches.</li>
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
 * @see GeodeticAuthorityFactory#createIdentifiedObjectFinder(Class)
 * @see IdentifiedObjects#lookupIdentifier(IdentifiedObject, boolean)
 */
public class IdentifiedObjectFinder {
    /**
     * The factory to use for creating objects. This is the factory specified at construction time.
     * We do not put this field in public API because the factory may not be the one the user would
     * expect. Some of our {@link GeodeticAuthorityFactory} implementations will use a wrapper
     * factory rather than the factory on which {@code createIdentifiedObjectFinder()} was invoked.
     */
    final AuthorityFactory factory;

    /**
     * The proxy for objects creation. This is usually set at construction time.
     * But in the particular case of {@link CachingAuthorityFactory#Finder}, this
     * is left to {@code null} and assigned only when a backing store factory is
     * in use.
     *
     * <p>If this field is initialized only when needed, then the following methods
     * must be overridden and ensure that the initialization has been performed:</p>
     * <ul>
     *   <li>{@link #getAuthority()}</li>
     *   <li>{@link #getCodeCandidates(IdentifiedObject)}</li>
     *   <li>{@link #find(IdentifiedObject)} (see note below)</li>
     *   <li>{@link #findIdentifier(IdentifiedObject)} (see note below)</li>
     * </ul>
     *
     * Note: the {@code find(…)} methods do not need to be overridden if all other methods
     * are overridden and the {@link #create(String, int)} method is overridden too.
     */
    private AuthorityFactoryProxy<?> proxy;

    /**
     * The parent finder, or {@code null} if none. This field is non-null only if this
     * finder is wrapped by another finder like {@link CachingAuthorityFactory.Finder}.
     */
    private IdentifiedObjectFinder parent;

    /**
     * The comparison mode.
     */
    private ComparisonMode comparisonMode = ComparisonMode.IGNORE_METADATA;

    /**
     * {@code true} for performing full scans, or {@code false} otherwise.
     */
    private boolean fullScan = true;

    /**
     * Creates a finder using the specified factory.
     *
     * <div class="note"><b>Design note:</b>
     * this constructor is protected because instances of this class should not be created directly.
     * Use {@link GeodeticAuthorityFactory#createIdentifiedObjectFinder(Class)} instead.</div>
     *
     * @param factory The factory to scan for the identified objects.
     * @param type    The type of objects to lookup.
     *
     * @see GeodeticAuthorityFactory#createIdentifiedObjectFinder(Class)
     */
    protected IdentifiedObjectFinder(final AuthorityFactory factory,
            final Class<? extends IdentifiedObject> type)
    {
        this.factory = factory;
        proxy = AuthorityFactoryProxy.getInstance(type);
    }

    /**
     * Returns the type of the objects to be created by the proxy instance.
     * This method needs to be overridden by the sub-classes that do not define a proxy.
     *
     * @return The type of object to be created, as a GeoAPI interface.
     */
    Class<? extends IdentifiedObject> getObjectType() {
        return proxy.type.asSubclass(IdentifiedObject.class);
    }

    /**
     * Returns the authority of the factory used by this finder.
     *
     * @return The authority of the factory used for the searches.
     * @throws FactoryException If an error occurred while fetching the authority.
     */
    public Citation getAuthority() throws FactoryException {
        return factory.getAuthority();
    }

    /**
     * Sets the given finder as the parent of this finder. The parent is typically a
     * {@link CachingAuthorityFactory} which will be used by {@link #findFromParent}.
     *
     * @param other The new parent.
     */
    final void setParent(final IdentifiedObjectFinder other) {
        parent = other;
    }

    /**
     * Copies the configuration of the given finder. This method provides a central place
     * where to add call to setters methods if such methods are added in a future version.
     *
     * <div class="section">Maintenance note</div>
     * Adding properties to this method is not sufficient. See also the classes that override
     * {@link #setFullScanAllowed(boolean)} - some of them may be defined in other packages.
     */
    final void copyConfiguration(final IdentifiedObjectFinder other) {
        setComparisonMode (other.getComparisonMode());
        setFullScanAllowed(other.isFullScanAllowed());
    }

    /**
     * Returns the criterion used for determining if a candidate found by this {@code IdentifiedObjectFinder}
     * should be considered equals to the requested object.
     * The default value is {@link ComparisonMode#IGNORE_METADATA}.
     *
     * @return The criterion to use for comparing objects.
     */
    public ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    /**
     * Sets the criterion used for determining if a candidate found by this {@code IdentifiedObjectFinder}
     * should be considered equals to the requested object.
     *
     * @param mode The criterion to use for comparing objects.
     */
    public void setComparisonMode(final ComparisonMode mode) {
        ArgumentChecks.ensureNonNull("mode", mode);
        comparisonMode = mode;
    }

    /**
     * If {@code true}, an exhaustive full scan against all registered objects will be performed (may be slow).
     * Otherwise only a fast lookup based on embedded identifiers and names will be performed.
     * The default value is {@code true}.
     *
     * @return {@code true} if exhaustive scans are allowed, or {@code false} for faster but less complete searches.
     */
    public boolean isFullScanAllowed() {
        return fullScan;
    }

    /**
     * Sets whatever an exhaustive scan against all registered objects is allowed.
     * The default value is {@code true}.
     *
     * @param fullScan {@code true} for allowing exhaustive scans,
     *        or {@code false} for faster but less complete searches.
     */
    public void setFullScanAllowed(final boolean fullScan) {
        this.fullScan = fullScan;
    }

    /**
     * Lookups an object which is {@linkplain Utilities#equalsIgnoreMetadata equal, ignoring metadata},
     * to the specified object.
     * The default implementation tries to instantiate some {@linkplain AbstractIdentifiedObject identified objects}
     * from the authority factory specified at construction time, in the following order:
     *
     * <ul>
     *   <li>If the specified object contains {@linkplain AbstractIdentifiedObject#getIdentifiers() identifiers}
     *       associated to the same authority than the factory, then those identifiers are used for
     *       {@linkplain GeodeticAuthorityFactory#createObject creating objects} to be tested.</li>
     *   <li>If the authority factory can create objects from their {@linkplain AbstractIdentifiedObject#getName() name}
     *       in addition of identifiers, then the name and {@linkplain AbstractIdentifiedObject#getAlias() aliases} are
     *       used for creating objects to be tested.</li>
     *   <li>If {@linkplain #isFullScanAllowed() full scan is allowed}, then full {@linkplain #getCodeCandidates
     *       set of authority codes} are used for creating objects to be tested.</li>
     * </ul>
     *
     * The first of the above created objects which is equal to the specified object in the
     * the sense of {@link Utilities#equalsIgnoreMetadata(Object, Object)} is returned.
     *
     * @param  object The object looked up.
     * @return The identified object, or {@code null} if not found.
     * @throws FactoryException if an error occurred while creating an object.
     */
    public IdentifiedObject find(final IdentifiedObject object) throws FactoryException {
        ArgumentChecks.ensureNonNull("object", object);
        /*
         * First check if one of the identifiers can be used to spot directly an
         * identified object (and check it's actually equal to one in the factory).
         */
        IdentifiedObject candidate = createFromIdentifiers(object);
        if (candidate != null) {
            return candidate;
        }
        /*
         * We are unable to find the object from its identifiers. Try a quick name lookup.
         * Some implementations like the one backed by the EPSG database are capable to find
         * an object from its name.
         */
        candidate = createFromNames(object);
        if (candidate != null) {
            return candidate;
        }
        /*
         * Here we exhausted the quick paths. Bail out if the user does not want a full scan.
         */
        return fullScan ? createFromCodes(object) : null;
    }

    /**
     * Returns the identifier of the specified object, or {@code null} if none. The default
     * implementation invokes <code>{@linkplain #find find}(object)</code> and extracts the
     * code from the returned {@linkplain AbstractIdentifiedObject identified object}.
     *
     * @param  object The object looked up.
     * @return The identifier of the given object, or {@code null} if none were found.
     * @throws FactoryException if an error occurred while creating an object.
     */
    public String findIdentifier(final IdentifiedObject object) throws FactoryException {
        final IdentifiedObject candidate = find(object);
        if (candidate == null) {
            return null;
        }
        final Citation authority = getAuthority();
        Identifier identifier = IdentifiedObjects.getIdentifier(candidate, authority);
        if (identifier == null) {
            identifier = candidate.getName();
        }
        return IdentifiedObjects.toString(identifier);
    }

    /**
     * Lookups an object from the parent finder, of from this finder if there is no parent.
     * A parent finder exists only if this finder is wrapped by another finder. The parent
     * can be the {@link CachingAuthorityFactory} finder or {@link AuthorityFactoryAdapter}
     * finder.
     *
     * <p>This method should be considered as an implementation details.
     * It is needed by {@link org.apache.sis.referencing.factory.epsg.EPSGFactory}.
     * The main purpose of this method is to allow {@link DirectAuthorityFactory}
     * implementations to look for dependencies while leveraging the cache managed
     * by their {@link CachingAuthorityFactory} wrappers.</p>
     *
     * @param  object The object looked up.
     * @param  type The type of object to look for. It does not need to be the type specified
     *         at construction time. This relaxation exists in order to allow dependencies lookup,
     *         since the dependencies may be of different kinds.
     * @return The identified object, or {@code null} if not found.
     * @throws FactoryException if an error occurred while creating an object.
     */
    final IdentifiedObject findFromParent(final IdentifiedObject object,
            final Class<? extends IdentifiedObject> type) throws FactoryException
    {
        return findFromParent(object, AuthorityFactoryProxy.getInstance(type));
    }

    /**
     * Implementation of {@link #findFromParent(IdentifiedObject, Class)}, which may invoke itself
     * recursively.  This method delegates to the parent if there is one, but still save and setup
     * the proxy for this context frame. This is because {@link CachingAuthorityFactory} delegates
     * to {@link DirectAuthorityFactory}, which delegate back to {@link CachingAuthorityFactory}
     * through this method (for object dependencies lookup), etc.
     */
    private IdentifiedObject findFromParent(final IdentifiedObject object,
            final AuthorityFactoryProxy<?> type) throws FactoryException
    {
        final IdentifiedObjectFinder parent = this.parent;
        final AuthorityFactoryProxy<?> old = proxy;
        proxy = type;
        try {
            return (parent != null) ? parent.findFromParent(object, type) : find(object);
        } finally {
            proxy = old;
        }
    }

    /**
     * Creates an object {@linkplain Utilities#equalsIgnoreMetadata equal, ignoring metadata}, to the
     * specified object using only the {@linkplain AbstractIdentifiedObject#getIdentifiers identifiers}.
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
    final IdentifiedObject createFromIdentifiers(final IdentifiedObject object) throws FactoryException {
        final Citation authority = getAuthority();
        for (final Identifier id : object.getIdentifiers()) {
            if (!Citations.identifierMatches(authority, id.getAuthority())) {
                // The identifier is not for this authority. Looks the other ones.
                continue;
            }
            final String code = IdentifiedObjects.toString(id);
            final IdentifiedObject candidate;
            try {
                candidate = (IdentifiedObject) proxy.createFromAPI(factory, code);
            } catch (NoSuchAuthorityCodeException e) {
                // The identifier was not recognized. No problem, let's go on.
                continue;
            }
            if (Utilities.deepEquals(candidate, object, comparisonMode)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Creates an object {@linkplain Utilities#equalsIgnoreMetadata equal, ignoring metadata}, to
     * the specified object using only the {@linkplain AbstractIdentifiedObject#getName name} and
     * {@linkplain AbstractIdentifiedObject#getAlias aliases}. If no such object is found, returns
     * {@code null}.
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
    final IdentifiedObject createFromNames(final IdentifiedObject object) throws FactoryException {
        String code = object.getName().getCode();
        IdentifiedObject candidate;
        try {
            candidate = (IdentifiedObject) proxy.createFromAPI(factory, code);
        } catch (FactoryException e) {
            /*
             * The identifier was not recognized. We will continue later will aliases.
             * Note: we catch a more generic exception than NoSuchAuthorityCodeException because
             *       this attempt may fail for various reasons (character string not supported
             *       by the underlying database for primary key, duplicated name found, etc.).
             */
            candidate = null;
        }
        if (Utilities.deepEquals(candidate, object, comparisonMode)) {
            return candidate;
        }
        for (final GenericName id : object.getAlias()) {
            code = id.toString();
            try {
                candidate = (IdentifiedObject) proxy.createFromAPI(factory, code);
            } catch (FactoryException e) {
                // The name was not recognized. No problem, let's go on.
                continue;
            }
            if (Utilities.deepEquals(candidate, object, comparisonMode)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Creates an object {@linkplain Utilities#equalsIgnoreMetadata equal, ignoring metadata}, to the
     * specified object. This method scans the {@linkplain #getAuthorityCodes authority codes},
     * create the objects and returns the first one which is equal to the specified object in
     * the sense of {@link Utilities#equalsIgnoreMetadata equalsIgnoreMetadata}.
     *
     * <p>This method may be used in order to get a fully {@linkplain AbstractIdentifiedObject identified
     * object} from an object without {@linkplain AbstractIdentifiedObject#getIdentifiers() identifiers}.</p>
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
    final IdentifiedObject createFromCodes(final IdentifiedObject object) throws FactoryException {
        final Set<String> codes = getCodeCandidates(object);
        if (!codes.isEmpty()) {
            for (final String code : codes) {
                final IdentifiedObject candidate;
                try {
                    candidate = (IdentifiedObject) proxy.createFromAPI(factory, code);
                } catch (FactoryException e) {
                    continue;
                }
                if (Utilities.deepEquals(candidate, object, comparisonMode)) {
                    return candidate;
                }
            }
            final Logger logger = Logging.getLogger(Loggers.CRS_FACTORY);
            logger.log(Level.FINEST, "No match found for “{0}” among {1}",
                    new Object[] {object.getName(), codes});
        }
        return null;
    }

    /**
     * Returns a set of authority codes that <strong>may</strong> identify the same object than the specified one.
     * The returned set must contains <em>at least</em> the code of every objects that are
     * {@linkplain Utilities#equalsIgnoreMetadata equal, ignoring metadata}, to the specified one.
     * However the set is not required to contains only the codes of those objects;
     * it may conservatively contains the code for more objects if an exact search is too expensive.
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
        return factory.getAuthorityCodes(getObjectType());
    }
}
