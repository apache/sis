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
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.collection.Containers;


/**
 * Conversion used as a carrier of parameters (without <abbr>CRS</abbr>s) for defining a final conversion.
 * Defining conversions are used during the construction of {@linkplain DerivedCRS derived <abbr>CRS</abbr>},
 * in which cases the source and target <abbr>CRS</abbr> are provided by the derived <abbr>CRS</abbr> itself.
 * When those <abbr>CRS</abbr>s become available, the {@link #specialize specialize(…)} method can be invoked
 * for {@linkplain DefaultMathTransformFactory#builder creating a math transform from the parameters}
 * and assign the source and target <abbr>CRS</abbr> to the final conversion.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.7
 * @since   1.7
 */
@XmlTransient
public class DefiningConversion extends DefaultConversion {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 901299137419800444L;

    /**
     * Key for a property specifying whether the conversion is fully-defined or defines only the part between
     * normalized <abbr>CRS</abbr>s. The associated value shall be an instance of {@link Boolean}.
     * Possible values are:
     *
     * <ul class="verbose">
     *   <li>{@link Boolean#TRUE} means that the conversion is defined between a pair of <abbr>CRS</abbr>s
     *     normalized in the sense of {@link AxesConvention#NORMALIZED}: the source and target coordinate
     *     systems are right-handed and use predetermined units of measurement such as degrees and metres.
     *     Such {@code DefiningConversion} may need to be completed with change of units and axis order.
     *     These changes can be applied by {@link #specialize DefaultConversion.specialize(…)}.</li>
     *   <li>{@link Boolean#FALSE} means that the conversion is already fully-defined,
     *     including any change of units or axis order that may be required.
     *     No conversion step will be added.</li>
     * </ul>
     *
     * The default value is {@link Boolean#TRUE}.
     *
     * @see #normalized()
     * @see AxesConvention#NORMALIZED
     */
    public static final String NORMALIZED_KEY = "normalized";

    /**
     * Whether this defining conversion provides a normalized transform.
     * If {@code true}, then an adjustment for axis directions and units of measurement will need to be
     * added when the source and target <abbr>CRS</abbr> will become known. If {@code false}, then this
     * defining conversion shall provide the fully-defined transform and no adjustments will be added.
     */
    private final boolean normalized;

    /**
     * Creates a defining conversion from the given transform and/or parameters.
     * This conversion has no source and target <abbr>CRS</abbr> since those elements
     * are usually unknown at <i>defining conversion</i> construction time.
     * The source and target <abbr>CRS</abbr> will become known later,
     * at the {@linkplain DerivedCRS derived <abbr>CRS</abbr>} construction time.
     *
     * <p>The {@code properties} map given in argument follows the same rules as for the
     * {@linkplain DefaultConversion#DefaultConversion(Map, CoordinateReferenceSystem, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, OperationMethod, MathTransform) parent constructor},
     * with the addition of the following properties:</p>
     *
     * <table class="sis">
     *   <caption>Additional properties</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value #CONVERSION_COMPLETION_KEY}</td>
     *     <td>{@link Boolean}</td>
     *     <td>{@link #normalized()}</td>
     *   </tr>
     * </table>
     *
     * <h4>Transform and parameters arguments</h4>
     * At least one of the {@code transform} or {@code parameters} argument must be non-null.
     * If the caller supplies a {@code transform} argument, then by default it shall be a transform expecting
     * {@linkplain AxesConvention#NORMALIZED normalized} input coordinates and producing normalized output coordinates
     * (see {@link AxesConvention} for more information about what Apache <abbr>SIS</abbr> means by "normalized").
     * This default behavior can be disabled by setting the {@value #NORMALIZED_KEY} key to {@code false}.
     *
     * <p>If the caller cannot yet supply a {@code MathTransform}, then it shall supply the parameter values needed
     * for creating that transform, with the possible omission of {@code "semi_major"} and {@code "semi_minor"} values.
     * The semi-major and semi-minor parameter values will be set automatically when the
     * {@link #specialize specialize(…)} method will be invoked.</p>
     *
     * <p>If both the {@code transform} and {@code parameters} arguments are non-null, then the latter should describe
     * the parameters used for creating the transform. Those parameters will be stored for information purpose and can
     * be given back by the {@link #getParameterValues()} method.</p>
     *
     * @param properties  the properties to be given to the identified object.
     * @param method      the operation method.
     * @param transform   transform from positions in the source CRS to positions in the target CRS, or {@code null}.
     * @param parameters  the {@code transform} parameter values, or {@code null}.
     */
    @SuppressWarnings("this-escape")    // False positive.
    public DefiningConversion(final Map<String,?>       properties,
                              final OperationMethod     method,
                              final MathTransform       transform,
                              final ParameterValueGroup parameters)
    {
        super(properties, method);
        this.transform = transform;
        if (transform == null && parameters == null) {
            throw new IllegalArgumentException(Resources.forProperties(properties)
                    .getString(Resources.Keys.UnspecifiedParameterValues));
        }
        normalized = !Boolean.FALSE.equals(Containers.property(properties, NORMALIZED_KEY, Boolean.class));
        setParameterValues(parameters, null);
        checkDimensions(properties);
    }

    /**
     * Returns {@code true} if this conversion is defined between a pair of normalized <abbr>CRS</abbr>s.
     * In such case, the source and target coordinate systems are right-handed and use predetermined units
     * of measurement such as degrees and metres. Such conversion needs to be completed by a call to
     * {@link #specialize specialize(…)}.
     *
     * <p>If this method returns {@code false}, then this {@code DefiningConversion} defines fully the conversion
     * and no conversion step should be added.</p>
     *
     * @return whether this conversion is defined between a pair of normalized <abbr>CRS</abbr>s.
     *
     * @see #NORMALIZED_KEY
     * @see AxesConvention#NORMALIZED
     */
    @Override
    public boolean normalized() {
        return normalized;
    }
}
