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
package org.apache.sis.internal.netcdf;

import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.ParseException;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import ucar.nc2.constants.CF;


/**
 * Temporary objects for creating a {@link GridGeometry} instance defined by attributes on a variable.
 * Those attributes are defined by CF-conventions, but some other non-CF attributes are also in usage
 * (e.g. GDAL or ESRI conventions). This class uses a different approach than {@link CRSBuilder},
 * which creates Coordinate Reference Systems by inspecting coordinate system axes.
 *
 * <p>Current implementation does not yet parse CF-convention attributes.
 * Only some GDAL and ESRI custom attributes are currently supported.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/StandardCoordinateTransforms.html">UCAR projections</a>
 *
 * @since 1.0
 * @module
 */
final class GridMapping {
    /**
     * The Coordinate Reference System, or {@code null} if none. This CRS can be constructed from Well Known Text
     * or EPSG codes declared in {@code "spatial_ref"}, {@code "ESRI_pe_string"} or {@code "EPSG_code"} attributes.
     *
     * <div class="note"><b>Note:</b> this come from different information than the one used by {@link CRSBuilder},
     * which creates CRS by inspection of coordinate system axes.</div>
     */
    final CoordinateReferenceSystem crs;

    /**
     * The <cite>grid to CRS</cite> transform, or {@code null} if none. This information is usually not specified
     * except when using GDAL conventions. If {@code null}, then the transform should be inferred by {@link Grid}.
     */
    private final MathTransform gridToCRS;

    /**
     * Whether the {@link #crs} where defined by an EPSG code.
     */
    private final boolean isEPSG;

    /**
     * Creates an instance for the given {@link #crs} and {@link #gridToCRS} values.
     */
    private GridMapping(final CoordinateReferenceSystem crs, final MathTransform gridToCRS, final boolean isEPSG) {
        this.crs       = crs;
        this.gridToCRS = gridToCRS;
        this.isEPSG    = isEPSG;
    }

    /**
     * Fetches grid geometry information from attributes associated to the given variable.
     *
     * @param  variable  the variable for which to create a grid geometry.
     */
    static GridMapping forVariable(final Variable variable) {
        final Map<Object,GridMapping> gridMapping = variable.decoder.gridMapping;
        for (final String name : variable.decoder.convention().gridMapping(variable)) {
            GridMapping gm = gridMapping.get(name);
            if (gm != null) {
                return gm;
            }
            final Node mapping = variable.decoder.findNode(name);
            if (mapping != null) {
                gm = parseProjectionParameters(mapping);
                if (gm == null) {
                    gm = parseGeoTransform(mapping);
                }
                if (gm != null) {
                    gridMapping.put(name, gm);
                    return gm;
                }
            }
        }
        /*
         * Found no "grid_mapping" attribute. The block below is not CF-compliant,
         * but we find some use of this non-standard approach in practice.
         */
        GridMapping gm = gridMapping.get(variable);
        if (gm == null) {
            gm = parseProjectionParameters(variable);
            if (gm == null) {
                gm = parseNonStandard(variable);
            }
            if (gm != null) {
                gridMapping.put(variable, gm);
            }
        }
        return gm;
    }

    /**
     * If the netCDF variable defines explicitly the map projection method and its parameters, returns those parameters.
     * Otherwise returns {@code null}.
     */
    private static GridMapping parseProjectionParameters(final Node node) {
        final Map<String, Object> definition = node.decoder.convention().projection(node);
        if (definition != null) try {
            final CoordinateOperationFactory factory = node.decoder.getCoordinateOperationFactory();
            final OperationMethod method = factory.getOperationMethod((String) definition.get(CF.GRID_MAPPING_NAME));
            final ParameterValueGroup parameters = method.getParameters().createValue();
            // TODO: set parameter values.
            final Map<String,?> name = Collections.singletonMap(Conversion.NAME_KEY, "NetCDF projection");      // TODO: find a better name.
            final Conversion conversion = factory.createDefiningConversion(name, method, parameters);

            final GeographicCRS baseCRS = (GeographicCRS) definition.get(Convention.BASE_CRS);
            final CartesianCS cs = CommonCRS.WGS84.universal(0,0).getCoordinateSystem();                     // TODO
            final ProjectedCRS crs = node.decoder.getCRSFactory().createProjectedCRS(name, baseCRS, conversion, cs);

            final MathTransform gridToCRS = node.decoder.convention().gridToCRS(node, crs);
            return new GridMapping(crs, gridToCRS, false);
        } catch (ClassCastException | IllegalArgumentException | FactoryException | TransformException e) {
            canNotCreate(node, Resources.Keys.CanNotCreateCRS_3, e);
        }
        return null;
    }

