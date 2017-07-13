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
package org.apache.sis.internal.storage.csv;

import org.apache.sis.internal.converter.SurjectiveConverter;
import org.apache.sis.util.CharSequences;


/**
 * The converter to use for converting a text into a geometry.
 * This converter performs only the first step, the conversion to a {@code double[]} array.
 * The second step (the conversion to a geometry object) is performed after we collected all arrays.
 * The resulting geometry class depends on the library available at runtime.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class GeometryParser extends SurjectiveConverter<String,double[]> {
    /**
     * The unique instance.
     */
    static final GeometryParser INSTANCE = new GeometryParser();

    /**
     * For the singleton instance.
     */
    private GeometryParser() {
    }

    /**
     * Returns the type of elements to convert.
     */
    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    /**
     * Returns the type of converted elements.
     */
    @Override
    public Class<double[]> getTargetClass() {
        return double[].class;
    }

    /**
     * Converts an element from the CSV file to the array type.
     */
    @Override
    public double[] apply(final String text) {
        return CharSequences.parseDoubles(text, Store.ORDINATE_SEPARATOR);
    }
}
