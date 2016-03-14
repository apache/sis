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
import java.util.Collections;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Duration;
import javax.measure.converter.ConversionException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;

// Branch-dependent imports
import java.util.Objects;


/**
 * Infers a conversion of transformation path from a source CRS to a target CRS.
 * Instances of this class are created by {@link DefaultCoordinateOperationFactory}.
 * The only public method is {@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)},
 * which dispatches its work to the {@code createOperationStep(…)} protected methods.
 * Developers can override those protected methods if they want to alter the way some operations are created.
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
 *
 * @see DefaultCoordinateOperationFactory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateOperationContext)
 */
public class CoordinateOperationInference {
    /**
     * The identifier for an identity operation.
     */
    private static final Identifier IDENTITY = createIdentifier(Vocabulary.Keys.Identity);

    /**
     * The identifier for conversion using an affine transform for axis swapping and/or unit conversions.
     */
    private static final Identifier AXIS_CHANGES = createIdentifier(Vocabulary.Keys.AxisChanges);

    /**
     * The identifier for a transformation which is a datum shift without {@link BursaWolfParameters}.
     * Only the changes in ellipsoid axis-length are taken in account.
     * Such ellipsoid shifts are approximative and may have 1 kilometre error.
     *
     * @see org.apache.sis.internal.referencing.PositionalAccuracyConstan#DATUM_SHIFT_OMITTED
     */
    private static final Identifier ELLIPSOID_CHANGE = createIdentifier(Vocabulary.Keys.EllipsoidChange);

    /**
     * The identifier for a transformation which is a datum shift.
     *
     * @see org.apache.sis.internal.referencing.PositionalAccuracyConstant#DATUM_SHIFT_APPLIED
     */
    private static final Identifier DATUM_SHIFT = createIdentifier(Vocabulary.Keys.DatumShift);

    /**
     * The identifier for a geocentric conversion.
     */
    private static final Identifier GEOCENTRIC_CONVERSION = createIdentifier(Vocabulary.Keys.GeocentricConversion);

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
     * The factory to use for creating coordinate operations.
     */
    private final CoordinateOperationFactory factory;

    /**
     * Used only when we need a SIS-specific method.
     */
    private final DefaultCoordinateOperationFactory factorySIS;

    /**
     * The area of interest, or {@code null} if none.
     */
    private GeographicBoundingBox areaOfInterest;

    /**
     * The desired accuracy in metres, or 0 for the best accuracy available.
     */
    private double desiredAccuracy;

    /**
     * Creates a new instance for the given factory and context.
     *
     * @param factory The factory to use for creating coordinate operations.
     * @param context The area of interest and desired accuracy, or {@code null} if none.
     */
    public CoordinateOperationInference(final CoordinateOperationFactory factory,
                                        final CoordinateOperationContext context)
    {
        ArgumentChecks.ensureNonNull("factory", factory);
        this.factory = factory;
        factorySIS = (factory instanceof DefaultCoordinateOperationFactory) ? (DefaultCoordinateOperationFactory) factory
                : DefaultFactories.forBuildin(CoordinateOperationFactory.class, DefaultCoordinateOperationFactory.class);
        if (context != null) {
            areaOfInterest  = context.getAreaOfInterest();
            desiredAccuracy = context.getDesiredAccuracy();
        }
    }

    /**
     * The operation to use by {@link #createTransformationStep(GeographicCRS,GeographicCRS)}
     * for datum shift. This string can have one of the following values, from most accurate
     * to most approximative operations:
     *
     * <ul>
     *   <li>{@code null} for performing datum shifts in geocentric coordinates.</li>
     *   <li>{@code "Molodensky"} for the Molodensky transformation.</li>
     *   <li>{@code "Abridged Molodensky"} for the abridged Molodensky transformation.</li>
     * </ul>
     *
     * @todo Value will need to be determined according the desired accuracy.
     */
    private String getMolodenskyMethod() {
        return "Molodensky";
    }

