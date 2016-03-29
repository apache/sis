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
import javax.measure.quantity.Duration;
import javax.measure.converter.ConversionException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.referencing.provider.GeographicToGeocentric;
import org.apache.sis.internal.referencing.provider.GeocentricToGeographic;
import org.apache.sis.internal.referencing.provider.GeocentricAffine;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.parameter.TensorParameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;

import static org.apache.sis.util.Utilities.equalsIgnoreMetadata;

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
     * The accuracy threshold (in metres) for allowing the use of Molodensky approximation instead than the
     * Geocentric Translation method. The accuracy of datum shifts with Molodensky approximation is about 5
     * or 10 metres. However for this constant, we are not interested in absolute accuracy but rather in the
     * difference between Molodensky and Geocentric Translation methods, which is much lower. We nevertheless
     * use a relatively high threshold as a conservative approach.
     *
     * @see #desiredAccuracy
     */
    private static final double MOLODENSKY_ACCURACY = 5;

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
     * The spatio-temporal area of interest, or {@code null} if none.
     */
    private Extent areaOfInterest;

    /**
     * The geographic component of the area of interest, or {@code null} if none.
     */
    private GeographicBoundingBox bbox;

    /**
     * The desired accuracy in metres, or 0 for the best accuracy available.
     *
     * @see #MOLODENSKY_ACCURACY
     */
    private double desiredAccuracy;

    /**
     * The pair of source and target CRS for which we already searched a coordinate operation.
     * This is used as a safety against infinite recursivity.
     */
    private final Map<CRSPair,Boolean> previousSearches;

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
            bbox            = context.getGeographicBoundingBox();
        }
        previousSearches = new HashMap<>(4);
    }

    /**
     * Infers an operation for conversion or transformation between two coordinate reference systems.
     * This method inspects the given CRS and delegates the work to one or many {@code createOperationStep(…)} methods.
     * Note that some {@code createOperationStep(…)} methods may callback this {@code createOperation(…)} method with
     * another source or target CRS.
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
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        if (!previousSearches.isEmpty()) {
            // TODO: verify here if the path is defined in EPSG database.
        }
        final CRSPair key = new CRSPair(sourceCRS, targetCRS);
        if (previousSearches.put(key, Boolean.TRUE) != null) {
            throw new FactoryException(Errors.format(Errors.Keys.RecursiveCreateCallForCode_2, CoordinateOperation.class, key));
        }
        if (bbox == null) {
            bbox = Extents.intersection(CRS.getGeographicBoundingBox(sourceCRS),
                                        CRS.getGeographicBoundingBox(targetCRS));
            areaOfInterest = CoordinateOperationContext.setGeographicBoundingBox(areaOfInterest, bbox);
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
        ////                       Derived  →  any Single CRS                       ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof GeneralDerivedCRS) {
            final GeneralDerivedCRS source = (GeneralDerivedCRS) sourceCRS;
            if (targetCRS instanceof GeneralDerivedCRS) {
                return createOperationStep(source, (GeneralDerivedCRS) targetCRS);
            }
            if (targetCRS instanceof SingleCRS) {
                return createOperationStep(source, (SingleCRS) targetCRS);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////                       any Single CRS  →  Derived                       ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (targetCRS instanceof GeneralDerivedCRS) {
            final GeneralDerivedCRS target = (GeneralDerivedCRS) targetCRS;
            if (sourceCRS instanceof SingleCRS) {
                return createOperationStep((SingleCRS) sourceCRS, target);
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
        ////                                                                        ////
        ////            Geodetic  →  Geocetric, Geographic or Projected             ////
        ////                                                                        ////
        ////////////////////////////////////////////////////////////////////////////////
        if (sourceCRS instanceof GeodeticCRS) {
            final GeodeticCRS source = (GeodeticCRS) sourceCRS;
            if (targetCRS instanceof GeodeticCRS) {
                return createOperationStep(source, (GeodeticCRS) targetCRS);
            }
            if (targetCRS instanceof VerticalCRS) {
//              return createOperationStep(source, (VerticalCRS) targetCRS);
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
     * Creates an operation from an arbitrary single CRS to a derived coordinate reference system.
     * Conversions from {@code GeographicCRS} to {@code ProjectedCRS} are also handled by this method,
     * since projected CRS are a special kind of {@code GeneralDerivedCRS}.
     *
     * <p>The default implementation constructs the following operation chain:</p>
     * <blockquote><code>sourceCRS  →  {@linkplain GeneralDerivedCRS#getBaseCRS() baseCRS}  →  targetCRS</code></blockquote>
     *
     * where the conversion from {@code baseCRS} to {@code targetCRS} is obtained from
     * <code>targetCRS.{@linkplain GeneralDerivedCRS#getConversionFromBase() getConversionFromBase()}</code>.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be constructed.
     */
    protected CoordinateOperation createOperationStep(final SingleCRS sourceCRS,
                                                      final GeneralDerivedCRS targetCRS)
            throws FactoryException
    {
        final CoordinateOperation step1 = createOperation(sourceCRS, targetCRS.getBaseCRS());
        final CoordinateOperation step2 = targetCRS.getConversionFromBase();
        return concatenate(step1, step2);
    }

    /**
     * Creates an operation from a derived CRS to an arbitrary single coordinate reference system.
     * Conversions from {@code ProjectedCRS} to {@code GeographicCRS} are also handled by this method,
     * since projected CRS are a special kind of {@code GeneralDerivedCRS}.
     *
     * <p>The default implementation constructs the following operation chain:</p>
     * <blockquote><code>sourceCRS  →  {@linkplain GeneralDerivedCRS#getBaseCRS() baseCRS}  →  targetCRS</code></blockquote>
     *
     * where the conversion from {@code sourceCRS} to {@code baseCRS} is obtained from the inverse of
     * <code>sourceCRS.{@linkplain GeneralDerivedCRS#getConversionFromBase() getConversionFromBase()}</code>.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be constructed.
     */
    protected CoordinateOperation createOperationStep(final GeneralDerivedCRS sourceCRS,
                                                      final SingleCRS targetCRS)
            throws FactoryException
    {
        // Create first the operation that is more at risk to fail.
        final CoordinateOperation step2 = createOperation(sourceCRS.getBaseCRS(), targetCRS);
        final CoordinateOperation step1 = inverse(sourceCRS.getConversionFromBase());
        return concatenate(step1, step2);
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
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be constructed.
     */
    protected CoordinateOperation createOperationStep(final GeneralDerivedCRS sourceCRS,
                                                      final GeneralDerivedCRS targetCRS)
            throws FactoryException
    {
        // Create first the operations that are more at risk to fail.
        final CoordinateOperation step2 = createOperation(sourceCRS.getBaseCRS(), targetCRS.getBaseCRS());
        final CoordinateOperation step1 = inverse(sourceCRS.getConversionFromBase());
        final CoordinateOperation step3 = targetCRS.getConversionFromBase();
        return concatenate(step1, step2, step3);
    }

    /**
     * Creates an operation between two geodetic (geographic or geocentric) coordinate reference systems.
     * The default implementation can:
     *
     * <ul>
     *   <li>adjust axis order and orientation, for example converting from (<cite>North</cite>, <cite>West</cite>)
     *       axes to (<cite>East</cite>, <cite>North</cite>) axes,</li>
     *   <li>apply units conversion if needed,</li>
     *   <li>perform longitude rotation if needed,</li>
     *   <li>perform datum shift if {@linkplain BursaWolfParameters Bursa-Wolf parameters} are available
     *       for the area of interest.</li>
     * </ul>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be constructed.
     */
    protected CoordinateOperation createOperationStep(final GeodeticCRS sourceCRS,
                                                      final GeodeticCRS targetCRS)
            throws FactoryException
    {
        final GeodeticDatum sourceDatum = sourceCRS.getDatum();
        final GeodeticDatum targetDatum = targetCRS.getDatum();
        Matrix datumShift = null;
        /*
         * If the prime meridian is not the same, we will concatenate a longitude rotation before or after datum shift
         * (that concatenation will be performed by the customized DefaultMathTransformFactory.Context created below).
         * Actually we do not know if the longitude rotation should be before or after datum shift. But this ambiguity
         * can usually be ignored because Bursa-Wolf parameters are always used with source and target prime meridians
         * set to Greenwich in EPSG dataset 8.9.  For safety, the SIS's DefaultGeodeticDatum class ensures that if the
         * prime meridian are not the same, then the target meridian must be Greenwich.
         */
        final DefaultMathTransformFactory.Context context = ReferencingUtilities.createTransformContext(
                sourceCRS, targetCRS, new MathTransformContext(sourceDatum, targetDatum));
        /*
         * If both CRS use the same datum and the same prime meridian, then the coordinate operation is only axis
         * swapping, unit conversion or change of coordinate system type (Ellipsoidal ↔ Cartesian ↔ Spherical).
         * Otherwise (if the datum are not the same), we will need to perform a scale, translation and rotation
         * in Cartesian space using the Bursa-Wolf parameters. If the user does not require the best accuracy,
         * then the Molodensky approximation may be used for avoiding the conversion step to geocentric CRS.
         */
        Identifier identifier;
        boolean isGeographicToGeocentric = false;
        final CoordinateSystem sourceCS = context.getSourceCS();
        final CoordinateSystem targetCS = context.getTargetCS();
        if (equalsIgnoreMetadata(sourceDatum, targetDatum)) {
            final boolean isGeocentricToGeographic;
            isGeographicToGeocentric = (sourceCS instanceof EllipsoidalCS && targetCS instanceof CartesianCS);
            isGeocentricToGeographic = (sourceCS instanceof CartesianCS && targetCS instanceof EllipsoidalCS);
            /*
             * Above booleans should never be true in same time. If it nevertheless happen (we are paranoiac;
             * maybe a lazy user implemented all interfaces in a single class), do not apply any geographic ↔
             * geocentric conversion. Instead do as if the coordinate system types were the same.
             */
            if (isGeocentricToGeographic ^ isGeographicToGeocentric) {
                identifier = GEOCENTRIC_CONVERSION;
            } else {
                identifier = AXIS_CHANGES;
            }
        } else {
            identifier = ELLIPSOID_CHANGE;
            if (sourceDatum instanceof DefaultGeodeticDatum) {
                datumShift = ((DefaultGeodeticDatum) sourceDatum).getPositionVectorTransformation(targetDatum, areaOfInterest);
                if (datumShift != null) {
                    identifier = DATUM_SHIFT;
                }
            }
        }
        /*
         * Conceptually, all transformations below could done by first converting from the source coordinate
         * system to geocentric Cartesian coordinates (X,Y,Z), apply an affine transform represented by the
         * datum shift matrix, then convert from the (X′,Y′,Z′) coordinates to the target coordinate system.
         * However there is two exceptions to this path:
         *
         *   1) In the particular where both the source and target CS are ellipsoidal, we may use the
         *      Molodensky approximation as a shortcut (if the desired accuracy allows).
         *
         *   2) Even if we really go through the XYZ coordinates without Molodensky approximation, there is
         *      at least 9 different ways to name this operation depending on whether the source and target
         *      CRS are geocentric or geographic, 2- or 3-dimensional, whether there is a translation or not,
         *      the rotation sign, etc. We try to use the most specific name if we can find one, and fallback
         *      on an arbitrary name only in last resort.
         */
        final DefaultMathTransformFactory mtFactory = factorySIS.getDefaultMathTransformFactory();
        MathTransform before = null, after = null;
        ParameterValueGroup parameters;
        if (datumShift != null) {
            /*
             * If the transform can be represented by a single coordinate operation, returns that operation.
             * Possible operations are:
             *
             *    - Geocentric translation         (in geocentric, geographic-2D or geographic-3D domains)
             *    - Position Vector transformation (in geocentric, geographic-2D or geographic-3D domains)
             *
             * Otherwise, maybe we failed to create the operation because the coordinate system type were not the same.
             * Convert unconditionally to XYZ geocentric coordinates and apply the datum shift in that coordinate space.
             */
            parameters = GeocentricAffine.createParameters(sourceCS, targetCS, datumShift, desiredAccuracy >= MOLODENSKY_ACCURACY);
            if (parameters == null) {
                parameters = TensorParameters.WKT1.createValueGroup(properties(Constants.AFFINE), datumShift);
                final CoordinateSystem normalized = CommonCRS.WGS84.geocentric().getCoordinateSystem();
                before = mtFactory.createCoordinateSystemChange(sourceCS, normalized);
                after  = mtFactory.createCoordinateSystemChange(normalized, targetCS);
                context.setSource(normalized);
                context.setTarget(normalized);
            }
        } else if (identifier == GEOCENTRIC_CONVERSION) {
            parameters = (isGeographicToGeocentric ? GeographicToGeocentric.PARAMETERS
                                                   : GeocentricToGeographic.PARAMETERS).createValue();
        } else {
            parameters = TensorParameters.WKT1.createValueGroup(properties(Constants.AFFINE));  // Initialized to identity.
            parameters.parameter(Constants.NUM_COL).setValue(sourceCS.getDimension() + 1);
            parameters.parameter(Constants.NUM_ROW).setValue(targetCS.getDimension() + 1);
            before = mtFactory.createCoordinateSystemChange(sourceCS, targetCS);
            context.setSource(targetCS);
        }
        /*
         * Transform between differents datums using Bursa Wolf parameters. The Bursa Wolf parameters are used
         * with "standard" geocentric CS, i.e. with X axis towards the prime meridian, Y axis towards East and
         * Z axis toward North, unless the Molodensky approximation is used. The following steps are applied:
         *
         *     source CRS                        →
         *     normalized CRS with source datum  →
         *     normalized CRS with target datum  →
         *     target CRS
         *
         * Those steps may be either explicit with the 'before' and 'after' transform, or implicit with the
         * Context parameter.
         */
        MathTransform transform = mtFactory.createParameterizedTransform(parameters, context);
        final OperationMethod method = mtFactory.getLastMethodUsed();
        if (before != null) {
            transform = mtFactory.createConcatenatedTransform(before, transform);
            if (after != null) {
                transform = mtFactory.createConcatenatedTransform(transform, after);
            }
        }
        return createFromMathTransform(properties(identifier), sourceCRS, targetCRS, transform, method, null);
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
        if (!equalsIgnoreMetadata(sourceDatum, targetDatum)) {
            throw new OperationNotFoundException(notFoundMessage(sourceDatum, targetDatum));
        }
        final VerticalCS sourceCS = sourceCRS.getCoordinateSystem();
        final VerticalCS targetCS = targetCRS.getCoordinateSystem();
        final Matrix matrix;
        try {
            matrix = CoordinateSystems.swapAndScaleAxes(sourceCS, targetCS);
        } catch (IllegalArgumentException | ConversionException exception) {
            throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS), exception);
        }
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
        final Matrix matrix;
        try {
            matrix = CoordinateSystems.swapAndScaleAxes(sourceCS, targetCS);
        } catch (IllegalArgumentException | ConversionException exception) {
            throw new OperationNotFoundException(notFoundMessage(sourceCRS, targetCRS), exception);
        }
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
        final MathTransform transform  = factorySIS.getMathTransformFactory().createAffineTransform(matrix);
        return createFromMathTransform(properties(name), sourceCRS, targetCRS, transform, null, null);
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
     * @param  type       {@code Conversion.class}, {@code Transformation.class}, or {@code null} if unknown.
     * @return A coordinate operation using the specified math transform.
     * @throws FactoryException if the operation can not be created.
     */
    private CoordinateOperation createFromMathTransform(final Map<String,Object>        properties,
                                                        final CoordinateReferenceSystem sourceCRS,
                                                        final CoordinateReferenceSystem targetCRS,
                                                        final MathTransform             transform,
                                                              OperationMethod           method,
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
        properties.put(ReferencingServices.OPERATION_TYPE_KEY, type);
        if (Conversion.class.isAssignableFrom(type)) {
            properties.replace(IdentifiedObject.NAME_KEY, AXIS_CHANGES, IDENTITY);
        }
        return factorySIS.createSingleOperation(properties, sourceCRS, targetCRS, null, method, transform);
    }

    /**
     * Creates the inverse of the given operation.
     */
    private CoordinateOperation inverse(final SingleOperation op) throws FactoryException {
        final CoordinateReferenceSystem sourceCRS = op.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = op.getTargetCRS();
        MathTransform transform = op.getMathTransform();
        try {
            transform = transform.inverse();
        } catch (NoninvertibleTransformException exception) {
            throw new OperationNotFoundException(notFoundMessage(targetCRS, sourceCRS), exception);
        }
        return createFromMathTransform(properties(INVERSE_OPERATION), targetCRS, sourceCRS,
                transform, InverseOperationMethod.create(op.getMethod()), null);
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
        final MathTransform mt1 = step1.getMathTransform();
        final MathTransform mt2 = step2.getMathTransform();
        if (step1 instanceof Conversion && mt1.isIdentity()) return step2;
        if (step2 instanceof Conversion && mt2.isIdentity()) return step1;
        final CoordinateReferenceSystem sourceCRS = step1.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = step2.getTargetCRS();
        /*
         * If one of the transform performs nothing more than a change of axis order or units, do
         * not expose that conversion in a ConcatenatedTransform.  Instead, merge that conversion
         * with the "main" operation. The intend is to simplify the operation chain by hidding
         * trivial operations.
         */
        CoordinateOperation main = null;
        if (step1.getName() == AXIS_CHANGES && mt1.getSourceDimensions() == mt1.getTargetDimensions()) main = step2;
        if (step2.getName() == AXIS_CHANGES && mt2.getSourceDimensions() == mt2.getTargetDimensions()) main = step1;
        if (main instanceof SingleOperation) {
            final MathTransform mt = factorySIS.getMathTransformFactory().createConcatenatedTransform(mt1, mt2);
            main = createFromMathTransform(new HashMap<>(IdentifiedObjects.getProperties(main)),
                   sourceCRS, targetCRS, mt, ((SingleOperation) main).getMethod(),
                   (main instanceof Transformation) ? Transformation.class : SingleOperation.class);
        } else {
            main = factory.createConcatenatedOperation(defaultName(sourceCRS, targetCRS), step1, step2);
        }
        /*
         * Sometime we get a concatenated operation made of an operation followed by its inverse.
         * We can identity those case when the associated MathTransform is the identity transform.
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
                    main.getSourceCRS(), main.getTargetCRS(), main.getMathTransform(), null, type);
        }
        return main;
    }

    /**
     * Concatenates three transformation steps. If the first and/or the last operation is an {@link #AXIS_CHANGES},
     * then it will be included as part of the second operation instead of creating a {@link ConcatenatedOperation}.
     * If a concatenated operation is created, it will get an automatically generated name.
     *
     * @param  step1 The first  step, or {@code null} for the identity operation.
     * @param  step2 The second step, or {@code null} for the identity operation.
     * @param  step3 The third  step, or {@code null} for the identity operation.
     * @return A concatenated operation, or {@code null} if all arguments were null.
     * @throws FactoryException if the operation can not be constructed.
     */
    protected CoordinateOperation concatenate(final CoordinateOperation step1,
                                              final CoordinateOperation step2,
                                              final CoordinateOperation step3)
            throws FactoryException
    {
        if (isIdentity(step1)) return concatenate(step2, step3);
        if (isIdentity(step2)) return concatenate(step1, step3);
        if (isIdentity(step3)) return concatenate(step1, step2);
        if (step1.getName() == AXIS_CHANGES) return concatenate(concatenate(step1, step2), step3);
        if (step3.getName() == AXIS_CHANGES) return concatenate(step1, concatenate(step2, step3));
        final Map<String,?> properties = defaultName(step1.getSourceCRS(), step3.getTargetCRS());
        return factory.createConcatenatedOperation(properties, step1, step2, step3);
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
    private static Map<String,Object> properties(final Identifier name) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(CoordinateOperation.NAME_KEY, name);
        if ((name == DATUM_SHIFT) || (name == ELLIPSOID_CHANGE)) {
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY, new PositionalAccuracy[] {
                      (name == DATUM_SHIFT) ? PositionalAccuracyConstant.DATUM_SHIFT_APPLIED
                                            : PositionalAccuracyConstant.DATUM_SHIFT_OMITTED});
        }
        return properties;
    }

    /**
     * Returns the given name in a singleton map.
     */
    private static Map<String,?> properties(final String name) {
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Returns a name for a transformation between two CRS.
     */
    private static Map<String,?> defaultName(CoordinateReferenceSystem source, CoordinateReferenceSystem target) {
        return properties(CRSPair.shortName(source) + " → " + CRSPair.shortName(target));
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
        return Errors.format(Errors.Keys.CoordinateOperationNotFound_2, CRSPair.shortName(source), CRSPair.shortName(target));
    }
}
