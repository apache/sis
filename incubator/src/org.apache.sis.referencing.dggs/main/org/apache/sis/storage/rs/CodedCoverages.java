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
package org.apache.sis.storage.rs;

import javax.measure.IncommensurableException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.dggs.internal.shared.GridAsDiscreteGlobalGridResource;
import org.apache.sis.storage.rs.internal.shared.CodedCoverageAsFeatureSet;
import org.apache.sis.storage.rs.internal.shared.GridAsCodedResource;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CodedCoverages {

    private CodedCoverages(){}

    /**
     * View given grid coverage as a DiscreteGlobalGridResource.
     */
    public static CodedResource viewAsDggrs(GenericName name, GridCoverageResource base, DiscreteGlobalGridReferenceSystem dggrs) throws DataStoreException, IncommensurableException, TransformException, FactoryException {
        final CoordinateReferenceSystem baseCrs = base.getGridGeometry().getCoordinateReferenceSystem();
        if (CRS.isHorizontalCRS(baseCrs)) {
            //use a more efficient implementation
            return new GridAsDiscreteGlobalGridResource(dggrs, base);
        }
        return new GridAsCodedResource(name, dggrs, base);
    }

    /**
     * View given ReferencedGridCoverage as a FeatureSet.
     */
    public static FeatureSet viewAsFeatureSet(CodedCoverage coverage, boolean idAsLong, String geometryType) {
        return new CodedCoverageAsFeatureSet(coverage, idAsLong, geometryType);
    }

}
