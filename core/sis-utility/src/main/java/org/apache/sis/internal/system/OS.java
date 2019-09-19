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

import org.apache.sis.util.logging.Logging;


/**
 * The operation system on which SIS is running.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
public enum OS {
    /**
     * Unknown system.
     */
    UNKNOWN,

    /**
     * Windows.
     */
    WINDOWS,

    /**
     * Mac OS.
     */
    MAC_OS,

    /**
     * Linux.
     */
    LINUX;

    /**
     * Returns the name value of {@code "os.name"} property, or {@code null} if the security manager
     * does not allow us to access this information.
     *
     * <div class="note"><b>Note:</b> {@code uname} is an Unix command providing the same information.</div>
     *
     * @return the operation system name, or {@code null} if this information is not available.
     */
    public static String uname() {
        try {
            return System.getProperty("os.name");
        } catch (SecurityException e) {
            Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM), OS.class, "uname", e);
            return null;
        }
    }

    /**
     * Returns the operating system SIS is currently on.
     *
     * @return the operation system.
     */
    public static OS current() {
        final String name = uname();
        if (name != null) {
            if (name.contains("Windows")) return WINDOWS;
            if (name.contains("Mac OS"))  return MAC_OS;
            if (name.contains("Linux"))   return LINUX;
        }
        return UNKNOWN;
    }
}
