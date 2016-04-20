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
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Formula;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.jaxb.gco.StringAdapter;
import org.apache.sis.internal.jaxb.referencing.CC_OperationMethod;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.FormattableObject;

import static org.apache.sis.util.ArgumentChecks.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Describes the algorithm and parameters used to perform a coordinate operation. An {@code OperationMethod}
 * is a kind of metadata: it does not perform any coordinate operation (e.g. map projection) by itself, but
 * tells us what is needed in order to perform such operation.
 *
 * <p>The most important parts of an {@code OperationMethod} are its {@linkplain #getName() name} and its
 * {@linkplain #getParameters() group of parameter descriptors}. The parameter descriptors do not contain
 * any value, but tell us what are the expected parameters, together with their units of measurement.</p>
 *
 * <div class="note"><b>Example:</b>
 * An operation method named “<cite>Mercator (variant A)</cite>” (EPSG:9804) expects the following parameters:
 * <ul>
 *   <li>“<cite>Latitude of natural origin</cite>” in degrees. Default value is 0°.</li>
 *   <li>“<cite>Longitude of natural origin</cite>” in degrees. Default value is 0°.</li>
 *   <li>“<cite>Scale factor at natural origin</cite>” as a dimensionless number. Default value is 1.</li>
 *   <li>“<cite>False easting</cite>” in metres. Default value is 0 m.</li>
 *   <li>“<cite>False northing</cite>” in metres. Default value is 0 m.</li>
 * </ul></div>
 *
 * In Apache SIS implementation, the {@linkplain #getName() name} is the only mandatory property. However it is
 * recommended to provide also {@linkplain #getIdentifiers() identifiers} (e.g. “EPSG:9804” in the above example)
 * since names can sometime be ambiguous or be spelled in different ways.
 *
 * <div class="note"><b>Departure from the ISO 19111 standard</b><br>
 * The following properties are mandatory according ISO 19111,
 * but may be missing under some conditions in Apache SIS:
 * <ul>
 *   <li>The {@linkplain #getFormula() formula} if it has not been provided to the
 *     {@linkplain #DefaultOperationMethod(Map, Integer, Integer, ParameterDescriptorGroup) constructor}, or if it
 *     can not be {@linkplain #DefaultOperationMethod(MathTransform) inferred from the given math transform}.</li>
 *   <li>The {@linkplain #getParameters() parameters} if the {@link #DefaultOperationMethod(MathTransform)}
 *     constructor can not infer them.</li>
 * </ul></div>
 *
 * <div class="section">Relationship with other classes or interfaces</div>
 * {@code OperationMethod} describes parameters without providing any value (except sometime default values).
 * When values have been assigned to parameters, the result is a {@link SingleOperation}.
 * Note that there is different kinds of {@code SingleOperation} depending on the nature and accuracy of the
 * coordinate operation. See {@link #getOperationType()} for more information.
 *
 * <p>The interface performing the actual work of taking coordinates in the
 * {@linkplain AbstractCoordinateOperation#getSourceCRS() source CRS} and calculating the new coordinates in the
 * {@linkplain AbstractCoordinateOperation#getTargetCRS() target CRS} is {@link MathTransform}.
 * In order to allow Apache SIS to instantiate those {@code MathTransform}s from given parameter values,
 * {@code DefaultOperationMethod} subclasses should implement the
 * {@link org.apache.sis.referencing.operation.transform.MathTransformProvider} interface.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thread-safe if all properties given to the constructor are also immutable and thread-safe.
 * It is strongly recommended for all subclasses to be thread-safe, especially the
 * {@link org.apache.sis.referencing.operation.transform.MathTransformProvider} implementations to be used with
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.7
 * @since   0.5
 * @module
 *
 * @see DefaultConversion
 * @see DefaultTransformation
 * @see org.apache.sis.referencing.operation.transform.MathTransformProvider
 */
@XmlType(name = "OperationMethodType", propOrder = {
    "formulaCitation",
    "formulaDescription",
    "sourceDimensions",
    "targetDimensions",
    "descriptors"
})
@XmlRootElement(name = "OperationMethod")
public class DefaultOperationMethod extends AbstractIdentifiedObject implements OperationMethod {
    /*
     * NOTE FOR JAVADOC WRITER:
     * The "method" word is ambiguous here, because it can be "Java method" or "coordinate operation method".
     * In this class, we reserve the "method" word for "coordinate operation method" as much as possible.
     */

    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2870579345991143357L;

    /**
     * Formula(s) or procedure used by this operation method. This may be a reference to a publication.
     * Note that the operation method may not be analytic, in which case this attribute references or
     * contains the procedure, not an analytic formula.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setFormulaCitation(Citation)}
     * or {@link #setFormulaDescription(String)}.</p>
     */
    private Formula formula;

    /**
     * Number of dimensions in the source CRS of this operation method.
     * May be {@code null} if this method can work with any number of
     * source dimensions (e.g. <cite>Affine Transform</cite>).
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setSourceDimensions(Integer)}</p>
     *
     * @see #getSourceDimensions()
     */
    private Integer sourceDimensions;

    /**
     * Number of dimensions in the target CRS of this operation method.
     * May be {@code null} if this method can work with any number of
     * target dimensions (e.g. <cite>Affine Transform</cite>).
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setTargetDimensions(Integer)}</p>
     *
     * @see #getTargetDimensions()
     */
    private Integer targetDimensions;

    /**
     * The set of parameters, or {@code null} if none.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDescriptors(GeneralParameterDescriptor[])}</p>
     */
    private ParameterDescriptorGroup parameters;

    /**
     * Constructs an operation method from a set of properties and a descriptor group. The properties map is given
     * unchanged to the {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * In addition to the properties documented in the parent constructor,
     * the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.OperationMethod#FORMULA_KEY}</td>
     *     <td>{@link Formula}, {@link Citation} or {@link CharSequence}</td>
     *     <td>{@link #getFormula()}</td>
     *   </tr>
     *   <tr>
     *     <th colspan="3" class="hsep">Defined in parent classes (reminder)</th>
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
     * The source and target dimensions may be {@code null} if this method can work
     * with any number of dimensions (e.g. <cite>Affine Transform</cite>).
     *
     * @param properties       Set of properties. Shall contain at least {@code "name"}.
     * @param sourceDimensions Number of dimensions in the source CRS of this operation method, or {@code null}.
     * @param targetDimensions Number of dimensions in the target CRS of this operation method, or {@code null}.
     * @param parameters       Description of parameters expected by this operation.
     */
    public DefaultOperationMethod(final Map<String,?> properties,
                                  final Integer sourceDimensions,
                                  final Integer targetDimensions,
                                  final ParameterDescriptorGroup parameters)
    {
        super(properties);
        if (sourceDimensions != null) ensurePositive("sourceDimensions", sourceDimensions);
        if (targetDimensions != null) ensurePositive("targetDimensions", targetDimensions);
        ensureNonNull("parameters", parameters);

        Object value = properties.get(FORMULA_KEY);
        if (value == null || value instanceof Formula) {
            formula = (Formula) value;
        } else if (value instanceof Citation) {
            formula = new DefaultFormula((Citation) value);
        } else if (value instanceof CharSequence) {
            formula = new DefaultFormula((CharSequence) value);
        } else {
            throw new IllegalArgumentException(Errors.getResources(properties)
                    .getString(Errors.Keys.IllegalPropertyValueClass_2, FORMULA_KEY, value.getClass()));
        }
        this.parameters       = parameters;
        this.sourceDimensions = sourceDimensions;
        this.targetDimensions = targetDimensions;
    }

    /**
     * Convenience constructor that creates an operation method from a math transform.
     * The information provided in the newly created object are approximative, and
     * usually acceptable only as a fallback when no other information are available.
     *
     * @param transform The math transform to describe.
     */
    public DefaultOperationMethod(final MathTransform transform) {
        super(getProperties(transform));
        sourceDimensions = transform.getSourceDimensions();
        targetDimensions = transform.getTargetDimensions();
        if (transform instanceof Parameterized) {
            parameters = ((Parameterized) transform).getParameterDescriptors();
        } else {
            parameters = null;
        }
        formula = null;
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Map<String,?> getProperties(final MathTransform transform) {
        ensureNonNull("transform", transform);
        if (transform instanceof Parameterized) {
            final ParameterDescriptorGroup parameters = ((Parameterized) transform).getParameterDescriptors();
            if (parameters != null) {
                return getProperties(parameters, null);
            }
        }
        return Collections.singletonMap(NAME_KEY, NilReferencingObject.UNNAMED);
    }

    /**
     * Returns the properties to be given to an identified object derived from the specified one.
     * This method returns the same properties than the supplied argument (as of
     * <code>{@linkplain IdentifiedObjects#getProperties(IdentifiedObject) getProperties}(info)</code>),
     * except for the following:
     *
     * <ul>
     *   <li>The {@linkplain IdentifiedObject#getName() name}'s authority is replaced by the specified one.</li>
     *   <li>All {@linkplain IdentifiedObject#getIdentifiers identifiers} are removed, because the new object
     *       to be created is probably not endorsed by the original authority.</li>
     * </ul>
     *
     * This method returns a mutable map. Consequently, callers can add their own identifiers
     * directly to this map if they wish.
     *
     * @param  info The identified object to view as a properties map.
     * @param  authority The new authority for the object to be created,
     *         or {@code null} if it is not going to have any declared authority.
     * @return The identified object properties in a mutable map.
     */
    private static Map<String,Object> getProperties(final IdentifiedObject info, final Citation authority) {
        final Map<String,Object> properties = new HashMap<String,Object>(IdentifiedObjects.getProperties(info));
        properties.put(NAME_KEY, new NamedIdentifier(authority, info.getName().getCode()));
        properties.remove(IDENTIFIERS_KEY);
        return properties;
    }

    /**
     * Creates a new operation method with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param method The operation method to copy.
     *
     * @see #castOrCopy(OperationMethod)
     */
    protected DefaultOperationMethod(final OperationMethod method) {
        super(method);
        formula          = method.getFormula();
        parameters       = method.getParameters();
        sourceDimensions = method.getSourceDimensions();
        targetDimensions = method.getTargetDimensions();
    }

    /**
     * Returns a SIS operation method implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then {@code null} is returned.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultOperationMethod castOrCopy(final OperationMethod object) {
        return (object == null) || (object instanceof DefaultOperationMethod)
               ? (DefaultOperationMethod) object : new DefaultOperationMethod(object);
    }

    /**
     * Constructs a new operation method with the same values than the specified one except the dimensions.
     * The source and target dimensions may be {@code null} if this method can work with any number of dimensions
     * (e.g. <cite>Affine Transform</cite>).
     *
     * @param method           The operation method to copy.
     * @param sourceDimensions Number of dimensions in the source CRS of this operation method.
     * @param targetDimensions Number of dimensions in the target CRS of this operation method.
     */
    private DefaultOperationMethod(final OperationMethod method,
                                   final Integer sourceDimensions,
                                   final Integer targetDimensions)
    {
        super(method);
        this.formula    = method.getFormula();
        this.parameters = method.getParameters();
        this.sourceDimensions = sourceDimensions;
        this.targetDimensions = targetDimensions;
    }

    /**
     * Returns an operation method with different dimensions, if we are allowed to change dimensionality.
     * This method accepts to change a dimension only if the value specified by the original method
     * is {@code null}. Otherwise an {@link IllegalArgumentException} is thrown.
     *
     * @param method           The operation method to redimension.
     * @param sourceDimensions The desired new source dimensions.
     * @param methodSource     The current number of source dimensions (may be {@code null}).
     * @param targetDimensions The desired new target dimensions.
     * @param methodTarget     The current number of target dimensions (may be {@code null}).
     * @throws IllegalArgumentException if the given dimensions are illegal for this operation method.
     */
    private static OperationMethod redimension(final OperationMethod method,
            final int sourceDimensions, final Integer methodSource,
            final int targetDimensions, final Integer methodTarget)
    {
        boolean sourceValids = (methodSource != null) && (methodSource == sourceDimensions);
        boolean targetValids = (methodTarget != null) && (methodTarget == targetDimensions);
        if (sourceValids && targetValids) {
            return method;
        }
        sourceValids |= (methodSource == null);
        targetValids |= (methodTarget == null);
        ensurePositive("sourceDimensions", sourceDimensions);
        ensurePositive("targetDimensions", targetDimensions);
        if (!sourceValids || !targetValids) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalOperationDimension_3,
                    method.getName().getCode(), sourceDimensions, targetDimensions));
        }
        return new DefaultOperationMethod(method, sourceDimensions, targetDimensions);
    }

    /**
     * Returns an operation method with different dimensions, if we are allowed to change dimensionality.
     * The need to change an {@code OperationMethod} dimensionality may occur in two contexts:
     *
     * <ul class="verbose">
     *   <li>When the original method can work with any number of dimensions. Those methods do not know
     *     in advance the number of dimensions, which is fixed only after the actual {@link MathTransform}
     *     instance has been created.
     *     Example: <cite>Affine</cite> conversion.</li>
     *   <li>When a three-dimensional method can also be used in the two-dimensional case, typically by
     *     assuming that the ellipsoidal height is zero everywhere.
     *     Example: <cite>Molodensky</cite> transform.</li>
     * </ul>
     *
     * This {@code redimension(…)} implementation performs the following choice:
     *
     * <ul class="verbose">
     *   <li>If the given method is an instance of {@code DefaultOperationMethod}, then delegate to
     *     {@link #redimension(int, int)} in order to allow subclasses to defines their own policy.
     *     For example the <cite>Molodensky</cite> method needs to override.</li>
     *   <li>Otherwise for each dimension (<var>source</var> and <var>target</var>):
     *     <ul>
     *       <li>If the corresponding dimension of the given method is {@code null}, then
     *         set that dimension to the given value in a new {@code OperationMethod}.</li>
     *       <li>Otherwise if the given value is not equal to the corresponding dimension
     *         in the given method, throw an {@link IllegalArgumentException}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param  method           The operation method to redimension, or {@code null}.
     * @param  sourceDimensions The desired number of input dimensions.
     * @param  targetDimensions The desired number of output dimensions.
     * @return The redimensioned operation method, or {@code null} if the given method was null,
     *         or {@code method} if no change is needed.
     * @throws IllegalArgumentException if the given dimensions are illegal for the given operation method.
     */
    public static OperationMethod redimension(OperationMethod method,
            final int sourceDimensions, final int targetDimensions)
    {
        if (method != null) {
            if (method instanceof DefaultOperationMethod) {
                return ((DefaultOperationMethod) method).redimension(sourceDimensions, targetDimensions);
            } else {
                method = redimension(method, sourceDimensions, method.getSourceDimensions(),
                                             targetDimensions, method.getTargetDimensions());
            }
        }
        return method;
    }

    /**
     * Returns this operation method with different dimensions, if we are allowed to change dimensionality.
     * See {@link #redimension(OperationMethod, int, int)} for more information.
     *
     * <p>The default implementation performs the following choice:
     * for each dimension (<var>source</var> and <var>target</var>):</p>
     * <ul>
     *   <li>If the corresponding dimension of the given method is {@code null}, then
     *       set that dimension to the given value in a new {@code OperationMethod}.</li>
     *   <li>Otherwise if the given value is not equal to the corresponding dimension
     *       in the given method, throw an {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * Subclasses should override this method if they can work with different number of dimensions.
     * For example a <cite>Molodensky</cite> transform usually works in a three-dimensional space,
     * but can also work in a two-dimensional space by assuming that the ellipsoidal height is zero
     * everywhere.
     *
     * @param  sourceDimensions The desired number of input dimensions.
     * @param  targetDimensions The desired number of output dimensions.
     * @return The redimensioned operation method, or {@code this} if no change is needed.
     * @throws IllegalArgumentException if the given dimensions are illegal for this operation method.
     *
     * @since 0.6
     */
    public OperationMethod redimension(final int sourceDimensions, final int targetDimensions) {
        return redimension(this, sourceDimensions, this.sourceDimensions,
                                 targetDimensions, this.targetDimensions);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code OperationMethod.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this information since GeoAPI does not define {@code OperationMethod}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code OperationMethod.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends OperationMethod> getInterface() {
        return OperationMethod.class;
    }

    /**
     * Returns the base interface of the {@code CoordinateOperation} instances that use this method.
     * The base {@code CoordinateOperation} interface is usually one of the following subtypes:
     *
     * <ul class="verbose">
     *   <li>{@link org.opengis.referencing.operation.Transformation}
     *     if the coordinate operation has some errors (typically of a few metres) because of the empirical process by
     *     which the operation parameters were determined. Those errors do not depend on the floating point precision
     *     or the accuracy of the implementation algorithm.</li>
     *   <li class="verbose">{@link org.opengis.referencing.operation.Conversion}
     *     if the coordinate operation is theoretically of infinite precision, ignoring the limitations of floating
     *     point arithmetic (including rounding errors) and the approximations implied by finite series expansions.</li>
     *   <li>{@link org.opengis.referencing.operation.Projection}
     *     if the coordinate operation is a conversion (as defined above) converting geodetic latitudes and longitudes
     *     to plane (map) coordinates. This type can optionally be refined with one of the
     *     {@link org.opengis.referencing.operation.CylindricalProjection},
     *     {@link org.opengis.referencing.operation.ConicProjection} or
     *     {@link org.opengis.referencing.operation.PlanarProjection} subtypes.</li>
     * </ul>
     *
     * In case of doubt, {@code getOperationType()} can conservatively return the base type.
     * The default implementation returns {@code SingleOperation.class},
     * which is the most conservative return value.
     *
     * @return Interface implemented by all coordinate operations that use this method.
     *
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#getAvailableMethods(Class)
     */
    public Class<? extends SingleOperation> getOperationType() {
        return SingleOperation.class;
    }

    /**
     * Formula(s) or procedure used by this operation method. This may be a reference to a
     * publication. Note that the operation method may not be analytic, in which case this
     * attribute references or contains the procedure, not an analytic formula.
     *
     * <div class="note"><b>Departure from the ISO 19111 standard:</b>
     * this property is mandatory according ISO 19111, but optional in Apache SIS.</div>
     *
     * @return The formula used by this method, or {@code null} if unknown.
     *
     * @see DefaultFormula
     * @see org.apache.sis.referencing.operation.transform.MathTransformProvider
     */
    @Override
    public Formula getFormula() {
        return formula;
    }

    /**
     * Number of dimensions in the source CRS of this operation method.
     * May be null if unknown, as in an <cite>Affine Transform</cite>.
     *
     * @return The dimension of source CRS, or {@code null} if unknown.
     *
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#getSourceDimensions()
     */
    @Override
    @XmlElement(name = "sourceDimensions")
    @XmlSchemaType(name = "positiveInteger")
    public Integer getSourceDimensions() {
        return sourceDimensions;
    }

    /**
     * Number of dimensions in the target CRS of this operation method.
     * May be null if unknown, as in an <cite>Affine Transform</cite>.
     *
     * @return The dimension of target CRS, or {@code null} if unknown.
     *
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#getTargetDimensions()
     */
    @Override
    @XmlElement(name = "targetDimensions")
    @XmlSchemaType(name = "positiveInteger")
    public Integer getTargetDimensions() {
        return targetDimensions;
    }

    /**
     * Returns the set of parameters.
     *
     * <div class="note"><b>Departure from the ISO 19111 standard:</b>
     * this property is mandatory according ISO 19111, but may be null in Apache SIS if the
     * {@link #DefaultOperationMethod(MathTransform)} constructor has been unable to infer it
     * or if this {@code OperationMethod} has been read from an incomplete GML document.</div>
     *
     * @return The parameters, or {@code null} if unknown.
     *
     * @see DefaultConversion#getParameterDescriptors()
     * @see DefaultConversion#getParameterValues()
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return parameters;
    }

    /**
     * Compares this operation method with the specified object for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties
     * are compared including the {@linkplain #getFormula() formula}.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    // Name and identifiers have been compared by super.equals(object, mode).
                    final DefaultOperationMethod that = (DefaultOperationMethod) object;
                    return Objects.equals(this.formula,          that.formula) &&
                           Objects.equals(this.sourceDimensions, that.sourceDimensions) &&
                           Objects.equals(this.targetDimensions, that.targetDimensions) &&
                           Objects.equals(this.parameters,       that.parameters);
                }
                case BY_CONTRACT: {
                    // Name and identifiers have been compared by super.equals(object, mode).
                    if (!Objects.equals(getFormula(), ((OperationMethod) object).getFormula())) {
                        return false;
                    }
                    break;
                }
                default: {
                    /*
                     * Name and identifiers have been ignored by super.equals(object, mode).
                     * Since they are significant for OperationMethod, we compare them here.
                     *
                     * According ISO 19162 (Well Known Text representation of Coordinate Reference Systems),
                     * identifiers shall have precedence over name at least in the case of operation methods
                     * and parameters.
                     */
                    final OperationMethod that = (OperationMethod) object;
                    final Boolean match = Citations.hasCommonIdentifier(getIdentifiers(), that.getIdentifiers());
                    if (match != null) {
                        if (!match) {
                            return false;
                        }
                    } else if (!isHeuristicMatchForName(that.getName().getCode())
                            && !IdentifiedObjects.isHeuristicMatchForName(that, getName().getCode()))
                    {
                        return false;
                    }
                    break;
                }
            }
            final OperationMethod that = (OperationMethod) object;
            return Objects.equals(getSourceDimensions(), that.getSourceDimensions()) &&
                   Objects.equals(getTargetDimensions(), that.getTargetDimensions()) &&
                   Utilities.deepEquals(getParameters(), that.getParameters(), mode);
        }
        return false;
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hash(sourceDimensions, targetDimensions, parameters);
    }

    /**
     * Formats this operation as a <cite>Well Known Text</cite> {@code Method[…]} element.
     *
     * @return {@code "Method"} (WKT 2) or {@code "Projection"} (WKT 1).
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#118">WKT 2 specification §17.2.3</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final boolean isWKT1 = formatter.getConvention().majorVersion() == 1;
        /*
         * The next few lines below are basically a copy of the work done by super.formatTo(formatter),
         * which search for the name to write inside METHOD["name"]. The difference is in the fallback
         * executed if we do not find a name for the given authority.
         */
        final Citation authority = formatter.getNameAuthority();
        String name = IdentifiedObjects.getName(this, authority);
        ElementKind kind = ElementKind.METHOD;
        if (name == null) {
            /*
             * No name found for the given authority. We may use the primary name as a fallback.
             * But before doing that, maybe we can find the name that we are looking for in the
             * hard-coded values in the 'org.apache.sis.internal.referencing.provider' package.
             * The typical use case is when this DefaultOperationMethod has been instantiated
             * by the EPSG factory using only the information found in the EPSG database.
             *
             * We can find the hard-coded names by looking at the ParameterDescriptorGroup of the
             * enclosing ProjectedCRS or DerivedCRS. This is because that parameter descriptor was
             * typically provided by the 'org.apache.sis.internal.referencing.provider' package in
             * order to create the MathTransform associated with the enclosing CRS.  The enclosing
             * CRS is either the immediate parent in WKT 1, or the parent of the parent in WKT 2.
             */
            final FormattableObject parent = formatter.getEnclosingElement(isWKT1 ? 1 : 2);
            if (parent instanceof GeneralDerivedCRS) {
                final Conversion conversion = ((GeneralDerivedCRS) parent).getConversionFromBase();
                if (conversion != null) {   // Should never be null, but let be safe.
                    final ParameterDescriptorGroup descriptor;
                    if (conversion instanceof Parameterized) {  // Usual case in SIS implementation.
                        descriptor = ((Parameterized) conversion).getParameterDescriptors();
                    } else {
                        descriptor = conversion.getParameterValues().getDescriptor();
                    }
                    name = IdentifiedObjects.getName(descriptor, authority);
                }
            }
            if (name == null) {
                name = IdentifiedObjects.getName(this, null);
                if (name == null) {
                    name = Vocabulary.getResources(formatter.getLocale()).getString(Vocabulary.Keys.Unnamed);
                    kind = ElementKind.NAME;  // Because the "Unnamed" string is not a real OperationMethod name.
                }
            }
        }
        formatter.append(name, kind);
        if (isWKT1) {
            /*
             * The WKT 1 keyword is "PROJECTION", which imply that the operation method should be of type
             * org.opengis.referencing.operation.Projection. So strictly speaking only the first check in
             * the following 'if' statement is relevant.
             *
             * Unfortunately in many cases we do not know the operation type, because the method that we
             * invoked - getOperationType() - is not a standard OGC/ISO property, so this information is
             * usually not provided in XML documents for example.  The user could also have instantiated
             * DirectOperationMethod directly without creating a subclass. Consequently we also accept to
             * format the keyword as "PROJECTION" if the operation type *could* be a projection. This is
             * the second check in the following 'if' statement.
             *
             * In other words, the combination of those two checks exclude the following operation types:
             * Transformation, ConcatenatedOperation, PassThroughOperation, or any user-defined type that
             * do not extend Projection. All other operation types are accepted.
             */
            final Class<? extends SingleOperation> type = getOperationType();
            if (Projection.class.isAssignableFrom(type) || type.isAssignableFrom(Projection.class)) {
                return WKTKeywords.Projection;
            }
            formatter.setInvalidWKT(this, null);
        }
        return WKTKeywords.Method;
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
    private DefaultOperationMethod() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getSourceDimensions()
     */
    private void setSourceDimensions(final Integer value) {
        if (sourceDimensions == null) {
            sourceDimensions = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultOperationMethod.class, "setSourceDimensions", "sourceDimensions");
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getTargetDimensions()
     */
    private void setTargetDimensions(final Integer value) {
        if (targetDimensions == null) {
            targetDimensions = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultOperationMethod.class, "setTargetDimensions", "targetDimensions");
        }
    }

    /**
     * Invoked by JAXB for marshalling a citation to the formula. In principle at most one of
     * {@code getFormulaCitation()} and {@link #getFormulaDescription()} methods can return a
     * non-null value. However SIS accepts both coexist (but this is invalid GML).
     */
    @XmlElement(name = "formulaCitation")
    private Citation getFormulaCitation() {
        final Formula formula = getFormula();   // Give to users a chance to override.
        return (formula != null) ? formula.getCitation() : null;
    }

    /**
     * Invoked by JAXB for marshalling the formula literally. In principle at most one of
     * {@code getFormulaDescription()} and {@link #getFormulaCitation()} methods can return
     * a non-null value. However SIS accepts both to coexist (but this is invalid GML).
     */
    @XmlElement(name = "formula")
    private String getFormulaDescription() {
        final Formula formula = getFormula();   // Give to users a chance to override.
        return (formula != null) ? StringAdapter.toString(formula.getFormula()) : null;
    }

    /**
     * Invoked by JAXB for setting the citation to the formula.
     */
    private void setFormulaCitation(final Citation citation) {
        if (formula == null || formula.getCitation() == null) {
            formula = (formula == null) ? new DefaultFormula(citation)
                      : new DefaultFormula(formula.getFormula(), citation);
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultOperationMethod.class, "setFormulaCitation", "formulaCitation");
        }
    }

    /**
     * Invoked by JAXB for setting the formula description.
     */
    private void setFormulaDescription(final String description) {
        if (formula == null || formula.getFormula() == null) {
            formula = (formula == null) ? new DefaultFormula(description)
                      : new DefaultFormula(new SimpleInternationalString(description), formula.getCitation());
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultOperationMethod.class, "setFormulaDescription", "formula");
        }
    }

    /**
     * Invoked by JAXB for getting the parameters to marshal. This method usually marshals the sequence of
     * descriptors without their {@link ParameterDescriptorGroup} wrapper, because GML is defined that way.
     * The {@code ParameterDescriptorGroup} wrapper is a GeoAPI addition done for allowing usage of its
     * methods as a convenience (e.g. {@link ParameterDescriptorGroup#descriptor(String)}).
     *
     * <p>However it could happen that the user really wanted to specify a {@code ParameterDescriptorGroup} as
     * the sole {@code <gml:parameter>} element. We currently have no easy way to distinguish those cases.</p>
     *
     * <div class="note"><b>Tip:</b>
     * One possible way to distinguish the two cases would be to check that the parameter group does not contain
     * any property that this method does not have:
     *
     * {@preformat java
     *   if (IdentifiedObjects.getProperties(this).entrySet().containsAll(
     *       IdentifiedObjects.getProperties(parameters).entrySet())) ...
     * }
     *
     * But we would need to make sure that {@link AbstractSingleOperation#getParameters()} is consistent
     * with the decision taken by this method.</div>
     *
     * <p><b>Historical note:</b> older, deprecated, names for the parameters were:
     * <ul>
     *   <li>{@code includesParameter}</li>
     *   <li>{@code generalOperationParameter} - note that this name was used by the EPSG registry</li>
     *   <li>{@code usesParameter}</li>
     * </ul>
     *
     * @see #getParameters()
     * @see AbstractSingleOperation#getParameters()
     */
    @XmlElement(name = "parameter")
    private GeneralParameterDescriptor[] getDescriptors() {
        if (parameters != null) {
            final List<GeneralParameterDescriptor> descriptors = parameters.descriptors();
            if (descriptors != null) {      // Paranoiac check (should not be allowed).
                return CC_OperationMethod.filterImplicit(descriptors.toArray(
                        new GeneralParameterDescriptor[descriptors.size()]));
            }
        }
        return null;
    }

    /**
     * Invoked by JAXB for setting the unmarshalled parameters.
     * This method wraps the given descriptors in a {@link DefaultParameterDescriptorGroup}.
     *
     * <p>The parameter descriptors created by this method are incomplete since we can not
     * provide a non-null value for {@link ParameterDescriptor#getValueClass()}. The value
     * class will be provided either by replacing this {@code OperationMethod} by one of the
     * pre-defined methods, or by unmarshalling the enclosing {@link AbstractSingleOperation}.</p>
     *
     * <p><b>Maintenance note:</b> the {@code "setDescriptors"} method name is also hard-coded in
     * {@link org.apache.sis.internal.jaxb.referencing.CC_GeneralOperationParameter} for logging purpose.</p>
     *
     * @see AbstractSingleOperation#setParameters
     */
    private void setDescriptors(final GeneralParameterDescriptor[] descriptors) {
        if (parameters == null) {
            parameters = CC_OperationMethod.group(super.getName(), descriptors);
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultOperationMethod.class, "setDescriptors", "parameter");
        }
    }

    /**
     * Invoked by {@link AbstractSingleOperation} for completing the parameter descriptor.
     */
    final void updateDescriptors(final GeneralParameterDescriptor[] descriptors) {
        final ParameterDescriptorGroup previous = parameters;
        parameters = new DefaultParameterDescriptorGroup(IdentifiedObjects.getProperties(previous),
                previous.getMinimumOccurs(), previous.getMaximumOccurs(), descriptors);
    }
}
