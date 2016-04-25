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
import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.system.Semaphores;
import org.apache.sis.internal.system.Loggers;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Describes the operation for transforming coordinates in the source CRS to coordinates in the target CRS.
 * Coordinate operations contain a {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform
 * math transform}, which does the actual work of transforming coordinates, together with the following information:
 *
 * <ul>
 *   <li>The {@linkplain #getSourceCRS() source} and {@linkplain #getTargetCRS() target CRS}.</li>
 *   <li>The {@linkplain #getInterpolationCRS() interpolation CRS} if a CRS other than source and target is needed
 *       for interpolating.</li>
 *   <li>In {@linkplain DefaultConversion conversion} and {@linkplain DefaultTransformation transformation} subclasses,
 *       a description of the {@linkplain DefaultOperationMethod operation method} together with the parameter values.</li>
 *   <li>The {@linkplain #getDomainOfValidity() domain of validity}.</li>
 *   <li>An estimation of the {@linkplain #getCoordinateOperationAccuracy() operation accuracy}.</li>
 * </ul>
 *
 * <div class="section">Instantiation</div>
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 * An exception to this rule may occur when it is not possible to identify the exact operation type.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code CoordinateOperation} instances created
 * using only SIS factories and static constants can be shared by many objects and passed between threads without
 * synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@XmlType(name = "AbstractCoordinateOperationType", propOrder = {
    "domainOfValidity",
    "scope",
    "operationVersion",
    "accuracy",
    "source",
    "target"
})
@XmlRootElement(name = "AbstractCoordinateOperation")
@XmlSeeAlso({
    AbstractSingleOperation.class,
    DefaultPassThroughOperation.class,
    DefaultConcatenatedOperation.class
})
public class AbstractCoordinateOperation extends AbstractIdentifiedObject implements CoordinateOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1237358357729193885L;

    /**
     * The source CRS, or {@code null} if not available.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors and for initialization
     * at XML unmarshalling time by {@link #setSource(CoordinateReferenceSystem)}.</p>
     *
     * @see #getSourceCRS()
     */
    CoordinateReferenceSystem sourceCRS;

    /**
     * The target CRS, or {@code null} if not available.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors and for initialization
     * at XML unmarshalling time by {@link #setTarget(CoordinateReferenceSystem)}.</p>
     *
     * @see #getTargetCRS()
     */
    CoordinateReferenceSystem targetCRS;

    /**
     * The CRS which is neither the {@linkplain #getSourceCRS() source CRS} or
     * {@linkplain #getTargetCRS() target CRS} but still required for performing the operation.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors.</p>
     *
     * @see #getInterpolationCRS()
     */
    private CoordinateReferenceSystem interpolationCRS;

    /**
     * Version of the coordinate transformation
     * (i.e., instantiation due to the stochastic nature of the parameters).
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setOperationVersion(String)}.</p>
     *
     * @see #getOperationVersion()
     */
    private String operationVersion;

    /**
     * Estimate(s) of the impact of this operation on point accuracy, or {@code null} if none.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors and for initialization
     * at XML unmarshalling time by {@link #setAccuracy(PositionalAccuracy[])}.</p>
     *
     * @see #getCoordinateOperationAccuracy()
     */
    Collection<PositionalAccuracy> coordinateOperationAccuracy;

    /**
     * Area in which this operation is valid, or {@code null} if not available.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDomainOfValidity(Extent)}.</p>
     *
     * @see #getDomainOfValidity()
     */
    private Extent domainOfValidity;

    /**
     * Description of domain of usage, or limitations of usage, for which this operation is valid.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setScope(InternationalString)}.</p>
     *
     * @see #getScope()
     */
    private InternationalString scope;

    /**
     * Transform from positions in the {@linkplain #getSourceCRS source coordinate reference system}
     * to positions in the {@linkplain #getTargetCRS target coordinate reference system}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors and for initialization
     * at XML unmarshalling time by {@link AbstractSingleOperation#afterUnmarshal(Unmarshaller, Object)}.</p>
     */
    MathTransform transform;

    /**
     * Creates a new coordinate operation initialized from the given properties.
     * It is caller's responsibility to:
     *
     * <ul>
     *   <li>Set the following fields:<ul>
     *     <li>{@link #sourceCRS}</li>
     *     <li>{@link #targetCRS}</li>
     *     <li>{@link #transform}</li>
     *   </ul></li>
     *   <li>Invoke {@link #checkDimensions(Map)} after the above-cited fields have been set.</li>
     * </ul>
     */
    AbstractCoordinateOperation(final Map<String,?> properties) {
        super(properties);
        this.scope            = Types.toInternationalString(properties, SCOPE_KEY);
        this.domainOfValidity = Containers.property(properties, DOMAIN_OF_VALIDITY_KEY, Extent.class);
        this.operationVersion = Containers.property(properties, OPERATION_VERSION_KEY, String.class);
        Object value = properties.get(COORDINATE_OPERATION_ACCURACY_KEY);
        if (value != null) {
            if (value instanceof PositionalAccuracy[]) {
                coordinateOperationAccuracy = CollectionsExt.nonEmptySet((PositionalAccuracy[]) value);
            } else {
                coordinateOperationAccuracy = Collections.singleton((PositionalAccuracy) value);
            }
        }
    }

    /**
     * Creates a coordinate operation from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#OPERATION_VERSION_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getOperationVersion()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#COORDINATE_OPERATION_ACCURACY_KEY}</td>
     *     <td>{@link PositionalAccuracy} (optionally as array)</td>
     *     <td>{@link #getCoordinateOperationAccuracy()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#SCOPE_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     *   <tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * <div class="section">Constraints</div>
     * All arguments except {@code properties} can be {@code null}.
     * If non-null, the dimension of CRS arguments shall be related to the {@code transform} argument as below:
     *
     * <ul>
     *   <li>Dimension of {@code sourceCRS} shall be equal to the transform
     *     {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#getSourceDimensions()
     *     source dimension} minus the dimension of the {@code interpolationCRS} (if any).</li>
     *   <li>Dimension of {@code targetCRS} shall be equal to the transform
     *     {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#getTargetDimensions()
     *     target dimension}, minus the dimension of the {@code interpolationCRS} (if any).</li>
     * </ul>
     *
     * If the {@code interpolationCRS} is non-null, then the given {@code transform} shall expect input ordinates
     * in the following order:
     *
     * <ol>
     *   <li>Ordinates of the interpolation CRS. Example: (<var>x</var>,<var>y</var>) in a vertical transform.</li>
     *   <li>Ordinates of the source CRS. Example: (<var>z</var>) in a vertical transform.</li>
     * </ol>
     *
     * The math transform shall let the interpolation coordinates {@linkplain DefaultPassThroughOperation pass through
     * the operation}.
     *
     * @param properties The properties to be given to the identified object.
     * @param sourceCRS  The source CRS, or {@code null} if unspecified.
     * @param targetCRS  The target CRS, or {@code null} if unspecified.
     * @param interpolationCRS The CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param transform  Transform from positions in the source CRS to positions in the target CRS,
     *                   or {@code null} if unspecified.
     */
    public AbstractCoordinateOperation(final Map<String,?>             properties,
                                       final CoordinateReferenceSystem sourceCRS,
                                       final CoordinateReferenceSystem targetCRS,
                                       final CoordinateReferenceSystem interpolationCRS,
                                       final MathTransform             transform)
    {
        this(properties);
        this.sourceCRS        = sourceCRS;
        this.targetCRS        = targetCRS;
        this.interpolationCRS = interpolationCRS;
        this.transform        = transform;
        checkDimensions(properties);
    }

    /**
     * Ensures that {@link #sourceCRS}, {@link #targetCRS} and {@link #interpolationCRS} dimensions
     * are consistent with {@link #transform} input and output dimensions.
     */
    final void checkDimensions(final Map<String,?> properties) {
        final MathTransform transform = this.transform;   // Protect from changes.
        if (transform != null) {
            final int interpDim = ReferencingUtilities.getDimension(interpolationCRS);
check:      for (int isTarget=0; ; isTarget++) {        // 0 == source check; 1 == target check.
                final CoordinateReferenceSystem crs;    // Will determine the expected dimensions.
                int actual;                             // The MathTransform number of dimensions.
                switch (isTarget) {
                    case 0: crs = sourceCRS; actual = transform.getSourceDimensions(); break;
                    case 1: crs = targetCRS; actual = transform.getTargetDimensions(); break;
                    default: break check;
                }
                int expected = ReferencingUtilities.getDimension(crs);
                if (interpDim != 0) {
                    if (actual == expected || actual < interpDim) {
                        // This check is not strictly necessary as the next check below would catch the error,
                        // but we provide here a hopefully more helpful error message for a common mistake.
                        throw new IllegalArgumentException(Errors.getResources(properties)
                                .getString(Errors.Keys.MissingInterpolationOrdinates));
                    }
                    expected += interpDim;
                }
                if (crs != null && actual != expected) {
                    throw new IllegalArgumentException(Errors.getResources(properties).getString(
                            Errors.Keys.MismatchedTransformDimension_3, isTarget, expected, actual));
                }
            }
        }
    }

    /**
     * Creates a new coordinate operation with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param operation The coordinate operation to copy.
     *
     * @see #castOrCopy(CoordinateOperation)
     */
    protected AbstractCoordinateOperation(final CoordinateOperation operation) {
        super(operation);
        sourceCRS                   = operation.getSourceCRS();
        targetCRS                   = operation.getTargetCRS();
        interpolationCRS            = getInterpolationCRS(operation);
        operationVersion            = operation.getOperationVersion();
        coordinateOperationAccuracy = operation.getCoordinateOperationAccuracy();
        domainOfValidity            = operation.getDomainOfValidity();
        scope                       = operation.getScope();
        transform                   = operation.getMathTransform();
    }

    /**
     * Returns a SIS coordinate operation implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of
     *       {@link org.opengis.referencing.operation.Transformation},
     *       {@link org.opengis.referencing.operation.Conversion},
     *       {@link org.opengis.referencing.operation.Projection},
     *       {@link org.opengis.referencing.operation.CylindricalProjection},
     *       {@link org.opengis.referencing.operation.ConicProjection},
     *       {@link org.opengis.referencing.operation.PlanarProjection},
     *       {@link org.opengis.referencing.operation.PassThroughOperation} or
     *       {@link org.opengis.referencing.operation.ConcatenatedOperation},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractCoordinateOperation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractCoordinateOperation} instance is created using the
     *       {@linkplain #AbstractCoordinateOperation(CoordinateOperation) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractCoordinateOperation castOrCopy(final CoordinateOperation object) {
        return SubTypes.castOrCopy(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code CoordinateOperation.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The coordinate operation interface implemented by this class.
     */
    @Override
    public Class<? extends CoordinateOperation> getInterface() {
        return CoordinateOperation.class;
    }

    /**
     * Returns {@code true} if this coordinate operation is for the definition of a
     * {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS derived} or
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
     * The standard (ISO 19111) approach constructs <cite>defining conversion</cite>
     * as an operation of type {@link org.opengis.referencing.operation.Conversion}
     * with null {@linkplain #getSourceCRS() source} and {@linkplain #getTargetCRS() target CRS}.
     * But SIS supports also defining conversions with non-null CRS provided that:
     *
     * <ul>
     *   <li>{@link GeneralDerivedCRS#getBaseCRS()} is the {@linkplain #getSourceCRS() source CRS} of this operation, and</li>
     *   <li>{@link GeneralDerivedCRS#getConversionFromBase()} is this operation instance.</li>
     * </ul>
     *
     * When this method returns {@code true}, the source and target CRS are not marshalled in XML documents.
     *
     * @return {@code true} if this coordinate operation is for the definition of a derived or projected CRS.
     */
    public boolean isDefiningConversion() {
        /*
         * Trick: we do not need to verify if (this instanceof Conversion) because:
         *   - Only DefaultConversion constructor accepts null source and target CRS.
         *   - GeneralDerivedCRS.getConversionFromBase() return type is Conversion.
         */
        return (sourceCRS == null && targetCRS == null)
               || ((targetCRS instanceof GeneralDerivedCRS)
                    && ((GeneralDerivedCRS) targetCRS).getBaseCRS() == sourceCRS
                    && ((GeneralDerivedCRS) targetCRS).getConversionFromBase() == this);
    }

    /**
     * Returns the source CRS, or {@code null} if unspecified.
     * The source CRS is mandatory for {@linkplain DefaultTransformation transformations} only.
     * This information is optional for {@linkplain DefaultConversion conversions} according
     * the ISO 19111 standard, but Apache SIS tries to provide that CRS in most cases anyway.
     *
     * @return The source CRS, or {@code null} if not available.
     */
    @Override
    public CoordinateReferenceSystem getSourceCRS() {
        return sourceCRS;
    }

    /**
     * Returns the target CRS, or {@code null} if unspecified.
     * The target CRS is mandatory for {@linkplain DefaultTransformation transformations} only.
     * This information is optional for {@linkplain DefaultConversion conversions} according
     * the ISO 19111 standard, but Apache SIS tries to provide that CRS in most cases anyway.
     *
     * @return The target CRS, or {@code null} if not available.
     */
    @Override
    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    /**
     * Returns the CRS which is neither the {@linkplain #getSourceCRS() source CRS} or
     * {@linkplain #getTargetCRS() target CRS} but still required for performing the operation.
     *
     * <div class="note"><b>Example:</b>
     * some transformations of vertical coordinates (<var>h</var>) require the horizontal coordinates (φ,λ)
     * in order to interpolate in a grid. This method returns the CRS of the grid where such interpolations
     * are performed.</div>
     *
     * @return The CRS (neither source or target CRS) required for interpolating the values, or {@code null} if none.
     */
    public CoordinateReferenceSystem getInterpolationCRS() {
        return interpolationCRS;
    }

    /**
     * Returns the interpolation CRS of the given coordinate operation, or {@code null} if none.
     */
    static CoordinateReferenceSystem getInterpolationCRS(final CoordinateOperation operation) {
        return (operation instanceof AbstractCoordinateOperation)
               ? ((AbstractCoordinateOperation) operation).getInterpolationCRS() : null;
    }

    /**
     * Returns the version of the coordinate operation. Different versions of a coordinate
     * {@linkplain DefaultTransformation transformation} may exist because of the stochastic
     * nature of the parameters. In principle this property is irrelevant to coordinate
     * {@linkplain DefaultConversion conversions}, but Apache SIS accepts it anyway.
     *
     * @return The coordinate operation version, or {@code null} in none.
     */
    @Override
    @XmlElement(name = "operationVersion")
    public String getOperationVersion() {
        return operationVersion;
    }

    /**
     * Returns an estimation of the impact of this operation on point accuracy.
     * The positional accuracy gives position error estimates for target coordinates
     * of this coordinate operation, assuming no errors in source coordinates.
     *
     * @return The position error estimations, or an empty collection if not available.
     *
     * @see #getLinearAccuracy()
     */
    @Override
    public Collection<PositionalAccuracy> getCoordinateOperationAccuracy() {
        return CollectionsExt.nonNull(coordinateOperationAccuracy);
    }

    /**
     * Returns an estimation of positional accuracy in metres, or {@code NaN} if unknown.
     * The default implementation tries to infer a value from the metadata returned by
     * {@link #getCoordinateOperationAccuracy()} using SIS-specific heuristics.
     *
     * <div class="section">Current implementation</div>
     * The current implementation uses the heuristic rules listed below.
     * Note that those rules may change in any future SIS version.
     *
     * <ul>
     *   <li>If at least one {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult quantitative
     *       result} is found with a linear unit, then returns the largest result value converted to metres.</li>
     *
     *   <li>Otherwise if the operation is a {@linkplain DefaultConversion conversion},
     *       then returns 0 since a conversion is by definition accurate up to rounding errors.</li>
     *
     *   <li>Otherwise if the operation is a {@linkplain DefaultTransformation transformation},
     *       then checks if the datum shift were applied with the help of Bursa-Wolf parameters.
     *       If a datum shift has been applied, returns 25 meters.
     *       If a datum shift should have been applied but has been omitted, returns 3000 meters.
     *
     *       <div class="note"><b>Note:</b>
     *       the 3000 meters value is higher than the highest value (999 meters) found in the EPSG
     *       database version 6.7. The 25 meters value is the next highest value found in the EPSG
     *       database for a significant number of transformations.</div>
     *
     *   <li>Otherwise if the operation is a {@linkplain DefaultConcatenatedOperation concatenated operation},
     *       returns the sum of the accuracy of all components.
     *       This is a conservative scenario where we assume that errors cumulate linearly.
     *
     *       <div class="note"><b>Note:</b>
     *       this is not necessarily the "worst case" scenario since the accuracy could be worst
     *       if the math transforms are highly non-linear.</div></li>
     * </ul>
     *
     * @return The accuracy estimation (always in meters), or NaN if unknown.
     *
     * @see org.apache.sis.referencing.CRS#getLinearAccuracy(CoordinateOperation)
     */
    public double getLinearAccuracy() {
        return PositionalAccuracyConstant.getLinearAccuracy(this);
    }

    /**
     * Returns the area or region or timeframe in which this coordinate operation is valid.
     *
     * @return The coordinate operation valid domain, or {@code null} if not available.
     */
    @Override
    @XmlElement(name = "domainOfValidity")
    public Extent getDomainOfValidity() {
        return domainOfValidity;
    }

    /**
     * Returns a description of domain of usage, or limitations of usage, for which this operation is valid.
     *
     * @return A description of domain of usage, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "scope", required = true)
    public InternationalString getScope() {
        return scope;
    }

    /**
     * Returns the object for transforming coordinates in the {@linkplain #getSourceCRS() source CRS}
     * to coordinates in the {@linkplain #getTargetCRS() target CRS}.
     *
     * <div class="section">Use with interpolation CRS</div>
     * If the {@linkplain #getInterpolationCRS() interpolation CRS} is non-null, then the math transform
     * input coordinates shall by (<var>interpolation</var>, <var>source</var>) tuples: for each value
     * to transform, the interpolation point ordinates shall be first, followed by the source coordinates.
     *
     * <div class="note"><b>Example:</b>
     * in a transformation between two {@linkplain org.apache.sis.referencing.crs.DefaultVerticalCRS vertical CRS},
     * if the {@linkplain #getSourceCRS() source} coordinates are (<var>z</var>) values but the coordinate operation
     * additionally requires (<var>x</var>,<var>y</var>) values for {@linkplain #getInterpolationCRS() interpolation}
     * purpose, then the math transform input coordinates shall be (<var>x</var>,<var>y</var>,<var>z</var>) tuples in
     * that order.</div>
     *
     * The interpolation coordinates will {@linkplain DefaultPassThroughOperation pass through the operation}
     * and appear in the math transform outputs, in the same order than inputs.
     *
     * @return The transform from source to target CRS, or {@code null} if not applicable.
     */
    @Override
    public MathTransform getMathTransform() {
        return transform;
    }

    /**
     * Returns the operation method. This apply only to {@link AbstractSingleOperation} subclasses,
     * which will make this method public.
     *
     * @return The operation method, or {@code null} if none.
     */
    OperationMethod getMethod() {
        return null;
    }

    /**
     * Returns the parameter descriptor. The default implementation infers the descriptor from the
     * {@linkplain #transform}, if possible. If no descriptor can be inferred from the math transform,
     * then this method fallback on the {@link OperationMethod} parameters.
     */
    ParameterDescriptorGroup getParameterDescriptors() {
        ParameterDescriptorGroup descriptor = getParameterDescriptors(transform);
        if (descriptor == null) {
            final OperationMethod method = getMethod();
            if (method != null) {
                descriptor = method.getParameters();
            }
        }
        return descriptor;
    }

    /**
     * Returns the parameter descriptors for the given transform, or {@code null} if unknown.
     */
    static ParameterDescriptorGroup getParameterDescriptors(MathTransform transform) {
        while (transform != null) {
            if (transform instanceof Parameterized) {
                final ParameterDescriptorGroup param;
                if (Semaphores.queryAndSet(Semaphores.ENCLOSED_IN_OPERATION)) {
                    throw new AssertionError();                                     // Should never happen.
                }
                try {
                    param = ((Parameterized) transform).getParameterDescriptors();
                } finally {
                    Semaphores.clear(Semaphores.ENCLOSED_IN_OPERATION);
                }
                if (param != null) {
                    return param;
                }
            }
            if (transform instanceof PassThroughTransform) {
                transform = ((PassThroughTransform) transform).getSubTransform();
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * Returns the parameter values. The default implementation infers the
     * parameter values from the {@linkplain #transform}, if possible.
     *
     * @return The parameter values (never {@code null}).
     * @throws UnsupportedOperationException if the parameter values can not
     *         be determined for the current math transform implementation.
     */
    ParameterValueGroup getParameterValues() throws UnsupportedOperationException {
        MathTransform mt = transform;
        while (mt != null) {
            if (mt instanceof Parameterized) {
                final ParameterValueGroup param;
                if (Semaphores.queryAndSet(Semaphores.ENCLOSED_IN_OPERATION)) {
                    throw new AssertionError();                                     // Should never happen.
                }
                try {
                    param = ((Parameterized) mt).getParameterValues();
                } finally {
                    Semaphores.clear(Semaphores.ENCLOSED_IN_OPERATION);
                }
                if (param != null) {
                    return param;
                }
            }
            if (mt instanceof PassThroughTransform) {
                mt = ((PassThroughTransform) mt).getSubTransform();
            } else {
                break;
            }
        }
        throw new UnsupportedImplementationException(Classes.getClass(mt));
    }

    /**
     * Compares this coordinate operation with the specified object for equality. If the {@code mode} argument
     * is {@link ComparisonMode#STRICT} or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available
     * properties are compared including the {@linkplain #getDomainOfValidity() domain of validity} and the
     * {@linkplain #getScope() scope}.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for ignoring properties
     *         that do not make a difference in the numerical results of coordinate operations.
     * @return {@code true} if both objects are equal for the given comparison mode.
     */
    @Override
    public boolean equals(final Object object, ComparisonMode mode) {
        if (super.equals(object, mode)) {
            if (mode == ComparisonMode.STRICT) {
                final AbstractCoordinateOperation that = (AbstractCoordinateOperation) object;
                if (Objects.equals(sourceCRS,                   that.sourceCRS)        &&
                    Objects.equals(interpolationCRS,            that.interpolationCRS) &&
                    Objects.equals(transform,                   that.transform)        &&
                    Objects.equals(scope,                       that.scope)            &&
                    Objects.equals(domainOfValidity,            that.domainOfValidity) &&
                    Objects.equals(coordinateOperationAccuracy, that.coordinateOperationAccuracy))
                {
                    // Check against never-ending recursivity with DerivedCRS.
                    if (Semaphores.queryAndSet(Semaphores.CONVERSION_AND_CRS)) {
                        return true;
                    } else try {
                        return Objects.equals(targetCRS, that.targetCRS);
                    } finally {
                        Semaphores.clear(Semaphores.CONVERSION_AND_CRS);
                    }
                }
            } else {
                /*
                 * This block is for all ComparisonModes other than STRICT. At this point we know that the metadata
                 * properties (class, name, identifiers, etc.) match the criterion of the given comparison mode.
                 * Before to continue perform the following checks:
                 *
                 *   - Scope, domain and accuracy properties only if NOT in "ignore metadata" mode.
                 *   - Interpolation CRS in all cases (regardless if ignoring metadata or not).
                 */
                final CoordinateOperation that = (CoordinateOperation) object;
                if ((mode.isIgnoringMetadata() ||
                    (deepEquals(getScope(),                       that.getScope(), mode) &&
                     deepEquals(getDomainOfValidity(),            that.getDomainOfValidity(), mode) &&
                     deepEquals(getCoordinateOperationAccuracy(), that.getCoordinateOperationAccuracy(), mode))) &&
                     deepEquals(getInterpolationCRS(),            getInterpolationCRS(that), mode))
                {
                    /*
                     * At this point all metdata match or can be ignored. First, compare the targetCRS.
                     * We need to perform this comparison only if this 'equals(…)' method is not invoked
                     * from AbstractDerivedCRS, otherwise we would fall in an infinite recursive loop
                     * (because targetCRS is the DerivedCRS, which in turn wants to compare this operation).
                     *
                     * We also opportunistically use this "anti-recursivity" check for another purpose.
                     * The Semaphores.COMPARING flag should be set only when AbstractDerivedCRS is comparing
                     * its "from base" conversion. The flag should never be set in any other circumstance,
                     * since this is an internal Apache SIS mechanism. If we know that we are comparing the
                     * AbstractDerivedCRS.fromBase conversion, then (in the way Apache SIS is implemented)
                     * this.sourceCRS == AbstractDerivedCRS.baseCRS. Consequently we can relax the check
                     * sourceCRS axis order if the mode is ComparisonMode.IGNORE_METADATA.
                     */
                    if (Semaphores.queryAndSet(Semaphores.CONVERSION_AND_CRS)) {
                        if (mode.isIgnoringMetadata()) {
                            mode = ComparisonMode.ALLOW_VARIANT;
                        }
                    } else try {
                        if (!deepEquals(getTargetCRS(), that.getTargetCRS(), mode)) {
                            return false;
                        }
                    } finally {
                        Semaphores.clear(Semaphores.CONVERSION_AND_CRS);
                    }
                    /*
                     * Now compare the sourceCRS, potentially with a relaxed ComparisonMode (see above comment).
                     * If the comparison mode allows the two CRS to have different axis order and units, then we
                     * need to take in account those difference before to compare the MathTransform. We proceed
                     * by modifying 'tr2' as if it was a MathTransform with crs1 as the source instead of crs2.
                     */
                    final CoordinateReferenceSystem crs1 = this.getSourceCRS();
                    final CoordinateReferenceSystem crs2 = that.getSourceCRS();
                    if (deepEquals(crs1, crs2, mode)) {
                        MathTransform tr1 = this.getMathTransform();
                        MathTransform tr2 = that.getMathTransform();
                        if (mode == ComparisonMode.ALLOW_VARIANT) try {
                            final MathTransform before = MathTransforms.linear(
                                    CoordinateSystems.swapAndScaleAxes(crs1.getCoordinateSystem(),
                                                                       crs2.getCoordinateSystem()));
                            final MathTransform after = MathTransforms.linear(
                                    CoordinateSystems.swapAndScaleAxes(that.getTargetCRS().getCoordinateSystem(),
                                                                       this.getTargetCRS().getCoordinateSystem()));
                            tr2 = MathTransforms.concatenate(before, tr2, after);
                        } catch (Exception e) {    // (ConversionException | RuntimeException) on the JDK7 branch.
                            Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                                    AbstractCoordinateOperation.class, "equals", e);
                        }
                        return deepEquals(tr1, tr2, mode);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link AbstractIdentifiedObject#computeHashCode()} for more information.
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        /*
         * Do NOT take 'getMethod()' in account in hash code calculation. See the comment
         * inside the above 'equals(Object, ComparisonMode)' method for more information.
         * Note that we use the 'transform' hash code, which should be sufficient.
         */
        return super.computeHashCode() + Objects.hash(sourceCRS, targetCRS, interpolationCRS, transform);
    }

    /**
     * Formats this coordinate operation in Well Known Text (WKT) version 2 format.
     *
     * @param  formatter The formatter to use.
     * @return {@code "CoordinateOperation"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#113">WKT 2 specification §17</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        formatter.newLine();
        /*
         * If the WKT is a component of a ConcatenatedOperation, do not format the source CRS since it is identical
         * to the target CRS of the previous step, or to the source CRS of the enclosing "ConcatenatedOperation" if
         * this step is the first step.
         *
         * This decision is SIS-specific since the WKT 2 specification does not define concatenated operations.
         * This choice may change in any future SIS version.
         */
        final FormattableObject enclosing = formatter.getEnclosingElement(1);
        final boolean isSubOperation = (enclosing instanceof PassThroughOperation);
        final boolean isComponent    = (enclosing instanceof ConcatenatedOperation);
        if (!isSubOperation && !isComponent) {
            append(formatter, getSourceCRS(), WKTKeywords.SourceCRS);
            append(formatter, getTargetCRS(), WKTKeywords.TargetCRS);
        }
        final OperationMethod method = getMethod();
        if (method != null) {
            formatter.append(DefaultOperationMethod.castOrCopy(method));
            ParameterValueGroup parameters;
            try {
                parameters = getParameterValues();
            } catch (UnsupportedOperationException e) {
                final IdentifiedObject c = getParameterDescriptors();
                formatter.setInvalidWKT(c != null ? c : this, e);
                parameters = null;
            }
            if (parameters != null) {
                formatter.newLine();
                formatter.indent(+1);
                for (final GeneralParameterValue param : parameters.values()) {
                    WKTUtilities.append(param, formatter);
                }
                formatter.indent(-1);
            }
        }
        if (!isSubOperation && !(this instanceof ConcatenatedOperation)) {
            append(formatter, getInterpolationCRS(), WKTKeywords.InterpolationCRS);
            final double accuracy = getLinearAccuracy();
            if (accuracy > 0) {
                formatter.append(new FormattableObject() {
                    @Override protected String formatTo(final Formatter formatter) {
                        formatter.append(accuracy);
                        return WKTKeywords.OperationAccuracy;
                    }
                });
            }
        }
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return isComponent ? "CoordinateOperationStep" : WKTKeywords.CoordinateOperation;
    }

    /**
     * Appends the given CRS (if non-null) wrapped in an element of the given name.
     *
     * @param formatter The formatter where to append the object name.
     * @param crs       The object to append, or {@code null} if none.
     * @param type      The keyword to write before the object.
     */
    private static void append(final Formatter formatter, final CoordinateReferenceSystem crs, final String type) {
        if (crs != null) {
            formatter.append(new FormattableObject() {
                @Override protected String formatTo(final Formatter formatter) {
                    formatter.indent(-1);
                    formatter.append(WKTUtilities.toFormattable(crs));
                    formatter.indent(+1);
                    return type;
                }
            });
            formatter.newLine();
        }
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractCoordinateOperation() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
    }

    /**
     * Invoked by JAXB for getting the source CRS to marshal.
     */
    @XmlElement(name = "sourceCRS")
    private CoordinateReferenceSystem getSource() {
        return isDefiningConversion() ? null : getSourceCRS();
    }

    /**
     * Invoked by JAXB at marshalling time for setting the source CRS.
     */
    private void setSource(final CoordinateReferenceSystem crs) {
        if (sourceCRS == null) {
            sourceCRS = crs;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractCoordinateOperation.class, "setSource", "sourceCRS");
        }
    }

    /**
     * Invoked by JAXB for getting the target CRS to marshal.
     */
    @XmlElement(name = "targetCRS")
    private CoordinateReferenceSystem getTarget() {
        return isDefiningConversion() ? null : getTargetCRS();
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the target CRS.
     */
    private void setTarget(final CoordinateReferenceSystem crs) {
        if (targetCRS == null) {
            targetCRS = crs;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractCoordinateOperation.class, "setTarget", "targetCRS");
        }
    }

    /**
     * Invoked by JAXB only at marshalling time.
     */
    @XmlElement(name = "coordinateOperationAccuracy")
    private PositionalAccuracy[] getAccuracy() {
        final Collection<PositionalAccuracy> accuracy = getCoordinateOperationAccuracy();
        final int size = accuracy.size();
        return (size != 0) ? accuracy.toArray(new PositionalAccuracy[size]) : null;
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     */
    private void setAccuracy(final PositionalAccuracy[] values) {
        if (coordinateOperationAccuracy == null) {
            coordinateOperationAccuracy = UnmodifiableArrayList.wrap(values);
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractCoordinateOperation.class, "setAccuracy", "coordinateOperationAccuracy");
        }
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getOperationVersion()
     */
    private void setOperationVersion(final String value) {
        if (operationVersion == null) {
            operationVersion = value;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractCoordinateOperation.class, "setOperationVersion", "operationVersion");
        }
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getDomainOfValidity()
     */
    private void setDomainOfValidity(final Extent value) {
        if (domainOfValidity == null) {
            domainOfValidity = value;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractCoordinateOperation.class, "setDomainOfValidity", "domainOfValidity");
        }
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getScope()
     */
    private void setScope(final InternationalString value) {
        if (scope == null) {
            scope = value;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractCoordinateOperation.class, "setScope", "scope");
        }
    }
}
