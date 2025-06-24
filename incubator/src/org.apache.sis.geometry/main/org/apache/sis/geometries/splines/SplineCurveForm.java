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
package org.apache.sis.geometries.splines;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="SplineCurveForm", specification=ISO_19107) // section 7.13.3
public enum SplineCurveForm {
    POLYLINE_FORM,
    CIRCULAR_ARC,
    ELLIPTICAL_ARC,
    PARABOLIC_ARC,
    HYPERBOLIC_ARC
}