    /**
     * Infers an operation for conversion or transformation between two coordinate reference systems.
     * This method inspects the given CRS and delegates the work to one or many {@code createOperationStep(…)} methods.
     * This method fails if no path between the CRS is found.
     *
     * @param  sourceCRS Input coordinate reference system.
     * @param  targetCRS Output coordinate reference system.
     * @return A coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws OperationNotFoundException, FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        if (areaOfInterest == null) {
            areaOfInterest = Extents.intersection(CRS.getGeographicBoundingBox(sourceCRS),
                                                  CRS.getGeographicBoundingBox(targetCRS));
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////                        Compound  →  various CRS                        ////
        ////                                                                        ////
        ////  We check CompoundCRS first because experience shows that it produces  ////
        ////  simpler transformation chains than if we check them last.             ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof CompoundCRS) {
            final CompoundCRS source = (CompoundCRS) sourceCRS;
            if (targetCRS instanceof CompoundCRS) {
//              return createOperationStep(source, (CompoundCRS) targetCRS);
            }
            if (targetCRS instanceof SingleCRS) {
//              return createOperationStep(source, (SingleCRS) targetCRS);
            }
        }
        if (targetCRS instanceof CompoundCRS) {
            final CompoundCRS target = (CompoundCRS) targetCRS;
            if (sourceCRS instanceof SingleCRS) {
//              return createOperationStep((SingleCRS) sourceCRS, target);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////                 Projected  →  Projected or Geographic                  ////
        ////                                                                        ////
        ////      This check needs to be done before the check for DerivedCRS       ////
        ////      because ProjectedCRS is a particular kind of derived CRS.         ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof ProjectedCRS) {
            final ProjectedCRS source = (ProjectedCRS) sourceCRS;
            if (targetCRS instanceof ProjectedCRS) {
//              return createOperationStep(source, (ProjectedCRS) targetCRS);
            }
            if (targetCRS instanceof GeographicCRS) {
//              return createOperationStep(source, (GeographicCRS) targetCRS);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////           Geographic  →  Geographic, Projected or Geocentric           ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof GeographicCRS) {
            final GeographicCRS source = (GeographicCRS) sourceCRS;
            if (targetCRS instanceof GeographicCRS) {
//              return createOperationStep(source, (GeographicCRS) targetCRS);
            }
            if (targetCRS instanceof ProjectedCRS) {
//              return createOperationStep(source, (ProjectedCRS) targetCRS);
            }
            if (targetCRS instanceof GeocentricCRS) {
//              return createOperationStep(source, (GeocentricCRS) targetCRS);
            }
            if (targetCRS instanceof VerticalCRS) {
//              return createOperationStep(source, (VerticalCRS) targetCRS);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////                Geocentric  →  Geocentric or Geographic                 ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof GeocentricCRS) {
            final GeocentricCRS source = (GeocentricCRS) sourceCRS;
            if (targetCRS instanceof GeocentricCRS) {
                return createOperationStep(source, (GeocentricCRS) targetCRS);
            }
            if (targetCRS instanceof GeographicCRS) {
//              return createOperationStep(source, (GeographicCRS) targetCRS);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////                         Vertical  →  Vertical                          ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof VerticalCRS) {
            final VerticalCRS source = (VerticalCRS) sourceCRS;
            if (targetCRS instanceof VerticalCRS) {
                return createOperationStep(source, (VerticalCRS) targetCRS);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////                         Temporal  →  Temporal                          ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof TemporalCRS) {
            final TemporalCRS source = (TemporalCRS) sourceCRS;
            if (targetCRS instanceof TemporalCRS) {
                return createOperationStep(source, (TemporalCRS) targetCRS);
            }
        }
        throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS));
    }

    /**
     * Creates an operation between two geocentric coordinate reference systems.
     * The default implementation can adjust for axis order, orientation and units of measurement.
     * If the datums are not the equal but {@linkplain DefaultGeodeticDatum#getBursaWolfParameters()
     * Bursa-Wolf parameters exists} between the two datum in the area of interest, then this method
     * will also perform a datum shift.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be constructed.
     *
     * @todo Rotation of prime meridian not yet implemented.
     * @todo Conversion between Cartesian and spherical CS not yet implemented.
     */
    protected CoordinateOperation createOperationStep(final GeocentricCRS sourceCRS,
                                                      final GeocentricCRS targetCRS)
            throws FactoryException
    {
        final GeodeticDatum sourceDatum = sourceCRS.getDatum();
        final GeodeticDatum targetDatum = targetCRS.getDatum();
        final CoordinateSystem sourceCS = sourceCRS.getCoordinateSystem();
        final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
        if (Utilities.equalsIgnoreMetadata(sourceDatum, targetDatum)) {
            /*
             * If both CRS use the same datum and the same prime meridian, then the coordinate operation is just
             * an axis swapping and unit conversion except if the coordinate systems are not of the same kind.
             */
            final Matrix matrix = swapAndScaleAxes(sourceCS, targetCS);
            return createFromAffineTransform(AXIS_CHANGES, sourceCRS, targetCRS, matrix);
        }
        if (!isGreenwichLongitudeEquals(sourceDatum.getPrimeMeridian(), targetDatum.getPrimeMeridian())) {
            throw new OperationNotFoundException("Rotation of prime meridian not yet implemented");
        }
        /*
         * Transform between differents ellipsoids using Bursa Wolf parameters.
         * The Bursa Wolf parameters are used with "standard" geocentric CS, i.e.
         * with x axis towards the prime meridian, y axis towards East and z axis
         * toward North. The following steps are applied:
         *
         *     source CRS                      →
         *     standard CRS with source datum  →
         *     standard CRS with target datum  →
         *     target CRS
         */
        Matrix datumShift = null;
        Identifier identifier = DATUM_SHIFT;
        MatrixSIS matrix;
        final CartesianCS standard = (CartesianCS) CommonCRS.WGS84.geocentric().getCoordinateSystem();
        final Matrix normalizeSource = swapAndScaleAxes(sourceCS, standard);
        final Matrix normalizeTarget = swapAndScaleAxes(standard, targetCS);
        /*
         * Since all steps are matrices, we can multiply them and get a single matrix.
         * MatrixSIS.multiply(Matrix) is equivalent to AffineTransform.concatenate(…):
         * First transform by the supplied transform and then transform the result by
         * the original transform. So we compute;
         *
         *     matrix = normalizeTarget × datumShift × normalizeSource
         */
        matrix = Matrices.multiply(normalizeTarget, datumShift);
        matrix = matrix.multiply(normalizeSource);
        return createFromAffineTransform(identifier, sourceCRS, targetCRS, matrix);
    }

