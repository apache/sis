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
package org.apache.sis.geometries.curve;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * The distribution of the knots in the parameter space of a spline.
 *
 * <p>Note: this code list is given for information only and should be consistent with the actual
 * knot sequence of the spline.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.2
 */
@UML(identifier="KnotType", specification=ISO_19107)
public enum KnotType {
    /**
     * Knots are evenly spaced and all of multiplicity 1.
     */
    UNIFORM,
    /**
     * Knots have varying spacings and multiplicities.
     */
    NON_UNIFORM,
    /**
     * Interior knots are evenly spaced with multiplicity 1, while the first and the last one have a
     * multiplicity one more than the degree of the spline.
     */
    QUASI_UNIFORM,
    /**
     * Knots are evenly spaced and interior knots have a multiplicity equal to the degree of the
     * spline, the first and the last one having a multiplicity one more than that degree. Such a
     * spline is a pure Bézier spline between its distinct knots.
     */
    PIECEWISE_BEZIER
}
