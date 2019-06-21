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

import org.opengis.util.FactoryException;


/**
 * A parser or a factory capable to create an object from a string in the WKT format.
 * The created objects may be {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems},
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform Math Transforms} or geometric
 * objects for instance.
 *
 * <p>Parsing services may be provided by factories which implement this interface:</p>
 * <ul>
 *   <li>{@link org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)}</li>
 *   <li>{@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createFromWKT(String)}</li>
 * </ul>
 *
 * Similar services are also available as convenience static methods:
 * <ul>
 *   <li>{@link org.apache.sis.referencing.CRS#fromWKT(String)}</li>
 *   <li>{@link org.apache.sis.geometry.Envelopes#fromWKT(CharSequence)}</li>
 * </ul>
 *
 * Non-fatal anomalies found in Well Known Texts are reported in a {@linkplain java.util.logging.Logger logger}
 * named {@code "org.apache.sis.io.wkt"}. Warnings may be for unknown or unsupported WKT elements, inconsistent
 * unit definitions (unit symbol, scale factor or EPSG code), unparsable axis abbreviations, <i>etc.</i>
 * However this parser does not verify if the overall parsed object matches the EPSG (or other authority) definition.
 * For such verification, see {@link org.apache.sis.referencing.CRS#fromWKT(String)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
public interface Parser {
    /**
     * Creates the object from a string.
     * Objects returned by this method are typically (but not necessarily)
     * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems} or
     * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform Math Transforms}.
     * If the given text contains non-fatal anomalies, warnings may be reported in a
     * {@linkplain java.util.logging.Logger logger} named {@code "org.apache.sis.io.wkt"}.
     *
     * <div class="note"><b>Tip:</b>
     * for processing warnings in a different way than logging them, one can use
     * {@link WKTFormat#parseObject(String)} followed by a call to {@link WKTFormat#getWarnings()}.</div>
     *
     * @param  text  object encoded in Well-Known Text format (version 1 or 2).
     * @return the result of parsing the given text.
     * @throws FactoryException if the object creation failed.
     */
    Object createFromWKT(String text) throws FactoryException;
}
