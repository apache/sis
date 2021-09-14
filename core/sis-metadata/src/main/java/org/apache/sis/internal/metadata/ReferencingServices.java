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
package org.apache.sis.internal.metadata;

import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.OptionalDependency;
import org.apache.sis.internal.system.Modules;


/**
 * Provides access to services defined in the {@code "sis-referencing"} module.
 * This class searches for the {@link org.apache.sis.internal.referencing.ServicesForMetadata}
 * implementation using Java reflection.
 *
 * <p>This class also opportunistically defines some methods and constants related to
 * <cite>"referencing by coordinates"</cite> but needed by metadata.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public class ReferencingServices extends OptionalDependency {
    /**
     * The length of one nautical mile, which is {@value} metres.
     */
    public static final double NAUTICAL_MILE = 1852;

    /**
     * The GRS80 {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius() authalic radius},
     * which is {@value} metres. This is close to the WGS84 authalic radius, which is about 6371007.180918474 when
     * computed with {@code double} precision.
     */
    public static final double AUTHALIC_RADIUS = 6371007;

    /**
     * The services, fetched when first needed.
     */
    private static volatile ReferencingServices instance;

    /**
     * For subclass only. This constructor registers this instance as a
     * {@link org.apache.sis.internal.system.SystemListener} in order to
     * force a new {@code ReferencingServices} lookup if the classpath changes.
     */
    protected ReferencingServices() {
        super(Modules.METADATA, "sis-referencing");
    }

    /**
     * Invoked when the classpath changed. Resets the {@link #instance} to {@code null}
     * in order to force the next call to {@link #getInstance()} to fetch a new one,
     * which may be different.
     */
    @Override
    protected final void classpathChanged() {
        synchronized (ReferencingServices.class) {
            super.classpathChanged();
            instance = null;
        }
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance.
     */
    @SuppressWarnings("DoubleCheckedLocking")
    public static ReferencingServices getInstance() {
        ReferencingServices c = instance;
        if (c == null) {
            synchronized (ReferencingServices.class) {
                c = instance;
                if (c == null) {
                    /*
                     * Double-checked locking: okay since Java 5 provided that the 'instance' field is volatile.
                     * In the particular case of this class, the intent is to ensure that SystemListener.add(…)
                     * is invoked only once.
                     */
                    c = getInstance(ReferencingServices.class, Modules.METADATA, "sis-referencing",
                            "org.apache.sis.internal.referencing.ServicesForMetadata");
                    if (c == null) {
                        c = new ReferencingServices();
                    }
                    instance = c;
                }
            }
        }
        return c;
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                        SERVICES FOR ISO 19115 METADATA                        ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sets a geographic bounding box from the specified envelope.
     * If the envelope contains a CRS which is not geographic, then the bounding box will be transformed
     * to a geographic CRS (without datum shift if possible). Otherwise, the envelope is assumed already
     * in a geographic CRS using (<var>longitude</var>, <var>latitude</var>) axis order.
     *
     * <p>If {@code findOpCaller} is non-null, then this method will be executed in optional mode:
     * some failures will cause this method to return {@code null} instead of throwing an exception.
     * Note that {@link TransformException} may still be thrown but not directly by this method.
     * Warning may be logged, but in such case this method presumes that public caller is the named method from
     * {@code Envelopes} — typically {@link org.apache.sis.geometry.Envelopes#findOperation(Envelope, Envelope)}.</p>
     *
     * @param  envelope      the source envelope.
     * @param  target        the target bounding box, or {@code null} for creating it automatically.
     * @param  findOpCaller  non-null for replacing some (not all) exceptions by {@code null} return value.
     * @return the bounding box or {@code null} on failure. Never {@code null} if {@code findOpCaller} argument is {@code null}.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     * @throws TransformException if the given envelope can not be transformed.
     */
    public DefaultGeographicBoundingBox setBounds(Envelope envelope, DefaultGeographicBoundingBox target, String findOpCaller)
            throws TransformException
    {
        throw moduleNotFound();
    }

    /**
     * Sets a vertical extent with the value inferred from the given envelope.
     * Only the vertical coordinates are extracted; all other coordinates are ignored.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target vertical extent.
     * @throws TransformException if no vertical component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultVerticalExtent target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Sets a temporal extent with the value inferred from the given envelope.
     * Only the temporal coordinates are extracted; all other coordinates are ignored.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target temporal extent.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultTemporalExtent target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Sets the geographic, vertical and temporal extents with the values inferred from the given envelope.
     * If the given {@code target} has more geographic or vertical extents than needed (0 or 1), then the
     * extraneous extents are removed.
     *
     * <p>Behavior regarding missing dimensions:</p>
     * <ul>
     *   <li>If the given envelope has no horizontal component, then all geographic extents are removed
     *       from the given {@code target}. Non-geographic extents (e.g. descriptions and polygons) are
     *       left unchanged.</li>
     *   <li>If the given envelope has no vertical component, then the vertical extent is set to {@code null}.</li>
     *   <li>If the given envelope has no temporal component, then the temporal extent is set to {@code null}.</li>
     * </ul>
     *
     * @param  envelope  the source envelope.
     * @param  target    the target spatiotemporal extent.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultSpatialTemporalExtent target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Initializes a horizontal, vertical and temporal extent with the values inferred from the given envelope.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target extent.
     * @throws TransformException if a coordinate transformation was required and failed.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void addElements(Envelope envelope, DefaultExtent target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Creates a two-dimensional geographic position associated to the default geographic CRS.
     * Axis order is (longitude, latitude).
     *
     * @param  λ  the longitude value.
     * @param  φ  the latitude value.
     * @return the direct position for the given geographic coordinate.
     *
     * @since 0.8
     */
    public DirectPosition geographic(final double λ, final double φ) {
        throw moduleNotFound();
    }

    /**
     * Returns an identifier for the given object, giving precedence to EPSG identifier if available.
     * The returned string should be of the form {@code "AUTHORITY:CODE"} if possible (no guarantees).
     *
     * @param  object  the object for which to get an identifier.
     * @return an identifier for the given object, with preference given to EPSG codes.
     * @throws FactoryException if an error occurred while searching for the EPSG code.
     *
     * @since 1.0
     */
    public String getPreferredIdentifier(final IdentifiedObject object) throws FactoryException {
        throw moduleNotFound();
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                          OTHER REFERENCING SERVICES                           ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a fully implemented parameter descriptor.
     *
     * @param  parameter  a partially implemented parameter descriptor, or {@code null}.
     * @return a fully implemented parameter descriptor, or {@code null} if the given argument was null.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     *
     * @since 0.5
     */
    public ParameterDescriptor<?> toImplementation(ParameterDescriptor<?> parameter) {
        throw moduleNotFound();
    }

    /**
     * Creates a format for {@link DirectPosition} instances.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     * @return a {@link org.apache.sis.geometry.CoordinateFormat}.
     *
     * @since 0.8
     */
    public Format createCoordinateFormat(final Locale locale, final TimeZone timezone) {
        throw moduleNotFound();
    }

    /**
     * Returns the default coordinate operation factory.
     *
     * @return the coordinate operation factory to use.
     */
    public CoordinateOperationFactory getCoordinateOperationFactory() {
        final CoordinateOperationFactory factory = DefaultFactories.forClass(CoordinateOperationFactory.class);
        if (factory != null) {
            return factory;
        } else {
            throw moduleNotFound();
        }
    }

    /**
     * Returns information about the Apache SIS configuration to be reported in {@link org.apache.sis.setup.About}.
     * This method is invoked only for aspects that depends on other modules than {@code sis-utility}.
     *
     * <p>Current keys are:</p>
     * <ul>
     *   <li>{@code "EPSG"}: version of EPSG database.</li>
     * </ul>
     *
     * @param  key     a key identifying the information to return.
     * @param  locale  language to use if possible.
     * @return the information, or {@code null} if none.
     *
     * @see org.apache.sis.internal.util.MetadataServices#getInformation(String, Locale)
     *
     * @since 0.7
     */
    public String getInformation(String key, Locale locale) {
        return null;
    }
}
