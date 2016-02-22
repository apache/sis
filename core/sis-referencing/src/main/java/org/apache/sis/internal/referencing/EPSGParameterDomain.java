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

import java.util.Set;
import javax.measure.unit.Unit;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * The domain of values of an EPSG parameter which accepts different units.
 * An example is the EPSG:8617 (<cite>Ordinate 1 of evaluation point</cite>) parameter,
 * which may be used in the EPSG database with either metres or degrees units.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class EPSGParameterDomain extends NumberRange<Double> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8462017652419319184L;

    /**
     * The units of measurement.
     */
    public final Set<Unit<?>> units;

    /**
     * Creates a new parameter descriptor for the given units.
     *
     * @param units The units.
     */
    public EPSGParameterDomain(final Set<Unit<?>> units) {
        super(Double.class, null, false, null, false);
        this.units = CollectionsExt.unmodifiableOrCopy(units);
    }
}
