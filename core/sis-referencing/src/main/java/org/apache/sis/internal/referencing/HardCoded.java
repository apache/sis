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
package org.apache.sis.internal.referencing;

import org.apache.sis.util.Static;


/**
 * Hard coded values (typically identifiers).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class HardCoded extends Static {
    /**
     * The {@value} code space.
     */
    public static final String EPSG = "EPSG";

    /**
     * The {@value} code space.
     */
    public static final String CRS = "CRS";

    /**
     * The {@code CRS:27} identifier for a coordinate reference system.
     */
    public static final byte CRS27 = 27;

    /**
     * The {@code CRS:83} identifier for a coordinate reference system.
     */
    public static final byte CRS83 = 83;

    /**
     * The {@code CRS:84} identifier for a coordinate reference system.
     */
    public static final byte CRS84 = 84;

    /**
     * Do not allow instantiation of this class.
     */
    private HardCoded() {
    }
}
