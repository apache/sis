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

    NULL (0),
    POINT(1),
    POLYLINE(3),
    POLYGON(5),
    MULTIPOINT(8),
    POINT_M(11),
    POLYLINE_M(13),
    POLYGON_M(15),
    MULTIPOINT_M(18),
    POINT_ZM(21),
    POLYLINE_ZM(23),
    POLYGON_ZM(25),
    MULTIPOINT_ZM(28),
    MULTIPATCH_ZM(31);

    public static final int VALUE_NULL = 0;
    public static final int VALUE_POINT = 1;
    public static final int VALUE_POLYLINE = 3;
    public static final int VALUE_POLYGON = 5;
    public static final int VALUE_MULTIPOINT = 8;
    public static final int VALUE_POINT_M = 11;
    public static final int VALUE_POLYLINE_M = 13;
    public static final int VALUE_POLYGON_M = 15;
    public static final int VALUE_MULTIPOINT_M = 18;
    public static final int VALUE_POINT_ZM = 21;
    public static final int VALUE_POLYLINE_ZM = 23;
    public static final int VALUE_POLYGON_ZM = 25;
    public static final int VALUE_MULTIPOINT_ZM = 28;
    public static final int VALUE_MULTIPATCH_ZM = 31;

    // used for initializing the enumeration
    public final int value;

    private ShapeType (int value ) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    private static final Map<Integer, ShapeType> lookup = new HashMap<Integer, ShapeType>();

    static {
        for (ShapeType ste : EnumSet.allOf(ShapeType.class)) {
            lookup.put(ste.getValue(), ste);
        }
    }

    public static ShapeType get(int value) {
        return lookup.get(value);
    }
}
