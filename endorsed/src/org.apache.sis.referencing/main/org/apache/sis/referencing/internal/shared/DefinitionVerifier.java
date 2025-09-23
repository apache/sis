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
package org.apache.sis.referencing.internal.shared;

import java.util.Iterator;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.FactoryException;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeneralDerivedCRS;


/**
 * Verifies the conformance of a given CRS with an authoritative description.
 * For example if a Well Known Text (WKT) contains an EPSG code, this class verifies that
 * the CRS created from the WKT is equivalent to the CRS identified by the authority code.
 * {@code DefinitionVerifier} contains two information:
 *
 * <ul>
 *   <li>The recommended CRS to use. May be the given CRS or a CRS created from the authority factory.</li>
 *   <li>Warnings if the given CRS does not match the authoritative description.</li>
 * </ul>
 *
 * <b>Note:</b> ISO 19162 said about the {@code Identifier} keyword:  <q>In the event of conflict in values given
 * in the CRS WKT string and given by an authority through an object’s name or an identifier, reading software should
 * throw an exception or give users a warning message. The WKT values should be assumed to prevail.</q>
 * In practice when such conflicts happen, we often see that the given WKT string contains mistakes and the
 * provider intended to use the authoritative description. We nevertheless comply with ISO 19162 requirement,
 * but provide a "recommended CRS" field for what we think is the intended CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefinitionVerifier {
    /**
     * List of CRS variants to try if the given CRS does not match the expected description.
     * For performance reason, this list should be ordered with most probable variant first
     * and less probable variant last.
     */
    private static final AxesConvention[] VARIANTS = {
        AxesConvention.NORMALIZED,
        AxesConvention.DISPLAY_ORIENTED,
        AxesConvention.RIGHT_HANDED
    };

    /**
     * Recommended CRS. May be the instance given to the {@link #withAuthority withAuthority(…)} method
     * or an instance created from the authority factory. May also be {@code null} if all CRS given to the
     * {@link #compare(CoordinateReferenceSystem, CoordinateReferenceSystem, Locale) compare(…)} method were null.
     *
     * Note that ISO 19162 said <q>Should any attributes or values given in the cited identifier be in conflict
     * with attributes or values given explicitly in the WKT description, the WKT values shall prevail.</q>
     * So we normally do not use this field.
     */
    public final CoordinateReferenceSystem recommendation;

    /**
     * If {@link #withAuthority withAuthority(…)} produced a localizable warning, the resource key for creating the
     * actual message. A value of 0 means that the warning is already localized and stored in {@code arguments[0]}.
     */
    private short resourceKey;

    /**
     * The arguments to use together with {@link #resourceKey} for producing the warning message.
     */
    private Object[] arguments;

    /**
     * The locale for warning messages, or {@code null} for the system default.
     */
    private final Locale locale;

    /**
     * Creates the result of a call to {@code withAuthority(…)}.
     */
    private DefinitionVerifier(final CoordinateReferenceSystem recommendation, final Locale locale) {
        this.recommendation = recommendation;
        this.locale = locale;
    }

    /**
     * Compares the given CRS description with the authoritative description.
     * If the comparison produces a warning, a message will be recorded to the given logger.
     *
     * @param  crs     the CRS to compare with the authoritative description.
     * @param  logger  the logger where to report warnings, if any.
     * @param  classe  the class to declare as the source of the warning.
     * @param  method  the method to declare as the source of the warning.
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    public static void withAuthority(final CoordinateReferenceSystem crs, final String logger,
            final Class<?> classe, final String method) throws FactoryException
    {
        final DefinitionVerifier verification = DefinitionVerifier.withAuthority(crs, null, false, null);
        if (verification != null) {
            final LogRecord record = verification.warning(true);
            if (record != null) {
                record.setLoggerName(logger);
                Logging.completeAndLog(null, classe, method, record);
            }
        }
    }

    /**
     * Compares the given CRS description with the authoritative description.
     * The authoritative description is inferred from the identifier, if any.
     *
     * @param  crs      the CRS to compare with the authoritative description.
     * @param  factory  the factory to use for fetching authoritative description, or {@code null} for the default.
     * @param  lookup   whether this method is allowed to use {@link IdentifiedObjectFinder}.
     * @param  locale   the locale for warning messages, or {@code null} for the system default.
     * @return verification result, or {@code null} if the given CRS should be used as-is.
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    public static DefinitionVerifier withAuthority(final CoordinateReferenceSystem crs, final CRSAuthorityFactory factory,
            final boolean lookup, final Locale locale) throws FactoryException
    {
        final CoordinateReferenceSystem authoritative;
        final Citation authority = (factory != null) ? factory.getAuthority() : null;
        final String identifier = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(crs, authority));
        if (identifier != null) try {
            /*
             * An authority code was explicitly given in the CRS description. Create a CRS for that code
             * (do not try to guess it). If the given code is unknown, we will report a warning and use
             * the given CRS as-is.
             */
            if (factory != null) {
                authoritative = factory.createCoordinateReferenceSystem(identifier);
            } else {
                authoritative = CRS.forCode(identifier);
            }
        } catch (NoSuchAuthorityCodeException e) {
            final DefinitionVerifier verifier = new DefinitionVerifier(crs, locale);
            verifier.arguments = new String[] {e.getLocalizedMessage()};
            return verifier;
        } else if (lookup) {
            /*
             * No authority code was given in the CRS description. Try to guess the code with IdentifiedObjectFinder,
             * ignoring axis order. If we cannot guess a code or if we guess wrongly, use the given CRS silently
             * (without reporting any warning) since there is apparently nothing wrong in the given CRS.
             */
            final IdentifiedObjectFinder finder;
            if (factory instanceof GeodeticAuthorityFactory) {
                finder = ((GeodeticAuthorityFactory) factory).newIdentifiedObjectFinder();
            } else {
                finder = IdentifiedObjects.newFinder(Citations.getIdentifier(authority));
            }
            finder.setIgnoringAxes(true);
            final IdentifiedObject ref = finder.findSingleton(crs);
            if (ref instanceof CoordinateReferenceSystem) {
                authoritative = (CoordinateReferenceSystem) ref;
            } else {
                return null;                                            // Found no identifier. Use the CRS as-is.
            }
        } else {
            return null;
        }
        /*
         * At this point we found an authoritative description (typically from EPSG database) for the given CRS.
         * Verify if the given CRS is equal to the authoritative description, or a variant of it.
         */
        return compare(crs, authoritative, identifier != null, identifier == null, locale);
    }

    /**
     * Compares the given CRS with an authoritative definition of that CRS.
     * Typically, {@code crs} is parsed from a Well-Known Text (WKT) definition while
     * {@code authoritative} is provided by a geodetic database from an authority code.
     *
     * <p>The {@link #recommendation} CRS is set as below:</p>
     * <ul>
     *   <li>If one of given CRS is {@code null}, then the other CRS (which may also be null) is selected.</li>
     *   <li>Otherwise if {@code crs} is compatible with {@code authority} with only a change in axis order,
     *       a CRS derived from {@code authority} but with {@code crs} axis order is silently selected.</li>
     *   <li>Otherwise {@code authority} is selected and a {@linkplain #warning(boolean) warning message} is prepared.</li>
     * </ul>
     *
     * @param  crs            the CRS to compare against an authoritative definition, or {@code null}.
     * @param  authoritative  the presumed authoritative definition of the given CRS, or {@code null}.
     * @param  locale         the locale for warning messages, or {@code null} for the system default.
     * @return verification result (never {@code null}).
     */
    public static DefinitionVerifier compare(final CoordinateReferenceSystem crs,
            final CoordinateReferenceSystem authoritative, final Locale locale)
    {
        if (crs == null || authoritative == null) {
            return new DefinitionVerifier((crs != null) ? crs : authoritative, locale);
        } else {
            return compare(crs, authoritative, false, false, locale);
        }
    }

    /**
     * Implementation of {@link #compare(CoordinateReferenceSystem, CoordinateReferenceSystem, Locale)}
     * and final step in {@code forAuthority(…)} methods. The boolean flags control the behavior
     * in case of mismatched axis order or full mismatch.
     *
     * @param  strictAxisOrder  whether the CRS should comply with authoritative axis order.
     *                          If {@code true}, mismatched axis order will be reported as a warning.
     *                          If {@code false}, they will be silently ignored.
     * @param  nullIfNoMatch    whether to return {@code null} if CRS do not match.
     *                          If {@code false}, then this method never return {@code null}.
     * @return verification result, possibly {@code null} if {@code nullIfNoMatch} is {@code true}.
     */
    private static DefinitionVerifier compare(final CoordinateReferenceSystem crs,
                                              final CoordinateReferenceSystem authoritative,
                                              final boolean strictAxisOrder,
                                              final boolean nullIfNoMatch,
                                              final Locale locale)
    {
        /*
         * The similarity flag has the following meaning:
         *   (-) mismatch
         *   (0) equality
         *   (+) equality when using a variant
         */
        int similarity = 0;
        final AbstractCRS ca = AbstractCRS.castOrCopy(authoritative);
        AbstractCRS variant = ca;
        while (!variant.equals(crs, ComparisonMode.APPROXIMATE)) {
            if (similarity < VARIANTS.length) {
                variant = ca.forConvention(VARIANTS[similarity++]);
            } else if (nullIfNoMatch) {
                return null;        // Mismatched CRS, but our "authoritative" description was only a guess. Ignore.
            } else {
                similarity = -1;    // Mismatched CRS and our authoritative description was not a guess. Need warning.
                break;
            }
        }
        final DefinitionVerifier verifier;
        if (similarity > 0) {
            /*
             * Warning message (from Resources.properties):
             *
             *     The coordinate system axes in the given “{0}” description do not conform to the expected axes
             *     according “{1}” authoritative description.
             */
            verifier = new DefinitionVerifier(variant, locale);
            if (strictAxisOrder) {
                verifier.resourceKey = Resources.Keys.NonConformAxes_2;
                verifier.arguments   = new String[2];
            }
        } else {
            verifier = new DefinitionVerifier(authoritative, locale);
            if (similarity != 0) {
                /*
                 * Warning message (from Resources.properties):
                 *
                 *     The given “{0}” description does not conform to the “{1}” authoritative description.
                 *     Differences are found in {2,choice,0#method|1#conversion|2#coordinate system|3#datum|4#CRS}.
                 */
                verifier.resourceKey  = Resources.Keys.NonConformCRS_3;
                verifier.arguments    = new Object[3];
                verifier.arguments[2] = diffCode(CRS.getSingleComponents(authoritative).iterator(),
                                                 CRS.getSingleComponents(crs).iterator());
            }
        }
        if (verifier.arguments != null) {
            verifier.arguments[0] = IdentifiedObjects.getDisplayName(crs, locale);
            verifier.arguments[1] = IdentifiedObjects.getIdentifierOrName(authoritative);
        }
        return verifier;
    }

    /**
     * Indicates in which part of CRS description a difference has been found. Numerical values must match the number
     * in the {@code {choice}} instruction in the message associated to {@link Resources.Keys#NonConformCRS_3}.
     */
    private static final int METHOD=0, CONVERSION=1, CS=2, ELLIPSOID=3, PRIME_MERIDIAN=4, DATUM=5, OTHER=6;

    /**
     * Returns a code indicating in which part the two given CRS differ. The given iterators usually iterate over
     * exactly one element, but may iterate over more elements if the CRS were instance of {@code CompoundCRS}.
     * The returned value is one of {@link #METHOD}, {@link #CONVERSION}, {@link #CS}, {@link #PRIME_MERIDIAN},
     * {@link #ELLIPSOID}, {@link #DATUM} or {@link #OTHER} constants.
     */
    private static int diffCode(final Iterator<SingleCRS> authoritative, final Iterator<SingleCRS> given) {
        while (authoritative.hasNext() && given.hasNext()) {
            final SingleCRS crsA = authoritative.next();
            final SingleCRS crsG = given.next();
            if (!Utilities.equalsApproximately(crsA, crsG)) {
                if (crsA instanceof GeneralDerivedCRS && crsG instanceof GeneralDerivedCRS) {
                    final Conversion cnvA = ((GeneralDerivedCRS) crsA).getConversionFromBase();
                    final Conversion cnvG = ((GeneralDerivedCRS) crsG).getConversionFromBase();
                    if (!Utilities.equalsApproximately(cnvA, cnvG)) {
                        return Utilities.equalsApproximately(cnvA.getMethod(), cnvG.getMethod()) ? CONVERSION : METHOD;
                    }
                }
                for (int code = CS; code <= DATUM; code++) {
                    final Function<SingleCRS, ?> getter;
                    switch (code) {
                        case CS:             getter = SingleCRS::getCoordinateSystem; break;
                        case ELLIPSOID:      getter = DatumOrEnsemble::getEllipsoid; break;
                        case PRIME_MERIDIAN: getter = DatumOrEnsemble::getPrimeMeridian; break;
                        case DATUM:          getter = DatumOrEnsemble::of; break;
                        default: throw new AssertionError(code);
                    }
                    if (!Utilities.equalsApproximately(getter.apply(crsA), getter.apply(crsG))) {
                        return code;
                    }
                }
                break;
            }
        }
        return OTHER;
    }

    /**
     * Returns the warning to log, or {@code null} if none. The caller is responsible for setting the logger name,
     * source class name and source method name.
     *
     * @param  fine  {@code true} for including warnings at fine level, or {@code false} for only the warning level.
     * @return the warning to log, or {@code null} if none.
     */
    public LogRecord warning(final boolean fine) {
        if (arguments != null) {
            if (resourceKey != 0) {
                return Resources.forLocale(locale).createLogRecord(Level.WARNING, resourceKey, arguments);
            } else if (fine) {
                return new LogRecord(Level.FINE, (String) arguments[0]);
            }
        }
        return null;
    }
}