    /**
     * Tries to parse a CRS and affine transform from GDAL GeoTransform coefficients.
     * Those coefficients are not in the usual order expected by matrix, affine
     * transforms or TFW files. The relationship from pixel/line (P,L) coordinates
     * to CRS are:
     *
     * {@preformat math
     *     X = c[0] + P*c[1] + L*c[2];
     *     Y = c[3] + P*c[4] + L*c[5];
     * }
     *
     * @param  mapping  the variable that contains attributes giving CRS definition.
     * @return the mapping, or {@code null} if this method did not found grid geometry attributes.
     */
    private static GridMapping parseGeoTransform(final Node mapping) {
        final String wkt = mapping.getAttributeAsString("spatial_ref");
        final String gtr = mapping.getAttributeAsString("GeoTransform");
        if (wkt == null && gtr == null) {
            return null;
        }
        short message = Resources.Keys.CanNotCreateCRS_3;
        CoordinateReferenceSystem crs = null;
        MathTransform gridToCRS = null;
        try {
            if (wkt != null) {
                crs = createFromWKT(mapping, wkt);
            }
            if (gtr != null) {
                message = Resources.Keys.CanNotCreateGridGeometry_3;
                final double[] c = CharSequences.parseDoubles(gtr, ' ');
                if (c.length == 6) {
                    gridToCRS = new AffineTransform2D(c[1], c[4], c[2], c[5], c[0], c[3]);         // X_DIMENSION, Y_DIMENSION
                } else {
                    canNotCreate(mapping, message, new DataStoreContentException(
                            Errors.getResources(mapping.getLocale())
                                  .getString(Errors.Keys.UnexpectedArrayLength_2, 6, c.length)));
                }
            }
        } catch (ParseException | NumberFormatException e) {
            canNotCreate(mapping, message, e);
        }
        return new GridMapping(crs, gridToCRS, false);
    }

    /**
     * Tries to parse the Coordinate Reference System using ESRI conventions or other non-CF conventions.
     * This method is invoked as a fallback if {@link #parseGeoTransform(Node)} found no grid geometry.
     *
     * @param  variable  the variable potentially with attributes to parse.
     * @return whether this method found grid geometry attributes.
     */
    private static GridMapping parseNonStandard(final Node variable) {
        boolean isEPSG = false;
        String code = variable.getAttributeAsString("ESRI_pe_string");
        if (code == null) {
            code = variable.getAttributeAsString("EPSG_code");
            if (code == null) {
                return null;
            }
            isEPSG = true;
        }
        /*
         * The Coordinate Reference System stored in those attributes often use the GeoTIFF flavor of EPSG codes,
         * with (longitude, latitude) axis order instead than the authoritative order specified in EPSG database.
         * Likewise, the "WKT 1" flavor used by ESRI is different than WKT 1 defined by OGC 01-009 specification.
         * The CRS parsings below need to take those differences in account, except axis order which is tested in
         * the `adaptGridCRS(…)` method.
         */
        CoordinateReferenceSystem crs;
        try {
            if (isEPSG) {
                crs = CRS.forCode(Constants.EPSG + ':' + isEPSG);
            } else {
                crs = createFromWKT(variable, code);
            }
        } catch (FactoryException | ParseException | ClassCastException e) {
            canNotCreate(variable, Resources.Keys.CanNotCreateCRS_3, e);
            crs = null;
        }
        return new GridMapping(crs, null, isEPSG);
    }

