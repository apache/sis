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
package org.apache.sis.measure;

import javax.measure.Quantity;


/**
 * The rate of change of an angular displacement with respect to time.
 * The SI unit is radians per second (rad/s) and the EPSG code is 1035.
 * This quantity is used for describing the rate of changes of {@code rX}, {@code rY} and {@code rZ} terms in
 * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters}, under the effect
 * of plate tectonics.
 *
 * <p>Most quantity types are defined in the {@link javax.measure.quantity}.
 * This {@code AngularVelocity} type is an extension to the standard types.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @see Units#RADIANS_PER_SECOND
 * @see org.apache.sis.referencing.datum.TimeDependentBWP
 */
public interface AngularVelocity extends Quantity<AngularVelocity> {
}
