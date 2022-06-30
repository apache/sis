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
package org.apache.sis.internal.map.coverage;

import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.projection.Mercator;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * Map projection for which to apply a limit for avoiding rendering problems.
 * The most common case is the Mercator projection, for which we need to put
 * a limit for avoiding to reach the poles.
 *
 * <p>This is a first draft to be expanded progressively.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
final class ProjectionLimits {
    /**
     * List of rules for which we defines limits.
     * This list may be expanded in future versions.
     */
    private static final ProjectionLimits[] RULES = {
        new ProjectionLimits(Mercator.class)
    };

    /**
     * The type of map projection for which this rule applies.
     */
    private final Class<? extends NormalizedProjection> target;

    /**
     * Creates a new rule for map projection limits.
     *
     * @param  target  the type of map projection for which this rule applies.
     */
    private ProjectionLimits(final Class<? extends NormalizedProjection> target) {
        this.target = target;
    }

    /**
     * Returns the map projection limits for rendering a map in the given objective CRS.
     * The default implementation returns the CRS domain of validity, which is okay for
     * the "World Mercator" projection but is often too conservative for other projections.
     * For example in the case of UTM projection, we needs to allow both hemisphere and a larger zone.
     *
     * @param  objectiveCRS  the CRS used for rendering the map.
     * @return limits where to crop the projected image in objective CRS, or {@code null} if none.
     */
    Envelope limits(final CoordinateReferenceSystem objectiveCRS) {
        return CRS.getDomainOfValidity(objectiveCRS);
    }

    /**
     * Returns the map projection limits for rendering a map after the specified "data to objective" transform.
     *
     * @param  changeOfCRS  the operation applied on data before rendering in objective CRS.
     * @return limits where to crop the projected image in objective CRS, or {@code null} if none.
     */
    static Envelope find(final CoordinateOperation changeOfCRS) {
        Envelope limits = null;
        if (changeOfCRS != null) {
            GeneralEnvelope intersection = null;
            for (final MathTransform step : MathTransforms.getSteps(changeOfCRS.getMathTransform())) {
                for (final ProjectionLimits rule : RULES) {
                    if (rule.target.isInstance(step)) {
                        final Envelope e = rule.limits(changeOfCRS.getTargetCRS());
                        if (e != null) {
                            if (limits == null) {
                                limits = e;
                            } else {
                                if (intersection == null) {
                                    limits = intersection = new GeneralEnvelope(limits);
                                }
                                intersection.intersect(e);
                            }
                        }
                    }
                }
            }
        }
        return limits;
    }
}
