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
import java.util.Collections;
import java.util.List;
import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polygon;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.BoundingPolygon;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.RefinementLevel;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.internal.shared.AbstractZone;
import org.apache.sis.storage.rs.internal.shared.s2.S2;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class S2Zone extends AbstractZone<S2Dggrs> {

    private static final SampleSystem CRS84 = SampleSystem.of(CommonCRS.WGS84.normalizedGeographic());

    private final long hash;

    public S2Zone(S2Dggrs dggrs, long hash) {
        super(dggrs);
        this.hash = hash;
    }

    @Override
    public Object getIdentifier() {
        return hash;
    }

    @Override
    public long getLongIdentifier() {
        return hash;
    }

    @Override
    public CharSequence getTextIdentifier() {
        return S2Dggh.idAsText(hash);
    }

    @Override
    public String getShapeType() {
        return "square";
    }

    @Override
    public Double getAreaMetersSquare() {
        return new S2Cell(new S2CellId(hash)).exactArea();
    }

    @Override
    public RefinementLevel getLocationType() {
        final int level = new S2CellId(hash).level();
        return new RefinementLevel(dggrs, level);
    }

    @Override
    public Zone getFirstParent() {
        final S2CellId cellId = new S2CellId(hash);
        if (cellId.isFace()) return null;
        return new S2Zone(dggrs, cellId.parent().id());
    }

    @Override
    public Zone getFirstParent(int refinementLevel) {
        final S2CellId cellId = new S2CellId(hash);
        if (cellId.isFace()) return null;
        return new S2Zone(dggrs, cellId.parent(refinementLevel).id());
    }

    @Override
    public Collection<? extends Zone> getParents() {
        final S2CellId cellId = new S2CellId(hash);
        if (cellId.isFace()) return Collections.EMPTY_LIST;
        return List.of(new S2Zone(dggrs, cellId.parent().id()));
    }

    @Override
    public Collection<? extends Zone> getChildren() {
        final S2CellId cellId = new S2CellId(hash);
        if (cellId.isLeaf()) return Collections.EMPTY_LIST;
        return List.of(
                new S2Zone(dggrs, cellId.child(0).id()),
                new S2Zone(dggrs, cellId.child(1).id()),
                new S2Zone(dggrs, cellId.child(2).id()),
                new S2Zone(dggrs, cellId.child(3).id())
        );
    }

    @Override
    public Collection<? extends Zone> getNeighbors() {
        final S2CellId cellId = new S2CellId(hash);
        final S2CellId[] neighbors = new S2CellId[4];
        cellId.getEdgeNeighbors(neighbors);
        return List.of(
                new S2Zone(dggrs, neighbors[0].id()),
                new S2Zone(dggrs, neighbors[1].id()),
                new S2Zone(dggrs, neighbors[2].id()),
                new S2Zone(dggrs, neighbors[3].id())
        );
    }

    @Override
    public BoundingPolygon getGeographicExtent() {
        final S2CellId cellId = new S2CellId(hash);
        final S2Cell cell = new S2Cell(cellId);
        final List<S2Point> contour = List.of(
            cell.getVertex(0),
            cell.getVertex(1),
            cell.getVertex(2),
            cell.getVertex(3));
        return S2.toGeographicExtent(new S2Polygon(new S2Loop(contour)));
    }

    @Override
    public DirectPosition getPosition() {
        final S2CellId cellId = new S2CellId(hash);
        final S2LatLng latLng = cellId.toLatLng();
        return new DirectPosition2D(CRS84.getCoordinateReferenceSystem(), latLng.lngDegrees(), latLng.latDegrees());
    }

    @Override
    public long countChildrenAtRelativeDepth(int depth) {
        return (long)Math.pow(4, depth);
    }

}
