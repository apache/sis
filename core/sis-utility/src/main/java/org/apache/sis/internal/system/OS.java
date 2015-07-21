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
 * The operation system on which SIS is running.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public enum OS {
    /**
     * Unknown system.
     */
    UNKNOWN(false),

    /**
     * Windows.
     */
    WINDOWS(false),

    /**
     * Mac OS.
     */
    MAC_OS(true),

    /**
     * Linux.
     */
    LINUX(true);

    /**
     * {@code true} if this OS is a kind of Unix.
     */
    public final boolean unix;

    /**
     * Creates a new enumeration.
     */
    private OS(final boolean unix) {
        this.unix = unix;
    }

    /**
     * Returns the operating system SIS is currently on.
     *
     * @return The operation system.
     */
    public static OS current() {
        final String name = System.getProperty("os.name");
        if (name != null) {
            if (name.contains("Windows")) return WINDOWS;
            if (name.contains("Mac OS"))  return MAC_OS;
            if (name.contains("Linux"))   return LINUX;
        }
        return UNKNOWN;
    }
}
