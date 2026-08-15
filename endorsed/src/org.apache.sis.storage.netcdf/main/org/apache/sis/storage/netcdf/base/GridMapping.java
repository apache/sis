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
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.provider.PseudoPlateCarree;
import org.apache.sis.referencing.internal.shared.AxisDirections;
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
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.math.NumberType;
import org.apache.sis.measure.Units;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;

// Specific to the geoapi-4.0 branch:
import org.opengis.referencing.crs.DerivedCRS;


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
     * Name of attributes where the "grid to <abbr>CRS</abbr> transform may be encoded as an affine transform.
     * The {@code GeoTransform} attribute is specific to <abbr>GDAL</abbr>. It uses pixel-corner convention and
     * interprets data as if it was an image (as if the row shown on the top had index 0), ignoring the netCDF
     * cell indices (where row 0 is often in the bottom).
     *
     * @see #gridToCRS
     * @see #SOURCE_AXIS_TO_FLIP
     */
    private static final String GEOTRANSFORM = "GeoTransform";

    /**
     * Index of the source axis to flip in a "grid to <abbr>CRS</abbr>" transform. This is for flipping the
     * <var>y</var> axis for switching from an image coordinate system to an arithmetic coordinate system.
     * The flip requires the number of cells (rows in the case of <var>y</var> axis) along the axis to flip.
     */
    private static final int SOURCE_AXIS_TO_FLIP = 1;

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
     * declared in {@value #SPATIAL_REF}, {@code "ESRI_pe_string"} or {@code "EPSG_code"} attributes.
     *
     * <h4>Usage note</h4>
     * This is built from different information than the one used by {@link CRSBuilder},
     * which creates <abbr>CRS</abbr> by inspection of coordinate system axes.
     *
     * @see #crs()
     */
    private CoordinateReferenceSystem crs;

    /**
     * The <i>grid corner to CRS</i> transform, or {@code null} if none.
     * This information is usually not specified except when using <abbr>GDAL</abbr> conventions.
     * If {@code null}, then the transform should be inferred by {@link Grid}.
     *
     * <h4>Image flip</h4>
     * If the {@code gridToCRS} transform has been specified by the <abbr>GDAL</abbr>'s {@value #GEOTRANSFORM}
     * attribute, then it needs to have the <var>y</var> axis flipped. This is because <abbr>GDAL</abbr> seems
     * to ignore the netCDF cell coordinate system and to handle the data has if it was an image with the last
     * row (in netCDF order) shown on the top.
     *
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
     * Parses the map projection parameters defined as attribute associated to the given variable.
     * This method tries to parse <abbr>CF</abbr>-compliant attributes, potentially mixed with
     * non-standard extensions (for example <abbr>GDAL</abbr> and <abbr>ESRI</abbr>).
     *
     * @param  variable  the variable from which to get the map projection parameters.
     * @return grid geometry information, or {@code null} if not found or if an error occurred.
     */
    @SuppressWarnings("UseSpecificCatch")
    private static GridMapping tryAllConventions(final Node mapping) {
        var gm = new GridMapping(mapping);
        // Tries CF-convention first, and if it doesn't work, try GDAL convention.
        int i = 0;
        boolean stop = false;
        Exception warning = null;
        do {
            try {
                switch (i++) {
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
        final Map<String, Object> definition = decoder.convention().projection(mapping);
        if (definition == null) {
            return false;
        }
        /*
         * Search in advance for a CRS that we can use as a fallback. We need only one such CRS for now,
         * the other CRS definitions will be parsed later in this method. We need this fallback in order
         * to provide default names to geodetic objects in the common case where the `definition` map
         * does not contain these names.
         */
        final var alreadyParsedWKT = new ArrayList<String>(2);
        if (crs == null) setOrVerifyWKT(alreadyParsedWKT, definition, CRS_WKT);
        if (crs == null) setOrVerifyWKT(alreadyParsedWKT, definition, SPATIAL_REF);
        final CoordinateReferenceSystem fromWKT = crs;
        /*
         * Fetch now numerical values that are not map projection parameters.
         * This step needs to be done before to try to set parameter values.
         */
        final Object greenwichLongitude = definition.remove(Convention.LONGITUDE_OF_PRIME_MERIDIAN);
        final String mappingName = (String) definition.remove(CF.GRID_MAPPING_NAME);
        /*
         * Prepare the group of projection parameters. The set of legal parameter depends on the map projection.
         * We assume that all numerical values are map projection parameters. Character sequences (assumed to be
         * component names) are handled later. The CF-conventions use parameter names that are slightly different
         * than OGC names, but Apache SIS implementations of map projections know how to handle them, including
         * the redundant parameters like "inverse_flattening" and "earth_radius".
         */
        final OperationMethod method = decoder.findOperationMethod(mappingName);
        final ParameterValueGroup parameters = method.getParameters().createValue();
        for (final Iterator<Map.Entry<String, Object>> it = definition.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, Object> entry = it.next();
            final String name  = entry.getKey();
            final Object value = entry.getValue();
            try {
                if (value instanceof Number || value instanceof double[] || value instanceof float[]) {
                    it.remove();
                    parameters.parameter(name).setValue(value);
                } else if (value instanceof String) {
                    final var text = (String) value;
                    if (name.endsWith(Convention.NAME_SUFFIX)) {
                        continue;
                    }
                    switch (name) {
                        case CRS_WKT:
                        case SPATIAL_REF: continue;     // Will be parsed after this loop.
                        case "geotransform": {          // "GeoTransform" made lower-case.
                            if (parseGeoTransform(text)) {
                                it.remove();
                            }
                            continue;
                        }
                    }
                    /*
                     * In principle, we should ignore non-numeric parameters. But in practice, some badly encoded
                     * netCDF files store parameters as strings instead of numbers. If the parameter name is known
                     * to the projection method, try to parse the character string.
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
                        it.remove();
                        parameter.setValue(Double.parseDouble(text));
                    } else if (NumberType.isReal(type.getComponentType())) {
                        it.remove();
                        parameter.setValue(parseDoubles(text), null);
                    }
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
        }
        /*
         * In principle, projection parameters do not include the semi-major and semi-minor axis lengths.
         * But if those information are provided, then we use them for building the geodetic reference frame.
         * Otherwise, a default reference frame will be used.
         */
        final CommonCRS defaultDefinitions = decoder.convention().defaultHorizontalCRS(false);
        final DatumFactory datumFactory = decoder.getDatumFactory();
        boolean hasBuiltSomeCustomObjects = false;
        /*
         * Prime meridian built from "longitude_of_prime_meridian".
         */
        final PrimeMeridian meridian;
        if (greenwichLongitude instanceof Number) {
            final double longitude = ((Number) greenwichLongitude).doubleValue();
            final Map<String,?> properties = properties(definition, false, Convention.PRIME_MERIDIAN_NAME, () -> {
                // Fallback if `definition` does not contain a name for the prime meridian.
                PrimeMeridian template = DatumOrEnsemble.getPrimeMeridian(fromWKT).orElse(null);
                if (template == null) {
                    if (longitude != 0) return null;
                    template = defaultDefinitions.primeMeridian();
                }
                return template.getName();
            });
            meridian = datumFactory.createPrimeMeridian(properties, longitude, Units.DEGREE);
            hasBuiltSomeCustomObjects = true;
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
            final double semiMajor = p.doubleValue();
            boolean isIvfDefinitive;
            try {
                isIvfDefinitive = parameters.parameter(Constants.IS_IVF_DEFINITIVE).booleanValue();
            } catch (ParameterNotFoundException e) {
                // Ignore - may be normal if the map projection is not an Apache SIS implementation.
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
            final Map<String,?> properties = properties(definition, false, Convention.ELLIPSOID_NAME, () -> {
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
            hasBuiltSomeCustomObjects = true;
        } catch (IllegalStateException e) {
            warningInMapping(mapping, e, Resources.Keys.MissingEllipsoid_3, e.getLocalizedMessage());
            ellipsoid = defaultDefinitions.ellipsoid();
        }
        /*
         * Geodetic reference frame built from "towgs84" and above properties.
         * The class of the "towgs84" entry will be verified by the datum constuctor.
         */
        final GeodeticDatum datum;
        DatumEnsemble<GeodeticDatum> ensemble = null;
        final Object bursaWolf = definition.remove(Convention.TOWGS84);
        if (hasBuiltSomeCustomObjects || bursaWolf != null) {
            Map<String, Object> properties = properties(definition, false, Convention.GEODETIC_DATUM_NAME, () -> {
                // Fallback if `definition` does not contain a name for the geodetic reference frame.
                return CRS.getGeodeticReferenceFrame(fromWKT).map(GeodeticDatum::getName).orElse(null);
            });
            if (bursaWolf != null) {
                properties = new HashMap<>(properties);
                properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, bursaWolf);
                hasBuiltSomeCustomObjects = true;
            }
            datum = datumFactory.createGeodeticDatum(properties, ellipsoid, meridian);
        } else {
            datum = defaultDefinitions.datum(false);
            if (datum == null) {
                ensemble = defaultDefinitions.datumEnsemble();
            }
        }
        /*
         * Geographic or projected CRS built from above properties.
         * The geographic CRS will always have (latitude, longitude) axes in that order and in degrees.
         * The swapping to (longitude, latitude) axis order will be done by the `baseToCRS` transform.
         */
        final boolean wantGeographicCRS = (method instanceof PseudoPlateCarree);
        GeographicCRS baseCRS = defaultDefinitions.geographic();
        final CRSFactory crsFactory = decoder.getCRSFactory();
        if (hasBuiltSomeCustomObjects) {
            final Map<String,?> properties = properties(definition, wantGeographicCRS, Convention.GEOGRAPHIC_CRS_NAME, () -> {
                // Fallback if `definition` does not contain a name for the geodetic CRS.
                IdentifiedObject base = CRS.getHorizontalComponent(fromWKT);
                if (base == null) {
                    base = datum;
                } else if (base instanceof DerivedCRS) {
                    base = ((DerivedCRS) fromWKT).getBaseCRS();
                }
                return base.getName();
            });
            baseCRS = crsFactory.createGeographicCRS(properties, datum, ensemble, baseCRS.getCoordinateSystem());
        }
        // Only swap axis order from (latitude, longitude) to (longitude, latitude).
        MathTransform baseToCRS = MathTransforms.swapTwoFirstAxes(2);
        if (wantGeographicCRS) {
            crs = baseCRS;
        } else {
            /*
             * For any "projection" other than Pseudo Plate Carrée, we will create a projected CRS,
             * which requires a `Conversion` object built from the values in the `parameters` group.
             * Reminder: this parameter group has been created from a subset of `definition` at the
             * beginning of this method.
             */
            Map<String,?> properties = properties(definition, false, Convention.CONVERSION_NAME, () -> {
                if (fromWKT instanceof ProjectedCRS) {
                    return ((ProjectedCRS) fromWKT).getConversionFromBase().getName();
                }
                return mapping.getName();   // Variable on which projection parameters are defined as attributes.
            });
            final Conversion conversion = decoder.getCoordinateOperationFactory()
                    .createDefiningConversion(properties, method, parameters);
            /*
             * Projected CRS. The "base to CRS" transform is the conversion from base directly.
             */
            properties = properties(definition, true, Convention.PROJECTED_CRS_NAME, () -> {
                return (fromWKT != null) ? fromWKT.getName() : conversion.getName();
            });
            final ProjectedCRS projected = crsFactory.createProjectedCRS(properties, baseCRS, conversion, decoder.getStandardProjectedCS());
            baseToCRS = MathTransforms.concatenate(baseToCRS, projected.getConversionFromBase().getMathTransform());
            crs = projected;
        }
        /*
         * The CF-Convention said that even if a WKT definition is provided, other attributes shall be present
         * and have precedence over the WKT definition. Consequently, the purpose of WKT in netCDF files is not
         * obvious (except for CompoundCRS).
         */
        if (fromWKT != null) verifyCRS(fromWKT);
        setOrVerifyWKT(alreadyParsedWKT, definition, CRS_WKT);
        setOrVerifyWKT(alreadyParsedWKT, definition, SPATIAL_REF);
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
        if (gridToCRS == null) {
            gridToCRS = decoder.convention().gridToCRS(mapping, baseToCRS);
            // Map pixel corners by `convention().gridToCRS(…)` contract.
        } else {
            gridToCRS = MathTransforms.concatenate(gridToCRS, baseToCRS);
        }
        return true;
    }

    /**
     * Returns the {@code properties} argument value to give to the factory methods of geodetic objects.
     * The returned map contains at least an entry for {@value IdentifiedObject#NAME_KEY} with the name
     * fetched from the value of the attribute named {@code nameAttribute}.
     *
     * @param definition     map containing the attribute values.
     * @param takeComment    whether to consume the {@code comment} attribute.
     * @param nameAttribute  name of the attribute from which to get the name.
     * @param nameFallback   can return {@link String}, {@link Identifier} or {@code null}.
     */
    private static Map<String, Object> properties(final Map<String, Object> definition,
                                                  final boolean             takeComment,
                                                  final String              nameAttribute,
                                                  final Supplier<?>         nameFallback)
    {
        Object name = definition.remove(nameAttribute);
        if (name == null) {
            name = nameFallback.get();
            if (name == null) {
                // Note: IdentifiedObject.name does not accept InternationalString.
                name = Vocabulary.format(Vocabulary.Keys.Unnamed);
            }
        }
        if (takeComment) {
            Object comment = definition.remove(ACDD.comment);
            if (comment != null) {
                return Map.of(IdentifiedObject.NAME_KEY,    name,
                              IdentifiedObject.REMARKS_KEY, comment);
            }
        }
        return Map.of(IdentifiedObject.NAME_KEY, name);
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
     * @param fromWKT the object parsed from <abbr>WKT</abbr>.
     */
    private void verifyCRS(final CoordinateReferenceSystem fromWKT) {
        if (!Utilities.deepEquals(crs, fromWKT, ComparisonMode.ALLOW_VARIANT)) {
            warning(mapping,        // Node
                    null,           // Exception
                    null,           // Resources
                    Resources.Keys.InconsistentCRS_2,
                    mapping.decoder.getFilename(),
                    mapping.getName());
        }
    }

    /**
     * Tries to parse a <abbr>CRS</abbr> and affine transform from <abbr>GDAL</abbr> GeoTransform coefficients.
     * This is used for parsing the <abbr>GDAL</abbr>'s {@value #GEOTRANSFORM} attribute.
     * The result is stored in the {@link #crs} and {@link #gridToCRS} fields.
     *
     * @return whether this method found grid geometry attributes.
     */
    private boolean parseGDAL() throws ParseException {
        boolean found = parseGeoTransform(mapping.getAttributeAsString(GEOTRANSFORM));
        final String wkt = mapping.getAttributeAsString(SPATIAL_REF);
        if (wkt != null) {
            crs   = createFromWKT(wkt);
            isWKT = true;
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
     * The result is stored in the {@link #gridToCRS} field.
     *
     * @return whether this method found grid geometry attributes.
     */
    @SuppressWarnings("UseSpecificCatch")
    private boolean parseGeoTransform(final String gtr) {
        if (gtr != null) {
            final double[] c = parseDoubles(gtr);
            if (c.length == 6) {
                /*
                 * GDAL convention maps pixel corners and see the data as if it was an image.
                 * The row which is visually on the top is handled as if its index was zero,
                 * ignoring the fact that this is usually the last row in a netCDF variable.
                 */
                gridToCRS = new AffineTransform2D(c[1], c[4], c[2], c[5], c[0], c[3]);    // X_DIMENSION, Y_DIMENSION
                anchorFromConvention = PixelInCell.CELL_CORNER;
                return true;
            }
            var e = new DataStoreContentException(mapping.errors().getString(Errors.Keys.UnexpectedArrayLength_2, 6, c.length));
            cannotCreateGridOrCRS(mapping, e, true);
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
            final var record = new LogRecord(Level.WARNING, warnings.toString());
            record.setLoggerName(Modules.NETCDF);
            record.setSourceClassName(Variable.class.getName());
            record.setSourceMethodName("getGridGeometry");
            mapping.decoder.listeners.warning(record);
        }
        return parsed;
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
     */
    final CoordinateReferenceSystem crs() {
        return crs;
    }

    /**
     * Returns the "grid to CRS", handling the reversal of <var>y</var> axis direction.
     *
     * @param  variable  the variable for which to obtain the transform.
     * @return the transform for the given variable.
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
     * Creates a new grid geometry with the extent of the given variable and a potentially null <abbr>CRS</abbr>.
     * This method should be invoked only as a fallback when no existing {@link GridGeometry} can be used.
     * The CRS and "grid to CRS" transform are null, unless some partial information was found for example
     * as <abbr>WKT</abbr> string.
     */
    final GridGeometry createGridCRS(final Variable variable) {
        final List<Dimension> dimensions = variable.getGridDimensions();
        final int srcDim = dimensions.size();
        final long[] upper = new long[srcDim];
        for (int i=0; i<srcDim; i++) {
            final int d = (srcDim - 1) - i;         // Convert CRS dimension to netCDF dimension.
            upper[i] = dimensions.get(d).length();
        }
        MathTransform implicitG2C = gridToCRS(variable);
        CoordinateReferenceSystem implicitCRS = crs;
        if (implicitG2C != null) {
            int tgtDim = CRS.getDimensionOrZero(implicitCRS);
            if (tgtDim == 0) tgtDim = srcDim;
            MathTransform step1 = changeOfDimension(srcDim, implicitG2C.getSourceDimensions());
            MathTransform step3 = changeOfDimension(implicitG2C.getTargetDimensions(), tgtDim);
            implicitG2C = MathTransforms.concatenate(step1, implicitG2C, step3);
        }
        final var extent = new GridExtent(null, null, upper, false);
        return new GridGeometry(extent, PixelInCell.CELL_CORNER, implicitG2C, implicitCRS);
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
         * The CRS and grid geometry built from grid mapping attributes are called "explicit" in this method.
         * This is by contrast with CRS derived from coordinate variables, which is only implicit.
         */
        CoordinateReferenceSystem explicitCRS = crs;
        MathTransform explicitG2C = gridToCRS(variable);
        if (anchorFromConvention != null) {
            // GDAL "GeoTransform" uses pixel corner convention.
            anchor = anchorFromConvention;
        }
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
                 * We have no guarantees that this latter assumption is correct, but it seems to match common practice.
                 */
                Matrix swapAxisOrder = null;
                final CoordinateSystem cs = implicitCRS.getCoordinateSystem();
                firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, explicitCRS.getCoordinateSystem());
                if (firstAffectedCoordinate < 0) {
                    final CoordinateReferenceSystem beforeSwap = explicitCRS;
                    explicitCRS = AbstractCRS.castOrCopy(explicitCRS).forConvention(AxesConvention.RIGHT_HANDED);
                    firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, explicitCRS.getCoordinateSystem());
                    if (firstAffectedCoordinate < 0) {
                        firstAffectedCoordinate = 0;
                        if (isWKT && crs != null) {
                            explicitCRS = crs;          // If specified by WKT, use the CRS verbatim.
                        }
                    }
                    if (explicitCRS != beforeSwap) try {
                        swapAxisOrder = CoordinateSystems.swapAndScaleAxes(beforeSwap.getCoordinateSystem(),
                                                                          explicitCRS.getCoordinateSystem());
                    } catch (IllegalArgumentException | IncommensurableException e) {
                        cannotCreateGridOrCRS(variable, e, false);
                        return null;
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
                    cannotCreateGridOrCRS(variable, e, false);
                    return null;
                }
                isSameGrid = implicitCRS.equals(explicitCRS);
                if (isSameGrid) {
                    explicitCRS = implicitCRS;          // Keep existing instance if appropriate.
                }
                /*
                 * If we have run the `AbstractCRS.castOrCopy(…).forConvention(…)` code above,
                 * the axis order of the CRS may become different than the axis order which was
                 * assumed when the "grid to CRS" transform was built.
                 */
                if (swapAxisOrder != null) {
                    MathTransform swap = MathTransforms.linear(swapAxisOrder);
                    int numTrailingCoordinates = explicitG2C.getTargetDimensions() - firstAffectedCoordinate;
                    swap = MathTransforms.passThrough(firstAffectedCoordinate, swap, numTrailingCoordinates);
                    explicitG2C = MathTransforms.concatenate(explicitG2C, swap);
                }
            }
        }
        /*
         * Perform the same substitution as above, but in the "grid to CRS" transform. Note that the "grid to CRS"
         * is usually not specified, so the block performing substitution will rarely be executed. If executed,
         * then we need to perform selection in target dimensions (not source dimensions) because the first affected
         * coordinate computed above is in CRS dimension, which is the target of "grid to CRS" transform.
         */
        if (implicit.isDefined(GridGeometry.GRID_TO_CRS)) {
            final MathTransform implicitG2C = implicit.getGridToCRS(anchor);
            if (explicitG2C == null) {
                explicitG2C = implicitG2C;
            } else try {
                final var sep = new TransformSeparator(implicitG2C, variable.decoder.getMathTransformFactory());
                final int end = firstAffectedCoordinate + explicitG2C.getTargetDimensions();
                explicitG2C = sep.replace(firstAffectedCoordinate, end, explicitG2C);
                if (explicitG2C != implicitG2C) {
                    isSameGrid = false;
                    warningInMapping(variable, null, Resources.Keys.InconsistentTransform_3, GEOTRANSFORM);
                    // In current version, GDAL's GeoTransform is the only supported attribute.
                }
            } catch (FactoryException e) {
                cannotCreateGridOrCRS(variable, e, true);
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
