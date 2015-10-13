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
package org.apache.sis.internal.metadata;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.opengis.parameter.*;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.apache.sis.util.Static;
import org.apache.sis.internal.util.Constants;


/**
 * The meaning of some part of URN in the {@code "ogc"} namespace.
 * The meaning are defined by <cite>OGC Naming Authority</cite> (OGCNA) or other OGC sources.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.internal.util.DefinitionURI
 * @see <a href="http://www.opengeospatial.org/ogcna">http://www.opengeospatial.org/ogcna</a>
 * @see <a href="http://portal.opengeospatial.org/files/?artifact_id=24045">Definition identifier URNs in OGC namespace</a>
 */
public final class NameMeaning extends Static {
    /**
     * Subtypes of {@link IdentifiedObject} for which an object type is defined.
     * For each interface at index <var>i</var>, the type is {@code TYPES[i]}.
     *
     * <p>For performance reasons, most frequently used types should be first.</p>
     */
    private static final Class<?>[] CLASSES = {
        CoordinateReferenceSystem.class,
        Datum.class,
        Ellipsoid.class,
        PrimeMeridian.class,
        CoordinateSystem.class,
        CoordinateSystemAxis.class,
        CoordinateOperation.class,
        OperationMethod.class,
        ParameterDescriptor.class,
        ReferenceSystem.class
    };

    /**
     * The object types for instances of {@link #CLASSES}.
     * See {@link org.apache.sis.internal.util.DefinitionURI} javadoc for a list of object types in URN.
     */
    private static final String[] TYPES = {
        "crs",
        "datum",
        "ellipsoid",
        "meridian",
        "cs",
        "axis",
        "coordinateOperation",
        "method",
        "parameter",
        "referenceSystem"
    };

    /**
     * Naming authorities allowed to appear in {@code "urn:ogc:def:"}.
     *
     * <p><b>Note on the case:</b> The <cite>"Name type specification — definitions"</cite> document (OGC 09-048) writes
     * authorities in upper cases, while <a href="http://www.opengis.net/def/auth/">http://www.opengis.net/def/auth/</a>
     * use lower cases. Apache SIS uses upper cases for now.</p>
     *
     * @see <a href="http://www.opengis.net/def/auth/">http://www.opengis.net/def/auth/</a>
     *
     * @since 0.7
     */
    private static final Map<String,String> AUTHORITIES = new HashMap<String,String>(12);
    static {
        add(Constants.EPSG);    // IOGP
        add(Constants.OGC);     // Open Geospatial Consortium
        add("OGC-WFS");         // OGC Web Feature Service
        add("SI");              // Système International d'Unités
        add("UCUM");            // Unified Code for Units of Measure
        add("UNSD");            // United Nations Statistics Division
        add("USNO");            // United States Naval Observatory
    }

    /**
     * Adds the given authority to the {@link #AUTHORITIES} map.
     * This method shall be invoked at class initialization time only.
     */
    private static void add(final String authority) {
        AUTHORITIES.put(authority, authority);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private NameMeaning() {
    }

    /**
     * Returns the authority to format for the given code space, or {@code null} if there is no known authority
     * in URN syntax for the given code space. The return value is used for fixing the Apache SIS policy regarding
     * lower or upper cases (both conventions are used in different OGC resources).
     *
     * @param  codeSpace The code space (can be {@code null}).
     * @return The authority to format in the URN, or {@code null} if none.
     *
     * @since 0.7
     */
    public static String authority(String codeSpace) {
        if (codeSpace != null) {
            codeSpace = AUTHORITIES.get(codeSpace.toUpperCase(Locale.US));
        }
        return codeSpace;
    }

    /**
     * Returns the "object type" part of an OGC URN for the given class, or {@code null} if unknown.
     * See {@link org.apache.sis.internal.util.DefinitionURI} javadoc for a list of object types in URN.
     *
     * @param  type The class for which to get the URN type.
     * @return The URN type, or {@code null} if unknown.
     */
    public static String toObjectType(final Class<?> type) {
        for (int i=0; i<CLASSES.length; i++) {
            if (CLASSES[i].isAssignableFrom(type)) {
                return TYPES[i];
            }
        }
        return null;
    }
}
