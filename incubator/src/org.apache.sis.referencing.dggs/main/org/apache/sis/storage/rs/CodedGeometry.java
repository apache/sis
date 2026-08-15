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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.internal.shared.CodeTransforms;
import org.apache.sis.util.ArraysExt;


/**
 * Coded grid geometry.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CodedGeometry {

    /**
     * A bitmask to specify the validity of the Reference System property.
     *
     * @see #isDefined(int)
     * @see #getReferenceSystem()
     */
    public static final int RS = 1;

    /**
     * A bitmask to specify the validity of the grid extent property.
     *
     * @see #isDefined(int)
     * @see #getExtent()
     */
    public static final int EXTENT = 2;

    /**
     * A bitmask to specify the validity of the <q>grid to RS</q> transform.
     *
     * @see #isDefined(int)
     * @see #getGridToRS(PixelInCell)
     */
    public static final int GRID_TO_RS = 4;

    /**
     * A bitmask to specify the validity of the geographic bounding box.
     *
     * @see #getGeographicExtent()
     */
    public static final int GEOGRAPHIC_EXTENT = 8;


    private final ReferenceSystem rs;
    private final GeographicExtent geoExtent;
    private final GridExtent extent;
    private final CodeTransform gridToRS;

    public CodedGeometry(GridGeometry grid) {
        this(grid.isDefined(GridGeometry.CRS) ? grid.getCoordinateReferenceSystem() : null,
             grid.isDefined(GridGeometry.EXTENT) ? grid.getExtent() : null,
             grid.isDefined(GridGeometry.GRID_TO_CRS) ? CodeTransforms.toTransform(grid) : null,
             null);
    }

    public CodedGeometry(ReferenceSystem rs, GridExtent extent, CodeTransform gridToRS, GeographicExtent geoExtent) {
        this.rs = rs;
        this.extent = extent;
        this.gridToRS = gridToRS;
        this.geoExtent = geoExtent;

        final int nbDim = (rs != null) ? ReferenceSystems.getDimension(rs) : ((extent != null) ? extent.getDimension() : 0);
        if (extent != null && extent.getDimension() != nbDim) {
            throw new IllegalArgumentException("Extent does not have the same number of dimension as the reference system, expected " + nbDim);
        }
        if (gridToRS != null && gridToRS.getDimension() != nbDim) {
            throw new IllegalArgumentException("Transform does not have the same number of dimension as the reference system, expected " + nbDim);
        }
    }

    /**
     * Returns the ReferenceSystem.
     *
     * @return ReferenceSystem, never null
     */
    public ReferenceSystem getReferenceSystem() {
        return rs;
    }

    public GridExtent getExtent() {
        return extent;
    }

    public CodeTransform getGridToRS() {
        return gridToRS;
    }

    /**
     * Returns an <em>estimation</em> of the grid resolution, in units of the reference system axes.
     * The length of the returned array is the number of RS dimensions, with {@code resolution[0]}
     * being the resolution along the first RS, {@code resolution[1]} the resolution along the second RS,
     * <i>etc</i>. Note that this order is not necessarily the same as grid axis order.
     *
     * <p>If the resolution at RS dimension <var>i</var> is not a constant factor
     * then {@code resolution[i]} is set to one of the following values:</p>
     *
     * <ul>
     *   <li>{@link Double#NaN} if {@code allowEstimates} is {@code false}.</li>
     *   <li>An arbitrary representative resolution otherwise.</li>
     * </ul>
     *
     * @param allowEstimates whether to provide some values even for resolutions that are not constant factors.
     * @return an <em>estimation</em> of the grid resolution or null
     */
    public double[] getResolution(final boolean allowEstimates) {
        if (rs == null || extent == null || gridToRS == null) return null;

        final List<ReferenceSystem> singles = ReferenceSystems.getSingleComponents(rs, true);
        if (singles.size() == 1) {
            if (gridToRS instanceof CodeTransforms.Grid g) {
                return g.getGrid().getResolution(allowEstimates);
            } else {
                final double[] res = new double[gridToRS.getDimension()];
                Arrays.fill(res, Double.NaN);
                return res;
            }
        } else {
            //slice it to evaluate it
            double[] res = new double[0];
            for (ReferenceSystem rs : singles) {
                final double[] rg = slice(rs).get().getResolution(allowEstimates);
                res = ArraysExt.concatenate(res, rg);
            }
            return res;
        }
    }

    /**
     * Resolution in CRS units.
     */
    public double[] getResolutionProjected(final boolean allowEstimates) {
        if (rs == null || extent == null || gridToRS == null) return null;

        final List<ReferenceSystem> singles = ReferenceSystems.getSingleComponents(rs, true);
        if (singles.size() == 1) {
            if (gridToRS instanceof CodeTransforms.Grid g) {
                return g.getGrid().getResolution(allowEstimates);
            } else {
                final double[] res = new double[gridToRS.getDimension()];
                Arrays.fill(res, Double.NaN);
                return res;
            }
        } else {
            //slice it to evaluate it
            double[] res = new double[0];
            for (ReferenceSystem rs : singles) {
                final double[] rg = slice(rs).get().getResolutionProjected(allowEstimates);
                res = ArraysExt.concatenate(res, rg);
            }
            return res;
        }
    }

    public Envelope getEnvelope() {
        final List<ReferenceSystem> singles = ReferenceSystems.getSingleComponents(rs, true);
        if (singles.size() == 1) {
            if (gridToRS instanceof CodeTransforms.Grid rgg) {
                return rgg.getGrid().getEnvelope();
            } else {
                throw new UnsupportedOperationException("todo");
            }
        } else {
            try {
                Envelope env = null;
                for (ReferenceSystem rs : singles) {
                    Envelope se = slice(rs).get().getEnvelope();
                    env = (env == null) ? se : Envelopes.compound(env, se);
                }
                return env;
            } catch (FactoryException ex) {
                return null;
            }
        }
    }

    public Envelope getEnvelope(CoordinateReferenceSystem crs) throws TransformException {
        final List<ReferenceSystem> singles = ReferenceSystems.getSingleComponents(rs, true);
        if (singles.size() == 1) {
            if (gridToRS instanceof CodeTransforms.Grid rgg) {
                return rgg.getGrid().getEnvelope(crs);
            } else {
                throw new UnsupportedOperationException("todo");
            }
        } else {
            try {
                Envelope env = null;
                for (ReferenceSystem rs : singles) {
                    CodedGeometry sbs = slice(rs).get();
                    try {
                        Envelope se = sbs.getEnvelope(crs);
                        env = (env == null) ? se : Envelopes.compound(env, se);
                    } catch (TransformException ex) {
                        if (ex.getCause() instanceof OperationNotFoundException e) {
                            //not compatible crs, skip it
                        } else {
                            throw ex;
                        }
                    }
                }
                return env;
            } catch (FactoryException ex) {
                return null;
            }
        }
    }

   /**
     * Returns the approximate latitude and longitude coordinates of the grid.
     * The prime meridian is Greenwich, but the geodetic reference frame is not necessarily WGS 84.
     * This is computed from the {@linkplain #getEnvelope() envelope} if the coordinate reference system
     * contains an horizontal component such as a geographic or projected CRS.
     *
     * @return the geographic bounding box in "real world" coordinates.
     */
    public GeographicExtent getGeographicExtent() {
        return geoExtent;
    }

    /**
     * Returns {@code true} if all the properties specified by the argument are set.
     * If this method returns {@code true}, then invoking the corresponding getter
     * methods will not throw {@link IncompleteGridGeometryException}.
     *
     * @param  bitmask  any combination of {@link #RS}, {@link #ENVELOPE}, {@link #EXTENT},
     *         {@link #GRID_TO_RS} and derived bit masks.
     * @return {@code true} if all specified properties are defined (i.e. invoking the
     *         corresponding getter methods will not throw {@link IncompleteGridGeometryException}).
     * @throws IllegalArgumentException if the specified bitmask is not a combination of known masks.
     *
     * @see #getReferenceSystem()
     * @see #getEnvelope()
     * @see #getExtent()
     * @see #getGridToRS()
     */
    public boolean isDefined(final int bitmask) {
        if ((bitmask & ~(RS | EXTENT | GRID_TO_RS | GEOGRAPHIC_EXTENT)) != 0) {
            throw new IllegalArgumentException("Incorrect bitmask values");
        }
        return ((bitmask & RS)               == 0 || (null != getReferenceSystem()))
            && ((bitmask & EXTENT)            == 0 || (null != extent))
            && ((bitmask & GRID_TO_RS)       == 0 || (null != gridToRS))
            && ((bitmask & GEOGRAPHIC_EXTENT) == 0 || (null != getGeographicExtent()));
    }

    public Optional<CodedGeometry> slice(ReferenceSystem rs) {
        if (this.rs.equals(rs)) {
            return Optional.of(this);
        }

        int offset = 0;
        for (ReferenceSystem single : ReferenceSystems.getSingleComponents(this.rs, true)) {
            if (single.equals(rs)) {
                if (rs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                    final int crsDim = ReferenceSystems.getDimension(dggrs);
                    GridExtent ext = null;
                    if (extent != null) {
                        final int[] select = new int[crsDim];
                        for (int i = 0; i < select.length; i++) {
                            select[i] = offset + i;
                        }
                        ext = extent.selectDimensions(select);
                    }
                    CodeTransform subtrs = null;
                    if (gridToRS != null) {
                        subtrs = gridToRS.split(offset, crsDim);
                    }
                    return Optional.of(DiscreteGlobalGridGeometry.unstructured(dggrs, ext, subtrs, geoExtent));

                } else if (rs instanceof CoordinateReferenceSystem crs) {
                    final int crsDim = ReferenceSystems.getDimension(crs);
                    GridExtent ext = null;
                    if (extent != null) {
                        final int[] select = new int[crsDim];
                        for (int i = 0; i < select.length; i++) {
                            select[i] = offset + i;
                        }
                        ext = extent.selectDimensions(select);
                    }
                    CodeTransform subtrs = null;
                    if (gridToRS != null) {
                        subtrs = gridToRS.split(offset, crsDim);
                    }

                    return Optional.of(new CodedGeometry(rs, ext, subtrs, null));
                } else {
                    throw new UnsupportedOperationException("Unexpected reference system " + rs.getClass().getName());
                }
            }
            offset += ReferenceSystems.getDimension(single);
        }

        return Optional.empty();
    }

    public Optional<GridGeometry> isRegularGrid() {
        if (gridToRS instanceof CodeTransforms.Grid g) {
            return Optional.of(g.getGrid());
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CodedGeometry");
        if (rs != null) sb.append("\n").append(rs);
        if (extent != null) sb.append("\n").append(extent);
        if (gridToRS != null) sb.append("\n").append(gridToRS);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.rs);
        hash = 23 * hash + Objects.hashCode(this.geoExtent);
        hash = 23 * hash + Objects.hashCode(this.extent);
        hash = 23 * hash + Objects.hashCode(this.gridToRS);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CodedGeometry other = (CodedGeometry) obj;
        if (!Objects.equals(this.rs, other.rs)) {
            return false;
        }
        if (!Objects.equals(this.geoExtent, other.geoExtent)) {
            return false;
        }
        if (!Objects.equals(this.extent, other.extent)) {
            return false;
        }
        return Objects.equals(this.gridToRS, other.gridToRS);
    }

    public static CodedGeometry compound(CodedGeometry ... grids) {
        return CompoundCodedGeometry.compound(grids);
    }
}
