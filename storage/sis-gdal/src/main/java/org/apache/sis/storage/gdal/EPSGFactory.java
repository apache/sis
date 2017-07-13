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

import java.util.Set;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.metadata.citation.Citation;

import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.AbstractFactory;


/**
 * A factory for {@linkplain CoordinateReferenceSystem Coordinate Reference System} objects created from EPSG codes.
 * While this factory is primarily designed for EPSG codes, it accepts also any other codespaces supported by the
 * Proj.4 library.
 *
 * <p>The main methods in this class are:</p>
 * <ul>
 *   <li>{@link #getAuthority()}</li>
 *   <li>{@link #createCoordinateReferenceSystem(String)}</li>
 * </ul>
 *
 * The following methods delegate to {@link #createCoordinateReferenceSystem(String)} and cast
 * the result if possible, or throw a {@link FactoryException} otherwise.
 * <ul>
 *   <li>{@link #createGeographicCRS(String)}</li>
 *   <li>{@link #createGeocentricCRS(String)}</li>
 *   <li>{@link #createProjectedCRS(String)}</li>
 *   <li>{@link #createObject(String)}</li>
 * </ul>
 *
 * All other methods are not supported by the default implementation of this factory.
 * However those methods will work if the {@link #createCoordinateReferenceSystem(String)}
 * method is overridden in order to return CRS objects of the appropriate type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class EPSGFactory extends AbstractFactory implements CRSAuthorityFactory {
    /**
     * {@code true} if the CRS created by this factory should use the axis order declared by the EPSG database.
     */
    private final boolean useEpsgAxisOrder;

    /**
     * The set of all EPSG codes known to Proj.4, created when first needed.
     */
    private Set<String> codes;

    /**
     * Creates a new coordinate operation factory. Whether the factory will follow
     * EPSG axis order or not is specified by the given {@code useEpsgAxisOrder} argument.
     *
     * @param useEpsgAxisOrder {@code true} if the CRS created by this factory should
     *        use the axis order declared by the EPSG database, or {@code false} for
     *        the Proj.4 axis order. The default value is {@code true}.
     */
    EPSGFactory(final boolean useEpsgAxisOrder) {
        this.useEpsgAxisOrder = useEpsgAxisOrder;
    }

    /**
     * Returns the authority for this factory, which is EPSG.
     */
    @Override
    public Citation getAuthority() {
        return useEpsgAxisOrder ? Citations.EPSG : Citations.PROJ4;
    }

    /**
     * Returns the authority codes.
     *
     * @throws FactoryException if an error occurred while fetching the authority codes.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
        if (codes == null) {
            codes = Collections.unmodifiableSet(ResourcesLoader.getAxisOrientations().keySet());
        }
        return codes;
    }

    /**
     * Returns the name of the CRS identified by the given code. The default implementation
     * returns a non-null value only for a few common codes.
     *
     * @throws FactoryException if an error occurred while fetching the description.
     */
    @Override
    public InternationalString getDescriptionText(final String code) throws FactoryException {
        final String name = getName(code, null, false);
        return (name != null) ? new SimpleInternationalString(code) : null;
    }

    /**
     * Returns a hard-coded name for the given code, or {@code null} if none.
     * Only the most frequent CRS are recognized by this method.
     *
     * @param isDatum  {@code false} for creating a CRS name (the usual case), or
     *                 {@code true} for creating a datum name.
     */
    private static String getName(String code, final String defaultValue, final boolean isDatum) {
        final int s = code.indexOf(':');
        if (s<0 || code.substring(0,s).trim().equalsIgnoreCase("epsg")) try {
            switch (Integer.parseInt(code.substring(s+1).trim())) {
                case 4326: return isDatum ? "World Geodetic System 1984" : "WGS 84";
            }
        } catch (NumberFormatException e) {
            // Ignore - this is okay for this method contract.
        }
        return defaultValue;
    }

    /**
     * Creates a new CRS from the given code. If the given string is of the form {@code "AUTHORITY:CODE"},
     * then any authority recognized by the Proj.4 library will be accepted (it doesn't need to be EPSG).
     * If no authority is given, then {@code "EPSG:"} is assumed.
     *
     * @param  code  the code of the CRS object to create.
     * @return a CRS created from the given code.
     * @throws FactoryException if the CRS object can not be created for the given code.
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(String code) throws FactoryException {
        String codespace = "EPSG";
        code = code.trim();
        final int s = code.indexOf(':');
        if (s >= 0) {
            codespace = code.substring(0, s).trim();
            code = code.substring(s+1).trim();
        }
        int dimension = 2;
        final StringBuilder definition = new StringBuilder(40);
        definition.append("+init=").append(codespace).append(':').append(code);
        if (useEpsgAxisOrder) {
            /*
             * If the user asked to honor the EPSG axis definitions, get the axis orientation
             * from the "axis-orientations.txt" file.   This may be a comma-separated list if
             * there is also a definition for the base CRS. It may be 2 or 3 characters long.
             * The number of characters determine the number of dimensions. However this will
             * have to be adjusted before to be given to Proj.4 since the later expects
             * exactly 3 characters.
             */
            String orientation = ResourcesLoader.getAxisOrientations().get(code);
            if (orientation != null) {
                definition.append(' ').append(Proj4.AXIS_ORDER_PARAM).append(orientation);
                final int end = orientation.indexOf(Proj4.AXIS_ORDER_SEPARATOR);
                dimension = (end >= 0) ? end : orientation.length();
            }
        }
        final String crsName   = getName(code, code,   false);
        final String datumName = getName(code, crsName, true);
        final ReferenceIdentifier crsId = new ImmutableIdentifier(null, codespace, crsName);
        final ReferenceIdentifier datumId = datumName.equals(crsName) ? crsId : new ImmutableIdentifier(null, codespace, datumName);
        try {
            return Proj4.createCRS(crsId, datumId, definition.toString(), dimension);
        } catch (IllegalArgumentException e) {
            throw new NoSuchAuthorityCodeException(e.getMessage(), codespace, code);
        }
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public GeographicCRS createGeographicCRS(String code) throws FactoryException {
        return cast(GeographicCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public GeocentricCRS createGeocentricCRS(String code) throws FactoryException {
        return cast(GeocentricCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public ProjectedCRS createProjectedCRS(String code) throws FactoryException {
        return cast(ProjectedCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public CompoundCRS createCompoundCRS(String code) throws FactoryException {
        return cast(CompoundCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public DerivedCRS createDerivedCRS(String code) throws FactoryException {
        return cast(DerivedCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public EngineeringCRS createEngineeringCRS(String code) throws FactoryException {
        return cast(EngineeringCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public ImageCRS createImageCRS(String code) throws FactoryException {
        return cast(ImageCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public TemporalCRS createTemporalCRS(String code) throws FactoryException {
        return cast(TemporalCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public VerticalCRS createVerticalCRS(String code) throws FactoryException {
        return cast(VerticalCRS.class, code);
    }

    /**
     * Delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     *
     * @throws FactoryException if {@code createCoordinateReferenceSystem(code)} failed.
     */
    @Override
    public IdentifiedObject createObject(String code) throws FactoryException {
        return createCoordinateReferenceSystem(code);
    }

    /**
     * Invokes {@link #createCoordinateReferenceSystem(String)} and casts the result
     * to the given type. If the result can not be casted, a factory exception is thrown.
     */
    private <T extends CoordinateReferenceSystem> T cast(final Class<T> type, final String code) throws FactoryException {
        final CoordinateReferenceSystem crs = createCoordinateReferenceSystem(code);
        try {
            return type.cast(crs);
        } catch (ClassCastException e) {
            throw new FactoryException("The \"" + code + "\" object is not a " + type.getSimpleName(), e);
        }
    }
}
