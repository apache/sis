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
package org.apache.sis.referencing.privy;

import java.util.Map;
import java.util.HashMap;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArraysExt;


/**
 * Constant strings for the keywords used in Well Known Text (WKT) formatting.
 * All constant values and identical to the names of the constants.
 *
 * <p><strong>Those constants should be used for WKT only.</strong>
 *
 * Do not use those constants in contexts unrelated to WKT (e.g. in {@code @XmlRootElement} annotations),
 * even if the string has the same value. It should be possible to change a keyword used in WKT formatting
 * without affecting GML for instance.</p>
 *
 * <h2>Implementation note</h2>
 * All constants in this class are static and final. The Java compiler should replace those constants
 * by their literal values at compile time, which avoid the loading of this class at run-time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class WKTKeywords extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private WKTKeywords() {
    }

    /**
     * Related to {@link org.apache.sis.referencing.AbstractIdentifiedObject}
     * (including {@link org.apache.sis.referencing.ImmutableIdentifier}).
     */
    public static final String
            Id        = "Id",
            URI       = "URI",
            Citation  = "Citation",
            Authority = "Authority",
            Anchor    = "Anchor",
            Usage     = "Usage",
            Scope     = "Scope",
            Area      = "Area",
            Remark    = "Remark";

    /**
     * Related to unit of measurements.
     */
    public static final String
            Unit             = "Unit",
            LengthUnit       = "LengthUnit",
            AngleUnit        = "AngleUnit",
            ScaleUnit        = "ScaleUnit",
            TimeUnit         = "TimeUnit",
            TemporalQuantity = "TemporalQuantity",
            ParametricUnit   = "ParametricUnit";

    /**
     * Related to {@link org.apache.sis.referencing.cs.AbstractCS}
     * and {@link org.apache.sis.referencing.datum.AbstractDatum}.
     */
    public static final String
            CS            = "CS",
            Axis          = "Axis",
            AxisMinValue  = "AxisMinValue",
            AxisMaxValue  = "AxisMaxValue",
            RangeMeaning  = "RangeMeaning",
            Order         = "Order",
            Meridian      = "Meridian",
            PrimeMeridian = "PrimeMeridian",
            PrimeM        = "PrimeM",
            Ellipsoid     = "Ellipsoid",
            Spheroid      = "Spheroid",
            Ensemble      = "Ensemble",
            ToWGS84       = "ToWGS84";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultGeocentricCRS}
     * and {@link org.apache.sis.referencing.crs.DefaultGeographicCRS}.
     */
    public static final String
            BBox          = "BBox",
            Datum         = "Datum",
            GeodeticDatum = "GeodeticDatum",
            GeodeticCRS   = "GeodeticCRS",
            GeographicCRS = "GeographicCRS",
            BaseGeodCRS   = "BaseGeodCRS",
            BaseGeogCRS   = "BaseGeogCRS",
            GeodCRS       = "GeodCRS",
            GeogCRS       = "GeogCRS",
            GeogCS        = "GeogCS",
            GeocCS        = "GeocCS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultVerticalCRS}.
     */
    public static final String
            VerticalExtent = "VerticalExtent",
            VerticalDatum  = "VerticalDatum",
            VerticalCRS    = "VerticalCRS",
            BaseVertCRS    = "BaseVertCRS",
            VDatum         = "VDatum",
            Vert_Datum     = "Vert_Datum",
            VertCRS        = "VertCRS",
            Vert_CS        = "Vert_CS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultTemporalCRS}.
     */
    public static final String
            TimeExtent  = "TimeExtent",
            TimeOrigin  = "TimeOrigin",
            TimeDatum   = "TimeDatum",
            TDatum      = "TDatum",
            TimeCRS     = "TimeCRS",
            BaseTimeCRS = "BaseTimeCRS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultParametricCRS}.
     */
    public static final String
            ParametricDatum = "ParametricDatum",
            PDatum          = "PDatum",
            ParametricCRS   = "ParametricCRS",
            BaseParamCRS    = "BaseParamCRS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultImageCRS}
     * and {@link org.apache.sis.referencing.crs.DefaultEngineeringCRS}.
     * Former can be seen as a special case of the latter.
     */
    public static final String
            ImageDatum       = "ImageDatum",
            ImageCRS         = "ImageCRS",
            IDatum           = "IDatum",
            EngineeringDatum = "EngineeringDatum",
            EngineeringCRS   = "EngineeringCRS",
            BaseEngCRS       = "BaseEngCRS",
            EngCRS           = "EngCRS",
            EDatum           = "EDatum",
            Local_Datum      = "Local_Datum",
            Local_CS         = "Local_CS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultCompoundCRS}.
     */
    public static final String
            CompoundCRS = "CompoundCRS",
            Compd_CS    = "Compd_CS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultProjectedCRS}.
     */
    public static final String
            ProjectedCRS = "ProjectedCRS",
            BaseProjCRS  = "BaseProjCRS",
            ProjCRS      = "ProjCRS",
            ProjCS       = "ProjCS";

    /**
     * Related to {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}.
     */
    public static final String
            BoundCRS            = "BoundCRS",
            Method              = "Method",
            Formula             = "Formula",
            Projection          = "Projection",
            Conversion          = "Conversion",
            DerivingConversion  = "DerivingConversion",
            CoordinateOperation = "CoordinateOperation",
            OperationAccuracy   = "OperationAccuracy",
            SourceCRS           = "SourceCRS",
            TargetCRS           = "TargetCRS",
            InterpolationCRS    = "InterpolationCRS",
            Parameter           = "Parameter",
            ParameterFile       = "ParameterFile",
            ParameterGroup      = "ParameterGroup",
            GeogTran            = "GeogTran";               // ESRI-specific.

    /**
     * Related to {@link org.apache.sis.referencing.operation.transform.AbstractMathTransform}.
     */
    public static final String
            Param_MT       = "Param_MT",
            Inverse_MT     = "Inverse_MT",
            Concat_MT      = "Concat_MT",
            PassThrough_MT = "PassThrough_MT",
            Fitted_CS      = "Fitted_CS";

    /**
     * Coordinate system types. They are not WKT keywords, but are legal values for the "type"
     * element in a {@code "CS[…]"} element.
     */
    public static final String
            affine      = "affine",
            Cartesian   = "Cartesian",      // Upper case 'C' is intentional.
            cylindrical = "cylindrical",
            ellipsoidal = "ellipsoidal",
            linear      = "linear",
            parametric  = "parametric",
            polar       = "polar",
            spherical   = "spherical",
            temporal    = "temporal",
            vertical    = "vertical";

    /**
     * Coordinates and epoch.
     */
    public static final String
            CoordinateMetadata = "CoordinateMetadata",
            Epoch              = "Epoch",
            Point              = "Point";

    /**
     * Mapping between types of object and WKT keywords. Each GeoAPI interfaces is associated to one
     * or many WKT keywords: new keywords defined by version 2 (sometimes with synonymous) and legacy
     * keywords defined by WKT 1.
     *
     * @see #forType(Class)
     */
    private static final Map<Class<?>,String[]> TYPES = new HashMap<>(30);
    static {
        /*
         * `SingleCRS` subtypes. The `GeographicCRS` and `GeocentricCRS` types are handled separately
         * because they are implied by the `GeodeticCRS` type in the `subtypes` array, so they do not
         * need to be repeated there.
         */
        addType(org.opengis.referencing.crs.GeographicCRS.class,  GeodeticCRS, GeodCRS, GeogCS);
        String[][] subtypes;
        subtypes = new String[][] {
            addType(org.opengis.referencing.crs.GeodeticCRS.class,    GeodeticCRS,    GeodCRS, GeocCS, GeogCS),
            addType(org.opengis.referencing.crs.ProjectedCRS.class,   ProjectedCRS,   ProjCRS, ProjCS),
            addType(org.opengis.referencing.crs.VerticalCRS.class,    VerticalCRS,    VertCRS, Vert_CS),
            addType(org.opengis.referencing.crs.EngineeringCRS.class, EngineeringCRS, EngCRS,  Local_CS),
            addType(org.opengis.referencing.crs.DerivedCRS.class,     /* TODO: check ISO. */   Fitted_CS),
            addType(org.opengis.referencing.crs.TemporalCRS.class,    TimeCRS)
        };
        /*
         * `CoordinateReferenceSystem` subtypes: all `SingleCRS` + the `CompoundCRS`.
         */
        subtypes = new String[][] {
            addType(org.opengis.referencing.crs.SingleCRS.class, ArraysExt.concatenate(subtypes)),
            addType(org.opengis.referencing.crs.CompoundCRS.class, CompoundCRS, Compd_CS),
        };
        addType(org.opengis.referencing.crs.CoordinateReferenceSystem.class, ArraysExt.concatenate(subtypes));
        /*
         * `Datum` subtypes.
         */
        subtypes = new String[][] {
            addType(org.opengis.referencing.datum.GeodeticDatum.class,    GeodeticDatum,    Datum),
            addType(org.opengis.referencing.datum.TemporalDatum.class,    TimeDatum,        TDatum),
            addType(org.opengis.referencing.datum.VerticalDatum.class,    VerticalDatum,    VDatum, Vert_Datum),
            addType(org.opengis.referencing.datum.EngineeringDatum.class, EngineeringDatum, EDatum, Local_Datum)
        };
        addType(org.opengis.referencing.datum.Datum.class, ArraysExt.concatenate(subtypes));
        /*
         * Other types having no common parent (ignoring `IdentifiedObject`).
         */
        addType(org.opengis.referencing.datum.Ellipsoid.class,              Ellipsoid, Spheroid);
        addType(org.opengis.referencing.datum.PrimeMeridian.class,          PrimeMeridian, PrimeM);
        addType(org.opengis.referencing.cs.CoordinateSystemAxis.class,      Axis);
        addType(org.apache.sis.referencing.datum.BursaWolfParameters.class, ToWGS84);
        addType(org.opengis.referencing.operation.MathTransform.class,      Param_MT, Concat_MT, Inverse_MT, PassThrough_MT);
        addType(org.opengis.coordinate.CoordinateMetadata.class,            CoordinateMetadata);
        addType(org.opengis.geometry.DirectPosition.class,                  Point);
    }

    /**
     * Adds WKT keywords for the specified type.
     */
    private static String[] addType(final Class<?> type, final String... keywords) {
        if (TYPES.put(type, keywords) != null) {
            throw new AssertionError(type);
        }
        return keywords;
    }

    /**
     * Returns the WKT keywords for an object of the specified type.
     * This method returns a direct reference to internal array;
     * <strong>do not modify</strong>.
     *
     * @param  type  the GeoAPI interface of the element.
     * @return the WKT keywords, or {@code null} if none or unknown.
     */
    public static String[] forType(final Class<?> type) {
        return TYPES.get(type);
    }
}
