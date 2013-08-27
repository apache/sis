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
package org.apache.sis.referencing.cs;

import java.util.Set;
import java.util.Collection;
import javax.measure.unit.Unit;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;


public class DefaultCoordinateSystemAxis implements CoordinateSystemAxis {
    public DefaultCoordinateSystemAxis(final String        abbreviation,
                                       final AxisDirection direction,
                                       final Unit<?>       unit)
    {
    }

    @Override
    public ReferenceIdentifier getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<GenericName> getAlias() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<ReferenceIdentifier> getIdentifiers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAbbreviation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AxisDirection getDirection() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMinimumValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMaximumValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RangeMeaning getRangeMeaning() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Unit<?> getUnit() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InternationalString getRemarks() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
