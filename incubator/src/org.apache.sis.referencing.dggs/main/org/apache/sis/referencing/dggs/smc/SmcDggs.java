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
package org.apache.sis.referencing.dggs.smc;

import java.util.List;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.transform.TransformFactoryException;
import org.apache.sis.util.Utilities;
import org.apache.sis.referencing.dggs.GridConstraints;
import org.apache.sis.referencing.dggs.PolyhedronOrientation;
import org.apache.sis.referencing.dggs.PolyhedronParameters;
import org.apache.sis.referencing.dggs.RefinementStrategy;
import org.apache.sis.referencing.dggs.internal.shared.AbstractDiscreteGlobalGridSystem;


/**
 * Grid system of an SMC reference system, with the conversions between its base CRS and lattice coordinates.
 * <p>
 * There is no base polyhedron: the lattice is defined directly on (rotated) latitude and longitude.
 * Base and lattice coordinates are (longitude, latitude) pairs in degrees, which differ only when the lattice
 * has a rotated pole. Longitudes returned in the base CRS are normalized to the [−180 … 180)° range.
 *
 * @author Johann Sorel (Geomatys)
 * @see "Li 2023, SMC grid guide, sections 3.1, 6.3 and 7.5"
 */
final class SmcDggs extends AbstractDiscreteGlobalGridSystem {

    private final SmcDggrs dggrs;

    /**
     * Rotation from base to lattice coordinates, or {@code null} for the identity.
     */
    private final MathTransform toLattice;

    final SmcDggh dggh;

    /**
     * Creates the grid system of the given reference system.
     *
     * @param  dggrs      the reference system, with its lattice definition already set.
     * @param  base       base CRS, or {@code null} for CRS:84. Its axes are normalized.
     * @param  toLattice  rotation from base to lattice coordinates, or {@code null} for the identity.
     */
    SmcDggs(final SmcDggrs dggrs, final GeographicCRS base, final MathTransform toLattice) {
        super(base == null ? CommonCRS.WGS84.normalizedGeographic()
                           : DefaultGeographicCRS.castOrCopy(base).forConvention(AxesConvention.NORMALIZED));
        this.dggrs     = dggrs;
        this.toLattice = toLattice;
        this.dggh      = new SmcDggh(dggrs);
    }

    /**
     * Returns the radius of the sphere having the surface of the celestial body.
     */
    double radius() {
        return Math.sqrt(getCelestialBodySurface().to(Units.SQUARE_METRE).getValue().doubleValue() / (4 * Math.PI));
    }

    /**
     * Converts a position to lattice coordinates.
     *
     * @param  position  position in any CRS. Positions without CRS are assumed in the base CRS.
     * @return (longitude, latitude) in lattice coordinates.
     * @throws TransformException if the position cannot be converted.
     */
    double[] toLattice(final DirectPosition position) throws TransformException {
        DirectPosition source = position;
        final CoordinateReferenceSystem positionCrs = position.getCoordinateReferenceSystem();
        if (positionCrs != null && !Utilities.equalsIgnoreMetadata(crs, positionCrs)) {
            try {
                source = CRS.findOperation(positionCrs, crs, null).getMathTransform().transform(position, null);
            } catch (FactoryException e) {
                throw new TransformFactoryException(e);
            }
        }
        final double[] coordinates = new double[] {source.getCoordinate(0), source.getCoordinate(1)};
        toLattice(coordinates, 0, coordinates, 0);
        return coordinates;
    }

    /**
     * Converts one (longitude, latitude) pair from the base CRS to lattice coordinates.
     * The source and target arrays may be the same.
     */
    void toLattice(final double[] source, final int sourceOffset, final double[] target, final int targetOffset)
            throws TransformException
    {
        if (toLattice != null) {
            toLattice.transform(source, sourceOffset, target, targetOffset, 1);
        } else {
            target[targetOffset]     = source[sourceOffset];
            target[targetOffset + 1] = source[sourceOffset + 1];
        }
    }

    /**
     * Converts (longitude, latitude) pairs from lattice coordinates to the base CRS, in place.
     *
     * @param  coordinates  pairs to convert.
     * @param  offset       index of the first longitude.
     * @param  count        number of pairs.
     */
    void toBase(final double[] coordinates, final int offset, final int count) throws TransformException {
        if (toLattice != null) {
            toLattice.inverse().transform(coordinates, offset, coordinates, offset, count);
        }
        for (int i = offset, end = offset + 2 * count; i < end; i += 2) {
            coordinates[i] = Longitude.normalize(coordinates[i]);
        }
    }

    /**
     * Converts lattice coordinates to a position in the base CRS.
     */
    DirectPosition2D toPosition(final double longitude, final double latitude) throws TransformException {
        final double[] coordinates = new double[] {longitude, latitude};
        toBase(coordinates, 0, 1);
        return new DirectPosition2D(crs, coordinates[0], coordinates[1]);
    }

    @Override
    public SmcDggh getHierarchy() {
        return dggh;
    }

    @Override
    public String getBasePolyhedron() {
        return "none";
    }

    /**
     * Returns 4, the number of children of a cell whose rows keep the same merge factor.
     */
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
        return List.of(SmcZone.QUADRILATERAL, SmcZone.POLAR_CAP);
    }

    /**
     * Returns the geographic position of the lattice north pole.
     */
    @Override
    public PolyhedronParameters getParameters() {
        return new PolyhedronParameters(new PolyhedronOrientation(
                dggrs.poleLatitude, dggrs.poleLongitude, 0, "Geographic position of the lattice north pole."));
    }

    @Override
    public List<RefinementStrategy> getRefinementStrategy() {
        return List.of(RefinementStrategy.nestedChildCell);
    }

    /**
     * Returns {@code cellAxisAligned} for unrotated lattices, and an empty list otherwise.
     */
    @Override
    public List<GridConstraints> getGridConstraints() {
        return dggrs.rotated ? List.of() : List.of(GridConstraints.cellAxisAligned);
    }
}
