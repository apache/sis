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
package org.apache.sis.geometries.internal.shared;

import java.util.List;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.cs.Projection;
import org.apache.sis.geometries.curve.ProductCurve;
import org.apache.sis.maths.Array;
import org.apache.sis.measure.Range;


/**
 * A curve composed of other curves which all share the same parameter space, each of them covering
 * a disjoint projection of the coordinate system. It is therefore both a set of curves in the
 * various projections of the coordinate reference system, and a single curve in the complete system.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultProductCurve extends DefaultGeometryCollection<Curve> implements ProductCurve {

    /**
     * Interval of the construction parameter shared by this curve and its projections.
     */
    protected final Range<?> parameterRange;

    /**
     * Projections of the coordinate system, in the same order as the element curves.
     */
    protected final List<Projection> projections;

    /**
     * Creates a product curve from the given element curves.
     *
     * @param  parameterRange  interval of the construction parameter shared by all the elements.
     * @param  projections     projections of the coordinate system matching the element curves.
     *                         The projections are disjoint and together cover the whole system.
     * @param  elements        projections of this curve, one per projection of the coordinate system.
     *                         They all share the knot array and the construction parameters of this curve.
     */
    public DefaultProductCurve(final Range<?> parameterRange, final List<Projection> projections,
            final Curve... elements)
    {
        super(elements);
        this.parameterRange = parameterRange;
        this.projections    = (projections == null) ? List.of() : List.copyOf(projections);
    }

    @Override
    public Range<?> getParameterRange() {
        return parameterRange;
    }

    @Override
    public List<Projection> getProjection() {
        return projections;
    }

    @Override
    public DataPoints getDataPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Array getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
