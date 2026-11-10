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
package org.apache.sis.style.xml;

import org.apache.sis.gml.GMLVersion;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.se1.function.Recode;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Writer of OGC Filter Encoding 1.0 filters and expressions, with geometries encoded in GML 2.
 * In this version, the functions defined by Symbology Encoding have no specific encoding.
 * They are written as {@code <ogc:Function>} elements.
 * This writer is used by {@link SEWriter100}.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class FilterWriter100 extends FilterWriter {
    /**
     * Creates a new writer sharing the cursor and the state of the given writer.
     *
     * @param  parent  the symbology writer which contains this filter writer.
     */
    FilterWriter100(final AbstractStaxWriter parent) {
        super(parent);
    }

    /**
     * Returns the version written by this writer, which is FE 1.0.0 as used by SLD 1.0.0.
     */
    @Override
    StyleVersion getVersion() {
        return StyleVersion.V1_0_0;
    }

    /**
     * Returns GML 2, which is the GML version used by Filter Encoding 1.0.
     */
    @Override
    GMLVersion gmlVersion() {
        return GMLVersion.V2;
    }

    /**
     * Does not write the functions defined by Symbology Encoding with a specific encoding,
     * since that encoding does not exist in version 1.0. Instead, the functions will be written
     * as {@code <ogc:Function>} elements. Fallback values are lost.
     */
    @Override
    <R> boolean writeSymbologyFunction(final Expression<R,?> expression) {
        final boolean hasFallback;
        if (expression instanceof Categorize<R,?> f) {
            hasFallback = f.getFallbackValue().isPresent();
        } else if (expression instanceof Interpolate<?> f) {
            hasFallback = f.getFallbackValue().isPresent();
        } else if (expression instanceof Recode<?> f) {
            hasFallback = f.getFallbackValue().isPresent();
        } else {
            hasFallback = false;
        }
        if (hasFallback) {
            warning("Fallback values of " + functionName(expression) + " functions are not supported in version 1.0, ignored.");
        }
        return false;
    }
}
