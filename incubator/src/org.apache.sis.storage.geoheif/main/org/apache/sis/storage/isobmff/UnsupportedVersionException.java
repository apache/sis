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
package org.apache.sis.storage.isobmff;

import org.apache.sis.storage.UnsupportedEncodingException;


/**
 * Thrown when a box declares an unsupported version.
 * This is declared as a specific sub-class for allowing callers
 * to catch the exception and skip the unsupported boxes.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public final class UnsupportedVersionException extends UnsupportedEncodingException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1224246149872476021L;

    /**
     * Creates a new exception for the given box identifier and version.
     *
     * @param  type     the four-character type of the box.
     * @param  version  the unsupported version.
     */
    public UnsupportedVersionException(final int type, final int version) {
        super("Version " + version + " of '" + Box.formatFourCC(type) + "' boxes is unsupported.");
    }
}
