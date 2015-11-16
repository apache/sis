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
package org.apache.sis.referencing.operation.transform;

import org.opengis.referencing.operation.TransformException;


/**
 * Small but non-constant translations to apply on coordinates for datum shifts or other transformation process.
 * The main purpose of this interface is to encapsulate the data provided by <cite>datum shift grid files</cite>
 * like NTv2, NADCON or RGF93. But this interface could also be used for other kind of transformations,
 * provided that the shifts are small.
 *
 * <p>The translations are often, but not always, applied directly on geographic coordinates (<var>λ</var>,<var>φ</var>).
 * This is the case of datum shift from NAD27 to NAD83 in North America for instance. But other algorithms rather apply
 * the translations in geocentric coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>). This is the case of datum shift
 * from NTF to RGF93 in France. This {@code CoordinateOffsets} interface can describe both cases, but will be used with
 * different {@code MathTransform} implementations.</p>
 *
 * <p>Implementations of this class should be immutable and thread-safe.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public interface CoordinateOffsets {
    /**
     * Computes the translation to apply for the given coordinate.
     * This method usually returns an array of length 2 or 3, but it could be of any length
     * (provided that this length never change). The values in the returned array are often
     * for the same dimensions than <var>x</var> and <var>y</var>, but not necessarily.
     *
     * <div class="section">Use cases</div>
     * <p>In datum shifts with NADCON or NTv2 grids, the (<var>x</var>,<var>y</var>) arguments are longitude (λ)
     * and latitude (φ) in angular <em>degrees</em> and the returned values are (<var>Δλ</var>, <var>Δφ</var>)
     * offsets in angular <em>seconds</em>, converted to degrees for SIS needs.
     * Those offsets will be added or subtracted directly on the given (<var>λ</var>,<var>φ</var>) coordinates.</p>
     *
     * <p>In datum shift to RGF93 in France, the (<var>x</var>,<var>y</var>) arguments are longitude and latitude
     * in angular degrees but the returned values are (<var>ΔX</var>, <var>ΔY</var>, <var>ΔZ</var>) offsets in
     * <em>metres</em>. Those offsets are <strong>not</strong> added to the given (<var>λ</var>,<var>φ</var>)
     * coordinates since there is a Geographic/Geocentric conversion in the middle.</p>
     *
     * @param  x First ordinate (typically longitude, but not necessarily) of the point for which to get the offset.
     * @param  y Second ordinate (typically latitude, but not necessarily) of the point for which to get the offset.
     * @param  offsets A pre-allocated array where to write the offsets, or {@code null}.
     * @return The offset for the given point in the given {@code offsets} array if non-null, or in a new array otherwise.
     * @throws TransformException if an error occurred while computing the offset.
     */
    double[] interpolate(double x, double y, double[] offsets) throws TransformException;
}
