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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.measure.IncommensurableException;
import com.google.common.geometry.S2Polygon;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.apache.sis.storage.dggs.internal.shared.GridAsDiscreteGlobalGridResource;
import org.apache.sis.storage.rs.internal.shared.CodeTransforms;
import org.apache.sis.storage.rs.internal.shared.s2.S2;
import org.apache.sis.util.Utilities;


/**
 * Creates a new coded geometry derived from a base coded geometry with a different extent.
 * {@code CodedDerivation} are created by calls to {@link CodedGeometry#derive()}.
 * Properties of the desired coded geometry can be specified by calls to the following methods,
 * in that order (each method is optional):
 *
 * <ol>
 *   <li>{@link #rounding(GridRoundingMode)}</li>
 *   <li>{@link #subgrid(CodedGeometry)} and/or {@link #subgrid(Envelope)}</li>
 * </ol>
 *
 * Then the coded geometry is created by a call to {@link #build()}.
 * The result is aligned with the base geometry : it uses the base reference system components
 * and only contains cells of the base geometry.
 *
 * <p>The derivation is applied on each single component of the base reference system:</p>
 * <ul>
 *   <li>Discrete Global Grid components keep the zones which intersect the area of interest.</li>
 *   <li>Regular grid components are derived using {@link GridGeometry#derive()}.</li>
 *   <li>Undefined components (without extent or zones), which means any cell is accepted,
 *       are replaced by the area of interest, converted to the base Discrete Global Grid if needed.</li>
 *   <li>Other components, or components without a matching area of interest, are unchanged.</li>
 * </ul>
 * The number of dimensions is preserved, area of interest components not in the base are ignored.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see CodedGeometry#derive()
 * @see org.apache.sis.coverage.grid.GridDerivation
 */
public class CodedDerivation {

    /**
     * The base coded geometry from which to derive a new coded geometry.
     */
    protected final CodedGeometry base;

    /**
     * Single components of the base reference system, empty if the base has no reference system.
     */
    private final List<ReferenceSystem> components;

    /**
     * Base geometry of each component.
     */
    private final CodedGeometry[] baseSlices;

    /**
     * Current derived geometry of each component.
     */
    private final CodedGeometry[] slices;

    /**
     * If the base has no reference system, the area of interest to use as result.
     */
    private CodedGeometry replacement;

    /**
     * Controls behavior of rounding from floating point values to integers on regular grid components.
     */
    private GridRoundingMode rounding = GridRoundingMode.NEAREST;

    /**
     * Whether a {@code subgrid(…)} method has been invoked.
     */
    private boolean subgridSet;

    /**
     * Creates a new builder for deriving a coded geometry from the specified base.
     *
     * @param  base  the base to use as a template for deriving a new coded geometry.
     *
     * @see CodedGeometry#derive()
     */
    protected CodedDerivation(final CodedGeometry base) {
        this.base = Objects.requireNonNull(base);
        final ReferenceSystem rs = base.getReferenceSystem();
        components = (rs == null) ? List.of() : ReferenceSystems.getSingleComponents(rs, true);
        baseSlices = new CodedGeometry[components.size()];
        for (int i = 0; i < baseSlices.length; i++) {
            baseSlices[i] = base.slice(components.get(i)).get();
        }
        slices = baseSlices.clone();
    }

    /**
     * Controls behavior of rounding from floating point values to integers on regular grid components.
     * If this method is never invoked, the default value is {@link GridRoundingMode#NEAREST}.
     *
     * @param  mode  the new rounding mode.
     * @return {@code this} for method call chaining.
     * @throws IllegalStateException if a {@code subgrid(…)} method has already been invoked.
     *
     * @see org.apache.sis.coverage.grid.GridDerivation#rounding(GridRoundingMode)
     */
    public CodedDerivation rounding(final GridRoundingMode mode) {
        if (subgridSet) {
            throw new IllegalStateException("Rounding must be set before subgrid");
        }
        rounding = Objects.requireNonNull(mode);
        return this;
    }

