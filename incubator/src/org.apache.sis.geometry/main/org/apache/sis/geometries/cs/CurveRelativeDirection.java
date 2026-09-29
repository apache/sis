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
package org.apache.sis.geometries.cs;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A reference direction relative to a curve, at a position on that curve.
 *
 * <p>These directions are the vectors of the local frame carried along the curve. They vary from
 * one position to another, so a {@linkplain Bearing bearing} measured from one of them is meaningful
 * only together with the position at which it is taken.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.2.26
 */
@UML(identifier="CurveRelativeDirection", specification=ISO_19107)
public enum CurveRelativeDirection implements ReferenceDirection {
    /**
     * The direction in which the curve is travelled, that is the unit vector collinear with the
     * derivative of the curve with respect to its arc length.
     */
    TANGENT,

    /**
     * Opposite to the {@linkplain #TANGENT tangent}.
     */
    REVERSE_TANGENT,

    /**
     * Perpendicular to the {@linkplain #TANGENT tangent}, in the direction of the curvature vector.
     */
    NORMAL,

    /**
     * Opposite to the {@linkplain #NORMAL normal}.
     */
    REVERSE_NORMAL,

    /**
     * Toward the center of curvature, that is toward the inside of the curve.
     */
    BINORMAL,

    /**
     * Opposite to the {@linkplain #BINORMAL binormal}, that is toward the outside of the curve.
     */
    REVERSE_BINORMAL,

    /**
     * Perpendicular to the {@linkplain #TANGENT tangent}, on its left side.
     */
    LEFT_NORMAL,

    /**
     * Perpendicular to the {@linkplain #TANGENT tangent}, on its right side.
     */
    RIGHT_NORMAL,

    /**
     * Perpendicular to the reference surface, pointing away from it.
     */
    UP_NORMAL,

    /**
     * Opposite to the {@linkplain #UP_NORMAL upward normal}.
     */
    DOWN_NORMAL
}