    /**
     * Creates an operation between two vertical coordinate reference systems.
     * The default implementation checks if both CRS use the same datum, then
     * adjusts for axis direction and units.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be constructed.
     *
     * @todo Needs to implement vertical datum shift.
     */
    protected CoordinateOperation createOperationStep(final VerticalCRS sourceCRS,
                                                      final VerticalCRS targetCRS)
            throws FactoryException
    {
        final VerticalDatum sourceDatum = sourceCRS.getDatum();
        final VerticalDatum targetDatum = targetCRS.getDatum();
        if (!Utilities.equalsIgnoreMetadata(sourceDatum, targetDatum)) {
            throw new OperationNotFoundException(notFoundMessage(sourceDatum, targetDatum));
        }
        final VerticalCS sourceCS = sourceCRS.getCoordinateSystem();
        final VerticalCS targetCS = targetCRS.getCoordinateSystem();
        final Matrix     matrix   = swapAndScaleAxes(sourceCS, targetCS);
        return createFromAffineTransform(AXIS_CHANGES, sourceCRS, targetCRS, matrix);
    }

    /**
     * Creates an operation between two temporal coordinate reference systems.
     * The default implementation checks if both CRS use the same datum, then
     * adjusts for axis direction, units and epoch.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be constructed.
     */
    protected CoordinateOperation createOperationStep(final TemporalCRS sourceCRS,
                                                      final TemporalCRS targetCRS)
            throws FactoryException
    {
        final TemporalDatum sourceDatum = sourceCRS.getDatum();
        final TemporalDatum targetDatum = targetCRS.getDatum();
        final TimeCS sourceCS = sourceCRS.getCoordinateSystem();
        final TimeCS targetCS = targetCRS.getCoordinateSystem();
        /*
         * Compute the epoch shift.  The epoch is the time "0" in a particular coordinate reference system.
         * For example, the epoch for java.util.Date object is january 1, 1970 at 00:00 UTC. We compute how
         * much to add to a time in 'sourceCRS' in order to get a time in 'targetCRS'. This "epoch shift" is
         * in units of 'targetCRS'.
         */
        final Unit<Duration> targetUnit = targetCS.getAxis(0).getUnit().asType(Duration.class);
        double epochShift = sourceDatum.getOrigin().getTime() -
                            targetDatum.getOrigin().getTime();
        epochShift = Units.MILLISECOND.getConverterTo(targetUnit).convert(epochShift);
        /*
         * Check axis directions. The method 'swapAndScaleAxes' should returns a matrix of size 2×2.
         * The element at index (0,0) may be +1 if source and target axes are in the same direction,
         * or -1 if there are in opposite direction ("PAST" vs "FUTURE"). The value may be something
         * else than ±1 if a unit conversion is applied too.  For example the value is 60 if time in
         * sourceCRS is in hours while time in targetCRS is in minutes.
         *
         * The "epoch shift" previously computed is a translation. Consequently, it is added to element (0,1).
         */
        final Matrix matrix = swapAndScaleAxes(sourceCS, targetCS);
        final int translationColumn = matrix.getNumCol() - 1;           // Paranoiac check: should always be 1.
        final double translation = matrix.getElement(0, translationColumn);
        matrix.setElement(0, translationColumn, translation + epochShift);
        return createFromAffineTransform(AXIS_CHANGES, sourceCRS, targetCRS, matrix);
    }

