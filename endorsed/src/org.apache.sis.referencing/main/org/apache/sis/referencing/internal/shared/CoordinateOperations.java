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
package org.apache.sis.referencing.internal.shared;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ObjectFactory;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.metadata.internal.shared.NameToIdentifier;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.URLs;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Loggers;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeneralDerivedCRS;

// Specific to the main branch:
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.MathTransformBuilder;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;


/**
 * Utility methods related to coordinate operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CoordinateOperations {
    /**
     * The logger where to report warnings.
     */
    public static final Logger LOGGER = Logger.getLogger(Loggers.COORDINATE_OPERATION);

    /**
     * The key for specifying explicitly the value to be returned by
     * {@link org.apache.sis.referencing.operation.DefaultConversion#getParameterValues()}.
     * It is usually not necessary to specify those parameters because they are inferred either from the
     * {@link org.opengis.referencing.operation.MathTransform}, or specified explicitly in a {@code DefiningConversion}.
     * However, there is a few cases, for example the Molodenski transform, where none of the above can apply,
     * because SIS implements those operations as a concatenation of math transforms,
     * and such concatenations do not have {@link org.opengis.parameter.ParameterValueGroup}.
     */
    public static final String PARAMETERS_KEY = "parameters";

    /**
     * The key for specifying the base type of the coordinate operation to create. This optional entry
     * is used by {@code DefaultCoordinateOperationFactory.createSingleOperation(…)}. Apache SIS tries
     * to infer this value automatically, but this entry may help SIS to perform a better choice in
     * some cases. For example, an "Affine" operation can be both a conversion or a transformation
     * (the latter is used in datum shift in geocentric coordinates).
     */
    public static final String OPERATION_TYPE_KEY = "operationType";

    /**
     * Value of {@link org.apache.sis.referencing.operation.CoordinateOperationContext#getConstantCoordinates()}.
     * This thread-local is used as a workaround for the fact that we do not yet provide a public API for this
     * functionality. This workaround should be deleted after a public API is defined.
     */
    public static final ThreadLocal<Supplier<double[]>> CONSTANT_COORDINATES = new ThreadLocal<>();

    /**
     * Cached values of {@link #wrapAroundChanges wrapAroundChanges(…)}, created when first needed.
     * Indices are bit masks computed by {@link #changes changes(…)}. Since the most common "wrap around" axes
     * are longitude at dimension 0 or 1, and some measurement of time (in climatology) at dimension 2 or 3,
     * then the most likely values are (binary digits):
     *
     * <pre class="text">
     *     0000    0100    1000
     *     0001    0101    1001
     *     0010    0110    1010</pre>
     *
     * The last decimal value is 10 (binary {@code 1010}); we don't need to cache more.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Set<Integer>[] CACHE = new Set[11];

    /**
     * Do not allow instantiation.
     */
    private CoordinateOperations() {
    }

    /**
     * Returns the coordinate operation factory to use for the given properties and math transform factory.
     * If the given properties are empty and the {@code mtFactory} is the system default, then this method
     * returns the system default {@code CoordinateOperationFactory} instead of creating a new one.
     *
     * <p>It is okay to set all parameters to {@code null} in order to get the system default factory.</p>
     *
     * @param  properties  the default properties.
     * @param  mtFactory   the math transform factory to use.
     * @param  crsFactory  the factory to use if the operation factory needs to create CRS for intermediate steps.
     * @param  csFactory   the factory to use if the operation factory needs to create CS for intermediate steps.
     * @return the coordinate operation factory to use.
     */
    public static CoordinateOperationFactory getCoordinateOperationFactory(Map<String,?> properties,
            final MathTransformFactory mtFactory, final CRSFactory crsFactory, final CSFactory csFactory)
    {
        if (Containers.isNullOrEmpty(properties)) {
            if (isDefaultInstance(mtFactory) &&
                isDefaultInstance(crsFactory) &&
                isDefaultInstance(csFactory))
            {
                return DefaultCoordinateOperationFactory.provider();
            }
            properties = Map.of();
        }
        final HashMap<String,Object> p = new HashMap<>(properties);
        p.putIfAbsent(ReferencingFactoryContainer.CRS_FACTORY, crsFactory);
        p.putIfAbsent(ReferencingFactoryContainer.CS_FACTORY,  csFactory);
        properties = p;
        return new DefaultCoordinateOperationFactory(properties, mtFactory);
    }

    /**
     * Returns the method of the given coordinate operation, or {@code null} if none.
     *
     * @param  operation  the operation from which to get the method, or {@code null}.
     * @return the coordinate operation method, or {@code null} if none.
     */
    public static OperationMethod getMethod(final CoordinateOperation operation) {
        if (operation instanceof SingleOperation) {
            return ((SingleOperation) operation).getMethod();
        }
        return null;
    }

    /**
     * Returns the operation method for the name of the given identified object.
     * The given object is typically a {@code ParameterDescriptorGroup}.
     *
     * @param  mtFactory   the factory where to search.
     * @param  descriptor  object from which to take the name of the operation method to fetch.
     * @return the operation method of the desired name.
     * @throws NoSuchIdentifierException if the requested operation method cannot be found.
     */
    public static OperationMethod findMethod(final MathTransformFactory mtFactory, final IdentifiedObject descriptor)
            throws NoSuchIdentifierException
    {
        final String methodName = descriptor.getName().getCode();
        String methodIdentifier = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(descriptor, Citations.EPSG));
        if (methodIdentifier == null) {
            methodIdentifier = methodName;
        }
        /*
         * Get the MathTransformProvider of the same name or identifier than the given parameter group.
         * We give precedence to EPSG identifier because operation method names are sometimes ambiguous
         * (e.g. "Lambert Azimuthal Equal Area (Spherical)"). If we fail to find the method by its EPSG code,
         * we will try searching by method name. As a side effect, this second attempt will produce a better
         * error message if the method is really not found.
         */
        OperationMethod method;
        try {
            method = findMethod(mtFactory, methodIdentifier);
        } catch (NoSuchIdentifierException exception) {
            if (methodIdentifier.equals(methodName)) {
                throw exception;
            } else try {
                method = findMethod(mtFactory, methodName);
            } catch (NoSuchIdentifierException e) {
                e.addSuppressed(exception);
                throw e;
            }
            // Source class and method name will be inferred from the stack trace.
            Logging.recoverableException(LOGGER, null, null, exception);
        }
        return method;
    }

    /**
     * Returns the operation method for the specified name or identifier, using user-overrideable method if possible.
     * If the given factory is an instance of {@link DefaultMathTransformFactory}, then this method delegates to it.
     * Otherwise, this method delegates to {@link #findMethod(Iterable, String)}.
     *
     * @param  mtFactory  the factory where to search for a method.
     * @param  name  the name of the operation method to fetch.
     * @return the operation method of the given name.
     * @throws NoSuchIdentifierException if the requested operation method cannot be found.
     */
    public static OperationMethod findMethod(final MathTransformFactory mtFactory, final String name)
            throws NoSuchIdentifierException
    {
        if (mtFactory instanceof DefaultMathTransformFactory) {
            return ((DefaultMathTransformFactory) mtFactory).getOperationMethod(name);
        }
        return findMethod(mtFactory.getAvailableMethods(SingleOperation.class), name);
    }

    /**
     * Returns the operation method for the specified name or identifier. The given argument shall be either
     * a method name (e.g. <q>Transverse Mercator</q>) or one of its identifiers (e.g. {@code "EPSG:9807"}).
     * The search is case-insensitive and comparisons against method names can be
     * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod#isHeuristicMatchForName(String) heuristic}.
     *
     * <p>If more than one method match the given name, then the first (according iteration order)
     * non-{@linkplain Deprecable#isDeprecated() deprecated} matching method is returned.
     * If all matching methods are deprecated, the first one is returned.</p>
     *
     * @param  methods     the method candidates.
     * @param  identifier  the name or identifier of the operation method to search.
     * @return the coordinate operation method for the given name or identifier.
     * @throws NoSuchIdentifierException if the requested operation method cannot be found.
     *
     * @see DefaultMathTransformFactory#getOperationMethod(String)
     */
    public static OperationMethod findMethod(final Iterable<? extends OperationMethod> methods, final String identifier)
            throws NoSuchIdentifierException
    {
        OperationMethod fallback = null;
        for (final OperationMethod method : methods) {
            if (NameToIdentifier.isHeuristicMatchForName(method, identifier) ||
                    NameToIdentifier.isHeuristicMatchForIdentifier(method.getIdentifiers(), identifier))
            {
                /*
                 * Stop the iteration at the first non-deprecated method.
                 * If we find only deprecated methods, take the first one.
                 */
                if (!(method instanceof Deprecable) || !((Deprecable) method).isDeprecated()) {
                    return method;
                }
                if (fallback == null) {
                    fallback = method;
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new NoSuchIdentifierException(Resources.format(
                Resources.Keys.NoSuchOperationMethod_2, identifier, URLs.OPERATION_METHODS), identifier);
    }

    /**
     * Returns the transform builder for the specified method name, using user-overrideable method if possible.
     * If the given factory is an instance of {@link DefaultMathTransformFactory}, then this method delegates to it.
     *
     * @param  mtFactory  the factory to use for a builder.
     * @param  method     the name of the operation method to fetch.
     * @return the transform builder for the operation method of the given name.
     * @throws NoSuchIdentifierException if the requested operation method cannot be found.
     */
    public static MathTransformBuilder builder(final MathTransformFactory mtFactory, final String method)
            throws NoSuchIdentifierException
    {
        if (mtFactory instanceof DefaultMathTransformFactory) {
            return ((DefaultMathTransformFactory) mtFactory).builder(method);
        } else {
            final var m = findMethod(mtFactory.getAvailableMethods(SingleOperation.class), method);
            return new ParameterizedTransformBuilder(mtFactory, m) {
                @Override public MathTransform create() throws FactoryException {
                    if (sourceCS == null && targetCS == null && sourceEllipsoid == null && targetEllipsoid == null) {
                        return swapAndScaleAxes(factory.createParameterizedTransform(parameters()));
                    }
                    return super.create();
                }
            };
        }
    }

    /**
     * Returns {@code true} if the given transform factory is the default instances used by Apache SIS.
     *
     * @param  mtFactory  the factory to test. May be null.
     * @return whether the given factory is the default instance.
     */
    public static boolean isDefaultInstance(final MathTransformFactory mtFactory) {
        return mtFactory == DefaultMathTransformFactory.provider();
    }

    /**
     * Returns {@code true} if the given factory is the default instances used by Apache SIS.
     *
     * @param  factory  the factory to test. May be null.
     * @return whether the given factory is the default instance.
     */
    private static boolean isDefaultInstance(final ObjectFactory factory) {
        return factory == GeodeticObjectFactory.provider();
    }

    /**
     * Returns {@code true} if the given axis is of kind "Wrap Around".
     * Defined here because used together with {@link #wrapAroundChanges wrapAroundChanges(…)}.
     * This method can be used in contexts of coordinate operations, when the caller wants to
     * be consistent with whatever policy change may happen in the future in this class.
     *
     * @param  axis  the axis to test.
     * @return {@code true} if the given axis has "wrap around" range meaning.
     *
     * @see org.apache.sis.geometry.AbstractEnvelope#isWrapAround(CoordinateSystemAxis)
     */
    public static boolean isWrapAround(final CoordinateSystemAxis axis) {
        return axis.getRangeMeaning() == RangeMeaning.WRAPAROUND;
    }

    /**
     * Returns indices of target dimensions where "wrap around" may happen as a result of a coordinate operation.
     * This is usually the longitude axis when the source CRS uses the [-180 … +180]° range and the target CRS
     * uses the [0 … 360]° range, or the converse.
     *
     * @param  op  the coordinate operation for which to get "wrap around" target dimensions.
     * @return target dimensions where "wrap around" may happen, or an empty set if none.
     *
     * @see AbstractCoordinateOperation#getWrapAroundChanges()
     */
    public static Set<Integer> wrapAroundChanges(final CoordinateOperation op) {
        if (op instanceof AbstractCoordinateOperation) {
            return ((AbstractCoordinateOperation) op).getWrapAroundChanges();
        } else if (op != null) {
            final CoordinateReferenceSystem source, target;
            if ((source = op.getSourceCRS()) != null &&
                (target = op.getTargetCRS()) != null)
            {
                return wrapAroundChanges(source, target.getCoordinateSystem());
            }
        }
        return Set.of();
    }

    /**
     * Computes indices of target dimensions where "wrap around" may happen as a result of a coordinate operation.
     * This is usually the longitude axis when the source CRS uses the [-180 … +180]° range and the target CRS uses
     * the [0 … 360]° range, or the converse.
     *
     * @param  source  the source of the coordinate operation.
     * @param  target  the target of the coordinate operation.
     * @return target dimensions where "wrap around" may happen, or an empty set if none.
     */
    public static Set<Integer> wrapAroundChanges(CoordinateReferenceSystem source, final CoordinateSystem target) {
        long changes = changes(source.getCoordinateSystem(), target);
        while (source instanceof GeneralDerivedCRS) {
            source = ((GeneralDerivedCRS) source).getBaseCRS();
            changes |= changes(source.getCoordinateSystem(), target);
        }
        final boolean useCache = (changes >= 0 && changes < CACHE.length);
        if (useCache) {
            /*
             * In most cases we have an existing instance. Since WrapAroundAxes are immutable, if we got a reference,
             * the object should be okay ("published" in memory model terminology) even if we did not synchronized.
             * The reference however may not be visible in the array, but we will check later in a synchronized block.
             */
            final Set<Integer> existing = CACHE[(int) changes];
            if (existing != null) {
                return existing;
            }
        }
        /*
         * No existing instance found. Expand the `changes` bit mask in an array of integers in order to create an
         * unmodifiable List<Integer>. The list is for public API; internally, Apache SIS will use toBitMask(…).
         */
        long r = changes;
        final Integer[] indices = new Integer[Long.bitCount(r)];
        for (int i=0; i<indices.length; i++) {
            final int dim = Long.numberOfTrailingZeros(r);
            indices[i] = dim;
            r &= ~(1L << dim);
        }
        final Set<Integer> dimensions = CollectionsExt.immutableSet(true, indices);
        if (useCache) {
            synchronized (CACHE) {
                final Set<Integer> existing = CACHE[(int) changes];
                if (existing != null) {
                    return existing;
                }
                CACHE[(int) changes] = dimensions;
            }
        }
        return dimensions;
    }

    /**
     * Returns the packed indices of target dimensions where coordinate values may need to be wrapped around.
     * This method matches target coordinate system axes having {@link RangeMeaning#WRAPAROUND} with source
     * axes, then verifies if the range of values changed (taking unit conversions in account). A target
     * dimension {@code i} may need to "wrap around" the coordinate values if the {@code 1 << i} bit is set.
     * If there is no change, then the value is zero.
     */
    private static long changes(final CoordinateSystem source, final CoordinateSystem target) {
        long changes = 0;
        if (source != target) {                                 // Optimization for a common case.
            /*
             * Get the dimensions of all axes in the source coordinate system as bit fields.
             * We create this list first because we may iterate more than once on those axes
             * and we want to clear the axes that we already matched. We use a bitmask for
             * efficiency, with the bits of dimensions to consider set to 1.
             *
             * Note: a previous version was creating a list of "wraparound" axes only. We removed that filter
             * because a target wraparound axis may match a source infinite axis. For example, when converting
             * dates on a temporal axis (with infinite span toward past and future) to months on a climatology
             * axis (January to December months without year), the same cycle is repeated after every 12 months
             * even if the source axis had no cycle.
             */
            long isWrapAroundAxis = (1 << source.getDimension()) - 1;
            /*
             * For each "wrap around" axis in the target CRS, search a matching axis in the source CRS
             * which is also "wrap around", is colinear and uses compatible unit of measurement. There
             * is usually at most one "wrap around" axis, but this code is nevertheless generic enough
             * for an arbitrary number of axes.
             */
            final int dim = Math.min(Long.SIZE, target.getDimension());
compare:    for (int i=0; i<dim; i++) {
                final CoordinateSystemAxis axis = target.getAxis(i);
                if (isWrapAround(axis)) {
                    long candidates = isWrapAroundAxis;
                    do {
                        final long mask = Long.lowestOneBit(candidates);
                        final CoordinateSystemAxis src = source.getAxis(Long.numberOfTrailingZeros(mask));
                        if (AxisDirections.isColinear(src.getDirection(), axis.getDirection())) {
                            try {
                                final UnitConverter c  = src.getUnit().getConverterToAny(axis.getUnit());
                                final double minimum   = axis.getMinimumValue();
                                final double maximum   = axis.getMaximumValue();
                                final double tolerance = (maximum - minimum) * Numerics.COMPARISON_THRESHOLD;
                                if (!Numerics.epsilonEqual(c.convert(src.getMinimumValue()), minimum, tolerance) ||
                                    !Numerics.epsilonEqual(c.convert(src.getMaximumValue()), maximum, tolerance))
                                {
                                    changes |= (1 << i);
                                }
                                isWrapAroundAxis &= ~mask;      // We are done with this source axis.
                                if (isWrapAroundAxis == 0) {
                                    break compare;              // Useless to continue if there are no more source axes.
                                }
                                continue compare;               // Match next pair of wrap around axes.
                            } catch (IncommensurableException e) {
                                // Ignore (should not happen often). We will try to match another pair of axes.
                            }
                        }
                        candidates &= ~mask;        // Unable to use that axis. Check if we can use another one.
                    } while (candidates != 0);
                }
            }
        }
        return changes;
    }
}
