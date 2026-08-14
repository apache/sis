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
package org.apache.sis.storage.dggs;

import com.google.common.geometry.S2Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.stream.Stream;
import javax.measure.IncommensurableException;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.internal.shared.GeodeticObjectBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.dggs.internal.shared.ComputedZoneIndexList;
import org.apache.sis.storage.dggs.internal.shared.FeatureSetAsDiscreteGlobalGridResource;
import org.apache.sis.storage.dggs.internal.shared.GridAsDiscreteGlobalGridResource;
import org.apache.sis.storage.dggs.internal.shared.MemoryDiscreteGlobalGridResource;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedResource;
import org.apache.sis.storage.rs.internal.shared.CodedCoverageAsFeatureSet;
import org.apache.sis.storage.rs.internal.shared.s2.Factory;
import org.apache.sis.storage.rs.internal.shared.s2.S2;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.BoundingPolygon;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Utility methods for DGGRS.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DiscreteGlobalGridSystems {

    private static final GeometryFactory GF = new GeometryFactory();

    public static List<Object> createComputedList(long start, long step, int count, LongFunction<Object> toId) {
        return new ComputedZoneIndexList(start, step, count, toId);
    }

    /**
     * Compute the full ellipsoid surface in m².
     *
     * @todo current version use a sphere formula
     * @param gcrs to extract ellipsoid from
     * @return ellipsoid surface
     */
    public static double computeSurface(GeographicCRS gcrs) {
        final Ellipsoid ellipsoid = DatumOrEnsemble.asDatum(gcrs).getEllipsoid();
        final double semiMajorAxis = ellipsoid.getSemiMajorAxis();
        final double semiMinorAxis = ellipsoid.getSemiMinorAxis();
        final double r = (semiMajorAxis + semiMinorAxis) / 2;
        final double surfaceArea = 4.0 * Math.PI * r * r;
        return surfaceArea;
    }

    /**
     * Create a local orthographic projection at given longitude and latitude.
     *
     * @todo move this somewhere else.
     */
    public static ProjectedCRS createOrthographicCRS(final GeographicCRS baseCRS,
                final double latitude, final double longitude) throws FactoryException
        {
            return newBuilder("ORTHOGRAPHIC", latitude, longitude)
                    .setConversionMethod("Orthographic")
                    .setParameter("Latitude of natural origin",  latitude,  Units.DEGREE)
                    .setParameter("Longitude of natural origin", longitude, Units.DEGREE)
                    .createProjectedCRS(baseCRS, null);
        }

    /**
     * Creates a new builder initialized to the projection name for the given coordinates.
     */
    private static final GeodeticObjectBuilder newBuilder(String name, final double latitude, final double longitude) {
        final AngleFormat  f = new AngleFormat("DD°MM′SS″");
        final StringBuffer b = new StringBuffer();
        b.append(name).append(" @ ");
        f.format(new Latitude (latitude),  b, null).append(' ');
        f.format(new Longitude(longitude), b, null);
        return new GeodeticObjectBuilder().addName(b.toString());
    }

    /**
     * View given grid coverage as a DiscreteGlobalGridResource.
     */
    public static DiscreteGlobalGridResource viewAsDggrs(GridCoverageResource base, DiscreteGlobalGridReferenceSystem dggrs) throws DataStoreException, IncommensurableException, TransformException {
        return new GridAsDiscreteGlobalGridResource(dggrs, base);
    }

    /**
     * View given FeatureSet as a DiscreteGlobalGridResource.
     */
    public static CodedResource viewAsDggrs(FeatureSet base, DiscreteGlobalGridReferenceSystem dggrs, DiscreteGlobalGridCoverageProcessor processor) throws DataStoreException, IncommensurableException {
        return new FeatureSetAsDiscreteGlobalGridResource(dggrs, base, processor);
    }

    /**
     * View given DiscreteGlobalGridCoverage as a DiscreteGlobalGridResource.
     */
    public static DiscreteGlobalGridResource viewAsResource(CodedCoverage coverage) {
        return new MemoryDiscreteGlobalGridResource(coverage);
    }

    /**
     * View given DiscreteGlobalGridCoverage as a FeatureSet.
     */
    public static FeatureSet viewAsFeatureSet(CodedCoverage coverage, boolean idAsLong, String geometryType) {
        return new CodedCoverageAsFeatureSet(coverage, idAsLong, geometryType);
    }

    /**
     * Find the first smallest single cell on it's level which intersects the given envelope.
     * Result may contain more then one zone in case the data overlaps several root zones.
     *
     */
    public static List<Zone> firstIntersect(DiscreteGlobalGridReferenceSystem dggrs, Envelope env) throws TransformException {
        final GeneralEnvelope genv = new GeneralEnvelope(Envelopes.transform(env, dggrs.getGridSystem().getCrs()));

        try (Stream<Zone> stream = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones()) {
            final S2Polygon polygon = S2.toS2Polygon(genv);
            return search(stream.toList(), polygon);
        }
    }

    private static List<Zone> search(Collection<? extends Zone> zones, S2Polygon env) {

        final List<Zone> candidates = new ArrayList<>(1);
        for (Zone z : zones) {
            S2Polygon geometry = DiscreteGlobalGridSystems.toS2Polygon(z.getGeographicExtent());
            if (geometry == null || env.intersects(geometry)) {
                candidates.add(z);
            }
        }

        if (candidates.size() == 1) {
            final List<Zone> subZones = search(candidates.get(0).getChildren(), env);
            if (subZones.size() == 1) {
                return subZones;
            } else {
                //return this zone
                return candidates;
            }
        } else {
            return candidates;
        }
    }

    /**
     * Compact the given list of zones.
     * A parent zone is added in the list when all it's children are present, those children will be removed.
     * Returned zones are sorted.
     *
     * @param zones to compact, expected to all be at the same level
     * @param minLevel minimum parent level to compact zone
     * @return sorted list of compact zones
     */
    public static List<Zone> compact(List<Zone> zones, int minLevel) {
        if (zones.size() <= 1) return zones;

        if (zones.get(0).getLocationType().getRefinementLevel() <= minLevel) {
            return zones;
        }

        final Set<Zone> allChildren = new HashSet(zones);
        final Set<Zone> candidateParents = new HashSet<>();

        for (Zone z : zones) {
            candidateParents.addAll(z.getParents());
        }

        //we keep the children to remove in a separate colleciton because they can be referenced by multiple parents
        final Set<Zone> childrenToRemove = new HashSet<>();
        final List<Zone> fullParents = new ArrayList<>();
        for (Zone parent : candidateParents) {
            Collection<? extends Zone> children = parent.getChildren();
            if (allChildren.containsAll(children)) {
                fullParents.add(parent);
                childrenToRemove.addAll(children);
            }
        }

        //remove the child zone if and only if all parents are in the list
        final Iterator<Zone> iterator = childrenToRemove.iterator();
        while (iterator.hasNext()) {
            final Zone zone = iterator.next();
            if (!fullParents.containsAll(zone.getParents())) {
                //we don't have all parents for the zone, we must keep it
                iterator.remove();
            }
        }


        //try to compact parents further
        final List<Zone> compacted = compact(fullParents, minLevel);
        //add leftover children
        allChildren.removeAll(childrenToRemove);
        compacted.addAll(allChildren);

        Collections.sort(compacted);

        return compacted;
    }

    public static Polygon toJTSPolygon(GeographicExtent extent) {
        return S2.toJTSPolygon(toS2Polygon(extent));
    }

    public static org.apache.sis.geometries.Polygon toSISPolygon(GeographicExtent extent) {
        return toSISPolygon(toS2Polygon(extent));

    }

    public static org.apache.sis.geometries.Polygon toSISPolygon(S2Polygon s2) {
        if (s2 == null) return null;
        final double[] coords = S2.toArray(s2.loop(0));
        final Array positions = NDArrays.of(CommonCRS.WGS84.normalizedGeographic(), coords);
        final PointSequence sequence = new ArraySequence(positions);
        final LinearRing exterior = org.apache.sis.geometries.GeometryFactory.createLinearRing(sequence);
        return org.apache.sis.geometries.GeometryFactory.createPolygon(exterior, null);
    }

    public static S2Polygon toS2Polygon(GeographicExtent extent) {
        if (extent == null) {
            return null;
        } else if (extent instanceof BoundingPolygon bp) {
            Collection<? extends org.opengis.geometry.Geometry> polygons = bp.getPolygons();
            if (polygons.size() != 1) {
                throw new UnsupportedOperationException("Only single geometry BoundingPolygon types supported.");
            }
            org.opengis.geometry.Geometry geometry = polygons.iterator().next();
            if (!(geometry instanceof GeometryWrapper gw)) {
                throw new UnsupportedOperationException("Only GeometryWrapper geometry type supported");
            }
            Object geom = Factory.INSTANCE.getGeometry(gw);
            if (geom instanceof S2Polygon p) {
                return p;
            } else {
                throw new UnsupportedOperationException("Only S2 geometry types supported.");
            }
        } else {
            throw new UnsupportedOperationException("Only BoundingPolygon types supported.");
        }
    }

    /**
     * Search zones which intersect the requested polygon.
     *
     * @param rootZones to start searching in
     * @param level wanted zones level
     * @param geometry to search
     * @return stream of zones at the requested level which intersect the polygon
     */
    public static Stream<Zone> spatialSearch(Collection< ? extends Zone> rootZones, int level, S2Polygon geometry) {

        //we need to remove duplicates because some children may be in multiple parents
        final Set<Object> visited = Collections.synchronizedSet(new HashSet<>());
        Stream<Zone> stream = rootZones.stream().flatMap((z) -> search(z, geometry, level, visited));
        return stream;
    }

    private static Stream<Zone> search(Zone zone, S2Polygon geometry, int level, Set<Object> visited) {
        final Object zid = zone.getIdentifier();
        if (visited.contains(zid)) return Stream.empty();

        //TODO comute subdivision
        final S2Polygon zgeom = DiscreteGlobalGridSystems.toS2Polygon(zone.getGeographicExtent(10));
        if (zgeom == null || geometry == null || geometry.intersects(zgeom)) {
            if (zone.getLocationType().getRefinementLevel() == level) {
                if (visited.add(zid)) {
                    //zone might have been visited earlier
                    return Stream.of(zone);
                } else {
                    return Stream.empty();
                }
            } else {
                //search children
                return zone.getChildren().stream().flatMap((c) -> search(c,geometry,level, visited));
            }
        }
        return Stream.empty();
    }
}
