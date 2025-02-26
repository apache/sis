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
package org.apache.sis.storage.geoheif;

import java.util.Optional;
import java.util.SortedMap;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.apache.sis.storage.isobmff.TreeNode;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class GimiTileMatrixSet implements TileMatrixSet {

    private final GenericName identifier;
    private final CoordinateReferenceSystem crs;
    final ScaleSortedMap<TileMatrix> matrices = new ScaleSortedMap<>();

    public GimiTileMatrixSet(GenericName identifier, CoordinateReferenceSystem crs) {
        this.identifier = identifier;
        this.crs = crs;
    }

    @Override
    public GenericName getIdentifier() {
        return identifier;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    @Override
    public Optional<Envelope> getEnvelope() {
        if (matrices.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matrices.lastEntry().getValue().getTilingScheme().getEnvelope());
    }

    @Override
    public SortedMap<GenericName, ? extends TileMatrix> getTileMatrices() {
        return matrices;
    }

    @Override
    public String toString() {
        return TreeNode.toStringTree(identifier.toString(), matrices.values());
    }

}
