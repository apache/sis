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
package org.apache.sis.pending.geoapi.referencing;

import java.util.Map;
import java.util.Collection;
import java.util.function.Function;
import java.time.temporal.Temporal;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.crs.DefaultParametricCRS;
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;
import org.apache.sis.referencing.datum.DefaultParametricDatum;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;


/**
 * Placeholder for methods missing in the GeoAPI 3.0 interface.
 */
public final class MissingMethods {
    /**
     * To be set by static {@code AbstractCRS} initializer.
     */
    public static volatile Function<CoordinateReferenceSystem, DefaultDatumEnsemble<?>> datumEnsemble;

    /**
     * To be set by static {@code DefaultGeodeticCRS} initializer.
     */
    public static volatile Function<GeodeticCRS, DefaultDatumEnsemble<GeodeticDatum>> geodeticDatumEnsemble;

    private MissingMethods() {
    }

    /**
     * Returns the datum ensemble of an arbitrary CRS.
     *
     * @param  datum  the CRS from which to get a datum ensemble, or {@code null} if none.
     * @return the datum ensemble, or {@code null} if none.
     */
    public static DefaultDatumEnsemble<?> getDatumEnsemble(final CoordinateReferenceSystem crs) {
        final var m = datumEnsemble;
        return (m != null) ? m.apply(crs) : null;
    }

    /**
     * Returns the datum ensemble of an arbitrary geodetic CRS.
     *
     * @param  datum  the CRS from which to get a datum ensemble, or {@code null} if none.
     * @return the datum ensemble, or {@code null} if none.
     */
    public static DefaultDatumEnsemble<GeodeticDatum> getDatumEnsemble(final GeodeticCRS crs) {
        final var m = geodeticDatumEnsemble;
        return (m != null) ? m.apply(crs) : null;
    }

    /**
     * Returns the datum ensemble of an arbitrary vertical CRS.
     *
     * @param  datum  the CRS from which to get a datum ensemble, or {@code null} if none.
     * @return the datum ensemble, or {@code null} if none.
     */
    public static DefaultDatumEnsemble<VerticalDatum> getDatumEnsemble(final VerticalCRS crs) {
        return (crs instanceof DefaultVerticalCRS) ? ((DefaultVerticalCRS) crs).getDatumEnsemble() : null;
    }

    /**
     * Returns the datum ensemble of an arbitrary temporal CRS.
     *
     * @param  datum  the CRS from which to get a datum ensemble, or {@code null} if none.
     * @return the datum ensemble, or {@code null} if none.
     */
    public static DefaultDatumEnsemble<TemporalDatum> getDatumEnsemble(final TemporalCRS crs) {
        return (crs instanceof DefaultTemporalCRS) ? ((DefaultTemporalCRS) crs).getDatumEnsemble() : null;
    }

    /**
     * Returns the datum ensemble of an arbitrary engineering CRS.
     *
     * @param  datum  the CRS from which to get a datum ensemble, or {@code null} if none.
     * @return the datum ensemble, or {@code null} if none.
     */
    public static DefaultDatumEnsemble<EngineeringDatum> getDatumEnsemble(final EngineeringCRS crs) {
        return (crs instanceof DefaultEngineeringCRS) ? ((DefaultEngineeringCRS) crs).getDatumEnsemble() : null;
    }

