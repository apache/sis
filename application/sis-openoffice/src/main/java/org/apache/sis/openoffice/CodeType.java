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
package org.apache.sis.openoffice;


/**
 * Whether an authority code is defined in the URN namespace.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
enum CodeType {
    /**
     * The authority code is defined in the {@code "urn:"} namespace.
     */
    URN,

    /**
     * The code is not defined in the URN namespace but is nevertheless presumed to be a CRS authority code.
     */
    CRS;

    /**
     * Infers the type for the given authority code.
     */
    static CodeType guess(final String codeOrPath) {
        if (codeOrPath.regionMatches(true, 0, "urn:", 0, 4)) {
            return URN;
        } else {
            return CRS;
        }
    }
}
