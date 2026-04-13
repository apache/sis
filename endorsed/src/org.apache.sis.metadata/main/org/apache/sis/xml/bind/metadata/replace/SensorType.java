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
package org.apache.sis.xml.bind.metadata.replace;

import java.util.List;
import org.opengis.annotation.UML;
import org.opengis.annotation.Specification;
import org.opengis.util.CodeList;


/**
 * The code list for {@code <gmi:MI_SensorTypeCode>}.
 * This code list is not defined in ISO 19115-2 but appears in XML schemas.
 * For now GeoAPI does not yet provides it, but this choice may be revisited in a future GeoAPI version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@UML(identifier = "MI_SensorTypeCode", specification = Specification.ISO_19115_2)   // Actually only in XML schema.
public final class SensorType extends CodeList<SensorType> {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 3510875680392393838L;

    /*
     * We need to construct values with `valueOf(String)` instead of the constructor
     * because this package is not exported to GeoAPI. See `CodeList` class javadoc.
     */

    /**
     * The sensor is a radiometer.
     */
    public static final SensorType RADIOMETER;

    /**
     * All code list values created in the currently running <abbr>JVM</abbr>.
     */
    private static final List<SensorType> VALUES = initialValues(
        // Inline assignments for getting compiler error if a field is missing or duplicated.
        RADIOMETER = new SensorType("RADIOMETER"));

    /**
     * Constructs an element of the given name.
     *
     * @param  name  the name of the new element.
     *         This name must not be in use by another element of this type.
     */
    private SensorType(final String name) {
        super(name);
    }

    /**
     * Returns the list of {@code SensorType}s.
     *
     * @return all code {@linkplain #values() values} for this code list.
     */
    @Override
    public SensorType[] family() {
        return values();
    }

    /**
     * Returns the list of {@code SensorType}s.
     * This method must be declared even if not invoked explicitly because it may be invoked
     * by reflection by {@link org.apache.sis.util.internal.shared.CodeLists#values(Class)}.
     *
     * @return the list of codes declared in the current <abbr>JVM</abbr>.
     */
    public static SensorType[] values() {
        return VALUES.toArray(SensorType[]::new);
    }

    /**
     * Returns the sensor type that matches the given string, or returns a new one if none match it.
     *
     * @param  code  the name of the code to fetch or to create.
     * @return a code matching the given name.
     */
    public static SensorType valueOf(String code) {
        return valueOf(VALUES, code, SensorType::new);
    }
}
