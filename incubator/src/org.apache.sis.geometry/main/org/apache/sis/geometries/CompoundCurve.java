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
package org.apache.sis.geometries;

import java.util.ArrayList;
import java.util.List;

/**
 * A curve made of several curves joined end to end, each of which may use a different
 * interpolation. The end point of each component is the start point of the next one, so a compound
 * curve is a single connected curve and not a collection — that is {@link MultiCurve}.
 *
 * @author Johann Sorel (Geomatys
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#compound_curve
 */
public interface CompoundCurve extends Curve {

    public static final String TYPE = "COMPOUNDCURVE";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns the number of curves this compound curve is made of.
     *
     * @return number of components, zero or more.
     */
    int getNumCurves();

    /**
     * Returns the component curve at the given index.
     *
     * @param  n  index of the component to return, from 0 inclusive to {@link #getNumCurves()} exclusive.
     * @return the component curve at the given index.
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    Curve getCurveN(int n);

    /**
     * Returns all the component curves, in order.
     *
     * @return the components, in the order in which they are traversed.
     */
    default List<Curve> getCurves() {
        final int n = getNumCurves();
        final List<Curve> curves = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            curves.add(getCurveN(i));
        }
        return curves;
    }

    /**
     * Returns {@link CurveInterpolation#COMPOSITE_CURVE}: the interpolation of a compound curve is
     * not a single one, it is whatever each component declares.
     */
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.COMPOSITE_CURVE;
    }

    /**
     * Returns the start point of the first component.
     */
    @Override
    default Point getStartPoint() {
        return getCurveN(0).getStartPoint();
    }

    /**
     * Returns the end point of the last component.
     */
    @Override
    default Point getEndPoint() {
        return getCurveN(getNumCurves() - 1).getEndPoint();
    }

    /**
     * Returns the sum of the lengths of the components.
     */
    @Override
    default double getLength() {
        double length = 0;
        for (int i = 0, n = getNumCurves(); i < n; i++) {
            length += getCurveN(i).getLength();
        }
        return length;
    }

    /**
     * Returns whether the last component ends where the first one starts.
     * An empty compound curve is not closed.
     */
    @Override
    default boolean isClosed() {
        final int n = getNumCurves();
        if (n == 0) {
            return false;
        }
        return getStartPoint().getPosition().equals(getEndPoint().getPosition(), 0);
    }
}
