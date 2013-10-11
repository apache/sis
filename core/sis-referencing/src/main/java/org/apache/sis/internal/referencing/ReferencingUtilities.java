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
package org.apache.sis.internal.referencing;

import java.util.Map;
import java.util.HashMap;
import javax.measure.unit.NonSI;
import org.apache.sis.util.Static;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;


/**
 * A set of static methods working on GeoAPI referencing objects.
 * Some of those methods may be useful, but not really rigorous.
 * This is why they do not appear in the public packages.
 *
 * <p><strong>Do not rely on this API!</strong> It may change in incompatible way in any future release.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public final class ReferencingUtilities extends Static {
    /**
     * The EPSG code for Greenwich meridian.
     */
    public static final String GREENWICH_CODE = "8901";

    /**
     * The Greenwich meridian (EPSG:8901), with angular measurements in decimal degrees.
     */
    public static final DefaultPrimeMeridian GREENWICH;
    static {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY, new NamedIdentifier(Citations.EPSG, "Greenwich")); // Name is fixed by ISO 19111.
        properties.put(IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, GREENWICH_CODE));
        GREENWICH = new DefaultPrimeMeridian(properties, 0, NonSI.DEGREE_ANGLE);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private ReferencingUtilities() {
    }
}
