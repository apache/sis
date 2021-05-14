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
package org.apache.sis.coverage.grid;


/**
 * Specifies rounding behavior during computations of {@link GridExtent} from floating-point values.
 * The rounding mode controls how real numbers are converted into {@code GridExtent}'s
 * {@linkplain GridExtent#getLow(int) low}, {@linkplain GridExtent#getHigh(int) high} and
 * {@linkplain GridExtent#getSize(int) size} integer values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public enum GridRoundingMode {
    /**
     * Converts grid low, high and size to nearest integer values.
     * This mode applies the following steps:
     *
     * <ol>
     *   <li>Floating point values are converted to integers with {@link Math#round(double)}.</li>
     *   <li>On the three integer values ({@linkplain GridExtent#getLow(int) low}, {@linkplain GridExtent#getHigh(int) high}
     *       and {@linkplain GridExtent#getSize(int) size}) obtained after rounding, add or subtract 1 to the value which is
     *       farthest from an integer value in order to keep unchanged the two values that are closer to integers.</li>
     * </ol>
     *
     * <div class="note"><b>Example:</b>
     * the [<var>low</var> … <var>high</var>] range may be slightly larger than desired in some rounding error situations.
     * For example if <var>low</var> before rounding was 1.49999 and <var>high</var> before rounding was 2.50001, then the
     * range after rounding will be [1…3] while the expected size is actually only 2 pixels. This {@code NEAREST} rounding
     * mode detects those rounding issues by comparing the <var>size</var> before and after rounding. In this example, the
     * size is 2.00002 pixels, which is closer to an integer value than the <var>low</var> and <var>high</var> values.
     * Consequently this {@code NEAREST} mode will rather adjust <var>low</var> or <var>high</var> (depending which one is
     * farthest from integer values) in order to keep <var>size</var> at its closest integer value, which is 2.</div>
     */
    NEAREST,

    /**
     * Converts grid low and high to values that fully encloses the envelope.
     * This mode applies the following steps:
     *
     * <ul>
     *   <li>Grid {@linkplain GridExtent#getLow(int) low} are converted to integers with {@link Math#floor(double)}.</li>
     *   <li>Grid {@linkplain GridExtent#getHigh(int) high} are converted to integers with {@link Math#ceil(double)}.</li>
     * </ul>
     *
     * In operations receiving grid coverages as inputs and producing grid coverages as outputs,
     * this rounding mode is convenient for computing the <em>inputs</em> extent because it gives
     * an extent large enough for providing data in the whole region to be computed.
     */
    ENCLOSING,

    /**
     * Converts grid low and high to values that are fully contained in the envelope.
     * This mode applies the following steps:
     *
     * <ul>
     *   <li>Grid {@linkplain GridExtent#getLow(int) low} are converted to integers with {@link Math#ceil(double)}.</li>
     *   <li>Grid {@linkplain GridExtent#getHigh(int) high} are converted to integers with {@link Math#floor(double)}.</li>
     * </ul>
     *
     * In operations receiving grid coverages as inputs and producing grid coverages as outputs,
     * this rounding mode is convenient for computing the <em>outputs</em> extent because it gives
     * an extent small enough for allowing inputs to provide data in the whole region.
     *
     * @since 1.1
     */
    CONTAINED
}
