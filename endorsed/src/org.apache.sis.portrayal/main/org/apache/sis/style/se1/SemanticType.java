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
package org.apache.sis.style.se1;


/**
 * Identifies the more general "type" of geometry that this style is meant to act upon.
 * In the SE 1.1.0 specification, this is restricted to the following values:
 *
 * <ul>
 *   <li>{@code generic:point}</li>
 *   <li>{@code generic:line}</li>
 *   <li>{@code generic:polygon}</li>
 *   <li>{@code generic:text}</li>
 *   <li>{@code generic:raster}</li>
 *   <li>{@code generic:any}</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 */
public enum SemanticType {
    /**
     * Semantic identifies a point geometry.
     */
    POINT,

    /**
     * Semantic identifies a line geometry.
     */
    LINE,

    /**
     * Semantic identifies a polygon geometry.
     */
    POLYGON,

    /**
     * Semantic identifies a text.
     */
    TEXT,

    /**
     * Semantic identifies a raster.
     */
    RASTER,

    /**
     * Semantic identifies any geometry.
     */
    ANY;
}
