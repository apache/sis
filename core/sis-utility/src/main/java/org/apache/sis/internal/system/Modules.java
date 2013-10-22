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
 * Constants related to SIS modules management.
 * This class contains the OSGi module symbolic names, as declared in the {@code Bundle-SymbolicName}
 * entry of the {@code META-INF/MANIFEST.MF} file in each JAR files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class Modules {
    /**
     * The {@value} module name.
     */
    public static final String UTILITIES = "org.apache.sis.utility";

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
    public static final String STORAGE = "org.apache.sis.storage";

    /**
     * The {@value} module name.
     */
    public static final String NETCDF = "org.apache.sis.storage.netcdf";

    /**
     * Do not allows instantiation of this class.
     */
    private Modules() {
    }
}
