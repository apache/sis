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
package org.apache.sis.internal.metadata;

import org.opengis.util.FactoryException;


/**
 * Interfaces of objects creating an object from a WKT.
 * {@link org.apache.sis.referencing.factory.GeodeticObjectFactory} and
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory}
 * could implement this interface if it was in a public package, but this is not yet
 * the purpose of that interface. But revisit this choice (which would imply moving
 * this interface in a public package) in a future SIS version if experience shows
 * that it would be useful.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public interface WKTParser {
    /**
     * Creates the object from a string.
     *
     * @param  text Coordinate system encoded in Well-Known Text format (version 1 or 2).
     * @return The result of parsing the given text.
     * @throws FactoryException if the object creation failed.
     */
    Object createFromWKT(String text) throws FactoryException;
}
