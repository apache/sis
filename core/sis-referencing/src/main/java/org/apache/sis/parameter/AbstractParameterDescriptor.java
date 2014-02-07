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
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;


/**
 * The base class of descriptor for single parameter value or group of parameter values.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public abstract class AbstractParameterDescriptor extends AbstractIdentifiedObject
           implements GeneralParameterDescriptor
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2630644278783845276L;

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     */
    private final int minimumOccurs;

    /**
     * Creates a descriptor from a set of properties. The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
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
     * @param minimumOccurs The {@linkplain #getMinimumOccurs() minimum number of times}
     *                      that values for this parameter group or parameter are required.
     * @param maximumOccurs The {@linkplain #getMaximumOccurs() maximum number of times}
     *                      that values for this parameter group or parameter are allowed to appear.
     *        This constructor uses the {@code maximumOccurs} value only for checking the range validity.
     *        For instances of {@link ParameterValue}, this argument should always be 1.
     */
    protected AbstractParameterDescriptor(final Map<String,?> properties,
                                          final int minimumOccurs,
                                          final int maximumOccurs)
    {
        super(properties);
        this.minimumOccurs = minimumOccurs;
        if (minimumOccurs < 0  || minimumOccurs > maximumOccurs) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
    }

    /**
     * Creates a new descriptor with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param descriptor The descriptor to shallow copy.
     */
    protected AbstractParameterDescriptor(final GeneralParameterDescriptor descriptor) {
        super(descriptor);
        minimumOccurs = descriptor.getMinimumOccurs();
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
     * Creates a new instance of {@linkplain AbstractParameterValue parameter value or group}
     * initialized with the {@linkplain DefaultParameterDescriptor#getDefaultValue default value(s)}.
     * The {@linkplain AbstractParameterValue#getDescriptor() parameter value descriptor} for the
     * created parameter value(s) will be {@code this} object.
     *
     * <p>Implementation example:</p>
     *
     * {@preformat java
     *     return new DefaultParameterValue(this);
     * }
     */
    @Override
    public abstract GeneralParameterValue createValue();

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     * The default value is one. A value of 0 means an optional parameter.
     *
     * @see #getMaximumOccurs()
     */
    @Override
    public int getMinimumOccurs() {
        return minimumOccurs;
    }

    /**
     * The maximum number of times that values for this parameter group or parameter can be included.
     * For a {@linkplain DefaultParameterDescriptor single parameter}, the value is always 1.
     * For a {@linkplain DefaultParameterDescriptorGroup parameter group}, it may vary.
     *
     * @see #getMinimumOccurs()
     */
    @Override
    public abstract int getMaximumOccurs();

    /**
     * Compares the specified object with this parameter for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final AbstractParameterDescriptor that = (AbstractParameterDescriptor) object;
                    return this.minimumOccurs == that.minimumOccurs;
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
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + minimumOccurs;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT) element.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        return null;
    }
}
