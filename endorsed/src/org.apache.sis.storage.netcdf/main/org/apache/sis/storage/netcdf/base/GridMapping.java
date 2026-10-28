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
package org.apache.sis.storage.netcdf.base;

import java.util.BitSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Supplier;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.ACDD;     // idem
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.provider.PseudoPlateCarree;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.system.Modules;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.math.NumberType;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeneralDerivedCRS;


/**
 * Helper object for creating a {@link GridGeometry} instance defined by attributes on a variable.
 * Those attributes are defined by <abbr>CF</abbr>-conventions, but some other non-<abbr>CF</abbr>
 * attributes are also recognized (e.g. <abbr>GDAL</abbr> and <abbr>ESRI</abbr> conventions).
 *
 * <p>This class uses a different approach than {@link CRSBuilder},
 * which creates Coordinate Reference Systems by inspecting coordinate system axes.
 * The two approaches are complementary, as this {@code GridMapping} class usually creates a
 * two-dimensional <abbr>CRS</abbr>. The other dimensions need to be taken from {@link CRSBuilder}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://cfconventions.org/cf-conventions/cf-conventions.html#grid-mappings-and-projections">CF-conventions</a>
 */
final class GridMapping {
    /**
     * Names of some (not all) attributes where the <abbr>CRS</abbr> may be encoded in <abbr>WKT</abbr> format.
     * Values must be in lower-cases because {@link Convention#projection(Node)} converts names to lower cases.
     * {@code "crs_wkt"} is defined by the <abbr>CF</abbr> convention, while {@code "spatial_ref"} was used in
     * old versions of <abbr>GDAL</abbr>.
     */
    private static final String CRS_WKT = "crs_wkt", SPATIAL_REF = "spatial_ref";

    /**
     * Other attribute names, may be in mixed case.
     */
    private static final String EPSG_CODE = "EPSG_code";

    /**
     * Name of attributes where the "grid to <abbr>CRS</abbr>" transform may be encoded as an affine transform.
     * The {@code GeoTransform} attribute is specific to <abbr>GDAL</abbr>. It uses pixel-corner convention and
     * interprets data as if it was an image (as if the row shown on the top had index 0), ignoring the netCDF
     * cell indices (where row 0 is often in the bottom).
     *
     * @see #gridToCRS
     * @see #SOURCE_AXIS_TO_FLIP
     * @see #parseGeoTransform(Object)
     */
    private static final String GEOTRANSFORM = "GeoTransform";

    /**
     * Index of the source axis to flip in a "grid to <abbr>CRS</abbr>" transform. This is for flipping the
     * <var>y</var> axis for switching from an image coordinate system to an arithmetic coordinate system.
     * The flip requires the number of cells (rows in the case of <var>y</var> axis) along the axis to flip.
     */
    private static final int SOURCE_AXIS_TO_FLIP = 1;

    /**
     * Number of points to sample for consistency check. Used only for checking if we should log a warning.
     */
    private static final int NUM_SAMPLING_POINTS = 5;

    /**
     * The variable on which projection parameters are defined as attributes.
     * This is typically an empty variable referenced by the value of the
     * {@value CF#GRID_MAPPING} attribute on the actual data variable (CF-conventions),
     * but may also be something else such as the data variable itself, or a group, <i>etc.</i>.
     * That node, together with the attributes to be parsed, depends on the {@link Convention} instance.
     */
    private final Node mapping;

    /**
     * The Coordinate Reference System inferred from grid mapping attribute values, or {@code null} if none.
     * This <abbr>CRS</abbr> may have been constructed from Well Known Text or <abbr>EPSG</abbr> codes
     * declared in {@value #SPATIAL_REF}, {@code "ESRI_pe_string"} or {@value #EPSG_CODE} attributes.
     * This is built from different information than the one used by {@link CRSBuilder},
     * which creates <abbr>CRS</abbr> by inspection of coordinate system axes.
     *
     * <p>If this <abbr>CRS</abbr> is geographic, its axis order may be (latitude, longitude).
     * This order may or may not be consistent with the axis order of netCDF variables using this CRS.
     * Each netCDF variable can have its own axis order and be associated to the same <abbr>CRS</abbr>,
     * so users of this <abbr>CRS</abbr> need to check anyway for change of axis order.</p>
     *
     * @see #crs()
     */
    private CoordinateReferenceSystem crs;

    /**
     * The two-dimensional <i>grid corner to <abbr>CRS</abbr></i> transform, or {@code null} if none.
     * This information is usually not specified except when using <abbr>GDAL</abbr> conventions.
     * The source coordinates are (column, row) indexes. The target coordinates are in the CRS
     * (including axis order and units) specified by the horizontal component of {@link #crs}.
     * If {@code null}, then the transform should be inferred by {@link Grid}.
     *
     * <h4>Image flip</h4>
     * If the {@code gridToCRS} transform has been specified by the <abbr>GDAL</abbr>'s {@value #GEOTRANSFORM}
     * attribute, then it needs to have the <var>y</var> axis flipped. This is because <abbr>GDAL</abbr> seems
     * to ignore the netCDF cell coordinate system and to handle the data has if it was an image with the last
     * row (in netCDF order) shown on the top. Flipping cannot be done at {@value #GEOTRANSFORM} reading time,
     * because it depends on the shape of the variable associated with the transform.
     *
     * @see #GEOTRANSFORM
     * @see #SOURCE_AXIS_TO_FLIP
     * @see #gridToCRS(Variable)
     */
    private MathTransform gridToCRS;

    /**
     * The pixel in cell convention of the method that decoded the {@link #gridToCRS} field.
     * This is null if the pixel in cell convention was not inferred.
     */
    private PixelInCell anchorFromConvention;

    /**
     * The attribute that provided the {@link #gridToCRS}, or {@code null} if none.
     * Used for debugging purposes and for producing warning messages.
     */
    private String sourceOfGridToCRS;

    /**
     * Creates an initially empty instance.
     *
     * @param  mapping  the variable on which attributes are defined for projection parameters.
     */
    private GridMapping(final Node mapping) {
        this.mapping = mapping;
    }

