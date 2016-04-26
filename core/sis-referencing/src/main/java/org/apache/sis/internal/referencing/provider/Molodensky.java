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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.Collections;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.transform.MolodenskyTransform;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Debug;


/**
 * The provider for "<cite>Molodensky transformation</cite>" (EPSG:9604).
 * This provider constructs transforms between two geographic reference systems without passing though a geocentric one.
 * This class nevertheless extends {@link GeocentricAffineBetweenGeographic} because it is an approximation of
 * {@link GeocentricTranslation3D}.
 *
 * <p>The translation terms (<var>dx</var>, <var>dy</var> and <var>dz</var>) are common to all authorities.
 * But remaining parameters are specified in different ways depending on the authority:</p>
 *
 * <ul>
 *   <li>EPSG defines <cite>"Semi-major axis length difference"</cite>
 *       and <cite>"Flattening difference"</cite> parameters.</li>
 *   <li>OGC rather defines "{@code src_semi_major}", "{@code src_semi_minor}",
 *       "{@code tgt_semi_major}", "{@code tgt_semi_minor}" and "{@code dim}" parameters.</li>
 * </ul>
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class Molodensky extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8126525068450868912L;

    /**
     * The operation parameter descriptor for the <cite>Semi-major axis length difference</cite>
     * optional parameter value. This parameter is defined by the EPSG database and can be used
     * in replacement of {@link #TGT_SEMI_MAJOR}.
     * Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> AXIS_LENGTH_DIFFERENCE;

    /**
     * The operation parameter descriptor for the <cite>Flattening difference</cite> optional
     * parameter value. This parameter is defined by the EPSG database and can be used in
     * replacement of {@link #TGT_SEMI_MINOR}.
     * Valid values range from -1 to +1, {@linkplain Unit#ONE dimensionless}.
     */
    public static final ParameterDescriptor<Double> FLATTENING_DIFFERENCE;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        AXIS_LENGTH_DIFFERENCE = builder.addName("Semi-major axis length difference").create(Double.NaN, SI.METRE);
        FLATTENING_DIFFERENCE  = builder.addName("Flattening difference").createBounded(-1, +1, Double.NaN, Unit.ONE);
        PARAMETERS = builder.setRequired(true)
                .addIdentifier("9604")
                .addName("Molodensky")
                .addName(Citations.OGC, "Molodenski")
                .createGroup(DIMENSION,                         // OGC only
                             SRC_SEMI_MAJOR, SRC_SEMI_MINOR,    // OGC only
                             TGT_SEMI_MAJOR, TGT_SEMI_MINOR,    // OGC only - redundant with differences
                             TX,                                // OGC and EPSG
                             TY,                                // OGC and EPSG
                             TZ,                                // OGC and EPSG
                             AXIS_LENGTH_DIFFERENCE,            // EPSG only
                             FLATTENING_DIFFERENCE);            // EPSG only
    }

    /**
     * Creates a descriptor for the internal parameters of {@link MolodenskyTransform}.
     * This is similar to the standard parameters except that the redundant target axes
     * lengths are omitted.
     *
     * @return Internal parameter descriptor.
     */
    @Debug
    public static ParameterDescriptorGroup internal() {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.SIS, Constants.SIS);
        ParameterDescriptor<Boolean> abridged = builder.addName("abridged").create(Boolean.class, null);
        return builder.addName("Molodensky (radians domain)")
                .createGroup(DIMENSION,
                             SRC_SEMI_MAJOR,
                             SRC_SEMI_MINOR,
                             AXIS_LENGTH_DIFFERENCE,
                             FLATTENING_DIFFERENCE,
                             TX,
                             TY,
                             TZ,
                             abridged);
    }

    /**
     * Constructs a new provider.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public Molodensky() {
        this(3, 3, new Molodensky[4]);
        redimensioned[0] = new Molodensky(2, 2, redimensioned);
        redimensioned[1] = new Molodensky(2, 3, redimensioned);
        redimensioned[2] = new Molodensky(3, 2, redimensioned);
        redimensioned[3] = this;
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param sourceDimensions  number of dimensions in the source CRS of this operation method.
     * @param targetDimensions  number of dimensions in the target CRS of this operation method.
     * @param redimensioned     providers for all combinations between 2D and 3D cases.
     */
    private Molodensky(int sourceDimensions, int targetDimensions, GeodeticOperation[] redimensioned) {
        super(sourceDimensions, targetDimensions, PARAMETERS, redimensioned);
    }

    /**
     * While Molodensky method is an approximation of geocentric translation, this is not exactly that.
     */
    @Override
    int getType() {
        return OTHER;
    }

    /**
     * Creates a Molodensky transform from the specified group of parameter values.
     *
     * @param  factory The factory to use for creating concatenated transforms.
     * @param  values  The group of parameter values.
     * @return The created Molodensky transform.
     * @throws FactoryException if a transform can not be created.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        return createMathTransform(factory, Parameters.castOrWrap(values),
                getSourceDimensions(), getTargetDimensions(), false);
    }

    /**
     * Creates a (potentially abridged) Molodensky transform from the specified group of parameter values.
     * The specified number of dimensions are <em>default</em> values; they may be overridden by user parameters.
     *
     * @param  factory          The factory to use for creating concatenated transforms.
     * @param  values           The group of parameter values specified by the users.
     * @param  sourceDimensions Number of source dimensions (2 or 3) of the operation method.
     * @param  targetDimensions Number of target dimensions (2 or 3) of the operation method.
     * @param  isAbridged       {@code true} for the abridged form.
     * @return The created (abridged) Molodensky transform.
     * @throws FactoryException if a transform can not be created.
     */
    static MathTransform createMathTransform(final MathTransformFactory factory, final Parameters values,
            int sourceDimensions, int targetDimensions, final boolean isAbridged) throws FactoryException
    {
        final Integer dim = values.getValue(DIMENSION);
        if (dim != null) {
            final int n = dim;  // Unboxing.
            if (n != 2 && n != 3) {
                throw new InvalidParameterValueException(Errors.format(
                        Errors.Keys.IllegalArgumentValue_2, "dim", dim), "dim", dim);
            }
            sourceDimensions = targetDimensions = n;
        }
        /*
         * Following method calls implicitly convert parameter values to metres.
         * We do not try to match ellipsoid axis units because:
         *
         *   1) It complicates the code.
         *   2) We have no guarantees that ellipsoid unit match the coordinate system unit.
         *   3) OGC 01-009 explicitly said that angles are in degrees and heights in metres.
         *   4) The above is consistent with what we do for map projections.
         */
        double sa = values.doubleValue(SRC_SEMI_MAJOR);
        double sb = values.doubleValue(SRC_SEMI_MINOR);
        double ta = optional(values,   TGT_SEMI_MAJOR);
        double tb = optional(values,   TGT_SEMI_MINOR);
        double Δa = optional(values, AXIS_LENGTH_DIFFERENCE);
        double Δf = optional(values, FLATTENING_DIFFERENCE);
        if (Double.isNaN(ta)) {
            ta = sa + Δa;
        }
        if (Double.isNaN(tb)) {
            tb = ta*(sb/sa - Δf);
        }
        final Map<String,?> name = Collections.singletonMap(DefaultEllipsoid.NAME_KEY, NilReferencingObject.UNNAMED);
        final Ellipsoid source = new Ellipsoid(name, sa, sb,  Δa,  Δf);
        final Ellipsoid target = new Ellipsoid(name, ta, tb, -Δa, -Δf);
        source.other = target;
        target.other = source;
        return MolodenskyTransform.createGeodeticTransformation(factory,
                source, sourceDimensions >= 3,
                target, targetDimensions >= 3,
                values.doubleValue(TX),
                values.doubleValue(TY),
                values.doubleValue(TZ),
                isAbridged);
    }

    /**
     * Returns the value of the given parameter, or NaN if undefined.
     */
    private static double optional(final Parameters values, final ParameterDescriptor<Double> parameter) {
        try {
            final Double value = values.getValue(parameter);
            if (value != null) {
                return value;
            }
        } catch (ParameterNotFoundException e) {
            // Ignore - this is okay for this method contract.
        } catch (IllegalStateException e) {
            // Ignore - this is okay for this method contract.
        }
        return Double.NaN;
    }

    /**
     * A temporary ellipsoid used only for passing arguments to the {@link MolodenskyTransform} constructor.
     * The intend is to use the Δa and Δf values explicitely specified in the EPSG parameters if available,
     * or to compute them only if no Δa or Δf values where specified.
     */
    @SuppressWarnings("serial")
    private static final class Ellipsoid extends DefaultEllipsoid {
        /** The EPSG parameter values, or NaN if unspecified. */
        private final double Δa, Δf;

        /** The ellipsoid for which Δa and Δf are valid. */
        Ellipsoid other;

        /** Creates a new temporary ellipsoid with explicitely provided Δa and Δf values. */
        Ellipsoid(Map<String,?> name, double a, double b, double Δa, double Δf) {
            super(name, a, b, Formulas.getInverseFlattening(a, b), false, SI.METRE);
            this.Δa = Δa;
            this.Δf = Δf;
        }

        /** Returns Δa as specified in the parameters if possible, or compute it otherwise. */
        @Override public double semiMajorAxisDifference(final org.opengis.referencing.datum.Ellipsoid target) {
            return (target == other && !Double.isNaN(Δa)) ? Δa : super.semiMajorAxisDifference(target);
        }

        /** Returns Δf as specified in the parameters if possible, or compute it otherwise. */
        @Override public double flatteningDifference(final org.opengis.referencing.datum.Ellipsoid target) {
            return (target == other && !Double.isNaN(Δf)) ? Δf : super.flatteningDifference(target);
        }
    }
}
