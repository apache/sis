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

import java.util.List;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.GridConstraints;
import org.apache.sis.referencing.dggs.PolyhedronOrientation;
import org.apache.sis.referencing.dggs.RefinementStrategy;
import org.apache.sis.referencing.dggs.PolyhedronParameters;
import org.apache.sis.referencing.dggs.internal.shared.AbstractDiscreteGlobalGridSystem;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class S2Dggs extends AbstractDiscreteGlobalGridSystem {

    final S2Dggh dggh;

    public S2Dggs(S2Dggrs dggrs) {
        super(CommonCRS.WGS84.normalizedGeographic());
        this.dggh = new S2Dggh(dggrs);
    }

    @Override
    public DiscreteGlobalGridHierarchy getHierarchy() {
        return dggh;
    }

    @Override
    public String getBasePolyhedron() {
        return "cube";
    }

    @Override
    public int getRefinementRatio() {
        return 4;
    }

    @Override
    public int getSpatialDimensions() {
        return 2;
    }

    @Override
    public int getTemporalDimensions() {
        return 0;
    }

    @Override
    public List<String> getZoneTypes() {
        return List.of("square");
    }

    @Override
    public PolyhedronParameters getParameters() {
        return new PolyhedronParameters(new PolyhedronOrientation(0, 0, 0, ""));
    }

    @Override
    public List<RefinementStrategy> getRefinementStrategy() {
        return List.of(RefinementStrategy.centredChildCell);
    }

    @Override
    public List<GridConstraints> getGridConstraints() {
        return List.of(GridConstraints.cellAxisAligned, GridConstraints.cellEquiSized);
    }
}
