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
package org.apache.sis.referencing.operation;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Predicate;
import javax.measure.IncommensurableException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.*;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.internal.DeferredCoordinateOperation;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.referencing.internal.shared.EllipsoidalHeightCombiner;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the geoapi-4.0 branch:
import org.opengis.referencing.crs.DerivedCRS;


/**
 * Base class of code that search for coordinate operation, either by looking in a registry maintained by an authority
 * or by trying to infer the coordinate operation by itself. For maintenance and testing purposes, we separate the task
 * in two classes for the two main strategies used for finding coordinate operations:
 *
 * <ul>
 *   <li>{@code CoordinateOperationRegistry} implements the <dfn>late-binding</dfn> approach
 *       (i.e. search coordinate operation paths specified by authorities like the ones listed
 *       in the EPSG dataset), which is the preferred approach.</li>
 *   <li>{@link CoordinateOperationFinder} adds an <dfn>early-binding</dfn> approach
 *       (i.e. find a coordinate operation path by inspecting the properties associated to the CRS).
 *       That approach is used only as a fallback when the late-binding approach gave no result.</li>
 * </ul>
 *
 * When <code>{@linkplain #createOperations createOperations}(sourceCRS, targetCRS)</code> is invoked,
 * this class fetches the authority codes for source and target CRS and submits them to the authority factory
 * through a call to its <code>{@linkplain GeodeticAuthorityFactory#createFromCoordinateReferenceSystemCodes
 * createFromCoordinateReferenceSystemCodes}(sourceCode, targetCode)</code> method.
 * If the authority factory does not know about the specified CRS,
 * then {@link CoordinateOperationFinder} will use its own fallback.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class CoordinateOperationRegistry {
    /**
     * The identifier for an identity operation.
     */
    static final Identifier IDENTITY = createIdentifier(Vocabulary.Keys.Identity);

    /**
     * The identifier for an operation setting some coordinates to constant values.
     */
    static final Identifier CONSTANTS = createIdentifier(Vocabulary.Keys.Constants);

    /**
     * The identifier for conversion using an affine transform for axis swapping and/or unit conversions.
     */
    static final Identifier AXIS_CHANGES = createIdentifier(Vocabulary.Keys.AxisChanges);

    /**
     * The identifier for a transformation which is a datum shift without Bursa-Wolf parameters.
     * Only the changes in ellipsoid axis-length are taken in account.
     * Such "Unspecified datum change" are approximations and may have 1 kilometre error.
     *
     * @see #isDatumChange(Identifier)
     * @see org.apache.sis.referencing.datum.BursaWolfParameters
     * @see PositionalAccuracyConstant#DATUM_SHIFT_OMITTED
     */
    static final Identifier UNSPECIFIED_DATUM_CHANGE = createIdentifier(Vocabulary.Keys.UnspecifiedDatumChange);

    /**
     * The identifier for a transformation which is a datum shift.
     *
     * @see #isDatumChange(Identifier)
     * @see PositionalAccuracyConstant#DATUM_SHIFT_APPLIED
     */
    static final Identifier DATUM_SHIFT = createIdentifier(Vocabulary.Keys.DatumShift);

    /**
     * The identifier for a transformation between two members of the same datum ensemble.
     * The accuracy is specified by the datum ensemble.
     *
     * @see #isDatumChange(Identifier)
     */
    static final Identifier SAME_DATUM_ENSEMBLE = createIdentifier(Vocabulary.Keys.SameDatumEnsemble);

    /**
     * The identifier for a geocentric conversion.
     */
    static final Identifier GEOCENTRIC_CONVERSION = createIdentifier(Vocabulary.Keys.GeocentricConversion);

    /**
     * The identifier for an inverse operation.
     */
    private static final Identifier INVERSE_OPERATION = createIdentifier(Vocabulary.Keys.InverseOperation);

    /**
     * Creates an identifier in the Apache SIS namespace for the given vocabulary key.
     */
    private static Identifier createIdentifier(final short key) {
        return new NamedIdentifier(Citations.SIS, Vocabulary.formatInternational(key));
    }

    /**
     * Returns whether the given identifier is one of the pre-defined values used for datum changes.
     *
     * @see CoordinateOperationFinder#canHide(Identifier)
     */
    static boolean isDatumChange(final Identifier typeOfChange) {
        return typeOfChange == DATUM_SHIFT
            || typeOfChange == UNSPECIFIED_DATUM_CHANGE
            || typeOfChange == SAME_DATUM_ENSEMBLE;
    }

    /**
     * The object to use for finding authority codes, or {@code null} if none.
     * An instance is fetched at construction time from the {@link #registry} if possible.
     *
     * <h4>Design note</h4>
     * Using a finder defined by the {@link #registry} instead of {@code MultiAuthoritiesFactory} may cause
     * the finder to perform extensive searches because it does not recognize the authority code of a given CRS.
     * For example if {@link #registry} is for EPSG and a given CRS is "CRS:84", then {@code codeFinder} would
     * not recognize the given CRS and would search for a match in the EPSG database. This is desired because
     * we need to have the two CRSs defined by the same authority (if possible) in order to find a predefined
     * operation, even if an equivalent definition was provided by another authority.
     *
     * @see #authorityCodes
     * @see #findCode(CoordinateReferenceSystem)
     */
    private IdentifiedObjectFinder codeFinder;

    /**
     * The factory to use for creating operations as defined by authority, or {@code null} if none.
     * This is the factory used by the <i>late-binding</i> approach.
     */
    protected final CoordinateOperationAuthorityFactory registry;

    /**
     * The factory to use for creating coordinate operations not found in the registry.
     * This is the factory used by the <i>early-binding</i> approach.
     */
    protected final CoordinateOperationFactory factory;

    /**
     * Used only when we need a SIS-specific method.
     */
    final DefaultCoordinateOperationFactory factorySIS;

    /**
     * The spatiotemporal area of interest, or {@code null} if none.
     * When a new {@code CoordinateOperationFinder} instance is created with a non-null
     * {@link CoordinateOperationContext}, the context is used for initializing this value.
     * After initialization, this field may be updated as {@code CoordinateOperationFinder}
     * progresses in its search for a coordinate operation.
     *
     * @see CoordinateOperationContext#getAreaOfInterest()
     */
    protected Extent areaOfInterest;

    /**
     * The desired accuracy in metres, or 0 for the best accuracy available.
     *
     * @see CoordinateOperationContext#getDesiredAccuracy()
     */
    protected double desiredAccuracy;

    /**
     * {@code true} if {@code search(…)} should stop after the first coordinate operation found.
     * This field is set to {@code true} when the caller is interested only in the "best" operation
     * instead of all of possibilities.
     *
     * @see #search(CoordinateReferenceSystem, CoordinateReferenceSystem)
     */
    boolean stopAtFirst;

    /**
     * A filter that can be used for applying additional restrictions on the coordinate operation,
     * or {@code null} if none. If non-null, only operations passing this filter will be considered.
     *
     * @see CoordinateOperationContext#getOperationFilter()
     */
    private Predicate<CoordinateOperation> filter;

    /**
     * Authority codes found for CRS. This is a cache for {@link #findCode(CoordinateReferenceSystem)}.
     * This map may be non-empty only if {@link #codeFinder} is non-null.
     *
     * <h4>Design note</h4>
     * A cache is used because codes for the same CRS can be requested many times while iterating over the
     * strategies enumerated by {@link Decomposition}. This cache partially duplicates the cache provided by
     * {@link IdentifiedObjectFinder} implementations, but we have no guarantees that those implementations
     * provide such cache, and the values cached here are the result of a little bit more work.
     *
     * @see #codeFinder
     * @see #findCode(CoordinateReferenceSystem)
     */
    private final Map<CoordinateReferenceSystem, List<String>> authorityCodes;

    /**
     * The locale for error messages.
     *
     * @todo Add a setter method, or make configurable in some way.
     */
    private Locale locale;

    /**
     * Creates a new instance for the given factory and context.
     *
     * @param  registry  the factory to use for creating operations as defined by authority, or {@code null} if none.
     * @param  factory   the factory to use for creating operations not found in the registry.
     * @param  context   the area of interest and desired accuracy, or {@code null} if none.
     * @throws FactoryException if an error occurred while initializing this {@link CoordinateOperationRegistry}.
     */
    CoordinateOperationRegistry(final CoordinateOperationAuthorityFactory registry,
                                final CoordinateOperationFactory          factory,
                                final CoordinateOperationContext          context) throws FactoryException
    {
        this.registry = registry;
        this.factory  = Objects.requireNonNull(factory);
        factorySIS    = (factory instanceof DefaultCoordinateOperationFactory)
                        ? (DefaultCoordinateOperationFactory) factory
                        : DefaultCoordinateOperationFactory.provider();

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        Map<CoordinateReferenceSystem, List<String>> authorityCodes = Collections.emptyMap();
        if (registry != null) {
            if (registry instanceof GeodeticAuthorityFactory) {
                codeFinder = ((GeodeticAuthorityFactory) registry).newIdentifiedObjectFinder();
            } else try {
                codeFinder = IdentifiedObjects.newFinder(Citations.toCodeSpace(registry.getAuthority()));
            } catch (NoSuchAuthorityFactoryException e) {
                recoverableException("<init>", e);
            }
            if (codeFinder != null) {
                authorityCodes = new IdentityHashMap<>(5);          // Rarely more than 4 entries.
            }
        }
        this.authorityCodes = authorityCodes;
        if (context != null) {
            areaOfInterest  = context.getAreaOfInterest();
            desiredAccuracy = context.getDesiredAccuracy();
            filter          = context.getOperationFilter();
        }
    }

    /**
     * If the authority defines an object equal, ignoring metadata, to the given object, returns that authority object.
     * Otherwise returns the given object unchanged. We do not invoke this method for user supplied CRS, but only for
     * CRS or other objects created by {@code CoordinateOperationRegistry} as intermediate step.
     */
    final <T extends IdentifiedObject> T toAuthorityDefinition(final Class<T> type, final T object) throws FactoryException {
        if (codeFinder != null) {
            codeFinder.setIgnoringAxes(false);
            codeFinder.setSearchDomain(IdentifiedObjectFinder.Domain.VALID_DATASET);
            final IdentifiedObject candidate = codeFinder.findSingleton(object);
            if (Utilities.deepEquals(object, candidate, ComparisonMode.COMPATIBILITY)) {
                return type.cast(candidate);
            }
        }
        return object;
    }

    /**
     * Finds the authority codes for the given coordinate reference system.
     * This method does not trust the code given by the user in the CRS - it verifies it.
     * This method may return codes even if the axis order does not match;
     * it will be caller's responsibility to make necessary adjustments.
     *
     * @param  crs  the CRS for which to search authority codes.
     * @return authority codes for the given CRS, or an empty list if none.
     *         <b>Do not modify</b> since this list may be cached.
     */
    private List<String> findCode(final CoordinateReferenceSystem crs) throws FactoryException {
        List<String> codes = authorityCodes.get(crs);
        if (codes == null) {
            if (codeFinder == null) {
                return Collections.emptyList();
            }
            codes = new ArrayList<>();
            codeFinder.setIgnoringAxes(true);
            codeFinder.setSearchDomain(isEasySearch(crs) ? IdentifiedObjectFinder.Domain.EXHAUSTIVE_VALID_DATASET
                                                         : IdentifiedObjectFinder.Domain.VALID_DATASET);
            int matchCount = 0;
            try {
                final Citation authority = registry.getAuthority();
                for (final IdentifiedObject candidate : codeFinder.find(crs)) {
                    final Identifier identifier = IdentifiedObjects.getIdentifier(candidate, authority);
                    if (identifier != null) {
                        final String code = identifier.getCode();
                        if (Utilities.deepEquals(candidate, crs, ComparisonMode.APPROXIMATE)) {
                            // If axis order matches, give precedence to that CRS.
                            codes.add(matchCount++, code);
                        } else {
                            codes.add(code);
                        }
                    }
                }
            } catch (UnavailableFactoryException e) {
                log(null, e);
                codeFinder = null;
            } catch (BackingStoreException e) {
                throw e.unwrapOrRethrow(FactoryException.class);
            }
            authorityCodes.put(crs, codes);
        }
        return codes;
    }

    /**
     * Strategies for decomposing a three-dimensional CRS into components that may be present in EPSG database.
     * The database may have no entries for a coordinate operation between a pair of 3D CRS with (x,y,z) axes,
     * while entries for the horizontal components (2D CRS with (x,y) axes) may exist. This class enumerates
     * which decompositions to apply on the 3D CRS before searching in EPSG database. Those decompositions will
     * be applied in the order they are enumerated.
     */
    private enum Decomposition {
        /**
         * Search for a coordinate operation with the CRS as specified, without any reduction.
         * It should be the first strategy to try before anything else.
         */
        NONE(false, false),

        /**
         * If the target CRS is three-dimensional, make it two-dimensional and search again in database.
         * The source CRS is left as-is; it may be two- or three-dimensional.
         * This strategy avoid the last of source coordinates since they may be used in calculation.
         */
        HORIZONTAL_TARGET(false, true),

        /**
         * Make both source and target CRS two-dimensional and search again in the database.
         * This strategy should not be applied before {@link #HORIZONTAL_TARGET} because we
         * want to use the <var>z</var> coordinate value to be used if possible.
         */
        HORIZONTAL(true, true),

        /**
         * If the source CRS is three-dimensional, make it two-dimensional and search again in database.
         * The target CRS is left as-is; it may be two- or three-dimensional. This strategy is rarely
         * applicable because it would cause the <var>z</var> coordinate value to appear from nowhere,
         * but we nevertheless test it as a matter of principle.
         */
        HORIZONTAL_SOURCE(true, false);

        /**
         * Whether to extract the horizontal component of the source or target CRS.
         */
        final boolean source, target;

        /**
         * Creates a new decomposition strategy.
         */
        private Decomposition(final boolean source, final boolean target) {
            this.source = source;
            this.target = target;
        }

        /**
         * Returns the horizontal component to search in the database, or {@code null} if no search should be done.
         * This method returns {@code null} if the horizontal component is not different then the given CRS,
         * in which case searching that CRS in the database would be redundant with previous searches.
         * It also returns {@code null} if the CRS is a compound CRS, in which case the separation
         * in single CRS should be done by the {@link CoordinateOperationFinder} subclass.
         */
        static SingleCRS horizontal(final CoordinateReferenceSystem crs) {
            if (crs instanceof SingleCRS) {
                final SingleCRS sep = CRS.getHorizontalComponent(crs);
                if (sep != crs) return sep;             // May be null.
            }
            return null;
        }
    }

    /**
     * Returns whether it is presumed easy for {@link IdentifiedObjectFinder} to perform a full search
     * (ignoring axis order) for the given CRS. An important criterion is to avoid all CRS containing
     * a coordinate operation (in particular projected CRS), because they are difficult to search.
     *
     * <p>We allow extensive search of geographic CRS because the EPSG database has some geographic CRS
     * defined with both (longitude, latitude) and (latitude, longitude) axis order. But operations may
     * be defined for only one axis order. So if the user specified the CRS with (longitude, latitude)
     * axis order, we want to check also the coordinate operations using (latitude, longitude) axis order
     * because they may be the only ones available.</p>
     */
    private static boolean isEasySearch(final CoordinateReferenceSystem crs) {
        if (crs instanceof DerivedCRS) {
            return false;
        }
        if (crs instanceof CompoundCRS) {
            for (CoordinateReferenceSystem c : ((CompoundCRS) crs).getComponents()) {
                if (!isEasySearch(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Finds or infers operations for conversions or transformations between two coordinate reference systems.
     * {@code CoordinateOperationRegistry} implements the <i>late-binding</i> approach (see definition
     * of terms in class javadoc) by extracting the authority codes from the supplied {@code sourceCRS} and
     * {@code targetCRS}, then by submitting those codes to the
     * <code>{@linkplain CoordinateOperationAuthorityFactory#createFromCoordinateReferenceSystemCodes
     * createFromCoordinateReferenceSystemCodes}(sourceCode, targetCode)</code> method.
     *
     * <p>Operations in the returned list are ordered in preference order (preferred operation first).
     * If no operation is found for those codes, then this method returns an empty list.
     * Note that it does not mean that no path exist;
     * it only means that it was not defined explicitly in the registry.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return coordinate operations from {@code sourceCRS} to {@code targetCRS},
     *         or an empty list if no such operation is explicitly defined in the underlying database.
     * @throws FactoryException if the operation creation failed.
     */
    public List<CoordinateOperation> createOperations(final CoordinateReferenceSystem sourceCRS,
                                                      final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        SingleCRS source2D = null;
        SingleCRS target2D = null;
        boolean sourceCached = false;
        boolean targetCached = false;
        for (final Decomposition decompose : Decomposition.values()) {
            /*
             * First, try directly the provided (sourceCRS, targetCRS) pair. If that pair does not work,
             * try to use different combinations of user-provided CRS and two-dimensional components of
             * those CRS. The code below assumes that the user-provided CRS are three-dimensional, but
             * actually it works for two-dimensional CRS too without testing twice the same combinations.
             */
            CoordinateReferenceSystem source = sourceCRS;
            CoordinateReferenceSystem target = targetCRS;
            if (decompose.source) {
                if (!sourceCached) {
                    sourceCached = true;
                    source2D = Decomposition.horizontal(sourceCRS);
                }
                source = source2D;
            }
            if (decompose.target) {
                if (!targetCached) {
                    targetCached = true;
                    target2D = Decomposition.horizontal(targetCRS);
                }
                target = target2D;
            }
            /*
             * Search for coordinate operations between the pair of CRS components that we just found.
             * We may need to force a search of all CRS variants (equivalent CRS except for axis order)
             * for resolving the case where the geodetic database contains such pairs of equivalent CRS
             * (e.g. EPSG::4171 versus EPSG::7084). For reducing the cost, we will force full scan only
             * for CRS for which `isEasySearch(…)` returns `true`.
             */
            if (source != null && target != null) try {
                final List<CoordinateOperation> operations = search(source, target);
                if (operations != null) {
                    /*
                     * Found at least one operation. If we had to extract the horizontal part of some 3D CRS, then
                     * we need to modify the coordinate operation in order to match the new number of dimensions.
                     * Some operations may be lost if we do not know how to propagate the vertical CRS.
                     * If at least one operation remains, we are done.
                     */
                    if (decompose.source | decompose.target) {
                        for (int i=operations.size(); --i >= 0;) {
                            CoordinateOperation operation = operations.get(i);
                            operation = propagateVertical(sourceCRS, targetCRS, operation, decompose);
                            if (operation != null) {
                                operation = complete(operation, sourceCRS, targetCRS);
                                operations.set(i, operation);
                            } else {
                                operations.remove(i);
                            }
                        }
                    }
                    if (!operations.isEmpty()) {
                        return operations;
                    }
                }
            } catch (IllegalArgumentException | IncommensurableException | NoninvertibleTransformException e) {
                CRSPair key = new CRSPair(sourceCRS, targetCRS);
                String message = resources().getString(Resources.Keys.CanNotInstantiateGeodeticObject_1, key);
                String details = e.getLocalizedMessage();
                if (details != null) {
                    message = message + ' ' + details;
                }
                throw new FactoryException(message, e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Returns operations for conversions or transformations between two coordinate reference systems.
     * This method extracts the authority code from the supplied {@code sourceCRS} and {@code targetCRS},
     * and submits them to the {@link #registry}. If no operation is found for those codes, then this method
     * returns {@code null}.
     *
     * @param  sourceCRS  source coordinate reference system.
     * @param  targetCRS  target coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS},
     *         or {@code null} if no such operation is explicitly defined in the underlying database.
     * @throws IllegalArgumentException if the coordinate systems are not of the same type or axes do not match.
     * @throws IncommensurableException if the units are not compatible or a unit conversion is non-linear.
     * @throws NoninvertibleTransformException if a step needs to be inverted but is not invertible.
     * @throws FactoryException if an error occurred while creating the operation.
     */
    private List<CoordinateOperation> search(final CoordinateReferenceSystem sourceCRS,
                                             final CoordinateReferenceSystem targetCRS)
            throws IncommensurableException, NoninvertibleTransformException, FactoryException
    {
        final List<String> sources = findCode(sourceCRS); if (sources.isEmpty()) return null;
        final List<String> targets = findCode(targetCRS); if (targets.isEmpty()) return null;
        final List<CoordinateOperation> operations = new ArrayList<>();
        boolean foundDirectOperations = false;
        boolean useDeprecatedOperations = false;
        for (final String sourceID : sources) {
            for (final String targetID : targets) {
                if (sourceID.equals(targetID)) {
                    /*
                     * Above check is necessary because this method may be invoked in some situations where the code
                     * are equal while the CRS are not. Such situation should be illegal, but unfortunately it still
                     * happen because many software products are not compliant with EPSG definition of axis order.
                     * In such cases we will need to compute a transform from sourceCRS to targetCRS ignoring the
                     * source and target codes. The CoordinateOperationFinder class can do that, providing that we
                     * prevent this CoordinateOperationRegistry to (legitimately) claims that the operation from
                     * sourceCode to targetCode is the identity transform.
                     */
                    return null;
                }
                /*
                 * Some pairs of CRS have a lot of coordinate operations backed by datum shift grids.
                 * We do not want to load all of them until we found the right coordinate operation.
                 * The non-public Semaphores.METADATA_ONLY mechanism instructs EPSGDataAccess to
                 * instantiate DeferredCoordinateOperation instead of full coordinate operations.
                 */
                final boolean mdOnly = Semaphores.queryAndSet(Semaphores.METADATA_ONLY);
                try {
                    Collection<CoordinateOperation> authoritatives;
                    try {
                        authoritatives = registry.createFromCoordinateReferenceSystemCodes(sourceID, targetID);
                        final boolean inverse = Containers.isNullOrEmpty(authoritatives);
                        if (inverse) {
                            /*
                             * No operation from `source` to `target` available. But maybe there is an inverse operation.
                             * This is typically the case when the user wants to convert from a projected to a geographic CRS.
                             * The EPSG database usually contains transformation paths for geographic to projected CRS only.
                             */
                            if (foundDirectOperations) {
                                continue;                       // Ignore inverse operations if we already have direct ones.
                            }
                            authoritatives = registry.createFromCoordinateReferenceSystemCodes(targetID, sourceID);
                            if (Containers.isNullOrEmpty(authoritatives)) {
                                continue;
                            }
                        } else if (!foundDirectOperations) {
                            foundDirectOperations = true;
                            operations.clear();                 // Keep only direct operations.
                        }
                    } catch (NoSuchAuthorityCodeException | MissingFactoryResourceException e) {
                        /*
                         * sourceCode or targetCode is unknown to the underlying authority factory.
                         * Ignores the exception and fallback on the generic algorithm provided by
                         * CoordinateOperationFinder.
                         */
                        log(null, e);
                        continue;
                    }
                    /*
                     * If we found at least one non-deprecated operation, we will stop the search at
                     * the first deprecated one (assuming that deprecated operations are sorted last).
                     * Deprecated operations are kept only if there are no non-deprecated operations.
                     */
                    try {
                        for (final CoordinateOperation candidate : authoritatives) {
                            if (candidate != null) {                                    // Paranoiac check.
                                if ((candidate instanceof Deprecable) && ((Deprecable) candidate).isDeprecated()) {
                                    if (!useDeprecatedOperations && !operations.isEmpty()) break;
                                    useDeprecatedOperations = true;
                                } else if (useDeprecatedOperations) {
                                    useDeprecatedOperations = false;
                                    operations.clear();              // Replace deprecated operations by non-deprecated ones.
                                }
                                operations.add(candidate);
                            }
                        }
                    } catch (BackingStoreException exception) {
                        throw exception.unwrapOrRethrow(FactoryException.class);
                    }
                } finally {
                    Semaphores.clearIfFalse(Semaphores.METADATA_ONLY, mdOnly);
                }
            }
        }
        /*
         * At this point we got the list of coordinate operations. Now, sort them in preference order.
         * We will loop over all coordinate operations and select the one having the largest intersection
         * with the area of interest. Note that if the user did not specify an area of interest himself,
         * then we need to get one from the CRS. This is necessary for preventing the transformation from
         * NAD27 to NAD83 in Idaho to select the transform for Alaska (since the latter has a larger area).
         */
        CoordinateOperationSorter.sort(operations, Extents.getGeographicBoundingBox(areaOfInterest));
        final ListIterator<CoordinateOperation> it = operations.listIterator();
        while (it.hasNext()) {
            /*
             * At this point we filtered a CoordinateOperation by looking only at its metadata.
             * Code following this point will need the full coordinate operation, including its
             * MathTransform. So if we got a deferred operation, we need to resolve it now.
             * Conversely, we should not use metadata below this point because the call to
             * inverse(CoordinateOperation) is not guaranteed to preserve all metadata.
             */
            CoordinateOperation operation = it.next();
            try {
                if (operation instanceof DeferredCoordinateOperation) {
                    operation = ((DeferredCoordinateOperation) operation).create();
                }
                if (operation instanceof SingleOperation && operation.getMathTransform() == null) {
                    operation = fromDefiningConversion((SingleOperation) operation,
                                    foundDirectOperations ? sourceCRS : targetCRS,
                                    foundDirectOperations ? targetCRS : sourceCRS);
                }
                if (!foundDirectOperations) {
                    operation = inverse(operation);
                }
                if (operation == null) {
                    it.remove();
                    continue;
                }
            } catch (NoninvertibleTransformException | MissingFactoryResourceException e) {
                /*
                 * If we failed to get the real CoordinateOperation instance, remove it from
                 * the collection and try again in order to get the next best choices.
                 */
                log(null, e);
                it.remove();
                continue;                                   // Try again with the next best case.
            }
            /*
             * It is possible that the CRS given to this method were not quite right.  For example, the user
             * may have created his CRS from a WKT using a different axis order than the order specified by
             * the authority and still (wrongly) call those CRS "EPSG:xxxx".  So we check if the source and
             * target CRS for the operation we just created are equivalent to the CRS specified by the user.
             *
             * NOTE: FactoryException may be thrown if we fail to create a transform from the user-provided
             * CRS to the authority-provided CRS. That transform should have been only an identity transform,
             * or a simple affine transform if the user specified wrong CRS as explained in above paragraph.
             * If we fail here, we are likely to fail for all other transforms. So we are better to let the
             * FactoryException propagate.
             */
            operation = complete(operation, sourceCRS, targetCRS);
            if (filter == null || filter.test(operation)) {
                if (stopAtFirst) {
                    operations.clear();
                    operations.add(operation);
                    break;
                }
                it.set(operation);
            } else {
                it.remove();
            }
        }
        return operations;
    }

    /**
     * Creates the inverse of the given single operation.
     * If this operation succeed, then the returned coordinate operation has the following properties:
     *
     * <ul>
     *   <li>Its {@code sourceCRS} is the {@code targetCRS} of the given operation.</li>
     *   <li>Its {@code targetCRS} is the {@code sourceCRS} of the given operation.</li>
     *   <li>Its {@code interpolationCRS} is {@code null}.</li>
     *   <li>Its {@code MathTransform} is the inverse of the {@code MathTransform} of the given operation.</li>
     *   <li>Its domain of validity and accuracy is the same.</li>
     * </ul>
     *
     * <h4>Accuracy note</h4>
     * In many cases, the inverse operation is numerically less accurate than the direct operation because it
     * uses approximations like series expansions or iterative methods. However, the numerical errors caused by
     * those approximations are not of interest here, because they are usually much smaller than the inaccuracy
     * due to the stochastic nature of coordinate transformations (not to be confused with coordinate conversions;
     * see ISO 19111 for more information).
     */
    final CoordinateOperation inverse(final SingleOperation op) throws NoninvertibleTransformException, FactoryException {
        CoordinateOperation inverse = AbstractCoordinateOperation.getCachedInverse(op);
        if (inverse != null) {
            return inverse;
        }
        final CoordinateReferenceSystem sourceCRS = op.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = op.getTargetCRS();
        final MathTransform transform = op.getMathTransform().inverse();
        final OperationMethod method = InverseOperationMethod.create(op.getMethod(), factorySIS);
        final Map<String,Object> properties = properties(INVERSE_OPERATION);
        InverseOperationMethod.properties(op, properties);
        inverse = createFromMathTransform(properties, targetCRS, sourceCRS, transform, method, null, typeOf(op));
        AbstractCoordinateOperation.setCachedInverse(op, inverse);
        return inverse;
    }

    /**
     * Creates the inverse of the given operation, which may be single or compound.
     *
     * <h4>Design note</h4>
     * We do not provide a {@code AbstractCoordinateOperation.inverse()} method. If the user wants an inverse method,
     * he should invoke {@code CRS.findOperation(targetCRS, sourceCRS, null)} or something equivalent. This is because
     * a new query of EPSG database may be necessary, and if no explicit definition is found there is too many arbitrary
     * values to set in a default inverse operation for making that API public.
     *
     * @param  operation  the operation to invert, or {@code null}.
     * @return the inverse of {@code operation}, or {@code null} if none.
     * @throws NoninvertibleTransformException if the operation is not invertible.
     * @throws FactoryException if the operation creation failed for another reason.
     */
    private CoordinateOperation inverse(final CoordinateOperation operation)
            throws NoninvertibleTransformException, FactoryException
    {
        if (operation instanceof SingleOperation) {
            return inverse((SingleOperation) operation);
        }
        CoordinateOperation inverse = AbstractCoordinateOperation.getCachedInverse(operation);
        if (inverse != null) {
            return inverse;
        }
        if (operation instanceof ConcatenatedOperation) {
            final CoordinateOperation[] inverted = getSteps((ConcatenatedOperation) operation, true);
            ArraysExt.reverse(inverted);
            final Map<String,Object> properties = properties(INVERSE_OPERATION);
            final MathTransform transform = operation.getMathTransform();
            if (transform != null) {
                properties.put(DefaultConcatenatedOperation.TRANSFORM_KEY, transform.inverse());
            }
            inverse = factory.createConcatenatedOperation(properties, null, null, inverted);
            AbstractCoordinateOperation.setCachedInverse(operation, inverse);
        }
        return inverse;
    }

    /**
     * Returns all steps of the given concatenated operation, making sure that they are in the specified direction.
     * This method returns an array where the source CRS of the first step is the {@code operation} source CRS, and
     * where the target CRS of each step is the source CRS of the next step. If needed, some steps may be inverted
     * in order to fulfill that requirement.
     *
     * <p>If {@code inverse} if {@code true}, then this method inverses all steps. Note however that the element
     * order in the returned array is not inverted (this is left to the caller).</p>
     *
     * @param  operation  the operation for which to get the steps.
     * @return all steps of the given concatenated operation.
     * @throws NoninvertibleTransformException if a step needs to be inverted but is not invertible.
     * @throws FactoryException if the operation creation failed for another reason (e.g., inconsistency found).
     */
    private CoordinateOperation[] getSteps(final ConcatenatedOperation operation, final boolean inverse)
            throws NoninvertibleTransformException, FactoryException
    {
        final var steps = operation.getOperations().toArray(CoordinateOperation[]::new);
        CoordinateReferenceSystem previous = operation.getSourceCRS();
        for (int i=0; i<steps.length; i++) {
            final CoordinateOperation step = steps[i];
            final CoordinateReferenceSystem source = step.getSourceCRS();
            final CoordinateReferenceSystem target = step.getTargetCRS();
            final boolean r = DefaultConcatenatedOperation.verifyStepChaining(null, i, previous, source, target);
            if (r != inverse) {
                if ((steps[i] = inverse(step)) == null) {
                    throw new NoninvertibleTransformException(resources().getString(
                                Resources.Keys.NonInvertibleOperation_1, label(step)));
                }
            }
            previous = r ? source : target;
        }
        return steps;
    }

    /**
     * Completes (if necessary) the given coordinate operation for making sure that the source CRS
     * is the given one and the target CRS is the given one.  In principle, the given CRS shall be
     * equivalent to the operation source/target CRS. However, discrepancies happen if the user CRS
     * have flipped axis order, or if we looked for 2D operation while the user provided 3D CRS.
     *
     * @param  operation  the coordinate operation to complete.
     * @param  sourceCRS  the source CRS requested by the user.
     * @param  targetCRS  the target CRS requested by the user.
     * @return a coordinate operation for the given source and target CRS.
     * @throws IllegalArgumentException if the coordinate systems are not of the same type or axes do not match.
     * @throws IncommensurableException if the units are not compatible or a unit conversion is non-linear.
     * @throws NoninvertibleTransformException if a step needs to be inverted but is not invertible.
     * @throws FactoryException if the operation cannot be constructed.
     */
    private CoordinateOperation complete(final CoordinateOperation       operation,
                                         final CoordinateReferenceSystem sourceCRS,
                                         final CoordinateReferenceSystem targetCRS)
            throws IncommensurableException, NoninvertibleTransformException, FactoryException
    {
        CoordinateReferenceSystem source = operation.getSourceCRS();
        CoordinateReferenceSystem target = operation.getTargetCRS();
        final MathTransformFactory mtFactory = factorySIS.getMathTransformFactory();
        final MathTransform prepend = swapAndScaleAxes(sourceCRS, source, mtFactory);
        final MathTransform append  = swapAndScaleAxes(target, targetCRS, mtFactory);
        if (prepend != null) source = sourceCRS;
        if (append  != null) target = targetCRS;
        return transform(source, prepend, operation, append, target, mtFactory);
    }

    /**
     * Returns an affine transform between two coordinate systems.
     * Only units and axes order are taken in account by this method.
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @param  mtFactory  the math transform factory to use.
     * @return the transform from the given source to the given target CRS, or {@code null} if none is needed.
     * @throws IllegalArgumentException if the coordinate systems are not of the same type or axes do not match.
     * @throws IncommensurableException if the units are not compatible or a unit conversion is non-linear.
     * @throws FactoryException if an error occurred while creating a math transform.
     */
    private static MathTransform swapAndScaleAxes(final CoordinateReferenceSystem sourceCRS,
                                                  final CoordinateReferenceSystem targetCRS,
                                                  final MathTransformFactory      mtFactory)
            throws IncommensurableException, FactoryException
    {
        /*
         * Assertion: source and target CRS must be equal, ignoring change in axis order or units.
         * The first line is for disabling this check if the number of dimensions are not the same
         * (e.g. as in the "geographic 3D to geographic 2D" conversion) because ALLOW_VARIANT mode
         * still requires a matching number of dimensions.
         */
        assert ReferencingUtilities.getDimension(sourceCRS) != ReferencingUtilities.getDimension(targetCRS)
                || Utilities.deepEquals(sourceCRS, targetCRS, ComparisonMode.ALLOW_VARIANT);
        final Matrix m = CoordinateSystems.swapAndScaleAxes(sourceCRS.getCoordinateSystem(), targetCRS.getCoordinateSystem());
        return (m.isIdentity()) ? null : mtFactory.createAffineTransform(m);
    }

    /**
     * Appends or prepends the specified math transforms to the transform of the given operation.
     * The new coordinate operation (if any) will share the same metadata as the original operation,
     * except the authority code.
     *
     * <p>This method is used in order to change axis order when the user-specified CRS disagree
     * with the authority-supplied CRS.</p>
     *
     * @param  sourceCRS  the source CRS to give to the new operation.
     * @param  prepend    the transform to prepend to the operation math transform, or {@code null} if none.
     * @param  operation  the operation in which to prepend the math transforms.
     * @param  append     the transform to append to the operation math transform, or {@code null} if none.
     * @param  targetCRS  the target CRS to give to the new operation.
     * @param  mtFactory  the math transform factory to use.
     * @return a new operation, or {@code operation} if {@code prepend} and {@code append} were nulls or identity transforms.
     * @throws IllegalArgumentException if the operation method cannot have the desired number of dimensions.
     * @throws NoninvertibleTransformException if a step needs to be inverted but is not invertible.
     * @throws FactoryException if the operation cannot be constructed.
     */
    private CoordinateOperation transform(final CoordinateReferenceSystem sourceCRS,
                                          final MathTransform             prepend,
                                                CoordinateOperation       operation,
                                          final MathTransform             append,
                                          final CoordinateReferenceSystem targetCRS,
                                          final MathTransformFactory      mtFactory)
            throws NoninvertibleTransformException, FactoryException
    {
        if ((prepend == null || prepend.isIdentity()) && (append == null || append.isIdentity())) {
            return operation;
        }
        /*
         * In the particular case of concatenated operations, we cannot prepend or append a math transform to
         * the operation as a whole (the math transform for a concatenated operation is computed automatically
         * as the concatenation of the transforms from every single operations, and we need to stay consistent
         * with that). Instead, prepend to the first single operation and append to the last single operation.
         */
        if (operation instanceof ConcatenatedOperation) {
            final CoordinateOperation[] steps = getSteps((ConcatenatedOperation) operation, false);
            switch (steps.length) {
                case 0: break;                              // Illegal, but we are paranoiac.
                case 1: operation = steps[0]; break;        // Useless ConcatenatedOperation.
                default: {
                    final int n = steps.length - 1;
                    final CoordinateOperation first = steps[0];
                    final CoordinateOperation last  = steps[n];
                    steps[0] = transform(sourceCRS, prepend, first, null, first.getTargetCRS(), mtFactory);
                    steps[n] = transform(last.getSourceCRS(), null, last, append, targetCRS,    mtFactory);
                    return factory.createConcatenatedOperation(derivedFrom(operation), null, null, steps);
                }
            }
        }
        /*
         * Single operation case.
         */
        MathTransform transform = operation.getMathTransform();
        if (prepend != null) transform = mtFactory.createConcatenatedTransform(prepend, transform);
        if (append  != null) transform = mtFactory.createConcatenatedTransform(transform, append);
        assert !transform.equals(operation.getMathTransform()) : transform;
        return recreate(operation, sourceCRS, targetCRS, transform, null);
    }

    /**
     * Creates a new coordinate operation with the same method as the given operation, but different CRS.
     * The CRS may differ either in the number of dimensions (i.e. let the vertical coordinate pass through),
     * or in axis order (i.e. axis order in user CRS were not compliant with authority definition).
     *
     * @param  operation  the operation specified by the authority.
     * @param  sourceCRS  the source CRS specified by the user.
     * @param  targetCRS  the target CRS specified by the user
     * @param  transform  the math transform to use in replacement to the one in {@code operation}.
     * @param  method     the operation method, or {@code null} for attempting an automatic detection.
     * @return a new operation from the given source CRS to target CRS using the given transform.
     * @throws IllegalArgumentException if the operation method cannot have the desired number of dimensions.
     * @throws FactoryException if an error occurred while creating the new operation.
     */
    private CoordinateOperation recreate(final CoordinateOperation       operation,
                                               CoordinateReferenceSystem sourceCRS,
                                               CoordinateReferenceSystem targetCRS,
                                         final MathTransform             transform,
                                               OperationMethod           method)
            throws FactoryException
    {
        /*
         * If the user-provided CRS are approximately equal to the coordinate operation CRS, keep the latter.
         * The reason is that coordinate operation CRS are built from the definitions provided by the authority,
         * while the user-provided CRS can be anything (e.g. parsed from a quite approximated WKT).
         */
        CoordinateReferenceSystem crs;
        if (Utilities.equalsApproximately(sourceCRS, crs = operation.getSourceCRS())) sourceCRS = crs;
        if (Utilities.equalsApproximately(targetCRS, crs = operation.getTargetCRS())) targetCRS = crs;
        final Map<String,Object> properties = new HashMap<>(derivedFrom(operation));
        properties.put(CoordinateOperations.OPERATION_TYPE_KEY, typeOf(operation));
        /*
         * Reuse the same operation method, but we may need to change its number of dimension.
         * For example the "Affine" set of parameters depend on the number of dimensions.
         * The capability to resize an operation method is specific to Apache SIS.
         */
        if (operation instanceof SingleOperation) {
            final SingleOperation single = (SingleOperation) operation;
            properties.put(CoordinateOperations.PARAMETERS_KEY, single.getParameterValues());
            if (method == null) {
                method = single.getMethod();
                if (method instanceof AbstractProvider) {
                    method = ((AbstractProvider) method).variantFor(transform);
                }
            }
        }
        return factorySIS.createSingleOperation(properties, sourceCRS, targetCRS,
                operation.getInterpolationCRS().orElse(null), method, transform);
    }

    /**
     * Creates a complete coordinate operation from a defining conversion. Defining conversions usually have
     * null source and target CRS, but this method nevertheless checks that, in order to reuse the operation
     * CRS if it happens to have some.
     *
     * @param  operation  the operation specified by the authority.
     * @param  sourceCRS  the source CRS specified by the user.
     * @param  targetCRS  the target CRS specified by the user
     * @return a new operation from the given source CRS to target CRS.
     * @throws FactoryException if an error occurred while creating the new operation.
     */
    private CoordinateOperation fromDefiningConversion(final SingleOperation     operation,
                                                       CoordinateReferenceSystem sourceCRS,
                                                       CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        final ParameterValueGroup parameters = operation.getParameterValues();
        if (parameters != null) {
            CoordinateReferenceSystem crs;
            if (Utilities.equalsApproximately(sourceCRS, crs = operation.getSourceCRS())) sourceCRS = crs;
            if (Utilities.equalsApproximately(targetCRS, crs = operation.getTargetCRS())) targetCRS = crs;
            final var builder = createTransformBuilder(parameters, sourceCRS, targetCRS);
            final MathTransform mt = builder.create();      // Must be before `operation.getMethod()`.
            return factorySIS.createSingleOperation(IdentifiedObjects.getProperties(operation),
                    sourceCRS, targetCRS, null, operation.getMethod(), mt);
        } else {
            // Should never happen because parameters are mandatory, but let be safe.
            log(resources().createLogRecord(Level.WARNING, Resources.Keys.MissingParameterValues_1,
                    IdentifiedObjects.getIdentifierOrName(operation)), null);
        }
        return null;
    }

    /**
     * Returns a new coordinate operation with the ellipsoidal height added either in the source coordinates,
     * in the target coordinates or both. If there is an ellipsoidal transform, then this method updates the
     * transforms in order to use the ellipsoidal height (it has an impact on the transformed values).
     *
     * <p>This method requires that the EPSG factory insert explicit <q>Geographic3D to 2D conversion</q>
     * operations (EPSG:9659) in the operations chain, or an equivalent operation (recognized by its matrix shape).
     * This method tries to locate and remove EPSG:9659 or equivalent operation from the operation chain in order
     * to get three-dimensional domains.</p>
     *
     * <p>This method is not guaranteed to succeed in adding the ellipsoidal height. It works on a
     * <em>best effort</em> basis. In any cases, the {@link #complete} method should be invoked
     * after this one in order to ensure that the source and target CRS are the expected ones.</p>
     *
     * @param  sourceCRS  the potentially three-dimensional source CRS
     * @param  targetCRS  the potentially three-dimensional target CRS
     * @param  operation  the original (typically two-dimensional) coordinate operation.
     * @param  decompose  the decomposition which has been applied.
     *         If {@code decompose.source} is {@code true}, ellipsoidal height will be added to source coordinates.
     *         If {@code decompose.target} is {@code true}, ellipsoidal height will be added to target coordinates.
     * @return a coordinate operation with the source and/or target coordinates made 3D,
     *         or {@code null} if this method does not know how to create the operation.
     * @throws IllegalArgumentException if the operation method cannot have the desired number of dimensions.
     * @throws NoninvertibleTransformException if a step needs to be inverted but is not invertible.
     * @throws FactoryException if an error occurred while creating the coordinate operation.
     */
    private CoordinateOperation propagateVertical(final CoordinateReferenceSystem sourceCRS,
                                                  final CoordinateReferenceSystem targetCRS,
                                                  final CoordinateOperation operation,
                                                  final Decomposition decompose)
            throws NoninvertibleTransformException, FactoryException
    {
        final List<CoordinateOperation> operations = new ArrayList<>();
        if (operation instanceof ConcatenatedOperation) {
            operations.addAll(Arrays.asList(getSteps((ConcatenatedOperation) operation, false)));
        } else {
            operations.add(operation);
        }
        if ((decompose.source && !propagateVertical(sourceCRS, targetCRS, operations.listIterator(), true)) ||
            (decompose.target && !propagateVertical(sourceCRS, targetCRS, operations.listIterator(operations.size()), false)))
        {
            return null;
        }
        switch (operations.size()) {
            case 0:  return null;
            case 1:  return operations.get(0);
            default: return factory.createConcatenatedOperation(
                    derivedFrom(operation),
                    null, null,     // Take source and target CRS from the first and last steps.
                    operations.toArray(CoordinateOperation[]::new));
        }
    }

    /**
     * Appends a vertical axis in the source CRS of the first step {@code forward = true} or in
     * the target CRS of the last step {@code forward = false} of the given operations chain.
     *
     * @param  source3D    the potentially three-dimensional source CRS
     * @param  target3D    the potentially three-dimensional target CRS
     * @param  operations  the chain of operations in which to add a vertical axis.
     * @param  forward     {@code true} for adding the vertical axis at the beginning, or
     *                     {@code false} for adding the vertical axis at the end.
     * @return {@code true} on success.
     * @throws IllegalArgumentException if the operation method cannot have the desired number of dimensions.
     */
    private boolean propagateVertical(final CoordinateReferenceSystem source3D,
                                      final CoordinateReferenceSystem target3D,
                                      final ListIterator<CoordinateOperation> operations,
                                      final boolean forward)
            throws FactoryException
    {
        while (forward ? operations.hasNext() : operations.hasPrevious()) {
            final CoordinateOperation op = forward ? operations.next() : operations.previous();
            /*
             * We will accept to increase the number of dimensions only for operations between geographic CRS.
             * We do not increase the number of dimensions for operations between other kinds of CRS because
             * we would not know which value to give to the new dimension.
             */
            CoordinateReferenceSystem sourceCRS, targetCRS;
            if ( !((sourceCRS = op.getSourceCRS()) instanceof GeodeticCRS
                && (targetCRS = op.getTargetCRS()) instanceof GeodeticCRS
                && sourceCRS.getCoordinateSystem() instanceof EllipsoidalCS
                && targetCRS.getCoordinateSystem() instanceof EllipsoidalCS))
            {
                break;
            }
            /*
             * We can process mostly linear operations, otherwise it is hard to know how to add a dimension.
             * Examples of linear operations are:
             *
             *   - Longitude rotation (EPSG:9601). Note that this is a transformation rather than a conversion.
             *   - Geographic3D to 2D conversion (EPSG:9659).
             *
             * However, there is a few special cases where we may be able to add a dimension in a non-linear operation.
             * We can attempt those special cases by just giving the same parameters to the math transform factory
             * together with the desired CRS. Examples of such special cases are:
             *
             *   - Geocentric translations (geog2D domain)
             *   - Coordinate Frame Rotation (geog2D domain)
             *   - Position Vector transformation (geog2D domain)
             */
            Matrix matrix = MathTransforms.getMatrix(op.getMathTransform());
            if (matrix == null) {
                if (op instanceof SingleOperation) {
                    if (forward) sourceCRS = toGeodetic3D(sourceCRS, source3D);
                    else         targetCRS = toGeodetic3D(targetCRS, target3D);
                    final MathTransform.Builder builder;
                    final MathTransform mt;
                    try {
                        final var parameters = ((SingleOperation) op).getParameterValues();
                        builder = createTransformBuilder(parameters, sourceCRS, targetCRS);
                        mt = builder.create();
                    } catch (InvalidGeodeticParameterException e) {
                        log(null, e);
                        break;
                    }
                    operations.set(recreate(op, sourceCRS, targetCRS, mt, builder.getMethod().orElse(null)));
                    return true;
                }
                break;
            }
            /*
             * We can process only one of the following cases:
             *
             *   - Replace a 2D → 2D operation by a 3D → 3D one (i.e. add a passthrough operation).
             *   - Usually remove (or otherwise edit) the operation that change the number of dimensions
             *     between the 2D and 3D cases.
             */
            final int numRow = matrix.getNumRow();
            final int numCol = matrix.getNumCol();
            final boolean is2D     = (numCol == 3 && numRow == 3);         // 2D → 2D operation.
            if (!(is2D || (forward ? (numCol == 3 && numRow == 4)          // 2D → 3D operation.
                                   : (numCol == 4 && numRow == 3))))       // 3D → 2D operation.
            {
                break;
            }
            matrix = Matrices.resizeAffine(matrix, 4, 4);
            if (matrix.isIdentity()) {
                operations.remove();
            } else {
                /*
                 * If we cannot just remove the operation, build a new one with the expected number of dimensions.
                 * The new operation will inherit the same properties except the identifiers, since it is no longer
                 * conform to the definition provided by the authority.
                 */
                final MathTransform mt = factorySIS.getMathTransformFactory().createAffineTransform(matrix);
                operations.set(recreate(op, toGeodetic3D(sourceCRS, source3D), toGeodetic3D(targetCRS, target3D), mt, null));
            }
            /*
             * If we processed the operation that change the number of dimensions, we are done.
             */
            if (!is2D) {
                return true;
            }
        }
        return false;
    }

    /**
     * If the given CRS is two-dimensional, appends an ellipsoidal height to it.
     * It is caller's responsibility to ensure that the given CRS is geographic.
     *
     * @param  crs        the two-dimensional CRS to replace by a three-dimensional CRS.
     * @param  candidate  an existing three-dimensional instance that may be suitable, or {@code null}.
     */
    private CoordinateReferenceSystem toGeodetic3D(CoordinateReferenceSystem crs,
            final CoordinateReferenceSystem candidate) throws FactoryException
    {
        assert (crs instanceof GeodeticCRS) && (crs.getCoordinateSystem() instanceof EllipsoidalCS) : crs;
        if (crs.getCoordinateSystem().getDimension() != 2) {
            return crs;
        }
        /*
         * The check for same class is a cheap way to ensure that the two CRS implement the same GeoAPI interface.
         * This test is stricter than necessary, but the result is still not wrong even if we miss an opportunity
         * to return the existing instance.
         */
        if (crs.getClass() == candidate.getClass() && candidate.getCoordinateSystem().getDimension() == 3) {
            if (Utilities.equalsIgnoreMetadata(DatumOrEnsemble.of((SingleCRS) candidate),
                                               DatumOrEnsemble.of((SingleCRS) crs)))
            {
                return candidate;               // Keep the existing instance since it may contain useful metadata.
            }
        }
        final EllipsoidalHeightCombiner c = new EllipsoidalHeightCombiner(factorySIS.crsFactory, factorySIS.csFactory, factory);
        return toAuthorityDefinition(CoordinateReferenceSystem.class, c.createCompoundCRS(derivedFrom(crs), crs, CommonCRS.Vertical.ELLIPSOIDAL.crs()));
    }

    /**
     * Returns the properties of the given object, excluding the identifiers.
     * This is used for new objects derived from an object specified by the authority.
     * Since the new object is not strictly as defined by the authority, we cannot keep its identifier code.
     */
    private static Map<String,?> derivedFrom(final IdentifiedObject object) {
        return IdentifiedObjects.getProperties(object, CoordinateOperation.IDENTIFIERS_KEY);
    }

    /**
     * Returns the specified identifier in a map to be given to coordinate operation constructors.
     * If the coordinate operation is a transformation, then it is caller's responsibility to set
     * the {@value CoordinateOperation#COORDINATE_OPERATION_ACCURACY_KEY} property.
     *
     * <h4>Missing properties</h4>
     * In the datum shift case, an operation version is mandatory but unknown at this time.
     * However, we noticed that the EPSG database does not always define a version neither.
     * Consequently, the Apache SIS implementation relaxes the rule requiring an operation
     * version and we do not try to provide this information here for now.
     *
     * @param  name  the name to put in a map.
     * @return a modifiable map containing the given name. Callers can put other entries in this map.
     */
    static Map<String,Object> properties(final Identifier name) {
        final var properties = new HashMap<String,Object>(4);
        properties.put(CoordinateOperation.NAME_KEY, name);
        return properties;
    }

    /**
     * Creates a transform builder which will use the given <abbr>CRS</abbr> as contextual information.
     * The ellipsoids will be used for completing the axis-length parameters, and the coordinate systems will
     * be used for axis order and units of measurement. This method does not perform <abbr>CRS</abbr> changes
     * other than axis order and units.
     *
     * @param  parameters  the operation parameter value group.
     * @param  sourceCRS   the CRS from which to get the source coordinate system and ellipsoid.
     * @param  targetCRS   the CRS from which to get the target coordinate system and ellipsoid.
     * @return the parameterized transform.
     * @throws FactoryException if the transform cannot be created.
     */
    private MathTransform.Builder createTransformBuilder(
            final ParameterValueGroup parameters,
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS) throws FactoryException
    {
        final var builder = new ParameterizedTransformBuilder(factorySIS.getMathTransformFactory(), null);
        builder.setParameters(parameters, true);
        builder.setSourceAxes(sourceCRS);
        builder.setTargetAxes(targetCRS);
        return builder;
    }

    /**
     * Creates a coordinate operation from a math transform.
     * The method performs the following steps:
     *
     * <ul class="verbose">
     *   <li>If the given {@code transform} is already an instance of {@code CoordinateOperation} and if its properties
     *       (operation method, source and target CRS) are compatible with the arguments values, then that operation is
     *       returned as-is.
     *
     *       <div class="note"><b>Note:</b> we do not have many objects that are both a {@code CoordinateOperation}
     *       and a {@code MathTransform}, but that combination is not forbidden. Since such practice is sometimes
     *       convenient for the implementer, Apache SIS allows that.</div></li>
     *
     *   <li>If the given {@code type} is null, then this method infers the type from whether the given properties
     *       specify and accuracy or not. If those properties were created by the {@link #properties(Identifier)}
     *       method, then the operation will be a {@link Transformation} instance instead of {@link Conversion} if
     *       the {@code name} identifier {@linkplain #isDatumChange(Identifier) is for a datum change}.</li>
     *
     *   <li>If the given {@code method} is {@code null}, then infer an operation method by inspecting the given transform.
     *       The transform needs to implement the {@link org.apache.sis.parameter.Parameterized} interface in order to allow
     *       operation method discovery.</li>
     *
     *   <li>Delegate to {@link DefaultCoordinateOperationFactory#createSingleOperation
     *       DefaultCoordinateOperationFactory.createSingleOperation(…)}.</li>
     * </ul>
     *
     * @param  properties  the properties to give to the operation, as a modifiable map.
     * @param  sourceCRS   the source coordinate reference system.
     * @param  targetCRS   the destination coordinate reference system.
     * @param  transform   the math transform.
     * @param  method      the operation method, or {@code null} if unknown.
     * @param  parameters  the operations parameters, or {@code null} for automatic detection (not always reliable).
     * @param  type        {@code Conversion.class}, {@code Transformation.class}, or {@code null} if unknown.
     * @return a coordinate operation using the specified math transform.
     * @throws FactoryException if the operation cannot be created.
     */
    final CoordinateOperation createFromMathTransform(final Map<String,Object>        properties,
                                                      final CoordinateReferenceSystem sourceCRS,
                                                      final CoordinateReferenceSystem targetCRS,
                                                      final MathTransform             transform,
                                                            OperationMethod           method,
                                                      final ParameterValueGroup       parameters,
                                                      Class<? extends CoordinateOperation> type)
            throws FactoryException
    {
        /*
         * If the specified math transform is already a coordinate operation, and if its properties (method,
         * source and target CRS) are compatible with the specified ones, then that operation is returned as-is.
         */
        if (transform instanceof CoordinateOperation) {
            final var operation = (CoordinateOperation) transform;
            if (Objects.equals(operation.getSourceCRS(),     sourceCRS) &&
                Objects.equals(operation.getTargetCRS(),     targetCRS) &&
                Objects.equals(operation.getMathTransform(), transform) &&
                (method == null || !(operation instanceof SingleOperation) ||
                    Objects.equals(((SingleOperation) operation).getMethod(), method)))
            {
                return operation;
            }
        }
        /*
         * The operation method is mandatory. If the user did not provided one, we need to infer it ourselves.
         * If we fail to infer an OperationMethod, let it to null - the exception will be thrown by the factory.
         */
        if (method == null) {
            final Matrix matrix = MathTransforms.getMatrix(transform);
            if (matrix != null) {
                method = Affine.provider(transform.getSourceDimensions(), transform.getTargetDimensions(), Matrices.isAffine(matrix));
            } else {
                final ParameterDescriptorGroup descriptor = AbstractCoordinateOperation.getParameterDescriptors(transform);
                if (descriptor != null) {
                    try {
                        method = CoordinateOperations.findMethod(factorySIS.getMathTransformFactory(), descriptor);
                    } catch (NoSuchIdentifierException e) {
                        recoverableException("createFromMathTransform", e);
                        method = factory.createOperationMethod(properties, descriptor);
                    }
                }
            }
        }
        if (parameters != null) {
            properties.put(CoordinateOperations.PARAMETERS_KEY, parameters);
        }
        if (type != null) {
            properties.put(CoordinateOperations.OPERATION_TYPE_KEY, type);
            if (Conversion.class.isAssignableFrom(type) && transform.isIdentity()) {
                properties.replace(IdentifiedObject.NAME_KEY, AXIS_CHANGES, IDENTITY);
            }
        }
        return factorySIS.createSingleOperation(properties, sourceCRS, targetCRS, null, method, transform);
    }

    /**
     * Returns the GeoAPI interface (not the implementation class) of the given operation.
     * This mostly for whether the operation is a {@link Conversion} or {@link Transformation}.
     * Legacy GeoAPI types such as {@code Projection} are intentionally omitted, in part because
     * the inverse of a {@code Projection} does not implement the same interface.
     *
     * @param  op  the coordinate operation for which to identify the type.
     * @return the operation type, or {@code null} if not one of the types of interest for this factory.
     *
     * @see AbstractCoordinateOperation#getInterface()
     */
    static Class<? extends CoordinateOperation> typeOf(final CoordinateOperation op) {
        if (op instanceof Transformation) return Transformation.class;
        if (op instanceof Conversion)     return Conversion.class;
        return null;
    }

    /**
     * Returns the localized resources for error messages.
     *
     * @return localized resources for errors.
     */
    final Resources resources() {
        return Resources.forLocale(locale);
    }

    /**
     * Returns a label for identifying the given object in error message.
     *
     * @param object the object of identify.
     * @return label identifying the given object.
     */
    final String label(final IdentifiedObject object) {
        return CRSPair.label(object, locale);
    }

    /**
     * Returns an error message for missing information about datum change.
     * This is used for the construction of {@link OperationNotFoundException}.
     *
     * @param  source  the source datum or datum ensemble.
     * @param  target  the target datum or datum ensemble.
     * @return a default error message.
     */
    final String datumChangeNotFound(final IdentifiedObject source, final IdentifiedObject target) {
        return resources().getString(Resources.Keys.DatumChangeNotFound_2,
                IdentifiedObjects.getDisplayName(source, locale),
                IdentifiedObjects.getDisplayName(target, locale));
    }

    /**
     * Logs an unexpected but ignorable exception. This method pretends that the logging
     * come from {@link CoordinateOperationFinder} since this is the public API which
     * use this {@code CoordinateOperationRegistry} class.
     *
     * @param record     the record to log, or {@code null} for creating from the exception.
     * @param exception  the exception which occurred, or {@code null} if a {@code record} is specified instead.
     */
    private static void log(LogRecord record, final Exception exception) {
        if (record == null) {
            record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        }
        /*
         * We usually do not log the stack trace because this method should be invoked only for exceptions
         * such as NoSuchAuthorityCodeException or MissingFactoryResourceException, for which the message
         * is descriptive enough. But we make a special case for NoninvertibleTransformException because
         * its cause may have deeper root.
         */
        if (exception instanceof NoninvertibleTransformException) {
            record.setThrown(exception);
        }
        Logging.completeAndLog(AbstractCoordinateOperation.LOGGER, CoordinateOperationFinder.class, "createOperations", record);
    }

    /**
     * Logs an ignorable exception. This method pretends that the logging come from
     * {@link CoordinateOperationFinder} since this is the public API which use this
     * {@code CoordinateOperationRegistry} class.
     *
     * @param  method     the method name where the error occurred.
     * @param  exception  the exception which occurred, or {@code null} if a {@code record} is specified instead.
     */
    static void recoverableException(final String method, final Exception exception) {
        Logging.recoverableException(AbstractCoordinateOperation.LOGGER,
                CoordinateOperationFinder.class, method, exception);
    }
}
