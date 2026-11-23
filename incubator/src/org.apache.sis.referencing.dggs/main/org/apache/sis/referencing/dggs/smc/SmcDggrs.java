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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.ObjectDomain;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.gazetteer.LocationType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.measure.Longitude;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.gazetteer.ModifiableLocationType;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.PoleRotation;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridSystem;
import org.apache.sis.referencing.dggs.SubZoneOrder;
import org.apache.sis.referencing.dggs.ZonalReferenceSystem;
import org.apache.sis.referencing.dggs.internal.shared.DefaultZonalReferenceSystem;


/**
 * Spherical Multiple-Cell (SMC) grid as a discrete global grid reference system.
 * <p>
 * The grid is a latitude/longitude lattice in lattice coordinates, which are the geographic coordinates rotated
 * when a pole rotation is given. Level 0 is the coarsest level and level <var>k</var> divides the level 0 increments
 * by 2<sup><var>k</var></sup>. A row merges its cells by 2<sup><var>p</var></sup>, where <var>p</var> is the number
 * of size-changing parallels (arccos 2<sup>−<var>p</var></sup>) strictly crossed by its poleward edge. When the domain
 * reaches a pole, the last row of each level is a single round polar cell.
 * <p>
 * Each level covers the whole domain, land included. A multi-resolution SMC grid of a wave model, such as SMC6-25,
 * is a set of zones taken from several levels. Zone identifiers are 64-bit integers or {@code "level-i-j"} texts,
 * where <var>i</var> is the column of the cell south-west corner and <var>j</var> its row, as in SMC cell arrays.
 *
 * @author Johann Sorel (Geomatys)
 * @see "Li 2023, SMC grid guide, sections 3.1 and 3.4"
 */
public final class SmcDggrs extends DiscreteGlobalGridReferenceSystem {

    /**
     * Identifier of the reference system on the global lattice.
     */
    public static final String IDENTIFIER = "SMC";

    /**
     * Tolerance in degrees on the domain bounds and on the size-changing parallels.
     */
    static final double TOLERANCE = 1e-9;

    private static final ZonalReferenceSystem ZRS = new DefaultZonalReferenceSystem("default",
            "Level, then column and row of the cell south-west corner in units of the level.");

    /**
     * Longitude and latitude in degrees of the domain south-west corner, in lattice coordinates.
     */
    final double westLongitude, southLatitude;

    /**
     * Cell width (before merging) and height in degrees at level 0.
     */
    final double longitudeIncrement, latitudeIncrement;

    /**
     * Number of columns and rows at level 0.
     */
    final int columnCount, rowCount;

    /**
     * Whether a pole rotation is applied, and the geographic position in degrees of the lattice north pole.
     */
    final boolean rotated;
    final double poleLatitude, poleLongitude;

    /**
     * Whether the columns span 360°, and whether the domain reaches the south and north poles.
     */
    final boolean wraps, southPolar, northPolar;

    final SmcDggs dggs;

    /**
     * Creates the reference system on the global lattice and CRS:84.
     */
    public SmcDggrs() {
        this((GeographicCRS) null);
    }

    /**
     * Creates the reference system on the global lattice aligned with the Met Office SMC grids.
     * <p>
     * Level 0 has 8×6 cells of 45°×30° with the origin at 0°E, 90°S. Level 6 is the 50 km grid, level 7 the 25 km base
     * (0.3515625°×0.234375°), and levels 8, 9 and 10 are the 12, 6 and 3 km refinements.
     *
     * @param  base  geographic CRS of zone positions and geometries, or {@code null} for CRS:84.
     * @see "Li 2023, SMC grid guide, section 3.1"
     */
    public SmcDggrs(final GeographicCRS base) {
        this(IDENTIFIER, base, 0, -90, 45, 30, 8, 6);
    }

    /**
     * Creates a reference system on an unrotated lattice.
     * <p>
     * Constraints:
     * <ul>
     *   <li>Increments are strictly positive and finite.</li>
     *   <li>The domain stays between −90° and 90° of latitude and spans at most 360° of longitude.</li>
     *   <li>The lattice wraps around the globe when its columns span exactly 360°. It then needs at least 2 columns.</li>
     *   <li>A domain reaching a pole must wrap around the globe.</li>
     *   <li>A domain reaching both poles needs at least 2 rows.</li>
     *   <li>Row and column indices of level 0 must fit in 58 bits.</li>
     *   <li>Merge factors are capped so that each row holds a whole number of cells, and at least 2 cells
     *       when the lattice wraps.</li>
     * </ul>
     *
     * @param  name                name of the reference system.
     * @param  base                geographic CRS of zone positions and geometries, or {@code null} for CRS:84.
     *                             Its axes are normalized to (longitude, latitude) in degrees.
     * @param  westLongitude       longitude in degrees of the domain west edge.
     * @param  southLatitude       latitude in degrees of the domain south edge.
     * @param  longitudeIncrement  cell width in degrees at level 0, before merging.
     * @param  latitudeIncrement   cell height in degrees at level 0.
     * @param  columnCount         number of columns at level 0.
     * @param  rowCount            number of rows at level 0.
     * @throws IllegalArgumentException if a constraint is violated.
     */
    public SmcDggrs(final String name, final GeographicCRS base,
                    final double westLongitude, final double southLatitude,
                    final double longitudeIncrement, final double latitudeIncrement,
                    final int columnCount, final int rowCount)
    {
        this(name, base, westLongitude, southLatitude, longitudeIncrement, latitudeIncrement,
             columnCount, rowCount, false, 90, 0, null);
    }

