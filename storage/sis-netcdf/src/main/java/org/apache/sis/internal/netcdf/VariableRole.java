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
package org.apache.sis.internal.netcdf;


/**
 * Specifies whether a variable is used as a coordinate system axis, a coverage or other purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.0
 * @module
 */
public enum VariableRole {
    /**
     * The variable is a coordinate system axis.
     */
    AXIS,

    /**
     * The variable is a continuous grid coverage.
     * Interpolation between cells is allowed.
     */
    COVERAGE,

    /**
     * The variable is a discrete grid coverage, for example data quality masks.
     * Interpolation between cells is not allowed.
     */
    DISCRETE_COVERAGE,

    /**
     * The variable is a property of a feature.
     */
    FEATURE_PROPERTY,

    /**
     * Values of the variable are bounds of values of another variable.
     * For example it may be the bounds of coordinate values specified by an axis.
     */
    BOUNDS,

    /**
     * Unidentified kind of variable.
     */
    OTHER;

    /**
     * Returns {@code true} if the role of the given variable is {@link #COVERAGE} or {@link #DISCRETE_COVERAGE}.
     *
     * @param  candidate  the variable for which to check the role, or {@code null}.
     * @return whether the given variable is non-null and its role is a continuous or discrete coverage.
     */
    public static boolean isCoverage(final Variable candidate) {
        if (candidate != null) {
            final VariableRole role = candidate.getRole();
            return (role == COVERAGE || role == DISCRETE_COVERAGE);
        }
        return false;
    }
}
