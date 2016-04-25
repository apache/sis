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
import java.util.Set;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.converter.ConversionException;

import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.*;

import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk8.Predicate;


/**
 * Base class of code that search for coordinate operation, either by looking in a registry maintained by an authority
 * or by trying to infer the coordinate operation by itself. For maintenance and testing purposes, we separate the task
 * in two classes for the two main strategies used for finding coordinate operations:
 *
 * <ul>
 *   <li>{@code CoordinateOperationRegistry} implements the <cite>late-binding</cite> approach
 *       (i.e. search coordinate operation paths specified by authorities like the ones listed
 *       in the EPSG dataset), which is the preferred approach.</li>
 *   <li>{@link CoordinateOperationFinder} adds an <cite>early-binding</cite> approach
 *       (i.e. find a coordinate operation path by inspecting the properties associated to the CRS).
 *       That approach is used only as a fallback when the late-binding approach gave no result.</li>
 * </ul>
 *
 * When <code>{@linkplain #createOperation createOperation}(sourceCRS, targetCRS)</code> is invoked,
 * this class fetches the authority codes for source and target CRS and submits them to the authority factory
 * through a call to its <code>{@linkplain GeodeticAuthorityFactory#createFromCoordinateReferenceSystemCodes
 * createFromCoordinateReferenceSystemCodes}(sourceCode, targetCode)</code> method.
 * If the authority factory does not know about the specified CRS,
 * then {@link CoordinateOperationFinder} will use its own fallback.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
class CoordinateOperationRegistry {
    /**
     * The identifier for an identity operation.
     */
    private static final Identifier IDENTITY = createIdentifier(Vocabulary.Keys.Identity);

    /**
     * The identifier for conversion using an affine transform for axis swapping and/or unit conversions.
     */
    static final Identifier AXIS_CHANGES = createIdentifier(Vocabulary.Keys.AxisChanges);

    /**
     * The identifier for a transformation which is a datum shift without {@link BursaWolfParameters}.
     * Only the changes in ellipsoid axis-length are taken in account.
     * Such ellipsoid shifts are approximative and may have 1 kilometre error.
     *
     * @see org.apache.sis.internal.referencing.PositionalAccuracyConstan#DATUM_SHIFT_OMITTED
     */
    static final Identifier ELLIPSOID_CHANGE = createIdentifier(Vocabulary.Keys.EllipsoidChange);

    /**
     * The identifier for a transformation which is a datum shift.
     *
     * @see org.apache.sis.internal.referencing.PositionalAccuracyConstant#DATUM_SHIFT_APPLIED
     */
    static final Identifier DATUM_SHIFT = createIdentifier(Vocabulary.Keys.DatumShift);

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
        return new NamedIdentifier(org.apache.sis.metadata.iso.citation.Citations.SIS, Vocabulary.formatInternational(key));
    }

    /**
     * The object to use for finding authority codes, or {@code null} if none.
     * An instance is fetched at construction time from the {@link #registry} if possible.
     */
    private final IdentifiedObjectFinder codeFinder;

    /**
     * The factory to use for creating operations as defined by authority, or {@code null} if none.
     * This is the factory used by the <cite>late-binding</cite> approach.
     */
    protected final CoordinateOperationAuthorityFactory registry;

    /**
     * The factory to use for creating coordinate operations not found in the registry.
     * This is the factory used by the <cite>early-binding</cite> approach.
     */
    protected final CoordinateOperationFactory factory;

    /**
     * Used only when we need a SIS-specific method.
     */
    final DefaultCoordinateOperationFactory factorySIS;

    /**
     * The spatio-temporal area of interest, or {@code null} if none.
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
     * A filter that can be used for applying additional restrictions on the coordinate operation,
     * or {@code null} if none.
     */
    private Predicate<CoordinateOperation> filter;

    /**
     * Creates a new instance for the given factory and context.
     *
     * @param  registry  the factory to use for creating operations as defined by authority.
     * @param  factory   the factory to use for creating operations not found in the registry.
     * @param  context   the area of interest and desired accuracy, or {@code null} if none.
     * @throws FactoryException if an error occurred while initializing this {@link CoordinateOperationRegistry}.
     */
    CoordinateOperationRegistry(final CoordinateOperationAuthorityFactory registry,
                                final CoordinateOperationFactory          factory,
                                final CoordinateOperationContext          context) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("factory", factory);
        this.registry = registry;
        this.factory  = factory;
        factorySIS    = (factory instanceof DefaultCoordinateOperationFactory)
                        ? (DefaultCoordinateOperationFactory) factory : CoordinateOperations.factory();
        IdentifiedObjectFinder codeFinder = null;
        if (registry != null) {
            if (registry instanceof GeodeticAuthorityFactory) {
                codeFinder = ((GeodeticAuthorityFactory) registry).newIdentifiedObjectFinder();
            } else try {
                codeFinder = IdentifiedObjects.newFinder(Citations.getIdentifier(registry.getAuthority(), false));
            } catch (NoSuchAuthorityFactoryException e) {
                Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                        CoordinateOperationRegistry.class, "<init>", e);
            }
            if (codeFinder != null) {
                codeFinder.setIgnoringAxes(true);
            }
        }
        this.codeFinder = codeFinder;
        if (context != null) {
            areaOfInterest  = context.getAreaOfInterest();
            desiredAccuracy = context.getDesiredAccuracy();
            filter          = context.getOperationFilter();
        }
    }

    /**
     * If the authority defines an object equal, ignoring metadata, to the given object, returns that authority object.
     * Otherwise returns the given object unchanged. We do not invoke this method for user-supplied CRS, but only for
     * CRS or other objects created by {@code CoordinateOperationRegistry} as intermediate step.
     */
    final <T extends IdentifiedObject> T toAuthorityDefinition(final Class<T> type, final T object) throws FactoryException {
        if (codeFinder != null) {
            codeFinder.setIgnoringAxes(false);
            final IdentifiedObject candidate = codeFinder.findSingleton(object);
            codeFinder.setIgnoringAxes(true);
            if (Utilities.equalsIgnoreMetadata(object, candidate)) {
                return type.cast(candidate);
            }
        }
        return object;
    }

    /**
     * Finds the authority code for the given coordinate reference system.
     * This method does not trust the code given by the user in its CRS - we verify it.
     * This method may return a code even if the axis order does not match;
     * it will be caller's responsibility to make necessary adjustments.
     */
    private String findCode(final CoordinateReferenceSystem crs) throws FactoryException {
        if (codeFinder != null) {
            final Identifier identifier = IdentifiedObjects.getIdentifier(codeFinder.findSingleton(crs), null);
            if (identifier != null) {
                return identifier.getCode();
            }
        }
        return null;
    }

    /**
     * Finds or infers an operation for conversion or transformation between two coordinate reference systems.
     * {@code CoordinateOperationRegistry} implements the <cite>late-binding</cite> approach (see definition
     * of terms in class javadoc) by extracting the authority codes from the supplied {@code sourceCRS} and
     * {@code targetCRS}, then by submitting those codes to the
     * <code>{@linkplain CoordinateOperationAuthorityFactory#createFromCoordinateReferenceSystemCodes
     * createFromCoordinateReferenceSystemCodes}(sourceCode, targetCode)</code> method.
     * If no operation is found for those codes, then this method returns {@code null}.
     * Note that it does not mean that no path exist;
     * it only means that it was not defined explicitely in the registry.
     *
     * <p>If the subclass implements the <cite>early-binding</cite> approach (which is the fallback if late-binding
     * gave no result), then this method should never return {@code null} since there is no other fallback.
     * Instead, this method may throw an {@link OperationNotFoundException}.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}, or {@code null}
     *         if no such operation is explicitly defined in the underlying database.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        CoordinateReferenceSystem source = sourceCRS;
        CoordinateReferenceSystem target = targetCRS;
        for (int combine=0; ;combine++) {
            /*
             * First, try directly the provided (sourceCRS, targetCRS) pair. If that pair does not work,
             * try to use different combinations of user-provided CRS and two-dimensional components of
             * those CRS. The code below assumes that the user-provided CRS are three-dimensional, but
             * actually it works for other kind of CRS too without testing twice the same combinations.
             */
            switch (combine) {
                case 0:  break;                                                         // 3D → 3D
                case 1:  target = CRS.getHorizontalComponent(targetCRS);                // 3D → 2D
                         if (target == targetCRS) continue;
                         break;
                case 2:  source = CRS.getHorizontalComponent(sourceCRS);                // 2D → 2D
                         if (source == sourceCRS) continue;
                         break;
                case 3:  if (source == sourceCRS || target == targetCRS) continue;
                         target = targetCRS;                                            // 2D → 3D
                         break;
                default: return null;
            }
            if (source != null && target != null) try {
                CoordinateOperation operation = search(source, target);
                if (operation != null) {
                    /*
                     * Found an operation. If we had to extract the horizontal part of some 3D CRS, then we
                     * need to modify the coordinate operation in order to match the new number of dimensions.
                     */
                    if (combine != 0) {
                        operation = propagateVertical(sourceCRS, source != sourceCRS,
                                                      targetCRS, target != targetCRS, operation);
                        if (operation == null) {
                            continue;
                        }
                        operation = complete(operation, sourceCRS, targetCRS);
                    }
                    return operation;
                }
            } catch (IllegalArgumentException e) {
                String message = Errors.format(Errors.Keys.CanNotInstantiate_1, new CRSPair(sourceCRS, targetCRS));
                String details = e.getLocalizedMessage();
                if (details != null) {
                    message = message + ' ' + details;
                }
                throw new FactoryException(message, e);
            } catch (ConversionException e) {
                throw new FactoryException(Errors.format(
                        Errors.Keys.CanNotInstantiate_1, new CRSPair(sourceCRS, targetCRS)), e);
            }
        }
    }

    /**
     * Returns an operation for conversion or transformation between two coordinate reference systems.
     * This method extracts the authority code from the supplied {@code sourceCRS} and {@code targetCRS},
     * and submit them to the {@link #registry}. If no operation is found for those codes, then this method
     * returns {@code null}.
     *
     * @param  sourceCRS  source coordinate reference system.
     * @param  targetCRS  target coordinate reference system.
     * @return A coordinate operation from {@code sourceCRS} to {@code targetCRS},
     *         or {@code null} if no such operation is explicitly defined in the underlying database.
     * @return A coordinate operation from {@code sourceCRS} to {@code targetCRS}, or {@code null}
     *         if no such operation is explicitly defined in the underlying database.
     * @throws IllegalArgumentException if the coordinate systems are not of the same type or axes do not match.
     * @throws ConversionException if the units are not compatible or a unit conversion is non-linear.
     * @throws FactoryException if an error occurred while creating the operation.
     */
    private CoordinateOperation search(final CoordinateReferenceSystem sourceCRS,
                                       final CoordinateReferenceSystem targetCRS)
            throws IllegalArgumentException, ConversionException, FactoryException
    {
        final String sourceID = findCode(sourceCRS);
        if (sourceID == null) {
            return null;
        }
        final String targetID = findCode(targetCRS);
        if (targetID == null) {
            return null;
        }
        if (sourceID.equals(targetID)) {
            /*
             * Above check is necessary because this method may be invoked in some situations where the code
             * are equal while the CRS are not. Such situation should be illegal, but unfortunately it still
             * happen because many softwares are not compliant with EPSG definition of axis order.   In such
             * cases we will need to compute a transform from sourceCRS to targetCRS ignoring the source and
             * target codes. The CoordinateOperationFinder class can do that, providing that we prevent this
             * CoordinateOperationRegistry to (legitimately) claims that the operation from sourceCode to
             * targetCode is the identity transform.
             */
            return null;
        }
        final boolean inverse;
        Set<CoordinateOperation> operations;
        try {
            operations = registry.createFromCoordinateReferenceSystemCodes(sourceID, targetID);
            inverse = Containers.isNullOrEmpty(operations);
            if (inverse) {
                /*
                 * No operation from 'source' to 'target' available. But maybe there is an inverse operation.
                 * This is typically the case when the user wants to convert from a projected to a geographic CRS.
                 * The EPSG database usually contains transformation paths for geographic to projected CRS only.
                 */
                operations = registry.createFromCoordinateReferenceSystemCodes(targetID, sourceID);
                if (Containers.isNullOrEmpty(operations)) {
                    return null;
                }
            }
        } catch (NoSuchAuthorityCodeException exception) {
            /*
             * sourceCode or targetCode is unknown to the underlying authority factory.
             * Ignores the exception and fallback on the generic algorithm provided by
             * CoordinateOperationFinder.
             */
            log(exception);
            return null;
        } catch (MissingFactoryResourceException exception) {
            log(exception);
            return null;
        }
        /*
         * We will loop over all coordinate operations and select the one having the largest intersection
         * with the area of interest. Note that if the user did not specified an area of interest himself,
         * then we need to get one from the CRS. This is necessary for preventing the transformation from
         * NAD27 to NAD83 in Idaho to select the transform for Alaska (since the later has a larger area).
         */
        double largestArea = 0;
        double finestAccuracy = Double.POSITIVE_INFINITY;
        CoordinateOperation bestChoice = null;
        boolean stopAtFirstDeprecated = false;
        for (final Iterator<CoordinateOperation> it=operations.iterator(); it.hasNext();) {
            CoordinateOperation candidate;
            try {
                try {
                    candidate = it.next();
                } catch (BackingStoreException exception) {
                    throw exception.unwrapOrRethrow(FactoryException.class);
                }
                if (inverse) try {
                    candidate = inverse(candidate);
                } catch (NoninvertibleTransformException exception) {
                    // It may be a normal failure - the operation is not required to be invertible.
                    Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                            CoordinateOperationRegistry.class, "createOperation", exception);
                    continue;
                }
            } catch (MissingFactoryResourceException e) {
                log(e);
                continue;
            }
            if (candidate != null) {
                /*
                 * If we found at least one non-deprecated operation, we will stop the search at
                 * the first deprecated one (assuming that deprecated operations are sorted last).
                 */
                final boolean isDeprecated = (candidate instanceof Deprecable) && ((Deprecable) candidate).isDeprecated();
                if (isDeprecated && stopAtFirstDeprecated) {
                    break;
                }
                final double area = Extents.area(Extents.intersection(
                        Extents.getGeographicBoundingBox(areaOfInterest),
                        Extents.getGeographicBoundingBox(candidate.getDomainOfValidity())));
                if (bestChoice == null || area >= largestArea) {
                    final double accuracy = CRS.getLinearAccuracy(candidate);
                    if (bestChoice == null || area != largestArea || accuracy < finestAccuracy) {
                        /*
                         * It is possible that the CRS given to this method were not quite right.  For example the user
                         * may have created his CRS from a WKT using a different axis order than the order specified by
                         * the authority and still (wrongly) call those CRS "EPSG:xxxx".  So we check if the source and
                         * target CRS for the operation we just created are equivalent to the CRS specified by the user.
                         *
                         * NOTE: FactoryException may be thrown if we failed to create a transform from the user-provided
                         * CRS to the authority-provided CRS. That transform should have been only an identity transform,
                         * or a simple affine transform if the user specified wrong CRS as explained in above paragraph.
                         * If we failed here, we are likely to fail for all other transforms. So we are better to let
                         * the FactoryException propagate.
                         */
                        candidate = complete(candidate, sourceCRS, targetCRS);
                        if (filter == null || filter.test(candidate)) {
                            bestChoice = candidate;
                            if (!Double.isNaN(area)) {
                                largestArea = area;
                            }
                            finestAccuracy = Double.isNaN(accuracy) ? Double.POSITIVE_INFINITY : accuracy;
                            stopAtFirstDeprecated = !isDeprecated;
                        }
                    }
                }
            }
        }
        return bestChoice;
    }

    /**
     * Creates the inverse of the given single operation.
     */
    final CoordinateOperation inverse(final SingleOperation op) throws NoninvertibleTransformException, FactoryException {
        final CoordinateReferenceSystem sourceCRS = op.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = op.getTargetCRS();
        final MathTransform transform = op.getMathTransform().inverse();
        Class<? extends CoordinateOperation> type = null;
        if (op instanceof Transformation)  type = Transformation.class;
        else if (op instanceof Conversion) type = Conversion.class;
        final Map<String,Object> properties = properties(INVERSE_OPERATION);
        InverseOperationMethod.putParameters(op, properties);
        return createFromMathTransform(properties, targetCRS, sourceCRS,
                transform, InverseOperationMethod.create(op.getMethod()), null, type);
    }

    /**
     * Creates the inverse of the given operation, which may be single or compound.
     *
     * @param  operation The operation to invert, or {@code null}.
     * @return The inverse of {@code operation}, or {@code null} if none.
     * @throws NoninvertibleTransformException if the operation is not invertible.
     * @throws FactoryException if the operation creation failed for an other reason.
     */
    private CoordinateOperation inverse(final CoordinateOperation operation)
            throws NoninvertibleTransformException, FactoryException
    {
        if (SubTypes.isSingleOperation(operation)) {
            return inverse((SingleOperation) operation);
        }
        if (operation instanceof ConcatenatedOperation) {
            final List<? extends CoordinateOperation> operations = ((ConcatenatedOperation) operation).getOperations();
            final CoordinateOperation[] inverted = new CoordinateOperation[operations.size()];
            for (int i=0; i<inverted.length;) {
                final CoordinateOperation op = inverse(operations.get(i));
                if (op == null) {
                    return null;
                }
                inverted[inverted.length - ++i] = op;
            }
            return factory.createConcatenatedOperation(properties(INVERSE_OPERATION), inverted);
        }
        return null;
    }

    /**
     * Completes (if necessary) the given coordinate operation for making sure that the source CRS
     * is the given one and the target CRS is the given one.  In principle, the given CRS shall be
     * equivalent to the operation source/target CRS. However discrepancies happen if the user CRS
     * have flipped axis order, or if we looked for 2D operation while the user provided 3D CRS.
     *
     * @param  operation  the coordinate operation to complete.
     * @param  sourceCRS  the source CRS requested by the user.
     * @param  targetCRS  the target CRS requested by the user.
     * @return a coordinate operation for the given source and target CRS.
     * @throws IllegalArgumentException if the coordinate systems are not of the same type or axes do not match.
     * @throws ConversionException if the units are not compatible or a unit conversion is non-linear.
     * @throws FactoryException if the operation can not be constructed.
     */
    private CoordinateOperation complete(final CoordinateOperation       operation,
                                         final CoordinateReferenceSystem sourceCRS,
                                         final CoordinateReferenceSystem targetCRS)
            throws IllegalArgumentException, ConversionException, FactoryException
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
     * @throws ConversionException if the units are not compatible or a unit conversion is non-linear.
     * @throws FactoryException if an error occurred while creating a math transform.
     */
    private static MathTransform swapAndScaleAxes(final CoordinateReferenceSystem sourceCRS,
                                                  final CoordinateReferenceSystem targetCRS,
                                                  final MathTransformFactory      mtFactory)
            throws IllegalArgumentException, ConversionException, FactoryException
    {
        assert ReferencingUtilities.getDimension(sourceCRS) != ReferencingUtilities.getDimension(targetCRS)
                || Utilities.deepEquals(sourceCRS, targetCRS, ComparisonMode.ALLOW_VARIANT);
        final Matrix m = CoordinateSystems.swapAndScaleAxes(sourceCRS.getCoordinateSystem(), targetCRS.getCoordinateSystem());
        return (m.isIdentity()) ? null : mtFactory.createAffineTransform(m);
    }

    /**
     * Appends or prepends the specified math transforms to the transform of the given operation.
     * The new coordinate operation (if any) will share the same metadata than the original operation,
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
     * @throws IllegalArgumentException if the operation method can not have the desired number of dimensions.
     * @throws FactoryException if the operation can not be constructed.
     */
    private CoordinateOperation transform(final CoordinateReferenceSystem sourceCRS,
                                          final MathTransform             prepend,
                                                CoordinateOperation       operation,
                                          final MathTransform             append,
                                          final CoordinateReferenceSystem targetCRS,
                                          final MathTransformFactory      mtFactory)
            throws IllegalArgumentException, FactoryException
    {
        if ((prepend == null || prepend.isIdentity()) && (append == null || append.isIdentity())) {
            return operation;
        }
        /*
         * In the particular case of concatenated operations, we can not prepend or append a math transform to
         * the operation as a whole (the math transform for a concatenated operation is computed automatically
         * as the concatenation of the transforms from every single operations, and we need to stay consistent
         * with that). Instead, prepend to the first single operation and append to the last single operation.
         */
        if (operation instanceof ConcatenatedOperation) {
            final List<? extends CoordinateOperation> c = ((ConcatenatedOperation) operation).getOperations();
            final CoordinateOperation[] op = c.toArray(new CoordinateOperation[c.size()]);
            switch (op.length) {
                case 0: break;                              // Illegal, but we are paranoiac.
                case 1: operation = op[0]; break;           // Useless ConcatenatedOperation.
                default: {
                    final int n = op.length - 1;
                    final CoordinateOperation first = op[0];
                    final CoordinateOperation last  = op[n];
                    op[0] = transform(sourceCRS, prepend, first, null, first.getTargetCRS(), mtFactory);
                    op[n] = transform(last.getSourceCRS(), null, last, append, targetCRS,    mtFactory);
                    return factory.createConcatenatedOperation(derivedFrom(operation), op);
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
     * Creates a new coordinate operation with the same method than the given operation, but different CRS.
     * The CRS may differ either in the number of dimensions (i.e. let the vertical coordinate pass through),
     * or in axis order (i.e. axis order in user CRS were not compliant with authority definition).
     *
     * @param  operation  the operation specified by the authority.
     * @param  sourceCRS  the source CRS specified by the user.
     * @param  targetCRS  the target CRS specified by the user
     * @param  transform  the math transform to use in replacement to the one in {@code operation}.
     * @param  method     the operation method, or {@code null} for attempting an automatic detection.
     * @return a new operation from the given source CRS to target CRS using the given transform.
     * @throws IllegalArgumentException if the operation method can not have the desired number of dimensions.
     * @throws FactoryException if an error occurred while creating the new operation.
     */
    private CoordinateOperation recreate(final CoordinateOperation       operation,
                                               CoordinateReferenceSystem sourceCRS,
                                               CoordinateReferenceSystem targetCRS,
                                         final MathTransform             transform,
                                               OperationMethod           method)
            throws IllegalArgumentException, FactoryException
    {
        /*
         * If the user-provided CRS are approximatively equal to the coordinate operation CRS, keep the later.
         * The reason is that coordinate operation CRS are built from the definitions provided by the authority,
         * while the user-provided CRS can be anything (e.g. parsed from a quite approximative WKT).
         */
        CoordinateReferenceSystem crs;
        if (Utilities.equalsApproximatively(sourceCRS, crs = operation.getSourceCRS())) sourceCRS = crs;
        if (Utilities.equalsApproximatively(targetCRS, crs = operation.getTargetCRS())) targetCRS = crs;
        final Map<String,Object> properties = new HashMap<String,Object>(derivedFrom(operation));
        /*
         * Determine whether the operation to create is a Conversion or a Transformation
         * (could also be a Conversion subtype like Projection, but this is less important).
         * We want the GeoAPI interface, not the implementation class.
         * The most reliable way is to ask to the 'AbstractOperation.getInterface()' method,
         * but this is SIS-specific. The fallback uses reflection.
         */
        final Class<? extends IdentifiedObject> type;
        if (operation instanceof AbstractIdentifiedObject) {
             type = ((AbstractIdentifiedObject) operation).getInterface();
        } else {
             type = Classes.getLeafInterfaces(operation.getClass(), CoordinateOperation.class)[0];
        }
        properties.put(ReferencingServices.OPERATION_TYPE_KEY, type);
        /*
         * Reuse the same operation method, but we may need to change its number of dimension.
         * The capability to resize an OperationMethod is specific to Apache SIS, so we must
         * be prepared to see the 'redimension' call fails. In such case, we will try to get
         * the SIS implementation of the operation method and try again.
         */
        if (SubTypes.isSingleOperation(operation)) {
            final SingleOperation single = (SingleOperation) operation;
            properties.put(ReferencingServices.PARAMETERS_KEY, single.getParameterValues());
            if (method == null) {
                final int sourceDimensions = transform.getSourceDimensions();
                final int targetDimensions = transform.getTargetDimensions();
                method = single.getMethod();
                try {
                    method = DefaultOperationMethod.redimension(method, sourceDimensions, targetDimensions);
                } catch (IllegalArgumentException ex) {
                    try {
                        method = factorySIS.getOperationMethod(method.getName().getCode());
                        method = DefaultOperationMethod.redimension(method, sourceDimensions, targetDimensions);
                    } catch (NoSuchIdentifierException se) {
                        // ex.addSuppressed(se) on the JDK7 branch.
                        throw ex;
                    } catch (IllegalArgumentException se) {
                        // ex.addSuppressed(se) on the JDK7 branch.
                        throw ex;
                    }
                }
            }
        }
        return factorySIS.createSingleOperation(properties, sourceCRS, targetCRS,
                AbstractCoordinateOperation.getInterpolationCRS(operation), method, transform);
    }

    /**
     * Returns a new coordinate operation with the ellipsoidal height added either in the source coordinates,
     * in the target coordinates or both. If there is an ellipsoidal transform, then this method updates the
     * transforms in order to use the ellipsoidal height (it has an impact on the transformed values).
     *
     * <p>This method requires that the EPSG factory insert explicit <cite>"Geographic3D to 2D conversion"</cite>
     * operations (EPSG:9659) in the operations chain, or an equivalent operation (recognized by its matrix shape).
     * This method tries to locate and remove EPSG:9659 or equivalent operation from the operation chain in order
     * to get three-dimensional domains.</p>
     *
     * <p>This method is not guaranteed to succeed in adding the ellipsoidal height. It works on a
     * <cite>best effort</cite> basis. In any cases, the {@link #complete} method should be invoked
     * after this one in order to ensure that the source and target CRS are the expected ones.</p>
     *
     * @param  sourceCRS the potentially three-dimensional source CRS
     * @param  source3D  {@code true} for adding ellipsoidal height in source coordinates.
     * @param  targetCRS the potentially three-dimensional target CRS
     * @param  target3D  {@code true} for adding ellipsoidal height in target coordinates.
     * @param  operation the original (typically two-dimensional) coordinate operation.
     * @return a coordinate operation with the source and/or target coordinates made 3D,
     *         or {@code null} if this method does not know how to create the operation.
     * @throws IllegalArgumentException if the operation method can not have the desired number of dimensions.
     * @throws FactoryException if an error occurred while creating the coordinate operation.
     */
    private CoordinateOperation propagateVertical(final CoordinateReferenceSystem sourceCRS, final boolean source3D,
                                                  final CoordinateReferenceSystem targetCRS, final boolean target3D,
                                                  final CoordinateOperation operation)
            throws IllegalArgumentException, FactoryException
    {
        final List<CoordinateOperation> operations = new ArrayList<CoordinateOperation>();
        if (operation instanceof ConcatenatedOperation) {
            operations.addAll(((ConcatenatedOperation) operation).getOperations());
        } else {
            operations.add(operation);
        }
        if ((source3D && !propagateVertical(sourceCRS, targetCRS, operations.listIterator(), true)) ||
            (target3D && !propagateVertical(sourceCRS, targetCRS, operations.listIterator(operations.size()), false)))
        {
            return null;
        }
        switch (operations.size()) {
            case 0:  return null;
            case 1:  return operations.get(0);
            default: return factory.createConcatenatedOperation(derivedFrom(operation),
                            operations.toArray(new CoordinateOperation[operations.size()]));

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
     * @throws IllegalArgumentException if the operation method can not have the desired number of dimensions.
     */
    private boolean propagateVertical(final CoordinateReferenceSystem source3D,
                                      final CoordinateReferenceSystem target3D,
                                      final ListIterator<CoordinateOperation> operations,
                                      final boolean forward)
            throws IllegalArgumentException, FactoryException
    {
        while (forward ? operations.hasNext() : operations.hasPrevious()) {
            final CoordinateOperation op = forward ? operations.next() : operations.previous();
            /*
             * We will accept to increase the number of dimensions only for operations between geographic CRS.
             * We do not increase the number of dimensions for operations between other kind of CRS because we
             * would not know which value to give to the new dimension.
             */
            CoordinateReferenceSystem sourceCRS, targetCRS;
            if (! ((sourceCRS = op.getSourceCRS()) instanceof GeodeticCRS
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
             * However there is a few special cases where we may be able to add a dimension in a non-linear operation.
             * We can attempt those special cases by just giving the same parameters to the math transform factory
             * together with the desired CRS. Examples of such special cases are:
             *
             *   - Geocentric translations (geog2D domain)
             *   - Coordinate Frame Rotation (geog2D domain)
             *   - Position Vector transformation (geog2D domain)
             */
            Matrix matrix = MathTransforms.getMatrix(op.getMathTransform());
            if (matrix == null) {
                if (SubTypes.isSingleOperation(op)) {
                    final MathTransformFactory mtFactory = factorySIS.getMathTransformFactory();
                    if (mtFactory instanceof DefaultMathTransformFactory) {
                        if (forward) sourceCRS = toGeodetic3D(sourceCRS, source3D);
                        else         targetCRS = toGeodetic3D(targetCRS, target3D);
                        final MathTransform mt;
                        try {
                            mt = ((DefaultMathTransformFactory) mtFactory).createParameterizedTransform(
                                    ((SingleOperation) op).getParameterValues(),
                                    ReferencingUtilities.createTransformContext(sourceCRS, targetCRS, null));
                        } catch (InvalidGeodeticParameterException e) {
                            log(e);
                            break;
                        }
                        operations.set(recreate(op, sourceCRS, targetCRS, mt, mtFactory.getLastMethodUsed()));
                        return true;
                    }
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
                 * If we can not just remove the operation, build a new one with the expected number of dimensions.
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
     * If the given CRS is two-dimensional, append an ellipsoidal height to it.
     * It is caller's responsibility to ensure that the given CRS is geographic.
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
         * This test is stricter than necessary, but the result should still not wrong if we miss an opportunity
         * to return the existing instance.
         */
        if (crs.getClass() == candidate.getClass() && candidate.getCoordinateSystem().getDimension() == 3) {
            if (Utilities.equalsIgnoreMetadata(((SingleCRS) crs).getDatum(), ((SingleCRS) candidate).getDatum())) {
                return candidate;               // Keep the existing instance since it may contain useful metadata.
            }
        }
        return toAuthorityDefinition(CoordinateReferenceSystem.class,
                ReferencingServices.getInstance().createCompoundCRS(
                        factorySIS.getCRSFactory(),
                        factorySIS.getCSFactory(),
                        derivedFrom(crs), crs, CommonCRS.Vertical.ELLIPSOIDAL.crs()));
    }

    /**
     * Returns the properties of the given object, excluding the identifiers.
     * This is used for new objects derived from an object specified by the authority.
     * Since the new object is not strictly as defined by the authority, we can not keep its identifier code.
     */
    private static Map<String,?> derivedFrom(final IdentifiedObject object) {
        return IdentifiedObjects.getProperties(object, CoordinateOperation.IDENTIFIERS_KEY);
    }

    /**
     * Returns the specified identifier in a map to be given to coordinate operation constructors.
     * In the special case where the {@code name} identifier is {@link #DATUM_SHIFT} or {@link #ELLIPSOID_CHANGE},
     * the map will contains extra informations like positional accuracy.
     *
     * <div class="note"><b>Note:</b>
     * in the datum shift case, an operation version is mandatory but unknown at this time.
     * However, we noticed that the EPSG database do not always defines a version neither.
     * Consequently, the Apache SIS implementation relaxes the rule requiring an operation
     * version and we do not try to provide this information here for now.</div>
     *
     * @param  name  The name to put in a map.
     * @return a modifiable map containing the given name. Callers can put other entries in this map.
     */
    static Map<String,Object> properties(final Identifier name) {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(CoordinateOperation.NAME_KEY, name);
        if ((name == DATUM_SHIFT) || (name == ELLIPSOID_CHANGE)) {
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY, new PositionalAccuracy[] {
                      (name == DATUM_SHIFT) ? PositionalAccuracyConstant.DATUM_SHIFT_APPLIED
                                            : PositionalAccuracyConstant.DATUM_SHIFT_OMITTED});
        }
        return properties;
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
     *       and a {@code MathTransform}, but that combination is not forbidden. Since such practice is sometime
     *       convenient for the implementor, Apache SIS allows that.</div></li>
     *
     *   <li>If the given {@code type} is null, then this method infers the type from whether the given properties
     *       specify and accuracy or not. If those properties were created by the {@link #properties(Identifier)}
     *       method, then the operation will be a {@link Transformation} instance instead of {@link Conversion} if
     *       the {@code name} identifier was {@link #DATUM_SHIFT} or {@link #ELLIPSOID_CHANGE}.</li>
     *
     *   <li>If the given {@code method} is {@code null}, then infer an operation method by inspecting the given transform.
     *       The transform needs to implement the {@link org.apache.sis.parameter.Parameterized} interface in order to allow
     *       operation method discovery.</li>
     *
     *   <li>Delegate to {@link DefaultCoordinateOperationFactory#createSingleOperation
     *       DefaultCoordinateOperationFactory.createSingleOperation(…)}.</li>
     * </ul>
     *
     * @param  properties The properties to give to the operation, as a modifiable map.
     * @param  sourceCRS  The source coordinate reference system.
     * @param  targetCRS  The destination coordinate reference system.
     * @param  transform  The math transform.
     * @param  method     The operation method, or {@code null} if unknown.
     * @param  parameters The operations parameters, or {@code null} for automatic detection (not always reliable).
     * @param  type       {@code Conversion.class}, {@code Transformation.class}, or {@code null} if unknown.
     * @return A coordinate operation using the specified math transform.
     * @throws FactoryException if the operation can not be created.
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
            final CoordinateOperation operation = (CoordinateOperation) transform;
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
         * If the operation type was not explicitely specified, infers it from whether an accuracy is specified
         * or not. In principle, only transformations has an accuracy property; conversions do not. This policy
         * is applied by the properties(Identifier) method in this class.
         */
        if (type == null) {
            type = properties.containsKey(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY)
                    ? Transformation.class : Conversion.class;
        }
        /*
         * The operation method is mandatory. If the user did not provided one, we need to infer it ourselves.
         * If we fail to infer an OperationMethod, let it to null - the exception will be thrown by the factory.
         */
        if (method == null) {
            final Matrix matrix = MathTransforms.getMatrix(transform);
            if (matrix != null) {
                method = Affine.getProvider(transform.getSourceDimensions(), transform.getTargetDimensions(), Matrices.isAffine(matrix));
            } else {
                final ParameterDescriptorGroup descriptor = AbstractCoordinateOperation.getParameterDescriptors(transform);
                if (descriptor != null) {
                    final Identifier name = descriptor.getName();
                    if (name != null) {
                        method = factorySIS.getOperationMethod(name.getCode());
                    }
                    if (method == null) {
                        method = factorySIS.createOperationMethod(properties,
                                sourceCRS.getCoordinateSystem().getDimension(),
                                targetCRS.getCoordinateSystem().getDimension(),
                                descriptor);
                    }
                }
            }
        }
        if (parameters != null) {
            properties.put(ReferencingServices.PARAMETERS_KEY, parameters);
        }
        properties.put(ReferencingServices.OPERATION_TYPE_KEY, type);
        if (Conversion.class.isAssignableFrom(type) && transform.isIdentity()) {
            JDK8.replace(properties, IdentifiedObject.NAME_KEY, AXIS_CHANGES, IDENTITY);
        }
        return factorySIS.createSingleOperation(properties, sourceCRS, targetCRS, null, method, transform);
    }

    /**
     * Logs an unexpected but ignorable exception. This method pretends that the logging
     * come from {@link CoordinateOperationFinder} since this is the public API which
     * use this {@code CoordinateOperationRegistry} class.
     *
     * @param exception  the exception which occurred.
     */
    private static void log(final FactoryException exception) {
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setLoggerName(Loggers.COORDINATE_OPERATION);
        Logging.log(CoordinateOperationFinder.class, "createOperation", record);
    }
}
