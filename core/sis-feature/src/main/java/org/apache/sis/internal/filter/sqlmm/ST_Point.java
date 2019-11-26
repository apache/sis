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

import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.filter.NamedFunction;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * An expression which creates a point geometry from separate coordinates.
 * This expression expects multiple combined arguments:
 * <ul>
 *   <li>X, Y</li>
 *   <li>X, Y, CoordinateReferenceSystem</li>
 * </ul>
 *
 * <p>
 *  Todo : many other arguments are defined is the SQL/MM specification.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class ST_Point extends NamedFunction implements FeatureExpression {

    /**
     * Name of this function as defined by SQL/MM standard.
     */
    static final String NAME = "ST_Point";

    private CoordinateReferenceSystem constantCrs;

    /**
     * Creates a new function with the given parameters. It is caller's responsibility to ensure
     * that the given array is non-null, has been cloned and does not contain null elements.
     *
     * @throws IllegalArgumentException if the number of arguments is less then two.
     */
    ST_Point(final Expression[] parameters) {
        super(parameters);
        if (parameters.length < 2) {
            throw new IllegalArgumentException("ST_Point function expect 2 or more parameters");
        }

        if (this.parameters.size() > 2) {
            Object cdt = this.parameters.get(2).evaluate(null);
            if (cdt instanceof Number) {
                try {
                    constantCrs = CRS.forCode("EPSG:" + ((Number) cdt).intValue());
                } catch (FactoryException ex) {
                    warning(ex);
                }
            } else if (cdt instanceof CoordinateReferenceSystem) {
                constantCrs = (CoordinateReferenceSystem) cdt;
            }
        }
    }

    /**
     * Returns the name of this function, which is {@value #NAME}.
     */
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object evaluate(Object object) {
        Number x = parameters.get(0).evaluate(object, Number.class);
        Number y = parameters.get(1).evaluate(object, Number.class);
        CoordinateReferenceSystem crs = constantCrs;
        if (crs == null && parameters.size() > 2) {
            Object cdt = parameters.get(2).evaluate(object);
            if (cdt instanceof Number) {
                try {
                    crs = CRS.forCode("EPSG:" + ((Number) cdt).intValue());
                } catch (FactoryException ex) {
                    warning(ex);
                }
            } else if (cdt instanceof CoordinateReferenceSystem) {
                crs = (CoordinateReferenceSystem) cdt;
            }
        }

        final Point point = SQLMM.GF.createPoint(new Coordinate(x.doubleValue(), y.doubleValue()));
        point.setUserData(crs);
        return point;
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        final AttributeTypeBuilder<Point> atb = addTo.addAttribute(Point.class)
                .setName(NAME)
                .setMinimumOccurs(1)
                .setMaximumOccurs(1);
        if (constantCrs != null) {
            atb.setCRS(constantCrs);
        }
        return atb;
    }

}
