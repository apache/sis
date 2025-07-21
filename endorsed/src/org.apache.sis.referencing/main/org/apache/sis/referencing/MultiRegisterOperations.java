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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.Objects;
import java.util.Optional;
import org.opengis.util.Factory;
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.factory.MultiAuthoritiesFactory;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.AbstractFactory;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.RegisterOperations;
import org.opengis.referencing.crs.SingleCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;


/**
 * Finds <abbr>CRS</abbr>s or coordinate operations in one or many geodetic registries.
 * Each {@code MultiRegisterOperations} instance can narrow the search to a single registry,
 * a specific version of that registry, or to a domain of validity.
 * Each instance is immutable and thread-safe.
 *
 * <p>This class delegates its work to {@linkplain CRS#forCode(String) static methods} or to
 * {@link MultiAuthoritiesFactory}. It does not provide new services compared to the above,
 * but provides a more high-level <abbr>API</abbr> with the most important registry-based
 * services in a single place. {@link RegisterOperations} can also be used as en entry point,
 * with accesses to the low-level <abbr>API</abbr> granted by {@link #getFactory(Class)}.</p>
 *
 * <h2>User-defined geodetic registries</h2>
 * User-defined authorities can be added to the SIS environment by creating {@link CRSAuthorityFactory}
 * implementations with a public no-argument constructor or a public static {@code provider()} method,
 * and declaring the name of those classes in the {@code module-info.java} file as a provider of the
 * {@code org.opengis.referencing.crs.CRSAuthorityFactory} service.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class MultiRegisterOperations extends AbstractFactory implements RegisterOperations {
    /**
     * Types of factories supported by this implementation.
     * A value of {@code true} means that the factory is an authority factory.
     * A value of {@code false} means that the factory is an object factory.
     *
     * @see #getFactory(Class)
     */
    private static final Map<Class<?>, Boolean> FACTORY_TYPES = Map.of(
            CoordinateOperationAuthorityFactory.class, Boolean.TRUE,
            DatumAuthorityFactory.class, Boolean.TRUE,
            CRSAuthorityFactory.class,   Boolean.TRUE,
            CSAuthorityFactory.class,    Boolean.TRUE,
            DatumFactory.class,          Boolean.FALSE,
            CRSFactory.class,            Boolean.FALSE,
            CSFactory.class,             Boolean.FALSE);

    /**
     * The authority of the <abbr>CRS</abbr> and coordinate operations to search,
     * or {@code null} for all authorities. In the latter case, the authority must
     * be specified in the code, for example {@code "EPSG:4326"} instead of "4326".
     *
     * @see #withAuthority(String)
     */
    private final String authority;

    /**
     * Version of the registry to use, or {@code null} for the default version.
     * Can be non-null only if {@link #authority} is also non-null.
     * If null, the default version is usually the latest one.
     *
     * @see #withVersion(String)
     */
    private final String version;

    /**
     * The area of interest for coordinate operations, or {@code null} for the whole world.
     *
     * @see #withAreaOfInterest(GeographicBoundingBox)
     */
    private final GeographicBoundingBox areaOfInterest;

    /**
     * The authority factory to use for extracting <abbr>CRS</abbr> instances, or {@code null} if no
     * authority has been specified. In the latter case, {@link CRS} static methods should be used.
     * In Apache SIS implementation, this is also an {@link CoordinateOperationAuthorityFactory}.
     *
     * @see #findCoordinateReferenceSystem(String)
     * @see #findCoordinateOperation(String)
     */
    private final CRSAuthorityFactory crsFactory;

    /**
     * The singleton instance for all authorities in their default versions, with no <abbr>AOI</abbr>.
     *
     * @see #provider()
     */
    private static final MultiRegisterOperations DEFAULT = new MultiRegisterOperations();

    /**
     * Returns an instance which will search <abbr>CRS</abbr> definitions in all registries that are known to SIS.
     * Because this instance is not for a specific registry, the authority will need to be part of the {@code code}
     * argument given to {@code create(String)} methods. For example, {@code "EPSG:4326"} instead of {@code "4326"}.
     * The registry can be made implicit by a call to {@link #withAuthority(String)}.
     *
     * @return the default instance for all registries known to SIS.
     */
    public static MultiRegisterOperations provider() {
        return DEFAULT;
    }

    /**
     * Creates an instance which will search <abbr>CRS</abbr> definitions in all registries that are known to SIS.
     *
     * @see #provider()
     */
    private MultiRegisterOperations() {
        authority      = null;
        version        = null;
        crsFactory     = null;
        areaOfInterest = null;
    }

    /**
     * Creates an instance with the same register than the given instance, but a different <abbr>AOI</abbr>.
     *
     * @param source          the register from which to copy the authority and version.
     * @param areaOfInterest  the new area of interest (<abbr>AOI</abbr>), or {@code null} if none.
     *
     * @see #withAreaOfInterest(GeographicBoundingBox)
     */
    protected MultiRegisterOperations(final MultiRegisterOperations source, final GeographicBoundingBox areaOfInterest) {
        authority  = source.authority;
        version    = source.version;
        crsFactory = source.crsFactory;
        this.areaOfInterest = areaOfInterest;
    }

    /**
     * Creates an instance which will use the registry of the specified authority, optionally at a specified version.
     *
     * @param source     the register from which to copy the area of interest.
     * @param authority  identification of the registry to use (e.g., "EPSG").
     * @param version    the registry version to use, or {@code null} for the default version.
     * @throws NoSuchAuthorityFactoryException if the specified registry has not been found.
     * @throws FactoryException if an error occurred while initializing this factory.
     *
     * @see #withAuthority(String)
     * @see #withVersion(String)
     */
    protected MultiRegisterOperations(final MultiRegisterOperations source, final String authority, final String version)
            throws FactoryException
    {
        this.authority      = Objects.requireNonNull(authority);
        this.version        = version;
        this.areaOfInterest = source.areaOfInterest;
        crsFactory = AuthorityFactories.ALL.getAuthorityFactory(CRSAuthorityFactory.class, authority, version);
    }

    /**
     * Returns an instance for a geodetic registry of the specified authority, such as "EPSG".
     * If a {@linkplain #withVersion(String) version number was specified} previously, that version is cleared.
     * If an area of interest was specified, the same area of interest is reused.
     *
     * <h2>User-defined geodetic registries</h2>
     * A user-defined authority can be specified if the implementation is declared in a {@code module-info}
     * file as a {@link CRSAuthorityFactory} service. See class javadoc for more information.
     *
     * @param  newValue  the desired authority, or {@code null} for all of them.
     * @return register operations for the specified authority.
     * @throws NoSuchAuthorityFactoryException if the given authority is unknown to SIS.
     * @throws FactoryException if the factory cannot be created for another reason.
     *
     * @see CRS#getAuthorityFactory(String)
     */
    public MultiRegisterOperations withAuthority(final String newValue) throws FactoryException {
        if (version == null && Objects.equals(authority, newValue)) {
            return this;
        } else if (newValue == null) {
            return DEFAULT.withAreaOfInterest(areaOfInterest);
        } else {
            return new MultiRegisterOperations(this, newValue, null);
        }
    }

    /**
     * Returns an instance for the specified version of the geodetic registry.
     * A non-null authority must have been {@linkplain #withAuthority(String) specified} before to invoke this method.
     * If an area of interest was specified, the same area of interest is reused.
     *
     * @param  newValue  the desired version, or {@code null} for the default version.
     * @return register operations for the specified version of the geodetic registry.
     * @throws IllegalStateException if the version is non-null and no authority has been specified previously.
     * @throws NoSuchAuthorityFactoryException if the given version is unknown to SIS.
     * @throws FactoryException if the factory cannot be created for another reason.
     */
    public MultiRegisterOperations withVersion(final String newValue) throws FactoryException {
        if (Objects.equals(version, newValue)) {
            return this;
        } else if (newValue == null && authority == null) {
            return DEFAULT.withAreaOfInterest(areaOfInterest);
        } else if (authority != null) {
            return new MultiRegisterOperations(this, authority, newValue);
        } else {
            throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForProperty_1, "authority"));
        }
    }

    /**
     * Returns an instance for the specified area of interest (<abbr>AOI</abbr>).
     * The area of interest is used for filtering coordinate operations between
     * a {@linkplain #findCoordinateOperations between a pair of CRSs}.
     *
     * @param  newValue  the desired area of interest, or {@code null} for the world.
     * @return register operations for the specified area of interest.
     */
    public MultiRegisterOperations withAreaOfInterest(final GeographicBoundingBox newValue) {
        if (Objects.equals(areaOfInterest, newValue)) {
            return this;
        } else if (newValue == null && authority == null && version == null) {
            return DEFAULT;
        } else {
            return new MultiRegisterOperations(this, newValue);
        }
    }

    /**
     * Extracts <abbr>CRS</abbr> details from the registry. If this {@code RegisterOperations} has not
     * been restricted to a specific authority by a call to {@link #withAuthority(String)}, then the
     * given code must contain the authority (e.g., {@code "EPSG:4326"} instead of {@code "4326"}.
     * Otherwise, this method delegates to {@link CRS#forCode(jString)}.
     *
     * <p>By default, this method recognizes the {@code "EPSG"} and {@code "OGC"} authorities.
     * In the {@code "EPSG"} case, whether the full set of EPSG codes is supported or not depends
     * on whether a {@linkplain org.apache.sis.referencing.factory.sql connection to the database}
     * can be established. If no connection can be established, then this method uses a small embedded
     * EPSG factory containing at least the CRS defined in the {@link #forCode(String)} method javadoc.</p>
     *
     * @param  code  <abbr>CRS</abbr> identifier allocated by the authority.
     * @return the <abbr>CRS</abbr> for the given authority code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the search failed for some other reason.
     *
     * @see CRS#forCode(String)
     */
    @Override
    public CoordinateReferenceSystem findCoordinateReferenceSystem(final String code) throws FactoryException {
        if (crsFactory != null) {
            return crsFactory.createCoordinateReferenceSystem(code);
        }
        return CRS.forCode(code);
    }

    /**
     * Extracts coordinate operation details from the registry. If this {@code RegisterOperations}
     * has not been restricted to a specific authority by a call to {@link #withAuthority(String)},
     * then the given code must contain the authority.
     *
     * @param  code  operation identifier allocated by the authority.
     * @return the operation for the given authority code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the search failed for some other reason.
     */
    @Override
    public CoordinateOperation findCoordinateOperation(String code) throws FactoryException {
        if (crsFactory instanceof CoordinateOperationAuthorityFactory) {
            ((CoordinateOperationAuthorityFactory) crsFactory).createCoordinateOperation(code);
        }
        return AuthorityFactories.ALL.createCoordinateOperation(code);
    }

    /**
     * Finds or infers any coordinate operations for which the given <abbr>CRS</abbr>s are the source and target,
     * in that order. This method searches for operation paths defined in the registry.
     * If none are found, this method tries to infer a path itself.
     *
     * @param  source  the source <abbr>CRS</abbr>.
     * @param  target  the target <abbr>CRS</abbr>.
     * @return coordinate operations found or inferred between the given pair <abbr>CRS</abbr>s. May be an empty set.
     * @throws FactoryException if an error occurred while searching for coordinate operations.
     */
    @Override
    public Set<CoordinateOperation> findCoordinateOperations(CoordinateReferenceSystem source, CoordinateReferenceSystem target)
            throws FactoryException
    {
        final List<CoordinateOperation> operations = CRS.findOperations(source, target, areaOfInterest);
        return new AbstractSet<>() {    // Assuming that the list does not contain duplicated elements.
            @Override public Iterator<CoordinateOperation> iterator() {return operations.iterator();}
            @Override public boolean isEmpty() {return operations.isEmpty();}
            @Override public int size() {return operations.size();}
        };
    }

    /**
     * Determines whether two <abbr>CRS</abbr>s are members of one ensemble.
     * If this method returns {@code true}, then for low accuracy purposes coordinate sets referenced
     * to these <abbr>CRS</abbr>s may be merged without coordinate transformation.
     * The attribute {@link DatumEnsemble#getEnsembleAccuracy()} gives some indication
     * of the inaccuracy introduced through such merger.
     *
     * @param  source  the source <abbr>CRS</abbr>.
     * @param  target  the target <abbr>CRS</abbr>.
     * @return whether the two <abbr>CRS</abbr>s are members of one ensemble.
     * @throws FactoryException if an error occurred while searching for ensemble information in the registry.
     */
    @Override
    public boolean areMembersOfSameEnsemble(CoordinateReferenceSystem source, CoordinateReferenceSystem target)
            throws FactoryException
    {
        final List<SingleCRS> sources = CRS.getSingleComponents(source);
        final List<SingleCRS> targets = CRS.getSingleComponents(target);
        final int n = targets.size();
        if (sources.size() != n) {
            return false;
        }
        for (int i=0; i<n; i++) {
            if (DatumOrEnsemble.ofTarget(sources.get(i), targets.get(i)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a factory used for building components of <abbr>CRS</abbr> or coordinate operations.
     * The factories returned by this method provide accesses to the low-level services used by this
     * {@code RegisterOperations} instance for implementing its high-level services.
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  the desired type of factory.
     * @return factory of the specified type.
     * @throws NullPointerException if the specified type is null.
     * @throws IllegalArgumentException if the specified type is not one of the above-cited values.
     * @throws FactoryException if an error occurred while searching or preparing the requested factory.
     */
    @Override
    public <T extends Factory> Optional<T> getFactory(final Class<? extends T> type) throws FactoryException {
        final Factory factory;
        final Boolean b = FACTORY_TYPES.get(type);
        if (b != null) {
            if (b) {
                final MultiAuthoritiesFactory mf = AuthorityFactories.ALL;
                if (authority == null) {
                    factory = mf;
                } else try {
                    factory = mf.getAuthorityFactory(type.asSubclass(AuthorityFactory.class), authority, version);
                } catch (NoSuchAuthorityFactoryException e) {
                    Logging.recoverableException(AuthorityFactories.LOGGER, MultiRegisterOperations.class, "getFactory", e);
                    return Optional.empty();
                }
            } else {
                factory = GeodeticObjectFactory.provider();
            }
        } else if (type == CoordinateOperationFactory.class) {
            factory = DefaultCoordinateOperationFactory.provider();
        } else if (type == MathTransformFactory.class) {
            factory = DefaultMathTransformFactory.provider();
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
        }
        return Optional.of(type.cast(factory));
    }
}
