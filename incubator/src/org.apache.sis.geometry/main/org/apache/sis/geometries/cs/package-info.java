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

/**
 * Coordinate systems in which geometries are defined, and the directions measured within them.
 *
 * <p>This package implements the <cite>Coordinate</cite> requirements class of ISO 19107:2019
 * (clause 6.2): the {@linkplain org.apache.sis.geometries.cs.GeometricCoordinateSystem coordinate
 * system} on which geometric measures are computed, the {@linkplain org.apache.sis.geometries.cs.Bearing
 * bearing} which expresses a direction at a position, and the code lists of reference directions
 * from which a bearing can be measured.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
package org.apache.sis.geometries.cs;
