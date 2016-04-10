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
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;

// Branch-dependent imports
import java.util.Objects;


/**
 * Base class of code that search for coordinate operation, either by looking in a registry maintained by an authority
 * or by trying to infer the coordinate operation by itself.  There is two subclasses which correspond to the two main
 * strategies for finding coordinate operations:
 *
 * <ul>
 *   <li>{@link CoordinateOperationRegistry} implements the <cite>late-binding</cite> approach
 *       (i.e. search coordinate operation paths specified by authorities like the ones listed in the EPSG dataset),
 *       which is the preferred approach.</li>
 *   <li>{@link CoordinateOperationFinder} implements the <cite>early-binding</cite> approach
 *       (i.e. find a coordinate operation path by inspecting the properties associated to the CRS,
 *       including {@code BOUNDCRS} or {@code TOWGS84} WKT elements).
 *       This approach is used only as a fallback in the late-binding approach gave no result.</li>
 * </ul>
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li>Each instance of this class shall be used only once.</li>
 *   <li>This class is not thread-safe. A new instance shall be created for each coordinate operation to infer.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
abstract class CoordinateOperationFinder {
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
    static final Identifier INVERSE_OPERATION = createIdentifier(Vocabulary.Keys.InverseOperation);

    /**
     * Creates an identifier in the Apache SIS namespace for the given vocabulary key.
     */
    private static Identifier createIdentifier(final short key) {
        return new NamedIdentifier(Citations.SIS, Vocabulary.formatInternational(key));
    }

    /**
     * The factory to use for creating coordinate operations.
     */
    final CoordinateOperationFactory factory;

    /**
     * Used only when we need a SIS-specific method.
     */
    final DefaultCoordinateOperationFactory factorySIS;

    /**
     * The spatio-temporal area of interest, or {@code null} if none.
     */
    Extent areaOfInterest;

    /**
     * The geographic component of the area of interest, or {@code null} if none.
     */
    GeographicBoundingBox bbox;

    /**
     * Creates a new instance for the given factory and context.
     *
     * @param factory The factory to use for creating coordinate operations.
     * @param context The area of interest and desired accuracy, or {@code null} if none.
     */
    public CoordinateOperationFinder(final CoordinateOperationFactory factory,
                                     final CoordinateOperationContext context)
    {
        ArgumentChecks.ensureNonNull("factory", factory);
        this.factory = factory;
        factorySIS = (factory instanceof DefaultCoordinateOperationFactory)
                     ? (DefaultCoordinateOperationFactory) factory : CoordinateOperations.factory();
        if (context != null) {
            areaOfInterest = context.getAreaOfInterest();
            bbox           = context.getGeographicBoundingBox();
        }
    }

    /**
     * Finds or infers an operation for conversion or transformation between two coordinate reference systems.
     * If the subclass implements the <cite>late-binding</cite> approach (see definition of terms in class javadoc),
     * then this method may return {@code null} if no operation path is defined in the registry for the given pair
     * of CRS. Note that it does not mean that no path exist.
     *
     * <p>If the subclass implements the <cite>early-binding</cite> approach (which is the fallback if late-binding
     * gave no result), then this method should never return {@code null} since there is no other fallback.
     * Instead, this method may throw an {@link OperationNotFoundException}.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}, or {@code null} if none.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    public abstract CoordinateOperation createOperation(CoordinateReferenceSystem sourceCRS,
                                                        CoordinateReferenceSystem targetCRS) throws FactoryException;

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
                        method = factory.getOperationMethod(name.getCode());
                    }
                    if (method == null) {
                        method = factory.createOperationMethod(properties,
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
            properties.replace(IdentifiedObject.NAME_KEY, AXIS_CHANGES, IDENTITY);
        }
        return factorySIS.createSingleOperation(properties, sourceCRS, targetCRS, null, method, transform);
    }

    /**
     * Creates the inverse of the given operation.
     */
    final CoordinateOperation inverse(final SingleOperation op) throws NoninvertibleTransformException, FactoryException {
        final CoordinateReferenceSystem sourceCRS = op.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = op.getTargetCRS();
        final MathTransform transform = op.getMathTransform().inverse();
        Class<? extends CoordinateOperation> type = null;
        if (op instanceof Transformation)  type = Transformation.class;
        else if (op instanceof Conversion) type = Conversion.class;
        return createFromMathTransform(properties(INVERSE_OPERATION), targetCRS, sourceCRS,
                transform, InverseOperationMethod.create(op.getMethod()), null, type);
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
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(CoordinateOperation.NAME_KEY, name);
        if ((name == DATUM_SHIFT) || (name == ELLIPSOID_CHANGE)) {
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY, new PositionalAccuracy[] {
                      (name == DATUM_SHIFT) ? PositionalAccuracyConstant.DATUM_SHIFT_APPLIED
                                            : PositionalAccuracyConstant.DATUM_SHIFT_OMITTED});
        }
        return properties;
    }
}
