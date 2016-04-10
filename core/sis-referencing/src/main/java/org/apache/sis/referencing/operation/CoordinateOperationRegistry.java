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
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.*;

import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import java.util.function.Predicate;


/**
 * Searches coordinate operations in a registry maintained by an authority (typically EPSG).
 * Authority factory may help to find transformation paths not available otherwise
 * (often determined from empirical parameters).
 * Authority factories can also provide additional informations like the
 * {@linkplain AbstractCoordinateOperation#getDomainOfValidity() domain of validity},
 * {@linkplain AbstractCoordinateOperation#getScope() scope} and
 * {@linkplain AbstractCoordinateOperation#getCoordinateOperationAccuracy() accuracy}.
 *
 * <p>When <code>{@linkplain #createOperation createOperation}(sourceCRS, targetCRS)</code> is invoked, this class
 * fetches the authority codes for source and target CRS and submits them to the authority factory through a call
 * to its <code>{@linkplain CoordinateOperationAuthorityFactory#createFromCoordinateReferenceSystemCodes
 * createFromCoordinateReferenceSystemCodes}(sourceCode, targetCode)</code> method.
 * If the authority factory does not know about the specified CRS,
 * then {@link CoordinateOperationInference} is used as a fallback.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class CoordinateOperationRegistry extends CoordinateOperationFinder {
    /**
     * The registry authority. This is typically EPSG.
     */
    private final Citation authority;

    /**
     * The object to use for finding authority codes.
     */
    private final IdentifiedObjectFinder codeFinder;

    /**
     * The authority factory to use for creating new operations.
     */
    private final CoordinateOperationAuthorityFactory registry;

    /**
     * Filter the coordinate operation, or {@code null} if none.
     */
    private final Predicate<CoordinateOperation> filter;

    /**
     * Creates a new finder for the given factory.
     */
    CoordinateOperationRegistry(final DefaultCoordinateOperationFactory   factory,
                                final CoordinateOperationAuthorityFactory registry,
                                final CoordinateOperationContext          context) throws FactoryException
    {
        super(factory, context);
        this.registry = registry;
        authority  = registry.getAuthority();
        codeFinder = IdentifiedObjects.newFinder(Citations.getIdentifier(authority, false));
        codeFinder.setSearchDomain(IdentifiedObjectFinder.Domain.ALL_DATASET);
        codeFinder.setIgnoringAxes(true);
        filter = null;  // TODO
    }

    /**
     * Finds the authority code for the given coordinate reference system.
     * This method does not trust the code given by the user in its CRS - we verify it.
     */
    private String findCode(final CoordinateReferenceSystem crs) throws FactoryException {
        final Identifier identifier = IdentifiedObjects.getIdentifier(codeFinder.findSingleton(crs), authority);
        return (identifier != null) ? identifier.getCode() : null;
    }

    /**
     * Returns an operation for conversion or transformation between two coordinate reference systems.
     * The default implementation extracts the authority code from the supplied {@code sourceCRS} and
     * {@code targetCRS}, and submit them to the
     * <code>{@linkplain CoordinateOperationAuthorityFactory#createFromCoordinateReferenceSystemCodes
     * createFromCoordinateReferenceSystemCodes}(sourceCode, targetCode)</code> methods.
     * If no operation is found for those codes, then this method returns {@code null}.
     *
     * @param  sourceCRS  source coordinate reference system.
     * @param  targetCRS  target coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}, or {@code null}
     *         if no such operation is explicitly defined in the underlying database.
     */
    @Override
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
                     * Found an operation. If we had to extract the horizontal part of some 3D CRS,
                     * then we need to modify the coordinate operation.
                     */
                    if (combine != 0) {
                        operation = propagateVertical(operation, source != sourceCRS, target != targetCRS);
                        operation = complete(operation, sourceCRS, targetCRS);
                    }
                    return operation;
                }
            } catch (IllegalArgumentException | ConversionException e) {
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
             * target codes.   The CoordinateOperationInference class can do that, providing that we prevent
             * this CoordinateOperationRegistry to (legitimately) claims that the operation from sourceCode
             * to targetCode is the identity transform.
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
        } catch (NoSuchAuthorityCodeException | MissingFactoryResourceException exception) {
            /*
             * sourceCode or targetCode is unknown to the underlying authority factory.
             * Ignores the exception and fallback on the generic algorithm provided by
             * CoordinateOperationInference.
             */
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
                            CoordinateOperationRegistry.class, "search", exception);
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
                final double area = Extents.area(Extents.intersection(bbox, Extents.getGeographicBoundingBox(candidate.getDomainOfValidity())));
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
     * Returns the inverse of the specified operation.
     *
     * @param  operation The operation to invert, or {@code null}.
     * @return The inverse of {@code operation}, or {@code null} if none.
     * @throws NoninvertibleTransformException if the operation is not invertible.
     * @throws FactoryException if the operation creation failed for an other reason.
     */
    protected CoordinateOperation inverse(final CoordinateOperation operation)
            throws NoninvertibleTransformException, FactoryException
    {
        if (operation instanceof SingleOperation) {
            return super.inverse((SingleOperation) operation);
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
        assert Utilities.deepEquals(sourceCRS, targetCRS, ComparisonMode.ALLOW_VARIANT);
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
     * @throws FactoryException if the operation can not be constructed.
     */
    private CoordinateOperation transform(final CoordinateReferenceSystem sourceCRS,
                                          final MathTransform             prepend,
                                                CoordinateOperation       operation,
                                          final MathTransform             append,
                                          final CoordinateReferenceSystem targetCRS,
                                          final MathTransformFactory      mtFactory)
            throws FactoryException
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
        return recreate(operation, sourceCRS, targetCRS, transform);
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
     * @return a new operation from the given source CRS to target CRS using the given transform.
     * @throws FactoryException if an error occurred while creating the new operation.
     */
    private CoordinateOperation recreate(final CoordinateOperation       operation,
                                               CoordinateReferenceSystem sourceCRS,
                                               CoordinateReferenceSystem targetCRS,
                                         final MathTransform             transform) throws FactoryException
    {
        /*
         * If the user-provided CRS are approximatively equal to the coordinate operation CRS, keep the later.
         * The reason is that coordinate operation CRS are built from the definitions provided by the authority,
         * while the user-provided CRS can be anything (e.g. parsed from a quite approximative WKT).
         */
        CoordinateReferenceSystem crs;
        if (Utilities.equalsApproximatively(sourceCRS, crs = operation.getSourceCRS())) sourceCRS = crs;
        if (Utilities.equalsApproximatively(targetCRS, crs = operation.getTargetCRS())) targetCRS = crs;
        /*
         * Determine whether the operation to create is a Conversion or a Transformation.
         * Conversion may also be a more accurate type like Projection. We want the GeoAPI
         * interface. The most reliable way is to ask to the operation, but this is SIS-specific.
         * The fallback uses reflection.
         */
        final Class<? extends CoordinateOperation> type;
        if (operation instanceof AbstractCoordinateOperation) {
            type = ((AbstractCoordinateOperation) operation).getInterface();
        } else {
            final Class<? extends CoordinateOperation>[] types =
                    Classes.getLeafInterfaces(operation.getClass(), CoordinateOperation.class);
            type = (types.length != 0) ? types[0] : SingleOperation.class;
        }
        /*
         * Reuse the same operation method, just changing its number of dimension.
         */
        OperationMethod method = null;
        if (operation instanceof SingleOperation) {
            method = DefaultOperationMethod.redimension(((SingleOperation) operation).getMethod(),
                            transform.getSourceDimensions(), transform.getTargetDimensions());
        }
        final Map<String,Object> properties = new HashMap<>(derivedFrom(operation));
        properties.put(ReferencingServices.OPERATION_TYPE_KEY, type);
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
     * @param  operation the original (typically two-dimensional) coordinate operation.
     * @param  source3D  {@code true} for adding ellipsoidal height in source coordinates.
     * @param  target3D  {@code true} for adding ellipsoidal height in target coordinates.
     * @return a coordinate operation with the source and/or target coordinates made 3D,
     *         or {@code null} if this method does not know how to create the operation.
     * @throws FactoryException if an error occurred while creating the coordinate operation.
     */
    private CoordinateOperation propagateVertical(final CoordinateOperation operation,
            final boolean source3D, final boolean target3D) throws FactoryException
    {
        final List<CoordinateOperation> operations = new ArrayList<>();
        if (operation instanceof ConcatenatedOperation) {
            operations.addAll(((ConcatenatedOperation) operation).getOperations());
        } else {
            operations.add(operation);
        }
        if ((source3D && !propagateVertical(operations.listIterator(), true)) ||
            (target3D && !propagateVertical(operations.listIterator(operations.size()), false)))
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
     * @param  operations  the chain of operations in which to add a vertical axis.
     * @param  forward     {@code true} for adding the vertical axis at the beginning, or
     *                     {@code false} for adding the vertical axis at the end.
     * @return {@code true} on success.
     */
    private boolean propagateVertical(final ListIterator<CoordinateOperation> operations, final boolean forward)
            throws FactoryException
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
             * We can process only linear operations, otherwise we would not know how to add a dimension.
             * Examples of linear operations are:
             *
             *   - Longitude rotation (EPSG:9601). Note that this is a transformation rather than a conversion.
             *   - Geographic3D to 2D conversion (EPSG:9659).
             */
            Matrix matrix = MathTransforms.getMatrix(op.getMathTransform());
            if (matrix == null) {
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
                operations.set(recreate(op, toGeodetic3D(sourceCRS), toGeodetic3D(targetCRS), mt));
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
    private CoordinateReferenceSystem toGeodetic3D(CoordinateReferenceSystem crs) throws FactoryException {
        assert (crs instanceof GeodeticCRS) && (crs.getCoordinateSystem() instanceof EllipsoidalCS) : crs;
        if (crs.getCoordinateSystem().getDimension() == 2) {
            crs = ReferencingServices.getInstance().createCompoundCRS(factorySIS.getCRSFactory(), factorySIS.getCSFactory(),
                    derivedFrom(crs), crs, CommonCRS.Vertical.ELLIPSOIDAL.crs());
        }
        return crs;
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
     * Logs an unexpected but ignorable exception. This method pretends that the logging
     * come from {@link DefaultCoordinateOperationFactory} since this is the public API
     * which use this {@code CoordinateOperationRegistry} class.
     *
     * @param exception  the exception which occurred.
     */
    private void log(final FactoryException exception) {
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setLoggerName(Loggers.COORDINATE_OPERATION);
        Logging.log(DefaultCoordinateOperationFactory.class, "createOperation", record);
    }
}
