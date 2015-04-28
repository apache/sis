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
 * Helper classes for creating {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform
 * Math Transforms} from a set of points.
 * The builder classes require a matched set of known positions, one from a "source" data set and another
 * from a "target" data set. The builder will then provide a transformation positions from the "source" CRS
 * to the "target" CRS.
 *
 * <p>Algorithms in this package use a <cite>least squares</cite> estimation method.
 * The matching parameters are estimated by minimizing the sum of the squared distances
 * between the given points and the fitted points (i.e. the points calculated using the
 * transform). Note that "distance" here is not necessarily the Euclidian distance or a
 * geodesic distance. It may be an approximation of Euclidian distance for implementation
 * convenience.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
package org.apache.sis.referencing.operation.builder;
