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
package org.apache.sis.storage.wkt;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.geometry.Geometry;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.referencing.CRS;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.referencing.privy.DefinitionVerifier;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.system.Loggers;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.ArraysExt;


/**
 * Helper class for reading and writing WKT in the metadata of a data store.
 * This is provided as a separated class for allowing reuse by other data stores.
 * For example, WKT may also appear in some global attributes of CF-netCDF files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})       // Not intended to be serialized.
public final class StoreFormat extends WKTFormat {
    /**
     * The geometry library, or {@code null} for the default.
     */
    private final GeometryLibrary library;

    /**
     * Where to send warnings.
     */
    private final StoreListeners listeners;

    /**
     * Creates a new WKT parser and encoder.
     * The given locale will be used for {@link InternationalString} localization;
     * this is <strong>not</strong> the locale for number format.
     *
     * @param  locale     the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone   the timezone, or {@code null} for UTC.
     * @param  library    the geometry library, or {@code null} for the default.
     * @param  listeners  where to send warnings.
     */
    public StoreFormat(final Locale locale, final ZoneId timezone,
                       final GeometryLibrary library, final StoreListeners listeners)
    {
        super(locale, timezone);
        this.library   = library;
        this.listeners = listeners;
    }

    /**
     * Parses a geometry together with its coordinate reference system, all in Well Known Text (WKT).
     *
     * @param  geometry       the geometry to parse, or {@code null} if none.
     * @param  crs            the horizontal part of the WKT (2D or 3D), or {@code null} if none.
     * @param  additionalCRS  the vertical or temporal part of the WKT, or {@code null} if none.
     * @return the geometry, or {@code null} if none or unparseable.
     */
    public Geometry parseGeometry(final String geometry, final String crs, final String additionalCRS) {
        if (geometry != null) try {
            final GeometryWrapper obj = Geometries.factory(library).parseWKT(geometry);
            obj.setCoordinateReferenceSystem(parseCRS(crs, additionalCRS));
            return obj;
        } catch (Exception e) {     // Implementation-specific exception (e.g. JTS has its own exception class).
            log(e);
        }
        return null;
    }

    /**
     * Parses the given WKTs as a coordinate reference system. The given array may contain two elements if,
     * for example, vertical CRS is specified separately from horizontal CRS. If an exception occurs during
     * parsing, it will be reported as a data store warning.
     *
     * @param  wkt  the Well Known Texts to parse. Null elements are ignored.
     * @return the parsed coordinate reference system, or {@code null} if none.
     */
    public CoordinateReferenceSystem parseCRS(final String... wkt) {
        try {
            final CoordinateReferenceSystem[] components = new CoordinateReferenceSystem[wkt.length];
            int n = 0;
            for (final String ct : wkt) {
                if (ct != null) {
                    final Object crs = parseObject(ct);
                    validate(Loggers.WKT, listeners.getSource().getClass(), "getMetadata", crs);
                    components[n++] = (CoordinateReferenceSystem) crs;
                }
            }
            if (n != 0) {
                return CRS.compound(ArraysExt.resize(components, n));
            }
        } catch (ParseException | ClassCastException | IllegalArgumentException | FactoryException e) {
            log(e);
        }
        return null;
    }

    /**
     * Reports pending warnings and verifies if the parsed WKT is conform with the authority definition
     * (if an authority code has been specified). This verification is not really necessary since we will
     * use the WKT definition anyway even if we find discrepancies. But non-conform WKT definitions happen
     * so often in practice that we are better to check and warn users.
     *
     * <p>This method does not need to be invoked after {@code parseGeometry(…)} or {@code parseCRS(…)}
     * because above-cited methods already invoke this method.</p>
     *
     * @param  logger  name of the logger where to send warnings, or {@code null} for the provider's logger.
     * @param  caller  the class to report as the warning source if a log record is created.
     * @param  method  the method to report as the warning source if a log record is created.
     * @param  parsed  the object parsed from WKT, or {@code null} if none.
     */
    public void validate(final String logger, final Class<?> caller, final String method, final Object parsed) {
        final Warnings warnings = getWarnings();
        if (warnings != null) {
            final var record = new LogRecord(Level.WARNING, warnings.toString());
            record.setSourceClassName(caller.getCanonicalName());
            record.setSourceMethodName(method);
            record.setLoggerName(logger);
            listeners.warning(record);
        }
        if (parsed instanceof CoordinateReferenceSystem) try {
            final DefinitionVerifier v = DefinitionVerifier.withAuthority(
                    (CoordinateReferenceSystem) parsed, null, false, listeners.getLocale());
            if (v != null) {
                final LogRecord record = v.warning(false);
                if (record != null) {
                    record.setSourceClassName(caller.getCanonicalName());
                    record.setSourceMethodName(method);
                    record.setLoggerName(logger);
                    listeners.warning(record);
                }
            }
        } catch (FactoryException e) {
            listeners.warning(e);
        }
    }

    /**
     * Reports a warning for a WKT that cannot be read. This method should be invoked only when the CRS
     * cannot be created at all. It should not be invoked if the CRS has been created with some warnings.
     * This method pretends that the warning come from {@code getMetadata()} method, which is the public
     * facade for the parsing method.
     */
    private void log(final Exception e) {
        final LogRecord record = Resources.forLocale(listeners.getLocale())
                .getLogRecord(Level.WARNING, Resources.Keys.CanNotReadCRS_WKT_1, listeners.getSourceName());
        record.setSourceClassName(listeners.getSource().getClass().getCanonicalName());
        record.setSourceMethodName("getMetadata");
        record.setLoggerName(Loggers.WKT);
        listeners.warning(record);
    }
}