    /**
     * Creates a reference system on a lattice whose north pole is moved to the given geographic position.
     * Rows and columns are defined in the rotated latitude/longitude coordinates.
     * The constraints of the unrotated constructor apply to the rotated coordinates.
     *
     * @param  name                name of the reference system.
     * @param  base                geographic CRS of zone positions and geometries, or {@code null} for CRS:84.
     * @param  westLongitude       rotated longitude in degrees of the domain west edge.
     * @param  southLatitude       rotated latitude in degrees of the domain south edge.
     * @param  longitudeIncrement  cell width in degrees at level 0, before merging.
     * @param  latitudeIncrement   cell height in degrees at level 0.
     * @param  columnCount         number of columns at level 0.
     * @param  rowCount            number of rows at level 0.
     * @param  poleLatitude        geographic latitude in degrees of the lattice north pole.
     * @param  poleLongitude       geographic longitude in degrees of the lattice north pole.
     * @throws IllegalArgumentException if a constraint is violated.
     * @throws FactoryException if the pole rotation cannot be created.
     * @see "Li 2023, SMC grid guide, sections 6.3 and 7.5"
     */
    public SmcDggrs(final String name, final GeographicCRS base,
                    final double westLongitude, final double southLatitude,
                    final double longitudeIncrement, final double latitudeIncrement,
                    final int columnCount, final int rowCount,
                    final double poleLatitude, final double poleLongitude) throws FactoryException
    {
        this(name, base, westLongitude, southLatitude, longitudeIncrement, latitudeIncrement,
             columnCount, rowCount, true, poleLatitude, poleLongitude,
             PoleRotation.rotateNorthPole(DefaultMathTransformFactory.provider(), poleLatitude, poleLongitude, 0));
    }

    private SmcDggrs(final String name, final GeographicCRS base,
                     final double westLongitude, final double southLatitude,
                     final double longitudeIncrement, final double latitudeIncrement,
                     final int columnCount, final int rowCount,
                     final boolean rotated, final double poleLatitude, final double poleLongitude,
                     final MathTransform rotation)
    {
        super(properties(name, rotated, westLongitude, southLatitude,
                         columnCount * longitudeIncrement, rowCount * latitudeIncrement), types(name));
        ArgumentChecks.ensureFinite("westLongitude", westLongitude);
        ArgumentChecks.ensureFinite("southLatitude", southLatitude);
        ArgumentChecks.ensureStrictlyPositive("longitudeIncrement", longitudeIncrement);
        ArgumentChecks.ensureStrictlyPositive("latitudeIncrement", latitudeIncrement);
        ArgumentChecks.ensureFinite("longitudeIncrement", longitudeIncrement);
        ArgumentChecks.ensureFinite("latitudeIncrement", latitudeIncrement);
        ArgumentChecks.ensureStrictlyPositive("columnCount", columnCount);
        ArgumentChecks.ensureStrictlyPositive("rowCount", rowCount);
        ArgumentChecks.ensureBetween("poleLatitude", -90.0, 90.0, poleLatitude);
        ArgumentChecks.ensureFinite("poleLongitude", poleLongitude);
        this.westLongitude      = westLongitude;
        this.southLatitude      = southLatitude;
        this.longitudeIncrement = longitudeIncrement;
        this.latitudeIncrement  = latitudeIncrement;
        this.columnCount        = columnCount;
        this.rowCount           = rowCount;
        this.rotated            = rotated;
        this.poleLatitude       = poleLatitude;
        this.poleLongitude      = poleLongitude;

        final double width = columnCount * longitudeIncrement;
        final double north = southLatitude + rowCount * latitudeIncrement;
        if (southLatitude < -90 - TOLERANCE || north > 90 + TOLERANCE) {
            throw new IllegalArgumentException("Lattice rows span " + southLatitude + "° to " + north
                    + "°, which is outside the −90° … 90° range.");
        }
        if (width > 360 + TOLERANCE) {
            throw new IllegalArgumentException("Lattice columns span " + width + "°, which is more than 360°.");
        }
        wraps      = Math.abs(width - 360) <= TOLERANCE;
        southPolar = Math.abs(southLatitude + 90) <= TOLERANCE;
        northPolar = Math.abs(north - 90) <= TOLERANCE;
        if ((southPolar || northPolar) && !wraps) {
            throw new IllegalArgumentException("A lattice reaching a pole must span 360° of longitude.");
        }
        if (wraps && columnCount < 2) {
            throw new IllegalArgumentException("A lattice spanning 360° of longitude needs at least 2 columns.");
        }
        if (southPolar && northPolar && rowCount < 2) {
            throw new IllegalArgumentException("A lattice reaching both poles needs at least 2 rows.");
        }
        dggs = new SmcDggs(this, base, rotation);
    }

