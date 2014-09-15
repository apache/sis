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
import javax.measure.unit.Unit;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDirection;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Debug;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Abstract definition of a parameter or group of parameters used by a coordinate operation or a process.
 * This interface combines information provided by Referencing by Coordinates (ISO 19111),
 * Service Metadata (ISO 19115) and Web Processing Services (WPS) standards.
 * The main information are:
 *
 * <table class="ogc">
 *   <caption>Main parameter properties</caption>
 *   <tr>
 *     <th>Getter method</th>
 *     <th>ISO 19111</th>
 *     <th>WPS</th>
 *     <th>ISO 19115</th>
 *     <th class="sep">Remarks</th>
 *   </tr>
 *   <tr>
 *      <td>{@link #getName()}</td>
 *      <td>{@code name}</td>
 *      <td>{@code Identifier}</td>
 *      <td>{@code name}</td>
 *      <td class="sep">See {@linkplain #getName() method javadoc} for {@code MemberName} ↔ {@code Identifier} mapping.</td>
 *   </tr>
 *   <!-- "Title" (WPS) equivalent to "designation" (Feature), but not yet provided. -->
 *   <tr>
 *      <td>{@link #getDescription()}</td>
 *      <td></td>
 *      <td>{@code Abstract}</td>
 *      <td>{@code description}</td>
 *      <td class="sep">Also known as “definition”.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link #getDirection()}</td>
 *      <td></td>
 *      <td></td>
 *      <td>{@code direction}</td>
 *      <td class="sep">Tells if the parameter is a WPS {@code Input} or {@code Output} structure.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link #getMinimumOccurs()}</td>
 *      <td>{@code minimumOccurs}</td>
 *      <td>{@code MinOccurs}</td>
 *      <td>{@code optionality}</td>
 *      <td class="sep">{@code optionality   = (minimumOccurs > 0)}</td>
 *   </tr>
 *   <tr>
 *      <td>{@link #getMaximumOccurs()}</td>
 *      <td>{@code maximumOccurs}</td>
 *      <td>{@code MaxOccurs}</td>
 *      <td>{@code repeatability}</td>
 *      <td class="sep">{@code repeatability = (maximumOccurs > 1)}</td>
 *   </tr>
 * </table>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public abstract class AbstractParameterDescriptor extends AbstractIdentifiedObject implements GeneralParameterDescriptor {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4346475760810353590L;

    /**
     * Key for the {@value} property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getDescription()}.
     */
    public static final String DESCRIPTION_KEY = "description";

    /**
     * A narrative explanation of the role of the parameter, or {@code null} if none.
     */
    private final InternationalString description;

    /**
     * The minimum number of times that values for this parameter group are required, as an unsigned short.
     * We use a short because this value is usually either 0 or 1, or a very small number like 2 or 3.
     * A large number would be a bad idea with this parameter implementation.
     */
    private final short minimumOccurs;

    /**
     * The maximum number of times that values for this parameter group are required, as an unsigned short.
     * Value {@code 0xFFFF} (or -1) means an unrestricted number of occurrences.
     *
     * <p>We use a short because this value is usually 1 or a very small number like 2 or 3. This also serve
     * as a safety since a large number would be a bad idea with this parameter implementation.</p>
     */
    private final short maximumOccurs;

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
     *     <td>{@value org.apache.sis.parameter.AbstractParameterDescriptor#DESCRIPTION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
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
        this.description   = Types.toInternationalString(properties, DESCRIPTION_KEY);
        this.minimumOccurs = (short) minimumOccurs;
        this.maximumOccurs = (short) maximumOccurs;
        if (minimumOccurs < 0 || minimumOccurs > maximumOccurs || maximumOccurs == 0) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        if (maximumOccurs > 0xFFFE && maximumOccurs != Integer.MAX_VALUE) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.TooManyOccurrences_2, 0xFFFE, super.getName()));
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
        description   = descriptor.getDescription();
        minimumOccurs = crop(descriptor.getMinimumOccurs());
        maximumOccurs = crop(descriptor.getMaximumOccurs());
    }

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
     * Returns an indication if the parameter is an input to the service, an output or both.
     * The default implementation returns {@link ParameterDirection#IN}.
     *
     * @return Indication if the parameter is an input or output to the service, or {@code null} if unspecified.
     */
    @Override
    public ParameterDirection getDirection() {
        return ParameterDirection.IN;
    }

    /**
     * Returns a narrative explanation of the role of the parameter.
     *
     * @return A narrative explanation of the role of the parameter, or {@code null} if none.
     */
    @Override
    public InternationalString getDescription() {
        return description;
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
                           maximumOccurs == that.maximumOccurs &&
                           Objects.equals(description, that.description);
                }
                default: {
                    final GeneralParameterDescriptor that = (GeneralParameterDescriptor) object;
                    return getMinimumOccurs() == that.getMinimumOccurs() &&
                           getMaximumOccurs() == that.getMaximumOccurs() &&
                           deepEquals(getDescription(), that.getDescription(), mode);
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
            return "ParameterGroup";
        } else if (this instanceof ParameterDescriptor<?>) {
            formatter.appendAny(((ParameterDescriptor<?>) this).getDefaultValue());
            final Unit<?> unit = ((ParameterDescriptor<?>) this).getUnit();
            if (unit != null) {
                if (!formatter.getConvention().isSimplified() || !unit.equals(formatter.toContextualUnit(unit))) {
                    formatter.append(unit);
                }
            }
        }
        return "Parameter";
    }
}
