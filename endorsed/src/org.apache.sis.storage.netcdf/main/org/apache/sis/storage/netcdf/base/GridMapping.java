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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Supplier;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.ACDD;     // idem
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.provider.PseudoPlateCarree;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.system.Modules;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.measure.Units;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;


/**
 * Helper object for creating a {@link GridGeometry} instance defined by attributes on a variable.
 * Those attributes are defined by CF-conventions, but some other non-CF attributes are also in usage
 * (e.g. GDAL or ESRI conventions). This class uses a different approach than {@link CRSBuilder},
 * which creates Coordinate Reference Systems by inspecting coordinate system axes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://cfconventions.org/cf-conventions/cf-conventions.html#grid-mappings-and-projections">CF-conventions</a>
 */
final class GridMapping {
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
     * This CRS may have been constructed from Well Known Text or EPSG codes declared in {@code "spatial_ref"},
     * {@code "ESRI_pe_string"} or {@code "EPSG_code"} attributes.
     *
     * <h4>Usage note</h4>
     * This is built from different information than the one used by {@link CRSBuilder},
     * which creates <abbr>CRS</abbr> by inspection of coordinate system axes.
     *
     * @see #crs()
     */
    private CoordinateReferenceSystem crs;

    /**
     * The <i>grid to CRS</i> transform, or {@code null} if none. This information is usually not specified
     * except when using GDAL conventions. If {@code null}, then the transform should be inferred by {@link Grid}.
     */
    private MathTransform gridToCRS;

    /**
     * Whether the {@link #crs} was defined by a WKT string.
     */
    private boolean isWKT;

    /**
     * Creates an initially empty instance.
     *
     * @param  mapping  the variable on which attributes are defined for projection parameters.
     */
    private GridMapping(final Node mapping) {
        this.mapping = mapping;
    }

