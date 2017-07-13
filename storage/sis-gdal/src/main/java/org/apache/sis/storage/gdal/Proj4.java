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
package org.apache.sis.storage.gdal;

import org.opengis.util.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.Static;


/**
 * Bindings to the Proj4 library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Proj4 extends Static {
    /**
     * The Proj4 parameter used for declaration of axis order. This parameter is handled in a special way
     * by the factories: it be a comma-separated list of axis order definitions, in which case the second
     * value is used as the axis order of the {@link org.opengis.referencing.crs.ProjectedCRS#getBaseCRS()}.
     *
     * <p>An other departure from Proj.4 is that Proj.4 expect the axis parameter to be exactly
     * 3 characters long, which our code accepts 2 characters as well. We relax the Proj.4
     * rule because we use the number of characters for determining the number of dimensions.
     * This is okay since 1 character = 1 axis.</p>
     */
    static final String AXIS_ORDER_PARAM = "+axis=";

    /**
     * The character used for separating the Proj4 axis order declarations.
     */
    static final char AXIS_ORDER_SEPARATOR = ',';

    /**
     * Do not allow instantiation of this class.
     */
    private Proj4() {
    }

    /**
     * Returns the version number of the Proj4 library.
     * Returns {@code null} if Proj.4 is not installed on the current system.
     *
     * @return the Proj4 release string, or {@code null} if no installation has been found.
     */
    public static String getVersion() {
        try {
            return PJ.getVersion();
        } catch (UnsatisfiedLinkError e) {
            // Thrown the first time that we try to use the library.
            Logging.unexpectedException(Logging.getLogger(Modules.GDAL), Proj4.class, "version", e);
        } catch (NoClassDefFoundError e) {
            // Thrown on all attempts after the first one.
            Logging.recoverableException(Logging.getLogger(Modules.GDAL), Proj4.class, "version", e);
        }
        return null;
    }

    /**
     * Creates a new CRS from the given Proj4 definition string. The CRS can have an arbitrary number of dimensions
     * in the [2-100] range. However Proj.4 will handle at most the 3 first dimensions. All supplemental dimensions
     * will be simply copied unchanged by {@link org.opengis.referencing.operation.MathTransform} implementations.
     *
     * @param  crsId       the name of the CRS to create, or {@code null} if none.
     * @param  datumId     the name of the datum to create, or {@code null} if none.
     * @param  definition  the Proj.4 definition string.
     * @param  dimension   the number of dimension of the CRS to create.
     * @return a CRS created from the given definition string and number of dimension.
     * @throws NullPointerException if the definition string is {@code null}.
     * @throws FactoryException if one of the given argument has an invalid value.
     */
    public static CoordinateReferenceSystem createCRS(final ReferenceIdentifier crsId,
            final ReferenceIdentifier datumId, String definition, final int dimension)
            throws FactoryException
    {
        if ((definition = definition.trim()).isEmpty()) {
            throw new IllegalArgumentException("The definition must be non-empty.");
        }
        if (dimension < 2 || dimension > PJ.DIMENSION_MAX) {
            throw new IllegalArgumentException("Illegal number of dimensions: " + dimension);
        }
        /*
         * Custom parsing of the "+axis=" parameter.
         * This code may modify the definition string.
         */
        String orientation = null;
        int beginParam = definition.indexOf(AXIS_ORDER_PARAM);
        if (beginParam >= 0) {
            beginParam += AXIS_ORDER_PARAM.length();
            final int length = definition.length();
            while (beginParam < length) {                                   // Skip whitespaces.
                final int c = definition.codePointAt(beginParam);
                if (!Character.isWhitespace(c)) break;
                beginParam += Character.charCount(c);
            }
            final StringBuilder modified = new StringBuilder(definition.length());
            modified.append(definition, 0, beginParam);
            int endParam = CRS.Projected.findWordEnd(definition, beginParam);
            orientation = definition.substring(beginParam, endParam);
            modified.append(CRS.Projected.ensure3D(orientation));
            if (endParam < length && definition.charAt(endParam) == AXIS_ORDER_SEPARATOR) {
                endParam = CRS.Projected.findWordEnd(definition, endParam+1);
                orientation = definition.substring(beginParam, endParam);
            }
            modified.append(definition, endParam, length);
            definition = modified.toString();
        }
        /*
         * Create the Proj.4 wrapper.
         */
        final PJ datum = new PJ(datumId, definition);
        final PJ.Type type = datum.getType();
        final CoordinateReferenceSystem crs;
        switch (type) {
            case GEOCENTRIC: crs = new CRS.Geocentric(crsId, datum, dimension); break;
            case GEOGRAPHIC: crs = new CRS.Geographic(crsId, datum, dimension); break;
            case PROJECTED:  crs = new CRS.Projected (crsId, datum, dimension, orientation); break;
            default: throw new UnsupportedOperationException("Unknown CRS type: " + type);
        }
        return crs;
    }

    /**
     * Creates an operation for conversion or transformation between two coordinate reference systems.
     * This given source and target CRS must be instances created by this factory.
     *
     * @param  identifier  the name of the operation to create, or {@code null} if none.
     * @param  sourceCRS   the source coordinate reference system.
     * @param  targetCRS   the target coordinate reference system.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS.
     * @throws ClassCastException if the given CRS are not instances created by this class.
     */
    public static CoordinateOperation createOperation(final ReferenceIdentifier identifier,
            final CoordinateReferenceSystem sourceCRS, final CoordinateReferenceSystem targetCRS)
            throws ClassCastException
    {
        return new Operation(identifier, (CRS) sourceCRS, (CRS) targetCRS);
    }

    /**
     * Returns the exception to throw when a feature is not yet supported.
     */
    static FactoryException unsupportedOperation() {
        return new FactoryException("Not supported yet.");
    }
}
