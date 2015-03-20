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
package org.apache.sis.util.iso;


/**
 * The global namespace. Only one instance of this class is allowed to exists. We do not expose
 * any global namespace in public API since ISO 19103 does not define them and users should not
 * need to handle them explicitely.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class GlobalNameSpace extends DefaultNameSpace {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4652341179694633152L;

    /**
     * The unique global namespace.
     */
    public static final GlobalNameSpace GLOBAL = new GlobalNameSpace();

    /**
     * Creates the global namespace.
     */
    private GlobalNameSpace() {
    }

    /**
     * Indicates that this namespace is a "top level" namespace.
     */
    @Override
    public boolean isGlobal() {
        return true;
    }

    /**
     * Returns the unique instance of global name space on deserialization.
     *
     * @return The unique instance.
     */
    @Override
    Object readResolve() {
        return GLOBAL;
    }
}
