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
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Polygon;
import com.google.common.geometry.S2RegionCoverer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.internal.shared.AbstractDiscreteGlobalGrid;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class S2Dgg extends AbstractDiscreteGlobalGrid<S2Dggh> {

    private final List<Zone> roots;

    public S2Dgg(S2Dggh dggh, int level) {
        super(dggh, level);

        if (level == 0) {
            roots = List.of(
                new S2Zone(hierarchy.dggrs, S2CellId.fromFace(0).id()),
                new S2Zone(hierarchy.dggrs, S2CellId.fromFace(1).id()),
                new S2Zone(hierarchy.dggrs, S2CellId.fromFace(2).id()),
                new S2Zone(hierarchy.dggrs, S2CellId.fromFace(3).id()),
                new S2Zone(hierarchy.dggrs, S2CellId.fromFace(4).id()),
                new S2Zone(hierarchy.dggrs, S2CellId.fromFace(5).id()));
        } else {
            roots = null;
        }
    }

    @Override
    public Zone getZone(DirectPosition dp) throws TransformException {
        final CoordinateReferenceSystem baseCrs = hierarchy.dggrs.dggs.getCrs();
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
        final long zid = cid.parent(level).id();
        return new S2Zone(hierarchy.dggrs, zid);
    }

    @Override
    public long getZoneCount() {
        if(level == 0) return 6;
        return 6l * (long)Math.pow(4, level);
    }

    @Override
    public Stream<Zone> getZones(GeographicExtent extent) throws TransformException {
        if (extent == null && level == 0) {
            return roots.stream();
        }

        final S2Polygon geometry = DiscreteGlobalGridSystems.toS2Polygon(extent);
        if (geometry == null) {
            //search from root
            try (Stream<Zone> zones = hierarchy.getGrids().get(0).getZones()) {
                return DiscreteGlobalGridSystems.spatialSearch(zones.toList(), level, geometry);
            }

        } else {
            final S2RegionCoverer coverer = S2RegionCoverer.builder().setMinLevel(level).setMaxLevel(level).build();
            final S2CellUnion covering = coverer.getCovering(geometry);

            return covering.cellIds().stream().flatMap(new Function<S2CellId, Stream<S2CellId>>() {
                @Override
                public Stream<S2CellId> apply(S2CellId t) {
                    //enforce requested level, S2 may return upper levels
                    if (t.level() == level) return Stream.of(t);
                    return StreamSupport.stream(t.childrenAtLevel(level).spliterator(), false);
                }
            }).mapToLong(S2CellId::id).mapToObj((id) -> new S2Zone(hierarchy.dggrs, id));
        }
    }

    @Override
    public Stream<Zone> getZones(Zone parent) throws TransformException {
        final int parentDepth = parent.getLocationType().getRefinementLevel();
        if (parent.getLocationType().getRefinementLevel() > level) {
            throw new IllegalArgumentException("Parent zone is at a lower level then this grid");
        }
        return parent.getChildrenAtRelativeDepth(level-parentDepth);
    }

    @Override
    protected long getZoneLongIdentifier(double[] source, int soffset) {
        S2CellId cid = S2CellId.fromLatLng(S2LatLng.fromDegrees(source[soffset+1], source[soffset]));
        return cid.parent(level).id();
    }

    @Override
    protected void getZonePosition(long zoneId, double[] target, int toffset) {
        final S2CellId cellId = new S2CellId(zoneId);
        final S2LatLng latLng = cellId.toLatLng();
        target[toffset] = latLng.lngDegrees();
        target[toffset+1] = latLng.latDegrees();
    }
}
