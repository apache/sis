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
package org.apache.sis.system;


/**
 * Constants related to SIS modules management.
 * The constants in this class are used for two purposes:
 *
 * <ul>
 *   <li>Module symbolic names, as declared in {@code module-info.java} files. In an OSGi context, this would be
 *       the {@code Bundle-SymbolicName} entry of the {@code META-INF/MANIFEST.MF} file in each JAR files.</li>
 *
 *   <li>Logger names for "module-wide" messages, or when the message to log does not fit in a more
 *       accurate category. Note that other logger names are listed in the {@link Loggers} class.</li>
 * </ul>
 *
 * Each constant should be the name of the main package of its corresponding module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Modules {
    /**
     * The {@value} module name.
     */
    public static final String UTILITIES = "org.apache.sis.util";

    /**
     * The {@value} module name.
     */
    public static final String METADATA = "org.apache.sis.metadata";

    /**
     * The {@value} module name.
     */
    public static final String REFERENCING = "org.apache.sis.referencing";

    /**
     * The {@value} module name.
     */
    public static final String REFERENCING_BY_IDENTIFIERS = "org.apache.sis.referencing.gazetteer";

    /**
     * The {@value} module name.
     */
    public static final String FEATURE = "org.apache.sis.feature";

    /**
     * The {@value} module name.
     */
    public static final String RASTER = "org.apache.sis.raster";

    /**
     * The {@value} module name.
     */
    public static final String STORAGE = "org.apache.sis.storage";

    /**
     * The {@value} module name.
     */
    public static final String NETCDF = "org.apache.sis.storage.netcdf";

    /**
     * The {@value} module name.
     */
    public static final String EARTH_OBSERVATION = "org.apache.sis.storage.earth-observation";

    /**
     * The {@value} module name.
     */
    public static final String SHAPEFILE = "org.apache.sis.shapefile";

    /**
     * The {@value} module name.
     */
    public static final String SQL = "org.apache.sis.sql";

    /**
     * The {@value} module name.
     */
    public static final String PORTRAYAL = "org.apache.sis.portrayal";

    /**
     * The {@value} module name.
     */
    public static final String CONSOLE = "org.apache.sis.console";

    /**
     * The {@value} module name.
     */
    public static final String APPLICATION = "org.apache.sis.gui";

    /**
     * The major version number of all Apache SIS modules.
     *
     * @see org.apache.sis.util.Version
     */
    public static final int MAJOR_VERSION = 1;

    /**
     * The minor version number of all Apache SIS modules.
     *
     * @see org.apache.sis.util.Version
     */
    public static final int MINOR_VERSION = 6;

    /**
     * The prefix of all classnames in Apache SIS, including a trailing dot.
     */
    public static final String CLASSNAME_PREFIX = "org.apache.sis.";

    /**
     * Do not allow instantiation of this class.
     */
    private Modules() {
    }
}
