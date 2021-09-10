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
package org.apache.sis.internal.filter.sqlmm;

import java.nio.ByteBuffer;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;

// Branch-dependent imports
import org.opengis.filter.Expression;


/**
 * Constructor for a geometry which is transformed from a Well-Known Binary (WKB) representation.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <G>  the implementation type of geometry objects.
 *
 * @since 1.1
 * @module
 */
final class ST_FromBinary<R,G> extends GeometryParser<R,G> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8111450596023441499L;

    /**
     * Creates a new function for the given parameters.
     */
    ST_FromBinary(final SQLMM operation, final Expression<? super R, ?>[] parameters, final Geometries<G> library) {
        super(operation, parameters, library);
    }

    /**
     * Creates a new expression of the same type than this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R,Object> recreate(final Expression<? super R, ?>[] effective) {
        return new ST_FromBinary<>(operation, effective, getGeometryLibrary());
    }

    /**
     * Returns the name of the kind of input expected by this expression.
     */
    @Override
    final String inputName() {
        return "wkb";
    }

    /**
     * Parses the given value.
     *
     * @param  value  the WKB value.
     * @return the geometry parsed from the given value.
     * @throws ClassCastException if the given value is not a {@link ByteBuffer} or an array of bytes.
     * @throws Exception if parsing failed for another reason. This is an implementation-specific exception.
     */
    @Override
    protected GeometryWrapper<G> parse(final Object value) throws Exception {
        if (value instanceof ByteBuffer) {
            return library.parseWKB((ByteBuffer) value);
        }
        // ClassCastException is part of method contract.
        return library.parseWKB(ByteBuffer.wrap((byte[]) value));
    }
}
