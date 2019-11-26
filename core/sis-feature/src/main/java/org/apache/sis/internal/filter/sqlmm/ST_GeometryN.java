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

import org.locationtech.jts.geom.Geometry;
import java.text.ParseException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;

/**
 * SQL/MM, ISO/IEC 13249-3:2011, ST_GeometryN. <br>
 * Return the specified ST_Geometry value in the ST_PrivateGeometries attribute of an ST_GeomCollection
 * value.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class ST_GeometryN extends AbstractAccessorSpatialFunction<Geometry> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4368199731276946357L;

    public static final String NAME = "ST_GeometryN";

    public ST_GeometryN(Expression... parameters) {
        super(parameters);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected int getMinParams() {
        return 2;
    }

    @Override
    protected int getMaxParams() {
        return 2;
    }

    @Override
    protected Class<Geometry> getExpectedClass() {
        return Geometry.class;
    }

    @Override
    public Object execute(Geometry geom, Object... params) throws ParseException {
        final int index = Integer.parseInt(params[1].toString());
        final Geometry sub = geom.getGeometryN(index-1);
        copyCrs(geom, sub);
        return sub;
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        return addTo.addAttribute(Geometry.class)
                .setCRS(expectedCrs(valueType, parameters.get(0)))
                .setName(NAME);
    }

}
