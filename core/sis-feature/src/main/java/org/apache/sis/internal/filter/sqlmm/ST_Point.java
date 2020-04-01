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

import java.math.BigDecimal;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.filter.FilterGeometryUtils;
import org.apache.sis.internal.filter.NamedFunction;
import org.apache.sis.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
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
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6280773709322350835L;

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
    ST_Point(final Expression... parameters) {
        super(parameters);
        if (parameters.length < 1 || parameters.length > 4) {
            throw new IllegalArgumentException("ST_Point function expect 1 to 4 parameters");
        }

        int nbarg = this.parameters.size();
        switch (nbarg) {
            case 2:
                constantCrs = toCrs(null, this.parameters.get(1));
                break;
            case 3:
                constantCrs = toCrs(null, this.parameters.get(2));
                break;
            case 4:
                constantCrs = toCrs(null, this.parameters.get(3));
                break;
            default:
                break;
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
    public Object evaluate(Object candidate) {

        CoordinateReferenceSystem crs = constantCrs;
        final int nbarg = parameters.size();
        Geometry geom;
        if (nbarg == 1) {
            //WKB or WKT
            geom = FilterGeometryUtils.toGeometry(candidate, parameters.get(0));

        } else if (nbarg == 2) {
            final Object arg0 = parameters.get(0).evaluate(candidate);
            final Object arg1 = parameters.get(1).evaluate(candidate);
            if (arg0 instanceof Number) {
                // X,Y
                final Number obj1 = (Number) arg0;
                final Number obj2 = (Number) arg1;
                geom = FilterGeometryUtils.GF.createPoint(new Coordinate(obj1.doubleValue(), obj2.doubleValue()));
            } else {
                //WKT/WKB + srid
                geom = FilterGeometryUtils.toGeometry(candidate, parameters.get(0));
                if (crs == null) {
                    crs = toCrs(candidate, parameters.get(1));
                }
            }
        } else if (nbarg == 3) {
            final Number obj1 = parameters.get(0).evaluate(candidate, Number.class);
            final Number obj2 = parameters.get(1).evaluate(candidate, Number.class);
            final Object obj3 = parameters.get(2).evaluate(candidate);
            geom = FilterGeometryUtils.GF.createPoint(new Coordinate(obj1.doubleValue(), obj2.doubleValue()));
            if (obj3 instanceof Float || obj3 instanceof Double || obj3 instanceof BigDecimal) {
                // Z
                geom.getCoordinate().z = ((Number) obj3).doubleValue();
            } else {
                // srid
                if (crs == null) {
                    crs = toCrs(candidate, parameters.get(2));
                }
            }
        } else if (nbarg == 4) {
            final Number obj1 = parameters.get(0).evaluate(candidate, Number.class);
            final Number obj2 = parameters.get(1).evaluate(candidate, Number.class);
            final Number obj3 = parameters.get(2).evaluate(candidate, Number.class);
            geom = FilterGeometryUtils.GF.createPoint(new Coordinate(obj1.doubleValue(), obj2.doubleValue(), obj3.doubleValue()));
            if (crs == null) {
                crs = toCrs(candidate, parameters.get(3));
            }
        } else {
            //should not happen,constructor prevents it
            geom = null;
        }

        if (geom != null) geom.setUserData(crs);
        return geom;
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

    private CoordinateReferenceSystem toCrs(Object candidate, Expression exp) {
        Object cdt = exp.evaluate(candidate);
        if (cdt instanceof Number) {
            try {
                cdt = CRS.forCode("EPSG:" + ((Number) cdt).intValue());
            } catch (FactoryException ex) {
                warning(ex);
            }
        } else if (cdt instanceof String) {
            try {
                cdt = CRS.forCode((String) cdt);
            } catch (FactoryException ex) {
                warning(ex);
            }
        }
        if (cdt instanceof CoordinateReferenceSystem) {
            return (CoordinateReferenceSystem) cdt;
        }
        return null;
    }

}
