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
package org.apache.sis.referencing.internal;

import java.util.Iterator;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import static java.util.logging.Logger.getLogger;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.metadata.privy.ReferencingServices;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.TemporalAccessor;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.Logging;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the main branch:
import java.util.Map;
import org.opengis.util.TypeName;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumFactory;
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.DefaultParametricDatum;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.metadata.iso.citation.DefaultCitation;


/**
 * Implements the referencing services needed by the {@code org.apache.sis.metadata} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ServicesForMetadata extends ReferencingServices {
    /**
     * Name of an {@link OnLineFunction} code list value, used for transferring information about the EPSG database.
     */
    public static final String CONNECTION = "CONNECTION";

    /**
     * Creates a new instance. This constructor is invoked by reflection only.
     */
    public ServicesForMetadata() {
    }




    //  ╔═════════════════════════════════════════════════════════════════════════════╗
    //  ║                                                                             ║
    //  ║                       SERVICES FOR ISO 19115 METADATA                       ║
    //  ║                                                                             ║
    //  ╚═════════════════════════════════════════════════════════════════════════════╝

    /**
     * Implementation of the public {@code setBounds(…, DefaultGeographicBoundingBox, …)} methods for
     * the horizontal extent. If the {@code crs} argument is null, then it is caller's responsibility
     * to ensure that the given envelope is two-dimensional.
     *
     * <p>If {@code findOpCaller} is non-null, then this method is assumed to be invoked by something
     * equivalent to {@link Envelopes#findOperation(Envelope, Envelope)} for the purpose of computing
     * a <em>hint</em>, not a bounding box that needs to be exact.</p>
     *
     * @param  envelope       the source envelope.
     * @param  target         the target bounding box, or {@code null} for creating it automatically.
     * @param  crs            the envelope CRS, or {@code null} if unknown.
     * @param  normalizedCRS  the horizontal component of the given CRS, or null if the {@code crs} argument is null.
     * @param  findOpCaller   non-null for computing a hint rather than an exact bounding box.
     * @return the bounding box or {@code null} on failure. Never {@code null} if {@code findOpCaller} argument is {@code null}.
     * @throws TransformException if the given envelope cannot be transformed.
     */
    private static DefaultGeographicBoundingBox setGeographicExtent(Envelope envelope, DefaultGeographicBoundingBox target,
            final CoordinateReferenceSystem crs, final GeographicCRS normalizedCRS, final String findOpCaller) throws TransformException
    {
        if (normalizedCRS != null) {
            // No need to check for dimension, since GeodeticCRS cannot have less than 2.
            final CoordinateSystem cs1 = crs.getCoordinateSystem();
            final CoordinateSystem cs2 = normalizedCRS.getCoordinateSystem();
            if (!Utilities.equalsIgnoreMetadata(cs2.getAxis(0), cs1.getAxis(0)) ||
                !Utilities.equalsIgnoreMetadata(cs2.getAxis(1), cs1.getAxis(1)))
            {
                final CoordinateOperation operation;
                try {
                    operation = CRS.findOperation(crs, normalizedCRS, null);
                } catch (FactoryException e) {
                    if (findOpCaller != null) {
                        // See javadoc for the assumption that optional mode is used by Envelopes.findOperation(…).
                        Logging.recoverableException(getLogger(Modules.REFERENCING), Envelopes.class, findOpCaller, e);
                        return null;
                    }
                    throw new TransformException(Resources.format(Resources.Keys.CanNotTransformEnvelopeToGeodetic), e);
                }
                envelope = Envelopes.transform(operation, envelope);
            }
        }
        /*
         * At this point, the envelope should use (longitude, latitude) coordinates in degrees.
         * The envelope may cross the anti-meridian if the envelope implementation is an Apache SIS one.
         * For other implementations, the longitude range may be conservatively expanded to [-180 … 180]°.
         */
        double westBoundLongitude, eastBoundLongitude;
        double southBoundLatitude, northBoundLatitude;
        if (envelope instanceof AbstractEnvelope) {
            final AbstractEnvelope ae = (AbstractEnvelope) envelope;
            westBoundLongitude = ae.getLower(0);
            eastBoundLongitude = ae.getUpper(0);            // Cross anti-meridian if eastBoundLongitude < westBoundLongitude.
            southBoundLatitude = ae.getLower(1);
            northBoundLatitude = ae.getUpper(1);
        } else {
            westBoundLongitude = envelope.getMinimum(0);
            eastBoundLongitude = envelope.getMaximum(0);    // Expanded to [-180 … 180]° if it was crossing the anti-meridian.
            southBoundLatitude = envelope.getMinimum(1);
            northBoundLatitude = envelope.getMaximum(1);
        }
        /*
         * The envelope transformation at the beginning of this method intentionally avoided to apply datum shift.
         * This implies that the prime meridian has not been changed and may be something else than Greenwich.
         * We need to take it in account manually.
         *
         * Note that there is no need to normalize the longitudes back to the [-180 … +180]° range after the rotation, or
         * to verify if the longitude span is 360°. Those verifications will be done automatically by target.setBounds(…).
         */
        if (normalizedCRS != null) {
            final double rotation = CRS.getGreenwichLongitude(normalizedCRS);
            westBoundLongitude += rotation;
            eastBoundLongitude += rotation;
        }
        /*
         * In the particular case where this method is invoked (indirectly) for Envelopes.findOperation(…) purposes,
         * replace NaN values by the whole world.  We do that only for Envelopes.findOperation(…) since we know that
         * the geographic bounding box will be used for choosing a CRS, and a conservative approach is to select the
         * CRS valid in the widest area. If this method is invoked for other usages, then we keep NaN values because
         * we don't know the context (union, intersection, something else?).
         */
        if (findOpCaller != null) {
            if (Double.isNaN(southBoundLatitude)) southBoundLatitude = Latitude.MIN_VALUE;
            if (Double.isNaN(northBoundLatitude)) northBoundLatitude = Latitude.MAX_VALUE;
            if (Double.isNaN(eastBoundLongitude) || Double.isNaN(westBoundLongitude)) {
                // Conservatively set the two bounds because may be crossing the anti-meridian.
                eastBoundLongitude = Longitude.MIN_VALUE;
                westBoundLongitude = Longitude.MAX_VALUE;
            }
        }
        if (target == null) {
            target = new DefaultGeographicBoundingBox();
        }
        target.setBounds(westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude);
        target.setInclusion(Boolean.TRUE);
        return target;
    }

    /**
     * Implementation of the public {@code setBounds} methods for the vertical extent.
     * If the {@code crs} argument is null, then it is caller's responsibility to ensure
     * that the given envelope is one-dimensional.
     *
     * @param  envelope     the source envelope.
     * @param  target       the target vertical extent.
     * @param  crs          the envelope CRS, or {@code null} if unknown.
     * @param  verticalCRS  the vertical component of the given CRS, or null if the {@code crs} argument is null.
     */
    private static void setVerticalExtent(final Envelope envelope, final DefaultVerticalExtent target,
            final CoordinateReferenceSystem crs, final VerticalCRS verticalCRS)
    {
        final int dim;
        if (verticalCRS == null) {
            dim = 0;
        } else {
            dim = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), verticalCRS.getCoordinateSystem());
            assert dim >= 0 : crs;      // Should not fail since 'verticalCRS' has been extracted from 'crs' by the caller.
        }
        target.setMinimumValue(envelope.getMinimum(dim));
        target.setMaximumValue(envelope.getMaximum(dim));
        target.setVerticalCRS(verticalCRS);
    }

    /**
     * Sets a geographic bounding box from the specified envelope.
     * If the envelope has no CRS, then (<var>longitude</var>, <var>latitude</var>) axis order is assumed.
     *
     * <p>If {@code findOpCaller} is {@code true}, then the envelope will be computed in <em>hint</em> mode:
     * some exception may be logged instead of thrown, and the envelope may be expanded to the whole world.
     * This mode is for {@link Envelopes#findOperation(Envelope, Envelope)} usage of equivalent functions.</p>
     *
     * @param  envelope      the source envelope.
     * @param  target        the target bounding box, or {@code null} for creating it automatically.
     * @param  findOpCaller  non-null for computing a hint rather than an exact bounding box.
     * @return the bounding box, or {@code null} on failure (in hint mode) or if no horizontal component was found.
     * @throws TransformException if the given envelope cannot be transformed.
     */
    @Override
    public DefaultGeographicBoundingBox setBounds(final Envelope envelope, final DefaultGeographicBoundingBox target,
            final String findOpCaller) throws TransformException
    {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        GeographicCRS normalizedCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs, false, false);
        if (normalizedCRS == null) {
            if (crs != null) {
                normalizedCRS = CommonCRS.defaultGeographic();
            } else if (envelope.getDimension() != 2) {
                return null;
            }
        }
        return setGeographicExtent(envelope, target, crs, normalizedCRS, findOpCaller);
    }

    /**
     * Sets a vertical extent with the value inferred from the given envelope.
     * Only the vertical coordinates are extracted; all other coordinates are ignored.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target vertical extent where to store envelope information.
     * @return whether the envelope contains a vertical component.
     */
    @Override
    public boolean setBounds(final Envelope envelope, final DefaultVerticalExtent target) {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        if (verticalCRS == null && envelope.getDimension() != 1) {
            return false;
        }
        setVerticalExtent(envelope, target, crs, verticalCRS);
        return true;
    }

    /**
     * Sets a temporal extent with the value inferred from the given envelope.
     * Only the vertical coordinates are extracted; all other coordinates are ignored.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target temporal extent where to store envelope information.
     * @return whether the envelope contains a temporal component.
     */
    @Override
    public boolean setBounds(final Envelope envelope, final DefaultTemporalExtent target) {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final TemporalAccessor accessor = TemporalAccessor.of(crs, 0);
        if (accessor == null) {                     // Mandatory for the conversion from numbers to dates.
            return false;
        }
        accessor.setTemporalExtent(envelope, target);
        return true;
    }

    /**
     * Sets the geographic, vertical and temporal extents with the values inferred from the given envelope.
     * If the given {@code target} has more geographic or vertical extents than needed (0 or 1), then the
     * extraneous extents are removed.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target spatiotemporal extent where to store envelope information.
     * @return whether the envelope contains a spatial or temporal component.
     * @throws TransformException if a coordinate transformation was required and failed.
     */
    @Override
    public boolean setBounds(final Envelope envelope, final DefaultSpatialTemporalExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final SingleCRS horizontalCRS = CRS.getHorizontalComponent(crs);
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        final TemporalAccessor accessor = TemporalAccessor.of(crs, 0);
        if (horizontalCRS == null && verticalCRS == null && accessor == null) {
            return false;
        }
        /*
         * Try to set the geographic bounding box first, because this operation may fail with a
         * TransformException while the other operations (vertical and temporal) should not fail.
         * So doing the geographic part first help us to get a "all or nothing" behavior.
         */
        DefaultGeographicBoundingBox box = null;
        boolean useExistingBox = (horizontalCRS != null);
        final Collection<GeographicExtent> spatialExtents = target.getSpatialExtent();
        final Iterator<GeographicExtent> it = spatialExtents.iterator();
        while (it.hasNext()) {
            final GeographicExtent extent = it.next();
            if (extent instanceof GeographicBoundingBox) {
                if (useExistingBox && (extent instanceof DefaultGeographicBoundingBox)) {
                    box = (DefaultGeographicBoundingBox) extent;
                    useExistingBox = false;
                } else {
                    it.remove();
                }
            }
        }
        if (horizontalCRS != null) {
            if (box == null) {
                box = new DefaultGeographicBoundingBox();
                spatialExtents.add(box);
            }
            GeographicCRS normalizedCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs, false, false);
            if (normalizedCRS == null) {
                normalizedCRS = CommonCRS.defaultGeographic();
            }
            setGeographicExtent(envelope, box, crs, normalizedCRS, null);
        }
        /*
         * Other dimensions (vertical and temporal).
         */
        if (verticalCRS != null) {
            VerticalExtent e = target.getVerticalExtent();
            if (!(e instanceof DefaultVerticalExtent)) {
                e = new DefaultVerticalExtent();
                target.setVerticalExtent(e);
            }
            setVerticalExtent(envelope, (DefaultVerticalExtent) e, crs, verticalCRS);
        } else {
            target.setVerticalExtent(null);
        }
        if (accessor != null) {
            accessor.setTemporalExtent(envelope, target);
        } else {
            target.setExtent(null);
        }
        return true;
    }

    /**
     * Initializes a horizontal, vertical and temporal extent with the values inferred from the given envelope.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target extent where to store envelope information.
     * @return whether the envelope contains a spatial or temporal component.
     * @throws TransformException if a coordinate transformation was required and failed.
     * @throws UnsupportedOperationException if this method requires an Apache SIS module
     *         which has been found on the module path.
     */
    @Override
    public boolean addElements(final Envelope envelope, final DefaultExtent target) throws TransformException {
        boolean found = false;
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        if (CRS.hasHorizontalComponent(crs)) {
            DefaultGeographicBoundingBox horizontal = setBounds(envelope, null, null);
            if (horizontal != null) {
                target.getGeographicElements().add(horizontal);
                found = true;
            }
        }
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        if (verticalCRS != null) {
            final DefaultVerticalExtent extent = new DefaultVerticalExtent();
            setVerticalExtent(envelope, extent, crs, verticalCRS);
            target.getVerticalElements().add(extent);
            found = true;
        }
        final TemporalAccessor accessor = TemporalAccessor.of(crs, 0);
        if (accessor != null) {
            final DefaultTemporalExtent extent = new DefaultTemporalExtent();
            accessor.setTemporalExtent(envelope, extent);
            target.getTemporalElements().add(extent);
            found = true;
        }
        return found;
    }

    /**
     * Creates a two-dimensional geographic position associated to the default geographic CRS.
     * Axis order is (longitude, latitude).
     *
     * @param  λ  the longitude value.
     * @param  φ  the latitude value.
     * @return the direct position for the given geographic coordinate.
     */
    @Override
    public DirectPosition geographic(final double λ, final double φ) {
        return new DirectPosition2D(CommonCRS.defaultGeographic(), λ, φ);
    }

    /**
     * Returns an identifier for the given object, giving precedence to EPSG identifier if available.
     * The returned string should be of the form {@code "AUTHORITY:CODE"} if possible (no guarantees).
     *
     * @param  object  the object for which to get an identifier.
     * @return an identifier for the given object, with preference given to EPSG codes.
     * @throws FactoryException if an error occurred while searching for the EPSG code.
     */
    @Override
    public String getPreferredIdentifier(final IdentifiedObject object) throws FactoryException {
        final Integer code = IdentifiedObjects.lookupEPSG(object);
        if (code != null) {
            return Constants.EPSG + Constants.DEFAULT_SEPARATOR + code;
        }
        /*
         * If above code did not found an EPSG code, discard EPSG codes that
         * we may find in the loop below because they are probably invalid.
         */
        for (final ReferenceIdentifier id : object.getIdentifiers()) {
            if (!Constants.EPSG.equalsIgnoreCase(id.getCodeSpace())) {
                return IdentifiedObjects.toString(id);
            }
        }
        return IdentifiedObjects.getSimpleNameOrIdentifier(object);
    }




    //  ╔═════════════════════════════════════════════════════════════════════════════╗
    //  ║                                                                             ║
    //  ║                         OTHER REFERENCING SERVICES                          ║
    //  ║                                                                             ║
    //  ╚═════════════════════════════════════════════════════════════════════════════╝

    /**
     * Returns the name of the type of values.
     */
    @Override
    public TypeName getValueType(final ParameterDescriptor<?> parameter) {
        return (parameter instanceof DefaultParameterDescriptor<?>)
                ? ((DefaultParameterDescriptor<?>) parameter).getValueType() : null;
    }

    /**
     * Returns a fully implemented parameter descriptor.
     *
     * @param  <T>        the type of values.
     * @param  parameter  a partially implemented parameter descriptor, or {@code null}.
     * @return a fully implemented parameter descriptor, or {@code null} if the given argument was null.
     */
    @Override
    public <T> ParameterDescriptor<T> toImplementation(final ParameterDescriptor<T> parameter) {
        return DefaultParameterDescriptor.castOrCopy(parameter);
    }

    /**
     * Creates a format for {@link DirectPosition} instances.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     * @return a {@link org.apache.sis.geometry.CoordinateFormat}.
     */
    @Override
    public Format createCoordinateFormat(final Locale locale, final TimeZone timezone) {
        return new CoordinateFormat(locale, timezone);
    }

    /**
     * Returns transform between a pair of vertical CRS.
     *
     * @param  source  first CRS.
     * @param  target  second CRS.
     * @return transform between the given pair of CRS.
     * @throws FactoryException if the transform cannot be found.
     */
    @Override
    public MathTransform1D findTransform(final VerticalCRS source, final VerticalCRS target) throws FactoryException {
        return (MathTransform1D) CRS.findOperation(source, target, null).getMathTransform();
    }

    /**
     * Returns information about the Apache SIS configuration.
     * See super-class for a list of keys.
     *
     * @param  key     a key identifying the information to return.
     * @param  locale  language to use if possible.
     * @return the information, or {@code null} if none.
     */
    @Override
    public String getInformation(final String key, final Locale locale) {
        switch (key) {
            /*
             * Get the version of the EPSG database and the version of the database software.
             * This operation can be relatively costly as it may open a JDBC connection.
             */
            case Constants.EPSG: {
                final Citation authority;
                try {
                    try {
                        authority = CRS.getAuthorityFactory(Constants.EPSG).getAuthority();
                    } catch (BackingStoreException e) {
                        throw e.unwrapOrRethrow(FactoryException.class);
                    }
                } catch (FactoryException e) {
                    final String msg = Exceptions.getLocalizedMessage(e, locale);
                    return (msg != null) ? msg : Classes.getShortClassName(e);
                }
                if (authority instanceof DefaultCitation) {
                    final OnLineFunction f = OnLineFunction.valueOf(CONNECTION);
                    for (final OnlineResource res : ((DefaultCitation) authority).getOnlineResources()) {
                        if (f.equals(res.getFunction())) {
                            final InternationalString i18n = res.getDescription();
                            if (i18n != null) return i18n.toString(locale);
                        }
                    }
                    final InternationalString i18n = authority.getTitle();
                    if (i18n != null) return i18n.toString(locale);
                }
                return Vocabulary.forLocale(locale).getString(Vocabulary.Keys.Untitled);
            }
            // More cases may be added in future SIS versions.
        }
        return super.getInformation(key, locale);
    }
}
