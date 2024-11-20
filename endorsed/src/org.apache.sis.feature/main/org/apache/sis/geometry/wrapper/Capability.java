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
package org.apache.sis.geometry.wrapper;


/**
 * Features that a library may support.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum Capability {
    /**
     * The capability to store <var>z</var> coordinate values in points or geometries.
     * Geometries with a <var>z</var> coordinates are sometime called 2.5-dimensional.
     */
    Z_COORDINATE,

    /**
     * The capability to store <var>m</var> coordinate values in points or geometries.
     */
    M_COORDINATE,

    /**
     * The capability to create geometries backed by the {@code float} primitive type instead
     * of the {@code double} primitive type. If single-precision mode is supported, using that
     * mode may reduce memory usage. This capability is used for checking whether it is worth
     * to invoke {@link Vector#isSinglePrecision()} for example.
     */
    SINGLE_PRECISION
}
