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
package org.apache.sis.internal.jdk7;


/**
 * Place holder for {@link java.nio.file.StandardOpenOption}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * We simulate the enum by {@link String} values.
 * This allow us to avoid putting a non-standard enum in public API.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class StandardOpenOption {
    /**
     * Do not allow instantiation of this class.
     */
    private StandardOpenOption() {
    }

    /** See {@link java.nio.file.StandardOpenOption#READ}. */
    public static final String READ = "READ";

    /** See {@link java.nio.file.StandardOpenOption#WRITE}. */
    public static final String WRITE = "WRITE";

    /** See {@link java.nio.file.StandardOpenOption#APPEND}. */
    public static final String APPEND = "APPEND";

    /** See {@link java.nio.file.StandardOpenOption#TRUNCATE_EXISTING}. */
    public static final String TRUNCATE_EXISTING = "TRUNCATE_EXISTING";

    /** See {@link java.nio.file.StandardOpenOption#DELETE_ON_CLOSE}. */
    public static final String DELETE_ON_CLOSE = "DELETE_ON_CLOSE";

    /** See {@link java.nio.file.StandardOpenOption#SYNC}. */
    public static final String SYNC = "SYNC";

    /** See {@link java.nio.file.StandardOpenOption#DSYNC}. */
    public static final String DSYNC = "DSYNC";
}
