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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.metadata.Identifier;
import org.apache.sis.parameter.Parameterized;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.system.Semaphores;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import java.util.Objects;


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
 * @version 0.6
 * @module
 */
@XmlType(name="AbstractCoordinateOperationType", propOrder = {
    "domainOfValidity",
    "scope",
    "operationVersion",
    "coordinateOperationAccuracy",
//  "sourceCRS",    // TODO
//  "targetCRS"
})
@XmlRootElement(name = "AbstractCoordinateOperation")
@XmlSeeAlso({
    AbstractSingleOperation.class
})
public class AbstractCoordinateOperation extends AbstractIdentifiedObject implements CoordinateOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1237358357729193885L;

    /**
     * The source CRS, or {@code null} if not available.
     *
     * @see #getSourceCRS()
     */
//  @XmlElement
    private final CoordinateReferenceSystem sourceCRS;

    /**
     * The target CRS, or {@code null} if not available.
     *
     * @see #getTargetCRS()
     */
//  @XmlElement
    private final CoordinateReferenceSystem targetCRS;

    /**
     * The CRS which is neither the {@linkplain #getSourceCRS() source CRS} or
     * {@linkplain #getTargetCRS() target CRS} but still required for performing the operation.
     *
     * @see #getInterpolationCRS()
     */
    private final CoordinateReferenceSystem interpolationCRS;

    /**
     * Version of the coordinate transformation
     * (i.e., instantiation due to the stochastic nature of the parameters).
     */
    @XmlElement
    private final String operationVersion;

    /**
     * Estimate(s) of the impact of this operation on point accuracy, or {@code null} if none.
     */
    @XmlElement
    private final Collection<PositionalAccuracy> coordinateOperationAccuracy;

    /**
     * Area in which this operation is valid, or {@code null} if not available.
     */
    @XmlElement
    private final Extent domainOfValidity;

    /**
     * Description of domain of usage, or limitations of usage, for which this operation is valid.
     */
    @XmlElement
    private final InternationalString scope;

    /**
     * Transform from positions in the {@linkplain #getSourceCRS source coordinate reference system}
     * to positions in the {@linkplain #getTargetCRS target coordinate reference system}.
     */
    private final MathTransform transform;

    /**
     * Creates a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractCoordinateOperation() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
        sourceCRS                   = null;
        targetCRS                   = null;
        interpolationCRS            = null;
        operationVersion            = null;
        coordinateOperationAccuracy = null;
        domainOfValidity            = null;
        scope                       = null;
        transform                   = null;
    }

    /**
     * Creates a new coordinate operation with the same values than the specified defining conversion,
     * except for the source CRS, target CRS and the math transform which are set the given values.
     *
     * <p>This constructor is (indirectly) for {@link DefaultConversion} usage only,
     * in order to create a "real" conversion from a defining conversion.</p>
     */
    AbstractCoordinateOperation(final CoordinateOperation definition,
                                final CoordinateReferenceSystem sourceCRS,
                                final CoordinateReferenceSystem targetCRS,
                                final MathTransform transform)
    {
        super(definition);
        this.sourceCRS                   = sourceCRS;
        this.targetCRS                   = targetCRS;
        this.interpolationCRS            = getInterpolationCRS(definition);
        this.operationVersion            = definition.getOperationVersion();
        this.coordinateOperationAccuracy = definition.getCoordinateOperationAccuracy();
        this.domainOfValidity            = definition.getDomainOfValidity();
        this.scope                       = definition.getScope();
        this.transform                   = transform;
        checkDimensions();
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
        super(properties);
        this.sourceCRS        = sourceCRS;
        this.targetCRS        = targetCRS;
        this.interpolationCRS = interpolationCRS;
        this.transform        = transform;
        this.domainOfValidity = Containers.property(properties, DOMAIN_OF_VALIDITY_KEY, Extent.class);
        this.scope            = Types.toInternationalString(properties, SCOPE_KEY);
        this.operationVersion = Containers.property(properties, OPERATION_VERSION_KEY, String.class);
        Object value = properties.get(COORDINATE_OPERATION_ACCURACY_KEY);
        if (value instanceof PositionalAccuracy[]) {
            coordinateOperationAccuracy = CollectionsExt.nonEmptySet((PositionalAccuracy[]) value);
        } else {
            coordinateOperationAccuracy = (value != null) ? Collections.singleton((PositionalAccuracy) value) : null;
        }
        checkDimensions();
    }

    /**
     * Ensures that {@link #sourceCRS}, {@link #targetCRS} and {@link #interpolationCRS} dimensions
     * are consistent with {@link #transform} input and output dimensions.
     */
    private void checkDimensions() {
        if (transform != null) {
            int sourceDim = transform.getSourceDimensions();
            int targetDim = transform.getTargetDimensions();
            if (interpolationCRS != null) {
                final int dim = interpolationCRS.getCoordinateSystem().getDimension();
                sourceDim -= dim;
                targetDim -= dim;
                if (sourceDim <= 0 || targetDim <= 0) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingInterpolationOrdinates));
                }
            }
            ArgumentChecks.ensureDimensionMatches("sourceCRS", sourceDim, sourceCRS);
            ArgumentChecks.ensureDimensionMatches("targetCRS", targetDim, targetCRS);
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
    private static CoordinateReferenceSystem getInterpolationCRS(final CoordinateOperation operation) {
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
        return (coordinateOperationAccuracy != null) ? coordinateOperationAccuracy : Collections.emptySet();
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
     *   <li>If a {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult quantitative result}
     *     is found with a linear unit, then this accuracy estimate is converted to
     *     {@linkplain javax.measure.unit.SI#METRE metres} and returned.</li>
     *
     *   <li>Otherwise, if the operation is a {@linkplain DefaultConversion conversion}, then returns 0 since a
     *     conversion is by definition accurate up to rounding errors.</li>
     *
     *   <li>Otherwise, if the operation is a {@linkplain DefaultTransformation transformation}, then checks if
     *     the datum shift were applied with the help of Bursa-Wolf parameters.
     *     If a datum shift has been applied, returns 25 meters.
     *     If a datum shift should have been applied but has been omitted, returns 3000 meters.
     *
     *     <div class="note"><b>Note:</b>
     *     the 3000 meters value is higher than the highest value (999 meters) found in the EPSG
     *     database version 6.7. The 25 meters value is the next highest value found in the EPSG
     *     database for a significant number of transformations.</div>
     *
     *   <li>Otherwise, if the operation is a {@linkplain DefaultConcatenatedOperation concatenated operation},
     *     returns the sum of the accuracy of all components. This is a conservative scenario where we assume that
     *     errors cumulate linearly.
     *
     *     <div class="note"><b>Note:</b>
     *     this is not necessarily the "worst case" scenario since the accuracy could be worst if the math transforms
     *     are highly non-linear.</div></li>
     * </ul>
     *
     * @return The accuracy estimation (always in meters), or NaN if unknown.
     */
    public double getLinearAccuracy() {
        return OperationMethods.getLinearAccuracy(this);
    }

    /**
     * Returns the area or region or timeframe in which this coordinate operation is valid.
     *
     * @return The coordinate operation valid domain, or {@code null} if not available.
     */
    @Override
    public Extent getDomainOfValidity() {
        return domainOfValidity;
    }

    /**
     * Returns a description of domain of usage, or limitations of usage, for which this operation is valid.
     *
     * @return A description of domain of usage, or {@code null} if none.
     */
    @Override
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
     */
    OperationMethod getMethod() {
        return null;
    }

    /**
     * Returns the parameter descriptor. The default implementation infers the descriptor from the
     * {@linkplain #transform}, if possible. If no descriptor can be inferred from the math transform,
     * then this method fallback on the {@link OperationMethod} parameters.
     */
    ParameterDescriptorGroup getParameterDescriptors() throws UnsupportedOperationException {
        MathTransform mt = transform;
        while (mt != null) {
            if (mt instanceof Parameterized) {
                final ParameterDescriptorGroup param;
                if (Semaphores.queryAndSet(Semaphores.ENCLOSED_IN_OPERATION)) {
                    throw new AssertionError(); // Should never happen.
                }
                try {
                    param = ((Parameterized) mt).getParameterDescriptors();
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
        final OperationMethod method = getMethod();
        return (method != null) ? method.getParameters() : null;
    }

    /**
     * Returns the parameter values. The default implementation infers the
     * parameter values from the {@linkplain #transform}, if possible.
     *
     * @throws UnsupportedOperationException if the parameter values can not
     *         be determined for the current math transform implementation.
     */
    ParameterValueGroup getParameterValues() {
        MathTransform mt = transform;
        while (mt != null) {
            if (mt instanceof Parameterized) {
                final ParameterValueGroup param;
                if (Semaphores.queryAndSet(Semaphores.ENCLOSED_IN_OPERATION)) {
                    throw new AssertionError(); // Should never happen.
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
    public boolean equals(final Object object, final ComparisonMode mode) {
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
                    if (Semaphores.queryAndSet(Semaphores.COMPARING)) {
                        return true;
                    } else try {
                        return Objects.equals(targetCRS, that.targetCRS);
                    } finally {
                        Semaphores.clear(Semaphores.COMPARING);
                    }
                }
            } else {
                final CoordinateOperation that = (CoordinateOperation) object;
                if (mode == ComparisonMode.BY_CONTRACT) {
                    if (!deepEquals(getScope(),                       that.getScope(), mode) ||
                        !deepEquals(getDomainOfValidity(),            that.getDomainOfValidity(), mode) ||
                        !deepEquals(getCoordinateOperationAccuracy(), that.getCoordinateOperationAccuracy(), mode))
                    {
                        return false;
                    }
                }
                if (deepEquals(getMathTransform(),    that.getMathTransform(),   mode) &&
                    deepEquals(getSourceCRS(),        that.getSourceCRS(),       mode) &&
                    deepEquals(getInterpolationCRS(), getInterpolationCRS(that), mode))
                {
                    if (Semaphores.queryAndSet(Semaphores.COMPARING)) {
                        return true;
                    } else try {
                        return deepEquals(getTargetCRS(), that.getTargetCRS(), mode);
                    } finally {
                        Semaphores.clear(Semaphores.COMPARING);
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
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#113">WKT 2 specification</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        formatter.newLine();
        append(formatter, getSourceCRS(), WKTKeywords.SourceCRS);
        append(formatter, getTargetCRS(), WKTKeywords.TargetCRS);
        formatter.append(DefaultOperationMethod.castOrCopy(getMethod()));
        final ParameterValueGroup parameters = getParameterValues();
        if (parameters != null) {
            formatter.newLine();
            for (final GeneralParameterValue param : parameters.values()) {
                WKTUtilities.append(param, formatter);
            }
        }
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
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return WKTKeywords.CoordinateOperation;
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
        }
    }
}
