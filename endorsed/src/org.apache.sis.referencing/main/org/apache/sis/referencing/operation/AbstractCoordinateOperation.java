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
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.Collection;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.measure.IncommensurableException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.metadata.internal.shared.ImplementationHelper;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.system.Semaphores;
import org.apache.sis.system.Loggers;
import static org.apache.sis.util.Utilities.deepEquals;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeneralDerivedCRS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.coordinate.CoordinateSet;


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
 *   <li>The {@linkplain org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity() domain of validity}.</li>
 *   <li>An estimation of the {@linkplain #getCoordinateOperationAccuracy() operation accuracy}.</li>
 * </ul>
 *
 * <h2>Instantiation</h2>
 * This class is conceptually <i>abstract</i>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 * An exception to this rule may occur when it is not possible to identify the exact operation type.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code CoordinateOperation} instances created
 * using only SIS factories and static constants can be shared by many objects and passed between threads without
 * synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.6
 */
@XmlType(name = "AbstractCoordinateOperationType", propOrder = {
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
    private static final long serialVersionUID = -5441746770453401219L;

    /**
     * The logger for coordinate operations.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.COORDINATE_OPERATION);

    /**
     * The source CRS, or {@code null} if not available.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors and for initialization
     * at XML unmarshalling time by {@link #setSource(CoordinateReferenceSystem)}.</p>
     *
     * @see #getSourceCRS()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
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
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    CoordinateReferenceSystem targetCRS;

    /**
     * The CRS required for performing the operation.
     * It may differ from the source and target CRS.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors.</p>
     *
     * @see #getInterpolationCRS()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
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
    @XmlElement(name = "operationVersion")
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
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    Collection<PositionalAccuracy> coordinateOperationAccuracy;

    /**
     * Transform from positions in the {@linkplain #getSourceCRS source coordinate reference system}
     * to positions in the {@linkplain #getTargetCRS target coordinate reference system}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for the convenience of constructors and for initialization
     * at XML unmarshalling time by {@link AbstractSingleOperation#afterUnmarshal(Unmarshaller, Object)}.</p>
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    MathTransform transform;

    /**
     * Indices of target dimensions where "wrap around" may happen as a result of this coordinate operation.
     * This is usually the longitude axis when the source CRS uses the [-180 … +180]° range and the target
     * CRS uses the [0 … 360]° range, or the converse. If there is no change, then this is an empty set.
     *
     * @see #getWrapAroundChanges()
     * @see #computeTransientFields()
     */
    private transient Set<Integer> wrapAroundChanges;

    /**
     * The inverse of this coordinate operation, computed when first needed. This is stored for making
     * possible to find the original operation when the inverse of an inverse operation is requested.
     * Serialized for avoiding information lost if the inverse is requested after deserialization.
     *
     * <p>This field is not formally part of coordinate operation definition,
     * so it is not compared by {@link #equals(Object, ComparisonMode)}.</p>
     *
     * @see #getCachedInverse(CoordinateOperation)
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private volatile CoordinateOperation inverse;

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
        this.operationVersion = Containers.property(properties, OPERATION_VERSION_KEY, String.class);
        Object value = properties.get(COORDINATE_OPERATION_ACCURACY_KEY);
        if (value != null) {
            if (value instanceof PositionalAccuracy[]) {
                coordinateOperationAccuracy = CollectionsExt.nonEmptySet((PositionalAccuracy[]) value);
            } else {
                coordinateOperationAccuracy = Set.of((PositionalAccuracy) value);
            }
        }
    }

    /**
     * Creates a coordinate operation from the given properties.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#OPERATION_VERSION_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getOperationVersion()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#COORDINATE_OPERATION_ACCURACY_KEY}</td>
     *     <td>{@link PositionalAccuracy} (optionally as array)</td>
     *     <td>{@link #getCoordinateOperationAccuracy()}</td>
     *   </tr><tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * <h4>Constraints</h4>
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
     * If the {@code interpolationCRS} is non-null, then the given {@code transform} shall expect input coordinates
     * in the following order:
     *
     * <ol>
     *   <li>Coordinates of the interpolation CRS. Example: (<var>x</var>,<var>y</var>) in a vertical transform.</li>
     *   <li>Coordinates of the source CRS. Example: (<var>z</var>) in a vertical transform.</li>
     * </ol>
     *
     * The math transform shall let the interpolation coordinates {@linkplain DefaultPassThroughOperation pass through
     * the operation}.
     *
     * @param  properties        the properties to be given to the identified object.
     * @param  sourceCRS         the source CRS, or {@code null} if unspecified.
     * @param  targetCRS         the target CRS, or {@code null} if unspecified.
     * @param  interpolationCRS  the CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param  transform         transform from positions in the source CRS to positions in the target CRS,
     *                           or {@code null} if unspecified.
     */
    @SuppressWarnings("this-escape")    // False positive.
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final MathTransform transform = this.transform;                     // Protect from changes.
        if (transform != null) {
            final int interpDim = CRS.getDimensionOrZero(interpolationCRS);
check:      for (int isTarget=0; ; isTarget++) {        // 0 == source check; 1 == target check.
                final CoordinateReferenceSystem crs;    // Will determine the expected dimensions.
                int actual;                             // The MathTransform number of dimensions.
                switch (isTarget) {
                    case 0: crs = sourceCRS; actual = transform.getSourceDimensions(); break;
                    case 1: crs = targetCRS; actual = transform.getTargetDimensions(); break;
                    default: break check;
                }
                int expected = CRS.getDimensionOrZero(crs);
                if (interpDim != 0) {
                    if (actual == expected || actual < interpDim) {
                        // This check is not strictly necessary as the next check below would catch the error,
                        // but we provide here a hopefully more helpful error message for a common mistake.
                        throw new IllegalArgumentException(Resources.forProperties(properties)
                                .getString(Resources.Keys.MissingInterpolationOrdinates));
                    }
                    expected += interpDim;
                }
                if (crs != null && actual != expected) {
                    throw new IllegalArgumentException(Errors.forProperties(properties).getString(
                            Errors.Keys.MismatchedTransformDimension_4, super.getName().getCode(),
                            isTarget, expected, actual));
                }
            }
        }
        computeTransientFields();
    }

    /**
     * Computes the {@link #wrapAroundChanges} field after we verified that the coordinate operation is valid.
     */
    final void computeTransientFields() {
        if (sourceCRS != null && targetCRS != null) {
            wrapAroundChanges = CoordinateOperations.wrapAroundChanges(sourceCRS, targetCRS.getCoordinateSystem());
        } else {
            wrapAroundChanges = Set.of();
        }
    }

    /**
     * Computes transient fields after deserialization.
     *
     * @param  in  the input stream from which to deserialize a coordinate operation.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        computeTransientFields();
    }

    /**
     * Creates a new coordinate operation with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  operation  the coordinate operation to copy.
     *
     * @see #castOrCopy(CoordinateOperation)
     */
    protected AbstractCoordinateOperation(final CoordinateOperation operation) {
        super(operation);
        sourceCRS                   = operation.getSourceCRS();
        targetCRS                   = operation.getTargetCRS();
        interpolationCRS            = operation.getInterpolationCRS().orElse(null);
        operationVersion            = operation.getOperationVersion();
        coordinateOperationAccuracy = operation.getCoordinateOperationAccuracy();
        transform                   = operation.getMathTransform();
        if (operation instanceof AbstractCoordinateOperation) {
            wrapAroundChanges = ((AbstractCoordinateOperation) operation).wrapAroundChanges;
        } else {
            computeTransientFields();
        }
    }

    /**
     * Returns a SIS coordinate operation implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of
     *       {@link org.opengis.referencing.operation.Conversion},
     *       {@link org.opengis.referencing.operation.Transformation},
     *       {@link org.opengis.referencing.operation.PointMotionOperation},
     *       {@link org.opengis.referencing.operation.PassThroughOperation} or
     *       {@link org.opengis.referencing.operation.ConcatenatedOperation},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractCoordinateOperation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractCoordinateOperation} instance is created using the
     *       {@linkplain #AbstractCoordinateOperation(CoordinateOperation) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractCoordinateOperation castOrCopy(final CoordinateOperation object) {
        if (object instanceof Transformation) {
            return DefaultTransformation.castOrCopy((Transformation) object);
        }
        if (object instanceof Conversion) {
            return DefaultConversion.castOrCopy((Conversion) object);
        }
        if (object instanceof PassThroughOperation) {
            return DefaultPassThroughOperation.castOrCopy((PassThroughOperation) object);
        }
        if (object instanceof ConcatenatedOperation) {
            return DefaultConcatenatedOperation.castOrCopy((ConcatenatedOperation) object);
        }
        if (isSingleOperation(object)) {
            if (object instanceof AbstractSingleOperation) {
                return (AbstractSingleOperation) object;
            }
            return new AbstractSingleOperation((SingleOperation) object);
        }
        /*
         * Intentionally check for `AbstractCoordinateOperation` after the interfaces because users may have defined
         * their own subclass implementing the same interfaces. If we were checking for `AbstractCoordinateOperation`
         * before the interfaces, the returned instance could have been a user subclass without the JAXB annotations
         * required for XML marshalling.
         */
        if (object == null || object instanceof AbstractCoordinateOperation) {
            return (AbstractCoordinateOperation) object;
        }
        return new AbstractCoordinateOperation(object);
    }

    /**
     * Returns {@code true} if the given operation is a single operation but not a pass-through operation.
     * In an older ISO 19111 model, {@link PassThroughOperation} extended {@link SingleOperation}, which
     * was a problem for providing a value to the inherited {@link SingleOperation#getMethod()} method.
     * This has been fixed in newer ISO 19111 model, but for safety with objects following the older model
     * (e.g. GeoAPI 3.0) we are better to perform an explicit exclusion of {@link PassThroughOperation}.
     */
    static boolean isSingleOperation(final CoordinateOperation operation) {
        return (operation instanceof SingleOperation) && !(operation instanceof PassThroughOperation);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code CoordinateOperation.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return the coordinate operation interface implemented by this class.
     */
    @Override
    public Class<? extends CoordinateOperation> getInterface() {
        return CoordinateOperation.class;
    }

    /**
     * Returns whether this coordinate operation is for the definition of a derived or projected <abbr>CRS</abbr>.
     * The <abbr>ISO</abbr> 19111 approach constructs <dfn>defining conversion</dfn> as an operation of type
     * {@link Conversion} with null {@linkplain #getSourceCRS() source} and {@linkplain #getTargetCRS() target CRS}.
     * But <abbr>SIS</abbr> supports also defining conversions with non-null <abbr>CRS</abbrr> provided that:
     *
     * <ul>
     *   <li>{@link DerivedCRS#getBaseCRS()} is the {@linkplain #getSourceCRS() source CRS} of this operation, and</li>
     *   <li>{@link DerivedCRS#getConversionFromBase()} is this operation instance.</li>
     * </ul>
     *
     * When this method returns {@code true}, the source and target CRS are not marshalled in XML documents.
     *
     * @return {@code true} if this coordinate operation is for the definition of a derived or projected CRS.
     */
    @SuppressWarnings("deprecation")
    public boolean isDefiningConversion() {
        /*
         * Trick: we do not need to verify if (this instanceof Conversion) because:
         *   - Only DefaultConversion constructor accepts null source and target CRS.
         *   - DerivedCRS.getConversionFromBase() return type is Conversion.
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
     * @return the source CRS, or {@code null} if not available.
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
     * @return the target CRS, or {@code null} if not available.
     */
    @Override
    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    /**
     * Returns the <abbr>CRS</abbr> to be used for interpolations in a grid.
     * Some single coordinate operations employ methods which include interpolation within a grid to derive
     * the values of operation parameters. The <abbr>CRS</abbr> to be used for the interpolation
     * may be different from either the source <abbr>CRS</abbr> or the target <abbr>CRS</abbr>.
     *
     * <h4>Example</h4>
     * Some transformations of vertical coordinates (<var>h</var>) require the horizontal coordinates (φ,λ)
     * in order to interpolate in a grid. This method returns the CRS of the grid where such interpolations
     * are performed.
     *
     * @return the <abbr>CRS</abbr> required for interpolating the values.
     */
    @Override
    public Optional<CoordinateReferenceSystem> getInterpolationCRS() {
        return Optional.ofNullable(interpolationCRS);
    }

    /**
     * Returns the version of the coordinate operation. Different versions of a coordinate
     * {@linkplain DefaultTransformation transformation} may exist because of the stochastic
     * nature of the parameters. In principle this property is irrelevant to coordinate
     * {@linkplain DefaultConversion conversions}, but Apache SIS accepts it anyway.
     *
     * @return the coordinate operation version, or {@code null} if none.
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
     * @return the position error estimations, or an empty collection if not available.
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
     * <h4>Current implementation</h4>
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
     * @return the accuracy estimation (always in meters), or NaN if unknown.
     *
     * @see org.apache.sis.referencing.CRS#getLinearAccuracy(CoordinateOperation)
     */
    public double getLinearAccuracy() {
        return PositionalAccuracyConstant.getLinearAccuracy(this);
    }

    /**
     * Returns the object for transforming coordinates in the source CRS to coordinates in the target CRS.
     * The transform may be {@code null} if this coordinate operation is a defining conversion.
     *
     * @return the transform from source to target CRS, or {@code null} if not applicable.
     */
    @Override
    public MathTransform getMathTransform() {
        return transform;
    }

    /**
     * Changes coordinates from being referenced to the source <abbr>CRS</abbr>
     * and/or epoch to being referenced to the target <abbr>CRS</abbr> and/or epoch.
     * This method operates on coordinate tuples and does not deal with interpolation of geometry types.
     *
     * <h4>Implementation specific behavior</h4>
     * The default implementation has the following characteristics. Those characteristics are not
     * guaranteed to be met by all implementations of the {@link CoordinateOperation} interface:
     *
     * <ul>
     *   <li>If the <abbr>CRS</abbr> and/or epoch of the given data are not equivalent to the source <abbr>CRS</abbr> and/or
     *       epoch of this coordinate operation, this method will automatically prepend an additional operation step.</li>
     *   <li>The coordinate tuples are not transformed immediately, but instead will be computed on-the-fly
     *       in the stream returned by {@link CoordinateSet#stream()}.</li>
     *   <li>If a {@link TransformException} occurs during on-the-fly coordinate operation, it will be wrapped
     *       in an unchecked {@link org.apache.sis.util.collection.BackingStoreException}.</li>
     *   <li>The returned coordinate set is serializable if the given data and the math transform are also serializable.</li>
     * </ul>
     *
     * @param  data  the coordinates to change.
     * @return the result of changing coordinates.
     * @throws TransformException if some coordinates cannot be changed. Note that an absence of exception during
     *         this method call is not a guarantee that the coordinate changes succeeded, because other errors can
     *         occur during the stream execution.
     *
     * @since 1.5
     */
    @Override
    public CoordinateSet transform(final CoordinateSet data) throws TransformException {
        return new TransformedCoordinateSet(this, data);
    }

    /**
     * Returns the operation method. This apply only to {@link AbstractSingleOperation} subclasses,
     * which will make this method public.
     *
     * @return the operation method, or {@code null} if none.
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
                if (Semaphores.TRANSFORM_ENCLOSED_IN_OPERATION.set()) try {
                    param = ((Parameterized) transform).getParameterDescriptors();
                } finally {
                    Semaphores.TRANSFORM_ENCLOSED_IN_OPERATION.clear();
                } else {
                    throw new AssertionError();     // Should never happen.
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
     * @return the parameter values (never {@code null}).
     * @throws UnsupportedOperationException if the parameter values cannot
     *         be determined for the current math transform implementation.
     */
    ParameterValueGroup getParameterValues() throws UnsupportedOperationException {
        MathTransform mt = transform;
        while (mt != null) {
            if (mt instanceof Parameterized) {
                final ParameterValueGroup param;
                if (Semaphores.TRANSFORM_ENCLOSED_IN_OPERATION.set()) try {
                    param = ((Parameterized) mt).getParameterValues();
                } finally {
                    Semaphores.TRANSFORM_ENCLOSED_IN_OPERATION.clear();
                } else {
                    throw new AssertionError();     // Should never happen.
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
     * Returns the indices of target dimensions where "wrap around" may happen as a result of this coordinate operation.
     * If such change exists, then this is usually the longitude axis when the source CRS uses the [-180 … +180]° range
     * and the target CRS uses the [0 … 360]° range, or the converse. If there is no change, then this is an empty set.
     *
     * <h4>Inverse relationship</h4>
     * sometimes the target dimensions returned by this method can be mapped directly to wraparound axes in source CRS,
     * but this is not always the case. For example, consider the following operation chain:
     *
     * <div style="text-align:center">source projected CRS ⟶ base CRS ⟶ target geographic CRS</div>
     *
     * In this example, a wraparound axis in the target CRS (the longitude) can be mapped to a wraparound axis in
     * the {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS#getBaseCRS() base CRS}. But there is no
     * corresponding wraparound axis in the source CRS because the <em>easting</em> axis in projected CRS does not
     * have a wraparound range meaning. We could argue that
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getDirection() axis directions} match,
     * but such matching is not guaranteed to exist since {@code ProjectedCRS} is a special case of {@code DerivedCRS}
     * and derived CRS can have rotations.
     *
     * <h4>Default implementation</h4>
     * The default implementation infers this set by inspecting the source and target coordinate system axes.
     * It returns the indices of all target axes having {@link org.opengis.referencing.cs.RangeMeaning#WRAPAROUND}
     * and for which the following condition holds: a colinear source axis exists with compatible unit of measurement,
     * and the range (taking unit conversions in account) or range meaning of those source and target axes are not
     * the same.
     *
     * @return indices of target dimensions where "wrap around" may happen as a result of this coordinate operation.
     *
     * @since 0.8
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<Integer> getWrapAroundChanges() {
        return wrapAroundChanges;
    }

    /**
     * Returns the inverse of the given coordinate operation, or {@code null} if unspecified.
     * This method only checks the cached value and does not compute a new value if none is present.
     *
     * @param  forward  the operation for which to get the inverse.
     * @return the cached inverse operation, or {@code null} if none.
     */
    static CoordinateOperation getCachedInverse(final CoordinateOperation forward) {
        if (forward instanceof AbstractCoordinateOperation) {
            final CoordinateOperation inverse = ((AbstractCoordinateOperation) forward).inverse;
            if (inverse != null) {
                return inverse;
            }
        }
        return null;
    }

    /**
     * Caches the inverse of the given coordinate operation.
     *
     * @param  forward  the operation for which to cache the inverse.
     * @param  inverse  the inverse operation to cache.
     */
    static void setCachedInverse(final CoordinateOperation forward, final CoordinateOperation inverse) {
        if (inverse instanceof AbstractCoordinateOperation) {
            ((AbstractCoordinateOperation) inverse).inverse = forward;
        }
        if (forward instanceof AbstractCoordinateOperation) {
            ((AbstractCoordinateOperation) forward).inverse = inverse;
        }
    }

    /**
     * Compares this coordinate operation with the specified object for equality. If the {@code mode} argument
     * is {@link ComparisonMode#STRICT} or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available
     * properties are compared including the {@linkplain #getDomains() domains} and the accuracy.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal for the given comparison mode.
     */
    @Override
    public boolean equals(final Object object, ComparisonMode mode) {
        if (super.equals(object, mode)) {
            if (mode == ComparisonMode.STRICT) {
                final var that = (AbstractCoordinateOperation) object;
                if (Objects.equals(sourceCRS,                   that.sourceCRS)        &&
                    Objects.equals(interpolationCRS,            that.interpolationCRS) &&
                    Objects.equals(transform,                   that.transform)        &&
                    Objects.equals(coordinateOperationAccuracy, that.coordinateOperationAccuracy))
                {
                    if (Semaphores.COMPARING_CONVERSION_OR_DERIVED_CRS.set()) try {
                        return Objects.equals(targetCRS, that.targetCRS);
                    } finally {
                        Semaphores.COMPARING_CONVERSION_OR_DERIVED_CRS.clear();
                    } else {
                        // Avoid never-ending recursion when the comparison was started by `AbstractDerivedCRS`.
                        return true;
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
                final var that = (CoordinateOperation) object;
                if ((mode.isIgnoringMetadata() ||
                    (deepEquals(getCoordinateOperationAccuracy(), that.getCoordinateOperationAccuracy(), mode))) &&
                     deepEquals(getInterpolationCRS(),            that.getInterpolationCRS(), mode))
                {
                    /*
                     * At this point all metadata match or can be ignored. First, compare the targetCRS.
                     * We need to perform this comparison only if this `equals(…)` method is not invoked
                     * from AbstractDerivedCRS, otherwise we would fall in an infinite recursive loop
                     * (because targetCRS is the DerivedCRS, which in turn wants to compare this operation).
                     *
                     * We also opportunistically use this "anti-recursion" check for another purpose.
                     * The Semaphores.COMPARING flag should be set only when AbstractDerivedCRS is comparing
                     * its "from base" conversion. The flag should never be set in any other circumstance,
                     * since this is an internal Apache SIS mechanism. If we know that we are comparing the
                     * AbstractDerivedCRS.fromBase conversion, then (in the way Apache SIS is implemented)
                     * this.sourceCRS == AbstractDerivedCRS.baseCRS. Consequently, we can relax the check of
                     * sourceCRS axis order if the mode is ComparisonMode.IGNORE_METADATA.
                     */
                    boolean debug = false;
                    if (Semaphores.COMPARING_CONVERSION_OR_DERIVED_CRS.set()) try {
                        if (!deepEquals(getTargetCRS(), that.getTargetCRS(), mode)) {
                            return false;
                        }
                    } finally {
                        Semaphores.COMPARING_CONVERSION_OR_DERIVED_CRS.clear();
                    } else {
                        if (mode.isIgnoringMetadata()) {
                            debug = (mode == ComparisonMode.DEBUG);
                            mode = ComparisonMode.ALLOW_VARIANT;
                        }
                    }
                    /*
                     * Now compare the sourceCRS, potentially with a relaxed ComparisonMode (see above comment).
                     * If the comparison mode allows the two CRS to have different axis order and units, then we
                     * need to take in account those difference before to compare the MathTransform. We proceed
                     * by modifying `tr2` as if it was a MathTransform with crs1 as the source instead of crs2.
                     */
                    final CoordinateReferenceSystem crs1 = this.getSourceCRS();
                    final CoordinateReferenceSystem crs2 = that.getSourceCRS();
                    if (deepEquals(crs1, crs2, mode)) {
                        MathTransform tr1 = this.getMathTransform();
                        MathTransform tr2 = that.getMathTransform();
                        if (mode.ordinal() >= ComparisonMode.ALLOW_VARIANT.ordinal()) try {
                            final MathTransform before = MathTransforms.linear(
                                    CoordinateSystems.swapAndScaleAxes(crs1.getCoordinateSystem(),
                                                                       crs2.getCoordinateSystem()));
                            final MathTransform after = MathTransforms.linear(
                                    CoordinateSystems.swapAndScaleAxes(that.getTargetCRS().getCoordinateSystem(),
                                                                       this.getTargetCRS().getCoordinateSystem()));
                            tr2 = MathTransforms.concatenate(before, tr2, after);
                        } catch (IncommensurableException | RuntimeException e) {
                            Logging.ignorableException(LOGGER, AbstractCoordinateOperation.class, "equals", e);
                        }
                        if (deepEquals(tr1, tr2, mode)) return true;
                        assert !debug || deepEquals(tr1, tr2, ComparisonMode.DEBUG);        // For locating the mismatch.
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
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        /*
         * Do NOT take `getMethod()` in account in hash code calculation. See the comment
         * inside the above `equals(Object, ComparisonMode)` method for more information.
         * Note that we use the `transform` hash code, which should be sufficient.
         */
        return super.computeHashCode() + Objects.hash(sourceCRS, targetCRS, interpolationCRS, transform);
    }

    /**
     * Formats this coordinate operation in Well Known Text (WKT) version 2 format.
     *
     * <h4>ESRI extension</h4>
     * Coordinate operations cannot be formatted in standard WKT 1 format, but an ESRI variant of WKT 1
     * allows a subset of coordinate operations with the ESRI-specific {@code GEOGTRAN} keyword.
     * To enabled this variant, {@link org.apache.sis.io.wkt.WKTFormat} can be configured as below:
     *
     * {@snippet lang="java" :
     *     format = new WKTFormat();
     *     format.setConvention(Convention.WKT1_IGNORE_AXES);
     *     format.setNameAuthority(Citations.ESRI);
     *     }
     *
     * @param  formatter  the formatter to use.
     * @return {@code "CoordinateOperation"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        final Convention convention = formatter.getConvention();
        final boolean isWKT1 = (convention.majorVersion() == 1);
        if (convention.supports(Convention.WKT2_2019)) {
            String version = getOperationVersion();
            if (version != null) {
                formatter.append(new FormattableObject() {
                    @Override protected String formatTo(final Formatter formatter) {
                        formatter.append(version, null);
                        return WKTKeywords.Version;
                    }
                });
            };
        }
        formatter.newLine();
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final CoordinateReferenceSystem sourceCRS = getSourceCRS(),
                                        targetCRS = getTargetCRS();
        /*
         * If the WKT is a component of a PassThroughOperation, do not format the source CRS since it is identical
         * to a component of the Source CRS of the enclosing `PassThroughOperation`. This decision is SIS-specific
         * because the WKT 2 specification does not define pass-through operations.
         * This choice may change in any future SIS version.
         */
        final boolean isSubOperation = (formatter.getEnclosingElement(1) instanceof PassThroughOperation);
        boolean isGeogTran = false;
        if (!isSubOperation) {
            isGeogTran = isWKT1 && (sourceCRS instanceof GeographicCRS) && (targetCRS instanceof GeographicCRS);
            if (isGeogTran) {
                // ESRI-specific, similar to WKT 1.
                formatter.append(WKTUtilities.toFormattable(sourceCRS)); formatter.newLine();
                formatter.append(WKTUtilities.toFormattable(targetCRS)); formatter.newLine();
            } else {
                // WKT 2 (ISO 19162).
                append(formatter, sourceCRS, WKTKeywords.SourceCRS);
                append(formatter, targetCRS, WKTKeywords.TargetCRS);
            }
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
                /*
                 * Format the parameter values. Apache SIS uses the EPSG geodetic dataset as the main source of
                 * parameter definitions. When a parameter is defined by both OGC and EPSG with different names,
                 * the Formatter class is responsible for choosing an appropriate name. But when the difference
                 * is more fundamental, we may have duplication. For example in the "Molodensky" operation, OGC
                 * uses source and target axis lengths while EPSG uses only difference between those lengths.
                 * In this case, OGC and EPSG parameters are defined separately and are redundant. To simplify
                 * the CoordinateOperation WKT, we omit non-EPSG parameters when we have determined that we are
                 * about to describe an EPSG operation. We could generalize this filtering to any authority, but
                 * we don't because few authorities are as complete as EPSG, so other authorities are more likely
                 * to mix EPSG or someone else components with their own. Note also that we don't apply filtering
                 * on MathTransform WKT neither for more reliable debugging.
                 */
                final boolean filter = isGeogTran ||
                        (WKTUtilities.isEPSG(parameters.getDescriptor(), false) &&   // NOT method.getName()
                        Constants.EPSG.equalsIgnoreCase(Citations.toCodeSpace(formatter.getNameAuthority())));
                formatter.newLine();
                formatter.indent(+1);
                for (final GeneralParameterValue param : parameters.values()) {
                    if (!filter || WKTUtilities.isEPSG(param.getDescriptor(), true)) {
                        WKTUtilities.append(param, formatter);
                    }
                }
                formatter.indent(-1);
            }
        }
        /*
         * If formatting a WKT 1 string, we need to declare the string as invalid (because `CoordinateOperation`
         * did not existed at that time) except if the CRS types are compliant with the ESRI extension and that
         * extension was enabled. Even if the ESRI extensions are not enabled, we still use the ESRI keyword if
         * applicable and use `setInvalidWKT(…)` for warning the user.
         */
        if (isWKT1) {
            if (!(isGeogTran && method != null && convention == Convention.WKT1_IGNORE_AXES)) {
                formatter.setInvalidWKT(this, null);
            }
            if (isGeogTran) {
                return WKTKeywords.GeogTran;
            }
        }
        if (!(isSubOperation || this instanceof ConcatenatedOperation)) {
            append(formatter, getInterpolationCRS().orElse(null), WKTKeywords.InterpolationCRS);
            WKTUtilities.appendElementIfPositive(WKTKeywords.OperationAccuracy, getLinearAccuracy(), formatter);
        }
        return WKTKeywords.CoordinateOperation;
    }

    /**
     * Appends the given CRS (if non-null) wrapped in an element of the given name.
     *
     * @param formatter  the formatter where to append the object name.
     * @param crs        the object to append, or {@code null} if none.
     * @param type       the keyword to write before the object.
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




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Creates a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    AbstractCoordinateOperation() {
        super(org.apache.sis.referencing.internal.shared.NilReferencingObject.INSTANCE);
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
        } else if (!sourceCRS.equals(crs)) {                    // Could be defined by ConcatenatedOperation.
            ImplementationHelper.propertyAlreadySet(AbstractCoordinateOperation.class, "setSource", "sourceCRS");
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
        } else if (!targetCRS.equals(crs)) {                    // Could be defined by ConcatenatedOperation.
            ImplementationHelper.propertyAlreadySet(AbstractCoordinateOperation.class, "setTarget", "targetCRS");
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
            ImplementationHelper.propertyAlreadySet(AbstractCoordinateOperation.class, "setAccuracy", "coordinateOperationAccuracy");
        }
    }

    /**
     * Invoked by JAXB after unmarshalling.
     * May be overridden by subclasses.
     */
    void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        computeTransientFields();
    }
}
