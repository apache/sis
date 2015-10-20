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

import java.util.Collections;
import java.util.Map;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArraysExt;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.apache.sis.metadata.iso.citation.Citations.NETCDF;

// Branch-specific imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Map projection parameters, with special processing for alternative ways to express the ellipsoid axis length
 * and the standard parallels. See {@link MapProjectionDescriptor} for more information about those non-standard
 * parameters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class MapProjectionParameters extends DefaultParameterValueGroup {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6801091012335717139L;

    /**
     * The {@link EarthRadius} parameter instance, created when first needed.
     * This is an "invisible" parameter, never shown in the {@link #values()} list.
     */
    private transient ParameterValue<Double> earthRadius;

    /**
     * The {@link InverseFlattening} parameter instance, created when first needed.
     * This is an "invisible" parameter, never shown in the {@link #values()} list.
     */
    private transient InverseFlattening inverseFlattening;

    /**
     * The {@link StandardParallel} parameter instance, created when first needed.
     * This is an "invisible" parameter, never shown in the {@link #values()} list.
     */
    private transient ParameterValue<double[]> standardParallel;

    /**
     * The {@link IsIvfDefinitive} parameter instance, created when first needed.
     * This is an "invisible" parameter, never shown in the {@link #values()} list.
     */
    private transient ParameterValue<Boolean> isIvfDefinitive;

    /**
     * Creates a new parameter value group. An instance of {@link MapProjectionDescriptor}
     * is mandatory, because some method in this class will need to cast the descriptor.
     */
    MapProjectionParameters(final MapProjectionDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * Returns {@code true} since the {@link #parameterIfExist(String)} method below is compatible with
     * {@link #parameter(String)}. Note that we would need to revisit this condition if this class was
     * no longer final.
     */
    @Override
    boolean isKnownImplementation() {
        return true;
    }

    /**
     * Returns the value in this group for the specified name. If the given name is one of the
     * "invisible" parameters, returns a dynamic parameter view without adding it to the list
     * of real parameter values.
     *
     * @param  name The case insensitive name of the parameter to search for.
     * @return The parameter value for the given name.
     * @throws ParameterNotFoundException if there is no parameter value for the given name.
     */
    @Override
    ParameterValue<?> parameterIfExist(final String name) throws ParameterNotFoundException {
        if (MapProjectionDescriptor.isHeuristicMatchForName(name, Constants.EARTH_RADIUS)) {
            if (earthRadius == null) {
                earthRadius = new EarthRadius(parameter(Constants.SEMI_MAJOR),
                                              parameter(Constants.SEMI_MINOR));
            }
            return earthRadius;
        }
        if (MapProjectionDescriptor.isHeuristicMatchForName(name, Constants.INVERSE_FLATTENING)) {
            return getInverseFlattening();
        }
        if (MapProjectionDescriptor.isHeuristicMatchForName(name, Constants.IS_IVF_DEFINITIVE)) {
            if (isIvfDefinitive == null) {
                isIvfDefinitive = new IsIvfDefinitive(getInverseFlattening());
            }
            return isIvfDefinitive;
        }
        if (((MapProjectionDescriptor) getDescriptor()).hasStandardParallels) {
            if (MapProjectionDescriptor.isHeuristicMatchForName(name, Constants.STANDARD_PARALLEL)) {
                if (standardParallel == null) {
                    standardParallel = new StandardParallel(parameter(Constants.STANDARD_PARALLEL_1),
                                                            parameter(Constants.STANDARD_PARALLEL_2));
                }
                return standardParallel;
            }
        }
        return super.parameterIfExist(name);
    }

    /**
     * Returns the {@link InverseFlattening} instance, creating it when first needed.
     * This parameter is used also by {@link IsIvfDefinitive}.
     */
    private InverseFlattening getInverseFlattening() {
        if (inverseFlattening == null) {
            inverseFlattening = new InverseFlattening(parameter(Constants.SEMI_MAJOR),
                                                      parameter(Constants.SEMI_MINOR));
        }
        return inverseFlattening;
    }




    /**
     * The earth radius parameter. This parameter is computed automatically from the {@code "semi_major"}
     * and {@code "semi_minor"} parameters. When explicitely set, this parameter value is also assigned
     * to the {@code "semi_major"} and {@code "semi_minor"} axis lengths.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius()
     */
    static final class EarthRadius extends DefaultParameterValue<Double> {
        /**
         * For cross-version compatibility. Actually instances of this class
         * are not expected to be serialized, but we try to be a bit safer here.
         */
        private static final long serialVersionUID = 5848432458976184182L;

        /**
         * All names known to Apache SIS for the Earth radius parameter.
         * This is used in some NetCDF files instead of {@code SEMI_MAJOR} and {@code SEMI_MINOR}.
         * This is not a standard parameter.
         */
        static final ParameterDescriptor<Double> DESCRIPTOR = new DefaultParameterDescriptor<Double>(
                InverseFlattening.toMap(Constants.EARTH_RADIUS), 0, 1, Double.class,
                MeasurementRange.createGreaterThan(0.0, SI.METRE), null, null);

        /**
         * The parameters for the semi-major and semi-minor axis length.
         */
        private final ParameterValue<?> semiMajor, semiMinor;

        /**
         * Creates a new parameter.
         */
        EarthRadius(final ParameterValue<?> semiMajor, final ParameterValue<?> semiMinor) {
            super(DESCRIPTOR);
            this.semiMajor = semiMajor;
            this.semiMinor = semiMinor;
        }

        /**
         * Invoked when a new parameter value is set. This method sets both axis length to the given radius.
         */
        @Override
        protected void setValue(final Object value, final Unit<?> unit) {
            super.setValue(value, unit);        // Perform argument check.
            final double r = (Double) value;    // At this point, can not be anything else than Double.
            semiMajor.setValue(r, unit);
            semiMinor.setValue(r, unit);
        }

        /**
         * Invoked when the parameter value is requested. Unconditionally computes the authalic radius.
         * If an Earth radius has been explicitely specified, the result will be the same unless the user
         * overwrote it with explicit semi-major or semi-minor axis length.
         */
        @Override
        public double doubleValue() {
            double r = semiMajor.doubleValue();
            if (semiMinor.getValue() != null) {
                // Compute in unit of the semi-major axis.
                r = Formulas.getAuthalicRadius(r, semiMinor.doubleValue(semiMajor.getUnit()));
            }
            return r;
        }

        /**
         * Unconditionally returns the unit of the semi-major axis, which is the unit
         * in which {@link #doubleValue()} performs its computation.
         */
        @Override
        public Unit<?> getUnit() {
            return semiMajor.getUnit();
        }

        /**
         * Getters other than the above {@code doubleValue()} delegate to this method.
         */
        @Override
        public Double getValue() {
            return doubleValue();
        }
    }




    /**
     * The inverse flattening parameter. This parameter is computed automatically from the {@code "semi_major"}
     * and {@code "semi_minor"} parameters. When explicitly set, this parameter value is used for computing the
     * semi-minor axis length.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#getInverseFlattening()
     */
    static final class InverseFlattening extends DefaultParameterValue<Double> {
        /**
         * For cross-version compatibility. Actually instances of this class
         * are not expected to be serialized, but we try to be a bit safer here.
         */
        private static final long serialVersionUID = 4490056024453509851L;

        /**
         * All names known to Apache SIS for the inverse flattening parameter.
         * This is used in some NetCDF files instead of {@code SEMI_MINOR}.
         * This is not a standard parameter.
         */
        static final ParameterDescriptor<Double> DESCRIPTOR = new DefaultParameterDescriptor<Double>(
                toMap(Constants.INVERSE_FLATTENING), 0, 1, Double.class,
                MeasurementRange.createGreaterThan(0.0, Unit.ONE), null, null);

        /**
         * Helper method for {@link #DESCRIPTOR} constructions.
         */
        static Map<String,?> toMap(final String name) {
            return Collections.singletonMap(NAME_KEY, new NamedIdentifier(NETCDF, name));
        }

        /**
         * The parameters for the semi-major and semi-minor axis length.
         */
        private final ParameterValue<?> semiMajor, semiMinor;

        /**
         * The declared inverse flattening values, together with a snapshot of axis lengths
         * at the time the inverse flattening has been set.
         */
        private double inverseFlattening, a, b;

        /**
         * Creates a new parameter.
         */
        InverseFlattening(final ParameterValue<?> semiMajor, final ParameterValue<?> semiMinor) {
            super(DESCRIPTOR);
            this.semiMajor = semiMajor;
            this.semiMinor = semiMinor;
            invalidate();
        }

        /**
         * Declares that the inverse flattening factor is not definitive.
         * We use the fact that the {@code ==} operator gives {@code false} if a value is NaN.
         */
        void invalidate() {
            a = b = Double.NaN;
        }

        /**
         * Returns {@code true} if the inverse flattening factor has been explicitely specified
         * and seems to still valid.
         */
        boolean isIvfDefinitive() {
            if (inverseFlattening > 0) {
                final Number ca = (Number) semiMajor.getValue();
                if (ca != null && ca.doubleValue() == a) {
                    final Number cb = (Number) semiMinor.getValue();
                    if (cb != null && cb.doubleValue() == b) {
                        return Objects.equals(semiMajor.getUnit(), semiMinor.getUnit());
                    }
                }
            }
            return false;
        }

        /**
         * Invoked when a new parameter value is set.
         * This method computes the semi-minor axis length from the given value.
         */
        @Override
        protected void setValue(final Object value, final Unit<?> unit) {
            super.setValue(value, unit);        // Perform argument check.
            final double ivf = (Double) value;  // At this point, can not be anything else than Double.
            final Number ca = (Number) semiMajor.getValue();
            if (ca != null) {
                a = ca.doubleValue();
                b = Formulas.getSemiMinor(a, ivf);
                semiMinor.setValue(b, semiMajor.getUnit());
            } else {
                invalidate();
            }
            inverseFlattening = ivf;
        }

        /**
         * Invoked when the parameter value is requested. Computes the inverse flattening factor
         * from the axis lengths if the currently stored value does not seem to be valid anymore.
         */
        @Override
        public double doubleValue() {
            final double ca = semiMajor.doubleValue();
            final double cb = semiMinor.doubleValue(semiMajor.getUnit());
            if (ca == a && cb == b && inverseFlattening > 0) {
                return inverseFlattening;
            }
            return Formulas.getInverseFlattening(ca, cb);
        }

        /**
         * Getters other than the above {@code doubleValue()} delegate to this method.
         */
        @Override
        public Double getValue() {
            return doubleValue();
        }
    }




    /**
     * Whether the inverse flattening parameter is definitive.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#isIvfDefinitive()
     */
    static final class IsIvfDefinitive extends DefaultParameterValue<Boolean> {
        /**
         * For cross-version compatibility. Actually instances of this class
         * are not expected to be serialized, but we try to be a bit safer here.
         */
        private static final long serialVersionUID = 5988883252321358629L;

        /**
         * All names known to Apache SIS for the "is IVF definitive" parameter.
         * This is not a standard parameter.
         */
        static final ParameterDescriptor<Boolean> DESCRIPTOR = new DefaultParameterDescriptor<Boolean>(
                InverseFlattening.toMap(Constants.IS_IVF_DEFINITIVE), 0, 1, Boolean.class, null, null, Boolean.FALSE);

        /**
         * The parameters for the inverse flattening factor.
         */
        private final InverseFlattening inverseFlattening;

        /**
         * Creates a new parameter.
         */
        IsIvfDefinitive(final InverseFlattening inverseFlattening) {
            super(DESCRIPTOR);
            this.inverseFlattening = inverseFlattening;
        }

        /**
         * Invoked when a new parameter value is set.
         */
        @Override
        protected void setValue(final Object value, final Unit<?> unit) {
            super.setValue(value, unit);   // Perform argument check.
            if (!(Boolean) value) {
                inverseFlattening.invalidate();
            }
        }

        /**
         * Invoked when the parameter value is requested.
         */
        @Override
        public boolean booleanValue() {
            return inverseFlattening.isIvfDefinitive();
        }

        /**
         * Getters other than the above {@code booleanValue()} delegate to this method.
         */
        @Override
        public Boolean getValue() {
            return booleanValue();
        }
    }




    /**
     * The standard parallels parameter as an array of {@code double}. This parameter is computed automatically
     * from the {@code "standard_parallel_1"} and {@code "standard_parallel_1"} standard parameters. When this
     * non-standard parameter is explicitely set, the array elements are given to the above-cited standard parameters.
     */
    static final class StandardParallel extends DefaultParameterValue<double[]> {
        /**
         * For cross-version compatibility. Actually instances of this class
         * are not expected to be serialized, but we try to be a bit safer here.
         */
        private static final long serialVersionUID = -1379566730374843040L;

        /**
         * All names known to Apache SIS for the standard parallels parameter, as an array of 1 or 2 elements.
         * This is used in some NetCDF files instead of {@link #STANDARD_PARALLEL_1} and
         * {@link #STANDARD_PARALLEL_2}. This is not a standard parameter.
         */
        static final ParameterDescriptor<double[]> DESCRIPTOR = new DefaultParameterDescriptor<double[]>(
                InverseFlattening.toMap(Constants.STANDARD_PARALLEL),
                0, 1, double[].class, null, null, null);

        /**
         * The parameters for the standard parallels.
         */
        private final ParameterValue<?> standardParallel1, standardParallel2;

        /**
         * Creates a new parameter.
         */
        StandardParallel(final ParameterValue<?> standardParallel1, final ParameterValue<?> standardParallel2) {
            super(DESCRIPTOR);
            this.standardParallel1 = standardParallel1;
            this.standardParallel2 = standardParallel2;
        }

        /**
         * Invoked when a new parameter value is set. This method assign the array elements
         * to {@code "standard_parallel_1"} and {@code "standard_parallel_1"} parameters.
         */
        @Override
        @SuppressWarnings("fallthrough")
        protected void setValue(final Object value, final Unit<?> unit) {
            super.setValue(value, unit);   // Perform argument check.
            double p1 = Double.NaN;
            double p2 = Double.NaN;
            if (value != null) {
                final double[] values = (double[]) value;
                switch (values.length) {
                    default: {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.UnexpectedArrayLength_2, 2, values.length));
                    }
                    case 2: p2 = values[1]; // Fallthrough
                    case 1: p1 = values[0]; // Fallthrough
                    case 0: break;
                }
            }
            standardParallel1.setValue(p1, unit);
            standardParallel2.setValue(p2, unit);
        }

        /**
         * Invoked when the parameter value is requested. Unconditionally computes the array
         * from the {@code "standard_parallel_1"} and {@code "standard_parallel_1"} parameters.
         */
        @Override
        public double[] getValue() {
            final Number p1 = (Number) standardParallel1.getValue();
            final Number p2 = (Number) standardParallel2.getValue();
            if (p2 == null) {
                if (p1 == null) {
                    return ArraysExt.EMPTY_DOUBLE;
                }
                return new double[] {p1.doubleValue()};
            }
            return new double[] {(p1 != null) ? p1.doubleValue() : Double.NaN, p2.doubleValue()};
        }
    }
}
