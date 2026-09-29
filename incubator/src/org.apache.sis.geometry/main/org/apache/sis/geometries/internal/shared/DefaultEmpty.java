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
package org.apache.sis.geometries.internal.shared;

import java.util.Objects;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometry.GeneralEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultEmpty extends AbstractGeometry implements Empty {

    private final DataPointsType attType;

    public DefaultEmpty(DataPointsType attType) {
        this.attType = attType;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return attType.getAttributeSystem(DataPointsType.ATT_POSITION).getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DataPointsType getDataPointsType() {
        return attType;
    }

    @Override
    public Envelope getEnvelope() {
        final GeneralEnvelope env = new GeneralEnvelope(getCoordinateReferenceSystem());
        env.setToNaN();
        return env;
    }

    @Override
    public int hashCode() {
        return 11 * Objects.hashCode(attType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(attType, ((DefaultEmpty) obj).attType);
    }
}
