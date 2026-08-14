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
import com.google.common.geometry.S2LatLng;
import javax.measure.IncommensurableException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.util.Utilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class S2Coder extends DiscreteGlobalGridReferenceSystem.Coder{

    private final S2Dggrs dggrs;
    private final CoordinateReferenceSystem baseCrs;
    private int level = 0;


    public S2Coder(S2Dggrs dggrs) {
        this.dggrs = dggrs;
        this.baseCrs = this.dggrs.getGridSystem().getCrs();
    }

    @Override
    public S2Dggrs getReferenceSystem() {
        return dggrs;
    }

    @Override
    public int getPrecisionLevel() {
        return level;
    }

    @Override
    public void setPrecisionLevel(int level) throws IncommensurableException {
        this.level = level;
    }

    @Override
    public String encode(DirectPosition dp) throws TransformException {
        return S2Dggh.idAsText(encodeIdentifier(dp));
    }

    @Override
    public Long encodeIdentifier(DirectPosition dp) throws TransformException {
        final CoordinateReferenceSystem dpcrs = dp.getCoordinateReferenceSystem();
        if (dpcrs != null && !Utilities.equalsIgnoreMetadata(baseCrs, dpcrs)) {
            MathTransform trs;
            try {
                trs = CRS.findOperation(dpcrs, baseCrs, null).getMathTransform();
                dp = trs.transform(dp, null);
            } catch (FactoryException ex) {
                throw new TransformException(ex.getMessage(), ex);
            }
        }
        S2CellId cid = S2CellId.fromLatLng(S2LatLng.fromDegrees(dp.getCoordinate(1), dp.getCoordinate(0)));
        return cid.parent(level).id();
    }

}
