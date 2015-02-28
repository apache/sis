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

import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;


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
     * The earth radius parameter. This parameter is computed automatically from the {@code "semi_major"}
     * and {@code "semi_minor"} parameters. When explicitely set, this parameter value is also assigned
     * to the {@code "semi_major"} and {@code "semi_minor"} axis lengths.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius()
     */
    private final class EarthRadius extends DefaultParameterValue<Double> {
        /**
         * For cross-version compatibility. Actually instances of this class
         * are not expected to be serialized, but we try to be a bit safer here.
         */
        private static final long serialVersionUID = 5848432458976184182L;

        /**
         * Creates a new parameter.
         */
        EarthRadius() {
            super(UniversalParameters.EARTH_RADIUS);
        }

        /**
         * Invoked when a new parameter value is set. This method sets both axis length to the given radius.
         */
        @Override
        protected void setValue(final Object value, final Unit<?> unit) {
            super.setValue(value, unit);   // Perform argument check.
            final double r = (Double) value;
            parameter(Constants.SEMI_MAJOR).setValue(r, unit);
            parameter(Constants.SEMI_MINOR).setValue(r, unit);
        }

        /**
         * Invoked when the parameter value is requested. Unconditionally computes the authalic radius.
         * If an Earth radius has been explicitely specified, the result will be the same unless the user
         * overwrote it with explicit semi-major or semi-minor axis length.
         */
        @Override
        public double doubleValue() {
            final ParameterValue<?> semiMajor = parameter(Constants.SEMI_MAJOR);
            final ParameterValue<?> semiMinor = parameter(Constants.SEMI_MINOR);
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
            return parameter(Constants.SEMI_MAJOR).getUnit();
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
    private final class InverseFlattening extends DefaultParameterValue<Double> {
        /**
         * For cross-version compatibility. Actually instances of this class
         * are not expected to be serialized, but we try to be a bit safer here.
         */
        private static final long serialVersionUID = 4490056024453509851L;

        /**
         * Creates a new parameter.
         */
        InverseFlattening() {
            super(UniversalParameters.INVERSE_FLATTENING);
        }

        /**
         * Invoked when a new parameter value is set.
         * This method computes the semi-minor axis length from the given value.
         */
        @Override
        protected void setValue(final Object value, final Unit<?> unit) {
            super.setValue(value, unit);   // Perform argument check.
            final double ivf = (Double) value;
            if (!Double.isNaN(ivf)) {
                final ParameterValue<?> semiMajor = parameter(Constants.SEMI_MAJOR);
                final ParameterValue<?> semiMinor = parameter(Constants.SEMI_MINOR);
                final Double a = (Double) semiMajor.getValue();
                if (a != null) {
                    semiMinor.setValue(a * (1 - 1/ivf), semiMajor.getUnit());
                }
            }
        }

        /**
         * Invoked when the parameter value is requested.
         * Unconditionally computes the inverse flattening factor from the axis lengths.
         */
        @Override
        public double doubleValue() {
            final ParameterValue<?> semiMajor = parameter(Constants.SEMI_MAJOR);
            final ParameterValue<?> semiMinor = parameter(Constants.SEMI_MINOR);
            final Double a = (Double) semiMajor.getValue();
            if (a != null && semiMinor.getValue() != null) {
                final double b = semiMinor.doubleValue(semiMajor.getUnit());
                return a / (a - b);
            }
            return Double.NaN;
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
     * The standard parallels parameter as an array of {@code double}. This parameter is computed automatically
     * from the {@code "standard_parallel_1"} and {@code "standard_parallel_1"} standard parameters. When this
     * non-standard parameter is explicitely set, the array elements are given to the above-cited standard parameters.
     */
    private final class StandardParallel extends DefaultParameterValue<double[]> {
        /**
         * For cross-version compatibility. Actually instances of this class
         * are not expected to be serialized, but we try to be a bit safer here.
         */
        private static final long serialVersionUID = -1379566730374843040L;

        /**
         * Creates a new parameter.
         */
        StandardParallel() {
            super(UniversalParameters.STANDARD_PARALLEL);
        }

        /**
         * Invoked when a new parameter value is set. This method assign the array elements
         * to {@code "standard_parallel_1"} and {@code "standard_parallel_1"} parameters.
         */
        @Override
        @SuppressWarnings("fallthrough")
        protected void setValue(final Object value, final Unit<?> unit) {
            super.setValue(value, unit);   // Perform argument check.
            double standardParallel1 = Double.NaN;
            double standardParallel2 = Double.NaN;
            if (value != null) {
                final double[] values = (double[]) value;
                switch (values.length) {
                    default: {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.UnexpectedArrayLength_2, 2, values.length));
                    }
                    case 2: standardParallel2 = values[1]; // Fallthrough
                    case 1: standardParallel1 = values[0]; // Fallthrough
                    case 0: break;
                }
            }
            parameter(MapProjectionDescriptor.STANDARD_PARALLEL_1).setValue(standardParallel1, unit);
            parameter(MapProjectionDescriptor.STANDARD_PARALLEL_2).setValue(standardParallel2, unit);
        }

        /**
         * Invoked when the parameter value is requested. Unconditionally computes the array
         * from the {@code "standard_parallel_1"} and {@code "standard_parallel_1"} parameters.
         */
        @Override
        public double[] getValue() {
            final Double standardParallel1 = (Double) parameter(MapProjectionDescriptor.STANDARD_PARALLEL_1).getValue();
            final Double standardParallel2 = (Double) parameter(MapProjectionDescriptor.STANDARD_PARALLEL_2).getValue();
            if (standardParallel2 == null) {
                if (standardParallel1 == null) {
                    return ArraysExt.EMPTY_DOUBLE;
                }
                return new double[] {standardParallel1};
            }
            return new double[] {(standardParallel1 != null) ? standardParallel1 : Double.NaN, standardParallel2};
        }
    }

    /**
     * The {@link EarthRadius} parameter instance, created when first needed.
     * This is an "invisible" parameter, never shown in the {@link #values()} list.
     */
    private transient ParameterValue<Double> earthRadius;

    /**
     * The {@link InverseFlattening} parameter instance, created when first needed.
     * This is an "invisible" parameter, never shown in the {@link #values()} list.
     */
    private transient ParameterValue<Double> inverseFlattening;

    /**
     * The {@link StandardParallel} parameter instance, created when first needed.
     * This is an "invisible" parameter, never shown in the {@link #values()} list.
     */
    private transient ParameterValue<double[]> standardParallel;

    /**
     * Creates a new parameter value group. An instance of {@link MapProjectionDescriptor}
     * is mandatory, because some method in this class will need to cast the descriptor.
     */
    MapProjectionParameters(final MapProjectionDescriptor descriptor) {
        super(descriptor);
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
    public ParameterValue<?> parameter(String name) throws ParameterNotFoundException {
        name = CharSequences.trimWhitespaces(name);
        final int dynamicParameters = ((MapProjectionDescriptor) getDescriptor()).dynamicParameters;
        if ((dynamicParameters & MapProjectionDescriptor.ADD_EARTH_RADIUS) != 0) {
            if (name.equalsIgnoreCase(MapProjectionDescriptor.EARTH_RADIUS)) {
                ParameterValue<?> value = earthRadius;
                if (value == null) {
                    value = earthRadius = new EarthRadius();
                }
                return value;
            }
            if (name.equalsIgnoreCase(MapProjectionDescriptor.INVERSE_FLATTENING)) {
                ParameterValue<?> value = inverseFlattening;
                if (value == null) {
                    value = inverseFlattening = new InverseFlattening();
                }
                return value;
            }
        }
        if ((dynamicParameters & MapProjectionDescriptor.ADD_STANDARD_PARALLEL) != 0) {
            if (name.equalsIgnoreCase(MapProjectionDescriptor.STANDARD_PARALLEL)) {
                ParameterValue<?> value = standardParallel;
                if (value == null) {
                    value = standardParallel = new StandardParallel();
                }
                return value;
            }
        }
        return super.parameter(name);
    }
}
