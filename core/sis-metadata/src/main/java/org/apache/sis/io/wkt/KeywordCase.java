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
 * Whether WKT keywords shall be written with upper case or CamelCase styles.
 * The most common usage for WKT keywords is upper case.
 * However with version 2 of Well Known Text, CamelCase keywords may be easier to read
 * because WKT 2 has more keywords made by combination of words. Examples:
 *
 * <table class="sis">
 *   <tr><th>Upper case</th>                 <th>Camel case</th></tr>
 *   <tr><td>{@code TIMEEXTENT}</td>         <td>{@code TimeExtent}</td></td>
 *   <tr><td>{@code ANGLEUNIT}</td>          <td>{@code AngleUnit}</td></td>
 *   <tr><td>{@code BASEGEODCRS}</td>        <td>{@code BaseGeodCRS}</td></td>
 *   <tr><td>{@code DERIVINGCONVERSION}</td> <td>{@code DerivingConversion}</td></td>
 *   <tr><td>{@code ENGINEERINGDATUM}</td>   <td>{@code EngineeringDatum}</td></td>
 * </table>
 *
 * <div class="note"><b>Note:</b>
 * Well-Known Text keywords are case insensitive at parsing time. {@code KEYWORD} is equivalent to
 * {@code keyword} is equivalent to {@code KeyWord} and to {@code kEYwORd}.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public enum KeywordCase {
    /**
     * Keywords case is determined by the WKT {@linkplain Convention convention}.
     * The current mapping is:
     *
     * <ul>
     *   <li>Well Known Text version 2 uses camel case.</li>
     *   <li>Well Known Text version 1 uses upper case.</li>
     * </ul>
     */
    DEFAULT,

    /**
     * WKT formatting uses CamelCase keywords. This is more useful in WKT 2 strings, which
     * use longer keywords than WKT 1 did. Examples: {@code "TimeExtent"}, {@code "AngleUnit"}.
     */
    CAMEL_CASE,

    /**
     * WKT formatting uses upper case keywords.
     * This is the most usual case in WKT 1 strings.
     */
    UPPER_CASE
}
