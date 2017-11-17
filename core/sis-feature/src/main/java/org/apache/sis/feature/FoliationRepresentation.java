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
package org.apache.sis.feature;


/**
 * Specifies whether trajectories are represented in a single <cite>moving feature</cite> instance
 * or fragmented in distinct static feature instances.
 *
 * <div class="note"><b>Example:</b>
 * consider the following trajectories:
 *
 * <table class="sis">
 *   <caption>Moving features example</caption>
 *   <tr><th>Identifier</th> <th>Time</th> <th>Trajectory</th>   <th>Status</th></tr>
 *   <tr><td>John Smith</td> <td>8:00</td> <td>(3 4), (3 5)</td> <td>Walking</td></tr>
 *   <tr><td>Joe  Blo</td>   <td>8:00</td> <td>(5 5), (6 6)</td> <td>Walking</td></tr>
 *   <tr><td>John Smith</td> <td>8:05</td> <td>(3 5), (3 9)</td> <td>Running</td></tr>
 * </table>
 *
 * In this example, John Smith's trajectory can be represented in two ways:
 * we can construct a single <cite>moving feature</cite> instance representing the full trajectory (3 4), (3 5), (3 9)
 * with some time representation (for example by adding a temporal dimension in each coordinate) and dynamic "Status"
 * property. Or alternatively, we can keep John Smith's trajectory fragmented in two {@code Feature} instance where
 * each instance can be handled as a static feature.
 * </div>
 *
 * This enumeration can be used with {@link org.apache.sis.storage.DataStore} as a hint about desired representation
 * of moving features.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public enum FoliationRepresentation {
    /**
     * Trajectories stored in a single {@code Feature} instance.
     * Every point on the trajectory may be at a different time.
     * Properties may be dynamic, i.e. have time-dependent value.
     */
    ASSEMBLED,

    /**
     * Trajectories stored in distinct {@code Feature} instances,
     * each of them handled as if it was a static feature.
     */
    FRAGMENTED
}