    /**
     * Adapts the base coded geometry for containing only the cells which intersect the given area of interest.
     * Each component of the area of interest is matched to the base component which can be converted to it.
     * Base components without a matching area of interest component are unchanged.
     *
     * <p>When the base and area of interest components use the same Discrete Global Grid,
     * the zone hierarchy is used : a base zone is kept if it is, contains or is contained
     * by a zone of the area of interest.</p>
     *
     * @param  areaOfInterest  the area of interest.
     * @return {@code this} for method call chaining.
     * @throws DisjointExtentException if the area of interest does not intersect the base geometry.
     * @throws IllegalGridGeometryException if the derivation can not be computed.
     */
    public CodedDerivation subgrid(final CodedGeometry areaOfInterest) {
        Objects.requireNonNull(areaOfInterest);
        subgridSet = true;
        if (components.isEmpty()) {
            //base is undefined, any geometry is accepted
            replacement = areaOfInterest;
            return this;
        }
        final List<ReferenceSystem> aoiComponents = ReferenceSystems.getSingleComponents(areaOfInterest.getReferenceSystem(), true);
        try {
            for (int i = 0; i < slices.length; i++) {
                final ReferenceSystem component = components.get(i);
                final CoordinateReferenceSystem target = leaningCRS(component);
                for (ReferenceSystem aoiComponent : aoiComponents) {
                    if (!isConvertible(leaningCRS(aoiComponent), target)) continue;
                    final CodedGeometry aoi = areaOfInterest.slice(aoiComponent).get();
                    if (component instanceof DiscreteGlobalGridReferenceSystem) {
                        slices[i] = subgridZones(asDggs(slices[i]), aoi);
                    } else {
                        slices[i] = subgridCells(component, slices[i], aoi);
                    }
                    break;
                }
            }
        } catch (TransformException | IncommensurableException ex) {
            throw new IllegalGridGeometryException(ex.getMessage(), ex);
        }
        return this;
    }

    /**
     * Adapts the base coded geometry for containing only the cells which intersect the given envelope.
     * Each component of the envelope CRS is matched to the base component which can be converted to it.
     * Base components without a matching envelope component, or undefined, are unchanged.
     * If the envelope has no CRS, it is assumed to be in the
     * {@linkplain ReferenceSystems#getLeaningCRS(ReferenceSystem) leaning CRS} of the base geometry.
     *
     * @param  areaOfInterest  the area of interest.
     * @return {@code this} for method call chaining.
     * @throws DisjointExtentException if the area of interest does not intersect the base geometry.
     * @throws IllegalGridGeometryException if the derivation can not be computed.
     */
    public CodedDerivation subgrid(Envelope areaOfInterest) {
        Objects.requireNonNull(areaOfInterest);
        subgridSet = true;
        if (components.isEmpty()) return this;
        try {
            if (areaOfInterest.getCoordinateReferenceSystem() == null) {
                final GeneralEnvelope env = new GeneralEnvelope(areaOfInterest);
                env.setCoordinateReferenceSystem(ReferenceSystems.getLeaningCRS(base.getReferenceSystem()));
                areaOfInterest = env;
            }
            for (int i = 0; i < slices.length; i++) {
                final ReferenceSystem component = components.get(i);
                if (component instanceof DiscreteGlobalGridReferenceSystem) {
                    slices[i] = subgrid(asDggs(slices[i]), areaOfInterest);
                } else if (component instanceof CoordinateReferenceSystem crs) {
                    final Optional<GridGeometry> grid = slices[i].isRegularGrid().filter(CodedDerivation::isComplete);
                    if (grid.isPresent()) {
                        final Envelope env = subEnvelope(areaOfInterest, crs);
                        if (env != null) {
                            slices[i] = toGeometry(component, grid.get().derive().rounding(rounding).subgrid(env).build());
                        }
                    }
                }
            }
        } catch (TransformException | FactoryException ex) {
            throw new IllegalGridGeometryException(ex.getMessage(), ex);
        }
        return this;
    }

    /**
     * Builds a coded geometry with the configurations specified by the other methods in this class.
     *
     * @return the modified coded geometry. May be the {@link #base} geometry if no change apply.
     */
    public CodedGeometry build() {
        if (replacement != null) return replacement;
        for (int i = 0; i < slices.length; i++) {
            if (slices[i] != baseSlices[i]) {
                return CodedGeometry.compound(slices);
            }
        }
        return base;
    }

    /**
     * Derives a DGGRS component using a coded area of interest.
     */
    private static CodedGeometry subgridZones(DiscreteGlobalGridGeometry slice, CodedGeometry aoi)
            throws TransformException, IncommensurableException {
        final DiscreteGlobalGridReferenceSystem dggrs = slice.getReferenceSystem();
        final List<Object> zoneIds = slice.getZoneIds();

        if (aoi.getReferenceSystem() instanceof DiscreteGlobalGridReferenceSystem) {
            final DiscreteGlobalGridGeometry dggsAoi = asDggs(aoi);
            final List<Object> aoiIds = dggsAoi.getZoneIds();
            if (aoiIds == null) return slice;
            if (aoiIds.isEmpty()) throw new DisjointExtentException("Area of interest has no zones");
            final boolean sameDggrs = dggrs.equals(dggsAoi.getReferenceSystem());
            if (zoneIds == null) {
                //base accepts any zone, use the area of interest zones
                return sameDggrs ? dggsAoi : dggsAoi.transformTo(dggrs, null, null);
            }
            return sameDggrs ? subgrid(slice, dggsAoi) : subgrid(slice, dggsAoi.getEnvelope());
        }

        final Optional<GridGeometry> grid = aoi.isRegularGrid().filter(CodedDerivation::isComplete);
        if (grid.isEmpty()) return slice;
        if (zoneIds == null) {
            //base accepts any zone, use the zones matching the grid resolution
            return toZones(dggrs, grid.get());
        }
        return subgrid(slice, grid.get().getEnvelope());
    }