    /**
     * Creates a coordinate reference system by parsing a Well Known Text (WKT) string. The WKT is presumed
     * to use the GDAL flavor of WKT 1, and warnings are redirected to decoder listeners.
     */
    private static CoordinateReferenceSystem createFromWKT(final Node node, final String wkt) throws ParseException {
        final WKTFormat f = new WKTFormat(node.getLocale(), node.decoder.getTimeZone());
        f.setConvention(org.apache.sis.io.wkt.Convention.WKT1_COMMON_UNITS);
        final CoordinateReferenceSystem crs = (CoordinateReferenceSystem) f.parseObject(wkt);
        final Warnings warnings = f.getWarnings();
        if (warnings != null) {
            final LogRecord record = new LogRecord(Level.WARNING, warnings.toString());
            record.setLoggerName(Modules.NETCDF);
            record.setSourceClassName(Variable.class.getCanonicalName());
            record.setSourceMethodName("getGridGeometry");
            node.decoder.listeners.warning(record);
        }
        return crs;
    }

    /**
     * Logs a warning about a CRS or grid geometry that can not be created.
     * This method presumes that {@link GridMapping} are invoked (indirectly) from {@link Variable#getGridGeometry()}.
     *
     * @param  key  one of {@link Resources.Keys#CanNotCreateCRS_3} or {@link Resources.Keys#CanNotCreateGridGeometry_3}.
     * @param  ex   the exception that occurred while creating the CRS or grid geometry.
     */
    private static void canNotCreate(final Node node, final short key, final Exception ex) {
        NamedElement.warning(node.decoder.listeners, Variable.class, "getGridGeometry", ex, null,
                key, node.decoder.getFilename(), node.getName(), ex.getLocalizedMessage());
    }

    /**
     * Creates a new grid geometry for the given extent.
     * This method should be invoked only when no existing {@link GridGeometry} can be used as template.
     */
    GridGeometry createGridCRS(final Variable variable) {
        final List<Dimension> dimensions = variable.getGridDimensions();
        final long[] upper = new long[dimensions.size()];
        for (int i=0; i<upper.length; i++) {
            final int d = (upper.length - 1) - i;           // Convert CRS dimension to netCDF dimension.
            upper[i] = dimensions.get(d).length();
        }
        return new GridGeometry(new GridExtent(null, null, upper, false), PixelInCell.CELL_CENTER, gridToCRS, crs);
    }

