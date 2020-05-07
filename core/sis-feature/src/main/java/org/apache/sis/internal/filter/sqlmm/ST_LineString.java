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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.filter.FilterGeometryUtils;
import static org.apache.sis.internal.filter.FilterGeometryUtils.getWKBReader;
import static org.apache.sis.internal.filter.FilterGeometryUtils.getWKTReader;
import org.apache.sis.internal.filter.NamedFunction;
import org.apache.sis.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * An expression which creates a lineString geometry from coordinates.
 * This expression expects multiple combined arguments:
 * <ul>
 *   <li>list of Point</li>
 *   <li>list of Point, CoordinateReferenceSystem</li>
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
final class ST_LineString extends NamedFunction implements FeatureExpression {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3654052127628404584L;

    /**
     * Name of this function as defined by SQL/MM standard.
     */
    static final String NAME = "ST_LineString";

    private CoordinateReferenceSystem constantCrs;

    /**
     * Creates a new function with the given parameters. It is caller's responsibility to ensure
     * that the given array is non-null, has been cloned and does not contain null elements.
     *
     * @throws IllegalArgumentException if the number of arguments is less then one.
     */
    ST_LineString(final Expression... parameters) {
        super(parameters);
        if (parameters.length < 1) {
            throw new IllegalArgumentException("ST_LineString function expect 2 or more parameters");
        }

        if (this.parameters.size() > 1) {
//          constantCrs = toCrs(this.parameters.get(1), null);      // TODO: NullPointerException here.
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
        CoordinateReferenceSystem crs = constantCrs;
        if (crs == null && parameters.size() > 1) {
            crs = toCrs(object, parameters.get(1));
        }

        Object x = parameters.get(0).evaluate(object);
        LineString geometry = null;
        if (x instanceof byte[]) {
            //wkb
            try {
                //try to convert from WKB
                geometry = (LineString) getWKBReader().read((byte[]) x);
            } catch (ParseException | ClassCastException ex) {
                //we have try
                warning(ex);
            }
        } else if (x instanceof String) {
            //wkt
            //todo handle gml
            try {
                //try to convert from WKT
                geometry = (LineString) getWKTReader().read(x.toString());
            } catch (ParseException | ClassCastException ex) {
                //we have try
                warning(ex);
            }
        } else {
            if (x == null) {
              x = Collections.EMPTY_LIST;
            } else if (x instanceof Point) {
                x = Arrays.asList(x);
            } else if (!(x instanceof Iterable)) {
                warning(new Exception("ST_LineString called with an object which is not a iterable"));
                return null;
            }

            Iterator i = ((Iterable) x).iterator();
            final List<Coordinate> coords = new ArrayList<>();
            while (i.hasNext()) {
                Object cdt = i.next();
                if (cdt instanceof Point) {
                    coords.add(((Point) cdt).getCoordinate());
                } else if (cdt instanceof Coordinate) {
                    coords.add(((Coordinate) cdt));
                } else {
                    //what should we do ?
                }
            }

            geometry = FilterGeometryUtils.GF.createLineString(coords.toArray(new Coordinate[coords.size()]));
        }

        if (geometry != null) geometry.setUserData(crs);
        return geometry;
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        final AttributeTypeBuilder<LineString> atb = addTo.addAttribute(LineString.class)
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
