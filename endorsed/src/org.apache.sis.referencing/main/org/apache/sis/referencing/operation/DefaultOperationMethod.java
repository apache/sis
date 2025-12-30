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
import java.util.Objects;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Formula;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.internal.shared.NilReferencingObject;
import org.apache.sis.xml.bind.gco.StringAdapter;
import org.apache.sis.xml.bind.referencing.CC_OperationMethod;
import org.apache.sis.metadata.internal.shared.Identifiers;
import org.apache.sis.metadata.internal.shared.ImplementationHelper;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.FormattableObject;

// Specific to the main and geoapi-3.1 branches:
import jakarta.xml.bind.annotation.XmlSchemaType;
import org.opengis.referencing.crs.GeneralDerivedCRS;


/**
 * Describes the algorithm and parameters used to perform a coordinate operation. An {@code OperationMethod}
 * is a kind of metadata: it does not perform any coordinate operation (e.g. map projection) by itself, but
 * tells us what is needed in order to perform such operation.
 *
 * <p>The most important parts of an {@code OperationMethod} are its {@linkplain #getName() name} and its
 * {@linkplain #getParameters() group of parameter descriptors}. The parameter descriptors do not contain
 * any value, but tell us what are the expected parameters, together with their units of measurement.</p>
 *
 * <p>In Apache SIS implementation, the {@linkplain #getName() name} is the only mandatory property.
 * However, it is recommended to provide also {@linkplain #getIdentifiers() identifiers}
 * (e.g. “EPSG:9804” in the following example)
 * because names can sometimes be ambiguous or be spelled in different ways.</p>
 *
 * <h2>Example</h2>
 * An operation method named <q>Mercator (variant A)</q> (EPSG:9804) expects the following parameters:
 * <ul>
 *   <li><q>Latitude of natural origin</q> in degrees. Default value is 0°.</li>
 *   <li><q>Longitude of natural origin</q> in degrees. Default value is 0°.</li>
 *   <li><q>Scale factor at natural origin</q> as a dimensionless number. Default value is 1.</li>
 *   <li><q>False easting</q> in metres. Default value is 0 m.</li>
 *   <li><q>False northing</q> in metres. Default value is 0 m.</li>
 * </ul>
 *
 * <h2>Departure from the ISO 19111 standard</h2>
 * The following properties are mandatory according ISO 19111,
 * but may be missing under some conditions in Apache SIS:
 * <ul>
 *   <li>The {@linkplain #getFormula() formula} if it has not been provided to the
 *     {@linkplain #DefaultOperationMethod(Map, ParameterDescriptorGroup) constructor}, or if it
 *     cannot be {@linkplain #DefaultOperationMethod(MathTransform) inferred from the given math transform}.</li>
 *   <li>The {@linkplain #getParameters() parameters} if the {@link #DefaultOperationMethod(MathTransform)}
 *     constructor cannot infer them.</li>
 * </ul>
 *
 * <h2>Relationship with other classes or interfaces</h2>
 * {@code OperationMethod} describes parameters without providing any value (except sometimes default values).
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
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thread-safe if all properties given to the constructor are also immutable and thread-safe.
 * It is strongly recommended for all subclasses to be thread-safe, especially the
 * {@link org.apache.sis.referencing.operation.transform.MathTransformProvider} implementations to be used with
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see DefaultConversion
 * @see DefaultTransformation
 * @see org.apache.sis.referencing.operation.transform.MathTransformProvider
 *
 * @since 0.5
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
    private static final long serialVersionUID = 6612049971779439502L;

    /**
     * Formula(s) or procedure used by this operation method. This may be a reference to a publication.
     * Note that the operation method may not be analytic, in which case this attribute references or
     * contains the procedure, not an analytic formula.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setFormulaCitation(Citation)}
     * or {@link #setFormulaDescription(String)}.</p>
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private Formula formula;

    /**
     * The set of parameters, or {@code null} if none.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDescriptors(GeneralParameterDescriptor[])}
     * or {@link #afterUnmarshal(Unmarshaller, Object)}.</p>
     */
    @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
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
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.operation.OperationMethod#FORMULA_KEY}</td>
     *     <td>{@link Formula}, {@link Citation} or {@link CharSequence}</td>
     *     <td>{@link #getFormula()}</td>
     *   </tr><tr>
     *     <th colspan="3" class="hsep">Defined in parent classes (reminder)</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  set of properties. Shall contain at least {@code "name"}.
     * @param  parameters  description of parameters expected by this operation.
     *
     * @since 1.1
     */
    public DefaultOperationMethod(final Map<String,?> properties,
                                  final ParameterDescriptorGroup parameters)
    {
        super(properties);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        Object value = properties.get(FORMULA_KEY);
        if (value == null || value instanceof Formula) {
            formula = (Formula) value;
        } else if (value instanceof Citation) {
            formula = new DefaultFormula((Citation) value);
        } else if (value instanceof CharSequence) {
            formula = new DefaultFormula((CharSequence) value);
        } else {
            throw new IllegalArgumentException(Errors.forProperties(properties)
                    .getString(Errors.Keys.IllegalPropertyValueClass_2, FORMULA_KEY, value.getClass()));
        }
        this.parameters = parameters;
    }

    /**
     * Convenience constructor that creates an operation method from a math transform.
     * The information provided in the newly created object are approximations, and
     * usually acceptable only as a fallback when no other information are available.
     *
     * @param  transform  the math transform to describe.
     */
    public DefaultOperationMethod(final MathTransform transform) {
        super(getProperties(transform));
        if (transform instanceof Parameterized) {
            parameters = ((Parameterized) transform).getParameterDescriptors();
        }
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="7", fixed="25")
    private static Map<String,?> getProperties(final MathTransform transform) {
        ArgumentChecks.ensureNonNull("transform", transform);
        if (transform instanceof Parameterized) {
            final ParameterDescriptorGroup parameters = ((Parameterized) transform).getParameterDescriptors();
            if (parameters != null) {
                return getProperties(parameters, null);
            }
        }
        return Map.of(NAME_KEY, NilReferencingObject.UNNAMED);
    }

    /**
     * Returns the properties to be given to an identified object derived from the specified one.
     * This method returns the same properties as the supplied argument
     * (as of <code>{@linkplain IdentifiedObjects#getProperties getProperties}(info)</code>),
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
     * @param  info       the identified object to view as a properties map.
     * @param  authority  the new authority for the object to be created,
     *                    or {@code null} if it is not going to have any declared authority.
     * @return the identified object properties in a mutable map.
     */
    private static Map<String,Object> getProperties(final IdentifiedObject info, final Citation authority) {
        final Map<String,Object> properties = new HashMap<>(IdentifiedObjects.getProperties(info));
        properties.put(NAME_KEY, new NamedIdentifier(authority, info.getName().getCode()));
        properties.remove(IDENTIFIERS_KEY);
        return properties;
    }

    /**
     * Creates a new operation method with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  method  the operation method to copy.
     *
     * @see #castOrCopy(OperationMethod)
     */
    protected DefaultOperationMethod(final OperationMethod method) {
        super(method);
        formula    = method.getFormula();
        parameters = method.getParameters();
    }

    /**
     * Returns a SIS operation method implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then {@code null} is returned.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultOperationMethod castOrCopy(final OperationMethod object) {
        return (object == null) || (object instanceof DefaultOperationMethod)
               ? (DefaultOperationMethod) object : new DefaultOperationMethod(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code OperationMethod.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this information since GeoAPI does not define {@code OperationMethod}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
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
     *   <li class="verbose">{@link org.opengis.referencing.operation.Conversion}
     *     if the coordinate operation is theoretically of infinite precision, ignoring the limitations of floating
     *     point arithmetic (including rounding errors) and the approximations implied by finite series expansions.</li>
     *   <li>{@link org.opengis.referencing.operation.Transformation}
     *     if the coordinate operation has some errors (typically of a few metres) because of the empirical process by
     *     which the operation parameters were determined. Those errors do not depend on the floating point precision
     *     or the accuracy of the implementation algorithm.</li>
     *   <li>{@link org.opengis.referencing.operation.PointMotionOperation}
     *     if the coordinate operation applies changes due to the motion of points between two coordinate epochs.</li>
     * </ul>
     *
     * In case of doubt, {@code getOperationType()} can conservatively return the base type.
     * The default implementation returns {@code SingleOperation.class},
     * which is the most conservative return value.
     *
     * @return interface implemented by all coordinate operations that use this method.
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
     * <h4>Departure from the ISO 19111 standard</h4>
     * This property is mandatory according ISO 19111, but optional in Apache SIS.
     *
     * @return the formula used by this method, or {@code null} if unknown.
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
     * May be null if unknown, as in an <i>Affine Transform</i>.
     *
     * @return the dimension of source CRS, or {@code null} if unknown.
     *
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#getSourceDimensions()
     *
     * @deprecated This attribute has been removed from ISO 19111:2019.
     */
    @Override
    @Deprecated(since="1.1")
    @XmlElement(name = "sourceDimensions")
    @XmlSchemaType(name = "positiveInteger")
    public Integer getSourceDimensions() {
        return null;
    }

    /**
     * Number of dimensions in the target CRS of this operation method.
     * May be null if unknown, as in an <i>Affine Transform</i>.
     *
     * @return the dimension of target CRS, or {@code null} if unknown.
     *
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#getTargetDimensions()
     *
     * @deprecated This attribute has been removed from ISO 19111:2019.
     */
    @Override
    @Deprecated(since="1.1")
    @XmlElement(name = "targetDimensions")
    @XmlSchemaType(name = "positiveInteger")
    public Integer getTargetDimensions() {
        return null;
    }

    /**
     * Returns the set of parameters.
     *
     * <h4>Departure from the ISO 19111 standard</h4>
     * This property is mandatory according ISO 19111, but may be {@code null} in Apache SIS if the
     * {@link #DefaultOperationMethod(MathTransform)} constructor has been unable to infer it.
     *
     * @return the parameters, or {@code null} if unknown.
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
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                                                // Slight optimization.
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    // Name and identifiers have been compared by super.equals(object, mode).
                    final var that = (DefaultOperationMethod) object;
                    return Objects.equals(this.formula,    that.formula) &&
                           Objects.equals(this.parameters, that.parameters);
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
                    final var that = (OperationMethod) object;
                    final Boolean match = Identifiers.hasCommonIdentifier(getIdentifiers(), that.getIdentifiers());
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
            final var that = (OperationMethod) object;
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
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(parameters);
    }

    /**
     * Formats this operation as a <i>Well Known Text</i> {@code Method[…]} element.
     *
     * @return {@code "Method"} (WKT 2) or {@code "Projection"} (WKT 1).
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
             * hard-coded values in the 'org.apache.sis.referencing.operation.provider' package.
             * The typical use case is when this DefaultOperationMethod has been instantiated
             * by the EPSG factory using only the information found in the EPSG database.
             *
             * We can find the hard-coded names by looking at the ParameterDescriptorGroup of the
             * enclosing ProjectedCRS or DerivedCRS. This is because that parameter descriptor was
             * typically provided by the 'org.apache.sis.referencing.operation.provider' package in
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
                    name = Vocabulary.forLocale(formatter.getLocale()).getString(Vocabulary.Keys.Unnamed);
                    kind = ElementKind.NAME;  // Because the "Unnamed" string is not a real OperationMethod name.
                }
            }
        }
        formatter.append(name, kind);
        if (isWKT1) {
            /*
             * The WKT 1 keyword is "PROJECTION", which imply that the operation method should be of type
             * org.opengis.referencing.operation.Conversion. So strictly speaking only the first check in
             * the following 'if' statement is relevant, and we should also check CRS types.
             *
             * Unfortunately in many cases we do not know the operation type, because the method that we
             * invoked - getOperationType() - is not a standard OGC/ISO property, so this information is
             * usually not provided in XML documents for example.  The user could also have instantiated
             * DirectOperationMethod directly without creating a subclass. Consequently, we also accept to
             * format the keyword as "PROJECTION" if the operation type *could* be a projection. This is
             * the second check in the following 'if' statement.
             *
             * In other words, the combination of those two checks exclude the following operation types:
             * Transformation, ConcatenatedOperation, PassThroughOperation, or any user-defined type that
             * do not extend Conversion. All other operation types are accepted.
             */
            final Class<? extends SingleOperation> type = getOperationType();
            if (Conversion.class.isAssignableFrom(type) || type.isAssignableFrom(Conversion.class)) {
                return WKTKeywords.Projection;
            }
            formatter.setInvalidWKT(this, null);
        }
        return WKTKeywords.Method;
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
    private DefaultOperationMethod() {
        super(org.apache.sis.referencing.internal.shared.NilReferencingObject.INSTANCE);
    }

    /**
     * Invoked by JAXB for marshalling a citation to the formula. In principle at most one of
     * {@code getFormulaCitation()} and {@link #getFormulaDescription()} methods can return a
     * non-null value. However, SIS accepts both coexist (but this is invalid GML).
     */
    @XmlElement(name = "formulaCitation")
    private Citation getFormulaCitation() {
        final Formula formula = getFormula();   // Give to users a chance to override.
        return (formula != null) ? formula.getCitation() : null;
    }

    /**
     * Invoked by JAXB for marshalling the formula literally. In principle at most one of
     * {@code getFormulaDescription()} and {@link #getFormulaCitation()} methods can return
     * a non-null value. However, SIS accepts both to coexist (but this is invalid GML).
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
            ImplementationHelper.propertyAlreadySet(DefaultOperationMethod.class, "setFormulaCitation", "formulaCitation");
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
            ImplementationHelper.propertyAlreadySet(DefaultOperationMethod.class, "setFormulaDescription", "formula");
        }
    }

    /**
     * Invoked by JAXB for getting the parameters to marshal. This method usually marshals the sequence of
     * descriptors without their {@link ParameterDescriptorGroup} wrapper, because GML is defined that way.
     * The {@code ParameterDescriptorGroup} wrapper is a GeoAPI addition done for allowing usage of its
     * methods as a convenience (e.g. {@link ParameterDescriptorGroup#descriptor(String)}).
     *
     * <p>However, it could happen that the user really wanted to specify a {@code ParameterDescriptorGroup} as
     * the sole {@code <gml:parameter>} element. We currently have no easy way to distinguish those cases.</p>
     *
     * <h4>Tip</h4>
     * One possible way to distinguish the two cases would be to check that the parameter group does not contain
     * any property that this method does not have:
     *
     * {@snippet lang="java" :
     *     if (IdentifiedObjects.getProperties(this).entrySet().containsAll(
     *         IdentifiedObjects.getProperties(parameters).entrySet())) ...
     *     }
     *
     * But we would need to make sure that {@link AbstractSingleOperation#getParameters()} is consistent
     * with the decision taken by this method.
     *
     * <h4>Historical note</h4>
     * Older, deprecated, names for the parameters were:
     * <ul>
     *   <li>{@code includesParameter}</li>
     *   <li>{@code generalOperationParameter} - note that this name was used by the EPSG repository</li>
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
                return CC_OperationMethod.filterImplicit(descriptors.toArray(GeneralParameterDescriptor[]::new));
            }
        }
        return null;
    }

    /**
     * Invoked by JAXB for setting the unmarshalled parameters.
     * This method wraps the given descriptors in a {@link DefaultParameterDescriptorGroup}.
     *
     * <p>The parameter descriptors created by this method are incomplete since we cannot
     * provide a non-null value for {@link ParameterDescriptor#getValueClass()}. The value
     * class will be provided either by replacing this {@code OperationMethod} by one of the
     * predefined methods, or by unmarshalling the enclosing {@link AbstractSingleOperation}.</p>
     *
     * <p><b>Maintenance note:</b> the {@code "setDescriptors"} method name is also hard-coded in
     * {@link org.apache.sis.xml.bind.referencing.CC_GeneralOperationParameter} for logging purpose.</p>
     *
     * @see AbstractSingleOperation#setParameters
     */
    private void setDescriptors(final GeneralParameterDescriptor[] descriptors) {
        if (parameters == null) {
            parameters = CC_OperationMethod.group(super.getName(), descriptors);
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultOperationMethod.class, "setDescriptors", "parameter");
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

    /**
     * Invoked by JAXB after unmarshalling. If the {@code <gml:OperationMethod>} element does not contain
     * any {@code <gml:parameter>}, we assume that this is a valid parameterless operation (as opposed to
     * an operation with unknown parameters). We need this assumption because, contrarily to GeoAPI model,
     * the GML schema does not differentiate "no parameters" from "unspecified parameters".
     */
    private void afterUnmarshal(final Unmarshaller unmarshaller, final Object parent) {
        if (parameters == null) {
            parameters = CC_OperationMethod.group(super.getName(), new GeneralParameterDescriptor[0]);
        }
    }
}
