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
package org.apache.sis.referencing.dggs.smc;

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.util.ArgumentChecks;


/**
 * Converts positions to SMC zone identifiers at a chosen refinement level.
 *
 * @author Johann Sorel (Geomatys)
 */
final class SmcCoder extends DiscreteGlobalGridReferenceSystem.Coder {

    private final SmcDggrs dggrs;

    private int level;

    SmcCoder(final SmcDggrs dggrs) {
        this.dggrs = dggrs;
    }

    @Override
    public SmcDggrs getReferenceSystem() {
        return dggrs;
    }

    @Override
    public int getPrecisionLevel() {
        return level;
    }

    /**
     * Sets the refinement level of encoded zones.
     *
     * @throws IllegalArgumentException if the level is outside the lattice levels.
     */
    @Override
    public void setPrecisionLevel(final int level) {
        ArgumentChecks.ensureBetween("level", 0, dggrs.dggs.dggh.size() - 1, level);
        this.level = level;
    }

    /**
     * Returns the {@code "level-i-j"} identifier of the zone containing the given position.
     *
     * @throws TransformException if the position cannot be converted or is outside the lattice domain.
     */
    @Override
    public String encode(final DirectPosition position) throws TransformException {
        return getZone(position).getTextIdentifier().toString();
    }

    /**
     * Returns the 64-bit identifier of the zone containing the given position.
     *
     * @throws TransformException if the position cannot be converted or is outside the lattice domain.
     */
    @Override
    public Long encodeIdentifier(final DirectPosition position) throws TransformException {
        return getZone(position).getLongIdentifier();
    }

    private SmcZone getZone(final DirectPosition position) throws TransformException {
        return dggrs.dggs.dggh.get(level).getZone(position);
    }
}
