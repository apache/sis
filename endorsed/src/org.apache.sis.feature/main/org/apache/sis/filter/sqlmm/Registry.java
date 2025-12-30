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
package org.apache.sis.filter.sqlmm;

import java.util.Set;
import org.apache.sis.util.internal.shared.ViewAsSet;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.filter.FunctionRegister;

// Specific to the main branch:
import org.apache.sis.filter.Expression;


/**
 * A register of functions defined by the SQL/MM standard.
 * This standard is defined by <a href="https://www.iso.org/standard/60343.html">ISO/IEC 13249-3:2016
 * Information technology — Database languages — SQL multimedia and application packages — Part 3: Spatial</a>.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class Registry implements FunctionRegister {
    /**
     * The geometry library used by this registry.
     */
    private final Geometries<?> library;

    /**
     * Creates the default register.
     *
     * @param  library  the geometry library to use.
     */
    public Registry(final Geometries<?> library) {
        this.library = library;
    }

    /**
     * Returns the name of the standard or authority defining the functions.
     */
    @Override
    public String getAuthority() {
        return "SQL/MM";
    }

    /**
     * Returns the names of all functions known to this register.
     */
    @Override
    public Set<String> getNames() {
        return ViewAsSet.namesOf(SQLMM.class);
    }

    /**
     * Create a new function of the given name with given parameters.
     * It is caller's responsibility to ensure that the given array is non-null,
     * has been cloned and does not contain null elements.
     * This method verifies only the number of parameters.
     *
     * @param  <R>         the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
     * @param  name        case-sensitive name of the function to create as an expression.
     * @param  parameters  function parameters.
     * @return function for the given name and parameters.
     * @throws IllegalArgumentException if function name is unknown or some parameters are illegal.
     */
    @Override
    public <R> Expression<R,?> create(final String name, Expression<R,?>[] parameters) {
        final SQLMM operation = SQLMM.valueOf(name);
        switch (operation) {
            case ST_PointFromWKB:       // Fallthrough
            case ST_LineFromWKB:        // Fallthrough
            case ST_PolyFromWKB:        // Fallthrough
            case ST_BdPolyFromWKB :     // Fallthrough
            case ST_GeomCollFromWKB:    // Fallthrough
            case ST_MPointFromWKB:      // Fallthrough
            case ST_MLineFromWKB:       // Fallthrough
            case ST_MPolyFromWKB:       // Fallthrough
            case ST_BdMPolyFromWKB:     // Fallthrough
            case ST_GeomFromWKB:        return new ST_FromBinary<>(operation, parameters, library);
            case ST_PointFromText:      // Fallthrough
            case ST_LineFromText:       // Fallthrough
            case ST_PolyFromText:       // Fallthrough
            case ST_BdPolyFromText:     // Fallthrough
            case ST_GeomCollFromText:   // Fallthrough
            case ST_MPointFromText:     // Fallthrough
            case ST_MLineFromText:      // Fallthrough
            case ST_MPolyFromText:      // Fallthrough
            case ST_BdMPolyFromText:    // Fallthrough
            case ST_GeomFromText:       return new ST_FromText<>(operation, parameters, library);
            case ST_Polygon:            // Fallthrough
            case ST_LineString:         // Fallthrough
            case ST_MultiPoint:         // Fallthrough
            case ST_MultiLineString:    // Fallthrough
            case ST_MultiPolygon:       // Fallthrough
            case ST_GeomCollection:     return new GeometryConstructor<>(operation, parameters, library);
            case ST_Point:              return new ST_Point<>(parameters, library);
            case ST_Transform:          return new ST_Transform<>(parameters, library);
            default: {
                switch (operation.geometryCount()) {
                    case 1: {
                        if (operation.maxParamCount == 1) {
                            return new OneGeometry<>(operation, parameters, library);
                        } else {
                            return new OneGeometry.WithArgument<>(operation, parameters, library);
                        }
                    }
                    case 2: {
                        if (operation.maxParamCount == 2) {
                            return new TwoGeometries<>(operation, parameters, library);
                        } else {
                            return new TwoGeometries.WithArgument<>(operation, parameters, library);
                        }
                    }
                    default: {
                        throw new AssertionError(operation);
                    }
                }
            }
        }
    }
}