    /**
     * Creates the grid geometry from the {@link #crs} and {@link #gridToCRS} field,
     * completing missing information with the given template.
     *
     * @param  variable  the variable for which to create a grid geometry.
     * @param  template  template to use for completing missing information.
     * @param  anchor    whether we computed "grid to CRS" transform relative to pixel center or pixel corner.
     * @return the grid geometry with modified CRS and "grid to CRS" transform, or {@code null} if case of failure.
     */
    GridGeometry adaptGridCRS(final Variable variable, final GridGeometry template, final PixelInCell anchor) {
        CoordinateReferenceSystem givenCRS = crs;
        int firstAffectedCoordinate = 0;
        boolean isSameGrid = true;
        if (template.isDefined(GridGeometry.CRS)) {
            final CoordinateReferenceSystem templateCRS = template.getCoordinateReferenceSystem();
            if (givenCRS == null) {
                givenCRS = templateCRS;
            } else {
                /*
                 * The CRS built by Grid may have a different axis order than the CRS specified by grid mapping attributes.
                 * Check which axis order seems to fit, then replace grid CRS by given CRS (potentially with swapped axes).
                 * This is where the potential difference between EPSG axis order and grid axis order is handled. If we can
                 * not find where to substitute the CRS, assume that the given CRS describes the first dimensions. We have
                 * no guarantees that this later assumption is right, but it seems to match common practice.
                 */
                final CoordinateSystem cs = templateCRS.getCoordinateSystem();
                CoordinateSystem subCS = givenCRS.getCoordinateSystem();
                firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, subCS);
                if (firstAffectedCoordinate < 0) {
                    givenCRS = AbstractCRS.castOrCopy(givenCRS).forConvention(AxesConvention.RIGHT_HANDED);
                    subCS = givenCRS.getCoordinateSystem();
                    firstAffectedCoordinate = AxisDirections.indexOfColinear(cs, subCS);
                    if (firstAffectedCoordinate < 0) {
                        firstAffectedCoordinate = 0;
                        if (!isEPSG) {
                            givenCRS = crs;                             // If specified by WKT, use the given CRS verbatim.
                            subCS = givenCRS.getCoordinateSystem();
                        }
                    }
                }
                /*
                 * Replace the grid CRS (or a component of it) by the CRS parsed from WKT or EPSG code with same (if possible)
                 * axis order. If the grid CRS contains more axes (for example elevation or time axis), we try to keep them.
                 */
                CoordinateReferenceSystem[] components = {
                    CRS.getComponentAt(templateCRS, 0, firstAffectedCoordinate), givenCRS,
                    CRS.getComponentAt(templateCRS, firstAffectedCoordinate + subCS.getDimension(), cs.getDimension())
                };
                int count = 0;
                for (CoordinateReferenceSystem c : components) {
                    if (c != null) components[count++] = c;
                }
                switch (count) {
                    case 0: /* Keep givenCRS as-is */ break;                // Should never happen.
                    case 1: givenCRS = components[0]; break;
                    default: {
                        components = ArraysExt.resize(components, count);
                        Map<String,?> properties = IdentifiedObjects.getProperties(templateCRS);
                        try {
                            givenCRS = variable.decoder.getCRSFactory().createCompoundCRS(properties, components);
                        } catch (FactoryException e) {
                            canNotCreate(variable, Resources.Keys.CanNotCreateCRS_3, e);
                            return null;
                        }
                        break;
                    }
                }
                isSameGrid = templateCRS.equals(givenCRS);
                if (isSameGrid) {
                    givenCRS = templateCRS;                                 // Keep existing instance if appropriate.
                }
            }
        }
        /*
         * Perform the same substitution than above, but in the "grid to CRS" transform. Note that the "grid to CRS"
         * is usually not specified, so the block performing substitution will rarely be executed. If executed, then
         * then we need to perform selection in target dimensions (not source dimensions) because the first affected
         * coordinate computed above is in CRS dimension, which is the target of "grid to CRS" transform.
         */
        MathTransform givenG2C = gridToCRS;
        if (template.isDefined(GridGeometry.GRID_TO_CRS)) {
            final MathTransform templateG2C = template.getGridToCRS(anchor);
            if (givenG2C == null) {
                givenG2C = templateG2C;
            } else try {
                int count = 0;
                MathTransform[] components = new MathTransform[3];
                final TransformSeparator sep = new TransformSeparator(templateG2C, variable.decoder.getMathTransformFactory());
                if (firstAffectedCoordinate != 0) {
                    sep.addTargetDimensionRange(0, firstAffectedCoordinate);
                    components[count++] = sep.separate();
                    sep.clear();
                }
                components[count++] = givenG2C;
                final int next = firstAffectedCoordinate + givenG2C.getTargetDimensions();
                final int upper = templateG2C.getTargetDimensions();
                if (next != upper) {
                    sep.addTargetDimensionRange(next, upper);
                    components[count++] = sep.separate();
                }
                components = ArraysExt.resize(components, count);
                givenG2C = MathTransforms.compound(components);
                if (templateG2C.equals(givenG2C)) {
                    givenG2C = templateG2C;                                 // Keep using existing instance if appropriate.
                } else {
                    isSameGrid = false;
                }
            } catch (FactoryException e) {
                canNotCreate(variable, Resources.Keys.CanNotCreateGridGeometry_3, e);
                return null;
            }
        }
        /*
         * At this point we finished to compute the grid geometry components.
         * If any of them have changed, create the new grid geometry.
         */
        if (isSameGrid) {
            return template;
        } else {
            return new GridGeometry(template.getExtent(), anchor, givenG2C, givenCRS);
        }
    }
}
