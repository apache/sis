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
package org.apache.sis.io.wkt;


/**
 * Whether to use short or long WKT keywords.
 *
 * <div class="note"><b>Note:</b>
 * ISO 19162 recommends {@linkplain KeywordCase#UPPER_CASE upper case} {@linkplain #SHORT short} keywords,
 * but {@linkplain KeywordCase#CAMEL_CASE camel case} {@linkplain #LONG long} keywords match more closely
 * the programmatic interface names (e.g. {@link org.opengis.referencing.crs.GeodeticCRS}, <i>etc</i>).
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public enum KeywordStyle {
    /**
     * Short keywords.
     *
     * <div class="note"><b>Examples:</b>
     * {@code "GeodCRS"}, {@code "VertCRS"}, {@code "Unit"}.
     * </div>
     */
    SHORT,

    /**
     * Long keywords.
     *
     * <div class="note"><b>Examples:</b>
     * {@code "GeodeticCRS"}, {@code "VerticalCRS"}, {@code "AngleUnit"}.
     * </div>
     */
    LONG,

    /**
     * Keywords style is determined by the WKT {@linkplain Convention convention}.
     *
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>For {@link Convention#WKT2}: {@code "GeodCRS"}, {@code "VertCRS"}, {@code "AngleUnit"}
     *       (keywords matching the ISO 19162 recommendations).</li>
     *   <li>For {@link Convention#WKT2_SIMPLIFIED}: {@code "GeodeticCRS"}, {@code "VerticalCRS"}, {@code "Unit"}
     *       (keywords matching the class or interface names).</li>
     * </ul></div>
     */
    DEFAULT
}
