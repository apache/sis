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
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Formula;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.ArgumentChecks.*;

// Branch-dependent imports
import java.util.Objects;


/**
 * Defines the algorithm and describes the parameters used to perform a coordinate operation. An {@code OperationMethod}
 * contains an arbitrary amount of {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor parameter descriptors}.
 * Values for those parameters will be assigned by {@linkplain DefaultSingleOperation coordinate operations}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.5
 * @since   0.5 (derived from geotk-2.0)
 * @module
 *
 * @see DefaultSingleOperation
 */
public class DefaultOperationMethod extends AbstractIdentifiedObject implements OperationMethod {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8181774670648793964L;

    /**
     * Formula(s) or procedure used by this operation method. This may be a reference to a publication.
     * Note that the operation method may not be analytic, in which case this attribute references or
     * contains the procedure, not an analytic formula.
     */
    private final Formula formula;

    /**
     * Number of dimensions in the source CRS of this operation method.
     * May be {@code null} if this method can work with any number of
     * source dimensions (e.g. <cite>Affine Transform</cite>).
     */
    private final Integer sourceDimension;

    /**
     * Number of dimensions in the target CRS of this operation method.
     * May be {@code null} if this method can work with any number of
     * target dimensions (e.g. <cite>Affine Transform</cite>).
     */
    private final Integer targetDimension;

    /**
     * The set of parameters, or {@code null} if none.
     */
    private final ParameterDescriptorGroup parameters;

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
     * @param properties      Set of properties. Shall contain at least {@code "name"}.
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     * @param parameters      The set of parameters.
     */
    public DefaultOperationMethod(final Map<String,?> properties,
                                  final Integer sourceDimension,
                                  final Integer targetDimension,
                                  final ParameterDescriptorGroup parameters)
    {
        super(properties);
        if (sourceDimension != null) ensurePositive("sourceDimension", sourceDimension);
        if (targetDimension != null) ensurePositive("targetDimension", targetDimension);
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
                    .getString(Errors.Keys.IllegalPropertyClass_2, FORMULA_KEY, value.getClass()));
        }
        this.parameters      = parameters;
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
    }

    /**
     * Convenience constructor that creates an operation method from a math transform.
     * The information provided in the newly created object are approximative, and
     * usually acceptable only as a fallback when no other information are available.
     *
     * @param transform The math transform to describe.
     */
    public DefaultOperationMethod(final MathTransform transform) {
        this(getProperties(transform),
             transform.getSourceDimensions(),
             transform.getTargetDimensions(),
             (transform instanceof Parameterized) ? ((Parameterized) transform).getParameterDescriptors() : null);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private static Map<String,?> getProperties(final MathTransform transform) {
        ensureNonNull("transform", transform);
        if (transform instanceof Parameterized) {
            final ParameterDescriptorGroup parameters = ((Parameterized) transform).getParameterDescriptors();
            if (parameters != null) {
                return getProperties(parameters, null);
            }
        }
        return Collections.singletonMap(NAME_KEY, Vocabulary.format(Vocabulary.Keys.Unnamed));
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
    static Map<String,Object> getProperties(final IdentifiedObject info, final Citation authority) {
        final Map<String,Object> properties = new HashMap<>(IdentifiedObjects.getProperties(info));
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
        formula         = method.getFormula();
        parameters      = method.getParameters();
        sourceDimension = method.getSourceDimensions();
        targetDimension = method.getTargetDimensions();
    }

    /**
     * Returns a SIS operation method implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
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
     * @param method The operation method to copy.
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     */
    private DefaultOperationMethod(final OperationMethod method,
                                   final Integer sourceDimension,
                                   final Integer targetDimension)
    {
        super(method);
        this.formula    = method.getFormula();
        this.parameters = method.getParameters();
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
    }

    /**
     * Returns an operation method with the same values than the specified one except the dimensions.
     * The source and target dimensions may be {@code null} if this method can work with any number of dimensions
     * (e.g. <cite>Affine Transform</cite>).
     *
     * @param  method The operation method to redimension, or {@code null}.
     * @param  sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param  targetDimension Number of dimensions in the target CRS of this operation method.
     * @return The redimensioned operation method, or {@code method} if the given method was {@code null}
     *         or already had th given dimensions.
     */
    public static OperationMethod redimension(OperationMethod method,
                                        final Integer sourceDimension,
                                        final Integer targetDimension)
    {
        if (sourceDimension != null) ensurePositive("sourceDimension", sourceDimension);
        if (targetDimension != null) ensurePositive("targetDimension", targetDimension);
        if (method != null && !(Objects.equals(sourceDimension, method.getSourceDimensions())
                             && Objects.equals(targetDimension, method.getTargetDimensions())))
        {
            method = new DefaultOperationMethod(method, sourceDimension, targetDimension);
        }
        return method;
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code OperationMethod.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code OperationMethod}
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
     * Formula(s) or procedure used by this operation method. This may be a reference to a
     * publication. Note that the operation method may not be analytic, in which case this
     * attribute references or contains the procedure, not an analytic formula.
     */
    @Override
    public Formula getFormula() {
        return formula;
    }

    /**
     * Number of dimensions in the source CRS of this operation method.
     * May be null if unknown, as in an <cite>Affine Transform</cite>.
     */
    @Override
    public Integer getSourceDimensions() {
        return sourceDimension;
    }

    /**
     * Number of dimensions in the target CRS of this operation method.
     * May be null if unknown, as in an <cite>Affine Transform</cite>.
     */
    @Override
    public Integer getTargetDimensions() {
        return targetDimension;
    }

    /**
     * Returns the set of parameters.
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
                    return Objects.equals(this.formula,         that.formula) &&
                           Objects.equals(this.sourceDimension, that.sourceDimension) &&
                           Objects.equals(this.targetDimension, that.targetDimension) &&
                           Objects.equals(this.parameters,      that.parameters);
                }
                case BY_CONTRACT: {
                    // Name and identifiers have been compared by super.equals(object, mode).
                    if (!Objects.equals(getFormula(), ((OperationMethod) object).getFormula())) {
                        return false;
                    }
                    break;
                }
                default: {
                    // Name and identifiers have been ignored by super.equals(object, mode).
                    // Since they are significant for OperationMethod, compare them here.
                    final OperationMethod that = (OperationMethod) object;
                    if (!isHeuristicMatchForName(that.getName().getCode())
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
        return super.computeHashCode() + Objects.hash(sourceDimension, targetDimension, parameters);
    }

    /**
     * Formats this operation as a <cite>Well Known Text</cite> {@code Method[…]} element.
     *
     * @return {@code "Method"} (WKT 2) or {@code "Projection"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return (formatter.getConvention().majorVersion() == 1) ? "Projection" : "Method";
    }
}