    /**
     * Derives a non DGGRS component using a coded area of interest.
     */
    private CodedGeometry subgridCells(ReferenceSystem component, CodedGeometry slice, CodedGeometry aoi) {
        final Optional<GridGeometry> grid = slice.isRegularGrid().filter(CodedDerivation::isComplete);
        if (grid.isPresent()) {
            final GridDerivation derivation = grid.get().derive().rounding(rounding);
            final Optional<GridGeometry> aoiGrid = aoi.isRegularGrid().filter(CodedDerivation::isComplete);
            if (aoiGrid.isPresent()) {
                return toGeometry(component, derivation.subgrid(aoiGrid.get()).build());
            }
            final Envelope env = envelopeOf(aoi);
            return (env != null) ? toGeometry(component, derivation.subgrid(env).build()) : slice;
        }
        if (!slice.isDefined(CodedGeometry.EXTENT | CodedGeometry.GRID_TO_RS)
                && Utilities.equalsIgnoreMetadata(component, aoi.getReferenceSystem())) {
            //base accepts any cell, use the area of interest
            return new CodedGeometry(component, aoi.getExtent(), aoi.getGridToRS(), null);
        }
        return slice;
    }

    /**
     * Keeps the zones of the given slice which intersect the envelope.
     */
    private static CodedGeometry subgrid(DiscreteGlobalGridGeometry slice, Envelope areaOfInterest) throws TransformException {
        final List<Object> zoneIds = slice.getZoneIds();
        if (zoneIds == null || zoneIds.isEmpty()) return slice;
        final Envelope env = subEnvelope(areaOfInterest, CommonCRS.WGS84.normalizedGeographic());
        if (env == null) return slice;

        final DiscreteGlobalGridHierarchy hierarchy = slice.getReferenceSystem().getGridSystem().getHierarchy();
        final List<Object> kept = new ArrayList<>();
        if (env.getSpan(0) == 0 && env.getSpan(1) == 0) {
            //envelope degenerates to a point, keep the zone which contains it
            final DirectPosition2D dp = new DirectPosition2D(env.getCoordinateReferenceSystem(), env.getMinimum(0), env.getMinimum(1));
            final long zid = hierarchy.getGrids().get(slice.getRefinementLevel()).getZone(dp).getLongIdentifier();
            for (Object zoneId : zoneIds) {
                if (hierarchy.getZone(zoneId).getLongIdentifier() == zid) kept.add(zoneId);
            }
        } else {
            final S2Polygon polygon = S2.toS2Polygon(env);
            for (Object zoneId : zoneIds) {
                final S2Polygon geometry = DiscreteGlobalGridSystems.toS2Polygon(hierarchy.getZone(zoneId).getGeographicExtent());
                if (geometry == null || polygon.intersects(geometry)) kept.add(zoneId);
            }
        }
        return toGeometry(slice, kept);
    }

    /**
     * Keeps the zones of the given slice which are, contain or are contained by a zone of the area of interest.
     * Both geometries must use the same DGGRS.
     */
    private static CodedGeometry subgrid(DiscreteGlobalGridGeometry slice, DiscreteGlobalGridGeometry areaOfInterest) {
        final List<Object> zoneIds = slice.getZoneIds();
        if (zoneIds.isEmpty()) return slice;

        final DiscreteGlobalGridHierarchy hierarchy = slice.getReferenceSystem().getGridSystem().getHierarchy();
        final int level = slice.getRefinementLevel();
        final int aoiLevel = areaOfInterest.getRefinementLevel();

        //compare zones at the coarsest level of both geometries
        final Set<Long> aoiZones = new HashSet<>();
        for (Object zoneId : areaOfInterest.getZoneIds()) {
            Zone zone = hierarchy.getZone(zoneId);
            if (aoiLevel > level) zone = zone.getFirstParent(level);
            aoiZones.add(zone.getLongIdentifier());
        }
        final List<Object> kept = new ArrayList<>();
        for (Object zoneId : zoneIds) {
            Zone zone = hierarchy.getZone(zoneId);
            if (level > aoiLevel) zone = zone.getFirstParent(aoiLevel);
            if (aoiZones.contains(zone.getLongIdentifier())) kept.add(zoneId);
        }
        return toGeometry(slice, kept);
    }

