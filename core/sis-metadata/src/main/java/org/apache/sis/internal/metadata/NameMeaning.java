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
 * @version 0.5
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
     * Do not allow instantiation of this class.
     */
    private NameMeaning() {
    }

    /**
     * Returns {@code true} if codes in the given code space are often represented using the URN syntax.
     * Current implementation conservatively returns {@code true} only for {@code "EPSG"}.
     * The list of accepted code spaces may be expanded in any future SIS version.
     *
     * @param  codeSpace The code space (can be {@code null}).
     * @return {@code true} if the given code space is known to use the URN syntax.
     */
    public static boolean usesURN(final String codeSpace) {
        return (codeSpace != null) && codeSpace.equalsIgnoreCase(Constants.EPSG);
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
