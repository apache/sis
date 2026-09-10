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
 * The kind of curve which a particular spline approximates.
 *
 * <p>Note: this code list is given for information only, to convey the original intent, and should
 * be consistent with the other properties of the spline.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.3
 */
@UML(identifier="SplineCurveForm", specification=ISO_19107)
public enum SplineCurveForm {
    /**
     * A connected sequence of line segments, represented by a spline of degree 1.
     */
    POLYLINE_FORM,
    /**
     * An arc of circle, or a complete circle.
     */
    CIRCULAR_ARC,
    /**
     * An arc of ellipse, or a complete ellipse.
     */
    ELLIPTICAL_ARC,
    /**
     * A finite arc of parabola.
     */
    PARABOLIC_ARC,
    /**
     * A finite arc of a single connected branch of hyperbola.
     */
    HYPERBOLIC_ARC
}