    /**
     * Fetches grid geometry information from attributes associated to the given variable.
     * This method should be invoked only one or two times per variable, but may return a
     * shared {@code GridMapping} instance for all variables because there is typically
     * only one set of grid mapping attributes for the whole file.
     *
     * @param  variable  the variable for which to create a grid geometry.
     */
    static GridMapping forVariable(final Variable variable) {
        final Map<String,GridMapping> gridMapping = variable.decoder.gridMapping;
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
                    gm = parse(mapping);
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
            gm = parse(variable);
            gridMapping.put(name, gm);      // Store even if null.
        }
        return gm;
    }

    /**
     * Parses the map projection parameters defined as attribute associated to the given variable.
     * This method tries to parse CF-compliant attributes first. If none are found, non-standard
     * extensions (for example GDAL usage) are tried next.
     */
    private static GridMapping parse(final Node mapping) {
        final var gm = new GridMapping(mapping);
        // Tries CF-convention first, and if it doesn't work, try GDAL convention.
        return (gm.parseProjectionParameters() || gm.parseGeoTransform() || gm.parseESRI()) ? gm : null;
    }

    /**
     * Sets the <abbr>CRS</abbr> and "grid to <abbr>CRS</abbr>" from the <abbr>CF</abbr> conventions.
     * If this method does not find the expected attributes, then it does nothing.
     *
     * @return whether this method found grid geometry attributes.
     *
     * @see <a href="http://cfconventions.org/cf-conventions/cf-conventions.html#grid-mappings-and-projections">CF-conventions</a>
     */
    private boolean parseProjectionParameters() {
        final Map<String,Object> definition = mapping.decoder.convention().projection(mapping);
        if (definition != null) try {
            /*
             * Fetch now numerical values that are not map projection parameters.
             * This step needs to be done before to try to set parameter values.
             */
            final Object greenwichLongitude = definition.remove(Convention.LONGITUDE_OF_PRIME_MERIDIAN);
            /*
             * Prepare the block of projection parameters. The set of legal parameter depends on the map projection.
             * We assume that all numerical values are map projection parameters; character sequences (assumed to be
             * component names) are handled later. The CF-conventions use parameter names that are slightly different
             * than OGC names, but Apache SIS implementations of map projections know how to handle them, including
             * the redundant parameters like "inverse_flattening" and "earth_radius".
             */
            final String mappingName = (String) definition.remove(CF.GRID_MAPPING_NAME);
            final OperationMethod method = mapping.decoder.findOperationMethod(mappingName);
            final ParameterValueGroup parameters = method.getParameters().createValue();
            for (final Iterator<Map.Entry<String,Object>> it = definition.entrySet().iterator(); it.hasNext();) {
                final Map.Entry<String,Object> entry = it.next();
                final String name  = entry.getKey();
                final Object value = entry.getValue();
                try {
                    if (value instanceof Number || value instanceof double[] || value instanceof float[]) {
                        it.remove();
                        parameters.parameter(name).setValue(value);
                    } else if (value instanceof String && !name.endsWith(Convention.NAME_SUFFIX)) {
                        /*
                         * In principle we should ignore non-numeric parameters. But in practice, some badly encoded
                         * netCDF files store parameters as strings instead of numbers. If the parameter name is
                         * known to the projection method, try to parse the character string.
                         */
                        final ParameterValue<?> parameter;
                        try {
                            parameter = parameters.parameter(name);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                        final Class<?> type = parameter.getDescriptor().getValueClass();
                        if (Numbers.isNumber(type)) {
                            it.remove();
                            parameter.setValue(Double.parseDouble((String) value));
                        } else if (Numbers.isNumber(type.getComponentType())) {
                            it.remove();
                            parameter.setValue(parseDoubles((String) value), null);
                        }
                    }
                } catch (IllegalArgumentException ex) {                     // Includes NumberFormatException.
                    warning(mapping, ex, null, Resources.Keys.CanNotSetProjectionParameter_5,
                            mapping.decoder.getFilename(), mapping.getName(), name, value, ex.getLocalizedMessage());
                }
            }
            /*
             * In principle, projection parameters do not include the semi-major and semi-minor axis lengths.
             * But if those information are provided, then we use them for building the geodetic reference frame.
             * Otherwise a default reference frame will be used.
             */
            final boolean geographic = (method instanceof PseudoPlateCarree);
            final GeographicCRS baseCRS = createBaseCRS(mapping.decoder, parameters, definition, greenwichLongitude, geographic);
            final MathTransform baseToCRS;
            if (geographic) {
                // Only swap axis order from (latitude, longitude) to (longitude, latitude).
                baseToCRS = MathTransforms.linear(new Matrix3(0, 1, 0, 1, 0, 0, 0, 0, 1));
                crs = baseCRS;
            } else {
                final CoordinateOperationFactory opFactory = mapping.decoder.getCoordinateOperationFactory();
                Map<String,?> properties = properties(definition, Convention.CONVERSION_NAME, false, mapping.getName());
                final Conversion conversion = opFactory.createDefiningConversion(properties, method, parameters);
                final CartesianCS cs = mapping.decoder.getStandardProjectedCS();
                properties = properties(definition, Convention.PROJECTED_CRS_NAME, true, conversion);
                final ProjectedCRS p = mapping.decoder.getCRSFactory().createProjectedCRS(properties, baseCRS, conversion, cs);
                baseToCRS = p.getConversionFromBase().getMathTransform();
                crs = p;
            }
            /*
             * The CF-Convention said that even if a WKT definition is provided, other attributes shall be present
             * and have precedence over the WKT definition. Consequently, the purpose of WKT in netCDF files is not
             * obvious (except for CompoundCRS).
             */
            final var done = new ArrayList<String>(2);
            setOrVerifyWKT(definition, "crs_wkt", done);
            setOrVerifyWKT(definition, "spatial_ref", done);
            /*
             * Report all projection parameters that have not been used. If the map is not rendered
             * at expected location, it may be because we have ignored some important parameters.
             */
            definition.remove(CF.LONG_NAME);
            if (!definition.isEmpty()) {
                warningInMapping(mapping, null, Resources.Keys.UnknownProjectionParameters_3,
                                 String.join(", ", definition.keySet()));
            }
            /*
             * Build the "grid to CRS" if present. This is not defined by CF-convention,
             * but may be present in some non-CF conventions.
             */
            gridToCRS = mapping.decoder.convention().gridToCRS(mapping, baseToCRS);
            return true;
        } catch (ClassCastException | IllegalArgumentException | FactoryException | TransformException e) {
            warningInMapping(mapping, e, Resources.Keys.CanNotCreateCRS_3, null);
        }
        return false;
    }

    /**
     * Creates the geographic CRS from axis length specified in the given map projection parameters.
     * The returned CRS will always have (latitude, longitude) axes in that order and in degrees.
     *
     * @param  parameters  parameters from which to get ellipsoid axis lengths. Will not be modified.
     * @param  definition  map from which to get element names. Elements used will be removed.
     * @param  main        whether the returned <abbr>CRS</abbr> will be the main one.
     */
    private static GeographicCRS createBaseCRS(final Decoder decoder, final ParameterValueGroup parameters,
            final Map<String,Object> definition, final Object greenwichLongitude, final boolean main)
            throws FactoryException
    {
        final DatumFactory datumFactory = decoder.getDatumFactory();
        final CommonCRS defaultDefinitions = decoder.convention().defaultHorizontalCRS(false);
        boolean isSpecified = false;
        /*
         * Prime meridian built from "longitude_of_prime_meridian".
         */
        final PrimeMeridian meridian;
        if (greenwichLongitude instanceof Number) {
            final double longitude = ((Number) greenwichLongitude).doubleValue();
            final String name = (longitude == 0) ? "Greenwich" : null;
            Map<String,?> properties = properties(definition, Convention.PRIME_MERIDIAN_NAME, false, name);
            meridian = datumFactory.createPrimeMeridian(properties, longitude, Units.DEGREE);
            isSpecified = true;
        } else {
            meridian = defaultDefinitions.primeMeridian();
        }
        /*
         * Ellipsoid built from "semi_major_axis" and "semi_minor_axis" parameters. Note that it is okay
         * to use the OGC name (e.g. "semi_major") instead of the netCDF name (e.g. ""semi_major_axis").
         * The Apache SIS implementation of parameter value group understands the aliases. Using the OGC
         * names is safer because they should be understood by most map projection implementations.
         */
        Ellipsoid ellipsoid;
        try {
            final ParameterValue<?> p = parameters.parameter(Constants.SEMI_MAJOR);
            final Unit<Length> axisUnit = p.getUnit().asType(Length.class);
            final double  semiMajor = p.doubleValue();
            final double  secondDefiningParameter;
            final boolean isSphere;
            final boolean isIvfDefinitive = parameters.parameter(Constants.IS_IVF_DEFINITIVE).booleanValue();
            if (isIvfDefinitive) {
                secondDefiningParameter = parameters.parameter(Constants.INVERSE_FLATTENING).doubleValue();
                isSphere = (secondDefiningParameter == 0) || Double.isInfinite(secondDefiningParameter);
            } else {
                secondDefiningParameter = parameters.parameter(Constants.SEMI_MINOR).doubleValue(axisUnit);
                isSphere = secondDefiningParameter == semiMajor;
            }
            final Supplier<Object> fallback = () -> {           // Default ellipsoid name if not specified.
                final Locale  locale = decoder.listeners.getLocale();
                final NumberFormat f = NumberFormat.getNumberInstance(locale);
                f.setMaximumFractionDigits(5);      // Centimetric precision.
                final double km = axisUnit.getConverterTo(Units.KILOMETRE).convert(semiMajor);
                final StringBuffer b = new StringBuffer()
                        .append(Vocabulary.forLocale(locale).getString(isSphere ? Vocabulary.Keys.Sphere : Vocabulary.Keys.Ellipsoid))
                        .append(isSphere ? " R=" : " a=");
                return f.format(km, b, new FieldPosition(0)).append(" km").toString();
            };
            final Map<String,?> properties = properties(definition, Convention.ELLIPSOID_NAME, false, fallback);
            if (isIvfDefinitive) {
                ellipsoid = datumFactory.createFlattenedSphere(properties, semiMajor, secondDefiningParameter, axisUnit);
            } else {
                ellipsoid = datumFactory.createEllipsoid(properties, semiMajor, secondDefiningParameter, axisUnit);
            }
            isSpecified = true;
        } catch (ParameterNotFoundException | IllegalStateException e) {
            // Ignore - may be normal if the map projection is not an Apache SIS implementation.
            ellipsoid = defaultDefinitions.ellipsoid();
        }
        /*
         * Geodetic reference frame built from "towgs84" and above properties.
         */
        final Object bursaWolf = definition.remove(Convention.TOWGS84);
        final GeodeticDatum datum;
        DatumEnsemble<GeodeticDatum> ensemble = null;
        if (isSpecified | bursaWolf != null) {
            Map<String,Object> properties = properties(definition, Convention.GEODETIC_DATUM_NAME, false, ellipsoid);
            if (bursaWolf instanceof BursaWolfParameters) {
                properties = new HashMap<>(properties);
                properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, bursaWolf);
                isSpecified = true;
            }
            datum = datumFactory.createGeodeticDatum(properties, ellipsoid, meridian);
        } else {
            datum = defaultDefinitions.datum();
            if (datum == null) {
                ensemble = defaultDefinitions.datumEnsemble();
            }
        }
        /*
         * Geographic CRS from all above properties.
         */
        if (isSpecified) {
            final Map<String,?> properties = properties(definition, Convention.GEOGRAPHIC_CRS_NAME, main, datum);
            return decoder.getCRSFactory().createGeographicCRS(properties, datum, ensemble,
                    defaultDefinitions.geographic().getCoordinateSystem());
        } else {
            return defaultDefinitions.geographic();
        }
    }

    /**
     * Returns the {@code properties} argument value to give to the factory methods of geodetic objects.
     * The returned map contains at least an entry for {@value IdentifiedObject#NAME_KEY} with the name
     * fetched from the value of the attribute named {@code nameAttribute}.
     *
     * @param definition     map containing the attribute values.
     * @param nameAttribute  name of the attribute from which to get the name.
     * @param takeComment    whether to consume the {@code comment} attribute.
     * @param fallback       fallback as an {@link IdentifiedObject} (from which the name will be copied),
     *                       or a character sequence, or {@code null} for "Unnamed" localized string.
     */
    private static Map<String,Object> properties(final Map<String,Object> definition, final String nameAttribute,
                                                 final boolean takeComment, final Object fallback)
    {
        Object name = definition.remove(nameAttribute);
        if (name == null) {
            if (fallback == null) {
                // Note: IdentifiedObject.name does not accept InternationalString.
                name = Vocabulary.format(Vocabulary.Keys.Unnamed);
            } else if (fallback instanceof IdentifiedObject) {
                name = ((IdentifiedObject) fallback).getName();
            } else if (fallback instanceof Supplier<?>) {
                name = ((Supplier<?>) fallback).get();
            } else {
                name = fallback.toString();
            }
        }
        if (takeComment) {
            Object comment = definition.remove(ACDD.comment);
            if (comment != null) {
                return Map.of(IdentifiedObject.NAME_KEY, name, IdentifiedObject.REMARKS_KEY, comment.toString());
            }
        }
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Parses a <abbr>CRS</abbr> defined by an <abbr>WKT</abbr> string, if present.
     * If {@link #crs} is null, it is set to the parsing result. Otherwise, the current {@link #crs} has precedence
     * but the parsed <abbr>CRS</abbr> is compared and a warning is logged if an inconsistency is found.
     *
     * @param definition     map containing the attribute values.
     * @param attributeName  name of the attribute to consume in the definition map.
     * @param done           <abbr>WKT</abbr> already parsed, for avoiding repetition.
     */
    private void setOrVerifyWKT(final Map<String,Object> definition, final String attributeName, final List<String> done) {
        Object value = definition.remove(attributeName);
        if (value instanceof String) {
            String wkt = ((String) value).strip();
            for (String previous : done) {
                if (wkt.equalsIgnoreCase(previous)) {
                    return;
                }
            }
            done.add(wkt);
            CoordinateReferenceSystem check;
            try {
                check = createFromWKT((String) value);
            } catch (Exception e) {
                warning(mapping, e, mapping.errors(), Errors.Keys.CanNotParseCRS_1, attributeName);
                return;
            }
            if (crs == null) {
                crs = check;
            } else if (!Utilities.equalsIgnoreMetadata(crs, check)) {
                warning(mapping, null, null, Resources.Keys.InconsistentCRS_2,
                        mapping.decoder.getFilename(), mapping.getName());
            }
        }
    }

    /**
     * Tries to parse a CRS and affine transform from GDAL GeoTransform coefficients.
     * Those coefficients are not in the usual order expected by matrix, affine
     * transforms or TFW files. The relationship from pixel/line (P,L) coordinates
     * to CRS are:
     *
     * {@snippet lang="java" :
     *     X = c[0] + P*c[1] + L*c[2];
     *     Y = c[3] + P*c[4] + L*c[5];
     *     }
     *
     * @return whether this method found grid geometry attributes.
     */
    private boolean parseGeoTransform() {
        final String wkt = mapping.getAttributeAsString("spatial_ref");
        final String gtr = mapping.getAttributeAsString("GeoTransform");
        short message = Resources.Keys.CanNotCreateCRS_3;
        boolean done = false;
        try {
            if (wkt != null) {
                crs = createFromWKT(wkt);
                isWKT = true;
                done = true;
            }
            if (gtr != null) {
                message = Resources.Keys.CanNotCreateGridGeometry_3;
                final double[] c = parseDoubles(gtr);
                if (c.length != 6) {
                    throw new DataStoreContentException(mapping.errors().getString(Errors.Keys.UnexpectedArrayLength_2, 6, c.length));
                }
                gridToCRS = new AffineTransform2D(c[1], c[4], c[2], c[5], c[0], c[3]);         // X_DIMENSION, Y_DIMENSION
                done = true;
            }
        } catch (Exception e) {
            warningInMapping(mapping, e, message, null);
        }
        return done;
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
     * This method is invoked as a fallback if {@link #parseGeoTransform()} found no grid geometry.
     *
     * @return whether this method found grid geometry attributes.
     */
    private boolean parseESRI() {
        String code = mapping.getAttributeAsString("ESRI_pe_string");
        isWKT = (code != null);
        if (code == null) {
            code = mapping.getAttributeAsString("EPSG_code");
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
        try {
            if (isWKT) {
                crs = createFromWKT(code);
            } else {
                crs = CRS.forCode(Constants.EPSG + ':' + code);
            }
        } catch (Exception e) {
            warningInMapping(mapping, e, Resources.Keys.CanNotCreateCRS_3, null);
            return false;
        }
        return true;
    }

    /**
     * Creates a coordinate reference system by parsing a Well Known Text (WKT) string.
     * The WKT is presumed to use the GDAL flavor of WKT 1, and warnings are redirected to decoder listeners.
     */
    private CoordinateReferenceSystem createFromWKT(final String wkt) throws ParseException {
        final var f = new WKTFormat(Decoder.DATA_LOCALE, TimeZone.getTimeZone(mapping.decoder.getTimeZone()));
        f.setConvention(org.apache.sis.io.wkt.Convention.WKT1_COMMON_UNITS);
        final var parsed = (CoordinateReferenceSystem) f.parseObject(wkt);
        final Warnings warnings = f.getWarnings();
        if (warnings != null) {
            final var record = new LogRecord(Level.WARNING, warnings.toString());
            record.setLoggerName(Modules.NETCDF);
            record.setSourceClassName(Variable.class.getCanonicalName());
            record.setSourceMethodName("getGridGeometry");
            mapping.decoder.listeners.warning(record);
        }
        return parsed;
    }

    /**
     * Logs a warning with a message that contains the netCDF file name and the mapping variable, in that order.
     * This method presumes that {@link GridMapping} are invoked (indirectly) from {@link Variable#getGridGeometry()}.
     *
     * @param  mapping  the variable on which the warning applies.
     * @param  ex       the exception that occurred while creating the CRS or grid geometry, or {@code null} if none.
     * @param  key      {@link Resources.Keys#CanNotCreateCRS_3} or {@link Resources.Keys#CanNotCreateGridGeometry_3}.
     * @param  more     an additional argument for localization, or {@code null} for the exception message.
     */
    private static void warningInMapping(final Node mapping, final Exception ex, final short key, String more) {
        if (more == null) {
            more = ex.getLocalizedMessage();
        }
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
     */
    final CoordinateReferenceSystem crs() {
        return crs;
    }

    /**
     * Creates a new grid geometry with the extent of the given variable and a potentially null CRS.
     * This method should be invoked only as a fallback when no existing {@link GridGeometry} can be used.
     * The CRS and "grid to CRS" transform are null, unless some partial information was found for example
     * as WKT string.
     */
    final GridGeometry createGridCRS(final Variable variable) {
        final List<Dimension> dimensions = variable.getGridDimensions();
        final long[] upper = new long[dimensions.size()];
        for (int i=0; i<upper.length; i++) {
            final int d = (upper.length - 1) - i;           // Convert CRS dimension to netCDF dimension.
            upper[i] = dimensions.get(d).length();
        }
        return new GridGeometry(new GridExtent(null, null, upper, false), PixelInCell.CELL_CENTER, gridToCRS, crs);
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
    final GridGeometry adaptGridCRS(final Variable variable, final GridGeometry implicit, final PixelInCell anchor) {
        /*
         * The CRS and grid geometry built from grid mapping attributes are called "explicit" in this method.
         * This is by contrast with CRS derived from coordinate variables, which is only implicit.
         */
        CoordinateReferenceSystem explicitCRS = crs;
        int firstAffectedCoordinate = 0;
        boolean isSameGrid = true;
        if (implicit.isDefined(GridGeometry.CRS)) {
            final CoordinateReferenceSystem implicitCRS = implicit.getCoordinateReferenceSystem();
            if (explicitCRS == null) {
                explicitCRS = implicitCRS;
            } else {
                /*
                 * The CRS built by the `Grid` class (based on an inspection of coordinate variables)
                 * may have a different axis order than the CRS specified by grid mapping attributes
                 * (the CRS built by this class). This block checks which axis order seems to fit,
                 * then potentially replaces `Grid` implicit CRS by `GridMapping` explicit CRS.
                 *
                 * This is where the potential difference between EPSG axis order and grid axis order is handled.
                 * If we cannot find which component to replace, assume that grid mapping describes the first dimensions.
                 * We have no guarantees that this latter assumption is right, but it seems to match common practice.
                 */
                final CoordinateSystem cs = implicitCRS.getCoordinateSystem();
                firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, explicitCRS.getCoordinateSystem());
                if (firstAffectedCoordinate < 0) {
                    explicitCRS = AbstractCRS.castOrCopy(explicitCRS).forConvention(AxesConvention.RIGHT_HANDED);
                    firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, explicitCRS.getCoordinateSystem());
                    if (firstAffectedCoordinate < 0) {
                        firstAffectedCoordinate = 0;
                        if (isWKT && crs != null) {
                            explicitCRS = crs;                         // If specified by WKT, use the CRS verbatim.
                        }
                    }
                }
                /*
                 * Replace the grid CRS (or a component of it) by the CRS parsed from WKT or EPSG code with same (if possible)
                 * axis order. If the grid CRS contains more axes (for example elevation or time axis), we try to keep them.
                 */
                try {
                    explicitCRS = new CRSMerger(variable.decoder)
                            .replaceComponent(implicitCRS, firstAffectedCoordinate, explicitCRS);
                } catch (FactoryException e) {
                    warningInMapping(variable, e, Resources.Keys.CanNotCreateCRS_3, null);
                    return null;
                }
                isSameGrid = implicitCRS.equals(explicitCRS);
                if (isSameGrid) {
                    explicitCRS = implicitCRS;                                 // Keep existing instance if appropriate.
                }
            }
        }
        /*
         * Perform the same substitution as above, but in the "grid to CRS" transform. Note that the "grid to CRS"
         * is usually not specified, so the block performing substitution will rarely be executed. If executed, then
         * then we need to perform selection in target dimensions (not source dimensions) because the first affected
         * coordinate computed above is in CRS dimension, which is the target of "grid to CRS" transform.
         */
        MathTransform explicitG2C = gridToCRS;
        if (implicit.isDefined(GridGeometry.GRID_TO_CRS)) {
            final MathTransform implicitG2C = implicit.getGridToCRS(anchor);
            if (explicitG2C == null) {
                explicitG2C = implicitG2C;
            } else try {
                int count = 0;
                var components = new MathTransform[3];
                final var sep = new TransformSeparator(implicitG2C, variable.decoder.getMathTransformFactory());
                if (firstAffectedCoordinate != 0) {
                    sep.addTargetDimensionRange(0, firstAffectedCoordinate);
                    components[count++] = sep.separate();
                    sep.clear();
                }
                components[count++] = explicitG2C;
                final int next = firstAffectedCoordinate + explicitG2C.getTargetDimensions();
                final int upper = implicitG2C.getTargetDimensions();
                if (next != upper) {
                    sep.addTargetDimensionRange(next, upper);
                    components[count++] = sep.separate();
                }
                components = ArraysExt.resize(components, count);
                explicitG2C = MathTransforms.compound(components);
                if (implicitG2C.equals(explicitG2C)) {
                    explicitG2C = implicitG2C;                                 // Keep using existing instance if appropriate.
                } else {
                    isSameGrid = false;
                }
            } catch (FactoryException e) {
                warningInMapping(variable, e, Resources.Keys.CanNotCreateGridGeometry_3, null);
                return null;
            }
        }
        /*
         * At this point we finished to compute the grid geometry components.
         * If any of them have changed, create the new grid geometry.
         */
        if (isSameGrid) {
            return implicit;
        } else {
            return new GridGeometry(implicit.getExtent(), anchor, explicitG2C, explicitCRS);
        }
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(),
                null, mapping.getName(),
                "crs", IdentifiedObjects.getName(crs, null),
                "isWKT", isWKT);
    }
}
