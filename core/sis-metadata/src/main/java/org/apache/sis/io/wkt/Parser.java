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
 * Interfaces of parsers or factories creating a math transform or geodetic object from a WKT.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public interface Parser {
    /**
     * Creates the object from a string.
     * Objects returned by this method are typically (but not necessarily)
     * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems} or
     * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform Math Transforms}.
     *
     * @param  text Object encoded in Well-Known Text format (version 1 or 2).
     * @return The result of parsing the given text.
     * @throws FactoryException if the object creation failed.
     */
    Object createFromWKT(String text) throws FactoryException;
}
