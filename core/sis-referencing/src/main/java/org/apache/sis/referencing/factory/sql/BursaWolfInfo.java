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
package org.apache.sis.referencing.factory.sql;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.sql.ResultSet;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.converter.ConversionException;
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.internal.referencing.ExtentSelector;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.TimeDependentBWP;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Units;


/**
 * Private structure for {@link EPSGDataAccess#createBursaWolfParameters(Integer, ResultSet)} usage.
 * Those information are for compatibility with <cite>Well Known Text</cite> (WKT) version 1 formatting.
 * That legacy format had a {@code TOWGS84} element which needs the information provided by this class.
 * Note that {@code TOWGS84} is a deprecated element as of WKT 2 (ISO 19162).
 *
 * <p><b>Note:</b> this class contains many hard-coded EPSG codes relative to Bursa-Wolf parameters.</p>
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see BursaWolfParameters
 * @see TimeDependentBWP
 */
final class BursaWolfInfo {
    // See org.apache.sis.measure.Units.valueOfEPSG(int) for hard-coded units from EPSG codes.
    // See TableInfo.EPSG for hard-coded table names, column names and GeoAPI types.

    /**
     * The target CRS for which to collect Bursa-Wolf parameters. Apache SIS accepts an arbitrary amount of targets,
     * but the {@code TOWGS84} element only needs the parameters toward the EPSG:4326 coordinate reference system.
     * For now we fix the number of target CRS to only 1, but we can increase that amount in a future SIS version
     * if needed. However it is better to restrict the target CRS to those that use a world-wide datum only.
     */
    static final int TARGET_CRS = 4326;

    /**
     * The datum of {@link #TARGET_CRS}.
     */
    static final int TARGET_DATUM = 6326;

    /** First Bursa-Wolf method. */ static final int MIN_METHOD_CODE = 9603;
    /** Last Bursa-Wolf method.  */ static final int MAX_METHOD_CODE = 9607;
    /** Rotation frame method.   */ private static final int ROTATION_FRAME_CODE = 9607;
    //* Time-dependent rotation. */ private static final int ROTATION_TMDEP_CODE = 1056;

    /**
     * Sets a Bursa-Wolf parameter from an EPSG parameter.
     * This method recognizes only the parameters that do not depend on time (EPSG:8605 to 8611).
     * This method does not recognize the time-dependent parameters (EPSG:1040 to 1046) because
     * they are not used in WKT 1 {@code TOWGS84} elements.
     *
     * @param  parameters The Bursa-Wolf parameters to modify.
     * @param  code       The EPSG code for a parameter from the [PARAMETER_CODE] column.
     * @param  value      The value of the parameter from the [PARAMETER_VALUE] column.
     * @param  unit       The unit of the parameter value from the [UOM_CODE] column.
     * @param  locale     The locale, used only if an error message need to be formatted.
     * @throws FactoryDataException if the code is unrecognized.
     */
    static void setBursaWolfParameter(final BursaWolfParameters parameters, final int code,
            double value, final Unit<?> unit, final Locale locale) throws FactoryDataException
    {
        Unit<?> target = unit;
        if (code >= 8605) {
            if      (code <= 8607) target = SI   .METRE;
            else if (code <= 8610) target = NonSI.SECOND_ANGLE;
            else if (code == 8611) target = Units.PPM;
        }
        if (target != unit) try {
            value = unit.getConverterToAny(target).convert(value);
        } catch (ConversionException e) {
            throw new FactoryDataException(Errors.getResources(locale).getString(Errors.Keys.IncompatibleUnit_1, unit), e);
        }
        switch (code) {
            case 8605: parameters.tX = value; break;
            case 8606: parameters.tY = value; break;
            case 8607: parameters.tZ = value; break;
            case 8608: parameters.rX = value; break;
            case 8609: parameters.rY = value; break;
            case 8610: parameters.rZ = value; break;
            case 8611: parameters.dS = value; break;
            default: throw new FactoryDataException(Errors.getResources(locale)
                                .getString(Errors.Keys.UnexpectedParameter_1, code));
        }
    }

    /**
     * The value of {@code COORD_OP_CODE}.
     */
    final int operation;

    /**
     * The value of {@code COORD_OP_METHOD_CODE}.
     */
    final int method;

    /**
     * The target datum inferred from value of {@code TARGET_CRS_CODE}.
     */
    final int target;

    /**
     * The value of {@code AREA_OF_USE_CODE}.
     */
    private final int domainOfValidity;

    /**
     * The domain of validity as an {@code Extent} object.
     */
    private Extent extent;

    /**
     * Fills a structure with the specified values.
     */
    BursaWolfInfo(final int operation, final int method, final int targetCRS, final int domainOfValidity) {
        this.operation        = operation;
        this.method           = method;
        this.domainOfValidity = domainOfValidity;
        switch (targetCRS) {
            case TARGET_CRS: target = TARGET_DATUM; break;
            // More codes may be added in future SIS version.
            default: throw new IllegalArgumentException(String.valueOf(targetCRS));
        }
    }

    /**
     * Returns {@code true} if this operation is a frame rotation.
     * Frame rotations methods are:
     *
     * <ul>
     *   <li>EPSG:9607 for the operation that does not depend on time.</li>
     *   <li>EPSG:1056 for the time-dependent operation (not handled by this class).</li>
     * </ul>
     */
    boolean isFrameRotation() {
        return method == ROTATION_FRAME_CODE;
    }

    /**
     * MUST returns the operation code. This is required by {@link EPSGDataAccess#sort(Object[])}.
     */
    @Override
    public String toString() {
        return String.valueOf(operation);
    }

    /**
     * Gets the domain of validity. The result is cached.
     *
     * @param factory The factory to use for creating {@code Extent} instances.
     */
    Extent getDomainOfValidity(final GeodeticAuthorityFactory factory) throws FactoryException {
        if (extent == null && domainOfValidity != 0) {
            extent = factory.createExtent(String.valueOf(domainOfValidity));
        }
        return extent;
    }

    /**
     * Given an array of {@code BursaWolfInfo} instances, retains only the instances having the largest
     * domain of validity for each target datum. If two instances have the same domain of validity, the
     * first one is retained. This presume that the instances have already been sorted for preference order
     * before to invoke this method.
     *
     * @param factory     The factory to use for creating {@code Extent} instances.
     * @param candidates  The Bursa-Wolf parameters candidates.
     * @param addTo       Where to add the instances retained by this method.
     */
    static void filter(final GeodeticAuthorityFactory factory, final BursaWolfInfo[] candidates,
            final List<BursaWolfInfo> addTo) throws FactoryException
    {
        final Map<Integer,ExtentSelector<BursaWolfInfo>> added = new LinkedHashMap<Integer,ExtentSelector<BursaWolfInfo>>();
        for (BursaWolfInfo candidate : candidates) {
            final Integer target = candidate.target;
            ExtentSelector<BursaWolfInfo> selector = added.get(target);
            if (selector == null) {
                selector = new ExtentSelector<BursaWolfInfo>(null);
                added.put(target, selector);
            }
            selector.evaluate(candidate.getDomainOfValidity(factory), candidate);
        }
        for (final ExtentSelector<BursaWolfInfo> select : added.values()) {
            addTo.add(select.best());
        }
    }
}
