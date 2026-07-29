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

import java.util.Collection;
import java.util.List;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystemFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class S2Factory implements DiscreteGlobalGridReferenceSystemFactory{

    private static final List<String> DGGHS = List.of(
            S2Dggrs.IDENTIFIER
        );

    @Override
    public Collection<String> listDggh() {
        return DGGHS;
    }

    @Override
    public Collection<String> listZonalRefId(String dggh) {
        return List.of("default");
    }

    @Override
    public DiscreteGlobalGridReferenceSystem createDggrs(String dgghId, String zonalRefId, CoordinateReferenceSystem base) throws FactoryException {
        switch (dgghId) {
            case S2Dggrs.IDENTIFIER : return S2Dggrs.INSTANCE;
            default : throw new NoSuchIdentifierException("Unknown identifier " + dgghId, dgghId);
        }
    }

}
