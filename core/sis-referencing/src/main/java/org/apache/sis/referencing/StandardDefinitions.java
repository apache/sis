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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.HashMap;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.datum.DefaultEllipsoid;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;


/**
 * Definitions of referencing objects identified by the {@link StandardObjects} constants.
 * This class is used only as a fallback if the objects can not be fetched from the EPSG database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
final class StandardDefinitions {
    /**
     * Do not allow instantiation of this class.
     */
    private StandardDefinitions() {
    }

    /**
     * Creates an ellipsoid from hard-coded values for the given code.
     *
     * @param  code The EPSG or SIS code.
     * @return The ellipsoid for the given code.
     */
    static Ellipsoid createEllipsoid(final short code) {
        String  name;          // No default value
        String  alias          = null;
        double  semiMajorAxis; // No default value
        double  other;         // No default value
        boolean ivfDefinitive  = true;
        Unit<Length> unit      = SI.METRE;
        switch (code) {
            case 7030: name  = "WGS 84";       alias = "WGS84";              semiMajorAxis = 6378137.0; other = 298.257223563; break;
            case 7043: name  = "WGS 72";       alias = "NWL 10D";            semiMajorAxis = 6378135.0; other = 298.26;        break;
            case 7019: name  = "GRS 1980";     alias = "International 1979"; semiMajorAxis = 6378137.0; other = 298.257222101; break;
            case 7022: alias = "Hayford 1909"; name  = "International 1924"; semiMajorAxis = 6378388.0; other = 297.0;         break;
            case 7008: name  = "Clarke 1866";  ivfDefinitive = false;        semiMajorAxis = 6378206.4; other = 6356583.8;     break;
            case   -1: name  = "Sphere";       ivfDefinitive = false;        semiMajorAxis =            other = 6371000;       break;
            default:   throw new AssertionError(code);
        }
        final Map<String,Object> map = new HashMap<>(8);
        final Citation authority;
        if (code >= 0) {
            map.put(IDENTIFIERS_KEY, new NamedIdentifier(authority = Citations.EPSG, String.valueOf(code)));
        } else {
            authority = Citations.SIS;
        }
        map.put(NAME_KEY, new NamedIdentifier(authority, name));
        map.put(ALIAS_KEY, alias); // May be null, which is okay.
        if (ivfDefinitive) {
            return DefaultEllipsoid.createFlattenedSphere(map, semiMajorAxis, other, unit);
        } else {
            return DefaultEllipsoid.createEllipsoid(map, semiMajorAxis, other, unit);
        }
    }
}
