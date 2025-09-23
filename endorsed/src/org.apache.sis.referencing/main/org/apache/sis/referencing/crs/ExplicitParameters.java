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
package org.apache.sis.referencing.crs;

import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.logging.Logging;


/**
 * Temporary object for formatting the projection method and parameters inside a {@code Conversion} element.
 * This object formats only the explicit parameters. Implicit parameters derived from source ellipsoid are omitted.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class ExplicitParameters extends FormattableObject {
    /**
     * The conversion which specify the operation method and parameters.
     */
    private final Conversion conversion;

    /**
     * Semi-major and semi-minor axis lengths, or {@code null} if the datum is not geodetic.
     */
    private final Ellipsoid ellipsoid;

    /**
     * The keyword to be returned by {@link #formatTo(Formatter)}.
     * Should be {@link WKTKeywords#Conversion} or {@link WKTKeywords#DerivingConversion}.
     */
    private final String keyword;

    /**
     * Creates a new temporary {@code Conversion} elements for the parameters of the given CRS.
     */
    ExplicitParameters(final AbstractDerivedCRS<?> crs, final String keyword) {
        conversion = crs.getConversionFromBase();
        ellipsoid = DatumOrEnsemble.getEllipsoid(crs).orElse(null);
        this.keyword = keyword;
    }

    /**
     * Formats this {@code Conversion} element.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(conversion, formatter, null);
        formatter.newLine();
        append(formatter);
        return keyword;
    }

    /**
     * Formats this {@code Conversion} element without the conversion name.
     */
    void append(final Formatter formatter) {
        formatter.append(DefaultOperationMethod.castOrCopy(conversion.getMethod()));
        formatter.newLine();
        for (final GeneralParameterValue param : conversion.getParameterValues().values()) {
            final GeneralParameterDescriptor desc = param.getDescriptor();
            if (ellipsoid != null) {
                String name;
                if (IdentifiedObjects.isHeuristicMatchForName(desc, name = Constants.SEMI_MAJOR) ||
                    IdentifiedObjects.isHeuristicMatchForName(desc, name = Constants.SEMI_MINOR))
                {
                    /*
                     * Do not format semi-major and semi-minor axis length in most cases,  since those
                     * information are provided in the ellipsoid.  An exception to this rule occurs if
                     * the lengths are different from the ones declared in the datum.
                     */
                    if (param instanceof ParameterValue<?>) {
                        final double value;
                        try {
                            value = ((ParameterValue<?>) param).doubleValue(ellipsoid.getAxisUnit());
                        } catch (IllegalStateException e) {
                            /*
                             * May happen if the `conversionFromBase` parameter group does not provide values
                             * for "semi_major" or "semi_minor" axis length. This should not happen with SIS
                             * implementation, but may happen with user-defined map projection implementations.
                             * Since the intent of this check was to skip those parameters anyway, it is okay
                             * for the purpose of WKT formatting if there are no parameters for axis lengths.
                             */
                            Logging.recoverableException(WKTUtilities.LOGGER, DefaultProjectedCRS.class, "formatTo", e);
                            continue;
                        }
                        if (Double.isNaN(value)) {
                            continue;
                        }
                        final double expected = (name == Constants.SEMI_MINOR)   // using `==` is okay here.
                                ? ellipsoid.getSemiMinorAxis() : ellipsoid.getSemiMajorAxis();
                        if (value == expected) {
                            continue;
                        }
                    }
                }
            }
            WKTUtilities.append(param, formatter);
        }
    }
}
