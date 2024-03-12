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

import org.opengis.annotation.UML;
import org.opengis.annotation.Specification;
import org.opengis.util.CodeList;

// Specific to the main branch:
import java.util.List;
import java.util.ArrayList;


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

    /**
     * List of all enumerations of this type.
     * Must be declared before any enum declaration.
     */
    private static final List<SensorType> VALUES = new ArrayList<>();

    /**
     * The sensor is a radiometer.
     */
    public static final SensorType RADIOMETER = new SensorType("RADIOMETER");

    /**
     * Constructs an element of the given name. The new element is
     * automatically added to the list returned by {@link #values()}.
     *
     * @param  name  the name of the new element.
     *         This name must not be in use by another element of this type.
     */
    private SensorType(final String name) {
        super(name, VALUES);
    }

    /**
     * Returns the list of {@code SensorType}s.
     *
     * @return the list of codes declared in the current JVM.
     */
    public static SensorType[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(SensorType[]::new);
        }
    }

    /**
     * Disables the search for UML identifiers because we do not export this package to GeoAPI.
     *
     * @return {@code null}.
     */
    @Override
    public String identifier() {
        return null;
    }

    /**
     * Returns the list of codes of the same kind as this code list element.
     * Invoking this method is equivalent to invoking {@link #values()}, except that
     * this method can be invoked on an instance of the parent {@code CodeList} class.
     *
     * @return all code {@linkplain #values() values} for this code list.
     */
    @Override
    public SensorType[] family() {
        return values();
    }

    /**
     * Returns the sensor type that matches the given string, or returns a new one if none match it.
     *
     * @param  code  the name of the code to fetch or to create.
     * @return a code matching the given name.
     */
    public static SensorType valueOf(String code) {
        return valueOf(SensorType.class, code);
    }
}
