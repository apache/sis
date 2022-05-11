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
package org.apache.sis.gui.referencing;

import java.util.List;
import java.util.ArrayList;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;

import static java.util.logging.Logger.getLogger;


/**
 * Provider of map projections centered on a point of interest.
 * The point of interest is typically determined by mouse location.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@SuppressWarnings("serial")         // We do not guarantee serialization compatibility.
public abstract class PositionableProjection extends CodeList<PositionableProjection> {
    /**
     * List of all enumerations of this type.
     * Must be declared before any enum declaration.
     */
    private static final List<PositionableProjection> VALUES = new ArrayList<>(1);

    /**
     * Provides <cite>Orthographic</cite> projection centered on a point of interest.
     *
     * @see org.apache.sis.referencing.operation.projection.Orthographic
     */
    public static final PositionableProjection ORTHOGRAPHIC =
            new PositionableProjection("ORTHOGRAPHIC", Resources.Keys.Orthographic)
    {
        @Override protected ProjectedCRS createProjectedCRS(final GeographicCRS baseCRS,
                final double latitude, final double longitude) throws FactoryException
        {
            return newBuilder(latitude, longitude)
                    .setConversionMethod("Orthographic")
                    .setParameter("Latitude of natural origin",  latitude,  Units.DEGREE)
                    .setParameter("Longitude of natural origin", longitude, Units.DEGREE)
                    .createProjectedCRS(baseCRS, null);
        }
    };

    /**
     * Provides <cite>Azimuthal Equidistant</cite> projection centered on a point of interest.
     * For projection on the ellipsoid, this is valid only under 800 km of the point of interest.
     *
     * @see org.apache.sis.referencing.operation.projection.AzimuthalEquidistant
     */
    public static final PositionableProjection AZIMUTHAL_EQUIDISTANT =
            new PositionableProjection("AZIMUTHAL_EQUIDISTANT", Resources.Keys.AzimuthalEquidistant)
    {
        @Override protected ProjectedCRS createProjectedCRS(final GeographicCRS baseCRS,
                final double latitude, final double longitude) throws FactoryException
        {
            return newBuilder(latitude, longitude)
                    .setConversionMethod("Azimuthal Equidistant (Spherical)")
                    .setParameter("Latitude of natural origin",  latitude,  Units.DEGREE)
                    .setParameter("Longitude of natural origin", longitude, Units.DEGREE)
                    .createProjectedCRS(baseCRS, null);
        }
    };

    /**
     * Provides <cite>Universal Transverse Mercator</cite> projection for the zone in the point of interest.
     *
     * @see org.apache.sis.referencing.operation.projection.Mercator
     */
    public static final PositionableProjection UTM =
            new PositionableProjection("UTM", Resources.Keys.UTM)
    {
        @Override protected ProjectedCRS createProjectedCRS(final GeographicCRS baseCRS,
                final double latitude, final double longitude) throws FactoryException
        {
            CommonCRS cd;
            try {
                cd = CommonCRS.forDatum(baseCRS);
            } catch (IllegalArgumentException e) {
                Logging.recoverableException(getLogger(Modules.APPLICATION),
                            PositionableProjection.class, "createProjectedCRS", e);
                cd = CommonCRS.WGS84;
            }
            return cd.universal(latitude, longitude);
        }
    };

    /**
     * Provides <cite>Mercator (variant C)</cite> projection centered on a point of interest.
     *
     * @see org.apache.sis.referencing.operation.projection.Mercator
     */
    public static final PositionableProjection MERCATOR =
            new PositionableProjection("MERCATOR", Resources.Keys.Mercator)
    {
        @Override protected ProjectedCRS createProjectedCRS(final GeographicCRS baseCRS,
                final double latitude, final double longitude) throws FactoryException
        {
            return newBuilder(latitude, longitude)
                    .setConversionMethod("Mercator (variant C)")
                    .setParameter("Latitude of false origin",    latitude,  Units.DEGREE)
                    .setParameter("Longitude of natural origin", longitude, Units.DEGREE)
                    .createProjectedCRS(baseCRS, null);
        }
    };

    /**
     * The projection name as a {@link Resources} keys.
     */
    private final short nameKey;

    /**
     * Constructs an element of the given name. The new element is automatically added to the list
     * returned by {@link #values()}. Subclasses shall ensure that only one instance is created for
     * each value because there is no mechanism for removing previously created values.
     *
     * @param name  the name of the new element. This name shall not be in use by another element of this type.
     */
    protected PositionableProjection(final String name) {
        super(name, VALUES);
        nameKey = 0;
    }

    /**
     * Creates a new enumeration.
     */
    private PositionableProjection(final String name, final short nameKey) {
        super(name, VALUES);
        this.nameKey = nameKey;
    }

    /**
     * Returns the list of {@code PositionableProjection}s.
     *
     * @return the list of codes declared in the current JVM.
     */
    public static PositionableProjection[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(new PositionableProjection[VALUES.size()]);
        }
    }

    /**
     * Returns the list of codes of the same kind than this code list element.
     * Invoking this method is equivalent to invoking {@link #values()}, except that
     * this method can be invoked on an instance of the parent {@code CodeList} class.
     *
     * @return all code {@linkplain #values() values} for this code list.
     */
    @Override
    public PositionableProjection[] family() {
        return values();
    }

    /**
     * Returns a name for this enumeration which can be used in a user interface.
     *
     * @return a human-readable name for the projection created by this enumeration.
     */
    @Override
    public String toString() {
        return (nameKey != 0) ? Resources.format(nameKey) : name();
    }

    /**
     * Creates a map projection centered on the given position. The position must have a coordinate reference system,
     * but that CRS does not need to be geographic. The projection created by this method will use the same reference
     * frame (datum) than the given position.
     *
     * <p>The default implementation converts the position to latitude and longitude values and delegates to
     * {@link #createProjectedCRS(org.opengis.referencing.crs.GeographicCRS, double, double)}.</p>
     *
     * @param  center  the position at the center of the projection to create.
     * @return projection centered on the given position.
     * @throws FactoryException if an error occurred while creating the projection.
     * @throws TransformException if an error occurred while converting the given position.
     */
    public ProjectedCRS createProjectedCRS(DirectPosition center) throws FactoryException, TransformException {
        ArgumentChecks.ensureNonNull("center", center);
        final CoordinateReferenceSystem inherit = center.getCoordinateReferenceSystem();
        if (inherit == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnspecifiedCRS));
        }
        GeographicCRS normalizedCRS = ReferencingUtilities.toNormalizedGeographicCRS(inherit, true, false);
        if (normalizedCRS == null) {
            normalizedCRS = CommonCRS.WGS84.geographic();
        }
        if (!Utilities.equalsIgnoreMetadata(normalizedCRS, inherit)) {
            center = CRS.findOperation(inherit, normalizedCRS, null).getMathTransform().transform(center, null);
        }
        return createProjectedCRS(normalizedCRS,
                Latitude .clamp    (center.getOrdinate(0)),
                Longitude.normalize(center.getOrdinate(1)));
    }

    /**
     * Creates a map projection centered on the given latitude and longitude.
     *
     * @param  baseCRS    the base CRS of the projection to create.
     * @param  latitude   latitude of projection center in degrees.
     * @param  longitude  longitude of projection center in degrees.
     * @return projection centered on the given position.
     * @throws FactoryException if an error occurred while creating the projection.
     */
    protected abstract ProjectedCRS createProjectedCRS(final GeographicCRS baseCRS,
                        double latitude, double longitude) throws FactoryException;

    /**
     * Creates a new builder initialized to the projection name for the given coordinates.
     */
    final GeodeticObjectBuilder newBuilder(final double latitude, final double longitude) {
        final AngleFormat  f = new AngleFormat("DD°MM′SS″");
        final StringBuffer b = new StringBuffer();
        synchronized (b) {
            b.append(this).append(" @ ");
            f.format(new Latitude (latitude),  b, null).append(' ');
            f.format(new Longitude(longitude), b, null);
            return new GeodeticObjectBuilder().addName(b.toString());
        }
    }
}
