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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.logging.Logging;


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
     * The preprocessing and post-processing steps to apply before and after a defining conversion.
     * Some {@code DefiningConversion} implementations require a preprocessing step for expressing
     * input coordinates in a conventional axis order and in predefined units of measurement,
     * and a post-processing step for converting the output to the target <abbr>CRS</abbr>.
     * This enumeration specifies which preprocessing and post-processing, if any, are required.
     */
    public enum SideProcessing {
        /**
         * The defining conversion works on coordinates supplied <i>as-is</i>, with no side-processing.
         * The most typical example is Affine Transform, which works on coordinates in any axis order
         * and units of measurement. If, for example, a unit conversion is desired, that conversion
         * should be bundled in the coefficients of the affine transform.
         */
        NONE,

        /**
         * The defining conversion is defined between a pair of normalized <abbr>CRS</abbr>s.
         * The normalization is described in the {@link AxesConvention#NORMALIZED} documentation:
         * right-handed source and target coordinate systems with predetermined units of measurement.
         * Such defining conversions need to be completed with unit conversions and axis order changes.
         * These preprocessing and post-processing steps can be added by invoking the
         * {@link #specialize DefiningConversion.specialize(…)} method.
         *
         * <p>They are the default side-processing expected by Apache <abbr>SIS</abbr>.
         * For example, map projections are implemented by {@link MathTransform} steps expecting
         * (<var>longitude</var>, <var>latitude</var>) coordinates in degrees and returning
         * (<var>easting</var>, <var>northing</var>) coordinates in metres.
         * If the source <abbr>CRS</abbr> is <abbr>EPSG</abbr>:4326,
         * the change of axis order must be applied before to execute the map projection.</p>
         */
        NORMALIZED
    }

    /**
     * Key for a property specifying the preprocessing and post-processing steps to apply before and after
     * the defining conversion. The associated value shall be an instance of {@link SideProcessing}.
     * Possible values are:
     *
     * <ul class="verbose">
     *   <li>{@link SideProcessing#NORMALIZED} means that the conversion is defined between a pair of <abbr>CRS</abbr>s
     *     normalized in the sense of {@link AxesConvention#NORMALIZED}: the source and target coordinate systems
     *     are right-handed and use predetermined units of measurement such as degrees and metres.
     *     The {@code DefiningConversion} needs to be completed with unit conversions and axis order changes.
     *     These changes can be applied by {@link #specialize DefaultConversion.specialize(…)}.</li>
     *   <li>{@link SideProcessing#NONE} means that no preprocessing or post-processing steps shall be added.</li>
     * </ul>
     *
     * The default value is {@link SideProcessing#NORMALIZED}.
     *
     * @see #getSideProcessing()
     * @see AxesConvention#NORMALIZED
     */
    public static final String SIDE_PROCESSING_KEY = "sideProcessing";

    /**
     * The preprocessing and post-processing steps to apply before and after the defining conversion.
     * If {@code NORMALIZED}, then an adjustment for axis directions and units of measurement will need
     * to be added when the source and target <abbr>CRS</abbr> will become known. If {@code NONE}, this
     * defining conversion shall provide the fully-defined transform and no adjustments will be added.
     */
    private final SideProcessing sideProcessing;

    /**
     * Cached result of the call to {@code specialize(…)}.
     *
     * @see #specialize(CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransformFactory)
     */
    private transient volatile Conversion cached;

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
     *     <td>{@value #SIDE_PROCESSING_KEY}</td>
     *     <td>{@link SideProcessing}</td>
     *     <td>{@link #getSideProcessing}</td>
     *   </tr>
     * </table>
     *
     * <h4>Transform and parameters arguments</h4>
     * At least one of the {@code transform} or {@code parameters} argument must be non-null.
     * If the caller supplies a {@code transform} argument, then by default it shall be a transform expecting
     * {@linkplain AxesConvention#NORMALIZED normalized} input coordinates and producing normalized output coordinates
     * (see {@link AxesConvention} for more information about what Apache <abbr>SIS</abbr> means by "normalized").
     * This default behavior can be disabled by setting the {@value #SIDE_PROCESSING_KEY} key to {@code NONE}.
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
        final var c = Containers.property(properties, SIDE_PROCESSING_KEY, SideProcessing.class);
        sideProcessing = (c != null) ? c : SideProcessing.NORMALIZED;
        setParameterValues(parameters, null);
        checkDimensions(properties);
    }

    /**
     * Returns the preprocessing and post-processing steps to apply before and after the defining conversion.
     * If {@code NORMALIZED}, the source and target coordinate systems are right-handed and use predetermined
     * units of measurement such as degrees and metres. Such conversion needs to be completed by a call to
     * {@link #specialize specialize(…)}.
     *
     * <p>If this method returns {@code NONE}, then this {@code DefiningConversion} defines fully the conversion
     * and no conversion step should be added.</p>
     *
     * @return the preprocessing and post-processing steps to apply before and after the defining conversion.
     *
     * @see #SIDE_PROCESSING_KEY
     * @see AxesConvention#NORMALIZED
     */
    public SideProcessing getSideProcessing() {
        return sideProcessing;
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
     */
    @Override
    final boolean normalized() {
        return getSideProcessing() != SideProcessing.NONE;
    }

    /**
     * Returns a specialization of this conversion with non-null <abbr>CRS</abbr>s.
     * This method should be invoked when more information become available about the conversion to create.
     *
     * @param  sourceCRS  the source <abbr>CRS</abbr>.
     * @param  targetCRS  the target <abbr>CRS</abbr>.
     * @param  factory    the factory to use for creating a transform from the parameters
     *         or for performing axis changes, or {@code null} for the default factory.
     * @return conversion which declares the given <abbr>CRS</abbr>s as the source and target.
     * @throws FactoryException if the creation of a {@link MathTransform} from the {@linkplain #getParameterValues()
     *         parameter values} failed.
     */
    @Override
    public Conversion specialize(final CoordinateReferenceSystem sourceCRS,
                                 final CoordinateReferenceSystem targetCRS,
                                 MathTransformFactory factory) throws FactoryException
    {
        Conversion specialized;
        final boolean cache = (factory == null) || (factory == DefaultMathTransformFactory.provider());
        if (cache) {
            specialized = cached;
            if (specialized != null
                    && specialized.getSourceCRS().equals(sourceCRS)
                    && specialized.getTargetCRS().equals(targetCRS))
            {
                return specialized;
            }
        }
        specialized = super.specialize(sourceCRS, targetCRS, factory);
        if (cache) {
            cached = specialized;
        }
        return specialized;
    }

    /**
     * Returns a conversion which can be compared with the given object.
     * IF {@code other} is not a defining conversion, then this method returns a fully-defined conversion
     * resolved with the same source and target <abbr>CRS</abbr>s as {@code other}.
     * This is necessary for allowing the comparison of {@link #getMathTransform()}.
     *
     * @param  other  the other operation which will be compared with this defining conversion.
     * @return an operation which can be compared with {@code other}.
     */
    @Override
    final CoordinateOperation comparableTo(final CoordinateOperation other) {
        if (getSourceCRS() == null && getTargetCRS() == null) {     // Verified by precaution.
            CoordinateReferenceSystem crs1, crs2;
            if ((crs1 = other.getSourceCRS()) != null && (crs2 = other.getTargetCRS()) != null) try {
                return specialize(crs1, crs2, null);
            } catch (FactoryException e) {
                Logging.ignorableException(LOGGER, DefiningConversion.class, "equals", e);
            }
        }
        return this;
    }
}
