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
package org.apache.sis.referencing.dggs.internal.shared;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.util.InternationalString;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractZone<T extends DiscreteGlobalGridReferenceSystem> implements Zone {

    protected T dggrs;

    public AbstractZone(T dggrs) {
        this.dggrs = dggrs;
    }

    @Override
    public Double volumeMetersCube() {
        return null;
    }

    @Override
    public Double temporalDurationSeconds() {
        return null;
    }

    @Override
    public Collection<? extends InternationalString> getAlternativeGeographicIdentifiers() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public TemporalExtent getTemporalExtent() {
        return null;
    }

    @Override
    public Party getAdministrator() {
        return dggrs.getOverallOwner();
    }

    @Override
    public boolean isNeighbor(Object zone) {
        final Zone cdt = dggrs.getGridSystem().getHierarchy().getZone(zone);
        return getNeighbors().contains(cdt);
    }

    @Override
    public boolean isSibling(Object zone) {
        final Zone cdt = dggrs.getGridSystem().getHierarchy().getZone(zone);
        if (cdt.getLocationType().getRefinementLevel() != getLocationType().getRefinementLevel()) return false;
        final Collection<? extends Zone> parents = getParents();
        for (Zone z : cdt.getParents()) {
            if (parents.contains(z)) return true;
        }
        return false;
    }

    @Override
    public boolean isAncestorOf(Object zone, int maxRelativeDepth) {
        final Zone cdt = dggrs.getGridSystem().getHierarchy().getZone(zone);
        final int cdtz = cdt.getLocationType().getRefinementLevel();
        final int relativeDepth = cdtz - getLocationType().getRefinementLevel();
        if (relativeDepth <= 0 || relativeDepth > maxRelativeDepth) return false;
        return searchInParents(cdt, this, maxRelativeDepth);
    }

    private boolean searchInParents(Zone base, Zone searched, int toDepth) {
        if (toDepth == 0) {
            return searched.equals(base);
        } else {
            for (Zone p : base.getParents()) {
                if (searchInParents(p, searched, toDepth-1)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDescendantOf(Object zone, int maxRelativeDepth) {
        final Zone cdt = dggrs.getGridSystem().getHierarchy().getZone(zone);
        return cdt.isAncestorOf(this, maxRelativeDepth);
    }

    @Override
    public boolean overlaps(Object zone) {
        final Zone cdt = dggrs.getGridSystem().getHierarchy().getZone(zone);
        if (cdt.getLocationType().getRefinementLevel() == getLocationType().getRefinementLevel()) return false;
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractZone other = (AbstractZone) obj;
        if (!Objects.equals(this.getTextIdentifier(), other.getTextIdentifier())) {
            return false;
        }
        return Objects.equals(this.dggrs, other.dggrs);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.dggrs);
        hash = 53 * hash + Objects.hashCode(this.getTextIdentifier());
        return hash;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getGeographicIdentifier();
    }
}