    /**
     * Returns the zones intersecting the given grid, at the level matching the grid resolution.
     */
    private static CodedGeometry toZones(DiscreteGlobalGridReferenceSystem dggrs, GridGeometry grid)
            throws TransformException, IncommensurableException {
        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
        coder.setPrecision(GridAsDiscreteGlobalGridResource.computeAverageResolution(grid), null);
        final DiscreteGlobalGrid dgg = dggrs.getGridSystem().getHierarchy().getGrids().get(coder.getPrecisionLevel());
        final List<Object> zoneIds;
        try (Stream<Zone> zones = dgg.getZones(grid.getEnvelope(dggrs.getGridSystem().getCrs()))) {
            zoneIds = zones.map(Zone::getIdentifier).toList();
        }
        if (zoneIds.isEmpty()) throw new DisjointExtentException("Area of interest does not intersect any zone");
        return DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds, null);
    }

    private static CodedGeometry toGeometry(DiscreteGlobalGridGeometry slice, List<Object> kept) {
        if (kept.isEmpty()) throw new DisjointExtentException("Area of interest does not intersect any zone");
        if (kept.size() == slice.getZoneIds().size()) return slice;
        return DiscreteGlobalGridGeometry.unstructured(slice.getReferenceSystem(), kept, null);
    }

    private static CodedGeometry toGeometry(ReferenceSystem component, GridGeometry grid) {
        return new CodedGeometry(component, grid.getExtent(), CodeTransforms.toTransform(grid), null);
    }

    /**
     * Returns the given DGGRS slice as a DiscreteGlobalGridGeometry.
     */
    private static DiscreteGlobalGridGeometry asDggs(CodedGeometry slice) {
        if (slice instanceof DiscreteGlobalGridGeometry dggs) return dggs;
        return DiscreteGlobalGridGeometry.unstructured((DiscreteGlobalGridReferenceSystem) slice.getReferenceSystem(),
                slice.getExtent(), slice.getGridToRS(), null);
    }

    /**
     * Returns true if the grid geometry has an extent and a grid to CRS transform.
     */
    private static boolean isComplete(GridGeometry grid) {
        return grid.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS);
    }

    /**
     * Returns the envelope of the given area of interest slice, or null if it can not be computed.
     */
    private static Envelope envelopeOf(CodedGeometry aoi) {
        if (aoi instanceof DiscreteGlobalGridGeometry dggs) {
            final List<Object> zoneIds = dggs.getZoneIds();
            return (zoneIds == null || zoneIds.isEmpty()) ? null : dggs.getEnvelope();
        }
        return aoi.isRegularGrid().filter((g) -> g.isDefined(GridGeometry.ENVELOPE)).map(GridGeometry::getEnvelope).orElse(null);
    }

    /**
     * Extracts from the area of interest the part which can be converted to the given CRS.
     *
     * @return envelope in target CRS, or null if the area of interest has no matching component.
     */
    private static Envelope subEnvelope(Envelope areaOfInterest, CoordinateReferenceSystem target) throws TransformException {
        final CoordinateReferenceSystem crs = areaOfInterest.getCoordinateReferenceSystem();
        int offset = 0;
        for (ReferenceSystem component : ReferenceSystems.getSingleComponents(crs, true)) {
            final CoordinateReferenceSystem ccrs = (CoordinateReferenceSystem) component;
            final int dim = ccrs.getCoordinateSystem().getDimension();
            if (isConvertible(ccrs, target)) {
                final GeneralEnvelope sub = GeneralEnvelope.castOrCopy(areaOfInterest).subEnvelope(offset, offset + dim).clone();
                sub.setCoordinateReferenceSystem(ccrs);
                return Envelopes.transform(sub, target);
            }
            offset += dim;
        }
        return null;
    }

    /**
     * Returns the CRS the given reference system is based on, or null if unknown.
     */
    private static CoordinateReferenceSystem leaningCRS(ReferenceSystem rs) {
        try {
            return ReferenceSystems.getLeaningCRS(rs);
        } catch (FactoryException ex) {
            return null;
        }
    }

    /**
     * Returns true if a coordinate operation exist between the two CRS.
     */
    private static boolean isConvertible(CoordinateReferenceSystem source, CoordinateReferenceSystem target) {
        if (source == null || target == null) return false;
        if (Utilities.equalsIgnoreMetadata(source, target)) return true;
        try {
            CRS.findOperation(source, target, null);
            return true;
        } catch (FactoryException ex) {
            return false;
        }
    }
}