    /**
     * Creates a coordinate operation from a matrix, which usually describes an affine transform.
     * A default {@link OperationMethod} object is given to this transform. In the special case
     * where the {@code name} identifier is {@link #DATUM_SHIFT} or {@link #ELLIPSOID_CHANGE},
     * the operation will be a {@link Transformation} instance instead of {@link Conversion}.
     *
     * @param  name      The identifier for the operation to be created.
     * @param  sourceCRS The source coordinate reference system.
     * @param  targetCRS The target coordinate reference system.
     * @param  matrix    The matrix which describe an affine transform operation.
     * @return The conversion or transformation.
     * @throws FactoryException if the operation can not be created.
     */
    private CoordinateOperation createFromAffineTransform(final Identifier                name,
                                                          final CoordinateReferenceSystem sourceCRS,
                                                          final CoordinateReferenceSystem targetCRS,
                                                          final Matrix                    matrix)
            throws FactoryException
    {
        final MathTransformFactory mtFactory = factorySIS.getMathTransformFactory();
        final MathTransform transform  = mtFactory.createAffineTransform(matrix);
        final Map<String,?> properties = properties(name);
        final Class<? extends SingleOperation> type =
                properties.containsKey(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY)
                ? Transformation.class : Conversion.class;
        return createFromMathTransform(properties, sourceCRS, targetCRS, transform,
                   Affine.getProvider(transform.getSourceDimensions(),
                                      transform.getTargetDimensions(),
                                      Matrices.isAffine(matrix)), type);
    }