    /**
     * Fetches grid geometry information from attributes associated with the given variable.
     * This method should be invoked only one or two times per variable, but may return a
     * shared {@code GridMapping} instance for all variables because there is typically
     * only one set of grid mapping attributes for the whole file.
     *
     * @param  variable  the variable for which to create a grid geometry.
     * @return grid geometry information, or {@code null} if not found.
     */
    static GridMapping forVariable(final Variable variable) {
        final Map<String, GridMapping> gridMapping = variable.decoder.gridMapping;
        for (final String name : variable.decoder.convention().nameOfMappingNode(variable)) {
            GridMapping gm = gridMapping.get(name);
            if (gm != null) {
                return gm;
            }
            /*
             * Value may be null if we already tried and failed to process that grid.
             * We detect those cases in order to avoid logging the same warning twice.
             */
            if (!gridMapping.containsKey(name)) {
                final Node mapping = variable.decoder.findNode(name);
                if (mapping != null) {
                    gm = tryAllConventions(mapping);
                }
                gridMapping.put(name, gm);      // Store even if null.
                if (gm != null) {
                    return gm;
                }
            }
        }
        /*
         * Found no "grid_mapping" attribute. Search for the CRS attributes directly on the variable.
         * This is not CF-compliant, but we find some uses of this non-standard approach in practice.
         */
        final String name = variable.getName();
        GridMapping gm = gridMapping.get(name);
        if (gm == null && !gridMapping.containsKey(name)) {
            gm = tryAllConventions(variable);
            gridMapping.put(name, gm);      // Store even if null.
        }
        return gm;
    }

    /**
     * Parses the map projection parameters defined as attribute associated with the given variable.
     * This method tries to parse <abbr>CF</abbr>-compliant attributes, potentially mixed with
     * non-standard extensions (for example <abbr>GDAL</abbr> and <abbr>ESRI</abbr>).
     *
     * <p>If the <abbr>CRS</abbr> is defined in many ways, an older version of CF-conventions
     * said that grid mapping attributes have precedence over definition by <abbr>WKT</abbr>.
     * A later revision of CF-conventions (around 2020) relaxed that requirement.
     * In case of inconsistent definitions, the choice of which definition prevails
     * is implementation dependent.</p>
     *
     * @param  variable  the variable from which to get the map projection parameters.
     * @return grid geometry information, or {@code null} if not found or if an error occurred.
     *
     * @see <a href="https://github.com/cf-convention/cf-conventions/issues/222">Allow CRS WKT
     * to represent the CRS without requiring reader to compare with grid mapping parameters</a>
     */
    @SuppressWarnings("UseSpecificCatch")
    private static GridMapping tryAllConventions(final Node mapping) {
        var gm = new GridMapping(mapping);
        int convention = 0;
        boolean stop = false;
        Exception warning = null;
        do {
            try {
                switch (convention++) {
                    case 0:  stop = gm.parseProjectionParameters(); break;
                    case 1:  stop = gm.parseGDAL(); break;
                    case 2:  stop = gm.parseESRI(); break;
                    default: stop = true; gm = null; break;
                }
            } catch (Exception e) {   // Checked exceptions | ClassCastException | IllegalArgumentException and more.
                e = Exceptions.unwrap(e);
                if (warning == null) warning = e;
                else warning.addSuppressed(e);
            }
        } while (!stop);
        if (warning != null) {
            cannotCreateGridOrCRS(mapping, warning, warning instanceof TransformException);
        }
        return gm;
    }

