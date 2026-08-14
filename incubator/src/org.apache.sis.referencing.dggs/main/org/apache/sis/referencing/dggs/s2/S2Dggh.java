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
package org.apache.sis.referencing.dggs.s2;

import com.google.common.geometry.S2CellId;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.internal.shared.AbstractDiscreteGlobalGridHierarchy;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class S2Dggh extends AbstractDiscreteGlobalGridHierarchy<S2Dggrs> {

    private final DiscreteGlobalGrid[] grids;

    S2Dggh(S2Dggrs dggrs) {
        super(dggrs);
        grids = new DiscreteGlobalGrid[S2CellId.MAX_LEVEL];
        for (int i = 0; i < grids.length; i++) {
            grids[i] = new S2Dgg(this, i);
        }
    }

    @Override
    public DiscreteGlobalGrid get(int level) {
        return grids[level];
    }

    @Override
    public int size() {
        return grids.length;
    }

    @Override
    public boolean supportLongIdentifiers() {
        return true;
    }

    @Override
    public String toTextIdentifier(Object zoneId) throws IllegalArgumentException {
        if (zoneId instanceof CharSequence cs) {
            return cs.toString();
        } else if (zoneId instanceof Long l) {
            return idAsText(l);
        } else if (zoneId instanceof S2Zone z) {
            return z.getTextIdentifier().toString();
        } else {
            throw new IllegalArgumentException("Identifer not supported " + zoneId);
        }
    }

    @Override
    public long toLongIdentifier(Object zoneId) throws IllegalArgumentException {
        if (zoneId instanceof CharSequence cs) {
            return idAsLong(cs);
        } else if (zoneId instanceof Long l) {
            return l;
        } else if (zoneId instanceof S2Zone z) {
            return z.getLongIdentifier();
        } else {
            throw new IllegalArgumentException("Identifer not supported " + zoneId);
        }
    }

    @Override
    public Zone getZone(Object identifier) throws IllegalArgumentException {
        if (identifier instanceof S2Zone z) return z;
        return new S2Zone(dggrs, toLongIdentifier(identifier));
    }

    static final String idAsText(final long hash) {
        return new S2CellId(hash).toToken();
    }

    static final long idAsLong(final CharSequence cs) {
        return S2CellId.fromToken(cs.toString()).id();
    }

}
