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
package org.apache.sis.referencing.dggs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.gazetteer.LocationType;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.util.SimpleInternationalString;


/**
 * A subtype of LocationType dedicated to DGGRS refinement levels.
 * It only stores a depth level.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class RefinementLevel implements LocationType {

    private final DiscreteGlobalGridReferenceSystem dggrs;
    private final int level;

    public RefinementLevel(DiscreteGlobalGridReferenceSystem dggrs, int level) {
        this.dggrs = dggrs;
        this.level = level;
    }

    public int getRefinementLevel() {
        return level;
    }

    @Override
    public InternationalString getName() {
        return new SimpleInternationalString("" + level);
    }

    @Override
    public InternationalString getTheme() {
        return new SimpleInternationalString("DGGRS");
    }

    @Override
    public Collection<? extends InternationalString> getIdentifications() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public InternationalString getDefinition() {
        return getName();
    }

    @Override
    public GeographicExtent getTerritoryOfUse() {
        return new DefaultGeographicBoundingBox(-180, 180, -90, 90);
    }

    @Override
    public DiscreteGlobalGridReferenceSystem getReferenceSystem() {
        return dggrs;
    }

    @Override
    public Party getOwner() {
        return new DefaultOrganisation();
    }

    @Override
    public Collection<? extends LocationType> getParents() {
        if (level == 0) return Collections.EMPTY_LIST;
        return List.of(new RefinementLevel(dggrs, level-1));
    }

    @Override
    public Collection<? extends LocationType> getChildren() {
        final int maxLevel = dggrs.getGridSystem().getHierarchy().getGrids().size();
        if (level >= maxLevel) return Collections.EMPTY_LIST;
        return List.of(new RefinementLevel(dggrs, level+1));
    }

}
