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
import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Optional;
import java.time.Duration;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Time;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.privy.EllipsoidalHeightCombiner;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.internal.AnnotatedMatrix;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.datum.PseudoDatum;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.provider.DatumShiftMethod;
import org.apache.sis.referencing.operation.provider.GeocentricAffine;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Finds a conversion or transformation path from a source CRS to a target CRS.
 * This class implements two strategies for searching the coordinate operation:
 *
 * <ol class="verbose">
 *   <li>When <code>{@linkplain #createOperation createOperation}(sourceCRS, targetCRS)</code> is invoked,
 *       this class first {@linkplain org.apache.sis.referencing.factory.IdentifiedObjectFinder tries to
 *       find the authority codes} for the given source and target CRS. If such codes are found, they are
 *       {@linkplain org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createFromCoordinateReferenceSystemCodes
 *       submitted to a registry of coordinate operations}. If an operation is found, it will be returned.
 *
 *       <div class="note"><b>Note:</b> the above is known as the <i>late-binding</i> approach.
 *       The late-binding approach allows the authority to define better suited operations than what
 *       we would get if we were transforming everything from and to a pivot system (e.g. WGS84).
 *       In addition, this approach provides useful information like the coordinate operation
 *       {@linkplain AbstractCoordinateOperation#getDomains() domain of validity} and
 *       {@linkplain AbstractCoordinateOperation#getCoordinateOperationAccuracy() accuracy}.</div>
 *   </li>
 *   <li>If the above authority factory does not know about the specified CRS, then this class tries to
 *       infer the coordinate operation by itself. The CRS type is examined and the work is dispatched
 *       to one or many of the {@code createOperationStep(…)} protected methods defined in this class.
 *       Those methods use properties associated to the CRS, including {@code BOUNDCRS} or {@code TOWGS84}
 *       elements found in <i>Well Known Text</i> (WKT).
 *
 *       <div class="note"><b>Note:</b> the use of elements like {@code TOWGS84} is known as the
 *       <i>early-binding</i> approach. The operation found by this approach may be sub-optimal.
 *       The early-binding approach is used only as a fallback when the late-binding approach gave no result.</div>
 *   </li>
 * </ol>
 *
 * <h2>Customization</h2>
 * Instances of this class are created by {@link DefaultCoordinateOperationFactory}.
 * The only public method is {@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)},
 * which dispatches its work to the {@code createOperationStep(…)} protected methods.
 * Developers can override those protected methods if they want to alter the way some operations are created.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Each instance of this class shall be used only once.</li>
 *   <li>This class is not thread-safe. A new instance shall be created for each coordinate operation to infer.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see DefaultCoordinateOperationFactory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateOperationContext)
 *
 * @since 0.7
 */
public class CoordinateOperationFinder extends CoordinateOperationRegistry {
    /**
     * Identifiers used as the basis for identifier of CRS used as an intermediate step.
     * The values can be of two kinds:
     *
     * <ul>
     *   <li>If the value is an instance of {@link Integer}, then this is the number
     *       of identifiers derived from the identifier associated to the key.</li>
     *   <li>Otherwise the key is itself an {@link Identifier} derived from another
     *       identifier, and the value is that identifier.</li>
     * </ul>
     *
     * @see #derivedFrom(IdentifiedObject)
     */
    private final Map<Identifier,Object> identifierOfStepCRS;

    /**
     * The pair of source and target CRS for which we already searched a coordinate operation.
     * This is used as a safety against infinite recursion.
     */
    private final Map<CRSPair,Boolean> previousSearches;

    /**
     * Whether this finder instance is allowed to use {@link DefaultCoordinateOperationFactory#cache}.
     */
    private final boolean useCache;

    /**
     * Creates a new instance for the given factory and context.
     *
     * @param  registry  the factory to use for creating operations as defined by authority, or {@code null} if none.
     * @param  factory   the factory to use for creating operations not found in the registry.
     * @param  context   the area of interest and desired accuracy, or {@code null} if none.
     * @throws FactoryException if an error occurred while initializing this {@code CoordinateOperationFinder}.
     *
     * @see DefaultCoordinateOperationFactory#createOperationFinder(CoordinateOperationAuthorityFactory, CoordinateOperationContext)
     */
    public CoordinateOperationFinder(final CoordinateOperationAuthorityFactory registry,
                                     final CoordinateOperationFactory          factory,
                                     final CoordinateOperationContext          context) throws FactoryException
    {
        super(registry, factory, context);
        identifierOfStepCRS = new HashMap<>(8);
        previousSearches    = new HashMap<>(8);
        useCache = (context == null) && (factory == factorySIS);
    }

    /**
     * Infers an operation for conversion or transformation between two coordinate reference systems.
     * If a non-null authority factory – the <i>registry</i> – has been specified at construction time,
     * then this method will first query that factory (<i>late-binding</i> approach – see class javadoc).
     * If no operation has been found in the registry or if no registry has been specified to the constructor,
     * this method inspects the given CRS and delegates the work to one or many {@code createOperationStep(…)}
     * methods (<i>early-binding</i> approach).
     *
     * <p>The default implementation invokes <code>{@linkplain #createOperations createOperations}(sourceCRS,
     * targetCRS)</code>, then returns the first operation in the returned list or throws an exception if the
     * list is empty.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws OperationNotFoundException, FactoryException
    {
        final boolean oldState = stopAtFirst;
        stopAtFirst = true;
        final List<CoordinateOperation> operations = createOperations(sourceCRS, targetCRS);
        stopAtFirst = oldState;
        if (!operations.isEmpty()) {
            return operations.get(0);
        }
        throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS));
    }

    /**
     * Infers operations for conversions or transformations between two coordinate reference systems.
     * If a non-null authority factory – the <i>registry</i> – has been specified at construction time,
     * then this method will first query that factory (<i>late-binding</i> approach – see class javadoc).
     * If no operation has been found in the registry or if no registry has been specified to the constructor,
     * this method inspects the given CRS and delegates the work to one or many {@code createOperationStep(…)}
     * methods (<i>early-binding</i> approach).
     *
     * <p>At first, this method is invoked with the {@code sourceCRS} and {@code targetCRS} arguments given to the
     * {@link DefaultCoordinateOperationFactory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem,
     * CoordinateOperationContext) CoordinateOperationFactory.createOperation(…)} method. But then, this method may
     * be invoked recursively by some {@code createOperationStep(…)} methods with different source or target CRS,
     * for example in order to process the {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS#getBaseCRS()
     * base geographic CRS} of a projected CRS.</p>
     *
     * <p>Coordinate operations are returned in preference order: best operations for the area of interest should be first.
     * The returned list is modifiable: callers can add, remove or set elements without impact on this
     * {@code CoordinateOperationFinder} instance.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return coordinate operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     *
     * @since 1.0
     */
    @Override
    public List<CoordinateOperation> createOperations(final CoordinateReferenceSystem sourceCRS,
                                                      final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        if (Utilities.equalsIgnoreMetadata(sourceCRS, targetCRS)) try {
            return asList(createFromAffineTransform(AXIS_CHANGES, sourceCRS, targetCRS, null,
                            CoordinateSystems.swapAndScaleAxes(sourceCRS.getCoordinateSystem(),
                                                               targetCRS.getCoordinateSystem())));
        } catch (IllegalArgumentException | IncommensurableException e) {
            final CRSPair key = new CRSPair(sourceCRS, targetCRS);
            throw new FactoryException(resources().getString(Resources.Keys.CanNotInstantiateGeodeticObject_1, key), e);
        }
        /*
         * If this method is invoked recursively, verify if the requested operation is already in the cache.
         * We do not perform this verification on the first invocation because it was already verified by
         * DefaultCoordinateOperationFactory.createOperation(…). We do not block if the operation is in
         * process of being computed in another thread because of the risk of deadlock. If the operation
         * is not in the cache, store the key in our internal map for preventing infinite recursion.
         */
        final CRSPair key = new CRSPair(sourceCRS, targetCRS);
        if (useCache && stopAtFirst && !previousSearches.isEmpty()) {
            final CoordinateOperation op = factorySIS.cache.peek(key);
            if (op != null) return asList(op);      // Must be a modifiable list as per this method contract.
        }
        if (previousSearches.put(key, Boolean.TRUE) != null) {
            throw new FactoryException(resources().getString(Resources.Keys.RecursiveCreateCallForCode_2, CoordinateOperation.class, key));
        }
        /*
         * If the user did not specify an area of interest, use the domain of validity of the CRS.
         */
        GeographicBoundingBox bbox = Extents.getGeographicBoundingBox(areaOfInterest);
        if (bbox == null) {
            bbox = Extents.intersection(CRS.getGeographicBoundingBox(sourceCRS),
                                        CRS.getGeographicBoundingBox(targetCRS));
            areaOfInterest = CoordinateOperationContext.setGeographicBoundingBox(areaOfInterest, bbox);
        }
        /*
         * Verify in the EPSG dataset if the operation is explicitly defined by an authority.
         */
        if (registry != null) {
            final List<CoordinateOperation> authoritatives = super.createOperations(sourceCRS, targetCRS);
            if (!authoritatives.isEmpty()) return authoritatives;
        }
        // ┌────────────────────────────────────────────────────────────────────────┐
        // │                       Derived  →  any Single CRS                       │
        // └────────────────────────────────────────────────────────────────────────┘
        if (sourceCRS instanceof DerivedCRS) {
            final var source = (DerivedCRS) sourceCRS;
            if (targetCRS instanceof DerivedCRS) {
                return createOperationStep(source, (DerivedCRS) targetCRS);
            }
            if (targetCRS instanceof SingleCRS) {
                return createOperationStep(source, (SingleCRS) targetCRS);
            }
        }
        // ┌────────────────────────────────────────────────────────────────────────┐
        // │                       any Single CRS  →  Derived                       │
        // └────────────────────────────────────────────────────────────────────────┘
        if (targetCRS instanceof DerivedCRS) {
            final var target = (DerivedCRS) targetCRS;
            if (sourceCRS instanceof SingleCRS) {
                return createOperationStep((SingleCRS) sourceCRS, target);
            }
        }
        // ┌────────────────────────────────────────────────────────────────────────┐
        // │            Geodetic  →  Geocetric, Geographic or Projected             │
        // └────────────────────────────────────────────────────────────────────────┘
        if (sourceCRS instanceof GeodeticCRS) {
            final GeodeticCRS source = (GeodeticCRS) sourceCRS;
            if (targetCRS instanceof GeodeticCRS) {
                return createOperationStep(source, (GeodeticCRS) targetCRS);
            }
            if (targetCRS instanceof VerticalCRS) {
                return createOperationStep(source, (VerticalCRS) targetCRS);
            }
        }
        // ┌────────────────────────────────────────────────────────────────────────┐
        // │                         Vertical  →  Vertical                          │
        // └────────────────────────────────────────────────────────────────────────┘
        if (sourceCRS instanceof VerticalCRS) {
            final VerticalCRS source = (VerticalCRS) sourceCRS;
            if (targetCRS instanceof VerticalCRS) {
                return createOperationStep(source, (VerticalCRS) targetCRS);
            }
        }
        // ┌────────────────────────────────────────────────────────────────────────┐
        // │                         Temporal  →  Temporal                          │
        // └────────────────────────────────────────────────────────────────────────┘
        if (sourceCRS instanceof TemporalCRS) {
            final TemporalCRS source = (TemporalCRS) sourceCRS;
            if (targetCRS instanceof TemporalCRS) {
                return createOperationStep(source, (TemporalCRS) targetCRS);
            }
        }
        // ┌────────────────────────────────────────────────────────────────────────┐
        // │                Any single CRS  ↔  CRS of the same type                 │
        // └────────────────────────────────────────────────────────────────────────┘
        if (sourceCRS instanceof SingleCRS && targetCRS instanceof SingleCRS) {
            final Optional<IdentifiedObject> datumOrEnsemble =
                    PseudoDatum.getDatumOrEnsemble((SingleCRS) sourceCRS,
                                                   (SingleCRS) targetCRS);
            if (datumOrEnsemble.isPresent()) try {
                /*
                 * Because the CRS type is determined by the datum type (sometimes completed by the CS type),
                 * having equivalent datum and compatible CS should be a sufficient criterion.
                 */
                return asList(createFromAffineTransform(AXIS_CHANGES, sourceCRS, targetCRS,
                                PseudoDatum.getEnsembleAccuracy(datumOrEnsemble.get()).orElse(null),
                                CoordinateSystems.swapAndScaleAxes(sourceCRS.getCoordinateSystem(),
                                                                   targetCRS.getCoordinateSystem())));
            } catch (IllegalArgumentException | IncommensurableException e) {
                throw new FactoryException(notFoundMessage(sourceCRS, targetCRS), e);
            }
        }
        // ┌────────────────────────────────────────────────────────────────────────┐
        // │                        Compound  ↔  various CRS                        │
        // └────────────────────────────────────────────────────────────────────────┘
        if (sourceCRS instanceof CompoundCRS || targetCRS instanceof CompoundCRS) {
            return createOperationStep(sourceCRS, CRS.getSingleComponents(sourceCRS),
                                       targetCRS, CRS.getSingleComponents(targetCRS));
        }
        throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS));
    }

    /**
     * Creates operations from an arbitrary single CRS to a derived coordinate reference system.
     * Conversions from {@code GeographicCRS} to {@code ProjectedCRS} are also handled by this method,
     * since projected CRS are a special kind of {@code DerivedCRS}.
     *
     * <p>The default implementation constructs the following operation chain:</p>
     * <blockquote><code>sourceCRS  →  {@linkplain DerivedCRS#getBaseCRS() baseCRS}  →  targetCRS</code></blockquote>
     *
     * where the conversion from {@code baseCRS} to {@code targetCRS} is obtained from
     * <code>targetCRS.{@linkplain DerivedCRS#getConversionFromBase() getConversionFromBase()}</code>.
     *
     * <p>This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods. Only one element
     * in the returned list should be selected (usually the first one).</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return coordinate operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     */
    protected List<CoordinateOperation> createOperationStep(final SingleCRS sourceCRS,
                                                            final DerivedCRS targetCRS)
            throws FactoryException
    {
        final List<CoordinateOperation> operations = createOperations(sourceCRS, targetCRS.getBaseCRS());
        final ListIterator<CoordinateOperation> it = operations.listIterator();
        if (it.hasNext()) {
            final CoordinateOperation step2 = targetCRS.getConversionFromBase();
            do {
                final CoordinateOperation step1 = it.next();
                it.set(concatenate(step1, step2));
            } while (it.hasNext());
        }
        return operations;
    }

    /**
     * Creates an operation from a derived CRS to an arbitrary single coordinate reference system.
     * Conversions from {@code ProjectedCRS} to {@code GeographicCRS} are also handled by this method,
     * since projected CRS are a special kind of {@code DerivedCRS}.
     *
     * <p>The default implementation constructs the following operation chain:</p>
     * <blockquote><code>sourceCRS  →  {@linkplain DerivedCRS#getBaseCRS() baseCRS}  →  targetCRS</code></blockquote>
     *
     * where the conversion from {@code sourceCRS} to {@code baseCRS} is obtained from the inverse of
     * <code>sourceCRS.{@linkplain DerivedCRS#getConversionFromBase() getConversionFromBase()}</code>.
     *
     * <p>This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods. Only one element
     * in the returned list should be selected (usually the first one).</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     */
    protected List<CoordinateOperation> createOperationStep(final DerivedCRS sourceCRS,
                                                            final SingleCRS targetCRS)
            throws FactoryException
    {
        final List<CoordinateOperation> operations = createOperations(sourceCRS.getBaseCRS(), targetCRS);
        final ListIterator<CoordinateOperation> it = operations.listIterator();
        if (it.hasNext()) {
            final CoordinateOperation step1;
            try {
                step1 = inverse(sourceCRS.getConversionFromBase());
            } catch (OperationNotFoundException exception) {
                throw exception;
            } catch (FactoryException | NoninvertibleTransformException exception) {
                throw new OperationNotFoundException(canNotInvert(sourceCRS), exception);
            }
            do {
                final CoordinateOperation step2 = it.next();
                it.set(concatenate(step1, step2));
            } while (it.hasNext());
        }
        return operations;
    }

    /**
     * Creates an operation between two derived coordinate reference systems.
     * The default implementation performs three steps:
     *
     * <ol>
     *   <li>Convert from {@code sourceCRS} to its base CRS.</li>
     *   <li>Convert the source base CRS to target base CRS.</li>
     *   <li>Convert from the target base CRS to the {@code targetCRS}.</li>
     * </ol>
     *
     * <p>This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods. Only one element
     * in the returned list should be selected (usually the first one).</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     */
    protected List<CoordinateOperation> createOperationStep(final DerivedCRS sourceCRS,
                                                            final DerivedCRS targetCRS)
            throws FactoryException
    {
        final List<CoordinateOperation> operations = createOperations(sourceCRS.getBaseCRS(), targetCRS.getBaseCRS());
        final ListIterator<CoordinateOperation> it = operations.listIterator();
        if (it.hasNext()) {
            final CoordinateOperation step3 = targetCRS.getConversionFromBase();
            final CoordinateOperation step1;
            try {
                step1 = inverse(sourceCRS.getConversionFromBase());
            } catch (OperationNotFoundException exception) {
                throw exception;
            } catch (FactoryException | NoninvertibleTransformException exception) {
                throw new OperationNotFoundException(canNotInvert(sourceCRS), exception);
            }
            do {
                final CoordinateOperation step2 = it.next();
                it.set(concatenate(step1, step2, step3));
            } while (it.hasNext());
        }
        return operations;
    }

    /**
     * Creates an operation between two geodetic (geographic or geocentric) coordinate reference systems.
     * The default implementation can:
     *
     * <ul>
     *   <li>adjust axis order and orientation, for example converting from (<i>North</i>, <i>West</i>)
     *       axes to (<i>East</i>, <i>North</i>) axes,</li>
     *   <li>apply units conversion if needed,</li>
     *   <li>perform longitude rotation if needed,</li>
     *   <li>perform datum shift if {@linkplain BursaWolfParameters Bursa-Wolf parameters} are available
     *       for the area of interest.</li>
     * </ul>
     *
     * This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods.
     * Only one element in the returned list should be selected (usually the first one).
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     */
    protected List<CoordinateOperation> createOperationStep(final GeodeticCRS sourceCRS,
                                                            final GeodeticCRS targetCRS)
            throws FactoryException
    {
        final CoordinateSystem sourceCS = sourceCRS.getCoordinateSystem();
        final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
        final GeodeticDatum sourceDatum = PseudoDatum.of(sourceCRS);
        final GeodeticDatum targetDatum = PseudoDatum.of(targetCRS);
        /*
         * Find the type of operation depending on whether there is a change of geodetic reference frame (datum).
         * The `DATUM_SHIFT` and `ELLIPSOID_CHANGE` identifiers mean that there is a datum change, and all other
         * identifiers mean that the coordinate operation is only a change of coordinate system type (Ellipsoidal
         * ↔ Cartesian ↔ Spherical), axis swapping and unit conversions.
         */
        final Matrix datumShift;
        final Identifier identifier;
        final MathTransform transform;
        ParameterValueGroup parameters;
        final Optional<OperationMethod> method;
        final Optional<GeodeticDatum> commonDatum = PseudoDatum.ofOperation(sourceCRS, targetCRS);
        if (commonDatum.isPresent()) {
            /*
             * Coordinate system change (including change in the number of dimensions) without datum shift.
             * May contain the addition of ellipsoidal height or spherical radius, which need an ellipsoid.
             */
            final boolean isGeographic = (sourceCS instanceof EllipsoidalCS);
            identifier = isGeographic != (targetCS instanceof EllipsoidalCS) ? GEOCENTRIC_CONVERSION : AXIS_CHANGES;
            final var builder = factorySIS.getMathTransformFactory().builder(Constants.COORDINATE_SYSTEM_CONVERSION);
            final var ellipsoid = (isGeographic ? sourceDatum : targetDatum).getEllipsoid();
            builder.setSourceAxes(sourceCS, ellipsoid);
            builder.setTargetAxes(targetCS, ellipsoid);
            transform  = builder.create();
            method     = builder.getMethod();
            parameters = builder.parameters();
            datumShift = null;
        } else {
            /*
             * Conceptually, all transformations below could be done by first converting from source coordinate
             * system to geocentric Cartesian coordinates (X,Y,Z), apply an affine transform represented by the
             * datum shift matrix, then convert from the (X′,Y′,Z′) coordinates to the target coordinate system.
             * However, there are exceptions to this path:
             *
             *   1) Conversion from ellipsoidal to spherical CS can skip the Cartesian step for performance.
             *   2) Transformation between ellipsoidal CS may use the Molodensky approximation as a shortcut.
             *   3) Even when really going through the XYZ coordinates, the name of that operation depends on
             *      whether the source and target CRS are geocentric or geographic, 2- or 3-dimensional,
             *      whether there is a translation, the rotation sign, etc.
             *
             * Possible operations are:
             *
             *    - Position Vector transformation (in geocentric, geographic-2D or geographic-3D domains)
             *    - Geocentric translation         (in geocentric, geographic-2D or geographic-3D domains)
             *    - [Abridged] Molodensky          (as an approximation of geocentric translation)
             *    - Identity                       (if the desired accuracy is so large than we can skip datum shift)
             */
            datumShift = (sourceDatum instanceof DefaultGeodeticDatum) ?
                    ((DefaultGeodeticDatum) sourceDatum).getPositionVectorTransformation(targetDatum, areaOfInterest) : null;
            identifier = (datumShift != null) ? DATUM_SHIFT : ELLIPSOID_CHANGE;
            var builder = new MathTransformContext(factorySIS.getMathTransformFactory(), sourceDatum, targetDatum);
            builder.setSourceAxes(sourceCS, sourceDatum.getEllipsoid());
            builder.setTargetAxes(targetCS, targetDatum.getEllipsoid());
            parameters = GeocentricAffine.createParameters(sourceCS, targetCS, datumShift,
                                            DatumShiftMethod.forAccuracy(desiredAccuracy));
            if (parameters != null) {
                builder.setParameters(parameters, false);
                transform = builder.create();
            } else {
                transform = builder.createAffineGeocentric(datumShift);
            }
            method     = builder.getMethod();
            parameters = builder.parametersForMetadata();
        }
        /*
         * Adjust the accuracy information if the datum shift has been computed by an indirect path.
         * The indirect path uses a third datum (typically WGS84) as an intermediate between the two
         * specified datum.
         */
        final Map<String, Object> properties = properties(identifier);
        PositionalAccuracy accuracy = commonDatum.flatMap(PseudoDatum::getEnsembleAccuracy).orElse(null);
        if (accuracy == null && datumShift instanceof AnnotatedMatrix) {
            accuracy = ((AnnotatedMatrix) datumShift).accuracy;
        }
        if (accuracy != null) {
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY, accuracy);
        }
        return asList(createFromMathTransform(properties, sourceCRS, targetCRS, transform, method.orElse(null), parameters, null));
    }

    /**
     * Creates an operation between a geodetic and a vertical coordinate reference systems.
     * The height returned by this method will usually be part of a
     * {@linkplain DefaultPassThroughOperation pass-through operation}.
     *
     * <p>This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods. Only one element
     * in the returned list should be selected (usually the first one).</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     */
    protected List<CoordinateOperation> createOperationStep(final GeodeticCRS sourceCRS,
                                                            final VerticalCRS targetCRS)
            throws FactoryException
    {
        /*
         * We will perform the conversion or transformation as a 3 steps process:
         *
         *     source CRS          →
         *     interpolation CRS   →
         *     ellipsoidal height  →
         *     target height
         */
        CoordinateOperation step1 = null;
        CoordinateOperation step2;
        CoordinateOperation step3 = null;
        /*
         * Convert the source CRS to the CRS needed for transforming the heights.
         * For now this step is fixed to a three-dimensional geographic CRS, but
         * a future version should use a plugin-mechanism, with the code below
         * as the last fallback.
         */
        CoordinateReferenceSystem interpolationCRS = sourceCRS;
        CoordinateSystem interpolationCS = interpolationCRS.getCoordinateSystem();
        if (!(interpolationCS instanceof EllipsoidalCS)) {
            final EllipsoidalCS cs = CommonCRS.WGS84.geographic3D().getCoordinateSystem();
            if (!Utilities.equalsIgnoreMetadata(interpolationCS, cs)) {
                final GeographicCRS stepCRS = factorySIS.crsFactory
                        .createGeographicCRS(derivedFrom(sourceCRS), sourceCRS.getDatum(), sourceCRS.getDatumEnsemble(), cs);
                step1 = createOperation(sourceCRS, toAuthorityDefinition(GeographicCRS.class, stepCRS));
                interpolationCRS = step1.getTargetCRS();
                interpolationCS  = interpolationCRS.getCoordinateSystem();
            }
        }
        /*
         * Transform from ellipsoidal height to the height requested by the caller.
         * This operation requires the horizontal components (φ,λ) of source CRS,
         * unless the user asked for ellipsoidal height (which strictly speaking
         * is not allowed by ISO 19111). Those horizontal components are given by
         * the interpolation CRS.
         *
         * TODO: store the interpolationCRS in some field for allowing other methods to use it.
         */
        final int i = AxisDirections.indexOfColinear(interpolationCS, AxisDirection.UP);
        if (i < 0) {
            throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS));
        }
        final CoordinateSystemAxis expectedAxis = interpolationCS.getAxis(i);
        final boolean isEllipsoidalHeight;      // Whether heightCRS is okay or need to be recreated.
        VerticalCRS heightCRS = targetCRS;      // First candidate, will be replaced if it doesn't fit.
        VerticalCS  heightCS  = heightCRS.getCoordinateSystem();
        if (Utilities.equalsIgnoreMetadata(heightCS.getAxis(0), expectedAxis)) {
            isEllipsoidalHeight = ReferencingUtilities.isEllipsoidalHeight(PseudoDatum.of(heightCRS));
        } else {
            heightCRS = CommonCRS.Vertical.ELLIPSOIDAL.crs();
            heightCS  = heightCRS.getCoordinateSystem();
            isEllipsoidalHeight = Utilities.equalsIgnoreMetadata(heightCS.getAxis(0), expectedAxis);
            if (!isEllipsoidalHeight) {
                heightCS = toAuthorityDefinition(VerticalCS.class, factorySIS.csFactory
                        .createVerticalCS(derivedFrom(heightCS), expectedAxis));
            }
        }
        if (!isEllipsoidalHeight) {                     // `false` if we need to change datum, unit or axis direction.
            heightCRS = toAuthorityDefinition(VerticalCRS.class,
                    factorySIS.crsFactory.createVerticalCRS(derivedFrom(heightCRS), CommonCRS.Vertical.ELLIPSOIDAL.datum(), heightCS));
        }
        if (heightCRS != targetCRS) {
            step3     = createOperation(heightCRS, targetCRS);  // May need interpolationCRS for performing datum change.
            heightCRS = (VerticalCRS) step3.getSourceCRS();
            heightCS  = heightCRS.getCoordinateSystem();
        }
        /*
         * Conversion from three-dimensional geographic CRS to ellipsoidal height.
         * This part does nothing more than dropping the horizontal components,
         * like the "Geographic3D to 2D conversion" (EPSG:9659).
         * It is not the job of this block to perform unit conversions.
         * Unit conversions, if needed, are done by `step3` computed in above block.
         *
         * The "Geographic3DtoVertical.txt" file in the provider package is a reminder.
         * If this policy is changed, that file should be edited accordingly.
         */
        final int srcDim = interpolationCS.getDimension();                          // Should always be 3.
        final int tgtDim = heightCS.getDimension();                                 // Should always be 1.
        final Matrix matrix = Matrices.createZero(tgtDim + 1, srcDim + 1);
        matrix.setElement(0,      i,      1);                                       // Scale factor for height.
        matrix.setElement(tgtDim, srcDim, 1);                                       // Always 1 for affine transform.
        step2 = createFromAffineTransform(AXIS_CHANGES, interpolationCRS, heightCRS, null, matrix);
        return asList(concatenate(step1, step2, step3));
    }

    /**
     * Creates an operation between two vertical coordinate reference systems.
     * The default implementation checks if both CRS use the same datum, then
     * adjusts for axis direction and units.
     *
     * <p>This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods. Only one element
     * in the returned list should be selected (usually the first one).</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     *
     * @todo Needs to implement vertical datum shift.
     */
    protected List<CoordinateOperation> createOperationStep(final VerticalCRS sourceCRS,
                                                            final VerticalCRS targetCRS)
            throws FactoryException
    {
        final Optional<VerticalDatum> commonDatum = PseudoDatum.ofOperation(sourceCRS, targetCRS);
        if (commonDatum.isEmpty()) {
            throw new OperationNotFoundException(notFoundMessage(PseudoDatum.getDatumOrEnsemble(sourceCRS),
                                                                 PseudoDatum.getDatumOrEnsemble(targetCRS)));
        }
        final VerticalCS sourceCS = sourceCRS.getCoordinateSystem();
        final VerticalCS targetCS = targetCRS.getCoordinateSystem();
        final Matrix matrix;
        try {
            matrix = CoordinateSystems.swapAndScaleAxes(sourceCS, targetCS);
        } catch (IllegalArgumentException | IncommensurableException exception) {
            throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS), exception);
        }
        return asList(createFromAffineTransform(AXIS_CHANGES, sourceCRS, targetCRS,
                PseudoDatum.getEnsembleAccuracy(commonDatum.get()).orElse(null), matrix));
    }

    /**
     * Creates an operation between two temporal coordinate reference systems.
     * The default implementation checks if both CRS use the same datum, then
     * adjusts for axis direction, units and epoch.
     *
     * <p>This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods. Only one element
     * in the returned list should be selected (usually the first one).</p>
     *
     * @todo The current version performs only unit conversion and change of datum epoch,
     *       assuming that everything else is equivalent. This is probably not sufficient,
     *       as the relationship between two temporal datum may be non-linear.
     *       We should invoke {@link PseudoDatum#ofOperation(TemporalCRS, TemporalCRS)}
     *       for checking if the datum are equivalent, but doing so require that we remove
     *       the origin attribute from {@link TemporalDatum}.
     *       This change has been proposed to <abbr>OGC</abbr>.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     */
    protected List<CoordinateOperation> createOperationStep(final TemporalCRS sourceCRS,
                                                            final TemporalCRS targetCRS)
            throws FactoryException
    {
        final TemporalDatum sourceDatum = PseudoDatum.of(sourceCRS);
        final TemporalDatum targetDatum = PseudoDatum.of(targetCRS);
        final TimeCS sourceCS = sourceCRS.getCoordinateSystem();
        final TimeCS targetCS = targetCRS.getCoordinateSystem();
        /*
         * Compute the epoch shift. The epoch is the "time zero" in a particular coordinate reference system.
         * For example, the epoch of Java temporal objects (e.g. `Instant`) is january 1, 1970 at 00:00 UTC.
         * We compute how much to add to a time in `sourceCRS` in order to get a time in `targetCRS`.
         * This "epoch shift" is in units of `targetCRS`.
         */
        final Unit<Time> targetUnit = targetCS.getAxis(0).getUnit().asType(Time.class);
        DoubleDouble epochShift = DoubleDouble.of(Duration.between(targetDatum.getOrigin(), sourceDatum.getOrigin()));
        epochShift = DoubleDouble.of(Units.NANOSECOND.getConverterTo(targetUnit).convert(epochShift), true);
        /*
         * Check axis directions. The method `swapAndScaleAxes` should returns a matrix of size 2×2.
         * The element at index (0,0) may be +1 if source and target axes are in the same direction,
         * or -1 if there are in opposite direction ("PAST" vs "FUTURE"). The value may be something
         * else than ±1 if a unit conversion is applied too.  For example, the value is 60 if time in
         * sourceCRS is in hours while time in targetCRS is in minutes.
         *
         * The "epoch shift" previously computed is a translation. Consequently, it is added to element (0,1).
         */
        final MatrixSIS matrix;
        try {
            matrix = MatrixSIS.castOrCopy(CoordinateSystems.swapAndScaleAxes(sourceCS, targetCS));
        } catch (IllegalArgumentException | IncommensurableException exception) {
            throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS), exception);
        }
        final int translationColumn = matrix.getNumCol() - 1;           // Paranoiac check: should always be 1.
        final var translation = DoubleDouble.of(matrix.getNumber(0, translationColumn), true);
        matrix.setNumber(0, translationColumn, translation.add(epochShift));
        return asList(createFromAffineTransform(AXIS_CHANGES, sourceCRS, targetCRS, null, matrix));
    }

    /**
     * Creates an operation between at least one {@code CompoundCRS} (usually the source) and an arbitrary CRS.
     * The default implementation tries to invoke the {@link #createOperation createOperation(…)} method with
     * various combinations of source and target components. A preference is given for components of the same
     * type (e.g. source {@link GeodeticCRS} with target {@code GeodeticCRS}, <i>etc.</i>).
     *
     * <p>This method returns only <em>one</em> step for a chain of concatenated operations (to be built by the caller).
     * But a list is returned because the same step may be implemented by different operation methods. Only one element
     * in the returned list should be selected (usually the first one).</p>
     *
     * @param  sourceCRS         input coordinate reference system.
     * @param  sourceComponents  components of the source CRS.
     * @param  targetCRS         output coordinate reference system.
     * @param  targetComponents  components of the target CRS.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be constructed.
     */
    protected List<CoordinateOperation> createOperationStep(
            final CoordinateReferenceSystem sourceCRS, final List<? extends SingleCRS> sourceComponents,
            final CoordinateReferenceSystem targetCRS, final List<? extends SingleCRS> targetComponents)
            throws FactoryException
    {
        /*
         * Operations found are stored in the `infos` array, but are not yet wrapped in PassThroughOperations.
         * We need to know first if some coordinate values need reordering for matching the target CRS order.
         * We also need to know if any source coordinates should be dropped.
         */
        final SubOperationInfo[] infos;
        try {
            infos = SubOperationInfo.createSteps(this, sourceComponents, targetComponents);
        } catch (TransformException e) {
            throw new FactoryException(notFoundMessage(sourceCRS, targetCRS), e);
        }
        if (infos == null) {
            throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS));
        }
        /*
         * At this point, a coordinate operation has been found for all components of the target CRS.
         * However, the CoordinateOperation.getSourceCRS() values are not necessarily in the same order
         * than in the `sourceComponents` list given to this method, and some dimensions may be dropped.
         * The matrix computed by sourceToSelected(…) gives us the rearrangement needed for the coordinate
         * operations that we just found.
         */
        final CoordinateReferenceSystem[] stepComponents = SubOperationInfo.getSourceCRS(infos);
        final Matrix select = SubOperationInfo.sourceToSelected(sourceCRS.getCoordinateSystem().getDimension(), infos);
        /*
         * First, we need a CRS matching the above-cited rearrangement. That CRS will be named `stepSourceCRS`
         * and its components will be named `stepComponents`. Then we will execute a loop in which each component
         * is progressively (one by one) updated from a source component to a target component. A new step CRS is
         * recreated each time, since it will be needed for each PassThroughOperation.
         */
        CoordinateReferenceSystem stepSourceCRS;
        CoordinateOperation operation;
        if (select.isIdentity()) {
            stepSourceCRS = sourceCRS;                // No rearrangement - we can use source CRS as-is.
            operation = null;
        } else {
            if (stepComponents.length == 1) {
                stepSourceCRS = stepComponents[0];    // Slight optimization of the next block (in the `else` case).
            } else {
                CompoundCRS crs = factorySIS.crsFactory.createCompoundCRS(derivedFrom(sourceCRS), stepComponents);
                stepSourceCRS = toAuthorityDefinition(CoordinateReferenceSystem.class, crs);
            }
            operation = createFromAffineTransform(AXIS_CHANGES, sourceCRS, stepSourceCRS, null, select);
        }
        /*
         * For each sub-operation, create a PassThroughOperation for the (stepSourceCRS → stepTargetCRS) operation.
         * Each source CRS inside this loop will be for dimensions at indices [startAtDimension … endAtDimension-1].
         * Note that those indices are not necessarily the same as the indices in the fields of the same name in
         * SubOperationInfo, because those indices are not relative to the same CompoundCRS.
         */
        int endAtDimension = 0;
        int remainingSourceDimensions = select.getNumRow() - 1;
        final int indexOfFinal = SubOperationInfo.indexOfFinal(infos);
        for (int i=0; i<stepComponents.length; i++) {
            final SubOperationInfo          info   = infos[i];
            final CoordinateReferenceSystem source = stepComponents[i];
            final CoordinateReferenceSystem target = targetComponents.get(info.targetComponentIndex);
            /*
             * In order to compute `stepTargetCRS`, replace in-place a single element in `stepComponents`.
             * For each step except the last one, `stepTargetCRS` is a mix of target CRS and source CRS.
             * Only after the loop finished, `stepTargetCRS` will become the complete target definition.
             */
            final CoordinateReferenceSystem stepTargetCRS;
            stepComponents[info.targetComponentIndex] = target;
            if (i >= indexOfFinal) {
                stepTargetCRS = targetCRS;              // If all remaining transforms are identity, we reached the final CRS.
            } else if (info.isIdentity()) {
                stepTargetCRS = stepSourceCRS;          // In any identity transform, the source and target CRS are equal.
            } else if (stepComponents.length == 1) {
                stepTargetCRS = target;                 // Slight optimization of the next block.
            } else {
                stepTargetCRS = createCompoundCRS(target, stepComponents);
            }
            int delta = source.getCoordinateSystem().getDimension();
            final int startAtDimension = endAtDimension;
            endAtDimension += delta;
            final int numTrailingCoordinates = remainingSourceDimensions - endAtDimension;
            CoordinateOperation subOperation = info.operation;
            if ((startAtDimension | numTrailingCoordinates) != 0) {
                subOperation = new DefaultPassThroughOperation(IdentifiedObjects.getProperties(subOperation),
                        stepSourceCRS, stepTargetCRS, subOperation, startAtDimension, numTrailingCoordinates);
            }
            /*
             * Concatenate the operation with the ones we have found so far, and use the current `stepTargetCRS`
             * as the source CRS for the next operation step. We also need to adjust the dimension indices,
             * since the previous operations may have removed some dimensions. Note that the delta may also
             * be negative in a few occasions.
             */
            operation = concatenate(operation, subOperation);
            stepSourceCRS = stepTargetCRS;
            delta -= target.getCoordinateSystem().getDimension();
            endAtDimension -= delta;
            remainingSourceDimensions -= delta;
        }
        /*
         * If there is some target dimensions that are set to constant values instead of computed from the
         * source dimensions, add those constants as the last step. It never occur except is some particular
         * contexts like when computing a transform between two `GridGeometry` instances.
         */
        if (stepComponents.length < infos.length) {
            final Matrix m = SubOperationInfo.createConstantOperation(infos, stepComponents.length,
                    stepSourceCRS.getCoordinateSystem().getDimension(),
                        targetCRS.getCoordinateSystem().getDimension());
            operation = concatenate(operation, createFromAffineTransform(CONSTANTS, stepSourceCRS, targetCRS, null, m));
        }
        return asList(operation);
    }





    //  ╔═════════════════════════════════════════════════════════╗
    //  ║                                                         ║
    //  ║                M I S C E L L A N E O U S                ║
    //  ║                                                         ║
    //  ╚═════════════════════════════════════════════════════════╝

    /**
     * Creates a coordinate operation from a matrix, which usually describes an affine transform.
     * A default {@link OperationMethod} object is given to this transform. In the special case
     * where the {@code name} identifier is {@link #DATUM_SHIFT} or {@link #ELLIPSOID_CHANGE},
     * the operation will be a {@link Transformation} instance instead of {@link Conversion}.
     *
     * @param  name       the identifier for the operation to be created.
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @param  accuracy   the positional accuracy, or {@code null} if unspecified.
     * @param  matrix     the matrix which describe an affine transform operation.
     * @return the conversion or transformation.
     * @throws FactoryException if the operation cannot be created.
     */
    private CoordinateOperation createFromAffineTransform(final Identifier                name,
                                                          final CoordinateReferenceSystem sourceCRS,
                                                          final CoordinateReferenceSystem targetCRS,
                                                          final PositionalAccuracy        accuracy,
                                                          final Matrix                    matrix)
            throws FactoryException
    {
        final MathTransform transform  = factorySIS.getMathTransformFactory().createAffineTransform(matrix);
        final Map<String, Object> properties = properties(name);
        if (accuracy != null) {
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY, accuracy);
        }
        return createFromMathTransform(properties, sourceCRS, targetCRS, transform, null, null, null);
    }

    /**
     * Creates a compound CRS, but we special processing for (two-dimensional Geographic + ellipsoidal heights) tuples.
     * If any such tuple is found, a three-dimensional geographic CRS is created instead of the compound CRS.
     *
     * @param  template    the CRS from which to inherit properties.
     * @param  components  ordered array of {@code CoordinateReferenceSystem} objects.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     *
     * @see EllipsoidalHeightCombiner#createCompoundCRS(Map, CoordinateReferenceSystem...)
     */
    private CoordinateReferenceSystem createCompoundCRS(final CoordinateReferenceSystem template,
            final CoordinateReferenceSystem[] components) throws FactoryException
    {
        EllipsoidalHeightCombiner c = new EllipsoidalHeightCombiner(factorySIS.crsFactory, factorySIS.csFactory, factory);
        CoordinateReferenceSystem crs = c.createCompoundCRS(derivedFrom(template), components);
        return toAuthorityDefinition(CoordinateReferenceSystem.class, crs);
    }

    /**
     * Concatenates two operation steps.
     * The new concatenated operation gets an automatically generated name.
     *
     * <h4>Special case</h4>
     * If one of the given operation steps performs a change of axis order or units,
     * then that change will be merged with the other operation instead of creating an {@link ConcatenatedOperation}.
     *
     * @param  step1  the first  step, or {@code null} for the identity operation.
     * @param  step2  the second step, or {@code null} for the identity operation.
     * @return a concatenated operation, or {@code null} if all arguments were null.
     * @throws FactoryException if the operation cannot be constructed.
     */
    private CoordinateOperation concatenate(final CoordinateOperation step1,
                                            final CoordinateOperation step2)
            throws FactoryException
    {
        if (isIdentity(step1)) return step2;
        if (isIdentity(step2)) return step1;
        final MathTransform mt1 = step1.getMathTransform();
        final MathTransform mt2 = step2.getMathTransform();
        final CoordinateReferenceSystem sourceCRS = step1.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = step2.getTargetCRS();
        /*
         * If one of the transform performs nothing more than a change of axis order or units, do
         * not expose that conversion in a ConcatenatedTransform.  Instead, merge that conversion
         * with the "main" operation. The intent is to simplify the operation chain by hidding
         * trivial operations.
         */
        CoordinateOperation main = null;
        final boolean isAxisChange1 = canHide(step1.getName());
        final boolean isAxisChange2 = canHide(step2.getName());
        if (isAxisChange1 && isAxisChange2 && isAffine(step1) && isAffine(step2)) {
            main = step2;                                           // Arbitrarily take the last step.
            if (main.getName() == IDENTITY && step1.getName() != IDENTITY) {
                main = step1;
            }
        } else {
            if (isAxisChange1 && mt1.getSourceDimensions() == mt1.getTargetDimensions()) main = step2;
            if (isAxisChange2 && mt2.getSourceDimensions() == mt2.getTargetDimensions()) main = step1;
        }
        if (main instanceof SingleOperation) {
            final SingleOperation op = (SingleOperation) main;
            final MathTransform mt = factorySIS.getMathTransformFactory().createConcatenatedTransform(mt1, mt2);
            main = createFromMathTransform(new HashMap<>(IdentifiedObjects.getProperties(main)),
                   sourceCRS, targetCRS, mt, op.getMethod(), op.getParameterValues(),
                   (main instanceof Transformation) ? Transformation.class :
                   (main instanceof Conversion) ? Conversion.class : SingleOperation.class);
        } else {
            main = factory.createConcatenatedOperation(defaultName(sourceCRS, targetCRS), step1, step2);
        }
        /*
         * Sometimes we get a concatenated operation made of an operation followed by its inverse.
         * We can identify thoses case when the associated MathTransform is the identity transform.
         * In such case, simplify by replacing the ConcatenatedTransform by a SingleTransform.
         */
        if (main instanceof ConcatenatedOperation && main.getMathTransform().isIdentity()) {
            Class<? extends CoordinateOperation> type = null;
            for (final CoordinateOperation component : ((ConcatenatedOperation) main).getOperations()) {
                if (component instanceof Transformation) {
                    type = Transformation.class;
                    break;
                }
            }
            main = createFromMathTransform(new HashMap<>(IdentifiedObjects.getProperties(main)),
                    main.getSourceCRS(), main.getTargetCRS(), main.getMathTransform(), null, null, type);
        }
        return main;
    }

    /**
     * Concatenates three transformation steps. If the first and/or the last operation is an {@link #AXIS_CHANGES},
     * then it will be included as part of the second operation instead of creating a {@link ConcatenatedOperation}.
     * If a concatenated operation is created, it will get an automatically generated name.
     *
     * @param  step1  the first  step, or {@code null} for the identity operation.
     * @param  step2  the second step, or {@code null} for the identity operation.
     * @param  step3  the third  step, or {@code null} for the identity operation.
     * @return a concatenated operation, or {@code null} if all arguments were null.
     * @throws FactoryException if the operation cannot be constructed.
     */
    private CoordinateOperation concatenate(final CoordinateOperation step1,
                                            final CoordinateOperation step2,
                                            final CoordinateOperation step3)
            throws FactoryException
    {
        if (isIdentity(step1)) return concatenate(step2, step3);
        if (isIdentity(step2)) return concatenate(step1, step3);
        if (isIdentity(step3)) return concatenate(step1, step2);
        if (canHide(step1.getName())) return concatenate(concatenate(step1, step2), step3);
        if (canHide(step3.getName())) return concatenate(step1, concatenate(step2, step3));
        final Map<String,?> properties = defaultName(step1.getSourceCRS(), step3.getTargetCRS());
        return factory.createConcatenatedOperation(properties, step1, step2, step3);
    }

    /**
     * Returns {@code true} if the given operation is non-null and use the affine operation method.
     */
    private static boolean isAffine(final CoordinateOperation operation) {
        return IdentifiedObjects.isHeuristicMatchForName(CoordinateOperations.getMethod(operation), Constants.AFFINE);
    }

    /**
     * Returns {@code true} if the specified operation is an identity conversion.
     * This method always returns {@code false} for transformations even if their
     * associated math transform is an identity one, because such transformations
     * are usually datum shift and must be visible.
     */
    private static boolean isIdentity(final CoordinateOperation operation) {
        if (operation == null) {
            return true;
        }
        if ((operation instanceof Conversion) && operation.getMathTransform().isIdentity()) {
            return CoordinateOperations.wrapAroundChanges(operation).isEmpty();
        }
        return false;
    }

    /**
     * Returns {@code true} if a coordinate operation of the given name can be hidden
     * in the list of operations. Note that the {@code MathTransform} will still take
     * the operation in account however.
     */
    private static boolean canHide(final Identifier id) {
        return (id == AXIS_CHANGES) || (id == IDENTITY);
    }

    /**
     * Returns the given name in a singleton map.
     */
    private static Map<String,?> properties(final String name) {
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Returns a name for an object derived from the specified one.
     * This method builds a name of the form "{@literal <original identifier>} (step 1)"
     * where "(step 1)" may be replaced by "(step 2)", "(step 3)", <i>etc.</i> if this
     * method has already been invoked for the same identifier (directly or indirectly).
     */
    private Map<String,?> derivedFrom(final IdentifiedObject object) {
        Identifier oldID = object.getName();
        Object p = identifierOfStepCRS.get(oldID);
        if (p instanceof Identifier) {
            oldID = (Identifier) p;
            p = identifierOfStepCRS.get(oldID);
        }
        final int count = (p != null) ? (Integer) p + 1 : 1;
        final Identifier newID = new NamedIdentifier(Citations.SIS, oldID.getCode() + " (step " + count + ')');
        identifierOfStepCRS.put(newID, oldID);
        identifierOfStepCRS.put(oldID, count);

        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(IdentifiedObject.NAME_KEY, newID);
        properties.put(IdentifiedObject.REMARKS_KEY, Vocabulary.formatInternational(Vocabulary.Keys.DerivedFrom_1, label(object)));
        return properties;
    }

    /**
     * Returns a name for a transformation between two CRS.
     */
    private static Map<String,?> defaultName(CoordinateReferenceSystem source, CoordinateReferenceSystem target) {
        return properties(new CRSPair(source, target).toString());
    }

    /**
     * Returns the given operation as a list of one element. We cannot use {@link Collections#singletonList(Object)}
     * because the list needs to be modifiable, as required by {@link #createOperations(CoordinateReferenceSystem,
     * CoordinateReferenceSystem)} method contract.
     */
    private static List<CoordinateOperation> asList(final CoordinateOperation operation) {
        final List<CoordinateOperation> operations = new ArrayList<>(1);
        operations.add(operation);
        return operations;
    }

    /**
     * Returns an error message for "No path found from sourceCRS to targetCRS".
     * This is used for the construction of {@link OperationNotFoundException}.
     *
     * @param  source  the source CRS.
     * @param  target  the target CRS.
     * @return a default error message.
     */
    private String notFoundMessage(final IdentifiedObject source, final IdentifiedObject target) {
        return resources().getString(Resources.Keys.CoordinateOperationNotFound_2, label(source), label(target));
    }

    /**
     * Returns an error message for "Cannot invert operation XYZ.".
     * This is used for the construction of {@link OperationNotFoundException}.
     *
     * @param  crs  the CRS having a conversion that cannot be inverted.
     * @return a default error message.
     */
    private String canNotInvert(final DerivedCRS crs) {
        return resources().getString(Resources.Keys.NonInvertibleOperation_1, label(crs.getConversionFromBase()));
    }
}
