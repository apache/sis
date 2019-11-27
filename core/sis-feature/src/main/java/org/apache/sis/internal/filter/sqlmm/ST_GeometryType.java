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
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;

/**
 * SQL/MM, ISO/IEC 13249-3:2011, ST_GeometryType. <br>
 * Returns the geometry type name.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class ST_GeometryType extends AbstractAccessorSpatialFunction<Geometry> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3950622078110541672L;

    public static final String NAME = "ST_GeometryType";

    private static final Map<Class,String> TYPES = new LinkedHashMap<Class, String>();
    static {
        TYPES.put(Point.class,              "ST_Point");
        TYPES.put(LineString.class,         "ST_LineString");
        TYPES.put(Polygon.class,            "ST_Polygon");
        TYPES.put(MultiPoint.class,         "ST_MultiPoint");
        TYPES.put(MultiLineString.class,    "ST_MultiLineString");
        TYPES.put(MultiPolygon.class,       "ST_MultiPolygon");
        TYPES.put(GeometryCollection.class, "ST_GeomCollection");
        TYPES.put(Geometry.class,           "ST_Geometry");
    }

    public ST_GeometryType(Expression... parameters) {
        super(parameters);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Class<Geometry> getExpectedClass() {
        return Geometry.class;
    }

    @Override
    public Object execute(Geometry geom, Object... params) throws ParseException {
        for (Entry<Class,String> entry : TYPES.entrySet()) {
            if (entry.getKey().isInstance(geom)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        return addTo.addAttribute(String.class).setName(NAME);
    }

}
