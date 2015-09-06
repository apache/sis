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
package org.apache.sis.parameter;

import java.util.Map;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlSchemaType;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Debug;

import static org.apache.sis.internal.jaxb.referencing.CC_GeneralOperationParameter.DEFAULT_OCCURRENCE;

/**
 * Abstract definition of a parameter or group of parameters used by a coordinate operation or a process.
 * This interface combines information provided by Referencing by Coordinates (ISO 19111),
 * Service Metadata (ISO 19115) and Web Processing Services (WPS) standards.
 * The main information are:
 *
 * <table class="sis">
 *   <caption>Main parameter properties</caption>
 *   <tr>
 *     <th>Getter method</th>
 *     <th class="sep">ISO 19111</th>
 *     <th class="sep">WPS</th>
 *     <th class="sep">ISO 19115</th>
 *     <th class="sep">Remarks</th>
 *   </tr>
 *   <tr>
 *     <td>{@link #getName() getName()}</td>
 *     <td class="sep">{@code name}</td>
 *     <td class="sep">{@code Identifier}</td>
 *     <td class="sep">{@code name}</td>
 *     <td class="sep">See {@link Parameters#getMemberName(ParameterDescriptor)} for {@code MemberName} ↔ {@code Identifier} mapping.</td>
 *   </tr>
 *   <!-- "Title" (WPS) equivalent to "designation" (Feature), but not yet provided. -->
 *   <tr>
 *     <td>{@link #getDescription()}</td>
 *     <td class="sep"></td>
 *     <td class="sep">{@code Abstract}</td>
 *     <td class="sep">{@code description}</td>
 *     <td class="sep">Also known as “definition”.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getDirection()}</td>
 *     <td class="sep"></td>
 *     <td class="sep"></td>
 *     <td class="sep">{@code direction}</td>
 *     <td class="sep">Tells if the parameter is a WPS {@code Input} or {@code Output} structure.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getMinimumOccurs()}</td>
 *     <td class="sep">{@code minimumOccurs}</td>
 *     <td class="sep">{@code MinOccurs}</td>
 *     <td class="sep">{@code optionality}</td>
 *     <td class="sep">{@code optionality   = (minimumOccurs > 0)}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getMaximumOccurs()}</td>
 *     <td class="sep">{@code maximumOccurs}</td>
 *     <td class="sep">{@code MaxOccurs}</td>
 *     <td class="sep">{@code repeatability}</td>
 *     <td class="sep">{@code repeatability = (maximumOccurs > 1)}</td>
 *   </tr>
 * </table>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
@XmlType(name = "AbstractGeneralOperationParameterType", propOrder = {
    "nonDefaultMinimumOccurs",
    "nonDefaultMaximumOccurs"
})
@XmlSeeAlso({
    DefaultParameterDescriptor.class,
    DefaultParameterDescriptorGroup.class
})
public abstract class AbstractParameterDescriptor extends AbstractIdentifiedObject implements GeneralParameterDescriptor {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4346475760810353590L;

    /**
     * The minimum number of times that values for this parameter group are required, as an unsigned short.
     * We use a short because this value is usually either 0 or 1, or a very small number like 2 or 3.
     * A large number would be a bad idea with this parameter implementation.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setNonDefaultMinimumOccurs(Integer)}</p>
     */
    private short minimumOccurs;

    /**
     * The maximum number of times that values for this parameter group are required, as an unsigned short.
     * Value {@code 0xFFFF} (or -1) means an unrestricted number of occurrences.
     *
     * <p>We use a short because this value is usually 1 or a very small number like 2 or 3. This also serve
     * as a safety since a large number would be a bad idea with this parameter implementation.</p>
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setNonDefaultMaximumOccurs(Integer)}</p>
     */
    private short maximumOccurs;

    /**
     * Constructs a parameter descriptor from a set of properties. The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties    The properties to be given to the identified object.
     * @param minimumOccurs The {@linkplain #getMinimumOccurs() minimum number of times} that values
     *                      for this parameter group are required, or 0 if no restriction.
     * @param maximumOccurs The {@linkplain #getMaximumOccurs() maximum number of times} that values
     *                      for this parameter group are required, or {@link Integer#MAX_VALUE} if no restriction.
     */
    protected AbstractParameterDescriptor(final Map<String,?> properties,
            final int minimumOccurs, final int maximumOccurs)
    {
        super(properties);
        this.minimumOccurs = (short) minimumOccurs;
        this.maximumOccurs = (short) maximumOccurs;
        if (minimumOccurs < 0 || minimumOccurs > maximumOccurs || maximumOccurs == 0) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        if (maximumOccurs > 0xFFFE && maximumOccurs != Integer.MAX_VALUE) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.TooManyOccurrences_2, 0xFFFE, super.getName().getCode()));
        }
    }

    /**
     * Constructs a new parameter descriptor with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param descriptor The object to shallow copy.
     */
    protected AbstractParameterDescriptor(final GeneralParameterDescriptor descriptor) {
        super(descriptor);
        minimumOccurs = crop(descriptor.getMinimumOccurs());
        maximumOccurs = crop(descriptor.getMaximumOccurs());
    }

    // NOTE: There is no 'castOrCopy' static method in this class because AbstractParameterDescriptor is abstract.
    // If nevertheless we choose to add such method in the future, then CC_GeneralOperationParameter.getElement()
    // should be simplified.

    /**
     * Crops the given integer in the [0 … 0xFFFF] range.
     */
    private static short crop(final int n) {
        return (short) Math.max(0, Math.min(0xFFFF, n));
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code GeneralParameterDescriptor.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The parameter descriptor interface implemented by this class.
     */
    @Override
    public Class<? extends GeneralParameterDescriptor> getInterface() {
        return GeneralParameterDescriptor.class;
    }

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     * A value of 0 means an optional parameter.
     *
     * @return The minimum occurrence.
     */
    @Override
    public int getMinimumOccurs() {
        return minimumOccurs & 0xFFFF;
    }

    /**
     * The maximum number of times that values for this parameter group or parameter can be included.
     * A value greater than 1 means a repeatable parameter.
     *
     * @return The maximum occurrence.
     */
    @Override
    public int getMaximumOccurs() {
        return (maximumOccurs != -1) ? (maximumOccurs & 0xFFFF) : Integer.MAX_VALUE;
    }

    /**
     * Compares the specified object with this parameter for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final AbstractParameterDescriptor that = (AbstractParameterDescriptor) object;
                    return minimumOccurs == that.minimumOccurs &&
                           maximumOccurs == that.maximumOccurs;
                }
                default: {
                    final GeneralParameterDescriptor that = (GeneralParameterDescriptor) object;
                    return getMinimumOccurs() == that.getMinimumOccurs() &&
                           getMaximumOccurs() == that.getMaximumOccurs();
                }
            }
        }
        return false;
    }

    /**
     * Returns a string representation of this descriptor.
     *
     * <p>This method is for information purpose only and may change in future SIS version.</p>
     */
    @Debug
    @Override
    public String toString() {
        if (this instanceof ParameterDescriptorGroup) {
            return ParameterFormat.sharedFormat(this);
        } else {
            return super.toString();
        }
    }

    /**
     * Prints a string representation of this descriptor to the {@linkplain System#out standard output stream}.
     * If a {@linkplain java.io.Console console} is attached to the running JVM (i.e. if the application is run
     * from the command-line and the output is not redirected to a file) and if Apache SIS thinks that the console
     * supports the ANSI escape codes (a.k.a. X3.64), then a syntax coloring will be applied.
     *
     * <p>This is a convenience method for debugging purpose and for console applications.</p>
     */
    @Debug
    @Override
    public void print() {
        if (this instanceof ParameterDescriptorGroup) {
            ParameterFormat.print(this);
        } else {
            super.print();
        }
    }

    /**
     * Formats this descriptor as a pseudo-<cite>Well Known Text</cite> element. The WKT specification
     * does not define any representation of parameter descriptors. Apache SIS fallback on a list of
     * {@linkplain DefaultParameterDescriptor#formatTo(Formatter) descriptors}.
     * The text formatted by this method is {@linkplain Formatter#setInvalidWKT flagged as invalid WKT}.
     *
     * @param  formatter The formatter where to format the inner content of this WKT element.
     * @return {@code "Parameter"} or {@code "ParameterGroup"}.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        formatter.setInvalidWKT(this, null);
        if (this instanceof ParameterDescriptorGroup) {
            for (GeneralParameterDescriptor parameter : ((ParameterDescriptorGroup) this).descriptors()) {
                if (!(parameter instanceof FormattableObject)) {
                    if (parameter instanceof ParameterDescriptor<?>) {
                        parameter = new DefaultParameterDescriptor((ParameterDescriptor<?>) parameter);
                    } else if (parameter instanceof ParameterDescriptorGroup) {
                        parameter = new DefaultParameterDescriptorGroup((ParameterDescriptorGroup) parameter);
                    } else {
                        continue;
                    }
                }
                formatter.newLine();
                formatter.append((FormattableObject) parameter);
            }
            return WKTKeywords.ParameterGroup;
        } else if (this instanceof ParameterDescriptor<?>) {
            final Object defaultValue = ((ParameterDescriptor<?>) this).getDefaultValue();
            if (defaultValue != null) {
                formatter.appendAny(defaultValue);
            }
            formatter.append(((ParameterDescriptor<?>) this).getUnit());
        }
        return WKTKeywords.Parameter;
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractParameterDescriptor() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
        minimumOccurs = DEFAULT_OCCURRENCE;  // Default value if XML element is omitted.
        maximumOccurs = DEFAULT_OCCURRENCE;
    }

    /**
     * Invoked by JAXB for marshalling the {@link #minimumOccurs} value. Omit marshalling of this
     * {@code gml:minimumOccurs} element if its value is equals to the default value, which is 1.
     */
    @XmlElement(name = "minimumOccurs")
    @XmlSchemaType(name = "nonNegativeInteger")
    private Integer getNonDefaultMinimumOccurs() {
        final int n = getMinimumOccurs();
        return (n != DEFAULT_OCCURRENCE) ? n : null;
    }

    /**
     * Invoked by JAXB for marshalling the {@link #maximumOccurs} value. Omit marshalling of this
     * {@code gml:maximumOccurs} element if its value is equals to the default value, which is 1.
     *
     * <p>This property should not be marshalled in {@link DefaultParameterDescriptor} objects (the GML schema
     * does not allow that). It should be marshalled only for {@link DefaultParameterDescriptorGroup} objects.
     * Since SIS marshals {@code minimumOccurs} and {@code maximumOccurs} properties only when their value is
     * different than 1, and since {@code ParameterDescriptor} should not have a {@code maximumOccurs} value
     * different than 1 when ISO 19111 compliance is desired, the GML document should be valid in most cases.</p>
     */
    @XmlElement(name = "maximumOccurs")
    @XmlSchemaType(name = "nonNegativeInteger")
    private Integer getNonDefaultMaximumOccurs() {
        final int n = getMaximumOccurs();
        return (n != DEFAULT_OCCURRENCE) ? n : null;
    }

    /**
     * Invoked by JAXB for unmarshalling the {@link #minimumOccurs} value.
     */
    private void setNonDefaultMinimumOccurs(final Integer n) {
        minimumOccurs = (n != null) ? crop(n) : DEFAULT_OCCURRENCE;
    }

    /**
     * Invoked by JAXB for unmarshalling the {@link #maximumOccurs} value.
     */
    private void setNonDefaultMaximumOccurs(final Integer n) {
        maximumOccurs = (n != null) ? crop(n) : DEFAULT_OCCURRENCE;
    }
}