    /**
     * Creates a datum ensemble. This method requires the <abbr>SIS</abbr> factory
     * since datum ensembles were not available in GeoAPI 3.0.
     *
     * @param  <D>         the type of datum contained in the ensemble.
     * @param  properties  name and other properties to give to the new object.
     * @param  members     datum or reference frames which are members of the datum ensemble.
     * @param  accuracy    inaccuracy introduced through use of the given collection of datums.
     * @return the datum ensemble for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    public static <D extends Datum> DefaultDatumEnsemble<D> createDatumEnsemble(
            final Map<String,?> properties,
            final Collection<? extends D> members,
            final PositionalAccuracy accuracy,
            DatumFactory factory) throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createDatumEnsemble(properties, members, accuracy);
    }

    /**
     * Creates a parametric CS. This method requires the <abbr>SIS</abbr> factory
     * since parametric CRS were not available in GeoAPI 3.0.
     *
     * @param  properties  the coordinate system name, and optionally other properties.
     * @param  axis        the axis of the parametric coordinate system.
     * @param  factory     the factory to use for creating the coordinate system.
     * @return a parametric coordinate system using the given axes.
     * @throws FactoryException if the parametric object creation failed.
     */
    public static DefaultParametricCS createParametricCS(final Map<String,?> properties, final CoordinateSystemAxis axis,
            CSFactory factory) throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createParametricCS(properties, axis);
    }

    /**
     * Creates a parametric <abbr>CRS</abbr>. This method requires the <abbr>SIS</abbr> factory
     * since parametric <abbr>CRS</abbr> were not available in GeoAPI 3.0.
     *
     * @param  properties  the coordinate reference system name, and optionally other properties.
     * @param  datum       the parametric datum.
     * @param  cs          the parametric coordinate system.
     * @param  factory     the factory to use for creating the coordinate reference system.
     * @return a parametric coordinate system using the given axes.
     * @throws FactoryException if the parametric object creation failed.
     */
    public static DefaultParametricCRS createParametricCRS(final Map<String,?> properties, final DefaultParametricDatum datum,
            final DefaultParametricCS cs, CRSFactory factory) throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createParametricCRS(properties, datum, cs);
    }

    /**
     * Creates a parametric datum. This method requires the <abbr>SIS</abbr> factory
     * since parametric <abbr>CRS</abbr> were not available in GeoAPI 3.0.
     *
     * @param  properties  the datum name, and optionally other properties.
     * @param  factory     the factory to use for creating the datum.
     * @return a parametric datum using the given name.
     * @throws FactoryException if the parametric object creation failed.
     */
    public static DefaultParametricDatum createParametricDatum(final Map<String,?> properties, DatumFactory factory)
            throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createParametricDatum(properties);
    }

    public static VerticalDatum createVerticalDatum(final Map<String,?> properties, final VerticalDatumType type,
            final Temporal epoch, DatumFactory factory) throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createVerticalDatum(properties, type, epoch);
    }

    public static GeodeticDatum createGeodeticDatum(final Map<String,?> properties, final Ellipsoid ellipsoid,
            final PrimeMeridian primeMeridian, final Temporal epoch, DatumFactory factory) throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createGeodeticDatum(properties, ellipsoid, primeMeridian, epoch);
    }

    public static GeographicCRS createGeographicCRS(final Map<String,?> properties, final GeodeticDatum datum,
            final DefaultDatumEnsemble<GeodeticDatum> ensemble, final EllipsoidalCS cs, CRSFactory factory)
            throws FactoryException
    {
        if (ensemble == null) {
            return factory.createGeographicCRS(properties, datum, cs);
        }
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createGeographicCRS(properties, datum, ensemble, cs);
    }

    public static GeodeticCRS createGeodeticCRS(final Map<String,?> properties, final GeodeticDatum datum,
            final DefaultDatumEnsemble<GeodeticDatum> ensemble, final SphericalCS cs, CRSFactory factory)
            throws FactoryException
    {
        if (ensemble == null) {
            return factory.createGeocentricCRS(properties, datum, cs);
        }
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createGeodeticCRS(properties, datum, ensemble, cs);
    }

    public static GeodeticCRS createGeodeticCRS(final Map<String,?> properties, final GeodeticDatum datum,
            final DefaultDatumEnsemble<GeodeticDatum> ensemble, final CartesianCS cs, CRSFactory factory)
            throws FactoryException
    {
        if (ensemble == null) {
            return factory.createGeocentricCRS(properties, datum, cs);
        }
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createGeodeticCRS(properties, datum, ensemble, cs);
    }

    public static VerticalCRS createVerticalCRS(final Map<String,?> properties, final VerticalDatum datum,
            final DefaultDatumEnsemble<VerticalDatum> ensemble, final VerticalCS cs, CRSFactory factory)
            throws FactoryException
    {
        if (ensemble == null) {
            return factory.createVerticalCRS(properties, datum, cs);
        }
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createVerticalCRS(properties, datum, ensemble, cs);
    }

    public static TemporalCRS createTemporalCRS(final Map<String,?> properties, final TemporalDatum datum,
            final DefaultDatumEnsemble<TemporalDatum> ensemble, final TimeCS cs, CRSFactory factory)
            throws FactoryException
    {
        if (ensemble == null) {
            return factory.createTemporalCRS(properties, datum, cs);
        }
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createTemporalCRS(properties, datum, ensemble, cs);
    }

    public static DefaultParametricCRS createParametricCRS(final Map<String,?> properties, final DefaultParametricDatum datum,
            final DefaultDatumEnsemble<DefaultParametricDatum> ensemble, final DefaultParametricCS cs, CRSFactory factory)
            throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createParametricCRS(properties, datum, ensemble, cs);
    }

    public static EngineeringCRS createEngineeringCRS(final Map<String,?> properties, final EngineeringDatum datum,
            final DefaultDatumEnsemble<EngineeringDatum> ensemble, final CoordinateSystem cs, CRSFactory factory)
            throws FactoryException
    {
        if (ensemble == null) {
            return factory.createEngineeringCRS(properties, datum, cs);
        }
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = GeodeticObjectFactory.provider();
        }
        return ((GeodeticObjectFactory) factory).createEngineeringCRS(properties, datum, ensemble, cs);
    }
}
