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
package org.apache.sis.internal.system;


/**
 * Names of loggers used in SIS other than the "module-wide" loggers. We often use approximatively one logger
 * per module, using the appropriate constant of the {@link Modules} class as the "module-wide" logger name.
 * However we also have a few more specialized loggers, which are listed here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class Loggers {
    /**
     * The logger for Apache SIS internal operations. The name of this logger does not match the package name
     * of the classes using it, because this logger name does not have the {@code "internal"} part in it.
     */
    public static final String SYSTEM = "org.apache.sis.system";

    /**
     * The logger for operations related to XML marshalling or unmarshalling.
     */
    public static final String XML = "org.apache.sis.xml";

    /**
     * The logger for operations related to WKT parsing or formatting.
     * Note that WKT formatting often occurs in different packages.
     */
    public static final String WKT = "org.apache.sis.io.wkt";

    /**
     * The logger for operations related to geometries.
     */
    public static final String GEOMETRY = "org.apache.sis.geometry";

    /**
     * The logger for metadata operation related to the ISO 19115 standard.
     * This is a child of the logger for all metadata operations.
     */
    public static final String ISO_19115 = "org.apache.sis.metadata.iso";

    /**
     * The logger name for operation related to the creating of CRS objects.
     * This is a child of the logger for all referencing operations.
     */
    public static final String CRS_FACTORY = "org.apache.sis.referencing.factory";

    /**
     * The logger name for operation related to coordinate operations, in particular math transforms.
     * This is a child of the logger for all referencing operations.
     */
    public static final String COORDINATE_OPERATION = "org.apache.sis.referencing.operation";

    /**
     * The logger name for operation related to localization.
     */
    public static final String LOCALIZATION = "org.apache.sis.util.resources";

    /**
     * Do not allow instantiation of this class.
     */
    private Loggers() {
    }
}
