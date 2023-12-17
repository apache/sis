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
package org.apache.sis.storage.shapefile.shp;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


/**
 * Provides a ShapefileType Enumeration.
 *
 * The names diverge from the specification on names.
 * The specification says a PointZ has X/Y dimensions and 1 measure so we renamed it to Point_M for the sake of coherency.
 * The same applies to PointM renamed Point_ZM since it has X/Y/Z dimensions and 1 measure.
 *
 * @author Travis L. Pinney
 * @author Johann Sorel (Geomatys)
 *
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile Specification</a>
 */
public enum ShapeType {

    /**
     * Null geometry type
     */
    NULL (0),
    /**
     * Point geometry type
     */
    POINT(1),
    /**
     * Polyline geometry type
     */
    POLYLINE(3),
    /**
     * Polygon geometry type
     */
    POLYGON(5),
    /**
     * MultiPoint geometry type
     */
    MULTIPOINT(8),
    /**
     * Point with M geometry type
     */
    POINT_M(11),
    /**
     * Polyline with M geometry type
     */
    POLYLINE_M(13),
    /**
     * Polygon with M geometry type
     */
    POLYGON_M(15),
    /**
     * MultiPoint with M geometry type
     */
    MULTIPOINT_M(18),
    /**
     * Point with Z and M geometry type
     */
    POINT_ZM(21),
    /**
     * Polyline with Z and M geometry type
     */
    POLYLINE_ZM(23),
    /**
     * Polygon with Z and M geometry type
     */
    POLYGON_ZM(25),
    /**
     * MultiPoint with Z and M geometry type
     */
    MULTIPOINT_ZM(28),
    /**
     * MultiPatch with Z and M geometry type
     */
    MULTIPATCH_ZM(31);

    private final int code;

    private ShapeType (int value ) {
        this.code = value;
    }

    /**
     * Get geometry type code.
     *
     * @return geometry type code
     */
    public int getCode() {
        return code;
    }

    private static final Map<Integer, ShapeType> lookup = new HashMap<Integer, ShapeType>();

    static {
        for (ShapeType ste : EnumSet.allOf(ShapeType.class)) {
            lookup.put(ste.getCode(), ste);
        }
    }

    /**
     * Get geometry type for given code.
     *
     * @param code geometry code
     * @return ShapeType for given code
     */
    public static ShapeType get(int code) {
        return lookup.get(code);
    }
}
