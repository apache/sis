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

import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.Deprecable;


/**
 * Provides access to services defined in the {@code "sis-referencing"} module.
 * This class searches for the {@link org.apache.sis.internal.referencing.ServicesForMetadata}
 * implementation using Java reflection.
 *
 * <p>This class also opportunistically defines some methods and constants related to
 * <cite>"referencing by coordinates"</cite> but needed by metadata.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public class ReferencingServices extends SystemListener {
    /**
     * The length of one nautical mile, which is {@value} metres.
     */
    public static final double NAUTICAL_MILE = 1852;

    /**
     * The GRS80 {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius() authalic radius},
     * which is {@value} metres.
     */
    public static final double AUTHALIC_RADIUS = 6371007;

    /**
     * The key for specifying explicitely the value to be returned by
     * {@link org.apache.sis.referencing.operation.DefaultConversion#getParameterValues()}.
     * It is usually not necessary to specify those parameters because they are inferred either from
     * the {@link MathTransform}, or specified explicitely in a {@code DefiningConversion}. However
     * there is a few cases, for example the Molodenski transform, where none of the above can apply,
     * because SIS implements those operations as a concatenation of math transforms, and such
     * concatenations do not have {@link org.opengis.parameter.ParameterValueGroup}.
     */
    public static final String PARAMETERS_KEY = "parameters";

    /**
     * The key for specifying a {@linkplain org.opengis.referencing.operation.MathTransformFactory}
     * instance to use for the construction of a geodetic object. This is usually not needed for CRS
     * construction, except in the special case of a derived CRS created from a defining conversion.
     */
    public static final String MT_FACTORY = "mtFactory";

    /**
     * The separator character between an identifier and its namespace in the argument given to
     * {@link #getOperationMethod(String)}. For example this is the separator in {@code "EPSG:9807"}.
     *
     * This is defined as a constant for now, but we may make it configurable in a future version.
     */
    private static final char IDENTIFIER_SEPARATOR = DefaultNameSpace.DEFAULT_SEPARATOR;

    /**
     * The services, fetched when first needed.
     */
    private static volatile ReferencingServices instance;

    /**
     * For subclass only. This constructor registers this instance as a {@link SystemListener}
     * in order to force a new {@code ReferencingServices} lookup if the classpath changes.
     */
    protected ReferencingServices() {
        super(Modules.METADATA);
        SystemListener.add(this);
    }

    /**
     * Invoked when the classpath changed. Resets the {@link #instance} to {@code null}
     * in order to force the next call to {@link #getInstance()} to fetch a new one,
     * which may be different.
     */
    @Override
    protected final void classpathChanged() {
        synchronized (ReferencingServices.class) {
            SystemListener.remove(this);
            instance = null;
        }
    }

    /**
     * Returns the singleton instance.
     *
     * @return The singleton instance.
     */
    public static ReferencingServices getInstance() {
        ReferencingServices c = instance;
        if (c == null) {
            synchronized (ReferencingServices.class) {
                c = instance;
                if (c == null) {
                    /*
                     * Double-checked locking: okay since Java 5 provided that the 'instance' field is volatile.
                     * In the particular case of this class, the intend is to ensure that SystemListener.add(…)
                     * is invoked only once.
                     */
                    try {
                        c = (ReferencingServices) Class.forName("org.apache.sis.internal.referencing.ServicesForMetadata").newInstance();
                    } catch (ClassNotFoundException exception) {
                        /*
                         * Log at the "warning" level. This method is usually invoked from a metadata class,
                         * except org.apache.sis.io.wkt.Formatter. So we infer the logger from the stacktrace.
                         */
                        String logger = Modules.METADATA; // Default logger.
                        for (final StackTraceElement e : exception.getStackTrace()) {
                            final String name = e.getClassName();
                            if (name.startsWith(Modules.METADATA)) break;
                            if (name.startsWith("org.apache.sis.io.wkt")) {
                                logger = "org.apache.sis.io.wkt";
                                break;
                            }
                        }
                        Logging.recoverableException(Logging.getLogger(logger),
                                ReferencingServices.class, "getInstance", exception);
                        c = new ReferencingServices();
                    } catch (ReflectiveOperationException exception) {
                        // Should never happen if we didn't broke our helper class.
                        throw new AssertionError(exception);
                    }
                    instance = c;
                }
            }
        }
        return c;
    }

    /**
     * Returns the exception to throw when a method requiring the {@code sis-referencing} module is invoked
     * but that module is not on the classpath.
     */
    private static UnsupportedOperationException referencingModuleNotFound() {
        return new UnsupportedOperationException(Errors.format(Errors.Keys.MissingRequiredModule_1, "sis-referencing"));
    }

    /**
     * Returns a fully implemented parameter descriptor.
     *
     * @param  parameter A partially implemented parameter descriptor, or {@code null}.
     * @return A fully implemented parameter descriptor, or {@code null} if the given argument was null.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     *
     * @since 0.5
     */
    public ParameterDescriptor<?> toImplementation(ParameterDescriptor<?> parameter) {
        throw referencingModuleNotFound();
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance.
     *
     * @param  object The object to wrap.
     * @return The given object converted to a {@code FormattableObject} instance.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     *
     * @see org.apache.sis.referencing.AbstractIdentifiedObject#castOrCopy(IdentifiedObject)
     *
     * @since 0.4
     */
    public FormattableObject toFormattableObject(IdentifiedObject object) {
        throw referencingModuleNotFound();
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance. Callers should verify that the given
     * object is not already an instance of {@code FormattableObject} before to invoke this method. This method
     * returns {@code null} if it can not convert the object.
     *
     * @param  object The object to wrap.
     * @param  internal {@code true} if the formatting convention is {@code Convention.INTERNAL}.
     * @return The given object converted to a {@code FormattableObject} instance, or {@code null}.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     *
     * @since 0.6
     */
    public FormattableObject toFormattableObject(MathTransform object, boolean internal) {
        throw referencingModuleNotFound();
    }

    /**
     * Sets a geographic bounding box from the specified envelope.
     * If the envelope contains a CRS which is not geographic, then the bounding box will be transformed
     * to a geographic CRS (without datum shift if possible). Otherwise, the envelope is assumed already
     * in a geographic CRS using (<var>longitude</var>, <var>latitude</var>) axis order.
     *
     * @param  envelope The source envelope.
     * @param  target The target bounding box.
     * @throws TransformException if the given envelope can't be transformed.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultGeographicBoundingBox target) throws TransformException {
        throw referencingModuleNotFound();
    }

    /**
     * Sets a vertical extent with the value inferred from the given envelope.
     * Only the vertical ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope The source envelope.
     * @param  target The target vertical extent.
     * @throws TransformException if no vertical component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultVerticalExtent target) throws TransformException {
        throw referencingModuleNotFound();
    }

    /**
     * Sets a temporal extent with the value inferred from the given envelope.
     * Only the temporal ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope The source envelope.
     * @param  target The target temporal extent.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultTemporalExtent target) throws TransformException {
        throw referencingModuleNotFound();
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
     * @param  envelope The source envelope.
     * @param  target The target spatio-temporal extent.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultSpatialTemporalExtent target) throws TransformException {
        throw referencingModuleNotFound();
    }

    /**
     * Initializes a horizontal, vertical and temporal extent with the values inferred from the given envelope.
     *
     * @param  envelope The source envelope.
     * @param  target The target extent.
     * @throws TransformException if a coordinate transformation was required and failed.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void addElements(Envelope envelope, DefaultExtent target) throws TransformException {
        throw referencingModuleNotFound();
    }

    /**
     * Returns {@code true} if the {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName()
     * primary name} or an aliases of the given object matches the given name. The comparison ignores case,
     * some Latin diacritical signs and any characters that are not letters or digits.
     *
     * @param  object The object for which to check the name or alias.
     * @param  name The name to compare with the object name or aliases.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     */
    public boolean isHeuristicMatchForName(final IdentifiedObject object, final String name) {
        return NameToIdentifier.isHeuristicMatchForName(object.getName(), object.getAlias(), name);
    }

    /**
     * Returns the operation method for the specified name or identifier. The given argument shall be either a
     * method name (e.g. <cite>"Transverse Mercator"</cite>) or one of its identifiers (e.g. {@code "EPSG:9807"}).
     *
     * @param  methods The method candidates.
     * @param  identifier The name or identifier of the operation method to search.
     * @return The coordinate operation method for the given name or identifier, or {@code null} if none.
     *
     * @see org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#getOperationMethod(String)
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#getOperationMethod(String)
     */
    public static OperationMethod getOperationMethod(final Iterable<? extends OperationMethod> methods, final String identifier) {
        OperationMethod fallback = null;
        for (final OperationMethod method : methods) {
            if (matches(method, identifier)) {
                /*
                 * Stop the iteration at the first non-deprecated method.
                 * If we find only deprecated methods, take the first one.
                 */
                if (!(method instanceof Deprecable) || !((Deprecable) method).isDeprecated()) {
                    return method;
                }
                if (fallback == null) {
                    fallback = method;
                }
            }
        }
        return fallback;
    }

    /**
     * Returns {@code true} if the name or an identifier of the given method matches the given {@code identifier}.
     *
     * @param  method     The method to test for a match.
     * @param  identifier The name or identifier of the operation method to search.
     * @return {@code true} if the given method is a match for the given identifier.
     */
    private static boolean matches(final OperationMethod method, final String identifier) {
        if (getInstance().isHeuristicMatchForName(method, identifier)) {
            return true;
        }
        for (int s = identifier.indexOf(IDENTIFIER_SEPARATOR); s >= 0;
                 s = identifier.indexOf(IDENTIFIER_SEPARATOR, s))
        {
            final String codespace = identifier.substring(0, s).trim();
            final String code = identifier.substring(++s).trim();
            for (final Identifier id : method.getIdentifiers()) {
                if (codespace.equalsIgnoreCase(id.getCodeSpace()) && code.equalsIgnoreCase(id.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }
}
