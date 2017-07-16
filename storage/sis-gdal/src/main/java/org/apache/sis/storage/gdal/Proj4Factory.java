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

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.measure.Units;


/**
 * A factory for Coordinate Reference Systems created from {@literal Proj.4} definitions.
 * This authority factory recognizes codes in the {@code "Proj4"} name space.
 * The main methods in this class are:
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
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class Proj4Factory extends GeodeticAuthorityFactory implements CRSAuthorityFactory {
    /**
     * The {@literal Proj.4} parameter used for declaration of axis order.  Proj.4 expects the axis parameter
     * to be exactly 3 characters long, but Apache SIS accepts 2 characters as well. We relax the Proj.4 rule
     * because we use the number of characters for determining the number of dimensions.
     * This is okay since 1 character = 1 axis.
     */
    static final String AXIS_ORDER_PARAM = "+axis=";

    /**
     * The default factory instance.
     */
    static final Proj4Factory INSTANCE = new Proj4Factory();

    /**
     * The factory for coordinate reference system objects.
     */
    private final CRSFactory crsFactory;

    /**
     * The factory for coordinate system objects.
     */
    private final CSFactory csFactory;

    /**
     * The factory for datum objects.
     */
    private final DatumFactory datumFactory;

    /**
     * The factory for coordinate operation objects.
     * Currently restricted to Apache SIS implementation because we use a method not yet available in GeoAPI.
     */
    private final DefaultCoordinateOperationFactory opFactory;

    /**
     * Poll of identifiers created by this factory.
     */
    private final Map<String,Identifier> identifiers = new HashMap<>();

    /**
     * Pool of {@literal Proj.4} objects created so far. The keys are the Proj.4 definition strings.
     * The same {@link PJ} instance may appear more than once if various definition strings resulted
     * in the same Proj.4 object.
     */
    private final WeakValueHashMap<String,PJ> pool = new WeakValueHashMap<>(String.class);

    /**
     * Creates a new {@literal Proj.4} factory using the default CRS, CS and datum factories.
     */
    public Proj4Factory() {
        crsFactory   = DefaultFactories.forBuildin(CRSFactory.class);
        csFactory    = DefaultFactories.forBuildin(CSFactory.class);
        datumFactory = DefaultFactories.forBuildin(DatumFactory.class);
        opFactory    = DefaultFactories.forBuildin(CoordinateOperationFactory.class, DefaultCoordinateOperationFactory.class);
    }

    /**
     * Creates a new {@literal Proj.4} factory using the specified CRS, CS and datum factories.
     *
     * @param crsFactory    the factory to use for creating Coordinate Reference Systems.
     * @param csFactory     the factory to use for creating Coordinate Systems.
     * @param datumFactory  the factory to use for creating Geodetic Datums.
     * @param opFactory     the factory to use for creating Coordinate Operations.
     *                      Current implementation requires an instance of {@link DefaultCoordinateOperationFactory},
     *                      but this may be related in a future Apache SIS version.
     */
    public Proj4Factory(final CRSFactory   crsFactory,
                        final CSFactory    csFactory,
                        final DatumFactory datumFactory,
                        final CoordinateOperationFactory opFactory)
    {
        ArgumentChecks.ensureNonNull("crsFactory",   crsFactory);
        ArgumentChecks.ensureNonNull("csFactory",    csFactory);
        ArgumentChecks.ensureNonNull("datumFactory", datumFactory);
        ArgumentChecks.ensureNonNull("opFactory",    opFactory);
        this.crsFactory   = crsFactory;
        this.csFactory    = csFactory;
        this.datumFactory = datumFactory;
        this.opFactory    = (DefaultCoordinateOperationFactory) opFactory;
    }

    /**
     * Returns the project that defines the codes recognized by this factory.
     * The authority determines the {@linkplain #getCodeSpaces() code space}.
     *
     * @return {@link Citations#PROJ4}.
     */
    @Override
    public Citation getAuthority() {
        return Citations.PROJ4;
    }

    /**
     * Returns the code space of the authority. The code space is the prefix that may appear before codes.
     * It allows to differentiate Proj.4 definitions from EPSG codes or other authorities. The code space is
     * removed by {@link #createCoordinateReferenceSystem(String)} before the definition string is passed to Proj.4
     *
     * <div class="note"><b>Example</b>
     * a complete identifier can be {@code "Proj4:+init=epsg:4326"}.
     * Note that this is <strong>not</strong> equivalent to the standard {@code "EPSG:4326"} definition since the
     * axis order is not the same. The {@code "Proj4:"} prefix specifies that the remaining part of the string is
     * a Proj.4 definition; the presence of an {@code "epsg"} word in the definition does not change that fact.
     * </div>
     *
     * @return {@code "Proj4"}.
     */
    @Override
    public Set<String> getCodeSpaces() {
        return Collections.singleton(Constants.PROJ4);
    }

    /**
     * Returns the set of authority codes for objects of the given type.
     * Current implementation can not return complete Proj.4 definition strings.
     * Instead, this method currently returns only fragments (e.g. {@code "+init="}).
     *
     * @param  type  the spatial reference objects type.
     * @return fragments of definition strings for spatial reference objects of the given type.
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
        final String method;
        if (type.isAssignableFrom(ProjectedCRS.class)) {                // Must be tested first.
            method = "";
        } else if (type.isAssignableFrom(GeographicCRS.class)) {        // Should be tested before GeocentricCRS.
            method = "latlon";
        } else if (type.isAssignableFrom(GeocentricCRS.class)) {
            method = "geocent";
        } else {
            return Collections.emptySet();
        }
        final Set<String> codes = new LinkedHashSet<>(4);
        codes.add("+init=");
        codes.add("+proj=".concat(method));
        return codes;
    }

    /**
     * Creates a new geodetic object from the given {@literal Proj.4} definition.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)}.
     *
     * @param  code  the Proj.4 definition of the geodetic object to create.
     * @return a geodetic created from the given definition.
     * @throws FactoryException if the geodetic object can not be created for the given definition.
     */
    @Override
    public IdentifiedObject createObject(String code) throws FactoryException {
        return createCoordinateReferenceSystem(code);
    }

    /**
     * Creates a new CRS from the given {@literal Proj.4} definition.
     * The {@code "Proj4:"} prefix (ignoring case), if present, is ignored.
     *
     * <div class="section">Apache SIS extension</div>
     * Proj.4 unconditionally requires 3 letters for the {@code "+axis="} parameter — for example {@code "neu"} for
     * <cite>North</cite>, <cite>East</cite> and <cite>Up</cite> respectively — regardless the number of dimensions
     * in the CRS to create. Apache SIS makes the vertical direction optional:
     *
     * <ul>
     *   <li>If the vertical direction is present (as in {@code "neu"}), a three-dimensional CRS is created.</li>
     *   <li>If the vertical direction is absent (as in {@code "ne"}), a two-dimensional CRS is created.</li>
     * </ul>
     *
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>{@code "+init=epsg:4326"} (<strong>not</strong> equivalent to the standard EPSG::4326 definition)</li>
     *   <li>{@code "+proj=latlong +datum=WGS84 +ellps=WGS84 +towgs84=0,0,0"} (default to two-dimensional CRS)</li>
     *   <li>{@code "+proj=latlon +a=6378137.0 +b=6356752.314245179 +pm=0.0 +axis=ne"} (explicitely two-dimensional)</li>
     *   <li>{@code "+proj=latlon +a=6378137.0 +b=6356752.314245179 +pm=0.0 +axis=neu"} (three-dimensional)</li>
     * </ul>
     * </div>
     *
     * @param  code  the Proj.4 definition of the CRS object to create.
     * @return a CRS created from the given definition.
     * @throws FactoryException if the CRS object can not be created for the given definition.
     *
     * @see Proj4#createCRS(String, int)
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(String code) throws FactoryException {
        code = trimNamespace(code);
        boolean hasHeight = false;
        /*
         * Count the number of axes declared in the "+axis" parameter.
         * If there is only two axes, add a 'u' (up) direction even in the two-dimensional case.
         * We make this addition because Proj.4 seems to require the 3-letters code in all case.
         */
        int offset = code.indexOf(AXIS_ORDER_PARAM);
        if (offset >= 0) {
            offset += AXIS_ORDER_PARAM.length();
            final CharSequence orientation = CharSequences.token(code, offset);
            for (int i=orientation.length(); --i >= 0;) {
                final char c = orientation.charAt(i);
                hasHeight = (c == 'u' || c == 'd');
                if (hasHeight) break;
            }
            if (!hasHeight && orientation.length() < 3) {
                offset = code.indexOf(orientation.toString(), offset);
                if (offset >= 0) {                          // Should never be -1, but we are paranoiac.
                    offset += orientation.length();
                    code = new StringBuilder(code).insert(offset, 'u').toString();
                }
            }
        }
        return createCRS(code, hasHeight);
    }

    /**
     * Returns the value of the given parameter as an identifier, or {@code null} if none.
     * The given parameter key shall include the {@code '+'} prefix and {@code '='} suffix,
     * for example {@code "+proj="}. This is a helper method for providing the {@code name}
     * property value in constructors.
     *
     * @param  definition  the Proj.4 definition string to parse.
     * @param  keyword     the parameter name.
     * @return the parameter value as an identifier.
     */
    private Map<String,Identifier> identifier(final String definition, final String keyword) {
        String value = "";
        if (keyword != null) {
            int i = definition.indexOf(keyword);
            if (i >= 0) {
                i += keyword.length();
                final int stop = definition.indexOf(' ', i);
                value = (stop >= 0) ? definition.substring(i, stop) : definition.substring(i);
                value = value.trim();
            }
        }
        if (value.isEmpty()) {
            value = "Unnamed";
        }
        return identifier(value);
    }

    /**
     * Returns the identifier for the given code in {@literal Proj.4} namespace.
     */
    private Map<String,Identifier> identifier(final String code) {
        Identifier id = identifiers.get(code);
        if (id == null) {
            short i18n = 0;
            if (code.equalsIgnoreCase("Unnamed")) i18n = Vocabulary.Keys.Unnamed;
            if (code.equalsIgnoreCase("Unknown")) i18n = Vocabulary.Keys.Unknown;
            id = new ImmutableIdentifier(Citations.PROJ4, Constants.PROJ4, code, null,
                    (i18n != 0) ? Vocabulary.formatInternational(i18n) : null);
            identifiers.put(code, id);
        }
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, id);
    }

    /**
     * Creates a geodetic datum from the given {@literal Proj.4} wrapper.
     *
     * @param  pj  the Proj.4 object to wrap.
     */
    private GeodeticDatum createDatum(final PJ pj) throws FactoryException {
        final PrimeMeridian pm;
        final double greenwichLongitude = pj.getGreenwichLongitude();
        if (greenwichLongitude == 0) {
            pm = CommonCRS.WGS84.datum().getPrimeMeridian();
        } else {
            pm = datumFactory.createPrimeMeridian(identifier("Unnamed"), greenwichLongitude, Units.DEGREE);
        }
        final String definition = pj.getCode();
        return datumFactory.createGeodeticDatum(identifier(definition, "+datum="),
               datumFactory.createEllipsoid    (identifier(definition, "+ellps="),
                    pj.getSemiMajorAxis(), pj.getSemiMinorAxis(), Units.METRE), pm);
    }

    /**
     * Creates a coordinate reference system from the given {@literal Proj.4} wrapper.
     * The given {@code pj} will be stored as the CRS identifier.
     *
     * @param  pj          the Proj.4 object to wrap.
     * @param  withHeight  whether to include a height axis.
     */
    private CoordinateReferenceSystem createCRS(final PJ pj, final boolean withHeight) throws FactoryException {
        final PJ.Type type = pj.getType();
        final boolean geographic = PJ.Type.GEOGRAPHIC.equals(type);
        final char[] dir = pj.getAxisDirections();
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[withHeight ? dir.length : 2];
        for (int i=0; i<axes.length; i++) {
            final char d = Character.toLowerCase(dir[i]);
            char abbreviation = Character.toUpperCase(d);
            boolean vertical = false;
            final AxisDirection c;
            final String name;
            switch (d) {
                case 'e': c = AxisDirection.EAST;  name = geographic ? "Geodetic longitude" : "Easting";  break;
                case 'w': c = AxisDirection.WEST;  name = geographic ? "Geodetic longitude" : "Westing";  break;
                case 'n': c = AxisDirection.NORTH; name = geographic ? "Geodetic latitude"  : "Northing"; break;
                case 's': c = AxisDirection.SOUTH; name = geographic ? "Geodetic latitude"  : "Southing"; break;
                case 'u': c = AxisDirection.UP;    name = "Height";  vertical = true; abbreviation = 'h'; break;
                case 'd': c = AxisDirection.DOWN;  name = "Depth";   vertical = true; break;
                default:  c = AxisDirection.OTHER; name = "Unknown"; break;
            }
            if (geographic && AxisDirections.isCardinal(c)) {
                abbreviation = (d == 'e' || d == 'w') ? 'λ' : 'φ';
            }
            final Unit<?> unit = (vertical || !geographic) ? Units.METRE.divide(pj.getLinearUnitToMetre(vertical)) : Units.DEGREE;
            axes[i] = csFactory.createCoordinateSystemAxis(identifier(name), String.valueOf(abbreviation).intern(), c, unit);
        }
        /*
         * At this point we got the coordinate system axes. Now create the CRS. The given Proj.4 object
         * will be stored as the CRS identifier for allowing OperationFactory to get it back before to
         * attempt to create a new one for a given CRS.
         */
        final Map<String,Identifier> csName = identifier("Unnamed");
        final Map<String,Identifier> name = new HashMap<>(identifier(String.valueOf(pj.getDescription())));
        name.put(CoordinateReferenceSystem.IDENTIFIERS_KEY, pj);
        switch (type) {
            case GEOGRAPHIC: {
                return crsFactory.createGeographicCRS(name, createDatum(pj), withHeight ?
                        csFactory.createEllipsoidalCS(csName, axes[0], axes[1], axes[2]) :
                        csFactory.createEllipsoidalCS(csName, axes[0], axes[1]));
            }
            case GEOCENTRIC: {
                return crsFactory.createGeocentricCRS(name, createDatum(pj),
                        csFactory.createCartesianCS(csName, axes[0], axes[1], axes[2]));
            }
            case PROJECTED: {
                final PJ base = unique(new PJ(pj));
                final CoordinateReferenceSystem baseCRS = createCRS(base, withHeight);
                final Transform tr = new Transform(pj, withHeight, base, withHeight);
                /*
                 * Try to convert the Proj.4 parameters into OGC parameters in order to have a less opaque structure.
                 * Failure to perform this conversion will not cause a failure to create the ProjectedCRS. After all,
                 * maybe the user invokes this method for using a map projection not yet supported by Apache SIS.
                 * Instead, fallback on the more opaque Transform.METHOD description. Apache SIS will not be able to
                 * perform analysis on those parameters, but it will not prevent the Proj.4 transformation to work.
                 */
                OperationMethod method;
                ParameterValueGroup parameters;
                try {
                    final Proj4Parser parser = new Proj4Parser(pj.getCode());
                    method = parser.method(opFactory);
                    parameters = parser.parameters();
                } catch (IllegalArgumentException | FactoryException e) {
                    Logging.recoverableException(Logging.getLogger(Modules.GDAL), Proj4Factory.class, "createProjectedCRS", e);
                    method = Transform.METHOD;
                    parameters = null;              // Will let Apache SIS infers the parameters from the Transform instance.
                }
                final Conversion fromBase = new DefaultConversion(name, method, tr, parameters);
                return crsFactory.createProjectedCRS(name, (GeographicCRS) baseCRS, fromBase, withHeight ?
                        csFactory.createCartesianCS(csName, axes[0], axes[1], axes[2]) :
                        csFactory.createCartesianCS(csName, axes[0], axes[1]));
            }
            default: {
                throw new FactoryException(Errors.format(Errors.Keys.UnknownEnumValue_2, type, PJ.Type.class));
            }
        }
    }

    /**
     * Gets the {@literal Proj.4} object from the given coordinate reference system. If an existing {@code PJ}
     * instance is found, returns it. Otherwise creates a new {@code PJ} instance from a Proj.4 definition
     * inferred from the given CRS. This method is the converse of {@link #createCRS(PJ, boolean)}.
     */
    private PJ unwrapOrCreate(final CoordinateReferenceSystem crs) throws FactoryException {
        for (final Identifier id : crs.getIdentifiers()) {
            if (id instanceof PJ) {
                return (PJ) id;
            }
        }
        return unique(new PJ(Proj4.definition(crs)));
    }

    /**
     * Returns a unique instance of the given {@literal Proj.4} object.
     */
    @SuppressWarnings("FinalizeCalledExplicitly")
    private PJ unique(PJ pj) {
        final PJ existing = pool.putIfAbsent(pj.getCode(), pj);
        if (existing != null) {
            pj.finalize();          // Release Proj.4 resources.
            return existing;
        }
        return pj;
    }

    /**
     * Creates a coordinate reference system from the given {@literal Proj.4} definition string.
     * The {@code Proj4} suffix shall have been removed before to invoke this method.
     *
     * @param  definition  the Proj.4 definition.
     * @param  withHeight  whether to include a height axis.
     *
     * @see Proj4#createCRS(String, int)
     * @see #createCoordinateReferenceSystem(String)
     */
    final CoordinateReferenceSystem createCRS(final String definition, final boolean withHeight) throws FactoryException {
        PJ pj = pool.get(definition);
        if (pj == null) {
            pj = unique(new PJ(definition));
            pool.putIfAbsent(definition, pj);
        }
        return createCRS(pj, withHeight);
    }

    /**
     * Creates an operation for conversion or transformation between two coordinate reference systems.
     * This implementation always uses Proj.4 for performing the coordinate operations, regardless if
     * the given CRS were created from a Proj.4 definition string or not. This method fails if it can
     * not map the given CRS to Proj.4 structures.
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS.
     * @throws FactoryException if the given CRS are not instances recognized by this class.
     *
     * @see Proj4#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)
     */
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        Identifier id;
        String src = null, tgt = null, name = "Unnamed";
        if ((id = sourceCRS.getName()) != null) src = id.getCode();
        if ((id = targetCRS.getName()) != null) tgt = id.getCode();
        if (src != null || tgt != null) {
            final StringBuilder buffer = new StringBuilder();
            if (src != null) buffer.append("From ").append(src);
            if (tgt != null) buffer.append(buffer.length() == 0 ? "To " : " to ").append(tgt);
            name = buffer.toString();
        }
        final Transform tr = new Transform(unwrapOrCreate(sourceCRS), is3D("sourceCRS", sourceCRS),
                                           unwrapOrCreate(targetCRS), is3D("targetCRS", targetCRS));
        return opFactory.createSingleOperation(identifier(name), sourceCRS, targetCRS, null, Transform.METHOD, tr);
    }

    /**
     * Returns whether the given CRS is three-dimensional.
     * Thrown an exception if the number of dimension is unsupported.
     */
    private static boolean is3D(final String arg, final CoordinateReferenceSystem crs) throws FactoryException {
        final int dim = crs.getCoordinateSystem().getDimension();
        final boolean is3D = (dim >= 3);
        if (dim < 2 || dim > 3) {
            throw new FactoryException(Errors.format(Errors.Keys.MismatchedDimension_3, arg, is3D ? 3 : 2, dim));
        }
        return is3D;
    }
}