    @Override
    public List<String> getKeywords() {
        return List.of("smc", "spherical multiple-cell", "dggs", "wave model");
    }

    @Override
    public Optional<InternationalString> getDescription() {
        return Optional.of(new SimpleInternationalString(
              "Spherical Multiple-Cell grid: a latitude-longitude grid whose cells are halved at each refinement "
            + "level, merged longitudinally at high latitudes to relax the CFL restriction, and closed by round "
            + "polar cells. It is used by the WAVEWATCH III ocean wave model."));
    }

    /**
     * Returns the WAVEWATCH III repository, which hosts the SMC grid documentation.
     */
    @Override
    public URI getUri() {
        return URI.create("https://github.com/NOAA-EMC/WW3");
    }

    @Override
    public DiscreteGlobalGridSystem getGridSystem() {
        return dggs;
    }

    @Override
    public ZonalReferenceSystem getZonalSystem() {
        return ZRS;
    }

    /**
     * Returns {@link SubZoneOrder#SCANLINE}: children are listed row by row from south to north, then west to east.
     */
    @Override
    public SubZoneOrder getSubZoneOrder() {
        return SubZoneOrder.SCANLINE;
    }

    @Override
    public Coder createCoder() {
        return new SmcCoder(this);
    }

    /**
     * Compares the lattice definitions in addition to the properties compared by the parent class.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (!super.equals(object, mode)) {
            return false;
        }
        if (object instanceof SmcDggrs other) {
            return Double.compare(westLongitude,      other.westLongitude)      == 0
                && Double.compare(southLatitude,      other.southLatitude)      == 0
                && Double.compare(longitudeIncrement, other.longitudeIncrement) == 0
                && Double.compare(latitudeIncrement,  other.latitudeIncrement)  == 0
                && Double.compare(poleLatitude,       other.poleLatitude)       == 0
                && Double.compare(poleLongitude,      other.poleLongitude)      == 0
                && columnCount == other.columnCount
                && rowCount    == other.rowCount
                && rotated     == other.rotated;
        }
        return mode != ComparisonMode.STRICT;
    }

    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + 31L * Objects.hash(westLongitude, southLatitude,
                longitudeIncrement, latitudeIncrement, columnCount, rowCount, rotated, poleLatitude, poleLongitude);
    }

    private static LocationType[] types(final String name) {
        final ModifiableLocationType type = new ModifiableLocationType(name);
        type.addIdentification(Vocabulary.formatInternational(Vocabulary.Keys.Code));
        return new LocationType[] {type};
    }

    private static Map<String,Object> properties(final String name, final boolean rotated,
            final double west, final double south, final double width, final double height)
    {
        final Map<String,Object> properties = new HashMap<>(8);
        properties.put(NAME_KEY, name);
        properties.put(IDENTIFIERS_KEY, new ImmutableIdentifier(Citations.SIS, Constants.SIS, name));
        properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY, domainOfValidity(rotated, west, south, width, height));
        properties.put(THEME_KEY, Vocabulary.formatInternational(Vocabulary.Keys.Mapping));
        return properties;
    }

    /**
     * Returns the geographic area covered by the lattice. Rotated lattices declare the whole world.
     */
    private static Extent domainOfValidity(final boolean rotated, final double west, final double south,
                                           final double width, final double height)
    {
        final double north = south + height;
        final boolean allLongitudes = width >= 360 - TOLERANCE;
        if (rotated || (allLongitudes && south <= -90 + TOLERANCE && north >= 90 - TOLERANCE)) {
            return Extents.WORLD;
        }
        double lower = -180;
        double upper =  180;
        if (!allLongitudes) {
            lower = Longitude.normalize(west);
            upper = lower + width;
            if (upper > 180) upper -= 360;
        }
        return new DefaultExtent(null, new DefaultGeographicBoundingBox(
                lower, upper, Math.max(-90, south), Math.min(90, north)), null, null);
    }
}