    /**
     * Sets the <abbr>CRS</abbr> and "grid to <abbr>CRS</abbr>" from the <abbr>CF</abbr> conventions.
     * If this method does not find the expected attributes, then it does nothing.
     *
     * <p>The <abbr>CRS</abbr> may also be specified in <abbr>WKT</abbr> form in attributes which are themselves
     * specified by different conventions (<abbr>CF</abbr>, <abbr>GDAL</abbr>, <abbr>ESRI</abbr>).
     * If the <abbr>CRS</abbr> is specified both by <abbr>WKT</abbr> and by attributes on a "grid mapping" variable,
     * then the grid mapping attributes have precedence as initially mandated by the <abbr>CF</abbr> conventions.
     * Note: the latter rule has been relaxed by <abbr>CF</abbr> issue #222, but <abbr>SIS</abbr> implementation
     * continues to follow the original specification.</p>
     *
     * <p>The <abbr>CRS</abbr> created by this method is two-dimensional.
     * The addition of vertical or temporal axes must be done by the caller.</p>
     *
     * @return whether this method found grid geometry attributes.
     * @throws ClassCastException if an attribute value is not of the expected type.
     * @throws FactoryException if an error occurred during the attempt to initialize {@link #crs}.
     * @throws TransformException if an error occurred during the attempt to initialize {@link #gridToCRS}.
     *
     * @see <a href="http://cfconventions.org/cf-conventions/cf-conventions.html#grid-mappings-and-projections">CF-conventions</a>
     * @see <a href="https://github.com/cf-convention/cf-conventions/issues/222">Allow CRS WKT to represent the <abbr>CRS</abbr>
     *      without requiring reader to compare with grid mapping parameters</a>
     */
    private boolean parseProjectionParameters() throws FactoryException, TransformException {
        final Decoder decoder = mapping.decoder;
        final Convention convention = decoder.convention();
        final Map<String, Object> definition = convention.projection(mapping);
        if (definition == null) {
            return false;
        }
        /*
         * Parse the WKT first because we will use some of their components (in particular their names)
         * as fallbacks if not specified in the grid mapping attributes. The WKT are redundant with the
         * grid mapping attributes, and the CF-conventions does not said which one has precedence.
         * This omission is intentional, so we have a bit of flexibility for implementation convenience.
         */
        {   // For keeping variable in local scope.
            final var alreadyParsedWKT = new ArrayList<String>(2);
            setOrVerifyWKT(alreadyParsedWKT, definition, CRS_WKT);
            setOrVerifyWKT(alreadyParsedWKT, definition, SPATIAL_REF);
        }
        final CoordinateReferenceSystem fromWKT = crs;
        /*
         * Fetch and remove the attributes that are not map projection parameters.
         * This step needs to be done before to iterate over remaining parameters.
         * Note that `gridMappingName` cannot be null as per `definition` contract.
         */
        parseGeoTransform(definition.remove("geotransform"));  // "GeoTransform" made lower-case.
        final Object greenwichLongitude = definition.remove(Convention.LONGITUDE_OF_PRIME_MERIDIAN);
        final String gridMappingName    = definition.remove(CF.GRID_MAPPING_NAME).toString();
        final Object crsIdentifier      = definition.remove("epsg_code");   // "EPSG_code" made lower-case.
        final Object variableName       = definition.remove(CF.LONG_NAME);
        /*
         * Prepare the group of projection parameters. The set of legal parameter depends on the map projection.
         * We assume that all numerical values are map projection parameters. Character sequences (assumed to be
         * component names) are handled later. The CF-conventions use parameter names that are slightly different
         * than OGC names, but Apache SIS implementations of map projections know how to handle them, including
         * the redundant parameters like "inverse_flattening" and "earth_radius".
         */
        final var properties = new HashMap<String, Object>(4);
        final OperationMethod method = decoder.findOperationMethod(gridMappingName);
        final ParameterValueGroup parameters = method.getParameters().createValue();
        for (final Iterator<Map.Entry<String, Object>> it = definition.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, Object> entry = it.next();
            final String name  = entry.getKey();
            final Object value = entry.getValue();
            try {
                if (value instanceof Number || value instanceof double[] || value instanceof float[]) {
                    parameters.parameter(name).setValue(value);
                } else if (value instanceof String) {
                    final var text = (String) value;
                    if (name.endsWith(Convention.NAME_SUFFIX)) {
                        continue;
                    }
                    /*
                     * Some badly encoded netCDF files store parameters as strings instead of numbers.
                     * If the parameter name is known to the projection method and declares a numeric
                     * type, try to parse the character string.
                     */
                    final ParameterValue<?> parameter;
                    try {
                        parameter = parameters.parameter(name);
                    } catch (ParameterNotFoundException e) {
                        // No warning because it may be normal.
                        continue;
                    }
                    final Class<?> type = parameter.getDescriptor().getValueClass();
                    if (NumberType.isReal(type)) {
                        parameter.setValue(Double.parseDouble(text));
                    } else if (NumberType.isReal(type.getComponentType())) {
                        parameter.setValue(parseDoubles(text), null);
                    } else {
                        parameter.setValue(value);
                    }
                } else {    // Other types such as Bursa-Wolf parameters.
                    continue;
                }
            } catch (IllegalArgumentException ex) {     // Includes NumberFormatException.
                warning(mapping,
                        ex,
                        null,       // Default to `Resources` bundle.
                        Resources.Keys.CanNotSetProjectionParameter_5,
                        decoder.getFilename(),
                        mapping.getName(),
                        name,
                        value,
                        ex.getLocalizedMessage());
            }
            it.remove();
        }
        /*
         * The remaining of this method will create prime meridian, ellipsoid, datum, geographic and projected CRS.
         * The ellipsoid needs the semi-major and semi-minor values, which is why above parameter parsing was first.
         * The `defaultDefinitions` enumeration value is used for providing default objects if there is no WKT.
         */
        final CommonCRS defaultDefinitions = convention.defaultHorizontalCRS(false);
        final DatumFactory datumFactory = decoder.getDatumFactory();
        final GeodeticDatum datum;
        {   // For keeping variables in local scope.
            PrimeMeridian meridian = null;
            if (greenwichLongitude != null) {
                final double longitude = ((Number) greenwichLongitude).doubleValue();
                final Optional<PrimeMeridian> template = DatumOrEnsemble.getPrimeMeridian(fromWKT);
                if (template.isPresent() || longitude != 0) {
                    initializeProperties(properties, definition, Convention.PRIME_MERIDIAN_NAME, () -> {
                        // Fallback if `definition` does not contain a name for the prime meridian.
                        return template.orElseGet(defaultDefinitions::primeMeridian).getName();
                    });
                    meridian = datumFactory.createPrimeMeridian(properties, longitude, Units.DEGREE);
                }
            }
            /*
             * Ellipsoid built from "semi_major_axis" and "semi_minor_axis" parameters. Note that it is okay
             * to use the OGC name (e.g. "semi_major") instead of the netCDF name (e.g. ""semi_major_axis").
             * The Apache SIS implementation of parameter value group understands the aliases. Using the OGC
             * names is safer because they should be understood by most map projection implementations.
             */
            Ellipsoid ellipsoid = null;
            try {
                final ParameterValue<?> p = parameters.parameter(Constants.SEMI_MAJOR);
                final Unit<Length> axisUnit = p.getUnit().asType(Length.class);
                final double semiMajor = p.doubleValue();
                boolean isIvfDefinitive;
                try {
                    isIvfDefinitive = parameters.parameter(Constants.IS_IVF_DEFINITIVE).booleanValue();
                } catch (ParameterNotFoundException e) {
                    // Ignore - may be normal if the map projection is not an Apache SIS implementation.
                    ignorable(e);
                    isIvfDefinitive = false;
                }
                final double  secondDefiningParameter;
                final boolean isSphere;
                if (isIvfDefinitive) {
                    secondDefiningParameter = parameters.parameter(Constants.INVERSE_FLATTENING).doubleValue();
                    isSphere = (secondDefiningParameter == 0) || Double.isInfinite(secondDefiningParameter);
                } else {
                    secondDefiningParameter = parameters.parameter(Constants.SEMI_MINOR).doubleValue(axisUnit);
                    isSphere = secondDefiningParameter == semiMajor;
                }
                initializeProperties(properties, definition, Convention.ELLIPSOID_NAME, () -> {
                    // Fallback if `definition` does not contain a name for the ellipsoid.
                    return DatumOrEnsemble.getEllipsoid(fromWKT).<Object>map(Ellipsoid::getName).orElseGet(() -> {
                        final Locale locale = decoder.getLocale();
                        final String name = Vocabulary.forLocale(locale).getString(isSphere ? Vocabulary.Keys.Sphere : Vocabulary.Keys.Ellipsoid);
                        final NumberFormat f = NumberFormat.getNumberInstance(locale);
                        f.setMaximumFractionDigits(5);      // Centimetric precision.
                        return f.format(axisUnit.getConverterTo(Units.KILOMETRE).convert(semiMajor),
                                        new StringBuffer(name).append(isSphere ? " R=" : " a="),
                                        new FieldPosition(0))
                                .append(" km").toString();
                    });
                });
                if (isIvfDefinitive) {
                    ellipsoid = datumFactory.createFlattenedSphere(properties, semiMajor, secondDefiningParameter, axisUnit);
                } else {
                    ellipsoid = datumFactory.createEllipsoid(properties, semiMajor, secondDefiningParameter, axisUnit);
                }
            } catch (IllegalStateException e) {
                warningInMapping(mapping, e, Resources.Keys.MissingEllipsoid_3, e.getLocalizedMessage());
            }
            /*
             * Geodetic reference frame built from "towgs84" and above properties.
             * The class of the "towgs84" entry will be verified by the datum constuctor.
             */
            final Object bursaWolf = definition.remove(Convention.TOWGS84);
            if (bursaWolf != null || ellipsoid != null || meridian != null) {
                if (ellipsoid == null) ellipsoid = defaultDefinitions.ellipsoid();
                if (meridian  == null) meridian  = defaultDefinitions.primeMeridian();
                initializeProperties(properties, definition, Convention.GEODETIC_DATUM_NAME, () -> {
                    // Fallback if `definition` does not contain a name for the geodetic reference frame.
                    return CRS.getGeodeticReferenceFrame(fromWKT).map(GeodeticDatum::getName).orElse(null);
                });
                properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, bursaWolf);
                datum = datumFactory.createGeodeticDatum(properties, ellipsoid, meridian);
            } else {
                datum = null;
            }
        }
        /*
         * Base of the map projection (including the geographic case, handled as Pseudo Plate Carrée).
         * The axis order of `baseCRS` is (latitude, longitude) even if recreated because it uses the
         * `defaultDefinitions.geographic()` coordinate system which is invariant by contract.
         */
        final boolean wantGeographicCRS = (method instanceof PseudoPlateCarree);
        GeographicCRS baseCRS = defaultDefinitions.geographic();
        final CRSFactory crsFactory = decoder.getCRSFactory();
        if (datum != null) {
            initializeProperties(properties, definition, Convention.GEOGRAPHIC_CRS_NAME, () -> {
                // Fallback if `definition` does not contain a name for the geodetic CRS.
                IdentifiedObject base = CRS.getHorizontalComponent(fromWKT);
                if (base == null) {
                    base = datum;
                } else if (base instanceof GeneralDerivedCRS) {
                    base = ((GeneralDerivedCRS) fromWKT).getBaseCRS();
                }
                return base.getName();
            });
            if (wantGeographicCRS) {
                properties.put(GeographicCRS.REMARKS_KEY, definition.remove(ACDD.comment));
            }
            baseCRS = crsFactory.createGeographicCRS(properties, datum, null, baseCRS.getCoordinateSystem());
        }
        /*
         * Geographic or projected CRS built from above objects declared in the grid mapping variable.
         * The geographic CRS will always have (latitude, longitude) axes in that order and in degrees.
         * We use that order because it will be the order that users see by default in user interfaces.
         * It also increases the chances that we can assign an EPSG code to the CRS.
         */
        if (wantGeographicCRS) {
            crs = baseCRS;
        } else {
            /*
             * For any "projection" other than Pseudo Plate Carrée, we will create a projected CRS,
             * which requires a `Conversion` object built from the values in the `parameters` group.
             * Reminder: this parameter group has been created from a subset of `definition` at the
             * beginning of this method.
             */
            initializeProperties(properties, definition, Convention.CONVERSION_NAME, () -> {
                if (fromWKT instanceof ProjectedCRS) {
                    return ((ProjectedCRS) fromWKT).getConversionFromBase().getName();
                }
                // Variable on which projection parameters are defined as attributes.
                return (variableName != null) ? variableName : mapping.getName();
            });
            final Conversion conversion = decoder.getCoordinateOperationFactory()
                    .createDefiningConversion(properties, method, parameters);
            final IdentifiedObject template = (fromWKT != null) ? fromWKT : conversion;
            initializeProperties(properties, definition, Convention.PROJECTED_CRS_NAME, template::getName);
            properties.put(ProjectedCRS.REMARKS_KEY, definition.remove(ACDD.comment));
            properties.put(ProjectedCRS.IDENTIFIERS_KEY, crsIdentifier);
            crs = crsFactory.createProjectedCRS(properties, baseCRS, conversion, decoder.getStandardProjectedCS());
        }
        /*
         * Report all projection parameters that have not been used. If the map is not rendered
         * at expected location, it may be because we have ignored some important parameters.
         */
        if (!definition.isEmpty()) {
            String remaining = String.join(", ", definition.keySet());
            warningInMapping(mapping, null, Resources.Keys.UnknownProjectionParameters_3, remaining);
        }
        /*
         * Verifies if the CRS parsed from WKT agrees with the CRS built from the netCDF attributes.
         * The comparison ignores axis order and units, because those two characteristics may differ
         * even if the WKT and the grid mapping attributes are otherwise consistent.
         */
        if (fromWKT != null && verifyCRS(fromWKT)) {
            // Assume that the WKT has the intended axis order and units.
            crs = fromWKT;
        } else if (wantGeographicCRS && gridToCRS != null) {
            MathTransform swap = MathTransforms.swapTwoFirstAxes(gridToCRS.getTargetDimensions());
            gridToCRS = MathTransforms.concatenate(gridToCRS, swap);
        }
        /*
         * Replace by the CRS from authority code if possible, because that CRS has more metadata.
         * Again (as for WKT), the axis order and units may differ. If the CRSs are inconsistent,
         * the authority code is ignored (grid mapping attributes or WKT have precedence).
         *
         * Note: we do not use `CRS.equivalent(…)` because the WGS84 is often not recognized as the same,
         * because of numerous variations in the name and because of datum versus ensemble differences.
         */
        if (crsIdentifier != null) try {
            final CoordinateReferenceSystem c = CRS.forCode(crsIdentifier.toString());
            final MathTransform swap = CRS.findOperation(crs, c, null).getMathTransform();
            if (Matrices.isAffine(MathTransforms.getMatrix(swap))) {  // See above note on `CRS.equivalent(…)`.
                /*
                 * If a "grid to CRS" transform was explicitly specified by the GDAL's "GeoTransform" attribute,
                 * the output axes may need to be adjusted for matching the selected CRS axis order and units.
                 * Reminder: `gridToCRS` is two-dimensional, so we need to work only on horizontal components.
                 */
                if (gridToCRS != null) {
                    gridToCRS = MathTransforms.concatenate(gridToCRS, swap);
                }
                crs = c;    // Set only on success.
            } else {
                inconsistentCRS(null, 1);   // 1 is for "EPSG code".
            }
        } catch (OperationNotFoundException e) {
            inconsistentCRS(e, 1);
        } catch (FactoryException e) {
            ignorable(e);   // May be because there is no EPSG database, which is not a netCDF problem.
        }
        /*
         * If no "grid to CRS" transform was explicitly specified by the conventions known to this class
         * (for example, the GDAL's "GeoTransform" attribute), test if a transform is specified by other
         * conventions added as a plugin. Example: GCOM-C from the Japan profile.
         */
        if (gridToCRS == null) {
            if (convention.getClass() != Convention.class) {    // Quick check for common case.
                final MathTransform geographicToCRS;            // From (latitude, longitude) in degrees to the CRS.
                geographicToCRS = CRS.findOperation(baseCRS, crs, null).getMathTransform();
                if ((gridToCRS = convention.gridToCRS(mapping, geographicToCRS)) != null) {
                    anchorFromConvention = PixelInCell.CELL_CORNER;     // By `onvention.gridToCRS(…)` contract.
                }
            }
        }
        return true;
    }

    /**
     * Sets the {@code properties} argument to the values to give to the factory methods of geodetic objects.
     * The returned map contains at least an entry for {@value IdentifiedObject#NAME_KEY} with the name
     * fetched from the value of the attribute named {@code nameAttribute}.
     *
     * @param properties     the map to clear and set. Will be modified in-place.
     * @param definition     map containing the attribute values.
     * @param nameAttribute  name of the attribute from which to get the name.
     * @param nameFallback   can return {@link String}, {@link Identifier} or {@code null}.
     */
    private static void initializeProperties(final Map<String, Object> properties,
                                             final Map<String, Object> definition,
                                             final String              nameAttribute,
                                             final Supplier<?>         nameFallback)
    {
        properties.clear();
        Object name = definition.remove(nameAttribute);
        if (name == null) {
            name = nameFallback.get();
            if (name == null) {
                // Note: IdentifiedObject.name does not accept InternationalString.
                name = Vocabulary.format(Vocabulary.Keys.Unnamed);
            }
        }
        properties.put(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Parses a <abbr>CRS</abbr> defined by an <abbr>WKT</abbr> string, if present.
     * If {@link #crs} is null, it is set to the parsing result. Otherwise, the current {@link #crs} has precedence
     * but the parsed <abbr>CRS</abbr> is compared and a warning is logged if an inconsistency is found.
     *
     * @param alreadyParsedWKT  <abbr>WKT</abbr> already parsed, for avoiding repetition.
     * @param definition        map containing the attribute values.
     * @param attributeName     name of the attribute to consume in the definition map.
     */
    @SuppressWarnings("UseSpecificCatch")
    private void setOrVerifyWKT(final List<String> alreadyParsedWKT, final Map<String, Object> definition, final String attributeName) {
        Object value = definition.remove(attributeName);
        if (value instanceof String) {
            String wkt = ((String) value).strip();
            for (String previous : alreadyParsedWKT) {
                if (wkt.equalsIgnoreCase(previous)) {
                    return;
                }
            }
            alreadyParsedWKT.add(wkt);
            CoordinateReferenceSystem fromWKT;
            try {
                fromWKT = createFromWKT((String) value);
            } catch (Exception e) {
                warning(mapping, e, mapping.errors(), Errors.Keys.CanNotParseCRS_1, attributeName);
                return;
            }
            if (crs == null) {
                crs = fromWKT;
            } else {
                verifyCRS(fromWKT);
            }
        }
    }

    /**
     * Verifies that the given <abbr>CRS</abbr> is consistent with the {@link #crs} attribute.
     * If not, a warning will be logger. This method does not change the state of this object.
     *
     * @param  fromWKT  the object parsed from <abbr>WKT</abbr>.
     * @return whether the <abbr>CRS</abbr>s are equivalent.
     */
    private boolean verifyCRS(final CoordinateReferenceSystem fromWKT) {
        if (Utilities.deepEquals(crs, fromWKT, ComparisonMode.ALLOW_VARIANT)) {
            return true;
        }
        inconsistentCRS(null, 0);   // 0 is for "WKT".
        return false;
    }

    /**
     * Logs a warning with a message saying that a <abbr>CRS</abbr> is inconsistent with other properties.
     *
     * @param  cause  the exception which has been thrown, or {@code null}.
     * @param  type   0 if the code was parsed from WKT, 1 if from EPSG code.
     */
    private void inconsistentCRS(final Exception cause, final int type) {
        warning(mapping, cause, null, Resources.Keys.InconsistentCRS_3,
                mapping.decoder.getFilename(), mapping.getName(), type);
    }

    /**
     * Tries to parse a <abbr>CRS</abbr> and affine transform from <abbr>GDAL</abbr> GeoTransform coefficients.
     * This is used for parsing the <abbr>GDAL</abbr>'s {@value #GEOTRANSFORM} attribute.
     * The result is stored in the {@link #crs} and {@link #gridToCRS} fields.
     *
     * @return whether this method found grid geometry attributes.
     */
    private boolean parseGDAL() throws ParseException {
        boolean found = parseGeoTransform(mapping.getAttributeValue(GEOTRANSFORM));
        final String wkt = mapping.getAttributeAsString(SPATIAL_REF);
        if (wkt != null) {
            crs   = createFromWKT(wkt);    // Axis order assumed consistent with `gridToCRS`.
            found = true;
        }
        return found;
    }

    /**
     * Tries to parse an affine transform from <abbr>GDAL</abbr> GeoTransform coefficients.
     * Those coefficients are not in the order usually found in matrices, affine transforms or <abbr>TFW</abbr> files.
     * The relationship from pixel/line (P,L) coordinates to <abbr>CRS</abbr> are:
     *
     * {@snippet lang="java" :
     *     X = c[0] + P*c[1] + L*c[2];
     *     Y = c[3] + P*c[4] + L*c[5];
     *     }
     *
     * The target <abbr>CRS</abbr> is presumed to have (longitude, latitude) or (easting, northing) axis order.
     * Note that this is not necessarily the axis order of the <abbr>CRS</abbr> in the {@link #crs} field.
     * Adjustment for change of axis order is done by the code which assigns a value to {@link #crs}.
     *
     * <p>The result is stored in the {@link #gridToCRS} field.
     * Note that the axis at dimension {@value #SOURCE_AXIS_TO_FLIP} is flipped compared to usual netCDF direction.
     * Setting the transform in the expected way depends on the size of the variable associated with this transform,
     * and therefore cannot be done here. See {@link #gridToCRS(Variable)}.</p>
     *
     * @return whether this method found grid geometry attributes.
     */
    private boolean parseGeoTransform(final Object gtr) {
        if (gtr != null) {
            Exception warning;
            try {
                final double[] c;
                if (gtr instanceof double[]) {
                    c = (double[]) gtr;
                } else if (gtr instanceof Vector) {
                    c = ((Vector) gtr).doubleValues();
                } else {
                    c = parseDoubles(gtr.toString());
                }
                if (c.length == 6) {
                    /*
                     * GDAL convention maps pixel corners and see the data as if it was an image.
                     * The row which is visually on the top is handled as if its index was zero,
                     * ignoring the fact that this is usually the last row in a netCDF variable.
                     */
                    gridToCRS = new AffineTransform2D(c[1], c[4], c[2], c[5], c[0], c[3]);    // X_DIMENSION, Y_DIMENSION
                    anchorFromConvention = PixelInCell.CELL_CORNER;
                    sourceOfGridToCRS = GEOTRANSFORM;
                    return true;
                }
                warning = new DataStoreContentException(mapping.errors().getString(Errors.Keys.UnexpectedArrayLength_2, 6, c.length));
            } catch (NumberFormatException e) {
                warning = e;
            }
            cannotCreateGridOrCRS(mapping, warning, true);
        }
        return false;
    }

    /**
     * Parses a comma-separated or space-separated array of numbers.
     *
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    private static double[] parseDoubles(final String values) {
        return CharSequences.parseDoubles(values.replace(',', ' '), ' ');
    }

    /**
     * Tries to parse the Coordinate Reference System using ESRI conventions or other non-CF conventions.
     * This method is invoked as a fallback if {@link #parseGDAL()} found no grid geometry.
     *
     * @return whether this method found grid geometry attributes.
     */
    private boolean parseESRI() throws ParseException, FactoryException {
        String code = mapping.getAttributeAsString("ESRI_pe_string");
        final boolean isWKT = (code != null);
        if (code == null) {
            code = mapping.getAttributeAsString(EPSG_CODE);
            if (code == null) {
                return false;
            }
        }
        /*
         * The Coordinate Reference System stored in those attributes often use the GeoTIFF flavor of EPSG codes,
         * with (longitude, latitude) axis order instead of the authoritative order specified in EPSG database.
         * Likewise, the "WKT 1" flavor used by ESRI is different than WKT 1 defined by OGC 01-009 specification.
         * The CRS parsings below need to take those differences in account, except axis order which is tested in
         * the `adaptGridCRS(…)` method.
         */
        if (isWKT) {
            crs = createFromWKT(code);
        } else {
            crs = CRS.forCode(Constants.EPSG + ':' + code);
        }
        return true;
    }

    /**
     * Creates a coordinate reference system by parsing a Well Known Text (<abbr>WKT</abbr>) string.
     * The WKT is presumed to use the GDAL flavor of WKT 1, and warnings are redirected to decoder listeners.
     */
    private CoordinateReferenceSystem createFromWKT(final String wkt) throws ParseException {
        final var f = new WKTFormat(Decoder.DATA_LOCALE, mapping.decoder.getTimeZone());
        f.setConvention(org.apache.sis.io.wkt.Convention.WKT1_COMMON_UNITS);
        final var parsed = (CoordinateReferenceSystem) f.parseObject(wkt);
        final Warnings warnings = f.getWarnings();
        if (warnings != null) {
            warning(new LogRecord(Level.WARNING, warnings.toString()));
        }
        return parsed;
    }

    /**
     * Logs the given warning, pretending that the warning come form {@link Variable#getGridGeometry()}.
     *
     * @param  record  the warning to log.
     */
    private void warning(final LogRecord record) {
        record.setLoggerName(Modules.NETCDF);
        record.setSourceClassName(Variable.class.getName());
        record.setSourceMethodName("getGridGeometry");
        mapping.decoder.listeners.warning(record);
    }

    /**
     * Logs an exception that can be ignored.
     *
     * @param  e  the exception to ignore.
     */
    private void ignorable(final Exception e) {
        final var record = new LogRecord(Level.FINER, e.getMessage());
        record.setThrown(e);
        warning(record);
    }

    /**
     * Logs a warning with a message saying that we cannot create the grid or the <abbr>CRS</abbr>.
     *
     * @param  mapping  the variable on which the warning applies.
     * @param  ex       the exception that occurred while creating the CRS or grid geometry.
     * @param  grid     {@code grid} if creating the whole grid, or {@code false} for only the <abbr>CRS</abbr>.
     */
    private static void cannotCreateGridOrCRS(final Node mapping, final Exception ex, final boolean grid) {
        warningInMapping(mapping, ex,
                grid ? Resources.Keys.CanNotCreateGridGeometry_3 : Resources.Keys.CanNotCreateCRS_3,
                ex.getLocalizedMessage());
    }

    /**
     * Logs a warning with a message that contains the netCDF file name and the mapping variable, in that order.
     * This method presumes that {@link GridMapping} are invoked (indirectly) from {@link Variable#getGridGeometry()}.
     *
     * @param  mapping  the variable on which the warning applies.
     * @param  ex       the exception that occurred while creating the CRS or grid geometry, or {@code null} if none.
     * @param  key      {@link Resources.Keys#CanNotCreateCRS_3} or {@link Resources.Keys#CanNotCreateGridGeometry_3}.
     * @param  more     an additional argument for localization, or {@code null}.
     */
    private static void warningInMapping(final Node mapping, final Exception ex, final short key, String more) {
        warning(mapping, ex, null, key, mapping.decoder.getFilename(), mapping.getName(), more);
    }

    /**
     * Logs a warning, presuming that {@link GridMapping} are invoked (indirectly) from {@link Variable#getGridGeometry()}.
     *
     * @param  mapping    the variable on which the warning applies.
     * @param  exception  the exception that occurred, or {@code null} if none.
     * @param  resources  the resources bundle for {@code key} and {@code arguments}, or {@code null} for {@link Resources}.
     * @param  key        one of the {@code resources} constants (by default, a {@link Resources.Keys} constant).
     * @param  arguments  values to be formatted in the {@link java.text.MessageFormat} pattern.
     */
    private static void warning(final Node mapping, Exception ex, IndexedResourceBundle resources, short key, Object... arguments) {
        NamedElement.warning(mapping.decoder.listeners, Variable.class, "getGridGeometry", ex, resources, key, arguments);
    }

    /**
     * Returns the Coordinate Reference System inferred from grid mapping attribute values, or {@code null} if none.
     * This method should be invoked for information purposes only, such as <abbr>ISO</abbr> 19115 metadata,
     * because the axis order of this <abbr>CRS</abbr> may not agree with the axis order netCDF variables.
     */
    final CoordinateReferenceSystem crs() {
        return crs;
    }

    /**
     * Returns the explicitly declared "grid to CRS" adapted to the specified variable.
     * The "grid to CRS" transform can be declared in a {@value #GEOTRANSFORM} attribute
     * or computed by a sub-class of {@link Convention} such as <abbr>GCOM</abbr>.
     * The adaptation consists in a flip of the <var>y</var> axis direction,
     * which depends on the shape of the variable.
     *
     * @param  variable  the variable for which to obtain the transform.
     * @return the transform for the given variable, or {@code null} if none.
     */
    private MathTransform gridToCRS(final Variable variable) {
        MathTransform flipped = gridToCRS;
        if (flipped != null) {
            final int yDim = variable.getNumDimensions() - (1 + SOURCE_AXIS_TO_FLIP);
            if (yDim >= 0) {
                final long height = variable.getGridDimensions().get(yDim).length();
                if (height >= 0) {    // Negative if undetermined length.
                    final int srcDim = flipped.getSourceDimensions();
                    final MatrixSIS m = Matrices.createIdentity(srcDim + 1);
                    m.setElement(SOURCE_AXIS_TO_FLIP, SOURCE_AXIS_TO_FLIP, -1);
                    m.setElement(SOURCE_AXIS_TO_FLIP, srcDim, height);
                    flipped = MathTransforms.concatenate(MathTransforms.linear(m), flipped);
                }
            }
        }
        return flipped;
    }

    /**
     * Creates a new grid geometry with the grid extent of the given netCDF variable.
     * Some information such as the <abbr>CRS</abbr> and the "grid to CRS" transform may be missing.
     * This method should be invoked only as a fallback when no existing {@link GridGeometry} can be used.
     *
     * @param  variable  the variable for which to create a grid geometry.
     * @return the default (often incomplete) grid geometry for the given variable.
     */
    final GridGeometry createGridCRS(final Variable variable) {
        final List<Dimension> dimensions = variable.getGridDimensions();
        final int srcDim = dimensions.size();
        final long[] upper = new long[srcDim];
        for (int i=0; i<srcDim; i++) {
            final int d = (srcDim - 1) - i;         // Convert CRS dimension to netCDF dimension.
            upper[i] = dimensions.get(d).length();
        }
        MathTransform explicitG2C = gridToCRS(variable);
        if (explicitG2C != null) {
            int tgtDim = CRS.getDimensionOrZero(crs);
            if (tgtDim == 0) tgtDim = srcDim;
            MathTransform step1 = changeOfDimension(srcDim, explicitG2C.getSourceDimensions());
            MathTransform step3 = changeOfDimension(explicitG2C.getTargetDimensions(), tgtDim);
            explicitG2C = MathTransforms.concatenate(step1, explicitG2C, step3);
        }
        final var anchor = (anchorFromConvention != null) ? anchorFromConvention : PixelInCell.CELL_CORNER;
        final var extent = new GridExtent(null, null, upper, false);
        return new GridGeometry(extent, anchor, explicitG2C, crs);
    }

    /**
     * Returns a transform for changing the number of dimensions of a math transform.
     * If the number of dimensions is increased, new coordinates are initialized to zero.
     * If the number of dimensions is decreased, the last coordinates are dropped.
     */
    private static MathTransform changeOfDimension(final int srcDim, final int tgtDim) {
        if (tgtDim == srcDim) {
            return MathTransforms.identity(srcDim);
        }
        return MathTransforms.linear(Matrices.createDimensionSelect(srcDim, ArraysExt.range(0, tgtDim)));
    }

    /**
     * Creates the grid geometry from the {@link #crs} and {@link #gridToCRS} fields,
     * completing missing information with the implicit grid geometry derived from coordinate variables.
     * For example, {@code GridMapping} may contain information only about the horizontal dimensions, so
     * the given {@code implicit} geometry is used for completing with vertical and temporal dimensions.
     *
     * @param  variable  the variable for which to create a grid geometry.
     * @param  implicit  template to use for completing missing information.
     * @param  anchor    whether we computed "grid to CRS" transform relative to pixel center or pixel corner.
     * @return the grid geometry with modified CRS and "grid to CRS" transform, or {@code null} in case of failure.
     */
    final GridGeometry adaptGridCRS(final Variable variable, final GridGeometry implicit, PixelInCell anchor) {
        /*
         * Some conventions, such as GDAL's "GeoTransform", imply hard-coded pixel corner/center convention.
         * If the `gridToCRS` value came from such convention, the associated "pixel in cell" has precedence.
         */
        if (anchorFromConvention != null) {
            anchor = anchorFromConvention;
        }
        final CoordinateReferenceSystem implicitCRS =
                implicit.isDefined(GridGeometry.CRS) ? implicit.getCoordinateReferenceSystem() : null;
        final MathTransform implicitG2C =
                implicit.isDefined(GridGeometry.GRID_TO_CRS) ? implicit.getGridToCRS(anchor) : null;
        /*
         * The CRS and "grid to CRS" transform built from grid mapping attributes are called "explicit" below.
         * This is by contrast with CRS and transform derived from coordinate variables, which are "implicit".
         */
        CoordinateReferenceSystem explicitCRS = crs;
        MathTransform explicitG2C = gridToCRS(variable);
        if (explicitG2C == null) {
            explicitG2C = implicitG2C;
        }
        int firstAffectedCoordinate = 0;
        if (implicitCRS != null) {
            if (explicitCRS == null) {
                explicitCRS = implicitCRS;
            } else {
                /*
                 * The CRS built by the `Grid` class (based on an inspection of coordinate variables)
                 * may have a different axis order than the CRS specified by grid mapping attributes
                 * (the CRS built by this class). This block locates the dimensions of the horizontal
                 * CRS and replaces `Grid` implicit CRS by `GridMapping` explicit CRS.
                 */
                MathTransform swapAxisOrder = null;
                final BitSet dimensions = CRS.locateDimensions(implicitCRS, explicitCRS, CRS.EquivalenceProperty.DATUM_TYPE);
                firstAffectedCoordinate = Math.max(dimensions.nextSetBit(0), 0);
                if (dimensions.nextSetBit(firstAffectedCoordinate + CRS.getDimensionOrZero(explicitCRS)) >= 0) {
                    // Did not found the expected number of dimensions, or dimension indexes are not consecutive.
                    return null;
                }
                try {
                    /*
                     * Replace the implicit CRS (derived from the netCDF dimensions of the variable) by the explicit CRS
                     * specified by the grid mapping attributes (analyzed by this class). The axis order and units may be
                     * changed from `implicitCRS` order to `explicitCRS` order.
                     */
                    final CoordinateReferenceSystem match = CRS.selectDimensions(
                            implicitCRS, dimensions, CRS.SeparationMode.EXACT);
                    // Following is useful only if a "grid to CRS" exists.
                    if (explicitG2C != null && match != explicitCRS) {
                        swapAxisOrder = MathTransforms.linear(
                                CoordinateSystems.swapAndScaleAxes(
                                        match.getCoordinateSystem(),
                                        explicitCRS.getCoordinateSystem()));
                    }
                    explicitCRS = new CRSMerger(variable.decoder)
                            .replaceComponent(implicitCRS, firstAffectedCoordinate, explicitCRS);
                } catch (IllegalArgumentException | IncommensurableException | FactoryException e) {
                    cannotCreateGridOrCRS(variable, e, false);
                    return null;
                }
                if (swapAxisOrder != null) {  // Implies `explicitG2C != null`
                    swapAxisOrder = MathTransforms.passThrough(firstAffectedCoordinate, swapAxisOrder,
                            explicitG2C.getTargetDimensions() - (firstAffectedCoordinate + swapAxisOrder.getSourceDimensions()));
                    explicitG2C = MathTransforms.concatenate(explicitG2C, swapAxisOrder);
                }
                if (implicitCRS.equals(explicitCRS)) {
                    explicitCRS = implicitCRS;          // Keep existing instance.
                }
            }
        }
        /*
         * Perform the same substitution as above, but in the "grid to CRS" transform instead of in the target CRS.
         * Note that the "grid to CRS" transform is usually not specified (it comes from non-standard sources such
         * as GDAL's "GeoTransform"), so the following will usually be skip because `(explicitG2C == implicitG2C)`.
         * If a substitution is executed, it works with target dimensions (not source dimensions) because the value
         * of `firstAffectedCoordinate` is in CRS dimension, which is the target of the "grid to CRS" transform.
         */
        if (explicitG2C != implicitG2C && implicitG2C != null) {  // Implies `explicitG2C != null` (see method start).
            try {
                final var sep = new TransformSeparator(implicitG2C, variable.decoder.getMathTransformFactory());
                final int end = firstAffectedCoordinate + explicitG2C.getTargetDimensions();
                explicitG2C = sep.replace(firstAffectedCoordinate, end, explicitG2C);
            } catch (FactoryException e) {
                cannotCreateGridOrCRS(variable, e, true);
                return null;
            }
            if (sourceOfGridToCRS != null) {
                TransformException unexpected = null;
                boolean consistent;
                try {
                    consistent = checkConsistency(MathTransforms.concatenate(implicitG2C, explicitG2C.inverse()), implicit.getExtent());
                } catch (TransformException e) {
                    unexpected = e;
                    consistent = false;
                }
                if (!consistent) {
                    warningInMapping(variable, unexpected, Resources.Keys.InconsistentTransform_3, sourceOfGridToCRS);
                }
            }
        }
        /*
         * At this point we finished to compute the grid geometry components.
         * If any of them have changed, create the new grid geometry.
         */
        if (implicitCRS == explicitCRS && implicitG2C == explicitG2C) {
            return implicit;
        } else {
            return new GridGeometry(implicit.getExtent(), anchor, explicitG2C, explicitCRS);
        }
    }

    /**
     * Verifies two "grid to CRS" transforms created in different ways are approximately equivalent.
     * The check uses a roundtrip using one transform then the inverse of the other transform,
     * and checking whether we get approximately the original values.
     *
     * @param  roundtrip  one transform followed by the inverse of the other transform.
     * @param  extent     range of values to test. A subsampling will be applied.
     * @return whether the transforms are equivalent, ignoring rounding errors.
     */
    private static boolean checkConsistency(final MathTransform roundtrip, final GridExtent extent) throws TransformException {
        if (!roundtrip.isIdentity()) {
            final int dimension = roundtrip.getSourceDimensions();
            if (roundtrip.getTargetDimensions() != dimension) {
                return false;
            }
            final int length = dimension * NUM_SAMPLING_POINTS;
            final double[] source = new double[length];
            final double[] target = new double[length];
            for (int c=0; c<dimension; c++) {
                final double scale  = extent.getSize(c, true) / (NUM_SAMPLING_POINTS - 1);
                final double offset = extent.getLow(c);
                for (int i=0; i<NUM_SAMPLING_POINTS; i++) {
                    source[i * dimension + c] = Math.fma(i, scale, offset);
                }
            }
            roundtrip.transform(source, 0, target, 0, NUM_SAMPLING_POINTS);
            for (int i=0; i<length; i++) {
                // Condition uses `!` for catching NaN values.
                if (!(Math.abs(target[i] - source[i]) <= Numerics.COMPARISON_THRESHOLD)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(),
                null,        mapping.getName(),
                "gridToCRS", sourceOfGridToCRS,
                "crs",       IdentifiedObjects.getName(crs, null));
    }
}
