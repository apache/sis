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
package org.apache.sis.coverage.grid;


/**
 * Specifies clipping behavior during computations of {@link GridExtent}.
 * The clipping mode controls which computation steps are constrained to
 * the extent of the {@linkplain GridDerivation#base base grid geometry}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum GridClippingMode {
    /**
     * No clipping is applied.
     */
    NONE,

    /**
     * All computation steps (including addition of a margin) are constrained to the base grid extent.
     * The final grid geometry computed by {@link GridDerivation} will have an extent completely contained
     * inside the extent of the base grid geometry.
     */
    STRICT,

    /**
     * Clipping is applied on the <cite>Area Of Interest</cite> supplied by user, before expansion for
     * margin and chunk size. After clipping, the addition of a margin and the rounding to chunk size
     * may cause the final grid extent to expand outside the base grid extent.
     *
     * @see GridDerivation#margin(int...)
     */
    BORDER_EXPANSION
}
