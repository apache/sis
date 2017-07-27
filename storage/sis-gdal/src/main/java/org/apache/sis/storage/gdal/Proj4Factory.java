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
import javax.measure.format.ParserException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.LazySet;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.measure.Units;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Predicate;


/**
 * A factory for Coordinate Reference Systems created from {@literal Proj.4} definitions.
 * This authority factory recognizes codes in the {@code "Proj4"} name space.
 * The main methods in this class are:
 * <ul>
 *   <li>{@link #getAuthority()}</li>
 *   <li>{@link #createCoordinateReferenceSystem(String)}</li>
 *   <li>{@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)}</li>
 *   <li>{@link #createParameterizedTransform(ParameterValueGroup)}</li>
 * </ul>
 *
 * Other methods delegate to one of above-cited methods if possible, or throw a {@link FactoryException} otherwise.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class Proj4Factory extends GeodeticAuthorityFactory implements CRSAuthorityFactory {
    /*
     * NOTE: Proj4Factory could implement CoordinateOperationFactory or MathTransformFactory.
     *       But we don't do that yet because we are not sure about exposing large amount of
     *       methods that are not really supported by Proj.4 wrappers. However this approach
     *       is experimented in the MTFactory class in the test directory. MTFactory methods
     *       could be refactored here if experience shows us that it would be useful.
     */

    /**
     * The {@literal Proj.4} parameter used for projection name.
     */
    static final String PROJ_PARAM = '+' + Proj4Parser.PROJ + '=';

    /**
     * The {@literal Proj.4} parameter used for declaration of axis order.  Proj.4 expects the axis parameter
     * to be exactly 3 characters long, but Apache SIS accepts 2 characters as well. We relax the Proj.4 rule
     * because we use the number of characters for determining the number of dimensions.
     * This is okay since 1 character = 1 axis.
     */
    static final String AXIS_ORDER_PARAM = "+axis=";

    /**
     * The name to use when no specific name were found for an object.
     */
    static final String UNNAMED = "Unnamed";

    /**
     * The default factory instance.
     */
    static final Proj4Factory INSTANCE = new Proj4Factory();

    /**
     * The {@literal Proj.4} authority completed by the version string. Created when first needed.
     *
     * @see #getAuthority()
     */
    private volatile Citation authority;

    /**
     * The default properties, or an empty map if none. This map shall not change after construction in
     * order to allow usage without synchronization in multi-thread context. But we do not need to wrap
     * in a unmodifiable map since {@code Proj4Factory} does not provide public access to it.
     */
    private final Map<String,?> defaultProperties;

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
     * The {@code MathTransform} factory on which to delegate operations that are not supported by {@code Proj4Factory}.
     */
    private final MathTransformFactory mtFactory;

    /**
     * The factory for coordinate operation objects, created when first needed.
     * Currently restricted to Apache SIS implementation because we use a method not yet available in GeoAPI and
     * because we configure it for using the {@link MathTransformFactory} provided by this {@code Proj4Factory}.
     *
     * @see #opFactory()
     */
    private volatile DefaultCoordinateOperationFactory opFactory;

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
     * Creates a default factory.
     */
    public Proj4Factory() {
        this(null);
    }

    /**
     * Creates a new {@literal Proj.4} factory. The {@code properties} argument is an optional map
     * for specifying common properties shared by the objects to create. Some available properties are
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#AbstractIdentifiedObject(Map) listed there}.
     * Unknown properties, or properties that do not apply, or properties for which {@code Proj4Factory} supplies
     * itself a value, are ignored.
     *
     * @param properties  common properties for the objects to create, or {@code null} if none.
     */
    public Proj4Factory(Map<String,?> properties) {
        properties   = new HashMap(properties != null ? properties : Collections.emptyMap());
        crsFactory   = factory(CRSFactory.class,           properties, ReferencingServices.CRS_FACTORY);
        csFactory    = factory(CSFactory.class,            properties, ReferencingServices.CS_FACTORY);
        datumFactory = factory(DatumFactory.class,         properties, ReferencingServices.DATUM_FACTORY);
        mtFactory    = factory(MathTransformFactory.class, properties, ReferencingServices.MT_FACTORY);
        defaultProperties = CollectionsExt.compact(properties);
    }

    /**
     * Returns the factory to use, using the instance specified in the properties map if it exists,
     * or the system-wide default instance otherwise.
     */
    @SuppressWarnings("unchecked")
    private static <T> T factory(final Class<T> type, final Map<String,?> properties, final String key) {
        final Object value = properties.remove(key);
        if (value == null) {
            return DefaultFactories.forBuildin(type);
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new IllegalArgumentException(Errors.getResources(properties)
                .getString(Errors.Keys.IllegalPropertyValueClass_2, key, Classes.getClass(value)));
    }

    /**
     * Returns the factory for coordinate operation objects.
     * The factory is backed by this {@code Proj4Factory} as the {@code MathTransformFactory} implementation.
     */
    final DefaultCoordinateOperationFactory opFactory() {
        DefaultCoordinateOperationFactory factory = opFactory;
        if (factory == null) {
            final Map<String,Object> properties = new HashMap<String,Object>(defaultProperties);
            properties.put(ReferencingServices.CRS_FACTORY,   crsFactory);
            properties.put(ReferencingServices.CS_FACTORY,    csFactory);
            properties.put(ReferencingServices.DATUM_FACTORY, datumFactory);
            factory = new DefaultCoordinateOperationFactory(properties, mtFactory);
            opFactory = factory;
        }
        return factory;
    }

    /**
     * Returns the project that defines the codes recognized by this factory.
     * The authority determines the {@linkplain #getCodeSpaces() code space}.
     *
     * @return {@link Citations#PROJ4}.
     */
    @Override
    public Citation getAuthority() {
        Citation c = authority;
        if (c == null) {
            c = Citations.PROJ4;
            final String release = Proj4.version();
            if (release != null) {
                final DefaultCitation df = new DefaultCitation(c);
                df.setEdition(new SimpleInternationalString(release));
                df.freeze();
                c = df;
            }
            authority = c;
        }
        return c;
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
        codes.add(PROJ_PARAM.concat(method));
        return codes;
    }

    /**
     * Returns some map projection methods supported by {@literal Proj.4}.
     * Current implementation can not return the complete list of Proj.4 method, but returns the main ones.
     * For each operation method in the returned set, the Proj.4 projection name can be obtained as below:
     *
     * {@preformat java
     *     String proj = IdentifiedObjects.getName(method, Citations.PROJ4);
     * }
     *
     * The {@code proj} names obtained as above can be given in argument to the
     * {@link #getOperationMethod(String)} and {@link #getDefaultParameters(String)} methods.
     *
     * @param  type <code>{@linkplain SingleOperation}.class</code> for fetching all operation methods, or
     *              <code>{@linkplain Projection}.class</code> for fetching only map projection methods.
     * @return methods available in this factory for coordinate operations of the given type.
     *
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#getAvailableMethods(Class)
     */
    public Set<OperationMethod> getAvailableMethods(final Class<? extends SingleOperation> type) {
        return new LazySet<>(CollectionsExt.filter(mtFactory.getAvailableMethods(type).iterator(), new Predicate<IdentifiedObject>() {
            @Override public boolean test(final IdentifiedObject method) {
                return isSupported(method);
            }
        }));
    }

    /**
     * Returns the operation method of the given name. The given argument can be a Proj.4 projection name,
     * but other authorities (OGC, EPSG…) are also accepted. A partial list of supported projection names
     * can be obtained by {@link #getAvailableMethods(Class)}. Some examples of Proj.4 projection names
     * are given below (not all of them are supported by this Proj.4 wrapper).
     *
     * <table class="sis">
     *   <caption>Some Proj.4 projection names</caption>
     *   <tr><th>Name</th>            <th>Meaning</th></tr>
     *   <tr><td>{@code aea}</td>     <td>Albers Equal-Area Conic</td></tr>
     *   <tr><td>{@code aeqd}</td>    <td>Azimuthal Equidistant</td></tr>
     *   <tr><td>{@code cass}</td>    <td>Cassini-Soldner</td></tr>
     *   <tr><td>{@code cea}</td>     <td>Cylindrical Equal Area</td></tr>
     *   <tr><td>{@code eck4}</td>    <td>Eckert IV</td></tr>
     *   <tr><td>{@code eck6}</td>    <td>Eckert VI</td></tr>
     *   <tr><td>{@code eqdc}</td>    <td>Equidistant Conic</td></tr>
     *   <tr><td>{@code gall}</td>    <td>Gall Stereograpic</td></tr>
     *   <tr><td>{@code geos}</td>    <td>Geostationary Satellite View</td></tr>
     *   <tr><td>{@code gnom}</td>    <td>Gnomonic</td></tr>
     *   <tr><td>{@code krovak}</td>  <td>Krovak Oblique Conic Conformal</td></tr>
     *   <tr><td>{@code laea}</td>    <td>Lambert Azimuthal Equal Area</td></tr>
     *   <tr><td>{@code lcc}</td>     <td>Lambert Conic Conformal</td></tr>
     *   <tr><td>{@code merc}</td>    <td>Mercator</td></tr>
     *   <tr><td>{@code mill}</td>    <td>Miller Cylindrical</td></tr>
     *   <tr><td>{@code moll}</td>    <td>Mollweide</td></tr>
     *   <tr><td>{@code nzmg}</td>    <td>New Zealand Map Grid</td></tr>
     *   <tr><td>{@code omerc}</td>   <td>Oblique Mercator</td></tr>
     *   <tr><td>{@code ortho}</td>   <td>Orthographic</td></tr>
     *   <tr><td>{@code sterea}</td>  <td>Oblique Stereographic</td></tr>
     *   <tr><td>{@code stere}</td>   <td>Stereographic</td></tr>
     *   <tr><td>{@code robin}</td>   <td>Robinson</td></tr>
     *   <tr><td>{@code sinu}</td>    <td>Sinusoidal</td></tr>
     *   <tr><td>{@code tmerc}</td>   <td>Transverse Mercator</td></tr>
     *   <tr><td>{@code vandg}</td>   <td>VanDerGrinten</td></tr>
     * </table>
     *
     * The default implementation delegates to a {@code DefaultCoordinateOperationFactory} instance.
     * It works because the Apache SIS operation methods declare the Proj.4 projection names as
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getAlias() aliases}.
     *
     * @param  name  the name of the operation method to fetch.
     * @return the operation method of the given name.
     * @throws FactoryException if the requested operation method can not be fetched.
     *
     * @see org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#getOperationMethod(String)
     */
    public OperationMethod getOperationMethod(final String name) throws FactoryException {
        final OperationMethod method = opFactory().getOperationMethod(name);
        if (isSupported(method)) {
            return method;
        }
        throw new NoSuchIdentifierException(Errors.getResources(defaultProperties)
                .getString(Errors.Keys.UnsupportedOperation_1, name), name);
    }

    /**
     * Returns the default parameter values for a math transform using the given operation method.
     * The given argument can be a Proj.4 projection name, but other authorities (OGC, EPSG…) are also accepted.
     * A partial list of supported projection names can be obtained by {@link #getAvailableMethods(Class)}.
     * The returned parameters can be given to {@link #createParameterizedTransform(ParameterValueGroup)}.
     *
     * @param  method  the case insensitive name of the coordinate operation method to search for.
     * @return a new group of parameter values for the {@code OperationMethod} identified by the given name.
     * @throws NoSuchIdentifierException if there is no method registered for the given name or identifier.
     */
    public ParameterValueGroup getDefaultParameters(final String method) throws NoSuchIdentifierException {
        final ParameterValueGroup parameters = mtFactory.getDefaultParameters(method);
        if (isSupported(parameters.getDescriptor())) {
            return parameters;
        }
        throw new NoSuchIdentifierException(Errors.getResources(defaultProperties)
                .getString(Errors.Keys.UnsupportedOperation_1, method), method);
    }

    /**
     * Creates a transform from a group of parameters. The {@link OperationMethod} name is inferred from
     * the {@linkplain org.opengis.parameter.ParameterDescriptorGroup#getName() parameter group name}.
     * Each parameter value is formatted as a Proj.4 parameter in a definition string.
     *
     * <div class="note"><b>Example:</b>
     * {@preformat java
     *     ParameterValueGroup p = factory.getDefaultParameters("Mercator");
     *     p.parameter("semi_major").setValue(6378137.000);
     *     p.parameter("semi_minor").setValue(6356752.314);
     *     MathTransform mt = factory.createParameterizedTransform(p);
     * }
     *
     * The corresponding Proj.4 definition string is:
     *
     * {@preformat text
     *     +proj=merc +a=6378137.0 +b=6356752.314
     * }
     * </div>
     *
     * @param  parameters  the parameter values.
     * @return the parameterized transform.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @see #getDefaultParameters(String)
     * @see #getAvailableMethods(Class)
     */
    public MathTransform createParameterizedTransform(final ParameterValueGroup parameters) throws FactoryException {
        final String proj = name(parameters.getDescriptor(), Errors.Keys.UnsupportedOperation_1);
        final StringBuilder buffer = new StringBuilder(100).append(PROJ_PARAM).append(proj);
        for (final GeneralParameterValue p : parameters.values()) {
            /*
             * Unconditionally ask the parameter name in order to throw an exception
             * with better error message in case of unrecognized parameter.
             */
            final String name = name(p.getDescriptor(), Errors.Keys.UnexpectedParameter_1);
            if (p instanceof ParameterValue) {
                final Object value = ((ParameterValue) p).getValue();
                if (value != null) {
                    buffer.append(" +").append(name).append('=').append(value);
                }
            }
        }
        final String definition = buffer.toString();
        try {
            final PJ pj = unique(new PJ(definition));
            final PJ base = unique(new PJ(pj));
            return new Transform(base, false, pj, false);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new UnavailableFactoryException(Proj4.unavailable(e), e);
        }
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
        try {
            return createCRS(code, hasHeight);
        } catch (IllegalArgumentException | ParserException e) {
            throw new InvalidGeodeticParameterException(Proj4.canNotParse(code), e);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new UnavailableFactoryException(Proj4.unavailable(e), e);
        }
    }

    /**
     * Returns the identifier for the given code in {@literal Proj.4} namespace.
     */
    private Map<String,Object> identifier(final String code) {
        Identifier id = identifiers.get(code);
        if (id == null) {
            short i18n = 0;
            if (code.equalsIgnoreCase( UNNAMED )) i18n = Vocabulary.Keys.Unnamed;
            if (code.equalsIgnoreCase("Unknown")) i18n = Vocabulary.Keys.Unknown;
            id = new ImmutableIdentifier(Citations.PROJ4, Constants.PROJ4, code, null,
                    (i18n != 0) ? Vocabulary.formatInternational(i18n) : null);
            identifiers.put(code, id);
        }
        final Map<String,Object> properties = new HashMap<String,Object>(defaultProperties);
        properties.put(IdentifiedObject.NAME_KEY, id);
        return properties;
    }

    /**
     * Creates a geodetic datum from the given {@literal Proj.4} wrapper.
     *
     * @param  pj      the Proj.4 object to wrap.
     * @param  parser  the parameter values of the Proj.4 object to wrap.
     * @throws NumberFormatException if a Proj.4 parameter value can not be parsed.
     */
    private GeodeticDatum createDatum(final PJ pj, final Proj4Parser parser) throws FactoryException {
        final PrimeMeridian pm;
        final double greenwichLongitude = Double.parseDouble(parser.value("pm", "0"));
        if (greenwichLongitude == 0) {
            pm = CommonCRS.WGS84.datum().getPrimeMeridian();
        } else {
            pm = datumFactory.createPrimeMeridian(identifier(UNNAMED), greenwichLongitude, Units.DEGREE);
        }
        final double[] def = pj.getEllipsoidDefinition();
        return datumFactory.createGeodeticDatum(identifier(parser.value("datum", UNNAMED)),
               datumFactory.createEllipsoid    (identifier(parser.value("ellps", UNNAMED)),
                    def[0],                             // Semi-major axis length
                    def[0] * Math.sqrt(1 - def[1]),     // Semi-minor axis length
                    Units.METRE), pm);
    }

    /**
     * Creates a coordinate reference system from the given {@literal Proj.4} wrapper.
     * The given {@code pj} will be stored as the CRS identifier.
     *
     * @param  pj          the Proj.4 object to wrap.
     * @param  withHeight  whether to include a height axis.
     * @throws IllegalArgumentException if a Proj.4 parameter value can not be parsed or assigned.
     * @throws ParserException if a unit symbol can not be parsed.
     */
    private CoordinateReferenceSystem createCRS(final PJ pj, final boolean withHeight) throws FactoryException {
        final PJ.Type type = pj.getType();
        final boolean geographic = PJ.Type.GEOGRAPHIC.equals(type);
        final Proj4Parser parser = new Proj4Parser(pj.getCode());
        final String dir = parser.value("axis", "enu");
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[withHeight ? dir.length() : 2];
        for (int i=0; i<axes.length; i++) {
            final char d = Character.toLowerCase(dir.charAt(i));
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
            final Unit<?> unit = (vertical || !geographic) ? parser.unit(vertical) : Units.DEGREE;
            axes[i] = csFactory.createCoordinateSystemAxis(identifier(name), String.valueOf(abbreviation).intern(), c, unit);
        }
        /*
         * At this point we got the coordinate system axes. Now create the CRS. The given Proj.4 object
         * will be stored as the CRS identifier for allowing OperationFactory to get it back before to
         * attempt to create a new one for a given CRS.
         */
        final Map<String,Object> csName = identifier(UNNAMED);
        final Map<String,Object> name = new HashMap<>(identifier(parser.name(type == PJ.Type.PROJECTED)));
        name.put(CoordinateReferenceSystem.IDENTIFIERS_KEY, pj);
        switch (type) {
            case GEOGRAPHIC: {
                return crsFactory.createGeographicCRS(name, createDatum(pj, parser), withHeight ?
                        csFactory.createEllipsoidalCS(csName, axes[0], axes[1], axes[2]) :
                        csFactory.createEllipsoidalCS(csName, axes[0], axes[1]));
            }
            case GEOCENTRIC: {
                return crsFactory.createGeocentricCRS(name, createDatum(pj, parser),
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
                    method = parser.method(opFactory());
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
                throw new FactoryException(Errors.getResources(defaultProperties)
                        .getString(Errors.Keys.UnknownEnumValue_2, type, PJ.Type.class));
            }
        }
    }

    /**
     * Gets the {@literal Proj.4} object from the given coordinate reference system. If an existing {@code PJ}
     * instance is found, returns it. Otherwise if {@code force} is {@code true}, creates a new {@code PJ}
     * instance from a Proj.4 definition inferred from the given CRS.
     * This method is the converse of {@link #createCRS(PJ, boolean)}.
     */
    private PJ unwrapOrCreate(final CoordinateReferenceSystem crs, final boolean force) throws FactoryException {
        for (final Identifier id : crs.getIdentifiers()) {
            if (id instanceof PJ) {
                return (PJ) id;
            }
        }
        return force ? unique(new PJ(Proj4.definition(crs))) : null;
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
     * The given CRSs should be instances {@linkplain #createCoordinateReferenceSystem created by this factory}.
     * If not, then there is a choice:
     *
     * <ul>
     *   <li>If {@code force} is {@code false}, then this method returns {@code null}.</li>
     *   <li>Otherwise this method always uses Proj.4 for performing the coordinate operations,
     *       regardless if the given CRS were created from Proj.4 definition strings or not.
     *       This method fails if it can not map the given CRS to Proj.4 data structures.</li>
     * </ul>
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @param  force      whether to force the creation of a Proj.4 transform
     *                    even if the given CRS are not wrappers around Proj.4 data structures.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS, or
     *         {@code null} if the given CRS are not wrappers around Proj.4 data structures and {@code force} is false.
     * @throws FactoryException if {@code force} is {@code true} and this method can not create Proj.4 transform
     *         for the given pair of coordinate reference systems.
     *
     * @see Proj4#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)
     * @see DefaultCoordinateOperationFactory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)
     */
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS,
                                               final boolean force)
            throws FactoryException
    {
        final PJ source, target;
        try {
            if ((source = unwrapOrCreate(sourceCRS, force)) == null ||
                (target = unwrapOrCreate(targetCRS, force)) == null)
            {
                return null;            // At least one CRS is not a Proj.4 wrapper and 'force' is false.
            }
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new UnavailableFactoryException(Proj4.unavailable(e), e);
        }
        /*
         * Before to create a transform, verify if the target CRS already contains a suitable transform.
         * In such case, returning the existing operation is preferable since it usually contains better
         * parameter description than what this method build.
         */
        if (targetCRS instanceof GeneralDerivedCRS) {
            final CoordinateOperation op = ((GeneralDerivedCRS) targetCRS).getConversionFromBase();
            final MathTransform tr = op.getMathTransform();
            if (tr instanceof Transform && ((Transform) tr).isFor(sourceCRS, source, targetCRS, target)) {
                return op;
            }
        }
        /*
         * The 'Transform' construction implies parameter validation, so we do it first before to
         * construct other objects.
         */
        final Transform tr = new Transform(source, is3D("sourceCRS", sourceCRS),
                                           target, is3D("targetCRS", targetCRS));
        Identifier id;
        String src = null, tgt = null, name = UNNAMED;
        if ((id = sourceCRS.getName()) != null) src = id.getCode();
        if ((id = targetCRS.getName()) != null) tgt = id.getCode();
        if (src != null || tgt != null) {
            final StringBuilder buffer = new StringBuilder();
            if (src != null) buffer.append("From ").append(src);
            if (tgt != null) buffer.append(buffer.length() == 0 ? "To " : " to ").append(tgt);
            name = buffer.toString();
        }
        return opFactory().createSingleOperation(identifier(name), sourceCRS, targetCRS, null, Transform.METHOD, tr);
    }

    /**
     * Returns whether the given CRS is three-dimensional.
     * Thrown an exception if the number of dimension is unsupported.
     */
    private boolean is3D(final String arg, final CoordinateReferenceSystem crs) throws FactoryException {
        final int dim = crs.getCoordinateSystem().getDimension();
        final boolean is3D = (dim >= 3);
        if (dim < 2 || dim > 3) {
            throw new FactoryException(Errors.getResources(defaultProperties)
                    .getString(Errors.Keys.MismatchedDimension_3, arg, is3D ? 3 : 2, dim));
        }
        return is3D;
    }

    /**
     * Returns {@code true} if the given coordinate operation method or parameter group is supported.
     */
    private static boolean isSupported(final IdentifiedObject method) {
        return IdentifiedObjects.getName(method, Citations.PROJ4) != null;
    }

    /**
     * Returns the {@literal Proj.4} name of the given parameter value or parameter group.
     *
     * @param  param    the parameter value or parameter group for which to get the Proj.4 name.
     * @param  errorKey the resource key of the error message to produce if no Proj.4 name has been found.
     *                  The message shall expect exactly one argument. This error key can be
     *                  {@link Errors.Keys#UnsupportedOperation_1} or {@link Errors.Keys#UnexpectedParameter_1}.
     * @return the Proj.4 name of the given object (never null).
     * @throws FactoryException if the Proj.4 name has not been found.
     */
    private String name(final IdentifiedObject param, final short errorKey) throws FactoryException {
        String name = IdentifiedObjects.getName(param, Citations.PROJ4);
        if (name == null) {
            name = param.getName().getCode();
            final String message = Errors.getResources(defaultProperties).getString(errorKey, name);
            if (errorKey == Errors.Keys.UnsupportedOperation_1) {
                throw new NoSuchIdentifierException(message, name);
            } else {
                throw new InvalidGeodeticParameterException(message);
            }
        }
        return name;
    }
}