    /**
     * Creates a coordinate operation from a math transform.
     *
     * @param  name       The identifier for the operation to be created.
     * @param  sourceCRS  The source coordinate reference system.
     * @param  targetCRS  The destination coordinate reference system.
     * @param  transform  The math transform.
     * @return A coordinate operation using the specified math transform.
     * @throws FactoryException if the operation can not be constructed.
     */
    private CoordinateOperation createFromMathTransform(final Identifier                name,
                                                        final CoordinateReferenceSystem sourceCRS,
                                                        final CoordinateReferenceSystem targetCRS,
                                                        final MathTransform             transform)
            throws FactoryException
    {
        return createFromMathTransform(Collections.singletonMap(CoordinateOperation.NAME_KEY, name),
                sourceCRS, targetCRS, transform, null, SingleOperation.class);
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
     *   <li>If the given {@code method} is {@code null}, then infer an operation method by inspecting the given transform.
     *       The transform needs to implement the {@link Parameterized} interface in order to allow operation method discovery.</li>
     *
     *   <li>Delegate to {@link DefaultCoordinateOperationFactory#createSingleOperation
     *       DefaultCoordinateOperationFactory.createSingleOperation(…)}.</li>
     * </ul>
     *
     * @param  properties The properties to give to the operation.
     * @param  sourceCRS  The source coordinate reference system.
     * @param  targetCRS  The destination coordinate reference system.
     * @param  transform  The math transform.
     * @param  method     The operation method, or {@code null} if unknown.
     * @param  type       The required super-class (e.g. <code>{@linkplain Transformation}.class</code>).
     * @return A coordinate operation using the specified math transform.
     * @throws FactoryException if the operation can't be constructed.
     */
    private CoordinateOperation createFromMathTransform(final Map<String,?>             properties,
                                                        final CoordinateReferenceSystem sourceCRS,
                                                        final CoordinateReferenceSystem targetCRS,
                                                        final MathTransform             transform,
                                                              OperationMethod           method,
                                                        final Class<? extends CoordinateOperation> type)
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
         * The operation method is mandatory. If the user did not provided one, we need to infer it ourselves.
         * If we fail to infer an OperationMethod, let it to null - the exception will be thrown by the factory.
         */
        if (method == null && transform instanceof Parameterized) {
            final ParameterDescriptorGroup descriptor = ((Parameterized) transform).getParameterDescriptors();
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
        final Map<String,Object> p = new HashMap<>(properties);
        p.put(ReferencingServices.OPERATION_TYPE_KEY, type);
        return factorySIS.createSingleOperation(p, sourceCRS, targetCRS, null, method, transform);
    }

    /**
     * Concatenates two operation steps.
     * The new concatenated operation gets an automatically generated name.
     *
     * <div class="section">Special case</div>
     * If one of the given operation steps performs a change of axis order or units,
     * then that change will be merged with the other operation instead of creating an {@link ConcatenatedOperation}.
     *
     * @param  step1 The first  step, or {@code null} for the identity operation.
     * @param  step2 The second step, or {@code null} for the identity operation.
     * @return A concatenated operation, or {@code null} if all arguments were null.
     * @throws FactoryException if the operation can't be constructed.
     */
    private CoordinateOperation concatenate(final CoordinateOperation step1,
                                            final CoordinateOperation step2)
            throws FactoryException
    {
        if (step1 == null)     return step2;
        if (step2 == null)     return step1;
        if (isIdentity(step1)) return step2;
        if (isIdentity(step2)) return step1;
        final MathTransform mt1 = step1.getMathTransform();
        final MathTransform mt2 = step2.getMathTransform();
        final CoordinateReferenceSystem sourceCRS = step1.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = step2.getTargetCRS();
        CoordinateOperation step = null;
        if (step1.getName() == AXIS_CHANGES && mt1.getSourceDimensions() == mt1.getTargetDimensions()) step = step2;
        if (step2.getName() == AXIS_CHANGES && mt2.getSourceDimensions() == mt2.getTargetDimensions()) step = step1;
        if (step instanceof SingleOperation) {
            /*
             * Applies only on operation in order to avoid merging with PassThroughOperation.
             * Also applies only if the transform to hide has identical source and target
             * dimensions in order to avoid mismatch with the method's dimensions.
             */
            final MathTransformFactory mtFactory = factorySIS.getMathTransformFactory();
            return createFromMathTransform(IdentifiedObjects.getProperties(step),
                   sourceCRS, targetCRS, mtFactory.createConcatenatedTransform(mt1, mt2),
                   ((SingleOperation) step).getMethod(), SingleOperation.class);
        }
        return factory.createConcatenatedOperation(defaultName(sourceCRS, targetCRS), step1, step2);
    }

    /**
     * Returns {@code true} if the specified operation is an identity conversion.
     * This method always returns {@code false} for transformations even if their
     * associated math transform is an identity one, because such transformations
     * are usually datum shift and must be visible.
     */
    private static boolean isIdentity(final CoordinateOperation operation) {
        return (operation instanceof Conversion) && operation.getMathTransform().isIdentity();
    }

    /**
     * Returns {@code true} if the Greenwich longitude of the {@code actual} prime meridian is equals to the
     * Greenwich longitude of the {@code expected} prime meridian. The comparison is performed in degrees.
     *
     * <p>A {@code null} argument is interpreted as "unknown prime meridian". Consequently this method
     * unconditionally returns {@code false} if one or both arguments is {@code null}.</p>
     *
     * @param expected The expected prime meridian, or {@code null}.
     * @param actual The actual prime meridian, or {@code null}.
     * @return {@code true} if both prime meridians have the same Greenwich longitude.
     */
    private static boolean isGreenwichLongitudeEquals(final PrimeMeridian expected, final PrimeMeridian actual) {
        if (expected == null || actual == null) {
            return false;                               // See method javadoc.
        }
        if (expected == actual) {
            return true;
        }
        final double diff = ReferencingUtilities.getGreenwichLongitude(expected, NonSI.DEGREE_ANGLE)
                          - ReferencingUtilities.getGreenwichLongitude(actual,   NonSI.DEGREE_ANGLE);
        return Math.abs(diff) <= Formulas.ANGULAR_TOLERANCE;
    }

    /**
     * Returns an affine transform between two coordinate systems. Only units and axis order
     * (e.g. transforming from (NORTH,WEST) to (EAST,NORTH)) are taken in account.
     *
     * <p>This method delegates to {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)}
     * and wraps the unchecked exceptions into the checked {@link OperationNotFoundException}.</p>
     *
     * @param  sourceCS  the source coordinate system.
     * @param  targetCS  the target coordinate system.
     * @return The transformation from {@code sourceCS} to {@code targetCS} as an affine transform.
     * @throws OperationNotFoundException if the affine transform can't be constructed.
     *
     * @see CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)
     */
    private static Matrix swapAndScaleAxes(final CoordinateSystem sourceCS,
                                           final CoordinateSystem targetCS)
            throws OperationNotFoundException
    {
        try {
            return CoordinateSystems.swapAndScaleAxes(sourceCS,targetCS);
        } catch (IllegalArgumentException | ConversionException exception) {
            throw new OperationNotFoundException(notFoundMessage(sourceCS, targetCS), exception);
        }
        // No attempt to catch ClassCastException since such
        // exception would indicates a programming error.
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
     */
    private static Map<String,Object> properties(final Identifier name) {
        final Map<String,Object> properties;
        if ((name == DATUM_SHIFT) || (name == ELLIPSOID_CHANGE)) {
            properties = new HashMap<>(4);
            properties.put(CoordinateOperation.NAME_KEY, name);
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY, new PositionalAccuracy[] {
                      (name == DATUM_SHIFT) ? PositionalAccuracyConstant.DATUM_SHIFT_APPLIED
                                            : PositionalAccuracyConstant.DATUM_SHIFT_OMITTED});
        } else {
            properties = Collections.singletonMap(CoordinateOperation.NAME_KEY, name);
        }
        return properties;
    }

    /**
     * Returns a name for the given object, truncating it if needed.
     */
    private static String shortName(final IdentifiedObject object) {
        String name = IdentifiedObjects.getName(object, null);
        if (name == null) {
            name = Classes.getShortClassName(object);
        } else {
            int i = 30;                 // Arbitrary length threshold.
            if (name.length() >= i) {
                while (i > 15) {        // Arbitrary minimal length.
                    final int c = name.codePointBefore(i);
                    if (Character.isSpaceChar(c)) break;
                    i -= Character.charCount(c);
                }
                name = CharSequences.trimWhitespaces(name, 0, i).toString() + '…';
            }
        }
        return name;
    }

    /**
     * Returns a temporary name for a transformation between two CRS.
     */
    private static Map<String,?> defaultName(CoordinateReferenceSystem source, CoordinateReferenceSystem target) {
        final String name = shortName(source) + " → " + shortName(target);
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Returns an error message for "No path found from sourceCRS to targetCRS".
     * This is used for the construction of {@link OperationNotFoundException}.
     *
     * @param  source The source CRS.
     * @param  target The target CRS.
     * @return A default error message.
     */
    private static String notFoundMessage(final IdentifiedObject source, final IdentifiedObject target) {
        return Errors.format(Errors.Keys.CoordinateOperationNotFound_2, shortName(source), shortName(target));
    }
}
