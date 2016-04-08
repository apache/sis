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
package org.apache.sis.internal.gpx;

import org.apache.sis.util.ArgumentChecks;

/**
 * GPX versions enumeration
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public enum GPXVersion {
    /**
     * GPX 1.0
     */
    v1_0_0,
    /**
     * GPX 1.1
     */
    v1_1_0;

    /**
     * Convert code to GPXVersion enum.
     *
     * @param code gpx version as string
     * @return enumeration
     * @throws NumberFormatException if version is not formatted as expected
     */
    public static GPXVersion toVersion(String code) throws NumberFormatException {
        ArgumentChecks.ensureNonNull("code", code);
        switch (code.trim()) {
            case "1.0":
                return v1_0_0;
            case "1.1":
                return v1_1_0;
            default:
                throw new NumberFormatException("Invalid version number " + code);
        }
    }
}
